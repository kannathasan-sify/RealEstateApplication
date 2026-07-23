package com.realestate.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PartnerDashboardScreen(
    viewModel: PartnerDashboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    DashboardScreenScaffold(
        title = "Channel Partner Dashboard",
        subtitle = "Referral funnel, conversions and commission payouts.",
        onBack = onBack,
        state = state,
        onRefresh = { viewModel.load(force = true) },
    ) { data ->
        KpiTileGrid(data.tiles)
        BarChartCard("Referral funnel", data.referralFunnel)
        LineChartCard("Commission payout trend", data.payoutTrend)
        DataTableCard("Referral pipeline", data.referralPipeline)
    }
}
