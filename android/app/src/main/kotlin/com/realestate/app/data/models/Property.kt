package com.realestate.app.data.models

import com.google.gson.annotations.SerializedName
import com.realestate.app.utils.CurrencyFormatter

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class ApprovalStatus { PENDING, APPROVED, REJECTED }

enum class ListingType {
    RENT, SALE, OFF_PLAN, HOLIDAY_STAY, GROUND,
    CONTRACTOR,    // Construction contractor profiles
    MAINTENANCE,   // Maintenance service provider profiles
}

enum class PropertyType {
    // ── Residential (Buy / Rent) ───────────────────────────────────────────────
    APARTMENT, VILLA, TOWNHOUSE, PENTHOUSE, INDEPENDENT_HOUSE,
    RESIDENTIAL_BUILDING, VILLA_COMPOUND, RESIDENTIAL_FLOOR, FARMHOUSE,
    // ── Commercial (Buy / Rent) ───────────────────────────────────────────────
    OFFICE, SHOP, SHOWROOM, WAREHOUSE, COMMERCIAL_BUILDING, COMMERCIAL_FLOOR,
    // ── Land / Agricultural / Industrial ──────────────────────────────────────
    AGRICULTURAL_LAND, INDUSTRIAL_LAND, INDUSTRIAL_PROPERTY, LAND,
    // ── Rental-specific ───────────────────────────────────────────────────────
    HOTEL, RESORT, HOME_STAY, PG_ROOM, ROOM,
    // ── Holiday Stay ─────────────────────────────────────────────────────────
    ENTIRE_HOME,
    // ── Ground / Sports ──────────────────────────────────────────────────────
    CRICKET_GROUND, FOOTBALL, BADMINTON, SWIMMING_POOL,
    OTHER_OPEN_GROUND, OTHER_CLOSED_GROUND,
    // ── Construction Contractor types ─────────────────────────────────────────
    CIVIL_CONTRACTOR, BUILDER, ARCHITECT, STRUCTURAL_ENGINEER, INTERIOR_DESIGNER,
    PLUMBING_CONTRACTOR, ELECTRICAL_CONTRACTOR, PAINTING_CONTRACTOR,
    FALSE_CEILING, TILES_CONTRACTOR, ROOFING, LANDSCAPING,
    // ── Maintenance Service types ─────────────────────────────────────────────
    ELECTRICIAN, PLUMBER, CARPENTER, AC_SERVICE, CCTV_SERVICE,
    CLEANING_SERVICE, PAINTING_SERVICE, PEST_CONTROL, BOREWELL, WATER_TANK_CLEANING,
    // ── Generic ───────────────────────────────────────────────────────────────
    OTHER,
}

enum class ContractorType(val displayName: String) {
    CIVIL_CONTRACTOR("Civil Contractor"),
    BUILDER("Builder"),
    ARCHITECT("Architect"),
    STRUCTURAL_ENGINEER("Structural Engineer"),
    INTERIOR_DESIGNER("Interior Designer"),
    PLUMBING("Plumbing"),
    ELECTRICAL("Electrical"),
    PAINTING("Painting"),
    FALSE_CEILING("False Ceiling"),
    TILES("Tiles"),
    ROOFING("Roofing"),
    LANDSCAPING("Landscaping");

    companion object {
        fun all() = values().toList()
        fun from(key: String?) = values().find { it.name.equals(key, ignoreCase = true) }
    }
}

enum class MaintenanceServiceType(val displayName: String) {
    ELECTRICIAN("Electrician"),
    PLUMBER("Plumber"),
    CARPENTER("Carpenter"),
    AC_SERVICE("AC Service"),
    CCTV("CCTV"),
    CLEANING("Cleaning"),
    PAINTING("Painting"),
    PEST_CONTROL("Pest Control"),
    BOREWELL("Borewell"),
    WATER_TANK_CLEANING("Water Tank Cleaning");

    companion object {
        fun all() = values().toList()
        fun from(key: String?) = values().find { it.name.equals(key, ignoreCase = true) }
    }
}

enum class Furnishing { FURNISHED, SEMI_FURNISHED, UNFURNISHED }

