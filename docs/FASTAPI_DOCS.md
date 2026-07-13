# Real Estate App — FastAPI Backend Documentation
# Tech: Python 3.11 + FastAPI + Supabase (PostgreSQL + Auth + Storage)
# Base URL: http://localhost:8000/api/v1
# Swagger UI: http://localhost:8000/docs
# Last Updated: 2026-03-31

---

## Table of Contents

1. [Project Setup](#1-project-setup)
2. [Project Structure](#2-project-structure)
3. [Environment Variables](#3-environment-variables)
4. [Authentication Flow](#4-authentication-flow)
5. [Auth Endpoints — /api/v1/auth](#5-auth-endpoints)
6. [Properties Endpoints — /api/v1/properties](#6-properties-endpoints)
7. [Bookings Endpoints — /api/v1/bookings](#7-bookings-endpoints)
8. [Saved Properties Endpoints — /api/v1/saved](#8-saved-properties-endpoints)
9. [Saved Searches Endpoints — /api/v1/searches](#9-saved-searches-endpoints)
10. [Reviews Endpoints — /api/v1/reviews](#10-reviews-endpoints)
11. [Agencies Endpoints — /api/v1/agencies](#11-agencies-endpoints)
12. [Users Endpoints — /api/v1/users](#12-users-endpoints)
13. [Request & Response Schemas](#13-request--response-schemas)
14. [Filter Query Parameters](#14-filter-query-parameters)
15. [Database Schema](#15-database-schema)
16. [RLS Policies](#16-rls-policies)
17. [Supabase Storage — Image Upload](#17-supabase-storage--image-upload)
18. [Error Handling](#18-error-handling)
19. [Middleware](#19-middleware)
20. [Amenities Master List (Enum)](#20-amenities-master-list-enum)
21. [Property Sub-Categories](#21-property-sub-categories)
22. [requirements.txt](#22-requirementstxt)
23. [Dockerfile](#23-dockerfile)

---

## 1. Project Setup

```bash
cd backend
python -m venv venv

# Activate virtualenv
source venv/bin/activate          # Linux / macOS
venv\Scripts\activate             # Windows

pip install -r requirements.txt
cp .env.example .env              # Fill in your Supabase + secret keys

# Run dev server
uvicorn app.main:app --reload --port 8000

# Swagger docs
open http://localhost:8000/docs

# Run tests
pytest tests/ -v
```

---

## 2. Project Structure

```
backend/
├── app/
│   ├── main.py                  # FastAPI app init, router registration
│   ├── config.py                # Pydantic Settings (env vars)
│   ├── database.py              # Supabase client initialization
│   ├── models/                  # SQLAlchemy-like data models (Pydantic)
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── property.py
│   │   ├── booking.py
│   │   ├── review.py
│   │   └── agency.py
│   ├── routers/                 # FastAPI APIRouter per resource
│   │   ├── __init__.py
│   │   ├── auth.py
│   │   ├── properties.py
│   │   ├── bookings.py
│   │   ├── users.py
│   │   ├── reviews.py
│   │   ├── agencies.py
│   │   └── saved.py
│   ├── schemas/                 # Pydantic request/response schemas
│   │   ├── __init__.py
│   │   ├── auth.py
│   │   ├── property.py
│   │   ├── user.py
│   │   └── booking.py
│   ├── services/                # Business logic layer
│   │   ├── __init__.py
│   │   ├── supabase_client.py
│   │   ├── auth_service.py
│   │   ├── property_service.py
│   │   ├── storage_service.py
│   │   └── role_service.py
│   └── middleware/
│       ├── auth_middleware.py   # JWT decode + user injection
│       └── cors_middleware.py   # CORS config
├── tests/
│   ├── test_auth.py
│   ├── test_properties.py
│   └── test_bookings.py
├── requirements.txt
├── .env.example
└── Dockerfile
```

---

## 3. Environment Variables

### backend/.env

```dotenv
# Supabase
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key   # NEVER expose to Android

# JWT
SECRET_KEY=your-super-secret-key-min-32-chars
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=1440    # 24 hours

# Google OAuth
GOOGLE_CLIENT_ID=your-google-web-client-id

# App
DEBUG=True
ALLOWED_ORIGINS=http://localhost,http://10.0.2.2
```

### .env.example (committed to repo — no real values)

```dotenv
SUPABASE_URL=
SUPABASE_ANON_KEY=
SUPABASE_SERVICE_ROLE_KEY=
SECRET_KEY=
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=1440
GOOGLE_CLIENT_ID=
DEBUG=False
ALLOWED_ORIGINS=http://localhost
```

---

## 4. Authentication Flow

### Email / Password Registration
```
POST /api/v1/auth/register
→ Supabase Auth signup (email + password)
→ Supabase triggers creates row in `profiles` (via DB trigger or API call)
→ role = 'buyer' (default)
→ user_id_code auto-generated: "RE-YYYYXXXX"
→ Returns: { access_token, user_id_code, role }
```

### Login
```
POST /api/v1/auth/login
→ Supabase Auth signIn
→ Backend wraps Supabase JWT + injects custom_claims { role, user_id_code }
→ Returns: { access_token, token_type: "bearer" }
```

### Google OAuth
```
POST /api/v1/auth/google   { id_token: "..." }
→ Verify Google ID token via Google's public keys
→ Supabase Auth upsert (create or find existing user)
→ Create profiles row if first time (temp password generated + hashed)
→ Store user_id_code
→ Returns: { access_token, is_new_user: bool }
```

### JWT Validation (all protected routes)
```
Authorization: Bearer <token>
→ auth_middleware.py decodes JWT
→ Injects current_user: dict into request state
→ Raises HTTP 401 if invalid/expired
```

### Role Hierarchy
```
buyer < landlord < agent < agency < developer < admin
```

---

## 5. Auth Endpoints

**Base path:** `/api/v1/auth`

---

### POST /register

Register with email + password.

**Request Body:**
```json
{
  "full_name": "John Doe",
  "email": "john@example.com",
  "phone": "+971501234567",
  "password": "SecurePass123!"
}
```

**Response 201:**
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "user_id_code": "RE-20261234",
  "role": "buyer",
  "is_new_user": true
}
```

**Errors:** `400` email already registered | `422` validation error

---

### POST /login

Login with email + password.

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response 200:**
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "user_id_code": "RE-20261234",
  "role": "landlord"
}
```

**Errors:** `401` invalid credentials

---

### POST /google

Exchange Google ID token for app JWT.

**Request Body:**
```json
{
  "id_token": "google-id-token-from-android"
}
```

**Response 200:**
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "user_id_code": "RE-20260001",
  "role": "buyer",
  "is_new_user": true
}
```

---

### POST /logout

Invalidate session token.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
{ "message": "Logged out successfully" }
```

---

### GET /me

Get current authenticated user's profile.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
{
  "id": "uuid",
  "full_name": "John Doe",
  "email": "john@example.com",
  "phone": "+971501234567",
  "avatar_url": "https://storage.supabase.co/...",
  "role": "landlord",
  "user_id_code": "RE-20261234",
  "is_verified": false,
  "agency_id": null,
  "biometric_enabled": false,
  "city": "Dubai",
  "language": "English",
  "created_at": "2026-03-01T10:00:00Z"
}
```

---

### PUT /me

Update profile fields.

**Headers:** `Authorization: Bearer <token>`

**Request Body (all optional):**
```json
{
  "full_name": "John Updated",
  "phone": "+971509876543",
  "avatar_url": "https://...",
  "city": "Abu Dhabi",
  "language": "Arabic"
}
```

**Response 200:** Updated profile object

---

### PUT /me/role

Set user role (called after Role Selection screen).

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "role": "landlord"
}
```

**Allowed values:** `buyer` | `landlord` | `agent` | `agency` | `developer`

**Response 200:**
```json
{ "role": "landlord", "message": "Role updated successfully" }
```

---

### POST /me/biometric

Toggle biometric login flag.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{ "enabled": true }
```

**Response 200:**
```json
{ "biometric_enabled": true }
```

---

## 6. Properties Endpoints

**Base path:** `/api/v1/properties`

---

### GET /

List properties with filters and pagination.

**Auth:** Optional (public listing; authenticated users get saved status)

**Query Parameters:** See [Section 14 — Filter Query Parameters](#14-filter-query-parameters)

**Response 200:**
```json
{
  "results": [
    {
      "id": "uuid",
      "title": "Luxury 2BR Apartment in Downtown",
      "price": 85000.00,
      "price_frequency": "yearly",
      "property_type": "apartment",
      "listing_type": "rent",
      "bedrooms": 2,
      "bathrooms": 2,
      "area_sqft": 1200.0,
      "city": "Dubai",
      "neighborhood": "Downtown Dubai",
      "images": ["https://storage.supabase.co/..."],
      "is_verified": true,
      "is_featured": false,
      "reference_id": "DP-S-12345",
      "created_at": "2026-03-15T08:00:00Z"
    }
  ],
  "total": 1234,
  "page": 1,
  "limit": 20,
  "pages": 62
}
```

---

### GET /featured

Get featured property listings (for Home screen sections).

**Auth:** Public

**Query Params:** `listing_type`, `city`, `limit` (default 10)

**Response 200:** Same structure as `GET /` but filtered to `is_featured = true`

---

### GET /search

Autocomplete search suggestions.

**Auth:** Public

**Query Params:**
- `q` (string, required) — search query
- `limit` (int, default 10)

**Response 200:**
```json
{
  "suggestions": [
    { "type": "property", "text": "Luxury 2BR in Downtown", "id": "uuid" },
    { "type": "location", "text": "Downtown Dubai" },
    { "type": "category", "text": "Apartments in Dubai" }
  ]
}
```

---

### GET /{id}

Get full property detail.

**Auth:** Public

**Path Param:** `id` (UUID)

**Response 200:**
```json
{
  "id": "uuid",
  "owner_id": "uuid",
  "agency_id": null,
  "listed_by": "landlord",
  "title": "Luxury 2BR Apartment in Downtown",
  "description": "Beautiful apartment with stunning Burj Khalifa views...",
  "price": 85000.00,
  "price_frequency": "yearly",
  "property_type": "apartment",
  "listing_type": "rent",
  "bedrooms": 2,
  "bathrooms": 2,
  "area_sqft": 1200.0,
  "address": "Downtown Dubai, Sheikh Mohammed Bin Rashid Blvd",
  "neighborhood": "Downtown Dubai",
  "city": "Dubai",
  "latitude": 25.1972,
  "longitude": 55.2744,
  "images": ["https://...", "https://..."],
  "video_url": null,
  "amenities": ["BALCONY", "COVERED_PARKING", "SECURITY"],
  "furnishing": "furnished",
  "completion_status": "ready",
  "payment_plan": null,
  "handover_date": null,
  "developer_name": null,
  "permit_number": "RERA-2024-XXXXX",
  "rera_number": "1234567",
  "reference_id": "DP-S-12345",
  "brn_dld": null,
  "zone_name": "Business Bay",
  "is_verified": true,
  "is_featured": false,
  "status": "active",
  "created_at": "2026-03-15T08:00:00Z",
  "updated_at": "2026-03-15T08:00:00Z",
  "owner": {
    "full_name": "Ahmed Al-Farsi",
    "avatar_url": "https://...",
    "phone": "+971501234567",
    "is_verified": true
  }
}
```

**Errors:** `404` not found

---

### GET /{id}/similar

Get similar properties.

**Auth:** Public

**Query Params:** `limit` (default 6)

**Response 200:** Array of property cards (same as list item schema)

---

### POST /

Create a new property listing.

**Auth:** Required — role must be `landlord` | `agent` | `agency` | `developer`

**Request Body:**
```json
{
  "title": "Spacious 3BR Villa in Jumeirah",
  "description": "Newly renovated villa with private pool...",
  "price": 250000.00,
  "price_frequency": "yearly",
  "property_type": "villa",
  "listing_type": "rent",
  "bedrooms": 3,
  "bathrooms": 4,
  "area_sqft": 3500.0,
  "address": "Jumeirah 1, Dubai",
  "neighborhood": "Jumeirah",
  "city": "Dubai",
  "latitude": 25.2048,
  "longitude": 55.2708,
  "amenities": ["PRIVATE_POOL", "PRIVATE_GYM", "COVERED_PARKING"],
  "furnishing": "unfurnished",
  "completion_status": "ready",
  "listed_by": "landlord"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "reference_id": "DP-S-67890",
  "status": "active",
  "created_at": "2026-03-31T12:00:00Z"
}
```

**Errors:** `401` unauthorized | `403` forbidden (wrong role) | `422` validation error

---

### PUT /{id}

Update an existing listing.

**Auth:** Required — property owner or admin only

**Request Body:** Partial — any fields from POST body

**Response 200:** Updated property object

**Errors:** `403` not owner | `404` not found

---

### DELETE /{id}

Delete a property listing.

**Auth:** Required — property owner or admin only

**Response 204:** No content

---

### POST /{id}/images

Upload images for a property (max 20 total).

**Auth:** Required — property owner or admin

**Request:** `multipart/form-data`

```
files: List[UploadFile]   (JPEG/PNG/WebP, max 5MB each)
```

**Response 200:**
```json
{
  "uploaded": 3,
  "image_urls": [
    "https://storage.supabase.co/...",
    "https://storage.supabase.co/...",
    "https://storage.supabase.co/..."
  ],
  "total_images": 5
}
```

**Errors:** `400` exceeds 20 image limit | `413` file too large

---

## 7. Bookings Endpoints

**Base path:** `/api/v1/bookings`

All endpoints require `Authorization: Bearer <token>`.

---

### POST /

Create a visit booking.

**Request Body:**
```json
{
  "property_id": "uuid",
  "visit_date": "2026-04-15",
  "visit_time": "10:00:00",
  "message": "I'd like to see the master bedroom specifically."
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "property_id": "uuid",
  "buyer_id": "uuid",
  "visit_date": "2026-04-15",
  "visit_time": "10:00:00",
  "status": "pending",
  "message": "I'd like to see the master bedroom specifically.",
  "created_at": "2026-03-31T12:00:00Z"
}
```

---

### GET /

Get my bookings (buyer's booked visits).

**Query Params:**
- `status` — filter by `pending` | `confirmed` | `cancelled` | `completed`
- `page`, `limit`

**Response 200:**
```json
{
  "results": [ { ...booking objects with nested property card... } ],
  "total": 5
}
```

---

### PUT /{id}/status

Confirm or cancel a booking (landlord/agent confirms; buyer can cancel).

**Request Body:**
```json
{ "status": "confirmed" }
```

**Allowed values:** `confirmed` | `cancelled` | `completed`

**Response 200:** Updated booking object

---

### DELETE /{id}

Cancel and delete a booking (buyer only).

**Response 204:** No content

---

## 8. Saved Properties Endpoints

**Base path:** `/api/v1/saved`

All endpoints require `Authorization: Bearer <token>`.

---

### GET /

Get all saved (favorited) properties for the current user.

**Query Params:** `page`, `limit`

**Response 200:**
```json
{
  "results": [ { ...property card objects... } ],
  "total": 12
}
```

---

### POST /{property_id}

Save (favorite) a property.

**Path Param:** `property_id` (UUID)

**Response 201:**
```json
{ "saved": true, "property_id": "uuid" }
```

**Errors:** `409` already saved

---

### DELETE /{property_id}

Remove a saved property.

**Path Param:** `property_id` (UUID)

**Response 204:** No content

---

## 9. Saved Searches Endpoints

**Base path:** `/api/v1/searches`

All endpoints require `Authorization: Bearer <token>`.

Used for "My Searches" in Menu and "Recent Searches" on Home screen.

---

### GET /

Get all saved searches for the current user.

**Response 200:**
```json
{
  "results": [
    {
      "id": "uuid",
      "label": "All Residential",
      "listing_type": "Property for Rent",
      "filters": {
        "city": "Dubai",
        "property_type": "apartment",
        "min_price": 50000,
        "max_price": 150000
      },
      "thumbnail_url": "https://...",
      "created_at": "2026-03-20T09:00:00Z"
    }
  ]
}
```

---

### POST /

Save a new search.

**Request Body:**
```json
{
  "label": "All Residential",
  "listing_type": "Property for Rent",
  "filters": {
    "city": "Dubai",
    "property_type": "apartment",
    "bedrooms": 2
  },
  "thumbnail_url": "https://..."
}
```

**Response 201:** Saved search object

---

### DELETE /{id}

Delete a saved search.

**Response 204:** No content

---

## 10. Reviews Endpoints

**Base path:** `/api/v1/reviews`

---

### GET /properties/{id}/reviews

Get all reviews for a property.

**Auth:** Public

**Path Param:** `id` (property UUID)

**Query Params:** `page`, `limit`

**Response 200:**
```json
{
  "average_rating": 4.3,
  "total_reviews": 27,
  "results": [
    {
      "id": "uuid",
      "reviewer": { "full_name": "Sara M.", "avatar_url": "https://..." },
      "rating": 5,
      "comment": "Beautiful apartment, great location!",
      "created_at": "2026-02-10T14:00:00Z"
    }
  ]
}
```

---

### POST /

Add a review for a property.

**Auth:** Required — any authenticated user (buyer)

**Request Body:**
```json
{
  "property_id": "uuid",
  "rating": 4,
  "comment": "Great value for money. Very clean."
}
```

**Validation:**
- `rating`: integer 1–5
- `comment`: max 1000 characters

**Response 201:** Review object

**Errors:** `409` already reviewed this property

---

## 11. Agencies Endpoints

**Base path:** `/api/v1/agencies`

---

### GET /

List and search agencies.

**Auth:** Public

**Query Params:**
- `q` — search by name
- `city` — filter by city
- `page`, `limit`

**Response 200:**
```json
{
  "results": [
    {
      "id": "uuid",
      "name": "Betterhomes",
      "logo_url": "https://...",
      "license_number": "DED-XXXXX",
      "rera_number": "1234",
      "phone": "+97144123456",
      "email": "info@betterhomes.com",
      "city": "Dubai",
      "is_verified": true,
      "listing_count": 450,
      "created_at": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 85
}
```

---

### GET /{id}

Get agency detail and its active listings.

**Auth:** Public

**Path Param:** `id` (UUID)

**Response 200:**
```json
{
  "agency": { ...full agency object... },
  "listings": {
    "results": [ { ...property cards... } ],
    "total": 450,
    "page": 1
  }
}
```

---

## 12. Users Endpoints

**Base path:** `/api/v1/users`

---

### GET /{id}/profile

Get a public user/agent profile.

**Auth:** Public

**Response 200:**
```json
{
  "id": "uuid",
  "full_name": "Ahmed Al-Farsi",
  "avatar_url": "https://...",
  "role": "agent",
  "is_verified": true,
  "agency_id": "uuid",
  "agency_name": "Betterhomes",
  "listing_count": 24,
  "joined_at": "2025-06-01T00:00:00Z"
}
```

---

## 13. Request & Response Schemas

### PropertyCreate (schemas/property.py)

```python
class PropertyCreate(BaseModel):
    title: str = Field(..., min_length=5, max_length=200)
    description: Optional[str] = Field(None, max_length=5000)
    price: float = Field(..., gt=0)
    price_frequency: Literal["yearly", "monthly", "weekly"] = "yearly"
    property_type: PropertyType          # enum
    listing_type: Literal["rent", "sale", "off_plan"]
    bedrooms: Optional[int] = Field(None, ge=0, le=20)
    bathrooms: Optional[int] = Field(None, ge=0, le=20)
    area_sqft: Optional[float] = Field(None, gt=0)
    address: Optional[str] = None
    neighborhood: Optional[str] = None
    city: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    amenities: Optional[List[AmenityEnum]] = []
    furnishing: Literal["furnished", "unfurnished", "semi"] = "unfurnished"
    completion_status: Optional[Literal["ready", "off_plan"]] = "ready"
    payment_plan: Optional[str] = None
    handover_date: Optional[date] = None
    developer_name: Optional[str] = None
    permit_number: Optional[str] = None
    rera_number: Optional[str] = None
    brn_dld: Optional[str] = None
    zone_name: Optional[str] = None
    listed_by: Literal["landlord", "agent", "agency", "developer"] = "landlord"
```

### UserRegister (schemas/auth.py)

```python
class UserRegister(BaseModel):
    full_name: str = Field(..., min_length=2, max_length=100)
    email: EmailStr
    phone: Optional[str] = None
    password: str = Field(..., min_length=8, max_length=128)
```

### BookingCreate (schemas/booking.py)

```python
class BookingCreate(BaseModel):
    property_id: UUID
    visit_date: date
    visit_time: time
    message: Optional[str] = Field(None, max_length=500)
```

### TokenResponse (schemas/auth.py)

```python
class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id_code: str
    role: str
    is_new_user: bool = False
```

---

## 14. Filter Query Parameters

Used with `GET /api/v1/properties/`

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `listing_type` | string | rent \| sale \| off_plan | `rent` |
| `property_type` | string | apartment \| villa \| townhouse \| penthouse \| hotel_apartment \| residential_building \| villa_compound \| residential_floor \| office \| shop \| warehouse \| land \| other | `apartment` |
| `city` | string | City name | `Dubai` |
| `neighborhood` | string | Neighborhood name | `Downtown Dubai` |
| `min_price` | float | Min price in AED | `50000` |
| `max_price` | float | Max price in AED | `200000` |
| `price_frequency` | string | yearly \| monthly \| weekly | `yearly` |
| `bedrooms` | int | Exact bedroom count (0 = Studio) | `2` |
| `bathrooms` | int | Exact bathroom count | `2` |
| `min_area` | float | Min area in sqft | `500` |
| `max_area` | float | Max area in sqft | `3000` |
| `furnishing` | string | furnished \| unfurnished \| semi | `furnished` |
| `completion_status` | string | ready \| off_plan | `ready` |
| `amenities` | array | List of amenity enum values | `BALCONY,PRIVATE_POOL` |
| `keyword` | string | Full-text search term | `marina view` |
| `listed_by` | string | landlord \| agent \| agency \| developer | `agent` |
| `agency_id` | UUID | Filter by agency | `uuid` |
| `verified_only` | bool | Only verified listings | `true` |
| `has_video` | bool | Only listings with video | `true` |
| `has_360` | bool | Only listings with 360 tour | `true` |
| `sort_by` | string | price_asc \| price_desc \| newest \| oldest | `newest` |
| `page` | int | Page number (1-indexed) | `1` |
| `limit` | int | Results per page (max 50) | `20` |

### Example Request
```
GET /api/v1/properties/?listing_type=rent&city=Dubai&property_type=apartment
    &bedrooms=2&min_price=50000&max_price=150000&amenities=BALCONY,SECURITY
    &sort_by=newest&page=1&limit=20
```

---

## 15. Database Schema

### profiles
```sql
CREATE TABLE profiles (
  id                UUID PRIMARY KEY REFERENCES auth.users(id),
  full_name         TEXT,
  phone             TEXT,
  avatar_url        TEXT,
  role              TEXT DEFAULT 'buyer',
  user_id_code      TEXT UNIQUE,
  is_verified       BOOLEAN DEFAULT FALSE,
  agency_id         UUID,
  biometric_enabled BOOLEAN DEFAULT FALSE,
  city              TEXT DEFAULT 'All Cities',
  language          TEXT DEFAULT 'English',
  created_at        TIMESTAMPTZ DEFAULT NOW(),
  updated_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Auto-generate user_id_code on insert
CREATE OR REPLACE FUNCTION generate_user_id_code()
RETURNS TRIGGER AS $$
BEGIN
  NEW.user_id_code := 'RE-' || TO_CHAR(NOW(), 'YYYY') || LPAD(FLOOR(RANDOM() * 9999)::TEXT, 4, '0');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_user_id_code
  BEFORE INSERT ON profiles
  FOR EACH ROW WHEN (NEW.user_id_code IS NULL)
  EXECUTE FUNCTION generate_user_id_code();
```

### agencies
```sql
CREATE TABLE agencies (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name             TEXT NOT NULL,
  logo_url         TEXT,
  license_number   TEXT,
  rera_number      TEXT,
  phone            TEXT,
  email            TEXT,
  city             TEXT,
  is_verified      BOOLEAN DEFAULT FALSE,
  created_at       TIMESTAMPTZ DEFAULT NOW()
);
```

### properties
```sql
CREATE TABLE properties (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id            UUID REFERENCES profiles(id),
  agency_id           UUID REFERENCES agencies(id),
  listed_by           TEXT DEFAULT 'landlord',
  title               TEXT NOT NULL,
  description         TEXT,
  price               NUMERIC(15,2) NOT NULL,
  price_frequency     TEXT DEFAULT 'yearly',
  property_type       TEXT,
  listing_type        TEXT,
  bedrooms            INT,
  bathrooms           INT,
  area_sqft           NUMERIC(10,2),
  address             TEXT,
  neighborhood        TEXT,
  city                TEXT,
  latitude            FLOAT,
  longitude           FLOAT,
  images              TEXT[],
  video_url           TEXT,
  amenities           TEXT[],
  furnishing          TEXT DEFAULT 'unfurnished',
  completion_status   TEXT,
  payment_plan        TEXT,
  handover_date       DATE,
  developer_name      TEXT,
  permit_number       TEXT,
  rera_number         TEXT,
  reference_id        TEXT UNIQUE,
  brn_dld             TEXT,
  zone_name           TEXT,
  is_verified         BOOLEAN DEFAULT FALSE,
  is_featured         BOOLEAN DEFAULT FALSE,
  status              TEXT DEFAULT 'active',
  created_at          TIMESTAMPTZ DEFAULT NOW(),
  updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Auto-generate reference_id on insert
CREATE OR REPLACE FUNCTION generate_reference_id()
RETURNS TRIGGER AS $$
BEGIN
  NEW.reference_id := 'DP-S-' || LPAD(FLOOR(RANDOM() * 99999)::TEXT, 5, '0');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_reference_id
  BEFORE INSERT ON properties
  FOR EACH ROW WHEN (NEW.reference_id IS NULL)
  EXECUTE FUNCTION generate_reference_id();
```

### bookings
```sql
CREATE TABLE bookings (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id  UUID REFERENCES properties(id),
  buyer_id     UUID REFERENCES profiles(id),
  visit_date   DATE,
  visit_time   TIME,
  status       TEXT DEFAULT 'pending',
  message      TEXT,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);
```

### reviews
```sql
CREATE TABLE reviews (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id  UUID REFERENCES properties(id),
  reviewer_id  UUID REFERENCES profiles(id),
  rating       INT CHECK (rating BETWEEN 1 AND 5),
  comment      TEXT,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(property_id, reviewer_id)   -- one review per user per property
);
```

### saved_properties
```sql
CREATE TABLE saved_properties (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID REFERENCES profiles(id),
  property_id  UUID REFERENCES properties(id),
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, property_id)
);
```

### saved_searches
```sql
CREATE TABLE saved_searches (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID REFERENCES profiles(id),
  label         TEXT,
  listing_type  TEXT,
  filters       JSONB,
  thumbnail_url TEXT,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 16. RLS Policies

Run these in Supabase SQL editor after creating tables.

```sql
-- Profiles: users can only read/write their own profile
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_profile_read"  ON profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "own_profile_write" ON profiles FOR ALL    USING (auth.uid() = id);

-- Properties: anyone can read active listings; owners can write
ALTER TABLE properties ENABLE ROW LEVEL SECURITY;
CREATE POLICY "public_read_active" ON properties FOR SELECT USING (status = 'active');
CREATE POLICY "owner_insert"       ON properties FOR INSERT WITH CHECK (auth.uid() = owner_id);
CREATE POLICY "owner_update"       ON properties FOR UPDATE USING (auth.uid() = owner_id);
CREATE POLICY "owner_delete"       ON properties FOR DELETE USING (auth.uid() = owner_id);

-- Bookings: buyers can read/write their own bookings
ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "buyer_own_bookings" ON bookings FOR ALL USING (auth.uid() = buyer_id);

-- Saved properties: users manage their own saved items
ALTER TABLE saved_properties ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_saved" ON saved_properties FOR ALL USING (auth.uid() = user_id);

-- Saved searches: users manage their own searches
ALTER TABLE saved_searches ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_searches" ON saved_searches FOR ALL USING (auth.uid() = user_id);

-- Reviews: public read; authenticated write (own reviews only)
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
CREATE POLICY "public_read_reviews"  ON reviews FOR SELECT USING (true);
CREATE POLICY "own_review_write"     ON reviews FOR INSERT WITH CHECK (auth.uid() = reviewer_id);
CREATE POLICY "own_review_delete"    ON reviews FOR DELETE USING (auth.uid() = reviewer_id);

-- Agencies: public read
ALTER TABLE agencies ENABLE ROW LEVEL SECURITY;
CREATE POLICY "public_read_agencies" ON agencies FOR SELECT USING (true);
```

---

## 17. Supabase Storage — Image Upload

**Bucket name:** `property-images` (public bucket)

### Upload Flow (services/storage_service.py)
```python
async def upload_property_image(
    file: UploadFile,
    property_id: str,
    index: int
) -> str:
    """
    Uploads image to Supabase Storage.
    Returns public URL.
    """
    ext = file.filename.split(".")[-1].lower()   # jpg, png, webp
    path = f"properties/{property_id}/{index}.{ext}"

    content = await file.read()
    supabase.storage.from_("property-images").upload(path, content)

    public_url = supabase.storage.from_("property-images").get_public_url(path)
    return public_url
```

### Storage Rules
- Max file size: **5 MB** per image
- Max 20 images per property
- Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`
- Path format: `properties/{property_id}/{0..19}.{ext}`

---

## 18. Error Handling

All errors return a consistent JSON body:

```json
{
  "detail": "Human-readable error message",
  "code": "ERROR_CODE",
  "field": "field_name_if_validation_error"
}
```

### Standard HTTP Status Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | OK | Successful GET / PUT |
| 201 | Created | Successful POST |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Invalid input, business rule violation |
| 401 | Unauthorized | Missing or expired JWT |
| 403 | Forbidden | Authenticated but wrong role/ownership |
| 404 | Not Found | Resource does not exist |
| 409 | Conflict | Duplicate (already saved, already reviewed) |
| 413 | Payload Too Large | File upload exceeds size limit |
| 422 | Unprocessable Entity | Pydantic validation failure |
| 500 | Internal Server Error | Unexpected server error |

### Global Exception Handler (main.py)
```python
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail, "code": str(exc.status_code)}
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    return JSONResponse(
        status_code=422,
        content={"detail": "Validation error", "errors": exc.errors()}
    )
```

---

## 19. Middleware

### CORS (middleware/cors_middleware.py)

```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS.split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

### Auth Middleware (middleware/auth_middleware.py)

```python
async def get_current_user(token: str = Depends(oauth2_scheme)) -> dict:
    """
    Decodes JWT, returns user payload.
    Raises HTTP 401 if invalid or expired.
    """
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        user_id = payload.get("sub")
        if not user_id:
            raise credentials_exception
        return payload
    except JWTError:
        raise credentials_exception

async def require_role(allowed_roles: List[str]):
    """Dependency factory to restrict endpoint by role."""
    async def role_checker(current_user: dict = Depends(get_current_user)):
        if current_user.get("role") not in allowed_roles:
            raise HTTPException(status_code=403, detail="Insufficient permissions")
        return current_user
    return role_checker
```

### Usage in Router
```python
@router.post("/", status_code=201)
async def create_property(
    data: PropertyCreate,
    current_user: dict = Depends(
        require_role(["landlord", "agent", "agency", "developer"])
    )
):
    ...
```

---

## 20. Amenities Master List (Enum)

**File:** `models/property.py` (or `schemas/property.py`)

```python
from enum import Enum

class AmenityEnum(str, Enum):
    MAIDS_ROOM                 = "MAIDS_ROOM"
    STUDY                      = "STUDY"
    CENTRAL_AC_HEATING         = "CENTRAL_AC_HEATING"
    BALCONY                    = "BALCONY"
    PRIVATE_GARDEN             = "PRIVATE_GARDEN"
    PRIVATE_POOL               = "PRIVATE_POOL"
    PRIVATE_GYM                = "PRIVATE_GYM"
    PRIVATE_JACUZZI            = "PRIVATE_JACUZZI"
    SHARED_POOL                = "SHARED_POOL"
    SHARED_SPA                 = "SHARED_SPA"
    SHARED_GYM                 = "SHARED_GYM"
    SECURITY                   = "SECURITY"
    CONCIERGE_SERVICE          = "CONCIERGE_SERVICE"
    MAID_SERVICE               = "MAID_SERVICE"
    COVERED_PARKING            = "COVERED_PARKING"
    BUILTIN_WARDROBES          = "BUILTIN_WARDROBES"
    WALKIN_CLOSET              = "WALKIN_CLOSET"
    BUILTIN_KITCHEN_APPLIANCES = "BUILTIN_KITCHEN_APPLIANCES"
    VIEW_OF_WATER              = "VIEW_OF_WATER"
    VIEW_OF_LANDMARK           = "VIEW_OF_LANDMARK"
    PETS_ALLOWED               = "PETS_ALLOWED"
    DOUBLE_GLAZED_WINDOWS      = "DOUBLE_GLAZED_WINDOWS"
    DAY_CARE_CENTER            = "DAY_CARE_CENTER"
    ELECTRICITY_BACKUP         = "ELECTRICITY_BACKUP"
    FIRST_AID_MEDICAL_CENTER   = "FIRST_AID_MEDICAL_CENTER"
    SERVICE_ELEVATORS          = "SERVICE_ELEVATORS"
    PRAYER_ROOM                = "PRAYER_ROOM"
    LAUNDRY_ROOM               = "LAUNDRY_ROOM"
```

> **Critical:** These enum values must be identical in Android (`enum class Amenity`) and stored in the DB `amenities TEXT[]` column for filter queries to work correctly.

---

## 21. Property Sub-Categories

### Property for Rent — Residential
`apartment` | `villa` | `townhouse` | `penthouse` | `hotel_apartment` | `residential_building` | `villa_compound` | `residential_floor`

### Property for Rent — Commercial
`office` | `shop` | `warehouse` | `labour_camp` | `commercial_building` | `commercial_floor` | `commercial_villa` | `factory` | `other`

### Property for Sale — Residential
`apartment` | `villa` | `townhouse` | `penthouse` | `hotel_apartment` | `residential_building` | `villa_compound` | `residential_floor`

### Property for Sale — Commercial
`office` | `shop` | `warehouse` | `labour_camp` | `commercial_building` | `commercial_floor` | `land` | `industrial_land` | `other`

---

## 22. requirements.txt

```
fastapi==0.111.0
uvicorn[standard]==0.29.0
supabase==2.4.0
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-multipart==0.0.9
pydantic==2.7.0
pydantic-settings==2.2.1
httpx==0.27.0
python-dotenv==1.0.1
pytest==8.1.1
pytest-asyncio==0.23.6
```

---

## 23. Dockerfile

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### docker-compose.yml (optional local dev)
```yaml
version: "3.9"
services:
  api:
    build: ./backend
    ports:
      - "8000:8000"
    env_file:
      - ./backend/.env
    volumes:
      - ./backend:/app
    command: uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## Quick Reference — All Endpoints

| Method | Path | Auth | Role |
|--------|------|------|------|
| POST | /api/v1/auth/register | No | — |
| POST | /api/v1/auth/login | No | — |
| POST | /api/v1/auth/google | No | — |
| POST | /api/v1/auth/logout | Yes | any |
| GET | /api/v1/auth/me | Yes | any |
| PUT | /api/v1/auth/me | Yes | any |
| PUT | /api/v1/auth/me/role | Yes | any |
| POST | /api/v1/auth/me/biometric | Yes | any |
| GET | /api/v1/properties/ | No | — |
| GET | /api/v1/properties/featured | No | — |
| GET | /api/v1/properties/search | No | — |
| GET | /api/v1/properties/{id} | No | — |
| GET | /api/v1/properties/{id}/similar | No | — |
| POST | /api/v1/properties/ | Yes | landlord/agent/agency/developer |
| PUT | /api/v1/properties/{id} | Yes | owner/admin |
| DELETE | /api/v1/properties/{id} | Yes | owner/admin |
| POST | /api/v1/properties/{id}/images | Yes | owner/admin |
| POST | /api/v1/bookings/ | Yes | buyer |
| GET | /api/v1/bookings/ | Yes | buyer |
| PUT | /api/v1/bookings/{id}/status | Yes | owner |
| DELETE | /api/v1/bookings/{id} | Yes | buyer |
| GET | /api/v1/saved/ | Yes | any |
| POST | /api/v1/saved/{property_id} | Yes | any |
| DELETE | /api/v1/saved/{property_id} | Yes | any |
| GET | /api/v1/searches/ | Yes | any |
| POST | /api/v1/searches/ | Yes | any |
| DELETE | /api/v1/searches/{id} | Yes | any |
| GET | /api/v1/reviews/properties/{id}/reviews | No | — |
| POST | /api/v1/reviews/ | Yes | buyer |
| GET | /api/v1/agencies/ | No | — |
| GET | /api/v1/agencies/{id} | No | — |
| GET | /api/v1/users/{id}/profile | No | — |
