"""
routers/auth.py — /api/v1/auth endpoints
POST /register, /login, /google, /logout
GET  /me
PUT  /me, /me/role
POST /me/biometric
"""

import logging
import httpx
from fastapi import APIRouter, HTTPException, Depends, status
from app.schemas.auth import (
    RegisterRequest, LoginRequest, GoogleAuthRequest,
    TokenResponse, ProfileUpdate, RoleUpdate, BiometricUpdate, UserProfile,
)
from app.services.supabase_client import get_supabase, get_supabase_admin
from app.services.auth_service import create_access_token, get_or_create_profile
from app.middleware.auth_middleware import get_current_user
from app.config import settings

router = APIRouter()
log = logging.getLogger(__name__)


# ─── helpers ──────────────────────────────────────────────────────────────────

def _find_user_by_email(admin, email: str):
    """Find an existing Supabase auth user by email."""
    # Method 1: list_users (works on most supabase-py versions)
    try:
        users = admin.auth.admin.list_users()
        if users:
            for u in users:
                if getattr(u, "email", None) == email:
                    return u
    except Exception as e:
        log.warning(f"list_users method 1 failed: {e}")

    # Method 2: list_users with page param
    try:
        users = admin.auth.admin.list_users(page=1, per_page=1000)
        if users:
            for u in users:
                if getattr(u, "email", None) == email:
                    return u
    except Exception as e:
        log.warning(f"list_users method 2 failed: {e}")

    return None


def _build_user_profile(profile: dict) -> UserProfile:
    """Safely build UserProfile, filling missing fields with defaults."""
    return UserProfile(
        id=profile.get("id", ""),
        email=profile.get("email"),
        full_name=profile.get("full_name"),
        phone=profile.get("phone"),
        avatar_url=profile.get("avatar_url"),
        role=profile.get("role", "buyer"),
        user_id_code=profile.get("user_id_code"),
        is_verified=profile.get("is_verified", False),
        agency_id=profile.get("agency_id"),
        biometric_enabled=profile.get("biometric_enabled", False),
        city=profile.get("city", "All Cities"),
        language=profile.get("language", "English"),
        created_at=profile.get("created_at"),
        updated_at=profile.get("updated_at"),
    )


# ─── Register ─────────────────────────────────────────────────────────────────

@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def register(body: RegisterRequest):
    admin = get_supabase_admin()
    user_id = None

    # Step 1a: try admin API (auto-confirms email, no confirmation mail)
    try:
        result = admin.auth.admin.create_user({
            "email": body.email,
            "password": body.password,
            "email_confirm": True,
            "user_metadata": {"full_name": body.full_name},
        })
        if result.user:
            user_id = result.user.id
            log.info(f"register: admin.create_user OK → {user_id}")
    except Exception as e:
        err = str(e).lower()
        if "already been registered" in err or "already exists" in err or "duplicate" in err:
            raise HTTPException(status_code=400, detail="An account with this email already exists.")
        log.warning(f"register: admin.create_user failed ({e}), falling back to sign_up")

    # Step 1b: fallback — sign_up then auto-confirm via admin update
    if not user_id:
        try:
            sb = get_supabase()
            su = sb.auth.sign_up({"email": body.email, "password": body.password})
            if su.user:
                user_id = su.user.id
                log.info(f"register: sign_up OK → {user_id}")
                # auto-confirm so login works immediately
                try:
                    admin.auth.admin.update_user_by_id(user_id, {"email_confirm": True})
                except Exception as ce:
                    log.warning(f"register: auto-confirm failed (non-fatal): {ce}")
        except Exception as e:
            err = str(e).lower()
            if "already" in err:
                raise HTTPException(status_code=400, detail="An account with this email already exists.")
            raise HTTPException(status_code=400, detail=f"Registration failed: {e}")

    if not user_id:
        raise HTTPException(status_code=500, detail="Could not create user account.")

    # Step 2: create/fetch profile row
    try:
        profile = await get_or_create_profile(user_id, body.email, body.full_name)
    except Exception as e:
        log.error(f"register: profile error for {user_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Profile creation failed: {e}")

    # Step 3: persist full_name & phone (non-fatal)
    try:
        update_data = {"full_name": body.full_name}
        if body.phone:
            update_data["phone"] = body.phone
        admin.table("profiles").update(update_data).eq("id", user_id).execute()
        profile.update(update_data)
    except Exception as e:
        log.warning(f"register: profile update non-fatal: {e}")

    token = create_access_token({"sub": user_id, "role": profile.get("role", "buyer")})
    return TokenResponse(access_token=token, user=_build_user_profile(profile))


# ─── Login ────────────────────────────────────────────────────────────────────

