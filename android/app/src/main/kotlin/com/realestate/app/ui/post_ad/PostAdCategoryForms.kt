package com.realestate.app.ui.post_ad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.ui.components.RealEstateFilterChip
import com.realestate.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun FormSectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextPrimary,
    )
}

@Composable
internal fun FormDivider() {
    HorizontalDivider(
        color     = BorderColor,
        modifier  = Modifier.padding(vertical = 4.dp),
    )
}

/** Two-option card row (e.g. Yes / No, Furnished / Unfurnished). */
@Composable
internal fun TwoOptionRow(
    options:  List<Pair<String, String>>,   // label to key
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier              = Modifier.fillMaxWidth(),
    ) {
        options.forEach { (label, key) ->
            val isSelected = selected == key
            OutlinedButton(
                onClick  = { onSelect(key) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.5.dp, if (isSelected) NestXBlue else BorderColor),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) NestXBlue.copy(alpha = 0.08f) else Color.Transparent,
                    contentColor   = if (isSelected) NestXBlue else TextSecondary,
                ),
            ) {
                Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Multi-select chip list for facilities / features / rules. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MultiSelectChipGrid(
    items:     List<Pair<String, String>>,   // displayName to key
    selected:  Set<String>,
    onToggle:  (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (label, key) ->
            RealEstateFilterChip(
                label    = label,
                selected = key in selected,
                onClick  = { onToggle(key) },
            )
        }
    }
}

/** Time selector row with a label and a compact editable time field. */
@Composable
internal fun TimeInputRow(
    label:    String,
    value:    String,
    onChange: (String) -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value         = value,
            onValueChange = onChange,
            placeholder   = { Text("HH:MM") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine    = true,
            shape         = RoundedCornerShape(8.dp),
            modifier      = Modifier.width(110.dp),
        )
    }
}

