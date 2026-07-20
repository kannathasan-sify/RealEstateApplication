package com.realestate.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.realestate.app.R
import com.realestate.app.data.models.AdBanner
import com.realestate.app.data.models.Property
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.ui.components.PropertyCard
import com.realestate.app.ui.theme.*
import kotlinx.coroutines.delay

// ── Mock Advertisement Banners ────────────────────────────────────────────────
// Each banner has an [order] field that controls the display sequence.
// Lower order = appears first. Change the order value to resequence without
// touching the list structure (simulates CMS-controlled ad ordering).

val mockAdBanners: List<AdBanner> = listOf(

    // ── ad-01 · PREMIUM · Rent targeting Chennai · A/B variant A ─────────────
    AdBanner(
        id = "ad-01",
        imageUrl = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800",
        title = "Dream Homes Await",
        subtitle = "Properties from ₹15L onwards",
        description = "Explore thousands of verified residential and commercial listings across Tamil Nadu. " +
            "From affordable 1 BHK apartments to luxury sea-view villas — all with transparent pricing, " +
            "RERA registration, and agent-verified photos.",
        accentHex = "#1565C0",
        order = 1,
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        advertiserName = "NestX Properties",
        advertiserLogo = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=100",
        ctaText = "Browse Listings",
        ctaUrl  = "nestx://property_list?listing_type=rent",
        // Marketing
        priority = 2, isSponsor = true, badge = "HOT",
        targetListingTypes = listOf("rent"),
        targetDistricts    = listOf("Chennai", "Coimbatore", "Madurai"),
        interestCount = 1284, viewCount = 45000,
        variant = "A", campaignId = "camp-nestx-main",
        frequencyCap = 5,
    ),

    // ── ad-01b · PREMIUM · Rent targeting Chennai · A/B variant B ────────────
    AdBanner(
        id = "ad-01b",
        imageUrl = "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=800",
        title = "Find Your Perfect Home",
        subtitle = "Rent, Buy or Invest in TN",
        description = "Thousands of verified homes across all 38 Tamil Nadu districts. " +
            "Search by budget, BHK, locality and amenities.",
        accentHex = "#1565C0",
        order = 2,
        advertiserName = "NestX Properties",
        ctaText = "Start Search",
        ctaUrl  = "nestx://property_list?listing_type=rent",
        // Marketing
        priority = 2, isSponsor = true, badge = "NEW",
        targetListingTypes = listOf("rent"),
        targetDistricts    = listOf("Chennai", "Coimbatore", "Madurai"),
        interestCount = 892, viewCount = 31000,
        variant = "B", campaignId = "camp-nestx-main",
        frequencyCap = 5,
    ),

    // ── ad-02 · FEATURED · Holiday Stay · Urgency ─────────────────────────────
    AdBanner(
        id = "ad-02",
        imageUrl = "https://images.unsplash.com/photo-1582407947304-fd86f028f716?w=800",
        title = "Holiday Stays",
        subtitle = "Book your perfect getaway",
        description = "Weekend resorts, heritage villas, beachfront stays, and budget hotels — " +
            "all verified and bookable instantly. Get the best rates directly from hosts across " +
            "Chennai, Ooty, Kodaikanal, Pondicherry, and Chettinad.",
        accentHex = "#00897B",
        order = 3,
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        advertiserName = "NestX Stays",
        ctaText = "Explore Stays",
        ctaUrl  = "nestx://property_list?listing_type=holiday_stay",
        // Marketing
        priority = 1, isSponsor = true, badge = "OFFER",
        targetListingTypes = listOf("holiday_stay"),
        interestCount = 432,
        isLimitedTime = true, offerText = "20% off this weekend!",
        expiresAt = "2026-04-25T23:59:59",
        variant = "A", campaignId = "camp-stays",
        frequencyCap = 4,
    ),

    // ── ad-03 · STANDARD · Contractor · All TN ────────────────────────────────
    AdBanner(
        id = "ad-03",
        imageUrl = "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=800",
        title = "Find a Contractor",
        subtitle = "Trusted builders across TN",
        description = "Licensed contractors for construction, plumbing, painting, AC service, " +
            "interior design and more. All contractors are background-verified with GST registration, " +
            "license number, and client reviews.",
        accentHex = "#E65100",
        order = 4,
        advertiserName = "NestX Services",
        ctaText = "Find Contractors",
        ctaUrl  = "nestx://property_list?listing_type=contractor",
        priority = 0,
        targetListingTypes = listOf("contractor"),
        badge = "TRENDING",
        interestCount = 276,
        variant = "A",
        frequencyCap = 3,
    ),

    // ── ad-04 · PREMIUM · Sale · Chennai Coimbatore · Video ───────────────────
    AdBanner(
        id = "ad-04",
        imageUrl = "https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800",
        title = "New Projects — Chennai",
        subtitle = "RERA approved, ready to move",
        description = "Discover the latest RERA-registered new launch projects in Chennai, Coimbatore, " +
            "and Madurai. Benefit from builder offers, pre-launch pricing, flexible EMI plans, " +
            "and direct developer contact.",
        accentHex = "#6A1B9A",
        order = 5,
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        advertiserName = "NestX New Projects",
        ctaText = "View Projects",
        ctaUrl  = "nestx://property_list?listing_type=new_project",
        priority = 2, isSponsor = true, badge = "HOT",
        targetListingTypes = listOf("sale", "new_project"),
        targetDistricts    = listOf("Chennai", "Coimbatore"),
        interestCount = 738, viewCount = 28000,
        isLimitedTime = true, offerText = "Pre-launch prices ending soon!",
        expiresAt = "2026-05-01T23:59:59",
        variant = "A", campaignId = "camp-newprojects",
        frequencyCap = 4,
    ),

    // ── ad-05 · FEATURED · Contractor · Build ─────────────────────────────────
    AdBanner(
        id = "ad-05",
        imageUrl = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800",
        title = "Build Your Villa",
        subtitle = "Premium construction services",
        description = "From foundation to finishing — hire trusted builders and interior designers " +
            "for your dream home. Get free 3D elevation designs, structural engineering, " +
            "and turnkey construction quotes. Serving all 38 districts of Tamil Nadu.",
        accentHex = "#AD1457",
        order = 6,
        advertiserName = "NestX Build",
        ctaText = "Get Free Quote",
        ctaUrl  = "nestx://property_list?listing_type=contractor&work_category=construction",
        priority = 1, isSponsor = true,
        targetListingTypes = listOf("contractor", "sale"),
        interestCount = 189,
        variant = "A", campaignId = "camp-build",
        frequencyCap = 3,
    ),

    // ── ad-06 · PREMIUM · Rent · Luxury Chennai ───────────────────────────────
    AdBanner(
        id = "ad-06",
        imageUrl = "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=800",
        title = "Luxury Villas for Rent",
        subtitle = "ECR, Coimbatore & more",
        description = "Exclusive private villas with swimming pools, landscaped gardens, and sea views " +
            "available for long-term and short-term rent along ECR, Coimbatore hills, and Ooty. " +
            "Verified agent listings with virtual tour available.",
        accentHex = "#1565C0",
        order = 7,
        advertiserName = "NestX Luxury",
        ctaText = "See Villas",
        ctaUrl  = "nestx://property_list?listing_type=rent&property_type=villa",
        priority = 2, isSponsor = true, badge = "TRENDING",
        targetListingTypes = listOf("rent"),
        targetDistricts    = listOf("Chennai"),
        interestCount = 524, viewCount = 19000,
        variant = "A", campaignId = "camp-luxury",
        frequencyCap = 4,
    ),

    // ── ad-07 · FEATURED · Holiday Stay · Resort · Urgency ───────────────────
    AdBanner(
        id = "ad-07",
        imageUrl = "https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800",
        title = "Weekend Resorts",
        subtitle = "Starting ₹2,500/night",
        description = "Escape the city — book weekend resort stays in Ooty, Kodaikanal, Yercaud, " +
            "and Munnar. All resorts feature pool access, room service, and complimentary breakfast. " +
            "Instant confirmation, no hidden charges.",
        accentHex = "#00897B",
        order = 8,
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
        advertiserName = "NestX Resorts",
        ctaText = "Book Now",
        ctaUrl  = "nestx://property_list?listing_type=holiday_stay&property_type=resort",
        priority = 1, badge = "OFFER",
        targetListingTypes = listOf("holiday_stay"),
        interestCount = 312,
        isLimitedTime = true, offerText = "Book 2 nights, get 1 free!",
        expiresAt = "2026-04-30T23:59:59",
        variant = "B", campaignId = "camp-stays",
        frequencyCap = 4,
    ),

    // ── ad-08 · STANDARD · Sale · Land ────────────────────────────────────────
    AdBanner(
        id = "ad-08",
        imageUrl = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800",
        title = "Land for Sale — TN",
        subtitle = "DTCP approved plots",
        description = "Agricultural, residential, and commercial land available across Tamil Nadu. " +
            "All plots are DTCP and RERA approved with clear title deeds. " +
            "Zero brokerage options available. Ideal for farm houses, layouts, and commercial development.",
        accentHex = "#2E7D32",
        order = 9,
        advertiserName = "NestX Land",
        ctaText = "View Plots",
        ctaUrl  = "nestx://property_list?listing_type=sale&property_type=land",
        priority = 0, badge = "NEW",
        targetListingTypes = listOf("sale", "ground"),
        interestCount = 98,
        variant = "A",
        frequencyCap = 3,
    ),

    // ── ad-09 · FEATURED · Rent · Madurai ─────────────────────────────────────
    AdBanner(
        id = "ad-09",
        imageUrl = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800",
        title = "2 & 3 BHK Flats",
        subtitle = "Affordable rentals in Madurai",
        description = "Well-maintained 2 BHK and 3 BHK apartments for rent in prime Madurai localities — " +
            "Annanagar, KK Nagar, Thirupparankundram, and Goripalayam. " +
            "Furnished and semi-furnished options. Society amenities included.",
        accentHex = "#1565C0",
        order = 10,
        advertiserName = "NestX Rentals",
        ctaText = "View Flats",
        ctaUrl  = "nestx://property_list?listing_type=rent&district=Madurai",
        priority = 1,
        targetListingTypes = listOf("rent"),
        targetDistricts    = listOf("Madurai", "Dindigul", "Theni"),
        interestCount = 67,
        variant = "A",
        frequencyCap = 3,
    ),

    // ── ad-10 · STANDARD · Contractor · AC Service · Urgency ─────────────────
    AdBanner(
        id = "ad-10",
        imageUrl = "https://images.unsplash.com/photo-1606122017369-d782bbb78f32?w=800",
        title = "AC Service & Repairs",
        subtitle = "All brands, all districts",
        description = "Authorised service partner for Daikin, Voltas, LG, Blue Star, and Hitachi. " +
            "Services: installation, annual maintenance contracts (AMC), gas refilling, " +
            "emergency breakdown. Same-day service in Chennai, Coimbatore, Trichy, and Madurai.",
        accentHex = "#E65100",
        order = 11,
        advertiserName = "CoolTech Services",
        ctaText = "Book Service",
        ctaUrl  = "nestx://property_list?listing_type=contractor&work_category=maintenance",
        priority = 0, badge = "OFFER",
        targetListingTypes = listOf("contractor"),
        interestCount = 143,
        isLimitedTime = true, offerText = "Free AMC check this month",
        expiresAt = "2026-04-30T23:59:59",
        variant = "A",
        frequencyCap = 3,
    ),

    // ── ad-11 · STANDARD · Contractor · Painting ─────────────────────────────
    AdBanner(
        id = "ad-11",
        imageUrl = "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800",
        title = "Interior Painting",
        subtitle = "Quality at best price",
        description = "Transform your home with professional interior and exterior painting. " +
            "Asian Paints and Berger authorised applicators. Texture painting, wallpaper, " +
            "and waterproofing available. Work guarantee with 1-year warranty.",
        accentHex = "#AD1457",
        order = 12,
        advertiserName = "ColorPro Painters",
        ctaText = "Get Estimate",
        ctaUrl  = "nestx://property_list?listing_type=contractor",
        priority = 0,
        targetListingTypes = listOf("contractor"),
        interestCount = 88,
        variant = "B",
        frequencyCap = 3,
    ),

    // ── ad-12 · FEATURED · Rent · PG · Student districts ─────────────────────
    AdBanner(
        id = "ad-12",
        imageUrl = "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800",
        title = "PG for Students",
        subtitle = "Chennai & Coimbatore",
        description = "Safe, affordable paying guest accommodations near major colleges and universities. " +
            "Facilities include WiFi, meals, laundry, CCTV, and biometric access. " +
            "Available in Chennai, Coimbatore, Trichy, and Salem.",
        accentHex = "#1565C0",
        order = 13,
        advertiserName = "NestX PG",
        ctaText = "Find PG Rooms",
        ctaUrl  = "nestx://property_list?listing_type=rent&property_type=pg",
        priority = 1, badge = "NEW",
        targetListingTypes = listOf("rent"),
        targetDistricts    = listOf("Chennai", "Coimbatore", "Tiruchirappalli", "Salem"),
        interestCount = 201,
        variant = "A",
        frequencyCap = 3,
    ),

    // ── ad-13 · PREMIUM · Sale+Rent · Commercial ──────────────────────────────
    AdBanner(
        id = "ad-13",
        imageUrl = "https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800",
        title = "Commercial Spaces",
        subtitle = "Shops, offices, warehouses",
        description = "Prime commercial properties for rent and sale — retail shops, co-working offices, " +
            "warehouses, industrial sheds, and labour camps. Verified listings with RERA compliance " +
            "and transparent pricing.",
        accentHex = "#6A1B9A",
        order = 14,
        advertiserName = "NestX Commercial",
        ctaText = "Browse Spaces",
        ctaUrl  = "nestx://property_list?listing_type=rent&property_type=shop",
        priority = 2, isSponsor = true, badge = "HOT",
        targetListingTypes = listOf("sale", "rent"),
        targetDistricts    = listOf("Chennai", "Coimbatore", "Tiruchirappalli"),
        interestCount = 356, viewCount = 14000,
        variant = "A", campaignId = "camp-commercial",
        frequencyCap = 4,
    ),

    // ── ad-14 · PREMIUM · Holiday Stay · Pool Villas · Video ──────────────────
    AdBanner(
        id = "ad-14",
        imageUrl = "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800",
        title = "Pool Villas",
        subtitle = "Private pool from ₹8,000/night",
        description = "Stunning private-pool villas for exclusive holiday stays. " +
            "Perfect for family vacations, destination weddings, and corporate retreats. " +
            "Locations: ECR, Coimbatore, Ooty foothills, Mahabalipuram.",
        accentHex = "#00897B",
        order = 15,
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        advertiserName = "NestX Pool Villas",
        ctaText = "View Pool Villas",
        ctaUrl  = "nestx://property_list?listing_type=holiday_stay&property_type=villa",
        priority = 2, isSponsor = true, badge = "TRENDING",
        targetListingTypes = listOf("holiday_stay"),
        interestCount = 614, viewCount = 22000,
        isLimitedTime = true, offerText = "Save ₹5,000 on first booking!",
        expiresAt = "2026-05-15T23:59:59",
        variant = "A", campaignId = "camp-luxury",
        frequencyCap = 4,
    ),

    // ── ad-15 · STANDARD · Rent · Warehouse ───────────────────────────────────
    AdBanner(
        id = "ad-15",
        imageUrl = "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?w=800",
        title = "Warehouse for Rent",
        subtitle = "Industrial zones, TN",
        description = "Spacious warehouse and storage facilities near industrial zones in Chennai, " +
            "Coimbatore, Hosur, and Tiruppur. Facilities include 24/7 CCTV, loading docks, " +
            "power backup, and truck access.",
        accentHex = "#2E7D32",
        order = 16,
        advertiserName = "NestX Industrial",
        ctaText = "View Warehouses",
        ctaUrl  = "nestx://property_list?listing_type=rent&property_type=warehouse",
        priority = 0,
        targetListingTypes = listOf("rent", "sale"),
        targetDistricts    = listOf("Chennai", "Coimbatore", "Hosur"),
        interestCount = 54,
        variant = "B",
        frequencyCap = 2,
    ),

    // ── ad-16 · FEATURED · Rent · Trichy Studios ──────────────────────────────
    AdBanner(
        id = "ad-16",
        imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800",
        title = "Studio Apartments",
        subtitle = "Fully furnished, Trichy",
        description = "Compact studio and 1 BHK apartments fully furnished with smart TV, " +
            "refrigerator, washing machine, and high-speed WiFi. " +
            "Ideal for working professionals and solo travellers in Trichy.",
        accentHex = "#1565C0",
        order = 17,
        advertiserName = "NestX Rentals",
        ctaText = "Explore Studios",
        ctaUrl  = "nestx://property_list?listing_type=rent&district=Tiruchirappalli",
        priority = 1,
        targetListingTypes = listOf("rent"),
        targetDistricts    = listOf("Tiruchirappalli", "Thanjavur"),
        interestCount = 112,
        variant = "A",
        frequencyCap = 3,
    ),

    // ── ad-17 · PREMIUM · Sale · ECR Sea-View ─────────────────────────────────
    AdBanner(
        id = "ad-17",
        imageUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800",
        title = "Sea-View Bungalows",
        subtitle = "ECR — exclusive listings",
        description = "Premium sea-view bungalows and independent houses available for rent and sale " +
            "along East Coast Road (ECR). Gated communities, private pools, " +
            "and landscaped gardens. Agent-verified listings only.",
        accentHex = "#00897B",
        order = 18,
        advertiserName = "NestX ECR Realty",
        ctaText = "View Bungalows",
        ctaUrl  = "nestx://property_list?listing_type=sale&neighborhood=ECR",
        priority = 2, isSponsor = true, badge = "HOT",
        targetListingTypes = listOf("sale", "rent"),
        targetDistricts    = listOf("Chennai"),
        interestCount = 487, viewCount = 17000,
        variant = "A", campaignId = "camp-luxury",
        frequencyCap = 4,
    ),

    // ── ad-18 · STANDARD · Contractor · Plumbing ──────────────────────────────
    AdBanner(
        id = "ad-18",
        imageUrl = "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=800",
        title = "Plumbing & Civil",
        subtitle = "Quick service guaranteed",
        description = "Licensed plumbing, civil maintenance, and waterproofing specialists. " +
            "Available 7 days a week with 2-hour emergency response. " +
            "Serving Coimbatore, Erode, Salem, and Tiruppur. Free estimate on WhatsApp.",
        accentHex = "#E65100",
        order = 19,
        advertiserName = "FixIt Services",
        ctaText = "Book Service",
        ctaUrl  = "nestx://property_list?listing_type=contractor",
        priority = 0,
        targetListingTypes = listOf("contractor"),
        targetDistricts    = listOf("Coimbatore", "Erode", "Salem", "Tiruppur"),
        interestCount = 71,
        variant = "B",
        frequencyCap = 3,
    ),

    // ── ad-19 · FEATURED · Sale · Farmhouses ──────────────────────────────────
    AdBanner(
        id = "ad-19",
        imageUrl = "https://images.unsplash.com/photo-1588880331179-bc9b93a8cb5e?w=800",
        title = "Farm Houses",
        subtitle = "Tamil Nadu highways",
        description = "Scenic farmhouses and countryside retreats available for sale and weekend rental " +
            "along Bangalore Highway, Hosur, Krishnagiri, and the Nilgiris district. " +
            "DTCP approved with clear title and water source.",
        accentHex = "#2E7D32",
        order = 20,
        advertiserName = "NestX Farms",
        ctaText = "View Farmhouses",
        ctaUrl  = "nestx://property_list?listing_type=sale&property_type=land",
        priority = 1,
        targetListingTypes = listOf("sale", "ground"),
        interestCount = 134,
        variant = "A",
        frequencyCap = 3,
    ),

    // ── ad-20 · STANDARD · Universal · Post Ad CTA ────────────────────────────
    AdBanner(
        id = "ad-20",
        imageUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800",
        title = "Post Your Ad Free",
        subtitle = "Reach lakhs of buyers today",
        description = "List your property for free on NestX and reach thousands of verified buyers, " +
            "renters, and investors across Tamil Nadu. " +
            "Properties go live within 24 hours after admin verification.",
        accentHex = "#1565C0",
        order = 21,
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        advertiserName = "NestX",
        ctaText = "Post Free Ad",
        ctaUrl  = "nestx://post_ad",
        priority = 0, badge = "NEW",
        interestCount = 0,        // universal — no targeting needed
        variant = "A",
        frequencyCap = 2,
    ),

).sortedBy { it.order }   // ← base order; personalisation re-ranks at runtime

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPropertyClick: (String) -> Unit,
    /** (listingType, district, workCategory?) — district is the currently selected district or "All TN" */
    onCategoryClick: (listingType: String, district: String, workCategory: String?) -> Unit,
    onFavouritesClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPostAdClick: () -> Unit = {},
) {
    val state       by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val homeAds     by viewModel.homeAds.collectAsState()

    val showSearchResults = searchQuery.isNotBlank() &&
            (state.searchResults.isNotEmpty() || state.isSearching)

    // ── District bottom-sheet ────────────────────────────────────────────────
    var showDistrictPicker by remember { mutableStateOf(false) }

    // ── Exit confirmation ────────────────────────────────────────────────────
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    // ── Auto-refresh on resume ───────────────────────────────────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitialResume by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isInitialResume) isInitialResume = false
                else viewModel.loadHome()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title  = { Text("Exit NestX?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text   = { Text("Are you sure you want to close the app?", fontSize = 14.sp, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { (context as? android.app.Activity)?.finish() },
                    colors  = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape   = RoundedCornerShape(10.dp),
                ) { Text("Exit", color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Stay")
                }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }

    // ── District Picker Bottom Sheet ─────────────────────────────────────────
    if (showDistrictPicker) {
        DistrictPickerSheet(
            districts        = state.allDistricts.ifEmpty { listOf("All TN") + TamilNaduData.districts },
            selectedDistrict = state.selectedDistrict,
            onSelect         = { district ->
                viewModel.selectDistrict(district)
                showDistrictPicker = false
            },
            onDismiss        = { showDistrictPicker = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().background(BackgroundWhite)) {

            // ── Top bar ──────────────────────────────────────────────────────
            HomeTopBar(
                selectedDistrict  = state.selectedDistrict,
                onDistrictClick   = { showDistrictPicker = true },
                onFavouritesClick = onFavouritesClick,
            )

            // ── Search bar ───────────────────────────────────────────────────
            SearchBarRow(
                query    = searchQuery,
                onChange = viewModel::onSearchQueryChange,
                onClear  = viewModel::clearSearch,
            )

            // ── Main scrollable feed ─────────────────────────────────────────
            LazyColumn(modifier = Modifier.fillMaxSize().background(BackgroundWhite)) {

                // 0. Quick-Access slider (Buy / Rent / Construction / Maintenance)
                item {
                    QuickAccessSlider(
                        onTileClick = { listingType, workCategory ->
                            onCategoryClick(listingType, state.selectedDistrict, workCategory)
                        }
                    )
                }


                // 2. Advertisement feed — ranked server-side by the Ad Ranking Engine
                //    (multi-factor AI score, 30% sponsored cap, labelled paid ads + CTAs).
                item {
                    HomeAdFeed(
                        ads = homeAds,
                        onImpression = { adId -> viewModel.recordAdImpression(adId) },
                        onHide = { adId -> viewModel.hideAd(adId) },
                        onAdClick = { ad ->
                            viewModel.recordAdClick(ad.adId)
                            val target = ad.ctaTarget
                            try {
                                when {
                                    target.isNullOrBlank() -> { /* no target */ }
                                    target.startsWith("http") -> {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(target),
                                            ),
                                        )
                                    }
                                    ad.cta.key == "call_owner" || ad.cta.key == "get_legal_verification" -> {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:$target"),
                                            ),
                                        )
                                    }
                                    target.startsWith("nestx://") -> {
                                        val uri = android.net.Uri.parse(target)
                                        if (uri.host == "property_list") {
                                            onCategoryClick(
                                                uri.getQueryParameter("listing_type") ?: "rent",
                                                state.selectedDistrict,
                                                uri.getQueryParameter("work_category"),
                                            )
                                        }
                                    }
                                    else -> { /* unhandled target — ignore */ }
                                }
                            } catch (e: Exception) {
                                // No handler installed — swallow to avoid crash
                            }
                        },
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }

                // Loading
                if (state.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NestXBlue)
                        }
                    }
                }
            }
        }

        // ── Search Results Overlay ────────────────────────────────────────────
        if (showSearchResults) {
            SearchResultsOverlay(
                results         = state.searchResults,
                isSearching     = state.isSearching,
                onPropertyClick = { id ->
                    viewModel.clearSearch()
                    onPropertyClick(id)
                },
                onDismiss = viewModel::clearSearch,
                topOffset = 128.dp,
            )
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(
    selectedDistrict: String,
    onDistrictClick: () -> Unit,
    onFavouritesClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NestXBlue)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo + App name
        Row(verticalAlignment = Alignment.CenterVertically) {
            // White chip so the navy+gold monogram stays visible on the blue header
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter            = painterResource(id = R.drawable.ic_dnestx_logo),
                    contentDescription = "DNestX",
                    modifier           = Modifier.size(26.dp),
                    tint               = Color.Unspecified,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "DNestX",
                color         = Color.White,
                fontWeight    = FontWeight.Bold,
                fontSize      = 21.sp,
                letterSpacing = (-0.3).sp,
            )
        }

        Spacer(Modifier.weight(1f))

        // ── District selector chip ────────────────────────────────────────
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = NestXBlueDark,
            modifier = Modifier.clickable(onClick = onDistrictClick),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text       = selectedDistrict,
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // Favourites heart icon
        IconButton(onClick = onFavouritesClick) {
            Icon(Icons.Outlined.FavoriteBorder, "Favourites", tint = Color.White)
        }
    }
}

