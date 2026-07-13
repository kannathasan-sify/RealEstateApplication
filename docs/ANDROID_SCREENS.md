# Real Estate App — Android Screens Documentation
# App: NestX — India Real Estate (Tamil Nadu Focus)
# Tech: Kotlin + Jetpack Compose + MVVM + Hilt
# Last Updated: 2026-04-01 (rev2 — DB seed + district-area filters)

---

## BUSINESS LOGIC OVERVIEW (v2)

| # | Rule | Detail |
|---|------|--------|
| 1 | **Default Region** | Tamil Nadu state; Home screen shows all 38 districts by default |
| 2 | **Currency** | Indian Rupees — ₹ (INR) everywhere; no AED |
| 3 | **Mock Data** | All list & detail screens use mock data until API is live |
| 4 | **User Roles** | `buyer` · `agent` · `builder` · `admin` (4 roles only) |
| 5 | **Admin Approval** | Every new property submission requires admin approval before it appears in any list |
| 6 | **Listing Flow** | Agent creates property → submitted to Admin → Admin approves/rejects → approved listings appear district-wise |

---

## Table of Contents

1. [Theme & Colors](#1-theme--colors)
2. [Navigation Architecture](#2-navigation-architecture)
3. [Tamil Nadu District System](#3-tamil-nadu-district-system)
4. [Currency & Price Formatting](#4-currency--price-formatting)
5. [User Roles & Permissions](#5-user-roles--permissions)
6. [Admin Approval Workflow](#6-admin-approval-workflow)
7. [Mock Data Definitions](#7-mock-data-definitions)
8. [SplashScreen](#8-splashscreen)
9. [OnboardingScreen](#9-onboardingscreen)
10. [LoginScreen](#10-loginscreen)
11. [RegisterScreen](#11-registerscreen)
12. [RoleSelectionScreen](#12-roleselectionscreen)
13. [HomeScreen](#13-homescreen)
14. [DistrictListScreen ← NEW](#14-districtlistscreen)
15. [PropertyListScreen](#15-propertylistscreen)
16. [PropertyFilterScreen](#16-propertyfilterscreen)
17. [AmenitiesScreen](#17-amenitiesscreen)
18. [PropertyDetailScreen](#18-propertydetailscreen)
19. [MenuScreen](#19-menuscreen)
20. [PostAdFlow (Agent only)](#20-postadflow-agent-only)
21. [AdminDashboardScreen ← NEW](#21-admindashboardscreen)
22. [AdminPropertyReviewScreen ← NEW](#22-adminpropertyreviewscreen)
23. [SavedScreen](#23-savedscreen)
24. [BookingScreen](#24-bookingscreen)
25. [ChatListScreen](#25-chatlistscreen)
26. [ProfileScreen](#26-profilescreen)
27. [Reusable Components](#27-reusable-components)
28. [Amenities Master List](#28-amenities-master-list)
29. [Dependencies (build.gradle)](#29-dependencies-buildgradle)
30. [Key Developer Notes](#30-key-developer-notes)

---

## 1. Theme & Colors

**File:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`

```kotlin
// Color.kt — NestX Brand Theme
val NestXBlue        = Color(0xFF1565C0)   // primary
val NestXBlueDark    = Color(0xFF0D47A1)   // pressed / status bar
val NestXBlueLight   = Color(0xFF42A5F5)   // highlight / swoosh
val NestXBlueAccent  = Color(0xFF1976D2)   // accent

// Semantic aliases
val PrimaryBlue      = NestXBlue
val PrimaryDark      = NestXBlueDark
val PrimaryRed       = NestXBlue           // legacy compat — no red in app
val BackgroundWhite  = Color(0xFFFFFFFF)
val SurfaceGray      = Color(0xFFF5F5F5)
val TextPrimary      = Color(0xFF1A1A1A)
val TextSecondary    = Color(0xFF757575)
val TextPrice        = NestXBlue           // ₹ price text
val BorderColor      = Color(0xFFE0E0E0)
val VerifiedBlue     = NestXBlue
val WhatsAppGreen    = Color(0xFF25D366)
val OnboardingBlob   = Color(0xFFBBDEFB)   // light blue blob
val BannerBlue       = Color(0xFFE3F2FD)

// Status colors
val StatusPending    = Color(0xFFFFA726)   // orange — pending approval
val StatusApproved   = Color(0xFF43A047)   // green — approved / active
val StatusRejected   = Color(0xFFE53935)   // red — rejected
val AdminBadge       = Color(0xFF6A1B9A)   // purple — admin UI elements
```

**Typography:** Material 3 type scale.
- Display/Heading: 24sp bold
- Section title: 18sp semi-bold
- Card title: 16sp semi-bold
- Body: 14sp regular
- Caption: 12sp gray

---

## 2. Navigation Architecture

**Files:** `navigation/AppNavGraph.kt`, `navigation/Screen.kt`

```kotlin
sealed class Screen(val route: String) {
    // Auth
    object Splash            : Screen("splash")
    object Onboarding        : Screen("onboarding")
    object Login             : Screen("login")
    object Register          : Screen("register")
    object RoleSelection     : Screen("role_selection")

    // Main bottom-nav tabs
    object Home              : Screen("home")
    object Saved             : Screen("saved")
    object PostAdCategory    : Screen("post_ad_category")   // "+" FAB
    object ChatList          : Screen("chat_list")
    object Menu              : Screen("menu")

    // District → Property browsing
    object DistrictList      : Screen("district_list/{listingType}")
    object PropertyList      : Screen("property_list/{district}/{listingType}")
    object PropertyFilter    : Screen("property_filter")
    object Amenities         : Screen("amenities")
    object PropertyDetail    : Screen("property_detail/{id}")

    // Post Ad wizard (agent only)
    object PostAdTitle       : Screen("post_ad_title")
    object PostAdSubCategory : Screen("post_ad_sub_category")
    object PostAdDetails     : Screen("post_ad_details")

    // Admin screens
    object AdminDashboard    : Screen("admin_dashboard")
    object AdminPropertyReview : Screen("admin_property_review/{id}")

    // Profile
    object Profile           : Screen("profile")
    object Booking           : Screen("booking/{propertyId}")
}
```

### Bottom Nav Bar (5 tabs — `BottomNavBar.kt`)
| Index | Icon | Label | Visible To |
|-------|------|-------|-----------|
| 0 | 🏠 Home | Home | All |
| 1 | ♥ Favorites | Saved | All |
| 2 | ➕ **Blue circle FAB** | Post Property | `agent` · `builder` only (hidden for buyer) |
| 3 | 💬 Chats | Chats | All |
| 4 | ☰ Menu | Menu | All |

- Active tab: filled icon + bold label in `NestXBlue`
- Center "+" FAB: 56dp blue circle, white plus, elevated shadow
- Hidden on Post Ad wizard screens
- Admin sees an extra "Admin" shield icon in Menu screen (not in bottom bar)

---

## 3. Tamil Nadu District System

**File:** `data/models/Location.kt`

### All 38 Tamil Nadu Districts

```kotlin
val TAMILNADU_DISTRICTS = listOf(
    "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem",
    "Tirunelveli", "Tiruppur", "Ranipet", "Erode", "Vellore",
    "Thoothukudi", "Dindigul", "Thanjavur", "Kanniyakumari", "Cuddalore",
    "Kancheepuram", "Nagapattinam", "Namakkal", "Nilgiris", "Perambalur",
    "Pudukkottai", "Ramanathapuram", "Sivaganga", "Tenkasi", "Theni",
    "Tirivannamalai", "Viridhunagar", "Ariyalur", "Chengalpattu",
    "Kallakurichi", "Karur", "Krishnagiri", "Mayiladuthurai",
    "Sivagangai", "Tirupattur", "Tiruvarur", "Villupuram", "Virudhunagar"
)
```

### Location Model
```kotlin
data class TamilNaduLocation(
    val district: String,
    val city: String,
    val area: String = "",
    val pincode: String = ""
)
```

### District Selection Logic (HomeScreen)
- App always defaults to **Tamil Nadu**
- Home screen shows a **District Picker** section
- User selects a district → navigates to `DistrictListScreen` showing properties in that district
- Selected district stored in `DataStore` as user preference
- If no district selected → shows all-Tamil Nadu aggregated view
- District shown in top bar: e.g. "Chennai" with a ▼ dropdown arrow

---

## 4. Currency & Price Formatting

**File:** `utils/CurrencyFormatter.kt`

```kotlin
object CurrencyFormatter {

    /** Format price with ₹ symbol and Indian number system (lakhs/crores) */
    fun format(amount: Long, frequency: String? = null): String {
        val formatted = when {
            amount >= 10_000_000 -> "₹${amount / 10_000_000.0:.1f} Cr"  // crore
            amount >= 100_000   -> "₹${amount / 100_000.0:.1f} L"       // lakh
            else                -> "₹${"%,d".format(amount)}"
        }
        return if (frequency != null) "$formatted/$frequency" else formatted
    }

    /** Short form for cards */
    fun short(amount: Long): String = when {
        amount >= 10_000_000 -> "₹${amount / 10_000_000.0:.1f}Cr"
        amount >= 100_000   -> "₹${amount / 100_000.0:.0f}L"
        else                -> "₹${"%,d".format(amount)}"
    }
}
```

### Price Ranges (INR Reference)
| Property Type | Rent/Month | Sale Price |
|--------------|------------|-----------|
| 1 BHK Apartment | ₹8,000 – ₹25,000 | ₹30L – ₹80L |
| 2 BHK Apartment | ₹15,000 – ₹50,000 | ₹60L – ₹1.5Cr |
| 3 BHK Apartment | ₹25,000 – ₹80,000 | ₹90L – ₹3Cr |
| Villa/House | ₹30,000 – ₹2L | ₹1Cr – ₹10Cr |
| Commercial Shop | ₹10,000 – ₹1L | ₹50L – ₹5Cr |
| Land (per cent) | N/A | ₹5L – ₹50L/cent |

**All price fields:** `Long` (paise not needed; store full rupees)
**Filter slider:** Min ₹0 — Max ₹5Cr (rent); ₹0 — Max ₹50Cr (sale)

---

## 5. User Roles & Permissions

**File:** `data/models/UserRole.kt`

```kotlin
enum class UserRole(val displayName: String, val description: String) {
    BUYER(
        "Buyer / Tenant",
        "Browse and enquire about properties"
    ),
    AGENT(
        "Agent",
        "List properties on behalf of owners; submit for admin approval"
    ),
    BUILDER(
        "Builder / Developer",
        "List new construction projects; submit for admin approval"
    ),
    ADMIN(
        "Admin",
        "Approve/reject listings; manage all users and properties"
    )
}
```

### Role Capability Matrix
| Feature | Buyer | Agent | Builder | Admin |
|---------|-------|-------|---------|-------|
| Browse properties | ✅ | ✅ | ✅ | ✅ |
| Save favourites | ✅ | ✅ | ✅ | ✅ |
| Book site visit | ✅ | ✅ | ✅ | ✅ |
| Post new property | ❌ | ✅ | ✅ | ✅ |
| Edit own property | ❌ | ✅ (own) | ✅ (own) | ✅ |
| Delete own property | ❌ | ✅ (own) | ✅ (own) | ✅ |
| Submit for approval | ❌ | ✅ | ✅ | auto-approved |
| Approve / Reject | ❌ | ❌ | ❌ | ✅ |
| View all users | ❌ | ❌ | ❌ | ✅ |
| Admin Dashboard | ❌ | ❌ | ❌ | ✅ |
| See pending listings | ❌ | own only | own only | all |

### Role shown in Profile card
- Buyer → gray badge "Buyer"
- Agent → blue badge "Agent"
- Builder → orange badge "Builder"
- Admin → purple badge "Admin" + shield icon

---

## 6. Admin Approval Workflow

**File:** `data/models/Property.kt` — `approvalStatus` field

```kotlin
enum class ApprovalStatus {
    PENDING,    // just submitted by agent/builder — not visible in public lists
    APPROVED,   // admin approved — visible in district-wise public listing
    REJECTED    // admin rejected — hidden; agent sees rejection reason
}
```

### Full Flow

```
Agent/Builder fills PostAdDetailsScreen
         │
         ▼
POST /api/v1/properties/
approvalStatus = PENDING
status = "inactive"          ← NOT visible in public lists yet
         │
         ▼
Admin sees it in AdminDashboard → "Pending Approval" tab
         │
    ┌────┴─────┐
    ▼          ▼
APPROVED    REJECTED
status =    rejection reason
"active"    stored + sent to agent via notification
    │
    ▼
Appears in PropertyListScreen
(district-wise, approved only)
```

### Visibility Rules
- **Public listing** (`GET /properties/`) returns ONLY `approvalStatus = APPROVED` + `status = active`
- **Agent's "My Ads"** shows all of their own listings (PENDING · APPROVED · REJECTED)
- **Admin Dashboard** shows ALL listings with filter by status
- Agent sees color-coded status badge on their listing cards:
  - 🟡 Pending Review
  - 🟢 Live / Approved
  - 🔴 Rejected (with reason)

---

## 7. Mock Data Definitions

**File:** `data/mock/MockData.kt`

Used when `BuildConfig.USE_MOCK_DATA = true` (default in debug builds).

### Mock Properties — Tamil Nadu

```kotlin
object MockData {

    val properties = listOf(
        Property(
            id = "mock-001",
            title = "Spacious 3 BHK Apartment near Anna Nagar",
            description = "Well-ventilated 3 BHK apartment on the 4th floor with beautiful city view. " +
                "Premium flooring, modular kitchen, and 2 covered car parking. " +
                "5 mins from Anna Nagar East metro station. " +
                "Society amenities include gym, swimming pool and 24/7 security.",
            price = 18000L,             // ₹18,000/month
            priceFrequency = "month",
            propertyType = "apartment",
            listingType = "rent",
            bedrooms = 3,
            bathrooms = 2,
            areaSqft = 1450.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "Anna Nagar",
            address = "Block 7, 4th Avenue, Anna Nagar West, Chennai - 600040",
            latitude = 13.0878,
            longitude = 80.2107,
            images = listOf(
                "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800"
            ),
            amenities = listOf("COVERED_PARKING","SHARED_GYM","SHARED_POOL","SECURITY","BUILTIN_WARDROBES"),
            furnishing = "semi",
            listedBy = "agent",
            agentName = "Rajesh Kumar",
            agentPhone = "+91 98401 23456",
            agentPhoto = "https://randomuser.me/api/portraits/men/32.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00001",
            createdAt = "2026-03-20T10:00:00Z"
        ),
        Property(
            id = "mock-002",
            title = "2 BHK Independent House for Sale — Coimbatore",
            description = "Independent 2 BHK house in a prime residential area of Coimbatore. " +
                "Ground floor with spacious garden. Newly constructed, ready to occupy. " +
                "Close to KMCH hospital, reputed schools, and shopping centres.",
            price = 6500000L,           // ₹65 Lakhs
            priceFrequency = null,
            propertyType = "independent_house",
            listingType = "sale",
            bedrooms = 2,
            bathrooms = 2,
            areaSqft = 1100.0,
            district = "Coimbatore",
            city = "Coimbatore",
            neighborhood = "RS Puram",
            address = "14, 3rd Street, RS Puram, Coimbatore - 641002",
            latitude = 11.0115,
            longitude = 76.9545,
            images = listOf(
                "https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800",
                "https://images.unsplash.com/photo-1588880331179-bc9b93a8cb5e?w=800"
            ),
            amenities = listOf("PRIVATE_GARDEN","COVERED_PARKING","SECURITY"),
            furnishing = "unfurnished",
            listedBy = "builder",
            agentName = "Priya Builders",
            agentPhone = "+91 99420 56789",
            agentPhoto = "https://randomuser.me/api/portraits/women/44.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00002",
            createdAt = "2026-03-22T09:00:00Z"
        ),
        Property(
            id = "mock-003",
            title = "Commercial Shop for Rent — T Nagar",
            description = "Prime commercial shop space on the ground floor at T Nagar main road. " +
                "High footfall area, suitable for retail, restaurant, or showroom. " +
                "Dedicated parking for 3 vehicles. 24-hour CCTV surveillance.",
            price = 75000L,             // ₹75,000/month
            priceFrequency = "month",
            propertyType = "shop",
            listingType = "rent",
            bedrooms = 0,
            bathrooms = 1,
            areaSqft = 850.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "T Nagar",
            address = "22, Usman Road, T Nagar, Chennai - 600017",
            latitude = 13.0418,
            longitude = 80.2341,
            images = listOf(
                "https://images.unsplash.com/photo-1604014237800-1c9102c219da?w=800",
                "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800"
            ),
            amenities = listOf("COVERED_PARKING","SECURITY","ELECTRICITY_BACKUP"),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Suresh Nair",
            agentPhone = "+91 94440 78901",
            agentPhoto = "https://randomuser.me/api/portraits/men/55.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00003",
            createdAt = "2026-03-25T11:00:00Z"
        ),
        Property(
            id = "mock-004",
            title = "4 BHK Luxury Villa in ECR",
            description = "Stunning luxury villa on East Coast Road with sea-view. " +
                "Private swimming pool, landscaped garden, home theatre, and modular kitchen. " +
                "Gated community with 24/7 security. 15 mins from Thiruvanmiyur beach.",
            price = 95000L,             // ₹95,000/month
            priceFrequency = "month",
            propertyType = "villa",
            listingType = "rent",
            bedrooms = 4,
            bathrooms = 4,
            areaSqft = 4200.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "ECR",
            address = "Plot 12, Sea Breeze Layout, ECR, Chennai - 600119",
            latitude = 12.9165,
            longitude = 80.2525,
            images = listOf(
                "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800",
                "https://images.unsplash.com/photo-1599427303058-f04cbcf4756f?w=800"
            ),
            amenities = listOf("PRIVATE_POOL","PRIVATE_GYM","PRIVATE_GARDEN","COVERED_PARKING","SECURITY","BUILTIN_KITCHEN_APPLIANCES"),
            furnishing = "furnished",
            listedBy = "agent",
            agentName = "Kavitha Devi",
            agentPhone = "+91 98841 34567",
            agentPhoto = "https://randomuser.me/api/portraits/women/22.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00004",
            createdAt = "2026-03-28T14:00:00Z"
        ),
        Property(
            id = "mock-005",
            title = "1 BHK Studio Flat — Madurai City Centre",
            description = "Compact and well-designed 1 BHK flat near Madurai Meenakshi Amman Temple. " +
                "Ideal for bachelors and young couples. Fully furnished with TV, fridge, and washing machine. " +
                "Walking distance to bus stand and railway station.",
            price = 8500L,              // ₹8,500/month
            priceFrequency = "month",
            propertyType = "apartment",
            listingType = "rent",
            bedrooms = 1,
            bathrooms = 1,
            areaSqft = 550.0,
            district = "Madurai",
            city = "Madurai",
            neighborhood = "City Centre",
            address = "Block C, 2nd Floor, Meenakshi Nagar, Madurai - 625001",
            latitude = 9.9252,
            longitude = 78.1198,
            images = listOf(
                "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800",
                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800"
            ),
            amenities = listOf("SECURITY","BUILTIN_WARDROBES"),
            furnishing = "furnished",
            listedBy = "agent",
            agentName = "Murugan Selva",
            agentPhone = "+91 99440 23456",
            agentPhoto = "https://randomuser.me/api/portraits/men/71.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00005",
            createdAt = "2026-03-30T08:00:00Z"
        ),
        Property(
            id = "mock-006",
            title = "30 Cents Land for Sale — Thanjavur Highway",
            description = "Prime agricultural and residential-convertible land along Thanjavur-Kumbakonam highway. " +
                "DTCP-approved plot, clear title, water and electricity available. " +
                "Suitable for residential plots, farm house, or commercial development.",
            price = 12000000L,          // ₹1.2 Crore
            priceFrequency = null,
            propertyType = "land",
            listingType = "sale",
            bedrooms = 0,
            bathrooms = 0,
            areaSqft = 13068.0,         // 30 cents = 13,068 sqft
            district = "Thanjavur",
            city = "Thanjavur",
            neighborhood = "NH67 Bypass",
            address = "Survey No. 45/2, Thanjavur-Kumbakonam Highway, Thanjavur - 613001",
            latitude = 10.7869,
            longitude = 79.1378,
            images = listOf(
                "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800"
            ),
            amenities = listOf(),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Balamurugan TN",
            agentPhone = "+91 98410 65432",
            agentPhoto = "https://randomuser.me/api/portraits/men/88.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00006",
            createdAt = "2026-03-31T16:00:00Z"
        ),
        // PENDING approval mock (visible to agent only)
        Property(
            id = "mock-007",
            title = "3 BHK New Flat for Rent — Tiruppur",
            description = "Brand new 3 BHK in a newly constructed apartment complex in Tiruppur. " +
                "All modern amenities, UDS included. Waiting for admin approval.",
            price = 14000L,
            priceFrequency = "month",
            propertyType = "apartment",
            listingType = "rent",
            bedrooms = 3,
            bathrooms = 2,
            areaSqft = 1200.0,
            district = "Tiruppur",
            city = "Tiruppur",
            neighborhood = "Avinashi Road",
            address = "Sunshine Apartments, Avinashi Road, Tiruppur - 641604",
            latitude = 11.1085,
            longitude = 77.3411,
            images = listOf(
                "https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800"
            ),
            amenities = listOf("COVERED_PARKING","SHARED_GYM","SECURITY"),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Vijay Agent",
            agentPhone = "+91 97890 12345",
            agentPhoto = "https://randomuser.me/api/portraits/men/15.jpg",
            approvalStatus = ApprovalStatus.PENDING,   // ← pending admin
            referenceId = "NX-TN-00007",
            createdAt = "2026-04-01T07:00:00Z"
        )
    )

    val districtSummaries = mapOf(
        "Chennai"          to DistrictSummary("Chennai",       128, "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?w=400"),
        "Coimbatore"       to DistrictSummary("Coimbatore",     64, "https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=400"),
        "Madurai"          to DistrictSummary("Madurai",         31, "https://images.unsplash.com/photo-1586763263060-72b5cd8add52?w=400"),
        "Tiruchirappalli"  to DistrictSummary("Tiruchirappalli", 22, "https://images.unsplash.com/photo-1600607688969-a5bfcd646154?w=400"),
        "Salem"            to DistrictSummary("Salem",           18, "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=400"),
        "Tirunelveli"      to DistrictSummary("Tirunelveli",     14, "https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=400"),
        "Tiruppur"         to DistrictSummary("Tiruppur",        11, "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=400"),
        "Vellore"          to DistrictSummary("Vellore",          9, "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=400"),
        "Erode"            to DistrictSummary("Erode",            7, "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=400"),
        "Thanjavur"        to DistrictSummary("Thanjavur",        6, "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400")
    )

    data class DistrictSummary(
        val name: String,
        val propertyCount: Int,
        val imageUrl: String
    )
}
```

---

## 8. SplashScreen

**File:** `ui/splash/SplashScreen.kt`

- Full-screen NestX blue (#1565C0) background
- Centered NestX logo + "NestX" wordmark in white
- Tagline: "Find Your Home in Tamil Nadu" (white, 14sp)
- Displayed for ~2 seconds
- Navigation:
  - First launch → OnboardingScreen
  - Returning user (token exists, role set) → HomeScreen
  - No token → LoginScreen

---

## 9. OnboardingScreen

**File:** `ui/onboarding/OnboardingScreen.kt`
**ViewModel:** `OnboardingViewModel.kt`

3 slides (HorizontalPager):

| Slide | Heading | Subtext |
|-------|---------|---------|
| 1 | "Find Your Dream Home in Tamil Nadu" | "Browse properties across all 38 districts" |
| 2 | "Connect with Trusted Agents" | "Verified agents and builders in your area" |
| 3 | "Move In with Confidence" | "Admin-verified listings only — no fake ads" |

- Dot indicators + "Next" blue button + "Skip" top-right
- Last slide → Login

---

## 10. LoginScreen

**File:** `ui/auth/LoginScreen.kt`

- NestX logo + "Welcome Back" heading
- Email + Password fields
- "Forgot Password?" link
- "Log In" blue button
- `— OR —` divider
- Google Sign-In button
- Footer: "New to NestX? **Register**"
- Admin login: same screen; role detected from backend profile

---

## 11. RegisterScreen

**File:** `ui/auth/RegisterScreen.kt`

- Full Name, Email, Phone, Password, Confirm Password
- "Register" blue button
- Role is selected on next screen (RoleSelectionScreen)

---

## 12. RoleSelectionScreen

**File:** `ui/auth/RoleSelectionScreen.kt`

Shown **only on first login**.

- Heading: "How will you use NestX?"
- 3 role cards (2-column grid):

| Card | Icon | Label | Description |
|------|------|-------|-------------|
| 1 | 🔍 | Buyer / Tenant | "Search and rent or buy property" |
| 2 | 👔 | Agent | "List properties for clients" |
| 3 | 🏗️ | Builder | "Post new construction projects" |

> Note: `admin` role is not selectable here — assigned manually by super admin.

- Selected card: blue border + bold label
- "Continue" blue full-width button

---

## 13. HomeScreen

**File:** `ui/home/HomeScreen.kt`
**ViewModel:** `HomeViewModel.kt`

### Top Bar
- Left: NestX logo (small, 32dp)
- Center: **District chip** showing current district (e.g. "Chennai ▼") — tap to change
- Right: 🔔 Bell icon

### Search Bar (below top bar)
- Full-width rounded, placeholder: "Search in [District]..."
- Tap → KeywordSearchScreen

### District Quick-Select Strip
- Horizontal scroll of district chips (top 10 by property count)
- Active district: blue filled chip
- "All TN" chip at start
- Tap chip → filters HomeScreen to that district

### Category Grid (3 columns)
| Row 1 | Row 2 | Row 3 |
|-------|-------|-------|
| 🏠 Flats for Rent | 🏢 Commercial | 🌿 Land / Plots |
| 🏡 Houses for Sale | 🏗️ New Projects | 🛋️ PG / Rooms |

Each cell taps → navigates to `DistrictListScreen` with that listing type.

### Featured Districts Section
- Section header: "Browse by District" + "View All →"
- Horizontal scroll of `DistrictCard`s:
  - District photo (aerial/landmark image)
  - District name (bold)
  - Property count badge (e.g. "128 Properties")
- Tap → `DistrictListScreen`

### Recent Searches
- Shows if user has previous searches
- Cards: thumbnail + "3 BHK Chennai • Rent" + filter summary

### Trending in Chennai (or selected district)
- Horizontal 3-card scroll of approved `PropertyCard`s
- Sorted by newest

### Verified Badge Banner
- Blue banner: "List with NestX" → for agents/builders

---

## 14. DistrictListScreen

**File:** `ui/property/DistrictListScreen.kt` ← NEW
**ViewModel:** `PropertyViewModel.kt`

Shown when user taps a category or district — shows all properties in a district for that type.

### Top Bar
- `←` Back
- Title: e.g. "Flats for Rent — Chennai"
- Filter icon → PropertyFilterScreen

### Sub-tabs
- **Rent | Buy | New Projects | Land** (blue underline on active)

### List Layout
- `LazyColumn` of `PropertyCard`s (approved only)
- Sort bar: "Newest | Price ↑ | Price ↓"
- Result count: "128 Properties in Chennai"
- Pull-to-refresh + infinite scroll pagination

---

## 15. PropertyListScreen

**File:** `ui/property/PropertyListScreen.kt`
**ViewModel:** `PropertyViewModel.kt`

Search results / filtered list view.

### Top Bar
- `←` Back
- Title: search query or category name
- Filter icon + Map toggle icon

### Tabs: **Rent | Buy | Land | New Projects**

### Cards
Each `PropertyCard` shows:
- Property photo
- **₹ price** (blue, INR format with lakh/crore shorthand)
- "X BHK • Y Bath • Z sqft"
- District + Neighborhood (gray, pin icon)
- Agent/Builder badge (bottom of card)
- Approval status badge (for agent's "My Ads" view only)

---

## 16. PropertyFilterScreen

**File:** `ui/property/PropertyFilterScreen.kt`

### Filter Sections (Tamil Nadu specific)

| Section | Options |
|---------|---------|
| District | All 38 TN districts (searchable dropdown) |
| City / Area | Text autocomplete within district |
| Property Type | Flat · House · Villa · Plot · Shop · Office · PG |
| Listing Type | Rent · Sale · New Project |
| BHK | 1 · 2 · 3 · 4 · 5+ |
| Price Range | ₹0 – ₹5Cr (rent) / ₹0 – ₹50Cr (sale) |
| Area (sqft) | Min – Max numeric |
| Furnishing | Furnished · Semi · Unfurnished |
| Amenities | Checkbox grid + "View All →" → AmenitiesScreen |
| Listed By | Agent · Builder |
| Verified Only | Toggle |

### Bottom: Blue "Show X Results" sticky button

---

## 17. AmenitiesScreen

**File:** `ui/property/AmenitiesScreen.kt`

- 2-column checkbox grid (full amenities list)
- Checked = blue filled checkbox + blue label
- Search bar to filter amenities
- Bottom: count + "Continue" button

---

## 18. PropertyDetailScreen

**File:** `ui/property/PropertyDetailScreen.kt`

### Image Area
- `ImageCarousel` — up to 20 images (unsplash mock images)
- "1/3" counter overlay
- `←` Back (top left)
- ♥ Heart + ↗ Share (below image, right)

### Detail Content (scrollable)

1. **Price** — large blue ₹ amount + "/month" or "Sale" badge
2. **Title** — bold
3. **Approval Badge** — 🟢 "Verified by NestX" (shown only if APPROVED)
4. **Location** — 📍 District · Neighborhood
5. **Stats row** — BHK · Bath · sqft
6. **Reference** — "Ref: NX-TN-00001" (small gray)
7. **Description** — expandable (3 lines default)
8. **Amenities** — horizontal chip scroll + "View All →"
9. **Map / Location Section** — placed between Amenities and Agent Card:
   - Section header: "Location" (bold) + "Directions →" TextButton (top right)
   - Address row: 📍 icon + full address string (neighborhood, district, Tamil Nadu)
   - If `latitude != null && longitude != null` → **Interactive GoogleMap** (200dp, `maps-compose`):
     - `CameraPosition` zoom 15f centred on property pin
     - Single `Marker` at property coords; title = property title, snippet = "neighborhood, district"
     - `MapUiSettings`: scrollGestures=false, zoomControls=false (tappable but non-scrollable in list)
     - Tap anywhere on map → launches geo Intent: `geo:lat,lng?q=lat,lng(title)` (opens Google Maps)
     - "Directions" button → same geo Intent
   - If coordinates are null → static fallback `Card` (140dp, SurfaceGray):
     - Centred 🗺️ map icon + "View on Map" text + address text
     - Tap opens `https://maps.google.com/?q=neighborhood,district,Tamil Nadu`
   - Rounded corners 12dp on map / fallback card
10. **Agent/Builder Card**:
    - Photo (circular 48dp)
    - Name + "Agent" or "Builder" badge
    - Phone number
    - "Listed on [date]"
11. **Similar Properties** — horizontal 3-card scroll (same district, same type)

### Bottom Contact Bar (fixed)
- 📞 **Call** — dials agent phone
- 💬 **WhatsApp** — opens WhatsApp with agent number (green)
- 📅 **Book Visit** — navigates to BookingScreen

---

## 19. MenuScreen

**File:** `ui/menu/MenuScreen.kt`

### Profile Card
- Avatar + Name + Role badge (blue=Agent, orange=Builder, gray=Buyer, purple=Admin)
- "Get Verified" button (for agents/builders)
- "Joined on [date]"

### Quick Actions
- **My Ads** — visible to Agent/Builder only; shows their listings with approval status
- **My Searches**

### Settings Groups

**Group 1 — Account**
- Profile → | Account Settings → | Notification Settings → | Security →

**Group 2 — Activity (role-dependent)**
- Agent: My Properties → | Pending Approvals → | Rejected Listings →
- Buyer: My Bookings → | Saved Searches →
- Admin: Admin Dashboard → *(prominent blue card at top)*

**Group 3 — Preferences**
- 🗺️ District → "[Current District]"
- 🌐 Language → "English / Tamil"

**Group 4 — Support**
- Support → | Call Us → | Legal Hub →

### Admin Quick-Access (admin role only)
- Shown as a **prominent blue card** at top of Menu:
  - "🛡️ Admin Dashboard" → `AdminDashboardScreen`
  - Shows badge count of pending properties

---

## 20. PostAd Flow (Agent / Builder Only)

Triggered by bottom nav "+" FAB. **Only visible to Agent and Builder roles.**
Buyer tapping "+" sees a bottom sheet: "Only agents and builders can list properties."

### Step 1: PostAdCategoryScreen
- Title: "What are you listing?"
- 4 cards:
  - 🏠 Residential (Rent)
  - 🏡 Residential (Sale)
  - 🏢 Commercial
  - 🌿 Land / Plot

### Step 2: PostAdTitleScreen
- "Enter a short title" + text field
- Placeholder: "e.g. 2 BHK Flat for Rent near Anna Nagar Metro..."

### Step 3: PostAdSubCategoryScreen
- Breadcrumb + list of sub-types (Flat / Villa / House / Shop / Office / Land etc.)

### Step 4: PostAdDetailsScreen (Full Form)

| Field | Component |
|-------|-----------|
| Images | Up to 10 photos (camera/gallery) |
| District | Dropdown — all 38 TN districts |
| City / Area | Text autocomplete |
| Address | Text field |
| Price (₹) | Numeric + INR suffix |
| Price Frequency | Monthly / Yearly / One-Time (sale) |
| BHK | Chip picker |
| Bathrooms | Chip picker |
| Area (sqft) | Numeric |
| Furnishing | Furnished / Semi / Unfurnished |
| Amenities | Multi-select (same list) |
| Description | Multiline (max 1000 chars) |
| Contact | Phone (pre-filled from profile) |

### Submit Button
- Blue "Submit for Approval" button
- On submit: `approvalStatus = PENDING`, `status = inactive`
- Toast: "Property submitted! Admin will review within 24 hours."
- Navigates back to Home

---

## 21. AdminDashboardScreen

**File:** `ui/admin/AdminDashboardScreen.kt` ← NEW
**ViewModel:** `AdminViewModel.kt`

**Visible to:** `admin` role only.

### Top Bar
- 🛡️ "Admin Dashboard" title
- Notification bell (pending count badge)

### Stats Cards Row (horizontal scroll)
| Card | Icon | Value |
|------|------|-------|
| Pending | 🟡 | e.g. "12" |
| Approved Today | 🟢 | e.g. "5" |
| Rejected | 🔴 | e.g. "3" |
| Total Listings | 📊 | e.g. "340" |
| Total Users | 👥 | e.g. "1,204" |

### Tab Row
- **Pending (12) | Approved | Rejected | All**

### Listing Cards (per tab)
Each card shows:
- Property thumbnail (left)
- Title + District + Type + ₹ Price
- Agent/Builder name + phone
- Submitted date
- Action buttons (Pending tab only):
  - ✅ "Approve" (green button)
  - ❌ "Reject" (red button)
- Tap card body → `AdminPropertyReviewScreen`

---

## 22. AdminPropertyReviewScreen

**File:** `ui/admin/AdminPropertyReviewScreen.kt` ← NEW

Full property detail view with admin action panel.

### Content
- All property details (same as PropertyDetailScreen)
- Agent info + submitted date
- Image gallery

### Admin Action Panel (bottom, fixed)
- **Current Status badge** (Pending / Approved / Rejected)
- **"Approve" blue button** → sets `approvalStatus = APPROVED` + `status = active`
- **"Reject" red button** → opens bottom sheet:
  - Heading: "Reason for Rejection"
  - Text field: type rejection reason
  - "Confirm Reject" button → saves reason; agent notified
- **"Request Changes"** text button → send message to agent

---

## 23. SavedScreen

**File:** `ui/saved/SavedScreen.kt`

- Tabs: **Saved Properties | My Searches**
- PropertyCard list with ♥ toggle
- District label on each saved card

---

## 24. BookingScreen

**File:** `ui/booking/BookingScreen.kt`

- Property summary card
- Date picker
- Time slot chips (9 AM – 6 PM, hourly)
- Message field
- "Confirm Visit Request" blue button

---

## 25. ChatListScreen

**File:** `ui/chat/ChatListScreen.kt`

- Chat rows: avatar + name + last message + time
- Direct chat between buyer ↔ agent

---

## 26. ProfileScreen

**File:** `ui/profile/ProfileScreen.kt`

- Avatar + edit
- Name, Email, Phone
- Role badge
- District preference
- Language: English / Tamil
- "Save Changes" blue button

---

## 27. Reusable Components

### PropertyCard (`ui/components/PropertyCard.kt`)
- Photo + **₹ Price** (blue, INR format) + BHK/Bath/sqft
- District · Neighborhood (gray)
- Agent/Builder badge
- Approval status chip (agent view only): 🟡 Pending / 🟢 Live / 🔴 Rejected

### DistrictCard (`ui/components/DistrictCard.kt`) ← NEW
- Background district photo
- District name overlay (white bold)
- Property count badge (e.g. "128 Properties")
- Used in HomeScreen "Browse by District" section

### PriceTag (`ui/components/PriceTag.kt`) ← NEW
- Formats `Long` INR value to "₹18K/mo", "₹65L", "₹1.2Cr" display
- Used on every card and in detail header

### ApprovalStatusBadge (`ui/components/ApprovalStatusBadge.kt`) ← NEW
- Shows colored pill: 🟡 Pending / 🟢 Approved / 🔴 Rejected
- Used in agent's "My Ads" and admin dashboard

### FilterChip — blue active state (not red)
### PriceRangeSlider — INR range (₹0 – ₹5Cr)
### BottomNavBar — blue FAB, blue active icons

---

## 28. Amenities Master List

```kotlin
enum class Amenity(val displayName: String) {
    COVERED_PARKING("Covered Parking"),
    SECURITY("Security"),
    LIFT("Lift"),
    POWER_BACKUP("Power Backup"),
    WATER_SUPPLY("24-Hour Water Supply"),
    BOREWELL("Borewell"),
    CCTV("CCTV Surveillance"),
    CHILDREN_PLAY_AREA("Children's Play Area"),
    SHARED_GYM("Gym"),
    SWIMMING_POOL("Swimming Pool"),
    CLUB_HOUSE("Club House"),
    PARK("Park / Garden"),
    PRIVATE_GARDEN("Private Garden"),
    PRIVATE_POOL("Private Pool"),
    PRIVATE_GYM("Private Gym"),
    MODULAR_KITCHEN("Modular Kitchen"),
    BUILTIN_WARDROBES("Built-in Wardrobes"),
    BUILTIN_KITCHEN_APPLIANCES("Built-in Kitchen Appliances"),
    BALCONY("Balcony"),
    TERRACE("Terrace"),
    STILT_PARKING("Stilt Parking"),
    RAINWATER_HARVESTING("Rainwater Harvesting"),
    SOLAR_PANELS("Solar Panels"),
    VASTU_COMPLIANT("Vastu Compliant"),
    INTERNET("High-Speed Internet"),
    INTERCOM("Intercom"),
    FIRE_SAFETY("Fire Safety System"),
    HOSPITAL_NEARBY("Hospital Nearby"),
    SCHOOL_NEARBY("School Nearby"),
    SHOPPING_NEARBY("Shopping Centre Nearby")
}
```

*(Updated for Indian real estate context — replaced UAE-specific amenities)*

---

## 29. Dependencies (build.gradle)

```kotlin
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Google Auth + Maps
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Accompanist (pager + swipe refresh)
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")

    // Coroutines + Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

---

## 30. Key Developer Notes

1. `USE_MOCK_DATA = true` in `BuildConfig` (debug) → all ViewModels branch between MockData and real API.
   **Post Ad in mock mode:** `PostAdViewModel.submitAd()` skips real POST, injects PENDING property into `MockData._userSubmitted`. **Do NOT call the real `/properties` API in debug** — it returns 401 (no JWT).
   **Listing/Detail in mock mode:** `PropertyViewModel` + `HomeViewModel` apply filters client-side on `MockData.approvedProperties`.
   **When backend is running** set `USE_MOCK_DATA = false` in `build.gradle.kts` — all ViewModels switch to real API automatically.
2. Currency: **always `Long` in rupees**, format via `CurrencyFormatter.short()` on cards
3. District is a **required filter** — `PropertyViewModel` always filters by selected district
4. Default district = `DataStore["selected_district"] ?: "All TN"`
5. `approvalStatus = APPROVED` is a **mandatory filter** on all public-facing property queries
6. Admin role is **never self-assignable** — set via Supabase service role directly or by another admin
7. Agent "My Ads" passes `owner_id = current_user.id` (no approval filter) to show own pending/rejected
8. Admin dashboard uses `service_role_key` on backend — never expose to Android APK
9. Bottom nav "+" FAB: check `currentUser.role in [AGENT, BUILDER]` before showing wizard; otherwise show "Upgrade account" bottom sheet
10. All mock image URLs use `unsplash.com` — will be replaced by Supabase Storage URLs in production
11. `DistrictListScreen` and `PropertyListScreen` are separate screens — district list shows district-level overview, property list shows individual cards
12. Amenities enum updated for Indian context (removed UAE-specific items, added Indian ones)
13. **District → Area filter chain**: `TamilNaduData.districtAreas` maps every TN district to its known localities. `PropertyFilterScreen` shows district dropdown first, then area dropdown populates based on selected district. `PropertyViewModel.currentFilter.area` = `neighborhood` column in DB.
14. **DB `district` column**: migration `008_add_district.sql` adds `TEXT district` to `properties` table. Backend router accepts `?district=Karur` query param and maps to `ilike` filter on `district` column. Seed data provides 20+ Tamil Nadu properties with real district + neighborhood values.
15. **HomeScreen live search**: `HomeViewModel.searchQuery` flows → filtered list updates on every character. In mock mode: client-side keyword filter. In API mode: `repo.listProperties(keyword = query)`.
    **HomeScreen search bar is interactive** — NOT `readOnly`. Collecting `viewModel.searchQuery` as state; calling `viewModel.onSearchQueryChange(it)` on every keystroke. When `state.searchResults.isNotEmpty() || state.isSearching`, a `SearchResultsOverlay` Card appears below the search bar (positioned with `padding(top)` to clear the sticky header). Clear "✕" button calls `viewModel.clearSearch()`. `onSearchClick` param retained for full-screen search nav fallback.
16. **PropertyFilterState**: data class in `Property.kt` holds all active filter values (listingType, district, area/neighborhood, price range, bedrooms, bathrooms, furnishing, propertyType, amenities, keyword). `PropertyViewModel` owns this state and exposes `currentFilter: StateFlow<PropertyFilterState>`.
17. **HomeScreen layout structure**: `Box` root → `Column` child with `HomeTopBar` + `SearchBarRow` + `LazyColumn` (categories, districts, property sections). `SearchResultsOverlay` is a second child of `Box` (rendered above all content) when search is active.
