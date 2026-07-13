package com.realestate.app.data.models

/**
 * Advertisement banner model — enriched with a full suite of marketing fields.
 *
 * ── Core display ────────────────────────────────────────────────────────────
 * @param id              Unique identifier.
 * @param imageUrl        Primary background / preview image URL.
 * @param title           Bold headline text.
 * @param subtitle        Supporting one-liner on the card.
 * @param description     Longer body shown in the detail dialog.
 * @param accentHex       Hex colour for chips and accent elements (#RRGGBB).
 * @param order           Base display order (overridden by personalised rank).
 * @param videoUrl        Optional MP4 / streamable video URL.
 * @param advertiserName  Brand / company name.
 * @param advertiserLogo  Logo URL (falls back to initial-letter avatar).
 * @param ctaText         CTA button label. Null = no button.
 * @param ctaUrl          Deep-link or web URL for the CTA button.
 *
 * ── Priority / placement ────────────────────────────────────────────────────
 * @param priority        0=standard | 1=featured | 2=premium.
 *                        Premium ads are pinned to position 0 regardless of order.
 * @param isSponsor       When true, shows a "Sponsored" label on the card.
 * @param badge           Short custom badge text: "HOT", "NEW", "TRENDING", "OFFER", etc.
 *
 * ── Targeting ───────────────────────────────────────────────────────────────
 * @param targetListingTypes  Listing types this ad is relevant to — ["rent","sale","holiday_stay"].
 *                            Empty list = show to all users regardless of browsing context.
 * @param targetDistricts     Districts this ad targets — ["Chennai","Coimbatore"]. Empty = all TN.
 * @param targetAudience      Audience label for analytics: "all" | "buyers" | "agents" | "builders".
 *
 * ── Urgency / FOMO ──────────────────────────────────────────────────────────
 * @param expiresAt       ISO-8601 date-time string ("2026-04-30T23:59:59").
 *                        When set, a live countdown is shown on the card.
 * @param isLimitedTime   Shows a "Limited Time" urgency pill even without a hard expiry.
 * @param offerText       Short offer line shown in urgency pill: "Save ₹50K on booking!"
 *
 * ── Social proof ────────────────────────────────────────────────────────────
 * @param interestCount   Number of users who tapped "I'm Interested" (from backend / mock).
 *                        Shown as "X people interested" on the card and in the dialog.
 * @param viewCount       Total impressions served — used internally for analytics display.
 *
 * ── A/B testing ─────────────────────────────────────────────────────────────
 * @param variant         "A" | "B" | "C" — which creative variant this banner belongs to.
 *                        Analytics are split per variant to measure performance.
 *
 * ── Re-targeting ────────────────────────────────────────────────────────────
 * @param retargetingLabel Label shown in the dialog when the ad was surfaced via
 *                         personalisation: e.g. "Recommended based on your search".
 *
 * ── Campaign ────────────────────────────────────────────────────────────────
 * @param campaignId      Groups multiple banners into one advertising campaign.
 * @param campaignName    Human-readable campaign label for admin analytics.
 * @param frequencyCap    Max times a single user sees this ad per session (default 3).
 *                        AdFrequencyStore uses this value to suppress over-served ads.
 */
data class AdBanner(
    // ── Core display ────────────────────────────────────────────────────────
    val id:             String,
    val imageUrl:       String,
    val title:          String,
    val subtitle:       String,
    val description:    String?  = null,
    val accentHex:      String   = "#1565C0",
    val order:          Int      = 0,
    val videoUrl:       String?  = null,
    val advertiserName: String   = "NestX",
    val advertiserLogo: String?  = null,
    val ctaText:        String?  = null,
    val ctaUrl:         String?  = null,

    // ── Priority / placement ─────────────────────────────────────────────
    val priority:    Int     = 0,
    val isSponsor:   Boolean = false,
    val badge:       String? = null,

    // ── Targeting ────────────────────────────────────────────────────────
    val targetListingTypes: List<String> = emptyList(),
    val targetDistricts:    List<String> = emptyList(),
    val targetAudience:     String       = "all",

    // ── Urgency / FOMO ───────────────────────────────────────────────────
    val expiresAt:      String?  = null,
    val isLimitedTime:  Boolean  = false,
    val offerText:      String?  = null,

    // ── Social proof ─────────────────────────────────────────────────────
    val interestCount:  Int = 0,
    val viewCount:      Int = 0,

    // ── A/B testing ──────────────────────────────────────────────────────
    val variant:        String  = "A",

    // ── Re-targeting ─────────────────────────────────────────────────────
    val retargetingLabel: String? = null,

    // ── Campaign ─────────────────────────────────────────────────────────
    val campaignId:    String? = null,
    val campaignName:  String? = null,
    val frequencyCap:  Int     = 3,
) {
    /** True only when both the CTA label and a URL are provided. */
    val hasValidCta: Boolean
        get() = !ctaText.isNullOrBlank() && !ctaUrl.isNullOrBlank()

    /** True when a non-blank video URL is provided. */
    val hasVideo: Boolean
        get() = !videoUrl.isNullOrBlank()

    /** True when this ad has a hard expiry date set. */
    val hasExpiry: Boolean
        get() = !expiresAt.isNullOrBlank()

    /** True when any urgency signal is present (expiry or limited-time flag). */
    val hasUrgency: Boolean
        get() = hasExpiry || isLimitedTime || !offerText.isNullOrBlank()

    /** True when social proof count is worth showing (≥ 5). */
    val hasSocialProof: Boolean
        get() = interestCount >= 5

    /** Human-readable social-proof label: "312 interested" / "1.2K interested" */
    val socialProofLabel: String
        get() = when {
            interestCount >= 1_000 -> "${String.format("%.1f", interestCount / 1000.0)}K interested"
            else                   -> "$interestCount interested"
        }

    /** True when the ad targets specific listing types (not universal). */
    val hasTargeting: Boolean
        get() = targetListingTypes.isNotEmpty() || targetDistricts.isNotEmpty()

    /**
     * Returns true if this ad is relevant to [userListingType] and [userDistrict].
     * An empty target list means "show to everyone" (wildcard).
     */
    fun isRelevantTo(userListingType: String, userDistrict: String): Boolean {
        val typeMatch = targetListingTypes.isEmpty() ||
            targetListingTypes.any { it.equals(userListingType, ignoreCase = true) }
        val districtMatch = targetDistricts.isEmpty() ||
            targetDistricts.any { it.equals(userDistrict, ignoreCase = true) }
        return typeMatch && districtMatch
    }

    /** Priority label string for admin display. */
    val priorityLabel: String
        get() = when (priority) {
            2    -> "Premium"
            1    -> "Featured"
            else -> "Standard"
        }
}
