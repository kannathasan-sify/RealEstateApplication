package com.realestate.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.models.ApprovalStatus
import com.realestate.app.data.models.Property
import com.realestate.app.data.models.User
import com.realestate.app.data.models.PropertyLead
import com.realestate.app.data.api.AdminPayment
import com.realestate.app.data.api.SupportTicket
import com.realestate.app.data.api.AdminStats
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val propertyRepo: PropertyRepository,
) : ViewModel() {

    // ── Admin Extensions States ───────────────────────────────────────────────
    val users = MutableStateFlow<List<User>>(emptyList())
    val payments = MutableStateFlow<List<AdminPayment>>(emptyList())
    val tickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val stats = MutableStateFlow<AdminStats?>(null)
    val leads = MutableStateFlow<List<PropertyLead>>(emptyList())

    // ── Loading / error state ─────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ── Master list — drives all four filtered flows ──────────────────────────
    private val _properties = MutableStateFlow<List<Property>>(
        if (BuildConfig.USE_MOCK_DATA) MockData.properties else emptyList()
    )
    val properties: StateFlow<List<Property>> = _properties

    val pending: StateFlow<List<Property>> = _properties
        .map { it.filter { p -> p.approvalStatus == ApprovalStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val approved: StateFlow<List<Property>> = _properties
        .map { it.filter { p -> p.approvalStatus == ApprovalStatus.APPROVED } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rejected: StateFlow<List<Property>> = _properties
        .map { it.filter { p -> p.approvalStatus == ApprovalStatus.REJECTED } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Returns a reactive [StateFlow] for a single property so the review screen
     * re-composes automatically after approve/reject actions.
     */
    fun propertyFlow(id: String): StateFlow<Property?> = _properties
        .map { list -> list.find { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MockData.getById(id))

    // ── Init — load from appropriate data source ──────────────────────────────
    init {
        loadAllAdminData()
    }

    fun loadAllAdminData() {
        loadAllProperties()
        loadUsers()
        loadPayments()
        loadTickets()
        loadStats()
        loadLeads()
    }

    fun loadAllProperties() {
        if (BuildConfig.USE_MOCK_DATA) {
            _properties.value = MockData.properties
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            propertyRepo.getAdminProperties(limit = 200)
                .onSuccess { response ->
                    _properties.value = response.data
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to load listings"
                }
            _isLoading.value = false
        }
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    fun approveProperty(id: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            MockData.approveProperty(id)
            _properties.value = MockData.properties
            return
        }
        viewModelScope.launch {
            propertyRepo.approveProperty(id)
                .onSuccess  { _updatePropertyStatus(id, "approved", null) }
                .onFailure  { e -> _errorMessage.value = e.message ?: "Approve failed" }
        }
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    fun rejectProperty(id: String, reason: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            MockData.rejectProperty(id, reason)
            _properties.value = MockData.properties
            return
        }
        viewModelScope.launch {
            propertyRepo.rejectProperty(id, reason)
                .onSuccess  { _updatePropertyStatus(id, "rejected", reason) }
                .onFailure  { e -> _errorMessage.value = e.message ?: "Reject failed" }
        }
    }

    // ── Re-approve with proof ─────────────────────────────────────────────────

    fun reApproveProperty(id: String, proofNote: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            MockData.reApproveProperty(id, proofNote)
            _properties.value = MockData.properties
            return
        }
        viewModelScope.launch {
            propertyRepo.reApproveProperty(id, proofNote)
                .onSuccess  { _updatePropertyStatus(id, "approved", null) }
                .onFailure  { e -> _errorMessage.value = e.message ?: "Re-approve failed" }
        }
    }

    // ── Local optimistic update after an API action ───────────────────────────
    // Updates the item in _properties immediately so the UI refreshes without
    // needing to re-fetch the entire list from the server.

    private fun _updatePropertyStatus(
        id: String,
        newApprovalStatus: String,
        newRejectionReason: String?,
    ) {
        val current = _properties.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return

        val parsed = when (newApprovalStatus.uppercase()) {
            "APPROVED" -> ApprovalStatus.APPROVED
            "REJECTED" -> ApprovalStatus.REJECTED
            else       -> ApprovalStatus.PENDING
        }
        val newStatus = if (parsed == ApprovalStatus.APPROVED) "active" else "inactive"

        current[idx] = current[idx].copy(
            approvalStatus  = parsed,
            status          = newStatus,
            rejectionReason = newRejectionReason,
        )
        _properties.value = current
    }

    fun clearError() { _errorMessage.value = null }

    // ── Admin Extensions Methods ──────────────────────────────────────────────

    fun loadUsers() {
        if (BuildConfig.USE_MOCK_DATA) {
            users.value = MockData.adminUsers
            return
        }
        viewModelScope.launch {
            propertyRepo.listUsers().onSuccess {
                users.value = it
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to load users"
            }
        }
    }

    fun verifyUser(userId: String, isVerified: Boolean) {
        if (BuildConfig.USE_MOCK_DATA) {
            val idx = MockData.adminUsers.indexOfFirst { it.id == userId }
            if (idx >= 0) {
                MockData.adminUsers[idx] = MockData.adminUsers[idx].copy(isVerified = isVerified)
            }
            loadUsers()
            return
        }
        viewModelScope.launch {
            propertyRepo.verifyUser(userId, isVerified).onSuccess {
                loadUsers()
            }.onFailure {
                _errorMessage.value = it.message ?: "Verify user failed"
            }
        }
    }

    fun changeUserRole(userId: String, role: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            val idx = MockData.adminUsers.indexOfFirst { it.id == userId }
            if (idx >= 0) {
                MockData.adminUsers[idx] = MockData.adminUsers[idx].copy(roleStr = role)
            }
            loadUsers()
            loadStats()
            return
        }
        viewModelScope.launch {
            propertyRepo.changeUserRole(userId, role).onSuccess {
                loadUsers()
            }.onFailure {
                _errorMessage.value = it.message ?: "Change role failed"
            }
        }
    }

    fun deleteUser(userId: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            MockData.adminUsers.removeAll { it.id == userId }
            loadUsers()
            loadStats()
            return
        }
        viewModelScope.launch {
            propertyRepo.deleteUser(userId).onSuccess {
                loadUsers()
            }.onFailure {
                _errorMessage.value = it.message ?: "Delete user failed"
            }
        }
    }

    fun loadPayments() {
        if (BuildConfig.USE_MOCK_DATA) {
            payments.value = MockData.adminPayments
            return
        }
        viewModelScope.launch {
            propertyRepo.listPayments().onSuccess {
                payments.value = it
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to load payments"
            }
        }
    }

    fun loadTickets() {
        if (BuildConfig.USE_MOCK_DATA) {
            tickets.value = MockData.adminTickets
            return
        }
        viewModelScope.launch {
            propertyRepo.listTickets().onSuccess {
                tickets.value = it
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to load tickets"
            }
        }
    }

    fun replyTicket(ticketId: String, reply: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            val idx = MockData.adminTickets.indexOfFirst { it.id == ticketId }
            if (idx >= 0) {
                MockData.adminTickets[idx] = MockData.adminTickets[idx].copy(reply = reply, status = "resolved")
            }
            loadTickets()
            loadStats()
            return
        }
        viewModelScope.launch {
            propertyRepo.replyTicket(ticketId, reply).onSuccess {
                loadTickets()
                loadStats()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to reply complaint"
            }
        }
    }

    fun loadStats() {
        if (BuildConfig.USE_MOCK_DATA) {
            stats.value = MockData.adminStats
            return
        }
        viewModelScope.launch {
            propertyRepo.getSystemStats().onSuccess {
                stats.value = it
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to load stats"
            }
        }
    }

    // ── Enquiries (Property Leads) — Admin Extensions ─────────────────────────

    /** All "I'm Interested" leads across every listing, for the Admin Enquiries tab. */
    fun loadLeads() {
        if (BuildConfig.USE_MOCK_DATA) {
            leads.value = MockData.propertyLeads.sortedByDescending { it.createdAt }
            return
        }
        viewModelScope.launch {
            propertyRepo.getAllLeadsAdmin().onSuccess {
                leads.value = it
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to load enquiries"
            }
        }
    }

    /** Edit a lead's status, message, and/or buyer contact info. */
    fun updateLead(
        leadId: String,
        status: String? = null,
        message: String? = null,
        buyerName: String? = null,
        buyerPhone: String? = null,
        buyerEmail: String? = null,
    ) {
        if (BuildConfig.USE_MOCK_DATA) {
            val idx = MockData.propertyLeads.indexOfFirst { it.id == leadId }
            if (idx >= 0) {
                val current = MockData.propertyLeads[idx]
                MockData.propertyLeads[idx] = current.copy(
                    status     = status ?: current.status,
                    message    = message ?: current.message,
                    buyerName  = buyerName ?: current.buyerName,
                    buyerPhone = buyerPhone ?: current.buyerPhone,
                    buyerEmail = buyerEmail ?: current.buyerEmail,
                )
            }
            loadLeads()
            return
        }
        viewModelScope.launch {
            propertyRepo.updateLeadAdmin(leadId, status, message, buyerName, buyerPhone, buyerEmail)
                .onSuccess { loadLeads() }
                .onFailure { _errorMessage.value = it.message ?: "Failed to update enquiry" }
        }
    }

    /** Quick action: mark a lead as rejected (spam / not a genuine enquiry). */
    fun rejectLead(leadId: String) = updateLead(leadId, status = "rejected")

    fun deleteLead(leadId: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            MockData.propertyLeads.removeAll { it.id == leadId }
            loadLeads()
            return
        }
        viewModelScope.launch {
            propertyRepo.deleteLeadAdmin(leadId)
                .onSuccess { loadLeads() }
                .onFailure { _errorMessage.value = it.message ?: "Failed to delete enquiry" }
        }
    }

    fun deleteSpamProperty(propertyId: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            val current = _properties.value.toMutableList()
            current.removeAll { it.id == propertyId }
            _properties.value = current
            loadStats()
            return
        }
        viewModelScope.launch {
            propertyRepo.deleteProperty(propertyId).onSuccess {
                loadAllProperties()
                loadStats()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to delete property"
            }
        }
    }
}
