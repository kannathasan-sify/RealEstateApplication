"""
services/auth_service.py — Supabase Auth helpers + JWT utils
"""

import random
import string
from datetime import datetime, timedelta, timezone
from typing import Optional

from jose import JWTError, jwt
from app.config import settings
from app.services.supabase_client import get_supabase, get_supabase_admin


# ─── user_id_code generation ─────────────────────────────────────────────────

def generate_user_id_code() -> str:
    """Generate a unique code like RE-20261234."""
    year = datetime.now().year
    digits = "".join(random.choices(string.digits, k=4))
    return f"RE-{year}{digits}"


# ─── JWT ──────────────────────────────────────────────────────────────────────

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + (
        expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)


def decode_token(token: str) -> dict:
    """Decode and verify JWT. Raises JWTError on failure."""
    return jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])


# ─── Profile helpers ──────────────────────────────────────────────────────────

async def get_or_create_profile(user_id: str, email: str, full_name: Optional[str] = None) -> dict:
    """Fetch profile; create with generated code if it doesn't exist."""
    admin = get_supabase_admin()
    result = admin.table("profiles").select("*").eq("id", user_id).maybe_single().execute()
    if result.data:
        return result.data

    code = generate_user_id_code()
    # Ensure uniqueness (simple retry)
    for _ in range(5):
        existing = admin.table("profiles").select("id").eq("user_id_code", code).maybe_single().execute()
        if not existing.data:
            break
        code = generate_user_id_code()

    new_profile = {
        "id": user_id,
        "full_name": full_name or email.split("@")[0],
        "user_id_code": code,
        "role": "buyer",
    }
    created = admin.table("profiles").insert(new_profile).execute()
    return created.data[0] if created.data else new_profile
