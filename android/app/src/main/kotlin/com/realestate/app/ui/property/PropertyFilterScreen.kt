package com.realestate.app.ui.property

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.data.models.Amenity
import com.realestate.app.data.models.PropertyFilterState
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.ui.components.PriceRangeSlider
import com.realestate.app.ui.components.RealEstateFilterChip
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PropertyFilterScreen(
    initialFilter: PropertyFilterState = PropertyFilterState(),
    onApply: (PropertyFilterState) -> Unit,
    onClose: () -> Unit,
    onViewAllAmenities: () -> Unit,
) {
    // ── Local filter state ────────────────────────────────────────────────────
    var listingType       by remember { mutableStateOf(initialFilter.listingType) }
    var selectedDistrict  by remember { mutableStateOf(initialFilter.district) }
    var selectedArea      by remember { mutableStateOf(initialFilter.area) }
    var minPrice          by remember { mutableFloatStateOf(initialFilter.minPrice) }
    var maxPrice          by remember { mutableFloatStateOf(initialFilter.maxPrice.coerceAtLeast(minPrice)) }
    var minArea           by remember { mutableStateOf(initialFilter.minArea?.toInt()?.toString() ?: "") }
    var maxArea           by remember { mutableStateOf(initialFilter.maxArea?.toInt()?.toString() ?: "") }
    var selectedBedrooms  by remember { mutableStateOf(initialFilter.bedrooms) }
    var selectedBathrooms by remember { mutableStateOf(initialFilter.bathrooms) }
    var selectedFurnishing by remember { mutableStateOf(initialFilter.furnishing) }
    var selectedType      by remember { mutableStateOf(initialFilter.propertyType) }
    var keyword           by remember { mutableStateOf(initialFilter.keyword) }
    val selectedAmenities = remember { mutableStateListOf<String>().also { it.addAll(initialFilter.amenities) } }

    // Radius Search
    var radiusKm          by remember { mutableStateOf(initialFilter.radiusKm) }

    // Category-specific sub-type filters
    var contractorType    by remember { mutableStateOf(initialFilter.contractorType) }
    var serviceType       by remember { mutableStateOf(initialFilter.serviceType) }
    // Construction vs Maintenance are BOTH listingType="contractor"; workCategory tells them apart.
    var workCategory      by remember { mutableStateOf(initialFilter.workCategory) }

    // Dropdown expanded states
    var districtExpanded  by remember { mutableStateOf(false) }
    var areaExpanded      by remember { mutableStateOf(false) }
    var contractorExpanded by remember { mutableStateOf(false) }
    var serviceExpanded    by remember { mutableStateOf(false) }

    val areaOptions = remember(selectedDistrict) {
        if (selectedDistrict.isBlank()) emptyList()
        else TamilNaduData.areasForDistrict(selectedDistrict)
    }

    LaunchedEffect(selectedDistrict) { selectedArea = "" }

    fun buildFilter(): PropertyFilterState {
        // Coimbatore coordinates as standard default center if doing radius search
        val centerLat = if (radiusKm != null) 11.0168 else null
        val centerLng = if (radiusKm != null) 76.9558 else null

        return PropertyFilterState(
            listingType    = listingType,
            district       = selectedDistrict,
            area           = selectedArea,
            minPrice       = minPrice,
            maxPrice       = maxPrice,
            minArea        = minArea.toFloatOrNull(),
            maxArea        = maxArea.toFloatOrNull(),
            bedrooms       = selectedBedrooms,
            bathrooms      = selectedBathrooms,
            furnishing     = selectedFurnishing,
            propertyType   = selectedType,
            amenities      = selectedAmenities.toList(),
            keyword        = keyword,
            radiusKm       = radiusKm,
            centerLat      = centerLat,
            centerLng      = centerLng,
            workCategory   = workCategory,
            contractorType = if (listingType == "contractor" && workCategory != "maintenance") contractorType else null,
            serviceType    = if (listingType == "contractor" && workCategory == "maintenance") serviceType else null,
        )
    }

    fun resetAll() {
        listingType = "rent"; selectedDistrict = ""; selectedArea = ""
        minPrice = 0f; maxPrice = 20_000_000f
        minArea = ""; maxArea = ""
        selectedBedrooms = null; selectedBathrooms = null
        selectedFurnishing = null; selectedType = null
        keyword = ""; selectedAmenities.clear()
        radiusKm = null
        contractorType = null
        serviceType = null
        workCategory = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Filters", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { resetAll() }) {
                        Text("Reset", color = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { onApply(buildFilter()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Show Results", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {

            // ── Keyword Search ────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by title, area…", color = TextSecondary) },
                leadingIcon  = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                singleLine   = true,
                shape        = RoundedCornerShape(12.dp),
                colors       = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PrimaryRed,
                    unfocusedBorderColor = BorderColor,
                ),
            )

            // ── Listing type tabs ─────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            FilterSectionTitle("Listing Type")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Construction & Maintenance are both listingType="contractor", told apart by
                // workCategory — so derive which chip is "selected" from both, and map a chip
                // tap back onto the correct (listingType, workCategory) pair.
                val selectedTypeKey = when {
                    listingType == "contractor" && workCategory == "maintenance" -> "maintenance"
                    listingType == "contractor"                                  -> "contractor"
                    else                                                         -> listingType
                }
                listOf(
                    "rent" to "Rent",
                    "sale" to "Buy",
                    "contractor" to "Construction",
                    "maintenance" to "Maintenance",
                    "holiday_stay" to "Stays",
                    "ground" to "Grounds"
                ).forEach { (type, label) ->
                    RealEstateFilterChip(
                        label    = label,
                        selected = selectedTypeKey == type,
                        onClick  = {
                            when (type) {
                                "contractor"  -> { listingType = "contractor"; workCategory = "construction" }
                                "maintenance" -> { listingType = "contractor"; workCategory = "maintenance" }
                                else          -> { listingType = type; workCategory = null }
                            }
                        }
                    )
                }
            }

            // ── Radius Bounding Box ───────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            FilterSectionTitle("Radius Distance")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Any", 10 to "10 km", 50 to "50 km", 100 to "100 km").forEach { (rad, label) ->
                    RealEstateFilterChip(
                        label    = label,
                        selected = radiusKm == rad,
                        onClick  = { radiusKm = rad }
                    )
                }
            }

            // ── District ──────────────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            FilterSectionTitle("District")
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded         = districtExpanded,
                onExpandedChange = { districtExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedDistrict.ifBlank { "All Districts" },
                    onValueChange = {},
                    readOnly   = true,
                    modifier   = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryRed,
                        unfocusedBorderColor = BorderColor,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = districtExpanded,
                    onDismissRequest = { districtExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("All Districts") },
                        onClick = { selectedDistrict = ""; districtExpanded = false },
                    )
                    TamilNaduData.districts.forEach { district ->
                        DropdownMenuItem(
                            text    = { Text(district) },
                            onClick = { selectedDistrict = district; districtExpanded = false },
                        )
                    }
                }
            }

            // ── Area / Locality ────────────────────────────────────────────────
            if (selectedDistrict.isNotBlank() && areaOptions.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FilterSectionTitle("Area / Locality in $selectedDistrict")
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded         = areaExpanded,
                    onExpandedChange = { areaExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedArea.ifBlank { "All Areas" },
                        onValueChange = {},
                        readOnly   = true,
                        modifier   = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryRed,
                            unfocusedBorderColor = BorderColor,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = areaExpanded,
                        onDismissRequest = { areaExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Areas in $selectedDistrict") },
                            onClick = { selectedArea = ""; areaExpanded = false },
                        )
                        areaOptions.forEach { area ->
                            DropdownMenuItem(
                                text    = { Text(area) },
                                onClick = { selectedArea = area; areaExpanded = false },
                            )
                        }
                    }
                }
            }

            // ── Contractor sub-types (Construction only) ──────────────────────
            if (listingType == "contractor" && workCategory != "maintenance") {
                Spacer(Modifier.height(20.dp))
                FilterSectionTitle("Contractor Work Type")
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded         = contractorExpanded,
                    onExpandedChange = { contractorExpanded = it },
                ) {
                    OutlinedTextField(
                        value = contractorType?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "All Types",
                        onValueChange = {},
                        readOnly   = true,
                        modifier   = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryRed),
                    )
                    ExposedDropdownMenu(
                        expanded = contractorExpanded,
                        onDismissRequest = { contractorExpanded = false },
                    ) {
                        DropdownMenuItem(text = { Text("All Types") }, onClick = { contractorType = null; contractorExpanded = false })
                        listOf(
                            "civil_contractor" to "Civil Contractor",
                            "builder" to "Builder",
                            "architect" to "Architect",
                            "structural_engineer" to "Structural Engineer",
                            "interior_designer" to "Interior Designer",
                            "landscaping" to "Landscaping"
                        ).forEach { (k, v) ->
                            DropdownMenuItem(
                                text    = { Text(v) },
                                onClick = { contractorType = k; contractorExpanded = false },
                            )
                        }
                    }
                }
            }

            // ── Maintenance sub-types (contractor + workCategory=maintenance) ──
            if (listingType == "contractor" && workCategory == "maintenance") {
                Spacer(Modifier.height(20.dp))
                FilterSectionTitle("Service Type")
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded         = serviceExpanded,
                    onExpandedChange = { serviceExpanded = it },
                ) {
                    OutlinedTextField(
                        value = serviceType?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "All Services",
                        onValueChange = {},
                        readOnly   = true,
                        modifier   = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryRed),
                    )
                    ExposedDropdownMenu(
                        expanded = serviceExpanded,
                        onDismissRequest = { serviceExpanded = false },
                    ) {
                        DropdownMenuItem(text = { Text("All Services") }, onClick = { serviceType = null; serviceExpanded = false })
                        listOf(
                            "electrician" to "Electrician",
                            "plumber" to "Plumber",
                            "carpenter" to "Carpenter",
                            "ac_service" to "AC Service",
                            "cleaning_service" to "Cleaning Service",
                            "pest_control" to "Pest Control",
                            "borewell" to "Borewell"
                        ).forEach { (k, v) ->
                            DropdownMenuItem(
                                text    = { Text(v) },
                                onClick = { serviceType = k; serviceExpanded = false },
                            )
                        }
                    }
                }
            }

            // ── Price Range ───────────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            FilterSectionTitle("Price Range (₹ INR)")
            Spacer(Modifier.height(8.dp))
            PriceRangeSlider(
                minPrice    = minPrice,
                maxPrice    = maxPrice,
                onRangeChange = { min, max -> minPrice = min; maxPrice = max },
            )

            // ── Area Sqft Range ───────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            FilterSectionTitle("Property Size (sqft)")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minArea,
                    onValueChange = { minArea = it },
                    label = { Text("Min Sqft") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = maxArea,
                    onValueChange = { maxArea = it },
                    label = { Text("Max Sqft") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                )
            }

            // ── Bedrooms & Bathrooms (Only for standard Property Buy/Rent) ───
            if (listingType == "rent" || listingType == "sale") {
                Spacer(Modifier.height(20.dp))
                FilterSectionTitle("Bedrooms")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null, 1, 2, 3, 4).forEach { bed ->
                        RealEstateFilterChip(
                            label    = if (bed == null) "Studio" else "$bed BHK",
                            selected = selectedBedrooms == bed,
                            onClick  = { selectedBedrooms = if (selectedBedrooms == bed) null else bed }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                FilterSectionTitle("Bathrooms")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 4).forEach { bath ->
                        RealEstateFilterChip(
                            label    = "$bath Baths",
                            selected = selectedBathrooms == bath,
                            onClick  = { selectedBathrooms = if (selectedBathrooms == bath) null else bath }
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text       = title,
        fontSize   = 14.sp,
        fontWeight = FontWeight.Bold,
        color      = TextPrimary,
    )
}
