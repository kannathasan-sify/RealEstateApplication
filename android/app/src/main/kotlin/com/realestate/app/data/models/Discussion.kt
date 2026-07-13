package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName

data class Reply(
    @SerializedName("id") val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("user_name") val userName: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

data class Discussion(
    @SerializedName("id") val id: String = "",
    @SerializedName("property_id") val propertyId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("user_name") val userName: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("replies") val replies: List<Reply> = emptyList()
)

data class DiscussionCreateRequest(
    @SerializedName("message") val message: String,
    @SerializedName("parent_id") val parentId: String? = null
)
