package com.realestate.app.data.mock

import com.realestate.app.data.models.AdCta
import com.realestate.app.data.models.HomeAd
import com.realestate.app.data.models.SponsoredStatus

/**
 * Sample ranked home ads for [com.realestate.app.BuildConfig.USE_MOCK_DATA] mode, shaped like the
 * `GET /ads/home` output so the redesigned home ad feed looks realistic offline: a priority mix,
 * sponsored + organic, distinct CTAs and ranking reasons. Ordered as the engine would return them.
 */
object AdMockData {
    val homeAds: List<HomeAd> = listOf(
        HomeAd(
            adId = "ad-100", campaignId = "camp-luxury", advertiserName = "Prestige Estates",
            title = "Sea-View Villas — ECR", subtitle = "3–5 BHK · Ready to move",
            imageUrl = "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800",
            adType = "property", category = "villa", priorityLevel = 1,
            sponsoredStatus = SponsoredStatus.FEATURED, aiScore = 92.4, bidAmount = 320.0,
            estCtr = 0.08, estConversion = 0.03, revenuePrediction = 25.6,
            rankingReason = "Near Chennai", displayPosition = 0,
            cta = AdCta.BOOK_SITE_VISIT, ctaTarget = "mock-001",
        ),
        HomeAd(
            adId = "ad-101", campaignId = null, advertiserName = "Owner Listing",
            title = "Urgent Sale — 2BHK OMR", subtitle = "Price dropped ₹6L this week",
            imageUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800",
            adType = "property", category = "apartment", priorityLevel = 1,
            sponsoredStatus = SponsoredStatus.ORGANIC, aiScore = 88.1, bidAmount = 0.0,
            estCtr = 0.06, estConversion = 0.02, revenuePrediction = 0.0,
            rankingReason = "Price drop", displayPosition = 1,
            cta = AdCta.VIEW_PROPERTY, ctaTarget = "mock-002",
        ),
        HomeAd(
            adId = "ad-102", campaignId = "camp-hdfc", advertiserName = "HDFC Home Loans",
            title = "Home Loans @ 8.35%", subtitle = "Instant eligibility · 0 processing fee",
            imageUrl = "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?w=800",
            adType = "financial", category = "home_loan", priorityLevel = 2,
            sponsoredStatus = SponsoredStatus.SPONSORED, aiScore = 79.5, bidAmount = 210.0,
            estCtr = 0.05, estConversion = 0.04, revenuePrediction = 10.5,
            rankingReason = "Verified advertiser", displayPosition = 2,
            cta = AdCta.APPLY_HOME_LOAN, ctaTarget = "https://hdfc.example/apply",
        ),
        HomeAd(
            adId = "ad-103", campaignId = null, advertiserName = "TN Legal Associates",
            title = "Property Verification & EC", subtitle = "Title check · Registration support",
            imageUrl = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=800",
            adType = "legal", category = "property_verification", priorityLevel = 2,
            sponsoredStatus = SponsoredStatus.ORGANIC, aiScore = 71.2, bidAmount = 0.0,
            estCtr = 0.04, estConversion = 0.02, revenuePrediction = 0.0,
            rankingReason = "Recommended for you", displayPosition = 3,
            cta = AdCta.GET_LEGAL_VERIFICATION, ctaTarget = "9198xxxxxx",
        ),
        HomeAd(
            adId = "ad-104", campaignId = "camp-skyline", advertiserName = "Skyline Developers",
            title = "Skyline Towers — New Launch", subtitle = "Pre-launch pricing · Coimbatore",
            imageUrl = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800",
            adType = "construction", category = "builder", priorityLevel = 3,
            sponsoredStatus = SponsoredStatus.PROMOTED, aiScore = 66.8, bidAmount = 180.0,
            estCtr = 0.05, estConversion = 0.02, revenuePrediction = 9.0,
            rankingReason = "Top opportunity", displayPosition = 4,
            cta = AdCta.VISIT_BUILDER, ctaTarget = "https://skyline.example",
        ),
        HomeAd(
            adId = "ad-105", campaignId = null, advertiserName = "Anna Nagar Owner",
            title = "3BHK Flat — Call Owner", subtitle = "No brokerage · Immediate",
            imageUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800",
            adType = "property", category = "apartment", priorityLevel = 1,
            sponsoredStatus = SponsoredStatus.ORGANIC, aiScore = 63.0, bidAmount = 0.0,
            estCtr = 0.04, estConversion = 0.01, revenuePrediction = 0.0,
            rankingReason = "Matches what you browse", displayPosition = 5,
            cta = AdCta.CALL_OWNER, ctaTarget = "9199xxxxxx",
        ),
        HomeAd(
            adId = "ad-106", campaignId = "camp-casa", advertiserName = "Casagrand Builder",
            title = "Casagrand Elite — Villas", subtitle = "Gated community · Chennai OMR",
            imageUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
            adType = "construction", category = "builder", priorityLevel = 3,
            sponsoredStatus = SponsoredStatus.FEATURED, aiScore = 74.2, bidAmount = 240.0,
            estCtr = 0.06, estConversion = 0.03, revenuePrediction = 14.4,
            rankingReason = "Near Chennai", displayPosition = 6,
            cta = AdCta.VISIT_BUILDER, ctaTarget = "https://casagrand.example",
        ),
        HomeAd(
            adId = "ad-107", campaignId = null, advertiserName = "DAC Developers",
            title = "DAC Smart Homes", subtitle = "Solar-ready · Coimbatore",
            imageUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800",
            adType = "construction", category = "builder", priorityLevel = 3,
            sponsoredStatus = SponsoredStatus.ORGANIC, aiScore = 61.5, bidAmount = 0.0,
            estCtr = 0.04, estConversion = 0.02, revenuePrediction = 0.0,
            rankingReason = "New launch", displayPosition = 7,
            cta = AdCta.VISIT_BUILDER, ctaTarget = "https://dac.example",
        ),
    )
}
