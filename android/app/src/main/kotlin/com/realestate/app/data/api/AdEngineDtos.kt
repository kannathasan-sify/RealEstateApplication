package com.realestate.app.data.api

import com.google.gson.annotations.SerializedName
import com.realestate.app.data.models.AdCta
import com.realestate.app.data.models.HomeAd
import com.realestate.app.data.models.SponsoredStatus

/** Wire DTOs for the Ad Ranking Engine (GET /ads/home + action endpoints). */

data class RankedAdDto(
    @SerializedName("ad_id")              val adId: String,
    @SerializedName("campaign_id")        val campaignId: String? = null,
    @SerializedName("advertiser_name")    val advertiserName: String = "",
    @SerializedName("title")              val title: String = "",
    @SerializedName("subtitle")           val subtitle: String? = null,
    @SerializedName("image_url")          val imageUrl: String? = null,
    @SerializedName("ad_type")            val adType: String? = null,
    @SerializedName("category")           val category: String? = null,
    @SerializedName("priority_level")     val priorityLevel: Int = 5,
    @SerializedName("sponsored_status")   val sponsoredStatus: String = "organic",
    @SerializedName("ai_score")           val aiScore: Double = 0.0,
    @SerializedName("bid_amount")         val bidAmount: Double = 0.0,
    @SerializedName("est_ctr")            val estCtr: Double = 0.0,
    @SerializedName("est_conversion")     val estConversion: Double = 0.0,
    @SerializedName("revenue_prediction") val revenuePrediction: Double = 0.0,
    @SerializedName("ranking_reason")     val rankingReason: String = "",
    @SerializedName("display_position")   val displayPosition: Int = 0,
    @SerializedName("cta")                val cta: String = "view_property",
    @SerializedName("cta_target")         val ctaTarget: String? = null,
) {
    fun toDomain(): HomeAd = HomeAd(
        adId = adId,
        campaignId = campaignId,
        advertiserName = advertiserName,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        adType = adType,
        category = category,
        priorityLevel = priorityLevel,
        sponsoredStatus = SponsoredStatus.from(sponsoredStatus),
        aiScore = aiScore,
        bidAmount = bidAmount,
        estCtr = estCtr,
        estConversion = estConversion,
        revenuePrediction = revenuePrediction,
        rankingReason = rankingReason,
        displayPosition = displayPosition,
        cta = AdCta.from(cta),
        ctaTarget = ctaTarget,
    )
}

data class AdActionRequestDto(
    @SerializedName("ad_id")      val adId: String,
    @SerializedName("reason")     val reason: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
)
