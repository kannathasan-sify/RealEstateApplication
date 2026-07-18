package com.realestate.app.ui.menu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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

        // ── Header card ──────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(OnboardingBlob),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (userName?.firstOrNull()?.uppercase() ?: "U"),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryRed,
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.BottomEnd)
                                .background(PrimaryRed, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = userName ?: user?.fullName ?: "Guest User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary,
                    )

                    // Role badge
                    user?.role?.let { role ->
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NestXBlueLight.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = role.displayName,
                                fontSize = 11.sp,
                                color = NestXBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(20.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get Verified", color = TextSecondary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
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
            Spacer(Modifier.height(8.dp))
            MenuGroup {
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
            Spacer(Modifier.height(8.dp))
            MenuGroup {
                MenuRowItem(
                    icon = Icons.Filled.Stars,
                    label = "Subscription Plans",
                    onClick = onNavigateSubscriptionPlans
                )
            }
        }

        // ── Admin panel (only visible to ADMIN role) ─────────────────────────
        if (isAdmin) {
            item {
                Spacer(Modifier.height(8.dp))
                MenuGroup {
                    MenuRowItem(
                        icon    = Icons.Filled.AdminPanelSettings,
                        label   = "Admin Dashboard",
                        tint    = AdminBadge,
                        onClick = onNavigateAdminPanel,
                    )
                }
            }
        }

        // ── Settings Group 1: Account ────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            MenuGroup {
                MenuRowItem(icon = Icons.Filled.Person,   label = "Profile",          onClick = onNavigateProfile)
                MenuDivider()
                MenuRowItem(icon = Icons.Filled.Settings, label = "Account Settings", onClick = onNavigateAccountSettings)
            }
        }

        // ── Support Group: WhatsApp chat ─────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            MenuGroup {
                WhatsAppSupportRow(
                    number  = SUPPORT_WHATSAPP_NUMBER,
                    context = context,
                )
            }
        }

        // ── Logout ───────────────────────────────────────────────────────────
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
                        .clickable { viewModel.logout(onLogout) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(22.dp))
                    Text("Log out", color = PrimaryRed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
private fun MenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = TextSecondary,
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
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, color = TextPrimary)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
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
