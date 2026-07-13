package com.realestate.app.ui.post_ad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

data class AdCategory(
    val label: String,
    val icon: ImageVector,
    val key: String,
)

val adCategories = listOf(
    AdCategory("Property for Sale",     Icons.Filled.Home,        "Property for Sale"),
    AdCategory("Property for Rent",     Icons.Filled.VpnKey,      "Property for Rent"),
    AdCategory("Construction Services", Icons.Filled.Construction, "Construction Services"),
    AdCategory("Maintenance Services",  Icons.Filled.Build,        "Maintenance Services"),
)

// Same gradient palette as HomeScreen QuickAccessTile
private val categoryGradients: Map<String, Pair<Color, Color>> = mapOf(
    "Property for Sale"     to (Color(0xFF1565C0) to Color(0xFF1976D2)),
    "Property for Rent"     to (Color(0xFF00796B) to Color(0xFF00897B)),
    "Construction Services" to (Color(0xFFE65100) to Color(0xFFF57C00)),
    "Maintenance Services"  to (Color(0xFF6A1B9A) to Color(0xFF7B1FA2)),
)

@Composable
fun PostAdCategoryScreen(
    viewModel: PostAdViewModel,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit = {},
) {
    val subscription by viewModel.subscriptionDetails.collectAsState()
    var showLimitDialog by remember { mutableStateOf(false) }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Subscription Limit Reached", fontWeight = FontWeight.Bold) },
            text = {
                val current = subscription?.currentListingsCount ?: 0
                val max     = subscription?.maxListings ?: 3
                Text("You have $current active listings. Your plan allows up to $max. Please upgrade to publish more.")
            },
            confirmButton = {
                Button(
                    onClick = { showLimitDialog = false; onUpgradeClick() },
                    colors  = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                ) { Text("Upgrade Plan", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        // ── Back button (original style) ─────────────────────────────────────
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))

        // ── Existing heading text ────────────────────────────────────────────
        Text(
            text       = "What are you listing?",
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text     = "Choose the category that your ad fits into.",
            fontSize = 14.sp,
            color    = TextSecondary,
        )
        Spacer(Modifier.height(28.dp))

        // ── 2-column grid of dashboard-style gradient tiles ──────────────────
        adCategories.chunked(2).forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                rowItems.forEach { cat ->
                    val (gradStart, gradEnd) = categoryGradients[cat.key]
                        ?: (NestXBlue to NestXBlueDark)
                    PostAdCategoryTile(
                        icon          = cat.icon,
                        title         = cat.label,
                        gradientStart = gradStart,
                        gradientEnd   = gradEnd,
                        modifier      = Modifier.weight(1f),
                        onClick       = {
                            val details = subscription
                            if (details != null && details.currentListingsCount >= details.maxListings) {
                                showLimitDialog = true
                            } else {
                                onCategorySelected(cat.key)
                            }
                        },
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Gradient tile matching HomeScreen QuickAccessTile ────────────────────────

@Composable
private fun PostAdCategoryTile(
    icon: ImageVector,
    title: String,
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientStart, gradientEnd),
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Frosted icon circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = title,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp),
                )
            }

            // Label
            Text(
                text       = title,
                color      = Color.White,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                lineHeight = 15.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}
