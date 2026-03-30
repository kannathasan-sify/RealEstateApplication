"""
services/storage_service.py — Supabase Storage helpers
"""

import uuid
from app.services.supabase_client import get_supabase_admin


PROPERTIES_BUCKET = "property-images"
AVATARS_BUCKET = "avatars"


async def upload_property_image(file_bytes: bytes, content_type: str, owner_id: str) -> str:
    """Upload a property image and return its public URL."""
    admin = get_supabase_admin()
    extension = content_type.split("/")[-1] if "/" in content_type else "jpg"
    path = f"{owner_id}/{uuid.uuid4()}.{extension}"
    admin.storage.from_(PROPERTIES_BUCKET).upload(
        path, file_bytes, {"content-type": content_type}
    )
    public_url = admin.storage.from_(PROPERTIES_BUCKET).get_public_url(path)
    return public_url


async def upload_avatar(file_bytes: bytes, content_type: str, user_id: str) -> str:
    """Upload a user avatar and return its public URL."""
    admin = get_supabase_admin()
    extension = content_type.split("/")[-1] if "/" in content_type else "jpg"
    path = f"{user_id}/avatar.{extension}"
    admin.storage.from_(AVATARS_BUCKET).upload(
        path, file_bytes, {"content-type": content_type, "upsert": "true"}
    )
    public_url = admin.storage.from_(AVATARS_BUCKET).get_public_url(path)
    return public_url


async def delete_file(bucket: str, path: str) -> None:
    """Delete a file from Supabase Storage."""
    admin = get_supabase_admin()
    admin.storage.from_(bucket).remove([path])
