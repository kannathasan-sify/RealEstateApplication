package com.realestate.app.ui.post_ad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAdTitleScreen(
    viewModel: PostAdViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val title by viewModel.title.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Place an Ad", fontWeight = FontWeight.SemiBold) },
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
                .padding(24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Enter a short title to describe your listing",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Make your title informative and attractive.",
                fontSize = 14.sp,
                color = TextSecondary,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                placeholder = {
                    Text(
                        "e.g. Studio apt. available for monthly rental in Garden...",
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryRed,
                    unfocusedBorderColor = BorderColor,
                ),
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (title.isNotBlank()) onNext()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank(),
            ) {
                Text("Let's Go", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
