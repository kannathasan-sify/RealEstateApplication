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
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBack: () -> Unit,
) {
    val bookingUpdates by viewModel.bookingUpdates.collectAsState()
    val priceAlerts    by viewModel.priceAlerts.collectAsState()
    val newListings    by viewModel.newListings.collectAsState()
    val approvalStatus by viewModel.approvalStatus.collectAsState()
    val promotions     by viewModel.promotions.collectAsState()
    val pushEnabled    by viewModel.pushEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
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

            // ── Master push toggle ────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color  = if (pushEnabled) NestXBlue.copy(alpha = 0.08f) else SurfaceGray,
                shape  = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        null,
                        tint     = if (pushEnabled) NestXBlue else TextSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Push Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            color      = TextPrimary,
                        )
                        Text(
                            if (pushEnabled) "Enabled — you'll receive alerts" else "Disabled — all notifications off",
                            fontSize = 12.sp,
                            color    = TextSecondary,
                        )
                    }
                    Switch(
                        checked         = pushEnabled,
                        onCheckedChange = { viewModel.togglePushEnabled() },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor  = Color.White,
                            checkedTrackColor  = NestXBlue,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Activity notifications ─────────────────────────────────────────
            NotifSectionHeader("Activity")
            NotifGroup {
                NotifToggleRow(
                    icon     = Icons.Default.CalendarToday,
                    title    = "Booking Updates",
                    subtitle = "Confirmations & cancellations for property visits",
                    checked  = bookingUpdates && pushEnabled,
                    enabled  = pushEnabled,
                    onToggle = { viewModel.toggleBookingUpdates() },
                )
                NotifDivider()
                NotifToggleRow(
                    icon     = Icons.Default.CheckCircle,
                    title    = "Approval Status",
                    subtitle = "When your ad is approved or rejected by admin",
                    checked  = approvalStatus && pushEnabled,
                    enabled  = pushEnabled,
                    onToggle = { viewModel.toggleApprovalStatus() },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Discovery notifications ────────────────────────────────────────
            NotifSectionHeader("Discovery")
            NotifGroup {
                NotifToggleRow(
                    icon     = Icons.Default.Home,
                    title    = "New Listings",
                    subtitle = "Properties matching your saved search criteria",
                    checked  = newListings && pushEnabled,
                    enabled  = pushEnabled,
                    onToggle = { viewModel.toggleNewListings() },
                )
                NotifDivider()
                NotifToggleRow(
                    icon     = Icons.Default.TrendingDown,
                    title    = "Price Drop Alerts",
                    subtitle = "When a saved property drops in price",
                    checked  = priceAlerts && pushEnabled,
                    enabled  = pushEnabled,
                    onToggle = { viewModel.togglePriceAlerts() },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Marketing notifications ────────────────────────────────────────
            NotifSectionHeader("Marketing")
            NotifGroup {
                NotifToggleRow(
                    icon     = Icons.Default.Campaign,
                    title    = "Promotions & Offers",
                    subtitle = "Featured listings, special deals, and platform news",
                    checked  = promotions && pushEnabled,
                    enabled  = pushEnabled,
                    onToggle = { viewModel.togglePromotions() },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Info note ─────────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color  = BannerBlue,
                shape  = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Default.Info, null, tint = NestXBlue, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                    Text(
                        "Individual settings only apply when Push Notifications are enabled. " +
                        "You can also manage notification permissions in your device settings.",
                        fontSize   = 12.sp,
                        color      = NestXBlue,
                        lineHeight = 17.sp,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun NotifSectionHeader(title: String) {
    Text(
        title,
        fontSize   = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextSecondary,
        modifier   = Modifier.padding(start = 20.dp, bottom = 6.dp, top = 2.dp),
    )
}

@Composable
private fun NotifGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color  = Color.White,
        shape  = RoundedCornerShape(14.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun NotifToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier         = Modifier
                .size(38.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint     = if (checked) NestXBlue else TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = if (enabled) TextPrimary else TextSecondary,
            )
            Text(
                subtitle,
                fontSize   = 11.sp,
                color      = TextSecondary,
                lineHeight = 15.sp,
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            enabled         = enabled,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = NestXBlue,
                disabledCheckedTrackColor = TextSecondary.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun NotifDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 66.dp),
        color     = BorderColor,
        thickness = 0.5.dp,
    )
}
