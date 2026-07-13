"""
routers/subscriptions.py — /api/v1/subscriptions
GET    /me               — get current subscription status and limits
POST   /upgrade          — mock checkout and upgrade user subscription plan
"""

from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from app.middleware.auth_middleware import get_current_user
from app.services.supabase_client import get_supabase_admin
from app.schemas.subscription import SubscriptionUpgradeRequest, SubscriptionDetails

router = APIRouter()

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
    limits = SUBSCRIPTION_LIMITS.get(tier, SUBSCRIPTION_LIMITS["free"])

    return SubscriptionDetails(
        subscription_tier=tier,
        subscription_expires_at=current_user.get("subscription_expires_at"),
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
