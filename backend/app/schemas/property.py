"""
schemas/property.py — Property-related Pydantic schemas

Supports all 4 modules:
  1. Buy   (listing_type = sale)         — Residential / Commercial / Land / Farmhouse
  2. Rent  (listing_type = rent)         — Residential / Commercial / Hotel-Resort / Home Stay / PG
  3. Construction (listing_type = contractor, work_category = construction)
  4. Maintenance  (listing_type = maintenance OR contractor, work_category = maintenance)
"""

from typing import Optional, List, Dict, Any
from datetime import date, datetime
from pydantic import BaseModel, field_validator, model_validator, HttpUrl
from enum import Enum


# ─── Enums / constants ────────────────────────────────────────────────────────

class ListingType(str, Enum):
    rent         = "rent"
    sale         = "sale"
    off_plan     = "off_plan"
    holiday_stay = "holiday_stay"   # Holiday Stay listings
    ground       = "ground"         # Ground / Sports venue listings
    contractor   = "contractor"     # Construction contractor profiles
    maintenance  = "maintenance"    # Maintenance service provider profiles


class PropertyType(str, Enum):
    # ── Residential (Buy / Rent) ───────────────────────────────────────────────
    apartment             = "apartment"
    villa                 = "villa"
    townhouse             = "townhouse"
    penthouse             = "penthouse"
    independent_house     = "independent_house"
    residential_building  = "residential_building"
    villa_compound        = "villa_compound"
    residential_floor     = "residential_floor"
    farmhouse             = "farmhouse"
    # ── Commercial (Buy / Rent) ───────────────────────────────────────────────
    office                = "office"
    shop                  = "shop"
    showroom              = "showroom"
    warehouse             = "warehouse"
    commercial_building   = "commercial_building"
    commercial_floor      = "commercial_floor"
    # ── Land / Agricultural / Industrial (Buy) ────────────────────────────────
    agricultural_land     = "agricultural_land"
    industrial_land       = "industrial_land"
    industrial_property   = "industrial_property"
    land                  = "land"
    # ── Rental-specific ───────────────────────────────────────────────────────
    hotel                 = "hotel"
    resort                = "resort"
    home_stay             = "home_stay"
    pg_room               = "pg_room"
    room                  = "room"
    # ── Holiday Stay ─────────────────────────────────────────────────────────
    entire_home           = "entire_home"
    # ── Ground / Sports ──────────────────────────────────────────────────────
    cricket_ground        = "cricket_ground"
    football              = "football"
    badminton             = "badminton"
    swimming_pool         = "swimming_pool"
    other_open_ground     = "other_open_ground"
    other_closed_ground   = "other_closed_ground"
    # ── Construction Contractor types ─────────────────────────────────────────
    civil_contractor      = "civil_contractor"
    builder               = "builder"
    architect             = "architect"
    structural_engineer   = "structural_engineer"
    interior_designer     = "interior_designer"
    plumbing_contractor   = "plumbing_contractor"
    electrical_contractor = "electrical_contractor"
    painting_contractor   = "painting_contractor"
    false_ceiling         = "false_ceiling"
    tiles_contractor      = "tiles_contractor"
    roofing               = "roofing"
    landscaping           = "landscaping"
    # ── Maintenance Service types ─────────────────────────────────────────────
    electrician           = "electrician"
    plumber               = "plumber"
    carpenter             = "carpenter"
    ac_service            = "ac_service"
    cctv_service          = "cctv_service"
    cleaning_service      = "cleaning_service"
    painting_service      = "painting_service"
    pest_control          = "pest_control"
    borewell              = "borewell"
    water_tank_cleaning   = "water_tank_cleaning"
    # ── Fallback/Sub-category Custom mappings ────────────────────────────────
    residential           = "residential"
    commercial            = "commercial"
    hotel_resort          = "hotel_resort"
    hotel___resort        = "hotel___resort"
    home_stay_pg          = "home_stay_pg"
    home_stay___pg        = "home_stay___pg"
    industrial_properties = "industrial_properties"
    # ── Generic ──────────────────────────────────────────────────────────────
    other                 = "other"


