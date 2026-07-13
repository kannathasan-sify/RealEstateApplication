# Real Estate App — CLAUDE.md (Full Development Guide)
# Brand: NestX  |  Original UI Reference: Dubizzle Android App
# Last Updated: 2026-07-13 (refreshed from actual codebase — see "Doc Drift Notes" at bottom)

---

## Tech Stack

| Layer        | Technology                              |
|--------------|-----------------------------------------|
| Frontend     | Kotlin + Jetpack Compose (Android)      |
| Backend      | Python 3.11 + FastAPI                   |
| Database     | Supabase (PostgreSQL + Auth + Storage)  |
| Auth         | Supabase Auth + Google OAuth            |
| HTTP Client  | Retrofit2 + OkHttp3                     |
| DI           | Hilt                                    |
| Architecture | MVVM + Repository Pattern               |
| Images       | Coil + Supabase Storage                 |
| Maps         | Google Maps SDK (Compose)               |
| Navigation   | Jetpack Navigation Component (incl. nested graphs) |

---

## App Scope (has grown beyond "listings app")

`backend/app/main.py` now describes the product as a **4-module marketplace**, not just a property listing app:

1. **Buy** — `listing_type = sale`
2. **Rent** — `listing_type = rent` (plus `holiday_stay`, `ground`)
3. **Construction Services** — `listing_type = contractor`, `work_category = construction`
4. **Maintenance Services** — `listing_type = contractor`/`maintenance`, `work_category = maintenance`

On top of property listings, the app now includes a services marketplace (service requests + contractor quotations), a paid subscription system, an admin back-office (user/payment/complaint management), an ads/sponsorship system with analytics, and Q&A discussions on listings.

---

## Project Directory Structure (actual, as of 2026-07-13)

```
Real_Estate_App/
├── CLAUDE.md
├── docs/
│   ├── ANDROID_SCREENS.md
│   └── FASTAPI_DOCS.md
│
├── backend/
│   ├── app/
│   │   ├── main.py                 ← mounts all routers (see API section)
│   │   ├── config.py
│   │   ├── models/
│   │   │   └── __init__.py         ← placeholder only, no ORM models; all persistence via Supabase
│   │   ├── routers/
│   │   │   ├── auth.py
│   │   │   ├── properties.py
│   │   │   ├── bookings.py
│   │   │   ├── saved.py            ← also defines `searches_router` (saved searches) in same file
│   │   │   ├── reviews.py
│   │   │   ├── agencies.py
│   │   │   ├── admin.py            ← approval workflow + user/payment/ticket/stats admin surface
│   │   │   ├── discussions.py      ← property Q&A, mounted under /properties/{id}/discussions
│   │   │   ├── service_requests.py ← construction/maintenance marketplace + quotations
│   │   │   ├── subscriptions.py    ← paid tiers (free/silver/gold/platinum/contractor)
│   │   │   ├── ad_interests.py     ← "I'm interested" leads on sponsored ads
│   │   │   ├── ad_analytics.py     ← ad impression/click/CTR analytics
│   │   │   └── support.py          ← user support tickets
│   │   ├── schemas/
│   │   │   ├── auth.py
│   │   │   ├── user.py             ← incl. SavedSearchCreate
│   │   │   ├── property.py
│   │   │   ├── booking.py
│   │   │   ├── subscription.py
│   │   │   ├── discussion.py
│   │   │   └── service_request.py
│   │   ├── services/
│   │   │   ├── supabase_client.py
│   │   │   ├── auth_service.py
│   │   │   ├── storage_service.py
│   │   │   └── role_service.py
│   │   └── middleware/
│   │       └── auth_middleware.py
│   ├── migrations/                 ← backend-local SQL, applied ad hoc (separate from supabase/migrations/)
│   │   ├── 003_module_redesign.sql     (service_requests, quotations, properties columns: youtube/instagram/rate_per_sqft/etc.)
│   │   ├── 004_subscription_system.sql (profiles.subscription_tier/expires_at)
│   │   ├── 005_admin_modules.sql       (payments, support_tickets)
│   │   └── 006_service_request_enhancements.sql (service_requests.urgency/preferred_date/contact_phone)
│   ├── requirements.txt
│   ├── .env / .env.example
│   ├── .mcp.json
│   └── run.py
│
├── android/
│   └── app/
│       ├── build.gradle.kts
│       ├── debug/app-debug.apk           ← already built at least once
│       ├── release/RealEstate_V10.0.apk  ← already released at least once (v10)
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── kotlin/com/realestate/app/
│           │   ├── MainActivity.kt
│           │   ├── RealEstateApp.kt
│           │   ├── data/
│           │   │   ├── api/            ApiService.kt, ApiClient.kt, AuthInterceptor.kt, AuthEventBus.kt
│           │   │   ├── local/          DataStoreManager.kt
│           │   │   ├── mock/           MockData.kt
│           │   │   ├── models/         User, Property, Booking, Review, Agency, Location,
│           │   │   │                   Discussion, ServiceRequest, AdBanner, AdInterest
│           │   │   └── repository/     AuthRepository, PropertyRepository, BookingRepository,
│           │   │                       UserRepository, AdInterestRepository,
│           │   │                       AdAnalyticsTracker, AdPersonalizationEngine
│           │   ├── di/                 AppModule, NetworkModule, RepositoryModule
│           │   ├── navigation/         AppNavGraph.kt, Screen.kt
│           │   └── ui/
│           │       ├── theme/          Theme.kt, Color.kt, Type.kt
│           │       ├── components/     PropertyCard, SearchBar, FilterChip, CategoryGrid,
│           │       │                   ImageCarousel, PriceRangeSlider, BottomNavBar, PropertyMapView
│           │       ├── splash/         SplashScreen.kt, SplashViewModel.kt
│           │       ├── onboarding/     OnboardingScreen.kt, OnboardingViewModel.kt
│           │       ├── auth/           LoginScreen, RegisterScreen, RoleSelectionScreen,
│           │       │                   BiometricScreen, AuthViewModel
│           │       ├── home/           HomeScreen, HomeViewModel, AdDetailDialog
│           │       ├── property/       PropertyListScreen, PropertyDetailScreen, PropertyFilterScreen,
│           │       │                   AmenitiesScreen, DistrictListScreen, PropertyViewModel
│           │       ├── post_ad/        PostAdCategoryScreen, PostAdCategoryForms, PostAdTitleScreen,
│           │       │                   PostAdSubCategoryScreen, PostAdRoleScreen, PostAdDetailsScreen,
│           │       │                   PostAdMapPickerScreen, PostAdViewModel
│           │       ├── saved/          SavedScreen.kt, SavedViewModel.kt
│           │       ├── mysearches/     MySearchesScreen.kt, MySearchesViewModel.kt   ← read-only currently
│           │       ├── myads/          MyAdsScreen.kt, MyAdsViewModel.kt
│           │       ├── booking/        BookingScreen, BookingViewModel, MyBookingsScreen, MyBookingsViewModel
│           │       ├── service_request/ PostServiceRequestScreen, ServiceRequestFeedScreen,
│           │       │                    ServiceRequestDetailScreen, ServiceRequestViewModel
│           │       ├── subscription/   SubscriptionPlansScreen.kt, SubscriptionViewModel.kt
│           │       ├── admin/          AdminDashboardScreen, AdminPropertyReviewScreen, AdminViewModel
│           │       ├── menu/           MenuScreen.kt, MenuViewModel.kt
│           │       ├── profile/        ProfileScreen.kt, ProfileViewModel.kt
│           │       ├── settings/       AccountSettingsScreen/ViewModel, NotificationSettingsScreen/ViewModel
│           │       ├── chat/           ChatListScreen.kt, ChatViewModel.kt
│           │       └── utils/          FormatUtils.kt
│           └── res/
│               ├── drawable/, mipmap-*/, values/, xml/
│
└── supabase/
    ├── migrations/
    │   ├── 001_profiles.sql
    │   ├── 002_agencies.sql
    │   ├── 003_properties.sql
    │   ├── 004_bookings.sql
    │   ├── 005_reviews.sql
    │   ├── 006_saved.sql              ← saved_properties AND saved_searches
    │   ├── 007_rls_policies.sql
    │   ├── 008_add_district.sql       ← district, approval_status, rejection_reason, agent_* fields, public_properties view
    │   ├── 008_backfill_agent_fields.sql  ← ⚠ duplicate "008" number, naming collision on disk
    │   ├── 009_fix_listed_by_constraint.sql
    │   ├── 010_add_agent_approval_fields.sql
    │   ├── 011_fix_furnishing_constraint.sql
    │   ├── 012_add_whatsapp_number.sql
    │   ├── 013_add_new_listing_types.sql   ← holiday_stay, contractor listing types + big property_type expansion
    │   ├── 014_add_ground_listing_type.sql ← ground listing type (sports grounds etc.)
    │   ├── 015_add_metadata_column.sql     ← properties.metadata JSONB (per-listing-type extra fields)
    │   ├── 016_ad_interests.sql
    │   └── 017_ad_analytics.sql
    └── seed.sql
```

