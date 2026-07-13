package com.realestate.app.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.realestate.app.BuildConfig
import com.realestate.app.ui.components.RealEstateFilterChip
import com.realestate.app.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var userIdCode by remember { mutableStateOf("") }
    var useUserIdCode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Google Sign-In client — rebuilt only when context changes
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build()
        )
    }

    var googleError by remember { mutableStateOf<String?>(null) }

    // Activity result launcher for Google Sign-In
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleError = null
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                viewModel.googleLogin(token)
            } else {
                googleError = "Google sign-in failed: no ID token returned"
            }
        } catch (e: ApiException) {
            googleError = when (e.statusCode) {
                10   -> "Google sign-in not configured. Check SHA-1 in Cloud Console."
                12501 -> null   // user cancelled — show nothing
                else  -> "Google sign-in error (code ${e.statusCode})"
            }
        }
    }

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            onLoginSuccess()   // navigate first — resetState would change the key and cancel this effect
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))

        // Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(PrimaryRed, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("RE", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Sign in to your account", fontSize = 14.sp, color = TextSecondary)
        Spacer(Modifier.height(32.dp))

        // Toggle between email and user ID login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RealEstateFilterChip(
                label = "Email",
                selected = !useUserIdCode,
                onClick = { useUserIdCode = false },
                modifier = Modifier.weight(1f),
            )
            RealEstateFilterChip(
                label = "User ID Code",
                selected = useUserIdCode,
                onClick = { useUserIdCode = true },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))

        if (useUserIdCode) {
            OutlinedTextField(
                value = userIdCode,
                onValueChange = { userIdCode = it },
                label = { Text("User ID Code (e.g. RE-20261234)") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
        } else {
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
        }

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

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Forgot Password?",
            color = PrimaryRed,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.End).clickable { },
        )

        Spacer(Modifier.height(24.dp))

        val errorMsg = googleError ?: (state as? AuthUiState.Error)?.message
        if (errorMsg != null) {
            Text(
                text = errorMsg,
                color = PrimaryRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }

        Button(
            onClick = {
                if (useUserIdCode) {
                    viewModel.login("", password, userIdCode)
                } else {
                    viewModel.login(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            shape = RoundedCornerShape(12.dp),
            enabled = state !is AuthUiState.Loading,
        ) {
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Divider with OR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Divider(modifier = Modifier.weight(1f), color = BorderColor)
            Text("OR", color = TextSecondary, fontSize = 12.sp)
            Divider(modifier = Modifier.weight(1f), color = BorderColor)
        }

        Spacer(Modifier.height(16.dp))

        // Google Sign-In button
        OutlinedButton(
            onClick = {
                // Sign out first so the account picker always appears
                googleSignInClient.signOut().addOnCompleteListener {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder,
            enabled = state !is AuthUiState.Loading,
        ) {
            Text("Continue with Google", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Don't have an account?", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Register",
                color = PrimaryRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onNavigateRegister),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
