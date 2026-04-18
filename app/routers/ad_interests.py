"""
ad_interests.py — Advertisement Interest Registration Router
POST /api/v1/ads/{ad_id}/interests   — user registers interest in an ad
GET  /api/v1/ads/{ad_id}/interests   — admin lists all interests for an ad
GET  /api/v1/ads/my-interests        — user sees their own interest list
DELETE /api/v1/ads/{ad_id}/interests — user withdraws their interest
"""

from datetime import datetime, timezone
from typing import Optional
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from app.middleware.auth_middleware import get_current_user, require_admin
from app.services.supabase_client import get_supabase

router = APIRouter()


# ── Schemas ──────────────────────────────────────────────────────────────────

class AdInterestRequest(BaseModel):
    """Body sent by the Android app when user taps 'I'm Interested'."""
    ad_id:           str = Field(..., description="Advertisement ID (from AdBanner.id)")
    ad_title:        str = Field(..., max_length=120)
    advertiser_name: str = Field(..., max_length=120)
    listing_type:    str = Field("general", max_length=40)
    note:            Optional[str] = Field(None, max_length=500)


class AdInterestResponse(BaseModel):
    id:              str
    ad_id:           str
    ad_title:        str
    advertiser_name: str
    listing_type:    str
    user_id:         str
    user_name:       str
    user_phone:      Optional[str]
    user_email:      str
    note:            Optional[str]
    status:          str
    created_at:      str


class AdInterestStatusUpdate(BaseModel):
    """Admin updates the follow-up status of an interest lead."""
    status: str = Field(..., pattern="^(pending|contacted|converted|closed)$")


# ── Supabase table: ad_interests ─────────────────────────────────────────────
# CREATE TABLE ad_interests (
#   id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
#   ad_id            TEXT NOT NULL,
#   ad_title         TEXT NOT NULL,
#   advertiser_name  TEXT NOT NULL,
#   listing_type     TEXT DEFAULT 'general',
#   user_id          UUID REFERENCES profiles(id),
#   user_name        TEXT,
#   user_phone       TEXT,
#   user_email       TEXT,
#   note             TEXT,
#   status           TEXT DEFAULT 'pending',
#   created_at       TIMESTAMPTZ DEFAULT NOW(),
#   UNIQUE(ad_id, user_id)
# );
# ALTER TABLE ad_interests ENABLE ROW LEVEL SECURITY;
# CREATE POLICY "own_interest" ON ad_interests
#   USING (auth.uid() = user_id);
# CREATE POLICY "admin_all" ON ad_interests
#   USING (EXISTS (
#     SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
#   ));


# ── POST /ads/{ad_id}/interests ───────────────────────────────────────────────

@router.post(
    "/{ad_id}/interests",
    response_model=AdInterestResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Register interest in an advertisement",
)
async def register_interest(
    ad_id:   str,
    body:    AdInterestRequest,
    current_user: dict = Depends(get_current_user),
    supabase      = Depends(get_supabase),
):
    """
    Called when a user taps "I'm Interested" on an AdDetailDialog.
    Stores the lead so the advertiser / admin can follow up.

    - Upserts on (ad_id, user_id) — re-tapping updates the note only.
    - Returns the saved interest record.
    """
    user_id = current_user["id"]

    # Fetch user contact details from profiles
    profile_res = supabase.table("profiles").select(
        "id, full_name, phone, role"
    ).eq("id", user_id).single().execute()
    profile = profile_res.data or {}

    payload = {
        "id":              str(uuid4()),
        "ad_id":           ad_id,
        "ad_title":        body.ad_title,
        "advertiser_name": body.advertiser_name,
        "listing_type":    body.listing_type,
        "user_id":         user_id,
        "user_name":       profile.get("full_name", ""),
        "user_phone":      profile.get("phone", ""),
        "user_email":      current_user.get("email", ""),
        "note":            body.note,
        "status":          "pending",
        "created_at":      datetime.now(timezone.utc).isoformat(),
    }

    # Upsert — conflict on (ad_id, user_id) updates note only
    res = supabase.table("ad_interests").upsert(
        payload,
        on_conflict="ad_id,user_id",
        returning="representation",
    ).execute()

    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to save ad interest.",
        )

    return res.data[0]


# ── GET /ads/my-interests ─────────────────────────────────────────────────────

@router.get(
    "/my-interests",
    response_model=list[AdInterestResponse],
    summary="Get current user's ad interest history",
)
async def my_interests(
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """Returns all ads the current user has expressed interest in, newest first."""
    user_id = current_user["id"]
    res = (
        supabase.table("ad_interests")
        .select("*")
        .eq("user_id", user_id)
        .order("created_at", desc=True)
        .execute()
    )
    return res.data or []


# ── GET /ads/{ad_id}/interests  (admin only) ──────────────────────────────────

@router.get(
    "/{ad_id}/interests",
    response_model=list[AdInterestResponse],
    summary="List all interest leads for an ad (admin)",
)
async def list_interests_for_ad(
    ad_id:        str,
    current_user: dict = Depends(require_admin),
    supabase           = Depends(get_supabase),
):
    """Admin-only: see every user who expressed interest in a specific ad."""
    res = (
        supabase.table("ad_interests")
        .select("*")
        .eq("ad_id", ad_id)
        .order("created_at", desc=True)
        .execute()
    )
    return res.data or []


# ── PATCH /ads/{ad_id}/interests/{interest_id}/status  (admin only) ──────────

@router.patch(
    "/{ad_id}/interests/{interest_id}/status",
    response_model=AdInterestResponse,
    summary="Update follow-up status of an interest lead (admin)",
)
async def update_interest_status(
    ad_id:       str,
    interest_id: str,
    body:        AdInterestStatusUpdate,
    current_user: dict = Depends(require_admin),
    supabase           = Depends(get_supabase),
):
    """Admin marks a lead as contacted / converted / closed."""
    res = (
        supabase.table("ad_interests")
        .update({"status": body.status})
        .eq("id", interest_id)
        .eq("ad_id", ad_id)
        .execute()
    )
    if not res.data:
        raise HTTPException(status_code=404, detail="Interest record not found.")
    return res.data[0]


# ── DELETE /ads/{ad_id}/interests  (own user withdraws interest) ──────────────

@router.delete(
    "/{ad_id}/interests",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Withdraw interest in an ad",
)
async def withdraw_interest(
    ad_id:        str,
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """User un-taps 'Interested' — removes their lead record."""
    user_id = current_user["id"]
    supabase.table("ad_interests").delete().eq("ad_id", ad_id).eq(
        "user_id", user_id
    ).execute()
