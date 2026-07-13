"""
schemas/service_request.py — Service Request & Quotation schemas

Service Requests are posted by property owners/users who need:
  - Construction work (Build your Property)
  - Maintenance work  (Maintain Your Property)

Nearby contractors/service providers receive the request and can send quotations.
"""

from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel
from enum import Enum


# ─── Enums ───────────────────────────────────────────────────────────────────

class ServiceCategory(str, Enum):
    construction = "construction"   # e.g. "I need a house plan + construction"
    maintenance  = "maintenance"    # e.g. "My apartment needs painting"


class RequestStatus(str, Enum):
    open        = "open"         # accepting quotations
    in_progress = "in_progress"  # contractor assigned
    completed   = "completed"
    cancelled   = "cancelled"


class QuotationStatus(str, Enum):
    pending  = "pending"
    accepted = "accepted"
    rejected = "rejected"


class RequestUrgency(str, Enum):
    normal    = "normal"     # no rush
    urgent    = "urgent"     # needed soon
    emergency = "emergency"  # needed immediately (e.g. burst pipe, no power)


# ─── Service Request schemas ──────────────────────────────────────────────────

class ServiceRequestCreate(BaseModel):
    category:       ServiceCategory
    service_type:   str                    # e.g. "architect", "painting", "plumbing"
    title:          str                    # short description of need
    description:    Optional[str]  = None # detailed description
    district:       str                    # Tamil Nadu district — for nearby matching
    latitude:       Optional[float] = None
    longitude:      Optional[float] = None
    radius_km:      int             = 50   # search radius for contractor matching
    budget_min:     Optional[float] = None
    budget_max:     Optional[float] = None
    images:         Optional[List[str]] = []  # photos of the site / project
    urgency:        RequestUrgency  = RequestUrgency.normal
    preferred_date: Optional[str]   = None    # ISO date (YYYY-MM-DD), desired start date
    contact_phone:  Optional[str]   = None    # shown to contractors on the request detail


class ServiceRequestResponse(BaseModel):
    id:             str
    user_id:        str
    category:       str
    service_type:   str
    title:          str
    description:    Optional[str]   = None
    district:       str
    latitude:       Optional[float] = None
    longitude:      Optional[float] = None
    radius_km:      int             = 50
    budget_min:     Optional[float] = None
    budget_max:     Optional[float] = None
    images:         Optional[List[str]] = []
    status:         str             = "open"
    created_at:     Optional[str]   = None
    urgency:        str             = "normal"
    preferred_date: Optional[str]   = None
    contact_phone:  Optional[str]   = None
    # Joined
    quotation_count: int          = 0


# ─── Quotation schemas ────────────────────────────────────────────────────────

class QuotationCreate(Bas