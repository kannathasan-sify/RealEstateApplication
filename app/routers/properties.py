"""
routers/properties.py — /api/v1/properties
GET    /             — list with full filter + pagination
GET    /featured     — featured listings
GET    /search       — autocomplete search
GET    /{id}         — property detail
GET    /{id}/similar — similar properties
POST   /             — create (landlord/agent/agency/developer)
PUT    /{id}         — update (owner/admin)
DELETE /{id}         — delete (owner/admin)
POST   /{id}/images  — upload images (owner/admin)
"""

import uuid
import random
import string
from typing import Optional, List
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File, status

from app.schemas.property import (
    PropertyCreate, PropertyUpdate, PropertyResponse, PropertyListResponse, SortBy
)
from app.services.supabase_client import get_supabase_admin
from app.services.storage_service import upload_property_image
from app.services.role_service import POSTER_ROLES
from app.middleware.auth_middleware import get_current_user, get_optional_user

router = APIRouter()

PROPERTY_FIELDS = (
    "id,owner_id,agency_id,listed_by,title,description,price,price_frequency,"
    "property_type,listing_type,bedrooms,bathrooms,area_sqft,address,neighborhood,"
    "district,city,latitude,longitude,images,video_url,amenities,furnishing,completion_status,"
    "payment_plan,handover_date,developer_name,permit_number,rera_number,reference_id,"
    "brn_dld,zone_name,is_verified,is_featured,status,approval_status,rejection_reason,"
    "agent_name,agent_phone,agent_photo,whatsapp_number,"
    "metadata,"           # JSONB — category-specific extra fields (Ground/Contractor/HolidayStay)
    "created_at,updated_at"
)

# Image limits per property
MIN_IMAGES = 2
MAX_IMAGES = 6


def _generate_reference_id() -> str:
    chars = string.ascii_uppercase + string.digits
    rand = "".join(random.choices(chars, k=5))
    return f"RE-S-{rand}"


def _apply_filters(query, params: dict):
    """Apply all filter query params to a Supabase query builder."""
    if params.get("listing_type"):
        query = query.eq("listing_type", params["listing_type"])
    if params.get("property_type"):
        query = query.eq("property_type", params["property_type"])
    if params.get("district"):
        # Tamil Nadu district filter (exact match, case-insensitive)
        query = query.ilike("district", f"%{params['district']}%")
    if params.get("city"):
        query = query.ilike("city", f"%{params['city']}%")
    if params.get("neighborhood"):
        # area / locality within district
        query = query.ilike("neighborhood", f"%{params['neighborhood']}%")
    if params.get("min_price") is not None:
        query = query.gte("price", params["min_price"])
    if params.get("max_price") is not None:
        query = query.lte("price", params["max_price"])
    if params.get("price_frequency"):
        query = query.eq("price_frequency", params["price_frequency"])
    if params.get("bedrooms") is not None:
        query = query.eq("bedrooms", params["bedrooms"])
    if params.get("bathrooms") is not None:
        query = query.eq("bathrooms", params["bathrooms"])
    if params.get("min_area") is not None:
        query = query.gte("area_sqft", params["min_area"])
    if params.get("max_area") is not None:
        query = query.lte("area_sqft", params["max_area"])
    if params.get("furnishing"):
        query = query.eq("furnishing", params["furnishing"])
    if params.get("completion_status"):
        query = query.eq("completion_status", params["completion_status"])
    if params.get("listed_by"):
        query = query.eq("listed_by", params["listed_by"])
    if params.get("agency_id"):
        query = query.eq("agency_id", params["agency_id"])
    if params.get("verified_only"):
        query = query.eq("is_verified", True)
    if params.get("has_video"):
        query = query.not_.is_("video_url", "null")
    if params.get("keyword"):
        kw = params["keyword"]
        query = query.or_(f"title.ilike.%{kw}%,description.ilike.%{kw}%,neighborhood.ilike.%{kw}%")
    # ── Category-specific metadata JSONB filters ──────────────────────────────
    # Uses PostgreSQL @> containment operator on the metadata JSONB column.
    # e.g. metadata @> '{"ground_type":"cricket"}'
    if params.get("ground_type"):
        query = query.contains("metadata", {"ground_type": params["ground_type"]})
    if params.get("work_category"):
        query = query.contains("metadata", {"work_category": params["work_category"]})
    if params.get("stay_type"):
        query = query.contains("metadata", {"stay_type": params["stay_type"]})
    return query


