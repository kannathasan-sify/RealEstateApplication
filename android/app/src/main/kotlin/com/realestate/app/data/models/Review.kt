package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName

data class Review(
    val id: String = "",
    @SerializedName("property_id") val propertyId: String = "",
    @SerializedName("reviewer_id") val reviewerId: String = "",
    val rating: Int = 5,
    val comment: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class ReviewCreateRequest(
    @SerializedName("property_id") val propertyId: String,
    val rating: Int,
    val comment: String? = null,
)
