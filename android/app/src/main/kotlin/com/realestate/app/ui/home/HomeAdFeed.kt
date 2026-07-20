package com.realestate.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.HomeAd
import com.realestate.app.data.models.SponsoredStatus
import com.realestate.app.ui.theme.*

/**
 * Redesigned home advertisement feed, driven by the server-side Ad Ranking Engine.
 *
 * Each card is a rich, image-led promo with: a clearly-visible Sponsored/Featured/Promoted label
 * (paid ads only), a "why you're seeing this" ranking-reason chip, the advertiser + title, a hide
 * (✕) affordance, and an explicit CTA button (View Property / Call Owner / Apply Home Loan …).
 * Impressions fire when a card first composes.
 */
@Composable
fun HomeAdFeed(
    ads: List<HomeAd>,
    onImpression: (String) -> Unit,
    onAdClick: (HomeAd) -> Unit,
    onHide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ads.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Sponsored & Recommended", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text("Ranked for you · a fair mix of paid & organic", fontSize = 12.sp, color = TextSecondary)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ads, key = { it.adId }) { ad ->
                HomeAdCard(
                    ad = ad,
                    onImpression = onImpression,
                    onClick = { onAdClick(ad) },
                    onHide = { onHide(ad.adId) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAdCard(
    ad: HomeAd,
    onImpression: (String) -> Unit,
    onClick: () -> Unit,
    onHide: () -> Unit,
) {
    LaunchedEffect(ad.adId) { onImpression(ad.adId) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(300.dp)
            .height(202.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NestXBlueDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = ad.imageUrl ?: "",
                contentDescription = ad.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Legibility scrim
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                        ),
                    ),
            )

            // Top: labels (left) + hide (right)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ad.sponsoredStatus.isPaid) SponsoredLabel(ad.sponsoredStatus)
                    if (ad.rankingReason.isNotBlank()) ReasonChip(ad.rankingReason)
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(28.dp),
                    onClick = onHide,
                ) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "Hide ad",
                        tint = Color.White, modifier = Modifier.padding(6.dp),
                    )
                }
            }

            // Bottom: advertiser, title, subtitle, CTA
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(
                    ad.advertiserName, color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ad.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (!ad.subtitle.isNullOrBlank()) {
                    Text(
                        ad.subtitle, color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text(ad.cta.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

/** Clearly-visible paid label (policy: every paid ad must be labelled). */
@Composable
private fun SponsoredLabel(status: SponsoredStatus) {
    Surface(shape = RoundedCornerShape(6.dp), color = StatusPending) {
        Text(
            status.label.uppercase(),
            color = Color(0xFF3A2E00),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/** "Why you're seeing this" reason chip (e.g. "Price drop", "Near Chennai"). */
@Composable
private fun ReasonChip(reason: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.92f)) {
        Text(
            reason,
            color = NestXBlueDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}
