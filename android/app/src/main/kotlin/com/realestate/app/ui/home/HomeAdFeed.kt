package com.realestate.app.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.realestate.app.data.models.HomeAd
import com.realestate.app.data.models.SponsoredStatus
import com.realestate.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Redesigned home advertisement feed, driven by the server-side Ad Ranking Engine.
 *
 * Each card is a rich, image-led promo with: a clearly-visible Sponsored/Featured/Promoted label
 * (paid ads only), a "why you're seeing this" ranking-reason chip, the advertiser + title, a hide
 * (✕) affordance, and a CTA hint. Tapping a card opens a **preview detail dialog** where the real
 * CTA (View Property / Call Owner / Apply Home Loan …) lives. Impressions fire on first compose.
 *
 * Callbacks:
 *  - [onImpression]   — ad became visible (adId)
 *  - [onClickTracked] — user opened the preview (adId) → engagement signal
 *  - [onCtaAction]    — user tapped the CTA in the preview → perform the target action
 *  - [onHide]/[onReport] — feedback signals (adId)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeAdFeed(
    ads: List<HomeAd>,
    onImpression: (String) -> Unit,
    onClickTracked: (String) -> Unit,
    onCtaAction: (HomeAd) -> Unit,
    onHide: (String) -> Unit,
    onReport: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Sponsored & Recommended",
    subtitle: String = "Ranked for you · a fair mix of paid & organic",
    isLoading: Boolean = false,
) {
    // Nothing to show and nothing coming — collapse entirely.
    if (ads.isEmpty() && !isLoading) return

    // Cold load: render the section frame + a shimmer placeholder immediately so the
    // home screen never shows a blank gap or blocks on a spinner.
    if (ads.isEmpty()) {
        Column(modifier.fillMaxWidth()) {
            AdFeedHeader(title, subtitle)
            AdBannerSkeleton()
        }
        return
    }

    var selectedAd by remember { mutableStateOf<HomeAd?>(null) }
    val pagerState = rememberPagerState(pageCount = { ads.size })

    // Auto-advance: show one ad at a time and cycle to the next every 3.5s.
    LaunchedEffect(ads.size) {
        if (ads.size > 1) {
            while (true) {
                delay(3500)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % ads.size)
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        AdFeedHeader(title, subtitle)
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val ad = ads[page]
            HomeAdCard(
                ad = ad,
                onImpression = onImpression,
                onOpen = { onClickTracked(ad.adId); selectedAd = ad },
                onHide = { onHide(ad.adId) },
            )
        }

        // Page dots
        if (ads.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(ads.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) NestXBlue else BorderColor),
                    )
                }
            }
        }
    }

    selectedAd?.let { ad ->
        HomeAdDetailDialog(
            ad = ad,
            onDismiss = { selectedAd = null },
            onCta = { onCtaAction(ad); selectedAd = null },
            onHide = { onHide(ad.adId); selectedAd = null },
            onReport = { onReport(ad.adId); selectedAd = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAdCard(
    ad: HomeAd,
    onImpression: (String) -> Unit,
    onOpen: () -> Unit,
    onHide: () -> Unit,
) {
    LaunchedEffect(ad.adId) { onImpression(ad.adId) }

    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp),
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
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                        ),
                    ),
            )

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
                    Icon(Icons.Filled.Close, contentDescription = "Hide ad", tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(ad.advertiserName, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ad.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!ad.subtitle.isNullOrBlank()) {
                    Text(ad.subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                // CTA hint — also opens the preview (the real action lives in the dialog).
                Surface(shape = RoundedCornerShape(10.dp), color = NestXBlue, onClick = onOpen) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(ad.cta.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// ── Preview detail dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAdDetailDialog(
    ad: HomeAd,
    onDismiss: () -> Unit,
    onCta: () -> Unit,
    onHide: () -> Unit,
    onReport: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Image header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(NestXBlueDark),
                ) {
                    AsyncImage(
                        model = ad.imageUrl ?: "",
                        contentDescription = ad.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                                ),
                            ),
                    )
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
                            modifier = Modifier.size(30.dp),
                            onClick = onDismiss,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Text(ad.advertiserName, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(ad.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Body
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!ad.subtitle.isNullOrBlank()) {
                        Text(ad.subtitle, fontSize = 14.sp, color = TextPrimary)
                    }

                    // Info chips: priority tier · AI match · category
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InfoChip(ad.priorityLabel)
                        InfoChip("AI match ${ad.aiScore.roundToInt()}%")
                        ad.category?.takeIf { it.isNotBlank() }?.let {
                            InfoChip(it.replace('_', ' ').replaceFirstChar { c -> c.uppercase() })
                        }
                    }

                    // Primary CTA
                    Button(
                        onClick = onCta,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                    ) {
                        Text(ad.cta.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }

                    // Secondary actions
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = onHide, modifier = Modifier.weight(1f)) {
                            Text("Not interested", color = TextSecondary, fontSize = 13.sp)
                        }
                        TextButton(onClick = onReport, modifier = Modifier.weight(1f)) {
                            Text("Report ad", color = StatusRejected, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Small building blocks ─────────────────────────────────────────────────────

@Composable
private fun AdFeedHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

/**
 * Shimmer placeholder shown during the first ad load. Renders at the exact banner size so
 * the layout doesn't shift when the real ads arrive (no content jump).
 */
@Composable
private fun AdBannerSkeleton() {
    val transition = rememberInfiniteTransition(label = "adSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(184.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BorderColor.copy(alpha = alpha)),
    )
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

@Composable
private fun InfoChip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceGray) {
        Text(
            text,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