class ContractorType(str, Enum):
    """Sub-type for Construction contractor listings."""
    civil_contractor    = "civil_contractor"
    builder             = "builder"
    architect           = "architect"
    structural_engineer = "structural_engineer"
    interior_designer   = "interior_designer"
    plumbing            = "plumbing"
    electrical          = "electrical"
    painting            = "painting"
    false_ceiling       = "false_ceiling"
    tiles               = "tiles"
    roofing             = "roofing"
    landscaping         = "landscaping"


class MaintenanceServiceType(str, Enum):
    """Sub-type for Maintenance service listings."""
    electrician        = "electrician"
    plumber            = "plumber"
    carpenter          = "carpenter"
    ac_service         = "ac_service"
    cctv               = "cctv"
    cleaning           = "cleaning"
    painting           = "painting"
    pest_control       = "pest_control"
    borewell           = "borewell"
    water_tank_cleaning = "water_tank_cleaning"


class PriceFrequency(str, Enum):
    yearly  = "yearly"
    monthly = "monthly"
    weekly  = "weekly"
    daily   = "daily"
    per_sqft = "per_sqft"


class Furnishing(str, Enum):
    furnished      = "furnished"
    unfurnished    = "unfurnished"
    semi_furnished = "semi_furnished"


class CompletionStatus(str, Enum):
    ready    = "ready"
    off_plan = "off_plan"


class PropertyStatus(str, Enum):
    active   = "active"
    sold     = "sold"
    rented   = "rented"
    inactive = "inactive"


class ListedBy(str, Enum):
    owner      = "owner"       # property owner (sale/rent)
    landlord   = "landlord"    # landlord (rent)
    agent      = "agent"       # real estate agent
    agency     = "agency"      # real estate agency
    developer  = "developer"   # property developer
    builder    = "builder"     # builder/contractor (construction)
    individual = "individual"  # individual contractor
    company    = "company"     # contracting company


class WorkCategory(str, Enum):
    """Work category for contractor / maintenance listings.
    Used by the dashboard Quick-Access slider to filter Construction vs Maintenance.
    """
    construction = "construction"   # Build your Property (new builds, extensions)
    maintenance  = "maintenance"    # Maintain Your Property (repairs, plumbing, AC)


class ApprovalStatus(str, Enum):
    pending  = "pending"
    approved = "approved"
    rejected = "rejected"


class SortBy(str, Enum):
    price_asc  = "price_asc"
    price_desc = "price_desc"
    newest     = "newest"
    oldest     = "oldest"
    rating     = "rating"


# ─── Amenities master list ────────────────────────────────────────────────────

VALID_AMENITIES = {
    # Residential
    "MAIDS_ROOM", "STUDY", "CENTRAL_AC_HEATING", "BALCONY",
    "PRIVATE_GARDEN", "PRIVATE_POOL", "PRIVATE_GYM", "PRIVATE_JACUZZI",
    "SHARED_POOL", "SHARED_SPA", "SHARED_GYM", "SECURITY",
    "CONCIERGE_SERVICE", "MAID_SERVICE", "COVERED_PARKING", "BUILTIN_WARDROBES",
    "WALKIN_CLOSET", "BUILTIN_KITCHEN_APPLIANCES",
    "VIEW_OF_WATER", "VIEW_OF_LANDMARK", "PETS_ALLOWED", "DOUBLE_GLAZED_WINDOWS",
    "DAY_CARE_CENTER", "ELECTRICITY_BACKUP", "FIRST_AID_MEDICAL_CENTER",
    "SERVICE_ELEVATORS", "PRAYER_ROOM", "LAUNDRY_ROOM",
    # Tamil Nadu / India specific
    "LIFT", "SOLAR_POWER", "RAINWATER_HARVESTING", "VASTU_COMPLIANT",
    "GATED_COMMUNITY", "BORE_WELL", "GENERATOR", "CCTV", "INTERCOM",
    "CHILDREN_PLAY_AREA", "CLUB_HOUSE", "JOGGING_TRACK",
}


# ─── Create / Update schemas ──────────────────────────────────────────────────

