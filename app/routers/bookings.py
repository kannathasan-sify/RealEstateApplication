"""
routers/bookings.py — /api/v1/bookings endpoints
POST /          — create booking
GET  /          — my bookings (buyer view)
PUT  /{id}/status — confirm/cancel (owner/admin)
DELETE /{id}    — cancel
"""

from fastapi import APIRouter, Depends, HTTPException, status
from app.schemas.booking import BookingCreate, BookingStatusUpdate, BookingResponse
from app.services.supabase_client import get_supabase_admin
from app.middleware.auth_middleware import get_current_user

router = APIRouter()


@router.post("/", response_model=BookingResponse, status_code=status.HTTP_201_CREATED)
async def create_booking(body: BookingCreate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    # Check property exists
    prop = admin.table("properties").select("id,title,city,images").eq("id", body.property_id).maybe_single().execute()
    if not prop.data:
        raise HTTPException(status_code=404, detail="Property not found")

    new_booking = {
        "property_id": body.property_id,
        "buyer_id": current_user["id"],
        "visit_date": str(body.visit_date),
        "visit_time": str(body.visit_time) if body.visit_time else None,
        "message": body.message,
        "status": "pending",
    }
    result = admin.table("bookings").insert(new_booking).execute()
    booking = result.data[0]

    p = prop.data
    return BookingResponse(
        **booking,
        property_title=p.get("title"),
        property_city=p.get("city"),
        property_image=p.get("images", [None])[0] if p.get("images") else None,
    )


@router.get("/", response_model=list[BookingResponse])
async def list_my_bookings(current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = (
        admin.table("bookings")
        .select("*, properties(title,city,images)")
        .eq("buyer_id", current_user["id"])
        .order("created_at", desc=True)
        .execute()
    )
    bookings = []
    for b in (result.data or []):
        prop = b.pop("properties", {}) or {}
        imgs = prop.get("images") or []
        bookings.append(BookingResponse(
            **b,
            property_title=prop.get("title"),
            property_city=prop.get("city"),
            property_image=imgs[0] if imgs else None,
        ))
    return bookings


@router.put("/{booking_id}/status", response_model=BookingResponse)
async def update_booking_status(
    booking_id: str,
    body: BookingStatusUpdate,
    current_user: dict = Depends(get_current_user),
):
    admin = get_supabase_admin()
    booking = admin.table("bookings").select("*").eq("id", booking_id).maybe_single().execute()
    if not booking.data:
        raise HTTPException(status_code=404, detail="Booking not found")

    b = booking.data
    user_id = current_user["id"]
    role = current_user.get("role", "buyer")

    # Property owner or admin can update status
    prop = admin.table("properties").select("owner_id").eq("id", b["property_id"]).maybe_single().execute()
    is_owner = prop.data and prop.data.get("owner_id") == user_id
    if not is_owner and role != "admin":
        raise HTTPException(status_code=403, detail="Only property owner or admin can update status")

    result = admin.table("bookings").update({"status": body.status}).eq("id", booking_id).execute()
    updated = result.data[0]
    return BookingResponse(**updated)


@router.delete("/{booking_id}", status_code=status.HTTP_204_NO_CONTENT)
async def cancel_booking(booking_id: str, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    booking = admin.table("bookings").select("buyer_id").eq("id", booking_id).maybe_single().execute()
    if not booking.data:
        raise HTTPException(status_code=404, detail="Booking not found")

    if booking.data["buyer_id"] != current_user["id"] and current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Cannot cancel another user's booking")

    admin.table("bookings").delete().eq("id", booking_id).execute()
