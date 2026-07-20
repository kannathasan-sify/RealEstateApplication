"""
routers/ads_engine.py — /api/v1/ads  (Advertisement Ranking & Delivery Engine)

Server-side, multi-factor ad ranking for the home feed. The score is a weighted blend of
user relevance, location/property/budget match, predicted CTR/conversion, advertiser quality,
verification, bid, premium, urgency and freshness — MINUS fraud/duplicate/spam penalties.
Ranking NEVER uses bid alone (bid is a 5% factor).

The engine is a rule-based scorer today, with a single seam (`_ml_adjustment`) where a trained
model score can later be blended in without touching the endpoints.

GET  /ads/home           — ranked, sponsored-capped home ad feed
POST /ads/impression     — log an impression
POST /ads/click          — log a click (spends CPC budget)
POST /ads/conversion     — log a conversion
POST /ads/hide           — user hides an ad (excluded from their future feeds)
POST /ads/report         — user reports an ad
"""

from datetime import datetime, timezone
from typing import List, Optional

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.middleware.auth_middleware import get_optional_user
from app.services.supabase_client import get_supabase_admin as get_supabase

router = APIRouter()

# ── Ranking weights (must reflect the product's stated policy) ────────────────
WEIGHTS = {
    "user_relevance":        0.30,
    "location_match":        0.15,
    "property_match":        0.10,
    "budget_match":          0.10,
    "ctr_prediction":        0.05,
    "conversion_prediction": 0.05,
    "advertiser_quality":    0.10,
    "verification":          0.05,
    "bid_amount":            0.05,
    "premium_membership":    0.02,
    "urgency":               0.01,
    "freshness":             0.01,
}
PENALTIES = {"fraud": 0.05, "duplicate": 0.05, "spam": 0.05}

# Home feed may be at most 30% sponsored (rest organic).
SPONSORED_CAP_HOME = 0.30
BID_NORMALISER = 500.0   # a bid of ₹500+ saturates the bid factor


# ── Output contract ───────────────────────────────────────────────────────────
class RankedAd(BaseModel):
    ad_id: str
    campaign_id: Optional[str] = None
    advertiser_name: str = ""
    title: str
    subtitle: Optional[str] = None
    image_url: Optional[str] = None
    ad_type: Optional[str] = None
    category: Optional[str] = None
    priority_level: int = 5
    sponsored_status: str = "organic"     # organic | sponsored | featured | promoted
    ai_score: float = 0.0                 # 0..100
    bid_amount: float = 0.0
    est_ctr: float = 0.0                  # 0..1
    est_conversion: float = 0.0           # 0..1
    revenue_prediction: float = 0.0       # expected ₹ per impression
    ranking_reason: str = ""
    display_position: int = 0
    cta: str = "view_property"
    cta_target: Optional[str] = None


class AdActionRequest(BaseModel):
    ad_id: str
    reason: Optional[str] = None
    session_id: Optional[str] = None


# ── Scoring helpers ───────────────────────────────────────────────────────────

def _clamp01(v: float) -> float:
    return max(0.0, min(1.0, v))


def _parse(ts: Optional[str]) -> Optional[datetime]:
    if not ts:
        return None
    try:
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except Exception:
        return None


def _ml_adjustment(ad: dict, ctx: dict) -> float:
    """Seam for a future ML model: return an additive score in [-1, 1]. 0 = rule-engine only."""
    return 0.0


