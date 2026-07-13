package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName

data class Agency(
    val id: String = "",
    val name: String = "",
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("license_number") val licenseNumber: String? = null,
    @SerializedName("rera_number") val reraNumber: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val city: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
)
