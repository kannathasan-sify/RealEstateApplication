package com.realestate.app.data.models

/**
 * Role dashboards (Owner / Agent / Channel Partner / Admin).
 *
 * These models describe the analytics surfaces shown on each role's dashboard: KPI tiles,
 * a bar chart, a line/trend chart and one or more data tables. They are plain data holders
 * with no Compose dependency — the UI layer (ui/dashboard) maps them to widgets.
 *
 * Colors are stored as ARGB [Long]s (see [ChartColors]) so models stay Compose-free; the
 * chart composables convert them with `Color(argb)`.
 */

/**
 * Chart data-visualisation palette. Intentionally distinct from the NestX brand palette —
 * charts are, like status chips, the one place multi-hue colour is allowed. Mirrors the
 * `--series-*` / `--ord-*` tokens from the original dashboards design reference.
 */
object ChartColors {
    const val BLUE    = 0xFF1565C0   // series-1 (also brand NestX blue)
    const val GREEN   = 0xFF008300   // series-2
    const val MAGENTA = 0xFFE87BA4   // series-3
    const val YELLOW  = 0xFFEDA100   // series-4
    const val AQUA    = 0xFF1BAF7A   // series-5
    const val ORANGE  = 0xFFEB6834   // series-6
    const val VIOLET  = 0xFF4A3AA7   // series-7
    const val RED     = 0xFFE34948   // series-8

    // Sequential blues (ord-250 → ord-600) for funnel / pipeline stage bars.
    const val ORD_250 = 0xFF86B6EF
    const val ORD_350 = 0xFF5598E7
    const val ORD_450 = 0xFF2A78D6
    const val ORD_550 = 0xFF1C5CAB
    const val ORD_600 = 0xFF184F95

    /** 8-colour categorical series used for the admin revenue-by-stream chart. */
    val series = listOf(BLUE, GREEN, MAGENTA, YELLOW, AQUA, ORANGE, VIOLET, RED)

    /** Sequential blues for funnel/pipeline stage bars (agent + channel-partner charts). */
    val ordinal = listOf(ORD_250, ORD_350, ORD_450, ORD_550, ORD_600)
}

/** Direction/severity of a KPI tile's delta, driving its colour + icon. */
enum class DeltaType { GOOD, NEUTRAL, CRITICAL }

/** A single KPI headline tile (top row of every dashboard). */
data class KpiTile(
    val label: String,
    val value: String,
    val delta: String,
    val deltaType: DeltaType = DeltaType.NEUTRAL,
    val deltaCaption: String = "",
)

/** One bar in a [BarChartData]. */
data class BarDatum(
    val label: String,
    val value: Double,
    val colorArgb: Long = ChartColors.BLUE,
)

data class BarChartData(
    val bars: List<BarDatum>,
    val valueLabel: String = "",
    /** Optional unit suffix appended to each value label, e.g. "L" for ₹ lakhs. */
    val valueSuffix: String = "",
)

/** A single named line in a [LineChartData]. */
data class LineSeries(
    val name: String,
    val colorArgb: Long,
    val values: List<Double>,
)

data class LineChartData(
    val xLabels: List<String>,
    val series: List<LineSeries>,
)

/**
 * A simple tabular section. Rows are pre-formatted display strings (one per column).
 * [statusColumnIndex] marks the column rendered as a coloured status pill instead of text.
 */
data class DashTable(
    val headers: List<String>,
    val rows: List<List<String>>,
    val statusColumnIndex: Int? = null,
)

// ── Per-role aggregate payloads ──────────────────────────────────────────────

data class OwnerDashboardData(
    val tiles: List<KpiTile>,
    val viewsByProperty: BarChartData,
    val viewsTrend: LineChartData,
    val properties: DashTable,
)

data class AgentDashboardData(
    val tiles: List<KpiTile>,
    val leadPipeline: BarChartData,
    val commissionEarned: LineChartData,
    val leadInbox: DashTable,
)

data class PartnerDashboardData(
    val tiles: List<KpiTile>,
    val referralFunnel: BarChartData,
    val payoutTrend: LineChartData,
    val referralPipeline: DashTable,
)

data class AdminDashboardData(
    val tiles: List<KpiTile>,
    val revenueByStream: BarChartData,
    val userGrowth: LineChartData,
    val approvalQueue: DashTable,
    val fraudAlerts: DashTable,
)

/**
 * Shared three-state wrapper for the dashboard screens, mirroring the app-wide
 * Loading / Success / Error convention but generic over the per-role payload type.
 */
sealed class DashboardUiState<out T> {
    object Loading : DashboardUiState<Nothing>()
    data class Success<T>(val data: T) : DashboardUiState<T>()
    data class Error(val message: String) : DashboardUiState<Nothing>()
}
