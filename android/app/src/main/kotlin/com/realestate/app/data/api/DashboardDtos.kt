package com.realestate.app.data.api

import com.google.gson.annotations.SerializedName
import com.realestate.app.data.models.AdminDashboardData
import com.realestate.app.data.models.AgentDashboardData
import com.realestate.app.data.models.BarChartData
import com.realestate.app.data.models.BarDatum
import com.realestate.app.data.models.ChartColors
import com.realestate.app.data.models.DashTable
import com.realestate.app.data.models.DeltaType
import com.realestate.app.data.models.KpiTile
import com.realestate.app.data.models.LineChartData
import com.realestate.app.data.models.LineSeries
import com.realestate.app.data.models.OwnerDashboardData
import com.realestate.app.data.models.PartnerDashboardData

/**
 * Wire DTOs for the dashboard endpoints (GET dashboard/owner, admin, agent, partner).
 *
 * The backend returns presentation-neutral data (values + deltas + a semantic delta_type);
 * these DTOs deserialize it and `toDomain()` maps it onto the UI models in
 * `data/models/Dashboard.kt`, applying the NestX chart colours on the client. Kept shared so
 * the Admin/Agent/Partner responses (later phases) reuse [KpiDto], [NamedValueDto], [TableDto].
 */

data class KpiDto(
    @SerializedName("label")         val label: String,
    @SerializedName("value")         val value: String,
    @SerializedName("delta")         val delta: String = "",
    @SerializedName("delta_type")    val deltaType: String = "NEUTRAL",
    @SerializedName("delta_caption") val deltaCaption: String = "",
) {
    fun toKpiTile() = KpiTile(
        label = label,
        value = value,
        delta = delta,
        deltaType = when (deltaType.uppercase()) {
            "GOOD" -> DeltaType.GOOD
            "CRITICAL" -> DeltaType.CRITICAL
            else -> DeltaType.NEUTRAL
        },
        deltaCaption = deltaCaption,
    )
}

data class NamedValueDto(
    @SerializedName("label") val label: String,
    @SerializedName("value") val value: Double,
)

data class TableDto(
    @SerializedName("headers")             val headers: List<String>,
    @SerializedName("rows")                val rows: List<List<String>>,
    @SerializedName("status_column_index") val statusColumnIndex: Int? = null,
) {
    fun toDashTable() = DashTable(headers, rows, statusColumnIndex)
}

data class SeriesDto(
    @SerializedName("name")   val name: String,
    @SerializedName("values") val values: List<Double>,
)

data class LineChartDto(
    @SerializedName("x_labels") val xLabels: List<String>,
    @SerializedName("series")   val series: List<SeriesDto>,
) {
    /** [seriesColors] is applied by index; falls back to the categorical palette. */
    fun toLineChartData(seriesColors: List<Long>): LineChartData = LineChartData(
        xLabels = xLabels,
        series = series.mapIndexed { i, s ->
            val color = seriesColors.getOrNull(i) ?: ChartColors.series[i % ChartColors.series.size]
            LineSeries(s.name, color, s.values)
        },
    )
}

// ── Owner ─────────────────────────────────────────────────────────────────────

data class OwnerDashboardResponseDto(
    @SerializedName("tiles")             val tiles: List<KpiDto>,
    @SerializedName("views_by_property") val viewsByProperty: List<NamedValueDto>,
    @SerializedName("views_trend")       val viewsTrend: List<NamedValueDto>,
    @SerializedName("properties")        val properties: TableDto,
) {
    fun toDomain(): OwnerDashboardData = OwnerDashboardData(
        tiles = tiles.map { it.toKpiTile() },
        viewsByProperty = BarChartData(
            bars = viewsByProperty.map { BarDatum(it.label, it.value, ChartColors.BLUE) },
            valueLabel = "Views",
        ),
        viewsTrend = LineChartData(
            xLabels = viewsTrend.map { it.label },
            series = listOf(LineSeries("Views", ChartColors.BLUE, viewsTrend.map { it.value })),
        ),
        properties = properties.toDashTable(),
    )
}

// ── Admin ─────────────────────────────────────────────────────────────────────

data class AdminDashboardResponseDto(
    @SerializedName("tiles")             val tiles: List<KpiDto>,
    @SerializedName("revenue_by_stream") val revenueByStream: List<NamedValueDto>,
    @SerializedName("user_growth")       val userGrowth: LineChartDto,
    @SerializedName("approval_queue")    val approvalQueue: TableDto,
    @SerializedName("fraud_alerts")      val fraudAlerts: TableDto,
) {
    fun toDomain(): AdminDashboardData = AdminDashboardData(
        tiles = tiles.map { it.toKpiTile() },
        revenueByStream = BarChartData(
            bars = revenueByStream.mapIndexed { i, nv ->
                BarDatum(nv.label, nv.value, ChartColors.series[i % ChartColors.series.size])
            },
            valueLabel = "Revenue (₹L)",
            valueSuffix = "L",
        ),
        userGrowth = userGrowth.toLineChartData(listOf(ChartColors.BLUE, ChartColors.GREEN)),
        approvalQueue = approvalQueue.toDashTable(),
        fraudAlerts = fraudAlerts.toDashTable(),
    )
}

// ── Agent ─────────────────────────────────────────────────────────────────────

data class AgentDashboardResponseDto(
    @SerializedName("tiles")             val tiles: List<KpiDto>,
    @SerializedName("lead_pipeline")     val leadPipeline: List<NamedValueDto>,
    @SerializedName("commission_earned") val commissionEarned: List<NamedValueDto>,
    @SerializedName("lead_inbox")        val leadInbox: TableDto,
) {
    fun toDomain(): AgentDashboardData = AgentDashboardData(
        tiles = tiles.map { it.toKpiTile() },
        leadPipeline = BarChartData(
            bars = leadPipeline.mapIndexed { i, nv ->
                BarDatum(nv.label, nv.value, ChartColors.ordinal[i % ChartColors.ordinal.size])
            },
            valueLabel = "Leads",
        ),
        commissionEarned = LineChartData(
            xLabels = commissionEarned.map { it.label },
            series = listOf(LineSeries("Commission (₹L)", ChartColors.BLUE, commissionEarned.map { it.value })),
        ),
        leadInbox = leadInbox.toDashTable(),
    )
}

// ── Channel Partner ─────────────────────────────────────────────────────────────

data class PartnerDashboardResponseDto(
    @SerializedName("tiles")             val tiles: List<KpiDto>,
    @SerializedName("referral_funnel")   val referralFunnel: List<NamedValueDto>,
    @SerializedName("payout_trend")      val payoutTrend: List<NamedValueDto>,
    @SerializedName("referral_pipeline") val referralPipeline: TableDto,
) {
    fun toDomain(): PartnerDashboardData = PartnerDashboardData(
        tiles = tiles.map { it.toKpiTile() },
        referralFunnel = BarChartData(
            bars = referralFunnel.mapIndexed { i, nv ->
                BarDatum(nv.label, nv.value, ChartColors.ordinal[i % ChartColors.ordinal.size])
            },
            valueLabel = "Referrals",
        ),
        payoutTrend = LineChartData(
            xLabels = payoutTrend.map { it.label },
            series = listOf(LineSeries("Commission (₹L)", ChartColors.BLUE, payoutTrend.map { it.value })),
        ),
        referralPipeline = referralPipeline.toDashTable(),
    )
}
