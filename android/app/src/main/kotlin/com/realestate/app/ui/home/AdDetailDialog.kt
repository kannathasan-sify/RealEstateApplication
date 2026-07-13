package com.realestate.app.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.VideoView
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.realestate.app.data.models.AdBanner
import com.realestate.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// AdDetailDialog
// Full-screen dialog shown when the user taps an advertisement banner.
//
// Features:
//  • Close (✕) button — top-right, always visible
//  • Tab row: "Image" | "Video" (Video tab hidden when ad.videoUrl is null)
//  • Image tab  — full-width image with shimmer loading + error fallback
//  • Video tab  — VideoView via AndroidView; auto-plays, pause/resume on tap
//  • Advertiser header — logo avatar (initial fallback) + name
//  • Scroll body: title, subtitle, description
//  • CTA button — only shown when ad.hasValidCta; opens URL/deep-link
//  • All validations listed below
// ─────────────────────────────────────────────────────────────────────────────

// ── Validation constants ──────────────────────────────────────────────────────
private const val MIN_TITLE_LEN  = 2
private const val MAX_TITLE_LEN  = 80
private const val MAX_SUBTITLE   = 120
private const val MAX_DESC_LEN   = 600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdDetailDialog(
    ad:              AdBanner,
    onDismiss:       () -> Unit,
    /** Called with the raw ctaUrl when user taps the CTA button. */
    onCtaClick:      (ctaUrl: String) -> Unit = {},
    /** Called when the user confirms "I'm Interested". */
    onInterestClick: (adId: String, note: String?) -> Unit = { _, _ -> },
    /** Whether this ad is already marked interested (restores state on reopen). */
    initialInterested: Boolean = false,
    /** Retargeting reason label — shown as "Recommended for you" chip when non-null. */
    retargetingLabel:  String? = null,
    /** Analytics: fired when user switches to Video tab. */
    onVideoPlayed:    () -> Unit = {},
    /** Analytics: fired when user taps Share. */
    onShared:         () -> Unit = {},
    /** Analytics: fired when dialog is dismissed. Provides dwell time in seconds. */
    onDismissWithDwell: (dwellSeconds: Int) -> Unit = {},
) {
    // ── Validate ad data on entry ─────────────────────────────────────────────
    val validationError: String? = remember(ad) {
        when {
            ad.imageUrl.isBlank()              -> "Ad image URL is missing."
            ad.title.isBlank()                 -> "Ad title is missing."
            ad.title.length < MIN_TITLE_LEN    -> "Ad title too short."
            ad.title.length > MAX_TITLE_LEN    -> "Ad title too long (max $MAX_TITLE_LEN chars)."
            ad.subtitle.length > MAX_SUBTITLE  -> "Ad subtitle too long (max $MAX_SUBTITLE chars)."
            !ad.description.isNullOrBlank()
                && ad.description.length > MAX_DESC_LEN
                                               -> "Ad description too long (max $MAX_DESC_LEN chars)."
            ad.hasVideo && ad.videoUrl!!.run {
                !startsWith("http://") && !startsWith("https://")
            }                                  -> "Invalid video URL format."
            ad.ctaText != null && ad.ctaUrl.isNullOrBlank()
                                               -> "CTA URL is missing for button \"${ad.ctaText}\"."
            else                               -> null
        }
    }

    // ── Tab state — only show tabs when video is present ─────────────────────
    val tabLabels = buildList {
        add("Image")
        if (ad.hasVideo) add("Video")
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Video playback state ─────────────────────────────────────────────────
    var videoPlaying by remember { mutableStateOf(true) }
    var videoError   by remember { mutableStateOf(false) }

    // ── Engagement timer — measures dwell time (seconds open) ────────────────
    val openedAtMs = remember { System.currentTimeMillis() }
    val context    = androidx.compose.ui.platform.LocalContext.current

    // Fire onDismissWithDwell when dialog is closed via any path
    val dismissWithDwell: () -> Unit = {
        val dwellSec = ((System.currentTimeMillis() - openedAtMs) / 1000L).toInt()
        onDismissWithDwell(dwellSec)
        onDismiss()
    }

    // Track when user switches to Video tab
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) onVideoPlayed()
    }

    // ── Share helpers ────────────────────────────────────────────────────────
    val shareContext = context
    val scope        = rememberCoroutineScope()
    /** true while image is being downloaded for the share sheet */
    var isSharing by remember { mutableStateOf(false) }

    // ── Interested button state ───────────────────────────────────────────────
    var isInterested      by remember { mutableStateOf(initialInterested) }
    var showNoteField     by remember { mutableStateOf(false) }
    var noteText          by remember { mutableStateOf("") }
    var showInterestToast by remember { mutableStateOf(false) }

    // Auto-dismiss the confirmation banner after 2.5 s
    LaunchedEffect(showInterestToast) {
        if (showInterestToast) {
            kotlinx.coroutines.delay(2_500L)
            showInterestToast = false
        }
    }

    // ── Accent colour ─────────────────────────────────────────────────────────
    val accentColor = remember(ad.accentHex) {
        try { Color(android.graphics.Color.parseColor(ad.accentHex)) }
        catch (e: Exception) { NestXBlue }
    }

    // ── Full-screen dialog ────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = dismissWithDwell,
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = false,     // prevent accidental dismiss on tap
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.BottomCenter,
        ) {

            // ── Bottom sheet–style card ───────────────────────────────────────
            Surface(
                modifier      = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape         = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color         = Color.White,
                tonalElevation = 4.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Top bar: drag handle + share + close ──────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 4.dp),
                    ) {
                        // Drag handle (centred)
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDDDDD))
                                .align(Alignment.TopCenter),
                        )
                        // Share + Close buttons (right-aligned)
                        Row(
                            modifier            = Modifier.align(Alignment.TopEnd),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment   = Alignment.CenterVertically,
                        ) {
                            // ── Share button (image + text) ────────────────────
                            IconButton(
                                onClick  = {
                                    if (isSharing) return@IconButton
                                    onShared()
                                    isSharing = true
                                    scope.launch {
                                        shareAdWithImage(
                                            context    = shareContext,
                                            ad         = ad,
                                            onComplete = { isSharing = false },
                                        )
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                                enabled  = !isSharing,
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFFF0F0F0)) {
                                    if (isSharing) {
                                        // Spinner while image downloads
                                        CircularProgressIndicator(
                                            modifier  = Modifier
                                                .padding(8.dp)
                                                .size(20.dp),
                                            color     = NestXBlue,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share advertisement",
                                            tint     = NestXBlue,
                                            modifier = Modifier.padding(6.dp).size(20.dp),
                                        )
                                    }
                                }
                            }
                            // ── Close button ───────────────────────────────────
                            IconButton(
                                onClick  = dismissWithDwell,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFFF0F0F0)) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close advertisement",
                                        tint     = Color(0xFF333333),
                                        modifier = Modifier.padding(6.dp).size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ── Validation error state ────────────────────────────────
                    if (validationError != null) {
                        Column(
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint     = Color(0xFFF57C00),
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Unable to display this ad",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = TextPrimary,
                                textAlign  = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                validationError,
                                fontSize  = 14.sp,
                                color     = TextSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = dismissWithDwell,
                                colors  = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                            ) {
                                Text("Close")
                            }
                        }
                        return@Surface
                    }

                    // ── Tab row (only when video exists) ──────────────────────
                    if (ad.hasVideo) {
                        TabRow(
                            selectedTabIndex  = selectedTab,
                            containerColor    = Color.White,
                            contentColor      = accentColor,
                            modifier          = Modifier.fillMaxWidth(),
                        ) {
                            tabLabels.forEachIndexed { index, label ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick  = {
                                        selectedTab = index
                                        // Reset video state when switching back to video tab
                                        if (index == 1) {
                                            videoPlaying = true
                                            videoError   = false
                                        }
                                    },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (label == "Image")
                                                    Icons.Default.Image
                                                else
                                                    Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                label,
                                                fontWeight = if (selectedTab == index)
                                                    FontWeight.Bold else FontWeight.Normal,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                    // ── Scrollable content ────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {

                        // ── Media area — extracted to break ColumnScope chain ──
                        // AnimatedVisibility inside Box inside Column resolves to
                        // ColumnScope.AnimatedVisibility (compile error). Moving to a
                        // standalone composable resets the implicit receiver chain so
                        // the top-level AnimatedVisibility is used instead.
                        AdMediaBox(
                            ad           = ad,
                            selectedTab  = selectedTab,
                            videoPlaying = videoPlaying,
                            videoError   = videoError,
                            accentColor  = accentColor,
                            onVideoError = { videoError = true },
                            onTogglePlay = { videoPlaying = !videoPlaying },
                        )

                        Spacer(Modifier.height(16.dp))

                        // ── Retargeting / personalisation chip ───────────────
                        if (!retargetingLabel.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .background(
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Recommend,
                                    contentDescription = null,
                                    tint     = NestXBlue,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    retargetingLabel,
                                    fontSize   = 11.sp,
                                    color      = NestXBlue,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        // ── Advertiser header ─────────────────────────────────
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Logo or initial avatar
                            Box(
                                modifier         = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!ad.advertiserLogo.isNullOrBlank()) {
                                    AsyncImage(
                                        model              = ad.advertiserLogo,
                                        contentDescription = ad.advertiserName,
                                        contentScale       = ContentScale.Crop,
                                        modifier           = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                    )
                                } else {
                                    val initial = ad.advertiserName
                                        .trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"
                                    Text(
                                        initial,
                                        color      = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 18.sp,
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    ad.advertiserName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 14.sp,
                                    color      = TextPrimary,
                                )
                                // Priority label + Sponsored
                                val advertiserMeta = buildList {
                                    if (ad.isSponsor) add("Sponsored")
                                    if (ad.priority >= 1) add(ad.priorityLabel + " Ad")
                                }.joinToString(" · ").ifBlank { "Advertisement" }
                                Text(
                                    advertiserMeta,
                                    fontSize = 11.sp,
                                    color    = TextSecondary,
                                )
                            }

                            // Social proof — right-aligned in header
                            if (ad.hasSocialProof) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint     = Color(0xFFE53935),
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            ad.socialProofLabel,
                                            fontSize   = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = Color(0xFFE53935),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 16.dp),
                            color     = Color(0xFFEEEEEE),
                        )
                        Spacer(Modifier.height(14.dp))

                        // ── Title ─────────────────────────────────────────────
                        Text(
                            text       = ad.title,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary,
                            modifier   = Modifier.padding(horizontal = 16.dp),
                        )

                        Spacer(Modifier.height(6.dp))

                        // ── Subtitle ──────────────────────────────────────────
                        Text(
                            text     = ad.subtitle,
                            fontSize = 14.sp,
                            color    = accentColor,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        // ── Description ───────────────────────────────────────
                        if (!ad.description.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text       = ad.description,
                                fontSize   = 14.sp,
                                color      = TextSecondary,
                                lineHeight = 22.sp,
                                modifier   = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── CTA Button ────────────────────────────────────────
                        if (ad.hasValidCta) {
                            Button(
                                onClick  = {
                                    onCtaClick(ad.ctaUrl!!)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(50.dp),
                                shape  = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    ad.ctaText!!,
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // ── I'm Interested Button ─────────────────────────────
                        val interestedGreen = Color(0xFF2E7D32)
                        OutlinedButton(
                            onClick = {
                                if (isInterested) {
                                    // Toggle off — un-register interest
                                    isInterested  = false
                                    showNoteField = false
                                    noteText      = ""
                                } else {
                                    // First tap: show note field then confirm
                                    showNoteField = !showNoteField
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(50.dp),
                            shape  = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isInterested) interestedGreen else NestXBlue,
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isInterested) interestedGreen else NestXBlue,
                            ),
                        ) {
                            Icon(
                                imageVector = if (isInterested)
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text       = if (isInterested) "Interested ✓" else "I'm Interested",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // ── Optional note field (slide in on button tap) ──────
                        if (showNoteField && !isInterested) {
                            Spacer(Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = Color(0xFFF0F4FF),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .padding(12.dp),
                            ) {
                                Text(
                                    "Add a note (optional)",
                                    fontSize   = 12.sp,
                                    color      = TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value         = noteText,
                                    onValueChange = { if (it.length <= 200) noteText = it },
                                    placeholder   = {
                                        Text(
                                            "e.g. Interested in 2BHK only, call after 6 PM...",
                                            fontSize = 13.sp,
                                            color    = TextSecondary,
                                        )
                                    },
                                    modifier      = Modifier.fillMaxWidth(),
                                    shape         = RoundedCornerShape(10.dp),
                                    maxLines      = 3,
                                    colors        = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor   = NestXBlue,
                                        unfocusedBorderColor = BorderColor,
                                    ),
                                )
                                Text(
                                    "${noteText.length}/200",
                                    fontSize = 11.sp,
                                    color    = TextSecondary,
                                    modifier = Modifier.align(Alignment.End),
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        isInterested      = true
                                        showNoteField     = false
                                        showInterestToast = true
                                        onInterestClick(ad.id, noteText.ifBlank { null })
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape  = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NestXBlue,
                                    ),
                                ) {
                                    Text(
                                        "Confirm Interest",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Color.White,
                                    )
                                }
                            }
                        }

                        // ── Interest confirmed banner ──────────────────────────
                        if (showInterestToast) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint     = interestedGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Interest recorded!",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = interestedGreen,
                                    )
                                    Text(
                                        "We'll contact you soon about ${ad.advertiserName}.",
                                        fontSize = 12.sp,
                                        color    = Color(0xFF388E3C),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ── Secondary close button ────────────────────────────
                        TextButton(
                            onClick  = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                "Close",
                                color    = TextSecondary,
                                fontSize = 14.sp,
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Downloads [ad.imageUrl] via Coil, writes it to the app cache, and fires the
 * system share sheet pre-populated with both the image and a text summary.
 *
 * Flow:
 *  1. Use [ImageLoader] to fetch the bitmap (respects Coil's disk cache — usually instant).
 *  2. Write JPEG to `cacheDir/shared_ads/ad_<id>.jpg`.
 *  3. Create a [FileProvider] URI (`${packageName}.provider`) — already declared in AndroidManifest.
 *  4. Build an `ACTION_SEND` intent with type `image/jpeg` and both EXTRA_STREAM + EXTRA_TEXT.
 *  5. If anything fails (network, OOM, no handler) fall back to text-only share.
 *
 * @param onComplete Called on both success and failure paths so the caller can reset [isSharing].
 */
private suspend fun shareAdWithImage(
    context:    android.content.Context,
    ad:         AdBanner,
    onComplete: () -> Unit,
) {
    try {
        // ── 1. Load bitmap via Coil ──────────────────────────────────────────
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(ad.imageUrl)
            .allowHardware(false)   // hardware bitmaps can't be written to streams
            .build()
        val result = imageLoader.execute(request)
        val bitmap: Bitmap? = (result as? SuccessResult)
            ?.let { (it.drawable as? BitmapDrawable)?.bitmap }

        if (bitmap == null) {
            // Image failed — share text only
            shareTextOnly(context, ad)
            return
        }

        // ── 2. Write to cache dir ────────────────────────────────────────────
        val cacheDir = java.io.File(context.cacheDir, "shared_ads").also { it.mkdirs() }
        val imageFile = java.io.File(cacheDir, "ad_${ad.id}.jpg")
        imageFile.outputStream().buffered().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        // ── 3. FileProvider URI ──────────────────────────────────────────────
        val imageUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile,
        )

        // ── 4. Build share intent with image + text ───────────────────────────
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT,   buildShareText(ad))
            putExtra(Intent.EXTRA_SUBJECT, ad.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share ${ad.title} via")
        )

    } catch (e: Exception) {
        // ── 5. Fallback: text-only ────────────────────────────────────────────
        shareTextOnly(context, ad)
    } finally {
        onComplete()
    }
}

/**
 * Text-only share fallback — used when image loading fails or no image URL is set.
 */
private fun shareTextOnly(context: android.content.Context, ad: AdBanner) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT,    buildShareText(ad))
            putExtra(Intent.EXTRA_SUBJECT, ad.title)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    } catch (e: Exception) { /* no handler at all — silent */ }
}

/**
 * Builds the share text body for the Android share sheet.
 * Includes title, subtitle, advertiser, and CTA URL (or a fallback NestX deep link).
 */
private fun buildShareText(ad: AdBanner): String = buildString {
    appendLine("🏠 ${ad.title}")
    appendLine(ad.subtitle)
    if (!ad.description.isNullOrBlank()) {
        appendLine()
        appendLine(ad.description.take(200))
    }
    appendLine()
    appendLine("From: ${ad.advertiserName}")
    if (ad.hasValidCta) {
        val url = ad.ctaUrl!!
        // Convert nestx:// deep links to user-friendly text
        if (url.startsWith("nestx://")) {
            appendLine("Browse on NestX App")
        } else {
            appendLine(url)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AdMediaBox — standalone composable that owns the 220dp media area.
//
// WHY standalone: AnimatedVisibility inside Box inside Column resolves to
// ColumnScope.AnimatedVisibility (compiler error "cannot be called in this
// context with an implicit receiver"). Extracting to a top-level composable
// resets the implicit receiver chain so the standard top-level
// AnimatedVisibility (androidx.compose.animation) is used instead.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AdMediaBox(
    ad:          AdBanner,
    selectedTab: Int,
    videoPlaying: Boolean,
    videoError:  Boolean,
    accentColor: Color,
    onVideoError: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        // ── IMAGE TAB ─────────────────────────────────────────────────────────
        // Top-level AnimatedVisibility — no ColumnScope conflict here
        AnimatedVisibility(
            visible = selectedTab == 0,
            enter   = fadeIn(tween(300)),
            exit    = fadeOut(tween(300)),
        ) {
            AdImageView(
                imageUrl = ad.imageUrl,
                title    = ad.title,
            )
        }

        // ── VIDEO TAB ─────────────────────────────────────────────────────────
        if (ad.hasVideo && selectedTab == 1) {
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn(tween(300)),
            ) {
                AdVideoPlayer(
                    videoUrl     = ad.videoUrl!!,
                    isPlaying    = videoPlaying,
                    onError      = onVideoError,
                    onTogglePlay = onTogglePlay,
                )
            }

            // Video error overlay
            if (videoError) {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint     = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Video unavailable",
                            color    = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // Pause / play overlay icon
            if (!videoError) {
                AnimatedVisibility(
                    visible = !videoPlaying,
                    enter   = fadeIn(),
                    exit    = fadeOut(),
                ) {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Resume video",
                            tint     = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
        }

        // ── "SPONSORED" badge — always visible ───────────────────────────────
        Surface(
            shape    = RoundedCornerShape(4.dp),
            color    = accentColor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        ) {
            Text(
                "SPONSORED",
                fontSize   = 9.sp,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AdImageView — image with loading shimmer and error state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AdImageView(imageUrl: String, title: String) {
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Loading(null)) }

    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Actual image
        AsyncImage(
            model              = imageUrl,
            contentDescription = title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
            onState            = { imageState = it },
        )

        // Loading shimmer
        if (imageState is AsyncImagePainter.State.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE0E0E0),
                                Color(0xFFF5F5F5),
                                Color(0xFFE0E0E0),
                            )
                        )
                    )
            )
            CircularProgressIndicator(
                color    = NestXBlue,
                modifier = Modifier.size(32.dp),
            )
        }

        // Error state
        if (imageState is AsyncImagePainter.State.Error) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint     = Color(0xFFBDBDBD),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Image not available",
                        color    = Color(0xFFBDBDBD),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Dark gradient overlay at the bottom for text readability
        if (imageState is AsyncImagePainter.State.Success) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AdVideoPlayer — VideoView wrapped in AndroidView
//
// FIX NOTES (v2):
//  • isPrepared flag: update() only calls start/pause after onPrepared fires.
//    Previously update() called start() on an unprepared view, which conflicted
//    with the factory's own start() and triggered the error listener.
//  • rememberUpdatedState: keeps onError / isPlaying refs fresh inside factory
//    callbacks so stale closure values can't cause wrong behaviour.
//  • setOnInfoListener: tracks BUFFERING_START / BUFFERING_END so the spinner
//    shows/hides correctly during mid-stream buffering.
//  • No start() in factory: only setVideoURI + listeners. The onPrepared
//    callback starts playback — avoids double-prepare race condition.
//  • requestFocus(): VideoView needs window focus to render on many devices.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AdVideoPlayer(
    videoUrl:     String,
    isPlaying:    Boolean,
    onError:      () -> Unit,
    onTogglePlay: () -> Unit,
) {
    // Buffering/prepared are Compose state so recompose drives the spinner
    var isBuffering by remember { mutableStateOf(true) }
    var isPrepared  by remember { mutableStateOf(false) }

    // rememberUpdatedState keeps the latest lambda values available inside
    // factory callbacks without re-running factory (which would reset the view).
    val latestOnError   = rememberUpdatedState(onError)
    val latestIsPlaying = rememberUpdatedState(isPlaying)

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onTogglePlay() },      // tap anywhere to pause/resume
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    // ── Listeners — set BEFORE setVideoURI ───────────────────
                    setOnPreparedListener { mp ->
                        isPrepared  = true
                        isBuffering = false
                        mp.isLooping = true
                        // Only start if the user hasn't paused between
                        // tab-switch and preparation completing
                        if (latestIsPlaying.value) mp.start()
                    }
                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("AdVideoPlayer",
                            "MediaPlayer error: what=$what extra=$extra")
                        latestOnError.value()
                        true    // true = error handled, don't call onCompletion
                    }
                    setOnInfoListener { _, what, _ ->
                        // Track mid-stream buffering so spinner appears/clears
                        when (what) {
                            android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START ->
                                isBuffering = true
                            android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END ->
                                isBuffering = false
                        }
                        false
                    }

                    // ── Set URI — triggers async prepare internally ───────────
                    // Do NOT call start() here. Let onPreparedListener do it.
                    try {
                        setVideoURI(Uri.parse(videoUrl))
                    } catch (e: Exception) {
                        latestOnError.value()
                    }

                    // VideoView needs focus to render frames on many devices
                    requestFocus()
                }
            },
            update = { videoView ->
                // Only touch playback state after the video is prepared.
                // Calling start() on an unprepared VideoView can silently
                // trigger the error listener on some OEM ROMs.
                if (isPrepared) {
                    when {
                        isPlaying && !videoView.isPlaying -> videoView.start()
                        !isPlaying && videoView.isPlaying -> videoView.pause()
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Spinner: shown while buffering or before first prepare ────────────
        if (isBuffering) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    color    = Color.White,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    "Loading video…",
                    color    = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}
