package com.realestate.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.Property
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    /** Currently selected district — "All TN" means no district filter */
    val selectedDistrict: String = "All TN",
    /** Ordered list shown in the district picker */
    val allDistricts: List<String> = emptyList(),
    val rentProperties: List<Property> = emptyList(),
    val saleProperties: List<Property> = emptyList(),
    val holidayStayProperties: List<Property> = emptyList(),
    val groundListings: List<Property> = emptyList(),
    val contractorListings: List<Property> = emptyList(),
    val searchResults: List<Property> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** The district the user has selected in the top-bar picker. */
    val selectedDistrict: StateFlow<String>
        get() = MutableStateFlow(_uiState.value.selectedDistrict).also { flow ->
            viewModelScope.launch {
                _uiState.collect { flow.value = it.selectedDistrict }
            }
        }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadHome("All TN")
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .collect { query -> executeSearch(query) }
        }
    }

    // ── Home data ─────────────────────────────────────────────────────────────

    /**
     * Reload all home-feed sections.
     * [district] = "All TN" → no district filter; otherwise filter by district.
     */
    fun loadHome(district: String = _uiState.value.selectedDistrict) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading        = true,
                selectedDistrict = district,
                error            = null,
            )

            val districtFilter = if (district == "All TN") null else district

            if (BuildConfig.USE_MOCK_DATA) {
                val all = MockData.approvedProperties
                val filtered = if (districtFilter != null)
                    all.filter { it.district.equals(districtFilter, ignoreCase = true) }
                else all

                // Fall back to all-TN if the selected district has no listings yet
                val base = if (filtered.isEmpty()) all else filtered

                _uiState.value = _uiState.value.copy(
                    isLoading             = false,
                    allDistricts          = listOf("All TN") + TamilNaduData.districts,
                    rentProperties        = base.filter { it.listingType == "rent" }.take(10),
                    saleProperties        = base.filter { it.listingType == "sale" }.take(10),
                    holidayStayProperties = all.filter { it.listingType == "holiday_stay" }.take(10),
                    groundListings        = all.filter { it.listingType == "ground" }.take(10),
                    contractorListings    = all.filter { it.listingType == "contractor" }.take(10),
                )
            } else {
                val rentResult       = repo.listProperties(district = districtFilter, listingType = "rent",         page = 1, limit = 10)
                val saleResult       = repo.listProperties(district = districtFilter, listingType = "sale",         page = 1, limit = 10)
                val holidayResult    = repo.listProperties(listingType = "holiday_stay", page = 1, limit = 10)
                val groundResult     = repo.listProperties(listingType = "ground",       page = 1, limit = 10)
                val contractorResult = repo.listProperties(listingType = "contractor",   page = 1, limit = 10)

                _uiState.value = _uiState.value.copy(
                    isLoading             = false,
                    allDistricts          = listOf("All TN") + TamilNaduData.districts,
                    rentProperties        = rentResult.getOrDefault(null)?.data       ?: emptyList(),
                    saleProperties        = saleResult.getOrDefault(null)?.data       ?: emptyList(),
                    holidayStayProperties = holidayResult.getOrDefault(null)?.data    ?: emptyList(),
                    groundListings        = groundResult.getOrDefault(null)?.data     ?: emptyList(),
                    contractorListings    = contractorResult.getOrDefault(null)?.data ?: emptyList(),
                    error                 = if (rentResult.isFailure) rentResult.exceptionOrNull()?.message else null,
                )
            }
        }
    }

    /** Called when user picks a district from the top-bar bottom-sheet. */
    fun selectDistrict(district: String) {
        loadHome(district)
    }

    // ── Live Search ───────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
        }
    }

    private fun executeSearch(query: String) {
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(isSearching = true)
        viewModelScope.launch {
            if (BuildConfig.USE_MOCK_DATA) {
                val kw = query.lowercase()
                val results = MockData.approvedProperties.filter {
                    it.title.orEmpty().lowercase().contains(kw) ||
                    it.neighborhood.orEmpty().lowercase().contains(kw) ||
                    it.district.orEmpty().lowercase().contains(kw) ||
                    it.description.orEmpty().lowercase().contains(kw)
                }.take(15)
                _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
            } else {
                repo.listProperties(keyword = query, page = 1, limit = 15).fold(
                    onSuccess = { resp ->
                        _uiState.value = _uiState.value.copy(searchResults = resp.data, isSearching = false)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
                    },
                )
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
    }
}
