# schemas package
from app.schemas.auth import (
    RegisterRequest,
    LoginRequest,
    GoogleAuthRequest,
    TokenResponse,
    ProfileUpdate,
    RoleUpdate,
    BiometricUpdate,
    UserProfile,
)
from app.schemas.property import (
    PropertyCreate,
    PropertyUpdate,
    PropertyResponse,
    PropertyListResponse,
    PropertyFilter,
    WorkCategory,
    ContractorType,
    MaintenanceServiceType,
    ListingType,
    PropertyType,
)
from app.schemas.service_request import (
    ServiceRequestCreate,
    ServiceRequestResponse,
    QuotationCreate,
    QuotationResponse,
    ServiceCategory,
)
from app.schemas.booking import (
    BookingCreate,
    BookingStatusUpdate,
    BookingResponse,
)
from app.schemas.user import SavedSearchCreate, SavedSearchResponse
from app.schemas.subscription import SubscriptionUpgradeRequest, SubscriptionDetails

__all__ = [
    # Auth
    "RegisterRequest", "LoginRequest", "GoogleAuthRequest",
    "TokenResponse", "ProfileUpdate", "RoleUpdate", "BiometricUpdate", "UserProfile",
    # Property
    "PropertyCreate", "PropertyUpdate", "PropertyResponse",
    "PropertyListResponse", "PropertyFilter",
    "WorkCategory", "ContractorType", "MaintenanceServiceType",
    "ListingType", "PropertyType",
    # Service Requests
    "ServiceRequestCreate", "ServiceRequestResponse",
    "QuotationCreate", "QuotationResponse", "ServiceCategory",
    # Bookings
    "BookingCreate", "BookingStatusUpdate", "BookingResponse",
    # User
    "SavedSearchCreate", "SavedSearchResponse",
    # Subscription
    "SubscriptionUpgradeRequest", "SubscriptionDetails",
]
