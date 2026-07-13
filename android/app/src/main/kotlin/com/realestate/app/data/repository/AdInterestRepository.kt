package com.realestate.app.data.repository

import com.realestate.app.data.models.AdBanner
import com.realestate.app.data.models.AdInterest
import com.realestate.app.data.models.AdInterestRequest
import com.realestate.app.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory store for ad interest registrations.
 *
 * Architecture:
 *  - Mock mode  (BuildConfig.USE_MOCK_DATA=true) → persists in this singleton
 *    only for the session lifetime; resets on app restart.
 *  - Production → replace [saveInterest] body with an API call:
 *      apiService.postAdInterest(adId, AdInterestRequest(...))
 *
 * Access via [AdInterestRepository.instance] (manual singleton) or inject
 * through Hilt once a @Singleton binding is added in RepositoryModule.
 */
object AdInterestRepository {

    // ── In-memory store ──────────────────────────────────────────────────────
    private val _interests = MutableStateFlow<List<AdInterest>>(emptyList())

    /** Observe the full list of recorded interests (admin / debug use) */
    val interests: StateFlow<List<AdInterest>> = _interests.asStateFlow()

    // ── Already-interested set — fast O(1) check per adId ───────────────────
    private val _interestedAdIds = MutableStateFlow<Set<String>>(emptySet())
    val interestedAdIds: StateFlow<Set<String>> = _interestedAdIds.asStateFlow()

    /**
     * Returns true if the current user has already expressed interest in [adId].
     * Called before rendering the button to restore state across dialog open/close.
     */
    fun isInterested(adId: String): Boolean = _interestedAdIds.value.contains(adId)

    /**
     * Saves the user's interest in [ad].
     *
     * [user]  — currently authenticated user (pass MockData.currentUser in debug).
     * [note]  — optional free-text note the user typed.
     *
     * Returns [AdInterest] so the caller can show the id or log it.
     *
     * In production: swap the body for:
     *   apiService.postAdInterest(ad.id, AdInterestRequest(ad.id, ad.title, ...))
     */
    fun saveInterest(
        ad:   AdBanner,
        user: User,
        note: String? = null,
    ): AdInterest {
        // Derive listing type from the CTA URL if present
        val listingType = deriveListing(ad.ctaUrl)

        val interest = AdInterest(
            adId           = ad.id,
            adTitle        = ad.title,
            advertiserName = ad.advertiserName,
            listingType    = listingType,
            userId         = user.id,
            userIdCode     = user.userIdCode,
            userName       = user.fullName,
            userPhone      = user.phone,
            userEmail      = user.email,
            note           = note?.ifBlank { null },
        )

        // Deduplicate — if already interested, update the note only
        _interests.update { current ->
            val existing = current.indexOfFirst { it.adId == ad.id && it.userId == user.id }
            if (existing >= 0) {
                current.toMutableList().also { list ->
                    list[existing] = list[existing].copy(note = interest.note)
                }
            } else {
                current + interest
            }
        }

        _interestedAdIds.update { it + ad.id }
        return interest
    }

    /**
     * Removes an interest (user un-taps "Interested").
     */
    fun removeInterest(adId: String, userId: String) {
        _interests.update { current ->
            current.filter { !(it.adId == adId && it.userId == userId) }
        }
        _interestedAdIds.update { it - adId }
    }

    /** All interests for a given ad (for advertiser/admin dashboard) */
    fun interestsForAd(adId: String): List<AdInterest> =
        _interests.value.filter { it.adId == adId }

    /** All interests registered by a given user */
    fun interestsByUser(userId: String): List<AdInterest> =
        _interests.value.filter { it.userId == userId }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun deriveListing(ctaUrl: String?): String {
        if (ctaUrl.isNullOrBlank()) return "general"
        return try {
            val uri = android.net.Uri.parse(ctaUrl)
            when (uri.scheme) {
                "nestx" -> uri.getQueryParameter("listing_type") ?: uri.host ?: "general"
                else    -> "general"
            }
        } catch (e: Exception) { "general" }
    }
}
