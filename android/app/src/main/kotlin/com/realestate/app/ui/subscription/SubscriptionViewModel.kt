package com.realestate.app.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.api.SubscriptionDetails
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SubscriptionUiState {
    object Loading : SubscriptionUiState()
    data class Success(val details: SubscriptionDetails) : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repo: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState

    val upgradeState = MutableStateFlow<UpgradeState>(UpgradeState.Idle)

    init {
        loadSubscription()
    }

    fun loadSubscription() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(400)
                _uiState.value = SubscriptionUiState.Success(MockData.mockSubscriptionDetails)
            } else {
                repo.getSubscriptionDetails().fold(
                    onSuccess = { _uiState.value = SubscriptionUiState.Success(it) },
                    onFailure = { _uiState.value = SubscriptionUiState.Error(it.message ?: "Failed to load subscription") }
                )
            }
        }
    }

    fun upgradePlan(tier: String) {
        viewModelScope.launch {
            upgradeState.value = UpgradeState.Loading
            if (BuildConfig.USE_MOCK_DATA) {
                delay(800)
                val maxList = when (tier.lowercase()) {
                    "free" -> 3
                    "silver" -> 10
                    else -> 99999
                }
                val maxImg = if (tier.lowercase() == "free") 10 else 20
                MockData.mockSubscriptionDetails = SubscriptionDetails(
                    subscriptionTier = tier,
                    maxListings = maxList,
                    maxImages = maxImg,
                    videoEnabled = tier.lowercase() != "free",
                    featuredEnabled = tier.lowercase() != "free" && tier.lowercase() != "silver",
                    currentListingsCount = MockData.mockSubscriptionDetails.currentListingsCount
                )
                upgradeState.value = UpgradeState.Success
                loadSubscription()
            } else {
                repo.upgradeSubscription(tier).fold(
                    onSuccess = {
                        upgradeState.value = UpgradeState.Success
                        loadSubscription()
                    },
                    onFailure = {
                        upgradeState.value = UpgradeState.Error(it.message ?: "Upgrade failed")
                    }
                )
            }
        }
    }
}

sealed class UpgradeState {
    object Idle : UpgradeState()
    object Loading : UpgradeState()
    object Success : UpgradeState()
    data class Error(val message: String) : UpgradeState()
}