def _apply_sort(query, sort_by: str):
    if sort_by == "price_asc":
        return query.order("price", desc=False)
    if sort_by == "price_desc":
        return query.order("price", desc=True)
    if sort_by == "oldest":
        return query.order("created_at", desc=False)
    return query.order("created_at", desc=True)  # newest (default)


# ─── List Properties ──────────────────────────────────────────────────────────

@router.get("/", response_model=PropertyListResponse)
async def list_properties(
    listing_type: Optional[str] = None,
    property_type: Optional[str] = None,
    district: Optional[str] = None,        # Tamil Nadu district filter
    city: Optional[str] = None,
    neighborhood: Optional[str] = None,    # area / locality within district
    min_price: Optional[float] = None,
    max_price: Optional[float] = None,
    price_frequency: Optional[str] = None,
    bedrooms: Optional[int] = None,
    bathrooms: Optional[int] = None,
    min_area: Optional[float] = None,
    max_area: Optional[float] = None,
    furnishing: Optional[str] = None,
    completion_status: Optional[str] = None,
    amenities: Optional[List[str]] = Query(None),
    keyword: Optional[str] = None,
    listed_by: Optional[str] = None,
    agency_id: Optional[str] = None,
    verified_only: bool = False,
    has_video: bool = False,
    sort_by: SortBy = SortBy.newest,
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    # ── Category-specific metadata filters ───────────────────────────────────
    ground_type:   Optional[str] = None,   # e.g. cricket | football | swimming_pool
    work_category: Optional[str] = None,   # construction | maintenance
    stay_type:     Optional[str] = None,   # entire_home | villa | resort | private_room
):
    admin = get_supabase_admin()
    params = {k: v for k, v in locals().items() if k not in ("admin", "page", "limit", "sort_by", "amenities")}

    # Public listing: only active + approved properties
    query = (admin.table("properties")
             .select(PROPERTY_FIELDS, count="exact")
             .eq("status", "active")
             .eq("approval_status", "approved"))
    query = _apply_filters(query, params)

    # Amenities overlap filter (PostgreSQL @> operator)
    if amenities:
        query = query.contains("amenities", amenities)

    query = _apply_sort(query, sort_by)

    offset = (page - 1) * limit
    result = query.range(offset, offset + limit - 1).execute()

    total = result.count or 0
    data = [PropertyResponse(**p) for p in (result.data or [])]
    return PropertyListResponse(
        data=data, total=total, page=page, limit=limit,
        has_next=(offset + limit) < total,
    )


# ─── My Properties (owner's own listings — all approval states) ──────────────

@router.get("/mine", response_model=PropertyListResponse)
async def get_my_properties(
    approval_status: Optional[str] = None,   # "pending" | "approved" | "rejected" | None = all
    page: int = Query(1, ge=1),
    limit: int = Query(50, ge=1, le=100),
    current_user: dict = Depends(get_current_user),
):
    """Return properties posted by the authenticated user — regardless of
    approval status.  Optionally filter by a specific approval_status."""
    admin = get_supabase_admin()
    query = (
        admin.table("properties")
        .select(PROPERTY_FIELDS, count="exact")
        .eq("owner_id", current_user["id"])
        .order("created_at", desc=True)
    )
    if approval_status:
        query = query.eq("approval_status", approval_status)

    offset = (page - 1) * limit
    result = query.range(offset, offset + limit - 1).execute()

    total = result.count or 0
    data = [PropertyResponse(**p) for p in (result.data or [])]
    return PropertyListResponse(
        data=data, total=total, page=page, limit=limit,
        has_next=(offset + limit) < total,
    )


