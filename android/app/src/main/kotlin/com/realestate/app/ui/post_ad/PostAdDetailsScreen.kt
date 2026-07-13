package com.realestate.app.ui.post_ad

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.realestate.app.ui.theme.*
import com.yalantis.ucrop.UCrop
import java.io.File

// ── UCrop helper ─────────────────────────────────────────────────────────────

/** Create a temp file URI that UCrop can write its cropped output to. */
private fun createCropOutputUri(context: Context): Uri {
    val dir  = File(context.cacheDir, "ucrop").apply { mkdirs() }
    val file = File(dir, "crop_${System.currentTimeMillis()}.jpg")
    return Uri.fromFile(file)
}

/** Build and return a UCrop intent — free-form aspect ratio, max 1080×1080 px. */
private fun buildCropIntent(context: Context, source: Uri, dest: Uri) =
    UCrop.of(source, dest)
        .withMaxResultSize(1080, 1080)
        .withOptions(UCrop.Options().apply {
            setCompressionQuality(90)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)   // user can freely drag the crop rect
            setToolbarTitle("Crop Image")
            setToolbarColor(android.graphics.Color.parseColor("#1565C0"))   // NestX blue
            setStatusBarColor(android.graphics.Color.parseColor("#0D47A1"))
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#1565C0"))
        })
        .getIntent(context)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostAdDetailsScreen(
    viewModel: PostAdViewModel,
    onSubmit: () -> Unit,
    onPickOnMap: () -> Unit,
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit = {},
) {
    val submitState    by viewModel.submitState.collectAsState()
    val selectedImages by viewModel.selectedImageUris.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val postedBy         by viewModel.postedBy.collectAsState()
    val subCategory      by viewModel.subCategory.collectAsState()
    val adTitle          by viewModel.title.collectAsState()
    val subscription     by viewModel.subscriptionDetails.collectAsState()

    val context = LocalContext.current

    var showImageSourceSheet by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Subscription Limit Reached", fontWeight = FontWeight.Bold) },
            text = {
                val current = subscription?.currentListingsCount ?: 0
                val max = subscription?.maxListings ?: 3
                Text("You have active listings: $current. Your current plan allows up to $max active listings. Please upgrade your subscription plan to publish more.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLimitDialog = false
                        onUpgradeClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Upgrade Plan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── URI of image being cropped (so we know which slot to replace) ─────────
    var cropSourceUri  by remember { mutableStateOf<Uri?>(null) }   // original
    var cropOutputUri  by remember { mutableStateOf<Uri?>(null) }   // ucrop dest

    // ── UCrop result launcher ─────────────────────────────────────────────────
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultUri = if (result.resultCode == Activity.RESULT_OK && result.data != null)
            UCrop.getOutput(result.data!!) else null

        if (resultUri != null) {
            val original = cropSourceUri
            if (original != null) {
                // re-crop of existing thumbnail
                viewModel.replaceImage(original, resultUri)
            } else {
                // new image from camera or gallery
                viewModel.addImages(listOf(resultUri))
            }
        }
        // reset crop tracking
        cropSourceUri = null
        cropOutputUri = null
    }

    /** Launch UCrop for a given source URI (new image or re-crop of existing). */
    fun launchCrop(source: Uri, originalUri: Uri? = null) {
        val dest = createCropOutputUri(context)
        cropSourceUri = originalUri
        cropOutputUri = dest
        cropLauncher.launch(buildCropIntent(context, source, dest))
    }

    // ── Camera — permission + TakePicture ────────────────────────────────────
    //
    // CAMERA is a "dangerous" permission on Android 6+ and must be requested at
    // runtime even if it is declared in the manifest. Without the runtime grant
    // the system throws SecurityException: "Permission Denial … with revoked
    // permission android.permission.CAMERA".
    //
    // Flow:
    //   1. User taps "Take Photo"
    //   2. checkAndLaunchCamera() checks current grant status
    //   3a. Granted → call launchCamera() directly
    //   3b. Not granted → cameraPermissionLauncher requests the permission
    //   4. On grant callback → set triggerCameraLaunch = true
    //   5. LaunchedEffect(triggerCameraLaunch) fires launchCamera() safely
    //   6. On deny → show in-line error text

    var cameraTempUri        by remember { mutableStateOf<Uri?>(null) }
    var triggerCameraLaunch  by remember { mutableStateOf(false) }
    var cameraPermDenied     by remember { mutableStateOf(false) }

    // Step 3b: request CAMERA permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPermDenied    = false
            triggerCameraLaunch = true   // signal LaunchedEffect to fire launchCamera()
        } else {
            cameraPermDenied = true      // show message to user
        }
    }

    // Step 4–5: react to permission grant
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraTempUri?.let { launchCrop(it) }
        cameraTempUri = null
    }

    // Actual camera launch — creates a FileProvider URI and fires TakePicture
    fun launchCamera() {
        val dir  = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        val uri  = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraTempUri = uri
        cameraLauncher.launch(uri)
    }

    // Step 2: check grant status before launching
    fun checkAndLaunchCamera() {
        cameraPermDenied = false
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Fires launchCamera() on the main composition thread after permission grant
    LaunchedEffect(triggerCameraLaunch) {
        if (triggerCameraLaunch) {
            triggerCameraLaunch = false
            launchCamera()
        }
    }

    // ── Gallery — pick ONE image then send to UCrop ───────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { launchCrop(it) }
    }

    LaunchedEffect(submitState) {
        if (submitState is SubmitState.Success ||
            submitState is SubmitState.SuccessWithImageError) {
            viewModel.resetWizard()
            onSubmit()
        }
    }

    // ── Image Source Bottom Sheet ─────────────────────────────────────────────
    if (showImageSourceSheet) {
        val remaining = MAX_IMAGES - selectedImages.size
        ModalBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            containerColor   = Color.White,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text       = "Add Photo ($remaining slot${if (remaining != 1) "s" else ""} left)",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                HorizontalDivider(color = BorderColor)

                // ── Take Photo ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showImageSourceSheet = false
                            checkAndLaunchCamera()   // checks CAMERA permission first
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NestXBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.CameraAlt, "Camera", tint = NestXBlue, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Take Photo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Open camera to capture a new photo", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp))

                // ── Choose from Gallery ───────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showImageSourceSheet = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NestXBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, "Gallery", tint = NestXBlue, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Choose from Gallery", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Pick a photo from your device", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ad Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.submitAd() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = submitState !is SubmitState.Loading,
                ) {
                    if (submitState is SubmitState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Post Ad", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Category selection ───────────────────────────────────────────
            Text("Category", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf(
                    Triple("Buy", Icons.Filled.Home, "Property for Sale"),
                    Triple("Rent", Icons.Filled.VpnKey, "Property for Rent"),
                    Triple("Construction", Icons.Filled.Construction, "Construction Services"),
                    Triple("Maintenance", Icons.Filled.Build, "Maintenance Services")
                )
                cats.forEach { (label, icon, key) ->
                    val isSelected = selectedCategory == key
                    val gradColors = when (key) {
                        "Property for Sale"     -> listOf(Color(0xFF1565C0), Color(0xFF1976D2))
                        "Property for Rent"     -> listOf(Color(0xFF00796B), Color(0xFF00897B))
                        "Construction Services" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
                        "Maintenance Services"  -> listOf(Color(0xFF6A1B9A), Color(0xFF7B1FA2))
                        else                    -> listOf(NestXBlue, NestXBlueDark)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = if (isSelected) {
                                    Brush.verticalGradient(gradColors)
                                } else {
                                    Brush.verticalGradient(listOf(SurfaceGray, SurfaceGray))
                                }
                            )
                            .clickable {
                                val details = subscription
                                if (details != null && details.currentListingsCount >= details.maxListings) {
                                    showLimitDialog = true
                                } else {
                                    viewModel.selectedCategory.value = key
                                    // Reset subcategory and role to match new category
                                    viewModel.subCategory.value = ""
                                    val pair = resolveRolePair(key)
                                    viewModel.postedBy.value = pair.leftKey
                                }
                            }
                            .then(
                                if (!isSelected) {
                                    Modifier.border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) Color.White else TextSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            if (selectedCategory.isNotBlank()) {
                val rolePair = remember(selectedCategory) { resolveAdRoleSelectionPair(selectedCategory) }

                // ── Posted By Role selection ─────────────────────────────────
                Spacer(Modifier.height(4.dp))
                Text(rolePair.heading, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val roles = listOf(rolePair.leftLabel to rolePair.leftKey, rolePair.rightLabel to rolePair.rightKey)
                    roles.forEach { (label, key) ->
                        val isSelected = postedBy == key
                        OutlinedButton(
                            onClick = { viewModel.postedBy.value = key },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, if (isSelected) NestXBlue else BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) NestXBlue.copy(alpha = 0.08f) else Color.Transparent,
                                contentColor = if (isSelected) NestXBlue else TextSecondary
                            )
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                // ── Sub-category selection ───────────────────────────────────
                val subCategories = when {
                    selectedCategory.contains("Sale", ignoreCase = true) -> saleSubCategories
                    selectedCategory.contains("Rent", ignoreCase = true) -> rentSubCategories
                    selectedCategory.contains("Construction", ignoreCase = true) -> constructionSubCategories
                    selectedCategory.contains("Maintenance", ignoreCase = true) -> maintenanceSubCategories
                    else -> emptyList()
                }

                if (subCategories.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Sub-category", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        subCategories.forEach { sc ->
                            val isSelected = subCategory == sc
                            val bgColor = if (isSelected) NestXBlue.copy(alpha = 0.08f) else Color.Transparent
                            val borderColor = if (isSelected) NestXBlue else BorderColor
                            val contentColor = if (isSelected) NestXBlue else TextSecondary
                            
                            Surface(
                                onClick = {
                                    viewModel.subCategory.value = sc
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.5.dp, borderColor),
                                color = bgColor,
                            ) {
                                Text(
                                    text = sc,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = contentColor,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // ── Ad Title input ───────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                Text("Ad Title", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = adTitle,
                    onValueChange = { viewModel.title.value = it },
                    placeholder = { Text("e.g. 3 BHK Apartment in Gandhipuram") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NestXBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )

                HorizontalDivider(color = BorderColor)

                // ── Photos (2–6 required, each cropped via UCrop) ────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SectionLabel("Photos")
                    Spacer(Modifier.width(6.dp))
                    Text("(min $MIN_IMAGES, max $MAX_IMAGES)", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text       = "${selectedImages.size}/$MAX_IMAGES",
                        fontSize   = 12.sp,
                        color      = if (selectedImages.size < MIN_IMAGES) PrimaryRed else TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Photo source hint
                Text(
                    "Tap + to take a photo or pick from gallery. Tap ✂ on any photo to crop it.",
                    fontSize = 11.sp,
                    color    = TextSecondary,
                )

                // Grid: show thumbnails + an "Add" button if slots remain
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    selectedImages.forEach { uri ->
                        ImageThumb(
                            uri      = uri,
                            onRemove = { viewModel.removeImage(uri) },
                            onCrop   = { launchCrop(uri, originalUri = uri) },
                        )
                    }
                    if (selectedImages.size < MAX_IMAGES) {
                        AddPhotoButton(
                            count   = selectedImages.size,
                            onClick = { showImageSourceSheet = true },
                        )
                    }
                }

                if (selectedImages.size < MIN_IMAGES) {
                    Text(
                        text     = "Please add at least $MIN_IMAGES photos so buyers can see your property.",
                        fontSize = 12.sp,
                        color    = TextSecondary,
                    )
                }

                HorizontalDivider(color = BorderColor)

                // ── Category-specific form body ─────────────────────────────────
                when {
                    selectedCategory.contains("Ground", ignoreCase = true) ->
                        GroundDetailsForm(viewModel = viewModel, onPickOnMap = onPickOnMap)

                    selectedCategory.contains("Construction", ignoreCase = true) ->
                        ContractorDetailsForm(viewModel = viewModel, onPickOnMap = onPickOnMap, isConstruction = true)

                    selectedCategory.contains("Maintenance", ignoreCase = true) ->
                        ContractorDetailsForm(viewModel = viewModel, onPickOnMap = onPickOnMap, isConstruction = false)

                    selectedCategory.contains("Holiday", ignoreCase = true) ->
                        HolidayStayDetailsForm(viewModel = viewModel, onPickOnMap = onPickOnMap)

                    else ->
                        // Property for Sale / Property for Rent (default)
                        PropertyDetailsForm(viewModel = viewModel, onPickOnMap = onPickOnMap)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Please select a category above to post your ad.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            // Admin review note
            Text(
                text     = "⚠️ Your listing will be reviewed by an admin before it goes live.",
                color    = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            // Error display
            when (val s = submitState) {
                is SubmitState.Error -> Text(
                    text     = s.message,
                    color    = PrimaryRed,
                    fontSize = 13.sp,
                )
                is SubmitState.SuccessWithImageError -> Text(
                    text     = "⚠️ Listing created but image upload failed: ${s.imageError}",
                    color    = Color(0xFFF57C00), // amber warning
                    fontSize = 13.sp,
                )
                else -> Unit
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

/**
 * Image thumbnail with two overlay actions:
 *  ✕ top-right   → remove the image
 *  ✂ bottom-left → re-crop via UCrop
 */
@Composable
private fun ImageThumb(uri: Uri, onRemove: () -> Unit, onCrop: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(10.dp)),
    ) {
        AsyncImage(
            model              = uri,
            contentDescription = "Selected photo",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )

        // ── ✕ Remove (top-right) ──────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .background(Color.Black.copy(alpha = 0.60f), CircleShape)
                .clickable { onRemove() },
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove photo",
                tint     = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }

        // ── ✂ Crop (bottom-left) ──────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .size(22.dp)
                .background(NestXBlue.copy(alpha = 0.82f), CircleShape)
                .clickable { onCrop() },
        ) {
            Icon(
                Icons.Filled.Crop,
                contentDescription = "Crop photo",
                tint     = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun AddPhotoButton(count: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.size(90.dp),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.5.dp, PrimaryRed),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
        contentPadding = PaddingValues(4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo", modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                if (count == 0) "Add Photo" else "Add More",
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
}

private data class AdRoleSelectionPair(
    val leftKey: String, val leftLabel: String,
    val rightKey: String, val rightLabel: String,
    val heading: String,
)

private fun resolveAdRoleSelectionPair(selectedCategory: String): AdRoleSelectionPair = when {
    selectedCategory.contains("Holiday Stay", ignoreCase = true) ->
        AdRoleSelectionPair(
            leftKey = "landlord",
            leftLabel = "Owner",
            rightKey = "agent",
            rightLabel = "Agent",
            heading = "Are you an owner or an agent?",
        )
    selectedCategory.trim().equals("Ground", ignoreCase = true) ->
        AdRoleSelectionPair(
            leftKey    = "landlord",
            leftLabel  = "Owner",
            rightKey   = "agent",
            rightLabel = "Manager",
            heading    = "Are you the owner or a manager?",
        )
    selectedCategory.contains("Contractor", ignoreCase = true) ||
    selectedCategory.contains("Construction", ignoreCase = true) ||
    selectedCategory.contains("Maintenance", ignoreCase = true) ->
        AdRoleSelectionPair(
            leftKey   = "individual",
            leftLabel = "Individual",
            rightKey  = "company",
            rightLabel = "Company",
            heading   = "Are you an individual or a company?",
        )
    else ->
        AdRoleSelectionPair(
            leftKey   = "landlord",
            leftLabel = "Owner",
            rightKey  = "agent",
            rightLabel = "Agent",
            heading    = "Are you an owner or an agent?",
        )
}
