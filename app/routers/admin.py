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


# ── User Management & Verification ───────────────────────────────────────────

class UserVerifyRequest(BaseModel):
    is_verified: bool


class UserRoleRequest(BaseModel):
    role: str


@router.get("/users")
async def list_users(
    role: Optional[str] = None,
    is_verified: Optional[bool] = None,
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
    
    admin_client = get_supabase_admin()
    query = admin_client.table("profiles").select("*")
    
    if role:
        query = query.eq("role", role)
    if is_verified is not None:
        query = query.eq("is_verified", is_verified)
        
    result = query.execute()
    return result.data or []


@router.patch("/users/{user_id}/verify")
async def verify_user(
    user_id: str,
    body: UserVerifyRequest,
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    admin_client = get_supabase_admin()
    result = (
        admin_client.table("profiles")
        .update({"is_verified": body.is_verified, "updated_at": datetime.utcnow().isoformat()})
        .eq("id", user_id)
        .execute()
    )
    if not result.data:
        raise HTTPException(status_code=404, detail="User not found")
    return {"status": "success", "user": result.data[0]}


@router.patch("/users/{user_id}/role")
async def change_user_role(
    user_id: str,
    body: UserRoleRequest,
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    admin_client = get_supabase_admin()
    result = (
        admin_client.table("profiles")
        .update({"role": body.role, "updated_at": datetime.utcnow().isoformat()})
        .eq("id", user_id)
        .execute()
    )
    if not result.data:
        raise HTTPException(status_code=404, detail="User not found")
    return {"status": "success", "user": result.data[0]}


@router.delete("/users/{user_id}")
async def delete_user(
    user_id: str,
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    admin_client = get_supabase_admin()
    try:
        admin_client.auth.admin.delete_user(user_id)
    except Exception:
        pass
    
    result = admin_client.table("profiles").delete().eq("id", user_id).execute()
    return {"status": "success", "message": "User deleted successfully"}


# ── Payment Management ────────────────────────────────────────────────────────

@router.get("/payments")
async def list_payments(
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    admin_client = get_supabase_admin()
    result = (
        admin_client.table("payments")
        .select("*, profiles(full_name, email)")
        .order("created_at", desc=True)
        .execute()
    )
    return result.data or []


# ── Support & Complaints Management ──────────────────────────────────────────

class TicketReplyRequest(BaseModel):
    reply: str
    status: str = "resolved"


@router.get("/tickets")
async def list_tickets(
    status: Optional[str] = None,
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    admin_client = get_supabase_admin()
    query = admin_client.table("support_tickets").select("*, profiles(full_name, email)")
    if status:
        query = query.eq("status", status)
    
    result = query.order("created_at", desc=True).execute()
    return result.data or []


@router.post("/tickets/{ticket_id}/reply")
async def reply_ticket(
    ticket_id: str,
    body: TicketReplyRequest,
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    admin_client = get_supabase_admin()
    result = (
        admin_client.table("support_tickets")
        .update({"reply": body.reply, "status": body.status})
        .eq("id", ticket_id)
        .execute()
    )
    if not result.data:
        raise HTTPException(status_code=404, detail="Ticket not found")
    return {"status": "success", "ticket": result.data[0]}


# ── System Stats / View Reports ───────────────────────────────────────────────

@router.get("/reports/stats")
async def get_system_stats(
    current_user: dict = Depends(get_current_user),
):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    admin_client = get_supabase_admin()
    
    # 1. Properties stats
    props = admin_client.table("properties").select("id, approval_status").execute()
    total_props = len(props.data) if props.data else 0
    pending_props = sum(1 for p in props.data if p.get("approval_status") == "pending") if props.data else 0
    
    # 2. Users count
    users = admin_client.table("profiles").select("id, role").execute()
    total_users = len(users.data) if users.data else 0
    agents_count = sum(1 for u in users.data if u.get("role") in ("agent", "agency")) if users.data else 0
    builders_count = sum(1 for u in users.data if u.get("role") == "builder") if users.data else 0
    
    # 3. Total revenue
    payments = admin_client.table("payments").select("amount").execute()
    total_revenue = sum(p.get("amount", 0) for p in payments.data) if payments.data else 0
    
    # 4. Open complaints
    tickets = admin_client.table("support_tickets").select("id, status").execute()
    total_tickets = len(tickets.data) if tickets.data else 0
    open_tickets = sum(1 for t in tickets.data if t.get("status") == "open") if tickets.data else 0
    
    return {
        "total_properties": total_props,
        "pending_properties": pending_props,
        "total_users": total_users,
        "agents_count": agents_count,
        "builders_count": builders_count,
        "total_revenue_inr": total_revenue,
        "total_complaints": total_tickets,
        "open_complaints": open_tickets
    }

