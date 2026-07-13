package com.realestate.app.ui.post_ad

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.ui.theme.NestXBlue
import com.realestate.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Full-screen interactive Google Map that lets the user pin their property location.
 *
 * • Camera opens centred on the selected Tamil Nadu district (or on the previously
 *   picked coordinate if one already exists in the ViewModel).
 * • Tapping the map drops / moves a blue marker and triggers Android Geocoder
 *   reverse-geocoding to show a human-readable address.
 * • "Confirm Location" writes lat / lng / address into [PostAdViewModel] and
 *   calls [onLocationConfirmed] to pop back to PostAdDetailsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAdMapPickerScreen(
    viewModel: PostAdViewModel,
    onLocationConfirmed: () -> Unit,
    onBack: () -> Unit,
) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val district     by viewModel.district.collectAsState()
    val initialLat   by viewModel.pickedLatitude.collectAsState()
    val initialLng   by viewModel.pickedLongitude.collectAsState()

    // Starting coordinates — previously picked point or district centre
    val districtCenter = remember(district) { TamilNaduData.coordinatesForDistrict(district) }
    val startLatLng = remember(initialLat, initialLng) {
        if (initialLat != null && initialLng != null)
            LatLng(initialLat!!, initialLng!!)
        else
            LatLng(districtCenter.first, districtCenter.second)
    }

    // Mutable state for the dropped pin and its reverse-geocoded address
    var markerPosition by remember { mutableStateOf<LatLng?>(
        if (initialLat != null && initialLng != null)
            LatLng(initialLat!!, initialLng!!)
        else null
    )}
    var pickedAddress  by remember { mutableStateOf(viewModel.address.value) }
    var isGeocoding    by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 13f)
    }

    /** Reverse-geocode [latLng] on the IO dispatcher using Android Geocoder. */
    fun reverseGeocode(latLng: LatLng) {
        scope.launch {
            isGeocoding = true
            pickedAddress = ""
            try {
                val result: String = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, Locale.ENGLISH)
                        .getFromLocation(latLng.latitude, latLng.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val a = addresses[0]
                        listOfNotNull(
                            a.thoroughfare,
                            a.subLocality ?: a.locality,
                            a.subAdminArea,
                            a.adminArea,
                        ).filter { it.isNotBlank() }.joinToString(", ")
                            .ifBlank { "%.5f, %.5f".format(latLng.latitude, latLng.longitude) }
                    } else {
                        "%.5f, %.5f".format(latLng.latitude, latLng.longitude)
                    }
                }
                pickedAddress = result
            } catch (e: Exception) {
                pickedAddress = "%.5f, %.5f".format(latLng.latitude, latLng.longitude)
            } finally {
                isGeocoding = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Pick Location",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp,
                        )
                        if (district.isNotBlank()) {
                            Text(
                                text     = district,
                                fontSize = 12.sp,
                                color    = TextSecondary,
                            )
                        }
                    }
                },
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            // ── Google Map fills the whole Box ────────────────────────────────
            GoogleMap(
                modifier            = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings          = MapUiSettings(
                    zoomControlsEnabled     = true,
                    scrollGesturesEnabled   = true,
                    zoomGesturesEnabled     = true,
                    tiltGesturesEnabled     = false,
                    rotationGesturesEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled          = true,
                    mapToolbarEnabled       = false,
                ),
                properties = MapProperties(isMyLocationEnabled = false),
                onMapClick = { latLng ->
                    markerPosition = latLng
                    reverseGeocode(latLng)
                    // Gently pan camera to the tapped point
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLng(latLng),
                            durationMs = 300,
                        )
                    }
                },
            ) {
                markerPosition?.let { pos ->
                    Marker(
                        state = MarkerState(position = pos),
                        title = pickedAddress.ifBlank { "Selected location" },
                    )
                }
            }

            // ── Hint badge (shown until the user taps) ────────────────────────
            if (markerPosition == null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text     = "Tap the map to pin your property",
                        color    = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Bottom confirmation strip ─────────────────────────────────────
            Surface(
                modifier      = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color         = Color.White,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Address / geocoding indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint     = NestXBlue,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        when {
                            markerPosition == null -> Text(
                                "No location selected yet",
                                fontSize = 13.sp,
                                color    = TextSecondary,
                            )
                            isGeocoding -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier  = Modifier.size(14.dp),
                                    color     = NestXBlue,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Fetching address…", fontSize = 13.sp, color = TextSecondary)
                            }
                            else -> Text(
                                text     = pickedAddress.ifBlank {
                                    markerPosition?.let {
                                        "%.5f, %.5f".format(it.latitude, it.longitude)
                                    } ?: ""
                                },
                                fontSize    = 13.sp,
                                color       = TextSecondary,
                                maxLines    = 2,
                                overflow    = TextOverflow.Ellipsis,
                                modifier    = Modifier.weight(1f),
                            )
                        }
                    }

                    // Confirm button
                    Button(
                        onClick = {
                            val pos = markerPosition ?: return@Button
                            viewModel.pickedLatitude.value  = pos.latitude
                            viewModel.pickedLongitude.value = pos.longitude
                            if (pickedAddress.isNotBlank()) {
                                viewModel.address.value = pickedAddress
                            }
                            onLocationConfirmed()
                        },
                        enabled  = markerPosition != null && !isGeocoding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                    ) {
                        Text(
                            "Confirm Location",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(4.dp))   // navigation-bar inset breathing room
                }
            }
        }
    }
}