---

## App Context — India / Tamil Nadu Focus

- **Region:** Tamil Nadu, India (38 districts as default browsing scope)
- **Currency:** Indian Rupees ₹ (INR) — all prices, filters, and display
- **Language:** English (Tamil language support planned)
- **Mock Data:** `BuildConfig.USE_MOCK_DATA = true` in debug; all ViewModels use MockData.kt

---

## User Roles System

> ⚠️ **Roles are in a messy transitional state across three layers — see "Doc Drift Notes" at the bottom before changing role logic.**

### Role Types — intended v2 model (India focus)
| Role | Description | Permissions |
|------|-------------|-------------|
| `buyer` | Default browsing/renting user | View, save, book visits, contact agent |
| `agent` | Licensed real estate agent | Post properties → admin approves before listing |
| `builder` | Developer / construction company | Post new projects → admin approves before listing |
| `admin` | Super admin | Approve/reject listings, manage users, payments, tickets |

Android's `UserRole` enum (`data/models/User.kt`) matches this cleanly: `BUYER, AGENT, BUILDER, ADMIN`.

The backend and DB, however, still carry legacy role values (see Doc Drift Notes) — `landlord`, `agency`, `developer` are still valid in `schemas/auth.py` `VALID_ROLES` and in the DB `profiles_role_check` constraint, and the backend's set does **not** include `builder`. Treat the table above as the target model, not the current enforced one.

### Admin Approval Workflow
1. Agent/Builder submits property → `approval_status = pending`, `status = inactive`
2. Admin reviews in AdminDashboard → **Approve**, **Reject** (reason required), or **Re-approve** (a previously rejected/adjusted listing, requires `proof_note`)
3. Approved → `approval_status = approved`, `status = active` → visible in public listing
4. Rejected → agent notified with reason; can edit & resubmit
5. **Public listing always filters `status = active AND approval_status = approved`** (enforced both via `public_read` RLS policy and the `public_properties` view)

### Role Assignment Flow
1. User registers via Email/Password or Google OAuth
2. System auto-creates `profiles` row, `role = buyer` by default
3. System generates unique `user_id_code` (e.g. `NX-TN-20261234`)
4. On FIRST login → **RoleSelectionScreen** displayed (3 options: Buyer / Agent / Builder)
5. User selects role → saved to `profiles.role`
6. Role injected into JWT `custom_claims`
7. `admin` role is **not self-assignable** — set via Supabase service role or by an existing admin (`PATCH /admin/users/{id}/role`)
8. Agent/Builder "+" FAB visible in bottom nav; Buyer sees upgrade prompt

### Alternative Login
- Google OAuth → stores `user_id_code` + hashed temp password
- Login with `user_id_code` + password OR via Google
- Change password: Menu → Security

---

## Screen Inventory (current navigation routes, from `Screen.kt` / `AppNavGraph.kt`)

| # | Screen | File | Notes |
|---|--------|------|-------|
| 1 | Splash | SplashScreen.kt | — |
| 2 | Onboarding (3 slides) | OnboardingScreen.kt | HorizontalPager |
| 3 | Login | LoginScreen.kt | — |
| 4 | Register | RegisterScreen.kt | — |
| 5 | Role Selection | RoleSelectionScreen.kt | — |
| 6 | Biometric prompt | BiometricScreen.kt | bottom sheet |
| 7 | Home | HomeScreen.kt | incl. AdBannerSection → AdDetailDialog |
| 8 | District List | DistrictListScreen.kt | **NEW** — Home → category tile → district picker → list |
| 9 | Property List | PropertyListScreen.kt | route: `property_list/{district}/{listingType}?workCategory=` |
| 10 | Filter | PropertyFilterScreen.kt | now mostly shown as in-screen overlay from list; standalone route kept for deep links |
| 11 | Amenities | AmenitiesScreen.kt | full checkbox list |
| 12 | Property Detail | PropertyDetailScreen.kt | incl. embedded Q&A/Discussions section |
| 13 | Menu | MenuScreen.kt | 2nd bottom-nav tab |
| 14 | Post Ad — Category | PostAdCategoryScreen.kt | entry to nested `post_ad_graph` |
| 15 | Post Ad — Title entry | PostAdTitleScreen.kt | — |
| 16 | Post Ad — Sub-category | PostAdSubCategoryScreen.kt | — |
| 17 | Post Ad — Landlord or Agent | PostAdRoleScreen.kt | — |
| 18 | Post Ad — Details form | PostAdDetailsScreen.kt | — |
| 19 | Post Ad — Map Picker | PostAdMapPickerScreen.kt | **NEW**, not in original flow |
| 20 | Saved / Favorites | SavedScreen.kt | reachable via Menu (no longer a bottom-nav tab) |
| 21 | My Searches | MySearchesScreen.kt | read-only list currently |
| 22 | My Ads | MyAdsScreen.kt | tabs: All/Pending/Approved/Rejected; also route `approved_ads` (pre-filtered) |
| 23 | Booking / Visit | BookingScreen.kt | route `booking/{propertyId}` |
| 24 | My Bookings | MyBookingsScreen.kt | tabs: My Visits / Received Inquiries |
| 25 | Chat List | ChatListScreen.kt | reachable via Menu (no longer a bottom-nav tab) |
| 26 | Profile | ProfileScreen.kt | — |
| 27 | Account Settings | AccountSettingsScreen.kt | — |
| 28 | Notification Settings | NotificationSettingsScreen.kt | — |
| 29 | Post Service Request | PostServiceRequestScreen.kt | **NEW** — construction/maintenance |
| 30 | Service Request Feed | ServiceRequestFeedScreen.kt | **NEW** — district/radius/category filters |
| 31 | Service Request Detail | ServiceRequestDetailScreen.kt | **NEW** — quotations list, accept/reject |
| 32 | Subscription Plans | SubscriptionPlansScreen.kt | **NEW** — free/silver/gold/platinum/contractor |
| 33 | Admin Dashboard | AdminDashboardScreen.kt | **NEW** — nested graph, shared `AdminViewModel`: Properties/Users/Payments/Complaints/Stats tabs |
| 34 | Admin Property Review | AdminPropertyReviewScreen.kt | **NEW** — approve/reject bottom sheet |

