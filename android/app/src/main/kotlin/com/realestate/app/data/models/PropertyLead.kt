package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName

/**
 * A buyer's "I'm Interested" lead on a property listing.
 *
 * Mirrors the backend `property_leads` table. The owner/agent sees incoming leads on
 * their listings; the buyer sees their own enquiry history. This is the free-path lead
 * capture — the future paid WhatsApp automation (Phase 2) fires off the stored lead.
 */
data class PropertyLead(
    @SerializedName("id")             val id: String            = "",
    @SerializedName("property_id")    val propertyId: String?   = null,
    @SerializedName("property_ref")   val propertyRef: String?  = null,
    @SerializedName("property_title") val propertyTitle: String? = null,
    @SerializedName("owner_id")       val ownerId: String?      = null,
    @SerializedName("buyer_id")       val buyerId: String?      = null,
    @SerializedName("buyer_name")     val buyerName: String?    = null,
    @SerializedName("buyer_phone")    val buyerPhone: String?   = null,
    @SerializedName("buyer_email")    val buyerEmail: String?   = null,
    // Only populated by the admin "all leads" endpoint (joined from profiles.role) —
    // null on the buyer/owner-scoped endpoints.
    @SerializedName("buyer_role")     val buyerRole: String?    = null,
    @SerializedName("channel")        val channel: String       = "app",
    @SerializedName("message")        val message: String?      = null,
    @SerializedName("status")         val status: String        = "pending",
    @SerializedName("created_at")     val createdAt: String     = "",
    @SerializedName("updated_at")     val updatedAt: String?    = null,
) {
    val statusEnum: LeadStatus get() = LeadStatus.from(status)
}

enum class LeadStatus(val label: String) {
    PENDING("New"),
    CONTACTED("Contacted"),
    VISIT_SCHEDULED("Visit Scheduled"),
    CONVERTED("Converted"),
    CLOSED("Closed"),
    REJECTED("Rejected");

    companion object {
        fun from(key: String?): LeadStatus =
            values().find { it.name.equals(key, ignoreCase = true) } ?: PENDING
    }
}

/** Body for POST /properties/{id}/interest. */
data class PropertyInterestRequest(
    @SerializedName("message") val message: String? = null,
    @SerializedName("channel") val channel: String  = "app",
)

/** Body for PATCH /properties/leads/{id}/status. */
data class LeadStatusUpdateRequest(
    @SerializedName("status") val status: String,
)

/** Body for PATCH /admin/leads/{id} — admin edit of status, message, and/or buyer contact info.
 * All fields optional; only non-null ones are changed server-side. */
data class AdminLeadUpdateRequest(
    @SerializedName("status")      val status: String?      = null,
    @SerializedName("message")     val message: String?     = null,
    @SerializedName("buyer_name")  val buyerName: String?    = null,
    @SerializedName("buyer_phone") val buyerPhone: String?   = null,
    @SerializedName("buyer_email") val buyerEmail: String?   = null,
)
