"""
routers/agencies.py — /api/v1/agencies
GET /       — list + search agencies
GET /{id}   — agency detail + their active property listings
"""

from typing import Optional
from fastapi import APIRouter, Query
from pydantic import BaseModel
from app.services.supabase_client import get_supabase_admin

router = APIRouter()


class AgencyResponse(BaseModel):
    id: str
    name: str
    logo_url: Optional[str] = None
    license_number: Optional[str] = None
    rera_number: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    city: Optional[str] = None
    is_verified: bool = False
    created_at: Optional[str] = None
    listing_count: Optional[int] = None


@router.get("/", response_model=list[AgencyResponse])
async def list_agencies(
    q: Optional[str] = Query(None, description="Search by name"),
    city: Optional[str] = None,
    page: int = 1,
    limit: int = 20,
):
    admin = get_supabase_admin()
    query = admin.table("agencies").select("*")

    if q:
        query = query.ilike("name", f"%{q}%")
    if city:
        query = query.eq("city", city)

    offset = (page - 1) * limit
    result = query.range(offset, offset + limit - 1).execute()
    agencies = result.data or []

    # Get listing counts
    enriched = []
    for agency in agencies:
        count_result = (
            admin.table("properties")
            .select("id", count="exact")
            .eq("agency_id", agency["id"])
            .eq("status", "active")
            .execute()
        )
        enriched.append(AgencyResponse(**agency, listing_count=count_result.count or 0))
    return enriched


@router.get("/{agency_id}")
async def get_agency_detail(agency_id: str):
    admin = get_supabase_admin()
    agency = admin.table("agencies").select("*").eq("id", agency_id).maybe_single().execute()
    if not agency.data:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Agency not found")

    props = (
        admin.table("properties")
        .select("id,title,price,price_frequency,property_type,listing_type,city,images,bedrooms,bathrooms,area_sqft,is_verified,is_featured,created_at")
        .eq("agency_id", agency_id)
        .eq("status", "active")
        .order("created_at", desc=True)
        .execute()
    )

    return {
        **agency.data,
        "listings": props.data or [],
        "listing_count": len(props.data or []),
    }