**Bottom nav has shrunk to 2 fixed tabs: Home and Menu.** Saved and Chat were removed from the nav bar (still reachable from Menu). The "+" Place-an-Ad button navigates directly into the nested `post_ad_graph`, not a tab.

Two nested Navigation Compose graphs share a single ViewModel instance across their steps:
- `post_ad_graph` — PostAdCategory → Title → SubCategory → Role → Details → MapPicker, all sharing one `PostAdViewModel`
- `admin_graph` — AdminDashboard ↔ AdminPropertyReview, sharing one `AdminViewModel`

---

## MODULE DESIGNS

*(Sections 1–7 below describe the original Dubizzle-referenced UI and are still accurate for those specific screens. New modules added since are documented after in "New Modules — Detailed".)*

### 1. Home Screen
Rounded search bar with rotating placeholder, category grid (Property for Rent/Sale, Off-Plan, Rooms, Motors, Jobs, Classifieds, Furniture, Community), "Got a verified badge yet?" banner, "What's new" scroller, "Recommended"/"Popular" horizontal scrollers, and (new) an **AdBannerSection** driven by `AdPersonalizationEngine` that opens `AdDetailDialog` on tap.

### 2. Amenities Screen
Dedicated full-screen amenities selector (search bar, 2-column checkbox grid, result count + Continue button). Master list below.

### 3. Menu Screen
Avatar/name/Get Verified card, Quick Actions (My Ads, My Searches), grouped settings list (Profile, Account Settings, Notification Settings, Security; City; Language; Blogs/Support/Call Us/Legal Hub/Advertising), Logout, build-version footer. Now also the entry point to Subscription Plans, My Bookings, and (for admins) the Admin Dashboard.

### 4. Place an Ad — Full Flow
6-card category grid → title entry ("Let's Go") → sub-category list w/ breadcrumb → Landlord/Agent choice → full details form (images, price, location via map picker, bedrooms/bathrooms/area, furnishing, amenities, description, contact toggles) → submit. Implemented as the `post_ad_graph` nested nav graph sharing one `PostAdViewModel`. The details form now also enforces subscription-tier listing/image limits (`PostAdViewModel.checkSubscriptionLimits()`), prompting an upgrade if exceeded.

### 5. Property Detail Screen
Image carousel (up to 20, "1/20" counter), heart/share icons, map view, agent card, Email/Call/WhatsApp contact bar. Now also includes an embedded **Q&A / Discussions** section (ask a question, see replies) backed by the Discussions module.

### 6. Filter Screen
Close/Filters/Reset header, Rent/Buy/Off-Plan tabs, scrollable filter sections, sticky "Show X Results" button.

### 7. Amenities Filter
Same amenities list reused across Filter screen, Property Detail, and Add Property amenities selection. Backend: `amenities` is a `TEXT[]` column on `properties`.

---

## New Modules — Detailed (not in original spec)

### 8. Subscriptions
Paid listing tiers, mock in-app checkout (no real payment gateway integration yet — `POST /subscriptions/upgrade` directly flips the tier and inserts a `payments` row).

| Tier | Max Listings | Max Images | Video | Featured | Price |
|------|-------------|-----------|-------|----------|-------|
| free | 3 | 10 | ✗ | ✗ | ₹0 |
| silver | 10 | 20 | ✓ | ✗ | ₹299 |
| gold | unlimited | unlimited | ✓ | ✓ | ₹599 |
| platinum | unlimited | unlimited | ✓ | ✓ | ₹999 |
| contractor | unlimited | unlimited | ✓ | ✓ | ₹999 |

Android: `SubscriptionPlansScreen.kt` + `SubscriptionViewModel.kt` (plan cards hardcoded to mirror backend tiers). Reached via Menu → Subscription Plans, or an upgrade prompt from the Post-Ad wizard when limits are hit.

### 9. Property Discussions (Q&A)
Buyers can ask questions on a property; owners/other users can reply. Hierarchical (parent question + nested replies).
- `GET/POST /api/v1/properties/{property_id}/discussions`
- Has an **in-memory fallback cache** used when the `property_discussions` table query fails — meaning this table may not exist in every environment. **No SQL migration file for `property_discussions` was found on disk** — it needs to be added as a real migration before relying on it in production.
- Android: embedded in `PropertyDetailScreen.kt`; model `Discussion.kt`; `PropertyRepository`/`PropertyViewModel` expose `discussions`, `loadDiscussions()`, `postDiscussion()`.

### 10. Service Requests (Construction & Maintenance Marketplace)
A separate two-sided marketplace layered on top of the property app: property owners/buyers post a service request (e.g. "need a plumber" or "need a contractor to build a wall"), contractors browse open requests and submit quotations, the requester accepts one (which flips the request to `in_progress`).

- `GET/POST /api/v1/service-requests` — list (filters: category, service_type, district, geo radius, status, **urgency, budget_min/budget_max**; sort via **sort_by**) / create
- `POST /api/v1/service-requests/{id}/images` — site photos
- `GET /api/v1/service-requests/{id}` — detail incl. quotation_count
- `PUT /api/v1/service-requests/{id}/status?new_status=`
- `GET/POST /api/v1/service-requests/{id}/quotations`
- `PUT /api/v1/service-requests/quotations/{id}?new_status=` — accept/reject (accept → request becomes `in_progress`)

DB tables `service_requests` and `quotations` (see `backend/migrations/003_module_redesign.sql`), with district/user indexes and RLS. That same migration also bolted extra columns onto `properties`: `youtube_url, instagram_url, rate_per_sqft (auto-computed trigger), deposit, availability_date, nearby_schools/nearby_hospitals (JSONB), document_urls (JSONB), company_profile, previous_projects (JSONB), rating_avg/rating_count (auto-computed trigger from reviews)`.

