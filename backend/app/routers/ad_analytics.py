"""
ad_analytics.py — Advertisement Analytics Router
POST /api/v1/ads/analytics/events          — batch-ingest events from Android app
GET  /api/v1/ads/analytics/summary         — admin: per-ad summary (CTR, interest rate…)
GET  /api/v1/ads/analytics/campaign/{id}   — admin: campaign-level report
GET  /api/v1/ads/analytics/top             — admin: top performing ads
"""

from datetime import datetime, timezone
from typing import Literal, Optional
from uuid import uuid4

from fastapi import APIRouter, Depends, status
from pydantic import BaseModel, Field

from app.middleware.auth_middleware import get_current_user_optional, require_admin
from app.services.supabase_client import get_supabase

router = APIRouter()

# ── Event types (mirrors Android AdEventType enum) ────────────────────────────

AdEventTypeStr = Literal[
    "impression", "click", "video_play", "video_complete",
    "share", "interest", "interest_removed", "cta_click", "dismiss",
]

# ── Schemas ───────────────────────────────────────────────────────────────────

class AdEventPayload(BaseModel):
    ad_id:         str
    ad_title:      str
    campaign_id:   Optional[str]  = None
    variant:       str            = "A"
    event_type:    AdEventTypeStr
    user_district: Optional[str]  = None
    session_id:    Optional[str]  = None
    dwell_seconds: int            = Field(0, ge=0, le=3600)


class AdEventBatchRequest(BaseModel):
    """Android app batches events and POSTs them together to reduce API calls."""
    events: list[AdEventPayload] = Field(..., max_items=200)


class AdSummaryResponse(BaseModel):
    ad_id:                str
    ad_title:             str
    campaign_id:          Optional[str]
    impressions:          int
    clicks:               int
    video_plays:          int
    video_completes:      int
    shares:               int
    interests:            int
    cta_clicks:           int
    dismissals:           int
    avg_dwell_seconds:    float
    ctr_pct:              float   # click-through rate (%)
    interest_rate_pct:    float   # interest / click (%)
    video_completion_pct: float   # video_complete / video_play (%)
    cta_conversion_pct:   float   # cta_click / click (%)


# ── POST /ads/analytics/events — batch ingest ─────────────────────────────────

@router.post(
    "/events",
    status_code=status.HTTP_201_CREATED,
    summary="Batch-upload ad analytics events from mobile app",
)
async def ingest_events(
    body:         AdEventBatchRequest,
    current_user: Optional[dict] = Depends(get_current_user_optional),
    supabase      = Depends(get_supabase),
):
    """
    Called by the Android app (AdAnalyticsTracker.flush()) after accumulating events.
    Inserts all events in one batch for efficiency.
    Accepts both authenticated and anonymous sessions.
    """
    now = datetime.now(timezone.utc).isoformat()
    user_id = current_user["id"] if current_user else None

    rows = [
        {
            "id":            str(uuid4()),
            "ad_id":         ev.ad_id,
            "ad_title":      ev.ad_title,
            "campaign_id":   ev.campaign_id,
            "variant":       ev.variant,
            "event_type":    ev.event_type,
            "user_id":       user_id,
            "user_district": ev.user_district,
            "session_id":    ev.session_id,
            "dwell_seconds": ev.dwell_seconds,
            "created_at":    now,
        }
        for ev in body.events
    ]

    supabase.table("ad_analytics").insert(rows).execute()
    return {"inserted": len(rows)}


# ── GET /ads/analytics/summary — per-ad report (admin) ────────────────────────

