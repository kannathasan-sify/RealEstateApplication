package com.realestate.app.ui.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

@Composable
fun RoleSelectionScreen(
    viewModel: AuthViewModel,
    onRoleSelected: () -> Unit,
) {
    var selectedRole by remember { mutableStateOf<String?>(null) }

    val roles = listOf(
        Triple("🔍", "Buyer / Tenant", "Search and browse properties"),
        Triple("👔", "Agent",          "List properties for clients"),
        Triple("🏗️", "Builder",        "Post new construction projects"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("How will you use NestX?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Choose your role to get started", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        roles.forEach { (emoji, label, desc) ->
            val isSelected = selectedRole == label
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .then(
                        if (isSelected) Modifier.border(2.dp, NestXBlue, RoundedCornerShape(12.dp))
                        else Modifier.border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    )
                    .clickable { selectedRole = label },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) BannerBlue else BackgroundWhite,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) NestXBlue else TextPrimary,
                        )
                        Text(desc, fontSize = 13.sp, color = TextSecondary)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, "Selected", tint = NestXBlue)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val roleKey = when (selectedRole) {
                    "Agent"   -> "agent"
                    "Builder" -> "builder"
                    else      -> "buyer"
                }
                viewModel.setRole(roleKey, onRoleSelected)
            },
            enabled = selectedRole != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NestXBlue),
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}