enum class Amenity(val displayName: String) {
    // ── Standard amenities (must match backend VALID_AMENITIES exactly) ───────
    MAIDS_ROOM("Maids Room"),
    STUDY("Study"),
    CENTRAL_AC_HEATING("Central A/C & Heating"),
    BALCONY("Balcony"),
    PRIVATE_GARDEN("Private Garden"),
    PRIVATE_POOL("Private Pool"),
    PRIVATE_GYM("Private Gym"),
    PRIVATE_JACUZZI("Private Jacuzzi"),
    SHARED_POOL("Shared Pool"),
    SHARED_SPA("Shared Spa"),
    SHARED_GYM("Shared Gym"),
    SECURITY("Security"),
    CONCIERGE_SERVICE("Concierge Service"),
    MAID_SERVICE("Maid Service"),
    COVERED_PARKING("Covered Parking"),
    BUILTIN_WARDROBES("Built-in Wardrobes"),
    WALKIN_CLOSET("Walk-in Closet"),
    BUILTIN_KITCHEN_APPLIANCES("Built-in Kitchen Appliances"),
    VIEW_OF_WATER("View of Water"),
    VIEW_OF_LANDMARK("View of Landmark"),
    PETS_ALLOWED("Pets Allowed"),
    DOUBLE_GLAZED_WINDOWS("Double Glazed Windows"),
    DAY_CARE_CENTER("Day Care Center"),
    ELECTRICITY_BACKUP("Electricity Backup"),
    FIRST_AID_MEDICAL_CENTER("First Aid Medical Center"),
    SERVICE_ELEVATORS("Service Elevators"),
    PRAYER_ROOM("Prayer Room"),
    LAUNDRY_ROOM("Laundry Room"),
    // ── Tamil Nadu / India specific ───────────────────────────────────────────
    LIFT("Lift"),
    SOLAR_POWER("Solar Power"),
    RAINWATER_HARVESTING("Rainwater Harvesting"),
    VASTU_COMPLIANT("Vastu Compliant"),
    GATED_COMMUNITY("Gated Community"),
    BORE_WELL("Bore Well"),
    GENERATOR("Generator"),
    CCTV("CCTV"),
    INTERCOM("Intercom"),
    CHILDREN_PLAY_AREA("Children Play Area"),
    CLUB_HOUSE("Club House"),
    JOGGING_TRACK("Jogging Track");

    companion object {
        fun from(name: String?): Amenity? = values().find { it.name == name }
    }
}

// ─── Property data class ──────────────────────────────────────────────────────

