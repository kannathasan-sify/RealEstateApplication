package com.realestate.app.ui.post_ad

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** Role pairs shown depending on the selected category. */
public data class RolePair(
    val leftKey: String, val leftLabel: String, val leftIcon: ImageVector,
    val rightKey: String, val rightLabel: String, val rightIcon: ImageVector,
    val heading: String,
)

public fun resolveRolePair(selectedCategory: String): RolePair = when {
    selectedCategory.contains("Holiday Stay", ignoreCase = true) ->
        RolePair(

            leftKey    = "landlord",
            leftLabel  = "Owner",
            leftIcon   = Icons.Filled.Home,
            rightKey   = "agent",
            rightLabel = "Agent",
            rightIcon  = Icons.Filled.BusinessCenter,
            heading    = "Are you an owner or an agent?",
        )
    selectedCategory.trim().equals("Ground", ignoreCase = true) ->
        RolePair(
            leftKey    = "landlord",
            leftLabel  = "Owner",
            leftIcon   = Icons.Filled.SportsSoccer,
            rightKey   = "agent",
            rightLabel = "Manager",
            rightIcon  = Icons.Filled.ManageAccounts,
            heading    = "Are you the owner or a manager?",
        )
    selectedCategory.contains("Contractor", ignoreCase = true) ->
        RolePair(
            leftKey   = "individual",
            leftLabel = "Individual",
            leftIcon  = Icons.Filled.Person,
            rightKey  = "company",
            rightLabel = "Company",
            rightIcon  = Icons.Filled.Business,
            heading   = "Are you an individual or a company?",
        )
    else ->
        RolePair(
            leftKey   = "landlord",
            leftLabel = "Landlord",
            leftIcon  = Icons.Filled.Home,
            rightKey  = "agent",
            rightLabel = "Agent",
            rightIcon  = Icons.Filled.BusinessCenter,
            heading   = "Are you a landlord or an agent?",
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAdRoleScreen(
    viewModel: PostAdViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val postedBy         by viewModel.postedBy.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val rolePair = resolveRolePair(selectedCategory)

    // Auto-select left option when screen opens if nothing is selected yet
    LaunchedEffect(rolePair) {
        if (postedBy.isBlank() || (postedBy != rolePair.leftKey && postedBy != rolePair.rightKey)) {
            viewModel.postedBy.value = rolePair.leftKey
        }
    }

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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text       = rolePair.heading,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(32.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RoleCard(
                    label    = rolePair.leftLabel,
                    icon     = rolePair.leftIcon,
                    selected = postedBy == rolePair.leftKey,
                    onClick  = { viewModel.postedBy.value = rolePair.leftKey },
                    modifier = Modifier.weight(1f),
                )
                RoleCard(
                    label    = rolePair.rightLabel,
                    icon     = rolePair.rightIcon,
                    selected = postedBy == rolePair.rightKey,
                    onClick  = { viewModel.postedBy.value = rolePair.rightKey },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick  = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape  = RoundedCornerShape(12.dp),
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoleCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) PrimaryRed else BorderColor

    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) OnboardingBlob else Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint     = PrimaryRed,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text       = label,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = if (selected) PrimaryRed else TextPrimary,
            )
        }
    }
}
