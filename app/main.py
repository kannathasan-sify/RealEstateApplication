"""
Real Estate App — FastAPI Backend
main.py — Application entry point
"""

import traceback
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager

from app.config import settings
from app.routers import auth, properties, bookings, saved, reviews, agencies
from app.routers.saved import searches_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events."""
    print(f"Real Estate API starting in {'DEBUG' if settings.DEBUG else 'PRODUCTION'} mode")
    yield
    print("Real Estate API shutting down")


app = FastAPI(
    title="Real Estate App API",
    description="Backend API for the Real Estate App — Dubizzle-style property marketplace",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# ─── CORS ────────────────────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── Routers ─────────────────────────────────────────────────────────────────
API_PREFIX = "/api/v1"

app.include_router(auth.router,       prefix=f"{API_PREFIX}/auth",       tags=["Auth"])
app.include_router(properties.router, prefix=f"{API_PREFIX}/properties", tags=["Properties"])
app.include_router(bookings.router,   prefix=f"{API_PREFIX}/bookings",   tags=["Bookings"])
app.include_router(saved.router,      prefix=f"{API_PREFIX}/saved",      tags=["Saved"])
app.include_router(searches_router,   prefix=f"{API_PREFIX}/searches",   tags=["Saved Searches"])
app.include_router(reviews.router,    prefix=f"{API_PREFIX}/reviews",    tags=["Reviews"])
app.include_router(agencies.router,   prefix=f"{API_PREFIX}/agencies",   tags=["Agencies"])


# ─── Global error handler (shows real traceback) ─────────────────────────────
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    tb = traceback.format_exc()
    print(tb)
    return JSONResponse(status_code=500, content={"detail": str(exc), "traceback": tb})


# ─── Root / Health check ─────────────────────────────────────────────────────
@app.get("/", tags=["Health"])
async def root():
    return {"message": "Real Estate App API", "version": "1.0.0", "status": "running"}


@app.get("/health", tags=["Health"])
async def health_check():
    return JSONResponse(
        content={"status": "healthy", "api_version": "1.0.0"},
        status_code=200,
    )
