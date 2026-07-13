package com.realestate.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.local.DataStoreManager
import com.realestate.app.data.models.ProfileUpdateRequest
import com.realestate.app.data.models.User
import com.realestate.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AccountSettingsState {
    object Idle    : AccountSettingsState()
    object Loading : AccountSettingsState()
    object Saved   : AccountSettingsState()
    data class Error(val message: String) : AccountSettingsState()
}

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val dataStore: DataStoreManager,
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _state = MutableStateFlow<AccountSettingsState>(AccountSettingsState.Idle)
    val state: StateFlow<AccountSettingsState> = _state

    // Editable fields
    val fullName  = MutableStateFlow("")
    val phone     = MutableStateFlow("")
    val city      = MutableStateFlow("")
    val language  = MutableStateFlow("English")

    init {
        viewModelScope.launch {
            authRepo.getMe().onSuccess { u ->
                _user.value = u
                fullName.value = u.fullName
                phone.value    = u.phone
                city.value     = u.city
                language.value = u.language
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _state.value = AccountSettingsState.Loading
            authRepo.updateMe(
                ProfileUpdateRequest(
                    fullName = fullName.value.trim(),
                    phone    = phone.value.trim(),
                    city     = city.value.trim(),
                    language = language.value,
                )
            ).onSuccess {
                dataStore.saveUserName(fullName.value.trim())
                _state.value = AccountSettingsState.Saved
            }.onFailure { e ->
                _state.value = AccountSettingsState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun resetState() { _state.value = AccountSettingsState.Idle }
}
