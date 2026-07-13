"""
routers/service_requests.py — /api/v1/service-requests

POST   /                    — user posts a construction/maintenance request
GET    /                    — list open requests (contractors browse nearby)
GET    /{id}                — detail
PUT    /{id}/status         — user updates request status
POST   /{id}/quotations     — contractor submits a quotation
GET    /{id}/quotations     — list quotations for a request
PUT    /quotations/{qid}    — accept/reject a quotation
"""

import uuid
import math
from typing import Optional, List
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File, status

from app.schemas.service_request import (
    ServiceRequestCreate,
    ServiceRequestResponse,
    QuotationCreate,
    QuotationResponse,
)
from app.services.supabase_client import get_supabase_admin
from app.services.storage_service import upload_property_image
from app.middleware.auth_middleware import get_current_user

router = APIRouter()

# ─── Haversine bounding-box helper ───────────────────────────────────────────

def _lat_lng_bounds(lat: float, lng: float, radius_km: int):
    """Return (lat_min, lat_max, lng_min, lng_max) for a square bounding box."""
    delta_lat = radius_km / 111.0              # 1 degree lat ≈ 111 km
    delta_lng = radius_km / (111.0 * math.cos(math.radians(lat)))
    return (
        lat - delta_lat, lat + delta_lat,
        lng - delta_lng, lng + delta_lng,
    )


# ─── List Service Requests ────────────────────────────────────────────────────

_URGENCY_RANK = {"emergency": 0, "urgent": 1, "normal": 2}


@router.get("/", response_model=List[ServiceRequestResponse])
async def list_service_requests(
    category:     Optional[str] = None,     # construction | maintenance
    service_type: Optional[str] = None,
    district:     Optional[str] = None,
    center_lat:   Optional[float] = None,
    center_lng:   Optional[float] = None,
    radius_km:    Optional[int]   = None,   # 10 | 50 | 100
    status_filter: str = Query("open", alias="status"),
    urgency:      Optional[str]   = None,   # normal | urgent | emergency
    budget_min:   Optional[float] = None,   # only requests whose range overlaps this floor
    budget_max:   Optional[float] = None,   # only requests whose range overlaps this ceiling
    sort_by:      str = Query("newest"),    # newest | budget_high | budget_low | urgent_first
    page:  int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    user=Depends(get_current_user),
):
    admin = get_supabase_admin()
    query = (
        admin.table("service_requests")
        .select("*", count="exact")
        .eq("status", status_filter)
    )

    if category:
        query = query.eq("category", category)
    if service_type:
        query = query.eq("service_type", service_type)
    if district:
        query = query.ilike("district", f"%{district}%")
    if urgency:
        query = query.eq("urgency", urgency)

    # Budget overlap filter: keep requests whose [budget_min, budget_max] range
    # overlaps the contractor's search range.
    if budget_min is not None:
        query = query.gte("budget_max", budget_min)
    if budget_max is not None:
        query = query.lte("budget_min", budget_max)

    # Bounding-box location filter (used when GPS coordinates are provided)
    if center_lat is not None and center_lng is not None and radius_km:
        lat_min, lat_max, lng_min, lng_max = _lat_lng_bounds(center_lat, center_lng, radius_km)
        query = (query
                 .gte("latitude", lat_min).lte("latitude", lat_max)
                 .gte("longitude", lng_min).lte("longitude", lng_max))

    # "urgent_first" needs a custom rank not expressible as a single SQL column
    # order via supabase-py, so it's sorted in Python after fetching the
    # (unpaginated) matching set, then sliced for the requested page.
    if sort_by == "urgent_first":
        query = query.order("created_at", desc=True)
        result = query.execute()
        rows = result.data or []
        rows.sort(key=lambda r: (_URGENCY_RANK.get(r.get("urgency", "normal"), 2), ))
        offset = (page - 1) * limit
        rows = rows[offset: offset + limit]
    else:
        if sort_by == "budget_high":
            query = query.order("budget_max", desc=True, nullsfirst=False)
        elif sort_by == "budget_low":
            query = query.order("budget_min", desc=False, nullsfirst=False)
        else:  # "newest" (default)
            query = query.order("created_at", desc=True)

        offset = (page - 1) * limit
        result = query.range(offset, offset + limit - 1).execute()
        rows = result.data or []

    # Attach quotation_count from quotations table
    for row in rows:
        qc = admin.table("quotations").select("id", count="exact").eq("request_id", row["id"]).execute()
        row["quotation_count"] = qc.count or 0

    return [ServiceRequestResponse(**r) for r in rows]


# ─── Create Service Request ───────────────────────────────────────────────────