@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest):
    if not body.email and not body.user_id_code:
        raise HTTPException(status_code=400, detail="Provide email or user_id_code.")

    admin = get_supabase_admin()
    email = body.email

    # Resolve user_id_code → email
    if body.user_id_code and not body.email:
        try:
            result = (
                admin.table("profiles")
                .select("id")
                .eq("user_id_code", body.user_id_code)
                .maybe_single()
                .execute()
            )
            if not result.data:
                raise HTTPException(status_code=404, detail="User ID code not found.")
            auth_user = admin.auth.admin.get_user_by_id(result.data["id"])
            if not auth_user.user:
                raise HTTPException(status_code=404, detail="Auth user not found.")
            email = auth_user.user.email
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"User lookup failed: {e}")

    # Step 1: sign in with Supabase
    sb = get_supabase()
    try:
        result = sb.auth.sign_in_with_password({"email": email, "password": body.password})
    except Exception as e:
        err = str(e).lower()
        if "email not confirmed" in err:
            # Auto-confirm the user and retry login
            try:
                admin.auth.admin.update_user_by_id(
                    _find_user_by_email(admin, email).id,
                    {"email_confirm": True}
                )
                result = sb.auth.sign_in_with_password({"email": email, "password": body.password})
            except Exception as e2:
                raise HTTPException(status_code=401, detail="Login failed. Please try again.")
        else:
            raise HTTPException(status_code=401, detail="Invalid email or password.")

    if not result.user:
        raise HTTPException(status_code=401, detail="Login failed.")

    user_id = result.user.id

    # Step 2: fetch/create profile
    try:
        profile = await get_or_create_profile(user_id, email)
    except Exception as e:
        log.error(f"Profile fetch failed for {user_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Profile error: {e}")

    token = create_access_token({"sub": user_id, "role": profile.get("role", "buyer")})
    return TokenResponse(access_token=token, user=_build_user_profile(profile))


# ─── Google OAuth ─────────────────────────────────────────────────────────────

@router.post("/google", response_model=TokenResponse)
async def google_auth(body: GoogleAuthRequest):
    # Step 1: verify token with Google
    try:
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.get(
                f"https://oauth2.googleapis.com/tokeninfo?id_token={body.id_token}"
            )
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"Could not reach Google: {e}")

    if resp.status_code != 200:
        raise HTTPException(status_code=401, detail=f"Google token invalid: {resp.text}")

    google_data = resp.json()
    email = google_data.get("email")
    full_name = google_data.get("name") or ""

    if not email:
        raise HTTPException(status_code=400, detail="Email not returned by Google.")

    # Audience check is a warning only — never block valid Google tokens
    configured_id = settings.GOOGLE_CLIENT_ID
    if configured_id:
        aud = google_data.get("aud", "")
        azp = google_data.get("azp", "")
        if configured_id not in (aud, azp):
            log.warning(f"Google aud mismatch: expected={configured_id} aud={aud} azp={azp}")

    admin = get_supabase_admin()
    user_id = None

    # Step 2a: try to create new auth user
    try:
        result = admin.auth.admin.create_user({
            "email": email,
            "email_confirm": True,
            "user_metadata": {"full_name": full_name},
        })
        if result and result.user:
            user_id = result.user.id
            log.info(f"Google: created user {user_id}")
    except Exception as e:
        log.info(f"Google: create_user failed ({e}), searching existing user")

    # Step 2b: user already exists — find by email
    if not user_id:
        existing = _find_user_by_email(admin, email)
        if existing:
            user_id = getattr(existing, "id", None)
            log.info(f"Google: found existing user {user_id}")

    # Step 2c: last resort — create without password and get id from exception message
    if not user_id:
        log.error(f"Google: could not find user for {email}, attempting recovery")
        raise HTTPException(
            status_code=500,
            detail=f"Could not create or find account for {email}. Please try again."
        )

    # Step 3: fetch/create profile (auth_service handles None results safely)
    try:
        profile = await get_or_create_profile(user_id, email, full_name)
    except Exception as e:
        log.error(f"Google profile error {user_id}: {e}")
        # Return minimal profile so app can still navigate
        profile = {"id": user_id, "role": "buyer", "full_name": full_name}

    token = create_access_token({"sub": user_id, "role": profile.get("role", "buyer")})
    return TokenResponse(access_token=token, user=_build_user_profile(profile))


# ─── Logout ───────────────────────────────────────────────────────────────────

@router.post("/logout")
async def logout(current_user: dict = Depends(get_current_user)):
    return {"message": "Logged out successfully"}


# ─── /me GET ──────────────────────────────────────────────────────────────────

@router.get("/me", response_model=UserProfile)
async def get_me(current_user: dict = Depends(get_current_user)):
    return _build_user_profile(current_user)


# ─── /me PUT ──────────────────────────────────────────────────────────────────

@router.put("/me", response_model=UserProfile)
async def update_me(body: ProfileUpdate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    update_data = body.model_dump(exclude_none=True)
    if not update_data:
        return _build_user_profile(current_user)
    result = admin.table("profiles").update(update_data).eq("id", current_user["id"]).execute()
    updated = result.data[0] if result.data else current_user
    return _build_user_profile(updated)


# ─── /me/role PUT ─────────────────────────────────────────────────────────────

@router.put("/me/role", response_model=UserProfile)
async def set_role(body: RoleUpdate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = admin.table("profiles").update({"role": body.role}).eq("id", current_user["id"]).execute()
    updated = result.data[0] if result.data else {**current_user, "role": body.role}
    return _build_user_profile(updated)


# ─── /me/biometric POST ───────────────────────────────────────────────────────

@router.post("/me/biometric", response_model=UserProfile)
async def toggle_biometric(body: BiometricUpdate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = admin.table("profiles").update({"biometric_enabled": body.enabled}).eq("id", current_user["id"]).execute()
    updated = result.data[0] if result.data else {**current_user, "biometric_enabled": body.enabled}
    return _build_user_profile(updated)
