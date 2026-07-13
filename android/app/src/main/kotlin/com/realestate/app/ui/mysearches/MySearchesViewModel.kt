package com.realestate.app.ui.mysearches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.api.SavedSearch
import com.realestate.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MySearchesUiState(
    val isLoading: Boolean = false,
    val searches: List<SavedSearch> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MySearchesViewModel @Inject constructor(
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MySearchesUiState())
    val state: StateFlow<MySearchesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            userRepo.getSavedSearches()
                .onSuccess { list -> _state.value = _state.value.copy(isLoading = false, searches = list) }
                .onFailure { e  -> _state.value = _state.value.copy(isLoading = false, error = e.message) }
        }
    }
}
