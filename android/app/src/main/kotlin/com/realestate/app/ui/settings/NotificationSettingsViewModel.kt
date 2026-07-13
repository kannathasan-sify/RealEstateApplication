package com.realestate.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.preferences.core.booleanPreferencesKey

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
) : ViewModel() {

    // All toggles stored locally in DataStore
    // Keys
    private val KEY_BOOKING_UPDATES   = booleanPreferencesKey("notif_booking_updates")
    private val KEY_PRICE_ALERTS      = booleanPreferencesKey("notif_price_alerts")
    private val KEY_NEW_LISTINGS      = booleanPreferencesKey("notif_new_listings")
    private val KEY_APPROVAL_STATUS   = booleanPreferencesKey("notif_approval_status")
    private val KEY_PROMOTIONS        = booleanPreferencesKey("notif_promotions")
    private val KEY_PUSH_ENABLED      = booleanPreferencesKey("notif_push_enabled")

    // Individual state flows — default all true except promotions
    val bookingUpdates  = MutableStateFlow(true)
    val priceAlerts     = MutableStateFlow(true)
    val newListings     = MutableStateFlow(true)
    val approvalStatus  = MutableStateFlow(true)
    val promotions      = MutableStateFlow(false)
    val pushEnabled     = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            // Load persisted values
            bookingUpdates.value  = dataStore.getBooleanPref(KEY_BOOKING_UPDATES, true)
            priceAlerts.value     = dataStore.getBooleanPref(KEY_PRICE_ALERTS, true)
            newListings.value     = dataStore.getBooleanPref(KEY_NEW_LISTINGS, true)
            approvalStatus.value  = dataStore.getBooleanPref(KEY_APPROVAL_STATUS, true)
            promotions.value      = dataStore.getBooleanPref(KEY_PROMOTIONS, false)
            pushEnabled.value     = dataStore.getBooleanPref(KEY_PUSH_ENABLED, true)
        }
    }

    fun toggle(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, flow: MutableStateFlow<Boolean>) {
        val newVal = !flow.value
        flow.value = newVal
        viewModelScope.launch { dataStore.saveBooleanPref(key, newVal) }
    }

    fun toggleBookingUpdates()  = toggle(KEY_BOOKING_UPDATES,  bookingUpdates)
    fun togglePriceAlerts()     = toggle(KEY_PRICE_ALERTS,     priceAlerts)
    fun toggleNewListings()     = toggle(KEY_NEW_LISTINGS,     newListings)
    fun toggleApprovalStatus()  = toggle(KEY_APPROVAL_STATUS,  approvalStatus)
    fun togglePromotions()      = toggle(KEY_PROMOTIONS,       promotions)
    fun togglePushEnabled()     = toggle(KEY_PUSH_ENABLED,     pushEnabled)
}
