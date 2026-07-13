package com.realestate.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Event types
// ─────────────────────────────────────────────────────────────────────────────

enum class AdEventType(val key: String, val label: String) {
    IMPRESSION ("impression",  "Impression"),    // banner scrolled into view
    CLICK      ("click",       "Click"),          // banner tapped → dialog opened
    VIDEO_PLAY ("video_play",  "Video Play"),     // user switched to Video tab
    VIDEO_COMPLETE("video_complete","Video Complete"), // ≥80% of video watched
    SHARE      ("share",       "Share"),          // share button tapped
    INTEREST   ("interest",    "Interested"),     // "I'm Interested" confirmed
    INTEREST_REMOVED("interest_removed","Interest Removed"),
    CTA_CLICK  ("cta_click",   "CTA Click"),      // CTA button tapped
    DISMISS    ("dismiss",     "Dismiss"),         // dialog closed without action
}

// ─────────────────────────────────────────────────────────────────────────────
// Event record
// ─────────────────────────────────────────────────────────────────────────────

data class AdEvent(
    val id:          String = UUID.randomUUID().toString(),
    val adId:        String,
    val adTitle:     String,
    val campaignId:  String?,
    val variant:     String,
    val eventType:   AdEventType,
    val userId:      String = "",
    val userDistrict:String = "",
    val sessionId:   String = AdAnalyticsTracker.SESSION_ID,
    /** How many seconds the user spent looking at the detail dialog (for DISMISS events). */
    val dwellSeconds: Int = 0,
    val timestamp:   String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()
    ).format(Date()),
)

// ─────────────────────────────────────────────────────────────────────────────
// Per-ad summary (aggregated from events, rebuilt on every mutation)
// ─────────────────────────────────────────────────────────────────────────────

data class AdAnalyticsSummary(
    val adId:          String,
    val adTitle:       String,
    val campaignId:    String?,
    val impressions:   Int = 0,
    val clicks:        Int = 0,
    val videoPlays:    Int = 0,
    val videoCompletes:Int = 0,
    val shares:        Int = 0,
    val interests:     Int = 0,
    val ctaClicks:     Int = 0,
    val dismissals:    Int = 0,
    val avgDwellSec:   Double = 0.0,
    // Derived KPIs
) {
    /** Click-through Rate: clicks / impressions (0–100 %) */
    val ctr: Double get() = if (impressions == 0) 0.0 else clicks.toDouble() / impressions * 100

    /** Interest Rate: interests / clicks (0–100 %) */
    val interestRate: Double get() = if (clicks == 0) 0.0 else interests.toDouble() / clicks * 100

    /** Video completion rate: completes / plays (0–100 %) */
    val videoCompletionRate: Double
        get() = if (videoPlays == 0) 0.0 else videoCompletes.toDouble() / videoPlays * 100

    /** CTA conversion rate: ctaClicks / clicks (0–100 %) */
    val ctaConversionRate: Double
        get() = if (clicks == 0) 0.0 else ctaClicks.toDouble() / clicks * 100

    fun formatted(value: Double) = String.format("%.1f%%", value)
}

// ─────────────────────────────────────────────────────────────────────────────
// Tracker singleton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Tracks all advertisement interaction events for marketing analytics.
 *
 * Mock mode — events are accumulated in-memory.
 * Production — call [flush] periodically to POST batches to
 *   POST /api/v1/ads/{adId}/analytics
 *
 * Call sites:
 *   AdBannerSection  → track(IMPRESSION, ad) when banner scrolls into view
 *   AdBannerCard     → track(CLICK, ad) on card tap
 *   AdDetailDialog   → track(VIDEO_PLAY / VIDEO_COMPLETE / SHARE / DISMISS)
 *   AdDetailDialog   → track(CTA_CLICK) on CTA button tap
 *   AdInterestRepo   → track(INTEREST / INTEREST_REMOVED) on interest toggle
 */
object AdAnalyticsTracker {

    /** Unique session ID — regenerated each app launch */
    val SESSION_ID: String = UUID.randomUUID().toString().take(12)

