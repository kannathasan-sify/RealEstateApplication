"""
routers/admin.py — /api/v1/admin  (admin-only endpoints)

GET  /properties            — list ALL properties (every approval_status)
PATCH /properties/{id}/approval — approve / reject / re-approve a listing
"""

from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel

from app.schemas.property import PropertyResponse, PropertyListResponse
from app.services.supabase_client import get_supabase_admin
from app.middleware.auth_middleware import get_current_user
from app.routers.properties import PROPERTY_FIELDS

router = APIRouter()


# ── Request / response schemas ────────────────────────────────────────────────

class ApprovalUpdateRequest(BaseModel):
    """Body for PATCH /admin/properties/{id}/approval"""
    action: str                       # "approve" | "reject" | "re_approve"
    rejection_reason: Optional[str] = None   # required when action = "reject"
    proof_note: Optional[str] = None         # required when action = "re_approve"


class ApprovalUpdateResponse(BaseModel):
    id: str
    approval_status: str
    status: str
    rejection_reason: Optional[str] = None
    message: str


# ── GET /admin/properties — all listings, all statuses ───────────────────────

@router.get("/properties", response_model=PropertyListResponse)
async def admin_list_properties(
    approval_status: Optional[str] = None,   # "pending" | "approved" | "rejected" | None=all
    page:  int = Query(1, ge=1),
    limit: int = Query(50, ge=1, le=200),
    current_user: dict = Depends(get_current_user),
):
    """Return ALL property listings — including pending and rejected ones.
    Only admins may call this endpoint.
    Optionally filter by approval_status (pending | approved | rejected).
    """
    if current_user.get("role") != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required",
        )

    admin = get_supabase_admin()

    query = (
        admin.table("properties")
        .select(PROPERTY_FIELDS, count="exact")
        .order("created_at", desc=True)
    )

    if approval_status:
        query = query.eq("approval_status", approval_status.lower())

    offset = (page - 1) * limit
    result = query.range(offset, offset + limit - 1).execute()

    total = result.count or 0
    data  = [PropertyResponse(**p) for p in (result.data or [])]
    return PropertyListResponse(
        data=data, total=total, page=page, limit=limit,
        has_next=(offset + limit) < total,
    )


# ── PATCH /admin/properties/{id}/approval — approve / reject ─────────────────

@router.patch("/properties/{property_id}/approval", response_model=ApprovalUpdateResponse)
async def update_approval_status(
    property_id: str,
    body: ApprovalUpdateRequest,
    current_user: dict = Depends(get_current_user),
):
    """Approve, reject, or re-approve a property listing.

    - action = "approve"    → approval_status=approved, status=active
    - action = "reject"     → approval_status=rejected, status=inactive, rejection_reason required
    - action = "re_approve" → approval_status=approved, status=active, proof_note required
    """
    if current_user.get("role") != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required",
        )

    admin_client = get_supabase_admin()

    # Verify property exists
    existing = (
        admin_client.table("properties")
        .select("id,approval_status")
        .eq("id", property_id)
        .maybe_single()
        .execute()
    )
    if not existing.data:
        raise HTTPException(status_code=404, detail="Property not found")

    action = body.action.lower()

    if action == "approve":
        update_data = {
            "approval_status": "approved",
            "status": "active",
            "rejection_reason": None,
            "updated_at": datetime.utcnow().isoformat(),
        }
        message = "Property approved and is now live."

    elif action == "reject":
        if not body.rejection_reason or not body.rejection_reason.strip():
            raise HTTPException(
                status_code=400,
                detail="rejection_reason is required when action is 'reject'",
            )
        update_data = {
            "approval_status": "rejected",
            "status": "inactive",
            "rejection_reason": body.rejection_reason.strip(),
            "updated_at": datetime.utcnow().isoformat(),
        }
        message = "Property rejected. Agent will be notified."

    elif action == "re_approve":
        if not body.proof_note or not body.proof_note.strip():
            raise HTTPException(
                status_code=400,
                detail="proof_note is required when action is 're_approve'",
            )
        update_data = {
            "approval_status": "approved",
            "status": "active",
            "rejection_reason": None,
            "updated_at": datetime.utcnow().isoformat(),
        }
        message = f"Property re-approved. Proof: {body.proof_note.strip()}"

    else:
        raise HTTPException(
            status_code=400,
            detail="action must be 'approve', 'reject', or 're_approve'",
        )

    result = (
        admin_client.table("properties")
        .update(update_data)
        .eq("id", property_id)
        .execute()
    )
    row = result.data[0] if result.data else {**existing.data, **update_data}

    return ApprovalUpdateResponse(
        id=property_id,
        approval_status=row.get("approval_status", ""),
        status=row.get("status", ""),
        rejection_reason=row.get("rejection_reason"),
        message=message,
    )