def _score_ad(ad: dict, ctx: dict) -> tuple[float, str]:
    """Return (ai_score 0..100, ranking_reason) for one ad given the user context `ctx`."""
    campaign = ad.get("ad_campaigns") or {}
    advertiser = ad.get("ad_advertisers") or {}

    # priority 1 (highest) → 1.0, priority 5 → 0.2
    priority = ad.get("priority_level") or 5
    priority_score = _clamp01((6 - priority) / 5.0)

    # location match (district targeting; empty target = neutral)
    districts = ad.get("target_districts") or []
    if not districts:
        location = 0.5
    else:
        location = 1.0 if (ctx.get("district") and ctx["district"] in districts) else 0.15

    # property/listing match
    ltypes = ad.get("target_listing_types") or []
    ptypes = ad.get("target_property_types") or []
    if not ltypes and not ptypes:
        prop = 0.5
    else:
        hit = (ctx.get("listing_type") in ltypes) or (ctx.get("property_type") in ptypes)
        prop = 1.0 if hit else 0.2

    # budget match (overlap of user budget with ad's [min,max])
    bmin, bmax = ad.get("budget_min"), ad.get("budget_max")
    umin, umax = ctx.get("budget_min"), ctx.get("budget_max")
    if bmin is None and bmax is None:
        budget = 0.5
    elif umin is None and umax is None:
        budget = 0.5
    else:
        lo = max(bmin or 0, umin or 0)
        hi = min(bmax or 10 ** 15, umax or 10 ** 15)
        budget = 1.0 if lo <= hi else 0.1

    ctr = _clamp01(float(ad.get("ctr") or 0))
    conv = _clamp01(float(ad.get("conversion_rate") or 0))

    # advertiser quality composite
    rating = _clamp01(float(advertiser.get("rating") or 0) / 5.0)
    lead_rate = _clamp01(float(advertiser.get("lead_success_rate") or 0))
    quality = _clamp01(float(ad.get("quality_score") or 0.5))
    adv_quality = rating * 0.5 + lead_rate * 0.3 + quality * 0.2

    verified = 0.0
    if advertiser.get("is_verified") or ad.get("is_verified"):
        verified += 0.7
    if advertiser.get("government_approved"):
        verified += 0.3
    verified = _clamp01(verified)

    bid = float(campaign.get("bid_amount") or 0)
    bid_score = _clamp01(bid / BID_NORMALISER)

    plan = campaign.get("plan") or "standard"
    premium = {"premium": 1.0, "featured": 0.5}.get(plan, 0.0)

    urgency = 1.0 if (ad.get("is_urgent") or ad.get("has_price_drop")) else 0.0

    # freshness — linear decay over 30 days
    created = _parse(ad.get("created_at"))
    if created:
        age_days = (ctx["now"] - created).days
        freshness = _clamp01(1.0 - age_days / 30.0)
    else:
        freshness = 0.5

    # user relevance (largest factor): opportunity priority + match reinforcement + affinity
    affinity = 0.0
    if advertiser.get("id") and advertiser.get("id") in ctx.get("favorite_advertisers", set()):
        affinity = 0.2
    user_relevance = _clamp01(0.6 * priority_score + 0.2 * max(location, prop) + affinity)

    score = (
        WEIGHTS["user_relevance"] * user_relevance
        + WEIGHTS["location_match"] * location
        + WEIGHTS["property_match"] * prop
        + WEIGHTS["budget_match"] * budget
        + WEIGHTS["ctr_prediction"] * ctr
        + WEIGHTS["conversion_prediction"] * conv
        + WEIGHTS["advertiser_quality"] * adv_quality
        + WEIGHTS["verification"] * verified
        + WEIGHTS["bid_amount"] * bid_score
        + WEIGHTS["premium_membership"] * premium
        + WEIGHTS["urgency"] * urgency
        + WEIGHTS["freshness"] * freshness
    )
    # penalties
    score -= PENALTIES["fraud"] * _clamp01(float(ad.get("fraud_score") or 0))
    score -= PENALTIES["spam"] * _clamp01(float(ad.get("spam_score") or 0))
    score -= PENALTIES["duplicate"] * (1.0 if ad.get("is_duplicate") else 0.0)

    score = _clamp01(score + _ml_adjustment(ad, ctx))

    # ranking reason — the strongest human-explainable signal
    reason = "Recommended for you"
    if ad.get("has_price_drop"):
        reason = "Price drop"
    elif ad.get("is_urgent"):
        reason = "Urgent — act soon"
    elif location >= 1.0:
        reason = f"Near {ctx.get('district')}" if ctx.get("district") else "Near you"
    elif prop >= 1.0:
        reason = "Matches what you browse"
    elif budget >= 1.0 and (bmin is not None or bmax is not None):
        reason = "Within your budget"
    elif verified >= 0.7:
        reason = "Verified advertiser"
    elif priority == 1:
        reason = "Top opportunity"

    return round(score * 100, 1), reason


