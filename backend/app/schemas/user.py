"""
schemas/user.py — Saved searches & misc user schemas
"""

from typing import Optional, Any, Dict
from pydantic import BaseModel


class SavedSearchCreate(BaseModel):
    label: str                         # e.g. "All Residential"
    listing_type: Optional[str] = None # e.g. "Property for Rent"
    filters: Optional[Dict[str, Any]] = {}
    thumbnail_url: Optional[str] = None


class SavedSearchResponse(BaseModel):
    id: str
    user_id: str
    label: str
    listing_type: Optional[str] = None
    filters: Optional[Dict[str, Any]] = {}
    thumbnail_url: Optional[str] = None
    created_at: Optional[str] = None


class ReviewCreate(BaseModel):
    property_id: str
    rating: int
    comment: Optional[str] = None

    class Config:
        # rating must be 1-5 — validated at router / DB level
        pass


class ReviewResponse(BaseModel):
    id: str
    property_id: str
    reviewer_id: str
    rating: int
    comment: Optional[str] = None
    created_at: Optional[str] = None
    reviewer_name: Optional[str] = None
    reviewer_avatar: Optional[str] = None
