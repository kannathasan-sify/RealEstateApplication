package com.realestate.app.data.repository

import com.realestate.app.data.api.ApiService
import com.realestate.app.data.local.DataStoreManager
import com.realestate.app.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val dataStore: DataStoreManager,
) {
    private suspend fun cacheUserSession(token: String, user: User) {
        dataStore.saveAuthToken(token)
        dataStore.saveUserId(user.id)
        dataStore.saveUserRole(user.role.name)
        dataStore.saveUserName(user.fullName.ifBlank { "" })
        if (user.phone.isNotBlank())         dataStore.saveUserPhone(user.phone)
        if (!user.avatarUrl.isNullOrBlank()) dataStore.saveUserAvatar(user.avatarUrl)
    }

    suspend fun register(email: String, password: String, fullName: String, phone: String?): Result<TokenResponse> =
        runCatching {
            val res = api.register(RegisterRequest(email, password, fullName, phone))
            cacheUserSession(res.accessToken, res.user)
            res
        }

    suspend fun login(email: String?, password: String, userIdCode: String?): Result<TokenResponse> =
        runCatching {
            val res = api.login(LoginRequest(email, password, userIdCode))
            cacheUserSession(res.accessToken, res.user)
            res
        }

    suspend fun googleAuth(idToken: String): Result<TokenResponse> =
        runCatching {
            val res = api.googleAuth(GoogleAuthRequest(idToken))
            cacheUserSession(res.accessToken, res.user)
            res
        }

    suspend fun logout() {
        runCatching { api.logout() }
        dataStore.clearAll()
    }

    suspend fun getMe(): Result<User> = runCatching { api.getMe() }

    suspend fun updateMe(request: ProfileUpdateRequest): Result<User> =
        runCatching { api.updateMe(request) }

    suspend fun setRole(role: String): Result<User> =
        runCatching {
            val user = api.setRole(RoleUpdateRequest(role))
            dataStore.saveUserRole(user.role.name)
            user
        }

    suspend fun toggleBiometric(enabled: Boolean): Result<User> =
        runCatching { api.toggleBiometric(mapOf("biometric_enabled" to enabled)) }
}
