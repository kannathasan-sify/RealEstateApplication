package com.realestate.app.ui.myads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.models.ApprovalStatus
import com.realestate.app.data.models.Property
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MyAdsTab(val label: String) {
    ALL("All"), PENDING("Pending"), APPROVED("Approved"), REJECTED("Rejected")
}

data class MyAdsUiState(
    val isLoading: Boolean = false,
    val properties: List<Property> = emptyList(),
    val error: String? = null,
    val selectedTab: MyAdsTab = MyAdsTab.ALL,
    val deleteSuccess: String? = null,
)

/** How long the fetched ad list stays fresh before a reload is allowed. */
private const val MY_ADS_FRESH_MS = 60_000L

@HiltViewModel
class MyAdsViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyAdsUiState())
    val state: StateFlow<MyAdsUiState> = _state.asStateFlow()

    /** Properties filtered by the currently selected tab */
    val filteredProperties: StateFlow<List<Property>> = _state
        .map { s ->
            when (s.selectedTab) {
                MyAdsTab.ALL      -> s.properties
                MyAdsTab.PENDING  -> s.properties.filter { it.approvalStatus == ApprovalStatus.PENDING }
                MyAdsTab.APPROVED -> s.properties.filter { it.approvalStatus == ApprovalStatus.APPROVED }
                MyAdsTab.REJECTED -> s.properties.filter { it.approvalStatus == ApprovalStatus.REJECTED }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** My Ads is user-scoped, so it is never disk-cached — this in-memory guard just
     *  stops the same screen session refetching (e.g. on resume) within the window. */
    private var lastLoadedAt = 0L

    init { load() }

    fun load(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val hasData = _state.value.properties.isNotEmpty()
        if (!force && hasData && now - lastLoadedAt < MY_ADS_FRESH_MS) return
        lastLoadedAt = now
        viewModelScope.launch {
            // Skeleton only on a cold load — a refresh keeps the current ads on screen.
            _state.value = _state.value.copy(isLoading = !hasData, error = null)
            repo.getMyProperties()
                .onSuccess { list -> _state.value = _state.value.copy(isLoading = false, properties = list) }
                .onFailure { e -> _state.value = _state.value.copy(isLoading = false, error = e.message) }
        }
    }

    fun selectTab(tab: MyAdsTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun deleteProperty(id: String) {
        viewModelScope.launch {
            repo.deleteProperty(id)
                .onSuccess {
                    // Remove from local list immediately
                    _state.value = _state.value.copy(
                        properties = _state.value.properties.filterNot { it.id == id },
                        deleteSuccess = "Ad deleted successfully",
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = "Delete failed: ${e.message}")
                }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, deleteSuccess = null)
    }
}
