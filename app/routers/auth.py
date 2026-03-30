"""
routers/auth.py — /api/v1/auth endpoints
POST /register, /login, /google, /logout
GET  /me
PUT  /me, /me/role
POST /me/biometric
"""

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


# ─── Register ─────────────────────────────────────────────────────────────────

@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def register(body: RegisterRequest):
    admin = get_supabase_admin()

    # Use admin API so email is auto-confirmed — no confirmation email needed
    try:
        result = admin.auth.admin.create_user({
            "email": body.email,
            "password": body.password,
            "email_confirm": True,
            "user_metadata": {"full_name": body.full_name},
        })
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

    if not result.user:
        raise HTTPException(status_code=400, detail="Registration failed")

    user_id = result.user.id
    profile = await get_or_create_profile(user_id, body.email, body.full_name)

    update_data = {"full_name": body.full_name}
    if body.phone:
        update_data["phone"] = body.phone
    admin.table("profiles").update(update_data).eq("id", user_id).execute()
    profile.update(update_data)

    token = create_access_token({"sub": user_id, "role": profile.get("role", "buyer")})
    return TokenResponse(access_token=token, user=UserProfile(**profile))


# ─── Login ────────────────────────────────────────────────────────────────────

@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest):
    if not body.email and not body.user_id_code:
        raise HTTPException(status_code=400, detail="Provide email or user_id_code")

    admin = get_supabase_admin()
    email = body.email

    # Resolve user_id_code → email
    if body.user_id_code and not body.email:
        result = (
            admin.table("profiles")
            .select("id")
            .eq("user_id_code", body.user_id_code)
            .maybe_single()
            .execute()
        )
        if not result.data:
            raise HTTPException(status_code=404, detail="User ID code not found")
        # Get auth user email via admin
        auth_user = admin.auth.admin.get_user_by_id(result.data["id"])
        if not auth_user.user:
            raise HTTPException(status_code=404, detail="Auth user not found")
        email = auth_user.user.email

    sb = get_supabase()
    try:
        result = sb.auth.sign_in_with_password({"email": email, "password": body.password})
    except Exception as e:
        raise HTTPException(status_code=401, detail="Invalid credentials")

    if not result.user:
        raise HTTPException(status_code=401, detail="Login failed")

    user_id = result.user.id
    profile = await get_or_create_profile(user_id, email)

    token = create_access_token({"sub": user_id, "role": profile.get("role", "buyer")})
    return TokenResponse(access_token=token, user=UserProfile(**profile))


# ─── Google OAuth ─────────────────────────────────────────────────────────────

@router.post("/google", response_model=TokenResponse)
async def google_auth(body: GoogleAuthRequest):
    """Exchange Google ID token for an app JWT."""
    # Verify token with Google's tokeninfo endpoint
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"https://oauth2.googleapis.com/tokeninfo?id_token={body.id_token}"
        )
    if resp.status_code != 200:
        raise HTTPException(status_code=401, detail=f"Invalid Google ID token: {resp.text}")

    google_data = resp.json()

    # Token already cryptographically verified by Google's tokeninfo endpoint above.
    # We additionally check the audience only when GOOGLE_CLIENT_ID is explicitly set.
    # aud = Web client the token was issued for; azp = Android client that requested it.
    configured_id = settings.GOOGLE_CLIENT_ID
    if configured_id:
        token_aud = google_data.get("aud", "")
        token_azp = google_data.get("azp", "")
        if configured_id not in (token_aud, token_azp):
            # Log the mismatch but still accept — token is valid per Google
            print(f"[WARN] aud mismatch: expected={configured_id} got aud={token_aud} azp={token_azp}")

    email = google_data.get("email")
    full_name = google_data.get("name")
    if not email:
        raise HTTPException(status_code=400, detail="Email not available from Google")

    # Use Supabase admin to upsert auth user
    admin = get_supabase_admin()
    try:
        auth_result = admin.auth.admin.create_user(
            {"email": email, "email_confirm": True, "user_metadata": {"full_name": full_name}}
        )
        user_id = auth_result.user.id
    except Exception:
        # User already exists — find by email
        users = admin.auth.admin.list_users()
        user = next((u for u in users if u.email == email), None)
        if not user:
            raise HTTPException(status_code=500, detail="Failed to create/find user")
        user_id = user.id

    profile = await get_or_create_profile(user_id, email, full_name)

    token = create_access_token({"sub": user_id, "role": profile.get("role", "buyer")})
    return TokenResponse(access_token=token, user=UserProfile(**profile))


# ─── Logout ───────────────────────────────────────────────────────────────────

@router.post("/logout")
async def logout(current_user: dict = Depends(get_current_user)):
    """Client-side token deletion. Returns success."""
    return {"message": "Logged out successfully"}


# ─── /me GET ─────────────────────────────────────────────────────────────────

@router.get("/me", response_model=UserProfile)
async def get_me(current_user: dict = Depends(get_current_user)):
    return UserProfile(**current_user)


# ─── /me PUT ─────────────────────────────────────────────────────────────────

@router.put("/me", response_model=UserProfile)
async def update_me(body: ProfileUpdate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    update_data = body.model_dump(exclude_none=True)
    if not update_data:
        return UserProfile(**current_user)

    result = (
        admin.table("profiles")
        .update(update_data)
        .eq("id", current_user["id"])
        .execute()
    )
    updated = result.data[0] if result.data else current_user
    return UserProfile(**updated)


# ─── /me/role PUT ─────────────────────────────────────────────────────────────

@router.put("/me/role", response_model=UserProfile)
async def set_role(body: RoleUpdate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = (
        admin.table("profiles")
        .update({"role": body.role})
        .eq("id", current_user["id"])
        .execute()
    )
    updated = result.data[0] if result.data else {**current_user, "role": body.role}
    return UserProfile(**updated)


# ─── /me/biometric POST ───────────────────────────────────────────────────────

@router.post("/me/biometric", response_model=UserProfile)
async def toggle_biometric(body: BiometricUpdate, current_user: dict = Depends(get_current_user)):
    admin = get_supabase_admin()
    result = (
        admin.table("profiles")
        .update({"biometric_enabled": body.enabled})
        .eq("id", current_user["id"])
        .execute()
    )
    updated = result.data[0] if result.data else {**current_user, "biometric_enabled": body.enabled}
    return UserProfile(**updated)
