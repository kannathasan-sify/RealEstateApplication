package com.realestate.app.ui.service_request

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.RequestUrgency
import com.realestate.app.data.models.ServiceRequest
import com.realestate.app.ui.components.RealEstateFilterChip
import com.realestate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class SortOption(val key: String, val label: String)

private val SORT_OPTIONS = listOf(
    SortOption("newest", "Newest"),
    SortOption("urgent_first", "Urgent First"),
    SortOption("budget_high", "Budget: High to Low"),
    SortOption("budget_low", "Budget: Low to High"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestFeedScreen(
    viewModel: ServiceRequestViewModel,
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    val listState by viewModel.listState.collectAsState()

    var filterCategory by remember { mutableStateOf<String?>(null) } // null = all, "construction", "maintenance"
    var selectedRadius by remember { mutableStateOf<Int?>(null) }    // null = any, 10, 50, 100
    var filterUrgency by remember { mutableStateOf<String?>(null) }  // null = all, "urgent", "emergency"
    var sortBy by remember { mutableStateOf("newest") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(filterCategory, selectedRadius, filterUrgency, sortBy) {
        viewModel.loadServiceRequests(
            category = filterCategory,
            radiusKm = selectedRadius,
            urgency = filterUrgency,
            sortBy = sortBy
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Requests Feed", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = BackgroundWhite,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters row
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter Requests", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)

                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            ) {
                                TextButton(onClick = { sortMenuExpanded = true }) {
                                    Icon(Icons.Default.Sort, null, tint = NestXBlue, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        SORT_OPTIONS.first { it.key == sortBy }.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NestXBlue
                                    )
                                }
                            }
                            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                SORT_OPTIONS.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = { sortBy = option.key; sortMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RealEstateFilterChip(
                            label = "All Category",
                            selected = filterCategory == null,
                            onClick = { filterCategory = null }
                        )
                        RealEstateFilterChip(
                            label = "Construction",
                            selected = filterCategory == "construction",
                            onClick = { filterCategory = "construction" }
                        )
                        RealEstateFilterChip(
                            label = "Maintenance",
                            selected = filterCategory == "maintenance",
                            onClick = { filterCategory = "maintenance" }
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(null to "Any Distance", 10 to "10 km", 50 to "50 km", 100 to "100 km").forEach { (rad, label) ->
                            RealEstateFilterChip(
                                label = label,
                                selected = selectedRadius == rad,
                                onClick = { selectedRadius = rad }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RealEstateFilterChip(
                            label = "Any Urgency",
                            selected = filterUrgency == null,
                            onClick = { filterUrgency = null }
                        )
                        RealEstateFilterChip(
                            label = "🟡 Urgent",
                            selected = filterUrgency == "urgent",
                            onClick = { filterUrgency = "urgent" }
                        )
                        RealEstateFilterChip(
                            label = "🔴 Emergency",
                            selected = filterUrgency == "emergency",
                            onClick = { filterUrgency = "emergency" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (val state = listState) {
                is ServiceRequestUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NestXBlue)
                    }
                }
                is ServiceRequestUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = PrimaryRed)
                    }
                }
                is ServiceRequestUiState.Success -> {
                    if (state.list.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No service requests match your criteria.", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.list) { req ->
                                ServiceRequestCard(req = req, onClick = { onNavigateToDetail(req.id) })
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

/** e.g. "2h ago", "3d ago" — falls back to blank on unparseable dates. */
private fun timeAgo(iso: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val then = sdf.parse(iso)?.time ?: return ""
        val diffMs = System.currentTimeMillis() - then
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h ago"
            days < 30 -> "${days}d ago"
            else -> "${days / 30}mo ago"
        }
    } catch (e: Exception) {
        ""
    }
}

private fun formatPreferredDate(iso: String): String = try {
    val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val outFmt = SimpleDateFormat("d MMM yyyy", Locale.US)
    outFmt.format(inFmt.parse(iso)!!)
} catch (e: Exception) {
    iso
}

@Composable
private fun UrgencyBadge(urgency: RequestUrgency) {
    if (urgency == RequestUrgency.NORMAL) return
    val (bg, fg, icon) = when (urgency) {
        RequestUrgency.EMERGENCY -> Triple(Color(0xFFFFEBEE), StatusRejected, Icons.Default.Warning)
        RequestUrgency.URGENT -> Triple(Color(0xFFFFF8E1), Color(0xFFB26A00), Icons.Default.Bolt)
        else -> Triple(SurfaceGray, TextSecondary, Icons.Default.Bolt)
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(urgency.displayName, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ServiceRequestCard(
    req: ServiceRequest,
    onClick: () -> Unit
) {
    val isConstruction = req.category == "construction"
    val categoryBg = if (isConstruction) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
    val categoryFg = if (isConstruction) Color(0xFFE65100) else Color(0xFF2E7D32)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Thumbnail: first photo if available, else a category avatar
                if (req.images.isNotEmpty()) {
                    AsyncImage(
                        model = req.images.first(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = categoryBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isConstruction) Icons.Default.Construction else Icons.Default.Handyman,
                                contentDescription = null,
                                tint = categoryFg,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(20.dp), color = categoryBg) {
                            Text(
                                text = req.serviceType,
                                color = categoryFg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        UrgencyBadge(req.urgencyEnum)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${req.quotationCount} Quotes",
                            fontSize = 12.sp,
                            color = NestXBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = req.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!req.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = req.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(req.district, fontSize = 12.sp, color = TextSecondary)
                }

                val budget = req.budgetDisplay
                if (budget != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Work, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(budget, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!req.preferredDate.isNullOrBlank() || req.createdAt.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val posted = timeAgo(req.createdAt)
                    if (posted.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Posted $posted", fontSize = 11.sp, color = TextSecondary)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (!req.preferredDate.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = NestXBlue, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
  