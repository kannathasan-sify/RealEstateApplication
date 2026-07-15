package com.realestate.app.data.mock

import com.realestate.app.data.models.ApprovalStatus
import com.realestate.app.data.models.Booking
import com.realestate.app.data.models.Property
import com.realestate.app.data.models.PropertyCreateRequest
import com.realestate.app.data.models.PropertyLead
import com.realestate.app.data.models.User
import com.realestate.app.data.models.ServiceRequest
import com.realestate.app.data.models.Quotation
import com.realestate.app.data.api.SubscriptionDetails
import com.realestate.app.data.api.UserProfilePreview
import com.realestate.app.data.api.AdminPayment
import com.realestate.app.data.api.SupportTicket
import com.realestate.app.data.api.AdminStats
import com.realestate.app.data.models.Discussion
import com.realestate.app.data.models.Reply
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object MockData {

    /**
     * Represents the currently logged-in user in debug/mock mode.
     * In production this is sourced from DataStoreManager / AuthViewModel.
     */
    val currentUser = User(
        id           = "mock-user-001",
        fullName     = "kannathasan sify",
        email        = "kannathasan.sify@gmail.com",
        phone        = "8056584080",
        userIdCode   = "NX-TN-20261234",
        roleStr      = "buyer",
        isVerified   = false,
        city         = "Chennai",
        language     = "English",
        createdAt    = "2026-03-01T00:00:00",
    )

    var mockSubscriptionDetails = SubscriptionDetails(
        subscriptionTier = "free",
        maxListings = 3,
        maxImages = 10,
        currentListingsCount = 0
    )

    /**
     * Property "I'm Interested" leads. Seeded with one enquiry the mock user sent (shows in
     * the buyer "My Enquiries" tab) and one received on the mock user's own listing (shows
     * in the owner "Received Leads" tab). Property fields are denormalized snapshots, so
     * they don't need to resolve to a live [Property].
     */
    val propertyLeads = mutableListOf<PropertyLead>(
        PropertyLead(
            id            = "lead-mock-001",
            propertyId    = "prop-002",
            propertyRef   = "TN-S-10231",
            propertyTitle = "2BHK Apartment for Sale in Anna Nagar",
            ownerId       = "owner-xyz",
            buyerId       = currentUser.id,
            buyerName     = currentUser.fullName,
            buyerPhone    = currentUser.phone,
            buyerEmail    = currentUser.email,
            buyerRole     = currentUser.roleStr,
            channel       = "app",
            message       = "Interested — please share more details.",
            status        = "contacted",
            createdAt     = "2026-07-12T09:30:00Z",
        ),
        PropertyLead(
            id            = "lead-mock-002",
            propertyId    = "prop-101",
            propertyRef   = "TN-R-20455",
            propertyTitle = "3BHK Villa for Rent in Coimbatore",
            ownerId       = currentUser.id,
            buyerId       = "buyer-abc",
            buyerName     = "Ramesh Kumar",
            buyerPhone    = "9876543210",
            buyerEmail    = "ramesh.k@example.com",
            buyerRole     = "buyer",
            channel       = "app",
            message       = "Is this still available? Looking to move in next month.",
            status        = "pending",
            createdAt     = "2026-07-13T18:05:00Z",
        ),
    )

    val serviceRequests = mutableListOf<ServiceRequest>(
        ServiceRequest(
            id = "sr-mock-001",
            userId = "user-abc",
            category = "construction",
            serviceType = "Civil Contractors",
            title = "Need builder for 2BHK villa",
            description = "Looking for an experienced builder to construct a 2BHK residential villa in Coimbatore. Land area is 1200 sqft. Need plan approval and full building construction.",
            district = "Coimbatore",
            radiusKm = 50,
            budgetMin = 1500000.0,
            budgetMax = 2000000.0,
            status = "open",
            createdAt = "2026-03-25T10:00:00Z",
            urgency = "normal",
            preferredDate = "2026-08-15",
            contactPhone = "+91 98765 43210"
        ),
        ServiceRequest(
            id = "sr-mock-002",
            userId = "user-xyz",
            category = "maintenance",
            serviceType = "Electrician",
            title = "Complete apartment rewiring",
            description = "Need a certified electrician for complete rewiring of a 3BHK flat in Anna Nagar. Old wiring needs to be replaced. Switches and DB boxes also need replacement.",
            district = "Chennai",
            radiusKm = 10,
            budgetMin = 40000.0,
            budgetMax = 50000.0,
            status = "open",
            createdAt = "2026-03-26T12:00:00Z",
            urgency = "urgent",
            preferredDate = "2026-07-20",
            contactPhone = "+91 90031 22334"
        ),
        ServiceRequest(
            id = "sr-mock-003",
            userId = "user-def",
            category = "maintenance",
            serviceType = "Plumber",
            title = "Burst pipe flooding kitchen — need plumber today",
            description = "A supply pipe under the kitchen sink burst this morning. Water is being shut off at the main but we need it fixed as soon as possible.",
            district = "Madurai",
            radiusKm = 10,
            budgetMin = 1500.0,
            budgetMax = 5000.0,
            status = "open",
            createdAt = "2026-07-12T08:30:00Z",
            urgency = "emergency",
            preferredDate = null,
            contactPhone = "+91 93445 67890"
        )
    )

    val quotations = mutableListOf<Quotation>(
        Quotation(
            id = "q-mock-001",
            requestId = "sr-mock-001",
            contractorId = "contractor-123",
            amount = 1750000.0,
            timeline = "3-4 months",
            notes = "We can complete the construction using A-grade materials. Full plan drawing and structural analysis included.",
            status = "pending",
            createdAt = "2026-03-26T14:00:00Z"
        )
    )

    val adminUsers = mutableListOf<User>(
        currentUser,
        User(
            id = "mock-user-002",
            fullName = "Rajesh Kumar",
            email = "rajesh.agent@gmail.com",
            phone = "+91 98401 23456",
            userIdCode = "NX-TN-20260002",
            roleStr = "agent",
            isVerified = true,
            city = "Chennai",
            createdAt = "2026-03-02T00:00:00"
        ),
        User(
            id = "mock-user-003",
            fullName = "Priya Builders",
            email = "priya.builder@gmail.com",
            phone = "+91 99420 56789",
            userIdCode = "NX-TN-20260003",
            roleStr = "builder",
            isVerified = false,
            city = "Coimbatore",
            createdAt = "2026-03-03T00:00:00"
        )
    )

    val adminPayments = mutableListOf<AdminPayment>(
        AdminPayment(
            id = "pay-mock-001",
            userId = "mock-user-002",
            amount = 299,
            tier = "silver",
            status = "success",
            createdAt = "2026-03-24T10:00:00Z",
            profiles = UserProfilePreview("Rajesh Kumar", "rajesh.agent@gmail.com")
        ),
        AdminPayment(
            id = "pay-mock-002",
            userId = "mock-user-003",
            amount = 999,
            tier = "contractor",
            status = "success",
            createdAt = "2026-03-25T11:00:00Z",
            profiles = UserProfilePreview("Priya Builders", "priya.builder@gmail.com")
        )
    )

    val adminTickets = mutableListOf<SupportTicket>(
        SupportTicket(
            id = "ticket-mock-001",
            userId = "mock-user-002",
            subject = "Image Upload Failing",
            description = "I am trying to upload 5 images of my Anna Nagar property but it fails at index 3.",
            status = "open",
            reply = null,
            createdAt = "2026-03-25T09:00:00Z",
            profiles = UserProfilePreview("Rajesh Kumar", "rajesh.agent@gmail.com")
        ),
        SupportTicket(
            id = "ticket-mock-002",
            userId = "mock-user-003",
            subject = "Request for Verification",
            description = "I have uploaded all licensing papers. Please verify my Builder profile.",
            status = "resolved",
            reply = "Verified. Thank you for submitting documents.",
            createdAt = "2026-03-24T08:00:00Z",
            profiles = UserProfilePreview("Priya Builders", "priya.builder@gmail.com")
        )
    )

    val propertyDiscussions = mutableListOf<Discussion>(
        Discussion(
            id = "d-mock-001",
            propertyId = "mock-001",
            userId = "mock-user-002",
            userName = "Rajesh Kumar",
            message = "Is the price negotiable? I am very interested.",
            createdAt = "2026-03-30T10:00:00Z",
            replies = listOf(
                Reply(
                    id = "r-mock-001",
                    userId = "mock-user-001",
                    userName = "kannathasan sify",
                    message = "Yes, we can discuss a slight discount during a visit.",
                    createdAt = "2026-03-30T10:15:00Z"
                )
            )
        )
    )

    val adminStats: AdminStats
        get() = AdminStats(
            totalProperties = properties.size,
            pendingProperties = properties.count { it.approvalStatus == ApprovalStatus.PENDING },
            totalUsers = adminUsers.size,
            agentsCount = adminUsers.count { it.roleStr == "agent" },
            buildersCount = adminUsers.count { it.roleStr == "builder" },
            totalRevenueInr = adminPayments.sumOf { it.amount },
            totalComplaints = adminTickets.size,
            openComplaints = adminTickets.count { it.status == "open" }
        )

    // ── Single mutable list (seed + user-submitted, mutations work uniformly) ──

    private val _allProperties: MutableList<Property> = mutableListOf(
        Property(
            id = "mock-001",
            title = "Spacious 3 BHK Apartment near Anna Nagar",
            description = "Well-ventilated 3 BHK apartment on the 4th floor with beautiful city view. " +
                "Premium flooring, modular kitchen, and 2 covered car parking. " +
                "5 mins from Anna Nagar East metro station. " +
                "Society amenities include gym, swimming pool and 24/7 security.",
            price = 18000L,
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
            amenities = listOf("COVERED_PARKING", "SHARED_GYM", "SECURITY", "BUILTIN_WARDROBES"),
            furnishing = "semi",
            listedBy = "agent",
            agentName = "Rajesh Kumar",
            agentPhone = "+91 98401 23456",
            agentPhoto = "https://randomuser.me/api/portraits/men/32.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00001",
            isVerified = true,
            status = "active",
            createdAt = "2026-03-20T10:00:00Z"
        ),
        Property(
            id = "mock-002",
            title = "2 BHK Independent House for Sale — Coimbatore",
            description = "Independent 2 BHK house in a prime residential area of Coimbatore. " +
                "Ground floor with spacious garden. Newly constructed, ready to occupy. " +
                "Close to KMCH hospital, reputed schools, and shopping centres. DTCP approved.",
            price = 6500000L,
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
            amenities = listOf("PRIVATE_GARDEN", "COVERED_PARKING", "SECURITY"),
            furnishing = "unfurnished",
            listedBy = "builder",
            agentName = "Priya Builders",
            agentPhone = "+91 99420 56789",
            agentPhoto = "https://randomuser.me/api/portraits/women/44.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00002",
            isVerified = true,
            status = "active",
            createdAt = "2026-03-22T09:00:00Z"
        ),
        Property(
            id = "mock-003",
            title = "Commercial Shop for Rent — T Nagar Main Road",
            description = "Prime commercial shop space on the ground floor at T Nagar main road. " +
                "High footfall area, suitable for retail, restaurant, or showroom. " +
                "Dedicated parking for 3 vehicles. 24-hour CCTV surveillance.",
            price = 75000L,
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
            amenities = listOf("COVERED_PARKING", "SECURITY"),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Suresh Nair",
            agentPhone = "+91 94440 78901",
            agentPhoto = "https://randomuser.me/api/portraits/men/55.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00003",
            status = "active",
            createdAt = "2026-03-25T11:00:00Z"
        ),
        Property(
            id = "mock-004",
            title = "4 BHK Luxury Villa — ECR with Sea View",
            description = "Stunning luxury villa on East Coast Road with sea-view. " +
                "Private swimming pool, landscaped garden, home theatre, and modular kitchen. " +
                "Gated community with 24/7 security. 15 mins from Thiruvanmiyur beach.",
            price = 95000L,
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
            amenities = listOf("PRIVATE_POOL", "PRIVATE_GYM", "PRIVATE_GARDEN", "COVERED_PARKING", "SECURITY", "BUILTIN_KITCHEN_APPLIANCES"),
            furnishing = "furnished",
            listedBy = "agent",
            agentName = "Kavitha Devi",
            agentPhone = "+91 98841 34567",
            agentPhoto = "https://randomuser.me/api/portraits/women/22.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00004",
            isVerified = true,
            status = "active",
            createdAt = "2026-03-28T14:00:00Z"
        ),
        Property(
            id = "mock-005",
            title = "1 BHK Studio Flat — Madurai City Centre",
            description = "Compact and well-designed 1 BHK flat near Madurai Meenakshi Amman Temple. " +
                "Ideal for bachelors and young couples. Fully furnished with TV, fridge, and washing machine. " +
                "Walking distance to bus stand and railway station.",
            price = 8500L,
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
            amenities = listOf("SECURITY", "BUILTIN_WARDROBES"),
            furnishing = "furnished",
            listedBy = "agent",
            agentName = "Murugan Selva",
            agentPhone = "+91 99440 23456",
            agentPhoto = "https://randomuser.me/api/portraits/men/71.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00005",
            status = "active",
            createdAt = "2026-03-30T08:00:00Z"
        ),
        Property(
            id = "mock-006",
            title = "30 Cents DTCP Land for Sale — Thanjavur Highway",
            description = "Prime agricultural and residential-convertible land along Thanjavur-Kumbakonam highway. " +
                "DTCP-approved plot, clear title, water and electricity available on-site. " +
                "Suitable for residential plots, farm house, or commercial development.",
            price = 12000000L,
            priceFrequency = null,
            propertyType = "land",
            listingType = "sale",
            bedrooms = 0,
            bathrooms = 0,
            areaSqft = 13068.0,
            district = "Thanjavur",
            city = "Thanjavur",
            neighborhood = "NH67 Bypass",
            address = "Survey No. 45/2, Thanjavur-Kumbakonam Highway, Thanjavur - 613001",
            latitude = 10.7869,
            longitude = 79.1378,
            images = listOf("https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800"),
            amenities = listOf(),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Balamurugan TN",
            agentPhone = "+91 98410 65432",
            agentPhoto = "https://randomuser.me/api/portraits/men/88.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00006",
            status = "active",
            createdAt = "2026-03-31T16:00:00Z"
        ),
        Property(
            id = "mock-007",
            title = "2 BHK New Apartment — Erode Perundurai Road",
            description = "Brand new 2 BHK apartment with all modern amenities near Erode bus stand. " +
                "Ready to occupy. UDS included. RERA registered project.",
            price = 3200000L,
            priceFrequency = null,
            propertyType = "apartment",
            listingType = "sale",
            bedrooms = 2,
            bathrooms = 2,
            areaSqft = 980.0,
            district = "Erode",
            city = "Erode",
            neighborhood = "Perundurai Road",
            address = "Green Valley Apartments, Perundurai Road, Erode - 638011",
            latitude = 11.3427,
            longitude = 77.7272,
            images = listOf(
                "https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800"
            ),
            amenities = listOf("COVERED_PARKING", "SECURITY"),
            furnishing = "unfurnished",
            listedBy = "builder",
            agentName = "Erode Constructions Pvt Ltd",
            agentPhone = "+91 96550 34567",
            agentPhoto = "https://randomuser.me/api/portraits/men/45.jpg",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-TN-00007",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-01T09:00:00Z"
        ),
        // ── Pending (waiting for admin approval) ────────────────────────────
        Property(
            id = "mock-008",
            title = "3 BHK New Flat for Rent — Tiruppur Avinashi Road",
            description = "Brand new 3 BHK in a newly constructed apartment complex. Waiting for admin approval.",
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
            images = listOf("https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800"),
            amenities = listOf("COVERED_PARKING", "SHARED_GYM", "SECURITY"),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Vijay Agent",
            agentPhone = "+91 97890 12345",
            agentPhoto = "https://randomuser.me/api/portraits/men/15.jpg",
            approvalStatus = ApprovalStatus.PENDING,
            referenceId = "NX-TN-00008",
            status = "inactive",
            createdAt = "2026-04-01T07:00:00Z"
        ),
        // ── Rejected (demo — can be re-approved by manager with proof) ───────
        Property(
            id = "mock-009",
            title = "2 BHK Apartment for Rent — Salem Town",
            description = "2 BHK apartment near Salem bus stand. Submitted with incomplete documents.",
            price = 9500L,
            priceFrequency = "month",
            propertyType = "apartment",
            listingType = "rent",
            bedrooms = 2,
            bathrooms = 1,
            areaSqft = 900.0,
            district = "Salem",
            city = "Salem",
            neighborhood = "Town",
            address = "12, Omalur Road, Salem - 636004",
            latitude = 11.6643,
            longitude = 78.1460,
            images = listOf("https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800"),
            amenities = listOf("SECURITY"),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Karthik Salem",
            agentPhone = "+91 97650 11223",
            agentPhoto = "https://randomuser.me/api/portraits/men/62.jpg",
            approvalStatus = ApprovalStatus.REJECTED,
            rejectionReason = "Incomplete ownership documents. Please resubmit with clear title deed.",
            referenceId = "NX-TN-00009",
            status = "inactive",
            createdAt = "2026-04-02T10:00:00Z"
        ),

        // ── Holiday Stay listings ────────────────────────────────────────────
        Property(
            id = "mock-010",
            title = "Sea-View Resort Stay — Marina Beach, Chennai",
            description = "Luxury resort room with a stunning sea view at Marina Beach. " +
                "Enjoy complimentary breakfast, rooftop pool, spa, and concierge service. " +
                "Perfect for weekend getaways, honeymoons, or family holidays. " +
                "5-star hospitality at affordable prices.",
            price = 4500L,
            priceFrequency = "month",   // used as "per night" context for stays
            propertyType = "resort",
            listingType = "holiday_stay",
            bedrooms = 1,
            bathrooms = 1,
            areaSqft = 380.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "Marina Beach",
            address = "Golden Sands Resort, Marina Beach Road, Chennai - 600001",
            latitude = 13.0499,
            longitude = 80.2824,
            images = listOf(
                "https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800",
                "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800",
                "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800"
            ),
            amenities = listOf(
                "SHARED_POOL", "SHARED_SPA", "SHARED_GYM", "CONCIERGE_SERVICE",
                "MAID_SERVICE", "SECURITY", "BUILTIN_KITCHEN_APPLIANCES"
            ),
            furnishing = "furnished",
            listedBy = "agent",
            agentName = "Golden Sands Resort",
            agentPhone = "+91 44 2810 1234",
            agentPhoto = "https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=200",
            whatsappNumber = "+91 98400 11111",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-HS-00010",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-03T10:00:00Z",
            metadata = mapOf(
                "stay_type"    to "resort_room",
                "max_guests"   to "2",
                "check_in"     to "14:00",
                "check_out"    to "11:00",
                "min_nights"   to "1",
                "facilities"   to "Rooftop Pool,Spa,Concierge,Breakfast,WiFi,Room Service",
                "house_rules"  to "No Smoking,No Pets,Quiet Hours After 10pm",
                "cancellation" to "moderate"
            )
        ),
        Property(
            id = "mock-011",
            title = "Heritage Villa Stay — Chettinad, Karaikudi",
            description = "Immerse yourself in Tamil Nadu's rich Chettinad heritage. " +
                "Antique wooden furniture, traditional Athangudi tiles, and authentic home-cooked Chettinad cuisine. " +
                "Ideal for cultural travellers seeking an authentic Tamilnadu experience. " +
                "6 rooms available, private courtyard, filtered water.",
            price = 3200L,
            priceFrequency = "month",
            propertyType = "villa",
            listingType = "holiday_stay",
            bedrooms = 3,
            bathrooms = 2,
            areaSqft = 2800.0,
            district = "Sivaganga",
            city = "Karaikudi",
            neighborhood = "Chettinad",
            address = "Heritage Bungalow, 12 Athangudi Street, Karaikudi - 630001",
            latitude = 10.0741,
            longitude = 78.7794,
            images = listOf(
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
                "https://images.unsplash.com/photo-1505691723518-36a5ac3be353?w=800"
            ),
            amenities = listOf("PRIVATE_GARDEN", "MAID_SERVICE", "SECURITY", "PETS_ALLOWED"),
            furnishing = "furnished",
            listedBy = "landlord",
            agentName = "Ramanathan Heritage Homes",
            agentPhone = "+91 99440 55678",
            agentPhoto = "https://randomuser.me/api/portraits/men/30.jpg",
            whatsappNumber = "+91 99440 55678",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-HS-00011",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-04T08:00:00Z",
            metadata = mapOf(
                "stay_type"    to "entire_home",
                "max_guests"   to "6",
                "check_in"     to "13:00",
                "check_out"    to "10:00",
                "min_nights"   to "2",
                "facilities"   to "Private Courtyard,Home-Cooked Meals,Traditional Kitchen,Guided Heritage Tour",
                "house_rules"  to "No Smoking,No Loud Music,Respect Heritage Property",
                "cancellation" to "flexible"
            )
        ),
        Property(
            id = "mock-012",
            title = "Budget Hotel Room — Near Madurai Airport",
            description = "Clean, comfortable hotel room 2 km from Madurai Airport. " +
                "Free WiFi, AC, hot water, and complimentary breakfast. " +
                "Ideal for transit stays, business travellers, and pilgrims visiting Meenakshi Temple. " +
                "24-hour check-in available.",
            price = 1200L,
            priceFrequency = "month",
            propertyType = "hotel",
            listingType = "holiday_stay",
            bedrooms = 1,
            bathrooms = 1,
            areaSqft = 220.0,
            district = "Madurai",
            city = "Madurai",
            neighborhood = "Avaniyapuram",
            address = "Airport View Hotel, Avaniyapuram Road, Madurai - 625012",
            latitude = 9.8320,
            longitude = 78.0937,
            images = listOf(
                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800",
                "https://images.unsplash.com/photo-1598928636135-d146006ff4be?w=800"
            ),
            amenities = listOf("SECURITY", "CENTRAL_AC_HEATING"),
            furnishing = "furnished",
            listedBy = "agent",
            agentName = "Madurai Stays",
            agentPhone = "+91 98424 99001",
            agentPhoto = "https://randomuser.me/api/portraits/men/50.jpg",
            whatsappNumber = "+91 98424 99001",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-HS-00012",
            status = "active",
            createdAt = "2026-04-05T11:00:00Z",
            metadata = mapOf(
                "stay_type"    to "hotel_room",
                "max_guests"   to "2",
                "check_in"     to "12:00",
                "check_out"    to "10:00",
                "min_nights"   to "1",
                "facilities"   to "Free WiFi,AC,Hot Water,Breakfast,TV",
                "house_rules"  to "No Smoking,ID Proof Required",
                "cancellation" to "strict"
            )
        ),

        // ── Find a Contractor listings ────────────────────────────────────────
        Property(
            id = "mock-013",
            title = "Licensed Building Contractor — Chennai, Coimbatore, Trichy",
            description = "Expert building construction services across Tamil Nadu. " +
                "Specialise in residential buildings, apartments, and commercial complexes. " +
                "15+ years experience. DTCP & RERA compliant. Free site inspection and estimate. " +
                "Portfolio of 200+ completed projects available on request.",
            price = 1500L,
            priceFrequency = "month",   // used as 'per sqft rate' context
            propertyType = "building",
            listingType = "contractor",
            bedrooms = 0,
            bathrooms = 0,
            areaSqft = 0.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "All Areas",
            address = "Kaveri Construction Works, Anna Nagar, Chennai - 600040",
            latitude = 13.0858,
            longitude = 80.2101,
            images = listOf(
                "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=800",
                "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?w=800"
            ),
            amenities = listOf(),
            furnishing = "unfurnished",
            listedBy = "company",
            agentName = "Kaveri Construction Works",
            agentPhone = "+91 98400 45678",
            agentPhoto = "https://randomuser.me/api/portraits/men/40.jpg",
            whatsappNumber = "+91 98400 45678",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-CO-00013",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-06T09:00:00Z",
            metadata = mapOf(
                "work_category"    to "construction",
                "work_types"       to "Residential Building,Apartment,Commercial Complex,Foundation Work",
                "experience_yrs"   to "15",
                "service_districts" to "Chennai,Coimbatore,Tiruchirappalli,Vellore",
                "pricing_model"    to "per_sqft",
                "license_no"       to "TN/DTCP/CBE-2010-4521",
                "team_size"        to "50+",
                "timeline"         to "6-12 months",
                "warranty"         to "true",
                "warranty_dur"     to "5 years structural warranty"
            )
        ),
        Property(
            id = "mock-014",
            title = "Villa & House Construction — Coimbatore District",
            description = "Custom villa and independent house construction specialist in Coimbatore. " +
                "Modern design, quality materials, on-time delivery guaranteed. " +
                "3D elevation design, structural engineering, and turnkey projects. " +
                "Rate: ₹1,800/sqft onwards. Free consultation.",
            price = 1800L,
            priceFrequency = "month",
            propertyType = "villa_house",
            listingType = "contractor",
            bedrooms = 0,
            bathrooms = 0,
            areaSqft = 0.0,
            district = "Coimbatore",
            city = "Coimbatore",
            neighborhood = "All Areas",
            address = "Dream Build Constructions, Ganapathy, Coimbatore - 641006",
            latitude = 11.0168,
            longitude = 76.9558,
            images = listOf(
                "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800",
                "https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=800"
            ),
            amenities = listOf(),
            furnishing = "unfurnished",
            listedBy = "company",
            agentName = "Dream Build Constructions",
            agentPhone = "+91 97900 23456",
            agentPhoto = "https://randomuser.me/api/portraits/men/60.jpg",
            whatsappNumber = "+91 97900 23456",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-CO-00014",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-07T10:00:00Z",
            metadata = mapOf(
                "work_category"    to "construction",
                "work_types"       to "Villa,Independent House,Interior Fitout,3D Design",
                "experience_yrs"   to "12",
                "service_districts" to "Coimbatore,Erode,Tiruppur,Salem",
                "pricing_model"    to "per_sqft",
                "license_no"       to "TN/DTCP/CBE-2014-7832",
                "team_size"        to "21-50",
                "timeline"         to "4-8 months",
                "warranty"         to "true",
                "warranty_dur"     to "3 years civil warranty"
            )
        ),
        Property(
            id = "mock-015",
            title = "Interior Design & Painting — All Tamil Nadu Districts",
            description = "Professional painting and interior fitout services for homes and offices. " +
                "Asian Paints / Berger authorised applicator. " +
                "Services: Interior painting, exterior waterproofing, texture painting, wallpaper installation. " +
                "Free colour consultation. Work completed within 3–7 days. 1-year warranty.",
            price = 18L,
            priceFrequency = "month",
            propertyType = "painting_work",
            listingType = "contractor",
            bedrooms = 0,
            bathrooms = 0,
            areaSqft = 0.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "All Districts",
            address = "ColorPro Painters, T Nagar, Chennai - 600017",
            latitude = 13.0418,
            longitude = 80.2341,
            images = listOf(
                "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800",
                "https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=800"
            ),
            amenities = listOf(),
            furnishing = "unfurnished",
            listedBy = "individual",
            agentName = "ColorPro Painters",
            agentPhone = "+91 96550 78901",
            agentPhoto = "https://randomuser.me/api/portraits/men/70.jpg",
            whatsappNumber = "+91 96550 78901",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-CO-00015",
            status = "active",
            createdAt = "2026-04-08T12:00:00Z",
            metadata = mapOf(
                "work_category"    to "maintenance",
                "work_types"       to "Painting Work,Interior Fitout,Wallpaper,Texture Paint,Waterproofing",
                "experience_yrs"   to "8",
                "service_districts" to "Chennai,Kancheepuram,Chengalpattu,Vellore,Tiruvallur",
                "pricing_model"    to "per_sqft",
                "team_size"        to "6-20",
                "timeline"         to "3-7 days",
                "warranty"         to "true",
                "warranty_dur"     to "1 year paint warranty"
            )
        ),
        Property(
            id = "mock-016",
            title = "AC Installation & Maintenance — Chennai, Trichy, Madurai",
            description = "Certified air conditioning technician for all brands. " +
                "Services: Split AC / Window AC installation, annual maintenance contract (AMC), " +
                "gas refilling, PCB repair, and emergency breakdown service. " +
                "Daikin, Voltas, Blue Star, LG authorised service partner.",
            price = 500L,
            priceFrequency = "month",
            propertyType = "air_conditioning",
            listingType = "contractor",
            bedrooms = 0,
            bathrooms = 0,
            areaSqft = 0.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "All Areas",
            address = "CoolTech Services, Anna Salai, Chennai - 600002",
            latitude = 13.0604,
            longitude = 80.2496,
            images = listOf(
                "https://images.unsplash.com/photo-1606122017369-d782bbb78f32?w=800",
                "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800"
            ),
            amenities = listOf(),
            furnishing = "unfurnished",
            listedBy = "company",
            agentName = "CoolTech AC Services",
            agentPhone = "+91 98410 55678",
            agentPhoto = "https://randomuser.me/api/portraits/men/35.jpg",
            whatsappNumber = "+91 98410 55678",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-CO-00016",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-09T08:00:00Z",
            metadata = mapOf(
                "work_category"    to "maintenance",
                "work_types"       to "Air Conditioning,Installation,AMC,Gas Refilling,PCB Repair",
                "experience_yrs"   to "10",
                "service_districts" to "Chennai,Tiruchirappalli,Madurai,Coimbatore",
                "pricing_model"    to "fixed",
                "team_size"        to "6-20",
                "timeline"         to "Same day / 1-2 days",
                "warranty"         to "true",
                "warranty_dur"     to "1 year service warranty"
            )
        ),
        // ── Ground listings ───────────────────────────────────────────────────
        Property(
            id = "mock-018",
            title = "Full-Size Cricket Ground for Rent — Chennai, Anna Nagar",
            description = "Well-maintained cricket ground with proper pitch, outfield, and boundary ropes. " +
                "Turf pitch available. Floodlights for evening matches. " +
                "Dressing rooms, scoreboard, and parking included. " +
                "Available for tournaments, practice sessions, and corporate matches.",
            price = 5000L,
            priceFrequency = "month",
            propertyType = "cricket_ground",
            listingType = "ground",
            bedrooms = 0,
            bathrooms = 2,
            areaSqft = 32000.0,
            district = "Chennai",
            city = "Chennai",
            neighborhood = "Anna Nagar",
            address = "Green Fields Cricket Ground, 4th Main Road, Anna Nagar, Chennai - 600040",
            latitude = 13.0900,
            longitude = 80.2090,
            images = listOf(
                "https://images.unsplash.com/photo-1540747913346-19212a4b2bce?w=800",
                "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800"
            ),
            amenities = listOf("SECURITY", "COVERED_PARKING", "ELECTRICITY_BACKUP"),
            furnishing = "unfurnished",
            listedBy = "landlord",
            agentName = "Krishnaswamy Sports",
            agentPhone = "+91 98400 77001",
            agentPhoto = "https://randomuser.me/api/portraits/men/20.jpg",
            whatsappNumber = "+91 98400 77001",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-GR-00018",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-11T09:00:00Z",
            metadata = mapOf(
                "ground_type"      to "cricket",
                "surface"          to "turf",
                "length_m"         to "137",
                "width_m"          to "137",
                "capacity"         to "500",
                "floodlights"      to "true",
                "open_time"        to "06:00",
                "close_time"       to "22:00",
                "advance_booking"  to "true",
                "cancellation"     to "24h notice required",
                "facilities"       to "Dressing Rooms,Scoreboard,Covered Pavilion,Water Supply,Parking"
            )
        ),
        Property(
            id = "mock-019",
            title = "Indoor Badminton Court — Coimbatore, 3 Courts Available",
            description = "Professional synthetic mat badminton courts. " +
                "3 courts available simultaneously. LED court lighting, ventilation fans. " +
                "Racket and shuttlecock rental available. " +
                "Suitable for academies, tournaments, and casual play. " +
                "Monthly coaching packages also available.",
            price = 300L,
            priceFrequency = "month",
            propertyType = "badminton",
            listingType = "ground",
            bedrooms = 0,
            bathrooms = 1,
            areaSqft = 6000.0,
            district = "Coimbatore",
            city = "Coimbatore",
            neighborhood = "Peelamedu",
            address = "Champions Badminton Academy, Peelamedu, Coimbatore - 641004",
            latitude = 11.0230,
            longitude = 77.0070,
            images = listOf(
                "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800",
                "https://images.unsplash.com/photo-1593113646773-028c64a8f1b8?w=800"
            ),
            amenities = listOf("SECURITY", "COVERED_PARKING"),
            furnishing = "unfurnished",
            listedBy = "landlord",
            agentName = "Champions Academy",
            agentPhone = "+91 97900 44501",
            agentPhoto = "https://randomuser.me/api/portraits/men/25.jpg",
            whatsappNumber = "+91 97900 44501",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-GR-00019",
            status = "active",
            createdAt = "2026-04-12T10:00:00Z",
            metadata = mapOf(
                "ground_type"      to "badminton",
                "surface"          to "synthetic_mat",
                "length_m"         to "13",
                "width_m"          to "6",
                "capacity"         to "50",
                "floodlights"      to "true",
                "open_time"        to "05:30",
                "close_time"       to "21:30",
                "advance_booking"  to "true",
                "cancellation"     to "No refund within 2h of slot",
                "facilities"       to "Racket Rental,Shuttlecock,Changing Room,Water Cooler,LED Lighting"
            )
        ),
        Property(
            id = "mock-020",
            title = "Olympic Swimming Pool — Madurai Sports Complex",
            description = "Olympic-size swimming pool (50m × 25m) available for club bookings and events. " +
                "Heated water, lane dividers, timing system. " +
                "Changing rooms, shower area, and life-guard on duty. " +
                "Ideal for swim academies, school competitions, and corporate wellness programs.",
            price = 8000L,
            priceFrequency = "month",
            propertyType = "swimming_pool",
            listingType = "ground",
            bedrooms = 0,
            bathrooms = 4,
            areaSqft = 25000.0,
            district = "Madurai",
            city = "Madurai",
            neighborhood = "Mattuthavani",
            address = "Madurai District Sports Complex, Mattuthavani, Madurai - 625009",
            latitude = 9.9120,
            longitude = 78.1300,
            images = listOf(
                "https://images.unsplash.com/photo-1572553819674-49aa47bcd58e?w=800",
                "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800"
            ),
            amenities = listOf("SECURITY", "COVERED_PARKING", "ELECTRICITY_BACKUP", "FIRST_AID_MEDICAL_CENTER"),
            furnishing = "unfurnished",
            listedBy = "agent",
            agentName = "Madurai Sports Authority",
            agentPhone = "+91 98424 33221",
            agentPhoto = "https://randomuser.me/api/portraits/men/43.jpg",
            whatsappNumber = "+91 98424 33221",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-GR-00020",
            isVerified = true,
            status = "active",
            createdAt = "2026-04-13T08:00:00Z",
            metadata = mapOf(
                "ground_type"      to "swimming_pool",
                "surface"          to "tiles",
                "length_m"         to "50",
                "width_m"          to "25",
                "capacity"         to "200",
                "floodlights"      to "true",
                "open_time"        to "05:00",
                "close_time"       to "20:00",
                "advance_booking"  to "true",
                "cancellation"     to "48h notice required for refund",
                "facilities"       to "Heated Pool,Lane Dividers,Timing System,Life Guard,Changing Room,Shower,Locker"
            )
        ),
        Property(
            id = "mock-021",
            title = "Open Football Ground — Trichy, Available for Tournaments",
            description = "Large open football ground with natural grass, goal posts, and corner flags. " +
                "Can accommodate up to 500 spectators. Floodlights installed for night matches. " +
                "Changing rooms, washrooms. Perfect for school leagues, corporate events. " +
                "Catering service can be arranged.",
            price = 3500L,
            priceFrequency = "month",
            propertyType = "football",
            listingType = "ground",
            bedrooms = 0,
            bathrooms = 2,
            areaSqft = 45000.0,
            district = "Tiruchirappalli",
            city = "Trichy",
            neighborhood = "Ariyamangalam",
            address = "Srirangam Sports Ground, Ariyamangalam, Trichy - 620010",
            latitude = 10.8830,
            longitude = 78.7042,
            images = listOf(
                "https://images.unsplash.com/photo-1431324155629-1a6deb1dec8d?w=800",
                "https://images.unsplash.com/photo-1517927033932-b3d18e61fb3a?w=800"
            ),
            amenities = listOf("SECURITY", "ELECTRICITY_BACKUP"),
            furnishing = "unfurnished",
            listedBy = "landlord",
            agentName = "Trichy Sports Club",
            agentPhone = "+91 96553 22110",
            agentPhoto = "https://randomuser.me/api/portraits/men/55.jpg",
            whatsappNumber = "+91 96553 22110",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-GR-00021",
            status = "active",
            createdAt = "2026-04-14T07:00:00Z",
            metadata = mapOf(
                "ground_type"      to "football",
                "surface"          to "natural_grass",
                "length_m"         to "105",
                "width_m"          to "68",
                "capacity"         to "500",
                "floodlights"      to "true",
                "open_time"        to "06:00",
                "close_time"       to "23:00",
                "advance_booking"  to "false",
                "cancellation"     to "12h notice required",
                "facilities"       to "Goal Posts,Corner Flags,Changing Room,Washroom,Spectator Stand,Parking"
            )
        ),

        Property(
            id = "mock-017",
            title = "Plumbing & Civil Maintenance — Coimbatore, Erode, Salem",
            description = "Expert plumbing and civil maintenance services for residential and commercial properties. " +
                "Services: leakage repair, pipe fitting, bathroom renovation, waterproofing, floor/wall tiling. " +
                "Available 7 days a week. Emergency call-out within 2 hours. " +
                "10+ years experience. Free estimate provided.",
            price = 800L,
            priceFrequency = "month",
            propertyType = "plumbing",
            listingType = "contractor",
            bedrooms = 0,
            bathrooms = 0,
            areaSqft = 0.0,
            district = "Coimbatore",
            city = "Coimbatore",
            neighborhood = "All Areas",
            address = "FixIt Plumbing Services, Gandhipuram, Coimbatore - 641012",
            latitude = 11.0180,
            longitude = 76.9674,
            images = listOf(
                "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800",
                "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800"
            ),
            amenities = listOf(),
            furnishing = "unfurnished",
            listedBy = "individual",
            agentName = "FixIt Plumbing",
            agentPhone = "+91 97650 33445",
            agentPhoto = "https://randomuser.me/api/portraits/men/80.jpg",
            whatsappNumber = "+91 97650 33445",
            approvalStatus = ApprovalStatus.APPROVED,
            referenceId = "NX-CO-00017",
            status = "active",
            createdAt = "2026-04-10T07:00:00Z",
            metadata = mapOf(
                "work_category"    to "maintenance",
                "work_types"       to "Plumbing,Civil Work,Bathroom Renovation,Waterproofing,Floor Tiling",
                "experience_yrs"   to "10",
                "service_districts" to "Coimbatore,Erode,Salem,Tiruppur,Namakkal",
                "pricing_model"    to "hourly",
                "team_size"        to "2-5",
                "timeline"         to "Same day / 1-3 days",
                "warranty"         to "true",
                "warranty_dur"     to "6 months workmanship warranty"
            )
        )
    )

    // ── Public read-only access ──────────────────────────────────────────────

    val properties: List<Property> get() = _allProperties.toList()

    val approvedProperties: List<Property>
        get() = _allProperties.filter { it.approvalStatus == ApprovalStatus.APPROVED && it.status == "active" }

    fun byDistrict(district: String): List<Property> =
        approvedProperties.filter { it.district.equals(district, ignoreCase = true) }

    fun byListingType(listingType: String): List<Property> =
        approvedProperties.filter { it.listingType.equals(listingType, ignoreCase = true) }

    fun byDistrictAndType(district: String, listingType: String): List<Property> =
        approvedProperties.filter {
            it.district.equals(district, ignoreCase = true) &&
            it.listingType.equals(listingType, ignoreCase = true)
        }

    fun getById(id: String): Property? = _allProperties.firstOrNull { it.id == id }

    // ── Admin approval mutations ─────────────────────────────────────────────

    fun approveProperty(id: String) {
        val idx = _allProperties.indexOfFirst { it.id == id }
        if (idx >= 0) {
            _allProperties[idx] = _allProperties[idx].copy(
                approvalStatus  = ApprovalStatus.APPROVED,
                status          = "active",
                rejectionReason = null,
            )
        }
    }

    fun rejectProperty(id: String, reason: String) {
        val idx = _allProperties.indexOfFirst { it.id == id }
        if (idx >= 0) {
            _allProperties[idx] = _allProperties[idx].copy(
                approvalStatus  = ApprovalStatus.REJECTED,
                rejectionReason = reason,
                status          = "inactive",
            )
        }
    }

    /**
     * Re-approve a previously rejected property.
     * [proofNote] is stored as a comment (cleared rejection reason) and the
     * property is published. Visible immediately in public listing.
     */
    fun reApproveProperty(id: String, proofNote: String) {
        val idx = _allProperties.indexOfFirst { it.id == id }
        if (idx >= 0) {
            _allProperties[idx] = _allProperties[idx].copy(
                approvalStatus  = ApprovalStatus.APPROVED,
                status          = "active",
                // Store proof note as a note prefix so audit trail is preserved
                rejectionReason = null,
            )
        }
    }

    // ── Mock create (used by PostAdViewModel in debug / USE_MOCK_DATA mode) ──

    fun createProperty(request: PropertyCreateRequest, agentName: String = "You"): Property {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val seq = (_allProperties.size + 1).toString().padStart(5, '0')
        // Derive a reference prefix from the listing type
        val refPrefix = when (request.listingType.lowercase()) {
            "ground"       -> "NX-GR"
            "contractor"   -> "NX-CO"
            "holiday_stay" -> "NX-HS"
            "sale"         -> "NX-SL"
            else           -> "NX-TN"
        }
        val property = Property(
            id             = "mock-user-${UUID.randomUUID().toString().take(8)}",
            title          = request.title,
            description    = request.description ?: "",
            price          = request.price.toLong(),
            priceFrequency = request.priceFrequency,
            propertyType   = request.propertyType,
            listingType    = request.listingType,
            bedrooms       = request.bedrooms ?: 0,
            bathrooms      = request.bathrooms ?: 0,
            areaSqft       = request.areaSqft ?: 0.0,
            district       = request.district ?: "",
            city           = request.city ?: "",
            address        = request.address ?: "",
            latitude       = request.latitude,
            longitude      = request.longitude,
            neighborhood   = "",
            images         = listOf("https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800"),
            amenities      = request.amenities,
            furnishing     = request.furnishing,
            listedBy       = request.listedBy,
            agentName      = agentName,
            agentPhone     = request.agentPhone ?: "",
            agentPhoto     = "https://randomuser.me/api/portraits/men/1.jpg",
            whatsappNumber = request.whatsappNumber,
            approvalStatus = ApprovalStatus.PENDING,
            referenceId    = "$refPrefix-$seq",
            status         = "inactive",
            createdAt      = now,
            metadata       = request.metadata,
        )
        _allProperties.add(property)
        return property
    }

    // ── Bookings storage (mock mode) ─────────────────────────────────────────

    private val _bookings: MutableList<Booking> = mutableListOf()
//
  //  val bookings: List<Booking> get() = _bookings.toList()

    fun createBooking(
        propertyId: String,
        date: String,
        time: String,
        message: String?,
    ): Booking {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val booking = Booking(
            id         = UUID.randomUUID().toString(),
            propertyId = propertyId,
            buyerId    = "mock-user",
            visitDate  = date,
            visitTime  = time,
            status     = "confirmed",
            message    = message,
            createdAt  = now,
        )
        _bookings.add(0, booking) // newest first
        return booking
    }

    fun getBookings(): List<Booking> = _bookings.toList()

    fun cancelBooking(id: String) {
        val idx = _bookings.indexOfFirst { it.id == id }
        if (idx >= 0) {
            _bookings[idx] = _bookings[idx].copy(status = "cancelled")
        }
    }

    fun updateBookingStatus(id: String, status: String) {
        val idx = _bookings.indexOfFirst { it.id == id }
        if (idx >= 0) {
            _bookings[idx] = _bookings[idx].copy(status = status)
        }
    }
}
