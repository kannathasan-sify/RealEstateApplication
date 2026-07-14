"""
property_leads.py — Property Buyer-Lead Router

When a buyer taps "I'm Interested" on a property listing, a lead is stored so the
owner/agent has a real enquiry inbox and the buyer keeps an enquiry history. This is
also the trigger point the future paid WhatsApp automation (Phase 2) will hook into.

POST  /api/v1/properties/{property_id}/interest   — buyer registers interest
GET   /api/v1/properties/leads/mine               — buyer's own enquiry history
GET   /api/v1/properties/{property_id}/leads      — owner/agent: leads on this listing
PATCH /api/v1/properties/leads/{lead_id}/status   — owner updates follow-up status
"""

from datetime import datetime, timezone
from typing import Optional
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from app.middleware.auth_middleware import get_current_user
# Use the service-role client: this router enforces owner/buyer authorization in Python
# (buyer_id comes from the JWT, owner checks are explicit), so it must bypass RLS the way
# the auth middleware does. The anon client has no bound auth.uid() and RLS would block it.
from app.services.supabase_client import get_supabase_admin as get_supabase

router = APIRouter()


# ── Schemas ──────────────────────────────────────────────────────────────────

class PropertyInterestRequest(BaseModel):
    """Body sent by the Android app when a buyer taps 'I'm Interested'."""
    message: Optional[str] = Field(None, max_length=500)
    channel: str = Field("app", pattern="^(app|whatsapp|call)$")


class PropertyLeadResponse(BaseModel):
    id:             str
    property_id:    Optional[str]
    property_ref:   Optional[str]
    property_title: Optional[str]
    owner_id:       Optional[str]
    buyer_id:       Optional[str]
    buyer_name:     Optional[str]
    buyer_phone:    Optional[str]
    buyer_email:    Optional[str]
    channel:        str
    message:        Optional[str]
    status:         str
    created_at:     str


class PropertyLeadStatusUpdate(BaseModel):
    """Owner updates the follow-up status of a lead."""
    status: str = Field(..., pattern="^(pending|contacted|visit_scheduled|converted|closed)$")


# ── POST /properties/{property_id}/interest ──────────────────────────────────

@router.post(
    "/properties/{property_id}/interest",
    response_model=PropertyLeadResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Register buyer interest in a property listing",
)
async def register_interest(
    property_id:  str,
    body:         PropertyInterestRequest,
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """
    Creates (or updates the message of) a lead on (property_id, buyer_id).
    The lead's `owner_id` resolves to the property's owner — for agent listings that
    is the agent — so the enquiry reaches whoever is responsible for the listing.
    """
    buyer_id = current_user["id"]

    # Resolve the listing so we can snapshot it + route the lead to its owner.
    prop_res = supabase.table("properties").select(
        "id, owner_id, reference_id, title"
    ).eq("id", property_id).maybe_single().execute()
    prop = prop_res.data
    if not prop:
        raise HTTPException(status_code=404, detail="Property not found.")

    # Buyer's contact details, snapshotted onto the lead for the owner to act on.
    profile_res = supabase.table("profiles").select(
        "id, full_name, phone"
    ).eq("id", buyer_id).single().execute()
    profile = profile_res.data or {}

    payload = {
        "id":             str(uuid4()),
        "property_id":    property_id,
        "property_ref":   prop.get("reference_id"),
        "property_title": prop.get("title"),
        "owner_id":       prop.get("owner_id"),
        "buyer_id":       buyer_id,
        "buyer_name":     profile.get("full_name", ""),
        "buyer_phone":    profile.get("phone", ""),
        "buyer_email":    current_user.get("email", ""),
        "channel":        body.channel,
        "message":        body.message,
        "status":         "pending",
        "created_at":     datetime.now(timezone.utc).isoformat(),
    }

    # Upsert — re-tapping updates the message only, doesn't create duplicate leads.
    res = supabase.table("property_leads").upsert(
        payload,
        on_conflict="property_id,buyer_id",
        returning="representation",
    ).execute()

    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to save property lead.",
        )

    # NOTE (Phase 2): this is where the paid WhatsApp Business API notification to the
    # owner + buyer will be triggered. Left as a no-op for the free path.
    return res.data[0]


# ── GET /properties/leads/mine ───────────────────────────────────────────────

@router.get(
    "/properties/leads/mine",
    response_model=list[PropertyLeadResponse],
    summary="Get the current buyer's enquiry history",
)
async def my_leads(
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """Every property the current user has expressed interest in, newest first."""
    res = (
        supabase.table("property_leads")
        .select("*")
        .eq("buyer_id", current_user["id"])
        .order("created_at", desc=True)
        .execute()
    )
    return res.data or []


# ── GET /properties/leads/received ───────────────────────────────────────────

@router.get(
    "/properties/leads/received",
    response_model=list[PropertyLeadResponse],
    summary="All leads across the current owner/agent's listings",
)
async def received_leads(
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """Aggregate inbox: every lead on any property the current user owns, newest first."""
    res = (
        supabase.table("property_leads")
        .select("*")
        .eq("owner_id", current_user["id"])
        .order("created_at", desc=True)
        .execute()
    )
    return res.data or []


# ── GET /properties/{property_id}/leads ──────────────────────────────────────

@router.get(
    "/properties/{property_id}/leads",
    response_model=list[PropertyLeadResponse],
    summary="List incoming leads on a property (owner/agent only)",
)
async def leads_for_property(
    property_id:  str,
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """
    Owner/agent view of who's interested in their listing. Only the property's owner
    (or an admin) may read the leads.
    """
    prop_res = supabase.table("properties").select("owner_id").eq(
        "id", property_id
    ).single().execute()
    prop = prop_res.data
    if not prop:
        raise HTTPException(status_code=404, detail="Property not found.")

    is_admin = current_user.get("role") == "admin"
    if prop.get("owner_id") != current_user["id"] and not is_admin:
        raise HTTPException(status_code=403, detail="Not your listing.")

    res = (
        supabase.table("property_leads")
        .select("*")
        .eq("property_id", property_id)
        .order("created_at", desc=True)
        .execute()
    )
    return res.data or []


# ── PATCH /properties/leads/{lead_id}/status ─────────────────────────────────

@router.patch(
    "/properties/leads/{lead_id}/status",
    response_model=PropertyLeadResponse,
    summary="Update follow-up status of a lead (owner only)",
)
async def update_lead_status(
    lead_id:      str,
    body:         PropertyLeadStatusUpdate,
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """Owner marks a lead contacted / visit_scheduled / converted / closed."""
    lead_res = supabase.table("property_leads").select(
        "id, owner_id"
    ).eq("id", lead_id).maybe_single().execute()
    lead = lead_res.data
    if not lead:
        raise HTTPException(status_code=404, detail="Lead not found.")

    is_admin = current_user.get("role") == "admin"
    if lead.get("owner_id") != current_user["id"] and not is_admin:
        raise HTTPException(status_code=403, detail="Not your lead.")

    res = (
        supabase.table("property_leads")
        .update({
            "status":     body.status,
            "updated_at": datetime.now(timezone.utc).isoformat(),
        })
        .eq("id", lead_id)
        .execute()
    )
    if not res.data:
        raise HTTPException(status_code=404, detail="Lead not found.")
    return res.data[0]
