package com.realestate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.ApprovalStatus
import com.realestate.app.data.models.Property
import com.realestate.app.ui.theme.*

@Composable
fun PropertyCard(
    property: Property,
    compact: Boolean = false,
    showApprovalStatus: Boolean = false,
    onClick: () -> Unit
) {
    val cardWidth = if (compact) 200.dp else Dp.Unspecified
    val imageHeight = if (compact) 120.dp else 180.dp

    Card(
        modifier = Modifier
            .then(if (compact) Modifier.width(cardWidth) else Modifier.fillMaxWidth())
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column {
            // ── Image ────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                AsyncImage(
                    model = property.images.firstOrNull() ?: "",
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
                // Listing type badge
                val badgeBg = when (property.listingType) {
                    "sale"         -> NestXBlueDark
                    "holiday_stay" -> Color(0xFF00897B)  // teal
                    "ground"       -> Color(0xFF2E7D32)  // green for sports
                    "contractor"   -> Color(0xFFE65100)  // deep orange
                    else           -> NestXBlue
                }
                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        property.listingTypeLabel,
                        color = Color.White, fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                // Verified badge
                if (property.isVerified) {
                    Icon(
                        Icons.Default.Verified, "Verified",
                        tint = NestXBlue,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(18.dp)
                            .background(Color.White, RoundedCornerShape(9.dp))
                            .padding(2.dp)
                    )
                }
                // Approval badge (for agent's My Ads view)
                if (showApprovalStatus) {
                    val (badgeText, badgeColor) = when (property.approvalStatus) {
                        ApprovalStatus.PENDING  -> "🟡 Pending" to StatusPending
                        ApprovalStatus.APPROVED -> "🟢 Live" to StatusApproved
                        ApprovalStatus.REJECTED -> "🔴 Rejected" to StatusRejected
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Text(badgeText, fontSize = 10.sp, color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // ── Info ─────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(10.dp)) {
                // Price / Rate label
                val priceLabel = when {
                    property.isContractor  -> "₹${property.priceShort}/sqft onwards"
                    property.isHolidayStay -> "₹${property.priceShort}/night"
                    property.isGround      -> "₹${property.priceShort}/booking"
                    else                   -> property.priceDisplay
                }
                Text(
                    priceLabel,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NestXBlue
                )
                Spacer(Modifier.height(2.dp))
                // Sub-info line
                when {
                    property.isContractor || property.isHolidayStay || property.isGround -> {
                        // Show sub-type label for these categories
                        Text(
                            property.displaySubCategory,
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                    (property.bedrooms ?: 0) > 0 || (property.areaSqft ?: 0.0) > 0.0 -> {
                        Text(
                            property.statsLine,
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                // Title
                Text(
                    property.title.orEmpty(),
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // Location
                val locationText = when {
                    !property.neighborhood.isNullOrBlank() -> "${property.neighborhood}, ${property.district.orEmpty()}"
                    else -> property.district.orEmpty()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = TextSecondary,
                        modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        locationText,
                        fontSize = 11.sp, color = TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ApprovalStatusBadge(status: ApprovalStatus, modifier: Modifier = Modifier) {
    val (text, color) = when (status) {
        ApprovalStatus.PENDING  -> "🟡 Pending Review" to StatusPending
        ApprovalStatus.APPROVED -> "🟢 Live"           to StatusApproved
        ApprovalStatus.REJECTED -> "🔴 Rejected"       to StatusRejected
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}