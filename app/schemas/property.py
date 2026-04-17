"""
schemas/property.py — Property-related Pydantic schemas
"""

from typing import Optional, List, Dict
from datetime import date, datetime
from pydantic import BaseModel, field_validator, model_validator
from enum import Enum


# ─── Enums / constants ────────────────────────────────────────────────────────

class ListingType(str, Enum):
    rent = "rent"
    sale = "sale"
    off_plan = "off_plan"
    holiday_stay = "holiday_stay"   # Holiday Stay listings
    ground = "ground"               # Ground / Sports venue listings
    contractor = "contractor"       # Find a Contractor listings


class PropertyType(str, Enum):
    # ── Residential ───────────────────────────────────────────────────────────
    apartment = "apartment"
    villa = "villa"
    townhouse = "townhouse"
    penthouse = "penthouse"
    hotel_apartment = "hotel_apartment"
    residential_building = "residential_building"
    villa_compound = "villa_compound"
    residential_floor = "residential_floor"
    # ── Commercial ────────────────────────────────────────────────────────────
    office = "office"
    shop = "shop"
    warehouse = "warehouse"
    labour_camp = "labour_camp"
    commercial_building = "commercial_building"
    commercial_floor = "commercial_floor"
    commercial_villa = "commercial_villa"
    land = "land"
    industrial_land = "industrial_land"
    factory = "factory"
    # ── Holiday Stay types ────────────────────────────────────────────────────
    hotel = "hotel"
    resort = "resort"
    room = "room"
    # ── Ground / Sports venue types ───────────────────────────────────────────
    cricket_ground = "cricket_ground"
    football = "football"
    other_open_ground = "other_open_ground"
    badminton = "badminton"
    swimming_pool = "swimming_pool"
    other_closed_ground = "other_closed_ground"
    # ── Contractor work types ─────────────────────────────────────────────────
    building = "building"
    villa_house = "villa_house"
    interior_fitout = "interior_fitout"
    civil_work = "civil_work"
    painting_work = "painting_work"
    air_conditioning = "air_conditioning"
    plumbing = "plumbing"
    household_equipment = "household_equipment"
    # ── Generic ───────────────────────────────────────────────────────────────
    other = "other"


class PriceFrequency(str, Enum):
    yearly = "yearly"
    monthly = "monthly"
    weekly = "weekly"


class Furnishing(str, Enum):
    furnished = "furnished"
    unfurnished = "unfurnished"
    semi_furnished = "semi_furnished"


class CompletionStatus(str, Enum):
    ready = "ready"
    off_plan = "off_plan"


class PropertyStatus(str, Enum):
    active = "active"
    sold = "sold"
    rented = "rented"
    inactive = "inactive"


class ListedBy(str, Enum):
    landlord = "landlord"
    agent = "agent"
    agency = "agency"
    developer = "developer"
    builder = "builder"
    individual = "individual"   # contractor posted by an individual
    company = "company"         # contractor posted by a company
    owner = "owner"             # holiday stay owner


class ApprovalStatus(str, Enum):
    pending = "pending"
    approved = "approved"
    rejected = "rejected"


class SortBy(str, Enum):
    price_asc = "price_asc"
    price_desc = "price_desc"
    newest = "newest"
    oldest = "oldest"


# ─── Amenities master list ────────────────────────────────────────────────────

VALID_AMENITIES = {
    "MAIDS_ROOM", "STUDY", "CENTRAL_AC_HEATING", "BALCONY",
    "PRIVATE_GARDEN", "PRIVATE_POOL", "PRIVATE_GYM", "PRIVATE_JACUZZI",
    "SHARED_POOL", "SHARED_SPA", "SHARED_GYM", "SECURITY",
    "CONCIERGE_SERVICE", "MAID_SERVICE", "COVERED_PARKING", "BUILTIN_WARDROBES",
    "WALKIN_CLOSET", "BUILTIN_KITCHEN_APPLIANCES",
    "VIEW_OF_WATER", "VIEW_OF_LANDMARK", "PETS_ALLOWED", "DOUBLE_GLAZED_WINDOWS",
    "DAY_CARE_CENTER", "ELECTRICITY_BACKUP", "FIRST_AID_MEDICAL_CENTER",
    "SERVICE_ELEVATORS", "PRAYER_ROOM", "LAUNDRY_ROOM",
}


