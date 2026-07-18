package com.realestate.app.ui.property

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.draw.alpha
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.realestate.app.data.models.Property
import com.realestate.app.ui.components.PropertyCard
import com.realestate.app.ui.components.PropertyMapView
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    viewModel: PropertyViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onBookVisit: (String) -> Unit,
    onPropertyClick: (String) -> Unit,
) {
    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
        viewModel.checkSaved(propertyId)          // ← check real saved state from API
        viewModel.checkInterest(propertyId)       // ← already enquired?
        viewModel.loadDiscussions(propertyId)
    }
    val state   by viewModel.detailState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val discussions by viewModel.discussions.collectAsState()
    val interestState by viewModel.interestState.collectAsState()
    val hasExpressedInterest by viewModel.hasExpressedInterest.collectAsState()

    when (val s = state) {
        is PropertyDetailUiState.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NestXBlue)
            }
        is PropertyDetailUiState.Error ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message)
            }
        is PropertyDetailUiState.Success ->
            PropertyDetailContent(
                property        = s.property,
                similar         = s.similar,
                isSaved         = isSaved,
                discussions     = discussions,
                buyerName            = viewModel.currentUserName,
                interestState        = interestState,
                hasExpressedInterest = hasExpressedInterest,
                onExpressInterest    = { viewModel.submitInterest(s.property.id) },
                onPostDiscussion = { msg, parentId -> viewModel.postDiscussion(s.property.id, msg, parentId) },
                onToggleSave    = { viewModel.toggleSave(s.property.id) },
                onBack          = onBack,
                onBookVisit     = { onBookVisit(s.property.id) },
                onPropertyClick = onPropertyClick,
            )
    }
}


