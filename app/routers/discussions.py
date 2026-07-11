"""
routers/discussions.py — /api/v1/properties/{property_id}/discussions
Discussion / Q&A thread for property listings
"""

import uuid
from datetime import datetime
from typing import List, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, status
from app.middleware.auth_middleware import get_current_user
from app.services.supabase_client import get_supabase_admin
from app.schemas.discussion import DiscussionResponse, ReplyResponse, DiscussionCreate

router = APIRouter()

# ─── Resilient In-Memory Fallback Store ───────────────────────────────────────
discussions_cache: List[Dict[str, Any]] = [
    {
        "id": "d-mock-001",
        "property_id": "mock-001",
        "user_id": "mock-user-002",
        "user_name": "Rajesh Kumar",
        "message": "Is the price negotiable? I am very interested.",
        "created_at": "2026-03-30T10:00:00Z",
        "replies": [
            {
                "id": "r-mock-001",
                "user_id": "mock-user-001",
                "user_name": "kannathasan sify",
                "message": "Yes, we can discuss a slight discount during a visit.",
                "created_at": "2026-03-30T10:15:00Z"
            }
        ]
    }
]


@router.get("/properties/{property_id}/discussions", response_model=List[DiscussionResponse])
async def list_property_discussions(property_id: str):
    """List all Q&A discussions for a property."""
    admin = get_supabase_admin()
    
    try:
        # Try fetching from DB
        result = (
            admin.table("property_discussions")
            .select("*, profiles(full_name)")
            .eq("property_id", property_id)
            .order("created_at", desc=False)
            .execute()
        )
        rows = result.data or []
        
        # Structure hierarchical Q&A thread (parents with replies)
        parents: Dict[str, DiscussionResponse] = {}
        child_rows: List[Dict[str, Any]] = []
        
        for r in rows:
            u_name = r.get("profiles", {}).get("full_name") or "User"
            if not r.get("parent_id"):
                parents[r["id"]] = DiscussionResponse(
                    id=r["id"],
                    property_id=r["property_id"],
                    user_id=r["user_id"],
                    user_name=u_name,
                    message=r["message"],
                    created_at=r["created_at"],
                    replies=[]
                )
            else:
                child_rows.append(r)
                
        for r in child_rows:
            p_id = r["parent_id"]
            if p_id in parents:
                u_name = r.get("profiles", {}).get("full_name") or "User"
                parents[p_id].replies.append(ReplyResponse(
                    id=r["id"],
                    user_id=r["user_id"],
                    user_name=u_name,
                    message=r["message"],
                    created_at=r["created_at"]
                ))
                
        return list(parents.values())
        
    except Exception as e:
        # Fallback to cache if table not found / database issues
        print(f"Fallback to cache for discussions on property {property_id}: {e}")
        return [
            DiscussionResponse(**d) for d in discussions_cache 
            if d["property_id"] == property_id
        ]


@router.post("/properties/{property_id}/discussions", response_model=DiscussionResponse)
async def post_discussion_message(
    property_id: str,
    body: DiscussionCreate,
    current_user: dict = Depends(get_current_user)
):
    """Post a new question (no parent_id) or reply to a question (with parent_id)."""
    admin = get_supabase_admin()
    user_id = current_user["id"]
    user_name = current_user.get("full_name") or current_user.get("email", "").split("@")[0] or "User"
    now_str = datetime.utcnow().isoformat() + "Z"
    
    try:
        new_row = {
            "property_id": property_id,
            "user_id": user_id,
            "message": body.message,
            "parent_id": body.parent_id
        }
        result = admin.table("property_discussions").insert(new_row).execute()
        row = result.data[0]
        
        # If it is a reply, we want to return the parent object with all replies
        target_parent_id = body.parent_id or row["id"]
        
        # Re-fetch parent thread from DB to return structured object
        parent_result = (
            admin.table("property_discussions")
            .select("*, profiles(full_name)")
            .eq("id", target_parent_id)
            .single()
            .execute()
        )
        p_row = parent_result.data
        p_name = p_row.get("profiles", {}).get("full_name") or "User"
        
        replies_result = (
            admin.table("property_discussions")
            .select("*, profiles(full_name)")
            .eq("parent_id", target_parent_id)
            .order("created_at", desc=False)
            .execute()
        )
        r_rows = replies_result.data or []
        
        replies = [
            ReplyResponse(
                id=r["id"],
                user_id=r["user_id"],
                user_name=r.get("profiles", {}).get("full_name") or "User",
                message=r["message"],
                created_at=r["created_at"]
            ) for r in r_rows
        ]
        
        return DiscussionResponse(
            id=p_row["id"],
            property_id=p_row["property_id"],
            user_id=p_row["user_id"],
            user_name=p_name,
            message=p_row["message"],
            created_at=p_row["created_at"],
            replies=replies
        )
        
    except Exception as e:
        # Fallback to cache
        print(f"Fallback insert to cache for discussions: {e}")
        
        if body.parent_id:
            # Find parent in cache
            parent = next((d for d in discussions_cache if d["id"] == body.parent_id), None)
            if not parent:
                # Create a parent in cache just in case
                parent = {
                    "id": body.parent_id,
                    "property_id": property_id,
                    "user_id": "system",
                    "user_name": "System",
                    "message": "Original question thread",
                    "created_at": now_str,
                    "replies": []
                }
                discussions_cache.append(parent)
                
            new_reply = {
                "id": f"r-mock-{uuid.uuid4()}",
                "user_id": user_id,
                "user_name": user_name,
                "message": body.message,
                "created_at": now_str
            }
            parent["replies"].append(new_reply)
            return DiscussionResponse(**parent)
            
        else:
            new_question = {
                "id": f"d-mock-{uuid.uuid4()}",
                "property_id": property_id,
                "user_id": user_id,
                "user_name": user_name,
                "message": body.message,
                "created_at": now_str,
                "replies": []
            }
            discussions_cache.append(new_question)
            return DiscussionResponse(**new_question)
