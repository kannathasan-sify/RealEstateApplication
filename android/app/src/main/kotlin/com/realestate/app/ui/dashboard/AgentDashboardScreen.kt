package com.realestate.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AgentDashboardScreen(
    viewModel: AgentDashboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    DashboardScreenScaffold(
        title = "Agent Dashboard",
        subtitle = "Portfolio, lead pipeline and commission for Priya Sharma.",
        onBack = onBack,
        state = state,
        onRefresh = { viewModel.load(force = true) },
    ) { data ->
        KpiTileGrid(data.tiles)
        BarChartCard("Lead pipeline by stage", data.leadPipeline)
        LineChartCard("Commission earned", data.commissionEarned)
        DataTableCard("Lead inbox", data.leadInbox)
    }
}
