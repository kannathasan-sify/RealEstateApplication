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
from app.routers.property_leads import PropertyLeadResponse
from app.services.auth_service import generate_user_id_code

router = APIRouter()

_VALID_LEAD_STATUSES = {"pending", "contacted", "visit_scheduled", "converted", "closed", "rejected"}


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
    profiles = result.data or []
    
    if profiles:
        try:
            auth_users = admin_client.auth.admin.list_users()
            email_map = {u.id: u.email for u in auth_users}
            for p in profiles:
                p["email"] = email_map.get(p["id"])
        except Exception:
            pass
            
    return profiles


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


# ── Builder Management ─────────────────────────────────────────────────────────

class CreateBuilderRequest(BaseModel):
    email: str
    password: str
    full_name: str
    phone: Optional[str] = None


@router.post("/builders")
async def create_builder(
    body: CreateBuilderRequest,
    current_user: dict = Depends(get_current_user),
):
    """Admin-created builder account. Creates the Supabase Auth user + a
    profiles row with role='builder' directly (bypasses get_or_create_profile's
    hardcoded role='buyer' default, since the admin is explicitly setting the role)."""
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
    if len(body.password) < 6:
        raise HTTPException(status_code=400, detail="Password must be at least 6 characters")

    admin_client = get_supabase_admin()
    user_id = None
    try:
        result = admin_client.auth.admin.create_user({
            "email": body.email,
            "password": body.password,
            "email_confirm": True,
            "user_metadata": {"full_name": body.full_name},
        })
        if result.user:
            user_id = result.user.id
    except Exception as e:
        err = str(e).lower()
        if "already been registered" in err or "already exists" in err or "duplicate" in err:
            raise HTTPException(status_code=400, detail="An account with this email already exists.")
        raise HTTPException(status_code=500, detail=f"Failed to create auth account: {e}")

    if not user_id:
        raise HTTPException(status_code=500, detail="Failed to create auth account")

    code = generate_user_id_code()
    for _ in range(5):
        try:
            existing = admin_client.table("profiles").select("id").eq("user_id_code", code).maybe_single().execute()
            if not existing or not existing.data:
                break
        except Exception:
            break
        code = generate_user_id_code()

    profile_row = {
        "id": user_id,
        "full_name": body.full_name,
        "phone": body.phone,
        "role": "builder",
        "user_id_code": code,
        "is_verified": False,
    }

    try:
        result = admin_client.table("profiles").insert(profile_row).execute()
    except Exception as e:
        try:
            admin_client.auth.admin.delete_user(user_id)
        except Exception:
            pass
        raise HTTPException(status_code=500, detail=f"Failed to create builder profile: {e}")

    if not result.data:
        try:
            admin_client.auth.admin.delete_user(user_id)
        except Exception:
            pass
        raise HTTPException(status_code=500, detail="Failed to create builder profile")

    profile = result.data[0]
    profile["email"] = body.email
    return {"status": "success", "user": profile}


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
        .select("*, profiles(full_name)")
        .order("created_at", desc=True)
        .execute()
    )
    payments = result.data or []
    
    if payments:
        try:
            auth_users = admin_client.auth.admin.list_users()
            email_map = {u.id: u.email for u in auth_users}
            for p in payments:
                u_id = p.get("user_id")
                if "profiles" in p and p["profiles"]:
                    p["profiles"]["email"] = email_map.get(u_id)
                else:
                    p["profiles"] = {"full_name": None, "email": email_map.get(u_id)}
        except Exception:
            pass
            
    return payments


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
    query = admin_client.table("support_tickets").select("*, profiles(full_name)")
    if status:
        query = query.eq("status", status)
    
    result = query.order("created_at", desc=True).execute()
    tickets = result.data or []
    
    if tickets:
        try:
            auth_users = admin_client.auth.admin.list_users()
            email_map = {u.id: u.email for u in auth_users}
            for t in tickets:
                u_id = t.get("user_id")
                if "profiles" in t and t["profiles"]:
                    t["profiles"]["email"] = email_map.get(u_id)
                else:
                    t["profiles"] = {"full_name": None, "email": email_map.get(u_id)}
        except Exception:
            pass
            
    return tickets


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


