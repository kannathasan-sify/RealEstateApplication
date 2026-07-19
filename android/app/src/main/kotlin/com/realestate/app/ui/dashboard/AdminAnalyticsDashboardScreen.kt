package com.realestate.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Platform-wide analytics dashboard for admins — revenue, growth, approval queue and fraud
 * alerts. Distinct from the operational [com.realestate.app.ui.admin.AdminDashboardScreen]
 * (the 8-tab back-office): this is the KPI/analytics overview from the dashboards design.
 */
@Composable
fun AdminAnalyticsDashboardScreen(
    viewModel: AdminAnalyticsDashboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    DashboardScreenScaffold(
        title = "Admin Dashboard",
        subtitle = "Platform revenue, growth, approvals and fraud monitoring.",
        onBack = onBack,
        state = state,
        onRefresh = viewModel::load,
    ) { data ->
        KpiTileGrid(data.tiles)
        BarChartCard("Revenue by stream", data.revenueByStream)
        LineChartCard("User growth", data.userGrowth)
        DataTableCard("Approval queue", data.approvalQueue)
        DataTableCard("Fraud & complaint alerts", data.fraudAlerts)
    }
}
