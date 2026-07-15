package com.realestate.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.app.data.models.ApprovalStatus
import com.realestate.app.data.models.Property
import com.realestate.app.data.models.User
import com.realestate.app.data.models.PropertyLead
import com.realestate.app.data.models.LeadStatus
import com.realestate.app.data.api.AdminPayment
import com.realestate.app.data.api.SupportTicket
import com.realestate.app.data.api.AdminStats
import com.realestate.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onPropertyClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Properties", "Users", "Payments", "Complaints", "System Stats", "Enquiries")

    val allProperties by viewModel.properties.collectAsState()
    val pending       by viewModel.pending.collectAsState()
    val approved      by viewModel.approved.collectAsState()
    val rejected      by viewModel.rejected.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val errorMessage  by viewModel.errorMessage.collectAsState()

    val usersList     by viewModel.users.collectAsState()
    val paymentsList  by viewModel.payments.collectAsState()
    val ticketsList   by viewModel.tickets.collectAsState()
    val systemStats   by viewModel.stats.collectAsState()
    val leadsList     by viewModel.leads.collectAsState()

    // Trigger load of everything on launch
    LaunchedEffect(Unit) {
        viewModel.loadAllAdminData()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    // Dialog state for delete spam listing
    var deleteSpamPropertyId by remember { mutableStateOf<String?>(null) }

    // Dialog state for user verify/role/delete
    var deleteUserId by remember { mutableStateOf<String?>(null) }
    var changeRoleUserId by remember { mutableStateOf<String?>(null) }
    var newRoleSelection by remember { mutableStateOf("buyer") }

    // Dialog state for complaint details & reply
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var ticketReplyDraft by remember { mutableStateOf("") }

    // Confirm dialog (approve / reject)
    var confirmAction     by remember { mutableStateOf<String?>(null) }
    var confirmPropertyId by remember { mutableStateOf("") }
    var rejectReasonDraft by remember { mutableStateOf("") }

    // Re-approve with proof dialog
    var reApprovePropertyId by remember { mutableStateOf("") }
    var reApproveProofNote  by remember { mutableStateOf("") }
    var showReApproveDialog by remember { mutableStateOf(false) }

    // Enquiries (property leads) — edit dialog + delete confirm
    var editLead          by remember { mutableStateOf<PropertyLead?>(null) }
    var editStatusDraft   by remember { mutableStateOf("pending") }
    var editMessageDraft  by remember { mutableStateOf("") }
    var editNameDraft     by remember { mutableStateOf("") }
    var editPhoneDraft    by remember { mutableStateOf("") }
    var editEmailDraft    by remember { mutableStateOf("") }
    var deleteLeadId      by remember { mutableStateOf<String?>(null) }

    // Render verification dialogs
    if (confirmAction != null) {
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(if (confirmAction == "approve") "Approve Listing?" else "Reject Listing?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (confirmAction == "approve") "Listing will go live immediately." else "Please explain the reason for rejection.")
                    if (confirmAction == "reject") {
                        OutlinedTextField(
                            value = rejectReasonDraft,
                            onValueChange = { rejectReasonDraft = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (confirmAction == "approve") {
                            viewModel.approveProperty(confirmPropertyId)
                        } else {
                            viewModel.rejectProperty(confirmPropertyId, rejectReasonDraft.ifBlank { "Policy violation" })
                        }
                        confirmAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (confirmAction == "approve") StatusApproved else StatusRejected)
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("Cancel") }
            }
        )
    }

    if (showReApproveDialog) {
        AlertDialog(
            onDismissRequest = { showReApproveDialog = false },
            title = { Text("Re-Approve Listing", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide a manager justification note to re-approve.")
                    OutlinedTextField(
                        value = reApproveProofNote,
                        onValueChange = { reApproveProofNote = it },
                        label = { Text("Manager Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reApproveProperty(reApprovePropertyId, reApproveProofNote)
                        showReApproveDialog = false
                    },
                    enabled = reApproveProofNote.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApproved)
                ) {
                    Text("Approve", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReApproveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete property confirm dialog
    deleteSpamPropertyId?.let { propertyId ->
        AlertDialog(
            onDismissRequest = { deleteSpamPropertyId = null },
            title = { Text("Remove Spam Listing?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this listing from the platform? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSpamProperty(propertyId)
                        deleteSpamPropertyId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("Delete Spam", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSpamPropertyId = null }) { Text("Cancel") }
            }
        )
    }

    // Delete user confirm dialog
    deleteUserId?.let { userId ->
        AlertDialog(
            onDismissRequest = { deleteUserId = null },
            title = { Text("Remove User Account?", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete this user profile? All associated properties and service requests will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(userId)
                        deleteUserId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("Delete User", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteUserId = null }) { Text("Cancel") }
            }
        )
    }

    // Change role dialog
    changeRoleUserId?.let { userId ->
        AlertDialog(
            onDismissRequest = { changeRoleUserId = null },
            title = { Text("Change User Role", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select new role for this user account:")
                    val rolesList = listOf("buyer", "landlord", "agent", "builder", "admin")
                    rolesList.forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newRoleSelection = role }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = newRoleSelection == role, onClick = { newRoleSelection = role })
                            Spacer(Modifier.width(8.dp))
                            Text(role.uppercase(), fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changeUserRole(userId, newRoleSelection)
                        changeRoleUserId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestXBlue)
                ) {
                    Text("Save Role", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { changeRoleUserId = null }) { Text("Cancel") }
            }
        )
    }

    // Ticket reply dialog
    selectedTicket?.let { ticket ->
        AlertDialog(
            onDismissRequest = { selectedTicket = null },
            title = { Text("Complaint Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Subject: ${ticket.subject}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Submitted By: ${ticket.profiles?.fullName ?: "Unknown User"}")
                    Text("Message: ${ticket.description}", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    if (!ticket.reply.isNullOrBlank()) {
                        Text("Current Reply:", fontWeight = FontWeight.SemiBold)
                        Text(ticket.reply, color = StatusApproved, fontSize = 13.sp)
                    } else {
                        OutlinedTextField(
                            value = ticketReplyDraft,
                            onValueChange = { ticketReplyDraft = it },
                            label = { Text("Admin Reply") },
                            placeholder = { Text("Write response to user...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }
                }
            },
            confirmButton = {
                if (ticket.reply.isNullOrBlank()) {
                    Button(
                        onClick = {
                            viewModel.replyTicket(ticket.id, ticketReplyDraft)
                            selectedTicket = null
                            ticketReplyDraft = ""
                        },
                        enabled = ticketReplyDraft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved)
                    ) {
                        Text("Send Reply & Resolve", color = Color.White)
                    }
                } else {
                    TextButton(onClick = { selectedTicket = null }) { Text("Close") }
                }
            },
            dismissButton = {
                if (ticket.reply.isNullOrBlank()) {
                    TextButton(onClick = { selectedTicket = null }) { Text("Cancel") }
                }
            }
        )
    }

    // Edit enquiry dialog
    editLead?.let { lead ->
        val statusOptions = listOf("pending", "contacted", "visit_scheduled", "converted", "closed", "rejected")
        AlertDialog(
            onDismissRequest = { editLead = null },
            title = { Text("Edit Enquiry", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Property: ${lead.propertyTitle ?: "—"}", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = editNameDraft,
                        onValueChange = { editNameDraft = it },
                        label = { Text("Buyer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhoneDraft,
                        onValueChange = { editPhoneDraft = it },
                        label = { Text("Buyer Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmailDraft,
                        onValueChange = { editEmailDraft = it },
                        label = { Text("Buyer Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editMessageDraft,
                        onValueChange = { editMessageDraft = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Text("Status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    statusOptions.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editStatusDraft = opt }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = editStatusDraft == opt, onClick = { editStatusDraft = opt })
                            Spacer(Modifier.width(4.dp))
                            Text(LeadStatus.from(opt).label, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLead(
                            leadId = lead.id,
                            status = editStatusDraft,
                            message = editMessageDraft,
                            buyerName = editNameDraft,
                            buyerPhone = editPhoneDraft,
                            buyerEmail = editEmailDraft,
                        )
                        editLead = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestXBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editLead = null }) { Text("Cancel") }
            }
        )
    }

    // Delete enquiry confirm dialog
    deleteLeadId?.let { leadId ->
        AlertDialog(
            onDismissRequest = { deleteLeadId = null },
            title = { Text("Delete Enquiry?", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete this enquiry? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLead(leadId)
                        deleteLeadId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteLeadId = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, null, tint = AdminBadge)
                        Spacer(Modifier.width(8.dp))
                        Text("Control Center", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllAdminData() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = NestXBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Scrollable Tab row for 5 Admin Panels
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.White,
                contentColor     = NestXBlue,
                edgePadding      = 12.dp,
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NestXBlue)
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> PropertiesTabContent(
                            pending = pending,
                            approved = approved,
                            rejected = rejected,
                            all = allProperties,
                            onPropertyClick = onPropertyClick,
                            onApprove = { id -> confirmPropertyId = id; confirmAction = "approve" },
                            onReject = { id -> confirmPropertyId = id; confirmAction = "reject" },
                            onReApprove = { id -> reApprovePropertyId = id; reApproveProofNote = ""; showReApproveDialog = true },
                            onDeleteSpam = { id -> deleteSpamPropertyId = id }
                        )
                        1 -> UsersTabContent(
                            users = usersList,
                            onVerifyToggle = { id, verified -> viewModel.verifyUser(id, verified) },
                            onChangeRole = { id, currentRole -> changeRoleUserId = id; newRoleSelection = currentRole },
                            onDelete = { id -> deleteUserId = id }
                        )
                        2 -> PaymentsTabContent(payments = paymentsList)
                        3 -> ComplaintsTabContent(
                            tickets = ticketsList,
                            onTicketClick = { selectedTicket = it }
                        )
                        4 -> StatsTabContent(stats = systemStats)
                        5 -> EnquiriesTabContent(
                            leads = leadsList,
                            onEdit = { lead ->
                                editLead = lead
                                editStatusDraft = lead.status
                                editMessageDraft = lead.message ?: ""
                                editNameDraft = lead.buyerName ?: ""
                                editPhoneDraft = lead.buyerPhone ?: ""
                                editEmailDraft = lead.buyerEmail ?: ""
                            },
                            onReject = { id -> viewModel.rejectLead(id) },
                            onDelete = { id -> deleteLeadId = id }
                        )
                    }
                }
            }
        }
    }
}

// ── PROPERTIES PANEL ─────────────────────────────────────────────────────────

@Composable
private fun PropertiesTabContent(
    pending: List<Property>,
    approved: List<Property>,
    rejected: List<Property>,
    all: List<Property>,
    onPropertyClick: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onReApprove: (String) -> Unit,
    onDeleteSpam: (String) -> Unit
) {
    var filterState by remember { mutableIntStateOf(0) }
    val displayList = when (filterState) {
        0 -> pending
        1 -> approved
        2 -> rejected
        else -> all
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = filterState, containerColor = Color.White) {
            Tab(selected = filterState == 0, onClick = { filterState = 0 }, text = { Text("Pending (${pending.size})", fontSize = 11.sp) })
            Tab(selected = filterState == 1, onClick = { filterState = 1 }, text = { Text("Approved", fontSize = 11.sp) })
            Tab(selected = filterState == 2, onClick = { filterState = 2 }, text = { Text("Rejected", fontSize = 11.sp) })
            Tab(selected = filterState == 3, onClick = { filterState = 3 }, text = { Text("All", fontSize = 11.sp) })
        }

        if (displayList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No properties found in this category", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayList) { property ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = property.images.firstOrNull() ?: "",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(SurfaceGray, RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                property?.title?.let { Text(it, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                Text("${property.district} • ${property.priceShort}", fontSize = 12.sp, color = TextSecondary)
                                Text("Owner: ${property.agentName}", fontSize = 11.sp, color = NestXBlue)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onDeleteSpam(property.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = PrimaryRed, modifier = Modifier.size(20.dp))
                                    }
                                }

                                when (property.approvalStatus) {
                                    ApprovalStatus.PENDING -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(onClick = { onApprove(property.id) }) {
                                                Text("Approve", color = StatusApproved, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(onClick = { onReject(property.id) }) {
                                                Text("Reject", color = StatusRejected, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    ApprovalStatus.REJECTED -> {
                                        Button(
                                            onClick = { onReApprove(property.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Re-Approve", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                    else -> {
                                        TextButton(onClick = { onPropertyClick(property.id) }) {
                                            Text("View Details", color = NestXBlue, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── USERS PANEL ──────────────────────────────────────────────────────────────

@Composable
private fun UsersTabContent(
    users: List<User>,
    onVerifyToggle: (String, Boolean) -> Unit,
    onChangeRole: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var roleFilter by remember { mutableStateOf("all") }
    val displayUsers = when (roleFilter) {
        "all" -> users
        "agent" -> users.filter { it.roleStr == "agent" || it.roleStr == "agency" }
        "builder" -> users.filter { it.roleStr == "builder" || it.roleStr == "developer" }
        else -> users.filter { it.roleStr == "buyer" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all" to "All Users", "agent" to "Agents", "builder" to "Contractors").forEach { (key, label) ->
                FilterChip(
                    selected = roleFilter == key,
                    onClick = { roleFilter = key },
                    label = { Text(label) }
                )
            }
        }

        if (displayUsers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found matching filter", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayUsers) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (user.isVerified) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, "Verified", tint = StatusApproved, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text("Email: ${user.email}", fontSize = 12.sp, color = TextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Role: ", fontSize = 11.sp, color = TextSecondary)
                                    Text(user.roleStr.uppercase(), fontSize = 11.sp, color = NestXBlue, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Actions
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Toggle Verification status
                                Button(
                                    onClick = { onVerifyToggle(user.id, !user.isVerified) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (user.isVerified) StatusPending else StatusApproved
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(if (user.isVerified) "Unverify" else "Verify", color = Color.White, fontSize = 10.sp)
                                }

                                IconButton(onClick = { onChangeRole(user.id, user.roleStr) }) {
                                    Icon(Icons.Default.ManageAccounts, "Role", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }

                                IconButton(onClick = { onDelete(user.id) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = PrimaryRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── PAYMENTS PANEL ───────────────────────────────────────────────────────────

@Composable
private fun PaymentsTabContent(payments: List<AdminPayment>) {
    if (payments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions or payments recorded yet", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payments) { pay ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pay.profiles?.fullName ?: "Anonymous User", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(pay.profiles?.email ?: "No email address", fontSize = 12.sp, color = TextSecondary)
                            Text("Plan upgraded: ${pay.tier.uppercase()}", fontSize = 11.sp, color = NestXBlue, fontWeight = FontWeight.Medium)
                            Text("Transaction: ${pay.createdAt.take(10)}", fontSize = 10.sp, color = TextSecondary)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${pay.amount}", fontWeight = FontWeight.Bold, color = StatusApproved, fontSize = 16.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusApproved.copy(alpha = 0.12f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    pay.status.uppercase(),
                                    fontSize = 9.sp,
                                    color = StatusApproved,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── COMPLAINTS PANEL ─────────────────────────────────────────────────────────

@Composable
private fun ComplaintsTabContent(
    tickets: List<SupportTicket>,
    onTicketClick: (SupportTicket) -> Unit
) {
    if (tickets.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No help complaints or support tickets found", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tickets) { ticket ->
                val isResolved = ticket.status == "resolved"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTicketClick(ticket) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ticket.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("From: ${ticket.profiles?.fullName ?: "User"}", fontSize = 12.sp, color = TextSecondary)
                            Text(ticket.description, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = TextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = (if (isResolved) StatusApproved else StatusPending).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = ticket.status.uppercase(),
                                color = if (isResolved) StatusApproved else StatusPending,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── ENQUIRIES (PROPERTY LEADS) PANEL ─────────────────────────────────────────

@Composable
private fun EnquiriesTabContent(
    leads: List<PropertyLead>,
    onEdit: (PropertyLead) -> Unit,
    onReject: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (leads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No property enquiries yet", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(leads, key = { it.id }) { lead ->
                val isRejected = lead.statusEnum == LeadStatus.REJECTED
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    lead.propertyTitle ?: "Property",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                if (!lead.propertyRef.isNullOrBlank()) {
                                    Text("Ref: ${lead.propertyRef}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = (if (isRejected) StatusRejected else NestXBlue).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    lead.statusEnum.label.uppercase(),
                                    color = if (isRejected) StatusRejected else NestXBlue,
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            val roleLabel = lead.buyerRole?.replaceFirstChar { it.uppercase() } ?: "Buyer"
                            Text(
                                "${lead.buyerName?.ifBlank { null } ?: "Unknown"} ($roleLabel)",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary
                            )
                        }
                        if (!lead.buyerPhone.isNullOrBlank()) {
                            Text(lead.buyerPhone, fontSize = 11.sp, color = TextSecondary)
                        }
                        if (!lead.message.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "\"${lead.message}\"", fontSize = 12.sp, color = TextSecondary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Divider(color = BorderColor, thickness = 0.5.dp)
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onEdit(lead) }) {
                                Icon(Icons.Default.Edit, null, tint = NestXBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit", color = NestXBlue, fontSize = 12.sp)
                            }
                            TextButton(onClick = { onReject(lead.id) }, enabled = !isRejected) {
                                Icon(
                                    Icons.Default.Block, null,
                                    tint = if (isRejected) TextSecondary else StatusPending,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (isRejected) "Rejected" else "Reject",
                                    color = if (isRejected) TextSecondary else StatusPending, fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { onDelete(lead.id) }) {
                                Icon(Icons.Default.Delete, null, tint = PrimaryRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", color = PrimaryRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── REPORTS / STATS PANEL ───────────────────────────────────────────────────

@Composable
private fun StatsTabContent(stats: AdminStats?) {
    if (stats == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NestXBlue)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("System Summary Reports", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPanel(title = "Total Listings", value = "${stats.totalProperties}", icon = "🏠", modifier = Modifier.weight(1f))
                StatPanel(title = "Pending Ads", value = "${stats.pendingProperties}", icon = "🟡", modifier = Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPanel(title = "Registered Users", value = "${stats.totalUsers}", icon = "👥", modifier = Modifier.weight(1f))
                StatPanel(title = "Open Complaints", value = "${stats.openComplaints}", icon = "🎟️", modifier = Modifier.weight(1f))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StatusApproved.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StatusApproved.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Simulated Revenue", fontSize = 12.sp, color = TextSecondary)
                        Text("₹${stats.totalRevenueInr}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StatusApproved)
                    }
                    Text("💰", fontSize = 32.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("User breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Agents / Agencies:")
                        Text("${stats.agentsCount}", fontWeight = FontWeight.Bold, color = NestXBlue)
                    }
                    Divider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Builders / Contractors:")
                        Text("${stats.buildersCount}", fontWeight = FontWeight.Bold, color = NestXBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPanel(title: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                Text(icon, fontSize = 16.sp)
            }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}