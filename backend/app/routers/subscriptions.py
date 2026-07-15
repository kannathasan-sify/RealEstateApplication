"""
routers/subscriptions.py — /api/v1/subscriptions
GET    /me               — get current subscription status and limits
POST   /upgrade          — mock checkout and upgrade user subscription plan

Validation rules (added 2026-07-15):
  1. A user cannot "upgrade" to the tier they're already actively subscribed to
     (blocks accidentally re-buying / re-charging for the same active plan).
  2. Once a paid plan's `subscription_expires_at` has passed, the user is lazily
     auto-downgraded to the minimum ("free") plan the next time GET /me runs —
     there's no cron job, so this is checked on read.
  3. A user cannot downgrade to the minimum ("free") plan while their current
     paid plan is still active (not yet expired) — they must wait for it to
     lapse (which auto-downgrades them per rule 2) before dropping to Free.
"""

from datetime import datetime, timedelta
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, status
from app.middleware.auth_middleware import get_current_user
from app.services.supabase_client import get_supabase_admin
from app.schemas.subscription import SubscriptionUpgradeRequest, SubscriptionDetails

router = APIRouter()

MIN_TIER = "free"  # the minimum/default plan every expired or new user maps back to


def _is_expired(expires_at: Optional[str]) -> bool:
    """True if `expires_at` (an ISO datetime string, as written by this router via
    `datetime.utcnow().isoformat()`) is in the past. None/unparseable => not expired
    (e.g. the free plan has no expiry)."""
    if not expires_at:
        return False
    try:
        exp = datetime.fromisoformat(expires_at.replace("Z", "+00:00"))
        now = datetime.now(exp.tzinfo) if exp.tzinfo else datetime.utcnow()
        return exp < now
    except ValueError:
        return False

# Subscription Tiers & Capabilities config
# Free: max 3 properties, standard 10 images
# Silver: max 10 properties, 20 images, video upload
# Gold: unlimited properties, featured listings
# Platinum: unlimited properties, featured listings, lead management
# Contractor: unlimited properties (portfolio), business profile, priority search, customer reviews
SUBSCRIPTION_LIMITS = {
    "free": {
        "max_listings": 3,
        "max_images": 10,
        "video_enabled": False,
        "featured_enabled": False,
    },
    "silver": {
        "max_listings": 10,
        "max_images": 20,
        "video_enabled": True,
        "featured_enabled": False,
    },
    "gold": {
        "max_listings": 99999,
        "max_images": 20,
        "video_enabled": True,
        "featured_enabled": True,
    },
    "platinum": {
        "max_listings": 99999,
        "max_images": 20,
        "video_enabled": True,
        "featured_enabled": True,
    },
    "contractor": {
        "max_listings": 99999,
        "max_images": 20,
        "video_enabled": True,
        "featured_enabled": True,
    },
}


@router.get("/me", response_model=SubscriptionDetails)
async def get_my_subscription(current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()

    # Count current properties of this user
    try:
        properties_res = admin.table("properties").select("id").eq("owner_id", current_user["id"]).execute()
        count = len(properties_res.data) if properties_res.data else 0
    except Exception:
        count = 0

    tier = current_user.get("subscription_tier", "free") or "free"
    expires_at = current_user.get("subscription_expires_at")

    # Rule 2: lazily auto-downgrade to the minimum plan once a paid tier has expired.
    # There's no background job, so this is the single place that enforces it — every
    # GET /me call self-heals a lapsed subscription before returning limits.
    if tier != MIN_TIER and _is_expired(expires_at):
        admin.table("profiles").update({
            "subscription_tier": MIN_TIER,
            "subscription_expires_at": None,
        }).eq("id", current_user["id"]).execute()
        tier = MIN_TIER
        expires_at = None

    limits = SUBSCRIPTION_LIMITS.get(tier, SUBSCRIPTION_LIMITS["free"])

    return SubscriptionDetails(
        subscription_tier=tier,
        subscription_expires_at=expires_at,
        max_listings=limits["max_listings"],
        max_images=limits["max_images"],
        video_enabled=limits["video_enabled"],
        featured_enabled=limits["featured_enabled"],
        current_listings_count=count
    )


@router.post("/upgrade", response_model=dict)
async def upgrade_subscription(body: SubscriptionUpgradeRequest, current_user: dict = Depends(get_current_user)):
    tier = body.tier.lower()
    if tier not in SUBSCRIPTION_LIMITS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid subscription tier name"
        )

    current_tier = current_user.get("subscription_tier", "free") or "free"
    current_expires_at = current_user.get("subscription_expires_at")
    currently_active = current_tier != MIN_TIER and not _is_expired(current_expires_at)

    # Rule 1: block re-buying the plan the user is already actively on.
    if currently_active and tier == current_tier:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                f"You already have an active {current_tier.capitalize()} plan"
                + (f" until {current_expires_at[:10]}." if current_expires_at else ".")
            ),
        )

    # Rule 3: block downgrading to the minimum (Free) plan while a paid plan is
    # still active — it can only be dropped to once it lapses (rule 2 in GET /me
    # will then auto-downgrade it, after which this same request would succeed).
    if currently_active and tier == MIN_TIER:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                f"Your {current_tier.capitalize()} plan is still active"
                + (f" until {current_expires_at[:10]}" if current_expires_at else "")
                + ". You can't downgrade to Free until it expires."
            ),
        )

    admin = get_supabase_admin()
    expiry_date = (datetime.utcnow() + timedelta(days=30)).isoformat()

    # Update profile row with the new subscription tier
    update_res = admin.table("profiles").update({
        "subscription_tier": tier,
        "subscription_expires_at": expiry_date
    }).eq("id", current_user["id"]).execute()

    if not update_res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update subscription in database"
        )

    # Record simulated payment transaction
    prices = {"free": 0, "silver": 299, "gold": 599, "platinum": 999, "contractor": 999}
    amount = prices.get(tier, 0)
    if amount > 0:
        try:
            admin.table("payments").insert({
                "user_id": current_user["id"],
                "amount": amount,
                "tier": tier,
                "status": "success"
            }).execute()
        except Exception as e:
            print(f"Failed to record payment row: {e}")

    return {
        "status": "success",
        "message": f"Successfully upgraded to {tier.capitalize()} plan",
        "subscription_tier": tier,
        "subscription_expires_at": expiry_date
    }
