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

/** Minimum/default plan every user falls back to once a paid plan lapses. */
const val MIN_TIER = "free"

/**
 * True if [expiresAt] (an ISO datetime string from the backend, e.g. "2026-07-15T10:30:00")
 * is in the past. Compares by date only (matches the "Expires: yyyy-MM-dd" truncation
 * already shown in the UI) — the authoritative check is still server-side; this is a
 * client-side pre-flight guard so the UI doesn't offer actions the backend will reject.
 */
fun isSubscriptionExpired(expiresAt: String?): Boolean {
    if (expiresAt.isNullOrBlank()) return false
    return runCatching {
        val expiryDate = expiresAt.take(10)
        val today = java.time.LocalDate.now().toString()
        expiryDate < today
    }.getOrDefault(false)
}

/** True if the user currently has a paid plan that hasn't expired yet. */
fun isActivePaidPlan(details: SubscriptionDetails): Boolean =
    details.subscriptionTier != MIN_TIER && !isSubscriptionExpired(details.subscriptionExpiresAt)

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
        // Pre-flight validation, mirroring the backend's rules — avoids a round-trip for
        // the two disallowed cases: re-buying the same active plan, or downgrading to the
        // minimum (Free) plan while a paid plan is still active.
        val current = (_uiState.value as? SubscriptionUiState.Success)?.details
        if (current != null && isActivePaidPlan(current)) {
            val expiryNote = current.subscriptionExpiresAt?.take(10)?.let { " until $it" } ?: ""
            if (tier == current.subscriptionTier) {
                upgradeState.value = UpgradeState.Error(
                    "You already have an active ${current.subscriptionTier.replaceFirstChar { it.uppercase() }} plan$expiryNote."
                )
                return
            }
            if (tier == MIN_TIER) {
                upgradeState.value = UpgradeState.Error(
                    "Your ${current.subscriptionTier.replaceFirstChar { it.uppercase() }} plan is still active$expiryNote. You can't downgrade to Free until it expires."
                )
                return
            }
        }

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
