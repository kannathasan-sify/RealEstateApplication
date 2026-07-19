package com.realestate.app.ui.menu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.BuildConfig
import com.realestate.app.ui.theme.*

/**
 * NestX support WhatsApp number in international format (country code + number, no spaces or +).
 * Used for doubts, clarifications, and general support chat.
 * Format: 91 (India) + 8056584080 = "918056584080"
 */
private const val SUPPORT_WHATSAPP_NUMBER = "918056584080"

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    onNavigateProfile: () -> Unit,
    onNavigateAdminPanel: () -> Unit,
    onNavigateMyAds: () -> Unit = {},
    onNavigateMyLeads: () -> Unit = {},
    onNavigateMySearches: () -> Unit = {},
    onNavigateAccountSettings: () -> Unit = {},
    onNavigatePostServiceRequest: () -> Unit = {},
    onNavigateServiceRequestFeed: () -> Unit = {},
    onNavigateSubscriptionPlans: () -> Unit = {},
    onNavigateOwnerDashboard: () -> Unit = {},
    onNavigateAgentDashboard: () -> Unit = {},
    onNavigatePartnerDashboard: () -> Unit = {},
    onNavigateAdminAnalytics: () -> Unit = {},
    onLogout: () -> Unit,
) {
    val userName by viewModel.userName.collectAsState()
    val user     by viewModel.user.collectAsState()
    val isAdmin  by viewModel.isAdmin.collectAsState()
    val context  = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray),
    ) {

        // ── Header (hero banner) ────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NestXBlueLight.copy(alpha = 0.18f), SurfaceGray),
                        ),
                    ),
            ) {
                // Decorative accent blobs
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-40).dp)
                        .clip(CircleShape)
                        .background(NestXBlue.copy(alpha = 0.08f)),
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-30).dp, y = 30.dp)
                        .clip(CircleShape)
                        .background(NestXBlue.copy(alpha = 0.06f)),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(NestXBlue)
                            .border(4.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (userName?.firstOrNull()?.uppercase() ?: "U"),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, Color.White, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                tint = NestXBlue,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = userName ?: user?.fullName ?: "Guest User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary,
                    )

                    // Role badge
                    user?.role?.let { role ->
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NestXBlue.copy(alpha = 0.12f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = NestXBlue,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = role.displayName,
                                    fontSize = 12.sp,
                                    color = NestXBlue,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, NestXBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NestXBlue),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NestXBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get Verified", color = NestXBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Joined ${user?.createdAt?.take(7) ?: "March 2026"}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // ── Quick actions ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    QuickActionItem(icon = Icons.Filled.ListAlt,       label = "My Ads",     onClick = onNavigateMyAds)
                    QuickActionItem(icon = Icons.Filled.MarkEmailRead, label = "Enquiries",  onClick = onNavigateMyLeads)
                }
            }
        }

        // ── Service Requests (Broadcast / Quotes) ─────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            MenuGroup(title = "Services") {
                MenuRowItem(
                    icon = Icons.Filled.PostAdd,
                    label = "Post Service Request (Broadcast)",
                    onClick = onNavigatePostServiceRequest
                )
                MenuDivider()
                MenuRowItem(
                    icon = Icons.Filled.List,
                    label = "Browse Service Requests",
                    onClick = onNavigateServiceRequestFeed
                )
            }
        }

        // ── Subscription Group ───────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            MenuGroup(title = "Membership") {
                MenuRowItem(
                    icon = Icons.Filled.Stars,
                    label = "Subscription Plans",
                    tint = StatusPending,
                    onClick = onNavigateSubscriptionPlans
                )
            }
        }

        // ── Dashboards (analytics) ───────────────────────────────────────────
        // All three are self-scoped to the current user (they see their own owner/agent/
        // partner data — empty state if they have none), so they're open to everyone. The
        // platform-wide Admin Analytics lives under the admin-only group below.
        item {
            Spacer(Modifier.height(16.dp))
            MenuGroup(title = "Dashboards") {
                MenuRowItem(
                    icon = Icons.Filled.Dashboard,
                    label = "Owner Dashboard",
                    onClick = onNavigateOwnerDashboard,
                )
                MenuDivider()
                MenuRowItem(
                    icon = Icons.Filled.SupportAgent,
                    label = "Agent Dashboard",
                    onClick = onNavigateAgentDashboard,
                )
                MenuDivider()
                MenuRowItem(
                    icon = Icons.Filled.Groups,
                    label = "Channel Partner Dashboard",
                    onClick = onNavigatePartnerDashboard,
                )
            }
        }

        // ── Admin panel (only visible to ADMIN role) ─────────────────────────
        if (isAdmin) {
            item {
                Spacer(Modifier.height(16.dp))
                MenuGroup(title = "Admin") {
                    MenuRowItem(
                        icon       = Icons.Filled.AdminPanelSettings,
                        label      = "Admin Dashboard",
                        tint       = AdminBadge,
                        badge      = "Admin",
                        badgeColor = AdminBadge,
                        onClick    = onNavigateAdminPanel,
                    )
                    MenuDivider()
                    MenuRowItem(
                        icon       = Icons.Filled.Insights,
                        label      = "Admin Analytics",
                        tint       = AdminBadge,
                        badge      = "Admin",
                        badgeColor = AdminBadge,
                        onClick    = onNavigateAdminAnalytics,
                    )
                }
            }
        }

        // ── Settings Group 1: Account ────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            MenuGroup(title = "Account") {
                MenuRowItem(icon = Icons.Filled.Person,   label = "Profile",          onClick = onNavigateProfile)
                MenuDivider()
                MenuRowItem(icon = Icons.Filled.Settings, label = "Account Settings", onClick = onNavigateAccountSettings)
            }
        }

        // ── Support Group: WhatsApp chat ─────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            MenuGroup(title = "Support") {
                WhatsAppSupportRow(
                    number  = SUPPORT_WHATSAPP_NUMBER,
                    context = context,
                )
            }
        }

        // ── Logout ───────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.logout(onLogout) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(StatusRejected.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(20.dp))
                    }
                    Text("Log out", color = StatusRejected, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }

        // ── Footer ───────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Build Version - ${BuildConfig.VERSION_NAME} (40405)",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Private composable helpers ───────────────────────────────────────────────

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(OnboardingBlob, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = PrimaryRed, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MenuGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = NestXBlue,
    labelColor: androidx.compose.ui.graphics.Color = TextPrimary,
    badge: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color = NestXBlue,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeColor.copy(alpha = 0.12f),
            ) {
                Text(
                    badge.uppercase(),
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MenuRowItemWithValue(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, color = TextPrimary)
        Text(value, fontSize = 14.sp, color = TextSecondary)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MenuDivider() {
    Divider(
        modifier = Modifier.padding(start = 56.dp),
        color = BorderColor,
        thickness = 0.5.dp,
    )
}

/**
 * WhatsApp support row — opens a direct chat to [number] in WhatsApp.
 * Shows a pre-filled greeting message; falls back to the browser link if
 * WhatsApp is not installed.
 *
 * @param number International format without + (e.g. "918056584080")
 */
@Composable
private fun WhatsAppSupportRow(number: String, context: android.content.Context) {
    // Pre-filled message so the user doesn't have to type from scratch
    val greetingText = "Hi NestX Support! I need help with:"
    val encodedMsg   = android.net.Uri.encode(greetingText)
    val waUrl        = "https://wa.me/$number?text=$encodedMsg"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                    setPackage("com.whatsapp")
                }
                // WhatsApp Business fallback
                val intentBusiness = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                    setPackage("com.whatsapp.w4b")
                }
                when {
                    intent.resolveActivity(context.packageManager) != null ->
                        context.startActivity(intent)
                    intentBusiness.resolveActivity(context.packageManager) != null ->
                        context.startActivity(intentBusiness)
                    else ->
                        // neither app installed → open browser
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                }
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // WhatsApp green icon box
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(WhatsAppGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Chat,
                contentDescription = null,
                tint     = WhatsAppGreen,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Chat Support",
                fontSize   = 15.sp,
                color      = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Doubts & clarifications — chat on WhatsApp",
                fontSize = 12.sp,
                color    = TextSecondary,
            )
        }
        // WhatsApp green arrow
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint     = WhatsAppGreen,
            modifier = Modifier.size(20.dp),
        )
    }
}
