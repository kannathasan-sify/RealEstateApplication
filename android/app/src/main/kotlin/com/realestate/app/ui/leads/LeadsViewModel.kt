package com.realestate.app.ui.leads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.PropertyLead
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Leads / Enquiries screen. Two lists:
 *  - [myLeads]        — enquiries the current user sent as a buyer ("My Enquiries")
 *  - [receivedLeads]  — enquiries received on the current user's own listings ("Received")
 *
 * Mirrors the MyBookings buyer/owner split. Follows the mock-data gate so it is fully
 * usable in debug without a backend.
 */
/** How long fetched enquiries stay fresh before a reload is allowed. */
private const val LEADS_FRESH_MS = 60_000L

@HiltViewModel
class LeadsViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {

    private val _myLeads = MutableStateFlow<List<PropertyLead>>(emptyList())
    val myLeads: StateFlow<List<PropertyLead>> = _myLeads

    private val _receivedLeads = MutableStateFlow<List<PropertyLead>>(emptyList())
    val receivedLeads: StateFlow<List<PropertyLead>> = _receivedLeads

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Enquiries are user-scoped (never disk-cached); this only prevents the same screen
     *  session refetching within the window. */
    private var lastLoadedAt = 0L

    init { load() }

    fun load(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val hasData = _myLeads.value.isNotEmpty() || _receivedLeads.value.isNotEmpty()
        if (!force && hasData && now - lastLoadedAt < LEADS_FRESH_MS) return
        lastLoadedAt = now
        viewModelScope.launch {
            // Skeleton only on a cold load — a refresh keeps the current enquiries visible.
            _isLoading.value = !hasData
            if (BuildConfig.USE_MOCK_DATA) {
                delay(300)
                val uid = MockData.currentUser.id
                _myLeads.value = MockData.propertyLeads
                    .filter { it.buyerId == uid }
                    .sortedByDescending { it.createdAt }
                _receivedLeads.value = MockData.propertyLeads
                    .filter { it.ownerId == uid }
                    .sortedByDescending { it.createdAt }
            } else {
                // Both tabs are shown on this screen, so fetch them concurrently —
                // sequential calls made the user wait for two round-trips.
                val mine = async { repo.getMyLeads() }
                val received = async { repo.getReceivedLeads() }
                _myLeads.value = mine.await().getOrDefault(emptyList())
                _receivedLeads.value = received.await().getOrDefault(emptyList())
            }
            _isLoading.value = false
        }
    }

    /** Owner updates a lead's follow-up status (e.g. contacted / converted / closed). */
    fun updateStatus(leadId: String, status: String) {
        viewModelScope.launch {
            if (BuildConfig.USE_MOCK_DATA) {
                val idx = MockData.propertyLeads.indexOfFirst { it.id == leadId }
                if (idx >= 0) {
                    MockData.propertyLeads[idx] = MockData.propertyLeads[idx].copy(status = status)
                }
            } else {
                repo.updateLeadStatus(leadId, status)
            }
            load()
        }
    }
}