class PropertyCreate(BaseModel):
    # ── Core ──────────────────────────────────────────────────────────────────
    title:            str
    description:      Optional[str]   = None
    price:            float
    price_frequency:  PriceFrequency  = PriceFrequency.yearly
    property_type:    PropertyType
    listing_type:     ListingType
    bedrooms:         Optional[int]   = None
    bathrooms:        Optional[int]   = None
    area_sqft:        Optional[float] = None
    address:          Optional[str]   = None
    neighborhood:     Optional[str]   = None   # area/locality within district
    district:         Optional[str]   = None   # Tamil Nadu district name
    city:             Optional[str]   = None
    latitude:         Optional[float] = None
    longitude:        Optional[float] = None

    # ── Media ─────────────────────────────────────────────────────────────────
    images:           Optional[List[str]] = []
    video_url:        Optional[str]   = None   # direct video file URL
    youtube_url:      Optional[str]   = None   # YouTube video link
    instagram_url:    Optional[str]   = None   # Instagram reel link

    # ── Property-specific fields ──────────────────────────────────────────────
    amenities:        Optional[List[str]]  = []
    furnishing:       Furnishing           = Furnishing.unfurnished
    completion_status: Optional[CompletionStatus] = None
    payment_plan:     Optional[str]        = None
    handover_date:    Optional[date]       = None
    developer_name:   Optional[str]        = None
    permit_number:    Optional[str]        = None
    rera_number:      Optional[str]        = None
    brn_dld:          Optional[str]        = None
    zone_name:        Optional[str]        = None

    # ── Rent-specific ─────────────────────────────────────────────────────────
    deposit:              Optional[float]  = None   # Security deposit (INR)
    availability_date:    Optional[date]   = None   # Available from date

    # ── Nearby places (for Buy / Rent listings) ───────────────────────────────
    nearby_schools:    Optional[List[str]] = []    # e.g. ["St. Joseph's School"]
    nearby_hospitals:  Optional[List[str]] = []    # e.g. ["Apollo Hospital"]

    # ── Documents (optional — ownership proof, NOC, etc.) ─────────────────────
    document_urls:    Optional[List[str]]  = []

    # ── Contractor / Maintenance-specific ─────────────────────────────────────
    company_profile:  Optional[str]        = None  # About the company
    previous_projects: Optional[List[str]] = []    # Project image/doc URLs (up to 5)

    # ── Who is listing ────────────────────────────────────────────────────────
    listed_by:        ListedBy             = ListedBy.landlord
    agency_id:        Optional[str]        = None

    # ── Contact details ───────────────────────────────────────────────────────
    agent_name:       Optional[str]        = None
    agent_phone:      Optional[str]        = None
    agent_photo:      Optional[str]        = None
    whatsapp_number:  Optional[str]        = None   # Required for most listing types

    # ── Category-specific extra fields (JSONB metadata column) ────────────────
    # ground      → ground_type, surface, length_m, width_m, capacity, facilities
    # contractor  → work_category, contractor_type, work_types, experience_yrs, team_size, pricing_model
    # maintenance → service_type, service_area_districts, pricing_model
    # holiday_stay → stay_type, max_guests, check_in, check_out, min_nights
    # rent        → stay_type (for hotel/resort/home_stay)
    metadata:         Optional[Dict[str, Any]] = None

    # ── Admin ─────────────────────────────────────────────────────────────────
    approval_status:  ApprovalStatus = ApprovalStatus.pending

    @field_validator("amenities")
    @classmethod
    def validate_amenities(cls, v: List[str]) -> List[str]:
        if v:
            invalid = [a for a in v if a not in VALID_AMENITIES]
            if invalid:
                raise ValueError(f"Invalid amenities: {invalid}. Must be from master list.")
        return v

    @field_validator("images")
    @classmethod
    def validate_images(cls, v: List[str]) -> List[str]:
        if v and len(v) > 10:
            raise ValueError("Maximum 10 images allowed per property")
        return v

    @field_validator("previous_projects")
    @classmethod
    def validate_prev_projects(cls, v: List[str]) -> List[str]:
        if v and len(v) > 5:
            raise ValueError("Maximum 5 previous project images allowed")
        return v

    @model_validator(mode="after")
    def validate_whatsapp(self) -> "PropertyCreate":
        """WhatsApp required for owner/landlord/contractor/maintenance listings."""
        needs_whatsapp = self.listing_type in (
            ListingType.sale, ListingType.rent,
            ListingType.contractor, ListingType.maintenance,
        ) and self.listed_by in (
            ListedBy.owner, ListedBy.landlord, ListedBy.builder,
            ListedBy.individual, ListedBy.company,
        )
        if needs_whatsapp and not self.whatsapp_number:
            raise ValueError("WhatsApp number is required for owner/landlord/contractor listings")
        return self

    @model_validator(mode="after")
    def validate_category_metadata(self) -> "PropertyCreate":
        """Ensure critical category-specific fields are present in metadata."""
        if self.listing_type == ListingType.ground:
            meta = self.metadata or {}
            if not meta.get("ground_type"):
                raise ValueError("metadata.ground_type is required for Ground listings")
        if self.listing_type in (ListingType.contractor, ListingType.maintenance):
            meta = self.metadata or {}
            if not meta.get("work_category"):
                raise ValueError("metadata.work_category is required for Contractor/Maintenance listings")
        return self


