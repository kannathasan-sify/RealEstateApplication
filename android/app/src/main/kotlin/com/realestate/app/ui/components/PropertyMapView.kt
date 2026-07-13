package com.realestate.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Native Google Maps view for displaying a property location.
 *
 * Used in three places:
 *  • PropertyDetailScreen   – full-width interactive map (pan + zoom enabled)
 *  • AdminPropertyReviewScreen – same full-width interactive map
 *  • PostAdDetailsScreen    – compact thumbnail after the user picks a location
 *                             (interactive = false so it doesn't block scroll)
 *
 * @param lat         Property latitude
 * @param lng         Property longitude
 * @param modifier    Compose modifier — caller sets the height
 * @param zoom        Initial camera zoom (default 15 — neighbourhood level)
 * @param interactive Allow pan / zoom gestures (set false for thumbnail previews
 *                    inside scrollable lists so the map doesn't swallow touch events)
 */
@Composable
fun PropertyMapView(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    zoom: Float = 15f,
    interactive: Boolean = true,
) {
    val position = remember(lat, lng) { LatLng(lat, lng) }

    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, zoom)
    }

    GoogleMap(
        modifier            = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings          = MapUiSettings(
            zoomControlsEnabled    = false,           // use pinch-to-zoom instead
            scrollGesturesEnabled  = interactive,
            zoomGesturesEnabled    = interactive,
            tiltGesturesEnabled    = false,
            rotationGesturesEnabled= false,
            myLocationButtonEnabled= false,
            compassEnabled         = false,
            mapToolbarEnabled      = false,
        ),
        properties = MapProperties(isMyLocationEnabled = false),
    ) {
        Marker(
            state = MarkerState(position = position),
            title = "Property Location",
        )
    }
}
