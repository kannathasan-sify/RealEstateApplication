package com.realestate.app.ui.leads

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.data.models.LeadStatus
import com.realestate.app.data.models.PropertyLead
import com.realestate.app.ui.theme.*

/**
 * "Enquiries" — property lead inbox. Two tabs mirroring MyBookings:
 *  - My Enquiries : leads the user sent as a buyer (read-only status)
 *  - Received     : leads on the user's own listings (owner can update status + call buyer)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    viewModel: LeadsViewModel,
    onBack: () -> Unit,
) {
    val myLeads       by viewModel.myLeads.collectAsState()
    val receivedLeads by viewModel.receivedLeads.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    var selectedTab   by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enquiries", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = NestXBlue)
                    }
                },
            )
        },
        containerColor = SurfaceGray,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NestXBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NestXBlue,
                    )
                },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Enquiries", fontWeight = FontWeight.Medium) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Received", fontWeight = FontWeight.Medium) },
                )
            }

            val currentList = if (selectedTab == 0) myLeads else receivedLeads
            val isOwnerTab  = selectedTab == 1

            when {
                // Skeleton rows instead of a blocking spinner — instant paint, no jump.
                isLoading -> com.realestate.app.ui.components.ListSkeleton(count = 5)

                currentList.isEmpty() -> EmptyLeads(isOwnerTab)

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(currentList, key = { it.id }) { lead ->
                        LeadCard(
                            lead        = lead,
                            isOwnerView = isOwnerTab,
                            onCall      = { phone ->
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            },
                            onSetStatus = { status -> viewModel.updateStatus(lead.id, status) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLeads(isOwnerView: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.MarkEmailUnread,
                contentDescription = null,
                tint = BorderColor,
                modifier = Modifier.size(64.dp),
            )
            Text(
                if (isOwnerView) "No enquiries yet" else "No enquiries sent yet",
                fontSize = 17.sp, color = TextSecondary, fontWeight = FontWeight.Medium,
            )
            Text(
                if (isOwnerView)
                    "When buyers tap \"I'm Interested\" on your listings,\ntheir enquiries appear here."
                else
                    "Tap \"I'm Interested\" on a property\nand your enquiry will appear here.",
                fontSize = 13.sp, color = TextSecondary,
                textAlign = TextAlign.Center, lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun LeadCard(
    lead: PropertyLead,
    isOwnerView: Boolean,
    onCall: (String) -> Unit,
    onSetStatus: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        lead.propertyTitle ?: "Property",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    )
                    if (!lead.propertyRef.isNullOrBlank()) {
                        Text("Ref: ${lead.propertyRef}", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                LeadStatusChip(lead.statusEnum)
            }

            if (!lead.message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("\"${lead.message}\"", fontSize = 13.sp, color = TextPrimary)
            }

            // Owner sees the buyer's contact + can act on it.
            if (isOwnerView) {
                Spacer(Modifier.height(10.dp))
                Divider(color = BorderColor, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        lead.buyerName ?: "Buyer",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (!lead.buyerPhone.isNullOrBlank()) {
                        TextButton(onClick = { onCall(lead.buyerPhone) }) {
                            Icon(Icons.Default.Call, null, tint = NestXBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Call", color = NestXBlue, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                LeadStatusMenu(current = lead.statusEnum, onSetStatus = onSetStatus)
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sent ${lead.createdAt.take(10)}",
                    fontSize = 11.sp, color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun LeadStatusChip(status: LeadStatus) {
    val color = when (status) {
        LeadStatus.PENDING         -> StatusPending
        LeadStatus.CONTACTED       -> NestXBlueAccent
        LeadStatus.VISIT_SCHEDULED -> NestXBlue
        LeadStatus.CONVERTED       -> StatusApproved
        LeadStatus.CLOSED          -> TextSecondary
        LeadStatus.REJECTED        -> StatusRejected
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            status.label,
            fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Owner-only dropdown to advance a lead through the follow-up pipeline. */
@Composable
private fun LeadStatusMenu(current: LeadStatus, onSetStatus: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text("Update status", fontSize = 13.sp, color = NestXBlue)
            Icon(Icons.Default.ArrowDropDown, null, tint = NestXBlue, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LeadStatus.values().forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label) },
                    onClick = {
                        expanded = false
                        if (status != current) onSetStatus(status.name.lowercase())
                    },
                )
            }
        }
    }
}