@router.get(
    "/summary",
    response_model=list[AdSummaryResponse],
    summary="Per-ad analytics summary (admin only)",
)
async def analytics_summary(
    current_user: dict = Depends(require_admin),
    supabase           = Depends(get_supabase),
):
    """Returns aggregated metrics for every ad, ordered by impressions descending."""
    res = supabase.table("ad_analytics").select("*").execute()
    events = res.data or []

    # Group by ad_id and compute metrics in Python
    from collections import defaultdict
    groups: dict[str, list[dict]] = defaultdict(list)
    for ev in events:
        groups[ev["ad_id"]].append(ev)

    summaries = []
    for ad_id, evs in groups.items():
        def count(t: str) -> int:
            return sum(1 for e in evs if e["event_type"] == t)

        impressions    = count("impression")
        clicks         = count("click")
        video_plays    = count("video_play")
        video_completes= count("video_complete")
        shares         = count("share")
        interests      = count("interest") - count("interest_removed")
        cta_clicks     = count("cta_click")
        dismissals     = count("dismiss")

        dwell_evs = [e["dwell_seconds"] for e in evs if e["event_type"] == "dismiss"]
        avg_dwell  = sum(dwell_evs) / len(dwell_evs) if dwell_evs else 0.0

        def pct(num, denom) -> float:
            return round(num / denom * 100, 2) if denom > 0 else 0.0

        summaries.append(AdSummaryResponse(
            ad_id                = ad_id,
            ad_title             = evs[0]["ad_title"],
            campaign_id          = evs[0].get("campaign_id"),
            impressions          = impressions,
            clicks               = clicks,
            video_plays          = video_plays,
            video_completes      = video_completes,
            shares               = shares,
            interests            = max(0, interests),
            cta_clicks           = cta_clicks,
            dismissals           = dismissals,
            avg_dwell_seconds    = round(avg_dwell, 1),
            ctr_pct              = pct(clicks, impressions),
            interest_rate_pct    = pct(interests, clicks),
            video_completion_pct = pct(video_completes, video_plays),
            cta_conversion_pct   = pct(cta_clicks, clicks),
        ))

    summaries.sort(key=lambda s: s.impressions, reverse=True)
    return summaries


# ── GET /ads/analytics/campaign/{campaign_id} (admin) ─────────────────────────

@router.get(
    "/campaign/{campaign_id}",
    summary="Campaign-level analytics — includes A/B variant breakdown (admin)",
)
async def campaign_analytics(
    campaign_id:  str,
    current_user: dict = Depends(require_admin),
    supabase           = Depends(get_supabase),
):
    """
    Returns per-variant event counts for a campaign.
    Use this to decide which A/B variant to promote.
    """
    res = (
        supabase.table("ad_analytics")
        .select("*")
        .eq("campaign_id", campaign_id)
        .execute()
    )
    events = res.data or []
    if not events:
        return {"campaign_id": campaign_id, "variants": {}}

    from collections import defaultdict
    by_variant: dict[str, list[dict]] = defaultdict(list)
    for ev in events:
        by_variant[ev.get("variant", "A")].append(ev)

    result = {}
    for variant, evs in by_variant.items():
        def count(t: str) -> int:
            return sum(1 for e in evs if e["event_type"] == t)
        imp = count("impression")
        clk = count("click")
        result[variant] = {
            "impressions":  imp,
            "clicks":       clk,
            "interests":    count("interest"),
            "cta_clicks":   count("cta_click"),
            "shares":       count("share"),
            "ctr_pct":      round(clk / imp * 100, 2) if imp else 0.0,
        }

    return {"campaign_id": campaign_id, "variants": result}


# ── GET /ads/analytics/top — top 10 ads by CTR (admin) ───────────────────────

@router.get(
    "/top",
    summary="Top 10 highest-CTR ads (admin)",
)
async def top_ads(
    current_user: dict = Depends(require_admin),
    supabase           = Depends(get_supabase),
):
    """Convenience endpoint for the admin dashboard 'Top Performing Ads' widget."""
    # Re-use summary and return top 10 by CTR
    summaries = await analytics_summary(current_user=current_user, supabase=supabase)
    top = sorted(summaries, key=lambda s: s.ctr_pct, reverse=True)[:10]
    return top
