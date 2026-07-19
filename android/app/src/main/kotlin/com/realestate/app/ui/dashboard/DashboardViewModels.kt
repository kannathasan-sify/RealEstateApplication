package com.realestate.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.mock.DashboardMockData
import com.realestate.app.data.models.AdminDashboardData
import com.realestate.app.data.models.AgentDashboardData
import com.realestate.app.data.models.DashboardUiState
import com.realestate.app.data.models.OwnerDashboardData
import com.realestate.app.data.models.PartnerDashboardData
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dashboard ViewModels.
 *
 * The role dashboards are analytics-only surfaces with no backend endpoint yet, so they serve
 * [DashboardMockData]. When a real dashboard endpoint exists, inject a repository and
 * replace the mock assignment in each `load()` with the fetch (falling back on failure).
 */

@HiltViewModel
class OwnerDashboardViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState<OwnerDashboardData>>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState<OwnerDashboardData>> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                _uiState.value = DashboardUiState.Success(DashboardMockData.owner)
            } else {
                repo.getOwnerDashboard().fold(
                    onSuccess = { _uiState.value = DashboardUiState.Success(it) },
                    onFailure = {
                        _uiState.value = DashboardUiState.Error(it.message ?: "Failed to load dashboard")
                    },
                )
            }
        }
    }
}

@HiltViewModel
class AgentDashboardViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState<AgentDashboardData>>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState<AgentDashboardData>> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                _uiState.value = DashboardUiState.Success(DashboardMockData.agent)
            } else {
                repo.getAgentDashboard().fold(
                    onSuccess = { _uiState.value = DashboardUiState.Success(it) },
                    onFailure = {
                        _uiState.value = DashboardUiState.Error(it.message ?: "Failed to load dashboard")
                    },
                )
            }
        }
    }
}

@HiltViewModel
class PartnerDashboardViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState<PartnerDashboardData>>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState<PartnerDashboardData>> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                _uiState.value = DashboardUiState.Success(DashboardMockData.partner)
            } else {
                repo.getPartnerDashboard().fold(
                    onSuccess = { _uiState.value = DashboardUiState.Success(it) },
                    onFailure = {
                        _uiState.value = DashboardUiState.Error(it.message ?: "Failed to load dashboard")
                    },
                )
            }
        }
    }
}

@HiltViewModel
class AdminAnalyticsDashboardViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState<AdminDashboardData>>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState<AdminDashboardData>> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                _uiState.value = DashboardUiState.Success(DashboardMockData.admin)
            } else {
                repo.getAdminDashboard().fold(
                    onSuccess = { _uiState.value = DashboardUiState.Success(it) },
                    onFailure = {
                        _uiState.value = DashboardUiState.Error(it.message ?: "Failed to load dashboard")
                    },
                )
            }
        }
    }
}
