package com.realestate.app.ui.property

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.Property
import com.realestate.app.data.models.PropertyFilterState
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI States ──────────────────────────────────────────────────────────────────

sealed class PropertyUiState {
    object Loading                                       : PropertyUiState()
    data class Success(val properties: List<Property>)  : PropertyUiState()
    data class Error(val message: String)               : PropertyUiState()
}

sealed class PropertyDetailUiState {
    object Loading                                                           : PropertyDetailUiState()
    data class Success(val property: Property, val similar: List<Property>) : PropertyDetailUiState()
    data class Error(val message: String)                                    : PropertyDetailUiState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────────

@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {

    // ── List state ──────────────────────────────────────────────────────────
    private val _listState = MutableStateFlow<PropertyUiState>(PropertyUiState.Loading)
    val listState: StateFlow<PropertyUiState> = _listState.asStateFlow()

    // ── Detail state ────────────────────────────────────────────────────────
    private val _detailState =
        MutableStateFlow<PropertyDetailUiState>(PropertyDetailUiState.Loading)
    val detailState: StateFlow<PropertyDetailUiState> = _detailState.asStateFlow()

    // ── Saved / heart state for the currently viewed property ───────────────
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    // ── Active filter ────────────────────────────────────────────────────────
    private val _currentFilter = MutableStateFlow(PropertyFilterState())
    val currentFilter: StateFlow<PropertyFilterState> = _currentFilter.asStateFlow()

    // ── Selected district (from DistrictListScreen / HomeScreen) ─────────────
    private val _selectedDistrict = MutableStateFlow("All TN")
    val selectedDistrict: StateFlow<String> = _selectedDistrict.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Load / reload the property list.
     * Mock mode  → filter [MockData.approvedProperties] client-side.
     * API mode   → call [PropertyRepository.listProperties] with filter params.
     */
    fun loadProperties(
        district: String = _selectedDistrict.value,
        listingType: String = _currentFilter.value.listingType,
        filter: PropertyFilterState? = null,
        workCategory: String? = null,
    ) {
        val effectiveFilter = filter ?: _currentFilter.value.copy(
            district = if (district == "All TN") "" else district,
            listingType = listingType,
            workCategory = workCategory ?: _currentFilter.value.workCategory,
        )
        _selectedDistrict.value = district
        _currentFilter.value = effectiveFilter

        viewModelScope.launch {
            _listState.value = PropertyUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                val result = applyMockFilters(MockData.approvedProperties, effectiveFilter)
                _listState.value = PropertyUiState.Success(result)
            } else {
                repo.listProperties(
                    district = effectiveFilter.district.takeIf { it.isNotBlank() },
                    neighborhood = effectiveFilter.area.takeIf { it.isNotBlank() },
                    listingType = effectiveFilter.listingType.takeIf { it.isNotBlank() && it != "all" },
                    propertyType = effectiveFilter.propertyType,
                    minPrice = effectiveFilter.minPrice.toDouble().takeIf { it > 0 },
                    maxPrice = effectiveFilter.maxPrice.toDouble().takeIf { it < 20_000_000 },
                    bedrooms = effectiveFilter.bedrooms,
                    furnishing = effectiveFilter.furnishing,
                    amenities = effectiveFilter.amenities.takeIf { it.isNotEmpty() },
                    keyword = effectiveFilter.keyword.takeIf { it.isNotBlank() },
                    workCategory = effectiveFilter.workCategory,
                    page = 1,
                    limit = 40,
                ).fold(
                    onSuccess = { resp -> _listState.value = PropertyUiState.Success(resp.data) },
                    onFailure = { e ->
                        _listState.value = PropertyUiState.Error(e.message ?: "Failed to load")
                    },
                )
            }
        }
    }

    /** Called from FilterScreen "Apply" button. */
    fun applyFilter(newFilter: PropertyFilterState) {
        loadProperties(
            district = newFilter.district.ifBlank { "All TN" },
            listingType = newFilter.listingType,
            filter = newFilter,
        )
    }

    /** Called from FilterScreen "Reset" button. */
    fun resetFilter() {
        val district = _selectedDistrict.value
        val reset = PropertyFilterState(
            district = if (district == "All TN") "" else district,
            listingType = _currentFilter.value.listingType,
        )
        loadProperties(district = district, filter = reset)
    }

    // ── Detail ───────────────────────────────────────────────────────────────

    /** Check if the current property is already saved (called on open). */
    fun checkSaved(id: String) {
        viewModelScope.launch {
            repo.checkSaved(id).onSuccess { saved -> _isSaved.value = saved }
        }
    }

    /** Toggle save/unsave; optimistic UI update with server confirmation. */
    fun toggleSave(id: String) {
        val wasSaved = _isSaved.value
        _isSaved.value = !wasSaved   // optimistic
        viewModelScope.launch {
            if (wasSaved) {
                repo.unsaveProperty(id).onFailure { _isSaved.value = true }   // revert on error
            } else {
                repo.saveProperty(id).onFailure { _isSaved.value = false }    // revert on error
            }
        }
    }