**Service Feed / Post-Request customization (2026-07-13):** `backend/migrations/006_service_request_enhancements.sql` adds three columns to `service_requests`:
- `urgency TEXT DEFAULT 'normal'` — CHECK `normal | urgent | emergency`, indexed for the feed's urgent-first sort/filter.
- `preferred_date DATE` — requester's desired start date (optional).
- `contact_phone TEXT` — direct number shown to contractors on the request detail screen (optional).

`GET /service-requests` gained `urgency` (exact-match filter), `budget_min`/`budget_max` (range-overlap filter — keeps requests whose `[budget_min, budget_max]` overlaps the searched range), and `sort_by` (`newest` default | `budget_high` | `budget_low` | `urgent_first`). `urgent_first` is ranked in Python after fetching the matching set (`emergency` > `urgent` > `normal`) since it isn't a plain SQL column order; the other three sort modes are done via Supabase `.order()`.

Android — **ServiceRequestFeedScreen.kt** redesigned: a sort dropdown (Newest / Urgent First / Budget High→Low / Budget Low→High) next to the existing category/radius filter chips, plus a new "Any Urgency / 🟡 Urgent / 🔴 Emergency" filter row. Cards now show a leading 64dp thumbnail (first photo, or a category icon avatar when no photo), an urgency badge next to the service-type chip, "Posted Xh/d ago" (`timeAgo()` local time formatter), and "Needed by <date>" when `preferred_date` is set.

**PostServiceRequestScreen.kt** gained: an urgency chip selector (Normal / 🟡 Urgent / 🔴 Emergency), a "Preferred Start Date" field backed by a Material3 `DatePickerDialog` (same pattern as `BookingScreen.kt`'s visit-date picker), and an optional "Contact Phone" field with a note that it's shown to contractors on the detail screen.

**ServiceRequestDetailScreen.kt** gained: an urgency badge next to the category chip, a "Preferred start: <date>" row, and (when `contact_phone` is set) a "Call" button (`ACTION_DIAL`, same pattern as `PropertyDetailScreen.kt`'s contact bar).

Android model: `data/models/ServiceRequest.kt` — new `RequestUrgency` enum (`NORMAL/URGENT/EMERGENCY`), `ServiceRequest.urgency/preferredDate/contactPhone` + `urgencyEnum` getter, matching fields on `ServiceRequestCreateRequest`. `ServiceRequestViewModel.loadServiceRequests()`/`createServiceRequest()` and `PropertyRepository.listServiceRequests()`/`ApiService.listServiceRequests()` all thread the new `urgency`/`budgetMin`/`budgetMax`/`sortBy` (list) and `urgency`/`preferredDate`/`contactPhone` (create) parameters through; mock mode (`MockData.serviceRequests`) replicates the same filter/sort logic client-side.

⚠ `backend/migrations/006_service_request_enhancements.sql` lives outside `supabase/migrations/`, so — like `003`–`005` in that folder — it must be applied manually and is not picked up by `supabase db push`.

Android: `ui/service_request/{PostServiceRequestScreen, ServiceRequestFeedScreen, ServiceRequestDetailScreen, ServiceRequestViewModel}.kt`; model `ServiceRequest.kt` (+ `Quotation`).

### 11. Ad System (Sponsored Banners + Interests + Analytics)
Three cooperating pieces:

**a) Ad personalization (client-side, `AdPersonalizationEngine.kt`)** ranks/filters ads shown in `HomeScreen`'s ad banner section using: district match (+30), listing-type match (+25), priority tier (+20/+10), has-video (+10), social proof i.e. `interestCount ≥ 50` (+10), already-interested (−5), urgency flag (+5). A/B bucketing is `userId.hashCode() % 2`; frequency-capped ads are filtered via `AdFrequencyStore`. **Does not** use user role or browsing/view history.

**b) Ad Interests (leads)** — user taps "I'm Interested" on a sponsored ad:
- `POST/DELETE /api/v1/ads/{ad_id}/interests`, `GET /api/v1/ads/my-interests`
- Admin-only: `GET /api/v1/ads/{ad_id}/interests`, `PATCH .../{interest_id}/status` (pending/contacted/converted/closed)
- DB: `ad_interests` (unique per ad+user), RLS: own rows + admin-all.

**c) Ad Analytics** — event tracking (impression/click/video_play/video_complete/share/interest/interest_removed/cta_click/dismiss):
- `POST /api/v1/ads/analytics/events` — batch ingest, max 200/request, works anonymously or authenticated
- Admin-only: `GET /summary` (CTR, interest rate, video completion %, etc.), `GET /campaign/{id}` (A/B breakdown), `GET /top` (top 10 by CTR)
- DB: `ad_analytics`
- ⚠ **`AdAnalyticsTracker.kt` computes analytics locally but its `flush()` is not actually wired to `POST /ads/analytics/events` yet** — client-side tracking is currently mock-only and not reaching the backend. No admin UI screen consumes the analytics endpoints yet either (backend-only for now).

### 12. Admin Back-Office
Beyond property approval, `admin.py` now covers a full back-office surface, all gated on `role == admin`:
- **Properties**: list by approval_status; `approve` / `reject` (reason required) / `re_approve` (proof_note required) actions
- **Users**: list/filter by role/verified; verify toggle; role change; delete user
- **Payments**: read-only ledger (joined with profile/email)
- **Support tickets**: list, reply (auto-resolves)
- **Stats**: `GET /reports/stats` — total properties/pending/users/agents/builders, total revenue (INR), complaint counts

Android: `AdminDashboardScreen.kt` (5 tabs: Properties/Users/Payments/Complaints/Stats) + `AdminPropertyReviewScreen.kt` + shared `AdminViewModel.kt`.

### 13. Support Tickets
Simple complaint system, separate from Admin:
- `POST /api/v1/support/tickets`, `GET /api/v1/support/tickets/me`
- Admin reply/list lives under `/admin/tickets` (see Admin section)
- DB: `support_tickets`

### 14. Saved Searches ("My Searches")
Lives in `backend/app/routers/saved.py` as a second router (`searches_router`), mounted separately at `/api/v1/searches` — **not** a standalone `searches.py` file as previously documented.
- `GET/POST /api/v1/searches`, `DELETE /api/v1/searches/{id}`
- Android `MySearchesViewModel.kt` is currently **read-only** (only `load()` is wired) — create/delete is not yet hooked up in the UI despite the DTOs existing.

### 15. My Ads
Android-only surface reusing `GET /properties/mine` + `DELETE /properties/{id}`. Tabs: All/Pending/Approved/Rejected. Actions: View, Delete only (no edit/renew/promote/mark-sold yet).

