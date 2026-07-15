package com.realestate.app.data.api

import com.google.gson.annotations.SerializedName
import com.realestate.app.data.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

// ── Admin approval DTOs ───────────────────────────────────────────────────────

/**
 * Request body for PATCH admin/properties/{id}/approval.
 * [action] = "approve" | "reject" | "re_approve"
 */
data class ApprovalUpdateRequest(
    val action: String,
    @SerializedName("rejection_reason") val rejectionReason: String? = null,
    @SerializedName("proof_note")       val proofNote: String? = null,
)

data class ApprovalUpdateResponse(
    val id: String = "",
    @SerializedName("approval_status") val approvalStatus: String = "",
    val status: String = "",
    @SerializedName("rejection_reason") val rejectionReason: String? = null,
    val message: String = "",
)

interface ApiService {

    // ── Auth ────────────────────────────────────────────────────────────────
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): TokenResponse

    @POST("auth/logout")
    suspend fun logout(): Map<String, String>

    @GET("auth/me")
    suspend fun getMe(): User

    @PUT("auth/me")
    suspend fun updateMe(@Body body: ProfileUpdateRequest): User

    @PUT("auth/me/role")
    suspend fun setRole(@Body body: RoleUpdateRequest): User

    @POST("auth/me/biometric")
    suspend fun toggleBiometric(@Body body: Map<String, Boolean>): User

    // ── Properties ─────────────────────────────────────────────────────────
    @GET("properties")
    suspend fun listProperties(
        @Query("listing_type")    listingType:    String?       = null,
        @Query("property_type")   propertyType:   String?       = null,
        @Query("district")        district:        String?       = null,
        @Query("city")            city:            String?       = null,
        @Query("neighborhood")    neighborhood:    String?       = null,
        @Query("min_price")       minPrice:        Double?       = null,
        @Query("max_price")       maxPrice:        Double?       = null,
        @Query("price_frequency") priceFrequency:  String?       = null,
        @Query("bedrooms")        bedrooms:        Int?          = null,
        @Query("bathrooms")       bathrooms:       Int?          = null,
        @Query("min_area")        minArea:         Double?       = null,
        @Query("max_area")        maxArea:         Double?       = null,
        @Query("furnishing")      furnishing:      String?       = null,
        @Query("amenities")       amenities:       List<String>? = null,
        @Query("keyword")         keyword:         String?       = null,
        @Query("listed_by")       listedBy:        String?       = null,
        @Query("verified_only")   verifiedOnly:    Boolean?      = null,
        @Query("has_video")       hasVideo:        Boolean?      = null,
        @Query("sort_by")         sortBy:          String?       = null,
        // Location radius filter (bounding box)
        @Query("center_lat")      centerLat:       Double?       = null,
        @Query("center_lng")      centerLng:       Double?       = null,
        @Query("radius_km")       radiusKm:        Int?          = null,
        // Category-specific metadata filters
        @Query("work_category")   workCategory:    String?       = null,
        @Query("contractor_type") contractorType:  String?       = null,
        @Query("service_type")    serviceType:     String?       = null,
        @Query("stay_type")       stayType:        String?       = null,
        @Query("ground_type")     groundType:      String?       = null,
        @Query("page")            page:            Int           = 1,
        @Query("limit")           limit:           Int           = 20,
    ): PropertyListResponse

    @GET("properties/featured")
    suspend fun getFeaturedProperties(): List<Property>

    @GET("properties/search")
    suspend fun searchProperties(@Query("q") query: String): List<Property>

    @GET("properties/{id}")
    suspend fun getProperty(@Path("id") id: String): Property

    @GET("properties/{id}/similar")
    suspend fun getSimilarProperties(@Path("id") id: String): List<Property>

    @POST("properties")
    suspend fun createProperty(@Body body: PropertyCreateRequest): Property

    /** Returns the authenticated user's own listings (all approval states). */
    @GET("properties/mine")
    suspend fun getMyProperties(
        @Query("approval_status") approvalStatus: String? = null,
        @Query("page")  page:  Int = 1,
        @Query("limit") limit: Int = 50,
    ): PropertyListResponse


    @Multipart
    @POST("properties/{id}/images")
    suspend fun uploadImages(
        @Path("id") id: String,
        @Part files: List<MultipartBody.Part>,
    ): Property

    // ── Bookings ────────────────────────────────────────────────────────────
    @GET("bookings")
    suspend fun getBookings(): List<Booking>

    @GET("bookings/owner")
    suspend fun getOwnerBookings(): List<Booking>

    @POST("bookings")
    suspend fun createBooking(@Body body: BookingCreateRequest): Booking

    @PUT("bookings/{id}/status")
    suspend fun updateBookingStatus(@Path("id") id: String, @Body body: BookingStatusUpdate): Booking

    @DELETE("bookings/{id}")
    suspend fun cancelBooking(@Path("id") id: String)

    // ── Saved Properties ────────────────────────────────────────────────────
    @GET("saved")
    suspend fun getSavedProperties(): List<Property>

    /** Lightweight check — returns {"saved": true/false} */
    @GET("saved/{propertyId}/check")
    suspend fun checkSavedStatus(@Path("propertyId") propertyId: String): Map<String, Boolean>

    @POST("saved/{propertyId}")
    suspend fun saveProperty(@Path("propertyId") propertyId: String): Map<String, String>

    @DELETE("saved/{propertyId}")
    suspend fun unsaveProperty(@Path("propertyId") propertyId: String)

    // ── Saved Searches ──────────────────────────────────────────────────────
    @GET("searches")
    suspend fun getSavedSearches(): List<SavedSearch>

    @POST("searches")
    suspend fun saveSearch(@Body body: SavedSearchRequest): SavedSearch

    @DELETE("searches/{id}")
    suspend fun deleteSavedSearch(@Path("id") id: String)

    // ── Reviews ─────────────────────────────────────────────────────────────
    @GET("reviews/properties/{id}/reviews")
    suspend fun getPropertyReviews(@Path("id") propertyId: String): List<Review>

    @POST("reviews")
    suspend fun createReview(@Body body: ReviewCreateRequest): Review

    // ── Agencies ─────────────────────────────────────────────────────────────
    @GET("agencies")
    suspend fun getAgencies(@Query("q") query: String? = null): List<Agency>

    @GET("agencies/{id}")
    suspend fun getAgency(@Path("id") id: String): Agency

    // ── Admin ─────────────────────────────────────────────────────────────────
    /** Returns ALL properties (pending / approved / rejected) — admin only. */
    @GET("admin/properties")
    suspend fun getAdminProperties(
        @Query("approval_status") approvalStatus: String? = null,
        @Query("page")  page:  Int = 1,
        @Query("limit") limit: Int = 100,
    ): PropertyListResponse

    /** Approve, reject, or re-approve a listing — admin only. */
    @PATCH("admin/properties/{id}/approval")
    suspend fun updateApprovalStatus(
        @Path("id") id: String,
        @Body body: ApprovalUpdateRequest,
    ): ApprovalUpdateResponse

    // ── Service Requests ─────────────────────────────────────────────────────

    /** List open service requests (construction / maintenance) — contractors browse these. */
    @GET("service-requests")
    suspend fun listServiceRequests(
        @Query("category")     category:     String?  = null,
        @Query("service_type") serviceType:  String?  = null,
        @Query("district")     district:      String?  = null,
        @Query("center_lat")   centerLat:     Double?  = null,
        @Query("center_lng")   centerLng:     Double?  = null,
        @Query("radius_km")    radiusKm:      Int?     = null,
        @Query("status")       status:        String   = "open",
        @Query("urgency")      urgency:       String?  = null,
        @Query("budget_min")   budgetMin:     Double?  = null,
        @Query("budget_max")   budgetMax:     Double?  = null,
        @Query("sort_by")      sortBy:        String   = "newest",
        @Query("page")         page:          Int      = 1,
        @Query("limit")        limit:         Int      = 20,
    ): List<ServiceRequest>

    /** Post a new construction / maintenance request. */
    @POST("service-requests")
    suspend fun createServiceRequest(
        @Body body: ServiceRequestCreateRequest,
    ): ServiceRequest

    @Multipart
    @POST("service-requests/{id}/images")
    suspend fun uploadServiceRequestImages(
        @Path("id") id: String,
        @Part files: List<MultipartBody.Part>,
    ): ServiceRequest


    /** Get detail of a single service request. */
    @GET("service-requests/{id}")
    suspend fun getServiceRequest(@Path("id") id: String): ServiceRequest

    /** Update status of a request (owner only). */
    @PUT("service-requests/{id}/status")
    suspend fun updateServiceRequestStatus(
        @Path("id") id: String,
        @Query("new_status") newStatus: String,
    ): Map<String, String>

    /** List quotations for a request (owner or contractor). */
    @GET("service-requests/{id}/quotations")
    suspend fun listQuotations(@Path("id") id: String): List<Quotation>

    /** Contractor submits a quotation for a request. */
    @POST("service-requests/{id}/quotations")
    suspend fun submitQuotation(
        @Path("id") id: String,
        @Body body: QuotationCreateRequest,
    ): Quotation

    /** Owner accepts or rejects a quotation. */
    @PUT("service-requests/quotations/{quotationId}")
    suspend fun updateQuotationStatus(
        @Path("quotationId") quotationId: String,
        @Query("new_status") newStatus: String,   // "accepted" | "rejected"
    ): Map<String, String>

    // ── Subscriptions ────────────────────────────────────────────────────────
    @GET("subscriptions/me")
    suspend fun getSubscriptionDetails(): SubscriptionDetails

    @POST("subscriptions/upgrade")
    suspend fun upgradeSubscription(@Body body: SubscriptionUpgradeRequest): Map<String, Any>

    // ── Discussions / Q&A ────────────────────────────────────────────────────
    @GET("properties/{propertyId}/discussions")
    suspend fun getPropertyDiscussions(@Path("propertyId") propertyId: String): List<Discussion>

    @POST("properties/{propertyId}/discussions")
    suspend fun postDiscussionMessage(
        @Path("propertyId") propertyId: String,
        @Body body: DiscussionCreateRequest
    ): Discussion


    // ── Property Leads ("I'm Interested") ────────────────────────────────────
    /** Buyer registers interest in a property listing. */
    @POST("properties/{propertyId}/interest")
    suspend fun registerPropertyInterest(
        @Path("propertyId") propertyId: String,
        @Body body: PropertyInterestRequest,
    ): PropertyLead

    /** The current buyer's enquiry history. */
    @GET("properties/leads/mine")
    suspend fun getMyLeads(): List<PropertyLead>

    /** Owner/agent: all leads across the current user's listings. */
    @GET("properties/leads/received")
    suspend fun getReceivedLeads(): List<PropertyLead>

    /** Owner/agent: leads received on one of their listings. */
    @GET("properties/{propertyId}/leads")
    suspend fun getPropertyLeads(@Path("propertyId") propertyId: String): List<PropertyLead>

    /** Owner updates the follow-up status of a lead. */
    @PATCH("properties/leads/{leadId}/status")
    suspend fun updateLeadStatus(
        @Path("leadId") leadId: String,
        @Body body: LeadStatusUpdateRequest,
    ): PropertyLead

    // ── Admin Extensions ─────────────────────────────────────────────────────
    @GET("admin/users")
    suspend fun listUsers(
        @Query("role") role: String? = null,
        @Query("is_verified") isVerified: Boolean? = null
    ): List<User>

    @PATCH("admin/users/{userId}/verify")
    suspend fun verifyUser(
        @Path("userId") userId: String,
        @Body body: UserVerifyRequest
    ): Map<String, Any>

    @PATCH("admin/users/{userId}/role")
    suspend fun changeUserRole(
        @Path("userId") userId: String,
        @Body body: UserRoleRequest
    ): Map<String, Any>

    @DELETE("admin/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Map<String, String>

    @GET("admin/payments")
    suspend fun listPayments(): List<AdminPayment>

    @GET("admin/tickets")
    suspend fun listTickets(@Query("status") status: String? = null): List<SupportTicket>

    @POST("admin/tickets/{ticketId}/reply")
    suspend fun replyTicket(
        @Path("ticketId") ticketId: String,
        @Body body: TicketReplyRequest
    ): Map<String, Any>

    @GET("admin/reports/stats")
    suspend fun getSystemStats(): AdminStats

    /** Admin: every property lead ("I'm Interested") across every listing, incl. buyer_role. */
    @GET("admin/leads")
    suspend fun getAllLeadsAdmin(@Query("status") status: String? = null): List<PropertyLead>

    /** Admin: edit a lead's status, message, or buyer contact info. */
    @PATCH("admin/leads/{leadId}")
    suspend fun updateLeadAdmin(
        @Path("leadId") leadId: String,
        @Body body: AdminLeadUpdateRequest,
    ): PropertyLead

    /** Admin: permanently delete a lead. */
    @DELETE("admin/leads/{leadId}")
    suspend fun deleteLeadAdmin(@Path("leadId") leadId: String): Map<String, String>

    @DELETE("properties/{id}")
    suspend fun deleteProperty(@Path("id") id: String): Map<String, String>

    // ── Support ──────────────────────────────────────────────────────────────
    @POST("support/tickets")
    suspend fun createSupportTicket(@Body body: SupportTicketCreateRequest): SupportTicket

    @GET("support/tickets/me")
    suspend fun getMySupportTickets(): List<SupportTicket>
}