data class Property(
    @SerializedName("id")              val id: String            = "",
    @SerializedName("owner_id")        val ownerId: String       = "",
    @SerializedName("title")           val title: String?        = null,
    @SerializedName("description")     val description: String?  = null,
    @SerializedName("price")           val price: Long           = 0L,          // ₹ in rupees
    @SerializedName("price_frequency") val priceFrequency: String? = null,
    @SerializedName("property_type")   val propertyType: String? = null,
    @SerializedName("listing_type")    val listingType: String   = "rent",
    @SerializedName("bedrooms")        val bedrooms: Int?        = null,
    @SerializedName("bathrooms")       val bathrooms: Int?       = null,
    @SerializedName("area_sqft")       val areaSqft: Double?     = null,
    @SerializedName("rate_per_sqft")   val ratePerSqft: Double?  = null,        // auto-computed by backend
    @SerializedName("district")        val district: String?     = null,
    @SerializedName("city")            val city: String?         = null,
    @SerializedName("neighborhood")    val neighborhood: String? = null,
    @SerializedName("address")         val address: String?      = null,
    @SerializedName("latitude")        val latitude: Double?     = null,
    @SerializedName("longitude")       val longitude: Double?    = null,
    // ── Media ──────────────────────────────────────────────────────────────
    @SerializedName("images")          val images: List<String>  = emptyList(),
    @SerializedName("video_url")       val videoUrl: String?     = null,
    @SerializedName("youtube_url")     val youtubeUrl: String?   = null,
    @SerializedName("instagram_url")   val instagramUrl: String? = null,
    // ── Property details ───────────────────────────────────────────────────
    @SerializedName("amenities")       val amenities: List<String>   = emptyList(),
    @SerializedName("furnishing")      val furnishing: String        = "unfurnished",
    // ── Rent-specific ──────────────────────────────────────────────────────
    @SerializedName("deposit")         val deposit: Long?            = null,
    @SerializedName("availability_date") val availabilityDate: String? = null,
    // ── Nearby places ──────────────────────────────────────────────────────
    @SerializedName("nearby_schools")  val nearbySchools: List<String>   = emptyList(),
    @SerializedName("nearby_hospitals")val nearbyHospitals: List<String> = emptyList(),
    // ── Documents ──────────────────────────────────────────────────────────
    @SerializedName("document_urls")   val documentUrls: List<String>    = emptyList(),
    // ── Contractor extras ──────────────────────────────────────────────────
    @SerializedName("company_profile") val companyProfile: String?       = null,
    @SerializedName("previous_projects")val previousProjects: List<String> = emptyList(),
    @SerializedName("rating_avg")      val ratingAvg: Float              = 0f,
    @SerializedName("rating_count")    val ratingCount: Int              = 0,
    // ── Who / contact ──────────────────────────────────────────────────────
    @SerializedName("listed_by")       val listedBy: String              = "agent",
    @SerializedName("agent_name")      val agentName: String             = "",
    @SerializedName("agent_phone")     val agentPhone: String            = "",
    @SerializedName("agent_photo")     val agentPhoto: String            = "",
    @SerializedName("whatsapp_number") val whatsappNumber: String?       = null,
    // ── Status / admin ─────────────────────────────────────────────────────
    @SerializedName("approval_status") val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    @SerializedName("rejection_reason")val rejectionReason: String?       = null,
    @SerializedName("reference_id")    val referenceId: String            = "",
    @SerializedName("is_verified")     val isVerified: Boolean            = false,
    @SerializedName("is_featured")     val isFeatured: Boolean            = false,
    @SerializedName("status")          val status: String                 = "inactive",
    @SerializedName("created_at")      val createdAt: String              = "",
    @SerializedName("updated_at")      val updatedAt: String              = "",
    /** Category-specific extra data (ground/contractor/holiday-stay). */
    @SerializedName("metadata")        val metadata: Map<String, Any>?   = null,
) {
    val isApproved: Boolean get() = approvalStatus == ApprovalStatus.APPROVED && status == "active"
    val isPending:  Boolean get() = approvalStatus == ApprovalStatus.PENDING
    val isRejected: Boolean get() = approvalStatus == ApprovalStatus.REJECTED

    val priceDisplay: String get() = CurrencyFormatter.format(price, priceFrequency)
    val priceShort:   String get() = CurrencyFormatter.short(price)

    val listingTypeLabel: String get() = when (listingType) {
        "rent"         -> "For Rent"
        "sale"         -> "For Sale"
        "off_plan"     -> "Off-Plan"
        "holiday_stay" -> "Holiday Stay"
        "ground"       -> "Ground"
        "contractor"   -> "Construction"
        "maintenance"  -> "Maintenance"
        else           -> listingType.replaceFirstChar { it.uppercase() }
    }

    val propertyTypeLabel: String get() = propertyType.orEmpty()
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    val displaySubCategory: String get() = metadata?.get("sub_category")?.toString()
        ?: propertyTypeLabel

    val isHolidayStay:   Boolean get() = listingType == "holiday_stay"
    val isGround:        Boolean get() = listingType == "ground"
    val isContractor:    Boolean get() = listingType == "contractor"
    val isMaintenance:   Boolean get() = listingType == "maintenance"
    val isRent:          Boolean get() = listingType == "rent"
    val isSale:          Boolean get() = listingType == "sale"

    val bhkLabel: String get() = when {
        (bedrooms ?: 0) == 0 && propertyType?.contains("land") == true -> "Plot"
        (bedrooms ?: 0) == 0 -> "Commercial"
        bedrooms == 1 -> "1 BHK"
        else -> "$bedrooms BHK"
    }

    val statsLine: String get() {
        val parts = mutableListOf<String>()
        if ((bedrooms ?: 0) > 0)     parts.add(bhkLabel)
        if ((bathrooms ?: 0) > 0)    parts.add("${bathrooms} Bath")
        if ((areaSqft ?: 0.0) > 0.0) parts.add("${areaSqft!!.toInt()} sqft")
        return parts.joinToString(" • ")
    }

    /** WhatsApp deep link URL for the listing's contact number. */
    val whatsappUrl: String? get() = whatsappNumber
        ?.filter { it.isDigit() }
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://wa.me/91$it" }

    /** Rate per sqft display string (server-computed or client-fallback). */
    val ratePerSqftDisplay: String? get() {
        val rate = ratePerSqft
            ?: if ((areaSqft ?: 0.0) > 0.0) price.toDouble() / areaSqft!! else null
        return rate?.let { "₹${"%,.0f".format(it)}/sqft" }
    }
}

// ─── Filter State ─────────────────────────────────────────────────────────────

/**
 * Holds all active filter values for the PropertyFilterScreen.
 * PropertyViewModel owns this state and exposes it as a StateFlow.
 */
