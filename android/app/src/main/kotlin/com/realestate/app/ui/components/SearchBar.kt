package com.realestate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*
import kotlinx.coroutines.delay

// Top-level constant — allocated once, never per-recomposition
private val SearchPlaceholders = listOf(
    "Search for classifieds",
    "Search for property for rent",
    "Search for motors",
)

@Composable
fun HomeSearchBar(
    onClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var placeholderIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            placeholderIndex = (placeholderIndex + 1) % SearchPlaceholders.size
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceGray)
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(20.dp))
            Text(
                text = SearchPlaceholders[placeholderIndex],
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onNotificationClick) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextPrimary)
        }
    }
}
