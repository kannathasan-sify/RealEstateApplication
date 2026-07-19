package com.realestate.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.app.ui.theme.*

/**
 * Platform-wide analytics dashboard for admins, with an **agent-wise** and **builder-wise**
 * filter: switch scope to By Agent / By Builder, pick a person, and their dashboard renders
 * inline (agents get the agent lens; builders get the owner/listings lens — since a builder's
 * data is their listings). Distinct from the operational
 * [com.realestate.app.ui.admin.AdminDashboardScreen] (the 8-tab back-office).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsDashboardScreen(
    viewModel: AdminAnalyticsDashboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val platform by viewModel.uiState.collectAsState()
    val scope by viewModel.scope.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val builders by viewModel.builders.collectAsState()
    val partners by viewModel.partners.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    val builderState by viewModel.builderState.collectAsState()
    val partnerState by viewModel.partnerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (scope == AdminScope.PLATFORM) viewModel.load() else viewModel.retrySelected()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = NestXBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = SurfaceGray,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScopeSelector(scope = scope, onSelect = viewModel::setScope)

            when (scope) {
                AdminScope.PLATFORM -> {
                    Text(
                        "Platform revenue, growth, approvals and fraud monitoring.",
                        fontSize = 13.sp, color = TextSecondary,
                    )
                    DashboardInlineState(platform, onRetry = viewModel::load) { data ->
                        KpiTileGrid(data.tiles)
                        BarChartCard("Revenue by stream", data.revenueByStream)
                        LineChartCard("User growth", data.userGrowth)
                        DataTableCard("Approval queue", data.approvalQueue)
                        DataTableCard("Fraud & complaint alerts", data.fraudAlerts)
                    }
                }

                AdminScope.AGENT -> {
                    PersonPicker("Select an agent", agents, selected, viewModel::selectPerson)
                    if (selected == null) {
                        EmptyHint("Pick an agent to view their listings, lead pipeline and commission.")
                    } else {
                        DashboardInlineState(agentState, onRetry = viewModel::retrySelected) { data ->
                            KpiTileGrid(data.tiles)
                            BarChartCard("Lead pipeline by stage", data.leadPipeline)
                            LineChartCard("Commission earned", data.commissionEarned)
                            DataTableCard("Lead inbox", data.leadInbox)
                        }
                    }
                }

                AdminScope.BUILDER -> {
                    PersonPicker("Select a builder", builders, selected, viewModel::selectPerson)
                    if (selected == null) {
                        EmptyHint("Pick a builder to view their listings, views and leads.")
                    } else {
                        DashboardInlineState(builderState, onRetry = viewModel::retrySelected) { data ->
                            KpiTileGrid(data.tiles)
                            BarChartCard("Views by property (last 30 days)", data.viewsByProperty)
                            LineChartCard("Total views trend", data.viewsTrend)
                            DataTableCard("Listings", data.properties)
                        }
                    }
                }

                AdminScope.PARTNER -> {
                    PersonPicker("Select a channel partner", partners, selected, viewModel::selectPerson)
                    if (selected == null) {
                        EmptyHint("Pick a channel partner to view their referral funnel and payouts.")
                    } else {
                        DashboardInlineState(partnerState, onRetry = viewModel::retrySelected) { data ->
                            KpiTileGrid(data.tiles)
                            BarChartCard("Referral funnel", data.referralFunnel)
                            LineChartCard("Commission payout trend", data.payoutTrend)
                            DataTableCard("Referral pipeline", data.referralPipeline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeSelector(scope: AdminScope, onSelect: (AdminScope) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScopeChip("Platform", scope == AdminScope.PLATFORM) { onSelect(AdminScope.PLATFORM) }
        ScopeChip("By Agent", scope == AdminScope.AGENT) { onSelect(AdminScope.AGENT) }
        ScopeChip("By Builder", scope == AdminScope.BUILDER) { onSelect(AdminScope.BUILDER) }
        ScopeChip("By Partner", scope == AdminScope.PARTNER) { onSelect(AdminScope.PARTNER) }
    }
}

@Composable
private fun ScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) NestXBlue else Color.White,
        border = if (selected) null else BorderStroke(1.dp, BorderColor),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PersonPicker(
    label: String,
    people: List<AdminPerson>,
    selected: AdminPerson?,
    onPick: (AdminPerson) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            ) {
                Text(
                    selected?.name ?: "Choose…",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    color = if (selected == null) TextSecondary else TextPrimary,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = NestXBlue)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (people.isEmpty()) {
                    DropdownMenuItem(text = { Text("No accounts found") }, onClick = { expanded = false })
                } else {
                    people.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person.name + if (person.verified) "  ✓" else "") },
                            onClick = { expanded = false; onPick(person) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(20.dp))
    }
}
