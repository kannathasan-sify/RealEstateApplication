package com.realestate.app.ui.post_ad

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.BuildConfig
import com.realestate.app.data.local.DataStoreManager
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.PropertyCreateRequest
import com.realestate.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.realestate.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Image count constraints enforced on both client and backend. */
const val MIN_IMAGES = 2
const val MAX_IMAGES = 10

@HiltViewModel
class PostAdViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val authRepo: AuthRepository,
    private val dataStore: DataStoreManager,
) : ViewModel() {

    val subscriptionDetails = MutableStateFlow<com.realestate.app.data.api.SubscriptionDetails?>(null)

    init {
        checkSubscriptionLimits()
    }

    fun checkSubscriptionLimits() {
        viewModelScope.launch {
            repo.getSubscriptionDetails().fold(
                onSuccess = { subscriptionDetails.value = it },
                onFailure = { /* fallback or ignore network failure */ }
            )
        }
    }

    // Step 1 — category (e.g. "Property for Rent")
    val selectedCategory = MutableStateFlow("")

    // Step 2 — short title
    val title = MutableStateFlow("")

    // Step 3 — sub-category (e.g. "apartment")
    val subCategory = MutableStateFlow("")

    // Step 4 — who is posting
    val postedBy = MutableStateFlow("agent")   // "agent" | "builder" | "landlord"

    // Step 5 — full details

    // ── Images (2 min, 10 max) ────────────────────────────────────────────────
    val selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())

    val price          = MutableStateFlow("")
    val priceFrequency = MutableStateFlow("monthly")  // "monthly" | "yearly" | "weekly"
    val bedrooms       = MutableStateFlow<Int?>(null)
    val bathrooms      = MutableStateFlow<Int?>(null)
    val area           = MutableStateFlow("")

    /** Tamil Nadu district — required for all listings */
    val district          = MutableStateFlow("")
    val address           = MutableStateFlow("")

    /** Coordinates picked via the interactive map */
    val pickedLatitude    = MutableStateFlow<Double?>(null)
    val pickedLongitude   = MutableStateFlow<Double?>(null)

    val description       = MutableStateFlow("")
    val furnishing        = MutableStateFlow("unfurnished")
    val selectedAmenities = MutableStateFlow<Set<String>>(emptySet())

    /** WhatsApp number — required for landlord listings, optional otherwise. */
    val whatsappNumber = MutableStateFlow("")

    // ── Media Support ────────────────────────────────────────────────────────
    val youtubeUrl        = MutableStateFlow("")
    val instagramUrl      = MutableStateFlow("")

    // ── Rent-specific ────────────────────────────────────────────────────────
    val deposit           = MutableStateFlow("")
    val availabilityDate  = MutableStateFlow("")

    // ── Buy / Rent Nearby ────────────────────────────────────────────────────
    val nearbySchools     = MutableStateFlow<List<String>>(emptyList())
    val nearbyHospitals   = MutableStateFlow<List<String>>(emptyList())
    val documentUris      = MutableStateFlow<List<Uri>>(emptyList())

    // ── Contractor-specific (Construction / Maintenance) ────────────────────
    val workCategory        = MutableStateFlow("")
    val contractorWorkTypes = MutableStateFlow<Set<String>>(emptySet())
    val companyProfile      = MutableStateFlow("")
    val previousProjectUris = MutableStateFlow<List<Uri>>(emptyList())
    val yearsExperience     = MutableStateFlow("")
    val serviceDistricts    = MutableStateFlow<Set<String>>(emptySet())
    val pricingModel        = MutableStateFlow("quotation")
    val licenseNumber       = MutableStateFlow("")
    val teamSize            = MutableStateFlow("small")
    val projectTimeline     = MutableStateFlow("")
    val warrantyOffered     = MutableStateFlow(false)
    val warrantyDuration    = MutableStateFlow("")

    // ── Ground-specific ──────────────────────────────────────────────────────
    val groundType         = MutableStateFlow("")
    val groundLength       = MutableStateFlow("")
    val groundWidth        = MutableStateFlow("")
    val surfaceType        = MutableStateFlow("")
    val hasFloodlights     = MutableStateFlow(false)
    val availableFrom      = MutableStateFlow("06:00")
    val availableTo        = MutableStateFlow("22:00")
    val groundCapacity     = MutableStateFlow("")
    val advanceBookingReq  = MutableStateFlow(false)
    val groundFacilities   = MutableStateFlow<Set<String>>(emptySet())
    val cancellationPolicy = MutableStateFlow("flexible")

    // ── Holiday Stay-specific ────────────────────────────────────────────────
    val stayType          = MutableStateFlow("entire_home")
    val maxGuests         = MutableStateFlow("")
    val checkInTime       = MutableStateFlow("12:00")
    val checkOutTime      = MutableStateFlow("11:00")
    val minStayNights     = MutableStateFlow("1")
    val stayFacilities    = MutableStateFlow<Set<String>>(emptySet())
    val houseRules        = MutableStateFlow<Set<String>>(emptySet())
    val stayCancellation  = MutableStateFlow("moderate")

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState

    // ── Image selection helpers ──────────────────────────────────────────────

    fun addImages(uris: List<Uri>) {
        val current = selectedImageUris.value.toMutableList()
        val limit = subscriptionDetails.value?.maxImages ?: 10
        for (uri in uris) {
            if (current.size >= limit) break
            if (uri !in current) current.add(uri)
        }
        selectedImageUris.value = current
    }

    fun removeImage(uri: Uri) {
        selectedImageUris.value = selectedImageUris.value.filter { it != uri }
    }

    fun replaceImage(oldUri: Uri, newUri: Uri) {
        val limit = subscriptionDetails.value?.maxImages ?: 10
        selectedImageUris.value = selectedImageUris.value.toMutableList().also { list ->
            val idx = list.indexOf(oldUri)
            if (idx >= 0) list[idx] = newUri else if (list.size < limit) list.add(newUri)
        }
    }

    fun resetWizard() {
        selectedCategory.value  = ""
        title.value             = ""
        subCategory.value       = ""
        postedBy.value          = "agent"
        selectedImageUris.value = emptyList()
        price.value             = ""
        priceFrequency.value    = "monthly"
        bedrooms.value          = null
        bathrooms.value         = null
        area.value              = ""
        district.value          = ""
        address.value           = ""
        pickedLatitude.value    = null
        pickedLongitude.value   = null
        description.value       = ""
        furnishing.value        = "unfurnished"
        selectedAmenities.value = emptySet()
        whatsappNumber.value    = ""

        // Media / Rent / Nearby
        youtubeUrl.value        = ""
        instagramUrl.value      = ""
        deposit.value           = ""
        availabilityDate.value  = ""
        nearbySchools.value     = emptyList()
        nearbyHospitals.value   = emptyList()
        documentUris.value      = emptyList()

        // Contractor
        companyProfile.value      = ""
        previousProjectUris.value = emptyList()
        yearsExperience.value     = ""
        serviceDistricts.value    = emptySet()
        pricingModel.value        = "quotation"
        licenseNumber.value       = ""
        teamSize.value            = "small"
        projectTimeline.value     = ""
        warrantyOffered.value     = false
        warrantyDuration.value    = ""

        // Ground
        groundType.value         = ""
        groundLength.value       = ""
        groundWidth.value        = ""
        surfaceType.value        = ""
        hasFloodlights.value     = false
        availableFrom.value      = "06:00"
        availableTo.value        = "22:00"
        groundCapacity.value     = ""
        advanceBookingReq.value  = false
        groundFacilities.value   = emptySet()
        cancellationPolicy.value = "flexible"

        // Holiday Stay
        stayType.value          = "entire_home"
        maxGuests.value         = ""
        checkInTime.value       = "12:00"
        checkOutTime.value      = "11:00"
        minStayNights.value     = "1"
        stayFacilities.value    = emptySet()
        houseRules.value        = emptySet()
        stayCancellation.value  = "moderate"

        _submitState.value      = SubmitState.Idle
    }

    fun submitAd() {
        val districtVal = district.value.trim()
        if (districtVal.isBlank()) {
            _submitState.value = SubmitState.Error("Please select a Tamil Nadu district.")
            return
        }
        if (title.value.isBlank()) {
            _submitState.value = SubmitState.Error("Please enter a title for your listing.")
            return
        }
        if (price.value.toDoubleOrNull() == null || price.value.toDouble() <= 0) {
            _submitState.value = SubmitState.Error("Please enter a valid price in Rupees.")
            return
        }
        val imageCount = selectedImageUris.value.size
        if (imageCount < MIN_IMAGES) {
            _submitState.value = SubmitState.Error(
                "Please add at least $MIN_IMAGES photos for your listing (you have $imageCount)."
            )
            return
        }

        val cat = selectedCategory.value
        val isConstruction = cat.contains("Construction", ignoreCase = true)
        val isMaintenance = cat.contains("Maintenance", ignoreCase = true)
        val isContractor = isConstruction || isMaintenance

        val needsWhatsApp = postedBy.value == "landlord" || isContractor
        if (needsWhatsApp && whatsappNumber.value.trim().isBlank()) {
            val who = if (isContractor) "service provider" else "landlord"
            _submitState.value = SubmitState.Error(
                "Please enter your WhatsApp number — it's required for $who listings."
            )
            return
        }

        if (cat.contains("Ground", ignoreCase = true) && groundType.value.isBlank()) {
            _submitState.value = SubmitState.Error("Please select the type of ground / sport.")
            return
        }

        viewModelScope.launch {
            _submitState.value = SubmitState.Loading

            // Auto-update user's role if they are registered as a "buyer" but trying to post a listing
            val currentRole = dataStore.userRole.first().orEmpty()
            if (currentRole.equals("buyer", ignoreCase = true) || currentRole.isBlank()) {
                val targetRole = if (postedBy.value == "agent") "agent" else "landlord"
                authRepo.setRole(targetRole).fold(
                    onSuccess = { updatedUser ->
                        dataStore.saveUserRole(updatedUser.roleStr)
                    },
                    onFailure = {
                        // Ignore/proceed: the backend will perform final validation
                    }
                )
            }

            val agentName  = dataStore.userName.first()?.ifBlank { null }
            val agentPhone = dataStore.userPhone.first()?.ifBlank { null }
            val agentPhoto = dataStore.userAvatar.first()?.ifBlank { null }

            val listingType = when {
                cat.contains("Rent", ignoreCase = true)         -> "rent"
                cat.contains("Sale", ignoreCase = true)         -> "sale"
                cat.contains("Construction", ignoreCase = true) -> "contractor"
                cat.contains("Maintenance", ignoreCase = true)  -> "maintenance"
                cat.contains("Holiday", ignoreCase = true)      -> "holiday_stay"
                cat.equals("Ground", ignoreCase = true)          -> "ground"
                else -> "rent"
            }

            val whatsapp = whatsappNumber.value.trim().ifBlank { null }

            // Build metadata
            val metadata: Map<String, String>? = when {
                cat.contains("Ground", ignoreCase = true) -> buildMap {
                    if (groundType.value.isNotBlank())       put("ground_type",    groundType.value)
                    if (groundLength.value.isNotBlank())     put("length_m",       groundLength.value)
                    if (groundWidth.value.isNotBlank())      put("width_m",        groundWidth.value)
                    if (surfaceType.value.isNotBlank())      put("surface",        surfaceType.value)
                    put("floodlights",     hasFloodlights.value.toString())
                    put("available_from",  availableFrom.value)
                    put("available_to",    availableTo.value)
                    if (groundCapacity.value.isNotBlank())   put("capacity",       groundCapacity.value)
                    put("advance_booking", advanceBookingReq.value.toString())
                    put("cancellation",    cancellationPolicy.value)
                    if (groundFacilities.value.isNotEmpty()) put("facilities",     groundFacilities.value.joinToString(","))
                }
                isContractor -> buildMap {
                    put("work_category", if (isConstruction) "construction" else "maintenance")
                    put("pricing_model", pricingModel.value)
                    put("team_size",     teamSize.value)
                    if (yearsExperience.value.isNotBlank())  put("experience_yrs", yearsExperience.value)
                    if (serviceDistricts.value.isNotEmpty()) put("service_areas",  serviceDistricts.value.joinToString(","))
                    if (licenseNumber.value.isNotBlank())    put("license_no",     licenseNumber.value)
                    if (projectTimeline.value.isNotBlank())  put("timeline",       projectTimeline.value)
                }
                cat.contains("Holiday", ignoreCase = true) -> buildMap {
                    put("stay_type",       stayType.value)
                    if (maxGuests.value.isNotBlank())        put("max_guests",     maxGuests.value)
                    put("check_in",        checkInTime.value)
                    put("check_out",       checkOutTime.value)
                    put("min_nights",      minStayNights.value)
                    if (stayFacilities.value.isNotEmpty())   put("facilities",     stayFacilities.value.joinToString(","))
                    if (houseRules.value.isNotEmpty())       put("house_rules",    houseRules.value.joinToString(","))
                    put("cancellation",    stayCancellation.value)
                }
                else -> null
            }

            val docUrls = documentUris.value.map { "https://supabase.co/storage/v1/object/public/documents/${it.lastPathSegment}" }
            val prevProjUrls = previousProjectUris.value.map { "https://supabase.co/storage/v1/object/public/contractor_projects/${it.lastPathSegment}" }

            // NOTE (fixed 2026-07-15): these values must match backend/app/schemas/property.py's
            // PropertyType Pydantic enum exactly — FastAPI validates the request body against
            // that enum BEFORE it ever reaches the DB, so the old values here (matched against
            // the Postgres CHECK constraint's vocabulary instead) were rejected with a 400 on
            // every Construction/Maintenance/Farmhouse submission. See CLAUDE.md Doc Drift Note #3.
            val rawPropertyType = subCategory.value.trim()
            val propertyTypeValue = when (rawPropertyType) {
                // Buy & Rent
                "Residential" -> "apartment"
                "Commercial" -> "office"
                "Hotel / Resort" -> "hotel"
                "Home Stay / PG" -> "room"
                "Industrial Properties" -> "industrial_land"
                "Agricultural Land" -> "land"
                "Farmhouse" -> "farmhouse"

                // Construction (Contractor)
                "Civil Contractors" -> "civil_contractor"
                "Builders" -> "builder"
                "Architects" -> "architect"
                "Structural Engineers" -> "structural_engineer"
                "Interior Designers" -> "interior_designer"
                "Plumbing" -> "plumbing_contractor"
                "Electrical" -> "electrical_contractor"
                // "Painting" is used by both the Construction and Maintenance sub-category
                // lists with the identical label, so disambiguate via the selected category.
                "Painting" -> if (isConstruction) "painting_contractor" else "painting_service"
                "False Ceiling" -> "false_ceiling"
                "Tiles" -> "tiles_contractor"
                "Roofing" -> "roofing"
                "Landscaping" -> "landscaping"

                // Maintenance (Contractor)
                "Electrician" -> "electrician"
                "Plumber" -> "plumber"
                "Carpenter" -> "carpenter"
                "AC Service" -> "ac_service"
                "CCTV" -> "cctv_service"
                "Cleaning" -> "cleaning_service"
                "Pest Control" -> "pest_control"
                "Borewell" -> "borewell"
                "Water Tank Cleaning" -> "water_tank_cleaning"

                else -> rawPropertyType.lowercase().replace(" ", "_").replace("/", "_").ifBlank { "apartment" }
            }

            val request = PropertyCreateRequest(
                title            = title.value.trim(),
                description      = description.value.trim().ifBlank { null },
                price            = price.value.toDouble(),
                priceFrequency   = priceFrequency.value,
                listingType      = listingType,
                propertyType     = propertyTypeValue,
                listedBy         = postedBy.value,
                district         = districtVal,
                city             = districtVal,
                address          = address.value.trim().ifBlank { null },
                bedrooms         = bedrooms.value,
                bathrooms        = bathrooms.value,
                areaSqft         = area.value.toDoubleOrNull(),
                furnishing       = furnishing.value,
                amenities        = selectedAmenities.value.toList(),
                latitude         = pickedLatitude.value,
                longitude        = pickedLongitude.value,
                youtubeUrl       = youtubeUrl.value.trim().ifBlank { null },
                instagramUrl     = instagramUrl.value.trim().ifBlank { null },
                deposit          = deposit.value.toDoubleOrNull(),
                availabilityDate = availabilityDate.value.trim().ifBlank { null },
                nearbySchools    = nearbySchools.value,
                nearbyHospitals  = nearbyHospitals.value,
                documentUrls     = docUrls,
                companyProfile   = companyProfile.value.trim().ifBlank { null },
                agentName        = agentName,
                agentPhone       = agentPhone,
                agentPhoto       = agentPhoto,
                whatsappNumber   = whatsapp,
                metadata         = metadata,
            )

            if (BuildConfig.USE_MOCK_DATA) {
                delay(800)
                MockData.createProperty(request)
                _submitState.value = SubmitState.Success
            } else {
                repo.createProperty(request).fold(
                    onSuccess = { property ->
                        val imageUris = selectedImageUris.value
                        if (imageUris.isNotEmpty()) {
                            repo.uploadPropertyImages(property.id, imageUris).fold(
                                onSuccess = { _submitState.value = SubmitState.Success },
                                onFailure = { e ->
                                    _submitState.value = SubmitState.SuccessWithImageError(
                                        propertyId = property.id,
                                        imageError = e.message ?: "Image upload failed. You can re-upload from My Ads."
                                    )
                                },
                            )
                        } else {
                            _submitState.value = SubmitState.Success
                        }
                    },
                    onFailure = { e ->
                        _submitState.value = SubmitState.Error(
                            e.message ?: "Failed to submit listing. Please try again."
                        )
                    },
                )
            }
        }
    }
}

sealed class SubmitState {
    object Idle    : SubmitState()
    object Loading : SubmitState()
    object Success : SubmitState()
    data class SuccessWithImageError(val propertyId: String, val imageError: String) : SubmitState()
    data class Error(val message: String) : SubmitState()
}
