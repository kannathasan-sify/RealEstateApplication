package com.realestate.app.ui.service_request

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.Quotation
import com.realestate.app.data.models.QuotationCreateRequest
import com.realestate.app.data.models.ServiceRequest
import com.realestate.app.data.models.ServiceRequestCreateRequest
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ServiceRequestUiState {
    object Idle : ServiceRequestUiState()
    object Loading : ServiceRequestUiState()
    data class Success(val list: List<ServiceRequest>) : ServiceRequestUiState()
    data class Error(val message: String) : ServiceRequestUiState()
}

sealed class ServiceRequestDetailUiState {
    object Loading : ServiceRequestDetailUiState()
    data class Success(val request: ServiceRequest, val quotations: List<Quotation>) : ServiceRequestDetailUiState()
    data class Error(val message: String) : ServiceRequestDetailUiState()
}

@HiltViewModel
class ServiceRequestViewModel @Inject constructor(
    private val repo: PropertyRepository
) : ViewModel() {

    // ── Feed / List State ────────────────────────────────────────────────────
    private val _listState = MutableStateFlow<ServiceRequestUiState>(ServiceRequestUiState.Idle)
    val listState: StateFlow<ServiceRequestUiState> = _listState

    // ── Single Detail State ──────────────────────────────────────────────────
    private val _detailState = MutableStateFlow<ServiceRequestDetailUiState>(ServiceRequestDetailUiState.Loading)
    val detailState: StateFlow<ServiceRequestDetailUiState> = _detailState

    // ── Posting state ────────────────────────────────────────────────────────
    val postState = MutableStateFlow<PostRequestState>(PostRequestState.Idle)

    // ── Load Requests Feed (Contractors browse open requests) ───────────────
    fun loadServiceRequests(
        category: String? = null,
        serviceType: String? = null,
        district: String? = null,
        radiusKm: Int? = null,
        urgency: String? = null,
        budgetMin: Double? = null,
        budgetMax: Double? = null,
        sortBy: String = "newest"
    ) {
        viewModelScope.launch {
            _listState.value = ServiceRequestUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(300)
                var filtered = MockData.serviceRequests.toList()
                if (category != null) {
                    filtered = filtered.filter { it.category.equals(category, ignoreCase = true) }
                }
                if (serviceType != null) {
                    filtered = filtered.filter { it.serviceType.equals(serviceType, ignoreCase = true) }
                }
                if (district != null) {
                    filtered = filtered.filter { it.district.equals(district, ignoreCase = true) }
                }
                if (radiusKm != null) {
                    filtered = filtered.filter { it.radiusKm <= radiusKm }
                }
                if (urgency != null) {
                    filtered = filtered.filter { it.urgency.equals(urgency, ignoreCase = true) }
                }
                // Budget overlap: keep requests whose range overlaps the searched range
                if (budgetMin != null) {
                    filtered = filtered.filter { it.budgetMax == null || it.budgetMax >= budgetMin }
                }
                if (budgetMax != null) {
                    filtered = filtered.filter { it.budgetMin == null || it.budgetMin <= budgetMax }
                }
                val urgencyRank = mapOf("emergency" to 0, "urgent" to 1, "normal" to 2)
                filtered = when (sortBy) {
                    "budget_high" -> filtered.sortedByDescending { it.budgetMax ?: Double.NEGATIVE_INFINITY }
                    "budget_low"  -> filtered.sortedBy { it.budgetMin ?: Double.POSITIVE_INFINITY }
                    "urgent_first" -> filtered.sortedBy { urgencyRank[it.urgency] ?: 2 }
                    else -> filtered.sortedByDescending { it.createdAt }
                }
                _listState.value = ServiceRequestUiState.Success(filtered)
            } else {
                repo.listServiceRequests(
                    category = category,
                    serviceType = serviceType,
                    district = district,
                    radiusKm = radiusKm,
                    urgency = urgency,
                    budgetMin = budgetMin,
                    budgetMax = budgetMax,
                    sortBy = sortBy
                ).fold(
                    onSuccess = { _listState.value = ServiceRequestUiState.Success(it) },
                    onFailure = { _listState.value = ServiceRequestUiState.Error(it.message ?: "Failed to load requests") }
                )
            }
        }
    }

    // ── Load single request detail + quotations ─────────────────────────────
    fun loadRequestDetail(id: String) {
        viewModelScope.launch {
            _detailState.value = ServiceRequestDetailUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(300)
                val req = MockData.serviceRequests.find { it.id == id }
                if (req != null) {
                    val quotes = MockData.quotations.filter { it.requestId == id }
                    _detailState.value = ServiceRequestDetailUiState.Success(req, quotes)
                } else {
                    _detailState.value = ServiceRequestDetailUiState.Error("Request not found")
                }
            } else {
                repo.getServiceRequest(id).fold(
                    onSuccess = { request ->
                        repo.listQuotations(id).fold(
                            onSuccess = { quotes ->
                                _detailState.value = ServiceRequestDetailUiState.Success(request, quotes)
                            },
                            onFailure = {
                                _detailState.value = ServiceRequestDetailUiState.Success(request, emptyList())
                            }
                        )
                    },
                    onFailure = {
                        _detailState.value = ServiceRequestDetailUiState.Error(it.message ?: "Failed to get request detail")
                    }
                )
            }
        }
    }

    // ── Post a new service request ──────────────────────────────────────────
    fun createServiceRequest(
        category: String,
        serviceType: String,
        title: String,
        description: String?,
        district: String,
        budgetMin: Double?,
        budgetMax: Double?,
        radiusKm: Int = 50,
        imageUris: List<Uri> = emptyList(),
        urgency: String = "normal",
        preferredDate: String? = null,
        contactPhone: String? = null
    ) {
        viewModelScope.launch {
            postState.value = PostRequestState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(600)
                val newReq = ServiceRequest(
                    id = "sr-mock-${UUID.randomUUID()}",
                    userId = MockData.currentUser.id,
                    category = category,
                    serviceType = serviceType,
                    title = title,
                    description = description,
                    district = district,
                    radiusKm = radiusKm,
                    budgetMin = budgetMin,
                    budgetMax = budgetMax,
                    images = imageUris.map { it.toString() },
                    status = "open",
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
                    quotationCount = 0,
                    urgency = urgency,
                    preferredDate = preferredDate,
                    contactPhone = contactPhone
                )
                MockData.serviceRequests.add(0, newReq)
                postState.value = PostRequestState.Success
            } else {
                val req = ServiceRequestCreateRequest(
                    category = category,
                    serviceType = serviceType,
                    title = title,
                    description = description,
                    district = district,
                    radiusKm = radiusKm,
                    budgetMin = budgetMin,
                    budgetMax = budgetMax,
                    images = emptyList(), // uploaded in secondary step
                    urgency = urgency,
                    preferredDate = preferredDate,
                    contactPhone = contactPhone
                )
                repo.createServiceRequest(req).fold(
                    onSuccess = { request ->
                        if (imageUris.isNotEmpty()) {
                            repo.uploadServiceRequestImages(request.id, imageUris).fold(
                                onSuccess = { postState.value = PostRequestState.Success },
                                onFailure = { e ->
                                    // Non-fatal: request created but images failed
                                    postState.value = PostRequestState.Success
                                }
                            )
                        } else {
                            postState.value = PostRequestState.Success
                        }
                    },
                    onFailure = { postState.value = PostRequestState.Error(it.message ?: "Failed to post request") }
                )
            }
        }
    }

    // ── Submit Quotation (Contractor) ───────────────────────────────────────
    fun submitQuotation(
        requestId: String,
        amount: Double,
        timeline: String,
        notes: String
    ) {
        viewModelScope.launch {
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                val newQuote = Quotation(
                    id = "q-mock-${UUID.randomUUID()}",
                    requestId = requestId,
                    contractorId = MockData.currentUser.id,
                    amount = amount,
                    timeline = timeline,
                    notes = notes,
                    status = "pending",
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
                )
                MockData.quotations.add(0, newQuote)
                // Increment quotation count on the request
                val idx = MockData.serviceRequests.indexOfFirst { it.id == requestId }
                if (idx >= 0) {
                    val req = MockData.serviceRequests[idx]
                    MockData.serviceRequests[idx] = req.copy(quotationCount = req.quotationCount + 1)
                }
                loadRequestDetail(requestId)
            } else {
                val qReq = QuotationCreateRequest(
                    requestId = requestId,
                    amount = amount,
                    timeline = timeline,
                    notes = notes
                )
                repo.submitQuotation(requestId, qReq).fold(
                    onSuccess = { loadRequestDetail(requestId) },
                    onFailure = { /* Handle quotation error */ }
                )
            }
        }
    }

    // ── Accept / Reject Quotation (Owner) ───────────────────────────────────
    fun updateQuotationStatus(requestId: String, quotationId: String, newStatus: String) {
        viewModelScope.launch {
            if (BuildConfig.USE_MOCK_DATA) {
                delay(300)
                val qIdx = MockData.quotations.indexOfFirst { it.id == quotationId }
                if (qIdx >= 0) {
                    val q = MockData.quotations[qIdx]
                    MockData.quotations[qIdx] = q.copy(status = newStatus)
                }
                if (newStatus == "accepted") {
                    val rIdx = MockData.serviceRequests.indexOfFirst { it.id == requestId }
                    if (rIdx >= 0) {
                        val r = MockData.serviceRequests[rIdx]
                        MockData.serviceRequests[rIdx] = r.copy(status = "in_progress")
                    }
                }
                loadRequestDetail(requestId)
            } else {
                repo.updateQuotationStatus(quotationId, newStatus).fold(
                    onSuccess = { loadRequestDetail(requestId) },
                    onFailure = { /