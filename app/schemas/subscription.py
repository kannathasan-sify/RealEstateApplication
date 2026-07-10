"""
schemas/subscription.py — Subscription Pydantic schemas
"""

from typing import Optional
from pydantic import BaseModel


class SubscriptionUpgradeRequest(BaseModel):
    tier: str  # "free", "silver", "gold", "platinum", "contractor"


class SubscriptionDetails(BaseModel):
    subscription_tier: str
    subscription_expires_at: Optional[str] = None
    max_listings: int
    max_images: int
    video_enabled: bool
    featured_enabled: bool
    current_listings_count: int