// ── Detail content ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
private fun PropertyDetailContent(
    property: Property,
    similar: List<Property>,
    isSaved: Boolean,
    discussions: List<com.realestate.app.data.models.Discussion>,
    buyerName: String?,
    interestState: InterestState,
    hasExpressedInterest: Boolean,
    onExpressInterest: () -> Unit,
    onPostDiscussion: (String, String?) -> Unit,
    onToggleSave: () -> Unit,
    onBack: () -> Unit,
    onBookVisit: () -> Unit,
    onPropertyClick: (String) -> Unit,
) {
    val context = LocalContext.current
    var showFullDesc by remember { mutableStateOf(false) }
    val pagerState   = rememberPagerState()

    // ── Resolve contact numbers ───────────────────────────────────────────────
    // whatsappNumber takes priority for WhatsApp; agentPhone is the general call number.
    // For Ground/Contractor listings the dedicated WhatsApp is in whatsappNumber.
    // .orEmpty() guards against Gson deserialising JSON null into a non-nullable String field.
    val callNumber    = property.agentPhone.orEmpty().ifBlank { null }
    val waNumber      = (property.whatsappNumber?.ifBlank { null } ?: callNumber)
        ?.replace(" ", "")
        ?.replace("+", "")
        ?.trimStart('0')
        ?.let { if (it.startsWith("91")) it else "91$it" }

    // The sticky bottom contact bar (Call / WhatsApp / Visit) is intentionally removed —
    // those actions are already shown in the contact card within the content below, so the
    // bar would be redundant. Contact numbers (callNumber/waNumber) are still resolved above
    // for that in-content card.

    Scaffold(containerColor = SurfaceGray) { padding ->
        LazyColumn(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {

            // ── Image Carousel (swipeable) ───────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {

                    // ── Swipeable pager ──────────────────────────────────────
                    HorizontalPager(
                        count    = property.images.size.coerceAtLeast(1),
                        state    = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        AsyncImage(
                            model              = property.images.getOrElse(page) { "" },
                            contentDescription = "${property.title} — image ${page + 1}",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }

                    // ── Back button (top-left) ───────────────────────────────
                    IconButton(
                        onClick  = onBack,
                        modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }

                    // ── "current / total" counter (top-right) ────────────────
                    if (property.images.size > 1) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                            shape    = RoundedCornerShape(12.dp),
                            color    = Color.Black.copy(alpha = 0.6f),
                        ) {
                            Text(
                                "${pagerState.currentPage + 1}/${property.images.size}",
                                color    = Color.White, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // ── Dot indicators (bottom-center) ───────────────────────
                    if (property.images.size > 1) {
                        Row(
                            modifier              = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            repeat(property.images.size) { idx ->
                                val isSelected = idx == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White
                                            else Color.White.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }

                    // ── Save + Share (bottom-right) ──────────────────────────
                    Row(
                        modifier              = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick  = onToggleSave,         // ← real API call
                            modifier = Modifier.background(Color.White, CircleShape).size(36.dp),
                        ) {
                            Icon(
                                if (isSaved) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isSaved) "Unsave" else "Save",
                                tint     = if (isSaved) NestXBlue else TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick  = {},
                            modifier = Modifier.background(Color.White, CircleShape).size(36.dp),
                        ) {
                            Icon(Icons.Default.Share, "Share", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── "I'm Interested" lead CTA ────────────────────────────────────
            item {
                PropertyInterestCta(
                    hasExpressedInterest = hasExpressedInterest,
                    interestState        = interestState,
                    onExpressInterest    = onExpressInterest,
                )
            }

            // ── Price + Title ───────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            property.priceDisplay,
                            fontSize   = 22.sp, fontWeight = FontWeight.Bold, color = NestXBlue,
                            modifier   = Modifier.weight(1f),
                        )
                        // ── "Admin Approved ✓" badge — shown for every approved listing ──
                        if (property.isApproved) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusApproved.copy(alpha = 0.12f),
                            ) {
                                Row(
                                    modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint     = StatusApproved,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "Admin Approved",
                                        fontSize     = 11.sp,
                                        color        = StatusApproved,
                                        fontWeight   = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                        // ── "Verified" badge — extra trust mark set manually by admin ──
                        if (property.isVerified) {
                            Surface(shape = RoundedCornerShape(8.dp), color = BannerBlue) {
                                Row(
                                    modifier            = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment   = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Verified, null, tint = NestXBlue, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Verified", fontSize = 11.sp, color = NestXBlue, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    if (property.ratePerSqftDisplay != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(property.ratePerSqftDisplay!!, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    }

                    // Rent Deposit and Availability
                    if (property.isRent) {
                        if (property.deposit != null && property.deposit > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("Security Deposit: ₹${"%,d".format(property.deposit)}", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        if (!property.availabilityDate.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Available from: ${property.availabilityDate}", fontSize = 13.sp, color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Contractor / Maintenance Ratings
                    if (property.isContractor || property.isMaintenance) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (property.ratingCount > 0) "${property.ratingAvg} (${property.ratingCount} reviews)" else "No reviews yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(property.title.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${property.neighborhood.orEmpty()}, ${property.district.orEmpty()}", fontSize = 13.sp, color = TextSecondary)
                    }
                }
                }
            }

            // ── Stats Row — category-aware ───────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    when {
                        property.isGround -> {
                            // Ground / Sports venue stats
                            val meta = property.metadata ?: emptyMap()
                            val groundType = meta["ground_type"]?.toString()
                                ?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                            val surface    = meta["surface"]?.toString()
                                ?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                            val capacity   = meta["capacity"]?.toString()
                            val fromTo     = if (meta["available_from"] != null && meta["available_to"] != null)
                                "${meta["available_from"]} – ${meta["available_to"]}" else null
                            if (!groundType.isNullOrBlank())  StatItem("🏟️", groundType)
                            if (!surface.isNullOrBlank())     StatItem("🌿", surface)
                            if (!capacity.isNullOrBlank())    StatItem("👥", "$capacity capacity")
                            if (!fromTo.isNullOrBlank())      StatItem("🕐", fromTo)
                        }
                        property.isContractor -> {
                            // Contractor / Service provider stats
                            val meta        = property.metadata ?: emptyMap()
                            val workCat     = meta["work_category"]?.toString()?.replaceFirstChar { it.uppercase() }
                            val experience  = meta["experience_yrs"]?.let { "$it yrs exp" }
                            val team        = meta["team_size"]?.toString()
                                ?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                            val pricing     = meta["pricing_model"]?.toString()
                                ?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                            val dispSub = property.displaySubCategory.ifBlank { workCat }
                            if (!dispSub.isNullOrBlank())   StatItem("🔨", dispSub)
                            if (!experience.isNullOrBlank())StatItem("⭐", experience)
                            if (!team.isNullOrBlank())      StatItem("👷", team)
                            if (!pricing.isNullOrBlank())   StatItem("💰", pricing)
                        }
                        property.isHolidayStay -> {
                            // Holiday Stay stats
                            val meta       = property.metadata ?: emptyMap()
                            val stayType   = meta["stay_type"]?.toString()
                                ?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                            val maxGuests  = meta["max_guests"]?.let { "$it guests" }
                            val minNights  = meta["min_nights"]?.let { "$it night min" }
                            val checkIn    = meta["check_in"]?.let { "In: $it" }
                            if (!stayType.isNullOrBlank())  StatItem("🏡", stayType)
                            if ((property.bedrooms ?: 0) > 0)      StatItem("🛏️", "${property.bedrooms} rooms")
                            if (!maxGuests.isNullOrBlank()) StatItem("👥", maxGuests)
                            if (!minNights.isNullOrBlank()) StatItem("📅", minNights)
                            if (checkIn != null)            StatItem("🕐", checkIn)
                        }
                        else -> {
                            // Standard property stats (Rent / Sale)
                            if ((property.bedrooms ?: 0) > 0)  StatItem("🛏️", "${property.bedrooms} BHK")
                            if ((property.bathrooms ?: 0) > 0) StatItem("🚿", "${property.bathrooms} Bath")
                            if ((property.areaSqft ?: 0.0) > 0.0)  StatItem("📐", "${property.areaSqft?.toInt() ?: 0} sqft")
                            StatItem("🏠", property.propertyType.orEmpty()
                                .replaceFirstChar { it.uppercase() }.replace("_", " "))
                        }
                    }
                }
                }
            }

            // ── Agent Contact Card (shown early — 3rd item) ──────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                AgentContactSection(
                    property   = property,
                    context    = context,
                    callNumber = callNumber,
                    waNumber   = waNumber,
                )
                }
            }

            // ── Reference ────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                Text(
                    "Ref: ${property.referenceId}  •  Listed: ${property.createdAt.take(10)}",
                    fontSize = 11.sp, color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                }
            }

            // ── Description ──────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        property.description.orEmpty(),
                        fontSize  = 14.sp, color = TextPrimary,
                        maxLines  = if (showFullDesc) Int.MAX_VALUE else 3,
                        overflow  = TextOverflow.Ellipsis,
                    )
                    if (property.description.orEmpty().length > 150) {
                        TextButton(onClick = { showFullDesc = !showFullDesc }) {
                            Text(if (showFullDesc) "Show Less" else "Read More", color = NestXBlue)
                        }
                    }
                }
                }
            }

            // ── Amenities ────────────────────────────────────────────────────
            if (property.amenities.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Amenities", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(property.amenities) { amenity ->
                                Surface(shape = RoundedCornerShape(20.dp), color = BannerBlue) {
                                    Text(
                                        amenity.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        color    = NestXBlue, fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }

            // ── Company Profile (Contractor / Maintenance) ───────────────────
            if (!property.companyProfile.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Company Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(property.companyProfile, fontSize = 14.sp, color = TextPrimary)
                    }
                    }
                }
            }

            // ── Category-specific extra details ─────────────────────────────
            if (!property.metadata.isNullOrEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                    CategoryMetaSection(property = property)
                    }
                }
            }

            // ── Media Links (YouTube Video / Instagram Reel) ──────────────────
            val hasYoutube = !property.youtubeUrl.isNullOrBlank()
            val hasInstagram = !property.instagramUrl.isNullOrBlank()
            if (hasYoutube || hasInstagram) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Virtual Media Tour", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (hasYoutube) {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(property.youtubeUrl)))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🎥 YouTube Video", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (hasInstagram) {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(property.instagramUrl)))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📸 Instagram Reel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    }
                }
            }

            // ── Nearby Places (Schools & Hospitals) ─────────────────────────
            val hasSchools = property.nearbySchools.isNotEmpty()
            val hasHospitals = property.nearbyHospitals.isNotEmpty()
            if (hasSchools || hasHospitals) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nearby Places", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (hasSchools) {
                            Spacer(Modifier.height(8.dp))
                            Text("🏫 Schools", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NestXBlue)
                            Spacer(Modifier.height(4.dp))
                            property.nearbySchools.forEach { school ->
                                Text("• $school", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        if (hasHospitals) {
                            Spacer(Modifier.height(8.dp))
                            Text("🏥 Hospitals", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NestXBlue)
                            Spacer(Modifier.height(4.dp))
                            property.nearbyHospitals.forEach { hospital ->
                                Text("• $hospital", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    }
                }
            }

            // ── Property Documents ──────────────────────────────────────────
            if (property.documentUrls.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Property Documents", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        property.documentUrls.forEachIndexed { index, docUrl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(docUrl)))
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, tint = NestXBlue, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Document ${index + 1} (PDF / Image)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NestXBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowForwardIos, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                        }
                    }
                    }
                }
            }

            // ── Location / Map ───────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                PropertyMapSection(property = property, context = context)
                }
            }

            // ── Similar Properties ───────────────────────────────────────────
            if (similar.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        Text("Similar Properties", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(similar) { prop ->
                                PropertyCard(
                                    property = prop,
                                    compact  = true,
                                    onClick  = { onPropertyClick(prop.id) },
                                )
                            }
                        }
                    }
                }
            }

            // Q&A / Discussions section hidden per product request (2026-07-15) — the
            // data layer (loadDiscussions/postDiscussion/discussions state) is left intact
            // above so this can be re-enabled by restoring this block if needed later.

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Category-specific metadata section ───────────────────────────────────────