// ── District Picker Bottom Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DistrictPickerSheet(
    districts: List<String>,
    selectedDistrict: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        containerColor    = Color.White,
        sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Select District",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                }
            }
            HorizontalDivider(color = BorderColor)

            // Scrollable district list
            LazyColumn(
                modifier       = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(districts) { district ->
                    val isSelected = district == selectedDistrict
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(district) }
                            .background(if (isSelected) BannerBlue else Color.White)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint     = if (isSelected) NestXBlue else TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            district,
                            fontSize   = 15.sp,
                            color      = if (isSelected) NestXBlue else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier   = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint     = NestXBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ── Search Bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarRow(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value         = query,
        onValueChange = onChange,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = {
            Text(
                if (query.isBlank()) "Search properties, grounds, stays…" else "",
                color = TextSecondary,
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
        },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, "Clear", tint = TextSecondary)
                }
            }
        } else null,
        singleLine = true,
        shape  = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = NestXBlue,
            unfocusedBorderColor = BorderColor,
            cursorColor          = NestXBlue,
        ),
    )
}

// ── Quick-Access Slider (Buy / Rent / Construction / Maintenance) ──────────
//
// Displayed as the very first section in the HomeScreen feed.
// Each tile is a gradient card with icon on top + bold title + subtitle text.
// [onTileClick] receives (listingType, workCategory?) so the caller can navigate
// directly to the filtered PropertyListScreen.

