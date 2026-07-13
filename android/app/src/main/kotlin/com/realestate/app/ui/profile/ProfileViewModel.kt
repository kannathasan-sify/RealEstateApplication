package com.realestate.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.models.ProfileUpdateRequest
import com.realestate.app.data.models.User
import com.realestate.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
    object Saved : ProfileState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(private val authRepo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state

    val fullName = MutableStateFlow("")
    val phone = MutableStateFlow("")
    val city = MutableStateFlow("")
    val language = MutableStateFlow("")

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepo.getMe().fold(
                onSuccess = { user ->
                    _state.value = ProfileState.Success(user)
                    fullName.value = user.fullName ?: ""
                    phone.value = user.phone ?: ""
                    city.value = user.city
                    language.value = user.language
                },
                onFailure = { _state.value = ProfileState.Error(it.message ?: "Failed to load profile") }
            )
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            authRepo.updateMe(
                ProfileUpdateRequest(
                    fullName = fullName.value,
                    phone = phone.value.ifBlank { null },
                    city = city.value,
                    language = language.value,
                )
            ).fold(
                onSuccess = { _state.value = ProfileState.Saved },
                onFailure = { _state.value = ProfileState.Error(it.message ?: "Failed to save") }
            )
        }
    }
}
