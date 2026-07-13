package com.realestate.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.models.User
import com.realestate.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state

    fun register(email: String, password: String, fullName: String, phone: String? = null) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            repo.register(email, password, fullName, phone).fold(
                onSuccess = { _state.value = AuthUiState.Success(it.user) },
                onFailure = { _state.value = AuthUiState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun login(email: String, password: String, userIdCode: String? = null) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            repo.login(email.ifBlank { null }, password, userIdCode).fold(
                onSuccess = { _state.value = AuthUiState.Success(it.user) },
                onFailure = { _state.value = AuthUiState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun setRole(role: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            repo.setRole(role).fold(
                onSuccess = {
                    _state.value = AuthUiState.Success(it)
                    onDone?.invoke()
                },
                onFailure = { _state.value = AuthUiState.Error(it.message ?: "Failed to set role") }
            )
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            repo.googleAuth(idToken).fold(
                onSuccess = { _state.value = AuthUiState.Success(it.user) },
                onFailure = { _state.value = AuthUiState.Error(it.message ?: "Google sign-in failed") }
            )
        }
    }

    fun resetState() { _state.value = AuthUiState.Idle }
}
