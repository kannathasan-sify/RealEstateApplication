package com.realestate.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            onSuccess()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Join NestX", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Create your account to get started", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (Optional)") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = TextSecondary,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = TextSecondary,
                        )
                    }
                },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
            )

            // Clear API error when user edits the email field
            LaunchedEffect(email) {
                if ((state as? AuthUiState.Error)?.message?.contains("already exists", ignoreCase = true) == true) {
                    viewModel.resetState()
                }
            }

            Spacer(Modifier.height(24.dp))

            val errorMsg = localError ?: (state as? AuthUiState.Error)?.message
            val isEmailTaken = errorMsg?.contains("already exists", ignoreCase = true) == true

            // ── "Email already exists" — special card with Login CTA ──────────
            if (isEmailTaken) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color    = NestXBlue.copy(alpha = 0.08f),
                    shape    = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment      = Alignment.CenterVertically,
                            horizontalArrangement  = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint     = NestXBlue,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "An account with this email already exists.",
                                fontSize   = 13.sp,
                                color      = NestXBlue,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            "Did you sign up with Google? Tap \"Log In\" and use Continue with Google.",
                            fontSize   = 12.sp,
                            color      = TextSecondary,
                            textAlign  = TextAlign.Center,
                            lineHeight = 17.sp,
                        )
                        Button(
                            onClick  = onBack,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                            shape    = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Filled.Login, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Go to Log In", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else if (errorMsg != null) {
                // Generic error
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color    = StatusRejected.copy(alpha = 0.08f),
                    shape    = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = StatusRejected, modifier = Modifier.size(16.dp))
                        Text(errorMsg, color = StatusRejected, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    localError = when {
                        fullName.isBlank()            -> "Full name is required"
                        email.isBlank()               -> "Email is required"
                        password.length < 6           -> "Password must be at least 6 characters"
                        password != confirmPassword   -> "Passwords do not match"
                        else                          -> null
                    }
                    if (localError == null) {
                        viewModel.register(email, password, fullName, phone.ifBlank { null })
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                shape    = RoundedCornerShape(12.dp),
                enabled  = state !is AuthUiState.Loading,
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("Already have an account?", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text       = "Log In",
                    color      = NestXBlue,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable(onClick = onBack),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
