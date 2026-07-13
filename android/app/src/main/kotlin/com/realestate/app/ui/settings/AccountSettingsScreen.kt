package com.realestate.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onBack: () -> Unit,
) {
    val state    by viewModel.state.collectAsState()
    val user     by viewModel.user.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val phone    by viewModel.phone.collectAsState()
    val city     by viewModel.city.collectAsState()
    val language by viewModel.language.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (state) {
            is AccountSettingsState.Saved ->
                snackbarHostState.showSnackbar("Account updated successfully ✓")
            is AccountSettingsState.Error ->
                snackbarHostState.showSnackbar((state as AccountSettingsState.Error).message)
            else -> Unit
        }
        viewModel.resetState()
    }

    // Language picker dialog
    var showLanguagePicker by remember { mutableStateOf(false) }
    if (showLanguagePicker) {
        val languages = listOf("English", "தமிழ் (Tamil)", "Hindi", "Telugu", "Kannada")
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("Select Language", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = language == lang,
                                onClick  = {
                                    viewModel.language.value = lang
                                    showLanguagePicker = false
                                },
                                colors   = RadioButtonDefaults.colors(selectedColor = NestXBlue),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(lang, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = SurfaceGray,
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {

            Spacer(Modifier.height(8.dp))

            // ── User ID code ─────────────────────────────────────────────────
            user?.userIdCode?.let { code ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color  = BannerBlue,
                    shape  = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.Badge, null, tint = NestXBlue, modifier = Modifier.size(22.dp))
                        Column {
                            Text("Your User ID Code", fontSize = 11.sp, color = TextSecondary)
                            Text(code, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NestXBlue)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Personal info section ─────────────────────────────────────────
            SettingsSectionHeader("Personal Information")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color  = Color.White,
                shape  = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsTextField(
                        icon        = Icons.Default.Person,
                        label       = "Full Name",
                        value       = fullName,
                        onValueChange = { viewModel.fullName.value = it },
                    )
                    Spacer(Modifier.height(12.dp))
                    // Email (read-only)
                    Surface(
                        color = SurfaceGray,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Default.Email, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Email", fontSize = 11.sp, color = TextSecondary)
                                Text(user?.email ?: "—", fontSize = 14.sp, color = TextPrimary)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NestXBlue.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    "Read-only",
                                    fontSize = 10.sp,
                                    color    = NestXBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SettingsTextField(
                        icon        = Icons.Default.Phone,
                        label       = "Phone Number",
                        value       = phone,
                        onValueChange = { viewModel.phone.value = it },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Preferences section ───────────────────────────────────────────
            SettingsSectionHeader("Preferences")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color  = Color.White,
                shape  = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsTextField(
                        icon        = Icons.Default.LocationCity,
                        label       = "City / District",
                        value       = city,
                        onValueChange = { viewModel.city.value = it },
                    )
                    Spacer(Modifier.height(12.dp))
                    // Language selector (tap to show picker)
                    Surface(
                        color     = SurfaceGray,
                        shape     = RoundedCornerShape(10.dp),
                        modifier  = Modifier.fillMaxWidth(),
                        onClick   = { showLanguagePicker = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Default.Language, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Language", fontSize = 11.sp, color = TextSecondary)
                                Text(language, fontSize = 14.sp, color = TextPrimary)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Account type (read-only) ──────────────────────────────────────
            SettingsSectionHeader("Account Type")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color  = Color.White,
                shape  = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.ManageAccounts, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Role", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            user?.role?.displayName ?: "Buyer",
                            fontSize   = 15.sp,
                            color      = TextPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        "Contact support to change",
                        fontSize = 11.sp,
                        color    = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                enabled  = state !is AccountSettingsState.Loading,
            ) {
                if (state is AccountSettingsState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        fontSize   = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextSecondary,
        modifier   = Modifier.padding(start = 20.dp, bottom = 6.dp, top = 4.dp),
    )
}

@Composable
private fun SettingsTextField(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontSize = 13.sp) },
        leadingIcon   = { Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = NestXBlue,
            unfocusedBorderColor = BorderColor,
        ),
    )
}
