package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName

data class Booking(
    val id: String = "",
    @SerializedName("property_id") val propertyId: String = "",
    @SerializedName("buyer_id") val buyerId: String = "",
    @SerializedName("visit_date") val visitDate: String? = null,
    @SerializedName("visit_time") val visitTime: String? = null,
    val status: String = "pending",
    val message: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class BookingCreateRequest(
    @SerializedName("property_id") val propertyId: String,
    @SerializedName("visit_date") val visitDate: String,
    @SerializedName("visit_time") val visitTime: String,
    val message: String? = null,
)

data class BookingStatusUpdate(val status: String)
