package com.realestate.app.data.models

/**
 * A ranked advertisement for the home feed, as returned by the server-side Ad Ranking Engine
 * (`GET /ads/home`). Unlike the legacy [AdBanner] (client-ranked mock banners), this carries the
 * engine's decision: AI score, priority tier, sponsored status, ranking reason and the CTA.
 */
data class HomeAd(
    val adId: String,
    val campaignId: String?,
    val advertiserName: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val adType: String?,
    val category: String?,
    val priorityLevel: Int,          // 1 = highest opportunity … 5 = general
    val sponsoredStatus: SponsoredStatus,
    val aiScore: Double,             // 0..100
    val bidAmount: Double,
    val estCtr: Double,              // 0..1
    val estConversion: Double,       // 0..1
    val revenuePrediction: Double,
    val rankingReason: String,       // "why you're seeing this"
    val displayPosition: Int,
    val cta: AdCta,
    val ctaTarget: String?,
) {
    /** Human label for the priority tier (matches the engine's 5-level model). */
    val priorityLabel: String
        get() = when (priorityLevel) {
            1 -> "Top Opportunity"
            2 -> "Financial & Legal"
            3 -> "Construction"
            4 -> "Utilities"
            else -> "General"
        }
}

/** Call-to-action shown on the ad card. Backend sends the [key]; UI shows the [label]. */
enum class AdCta(val label: String, val key: String) {
    VIEW_PROPERTY("View Property", "view_property"),
    CALL_OWNER("Call Owner", "call_owner"),
    BOOK_SITE_VISIT("Book Site Visit", "book_site_visit"),
    APPLY_HOME_LOAN("Apply Home Loan", "apply_home_loan"),
    VISIT_BUILDER("Visit Builder", "visit_builder"),
    GET_LEGAL_VERIFICATION("Get Legal Verification", "get_legal_verification");

    companion object {
        fun from(key: String?): AdCta = values().firstOrNull { it.key == key } ?: VIEW_PROPERTY
    }
}

/** Paid-status label. Paid ads MUST be clearly labelled per the sponsored-ad policy. */
enum class SponsoredStatus(val label: String, val key: String) {
    ORGANIC("", "organic"),
    SPONSORED("Sponsored", "sponsored"),
    FEATURED("Featured", "featured"),
    PROMOTED("Promoted", "promoted");

    /** True for any paid placement (must show the label). */
    val isPaid: Boolean get() = this != ORGANIC

    companion object {
        fun from(key: String?): SponsoredStatus =
            values().firstOrNull { it.key.equals(key, ignoreCase = true) } ?: ORGANIC
    }
}