def _revenue_prediction(ad: dict, est_ctr: float, est_conv: float) -> float:
    campaign = ad.get("ad_campaigns") or {}
    bid = float(campaign.get("bid_amount") or 0)
    model = campaign.get("revenue_model") or "cpc"
    if model == "cpm":
        return round(bid / 1000.0, 2)
    if model == "cpa":
        return round(bid * est_conv, 2)
    if model in ("subscription", "featured"):
        return round(bid, 2)
    # cpc (default): expected value per impression
    return round(bid * est_ctr, 2)


def _is_sponsored(ad: dict) -> bool:
    return (ad.get("sponsored_status") or "organic") != "organic"


# ── GET /ads/home ─────────────────────────────────────────────────────────────

@router.get("/home", response_model=List[RankedAd], summary="Ranked home ad feed")
async def home_ads(
    district: Optional[str] = None,
    listing_type: Optional[str] = None,
    property_type: Optional[str] = None,
    budget_min: Optional[float] = None,
    budget_max: Optional[float] = None,
    lat: Optional[float] = None,
    lng: Optional[float] = None,
    limit: int = Query(10, ge=1, le=30),
    current_user: Optional[dict] = Depends(get_optional_user),
    supabase                     = Depends(get_supabase),
):
    """
    Returns the ranked, sponsored-capped ad feed for the home screen. Anonymous allowed.
    Sorting is by AI score (multi-factor); sponsored ads are capped at 30% and interleaved
    with organic recommendations.
    """
    now = datetime.now(timezone.utc)
    uid = current_user.get("id") if current_user else None

    # Ads the user has hidden — excluded from their feed.
    hidden: set = set()
    if uid:
        h = (supabase.table("ad_user_actions")
             .select("ad_id").eq("user_id", uid).eq("action", "hide").execute().data) or []
        hidden = {row["ad_id"] for row in h}

    rows = (supabase.table("advertisements")
            .select(
                "*, "
                "ad_campaigns(bid_amount, daily_budget, remaining_budget, plan, status, revenue_model), "
                "ad_advertisers(id, name, is_verified, government_approved, rating, "
                "lead_success_rate, years_in_business)"
            )
            .eq("status", "active")
            .execute().data) or []

    ctx = {
        "now": now,
        "district": district,
        "listing_type": listing_type,
        "property_type": property_type,
        "budget_min": budget_min,
        "budget_max": budget_max,
        "lat": lat,
        "lng": lng,
        "favorite_advertisers": set(),
    }

    scored: list[tuple[float, str, dict]] = []
    for ad in rows:
        if ad["id"] in hidden:
            continue
        exp = _parse(ad.get("expires_at"))
        if exp and exp < now:
            continue
        campaign = ad.get("ad_campaigns") or {}
        if campaign:
            if campaign.get("status") not in (None, "active"):
                continue
            # A paid campaign with no budget left stops serving.
            if _is_sponsored(ad) and (campaign.get("remaining_budget") or 0) <= 0 \
                    and (campaign.get("daily_budget") or 0) > 0:
                continue
        score, reason = _score_ad(ad, ctx)
        scored.append((score, reason, ad))

    scored.sort(key=lambda t: t[0], reverse=True)

    # Sponsored cap + interleave: keep highest-scoring, but no more than 30% sponsored.
    max_sponsored = max(1, int(limit * SPONSORED_CAP_HOME))
    out: List[RankedAd] = []
    sponsored_used = 0
    for score, reason, ad in scored:
        if len(out) >= limit:
            break
        if _is_sponsored(ad):
            if sponsored_used >= max_sponsored:
                continue
            sponsored_used += 1
        est_ctr = _clamp01(float(ad.get("ctr") or 0))
        est_conv = _clamp01(float(ad.get("conversion_rate") or 0))
        campaign = ad.get("ad_campaigns") or {}
        advertiser = ad.get("ad_advertisers") or {}
        out.append(RankedAd(
            ad_id=ad["id"],
            campaign_id=ad.get("campaign_id"),
            advertiser_name=advertiser.get("name") or "",
            title=ad.get("title") or "",
            subtitle=ad.get("subtitle"),
            image_url=ad.get("image_url"),
            ad_type=ad.get("ad_type"),
            category=ad.get("category"),
            priority_level=ad.get("priority_level") or 5,
            sponsored_status=ad.get("sponsored_status") or "organic",
            ai_score=score,
            bid_amount=float(campaign.get("bid_amount") or 0),
            est_ctr=est_ctr,
            est_conversion=est_conv,
            revenue_prediction=_revenue_prediction(ad, est_ctr, est_conv),
            ranking_reason=reason,
            display_position=len(out),
            cta=ad.get("cta") or "view_property",
            cta_target=ad.get("cta_target"),
        ))
    return out


