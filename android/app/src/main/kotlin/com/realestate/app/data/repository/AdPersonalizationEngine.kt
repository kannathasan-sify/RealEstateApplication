package com.realestate.app.data.repository

import com.realestate.app.data.models.AdBanner

/**
 * Personalised ad ranking engine.
 *
 * Scores each ad against the current user context and returns a re-ordered
 * list with the most relevant ads first. The original [AdBanner.order] field
 * is used as a tie-breaker so advertiser-defined sequence is respected when
 * relevance scores are equal.
 *
 * ── Scoring rubric (max 100 pts) ───────────────────────────────────────────
 *
 *  +30  District match  (user's selected district == one of targetDistricts)
 *  +25  Listing-type match (user's current browse type == targetListingTypes)
 *  +20  Priority boost (premium=20, featured=10, standard=0)
 *  +10  Has video (richer engagement predicted)
 *  +10  Social proof boost (≥50 interested)
 *  + 5  Already-interested penalty reversal: skip ads the user already acted on
 *        (so they see new content instead of repeat ads)
 *  − 5  User already marked this ad "Interested" (de-prioritise to avoid fatigue)
 *
 * ── A/B Test rotation ────────────────────────────────────────────────────────
 * When two ads share the same [AdBanner.campaignId], only the one matching the
 * user's assigned variant bucket is surfaced. Variant is determined by
 * (userId.hashCode() % 2 == 0) → "A", else → "B".
 */
object AdPersonalizationEngine {

    /**
     * Rank [ads] for display to a user described by [userDistrict],
     * [userListingType] (the category they last browsed), and [userId].
     *
     * Ads suppressed by frequency capping ([AdFrequencyStore]) are removed.
     * Campaign A/B deduplication is applied so only one variant per campaign
     * is shown to each user.
     *
     * @return Re-ordered list, highest score first.
     */
    fun rank(
        ads:             List<AdBanner>,
        userDistrict:    String,
        userListingType: String,
        userId:          String = "",
    ): List<AdBanner> {
        // 1. Determine user's A/B bucket ("A" or "B")
        val userVariant = if ((userId.hashCode() % 2) == 0) "A" else "B"

        // 2. Deduplicate campaigns — keep only the matching variant per campaignId
        val deduplicated = deduplicateCampaigns(ads, userVariant)

        // 3. Remove frequency-capped ads
        val visible = deduplicated.filter { !AdFrequencyStore.isCapped(it.id) }

        // 4. Score & sort
        return visible
            .map { ad -> ad to score(ad, userDistrict, userListingType) }
            .sortedWith(compareByDescending<Pair<AdBanner, Int>> { it.second }
                .thenBy { it.first.order })
            .map { (ad, _) -> ad }
    }

    /**
     * Returns a label explaining why an ad was surfaced, suitable for the
     * "Recommended for you" tag in AdDetailDialog.
     */
    fun retargetingLabel(
        ad:              AdBanner,
        userDistrict:    String,
        userListingType: String,
    ): String? {
        val reasons = mutableListOf<String>()
        if (ad.targetDistricts.any { it.equals(userDistrict, ignoreCase = true) })
            reasons += "listings in $userDistrict"
        if (ad.targetListingTypes.any { it.equals(userListingType, ignoreCase = true) })
            reasons += userListingType.replace("_", " ") + " search"
        if (reasons.isEmpty()) return null
        return "Based on your ${reasons.joinToString(" & ")}"
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun score(
        ad:              AdBanner,
        userDistrict:    String,
        userListingType: String,
    ): Int {
        var pts = 0

        // District targeting match
        if (ad.targetDistricts.isNotEmpty() &&
            ad.targetDistricts.any { it.equals(userDistrict, ignoreCase = true) }
        ) pts += 30

        // Listing-type targeting match
        if (ad.targetListingTypes.isNotEmpty() &&
            ad.targetListingTypes.any { it.equals(userListingType, ignoreCase = true) }
        ) pts += 25

        // Priority/bid boost
        pts += when (ad.priority) {
            2    -> 20   // premium
            1    -> 10   // featured
            else -> 0
        }

        // Richer media
        if (ad.hasVideo) pts += 10

        // Social proof (credibility signal → higher CTR predicted)
        if (ad.interestCount >= 50) pts += 10

        // Already-interested: show less (user has acted, move on to new content)
        if (AdInterestRepository.isInterested(ad.id)) pts -= 5

        // Urgency: bump ads that are expiring soon
        if (ad.hasUrgency) pts += 5

        return pts.coerceAtLeast(0)
    }

    private fun deduplicateCampaigns(
        ads:         List<AdBanner>,
        userVariant: String,
    ): List<AdBanner> {
        val seenCampaigns = mutableSetOf<String>()
        return ads.filter { ad ->
            val cid = ad.campaignId ?: return@filter true   // no campaign → always show
            if (ad.variant != userVariant) return@filter false // wrong variant for this user
            if (seenCampaigns.contains(cid)) return@filter false // duplicate campaign
            seenCampaigns.add(cid)
            true
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Frequency capping store
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Session-scoped frequency cap.
 *
 * Each time an impression is recorded for an ad, the count is incremented here.
 * Once the count reaches [AdBanner.frequencyCap], the ad is filtered out of
 * the personalised feed by [AdPersonalizationEngine.rank].
 *
 * Reset on app restart (in-memory only; no persistence needed for this behaviour).
 */
object AdFrequencyStore {

    private val impressionCounts = mutableMapOf<String, Int>()

    /** Increment impression counter for [adId] by 1. */
    fun recordImpression(adId: String) {
        impressionCounts[adId] = (impressionCounts[adId] ?: 0) + 1
    }

    /** Returns true when this ad has reached its per-session cap. */
    fun isCapped(adId: String, cap: Int = 3): Boolean =
        (impressionCounts[adId] ?: 0) >= cap

    /** Override cap check using the ad's own [AdBanner.frequencyCap] value. */
    fun isCapped(ad: com.realestate.app.data.models.AdBanner): Boolean =
        isCapped(ad.id, ad.frequencyCap)

    /** How many times this ad has been shown this session. */
    fun countFor(adId: String): Int = impressionCounts[adId] ?: 0

    /** Clear all counts (e.g. on logout or explicit reset). */
    fun reset() { impressionCounts.clear() }
}