data class PropertyFilterState(
    val listingType:    String      = "rent",
    val district:       String      = "",           // "" = All TN
    val area:           String      = "",           // neighborhood within district
    val minPrice:       Float       = 0f,
    val maxPrice:       Float       = 20_000_000f,
    val minArea:        Float?      = null,          // sqft min
    val maxArea:        Float?      = null,          // sqft max
    val bedrooms:       Int?        = null,
    val bathrooms:      Int?        = null,
    val furnishing:     String?     = null,
    val propertyType:   String?     = null,
    val amenities:      List<String> = emptyList(),
    val keyword:        String      = "",
    // Location radius
    val radiusKm:       Int?        = null,          // 10 | 50 | 100
    val centerLat:      Double?     = null,
    val centerLng:      Double?     = null,
    // Category-specific metadata filters
    /** "construction" | "maintenance" | null */
    val workCategory:   String?     = null,
    /** contractor sub-type e.g. "architect" | "civil_contractor" */
    val contractorType: String?     = null,
    /** maintenance service type e.g. "electrician" | "plumber" */
    val serviceType:    String?     = null,
) {
    val isActive: Boolean get() =
        district.isNotBlank() || area.isNotBlank() ||
        minPrice > 0f || maxPrice < 20_000_000f ||
        minArea != null || maxArea != null ||
        bedrooms != null || bathrooms != null ||
        furnishing != null || propertyType != null ||
        amenities.isNotEmpty() || keyword.isNotBlank() ||
        radiusKm != null ||
        workCategory != null || contractorType != null || serviceType != null
}

// ─── Paginated response ───────────────────────────────────────────────────────

data class PropertyListResponse(
    @SerializedName("data")     val data: List<Property>  = emptyList(),
    @SerializedName("total")    val total: Int             = 0,
    @SerializedName("page")     val page: Int              = 1,
    @SerializedName("limit")    val limit: Int             = 20,
    @SerializedName("has_next") val hasNext: Boolean       = false,
)

// ─── Create request ───────────────────────────────────────────────────────────

/** Request body sent when a user posts a new property listing. */
data class PropertyCreateRequest(
    // ── Core ──────────────────────────────────────────────────────────────
    @SerializedName("title")           val title: String,
    @SerializedName("description")     val description: String?          = null,
    @SerializedName("price")           val price: Double,
    @SerializedName("price_frequency") val priceFrequency: String        = "monthly",
    @SerializedName("property_type")   val propertyType: String,
    @SerializedName("listing_type")    val listingType: String,

    // ── Property details ──────────────────────────────────────────────────
    @SerializedName("bedrooms")        val bedrooms: Int?                 = null,
    @SerializedName("bathrooms")       val bathrooms: Int?                = null,
    @SerializedName("area_sqft")       val areaSqft: Double?              = null,
    @SerializedName("furnishing")      val furnishing: String             = "unfurnished",

    // ── Location ──────────────────────────────────────────────────────────
    @SerializedName("address")         val address: String?               = null,
    @SerializedName("neighborhood")    val neighborhood: String?          = null,
    @SerializedName("district")        val district: String?              = null,
    @SerializedName("city")            val city: String?                  = null,
    @SerializedName("latitude")        val latitude: Double?              = null,
    @SerializedName("longitude")       val longitude: Double?             = null,

    // ── Media ─────────────────────────────────────────────────────────────
    @SerializedName("video_url")       val videoUrl: String?              = null,
    @SerializedName("youtube_url")     val youtubeUrl: String?            = null,
    @SerializedName("instagram_url")   val instagramUrl: String?          = null,

    // ── Rent-specific ─────────────────────────────────────────────────────
    @SerializedName("deposit")         val deposit: Double?               = null,
    @SerializedName("availability_date") val availabilityDate: String?    = null,

    // ── Nearby places (Buy / Rent) ────────────────────────────────────────
    @SerializedName("nearby_schools")  val nearbySchools: List<String>    = emptyList(),
    @SerializedName("nearby_hospitals")val nearbyHospitals: List<String>  = emptyList(),

    // ── Documents (optional) ──────────────────────────────────────────────
    @SerializedName("document_urls")   val documentUrls: List<String>     = emptyList(),

    // ── Contractor extras ─────────────────────────────────────────────────
    @SerializedName("company_profile") val companyProfile: String?        = null,

    // ── Amenities + posting details ───────────────────────────────────────
    @SerializedName("amenities")       val amenities: List<String>        = emptyList(),
    @SerializedName("listed_by")       val listedBy: String               = "agent",

    // ── Contact ───────────────────────────────────────────────────────────
    @SerializedName("agent_name")      val agentName: String?             = null,
    @SerializedName("agent_phone")     val agentPhone: String?            = null,
    @SerializedName("agent_photo")     val agentPhoto: String?            = null,
    @SerializedName("whatsapp_number") val whatsappNumber: String?        = null,

    // ── Category-specific JSONB metadata ──────────────────────────────────
    @SerializedName("metadata")        val metadata: Map<String, String>? = null,
)