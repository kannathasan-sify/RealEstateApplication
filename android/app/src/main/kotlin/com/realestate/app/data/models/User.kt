package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName

enum class UserRole(val displayName: String, val badgeColor: String) {
    BUYER("Buyer",   "#9E9E9E"),
    AGENT("Agent",   "#1565C0"),
    BUILDER("Builder","#F57C00"),
    ADMIN("Admin",   "#6A1B9A");

    companion object {
        fun from(value: String?): UserRole = values().firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: BUYER
    }
}

data class User(
    @SerializedName("id")               val id: String = "",
    @SerializedName("full_name")        val fullName: String = "",
    @SerializedName("email")            val email: String = "",
    @SerializedName("phone")            val phone: String = "",
    @SerializedName("avatar_url")       val avatarUrl: String? = null,
    @SerializedName("role")             val roleStr: String = "buyer",
    @SerializedName("user_id_code")     val userIdCode: String = "",
    @SerializedName("is_verified")      val isVerified: Boolean = false,
    @SerializedName("district")         val district: String = "Chennai",
    @SerializedName("city")             val city: String = "Chennai",
    @SerializedName("language")         val language: String = "English",
    @SerializedName("biometric_enabled")val biometricEnabled: Boolean = false,
    @SerializedName("subscription_tier")val subscriptionTier: String = "free",
    @SerializedName("subscription_expires_at")val subscriptionExpiresAt: String? = null,
    @SerializedName("created_at")       val createdAt: String = ""
) {
    val role: UserRole get() = UserRole.from(roleStr)

    val maxListings: Int get() = when (subscriptionTier.lowercase()) {
        "free" -> 3
        "silver" -> 10
        else -> 99999
    }

    val maxImages: Int get() = if (subscriptionTier.lowercase() == "free") 10 else 20
    val videoUploadEnabled: Boolean get() = subscriptionTier.lowercase() != "free"
    val featuredListingsEnabled: Boolean get() = subscriptionTier.lowercase() != "free" && subscriptionTier.lowercase() != "silver"

    /** True if this user is allowed to post property listings */
    val canPostProperty: Boolean
        get() = role == UserRole.AGENT || role == UserRole.BUILDER || role == UserRole.ADMIN

    /** True if this user has admin privileges */
    val isAdmin: Boolean get() = role == UserRole.ADMIN

    /** Display-friendly initials (for avatar placeholder) */
    val initials: String get() {
        val parts = fullName.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "?"
        }
    }

    /** Short display name — first name only */
    val firstName: String get() = fullName.trim().split(" ").firstOrNull()?.ifBlank { fullName } ?: fullName

    /** Formatted join date — e.g. "Joined March 2026" */
    val joinedLabel: String get() = if (createdAt.length >= 7) {
        val month = when (createdAt.substring(5, 7)) {
            "01" -> "January"; "02" -> "February"; "03" -> "March"
            "04" -> "April";   "05" -> "May";       "06" -> "June"
            "07" -> "July";    "08" -> "August";    "09" -> "September"
            "10" -> "October"; "11" -> "November";  "12" -> "December"
            else -> ""
        }
        val year = createdAt.take(4)
        "Joined $month $year"
    } else "New Member"
}

// ── Auth request / response DTOs ──────────────────────────────────────────────

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String,
    val phone: String? = null,
)

data class LoginRequest(
    val email: String? = null,
    val password: String,
    @SerializedName("user_id_code") val userIdCode: String? = null,
)

data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String,
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type")   val tokenType: String = "bearer",
    val user: User,
)

data class ProfileUpdateRequest(
    @SerializedName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val language: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
)

data class RoleUpdateRequest(
    val role: String,
)