/**
 * Renders category-specific extra detail cards for Ground, Contractor, and Holiday Stay
 * listings. Uses the [Property.metadata] map populated from the category-form at ad creation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryMetaSection(property: Property) {
    val meta = property.metadata ?: return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

        when {
            // ── Ground ────────────────────────────────────────────────────────
            property.isGround -> {
                val groundType = meta["ground_type"]?.toString()?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                val surface    = meta["surface"]?.toString()?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                val length     = meta["length_m"]?.toString()
                val width      = meta["width_m"]?.toString()
                val capacity   = meta["capacity"]?.toString()
                val avFrom     = meta["available_from"]?.toString()
                val avTo       = meta["available_to"]?.toString()
                val flood      = meta["floodlights"]?.toString()
                val advance    = meta["advance_booking"]?.toString()
                val cancel     = meta["cancellation"]?.toString()?.replaceFirstChar { it.uppercase() }

                Text("Ground Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                MetaDetailGrid(
                    listOfNotNull(
                        groundType?.let { "Sport" to it },
                        surface?.let { "Surface" to it },
                        length?.let { l -> width?.let { w -> "Dimensions" to "${l}m × ${w}m" } },
                        capacity?.let { "Capacity" to "$it players" },
                        avFrom?.let { f -> avTo?.let { t -> "Hours" to "$f – $t" } },
                        flood?.let { if (it == "true") "Floodlights" to "Available" else null },
                        advance?.let { if (it == "true") "Booking" to "Advance required" else null },
                        cancel?.let { "Cancellation" to it },
                    )
                )
                val facilities = meta["facilities"]?.toString()
                if (!facilities.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Facilities", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        facilities.split(",").forEach { f ->
                            MetaChip(f.trim().replace("_", " ").replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }

            // ── Contractor ────────────────────────────────────────────────────
            property.isContractor -> {
                val workCategory = meta["work_category"]?.toString()?.replaceFirstChar { it.uppercase() }
                val experience   = meta["experience_yrs"]?.toString()?.let { "Experience" to "$it years" }
                val teamSize     = meta["team_size"]?.toString()?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                val pricingModel = meta["pricing_model"]?.toString()?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                val timeline     = meta["timeline"]?.toString()
                val licenseNo    = meta["license_no"]?.toString()
                val warranty     = meta["warranty"]?.toString()
                val warrantyDur  = meta["warranty_dur"]?.toString() ?: "Yes"

                Text("Contractor Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                MetaDetailGrid(
                    listOfNotNull(
                        workCategory?.let { "Category" to it },
                        experience,
                        teamSize?.let { "Team Size" to it },
                        pricingModel?.let { "Pricing" to it },
                        timeline?.let { "Timeline" to it },
                        licenseNo?.let { "License No." to it },
                        warranty?.let { if (it == "true") "Warranty" to warrantyDur else null },
                    )
                )
                val workTypes = meta["work_types"]?.toString()
                if (!workTypes.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Services Offered", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        workTypes.split(",").forEach { t ->
                            MetaChip(t.trim().replace("_", " ").replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                val serviceAreas = meta["service_areas"]?.toString()
                if (!serviceAreas.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Service Areas", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        serviceAreas.split(",").forEach { a ->
                            MetaChip(a.trim())
                        }
                    }
                }
            }

            // ── Holiday Stay ──────────────────────────────────────────────────
            property.isHolidayStay -> {
                val stayType     = meta["stay_type"]?.toString()?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                val maxGuests    = meta["max_guests"]?.toString()?.let { "$it guests" }
                val checkIn      = meta["check_in"]?.toString()
                val checkOut     = meta["check_out"]?.toString()
                val minNights    = meta["min_nights"]?.toString()?.let { "$it night${if (it != "1") "s" else ""}" }
                val cancellation = meta["cancellation"]?.toString()?.replaceFirstChar { it.uppercase() }

                Text("Stay Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                MetaDetailGrid(
                    listOfNotNull(
                        stayType?.let { "Type" to it },
                        maxGuests?.let { "Max Guests" to it },
                        checkIn?.let { "Check-in" to it },
                        checkOut?.let { "Check-out" to it },
                        minNights?.let { "Min Stay" to it },
                        cancellation?.let { "Cancellation" to it },
                    )
                )
                val facilities = meta["facilities"]?.toString()
                if (!facilities.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Facilities", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        facilities.split(",").forEach { f ->
                            MetaChip(f.trim().replace("_", " ").replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                val houseRules = meta["house_rules"]?.toString()
                if (!houseRules.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("House Rules", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        houseRules.split(",").forEach { r ->
                            MetaChip(r.trim().replace("_", " ").replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }
    }
}

/** 2-column grid of key/value pairs for compact metadata display. */
@Composable
private fun MetaDetailGrid(items: List<Pair<String, String>>) {
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (key, value) ->
                    Surface(
                        modifier  = Modifier.weight(1f),
                        shape     = RoundedCornerShape(8.dp),
                        color     = SurfaceGray,
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(key, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                // pad last row if odd count
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Single blue-tinted chip for listing facilities, house rules, work types, etc. */
@Composable
private fun MetaChip(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = BannerBlue) {
        Text(
            text     = text,
            color    = NestXBlue,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

// ── Agent Contact Section ─────────────────────────────────────────────────────
// Shown as the 3rd content item so users see agent info immediately.

@Composable
private fun AgentContactSection(
    property:   Property,
    context:    android.content.Context,
    callNumber: String?,      // resolved agentPhone (null if blank)
    waNumber:   String?,      // resolved WhatsApp e164 number (whatsappNumber ?? agentPhone)
) {
    // ── Category-aware labels ─────────────────────────────────────────────────
    val sectionTitle = when {
        property.isGround      -> "Contact Venue Owner"
        property.isContractor  -> "Contact Contractor"
        property.isHolidayStay -> "Contact Host"
        property.listedBy == "landlord" -> "Contact Landlord"
        property.listedBy == "builder"  -> "Contact Builder"
        else                            -> "Contact Agent"
    }
    val roleBadgeLabel = when {
        property.isGround      -> "Venue Owner"
        property.isContractor  -> "Contractor"
        property.isHolidayStay -> "Host"
        property.listedBy == "landlord" -> "Landlord"
        property.listedBy == "builder"  -> "Builder"
        else                            -> "Agent"
    }
    val displayName = property.agentName.ifBlank {
        when {
            property.isGround      -> "Venue Owner"
            property.isContractor  -> "Contractor"
            property.isHolidayStay -> "Host"
            else                   -> "Agent"
        }
    }

    // Displayed phone number — prefer whatsappNumber for WA-only contacts (Contractor/Ground)
    val displayPhone = callNumber
        ?: property.whatsappNumber?.ifBlank { null }

    val hasAnyContact = callNumber != null || waNumber != null

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

        // ── Section header ────────────────────────────────────────────────────
        Text(
            sectionTitle,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))

        // ── Contact person info row ───────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            // Avatar
            Box(
                modifier         = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NestXBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                if (property.agentPhoto.isNotBlank()) {
                    AsyncImage(
                        model              = property.agentPhoto,
                        contentDescription = displayName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Default.Person, null,
                        tint     = NestXBlue,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = TextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NestXBlue.copy(alpha = 0.10f),
                ) {
                    Text(
                        roleBadgeLabel,
                        fontSize   = 11.sp,
                        color      = NestXBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }

        if (hasAnyContact) {
            // ── Phone / WhatsApp number display bar ───────────────────────────
            if (displayPhone != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    color    = BannerBlue,
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.Phone, null,
                            tint     = NestXBlue,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            displayPhone,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = NestXBlue,
                        )
                    }
                }
            }

            // ── If dedicated WhatsApp number differs from call number, show it ──
            val showSeparateWa = property.whatsappNumber?.isNotBlank() == true
                    && property.whatsappNumber != property.agentPhone
                    && callNumber != null  // only show if call line also exists
            if (showSeparateWa) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    color    = Color(0xFF25D366).copy(alpha = 0.10f),
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("📱", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "WhatsApp: ${property.whatsappNumber}",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color(0xFF1B5E20),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Call button — only shown when a phone number exists
                if (callNumber != null) {
                    Button(
                        onClick  = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$callNumber"))
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                        shape    = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // WhatsApp button — uses dedicated waNumber (whatsappNumber ?? agentPhone)
                if (waNumber != null) {
                    Button(
                        onClick  = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/$waNumber?text=${Uri.encode(buildEnquiryMessage(property, null))}")
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape    = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            "💬  WhatsApp",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White,
                        )
                    }
                }
            }

        } else {
            // No contact info at all
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                color    = SurfaceGray,
            ) {
                Text(
                    "Contact details not available",
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

// ── Map / Location Section ───────────────────────────────────────────────────
// Uses OpenStreetMap static tile via Coil — no Google Maps SDK / API key needed.
// Tapping anywhere opens the native Maps app with the exact coordinates.

@Composable
private fun PropertyMapSection(
    property: Property,
    context: android.content.Context,
) {
    val lat = property.latitude
    val lng = property.longitude
    val hasCoords = lat != null && lng != null

    // Null-safe helpers — Gson can set non-null String fields to null at runtime
    // if the API returns `null` for that field, so we always use orEmpty().
    val safeAddress      = property.address.orEmpty()
    val safeNeighborhood = property.neighborhood.orEmpty()
    val safeDistrict     = property.district.orEmpty()
    val safeTitle        = property.title.orEmpty()

    // Tap intent — geo: URI opens any installed Maps app; fallback to web
    fun openMaps() {
        val uri = if (hasCoords)
            Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(safeTitle)})")
        else
            Uri.parse("https://maps.google.com/?q=${Uri.encode(
                listOfNotNull(
                    safeNeighborhood.takeIf { it.isNotBlank() },
                    safeDistrict.takeIf { it.isNotBlank() },
                    "Tamil Nadu"
                ).joinToString(", ")
            )}")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    Column(modifier = Modifier.padding(16.dp)) {

        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Location",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                modifier   = Modifier.weight(1f),
            )
            TextButton(onClick = { openMaps() }) {
                Icon(Icons.Default.Directions, null, tint = NestXBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Directions", color = NestXBlue, fontSize = 13.sp)
            }
        }

        // ── Address line ──────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.Top,
            modifier          = Modifier.padding(bottom = 12.dp),
        ) {
            Icon(
                Icons.Default.LocationOn, null,
                tint     = NestXBlue,
                modifier = Modifier.size(16.dp).padding(top = 2.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = buildString {
                    if (safeAddress.isNotBlank())      append("$safeAddress, ")
                    if (safeNeighborhood.isNotBlank()) append("$safeNeighborhood, ")
                    if (safeDistrict.isNotBlank())     append("$safeDistrict, ")
                    append("Tamil Nadu")
                },
                fontSize   = 13.sp,
                color      = TextSecondary,
                lineHeight = 18.sp,
            )
        }

        // ── Map (Google Maps native) or fallback card ─────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { openMaps() },
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceGray),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCoords) {
                    // ── Native Google Map ─────────────────────────────────────
                    PropertyMapView(
                        lat      = lat!!,
                        lng      = lng!!,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // No coords — show plain "View on Map" fallback
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.Map, null, tint = NestXBlue, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("View on Map", color = NestXBlue, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${safeNeighborhood.ifBlank { safeDistrict }}, Tamil Nadu",
                            fontSize = 12.sp, color = TextSecondary,
                        )
                    }
                }
                // Tap hint badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                ) {
                    Text(
                        "Tap to open in Maps",
                        fontSize = 11.sp,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// ── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun StatItem(emoji: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(BannerBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PropertyContactBar(
    hasCall:     Boolean,        // show Call button only when a phone number exists
    hasWhatsApp: Boolean,        // show WhatsApp button only when a WA number exists
    listingType: String,         // drives Visit/Book label
    onCall:      () -> Unit,
    onWhatsApp:  () -> Unit,
    onBookVisit: () -> Unit,
) {
    // Decide Visit button label by listing type
    val visitLabel = when (listingType) {
        "ground"       -> "Book Slot"
        "holiday_stay" -> "Book Stay"
        "contractor"   -> "Get Quote"
        else           -> "Visit"
    }

    // At least one contact exists — always show the bar; hide individual buttons if absent
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasCall) {
                Button(
                    onClick  = onCall,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                ) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", fontSize = 13.sp)
                }
            }
            if (hasWhatsApp) {
                Button(
                    onClick  = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                ) {
                    Text("💬 WhatsApp", fontSize = 13.sp)
                }
            }
            // Book/Visit button always present
            Button(
                onClick  = onBookVisit,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = NestXBlueDark),
            ) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(visitLabel, fontSize = 13.sp)
            }
        }
    }
}

