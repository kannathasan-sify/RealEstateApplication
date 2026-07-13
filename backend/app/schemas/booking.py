"""
schemas/booking.py — Visit booking Pydantic schemas
"""

from typing import Optional
from datetime import date, time
from pydantic import BaseModel
from enum import Enum


class BookingStatus(str, Enum):
    pending = "pending"
    confirmed = "confirmed"
    cancelled = "cancelled"
    completed = "completed"


class BookingCreate(BaseModel):
    property_id: str
    visit_date: date
    visit_time: Optional[time] = None
    message: Optional[str] = None


class BookingStatusUpdate(BaseModel):
    status: BookingStatus


class BookingResponse(BaseModel):
    id: str
    property_id: str
    buyer_id: str
    visit_date: Optional[str] = None
    visit_time: Optional[str] = None
    status: str = "pending"
    message: Optional[str] = None
    created_at: Optional[str] = None

    # Joined
    property_title: Optional[str] = None
    property_city: Optional[str] = None
    property_image: Optional[str] = None