/** Stepper control (+/- buttons with a number display). */
@Composable
internal fun StepperField(
    label:    String,
    value:    Int,
    min:      Int = 0,
    max:      Int = 99,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            IconButton(
                onClick  = { if (value > min) onValueChange(value - 1) },
                modifier = Modifier
                    .size(36.dp)
                    .background(if (value > min) NestXBlue.copy(0.08f) else SurfaceGray, CircleShape),
            ) {
                Icon(
                    Icons.Filled.Remove, null,
                    tint     = if (value > min) NestXBlue else TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                "$value",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                modifier   = Modifier.widthIn(min = 40.dp).wrapContentWidth(Alignment.CenterHorizontally),
            )
            IconButton(
                onClick  = { if (value < max) onValueChange(value + 1) },
                modifier = Modifier
                    .size(36.dp)
                    .background(if (value < max) NestXBlue.copy(0.08f) else SurfaceGray, CircleShape),
            ) {
                Icon(
                    Icons.Filled.Add, null,
                    tint     = if (value < max) NestXBlue else TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Large selection card used for Work Category / Stay Type etc. */
@Composable
internal fun SelectionCard(
    icon:       ImageVector,
    label:      String,
    subLabel:   String = "",
    isSelected: Boolean,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier,
) {
    Card(
        modifier  = modifier
            .clickable(onClick = onClick)
            .border(
                width  = 1.5.dp,
                color  = if (isSelected) NestXBlue else BorderColor,
                shape  = RoundedCornerShape(12.dp),
            ),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected) NestXBlue.copy(alpha = 0.07f) else Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .background(
                        color  = if (isSelected) NestXBlue.copy(0.15f) else OnboardingBlob,
                        shape  = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint     = if (isSelected) NestXBlue else TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (isSelected) NestXBlue else TextPrimary,
            )
            if (subLabel.isNotEmpty()) {
                Text(subLabel, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

/** Small info chip with blue border. */
@Composable
internal fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BannerBlue,
    ) {
        Text(
            text     = text,
            color    = NestXBlue,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Location + Price block (used by all three specialty forms)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharedLocationPriceBlock(
    viewModel:       PostAdViewModel,
    onPickOnMap:     () -> Unit,
    priceLabel:      String = "Price (₹)",
    pricePlaceholder:String = "e.g. 500",
    showFrequency:   Boolean = true,
    frequencyOptions: List<Pair<String, String>> = listOf(
        "monthly" to "Monthly", "yearly" to "Yearly", "weekly" to "Weekly"
    ),
) {
    val district       by viewModel.district.collectAsState()
    val address        by viewModel.address.collectAsState()
    val price          by viewModel.price.collectAsState()
    val priceFrequency by viewModel.priceFrequency.collectAsState()
    val pickedLat      by viewModel.pickedLatitude.collectAsState()
    val pickedLng      by viewModel.pickedLongitude.collectAsState()

    var districtExpanded by remember { mutableStateOf(false) }
    val hasLocation = pickedLat != null && pickedLng != null

    // District
    FormSectionLabel("District (Tamil Nadu) *")
    ExposedDropdownMenuBox(
        expanded         = districtExpanded,
        onExpandedChange = { districtExpanded = !districtExpanded },
    ) {
        OutlinedTextField(
            value         = district.ifBlank { "Select district" },
            onValueChange = {},
            readOnly      = true,
            label         = { Text("District") },
            trailingIcon  = { Icon(Icons.Filled.ArrowDropDown, null, tint = TextSecondary) },
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = NestXBlue,
                unfocusedBorderColor = BorderColor,
            ),
        )
        ExposedDropdownMenu(
            expanded         = districtExpanded,
            onDismissRequest = { districtExpanded = false },
        ) {
            TamilNaduData.districts.forEach { d ->
                DropdownMenuItem(
                    text    = { Text(d) },
                    onClick = { viewModel.district.value = d; districtExpanded = false },
                )
            }
        }
    }

    // Location on Map
    FormSectionLabel("Location on Map")
    if (hasLocation) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, NestXBlue), RoundedCornerShape(10.dp))
                .clickable(onClick = onPickOnMap)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = NestXBlue, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    address.ifBlank { "Location pinned" },
                    fontSize = 13.sp, color = TextPrimary,
                )
                Text(
                    "%.5f, %.5f".format(pickedLat!!, pickedLng!!),
                    fontSize = 11.sp, color = TextSecondary,
                )
            }
            Text("Change", fontSize = 12.sp, color = NestXBlue)
        }
    } else {
        OutlinedButton(
            onClick  = onPickOnMap,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(10.dp),
            border   = BorderStroke(1.5.dp, NestXBlue),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = NestXBlue),
            enabled  = district.isNotBlank(),
        ) {
            Icon(Icons.Filled.LocationOn, null)
            Spacer(Modifier.width(8.dp))
            Text(if (district.isBlank()) "Select district first" else "Pin Location on Map", fontSize = 14.sp)
        }
    }

    // Address
    OutlinedTextField(
        value         = address,
        onValueChange = { viewModel.address.value = it },
        label         = { Text("Street / Landmark") },
        placeholder   = { Text("Auto-filled when you pin on map") },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        singleLine    = true,
    )

    // Price
    FormDivider()
    FormSectionLabel(priceLabel)
    OutlinedTextField(
        value           = price,
        onValueChange   = { viewModel.price.value = it },
        label           = { Text("Amount in ₹") },
        placeholder     = { Text(pricePlaceholder) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(10.dp),
        singleLine      = true,
        leadingIcon     = { Text("₹", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
    )

    if (showFrequency) {
        FormSectionLabel("Per")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            frequencyOptions.forEach { (key, label) ->
                RealEstateFilterChip(
                    label    = label,
                    selected = priceFrequency == key,
                    onClick  = { viewModel.priceFrequency.value = key },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. GROUND DETAILS FORM
// ─────────────────────────────────────────────────────────────────────────────

private val GROUND_TYPES = listOf(
    Icons.Filled.SportsCricket    to "Cricket",
    Icons.Filled.SportsSoccer     to "Football",
    Icons.Filled.SportsBasketball to "Basketball",
    Icons.Filled.SportsTennis     to "Badminton / Tennis",
    Icons.Filled.Pool             to "Swimming Pool",
    Icons.Filled.SportsHandball to "Multi-Sport",
    Icons.Filled.FitnessCenter    to "Gym / Indoor",
    Icons.Filled.GridOn           to "Other",
)

private val SURFACE_TYPES = listOf(
    "Natural Grass"    to "natural_grass",
    "Artificial Turf"  to "artificial_turf",
    "Hard Court"       to "hard_court",
    "Clay"             to "clay",
    "Concrete"         to "concrete",
    "Synthetic"        to "synthetic",
    "Sand"             to "sand",
)

private val GROUND_FACILITIES = listOf(
    "Changing Room"    to "changing_room",
    "Washrooms"        to "washrooms",
    "Parking"          to "parking",
    "Seating Area"     to "seating",
    "Drinking Water"   to "drinking_water",
    "Canteen / Café"   to "canteen",
    "Floodlights"      to "floodlights",
    "First Aid"        to "first_aid",
    "Equipment Rental" to "equipment_rental",
    "Coach Available"  to "coach",
)

private val CANCELLATION_OPTIONS = listOf(
    "Flexible"  to "flexible",
    "Moderate"  to "moderate",
    "Strict"    to "strict",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroundDetailsForm(
    viewModel:   PostAdViewModel,
    onPickOnMap: () -> Unit,
) {
    val groundType        by viewModel.groundType.collectAsState()
    val groundLength      by viewModel.groundLength.collectAsState()
    val groundWidth       by viewModel.groundWidth.collectAsState()
    val surfaceType       by viewModel.surfaceType.collectAsState()
    val hasFloodlights    by viewModel.hasFloodlights.collectAsState()
    val availableFrom     by viewModel.availableFrom.collectAsState()
    val availableTo       by viewModel.availableTo.collectAsState()
    val groundCapacity    by viewModel.groundCapacity.collectAsState()
    val advanceBookingReq by viewModel.advanceBookingReq.collectAsState()
    val groundFacilities  by viewModel.groundFacilities.collectAsState()
    val cancellation      by viewModel.cancellationPolicy.collectAsState()
    val whatsappNumber    by viewModel.whatsappNumber.collectAsState()
    val description       by viewModel.description.collectAsState()

    // ── Info banner ──────────────────────────────────────────────────────────
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BannerBlue,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.SportsSoccer, null, tint = NestXBlue, modifier = Modifier.size(20.dp))
            Text(
                "List your sports ground or venue for hourly bookings",
                fontSize = 13.sp, color = NestXBlue, fontWeight = FontWeight.Medium,
            )
        }
    }

    FormDivider()

    // ── Ground Type ──────────────────────────────────────────────────────────
    FormSectionLabel("Ground / Sport Type")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        modifier              = Modifier.fillMaxWidth(),
    ) {
        GROUND_TYPES.forEach { (icon, label) ->
            val key = label.lowercase().replace(" / ", "_").replace(" ", "_")
            SelectionCard(
                icon       = icon,
                label      = label,
                isSelected = groundType == key,
                onClick    = { viewModel.groundType.value = if (groundType == key) "" else key },
                modifier   = Modifier.width(100.dp),
            )
        }
    }

    FormDivider()

    // ── Dimensions ───────────────────────────────────────────────────────────
    FormSectionLabel("Ground Dimensions (metres)")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value           = groundLength,
            onValueChange   = { viewModel.groundLength.value = it },
            label           = { Text("Length") },
            placeholder     = { Text("e.g. 68") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier        = Modifier.weight(1f),
            shape           = RoundedCornerShape(10.dp),
            singleLine      = true,
            trailingIcon    = { Text("m", fontSize = 12.sp, color = TextSecondary) },
        )
        OutlinedTextField(
            value           = groundWidth,
            onValueChange   = { viewModel.groundWidth.value = it },
            label           = { Text("Width") },
            placeholder     = { Text("e.g. 105") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier        = Modifier.weight(1f),
            shape           = RoundedCornerShape(10.dp),
            singleLine      = true,
            trailingIcon    = { Text("m", fontSize = 12.sp, color = TextSecondary) },
        )
    }

    // ── Surface Type ─────────────────────────────────────────────────────────
    FormSectionLabel("Surface Type")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SURFACE_TYPES.forEach { (label, key) ->
            RealEstateFilterChip(
                label    = label,
                selected = surfaceType == key,
                onClick  = { viewModel.surfaceType.value = if (surfaceType == key) "" else key },
            )
        }
    }

    FormDivider()

    // ── Availability ─────────────────────────────────────────────────────────
    FormSectionLabel("Availability Hours")
    TimeInputRow("Opens at", availableFrom) { viewModel.availableFrom.value = it }
    Spacer(Modifier.height(8.dp))
    TimeInputRow("Closes at", availableTo) { viewModel.availableTo.value = it }

    Spacer(Modifier.height(4.dp))

    // ── Capacity ─────────────────────────────────────────────────────────────
    FormSectionLabel("Maximum Capacity (players / persons)")
    OutlinedTextField(
        value           = groundCapacity,
        onValueChange   = { viewModel.groundCapacity.value = it },
        label           = { Text("Max capacity") },
        placeholder     = { Text("e.g. 22 for football") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(10.dp),
        singleLine      = true,
        leadingIcon     = { Icon(Icons.Filled.People, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
    )

    FormDivider()

    // ── Facilities ───────────────────────────────────────────────────────────
    FormSectionLabel("Facilities Available")
    MultiSelectChipGrid(
        items    = GROUND_FACILITIES,
        selected = groundFacilities,
        onToggle = { key ->
            val set = viewModel.groundFacilities.value.toMutableSet()
            if (key in set) set.remove(key) else set.add(key)
            viewModel.groundFacilities.value = set
        },
    )

    FormDivider()

    // ── Floodlights toggle ───────────────────────────────────────────────────
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Floodlights Available", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Enable night-time bookings", fontSize = 12.sp, color = TextSecondary)
        }
        Switch(
            checked         = hasFloodlights,
            onCheckedChange = { viewModel.hasFloodlights.value = it },
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = NestXBlue,
            ),
        )
    }

    // ── Advance Booking ──────────────────────────────────────────────────────
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Advance Booking Required", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Customers must pre-book before visiting", fontSize = 12.sp, color = TextSecondary)
        }
        Switch(
            checked         = advanceBookingReq,
            onCheckedChange = { viewModel.advanceBookingReq.value = it },
            colors          = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NestXBlue,
            ),
        )
    }

    FormDivider()

    // ── Cancellation Policy ──────────────────────────────────────────────────
    FormSectionLabel("Cancellation Policy")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CANCELLATION_OPTIONS.forEach { (label, key) ->
            RealEstateFilterChip(
                label    = label,
                selected = cancellation == key,
                onClick  = { viewModel.cancellationPolicy.value = key },
            )
        }
    }
    Text(
        when (cancellation) {
            "flexible" -> "✓ Free cancellation up to 24 hours before booking"
            "moderate" -> "✓ Free cancellation up to 5 days before; 50% refund after"
            "strict"   -> "✓ No refund within 7 days of booking"
            else -> ""
        },
        fontSize = 12.sp, color = TextSecondary,
        modifier = Modifier.padding(top = 2.dp),
    )

    FormDivider()

    // ── Location + Price ─────────────────────────────────────────────────────
    SharedLocationPriceBlock(
        viewModel        = viewModel,
        onPickOnMap      = onPickOnMap,
        priceLabel       = "Booking Rate (₹ per hour)",
        pricePlaceholder = "e.g. 500 (per hour)",
        showFrequency    = false,
    )

    // ── WhatsApp ─────────────────────────────────────────────────────────────
    FormDivider()
    FormSectionLabel("Contact WhatsApp")
    OutlinedTextField(
        value           = whatsappNumber,
        onValueChange   = { viewModel.whatsappNumber.value = it },
        label           = { Text("WhatsApp number") },
        placeholder     = { Text("+91 98765 43210") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(10.dp),
        singleLine      = true,
        leadingIcon     = { Text("📱", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF25D366),
            unfocusedBorderColor = BorderColor,
        ),
    )

    // ── Description ──────────────────────────────────────────────────────────
    FormDivider()
    FormSectionLabel("Additional Details")
    OutlinedTextField(
        value         = description,
        onValueChange = { viewModel.description.value = it },
        label         = { Text("Describe your venue, rules, special features…") },
        modifier      = Modifier.fillMaxWidth().height(110.dp),
        shape         = RoundedCornerShape(10.dp),
        maxLines      = 6,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. CONTRACTOR DETAILS FORM
// ─────────────────────────────────────────────────────────────────────────────

private val CONSTRUCTION_WORK_TYPES = listOf(
    Icons.Filled.Apartment      to ("Building Construction" to "building"),
    Icons.Filled.Home           to ("Villa / House"         to "villa_house"),
    Icons.Filled.Inventory2     to ("Warehouse / Godown"   to "warehouse"),
    Icons.Filled.DesignServices to ("Interior Fitout"      to "interior_fitout"),
    Icons.Filled.Construction   to ("Renovation / Remodel" to "renovation"),
)

private val MAINTENANCE_WORK_TYPES = listOf(
    Icons.Filled.Engineering    to ("Civil Work"          to "civil_work"),
    Icons.Filled.FormatPaint    to ("Painting"            to "painting"),
    Icons.Filled.AcUnit         to ("AC / HVAC"           to "air_conditioning"),
    Icons.Filled.Plumbing       to ("Plumbing"            to "plumbing"),
    Icons.Filled.Power          to ("Electrical"          to "electrical"),
    Icons.Filled.Build          to ("Household Equipment" to "household_equipment"),
    Icons.Filled.AutoFixHigh    to ("Deep Cleaning"       to "cleaning"),
)

private val PRICING_MODELS = listOf(
    "Per Sqft"         to "per_sqft",
    "Per Day"          to "per_day",
    "Lump Sum"         to "lump_sum",
    "Quotation Based"  to "quotation",
)

private val TEAM_SIZES = listOf(
    "Solo (1)"        to "solo",
    "Small (2–5)"     to "small",
    "Medium (6–15)"   to "medium",
    "Large (15+)"     to "large",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContractorDetailsForm(
    viewModel:   PostAdViewModel,
    onPickOnMap: () -> Unit,
    isConstruction: Boolean,
) {
    val workCategory        by viewModel.workCategory.collectAsState()
    val contractorWorkTypes by viewModel.contractorWorkTypes.collectAsState()
    val yearsExperience     by viewModel.yearsExperience.collectAsState()
    val serviceDistricts    by viewModel.serviceDistricts.collectAsState()
    val pricingModel        by viewModel.pricingModel.collectAsState()
    val licenseNumber       by viewModel.licenseNumber.collectAsState()
    val teamSize            by viewModel.teamSize.collectAsState()
    val projectTimeline     by viewModel.projectTimeline.collectAsState()
    val warrantyOffered     by viewModel.warrantyOffered.collectAsState()
    val warrantyDuration    by viewModel.warrantyDuration.collectAsState()
    val whatsappNumber      by viewModel.whatsappNumber.collectAsState()
    val description         by viewModel.description.collectAsState()
    val companyProfile      by viewModel.companyProfile.collectAsState()

    // Sync workCategory automatically based on isConstruction parameter
    LaunchedEffect(isConstruction) {
        viewModel.workCategory.value = if (isConstruction) "construction" else "maintenance"
    }

    // ── Info banner ──────────────────────────────────────────────────────────
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Construction, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
            Text(
                text = if (isConstruction) "Post your Construction Contractor profile" else "Post your Maintenance Provider profile",
                fontSize = 13.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium,
            )
        }
    }

    FormDivider()

    // ── Company Profile ───────────────────────────────────────────────────────
    FormSectionLabel("Company Profile *")
    OutlinedTextField(
        value         = companyProfile,
        onValueChange = { viewModel.companyProfile.value = it },
        label         = { Text("About your company / services") },
        placeholder   = { Text("e.g. Quality building construction with 10+ years experience") },
        modifier      = Modifier.fillMaxWidth().height(100.dp),
        shape         = RoundedCornerShape(10.dp),
        maxLines      = 5,
    )

    FormDivider()

    // ── Experience ───────────────────────────────────────────────────────────
    FormSectionLabel("Years of Experience")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("1-2", "3-5", "5-10", "10-15", "15+").forEach { exp ->
            RealEstateFilterChip(
                label    = "$exp yrs",
                selected = yearsExperience == exp,
                onClick  = { viewModel.yearsExperience.value = exp },
            )
        }
    }

    // ── Team Size ────────────────────────────────────────────────────────────
    FormSectionLabel("Team Size")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TEAM_SIZES.forEach { (label, key) ->
            RealEstateFilterChip(
                label    = label,
                selected = teamSize == key,
                onClick  = { viewModel.teamSize.value = key },
            )
        }
    }

    FormDivider()

    // ── Service Areas ────────────────────────────────────────────────────────
    FormSectionLabel("Service Areas (Districts)")
    Text(
        "Select all Tamil Nadu districts you serve",
        fontSize = 12.sp, color = TextSecondary,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxWidth(),
    ) {
        TamilNaduData.districts.forEach { district ->
            RealEstateFilterChip(
                label    = district,
                selected = district in serviceDistricts,
                onClick  = {
                    val set = viewModel.serviceDistricts.value.toMutableSet()
                    if (district in set) set.remove(district) else set.add(district)
                    viewModel.serviceDistricts.value = set
                },
            )
        }
    }

    FormDivider()

    // ── Pricing Model ────────────────────────────────────────────────────────
    FormSectionLabel("Pricing Model")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PRICING_MODELS.forEach { (label, key) ->
            RealEstateFilterChip(
                label    = label,
                selected = pricingModel == key,
                onClick  = { viewModel.pricingModel.value = key },
            )
        }
    }

    // ── License Number ───────────────────────────────────────────────────────
    FormSectionLabel("License / Registration Number")
    OutlinedTextField(
        value         = licenseNumber,
        onValueChange = { viewModel.licenseNumber.value = it },
        label         = { Text("License or registration No. (optional)") },
        placeholder   = { Text("e.g. TN-CON-2023-12345") },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        singleLine    = true,
        leadingIcon   = { Icon(Icons.Filled.Badge, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
    )

    // ── Project Timeline ─────────────────────────────────────────────────────
    FormSectionLabel("Typical Project Timeline")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("1-3 days", "1-2 weeks", "2-4 weeks", "1-3 months", "3+ months").forEach { t ->
            RealEstateFilterChip(
                label    = t,
                selected = projectTimeline == t,
                onClick  = { viewModel.projectTimeline.value = t },
            )
        }
    }

    FormDivider()

    // ── Warranty ─────────────────────────────────────────────────────────────
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Warranty / Guarantee Offered", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Do you offer a workmanship warranty?", fontSize = 12.sp, color = TextSecondary)
        }
        Switch(
            checked         = warrantyOffered,
            onCheckedChange = { viewModel.warrantyOffered.value = it },
            colors          = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NestXBlue,
            ),
        )
    }

    if (warrantyOffered) {
        OutlinedTextField(
            value         = warrantyDuration,
            onValueChange = { viewModel.warrantyDuration.value = it },
            label         = { Text("Warranty duration") },
            placeholder   = { Text("e.g. 1 year, 6 months") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            singleLine    = true,
            leadingIcon   = { Icon(Icons.Filled.Shield, null, tint = NestXBlue, modifier = Modifier.size(18.dp)) },
        )
    }

    FormDivider()

    // ── Location + Price ─────────────────────────────────────────────────────
    SharedLocationPriceBlock(
        viewModel        = viewModel,
        onPickOnMap      = onPickOnMap,
        priceLabel       = when (pricingModel) {
            "per_sqft" -> "Starting Rate (₹ per sqft)"
            "per_day"  -> "Day Rate (₹ per day)"
            "lump_sum" -> "Typical Project Cost (₹)"
            else       -> "Estimated Starting Price (₹)"
        },
        pricePlaceholder = when (pricingModel) {
            "per_sqft" -> "e.g. 250 per sqft"
            "per_day"  -> "e.g. 3000 per day"
            "lump_sum" -> "e.g. 150000"
            else       -> "e.g. 50000 onwards"
        },
        showFrequency = false,
    )

    // ── WhatsApp ─────────────────────────────────────────────────────────────
    FormDivider()
    FormSectionLabel("Contact WhatsApp *")
    OutlinedTextField(
        value           = whatsappNumber,
        onValueChange   = { viewModel.whatsappNumber.value = it },
        label           = { Text("WhatsApp number") },
        placeholder     = { Text("e.g. 9876543210") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(10.dp),
        singleLine      = true,
        leadingIcon     = { Text("📱", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF25D366),
            unfocusedBorderColor = BorderColor,
        ),
    )

    // ── Portfolio / Description ───────────────────────────────────────────────
    FormDivider()
    FormSectionLabel("Work Portfolio & Description")
    OutlinedTextField(
        value         = description,
        onValueChange = { viewModel.description.value = it },
        label         = { Text("Describe your past projects, skills, specialities…") },
        modifier      = Modifier.fillMaxWidth().height(130.dp),
        shape         = RoundedCornerShape(10.dp),
        maxLines      = 7,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. HOLIDAY STAY DETAILS FORM
// ─────────────────────────────────────────────────────────────────────────────

private val STAY_TYPES = listOf(
    Icons.Filled.Home        to ("Entire Home"    to "entire_home"),
    Icons.Filled.MeetingRoom to ("Private Room"   to "private_room"),
    Icons.Filled.PeopleAlt   to ("Shared Room"    to "shared_room"),
    Icons.Filled.KingBed     to ("Villa"          to "villa"),
    Icons.Filled.Hotel       to ("Resort / Hotel" to "resort"),
)

private val STAY_FACILITIES = listOf(
    "WiFi"               to "wifi",
    "Air Conditioning"   to "ac",
    "Kitchen"            to "kitchen",
    "Parking"            to "parking",
    "Swimming Pool"      to "pool",
    "Gym"                to "gym",
    "TV"                 to "tv",
    "Washing Machine"    to "washing_machine",
    "Breakfast Included" to "breakfast",
    "Hot Water"          to "hot_water",
    "Power Backup"       to "power_backup",
    "CCTV"               to "cctv",
)

private val HOUSE_RULES = listOf(
    "No Smoking"         to "no_smoking",
    "No Pets"            to "no_pets",
    "No Parties"         to "no_parties",
    "No Visitors"        to "no_visitors",
    "Quiet Hours (10pm)" to "quiet_hours",
    "No Outside Food"    to "no_outside_food",
    "ID Proof Required"  to "id_required",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HolidayStayDetailsForm(
    viewModel:   PostAdViewModel,
    onPickOnMap: () -> Unit,
) {
    val stayType         by viewModel.stayType.collectAsState()
    val maxGuests        by viewModel.maxGuests.collectAsState()
    val bedrooms         by viewModel.bedrooms.collectAsState()
    val bathrooms        by viewModel.bathrooms.collectAsState()
    val checkInTime      by viewModel.checkInTime.collectAsState()
    val checkOutTime     by viewModel.checkOutTime.collectAsState()
    val minStayNights    by viewModel.minStayNights.collectAsState()
    val stayFacilities   by viewModel.stayFacilities.collectAsState()
    val houseRules       by viewModel.houseRules.collectAsState()
    val stayCancellation by viewModel.stayCancellation.collectAsState()
    val whatsappNumber   by viewModel.whatsappNumber.collectAsState()
    val description      by viewModel.description.collectAsState()

    // ── Info banner ──────────────────────────────────────────────────────────
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFE8F5E9),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.BeachAccess, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
            Text(
                "List your holiday property for short-term stays",
                fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium,
            )
        }
    }

    FormDivider()

    // ── Stay Type ────────────────────────────────────────────────────────────
    FormSectionLabel("Type of Stay")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        modifier              = Modifier.fillMaxWidth(),
    ) {
        STAY_TYPES.forEach { (icon, pair) ->
            val (label, key) = pair
            SelectionCard(
                icon       = icon,
                label      = label,
                isSelected = stayType == key,
                onClick    = { viewModel.stayType.value = key },
                modifier   = Modifier.width(100.dp),
            )
        }
    }

    FormDivider()

    // ── Guest + Room Count ───────────────────────────────────────────────────
    FormSectionLabel("Capacity")
    StepperField(
        label          = "Maximum Guests",
        value          = maxGuests.toIntOrNull() ?: 1,
        min            = 1,
        max            = 30,
        onValueChange  = { viewModel.maxGuests.value = it.toString() },
    )
    Spacer(Modifier.height(12.dp))
    StepperField(
        label          = "Bedrooms",
        value          = bedrooms ?: 1,
        min            = 0,
        max            = 20,
        onValueChange  = { viewModel.bedrooms.value = it },
    )
    Spacer(Modifier.height(12.dp))
    StepperField(
        label          = "Bathrooms",
        value          = bathrooms ?: 1,
        min            = 0,
        max            = 20,
        onValueChange  = { viewModel.bathrooms.value = it },
    )

    FormDivider()

    // ── Check-in / Check-out ─────────────────────────────────────────────────
    FormSectionLabel("Check-in / Check-out Times")
    TimeInputRow("Check-in Time",  checkInTime)  { viewModel.checkInTime.value  = it }
    Spacer(Modifier.height(10.dp))
    TimeInputRow("Check-out Time", checkOutTime) { viewModel.checkOutTime.value = it }

    // ── Min Stay ─────────────────────────────────────────────────────────────
    Spacer(Modifier.height(12.dp))
    StepperField(
        label          = "Minimum Stay (nights)",
        value          = minStayNights.toIntOrNull() ?: 1,
        min            = 1,
        max            = 30,
        onValueChange  = { viewModel.minStayNights.value = it.toString() },
    )

    FormDivider()

    // ── Facilities ───────────────────────────────────────────────────────────
    FormSectionLabel("Facilities & Amenities")
    MultiSelectChipGrid(
        items    = STAY_FACILITIES,
        selected = stayFacilities,
        onToggle = { key ->
            val set = viewModel.stayFacilities.value.toMutableSet()
            if (key in set) set.remove(key) else set.add(key)
            viewModel.stayFacilities.value = set
        },
    )

    FormDivider()

    // ── House Rules ──────────────────────────────────────────────────────────
    FormSectionLabel("House Rules")
    MultiSelectChipGrid(
        items    = HOUSE_RULES,
        selected = houseRules,
        onToggle = { key ->
            val set = viewModel.houseRules.value.toMutableSet()
            if (key in set) set.remove(key) else set.add(key)
            viewModel.houseRules.value = set
        },
    )

    FormDivider()

    // ── Cancellation Policy ──────────────────────────────────────────────────
    FormSectionLabel("Cancellation Policy")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CANCELLATION_OPTIONS.forEach { (label, key) ->
            RealEstateFilterChip(
                label    = label,
                selected = stayCancellation == key,
                onClick  = { viewModel.stayCancellation.value = key },
            )
        }
    }
    Text(
        when (stayCancellation) {
            "flexible" -> "✓ Full refund if cancelled 24 hours before check-in"
            "moderate" -> "✓ Full refund if cancelled 5 days before; 50% refund after"
            "strict"   -> "✓ No refund within 7 days of check-in"
            else -> ""
        },
        fontSize = 12.sp, color = TextSecondary,
        modifier = Modifier.padding(top = 2.dp),
    )

    FormDivider()

    // ── Location + Price ─────────────────────────────────────────────────────
    SharedLocationPriceBlock(
        viewModel        = viewModel,
        onPickOnMap      = onPickOnMap,
        priceLabel       = "Price per Night (₹)",
        pricePlaceholder = "e.g. 2500 per night",
        showFrequency    = false,
    )

    // ── WhatsApp ─────────────────────────────────────────────────────────────
    FormDivider()
    FormSectionLabel("Contact WhatsApp")
    OutlinedTextField(
        value           = whatsappNumber,
        onValueChange   = { viewModel.whatsappNumber.value = it },
        label           = { Text("WhatsApp number") },
        placeholder     = { Text("+91 98765 43210") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(10.dp),
        singleLine      = true,
        leadingIcon     = { Text("📱", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF25D366),
            unfocusedBorderColor = BorderColor,
        ),
    )

    // ── Description ──────────────────────────────────────────────────────────
    FormDivider()
    FormSectionLabel("About Your Stay")
    OutlinedTextField(
        value         = description,
        onValueChange = { viewModel.description.value = it },
        label         = { Text("Describe the space, neighbourhood, what guests can expect…") },
        modifier      = Modifier.fillMaxWidth().height(120.dp),
        shape         = RoundedCornerShape(10.dp),
        maxLines      = 6,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. PROPERTY DETAILS FORM (existing fields — extracted for routing)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailsForm(
    viewModel:   PostAdViewModel,
    onPickOnMap: () -> Unit,
) {
    val selectedCategory  by viewModel.selectedCategory.collectAsState()
    val price             by viewModel.price.collectAsState()
    val priceFrequency    by viewModel.priceFrequency.collectAsState()
    val bedrooms          by viewModel.bedrooms.collectAsState()
    val bathrooms         by viewModel.bathrooms.collectAsState()
    val area              by viewModel.area.collectAsState()
    val description       by viewModel.description.collectAsState()
    val furnishing        by viewModel.furnishing.collectAsState()
    val selectedAmenities by viewModel.selectedAmenities.collectAsState()
    val postedBy          by viewModel.postedBy.collectAsState()
    val whatsappNumber    by viewModel.whatsappNumber.collectAsState()
    val isLandlord        = postedBy == "landlord"

    // Media & Rent-specific states
    val youtubeUrl        by viewModel.youtubeUrl.collectAsState()
    val instagramUrl      by viewModel.instagramUrl.collectAsState()
    val deposit           by viewModel.deposit.collectAsState()
    val availabilityDate  by viewModel.availabilityDate.collectAsState()

    val nearbySchoolsList   by viewModel.nearbySchools.collectAsState()
    val nearbyHospitalsList by viewModel.nearbyHospitals.collectAsState()

    val isRent = selectedCategory.contains("Rent", ignoreCase = true)

    var districtMenuExpanded by remember { mutableStateOf(false) }
    val district             by viewModel.district.collectAsState()
    val address              by viewModel.address.collectAsState()
    val pickedLat            by viewModel.pickedLatitude.collectAsState()
    val pickedLng            by viewModel.pickedLongitude.collectAsState()
    val hasLocation          = pickedLat != null && pickedLng != null

    // ── Price ────────────────────────────────────────────────────────────────
    FormSectionLabel("Price (₹ INR)")
    OutlinedTextField(
        value           = price,
        onValueChange   = { viewModel.price.value = it },
        label           = { Text("Enter price in Rupees") },
        placeholder     = { Text("e.g. 18000 (monthly) or 6500000 (sale)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(10.dp),
        singleLine      = true,
        leadingIcon     = { Text("₹", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
    )

    FormSectionLabel("Price Frequency")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("monthly" to "Monthly", "yearly" to "Yearly", "weekly" to "Weekly").forEach { (k, v) ->
            RealEstateFilterChip(
                label    = v,
                selected = priceFrequency == k,
                onClick  = { viewModel.priceFrequency.value = k },
            )
        }
    }

    // ── Rent-specific inputs (Deposit & Availability Date) ──────────────────
    if (isRent) {
        FormDivider()
        FormSectionLabel("Security Deposit (₹)")
        OutlinedTextField(
            value           = deposit,
            onValueChange   = { viewModel.deposit.value = it },
            label           = { Text("Deposit amount") },
            placeholder     = { Text("e.g. 50000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(10.dp),
            singleLine      = true,
            leadingIcon     = { Text("₹", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
        )

        FormSectionLabel("Availability Date")
        OutlinedTextField(
            value         = availabilityDate,
            onValueChange = { viewModel.availabilityDate.value = it },
            label         = { Text("e.g. Immediate, 1st Aug 2026") },
            placeholder   = { Text("When is this property available?") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            singleLine    = true,
        )
    }

    FormDivider()

    // ── Bedrooms ─────────────────────────────────────────────────────────────
    FormSectionLabel("Bedrooms")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(null, 1, 2, 3, 4, 5).forEach { bed ->
            RealEstateFilterChip(
                label    = if (bed == null) "Studio" else "$bed",
                selected = bedrooms == bed,
                onClick  = { viewModel.bedrooms.value = if (viewModel.bedrooms.value == bed) null else bed },
            )
        }
    }

    // ── Bathrooms ────────────────────────────────────────────────────────────
    FormSectionLabel("Bathrooms")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1, 2, 3, 4, 5).forEach { bath ->
            RealEstateFilterChip(
                label    = "$bath",
                selected = bathrooms == bath,
                onClick  = { viewModel.bathrooms.value = if (viewModel.bathrooms.value == bath) null else bath },
            )
        }
    }

    // ── Area ─────────────────────────────────────────────────────────────────
    FormSectionLabel("Area (sqft)")
    OutlinedTextField(
        value           = area,
        onValueChange   = { viewModel.area.value = it },
        label           = { Text("Area in sqft") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(10.dp),
        singleLine      = true,
    )

    // Auto-calculate Rate per SQFT
    val priceVal = price.toDoubleOrNull()
    val areaVal  = area.toDoubleOrNull()
    if (priceVal != null && areaVal != null && areaVal > 0) {
        val rate = priceVal / areaVal
        Spacer(Modifier.height(8.dp))
        Surface(
            color = BannerBlue,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Calculated Rate: ₹${"%,.2f".format(rate)} / sqft",
                color = NestXBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    FormDivider()

    // ── District ─────────────────────────────────────────────────────────────
    FormSectionLabel("District (Tamil Nadu) *")
    ExposedDropdownMenuBox(
        expanded         = districtMenuExpanded,
        onExpandedChange = { districtMenuExpanded = !districtMenuExpanded },
    ) {
        OutlinedTextField(
            value         = district.ifBlank { "Select district" },
            onValueChange = {},
            readOnly      = true,
            label         = { Text("District") },
            trailingIcon  = { Icon(Icons.Filled.ArrowDropDown, null, tint = TextSecondary) },
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = NestXBlue,
                unfocusedBorderColor = BorderColor,
            ),
        )
        ExposedDropdownMenu(
            expanded         = districtMenuExpanded,
            onDismissRequest = { districtMenuExpanded = false },
        ) {
            TamilNaduData.districts.forEach { d ->
                DropdownMenuItem(
                    text    = { Text(d) },
                    onClick = { viewModel.district.value = d; districtMenuExpanded = false },
                )
            }
        }
    }

    // ── Location on Map ──────────────────────────────────────────────────────
    FormSectionLabel("Location on Map")
    if (hasLocation) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, NestXBlue), RoundedCornerShape(10.dp))
                .clickable(onClick = onPickOnMap)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = NestXBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(address.ifBlank { "Location pinned" }, fontSize = 13.sp, color = TextPrimary)
                Text("%.5f, %.5f".format(pickedLat!!, pickedLng!!), fontSize = 11.sp, color = TextSecondary)
            }
            Text("Change", fontSize = 12.sp, color = NestXBlue)
        }
    } else {
        OutlinedButton(
            onClick  = onPickOnMap,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(10.dp),
            border   = BorderStroke(1.5.dp, NestXBlue),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = NestXBlue),
            enabled  = district.isNotBlank(),
        ) {
            Icon(Icons.Filled.LocationOn, null)
            Spacer(Modifier.width(8.dp))
            Text(if (district.isBlank()) "Select district first" else "Pin Location on Map", fontSize = 14.sp)
        }
    }

    // ── Address ───────────────────────────────────────────────────────────────
    OutlinedTextField(
        value         = address,
        onValueChange = { viewModel.address.value = it },
        label         = { Text("Street / Locality / Landmark") },
        placeholder   = { Text("Auto-filled when you pin on map") },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        singleLine    = true,
    )

    // ── WhatsApp (landlord only) ──────────────────────────────────────────────
    if (isLandlord) {
        FormDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormSectionLabel("WhatsApp Number")
            Spacer(Modifier.width(4.dp))
            Text(" *required", fontSize = 12.sp, color = NestXBlue, fontWeight = FontWeight.Medium)
        }
        OutlinedTextField(
            value           = whatsappNumber,
            onValueChange   = { viewModel.whatsappNumber.value = it },
            label           = { Text("WhatsApp number") },
            placeholder     = { Text("e.g. 9876543210") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(10.dp),
            singleLine      = true,
            leadingIcon     = { Text("📱", fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) },
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Color(0xFF25D366),
                unfocusedBorderColor = BorderColor,
            ),
        )
        Text("Buyers will be able to message you directly on WhatsApp.", fontSize = 12.sp, color = TextSecondary)
        FormDivider()
    }

    // ── Media Links (YouTube, Instagram) ──────────────────────────────────────
    FormSectionLabel("Video & Media Links (Optional)")
    OutlinedTextField(
        value         = youtubeUrl,
        onValueChange = { viewModel.youtubeUrl.value = it },
        label         = { Text("YouTube Video Link") },
        placeholder   = { Text("e.g. https://www.youtube.com/watch?v=...") },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        singleLine    = true,
        leadingIcon   = { Text("🎥", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value         = instagramUrl,
        onValueChange = { viewModel.instagramUrl.value = it },
        label         = { Text("Instagram Reel Link") },
        placeholder   = { Text("e.g. https://www.instagram.com/reel/...") },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        singleLine    = true,
        leadingIcon   = { Text("📸", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
    )

    FormDivider()

    // ── Nearby places (comma-separated lists) ─────────────────────────────────
    FormSectionLabel("Nearby Places (Optional)")
    var schoolsInput by remember { mutableStateOf(nearbySchoolsList.joinToString(", ")) }
    OutlinedTextField(
        value         = schoolsInput,
        onValueChange = {
            schoolsInput = it
            viewModel.nearbySchools.value = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotBlank() }
        },
        label         = { Text("Nearby Schools (comma separated)") },
        placeholder   = { Text("e.g. St. Joseph, Green Valley School") },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
    )
    Spacer(Modifier.height(8.dp))
    var hospitalsInput by remember { mutableStateOf(nearbyHospitalsList.joinToString(", ")) }
    OutlinedTextField(
        value         = hospitalsInput,
        onValueChange = {
            hospitalsInput = it
            viewModel.nearbyHospitals.value = it.split(",").map { h -> h.trim() }.filter { h -> h.isNotBlank() }
        },
        label         = { Text("Nearby Hospitals (comma separated)") },
        placeholder   = { Text("e.g. Apollo Hospital, City Clinic") },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
    )

    FormDivider()

    // ── Furnishing ────────────────────────────────────────────────────────────
    FormSectionLabel("Furnishing")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "furnished"      to "Furnished",
            "semi_furnished" to "Semi-Furnished",
            "unfurnished"    to "Unfurnished",
        ).forEach { (k, v) ->
            RealEstateFilterChip(
                label    = v,
                selected = furnishing == k,
                onClick  = { viewModel.furnishing.value = k },
            )
        }
    }

    // ── Description ───────────────────────────────────────────────────────────
    FormSectionLabel("Description")
    OutlinedTextField(
        value         = description,
        onValueChange = { viewModel.description.value = it },
        label         = { Text("Describe your property") },
        modifier      = Modifier.fillMaxWidth().height(120.dp),
        shape         = RoundedCornerShape(10.dp),
        maxLines      = 6,
    )

    // ── Amenities ─────────────────────────────────────────────────────────────
    FormSectionLabel("Amenities")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        com.realestate.app.data.models.Amenity.values().forEach { amenity ->
            RealEstateFilterChip(
                label    = amenity.displayName,
                selected = amenity.name in selectedAmenities,
                onClick  = {
                    val current = viewModel.selectedAmenities.value.toMutableSet()
                    if (amenity.name in current) current.remove(amenity.name) else current.add(amenity.name)
                    viewModel.selectedAmenities.value = current
                },
            )
        }
    }
}