    fun loadPropertyDetail(id: String) {
        viewModelScope.launch {
            _detailState.value = PropertyDetailUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                val property = MockData.getById(id)
                if (property != null) {
                    val similar = MockData.approvedProperties.filter {
                        it.id != id &&
                                it.district == property.district &&
                                it.listingType == property.listingType
                    }.take(4)
                    _detailState.value = PropertyDetailUiState.Success(property, similar)
                } else {
                    _detailState.value = PropertyDetailUiState.Error("Property not found")
                }
            } else {
                val propResult = repo.getProperty(id)
                val simResult = repo.getSimilar(id)
                propResult.fold(
                    onSuccess = { property ->
                        val similar = simResult.getOrDefault(emptyList())
                        _detailState.value = PropertyDetailUiState.Success(property, similar)
                    },
                    onFailure = { e ->
                        _detailState.value =
                            PropertyDetailUiState.Error(e.message ?: "Property not found")
                    },
                )
            }
        }
    }

    fun selectDistrict(district: String) {
        // Clear the area filter when district changes
        val newFilter = _currentFilter.value.copy(
            district = if (district == "All TN") "" else district,
            area = "",
        )
        loadProperties(district = district, filter = newFilter)
    }

    // ── Mock-mode client-side filter ─────────────────────────────────────────

    private fun applyMockFilters(
        props: List<Property>,
        filter: PropertyFilterState
    ): List<Property> {
        var result = props

        if (filter.listingType.isNotBlank() && filter.listingType != "all")
            result = result.filter { it.listingType.equals(filter.listingType, ignoreCase = true) }

        if (filter.district.isNotBlank())
            result =
                result.filter { it.district.orEmpty().equals(filter.district, ignoreCase = true) }

        if (filter.area.isNotBlank())
            result =
                result.filter { it.neighborhood.orEmpty().equals(filter.area, ignoreCase = true) }

        if (filter.minPrice > 0f)
            result = result.filter { it.price >= filter.minPrice.toLong() }

        if (filter.maxPrice < 20_000_000f)
            result = result.filter { it.price <= filter.maxPrice.toLong() }

        if (filter.bedrooms != null)
            result = result.filter { it.bedrooms == filter.bedrooms }

        if (filter.bathrooms != null)
            result = result.filter { it.bathrooms == filter.bathrooms }

        if (filter.furnishing != null)
            result = result.filter {
                it.furnishing.orEmpty().equals(filter.furnishing, ignoreCase = true)
            }

        if (filter.propertyType != null)
            result = result.filter {
                it.propertyType.orEmpty().equals(filter.propertyType, ignoreCase = true)
            }

        if (filter.amenities.isNotEmpty())
            result = result.filter { prop -> filter.amenities.all { it in prop.amenities } }

        if (filter.keyword.isNotBlank()) {
            val kw = filter.keyword.lowercase()
            result = result.filter {
                it.title.orEmpty().lowercase().contains(kw) ||
                        it.description.orEmpty().lowercase().contains(kw) ||
                        it.neighborhood.orEmpty().lowercase().contains(kw) ||
                        it.district.orEmpty().lowercase().contains(kw)
            }
        }

        // ── Work-category filter (contractor listings only) ─────────────────
        // Checks the JSONB metadata map for the "work_category" key.
        if (filter.workCategory != null) {
            result = result.filter {
                it.metadata?.get("work_category")
                    ?.toString()
                    ?.equals(filter.workCategory, ignoreCase = true) == true
            }
        }

        return result
    }

    // ── Discussions / Q&A State & Actions ────────────────────────────────────
    private val _discussions = MutableStateFlow<List<com.realestate.app.data.models.Discussion>>(emptyList())
    val discussions: StateFlow<List<com.realestate.app.data.models.Discussion>> = _discussions.asStateFlow()

    fun loadDiscussions(propertyId: String) {
        viewModelScope.launch {
            if (BuildConfig.USE_MOCK_DATA) {
                _discussions.value = MockData.propertyDiscussions.filter { it.propertyId == propertyId }
            } else {
                repo.getPropertyDiscussions(propertyId).fold(
                    onSuccess = { _discussions.value = it },
                    onFailure = { _discussions.value = emptyList() }
                )
            }
        }
    }

    fun postDiscussion(propertyId: String, message: String, parentId: String? = null) {
        viewModelScope.launch {
            if (BuildConfig.USE_MOCK_DATA) {
                val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
                if (parentId != null) {
                    val idx = MockData.propertyDiscussions.indexOfFirst { it.id == parentId }
                    if (idx >= 0) {
                        val parent = MockData.propertyDiscussions[idx]
                        val newReply = com.realestate.app.data.models.Reply(
                            id = "r-mock-${java.util.UUID.randomUUID()}",
                            userId = MockData.currentUser.id,
                            userName = MockData.currentUser.fullName,
                            message = message,
                            createdAt = nowStr
                        )
                        MockData.propertyDiscussions[idx] = parent.copy(replies = parent.replies + newReply)
                    }
                } else {
                    val newQuestion = com.realestate.app.data.models.Discussion(
                        id = "d-mock-${java.util.UUID.randomUUID()}",
                        propertyId = propertyId,
                        userId = MockData.currentUser.id,
                        userName = MockData.currentUser.fullName,
                        message = message,
                        createdAt = nowStr,
                        replies = emptyList()
                    )
                    MockData.propertyDiscussions.add(newQuestion)
                }
                loadDiscussions(propertyId)
            } else {
                val req = com.realestate.app.data.models.DiscussionCreateRequest(message, parentId)
                repo.postDiscussionMessage(propertyId, req).fold(
                    onSuccess = { loadDiscussions(propertyId) },
                    onFailure = { /* Handle error */ }
                )
            }
        }
    }
}
