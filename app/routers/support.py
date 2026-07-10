"""
routers/support.py — /api/v1/support
POST /tickets    — create support ticket / complaint
GET  /tickets/me — view my submitted complaints
"""

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from typing import Optional, List
from app.middleware.auth_middleware import get_current_user
from app.services.supabase_client import get_supabase_admin

router = APIRouter()


class TicketCreate(BaseModel):
    subject: str
    description: str


class TicketResponse(BaseModel):
    id: str
    user_id: str
    subject: str
    description: str
    status: str
    reply: Optional[str] = None
    created_at: str


@router.post("/tickets", response_model=TicketResponse, status_code=status.HTTP_201_CREATED)
async def create_ticket(body: TicketCreate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    data = {
        "user_id": current_user["id"],
        "subject": body.subject,
        "description": body.description,
        "status": "open",
    }
    result = admin.table("support_tickets").insert(data).execute()
    if not result.data:
        raise HTTPException(status_code=500, detail="Failed to create support ticket")
    return TicketResponse(**result.data[0])


@router.get("/tickets/me", response_model=List[TicketResponse])
async def get_my_tickets(current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = (
        admin.table("support_tickets")
        .select("*")
        .eq("user_id", current_user["id"])
        .order("created_at", desc=True)
        .execute()
    )
    return [TicketResponse(**t) for t in (result.data or [])]