### 16. My Bookings (booking management, beyond the basic visit-booking form)
`MyBookingsScreen.kt` adds a tab row: "My Visits" (buyer's own bookings) vs "Received Inquiries" (bookings on properties the user owns, `GET /bookings/owner`). Supports cancel (buyer) and confirm/decline (owner). No reschedule action yet.

---

## Amenities Master List

```
MAIDS_ROOM, STUDY, CENTRAL_AC_HEATING, BALCONY,
PRIVATE_GARDEN, PRIVATE_POOL, PRIVATE_GYM, PRIVATE_JACUZZI,
SHARED_POOL, SHARED_SPA, SHARED_GYM, SECURITY,
CONCIERGE_SERVICE, MAID_SERVICE, COVERED_PARKING, BUILTIN_WARDROBES,
WALKIN_CLOSET, BUILTIN_KITCHEN_APPLIANCES,
VIEW_OF_WATER, VIEW_OF_LANDMARK, PETS_ALLOWED, DOUBLE_GLAZED_WINDOWS,
DAY_CARE_CENTER, ELECTRICITY_BACKUP, FIRST_AID_MEDICAL_CENTER,
SERVICE_ELEVATORS, PRAYER_ROOM, LAUNDRY_ROOM
```

Android: `enum class Amenity` with display string resource. Backend: validate against this list in Pydantic schema.

---

## Property Sub-Categories & Listing Types (expanded)

`listing_type` now spans, per migrations 013/014: `rent | sale | off_plan | holiday_stay | contractor | ground` (Pydantic schema goes further still — includes a `maintenance` listing_type — see Doc Drift Notes).

For **Residential** (Rent or Sale): Apartment, Villa, Townhouse, Penthouse, Hotel Apartment, Residential Building, Villa Compound, Residential Floor.

For **Commercial**: Office, Shop, Warehouse, Labour Camp, Commercial Building, Commercial Floor, Commercial Villa, Factory, Land, Industrial Land, Other.

For **Ground/Sports** (new, migration 014): Cricket Ground, Football, Other Open Ground, Badminton, Swimming Pool, Other Closed Ground.

For **Contractor/Maintenance** (new): work_category `construction | maintenance`, plus property_type values like Building, Villa House, Interior Fitout, Civil Work, Painting Work, Air Conditioning, Plumbing, Household Equipment (and many more in `schemas/property.py` — e.g. `civil_contractor`, `electrician`, `ac_service`, `pest_control`).

Per-listing-type extra fields live in `properties.metadata JSONB` (migration 015): grounds get `ground_type/length_m/width_m/surface/floodlights/capacity/facilities`; contractors get `work_category/work_types/experience_yrs/team_size/pricing_model/license_no/warranty`; holiday stays get `stay_type/max_guests/check_in/check_out/min_nights/facilities/house_rules/cancellation`.

---

## Database Schema (current tables)

`profiles, agencies, properties, bookings, reviews, saved_properties, saved_searches, payments, support_tickets, service_requests, quotations, ad_interests, ad_analytics, property_discussions (⚠ referenced in code, no committed migration file)`. `public_properties` is a **view**, not a table (migration 008).

### profiles
```sql
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id),
  full_name TEXT,
  phone TEXT,
  avatar_url TEXT,
  role TEXT DEFAULT 'buyer',
    -- CHECK: buyer | agent | builder | admin | landlord | agency | developer  (legacy values retained — see Doc Drift Notes)
  user_id_code TEXT UNIQUE,
  is_verified BOOLEAN DEFAULT FALSE,
  agency_id UUID,
  biometric_enabled BOOLEAN DEFAULT FALSE,
  city TEXT DEFAULT 'All Cities',
  language TEXT DEFAULT 'English',
  subscription_tier VARCHAR(50) DEFAULT 'free',      -- migration 004
  subscription_expires_at TIMESTAMPTZ,               -- migration 004
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### agencies
```sql
CREATE TABLE agencies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  logo_url TEXT,
  license_number TEXT,
  rera_number TEXT,
  phone TEXT,
  email TEXT,
  city TEXT,
  is_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### properties (expanded)
```sql
CREATE TABLE properties (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID REFERENCES profiles(id),
  agency_id UUID REFERENCES agencies(id),
  listed_by TEXT DEFAULT 'landlord',
    -- CHECK: landlord | agent | agency | developer | builder | individual | company | owner
  title TEXT NOT NULL,
  description TEXT,
  price NUMERIC(15,2) NOT NULL,
  price_frequency TEXT DEFAULT 'yearly',  -- yearly | monthly | weekly
  property_type TEXT,   -- greatly expanded, see "Property Sub-Categories" above
  listing_type TEXT,     -- rent | sale | off_plan | holiday_stay | contractor | ground
  bedrooms INT,
  bathrooms INT,
  area_sqft NUMERIC(10,2),
  address TEXT,
  neighborhood TEXT,
  city TEXT,
  district TEXT,                          -- migration 008
  latitude FLOAT,
  longitude FLOAT,
  images TEXT[],          -- max 20 images
  video_url TEXT,
  youtube_url TEXT,                       -- migration 003 (backend/migrations)
  instagram_url TEXT,                     -- migration 003
  amenities TEXT[],
  furnishing TEXT DEFAULT 'unfurnished',  -- CHECK: furnished | unfurnished | semi_furnished (migration 011 renamed 'semi')
  completion_status TEXT, -- ready | off_plan
  payment_plan TEXT,
  handover_date DATE,
  developer_name TEXT,
  permit_number TEXT,
  rera_number TEXT,
  reference_id TEXT UNIQUE,   -- auto-generated "XX-S-XXXXX" / "DP-S-XXXXX"
  brn_dld TEXT,
  zone_name TEXT,
  whatsapp_number TEXT,                   -- migration 012
  rate_per_sqft NUMERIC,                  -- migration 003, auto-computed trigger
  deposit NUMERIC,                        -- migration 003
  availability_date DATE,                 -- migration 003
  nearby_schools JSONB,                   -- migration 003
  nearby_hospitals JSONB,                 -- migration 003
  document_urls JSONB,                    -- migration 003
  company_profile TEXT,                   -- migration 003
  previous_projects JSONB,                -- migration 003
  rating_avg NUMERIC,                     -- migration 003, auto-computed trigger from reviews
  rating_count INT,                       -- migration 003, auto-computed trigger from reviews
  metadata JSONB,                         -- migration 015, per-listing-type extra fields (see above)
  approval_status TEXT DEFAULT 'pending', -- migration 008: pending | approved | rejected
  rejection_reason TEXT,                  -- migration 008
  agent_name TEXT,                        -- migration 008/010
  agent_phone TEXT,                       -- migration 008/010
  agent_photo TEXT,                       -- migration 008/010
  is_verified BOOLEAN DEFAULT FALSE,
  is_featured BOOLEAN DEFAULT FALSE,
  status TEXT DEFAULT 'active',   -- active | sold | rented | inactive
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### bookings
```sql
CREATE TABLE bookings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID REFERENCES properties(id),
  buyer_id UUID REFERENCES profiles(id),
  visit_date DATE,
  visit_time TIME,
  status TEXT DEFAULT 'pending',  -- pending | confirmed | cancelled | completed
  message TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### reviews
```sql
CREATE TABLE reviews (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID REFERENCES properties(id),
  reviewer_id UUID REFERENCES profiles(id),
  rating INT CHECK (rating BETWEEN 1 AND 5),
  comment TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### saved_properties
```sql
CREATE TABLE saved_properties (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES profiles(id),
  property_id UUID REFERENCES properties(id),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, property_id)
);
```

### saved_searches
```sql
CREATE TABLE saved_searches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES profiles(id),
  label TEXT,
  listing_type TEXT,
  filters JSONB,
  thumbnail_url TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### service_requests / quotations (new)
