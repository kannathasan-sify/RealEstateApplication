"""
schemas/discussion.py — Property discussion / Q&A schemas
"""

from typing import Optional, List
from pydantic import BaseModel


class ReplyResponse(BaseModel):
    id: str
    user_id: str
    user_name: str
    message: str
    created_at: str


class DiscussionResponse(BaseModel):
    id: str
    property_id: str
    user_id: str
    user_name: str
    message: str
    created_at: str
    replies: List[ReplyResponse] = []


class DiscussionCreate(BaseModel):
    message: str
    parent_id: Optional[str] = None
