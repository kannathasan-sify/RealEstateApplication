package com.realestate.app.ui.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.data.models.*
import com.realestate.app.ui.theme.*
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Shared scaffold — every dashboard screen is title + subtitle + scrolling sections,
// with the app-wide Loading / Success / Error handling done once here.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DashboardScreenScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    state: DashboardUiState<T>,
    onRefresh: (() -> Unit)? = null,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = NestXBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = SurfaceGray,
    ) { padding ->
        when (state) {
            is DashboardUiState.Loading -> DashboardSkeleton(Modifier.padding(padding))

            is DashboardUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, color = StatusRejected, fontSize = 14.sp, textAlign = TextAlign.Center)
                if (onRefresh != null) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                    ) { Text("Retry") }
                }
            }

            is DashboardUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(subtitle, fontSize = 13.sp, color = TextSecondary)
                content(state.data)
            }
        }
    }
}

/** Animated shimmer placeholder shown while a dashboard loads (KPI tiles + chart/table cards). */
@Composable
private fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha",
    )
    val block = BorderColor.copy(alpha = alpha)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(block),
                    )
                }
            }
        }
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(block),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KPI tiles — 2-column grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KpiTileGrid(tiles: List<KpiTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile ->
                    KpiTileCard(tile, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiTileCard(tile: KpiTile, modifier: Modifier = Modifier) {
    val deltaColor = when (tile.deltaType) {
        DeltaType.GOOD -> StatusApproved
        DeltaType.CRITICAL -> StatusRejected
        DeltaType.NEUTRAL -> TextSecondary
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = tile.label.uppercase(),
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tile.value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (tile.deltaType) {
                        DeltaType.GOOD -> Icons.Filled.ArrowUpward
                        DeltaType.CRITICAL -> Icons.Filled.Warning
                        DeltaType.NEUTRAL -> Icons.Filled.Remove
                    },
                    contentDescription = null,
                    tint = deltaColor,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(tile.delta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = deltaColor)
                if (tile.deltaCaption.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        tile.deltaCaption,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card wrapper used by the chart / table sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Horizontal bar chart — pure Compose (robust on mobile with long category labels)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BarChartCard(title: String, data: BarChartData) {
    SectionCard(title) {
        val maxV = (data.bars.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.bars.forEach { bar ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            bar.label,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatValue(bar.value, data.valueSuffix),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(SurfaceGray),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((bar.value / maxV).toFloat().coerceIn(0.02f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(bar.colorArgb)),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Line / trend chart — Canvas, supports multiple series with a legend
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LineChartCard(title: String, data: LineChartData) {
    SectionCard(title) {
        if (data.series.size > 1) {
            LegendRow(data.series)
            Spacer(Modifier.height(12.dp))
        }

        val allValues = data.series.flatMap { it.values }
        val maxV = allValues.maxOrNull() ?: 1.0
        val minV = allValues.minOrNull() ?: 0.0
        val range = (maxV - minV).let { if (it == 0.0) 1.0 else it }
        val gridColor = BorderColor
        val n = data.xLabels.size

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val w = size.width
            val h = size.height
            val padX = 6f
            val padY = 10f
            val usableW = w - padX * 2
            val usableH = h - padY * 2

            fun px(i: Int): Float = if (n <= 1) padX else padX + usableW * i / (n - 1)
            fun py(v: Double): Float = padY + (1f - ((v - minV) / range).toFloat()) * usableH

            // horizontal grid lines (baseline, mid, top)
            for (g in 0..2) {
                val gy = padY + usableH * g / 2f
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            }

            data.series.forEach { s ->
                val color = Color(s.colorArgb)
                val path = Path()
                s.values.forEachIndexed { i, v ->
                    val x = px(i)
                    val y = py(v)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))
                s.values.forEachIndexed { i, v ->
                    drawCircle(color, radius = 3.5f, center = Offset(px(i), py(v)))
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            data.xLabels.forEach {
                Text(it, fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun LegendRow(series: List<LineSeries>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(s.colorArgb)),
                )
                Spacer(Modifier.width(5.dp))
                Text(s.name, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data table — horizontally scrollable, with a status-pill column
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DataTableCard(title: String, table: DashTable) {
    SectionCard(title) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            // header
            Row(Modifier.padding(bottom = 8.dp)) {
                table.headers.forEachIndexed { i, header ->
                    Text(
                        header.uppercase(),
                        modifier = Modifier.width(columnWidth(i)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Divider(color = BorderColor, thickness = 1.dp)
            table.rows.forEach { row ->
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEachIndexed { i, cell ->
                        Box(Modifier.width(columnWidth(i))) {
                            if (i == table.statusColumnIndex) {
                                StatusPill(cell)
                            } else {
                                Text(
                                    cell,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    }
                }
                Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

/** First column is the primary label (wider); the rest share a compact fixed width. */
private fun columnWidth(index: Int) = if (index == 0) 140.dp else 110.dp

@Composable
fun StatusPill(status: String) {
    val color = statusColor(status)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            status,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

/** Maps a free-text status/stage/severity string to a semantic colour. */
private fun statusColor(status: String): Color {
    val s = status.lowercase()
    return when {
        listOf("live", "approved", "converted", "paid", "closed").any { s.contains(it) } -> StatusApproved
        listOf("rejected", "lost", "flagged").any { s.contains(it) } -> StatusRejected
        listOf("pending", "review", "contacted", "site visit", "negotiation", "new").any { s.contains(it) } -> StatusPending
        else -> NestXBlue
    }
}

/** Whole numbers render without decimals; fractional values keep one decimal. Suffix appended. */
private fun formatValue(v: Double, suffix: String): String {
    val base = if (v % 1.0 == 0.0) v.roundToInt().toString()
    else ((v * 10).roundToInt() / 10.0).toString()
    return base + suffix
}