@router.post("/", response_model=ServiceRequestResponse, status_code=status.HTTP_201_CREATED)
async def create_service_request(
    body: ServiceRequestCreate,
    user=Depends(get_current_user),
):
    admin = get_supabase_admin()
    data = body.model_dump()
    data["id"]         = str(uuid.uuid4())
    data["user_id"]    = user["id"]
    data["status"]     = "open"
    data["created_at"] = datetime.utcnow().isoformat()
    data["updated_at"] = datetime.utcnow().isoformat()
    data["category"]   = data["category"].value if hasattr(data["category"], "value") else data["category"]
    data["urgency"]    = data["urgency"].value if hasattr(data["urgency"], "value") else data["urgency"]

    result = admin.table("service_requests").insert(data).execute()
    if not result.data:
        raise HTTPException(status_code=500, detail="Failed to create service request")
    row = result.data[0]
    row["quotation_count"] = 0
    return ServiceRequestResponse(**row)


# ─── Upload Images ────────────────────────────────────────────────────────────

@router.post("/{request_id}/images", response_model=ServiceRequestResponse)
async def upload_service_request_images(
    request_id: str,
    files: List[UploadFile] = File(...),
    user=Depends(get_current_user),
):
    admin = get_supabase_admin()
    existing = admin.table("service_requests").select("user_id,images").eq("id", request_id).maybe_single().execute()
    if not existing.data:
        raise HTTPException(status_code=404, detail="Service request not found")

    if existing.data["user_id"] != user["id"]:
        raise HTTPException(status_code=403, detail="Not your service request")

    current_images: List[str] = existing.data.get("images") or []
    new_urls: List[str] = []
    for upload in files:
        content = await upload.read()
        url = await upload_property_image(
            content,
            upload.content_type or "image/jpeg",
            user["id"],
            property_id=request_id,
        )
        new_urls.append(url)

    all_images = current_images + new_urls
    result = (
        admin.table("service_requests")
        .update({"images": all_images, "updated_at": datetime.utcnow().isoformat()})
        .eq("id", request_id)
        .execute()
    )
    row = result.data[0]
    qc = admin.table("quotations").select("id", count="exact").eq("request_id", request_id).execute()
    row["quotation_count"] = qc.count or 0
    return ServiceRequestResponse(**row)



# ─── Get Service Request Detail ───────────────────────────────────────────────

@router.get("/{request_id}", response_model=ServiceRequestResponse)
async def get_service_request(
    request_id: str,
    user=Depends(get_current_user),
):
    admin  = get_supabase_admin()
    result = admin.table("service_requests").select("*").eq("id", request_id).single().execute()
    if not result.data:
        raise HTTPException(status_code=404, detail="Service request not found")
    row = result.data
    qc  = admin.table("quotations").select("id", count="exact").eq("request_id", request_id).execute()
    row["quotation_count"] = qc.count or 0
    return ServiceRequestResponse(**row)


# ─── Update Request Status ────────────────────────────────────────────────────

@router.put("/{request_id}/status")
async def update_request_status(
    request_id: str,
    new_status: str = Query(...),   # open | in_progress | completed | cancelled
    user=Depends(get_current_user),
):
    valid = {"open", "in_progress", "completed", "cancelled"}
    if new_status not in valid:
        raise HTTPException(status_code=400, detail=f"status must be one of {valid}")

    admin  = get_supabase_admin()
    result = admin.table("service_requests").select("user_id").eq("id", request_id).single().execute()
    if not result.data:
        raise HTTPException(status_code=404, detail="Request not found")
    if result.data["user_id"] != user["id"]:
        raise HTTPException(status_code=403, detail="Not your request")

    admin.table("service_requests").update({
        "status":     new_status,
        "updated_at": datetime.utcnow().isoformat(),
    }).eq("id", request_id).execute()
    return {"status": new_status}


# ─── List Quotations for a Request ───────────────────────────────────────────

@router.get("/{request_id}/quotations", response_model=List[QuotationResponse])
async def list_quotations(
    request_id: str,
    user=Depends(get_current_user),
):
    admin = get_supabase_admin()
    # Verify user is the request owner or a contractor (relax later with RLS)
    result = admin.table("quotations").select("*").eq("request_id", request_id).order("created_at", desc=True).execute()
    return [QuotationResponse(**q) for q in (result.data or [])]


# ─── Submit Quotation ─────────────────────────────────────────────────────────

@router.post("/{request_id}/quotations", response_model=QuotationResponse, status_code=status.HTTP_201_CREATED)
async def submit_quotation(
    request_id: str,
    body: QuotationCreate,
    user=Depends(get_current_user),
):
    admin  = get_supabase_admin()
    # Verify request exists and is open
    req = admin.table("service_requests").select("status").eq("id", request_id).single().execute()
    if not req.data:
        raise HTTPException(status_code=404, detail="Service request not found")
    if req.data["status"] != "open":
        raise HTTPException(status_code=400, detail="Service request is no longer accepting quotations")

    data = body.model_dump()
    data["id"]             = str(uuid.uuid4())
    data["request_id"]     = request_id
    data["contractor_id"]  = user["id"]
    data["status"]         = "pending"
    data["created_at"]     = datetime.utcnow().isoformat()

    re