# ─── Create / Update schemas ──────────────────────────────────────────────────

class PropertyCreate(BaseModel):
    title: str
    description: Optional[str] = None
    price: float
    price_frequency: PriceFrequency = PriceFrequency.yearly
    property_type: PropertyType
    listing_type: ListingType
    bedrooms: Optional[int] = None
    bathrooms: Optional[int] = None
    area_sqft: Optional[float] = None
    address: Optional[str] = None
    neighborhood: Optional[str] = None   # area/locality within district
    district: Optional[str] = None        # Tamil Nadu district name
    city: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    images: Optional[List[str]] = []
    video_url: Optional[str] = None
    amenities: Optional[List[str]] = []
    furnishing: Furnishing = Furnishing.unfurnished
    completion_status: Optional[CompletionStatus] = None
    payment_plan: Optional[str] = None
    handover_date: Optional[date] = None
    developer_name: Optional[str] = None
    permit_number: Optional[str] = None
    rera_number: Optional[str] = None
    brn_dld: Optional[str] = None
    zone_name: Optional[str] = None
    listed_by: ListedBy = ListedBy.landlord
    agency_id: Optional[str] = None

    # Agent / builder contact info (stored directly on the property)
    agent_name: Optional[str] = None
    agent_phone: Optional[str] = None
    agent_photo: Optional[str] = None

    # WhatsApp number — required for landlord listings, optional for agents/builders
    whatsapp_number: Optional[str] = None

    # Category-specific extra fields (Ground / Contractor / Holiday Stay)
    # Stored as JSONB metadata column; key examples:
    #   ground      → ground_type, surface, length_m, width_m, capacity, facilities
    #   contractor  → work_category, work_types, experience_yrs, team_size, pricing_model
    #   holiday_stay → stay_type, max_guests, check_in, check_out, min_nights
    metadata: Optional[Dict[str, str]] = None

    # Admin approval flow — default PENDING on creation
    approval_status: ApprovalStatus = ApprovalStatus.pending

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
        if v and len(v) > 6:
            raise ValueError("Maximum 6 images allowed per property")
        return v

    @model_validator(mode="after")
    def validate_landlord_whatsapp(self) -> "PropertyCreate":
        # Landlord property listings require WhatsApp (skip for Ground — no WhatsApp needed)
        # Contractor profiles always require WhatsApp for direct contact
        is_landlord_property = (
            self.listed_by == ListedBy.landlord
            and self.listing_type not in (ListingType.ground,)
        )
        is_contractor = self.listing_type == ListingType.contractor
        if (is_landlord_property or is_contractor) and not self.whatsapp_number:
            who = "contractor" if is_contractor else "landlord"
            raise ValueError(f"WhatsApp number is required for {who} listings")
        return self

    @model_validator(mode="after")
    def validate_category_metadata(self) -> "PropertyCreate":
        """Ensure critical category-specific fields are present in metadata."""
        if self.listing_type == ListingType.ground:
            meta = self.metadata or {}
            if not meta.get("ground_type"):
                raise ValueError("metadata.ground_type is required for Ground listings")
        return self


class PropertyUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    price_frequency: Optional[PriceFrequency] = None
    property_type: Optional[PropertyType] = None
    listing_type: Optional[ListingType] = None
    bedrooms: Optional[int] = None
    bathrooms: Optional[int] = None
    area_sqft: Optional[float] = None
    address: Optional[str] = None
    neighborhood: Optional[str] = None
    city: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    images: Optional[List[str]] = None
    video_url: Optional[str] = None
    amenities: Optional[List[str]] = None
    furnishing: Optional[Furnishing] = None
    completion_status: Optional[CompletionStatus] = None
    payment_plan: Optional[str] = None
    handover_date: Optional[date] = None
    developer_name: Optional[str] = None
    permit_number: Optional[str] = None
    rera_number: Optional[str] = None
    status: Optional[PropertyStatus] = None
    # Allow updating category-specific metadata (e.g. after editing a Ground / Contractor ad)
    metadata: Optional[Dict[str, str]] = None

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
    id: str
    name: str
    logo_url: Optional[str] = None
    is_verified: bool = False


class OwnerMini(BaseModel):
    id: str
    full_name: Optional[str] = None
    avatar_url: Optional[str] = None
    is_verified: bool = False


class PropertyResponse(BaseModel):
    id: str
    owner_id: Optional[str] = None
    agency_id: Optional[str] = None
    listed_by: str = "landlord"
    title: str
    description: Optional[str] = None
    price: float
    price_frequency: str = "yearly"
    property_type: Optional[str] = None
    listing_type: Optional[str] = None
    bedrooms: Optional[int] = None
    bathrooms: Optional[int] = None
    area_sqft: Optional[float] = None
    address: Optional[str] = None
    neighborhood: Optional[str] = None   # area/locality within district
    district: Optional[str] = None        # Tamil Nadu district name
    city: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    images: Optional[List[str]] = []
    video_url: Optional[str] = None
    amenities: Optional[List[str]] = []
    furnishing: str = "unfurnished"
    completion_status: Optional[str] = None
    payment_plan: Optional[str] = None
    handover_date: Optional[str] = None
    developer_name: Optional[str] = None
    permit_number: Optional[str] = None
    rera_number: Optional[str] = None
    reference_id: Optional[str] = None
    brn_dld: Optional[str] = None
    zone_name: Optional[str] = None
    is_verified: bool = False
    is_featured: bool = False
    status: str = "active"
    approval_status: str = "pending"
    rejection_reason: Optional[str] = None
    agent_name: Optional[str] = None
    agent_phone: Optional[str] = None
    agent_photo: Optional[str] = None
    whatsapp_number: Optional[str] = None
    # Category-specific extra fields (null for standard property listings)
    metadata: Optional[Dict[str, str]] = None
    created_at: Optional[str] = None
    updated_at: Optional[str] = None

    # Joined fields
    owner: Optional[OwnerMini] = None
    agency: Optional[AgencyMini] = None


class PropertyListResponse(BaseModel):
    data: List[PropertyResponse]
    total: int
    page: int
    limit: int
    has_next: bool


# ─── Filter schema ────────────────────────────────────────────────────────────

class PropertyFilter(BaseModel):
    listing_type: Optional[ListingType] = None
    property_type: Optional[PropertyType] = None
    district: Optional[str] = None        # Tamil Nadu district filter
    city: Optional[str] = None
    neighborhood: Optional[str] = None   # area/locality within district
    min_price: Optional[float] = None
    max_price: Optional[float] = None
    price_frequency: Optional[PriceFrequency] = None
    bedrooms: Optional[int] = None
    bathrooms: Optional[int] = None
    min_area: Optional[float] = None
    max_area: Optional[float] = None
    furnishing: Optional[Furnishing] = None
    completion_status: Optional[CompletionStatus] = None
    amenities: Optional[List[str]] = None
    keyword: Optional[str] = None
    listed_by: Optional[ListedBy] = None
    agency_id: Optional[str] = None
    verified_only: bool = False
    has_video: bool = False
    sort_by: SortBy = SortBy.newest
    page: int = 1
    limit: int = 20
    # ── Category-specific metadata filters ────────────────────────────────────
    # Filter Ground listings by sport type (e.g. ground_type=cricket)
    ground_type:     Optional[str] = None
    # Filter Contractor listings by work category (construction / maintenance)
    work_category:   Optional[str] = None
    # Filter Holiday Stay listings by type (entire_home / villa / resort / etc.)
    stay_type:       Optional[str] = None