    // ── Raw event log ────────────────────────────────────────────────────────
    private val _events = MutableStateFlow<List<AdEvent>>(emptyList())
    val events: StateFlow<List<AdEvent>> = _events.asStateFlow()

    // ── Per-ad summaries (recomputed after each new event) ───────────────────
    private val _summaries = MutableStateFlow<Map<String, AdAnalyticsSummary>>(emptyMap())
    val summaries: StateFlow<Map<String, AdAnalyticsSummary>> = _summaries.asStateFlow()

    // ── Pending (un-flushed) events for batched API upload ───────────────────
    private val _pending = mutableListOf<AdEvent>()

    // ── Impression deduplication — one impression per ad per session ─────────
    private val impressedThisSession = mutableSetOf<String>()

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Record an ad interaction event.
     *
     * @param type         What happened.
     * @param ad           The ad involved.
     * @param userId       Current user id (empty string = anonymous).
     * @param userDistrict User's currently selected district.
     * @param dwellSeconds Seconds the dialog was open (for DISMISS events).
     */
    fun track(
        type:          AdEventType,
        ad:            com.realestate.app.data.models.AdBanner,
        userId:        String = "",
        userDistrict:  String = "",
        dwellSeconds:  Int    = 0,
    ) {
        // Deduplicate impressions — fire only once per ad per session
        if (type == AdEventType.IMPRESSION) {
            if (impressedThisSession.contains(ad.id)) return
            impressedThisSession.add(ad.id)
        }

        val event = AdEvent(
            adId          = ad.id,
            adTitle       = ad.title,
            campaignId    = ad.campaignId,
            variant       = ad.variant,
            eventType     = type,
            userId        = userId,
            userDistrict  = userDistrict,
            dwellSeconds  = dwellSeconds,
        )

        _events.update { it + event }
        _pending.add(event)

        recomputeSummary(ad.id)
    }

    /** Returns the summary for a single ad (or null if no events yet). */
    fun summaryFor(adId: String): AdAnalyticsSummary? = _summaries.value[adId]

    /** Returns live interest count for an ad from the analytics log. */
    fun interestCountFor(adId: String): Int =
        _events.value.count { it.adId == adId && it.eventType == AdEventType.INTEREST } -
        _events.value.count { it.adId == adId && it.eventType == AdEventType.INTEREST_REMOVED }

    /** All summaries, sorted by impressions descending (highest-traffic ads first). */
    fun sortedSummaries(): List<AdAnalyticsSummary> =
        _summaries.value.values.sortedByDescending { it.impressions }

    /**
     * Returns and clears the pending event queue.
     * Call this from a coroutine / WorkManager task to batch-POST to the backend.
     */
    fun flush(): List<AdEvent> {
        val batch = _pending.toList()
        _pending.clear()
        return batch
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun recomputeSummary(adId: String) {
        val adEvents = _events.value.filter { it.adId == adId }
        if (adEvents.isEmpty()) return

        val dismissEvents = adEvents.filter { it.eventType == AdEventType.DISMISS }
        val avgDwell = if (dismissEvents.isEmpty()) 0.0
        else dismissEvents.sumOf { it.dwellSeconds }.toDouble() / dismissEvents.size

        val summary = AdAnalyticsSummary(
            adId           = adId,
            adTitle        = adEvents.first().adTitle,
            campaignId     = adEvents.first().campaignId,
            impressions    = adEvents.count { it.eventType == AdEventType.IMPRESSION },
            clicks         = adEvents.count { it.eventType == AdEventType.CLICK },
            videoPlays     = adEvents.count { it.eventType == AdEventType.VIDEO_PLAY },
            videoCompletes = adEvents.count { it.eventType == AdEventType.VIDEO_COMPLETE },
            shares         = adEvents.count { it.eventType == AdEventType.SHARE },
            interests      = adEvents.count { it.eventType == AdEventType.INTEREST } -
                             adEvents.count { it.eventType == AdEventType.INTEREST_REMOVED },
            ctaClicks      = adEvents.count { it.eventType == AdEventType.CTA_CLICK },
            dismissals     = dismissEvents.size,
            avgDwellSec    = avgDwell,
        )
        _summaries.update { it + (adId to summary) }
    }
}
