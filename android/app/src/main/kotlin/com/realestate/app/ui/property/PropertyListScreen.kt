package com.realestate.app.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.ui.components.PropertyCard
import com.realestate.app.ui.theme.*
import com.realestate.app.ui.post_ad.constructionSubCategories
import com.realestate.app.ui.post_ad.maintenanceSubCategories

/**
 * Shows a list of properties for a given [district] + [listingType].
 *
 * The Filter sheet is shown as a full-screen overlay so it shares the same
 * [PropertyViewModel] instance — no back-stack state communication needed.
 *
 * [onViewAllAmenities] is forwarded from AppNavGraph to navigate to AmenitiesScreen
 * from inside the filter overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    viewModel: PropertyViewModel,
    district: String,
    listingType: String?,
    /** Optional work-category pre-filter for contractor listings:
     *  "construction" (Build your Property) | "maintenance" (Maintain Your Property) | null */
    workCategory: String? = null,
    onPropertyClick: (String) -> Unit,
    onViewAllAmenities: () -> Unit = {},
    onPostAdClick: () -> Unit = {},
    onBack: () -> Unit,
) {
    var selectedSubCategoryTab by remember { mutableStateOf("") }

    LaunchedEffect(district, listingType, workCategory) {
        selectedSubCategoryTab = ""
        viewModel.loadProperties(
            district     = district,
            listingType  = listingType ?: "all",
            workCategory = workCategory,
        )
    }

    // ── Auto-refresh on resume ───────────────────────────────────────────────
    // Re-fetch the list every time this screen comes back into focus so newly
    // approved properties appear without requiring a manual pull-to-refresh.
    // Skip the first RESUME — the LaunchedEffect above already triggers the load.
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitialResume by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isInitialResume) {
                    isInitialResume = false
                } else {
                    viewModel.loadProperties(
                        district     = district,
                        listingType  = listingType ?: "all",
                        workCategory = workCategory,
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state         by viewModel.listState.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()

    var showFilter by remember { mutableStateOf(false) }
    var selectedCategoryTab by remember { mutableStateOf("Residential & Commercial") }

    // Active filter badge count (for toolbar badge)
    val activeFilterCount = remember(currentFilter) {
        listOfNotNull(
            currentFilter.district.takeIf { it.isNotBlank() },
            currentFilter.area.takeIf { it.isNotBlank() },
            if (currentFilter.minPrice > 0f || currentFilter.maxPrice < 20_000_000f) "" else null,
            currentFilter.bedrooms?.toString(),
            currentFilter.bathrooms?.toString(),
            currentFilter.furnishing,
            currentFilter.propertyType,
            if (currentFilter.amenities.isNotEmpty()) "" else null,
            currentFilter.keyword.takeIf { it.isNotBlank() },
        ).size
    }

    // Human-readable listing type label
    fun listingTypeLabel(type: String?): String = when (type) {
        "rent"         -> "Property for Rent"
        "sale"         -> "Property for Sale"
        "off_plan"     -> "Off-Plan"
        "new_project"  -> "New Projects"
        "commercial"   -> "Commercial"
        "land"         -> "Land / Plots"
        "pg"           -> "PG / Rooms"
        "holiday_stay" -> "Holiday Stay"
        "ground"       -> "Ground"
        "contractor"   -> when (workCategory) {
            "construction" -> "Build your Property"
            "maintenance"  -> "Maintain Your Property"
            else           -> "Find a Contractor"
        }
        else           -> ""
    }

    val typeLabel = listingTypeLabel(listingType)
    val title = buildString {
        if (typeLabel.isNotBlank()) append("$typeLabel · ")
        append(if (district == "All TN") "Tamil Nadu" else district)
    }

    // Empty-state label & emoji per category
    val emptyEmoji = when (listingType) {
        "holiday_stay" -> "🏖️"
        "ground"       -> "🏟️"
        "contractor"   -> when (workCategory) {
            "construction" -> "🏗️"
            "maintenance"  -> "🔧"
            else           -> "🔧"
        }
        else           -> "🏠"
    }
    val emptyMessage = when (listingType) {
        "holiday_stay" -> "No holiday stays found"
        "ground"       -> "No grounds found"
        "contractor"   -> when (workCategory) {
            "construction" -> "No construction services found"
            "maintenance"  -> "No maintenance services found"
            else           -> "No contractors found"
        }
        else           -> "No properties found"
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NestXBlue)
                                .clickable { onPostAdClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                contentDescription = "New Post",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0)
                                    Badge { Text(activeFilterCount.toString()) }
                            },
                        ) {
                            IconButton(onClick = { showFilter = true }) {
                                Icon(Icons.Default.FilterList, "Filter", tint = NestXBlue)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite),
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // ── Tab selection (Category tabs for sale/rent, otherwise District tabs) ──
                if (listingType == "sale" || listingType == "rent") {
                    CategoryTabBar(
                        selectedTab = selectedCategoryTab,
                        onSelectTab = { selectedCategoryTab = it }
                    )
                } else if (listingType == "contractor") {
                    val subCats = if (workCategory == "construction") {
                        listOf("All") + constructionSubCategories
                    } else {
                        listOf("All") + maintenanceSubCategories
                    }
                    SubCategoryTabBar(
                        selectedSubCat = selectedSubCategoryTab,
                        subCats = subCats,
                        onSelectSubCat = { selectedSubCategoryTab = it }
                    )
                } else {
                    DistrictTabBar(
                        selectedDistrict = district,
                        onSelectDistrict = { newDistrict ->
                            viewModel.selectDistrict(newDistrict)
                            viewModel.loadProperties(newDistrict, listingType ?: "all")
                        },
                    )
                }
                HorizontalDivider(color = BorderColor)

                when (val s = state) {
                    is PropertyUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NestXBlue)
                        }
                    }
                    is PropertyUiState.Success -> {
                        val filteredProperties = remember(s.properties, selectedCategoryTab, selectedSubCategoryTab, listingType) {
                            var list = s.properties
                            // Sale/Rent: filter by category tab
                            if (listingType == "sale" || listingType == "rent") {
                                list = list.filter { prop ->
                                    val type = prop.propertyType.orEmpty().uppercase()
                                    val subCat = prop.metadata?.get("sub_category")?.toString()?.uppercase().orEmpty()
                                    when (selectedCategoryTab) {
                                        "Residential & Commercial" -> {
                                            type != "AGRICULTURAL_LAND" && type != "LAND" &&
                                            type != "INDUSTRIAL_PROPERTY" && type != "INDUSTRIAL_LAND" &&
                                            type != "FARMHOUSE" &&
                                            !subCat.contains("AGRICULTURAL") && !subCat.contains("INDUSTRIAL") &&
                                            !subCat.contains("FARMHOUSE")
                                        }
                                        "Agriculture Land" -> {
                                            type == "AGRICULTURAL_LAND" || type == "LAND" || subCat.contains("AGRICULTURAL")
                                        }
                                        "Industrial Properties" -> {
                                            type == "INDUSTRIAL_PROPERTY" || type == "INDUSTRIAL_LAND" || subCat.contains("INDUSTRIAL")
                                        }
                                        "Farmhouse" -> {
                                            type == "FARMHOUSE" || subCat.contains("FARMHOUSE")
                                        }
                                        else -> true
                                    }
                                }
                            }
                            // Contractor: filter by sub-category chip
                            if (listingType == "contractor" && selectedSubCategoryTab.isNotBlank() && selectedSubCategoryTab != "All") {
                                list = list.filter { prop ->
                                    val metaSubCat = prop.metadata?.get("sub_category")?.toString() ?: ""
                                    metaSubCat.equals(selectedSubCategoryTab, ignoreCase = true)
                                }
                            }
                            list
                        }

                        if (filteredProperties.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emptyEmoji, fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(emptyMessage, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Text("Try adjusting your filters", fontSize = 13.sp, color = TextSecondary)
                                    Spacer(Modifier.height(12.dp))
                                    if (currentFilter.isActive) {
                                        OutlinedButton(onClick = { viewModel.resetFilter() }) {
                                            Text("Clear Filters", color = NestXBlue)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val countLabel = when (listingType) {
                                        "holiday_stay" -> "${filteredProperties.size} Stays"
                                        "ground"       -> "${filteredProperties.size} Grounds"
                                        "contractor"   -> "${filteredProperties.size} Contractors"
                                        else           -> "${filteredProperties.size} Properties"
                                    }
                                    Text(
                                        countLabel,
                                        fontSize = 13.sp, color = TextSecondary,
                                    )
                                    if (currentFilter.area.isNotBlank()) {
                                        SuggestionChip(
                                            onClick = {},
                                            label   = { Text(currentFilter.area, fontSize = 11.sp) },
                                        )
                                    }
                                }
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(filteredProperties) { prop ->
                                        PropertyCard(
                                            property = prop,
                                            compact  = false,
                                            onClick  = { onPropertyClick(prop.id) },
                                        )
                                    }
                                    item { Spacer(Modifier.height(80.dp)) }
                                }
                            }
                        }
                    }
                    is PropertyUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                  Text(s.message, color = MaterialTheme.colorScheme.error)
                                  Spacer(Modifier.height(8.dp))
                                  Button(onClick = { viewModel.loadProperties(district, listingType ?: "all") }) {
                                      Text("Retry")
                                  }
                            }
                        }
                    }
                }
            } // end Column (district tab bar + content)
        }

        // ── Filter overlay ────────────────────────────────────────────────
        if (showFilter) {
            PropertyFilterScreen(
                initialFilter      = currentFilter,
                onApply            = { filter ->
                    viewModel.applyFilter(filter)
                    showFilter = false
                },
                onClose            = { showFilter = false },
                onViewAllAmenities = { onViewAllAmenities() },
            )
        }
    }
}

