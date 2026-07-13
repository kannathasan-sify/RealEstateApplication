package com.realestate.app.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.local.DataStoreManager
import com.realestate.app.data.models.User
import com.realestate.app.data.models.UserRole
import com.realestate.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val dataStore: DataStoreManager,
) : ViewModel() {

    val userName: StateFlow<String?> = dataStore.userName
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    /** True when the signed-in user has the ADMIN role. */
    val isAdmin: StateFlow<Boolean> = _user
        .map { it?.role == UserRole.ADMIN }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            authRepo.getMe().onSuccess { _user.value = it }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }
}
