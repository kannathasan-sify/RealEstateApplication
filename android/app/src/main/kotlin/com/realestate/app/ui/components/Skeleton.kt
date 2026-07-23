package com.realestate.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.realestate.app.ui.theme.BackgroundWhite
import com.realestate.app.ui.theme.BorderColor

/**
 * Shared loading-skeleton primitives.
 *
 * Skeletons beat spinners here: they paint the screen's real structure immediately, so the
 * page never looks blank and content swaps in without a layout jump.
 */

/** The gentle pulse shared by every skeleton so all screens feel consistent. */
@Composable
fun rememberShimmerAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    return alpha
}

/** A single shimmering placeholder block. Size it via [modifier]. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, corner: Dp = 4.dp) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(BorderColor.copy(alpha = rememberShimmerAlpha())),
    )
}

/**
 * Generic "list row" skeleton: a leading thumbnail plus three text lines — matches the shape
 * of the ad / enquiry rows used across My Ads, Enquiries and similar list screens.
 */
@Composable
fun ListRowSkeleton(
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = 72.dp,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
    ) {
        Row(Modifier.padding(12.dp)) {
            SkeletonBlock(Modifier.size(thumbnailSize), corner = 8.dp)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.45f).height(12.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.3f).height(12.dp))
            }
        }
    }
}

/** A vertical stack of [count] [ListRowSkeleton]s with list-style padding. */
@Composable
fun ListSkeleton(count: Int = 5, thumbnailSize: Dp = 72.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) { ListRowSkeleton(thumbnailSize = thumbnailSize) }
    }
}
