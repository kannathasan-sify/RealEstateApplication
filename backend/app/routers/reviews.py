"""
routers/reviews.py — /api/v1/reviews
GET /properties/{id}/reviews — list reviews for a property
POST /                        — add a review (authenticated)
"""

from fastapi import APIRouter, Depends, HTTPException, status
from app.schemas.user import ReviewCreate, ReviewResponse
from app.services.supabase_client import get_supabase_admin
from app.middleware.auth_middleware import get_current_user

router = APIRouter()


@router.get("/properties/{property_id}/reviews", response_model=list[ReviewResponse])
async def list_reviews(property_id: str):
    admin = get_supabase_admin()
    result = (
        admin.table("reviews")
        .select("*, profiles(full_name, avatar_url)")
        .eq("property_id", property_id)
        .order("created_at", desc=True)
        .execute()
    )
    reviews = []
    for r in (result.data or []):
        profile = r.pop("profiles", {}) or {}
        reviews.append(ReviewResponse(
            **r,
            reviewer_name=profile.get("full_name"),
            reviewer_avatar=profile.get("avatar_url"),
        ))
    return reviews


@router.post("/", response_model=ReviewResponse, status_code=status.HTTP_201_CREATED)
async def add_review(body: ReviewCreate, current_user: dict = Depends(get_current_user)):
    if not (1 <= body.rating <= 5):
        raise HTTPException(status_code=400, detail="Rating must be between 1 and 5")

    admin = get_supabase_admin()
    prop = admin.table("properties").select("id").eq("id", body.property_id).maybe_single().execute()
    if not prop.data:
        raise HTTPException(status_code=404, detail="Property not found")

    # One review per user per property
    existing = (
        admin.table("reviews")
        .select("id")
        .eq("property_id", body.property_id)
        .eq("reviewer_id", current_user["id"])
        .maybe_single()
        .execute()
    )
    if existing.data:
        raise HTTPException(status_code=409, detail="You already reviewed this property")

    new_review = {
        "property_id": body.property_id,
        "reviewer_id": current_user["id"],
        "rating": body.rating,
        "comment": body.comment,
    }
    result = admin.table("reviews").insert(new_review).execute()
    review = result.data[0]
    return ReviewResponse(
        **review,
        reviewer_name=current_user.get("full_name"),
        reviewer_avatar=current_user.get("avatar_url"),
    )