@Composable
private fun QuickAccessSlider(
    onTileClick: (listingType: String, workCategory: String?) -> Unit,
) {
    data class QuickTile(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val title: String,
        val subtitle: String,
        val listingType: String,
        val workCategory: String?,
        val gradientStart: Color,
        val gradientEnd: Color,
    )

    val tiles = listOf(
        QuickTile(
            icon         = Icons.Filled.Home,
            title        = "Buy",
            subtitle     = "For Sale Property",
            listingType  = "sale",
            workCategory = null,
            gradientStart = Color(0xFF1565C0),
            gradientEnd   = Color(0xFF1976D2),
        ),
        QuickTile(
            icon         = Icons.Filled.VpnKey,
            title        = "Rent",
            subtitle     = "Property For Rent",
            listingType  = "rent",
            workCategory = null,
            gradientStart = Color(0xFF00796B),
            gradientEnd   = Color(0xFF00897B),
        ),
        QuickTile(
            icon         = Icons.Filled.Construction,
            title        = "Construction",
            subtitle     = "Build your Property",
            listingType  = "contractor",
            workCategory = "construction",
            gradientStart = Color(0xFFE65100),
            gradientEnd   = Color(0xFFF57C00),
        ),
        QuickTile(
            icon         = Icons.Filled.Build,
            title        = "Maintenance",
            subtitle     = "Maintain Your Property",
            listingType  = "contractor",
            workCategory = "maintenance",
            gradientStart = Color(0xFF6A1B9A),
            gradientEnd   = Color(0xFF7B1FA2),
        ),
    )

    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text       = "What are you looking for?",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tiles.forEach { tile ->
                QuickAccessTile(
                    icon          = tile.icon,
                    title         = tile.title,
                    subtitle      = tile.subtitle,
                    gradientStart = tile.gradientStart,
                    gradientEnd   = tile.gradientEnd,
                    modifier      = Modifier.weight(1f),
                    onClick       = { onTileClick(tile.listingType, tile.workCategory) },
                )
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientStart, gradientEnd),
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier              = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.SpaceBetween,
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = title,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp),
                )
            }

            // Title + subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text       = title,
                    color      = Color.White,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                )
                Text(
                    text       = subtitle,
                    color      = Color.White.copy(alpha = 0.82f),
                    fontSize   = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign  = TextAlign.Center,
                    lineHeight = 12.sp,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Category Section (5 tiles only) ──────────────────────────────────────────

@Composable
private fun CategorySection(onCategoryClick: (String) -> Unit) {
    data class HomeCat(val emoji: String, val label: String, val type: String, val highlight: Boolean = false)

    val categories = listOf(
        HomeCat("🏠", "Property for Rent",   "rent"),
        HomeCat("🏡", "Property for Sale",   "sale"),
        HomeCat("🏖️", "Holiday Stay",        "holiday_stay", highlight = true),
        HomeCat("🏟️", "Ground",              "ground",       highlight = true),
        HomeCat("🔧", "Find a Contractor",   "contractor",   highlight = true),
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Spacer(Modifier.height(8.dp))

        // Row 1: Property for Rent | Property for Sale
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.take(2).forEach { cat ->
                CategoryTile(cat.emoji, cat.label, cat.type, cat.highlight, Modifier.weight(1f), onCategoryClick)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Row 2: Holiday Stay | Ground
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.drop(2).take(2).forEach { cat ->
                CategoryTile(cat.emoji, cat.label, cat.type, cat.highlight, Modifier.weight(1f), onCategoryClick)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Row 3: Find a Contractor (full width)
        val last = categories.last()
        CategoryTile(
            emoji    = last.emoji,
            label    = last.label,
            type     = last.type,
            highlight= last.highlight,
            modifier = Modifier.fillMaxWidth(),
            onClick  = onCategoryClick,
            fullWidth = true,
        )
    }
}

@Composable
private fun CategoryTile(
    emoji: String,
    label: String,
    type: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
    fullWidth: Boolean = false,
) {
    val bgColor = if (highlight) BannerBlue else SurfaceGray
    Card(
        modifier  = modifier
            .then(if (!fullWidth) Modifier.height(72.dp) else Modifier.height(60.dp))
            .clickable { onClick(type) },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 2.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(
                label,
                fontSize   = 13.sp,
                color      = if (highlight) NestXBlue else TextPrimary,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Medium,
                lineHeight = 17.sp,
            )
        }
    }
}

// ── Advertisement Banner Section (auto-scrolling HorizontalPager) ────────────
//
// Ads are already sorted by [AdBanner.order] in [mockAdBanners].
// • Uses Accompanist HorizontalPager with count = Int.MAX_VALUE for infinite loop.
// • Start page is Int.MAX_VALUE / 2 so left-swipe is possible from the first ad.
// • Auto-advances every 3.5 s; pauses while user is manually scrolling.
// • Dot indicators map virtual page index → actual ad index.

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun AdBannerSection(
    ads:             List<AdBanner>,
    onCtaClick:      (ctaUrl: String) -> Unit = {},
    onInterestClick: (adId: String, note: String?) -> Unit = { _, _ -> },
    /** Current user's selected district — used for personalisation */
    userDistrict:    String = "All TN",
    /** Last browsing context — used for personalisation */
    userListingType: String = "rent",
    userId:          String = "",
) {
    if (ads.isEmpty()) return

    // ── Personalised + frequency-capped ordering ──────────────────────────────
    val sortedAds = remember(ads, userDistrict, userListingType) {
        com.realestate.app.data.repository.AdPersonalizationEngine.rank(
            ads             = ads,
            userDistrict    = userDistrict,
            userListingType = userListingType,
            userId          = userId,
        ).ifEmpty { ads.sortedBy { it.order } }   // fallback if all capped
    }
    val adCount   = sortedAds.size

    // Virtual page space — start in the middle so both directions are scrollable
    val pageCount = Int.MAX_VALUE
    val startPage = pageCount / 2
    val pagerState = rememberPagerState(initialPage = startPage)

    // ── Dialog state ──────────────────────────────────────────────────────────
    var selectedAd by remember { mutableStateOf<AdBanner?>(null) }

    // ── Auto-advance timer — pauses while dialog is open ─────────────────────
    LaunchedEffect(pagerState, selectedAd) {
        while (selectedAd == null) {          // don't advance while dialog is open
            delay(3_500L)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    // Actual ad index (0-based, always non-negative mod)
    val currentAdIndex = (pagerState.currentPage - startPage).mod(adCount)

    Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {

        // ── Header: title + "X / 20" counter ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Advertisements",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${currentAdIndex + 1} / $adCount",
                fontSize = 11.sp,
                color    = TextSecondary,
            )
        }

        // ── Full-width pager ──────────────────────────────────────────────────
        HorizontalPager(
            count          = pageCount,
            state          = pagerState,
            modifier       = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            itemSpacing    = 10.dp,
        ) { page ->
            val adIndex = (page - startPage).mod(adCount)
            val ad      = sortedAds[adIndex]

            // Track impression once per ad per session
            LaunchedEffect(ad.id) {
                com.realestate.app.data.repository.AdFrequencyStore.recordImpression(ad.id)
                com.realestate.app.data.repository.AdAnalyticsTracker.track(
                    type         = com.realestate.app.data.repository.AdEventType.IMPRESSION,
                    ad           = ad,
                    userId       = userId,
                    userDistrict = userDistrict,
                )
            }

            AdBannerCard(
                ad      = ad,
                onClick = {
                    // Track click event
                    com.realestate.app.data.repository.AdAnalyticsTracker.track(
                        type         = com.realestate.app.data.repository.AdEventType.CLICK,
                        ad           = ad,
                        userId       = userId,
                        userDistrict = userDistrict,
                    )
                    selectedAd = ad
                },
            )
        }

        // ── Dot indicators ────────────────────────────────────────────────────
        HorizontalPagerIndicator(
            pagerState       = pagerState,
            pageCount        = adCount,
            pageIndexMapping = { page -> (page - startPage).mod(adCount) },
            modifier         = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
            activeColor      = NestXBlue,
            inactiveColor    = BorderColor,
            indicatorWidth   = 6.dp,
            indicatorHeight  = 6.dp,
            spacing          = 4.dp,
        )
    }

    // ── Ad detail dialog ──────────────────────────────────────────────────────
    selectedAd?.let { ad ->
        val retargetingLabel = remember(ad.id) {
            com.realestate.app.data.repository.AdPersonalizationEngine.retargetingLabel(
                ad              = ad,
                userDistrict    = userDistrict,
                userListingType = userListingType,
            )
        }
        AdDetailDialog(
            ad                = ad,
            onDismiss         = { selectedAd = null },
            onCtaClick        = { ctaUrl ->
                com.realestate.app.data.repository.AdAnalyticsTracker.track(
                    type         = com.realestate.app.data.repository.AdEventType.CTA_CLICK,
                    ad           = ad,
                    userId       = userId,
                    userDistrict = userDistrict,
                )
                onCtaClick(ctaUrl)
            },
            onInterestClick   = onInterestClick,
            initialInterested = com.realestate.app.data.repository.AdInterestRepository
                .isInterested(ad.id),
            retargetingLabel  = retargetingLabel,
            onVideoPlayed     = {
                com.realestate.app.data.repository.AdAnalyticsTracker.track(
                    type = com.realestate.app.data.repository.AdEventType.VIDEO_PLAY,
                    ad   = ad, userId = userId,
                )
            },
            onShared          = {
                com.realestate.app.data.repository.AdAnalyticsTracker.track(
                    type = com.realestate.app.data.repository.AdEventType.SHARE,
                    ad   = ad, userId = userId,
                )
            },
            onDismissWithDwell = { dwellSec ->
                com.realestate.app.data.repository.AdAnalyticsTracker.track(
                    type         = com.realestate.app.data.repository.AdEventType.DISMISS,
                    ad           = ad,
                    userId       = userId,
                    dwellSeconds = dwellSec,
                )
            },
        )
    }
}

/**
 * AdBannerCard — marketing-enriched ad card.
 *
 * Displays:
 *  • Background photo with dark gradient for legibility
 *  • Priority badges: "PREMIUM" / "FEATURED" chip (top-left, above AD label)
 *  • Custom badge chip: "HOT", "NEW", "TRENDING", "OFFER", etc. (top-right)
 *  • "Sponsored" label (if isSponsor)
 *  • Countdown timer (if expiresAt is set and within 7 days)
 *  • Urgency pill: "Limited Time" or custom offerText
 *  • Social proof: "312 interested" bottom-left (if interestCount ≥ 5)
 *  • Video play icon (right-center, if hasVideo)
 */
@Composable
private fun AdBannerCard(
    ad:      AdBanner,
    onClick: () -> Unit,
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(ad.accentHex))
    } catch (e: Exception) { NestXBlue }

    // Countdown ticker — updates every second when expiry < 7 days away
    var countdownText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(ad.expiresAt) {
        if (ad.expiresAt.isNullOrBlank()) return@LaunchedEffect
        while (true) {
            countdownText = computeCountdown(ad.expiresAt)
            if (countdownText == null) break
            kotlinx.coroutines.delay(1_000L)
        }
    }

    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .height(148.dp),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Background image ──────────────────────────────────────────────
            AsyncImage(
                model              = ad.imageUrl,
                contentDescription = ad.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )

            // ── Dark gradient for text legibility ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.80f),
                                Color.Black.copy(alpha = 0.20f),
                            )
                        )
                    )
            )

            // ── Top-left: Priority badge + AD chip + Sponsored ────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Priority badge
                    if (ad.priority >= 1) {
                        val (badgeColor, badgeLabel) = if (ad.priority >= 2)
                            Color(0xFFFF6F00) to "PREMIUM"
                        else
                            Color(0xFF6A1B9A) to "FEATURED"
                        AdChip(text = badgeLabel, color = badgeColor)
                    }
                    // AD chip
                    AdChip(text = "AD", color = accentColor)
                    // Sponsored label
                    if (ad.isSponsor) {
                        AdChip(text = "SPONSORED", color = Color(0xFF37474F))
                    }
                }
            }

            // ── Top-right: Custom badge ───────────────────────────────────────
            if (!ad.badge.isNullOrBlank()) {
                val badgeCol = when (ad.badge.uppercase()) {
                    "HOT"      -> Color(0xFFE53935)
                    "NEW"      -> Color(0xFF1565C0)
                    "TRENDING" -> Color(0xFFE65100)
                    "OFFER"    -> Color(0xFF2E7D32)
                    else       -> accentColor
                }
                Surface(
                    shape    = RoundedCornerShape(bottomStart = 8.dp),
                    color    = badgeCol,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        ad.badge.uppercase(),
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // ── Center-left: Title + subtitle + urgency ────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp, end = 54.dp, top = 24.dp),
            ) {
                Text(
                    ad.title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    ad.subtitle,
                    fontSize   = 12.sp,
                    color      = Color.White.copy(alpha = 0.88f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                // Urgency pill
                if (ad.hasUrgency) {
                    Spacer(Modifier.height(5.dp))
                    val urgencyText = when {
                        countdownText != null -> "⏰ $countdownText"
                        !ad.offerText.isNullOrBlank() -> "🔥 ${ad.offerText}"
                        else -> "⚡ Limited Time"
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.90f),
                    ) {
                        Text(
                            urgencyText,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White,
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            // ── Bottom-left: Social proof ─────────────────────────────────────
            if (ad.hasSocialProof) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint     = Color(0xFFEF9A9A),
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        ad.socialProofLabel,
                        fontSize   = 10.sp,
                        color      = Color.White.copy(alpha = 0.90f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // ── Right-center: Video play button ───────────────────────────────
            if (ad.hasVideo) {
                Surface(
                    shape    = CircleShape,
                    color    = Color.Black.copy(alpha = 0.50f),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Has video preview",
                            tint     = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Small pill chip used for AD / SPONSORED / PREMIUM / FEATURED labels */
@Composable
private fun AdChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color) {
        Text(
            text,
            fontSize   = 8.sp,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

/**
 * Computes a human-readable countdown string from an ISO-8601 expiry string.
 * Returns null when the expiry is in the past or > 7 days away (no urgency).
 */
private fun computeCountdown(expiresAt: String): String? {
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val expiry = fmt.parse(expiresAt) ?: return null
        val diff = expiry.time - System.currentTimeMillis()
        if (diff <= 0) return null
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        if (diff > sevenDaysMs) return null        // not urgent yet
        val days    = diff / (24 * 60 * 60 * 1000)
        val hours   = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = (diff % (60 * 1000)) / 1000
        when {
            days > 0   -> "Ends in ${days}d ${hours}h"
            hours > 0  -> "Ends in ${hours}h ${minutes}m"
            else       -> "Ends in ${minutes}m ${seconds}s"
        }
    } catch (e: Exception) { null }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        TextButton(onClick = onSeeAll) {
            Text("See All", color = NestXBlue, fontSize = 13.sp)
            Icon(Icons.Default.ArrowForward, null, tint = NestXBlue, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Search Results Overlay ────────────────────────────────────────────────────

@Composable
private fun SearchResultsOverlay(
    results: List<Property>,
    isSearching: Boolean,
    onPropertyClick: (String) -> Unit,
    onDismiss: () -> Unit,
    topOffset: androidx.compose.ui.unit.Dp,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(onClick = onDismiss),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topOffset, start = 12.dp, end = 12.dp)
                .heightIn(max = 440.dp),
            shape     = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            if (isSearching) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NestXBlue, strokeWidth = 2.dp)
                        Text("Searching…", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else if (results.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No results found", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn {
                    item {
                        Text(
                            "${results.size} Results",
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontSize   = 12.sp,
                            color      = TextSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                        HorizontalDivider(color = BorderColor)
                    }
                    items(results) { prop ->
                        SearchResultRow(property = prop, onClick = { onPropertyClick(prop.id) })
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(property: Property, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(1.dp)) {
            AsyncImage(
                model              = property.images.firstOrNull(),
                contentDescription = property.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            property?.title?.let {
                Text(

                    text       = it,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
            Text(
                text     = buildString {
                    if (property.neighborhood?.isNotBlank() == true) append("${property.neighborhood}, ")
                    append(property.district)
                },
                fontSize = 12.sp,
                color    = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text       = "₹${formatSearchPrice(property.price)} · ${property.listingTypeLabel}",
                fontSize   = 13.sp,
                color      = NestXBlue,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(Icons.Default.ArrowForward, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
    }
}

private fun formatSearchPrice(price: Long): String = when {
    price >= 10_000_000L -> "%.1fCr".format(price / 10_000_000.0)
    price >= 100_000L    -> "%.0fL".format(price / 100_000.0)
    else                 -> "%,d".format(price)
}
