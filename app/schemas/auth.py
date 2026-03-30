"""
schemas/auth.py — Auth & Profile Pydantic schemas
"""

from typing import Optional
from pydantic import BaseModel, EmailStr, field_validator
import re


# ─── Request schemas ──────────────────────────────────────────────────────────

class RegisterRequest(BaseModel):
    email: EmailStr
    password: str
    full_name: str
    phone: Optional[str] = None

    @field_validator("password")
    @classmethod
    def password_strength(cls, v: str) -> str:
        if len(v) < 6:
            raise ValueError("Password must be at least 6 characters")
        return v


class LoginRequest(BaseModel):
    email: Optional[EmailStr] = None
    user_id_code: Optional[str] = None   # e.g. RE-20261234
    password: str

    @field_validator("email", mode="before")
    @classmethod
    def require_email_or_code(cls, v, info):
        # validation is handled at endpoint level
        return v


class GoogleAuthRequest(BaseModel):
    id_token: str   # Google ID token from Android GoogleSignIn


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: "UserProfile"


# ─── Profile schemas ──────────────────────────────────────────────────────────

VALID_ROLES = {"buyer", "landlord", "agent", "agency", "developer", "admin"}


class UserProfile(BaseModel):
    id: str
    email: Optional[str] = None
    full_name: Optional[str] = None
    phone: Optional[str] = None
    avatar_url: Optional[str] = None
    role: str = "buyer"
    user_id_code: Optional[str] = None
    is_verified: bool = False
    agency_id: Optional[str] = None
    biometric_enabled: bool = False
    city: str = "All Cities"
    language: str = "English"
    created_at: Optional[str] = None
    updated_at: Optional[str] = None


class ProfileUpdate(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None
    avatar_url: Optional[str] = None
    city: Optional[str] = None
    language: Optional[str] = None


class RoleUpdate(BaseModel):
    role: str

    @field_validator("role")
    @classmethod
    def validate_role(cls, v: str) -> str:
        if v not in VALID_ROLES:
            raise ValueError(f"Role must be one of: {', '.join(VALID_ROLES)}")
        return v


class BiometricUpdate(BaseModel):
    enabled: bool
