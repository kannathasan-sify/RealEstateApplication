package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Records a user's expressed interest in an advertisement.
 *
 * Flow:
 *  1. User taps "I'm Interested" on an AdDetailDialog.
 *  2. AdInterestStore.save(AdInterest) persists it in-memory (mock) or
 *     POSTs to /api/v1/ads/{adId}/interests (production).
 *  3. Admin / advertiser dashboard lists all interests for re-contact.
 */
data class AdInterest(
    /** Unique local ID for deduplication */
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    // ── Ad context ──────────────────────────────────────────────────────────
    @SerializedName("ad_id")
    val adId: String,

    @SerializedName("ad_title")
    val adTitle: String,

    @SerializedName("advertiser_name")
    val advertiserName: String,

    /** Derived from ad ctaUrl — e.g. "rent", "sale", "holiday_stay" */
    @SerializedName("listing_type")
    val listingType: String = "general",

    // ── User contact details ─────────────────────────────────────────────────
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("user_id_code")
    val userIdCode: String,

    @SerializedName("user_name")
    val userName: String,

    @SerializedName("user_phone")
    val userPhone: String,

    @SerializedName("user_email")
    val userEmail: String,

    /** Optional note the user adds (e.g. "Interested in 2BHK only") */
    @SerializedName("note")
    val note: String? = null,

    // ── Timestamps ────────────────────────────────────────────────────────────
    @SerializedName("created_at")
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        .format(Date()),

    /** follow-up status managed by admin: pending | contacted | converted | closed */
    @SerializedName("status")
    val status: String = "pending",
) {
    /** Human-readable date for UI display — e.g. "18 Apr 2026, 10:30 AM" */
    val displayDate: String
        get() = try {
            val parser    = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            formatter.format(parser.parse(createdAt) ?: Date())
        } catch (e: Exception) { createdAt }

    /** Status label for admin view */
    val statusLabel: String
        get() = when (status) {
            "contacted"  -> "Contacted"
            "converted"  -> "Converted"
            "closed"     -> "Closed"
            else         -> "Pending"
        }

    /** Status colour hex for admin badges */
    val statusColor: String
        get() = when (status) {
            "contacted"  -> "#1565C0"
            "converted"  -> "#2E7D32"
            "closed"     -> "#9E9E9E"
            else         -> "#E65100"
        }
}

/** Request body sent to backend */
data class AdInterestRequest(
    @SerializedName("ad_id")           val adId: String,
    @SerializedName("ad_title")        val adTitle: String,
    @SerializedName("advertiser_name") val advertiserName: String,
    @SerializedName("listing_type")    val listingType: String = "general",
    @SerializedName("note")            val note: String? = null,
)