```sql
CREATE TABLE service_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id),
  category TEXT CHECK (category IN ('construction','maintenance')),
  service_type TEXT,
  title TEXT,
  description TEXT,
  district TEXT NOT NULL,
  latitude FLOAT,
  longitude FLOAT,
  radius_km NUMERIC DEFAULT 50,
  budget_min NUMERIC,
  budget_max NUMERIC,
  images JSONB,
  status TEXT CHECK (status IN ('open','in_progress','completed','cancelled')) DEFAULT 'open',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE quotations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID REFERENCES service_requests(id),
  contractor_id UUID REFERENCES auth.users(id),
  property_id UUID REFERENCES properties(id),
  amount NUMERIC,
  timeline TEXT,
  notes TEXT,
  status TEXT CHECK (status IN ('pending','accepted','rejected')) DEFAULT 'pending',
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### payments / support_tickets (new)
```sql
CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES profiles(id),
  amount INT,
  tier TEXT,
  status TEXT DEFAULT 'success',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE support_tickets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES profiles(id),
  subject TEXT,
  description TEXT,
  status TEXT DEFAULT 'open',
  reply TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### ad_interests / ad_analytics (new)
```sql
CREATE TABLE ad_interests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ad_id TEXT,
  ad_title TEXT,
  advertiser_name TEXT,
  listing_type TEXT DEFAULT 'general',
  user_id UUID REFERENCES profiles(id),
  user_name TEXT,
  user_phone TEXT,
  user_email TEXT,
  note TEXT,
  status TEXT CHECK (status IN ('pending','contacted','converted','closed')) DEFAULT 'pending',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(ad_id, user_id)
);

CREATE TABLE ad_analytics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ad_id TEXT,
  ad_title TEXT,
  campaign_id TEXT,
  variant TEXT DEFAULT 'A',
  event_type TEXT CHECK (event_type IN (
    'impression','click','video_play','video_complete','share',
    'interest','interest_removed','cta_click','dismiss'
  )),
  user_id UUID REFERENCES profiles(id),  -- nullable, anonymous allowed
  user_district TEXT,
  session_id TEXT,
  dwell_seconds NUMERIC,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### property_discussions (⚠ used in code, not yet a committed migration — add before relying on it)
```sql
-- Suggested shape, to be formalized as a real migration:
CREATE TABLE property_discussions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID REFERENCES properties(id),
  user_id UUID REFERENCES profiles(id),
  user_name TEXT,
  message TEXT,
  parent_id UUID REFERENCES property_discussions(id),  -- null = top-level question
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## Backend API Endpoints (definitive — from `main.py` router mounts)

All prefixed `/api/v1` unless noted.

### Auth  /auth
| Method | Path | Description |
|--------|------|-------------|
| POST | /register | Email/password registration |
| POST | /login | Login → JWT |
| POST | /google | Google OAuth token exchange |
| POST | /logout | Logout |
| GET | /me | Current user profile |
| PUT | /me | Update profile |
| PUT | /me/role | Set role |
| POST | /me/biometric | Toggle biometric flag |

### Properties  /properties
| Method | Path | Description |
|--------|------|-------------|
| GET | / | List with filters + pagination |
| GET | /featured | Featured listings |
| GET | /search | Search autocomplete |
| GET | /mine | Current user's own listings (feeds My Ads) |
| GET | /{id} | Property detail |
| GET | /{id}/similar | Similar properties |
| POST | / | Create |
| PUT | /{id} | Update (owner/admin) |
| DELETE | /{id} | Delete (owner/admin) |
| POST | /{id}/images | Upload images (max 20) |

### Property Discussions  /properties/{property_id}/discussions
| Method | Path | Description |
|--------|------|-------------|
| GET | / | List Q&A thread (with nested replies) |
| POST | / | Post question (`parent_id` omitted) or reply (`parent_id` set) |

### Bookings  /bookings
| Method | Path | Description |
|--------|------|-------------|
| POST | / | Create visit booking |
| GET | / | My bookings (as buyer) |
| GET | /owner | Bookings on properties I own |
| PUT | /{id}/status | Confirm / decline / complete |
| DELETE | /{id} | Cancel |

### Saved Properties  /saved
| Method | Path | Description |
|--------|------|-------------|
| GET | / | My saved properties |
| POST | /{property_id} | Save |
| DELETE | /{property_id} | Unsave |

### Saved Searches  /searches
| Method | Path | Description |
|--------|------|-------------|
| GET | / | My saved searches |
| POST | / | Save a search |
| DELETE | /{id} | Delete saved search |

### Reviews  /reviews
| Method | Path | Description |
|--------|------|-------------|
| GET | /properties/{id}/reviews | List reviews |
| POST | / | Add review |

### Agencies  /agencies
| Method | Path | Description |
|--------|------|-------------|
| GET | / | List + search |
| GET | /{id} | Agency detail + listings |

