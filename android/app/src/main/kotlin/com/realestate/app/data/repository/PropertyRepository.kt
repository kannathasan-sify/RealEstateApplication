package com.realestate.app.data.repository

import android.content.Context
import android.net.Uri
import com.realestate.app.data.api.AdminPayment
import com.realestate.app.data.api.AdminStats
import com.realestate.app.data.api.ApiService
import com.realestate.app.data.api.ApprovalUpdateRequest
import com.realestate.app.data.api.ApprovalUpdateResponse
import com.realestate.app.data.api.CreateBuilderRequest
import com.realestate.app.data.api.CreateBuilderResponse
import com.realestate.app.data.api.SubscriptionDetails
import com.realestate.app.data.api.SubscriptionUpgradeRequest
import com.realestate.app.data.api.SupportTicket
import com.realestate.app.data.api.SupportTicketCreateRequest
import com.realestate.app.data.api.TicketReplyRequest
import com.realestate.app.data.api.UserRoleRequest
import com.realestate.app.data.api.UserVerifyRequest
import com.realestate.app.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService
) {

    /**
     * Fetch paginated property list with optional filters.
     * [district] = Tamil Nadu district name (e.g. "Karur", "Chennai")
     * [neighborhood] = area / locality within district (e.g. "Thanthoni", "Anna Nagar")
     */
    suspend fun listProperties(
        listingType: String? = null,
        propertyType: String? = null,
        district: String? = null,
        neighborhood: String? = null,
        city: String? = null,
        keyword: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        bedrooms: Int? = null,
        bathrooms: Int? = null,
        minArea: Double? = null,
        maxArea: Double? = null,
        furnishing: String? = null,
        amenities: List<String>? = null,
        sortBy: String? = null,
        centerLat: Double? = null,
        centerLng: Double? = null,
        radiusKm: Int? = null,
        workCategory: String? = null,
        contractorType: String? = null,
        serviceType: String? = null,
        stayType: String? = null,
        groundType: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): Result<PropertyListResponse> = runCatching {
        api.listProperties(
            listingType = listingType,
            propertyType = propertyType,
            district = district,
            neighborhood = neighborhood,
            city = city,
            keyword = keyword,
            minPrice = minPrice,
            maxPrice = maxPrice,
            bedrooms = bedrooms,
            bathrooms = bathrooms,
            minArea = minArea,
            maxArea = maxArea,
            furnishing = furnishing,
            amenities = amenities,
            sortBy = sortBy,
            centerLat = centerLat,
            centerLng = centerLng,
            radiusKm = radiusKm,
            workCategory = workCategory,
            contractorType = contractorType,
            serviceType = serviceType,
            stayType = stayType,
            groundType = groundType,
            page = page,
            limit = limit,
        )
    }

    suspend fun getFeatured(): Result<List<Property>> = runCatching { api.getFeaturedProperties() }

    suspend fun search(query: String, district: String? = null): Result<List<Property>> =
        runCatching { api.searchProperties(query) }

    suspend fun getProperty(id: String): Result<Property> = runCatching { api.getProperty(id) }

    suspend fun getSimilar(id: String): Result<List<Property>> = runCatching { api.getSimilarProperties(id) }

    suspend fun createProperty(request: PropertyCreateRequest): Result<Property> =
        runCatching { api.createProperty(request) }

    /**
     * Upload [uris] (up to 10) as images for [propertyId].
     */
    suspend fun uploadPropertyImages(propertyId: String, uris: List<Uri>): Result<Property> =
        runCatching {
            val parts = uris.mapIndexed { index, uri ->
                val cr = context.contentResolver
                val mimeType = cr.getType(uri) ?: "image/jpeg"
                val bytes = cr.openInputStream(uri)?.readBytes()
                    ?: error("Could not read image at index $index")
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val ext = mimeType.substringAfterLast("/", "jpg")
                MultipartBody.Part.createFormData("files", "image_$index.$ext", requestBody)
            }
            api.uploadImages(propertyId, parts)
        }

    /** Get the signed-in user's own listings (all approval states). */
    suspend fun getMyProperties(approvalStatus: String? = null): Result<List<Property>> =
        runCatching { api.getMyProperties(approvalStatus).data }

    suspend fun deleteProperty(id: String): Result<Unit> = runCatching { api.deleteProperty(id); Unit }

    suspend fun getSaved(): Result<List<Property>> = runCatching { api.getSavedProperties() }

    /** Returns true if this property is in the current user's saved list. */
    suspend fun checkSaved(id: String): Result<Boolean> = runCatching {
        api.checkSavedStatus(id)["saved"] ?: false
    }

    suspend fun saveProperty(id: String): Result<Unit> = runCatching { api.saveProperty(id); Unit }

    suspend fun unsaveProperty(id: String): Result<Unit> = runCatching { api.unsaveProperty(id) }

    // ── Service Requests ─────────────────────────────────────────────────────

    suspend fun listServiceRequests(
        category: String? = null,
        serviceType: String? = null,
        district: String? = null,
        centerLat: Double? = null,
        centerLng: Double? = null,
        radiusKm: Int? = null,
        status: String = "open",
        urgency: String? = null,
        budgetMin: Double? = null,
        budgetMax: Double? = null,
        sortBy: String = "newest",
        page: Int = 1,
        limit: Int = 20,
    ): Result<List<ServiceRequest>> = runCatching {
        api.listServiceRequests(
            category, serviceType, district, centerLat, centerLng, radiusKm, status,
            urgency, budgetMin, budgetMax, sortBy, page, limit
        )
    }

    suspend fun createServiceRequest(request: ServiceRequestCreateRequest): Result<ServiceRequest> = runCatching {
        api.createServiceRequest(request)
    }

    suspend fun uploadServiceRequestImages(requestId: String, uris: List<Uri>): Result<ServiceRequest> =
        runCatching {
            val parts = uris.mapIndexed { index, uri ->
                val cr = context.contentResolver
                val mimeType = cr.getType(uri) ?: "image/jpeg"
                val bytes = cr.openInputStream(uri)?.readBytes()
                    ?: error("Could not read image at index $index")
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val ext = mimeType.substringAfterLast("/", "jpg")
                MultipartBody.Part.createFormData("files", "image_$index.$ext", requestBody)
            }
            api.uploadServiceRequestImages(requestId, parts)
        }


    suspend fun getServiceRequest(id: String): Result<ServiceRequest> = runCatching {
        api.getServiceRequest(id)
    }

    suspend fun updateServiceRequestStatus(id: String, newStatus: String): Result<Unit> = runCatching {
        api.updateServiceRequestStatus(id, newStatus)
        Unit
    }

    suspend fun listQuotations(id: String): Result<List<Quotation>> = runCatching {
        api.listQuotations(id)
    }

    suspend fun submitQuotation(id: String, request: QuotationCreateRequest): Result<Quotation> = runCatching {
        api.submitQuotation(id, request)
    }

    suspend fun updateQuotationStatus(quotationId: String, newStatus: String): Result<Unit> = runCatching {
        api.updateQuotationStatus(quotationId, newStatus)
        Unit
    }

    // ── Property Leads ("I'm Interested") ────────────────────────────────────

    suspend fun registerInterest(
        propertyId: String,
        message: String? = null,
        channel: String = "app",
    ): Result<PropertyLead> = runCatching {
        api.registerPropertyInterest(propertyId, PropertyInterestRequest(message, channel))
    }

    suspend fun getMyLeads(): Result<List<PropertyLead>> = runCatching { api.getMyLeads() }

    suspend fun getReceivedLeads(): Result<List<PropertyLead>> = runCatching { api.getReceivedLeads() }

    suspend fun getPropertyLeads(propertyId: String): Result<List<PropertyLead>> =
        runCatching { api.getPropertyLeads(propertyId) }

    suspend fun updateLeadStatus(leadId: String, status: String): Result<PropertyLead> =
        runCatching { api.updateLeadStatus(leadId, LeadStatusUpdateRequest(status)) }

    // ── Dashboards ──────────────────────────────────────────────────────────────

    /** Real owner analytics (properties/views/leads/saves), mapped to the UI model. */
    suspend fun getOwnerDashboard(): Result<OwnerDashboardData> =
        runCatching { api.getOwnerDashboard().toDomain() }

    /** Real admin analytics (users/revenue/approvals/fraud). Admin-only on the backend. */
    suspend fun getAdminDashboard(): Result<AdminDashboardData> =
        runCatching { api.getAdminDashboard().toDomain() }

    /** Real agent analytics (listings/pipeline/commission). Agent-only on the backend. */
    suspend fun getAgentDashboard(): Result<AgentDashboardData> =
        runCatching { api.getAgentDashboard().toDomain() }

    /** Real channel-partner analytics (referrals/payouts). Partner-only on the backend. */
    suspend fun getPartnerDashboard(): Result<PartnerDashboardData> =
        runCatching { api.getPartnerDashboard().toDomain() }

    /** Best-effort: record a property-detail view for analytics. Never surface failure. */
    suspend fun recordPropertyView(propertyId: String): Result<Unit> =
        runCatching { api.recordPropertyView(propertyId); Unit }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Fetch ALL listings for admin (pending / approved / rejected). */
    suspend fun getAdminProperties(
        approvalStatus: String? = null,
        page: Int = 1,
        limit: Int = 100,
    ): Result<PropertyListResponse> = runCatching {
        api.getAdminProperties(approvalStatus, page, limit)
    }

    /** Approve a pending listing. */
    suspend fun approveProperty(id: String): Result<ApprovalUpdateResponse> = runCatching {
        api.updateApprovalStatus(id, ApprovalUpdateRequest(action = "approve"))
    }

    /** Reject a listing with a mandatory reason. */
    suspend fun rejectProperty(id: String, reason: String): Result<ApprovalUpdateResponse> = runCatching {
        api.updateApprovalStatus(id, ApprovalUpdateRequest(action = "reject", rejectionReason = reason))
    }

    /** Re-approve a previously rejected listing (requires manager proof note). */
    suspend fun reApproveProperty(id: String, proofNote: String): Result<ApprovalUpdateResponse> = runCatching {
        api.updateApprovalStatus(id, ApprovalUpdateRequest(action = "re_approve", proofNote = proofNote))
    }

    // ── Subscriptions ────────────────────────────────────────────────────────
    suspend fun getSubscriptionDetails(): Result<SubscriptionDetails> = runCatching {
        api.getSubscriptionDetails()
    }

    suspend fun upgradeSubscription(tier: String): Result<Map<String, Any>> = runCatching {
        api.upgradeSubscription(SubscriptionUpgradeRequest(tier))
    }

    // ── Discussions ──────────────────────────────────────────────────────────
    suspend fun getPropertyDiscussions(propertyId: String): Result<List<Discussion>> = runCatching {
        api.getPropertyDiscussions(propertyId)
    }

    suspend fun postDiscussionMessage(propertyId: String, request: DiscussionCreateRequest): Result<Discussion> = runCatching {
        api.postDiscussionMessage(propertyId, request)
    }


    // ── Admin Extensions ─────────────────────────────────────────────────────
    suspend fun listUsers(role: String? = null, isVerified: Boolean? = null): Result<List<User>> = runCatching {
        api.listUsers(role, isVerified)
    }

    suspend fun verifyUser(userId: String, isVerified: Boolean): Result<Unit> = runCatching {
        api.verifyUser(userId, UserVerifyRequest(isVerified))
        Unit
    }

    suspend fun changeUserRole(userId: String, role: String): Result<Unit> = runCatching {
        api.changeUserRole(userId, UserRoleRequest(role))
        Unit
    }

    suspend fun deleteUser(userId: String): Result<Unit> = runCatching {
        api.deleteUser(userId)
        Unit
    }

    suspend fun createBuilder(email: String, password: String, fullName: String, phone: String? = null): Result<CreateBuilderResponse> = runCatching {
        api.createBuilder(CreateBuilderRequest(email, password, fullName, phone))
    }

    suspend fun listPayments(): Result<List<AdminPayment>> = runCatching {
        api.listPayments()
    }

    suspend fun listTickets(status: String? = null): Result<List<SupportTicket>> = runCatching {
        api.listTickets(status)
    }

    suspend fun replyTicket(ticketId: String, reply: String, status: String = "resolved"): Result<Unit> = runCatching {
        api.replyTicket(ticketId, TicketReplyRequest(reply, status))
        Unit
    }

    suspend fun getSystemStats(): Result<AdminStats> = runCatching {
        api.getSystemStats()
    }

    /** Admin: every property lead across every listing (Enquiries tab), incl. buyer_role. */
    suspend fun getAllLeadsAdmin(status: String? = null): Result<List<PropertyLead>> = runCatching {
        api.getAllLeadsAdmin(status)
    }

    /** Admin: edit a lead's status / message / buyer contact info. */
    suspend fun updateLeadAdmin(
        leadId: String,
        status: String? = null,
        message: String? = null,
        buyerName: String? = null,
        buyerPhone: String? = null,
        buyerEmail: String? = null,
    ): Result<PropertyLead> = runCatching {
        api.updateLeadAdmin(leadId, AdminLeadUpdateRequest(status, message, buyerName, buyerPhone, buyerEmail))
    }

    /** Admin: permanently delete a lead. */
    suspend fun deleteLeadAdmin(leadId: String): Result<Unit> = runCatching {
        api.deleteLeadAdmin(leadId)
        Unit
    }


    // ── Support ──────────────────────────────────────────────────────────────
    suspend fun createSupportTicket(subject: String, description: String): Result<SupportTicket> = runCatching {
        api.createSupportTicket(SupportTicketCreateRequest(subject, description))
    }

    suspend fun getMySupportTickets(): Result<List<SupportTicket>> = runCatching {
        api.getMySupportTickets()
    }
}
