package com.realestate.app.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.data.models.*
import com.realestate.app.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Shared scaffold — title + a "live" subtitle band + scrolling sections, with the
// app-wide Loading / Success / Error handling done once here.
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
                LiveSubtitle(subtitle)
                content(state.data)
            }
        }
    }
}

/**
 * Renders a dashboard body inline (for use inside an existing scrolling column, e.g. the
 * admin console's agent-wise / builder-wise filtered views). Unlike [DashboardScreenScaffold]
 * it has no scaffold/scroll of its own — Loading is a compact spinner (not the full skeleton,
 * which needs fillMaxSize), Error offers a Retry, Success emits the sections.
 */
@Composable
fun <T> DashboardInlineState(
    state: DashboardUiState<T>,
    onRetry: (() -> Unit)? = null,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    when (state) {
        is DashboardUiState.Loading -> Box(
            Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = NestXBlue) }

        is DashboardUiState.Error -> Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(state.message, color = StatusRejected, fontSize = 14.sp, textAlign = TextAlign.Center)
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                ) { Text("Retry") }
            }
        }

        is DashboardUiState.Success -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content(state.data)
        }
    }
}

/** Subtitle with a gently-pulsing "live" dot — reads as a real-time surface. */
@Composable
private fun LiveSubtitle(subtitle: String) {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(StatusApproved.copy(alpha = pulse)),
        )
        Spacer(Modifier.width(7.dp))
        Text(subtitle, fontSize = 13.sp, color = TextSecondary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KPI tiles — gradient cards, pop-in, delta pills and count-up values
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KpiTileGrid(tiles: List<KpiTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { col, tile ->
                    KpiTileCard(tile, rowIndex * 2 + col, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiTileCard(tile: KpiTile, index: Int, modifier: Modifier = Modifier) {
    val accent = when (tile.deltaType) {
        DeltaType.GOOD -> StatusApproved
        DeltaType.CRITICAL -> StatusRejected
        DeltaType.NEUTRAL -> NestXBlue
    }

    // Staggered pop-in
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val scale by animateFloatAsState(
        targetValue = if (appear) 1f else 0.92f,
        animationSpec = tween(420, delayMillis = index * 90), label = "scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (appear) 1f else 0f,
        animationSpec = tween(420, delayMillis = index * 90), label = "alpha",
    )

    Card(
        modifier = modifier.graphicsLayer {
            scaleX = scale; scaleY = scale; this.alpha = alpha
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(Color.White, accent.copy(alpha = 0.08f))))
                .padding(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    tile.label.uppercase(),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (tile.delta.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    DeltaPill(tile.delta, tile.deltaType, accent)
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedCountText(
                text = tile.value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            if (tile.deltaCaption.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(tile.deltaCaption, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DeltaPill(delta: String, type: DeltaType, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.14f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Icon(
                imageVector = when (type) {
                    DeltaType.GOOD -> Icons.Filled.ArrowUpward
                    DeltaType.CRITICAL -> Icons.Filled.Warning
                    DeltaType.NEUTRAL -> Icons.Filled.Remove
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(delta, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

/**
 * Text that counts up from 0 to its numeric value on first appearance (and on value change).
 * Splits a formatted value like "₹1.85L" / "3,412" / "22%" into prefix + number + suffix, so the
 * currency symbol / unit stays put while only the number animates.
 */
@Composable
private fun AnimatedCountText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    color: Color,
) {
    val match = remember(text) {
        Regex("^([^0-9]*)([0-9][0-9,]*(?:\\.[0-9]+)?)(.*)$").find(text)
    }
    if (match == null) {
        Text(text, fontSize = fontSize, fontWeight = fontWeight, color = color)
        return
    }
    val (prefix, numStr, suffix) = match.destructured
    val decimals = numStr.substringAfter('.', "").length
    val grouping = numStr.contains(',')
    val target = numStr.replace(",", "").toFloatOrNull() ?: 0f

    val anim = remember(text) { Animatable(0f) }
    LaunchedEffect(text) {
        anim.animateTo(target, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
    val shown = formatNumber(anim.value, decimals, grouping)
    Text("$prefix$shown$suffix", fontSize = fontSize, fontWeight = fontWeight, color = color, maxLines = 1)
}

private fun formatNumber(v: Float, decimals: Int, grouping: Boolean): String = when {
    decimals > 0 && grouping -> String.format(Locale.US, "%,.${decimals}f", v)
    decimals > 0 -> String.format(Locale.US, "%.${decimals}f", v)
    grouping -> String.format(Locale.US, "%,d", v.roundToInt())
    else -> v.roundToInt().toString()
}

// ─────────────────────────────────────────────────────────────────────────────
// Section card wrapper (colored accent dot + title)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NestXBlueLight, NestXBlue))),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Horizontal bar chart — gradient bars that grow in, staggered
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BarChartCard(title: String, data: BarChartData) {
    SectionCard(title) {
        val maxV = (data.bars.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(data) { visible = false; visible = true }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.bars.forEachIndexed { i, bar ->
                val target = (bar.value / maxV).toFloat().coerceIn(0.02f, 1f)
                val frac by animateFloatAsState(
                    targetValue = if (visible) target else 0f,
                    animationSpec = tween(700, delayMillis = i * 70, easing = FastOutSlowInEasing),
                    label = "bar",
                )
                val color = Color(bar.colorArgb)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            bar.label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(formatValue(bar.value, data.valueSuffix), fontSize = 12.sp,
                            color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceGray),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(frac)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(color.copy(alpha = 0.75f), color),
                                    ),
                                ),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Line / trend chart — gradient area fill + left-to-right draw-in reveal
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
        val singleSeries = data.series.size == 1

        val progress = remember(data) { Animatable(0f) }
        LaunchedEffect(data) { progress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing)) }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
        ) {
            val w = size.width
            val h = size.height
            val padX = 6f
            val padTop = 12f
            val padBottom = 10f
            val usableW = w - padX * 2
            val usableH = h - padTop - padBottom
            val baseY = h - padBottom

            fun px(i: Int): Float = if (n <= 1) padX else padX + usableW * i / (n - 1)
            fun py(v: Double): Float = padTop + (1f - ((v - minV) / range).toFloat()) * usableH

            // grid lines
            for (g in 0..3) {
                val gy = padTop + usableH * g / 3f
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            }

            clipRect(right = w * progress.value) {
                data.series.forEachIndexed { si, s ->
                    val color = Color(s.colorArgb)
                    val pts = s.values.mapIndexed { i, v -> Offset(px(i), py(v)) }
                    if (pts.isEmpty()) return@forEachIndexed

                    // gradient area fill (single-series only, to avoid muddy overlaps)
                    if (singleSeries) {
                        val area = Path().apply {
                            moveTo(pts.first().x, baseY)
                            pts.forEach { lineTo(it.x, it.y) }
                            lineTo(pts.last().x, baseY)
                            close()
                        }
                        drawPath(
                            area,
                            brush = Brush.verticalGradient(
                                listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)),
                                startY = padTop, endY = baseY,
                            ),
                        )
                    }

                    // line
                    val line = Path().apply {
                        pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                    }
                    drawPath(line, color, style = Stroke(width = 3f, cap = StrokeCap.Round))

                    // dots with white core
                    pts.forEach {
                        drawCircle(color, radius = 3.8f, center = it)
                        drawCircle(Color.White, radius = 1.6f, center = it)
                    }
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
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(s.colorArgb)))
                Spacer(Modifier.width(5.dp))
                Text(s.name, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data table — horizontally scrollable, alternating rows, status pills
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DataTableCard(title: String, table: DashTable) {
    SectionCard(title) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
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
            table.rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .background(if (rowIndex % 2 == 1) SurfaceGray.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEachIndexed { i, cell ->
                        Box(Modifier.width(columnWidth(i))) {
                            if (i == table.statusColumnIndex) {
                                StatusPill(cell)
                            } else {
                                Text(
                                    cell, fontSize = 12.sp, color = TextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun columnWidth(index: Int) = if (index == 0) 140.dp else 110.dp

@Composable
fun StatusPill(status: String) {
    val color = statusColor(status)
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.14f)) {
        Text(
            status, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

private fun statusColor(status: String): Color {
    val s = status.lowercase()
    return when {
        listOf("live", "approved", "converted", "paid", "closed").any { s.contains(it) } -> StatusApproved
        listOf("rejected", "lost", "flagged").any { s.contains(it) } -> StatusRejected
        listOf("pending", "review", "contacted", "site visit", "negotiation", "new").any { s.contains(it) } -> StatusPending
        else -> NestXBlue
    }
}

private fun formatValue(v: Double, suffix: String): String {
    val base = if (v % 1.0 == 0.0) v.roundToInt().toString()
    else ((v * 10).roundToInt() / 10.0).toString()
    return base + suffix
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading shimmer skeleton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha",
    )
    val block = BorderColor.copy(alpha = alpha)
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Box(Modifier.weight(1f).height(88.dp).clip(RoundedCornerShape(16.dp)).background(block))
                }
            }
        }
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(block))
        }
    }
}