# ── Action logging ────────────────────────────────────────────────────────────

def _log_action(supabase, ad_id: str, uid: Optional[str], action: str,
                reason: Optional[str], session_id: Optional[str]):
    try:
        supabase.table("ad_user_actions").insert({
            "ad_id": ad_id, "user_id": uid, "action": action,
            "reason": reason, "session_id": session_id,
        }).execute()
    except Exception:
        pass


@router.post("/impression", status_code=204, summary="Log an ad impression")
async def ad_impression(body: AdActionRequest,
                        current_user: Optional[dict] = Depends(get_optional_user),
                        supabase                     = Depends(get_supabase)):
    uid = current_user.get("id") if current_user else None
    _log_action(supabase, body.ad_id, uid, "impression", None, body.session_id)
    try:
        cur = supabase.table("advertisements").select("impressions_count").eq("id", body.ad_id).maybe_single().execute().data
        if cur is not None:
            supabase.table("advertisements").update(
                {"impressions_count": (cur.get("impressions_count") or 0) + 1}
            ).eq("id", body.ad_id).execute()
    except Exception:
        pass
    return


@router.post("/click", status_code=204, summary="Log an ad click (spends CPC budget)")
async def ad_click(body: AdActionRequest,
                   current_user: Optional[dict] = Depends(get_optional_user),
                   supabase                     = Depends(get_supabase)):
    uid = current_user.get("id") if current_user else None
    _log_action(supabase, body.ad_id, uid, "click", None, body.session_id)
    try:
        ad = (supabase.table("advertisements")
              .select("clicks_count, campaign_id, ad_campaigns(bid_amount, remaining_budget, revenue_model)")
              .eq("id", body.ad_id).maybe_single().execute().data)
        if ad is not None:
            supabase.table("advertisements").update(
                {"clicks_count": (ad.get("clicks_count") or 0) + 1}
            ).eq("id", body.ad_id).execute()
            camp = ad.get("ad_campaigns") or {}
            # CPC: spend one bid from the remaining budget.
            if ad.get("campaign_id") and (camp.get("revenue_model") or "cpc") == "cpc":
                new_budget = max(0.0, float(camp.get("remaining_budget") or 0) - float(camp.get("bid_amount") or 0))
                supabase.table("ad_campaigns").update(
                    {"remaining_budget": new_budget}
                ).eq("id", ad["campaign_id"]).execute()
    except Exception:
        pass
    return


@router.post("/conversion", status_code=204, summary="Log an ad conversion")
async def ad_conversion(body: AdActionRequest,
                        current_user: Optional[dict] = Depends(get_optional_user),
                        supabase                     = Depends(get_supabase)):
    uid = current_user.get("id") if current_user else None
    _log_action(supabase, body.ad_id, uid, "conversion", None, body.session_id)
    try:
        cur = supabase.table("advertisements").select("conversions_count").eq("id", body.ad_id).maybe_single().execute().data
        if cur is not None:
            supabase.table("advertisements").update(
                {"conversions_count": (cur.get("conversions_count") or 0) + 1}
            ).eq("id", body.ad_id).execute()
    except Exception:
        pass
    return


@router.post("/hide", status_code=204, summary="Hide an ad from the user's feed")
async def ad_hide(body: AdActionRequest,
                  current_user: Optional[dict] = Depends(get_optional_user),
                  supabase                     = Depends(get_supabase)):
    uid = current_user.get("id") if current_user else None
    _log_action(supabase, body.ad_id, uid, "hide", body.reason, body.session_id)
    return


@router.post("/report", status_code=204, summary="Report an ad")
async def ad_report(body: AdActionRequest,
                    current_user: Optional[dict] = Depends(get_optional_user),
                    supabase                     = Depends(get_supabase)):
    uid = current_user.get("id") if current_user else None
    _log_action(supabase, body.ad_id, uid, "report", body.reason, body.session_id)
    return