class PropertyUpdate(BaseModel):
    title:             Optional[str]        = None
    description:       Optional[str]        = None
    price:             Optional[float]      = None
    price_frequency:   Optional[PriceFrequency] = None
    property_type:     Optional[PropertyType] = None
    listing_type:      Optional[ListingType] = None
    bedrooms:          Optional[int]        = None
    bathrooms:         Optional[int]        = None
    area_sqft:         Optional[float]      = None
    address:           Optional[str]        = None
    neighborhood:      Optional[str]        = None
    city:              Optional[str]        = None
    latitude:          Optional[float]      = None
    longitude:         Optional[float]      = None
    images:            Optional[List[str]]  = None
    video_url:         Optional[str]        = None
    youtube_url:       Optional[str]        = None
    instagram_url:     Optional[str]        = None
    amenities:         Optional[List[str]]  = None
    furnishing:        Optional[Furnishing] = None
    completion_status: Optional[CompletionStatus] = None
    payment_plan:      Optional[str]        = None
    handover_date:     Optional[date]       = None
    developer_name:    Optional[str]        = None
    permit_number:     Optional[str]        = None
    rera_number:       Optional[str]        = None
    status:            Optional[PropertyStatus] = None
    deposit:           Optional[float]      = None
    availability_date: Optional[date]       = None
    nearby_schools:    Optional[List[str]]  = None
    nearby_hospitals:  Optional[List[str]]  = None
    document_urls:     Optional[List[str]]  = None
    company_profile:   Optional[str]        = None
    previous_projects: Optional[List[str]]  = None
    whatsapp_number:   Optional[str]        = None
    metadata:          Optional[Dict[str, Any]] = None

    @field_validator("amenities")
    @classmethod
    def validate_amenities(cls, v: Optional[List[str]]) -> Optional[List[str]]:
        if v:
            invalid = [a for a in v if a not in VALID_AMENITIES]
            if invalid:
                raise ValueError(f"Invalid amenities: {invalid}")
        return v


# ─── Response schemas ─────────────────────────────────────────────────────────

class AgencyMini(BaseModel):
    id:          str
    name:        str
    logo_url:    Optional[str] = None
    is_verified: bool = False


class OwnerMini(BaseModel):
    id:          str
    full_name:   Optional[str] = None
    avatar_url:  Optional[str] = None
    is_verified: bool = False


