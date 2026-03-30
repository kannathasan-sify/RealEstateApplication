"""
routers/saved.py — /api/v1/saved (saved properties) + /api/v1/searches (saved searches)
GET/POST/DELETE for both resources
"""

from fastapi import APIRouter, Depends, HTTPException, status
from app.schemas.user import SavedSearchCreate, SavedSearchResponse
from app.schemas.property import PropertyResponse
from app.services.supabase_client import get_supabase_admin
from app.middleware.auth_middleware import get_current_user

router = APIRouter()


# ─── Saved Properties ─────────────────────────────────────────────────────────

@router.get("/", response_model=list[PropertyResponse])
async def get_saved_properties(current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = (
        admin.table("saved_properties")
        .select("property_id, properties(*)")
        .eq("user_id", current_user["id"])
        .order("created_at", desc=True)
        .execute()
    )
    properties = []
    for row in (result.data or []):
        prop_data = row.get("properties")
        if prop_data:
            properties.append(PropertyResponse(**prop_data))
    return properties


@router.post("/{property_id}", status_code=status.HTTP_201_CREATED)
async def save_property(property_id: str, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    # Check if property exists
    prop = admin.table("properties").select("id").eq("id", property_id).maybe_single().execute()
    if not prop.data:
        raise HTTPException(status_code=404, detail="Property not found")

    try:
        admin.table("saved_properties").insert({
            "user_id": current_user["id"],
            "property_id": property_id,
        }).execute()
    except Exception as e:
        if "unique" in str(e).lower() or "duplicate" in str(e).lower():
            raise HTTPException(status_code=409, detail="Already saved")
        raise HTTPException(status_code=500, detail=str(e))

    return {"message": "Property saved"}


@router.delete("/{property_id}", status_code=status.HTTP_204_NO_CONTENT)
async def unsave_property(property_id: str, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    admin.table("saved_properties").delete().eq("user_id", current_user["id"]).eq("property_id", property_id).execute()


# ─── Saved Searches (mounted at /api/v1/searches by main.py) ──────────────────
# These endpoints are hoisted here for inclusion via separate include in main —
# or imported in main as saved.searches_router

searches_router = APIRouter()


@searches_router.get("/", response_model=list[SavedSearchResponse])
async def get_saved_searches(current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = (
        admin.table("saved_searches")
        .select("*")
        .eq("user_id", current_user["id"])
        .order("created_at", desc=True)
        .execute()
    )
    return [SavedSearchResponse(**s) for s in (result.data or [])]


@searches_router.post("/", response_model=SavedSearchResponse, status_code=status.HTTP_201_CREATED)
async def save_search(body: SavedSearchCreate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    new_search = {
        "user_id": current_user["id"],
        "label": body.label,
        "listing_type": body.listing_type,
        "filters": body.filters,
        "thumbnail_url": body.thumbnail_url,
    }
    result = admin.table("saved_searches").insert(new_search).execute()
    return SavedSearchResponse(**result.data[0])


@searches_router.delete("/{search_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_saved_search(search_id: str, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    saved = (
        admin.table("saved_searches")
        .select("user_id")
        .eq("id", search_id)
        .maybe_single()
        .execute()
    )
    if not saved.data:
        raise HTTPException(status_code=404, detail="Saved search not found")
    if saved.data["user_id"] != current_user["id"]:
        raise HTTPException(status_code=403, detail="Not your saved search")
    admin.table("saved_searches").delete().eq("id", search_id).execute()
