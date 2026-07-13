package com.realestate.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val city by viewModel.city.collectAsState()
    val language by viewModel.language.collectAsState()

    var showSavedSnackbar by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state is ProfileState.Saved) showSavedSnackbar = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
        snackbarHost = {
            if (showSavedSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = TextPrimary,
                    action = {
                        TextButton(onClick = { showSavedSnackbar = false }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text("Profile saved!", color = Color.White)
                }
            }
        },
    ) { padding ->
        when (state) {
            is ProfileState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryRed)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(OnboardingBlob),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(44.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {}) {
                        Text("Change Photo", color = PrimaryRed, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { viewModel.fullName.value = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryRed, unfocusedBorderColor = BorderColor),
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { viewModel.phone.value = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryRed, unfocusedBorderColor = BorderColor),
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = { viewModel.city.value = it },
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryRed, unfocusedBorderColor = BorderColor),
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = language,
                        onValueChange = { viewModel.language.value = it },
                        label = { Text("Language") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryRed, unfocusedBorderColor = BorderColor),
                    )

                    Spacer(Modifier.height(28.dp))

                    if (state is ProfileState.Error) {
                        Text(
                            text = (state as ProfileState.Error).message,
                            color = PrimaryRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    Button(
                        onClick = { viewModel.saveProfile() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