@Composable
fun CategoryTabBar(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
) {
    val tabs = listOf(
        "Residential & Commercial",
        "Agriculture Land",
        "Industrial Properties",
        "Farmhouse",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWhite)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Surface(
                shape    = RoundedCornerShape(20.dp),
                color    = if (isSelected) NestXBlue else SurfaceGray,
                modifier = Modifier.clickable { onSelectTab(tab) },
            ) {
                Text(
                    text = tab,
                    fontSize   = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else TextPrimary,
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// ── District Tab Bar ──────────────────────────────────────────────────────────

/**
 * Horizontal scrollable row of district chips — shown as a persistent tab bar
 * just below the top app bar on PropertyListScreen.
 *
 * Shows "All TN" + the 10 most popular districts.
 * The currently active [selectedDistrict] chip is highlighted in NestXBlue.
 * Tapping a different chip calls [onSelectDistrict] which reloads the list.
 */
@Composable
fun DistrictTabBar(
    selectedDistrict: String,
    onSelectDistrict: (String) -> Unit,
) {
    val quickDistricts = listOf(
        "All TN", "Chennai", "Coimbatore", "Madurai",
        "Tiruchirappalli", "Salem", "Tirunelveli",
        "Tiruppur", "Erode", "Vellore", "Thanjavur",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWhite)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        quickDistricts.forEach { district ->
            val isSelected = district == selectedDistrict ||
                (district == "All TN" && selectedDistrict == "All TN")
            Surface(
                shape    = RoundedCornerShape(20.dp),
                color    = if (isSelected) NestXBlue else SurfaceGray,
                modifier = Modifier.clickable { onSelectDistrict(district) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (district != "All TN") {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint     = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    Text(
                        district,
                        fontSize   = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isSelected) Color.White else TextPrimary,
                    )
                }
            }
        }
    }
}

// ── Sub-category chip tab bar for Construction / Maintenance ─────────────────

@Composable
fun SubCategoryTabBar(
    selectedSubCat: String,
    subCats: List<String>,
    onSelectSubCat: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWhite)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        subCats.forEach { cat ->
            val isAll      = cat == "All"
            val isSelected = if (isAll) selectedSubCat.isBlank() || selectedSubCat == "All"
                             else cat == selectedSubCat
            Surface(
                shape    = RoundedCornerShape(20.dp),
                color    = if (isSelected) NestXBlue else SurfaceGray,
                modifier = Modifier.clickable {
                    onSelectSubCat(if (isAll) "" else cat)
                },
            ) {
                Text(
                    text       = cat,
                    fontSize   = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else TextPrimary,
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}
