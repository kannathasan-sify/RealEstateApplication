package com.realestate.app.navigation

sealed class Screen(val route: String) {
    // Auth flow
    object Splash            : Screen("splash")
    object Onboarding        : Screen("onboarding")
    object Login             : Screen("login")
    object Register          : Screen("register")
    object RoleSelection     : Screen("role_selection")

    // Bottom nav tabs
    object Home              : Screen("home")
    object Saved             : Screen("saved")
    object PostAdCategory    : Screen("post_ad_category?listingType={listingType}&workCategory={workCategory}") {
        fun createRoute(listingType: String? = null, workCategory: String? = null) =
            "post_ad_category?listingType=${listingType.orEmpty()}&workCategory=${workCategory.orEmpty()}"
    }
    object Chat              : Screen("chat")          // ChatListScreen
    object Menu              : Screen("menu")

    // District → Property browsing
    object DistrictList      : Screen("district_list/{listingType}") {
        fun createRoute(listingType: String) = "district_list/$listingType"
    }
    object PropertyList      : Screen("property_list/{district}/{listingType}?workCategory={workCategory}") {
        fun createRoute(
            district: String,
            listingType: String,
            workCategory: String? = null,
        ) = if (workCategory != null)
            "property_list/$district/$listingType?workCategory=$workCategory"
        else
            "property_list/$district/$listingType"
    }
    object PropertyFilter    : Screen("property_filter")
    object Amenities         : Screen("amenities")
    object PropertyDetail    : Screen("property_detail/{id}") {
        fun createRoute(id: String) = "property_detail/$id"
    }

    // Post Ad wizard — all screens share one PostAdViewModel via the nested graph
    object PostAdGraph       : Screen("post_ad_graph?listingType={listingType}&workCategory={workCategory}") {
        fun createRoute(listingType: String? = null, workCategory: String? = null) =
            "post_ad_graph?listingType=${listingType.orEmpty()}&workCategory=${workCategory.orEmpty()}"
    }
    object PostAdTitle       : Screen("post_ad_title")
    object PostAdSubCategory : Screen("post_ad_sub_category")
    object PostAdRole        : Screen("post_ad_role")
    object PostAdDetails     : Screen("post_ad_details")
    object PostAdMapPicker   : Screen("post_ad_map_picker")

    // Admin screens — nested graph so Dashboard + Review share ONE AdminViewModel
    object AdminGraph            : Screen("admin_graph")
    object AdminDashboard        : Screen("admin_dashboard")
    object AdminPropertyReview   : Screen("admin_property_review/{id}") {
        fun createRoute(id: String) = "admin_property_review/$id"
    }

    // Profile & misc
    object Profile              : Screen("profile")
    object Booking              : Screen("booking/{propertyId}") {
        fun createRoute(propertyId: String) = "booking/$propertyId"
    }
    object MyBookings           : Screen("my_bookings")

    // Menu sub-screens
    object MyAds                : Screen("my_ads")
    object MySearches           : Screen("my_searches")
    object AccountSettings      : Screen("account_settings")
    object NotificationSettings : Screen("notification_settings")
    object ApprovedAds          : Screen("approved_ads")    // filtered view of MyAds

    // Service Requests
    object PostServiceRequest   : Screen("post_service_request")
    object ServiceRequestList   : Screen("service_request_list")
    object ServiceRequestDetail : Screen("service_request_detail/{id}") {
        fun createRoute(id: String) = "service_request_detail/$id"
    }

    // Subscription
    object SubscriptionPlans    : Screen("subscription_plans")
}
