package com.realestate.app.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.models.Property
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(private val repo: PropertyRepository) : ViewModel() {
    private val _properties = MutableStateFlow<List<Property>>(emptyList())
    val properties: StateFlow<List<Property>> = _properties

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Non-null = an error occurred; null = no error */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getSaved()
                .onSuccess { list ->
                    _properties.value = list
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to load saved properties"
                }
            _isLoading.value = false
        }
    }

    fun unsave(id: String) {
        viewModelScope.launch {
            // Optimistic local removal
            _properties.value = _properties.value.filter { it.id != id }
            repo.unsaveProperty(id).onFailure {
                // Revert on failure by reloading
                load()
            }
        }
    }
}
