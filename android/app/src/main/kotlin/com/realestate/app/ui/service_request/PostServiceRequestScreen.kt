package com.realestate.app.ui.service_request

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.RequestUrgency
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.ui.components.RealEstateFilterChip
import com.realestate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostServiceRequestScreen(
    viewModel: ServiceRequestViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val postState by viewModel.postState.collectAsState()

    var category by remember { mutableStateOf("construction") } // "construction" | "maintenance"
    var serviceType by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var budgetMin by remember { mutableStateOf("") }
    var budgetMax by remember { mutableStateOf("") }
    var radiusKm by remember { mutableStateOf(50) }
    var urgency by remember { mutableStateOf(RequestUrgency.NORMAL) }
    var preferredDate by remember { mutableStateOf<String?>(null) }
    var contactPhone by remember { mutableStateOf("") }

    var selectedImages by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    var districtExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = System.currentTimeMillis() - (System.currentTimeMillis() % 86_400_000L)
                return utcTimeMillis >= today
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            preferredDate = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK", color = PrimaryRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = PrimaryRed,
                    todayDateBorderColor = PrimaryRed,
                    todayContentColor = PrimaryRed,
                ),
            )
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        selectedImages = (selectedImages + uris).take(5)
    }

    LaunchedEffect(postState) {
        if (postState is PostRequestState.Success) {
            viewModel.postState.value = PostRequestState.Idle
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a Service Request", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Broadcast what you need to local builders, contractors and technicians. They will submit direct quotations.",
                fontSize = 13.sp,
                color = TextSecondary
            )

            // Category select
            Text("Service Category", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RealEstateFilterChip(
                    label = "Construction Work",
                    selected = category == "construction",
                    onClick = { category = "construction"; serviceType = "" }
                )
                RealEstateFilterChip(
                    label = "Maintenance & Repair",
                    selected = category == "maintenance",
                    onClick = { category = "maintenance"; serviceType = "" }
                )
            }

            // Work Type / Service Type (e.g. Civil Contractor, Electrician)
            Text("Service Type / Subcategory", fontWeight = FontWeight.Bold)
            val subCategories = if (category == "construction") {
                listOf("Civil Contractors", "Builders", "Architects", "Structural Engineers", "Interior Designers", "Landscaping")
            } else {
                listOf("Electrician", "Plumber", "Carpenter", "AC Service", "CCTV", "Cleaning", "Pest Control")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subCategories.forEach { sub ->
                    RealEstateFilterChip(
                        label = sub,
                        selected = serviceType == sub,
                        onClick = { serviceType = sub }
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What do you need? (Title)") },
                placeholder = { Text("e.g. Need civil contractor to build 2BHK house") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe details / requirements") },
                placeholder = { Text("Include area size, flooring details, timelines, specific preferences...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(10.dp),
                maxLines = 6
            )

            // Location
            Text("Where do you need the service?", fontWeight = FontWeight.Bold)
            ExposedDropdownMenuBox(
                expanded = districtExpanded,
                onExpandedChange = { districtExpanded = it }
            ) {
                OutlinedTextField(
                    value = district.ifBlank { "Select District" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = districtExpanded,
                    onDismissRequest = { districtExpanded = false }
                ) {
                    TamilNaduData.districts.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d) },
                            onClick = { district = d; districtExpanded = false }
                        )
                    }
                }
            }

            // Budget
            Text("Estimate Budget Range (Optional)", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = budgetMin,
                    onValueChange = { budgetMin = it },
                    label = { Text("Min Budget (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = budgetMax,
                    onValueChange = { budgetMax = it },
                    label = { Text("Max Budget (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Broadcast Radius
            Text("Broadcast Radius", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10 to "10 km", 50 to "50 km", 100 to "100 km").forEach { (km, label) ->
                    RealEstateFilterChip(
                        label = label,
                        selected = radiusKm == km,
                        onClick = { radiusKm = km }
                    )
                }
            }

            // Urgency
            Text("How urgent is this?", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    RequestUrgency.NORMAL to "Normal",
                    RequestUrgency.URGENT to "🟡 Urgent",
                    RequestUrgency.EMERGENCY to "🔴 Emergency",
                ).forEach { (level, label) ->
                    RealEstateFilterChip(
                        label = label,
                        selected = urgency == level,
                        onClick = { urgency = level }
                    )
                }
            }

            // Preferred start date
            Text("Preferred Start Date (Optional)", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = preferredDate?.let {
                    try {
                        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val outFmt = SimpleDateFormat("d MMM yyyy", Locale.US)
                        outFmt.format(inFmt.parse(it)!!)
                    } catch (e: Exception) { it }
                } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Flexible / anytime") },
                trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = BorderColor,
                    disabledPlaceholderColor = TextSecondary,
                    disabledTrailingIconColor = TextSecondary,
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Direct contact phone
            Text("Contact Phone (Optional)", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Phone number") },
                placeholder = { Text("e.g. +91 98765 43210") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Text(
                "Shown to contractors on the request detail screen so they can reach you directly.",
                fontSize = 11.sp,
                color = TextSecondary
            )

            // Photos Selector
            Text("Photos (Up to 5, Optional)", fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedImages.size < 5) {
                    Card(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddAPhoto, "Add Photo", tint = TextSecondary)
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(selectedImages) { uri ->
                        Box(modifier = Modifier.size(72.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { selectedImages = selectedImages.filter { it != uri } },
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (postState is PostRequestState.Error) {
                Text((postState as PostRequestState.Error).message, color = PrimaryRed, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (title.isBlank() || district.isBlank() || serviceType.isBlank()) {
                        viewModel.postState.value = PostRequestState.Error("Please fill out Title, District, and Service Type.")
                    } else {
                        viewModel.createServiceRequest(
                            category = category,
                            serviceType = serviceType,
                            title = title,
                            description = description.ifBlank { null },
                            district = district,
                            budgetMin = budgetMin.toDoubleOrNull(),
                            budgetMax = budgetMax.toDoubleOrNull(),
                            radiusKm = radiusKm,
                            imageUris = selectedImages,
                            urgency = urgency.key,
                            preferredDate = preferredDate,
                            contactPhone = contactPhone.ifBlank { null }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
     