### Service Requests  /service-requests
| Method | Path | Description |
|--------|------|-------------|
| GET | / | List open requests (category/service_type/district/geo-radius/status/**urgency/budget_min/budget_max** filters; **sort_by**: newest\|budget_high\|budget_low\|urgent_first) |
| POST | / | Create request |
| POST | /{id}/images | Upload site photos (owner) |
| GET | /{id} | Detail (incl. quotation_count) |
| PUT | /{id}/status?new_status= | Update status |
| GET | /{id}/quotations | List quotations |
| POST | /{id}/quotations | Submit quotation (contractor) |
| PUT | /quotations/{id}?new_status= | Accept/reject quotation (owner) |

### Subscriptions  /subscriptions
| Method | Path | Description |
|--------|------|-------------|
| GET | /me | Current tier, expiry, limits, live listing count |
| POST | /upgrade | Mock checkout → sets tier + expiry, logs payment |

### Ads — Interests  /ads
| Method | Path | Description |
|--------|------|-------------|
| POST | /{ad_id}/interests | Register interest |
| DELETE | /{ad_id}/interests | Withdraw interest |
| GET | /ads/my-interests | My interest history |
| GET | /{ad_id}/interests | Admin: all leads for an ad |
| PATCH | /{ad_id}/interests/{interest_id}/status | Admin: update lead status |

### Ads — Analytics  /ads/analytics
| Method | Path | Description |
|--------|------|-------------|
| POST | /events | Batch ingest (max 200), anon or authed |
| GET | /summary | Admin: per-ad CTR/interest-rate/etc. |
| GET | /campaign/{campaign_id} | Admin: A/B variant breakdown |
| GET | /top | Admin: top 10 ads by CTR |

### Support  /support
| Method | Path | Description |
|--------|------|-------------|
| POST | /tickets | File a ticket |
| GET | /tickets/me | My tickets |

### Admin  /admin  (all require `role == admin`)
| Method | Path | Description |
|--------|------|-------------|
| GET | /properties?approval_status=&page=&limit= | List all properties by status |
| PATCH | /properties/{id}/approval | `action`: approve / reject (reason) / re_approve (proof_note) |
| GET | /users?role=&is_verified= | List users |
| PATCH | /users/{id}/verify | Toggle verified |
| PATCH | /users/{id}/role | Change role |
| DELETE | /users/{id} | Delete user |
| GET | /payments | Payment ledger |
| GET | /tickets?status= | List support tickets |
| POST | /tickets/{id}/reply | Reply + resolve |
| GET | /reports/stats | Dashboard stats |

### Filter Query Params (Properties list)
```
listing_type, property_type, city, district, neighborhood,
min_price, max_price, price_frequency,
bedrooms, bathrooms, min_area, max_area,
furnishing, completion_status, amenities (array),
keyword, listed_by, agency_id, work_category,
verified_only, has_video, has_360,
sort_by (price_asc|price_desc|newest|oldest),
page, limit
```

---

## Android UI Theme

**Brand:** NestX — blue identity (#1565C0). All primary actions, buttons, prices, active states, and the launcher icon use NestX blue. No red in the theme.

```kotlin
// Color.kt — NestX Brand Theme
val NestXBlue        = Color(0xFF1565C0)   // primary
val NestXBlueDark    = Color(0xFF0D47A1)   // pressed / status bar
val NestXBlueLight   = Color(0xFF42A5F5)   // swoosh / highlight
val NestXBlueAccent  = Color(0xFF1976D2)   // mid-tone accent

// Semantic aliases
val PrimaryRed       = NestXBlue           // legacy alias — all "red" → blue
val PrimaryDark      = NestXBlueDark
val BackgroundWhite  = Color(0xFFFFFFFF)
val SurfaceGray      = Color(0xFFF5F5F5)
val TextPrimary      = Color(0xFF1A1A1A)
val TextSecondary    = Color(0xFF757575)
val TextPrice        = NestXBlue
val BorderColor      = Color(0xFFE0E0E0)
val VerifiedBlue     = NestXBlue
val WhatsAppGreen    = Color(0xFF25D366)
val OnboardingBlob   = Color(0xFFBBDEFB)
val BannerBlue       = Color(0xFFE3F2FD)

// Admin/status chip colors (new)
val StatusPending    = Color(0xFFFFC107)   // yellow
val StatusApproved   = Color(0xFF4CAF50)   // green
val StatusRejected   = Color(0xFFF44336)   // red
val AdminBadge       = Color(0xFF6A1B9A)   // purple
```

### Launcher Icon
- PNG icons at all mipmap densities (mdpi → xxxhdpi)
- Adaptive icon (API 26+): blue background + NestX X+swoosh foreground vector
- Icon files: `res/mipmap-{density}/ic_launcher.png` + `ic_launcher_round.png`
- Vector foreground: `res/drawable/ic_launcher_foreground_vector.xml`

---

## Android Dependencies (build.gradle)

```kotlin
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Google Auth
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Pager (onboarding + image carousel)
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")

    // Coroutines + Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

---

## Backend requirements.txt

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

## Environment Variables

### backend/.env
```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
SECRET_KEY=your-super-secret-key
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=1440
GOOGLE_CLIENT_ID=your-google-client-id
DEBUG=True
ALLOWED_ORIGINS=http://localhost,http://10.0.2.2
```

### android/local.properties
```
BASE_URL=http://10.0.2.2:8000/api/v1/
GOOGLE_MAPS_API_KEY=your-maps-key
GOOGLE_WEB_CLIENT_ID=your-web-client-id
```

---

## RLS Policies (key ones)

```sql
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_profile" ON profiles USING (auth.uid() = id);

ALTER TABLE properties ENABLE ROW LEVEL SECURITY;
CREATE POLICY "public_read" ON properties FOR SELECT
  USING (status = 'active' AND approval_status = 'approved');  -- updated by migration 010
CREATE POLICY "owner_write" ON properties FOR ALL USING (auth.uid() = owner_id);

ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "buyer_own" ON bookings USING (auth.uid() = buyer_id);

ALTER TABLE saved_properties ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_saved" ON saved_properties USING (auth.uid() = user_id);

ALTER TABLE saved_searches ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_searches" ON saved_searches USING (auth.uid() = user_id);

ALTER TABLE service_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY "view_open_requests" ON service_requests FOR SELECT USING (status = 'open' OR auth.uid() = user_id);
CREATE POLICY "insert_own_request" ON service_requests FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "update_own_request" ON service_requests FOR UPDATE USING (auth.uid() = user_id);

ALTER TABLE quotations ENABLE ROW LEVEL SECURITY;
CREATE POLICY "view_quotations_for_owner" ON quotations FOR SELECT
  USING (auth.uid() = contractor_id OR auth.uid() IN (SELECT user_id FROM service_requests WHERE id = request_id));
CREATE POLICY "insert_quotation" ON quotations FOR INSERT WITH CHECK (auth.uid() = contractor_id);
CREATE POLICY "update_own_quotation" ON quotations FOR UPDATE USING (auth.uid() = contractor_id);

ALTER TABLE ad_interests ENABLE ROW LEVEL SECURITY;
CREATE POLICY "user_own_interests" ON ad_interests USING (auth.uid() = user_id);
CREATE POLICY "admin_all_interests" ON ad_interests USING (
  EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
);

ALTER TABLE ad_analytics ENABLE ROW LEVEL SECURITY;
CREATE POLICY "insert_own_event" ON ad_analytics FOR INSERT WITH CHECK (true);  -- open, incl. anonymous
CREATE POLICY "admin_read_all" ON ad_analytics FOR SELECT USING (
  EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
);

ALTER TABLE support_tickets ENABLE ROW LEVEL SECURITY;
-- users insert/view own tickets; admins full access
```

---

## Place an Ad — Navigation Flow

```
Bottom Nav "+" button
    │
    ▼
PostAdCategoryScreen         ← "What are you listing?" (category grid)
    │
    ▼
PostAdTitleScreen            ← "Enter a short title" + "Let's Go"
    │
    ▼
PostAdSubCategoryScreen      ← breadcrumb + list (Apartment, Villa, etc.)
    │
    ▼
PostAdRoleScreen             ← "Landlord or Agent?" (2 cards)
    │
    ▼
PostAdDetailsScreen          ← Full form (images, price, location, amenities...)
    │                          also enforces subscription-tier limits here
    ▼
PostAdMapPickerScreen        ← map-based location picker (new step)
    │
    ▼
Success / Preview Screen
```

---

## Development Phases

The app has already been built and released at least once (`android/app/release/RealEstate_V10.0.apk` exists), so most of Phases 1–4 are functionally complete. Treat the checklist below as a gap list, not a from-scratch plan.

### Phase 1 — Backend + DB — mostly done, gaps noted
- [x] FastAPI project setup, Supabase client
- [x] Auth, Properties CRUD + filters, Bookings, Saved, Reviews, Agencies
- [x] Admin approval workflow (+ user/payment/ticket/stats surface)
- [x] Subscriptions, Service Requests + Quotations, Ad Interests + Analytics, Support
- [ ] **Commit a real migration for `property_discussions`** (currently code-only with an in-memory fallback)
- [ ] Reconcile `VALID_ROLES` (backend) with the Android `UserRole` enum and DB `profiles_role_check` (see Doc Drift Notes)
- [ ] Wire `AdAnalyticsTracker.flush()` to `POST /ads/analytics/events` (currently mock-only, not reaching backend)
- [ ] pytest coverage for the newer routers (subscriptions/service_requests/admin/ads/discussions) — only auth/properties/bookings tests exist

### Phase 2 — Android Auth + Nav — done
- [x] Kotlin + Compose + Hilt, Retrofit + AuthInterceptor + DataStore
- [x] AppNavGraph + Screen sealed class (now incl. nested `post_ad_graph`/`admin_graph`)
- [x] Splash, Onboarding, Login/Register/Google, Role Selection, Biometric

### Phase 3 — Core Screens — done, extended
- [x] Home, Property List, Filter, Amenities, Property Detail, Google Maps
- [x] District-based browsing (`DistrictListScreen`) — new, not in original plan
- [x] Q&A/Discussions embedded in Property Detail

### Phase 4 — Menu + Post Ad — done, extended
- [x] Menu screen, Post Ad wizard (now 6 steps incl. map picker)
- [x] My Ads (view/delete only — edit/renew/promote still open)
- [ ] **My Searches create/delete/rename** — currently read-only in the UI, needs wiring to existing `SavedSearchRequest` DTOs

### Phase 5 — User Features — mostly done, extended
- [x] Saved/Favorites, Booking + My Bookings (incl. owner "Received Inquiries" view), Chat List, Profile, Account/Notification Settings
- [x] Subscription Plans screen
- [x] Service Request post/feed/detail + quotations flow
- [x] Admin Dashboard + Property Review

### Phase 6 — Polish + Deploy — partially done
- [x] At least one debug + one release APK built (`RealEstate_V10.0.apk`)
- [ ] Admin UI for ad analytics (backend endpoints exist, unconsumed)
- [ ] Loading skeletons, pull-to-refresh, infinite scroll, empty/error states — verify per-screen coverage
- [ ] Dockerize backend / deploy / Play Store submission — status unconfirmed, verify before assuming done

---

## Key Commands

### Backend
```bash
cd backend
python -m venv venv
source venv/bin/activate    # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --port 8000
# Swagger: http://localhost:8000/docs
```

### Android
```bash
# Open android/ in Android Studio
# Emulator localhost = 10.0.2.2
./gradlew assembleDebug
./gradlew test
```

### Supabase
```bash
# Paste each migration file into Supabase SQL Editor (in numeric order; note the duplicate "008" filename pair)
# OR via CLI:
supabase db push
```
Also apply `backend/migrations/003_module_redesign.sql`, `004_subscription_system.sql`, `005_admin_modules.sql`, `006_service_request_enhancements.sql` — these live outside `supabase/migrations/` and are not part of the `supabase db push` flow, so they must be run manually.

---

## Claude Code Notes

1. `10.0.2.2:8000` = localhost for Android emulator
2. Supabase `service_role_key` → backend ONLY, never in Android APK
3. All JWT via `Authorization: Bearer <token>` header
4. All image uploads: Android → Backend → Supabase Storage
5. Role in `profiles.role` + injected into JWT `custom_claims` — but see Doc Drift Notes before trusting role strings across layers
6. ViewModel pattern: `sealed class UiState<T>: Loading / Success(data) / Error(msg)`
7. `PropertyFilterScreen` and `AmenitiesScreen` navigated as separate full screens (not bottom sheets)
8. `PostAdCategoryScreen` navigated from bottom nav "+" button — hides bottom nav; wizard is a nested nav graph (`post_ad_graph`) sharing one `PostAdViewModel`
9. Menu screen IS a bottom-nav tab; bottom nav is now only **Home + Menu** (Saved/Chat moved to Menu-only access)
10. "My Searches" data stored in `saved_searches` table, endpoints live in `saved.py`'s `searches_router` (not a separate file); Android UI for it is currently read-only
11. `listed_by` field on property distinguishes landlord/agent/agency/developer/builder/individual/company/owner (8 values after migration 013 — reconcile before assuming a clean 4-value set)
12. Reference ID format: backend auto-generates "XX-S-XXXXX" or "DP-S-XXXXX"
13. Build version displayed at bottom of Menu screen — store in `BuildConfig.VERSION_NAME`
14. Amenities enum must match exactly between Android, Backend, and DB for filter queries
15. `AdminViewModel` and `PostAdViewModel` are each shared across their nested nav graph's screens — don't create per-screen instances
16. `property_discussions` has no committed SQL migration — add one before depending on it in a fresh environment
17. `AdAnalyticsTracker.flush()` is not wired to the backend — ad analytics are currently client-local mock data only

---

## Doc Drift Notes — reconcile before further role/listing-type work

These are real inconsistencies found across the codebase, not stylistic choices — flagging so they get fixed deliberately rather than papered over:

1. **Roles disagree across three layers.**
   - Android `UserRole` enum: `BUYER, AGENT, BUILDER, ADMIN` (clean v2 model)
   - Backend `schemas/auth.py` `VALID_ROLES`: `buyer, landlord, agent, agency, developer, admin` (no `builder`, keeps legacy values)
   - DB `profiles_role_check` (migration 008): `buyer, agent, builder, admin, landlord, agency, developer` (superset of both)
   Pick one canonical set and migrate the other two layers to match.

2. **`listed_by` has ballooned to 8 values**: `landlord, agent, agency, developer, builder, individual, company, owner` (final state after migration 013) — far beyond the `landlord|agent|agency|developer` in earlier docs. Decide the real intended set.

3. **`property_type`/`listing_type` in `schemas/property.py` (Pydantic) go further than the SQL `CHECK` constraints on disk** — e.g. a `maintenance` listing_type and many contractor/maintenance property_type values (`civil_contractor`, `electrician`, `ac_service`, `pest_control`, etc.) exist in Python validation but may not be reflected in the DB constraint. Risk: valid-per-API payloads could be rejected at the DB layer, or vice versa. Audit and sync.

4. **`property_discussions` table has no committed migration file** — the router has a fallback in-memory cache specifically because the table may not exist. Add a real migration (draft provided above) before shipping this feature to a new environment.

5. **Migration filename collision**: two files are both named with prefix `008` (`008_add_district.sql` and `008_backfill_agent_fields.sql`). Not currently breaking anything since they're applied manually/pasted in order, but rename one to avoid ordering ambiguity via tooling that sorts by filename.

6. **`AdAnalyticsTracker.flush()` does not call the backend** — analytics summary shown to admins today would be empty/absent since no events are actually being ingested from the app. Either wire it up or explicitly mark analytics as backend-only/seed-data for now.

7. **No admin UI consumes the ad analytics endpoints** (`/ads/analytics/summary`, `/campaign/{id}`, `/top`) yet — backend is ready, frontend isn't.

8. **"My Searches" is read/display-only in the Android app** — save/delete/rename DTOs exist (`SavedSearchRequest`) but aren't wired to any button/action in `MySearchesViewModel`.
