package com.realestate.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.mock.AdMockData
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.HomeAd
import com.realestate.app.data.models.Property
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Home data is considered fresh for this long, so returning to the screen doesn't refetch. */
private const val FRESH_WINDOW_MS = 60_000L

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

    /**
     * The district the user has selected in the top-bar picker.
     *
     * Derived once and shared. (Previously this was a `get()` that built a NEW
     * MutableStateFlow and launched a NEW collector coroutine on every access —
     * one leaked coroutine per read, which on a recomposing screen adds up fast.)
     */
    val selectedDistrict: StateFlow<String> = _uiState
        .map { it.selectedDistrict }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "All TN")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** True while the ad feed is being fetched — drives the skeleton placeholder. */
    private val _adsLoading = MutableStateFlow(false)
    val adsLoading: StateFlow<Boolean> = _adsLoading.asStateFlow()

    /** Timestamp of the last successful home load, used to skip redundant refetches. */
    private var lastLoadedAt = 0L

    /** Ranked home advertisements from the server-side Ad Ranking Engine (GET /ads/home). */
    private val _homeAds = MutableStateFlow<List<HomeAd>>(emptyList())
    val homeAds: StateFlow<List<HomeAd>> = _homeAds.asStateFlow()

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
    fun loadHome(district: String = _uiState.value.selectedDistrict, force: Boolean = false) {
        // Skip redundant work: returning to Home (ON_RESUME) re-triggers this, but if the
        // district hasn't changed and we loaded recently there's nothing new to fetch.
        val now = System.currentTimeMillis()
        val sameDistrict = district == _uiState.value.selectedDistrict
        if (!force && sameDistrict && now - lastLoadedAt < FRESH_WINDOW_MS) return
        lastLoadedAt = now

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading        = false,   // Home no longer renders a loading state
                selectedDistrict = district,
                allDistricts     = listOf("All TN") + TamilNaduData.districts,
                error            = null,
            )

            loadHomeAds(district)

            // ── DISABLED: category-wise property sections ────────────────────────
            // The Rent / Sale / Holiday / Ground / Contractor rows are no longer
            // rendered on Home, but their fetches were still firing 5 property
            // requests on every load and district change. Commented out to stop the
            // unused network calls — re-enable this block if those rows come back.
            /*
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
            */
        }
    }

    /** Called when user picks a district from the top-bar bottom-sheet. */
    fun selectDistrict(district: String) {
        loadHome(district)
    }

    // ── Ranked home ads ─────────────────────────────────────────────────────────

    /** Load the ranked ad feed for the current district (non-critical; failures are ignored). */
    fun loadHomeAds(district: String = _uiState.value.selectedDistrict) {
        viewModelScope.launch {
            // Only show the skeleton on a cold load — a refresh keeps the current ads
            // on screen so the feed never flashes empty.
            if (_homeAds.value.isEmpty()) _adsLoading.value = true
            if (BuildConfig.USE_MOCK_DATA) {
                _homeAds.value = AdMockData.homeAds
            } else {
                val districtFilter = if (district == "All TN") null else district
                repo.getHomeAds(district = districtFilter, listingType = "rent", limit = 10)
                    // Dev fallback: if the ads table is empty (no seed yet) or the request fails,
                    // show the sample ads so the feed is never blank. Real ads always win when present.
                    // TODO(prod): drop the fallback and show an empty state instead.
                    .onSuccess { _homeAds.value = it.ifEmpty { AdMockData.homeAds } }
                    .onFailure { _homeAds.value = AdMockData.homeAds }
            }
            _adsLoading.value = false
        }
    }

    /** Fire-and-forget engagement signals fed back into the ranking engine. */
    fun recordAdImpression(adId: String) = viewModelScope.launch { repo.adImpression(adId) }.let {}
    fun recordAdClick(adId: String) = viewModelScope.launch { repo.adClick(adId) }.let {}
    fun reportAd(adId: String) = viewModelScope.launch { repo.adReport(adId, "reported from home") }.let {}

    /** Hide an ad: drop it locally for instant feedback, then persist so it stays hidden. */
    fun hideAd(adId: String) {
        _homeAds.value = _homeAds.value.filterNot { it.adId == adId }
        viewModelScope.launch { repo.adHide(adId) }
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