class PropertyResponse(BaseModel):
    id:                str
    owner_id:          Optional[str]  = None
    agency_id:         Optional[str]  = None
    listed_by:         str            = "landlord"
    title:             str
    description:       Optional[str]  = None
    price:             float
    price_frequency:   str            = "yearly"
    property_type:     Optional[str]  = None
    listing_type:      Optional[str]  = None
    bedrooms:          Optional[int]  = None
    bathrooms:         Optional[int]  = None
    area_sqft:         Optional[float] = None
    rate_per_sqft:     Optional[float] = None   # computed: price / area_sqft
    address:           Optional[str]  = None
    neighborhood:      Optional[str]  = None
    district:          Optional[str]  = None
    city:              Optional[str]  = None
    latitude:          Optional[float] = None
    longitude:         Optional[float] = None
    # ── Media ──────────────────────────────────────────────────────────────
    images:            Optional[List[str]]  = []
    video_url:         Optional[str]        = None
    youtube_url:       Optional[str]        = None
    instagram_url:     Optional[str]        = None
    # ── Property extras ────────────────────────────────────────────────────
    amenities:         Optional[List[str]]  = []
    furnishing:        str                  = "unfurnished"
    completion_status: Optional[str]        = None
    payment_plan:      Optional[str]        = None
    handover_date:     Optional[str]        = None
    developer_name:    Optional[str]        = None
    permit_number:     Optional[str]        = None
    rera_number:       Optional[str]        = None
    reference_id:      Optional[str]        = None
    brn_dld:           Optional[str]        = None
    zone_name:         Optional[str]        = None
    # ── Rent-specific ──────────────────────────────────────────────────────
    deposit:           Optional[float]      = None
    availability_date: Optional[str]        = None
    # ── Nearby places ──────────────────────────────────────────────────────
    nearby_schools:    Optional[List[str]]  = []
    nearby_hospitals:  Optional[List[str]]  = []
    # ── Documents ──────────────────────────────────────────────────────────
    document_urls:     Optional[List[str]]  = []
    # ── Contractor extras ──────────────────────────────────────────────────
    company_profile:   Optional[str]        = None
    previous_projects: Optional[List[str]]  = []
    rating_avg:        float                = 0.0
    rating_count:      int                  = 0
    # ── Status / admin ─────────────────────────────────────────────────────
    is_verified:       bool                 = False
    is_featured:       bool                 = False
    status:            str                  = "active"
    approval_status:   str                  = "pending"
    rejection_reason:  Optional[str]        = None
    # ── Contact ────────────────────────────────────────────────────────────
    agent_name:        Optional[str]        = None
    agent_phone:       Optional[str]        = None
    agent_photo:       Optional[str]        = None
    whatsapp_number:   Optional[str]        = None
    # ── JSONB metadata ─────────────────────────────────────────────────────
    metadata:          Optional[Dict[str, Any]] = None
    # ── Timestamps ─────────────────────────────────────────────────────────
    created_at:        Optional[str]        = None
    updated_at:        Optional[str]        = None
    # ── Joined ─────────────────────────────────────────────────────────────
    owner:             Optional[OwnerMini]  = None
    agency:            Optional[AgencyMini] = None


class PropertyListResponse(BaseModel):
    data:     List[PropertyResponse]
    total:    int
    page:     int
    limit:    int
    has_next: bool


# ─── Filter schema ────────────────────────────────────────────────────────────

class PropertyFilter(BaseModel):
    listing_type:     Optional[ListingType]       = None
    property_type:    Optional[PropertyType]       = None
    district:         Optional[str]               = None
    city:             Optional[str]               = None
    neighborhood:     Optional[str]               = None
    min_price:        Optional[float]             = None
    max_price:        Optional[float]             = None
    price_frequency:  Optional[PriceFrequency]    = None
    bedrooms:         Optional[int]               = None
    bathrooms:        Optional[int]               = None
    min_area:         Optional[float]             = None
    max_area:         Optional[float]             = None
    furnishing:       Optional[Furnishing]        = None
    completion_status: Optional[CompletionStatus] = None
    amenities:        Optional[List[str]]         = None
    keyword:          Optional[str]               = None
    listed_by:        Optional[ListedBy]          = None
    agency_id:        Optional[str]               = None
    verified_only:    bool                        = False
    has_video:        bool                        = False
    sort_by:          SortBy                      = SortBy.newest
    page:             int                         = 1
    limit:            int                         = 20
    # ── Location radius (km) — uses bounding box approximation ───────────────
    # Requires latitude + longitude of the user/centre point
    center_lat:       Optional[float]             = None
    center_lng:       Optional[float]             = None
    radius_km:        Optional[int]               = None   # 10 | 50 | 100
    # ── Category-specific metadata filters ───────────────────────────────────
    ground_type:      Optional[str]               = None   # cricket | football | ...
    work_category:    Optional[str]               = None   # construction | maintenance
    contractor_type:  Optional[str]               = None   # civil_contractor | architect | ...
    service_type:     Optional[str]               = None   # electrician | plumber | ...
    stay_type:        Optional[str]               = None   # entire_home | villa | ...
