package com.realestate.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.app.data.models.ApprovalStatus
import com.realestate.app.ui.components.PropertyMapView
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPropertyReviewScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    propertyId: String,
    onBack: () -> Unit,
) {
    // ── Observe property reactively from the shared AdminViewModel ───────────
    // Both Dashboard and Review now share ONE AdminViewModel instance (via the
    // nested nav graph in AppNavGraph). The Dashboard already loaded all
    // properties, so propertyFlow emits immediately — no "not found" flash.
    val property  by viewModel.propertyFlow(propertyId).collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectSheet by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    // Show loading spinner while the property list is still being fetched
    if (property == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = NestXBlue)
            } else {
                Text("Property not found", color = TextSecondary)
            }
        }
        return
    }

    val p = property!!
    // Null-safe: before the TypeAdapter fix, Gson could deserialise
    // lowercase "pending" as null. Elvis fallback prevents a crash.
    val approvalStatus = p.approvalStatus ?: ApprovalStatus.PENDING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Listing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite),
            )
        },
        // Bottom action bar — only shown while still pending
        bottomBar = {
            if (approvalStatus == ApprovalStatus.PENDING) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { showApproveDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("✓ Approve", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { showRejectSheet = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, StatusRejected),
                        ) {
                            Text(
                                "✗ Reject",
                                color = StatusRejected,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        containerColor = BackgroundWhite,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Status banner ─────────────────────────────────────────────────
            val (bannerBg, bannerLabel) = when (approvalStatus) {
                ApprovalStatus.APPROVED -> StatusApproved.copy(alpha = 0.12f) to "🟢  Approved — Listing is Live"
                ApprovalStatus.REJECTED -> StatusRejected.copy(alpha = 0.12f) to "🔴  Rejected"
                else                    -> BannerBlue to "🟡  Pending Admin Review"
            }
            Surface(color = bannerBg, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(bannerLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    // Show rejection reason if present
                    if (approvalStatus == ApprovalStatus.REJECTED &&
                        !p.rejectionReason.isNullOrBlank()
                    ) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Reason: ${p.rejectionReason}",
                            fontSize = 13.sp,
                            color = StatusRejected,
                        )
                    }
                }
            }

            // ── Property hero image ───────────────────────────────────────────
            AsyncImage(
                model = p.images.firstOrNull() ?: "",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            Column(modifier = Modifier.padding(16.dp)) {

                // ── Price & title ─────────────────────────────────────────────
                Text(
                    p.priceDisplay,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NestXBlue
                )
                Spacer(Modifier.height(2.dp))
                Text(p.title.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${p.neighborhood}, ${p.district}", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(p.statsLine, fontSize = 13.sp, color = TextSecondary)

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                // ── Location map (shown only when lat/lng are available) ───────
                if (p.latitude != null && p.longitude != null) {
                    val lat = p.latitude
                    val lng = p.longitude

                    Text("Property Location", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // ── Native Google Map ────────────────────────────────
                        PropertyMapView(
                            lat = lat,
                            lng = lng,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        )
                        // Coordinates row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = NestXBlue,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = p.address.orEmpty().ifBlank { "Location pinned" },
                                fontSize = 13.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text = "%.5f, %.5f".format(lat, lng),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 28.dp, bottom = 10.dp),
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                }

                // ── Agent / submitter info ─────────────────────────────────────
                Text("Submitted By", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text("Name: ${p.agentName.ifBlank { "—" }}", fontSize = 14.sp)
                Text(
                    "Phone: ${p.agentPhone.ifBlank { "—" }}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text("Role: ${p.listedBy}", fontSize = 13.sp, color = NestXBlue)
                Text("Submitted: ${p.createdAt.take(10)}", fontSize = 12.sp, color = TextSecondary)
                Text("Ref: ${p.referenceId}", fontSize = 12.sp, color = TextSecondary)

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                // ── Description ───────────────────────────────────────────────
                Text("Description", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    p.description.orEmpty().ifBlank { "No description provided." },
                    fontSize = 14.sp,
                    color = if (p.description.isNullOrBlank()) TextSecondary else TextPrimary,
                )

                // ── Amenities ─────────────────────────────────────────────────
                if (p.amenities.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                    Text("Amenities", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        p.amenities.joinToString(" • ") { name ->
                            name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }

                // Extra bottom padding so content clears the sticky bottom bar
                Spacer(Modifier.height(88.dp))
            }
        }
    }

    // ── Approve confirmation dialog ───────────────────────────────────────────
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve Listing?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This listing will be published and visible to all users on NestX.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Writes to MockData + refreshes _properties StateFlow →
                        // propertyFlow emits the updated Property → UI re-composes
                        viewModel.approveProperty(propertyId)
                        showApproveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Approve", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showApproveDialog = false },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }

    // ── Reject bottom sheet ───────────────────────────────────────────────────
    if (showRejectSheet) {
        ModalBottomSheet(onDismissRequest = { showRejectSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Reason for Rejection", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Describe why this listing is being rejected…") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusRejected,
                        unfocusedBorderColor = BorderColor,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Writes reason to MockData → propertyFlow emits updated Property
                        // → status banner shows "Rejected" + the reason automatically
                        viewModel.rejectProperty(propertyId, rejectReason)
                        showRejectSheet = false
                        rejectReason = ""   // reset for next use
                    },
                    enabled = rejectReason.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Confirm Rejection", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}