package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName

// ─── Service Request enums ────────────────────────────────────────────────────

enum class ServiceCategory(val displayName: String, val key: String) {
    CONSTRUCTION("Construction", "construction"),
    MAINTENANCE("Maintenance",   "maintenance");

    companion object {
        fun from(key: String?) = values().find { it.key == key }
    }
}

enum class RequestStatus(val displayName: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun from(key: String?) = values().find { it.name.lowercase() == key }
    }
}

enum class QuotationStatus(val displayName: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected");

    companion object {
        fun from(key: String?) = values().find { it.name.lowercase() == key }
    }
}

/** How time-sensitive a service request is — drives the Service Feed badge, filter, and "Urgent first" sort. */
enum class RequestUrgency(val displayName: String, val key: String) {
    NORMAL("Normal", "normal"),
    URGENT("Urgent", "urgent"),
    EMERGENCY("Emergency", "emergency");

    companion object {
        fun from(key: String?) = values().find { it.key == key } ?: NORMAL
    }
}

// ─── Service Request data classes ────────────────────────────────────────────

/**
 * A broadcast request posted by a property owner who needs construction or maintenance work.
 * Nearby contractors see this in their "Requests" feed and can send quotations.
 *
 * Examples:
 *   "I purchased a plot in Coimbatore. Need house plan + construction + interior."
 *   "My apartment needs painting — nearby painters receive the request."
 */
data class ServiceRequest(
    @SerializedName("id")              val id: String              = "",
    @SerializedName("user_id")         val userId: String          = "",
    @SerializedName("category")        val category: String        = "",   // "construction" | "maintenance"
    @SerializedName("service_type")    val serviceType: String     = "",
    @SerializedName("title")           val title: String           = "",
    @SerializedName("description")     val description: String?    = null,
    @SerializedName("district")        val district: String        = "",
    @SerializedName("latitude")        val latitude: Double?       = null,
    @SerializedName("longitude")       val longitude: Double?      = null,
    @SerializedName("radius_km")       val radiusKm: Int           = 50,
    @SerializedName("budget_min")      val budgetMin: Double?      = null,
    @SerializedName("budget_max")      val budgetMax: Double?      = null,
    @SerializedName("images")          val images: List<String>    = emptyList(),
    @SerializedName("status")          val status: String          = "open",
    @SerializedName("created_at")      val createdAt: String       = "",
    @SerializedName("quotation_count") val quotationCount: Int     = 0,
    @SerializedName("urgency")         val urgency: String         = "normal",
    @SerializedName("preferred_date")  val preferredDate: String?  = null,
    @SerializedName("contact_phone")   val contactPhone: String?   = null,
) {
    val statusEnum: RequestStatus? get() = RequestStatus.from(status)
    val categoryEnum: ServiceCategory? get() = ServiceCategory.from(category)
    val urgencyEnum: RequestUrgency get() = RequestUrgency.from(urgency)

    val budgetDisplay: String? get() = when {
        budgetMin != null && budgetMax != null ->
            "₹${"%,.0f".format(budgetMin)} – ₹${"%,.0f".format(budgetMax)}"
        budgetMin != null ->
            "From ₹${"%,.0f".format(budgetMin)}"
        budgetMax != null ->
            "Up to ₹${"%,.0f".format(budgetMax)}"
        else -> null
    }
}

/** Create/send a service request. */
data class ServiceRequestCreateRequest(
    @SerializedName("category")     val category: String,
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("title")        val title: String,
    @SerializedName("description")  val description: String?    = null,
    @SerializedName("district")     val district: String,
    @SerializedName("latitude")     val latitude: Double?       = null,
    @SerializedName("longitude")    val longitude: Double?      = null,
    @SerializedName("radius_km")    val radiusKm: Int           = 50,
    @SerializedName("budget_min")   val budgetMin: Double?      = null,
    @SerializedName("budget_max")   val budgetMax: Double?      = null,
    @SerializedName("images")       val images: List<String>    = emptyList(),
    @SerializedName("urgency")        val urgency: String        = "normal",
    @SerializedName("preferred_date") val preferredDate: String? = null,
    @SerializedName("contact_phone")  val contactPhone: String?  = null,
)

// ─── Quotation data classes ───────────────────────────────────────────────────

/** A quotation submitted by a contractor in response to a service request. */
data class Quotation(
    @SerializedName("id")              val id: String          = "",
    @SerializedName("request_id")      val requestId: String   = "",
    @SerializedName("contractor_id")   val contractorId: String = "",
    @SerializedName("property_id")     val propertyId: String? = null,
    @SerializedName("amount")          val amount: Double?     = null,
    @SerializedName("timeline")        val timeline: String?   = null,
    @SerializedName("notes")           val notes: String?      = null,
    @SerializedName("status")          val status: String      = "pending",
    @SerializedName("created_at")      val createdAt: String   = "",
) {
    val statusEnum: QuotationStatus? get() = QuotationStatus.from(status)
    val amountDisplay: String? get() = amount?.let { "₹${"%,.0f".format(it)}" }
}

data class QuotationCreateRequest(
    @SerializedName("request_id")   val requestId: String,
    @SerializedName("property_id")  val propertyId: String?  = null,
    @SerializedName("amount")       val amount: Double?       = null,
    @Seri