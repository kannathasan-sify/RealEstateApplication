package com.realestate.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerDashboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    DashboardScreenScaffold(
        title = "Property Owner Dashboard",
        subtitle = "Your 6 active listings across Chennai & Bangalore.",
        onBack = onBack,
        state = state,
        onRefresh = { viewModel.load(force = true) },
    ) { data ->
        KpiTileGrid(data.tiles)
        BarChartCard("Views by property (last 30 days)", data.viewsByProperty)
        LineChartCard("Total views trend", data.viewsTrend)
        DataTableCard("My properties", data.properties)
    }
}
