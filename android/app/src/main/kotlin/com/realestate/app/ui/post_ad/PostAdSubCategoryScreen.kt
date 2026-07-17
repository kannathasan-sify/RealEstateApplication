package com.realestate.app.ui.post_ad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

// ── Sub-category definitions ─────────────────────────────────────────────────

val saleSubCategories = listOf(
    "Residential",
    "Commercial",
    "Agricultural Land",
    "Industrial Properties",
    "Farmhouse"
)

val rentSubCategories = listOf(
    "Residential",
    "Commercial",
    "Hotel / Resort",
    "Home Stay / PG"
)

val constructionSubCategories = listOf(
    "Civil Contractors",
    "Builders",
    "Architects",
    "Structural Engineers",
    "Interior Designers",
    "Plumbing",
    "Electrical",
    "Painting",
    "False Ceiling",
    "Tiles",
    "Roofing",
    "Landscaping"
)

val maintenanceSubCategories = listOf(
    "Electrician",
    "Plumber",
    "Carpenter",
    "AC Service",
    "CCTV",
    "Cleaning",
    "Painting",
    "Pest Control",
    "Borewell",
    "Water Tank Cleaning"
)

val holidayStaySubCategories = listOf(
    "Hotel",
    "Resort",
    "Villa",
    "Apartment",
    "Room",
)

val groundSubCategories = listOf(
    "Cricket Ground",
    "Football",
    "Other Open Ground",
    "Badminton",
    "Swimming Pool",
    "Other Closed Ground",
)

/**
 * Maps a Construction/Maintenance sub-category label (as shown in [constructionSubCategories] /
 * [maintenanceSubCategories]) to the backend `PropertyType` enum value
 * (`backend/app/schemas/property.py`).
 *
 * Shared between the Post-Ad flow (`PostAdViewModel.submitAd()`) and the property list's
 * sub-category filter chips (`PropertyListScreen`'s `SubCategoryTabBar`) so the two can never
 * drift out of sync again — that exact drift is what caused property_type validation errors
 * on submit, and would otherwise cause the list filter to silently match nothing.
 *
 * "Painting" appears with the identical label in both sub-category lists, so [isConstruction]
 * disambiguates it to `painting_contractor` vs `painting_service`.
 */
fun subCategoryToPropertyType(label: String, isConstruction: Boolean): String = when (label.trim()) {
    "Civil Contractors" -> "civil_contractor"
    "Builders" -> "builder"
    "Architects" -> "architect"
    "Structural Engineers" -> "structural_engineer"
    "Interior Designers" -> "interior_designer"
    "Plumbing" -> "plumbing_contractor"
    "Electrical" -> "electrical_contractor"
    "Painting" -> if (isConstruction) "painting_contractor" else "painting_service"
    "False Ceiling" -> "false_ceiling"
    "Tiles" -> "tiles_contractor"
    "Roofing" -> "roofing"
    "Landscaping" -> "landscaping"
    "Electrician" -> "electrician"
    "Plumber" -> "plumber"
    "Carpenter" -> "carpenter"
    "AC Service" -> "ac_service"
    "CCTV" -> "cctv_service"
    "Cleaning" -> "cleaning_service"
    "Pest Control" -> "pest_control"
    "Borewell" -> "borewell"
    "Water Tank Cleaning" -> "water_tank_cleaning"
    else -> label.trim().lowercase().replace(" ", "_").replace("/", "_")
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAdSubCategoryScreen(
    viewModel: PostAdViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val isSale = selectedCategory.contains("Sale", ignoreCase = true)
    val isRent = selectedCategory.contains("Rent", ignoreCase = true)
    val isConstruction = selectedCategory.contains("Construction", ignoreCase = true)
    val isMaintenance = selectedCategory.contains("Maintenance", ignoreCase = true)
    val isHolidayStay = selectedCategory.contains("Holiday", ignoreCase = true)
    val isGround = selectedCategory.equals("Ground", ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Place an Ad", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            val headingText = when {
                isConstruction -> "Select Construction Service:"
                isMaintenance -> "Select Maintenance Service:"
                isHolidayStay -> "Select type of stay:"
                isGround -> "Select type of ground:"
                isSale -> "Select type of Property for Sale:"
                isRent -> "Select type of Property for Rent:"
                else -> "Select sub-category:"
            }
            Text(
                text       = headingText,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(12.dp))

            // ── Breadcrumb ───────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint     = PrimaryRed,
                    modifier = Modifier.size(16.dp),
                )
                Icon(
                    Icons.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text       = selectedCategory.ifBlank { "Category" },
                    color      = PrimaryRed,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    Icons.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Text("Sub-category", color = TextSecondary, fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)

            val itemsToDisplay = when {
                isSale -> saleSubCategories
                isRent -> rentSubCategories
                isConstruction -> constructionSubCategories
                isMaintenance -> maintenanceSubCategories
                isHolidayStay -> holidayStaySubCategories
                isGround -> groundSubCategories
                else -> emptyList()
            }

            LazyColumn {
                items(itemsToDisplay) { subCat ->
                    SubCategoryRow(
                        label   = subCat,
                        onClick = {
                            viewModel.subCategory.value = subCat
                            onNext()
                        },
                    )
                    HorizontalDivider(color = BorderColor)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Shared row composable ─────────────────────────────────────────────────────

@Composable
private fun SubCategoryRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = label,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            color      = TextPrimary,
        )
        Icon(
            Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            tint     = TextSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}
