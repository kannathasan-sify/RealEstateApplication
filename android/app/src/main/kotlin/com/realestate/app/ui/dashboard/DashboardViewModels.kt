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

/** Which lens the admin dashboard is showing. */
enum class AdminScope { PLATFORM, AGENT, BUILDER }

/** A pickable agent/builder for the admin's agent-wise / builder-wise filter. */
data class AdminPerson(val id: String, val name: String, val verified: Boolean)

@HiltViewModel
class AdminAnalyticsDashboardViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {
    // Platform-wide analytics
    private val _uiState = MutableStateFlow<DashboardUiState<AdminDashboardData>>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState<AdminDashboardData>> = _uiState

    // Filter scope + pickable people
    private val _scope = MutableStateFlow(AdminScope.PLATFORM)
    val scope: StateFlow<AdminScope> = _scope

    private val _agents = MutableStateFlow<List<AdminPerson>>(emptyList())
    val agents: StateFlow<List<AdminPerson>> = _agents

    private val _builders = MutableStateFlow<List<AdminPerson>>(emptyList())
    val builders: StateFlow<List<AdminPerson>> = _builders

    private val _selected = MutableStateFlow<AdminPerson?>(null)
    val selected: StateFlow<AdminPerson?> = _selected

    // Selected agent's dashboard (agent lens) / selected builder's dashboard (owner lens)
    private val _agentState = MutableStateFlow<DashboardUiState<AgentDashboardData>>(DashboardUiState.Loading)
    val agentState: StateFlow<DashboardUiState<AgentDashboardData>> = _agentState

    private val _builderState = MutableStateFlow<DashboardUiState<OwnerDashboardData>>(DashboardUiState.Loading)
    val builderState: StateFlow<DashboardUiState<OwnerDashboardData>> = _builderState

    init {
        load()
        loadPeople()
    }

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

    private fun loadPeople() {
        viewModelScope.launch {
            if (BuildConfig.USE_MOCK_DATA) {
                _agents.value = listOf(AdminPerson("agent-1", "Priya Sharma", true))
                _builders.value = listOf(AdminPerson("builder-1", "Skyline Developers", true))
                return@launch
            }
            repo.listUsers("agent").onSuccess { list ->
                _agents.value = list.map { AdminPerson(it.id, it.fullName.ifBlank { "Agent" }, it.isVerified) }
            }
            repo.listUsers("builder").onSuccess { list ->
                _builders.value = list.map { AdminPerson(it.id, it.fullName.ifBlank { "Builder" }, it.isVerified) }
            }
        }
    }

    fun setScope(newScope: AdminScope) {
        _scope.value = newScope
        _selected.value = null
    }

    fun selectPerson(person: AdminPerson) {
        _selected.value = person
        when (_scope.value) {
            AdminScope.AGENT -> loadAgent(person.id)
            AdminScope.BUILDER -> loadBuilder(person.id)
            AdminScope.PLATFORM -> Unit
        }
    }

    /** Re-run whichever filtered view is currently selected (Retry / refresh). */
    fun retrySelected() {
        _selected.value?.let { selectPerson(it) }
    }

    private fun loadAgent(id: String) {
        viewModelScope.launch {
            _agentState.value = DashboardUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                _agentState.value = DashboardUiState.Success(DashboardMockData.agent)
            } else {
                repo.getAgentDashboard(id).fold(
                    onSuccess = { _agentState.value = DashboardUiState.Success(it) },
                    onFailure = {
                        _agentState.value = DashboardUiState.Error(it.message ?: "Failed to load agent dashboard")
                    },
                )
            }
        }
    }

    private fun loadBuilder(id: String) {
        viewModelScope.launch {
            _builderState.value = DashboardUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                _builderState.value = DashboardUiState.Success(DashboardMockData.owner)
            } else {
                repo.getOwnerDashboard(id).fold(
                    onSuccess = { _builderState.value = DashboardUiState.Success(it) },
                    onFailure = {
                        _builderState.value = DashboardUiState.Error(it.message ?: "Failed to load builder dashboard")
                    },
                )
            }
        }
    }
}
