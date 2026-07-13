package com.realestate.app.ui.splash

import androidx.lifecycle.ViewModel
import com.realestate.app.data.local.DataStoreManager
import com.realestate.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
) : ViewModel() {

    suspend fun getStartDestination(): String {
        val onboardingDone = dataStore.isOnboardingDone.firstOrNull() ?: false
        val token = dataStore.authToken.firstOrNull()
        return when {
            !onboardingDone  -> Screen.Onboarding.route
            token.isNullOrBlank() -> Screen.Login.route
            else             -> Screen.Home.route
        }
    }
}
