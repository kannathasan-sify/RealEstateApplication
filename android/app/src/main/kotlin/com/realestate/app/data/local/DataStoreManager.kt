package com.realestate.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "real_estate_prefs")

@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val AUTH_TOKEN       = stringPreferencesKey("auth_token")
        val USER_ID          = stringPreferencesKey("user_id")
        val USER_ROLE        = stringPreferencesKey("user_role")
        val USER_NAME        = stringPreferencesKey("user_name")
        val USER_EMAIL       = stringPreferencesKey("user_email")
        val USER_PHONE       = stringPreferencesKey("user_phone")   // agent's phone for ad posting
        val USER_AVATAR      = stringPreferencesKey("user_avatar")  // agent's photo url
        val ONBOARDING_DONE  = booleanPreferencesKey("onboarding_done")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userPhone: Flow<String?> = context.dataStore.data.map { it[USER_PHONE] }
    val userAvatar: Flow<String?> = context.dataStore.data.map { it[USER_AVATAR] }
    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }

    suspend fun saveAuthToken(token: String) = context.dataStore.edit { it[AUTH_TOKEN] = token }
    suspend fun saveUserId(id: String) = context.dataStore.edit { it[USER_ID] = id }
    suspend fun saveUserRole(role: String) = context.dataStore.edit { it[USER_ROLE] = role }
    suspend fun saveUserName(name: String) = context.dataStore.edit { it[USER_NAME] = name }
    suspend fun saveUserPhone(phone: String) = context.dataStore.edit { it[USER_PHONE] = phone }
    suspend fun saveUserAvatar(url: String) = context.dataStore.edit { it[USER_AVATAR] = url }
    suspend fun setOnboardingDone() = context.dataStore.edit { it[ONBOARDING_DONE] = true }
    suspend fun setBiometricEnabled(enabled: Boolean) = context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }

    // ── Generic boolean pref helpers (used by NotificationSettingsViewModel) ──
    suspend fun getBooleanPref(key: Preferences.Key<Boolean>, default: Boolean = false): Boolean =
        context.dataStore.data.map { it[key] ?: default }.first()

    suspend fun saveBooleanPref(key: Preferences.Key<Boolean>, value: Boolean) =
        context.dataStore.edit { it[key] = value }
}
