package com.realestate.app.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.Booking
import com.realestate.app.data.models.Property
import com.realestate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    viewModel: MyBookingsViewModel,
    onBack: () -> Unit,
    onPropertyClick: (String) -> Unit,
) {
    val bookings  by viewModel.bookings.collectAsState()
    val ownerBookings by viewModel.ownerBookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visits & Meetings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = NestXBlue)
                    }
                }
            )
        },
        containerColor = SurfaceGray,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NestXBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NestXBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Visits", fontWeight = FontWeight.Medium) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Received Inquiries", fontWeight = FontWeight.Medium) }
                )
            }

            val currentList = if (selectedTab == 0) bookings else ownerBookings

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NestXBlue)
                    }
                }

                currentList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint     = BorderColor,
                                modifier = Modifier.size(64.dp),
                            )
                            Text(
                                if (selectedTab == 0) "No visits booked yet" else "No inquiries received yet",
                                fontSize   = 17.sp,
                                color      = TextSecondary,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                if (selectedTab == 0) "Book a property visit from the listing page\nand it will appear here."
                                else "When customers request property visits or meetings\nthey will show up here.",
                                fontSize   = 13.sp,
                                color      = TextSecondary,
                                textAlign  = TextAlign.Center,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier        = Modifier.fillMaxSize(),
                        contentPadding  = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(currentList) { bwp ->
                            BookingCard(
                                booking          = bwp.booking,
                                property         = bwp.property,
                                onViewProperty   = { bwp.property?.let { onPropertyClick(it.id) } },
                                onCancelBooking  = { viewModel.cancelBooking(bwp.booking.id) },
                                isOwnerView      = selectedTab == 1,
                                onUpdateStatus   = { status -> viewModel.updateBookingStatus(bwp.booking.id, status) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Booking detail card ──────────────────────────────────────────────────────

@Composable
private fun BookingCard(
    booking: Booking,
    property: Property?,
    onViewProperty: () -> Unit,
    onCancelBooking: () -> Unit,
    isOwnerView: Boolean = false,
    onUpdateStatus: ((String) -> Unit)? = null,
) {
    var showCancelConfirm by remember { mutableStateOf(false) }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title   = { Text("Cancel Visit?", fontWeight = FontWeight.Bold) },
            text    = { Text("Are you sure you want to cancel this visit booking?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { onCancelBooking(); showCancelConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusRejected),
                ) { Text("Yes, Cancel", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirm = false }) { Text("Keep") }
            },
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Property image + title ───────────────────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                if (property != null) {
                    AsyncImage(
                        model              = property.images.firstOrNull() ?: "",
                        contentDescription = property.title,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceGray),
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceGray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Home, null, tint = BorderColor, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = property?.title ?: "Property",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = TextPrimary,
                        maxLines   = 2,
                    )
                    Spacer(Modifier.height(2.dp))
                    if (property != null) {
                        Text(
                            "${property.neighborhood.orEmpty().ifBlank { property.district.orEmpty() }}, ${property.district.orEmpty()}",
                            fontSize = 12.sp,
                            color    = TextSecondary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            property.priceDisplay,
                            fontSize   = 13.sp,
                            color      = NestXBlue,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Status badge (top-right)
                StatusBadge(status = booking.status)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)

            // ── Visit date + time ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BookingInfoChip(
                    icon  = Icons.Default.CalendarToday,
                    label = "Visit Date",
                    value = formatDate(booking.visitDate),
                )
                BookingInfoChip(
                    icon  = Icons.Default.Schedule,
                    label = "Time",
                    value = booking.visitTime ?: "--",
                )
                BookingInfoChip(
                    icon  = Icons.Default.ConfirmationNumber,
                    label = "Ref",
                    value = property?.referenceId?.takeLast(5) ?: "--",
                )
            }

            // ── Message (if any) ─────────────────────────────────────────────
            if (!booking.message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceGray,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Default.Chat, null, tint = TextSecondary, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                        Text(booking.message, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                }
            }

            // ── Action buttons ───────────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isOwnerView) {
                    if (booking.status == "pending") {
                        Button(
                            onClick  = { onUpdateStatus?.invoke("confirmed") },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = StatusApproved)
                        ) {
                            Text("Confirm Visit", fontSize = 13.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick  = { onUpdateStatus?.invoke("cancelled") },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Text("Decline", fontSize = 13.sp, color = TextSecondary)
                        }
                    } else {
                        if (property != null) {
                            OutlinedButton(
                                onClick  = onViewProperty,
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(8.dp),
                            ) {
                                Text("View Property Detail", fontSize = 13.sp, color = NestXBlue)
                            }
                        }
                    }
                } else {
                    if (property != null) {
                        OutlinedButton(
                            onClick  = onViewProperty,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            Text("View Property", fontSize = 13.sp, color = NestXBlue)
                        }
                    }

                    if (booking.status == "confirmed" || booking.status == "pending") {
                        OutlinedButton(
                            onClick  = { showCancelConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, StatusRejected.copy(alpha = 0.5f)),
                        ) {
                            Text("Cancel Visit", fontSize = 13.sp, color = StatusRejected)
                        }
                    }
                }
            }
        }
    }
}

// ── Status badge ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status.lowercase()) {
        "confirmed"  -> "✓ Confirmed"  to StatusApproved
        "pending"    -> "⏳ Pending"    to StatusPending
        "cancelled"  -> "✗ Cancelled"  to StatusRejected
        "completed"  -> "🏁 Completed"  to NestXBlue
        else         -> status.replaceFirstChar { it.uppercase() } to TextSecondary
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color    = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ── Small info chip ───────────────────────────────────────────────────────────

@Composable
private fun BookingInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, tint = NestXBlue, modifier = Modifier.size(13.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

// ── Date formatter ────────────────────────────