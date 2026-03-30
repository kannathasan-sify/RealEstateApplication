"""
services/role_service.py — Role-based access control helpers
"""

from fastapi import HTTPException, status

POSTER_ROLES = {"landlord", "agent", "agency", "developer", "admin"}
ADMIN_ROLES = {"admin"}


def require_role(user_role: str, allowed_roles: set, detail: str = "Insufficient permissions"):
    if user_role not in allowed_roles:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=detail)


def can_post_property(role: str) -> bool:
    return role in POSTER_ROLES


def is_admin(role: str) -> bool:
    return role in ADMIN_ROLES