# ── Property Leads / Enquiries Management ────────────────────────────────────

class AdminLeadUpdateRequest(BaseModel):
    """Body for PATCH /admin/leads/{lead_id} — admin can edit the status, the enquiry
    message, and/or the buyer's contact details snapshotted on the lead. All fields
    optional; only the ones sent are changed."""
    status:      Optional[str] = None   # pending|contacted|visit_scheduled|converted|closed|rejected
    message:     Optional[str] = None
    buyer_name:  Optional[str] = None
    buyer_phone: Optional[str] = None
    buyer_email: Optional[str] = None


@router.get("/leads", response_model=list[PropertyLeadResponse])
async def admin_list_leads(
    lead_status: Optional[str] = Query(None, alias="status", description="Filter by lead status"),
    current_user: dict = Depends(get_current_user),
):
    """All property 'I'm Interested' leads across every listing — the Admin Dashboard
    Enquiries tab. Joins profiles.role for the buyer so the UI can show 'Name (role)'.
    """
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")

    admin_client = get_supabase_admin()
    query = (
        admin_client.table("property_leads")
        .select("*, profiles!property_leads_buyer_id_fkey(role)")
        .order("created_at", desc=True)
    )
    if lead_status:
        query = query.eq("status", lead_status.lower())

    result = query.execute()

    leads = []
    for row in (result.data or []):
        profile = row.pop("profiles", None) or {}
        row["buyer_role"] = profile.get("role")
        leads.append(PropertyLeadResponse(**row))
    return leads


@router.patch("/leads/{lead_id}", response_model=PropertyLeadResponse)
async def admin_update_lead(
    lead_id: str,
    body: AdminLeadUpdateRequest,
    current_user: dict = Depends(get_current_user),
):
    """Admin edits a lead — correct a buyer's phone number, tweak the message, change the
    follow-up status, or mark it 'rejected' (spam / not a genuine enquiry, distinct from
    outright deleting it)."""
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")

    if body.status is not None and body.status.lower() not in _VALID_LEAD_STATUSES:
        raise HTTPException(
            status_code=400,
            detail=f"status must be one of {sorted(_VALID_LEAD_STATUSES)}",
        )

    admin_client = get_supabase_admin()

    existing = (
        admin_client.table("property_leads").select("id").eq("id", lead_id).maybe_single().execute()
    )
    if not existing.data:
        raise HTTPException(status_code=404, detail="Lead not found")

    update_data = {"updated_at": datetime.utcnow().isoformat()}
    if body.status is not None:
        update_data["status"] = body.status.lower()
    if body.message is not None:
        update_data["message"] = body.message
    if body.buyer_name is not None:
        update_data["buyer_name"] = body.buyer_name
    if body.buyer_phone is not None:
        update_data["buyer_phone"] = body.buyer_phone
    if body.buyer_email is not None:
        update_data["buyer_email"] = body.buyer_email

    result = admin_client.table("property_leads").update(update_data).eq("id", lead_id).execute()
    if not result.data:
        raise HTTPException(status_code=500, detail="Failed to update lead")
    return PropertyLeadResponse(**result.data[0])


@router.delete("/leads/{lead_id}")
async def admin_delete_lead(
    lead_id: str,
    current_user: dict = Depends(get_current_user),
):
    """Admin permanently deletes a lead (e.g. duplicate or clearly fraudulent enquiry)."""
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")

    admin_client = get_supabase_admin()
    existing = (
        admin_client.table("property_leads").select("id").eq("id", lead_id).maybe_single().execute()
    )
    if not existing.data:
        raise HTTPException(status_code=404, detail="Lead not found")

    admin_client.table("property_leads").delete().eq("id", lead_id).execute()
    return {"status": "success", "message": "Lead deleted successfully"}


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