# ─── Featured Listings ────────────────────────────────────────────────────────

@router.get("/featured", response_model=list[PropertyResponse])
async def get_featured():
    admin = get_supabase_admin()
    result = (
        admin.table("properties")
        .select(PROPERTY_FIELDS)
        .eq("is_featured", True)
        .eq("status", "active")
        .order("created_at", desc=True)
        .limit(20)
        .execute()
    )
    return [PropertyResponse(**p) for p in (result.data or [])]


# ─── Search Autocomplete ──────────────────────────────────────────────────────

@router.get("/search")
async def search_properties(
    q: str = Query(..., min_length=1),
    district: Optional[str] = None,
):
    admin = get_supabase_admin()
    query = (
        admin.table("properties")
        .select("id,title,district,city,neighborhood,listing_type,property_type,price,images")
        .or_(f"title.ilike.%{q}%,district.ilike.%{q}%,neighborhood.ilike.%{q}%,city.ilike.%{q}%")
        .eq("status", "active")
        .eq("approval_status", "approved")
    )
    if district:
        query = query.ilike("district", f"%{district}%")
    result = query.limit(15).execute()
    return result.data or []


# ─── Property Detail ──────────────────────────────────────────────────────────

@router.get("/{property_id}", response_model=PropertyResponse)
async def get_property(property_id: str):
    # Guard: reject non-UUID path segments early (e.g. "mine", "featured") so
    # PostgreSQL never sees an invalid UUID, which would cause an unhandled 500.
    try:
        uuid.UUID(property_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Property not found")

    admin = get_supabase_admin()
    result = admin.table("properties").select(PROPERTY_FIELDS).eq("id", property_id).maybe_single().execute()
    if not result.data:
        raise HTTPException(status_code=404, detail="Property not found")
    return PropertyResponse(**result.data)


# ─── Similar Properties ───────────────────────────────────────────────────────

@router.get("/{property_id}/similar", response_model=list[PropertyResponse])
async def get_similar(property_id: str):
    try:
        uuid.UUID(property_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Property not found")

    admin = get_supabase_admin()
    prop = admin.table("properties").select("listing_type,property_type,city,price").eq("id", property_id).maybe_single().execute()
    if not prop.data:
        raise HTTPException(status_code=404, detail="Property not found")
    p = prop.data

    result = (
        admin.table("properties")
        .select(PROPERTY_FIELDS)
        .eq("listing_type", p.get("listing_type"))
        .eq("property_type", p.get("property_type"))
        .eq("city", p.get("city"))
        .eq("status", "active")
        .neq("id", property_id)
        .limit(6)
        .execute()
    )
    return [PropertyResponse(**r) for r in (result.data or [])]


# ─── Create Property ──────────────────────────────────────────────────────────

@router.post("/", response_model=PropertyResponse, status_code=status.HTTP_201_CREATED)
async def create_property(body: PropertyCreate, current_user: dict = Depends(get_current_user)):
    role = current_user.get("role", "buyer")
    if role not in POSTER_ROLES:
        raise HTTPException(status_code=403, detail="Only landlord/agent/agency/developer can post listings")

    admin = get_supabase_admin()
    data = body.model_dump(mode="json")
    data["owner_id"] = current_user["id"]
    data["reference_id"] = _generate_reference_id()

    # Auto-populate agent contact fields from the poster's profile row
    # so every listing has visible contact details even if the client
    # didn't explicitly provide them (older app versions / PostAdFlow).
    if not data.get("agent_name") or not data.get("agent_phone"):
        try:
            profile_res = (
                admin.table("profiles")
                .select("full_name,phone,avatar_url,whatsapp_number")
                .eq("id", current_user["id"])
                .maybe_single()
                .execute()
            )
            if profile_res.data:
                p = profile_res.data
                if not data.get("agent_name"):
                    data["agent_name"] = p.get("full_name") or ""
                if not data.get("agent_phone"):
                    data["agent_phone"] = p.get("phone") or ""
                if not data.get("agent_photo"):
                    data["agent_photo"] = p.get("avatar_url") or ""
                # For landlord listings, fall back to profile whatsapp if not provided
                if data.get("listed_by") == "landlord" and not data.get("whatsapp_number"):
                    data["whatsapp_number"] = p.get("whatsapp_number") or p.get("phone") or ""
        except Exception:
            pass  # non-fatal — listing still saves, contact info just stays blank

    result = admin.table("properties").insert(data).execute()
    return PropertyResponse(**result.data[0])


# ─── Update Property ──────────────────────────────────────────────────────────

@router.put("/{property_id}", response_model=PropertyResponse)
async def update_property(
    property_id: str,
    body: PropertyUpdate,
    current_user: dict = Depends(get_current_user),
):
    admin = get_supabase_admin()
    existing = admin.table("properties").select("owner_id").eq("id", property_id).maybe_single().execute()
    if not existing.data:
        raise HTTPException(status_code=404, detail="Property not found")

    if existing.data["owner_id"] != current_user["id"] and current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Not the property owner")

    update_data = {k: v for k, v in body.model_dump(mode="json").items() if v is not None}
    update_data["updated_at"] = datetime.utcnow().isoformat()

    result = admin.table("properties").update(update_data).eq("id", property_id).execute()
    return PropertyResponse(**result.data[0])


# ─── Delete Property ──────────────────────────────────────────────────────────

@router.delete("/{property_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_property(property_id: str, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    existing = admin.table("properties").select("owner_id").eq("id", property_id).maybe_single().execute()
    if not existing.data:
        raise HTTPException(status_code=404, detail="Property not found")

    if existing.data["owner_id"] != current_user["id"] and current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Not the property owner")

    admin.table("properties").delete().eq("id", property_id).execute()


# ─── Upload Images ────────────────────────────────────────────────────────────

@router.post("/{property_id}/images", response_model=PropertyResponse)
async def upload_images(
    property_id: str,
    files: List[UploadFile] = File(...),
    current_user: dict = Depends(get_current_user),
):
    """Upload images for a property.
    - Minimum 2 images required per property.
    - Maximum 6 images allowed per property (across all upload calls).
    - Images are stored in Supabase Storage under property-images/{owner_id}/{property_id}/.
    """
    if len(files) < MIN_IMAGES:
        raise HTTPException(
            status_code=400,
            detail=f"Minimum {MIN_IMAGES} images are required per property listing"
        )
    if len(files) > MAX_IMAGES:
        raise HTTPException(
            status_code=400,
            detail=f"Maximum {MAX_IMAGES} images allowed per property"
        )

    admin = get_supabase_admin()
    existing = admin.table("properties").select("owner_id,images").eq("id", property_id).maybe_single().execute()
    if not existing.data:
        raise HTTPException(status_code=404, detail="Property not found")

    if existing.data["owner_id"] != current_user["id"] and current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Not the property owner")

    current_images: List[str] = existing.data.get("images") or []
    total_after = len(current_images) + len(files)
    if total_after > MAX_IMAGES:
        raise HTTPException(
            status_code=400,
            detail=f"This property already has {len(current_images)} image(s). "
                   f"Adding {len(files)} more would exceed the {MAX_IMAGES}-image limit."
        )

    new_urls: List[str] = []
    for upload in files:
        content = await upload.read()
        # Store images per-property: property-images/{owner_id}/{property_id}/{uuid}.ext
        url = await upload_property_image(
            content,
            upload.content_type or "image/jpeg",
            current_user["id"],
            property_id=property_id,
        )
        new_urls.append(url)

    all_images = current_images + new_urls
    result = (
        admin.table("properties")
        .update({"images": all_images, "updated_at": datetime.utcnow().isoformat()})
        .eq("id", property_id)
        .execute()
    )
    return PropertyResponse(**result.data[0])