// ── "I'm Interested" lead CTA ─────────────────────────────────────────────────

/**
 * Prominent lead-capture CTA. Tapping records the buyer's interest as a [PropertyLead]
 * so the owner/agent gets the enquiry (and, in the future paid phase, an auto WhatsApp).
 * Once sent, it collapses into a confirmation chip.
 */
@Composable
private fun PropertyInterestCta(
    hasExpressedInterest: Boolean,
    interestState: InterestState,
    onExpressInterest: () -> Unit,
) {
    val alreadySent = hasExpressedInterest || interestState is InterestState.Success
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (alreadySent) StatusApproved.copy(alpha = 0.10f) else BannerBlue,
    ) {
        if (alreadySent) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = StatusApproved, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        "Enquiry sent",
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary,
                    )
                    Text(
                        "The owner has your contact details and will reach out.",
                        fontSize = 12.sp, color = TextSecondary,
                    )
                }
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Interested in this property?",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Share your details with the owner so they can contact you.",
                    fontSize = 12.sp, color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = onExpressInterest,
                    enabled  = interestState !is InterestState.Loading,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                ) {
                    if (interestState is InterestState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("I'm Interested", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (interestState is InterestState.Error) {
                    Spacer(Modifier.height(6.dp))
                    Text(interestState.message, color = StatusRejected, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Builds the pre-filled WhatsApp enquiry text (Phase 0). Includes the listing's key
 * details + reference so the owner/agent immediately knows which property and buyer.
 * [buyerName] is appended when known (display-only; the backend authoritative source
 * is the JWT when a lead is created).
 */
private fun buildEnquiryMessage(property: Property, buyerName: String?): String {
    val loc = listOfNotNull(
        property.neighborhood?.takeIf { it.isNotBlank() },
        property.district?.takeIf { it.isNotBlank() },
    ).joinToString(", ")
    return buildString {
        append("Hi")
        if (property.agentName.isNotBlank()) append(" ${property.agentName}")
        append(", I'm interested in this property on NestX:\n\n")
        append("🏠 ${property.title.orEmpty()}\n")
        if (loc.isNotBlank()) append("📍 $loc\n")
        append("💰 ${property.priceDisplay}\n")
        if (property.referenceId.isNotBlank()) append("🔖 Ref: ${property.referenceId}\n")
        append("\nPlease share more details.")
        if (!buyerName.isNullOrBlank()) append("\n\n— $buyerName")
    }
}
