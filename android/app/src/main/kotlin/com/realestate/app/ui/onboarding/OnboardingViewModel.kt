package com.realestate.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
) : ViewModel() {

    fun markOnboardingDone() {
        viewModelScope.launch {
            dataStore.setOnboardingDone()
        }
    }
}