data class SavedSearch(
    val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    val label: String? = null,
    @SerializedName("listing_type") val listingType: String? = null,
    val filters: Map<String, Any> = emptyMap(),
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class SavedSearchRequest(
    val label: String,
    @SerializedName("listing_type") val listingType: String? = null,
    val filters: Map<String, Any> = emptyMap(),
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
)

// ── Subscription DTOs ──────────────────────────────────────────────────────────

data class SubscriptionUpgradeRequest(
    val tier: String
)

data class SubscriptionDetails(
    @SerializedName("subscription_tier") val subscriptionTier: String = "free",
    @SerializedName("subscription_expires_at") val subscriptionExpiresAt: String? = null,
    @SerializedName("max_listings") val maxListings: Int = 3,
    @SerializedName("max_images") val maxImages: Int = 10,
    @SerializedName("video_enabled") val videoEnabled: Boolean = false,
    @SerializedName("featured_enabled") val featuredEnabled: Boolean = false,
    @SerializedName("current_listings_count") val currentListingsCount: Int = 0,
)

// ── Admin Modules DTOs ────────────────────────────────────────────────────────

data class UserVerifyRequest(
    @SerializedName("is_verified") val isVerified: Boolean
)

data class UserRoleRequest(
    val role: String
)

data class UserProfilePreview(
    @SerializedName("full_name") val fullName: String?,
    val email: String?
)

data class AdminPayment(
    val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    val amount: Int = 0,
    val tier: String = "",
    val status: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    val profiles: UserProfilePreview? = null
)

data class SupportTicket(
    val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    val subject: String = "",
    val description: String = "",
    val status: String = "open",
    val reply: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    val profiles: UserProfilePreview? = null
)

data class TicketReplyRequest(
    val reply: String,
    val status: String = "resolved"
)

data class AdminStats(
    @SerializedName("total_properties") val totalProperties: Int = 0,
    @SerializedName("pending_properties") val pendingProperties: Int = 0,
    @SerializedName("total_users") val totalUsers: Int = 0,
    @SerializedName("agents_count") val agentsCount: Int = 0,
    @SerializedName("builders_count") val buildersCount: Int = 0,
    @SerializedName("total_revenue_inr") val totalRevenueInr: Int = 0,
    @SerializedName("total_complaints") val totalComplaints: Int = 0,
    @SerializedName("open_complaints") val openComplaints: Int = 0
)

data class SupportTicketCreateRequest(
    val subject: String,
    val description: String
)
