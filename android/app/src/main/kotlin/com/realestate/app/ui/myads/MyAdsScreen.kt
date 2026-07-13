package com.realestate.app.ui.myads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.ApprovalStatus
import com.realestate.app.data.models.Property
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAdsScreen(
    viewModel: MyAdsViewModel,
    onBack: () -> Unit,
    onPropertyClick: (String) -> Unit,
    onPostNew: () -> Unit,
) {
    val state      by viewModel.state.collectAsState()
    val properties by viewModel.filteredProperties.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on success / error
    LaunchedEffect(state.deleteSuccess, state.error) {
        val msg = state.deleteSuccess ?: state.error
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Ads", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = NestXBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick       = onPostNew,
                containerColor = NestXBlue,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, "Post New Ad")
            }
        },
        containerColor = SurfaceGray,
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            // ── Tab row ──────────────────────────────────────────────────────
            Surface(color = Color.White, shadowElevation = 2.dp) {
                ScrollableTabRow(
                    selectedTabIndex = MyAdsTab.values().indexOf(state.selectedTab),
                    containerColor   = Color.White,
                    contentColor     = NestXBlue,
                    edgePadding      = 0.dp,
                    indicator        = { tabPositions ->
                        val idx = MyAdsTab.values().indexOf(state.selectedTab)
                        if (idx < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                                color    = NestXBlue,
                                height   = 2.dp,
                            )
                        }
                    },
                ) {
                    MyAdsTab.values().forEach { tab ->
                        val count = when (tab) {
                            MyAdsTab.ALL      -> state.properties.size
                            MyAdsTab.PENDING  -> state.properties.count { it.approvalStatus == ApprovalStatus.PENDING }
                            MyAdsTab.APPROVED -> state.properties.count { it.approvalStatus == ApprovalStatus.APPROVED }
                            MyAdsTab.REJECTED -> state.properties.count { it.approvalStatus == ApprovalStatus.REJECTED }
                        }
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick  = { viewModel.selectTab(tab) },
                            text = {
                                Row(
                                    verticalAlignment      = Alignment.CenterVertically,
                                    horizontalArrangement  = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        tab.label,
                                        fontSize   = 13.sp,
                                        fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    if (count > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (state.selectedTab == tab) NestXBlue else SurfaceGray,
                                        ) {
                                            Text(
                                                "$count",
                                                fontSize  = 10.sp,
                                                color     = if (state.selectedTab == tab) Color.White else TextSecondary,
                                                modifier  = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // ── Body ─────────────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NestXBlue)
                    }
                }

                properties.isEmpty() && !state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Apartment,
                                contentDescription = null,
                                tint     = BorderColor,
                                modifier = Modifier.size(64.dp),
                            )
                            Text(
                                text = when (state.selectedTab) {
                                    MyAdsTab.ALL      -> "No ads posted yet"
                                    MyAdsTab.PENDING  -> "No pending ads"
                                    MyAdsTab.APPROVED -> "No approved ads"
                                    MyAdsTab.REJECTED -> "No rejected ads"
                                },
                                fontSize   = 17.sp,
                                color      = TextSecondary,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Tap the + button to post your first property.",
                                fontSize  = 13.sp,
                                color     = TextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier            = Modifier.fillMaxSize(),
                    ) {
                        items(properties, key = { it.id }) { property ->
                            MyAdCard(
                                property      = property,
                                onClick       = { onPropertyClick(property.id) },
                                onDelete      = { viewModel.deleteProperty(property.id) },
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Ad Card ───────────────────────────────────────────────────────────────────

@Composable
private fun MyAdCard(
    property: Property,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text("Delete Ad?", fontWeight = FontWeight.Bold) },
            text    = { Text("This will permanently remove the listing. This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusRejected),
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(verticalAlignment = Alignment.Top) {

                // ── Thumbnail ────────────────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceGray),
                ) {
                    if (property.images.isNotEmpty()) {
                        AsyncImage(
                            model              = property.images.first(),
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Home, null, tint = BorderColor, modifier = Modifier.size(32.dp))
                        }
                    }
                    // Image count badge
                    if (property.images.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                        ) {
                            Text(
                                "⊞ ${property.images.size}",
                                color    = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Title + approval badge
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text       = property.title.orEmpty(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis,
                            color      = TextPrimary,
                            modifier   = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        ApprovalBadge(status = property.approvalStatus)
                    }

                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${property.neighborhood.orEmpty().ifBlank { property.district.orEmpty() }}, ${property.district.orEmpty()}",
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        property.priceDisplay,
                        fontSize   = 14.sp,
                        color      = NestXBlue,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        buildString {
                            if ((property.bedrooms ?: 0) > 0) append("${property.bedrooms} Bed  ")
                            if ((property.bathrooms ?: 0) > 0) append("${property.bathrooms} Bath  ")
                            if ((property.areaSqft ?: 0.0) > 0.0) append("${property.areaSqft?.toInt() ?: 0} sqft")
                        }.trim(),
                        fontSize = 12.sp,
                        color    = TextSecondary,
                    )
                }
            }

            // ── Rejection reason ─────────────────────────────────────────────
            if (property.approvalStatus == ApprovalStatus.REJECTED && !property.rejectionReason.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusRejected.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Default.Info, null, tint = StatusRejected, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                        Text(
                            "Reason: ${property.rejectionReason}",
                            fontSize   = 12.sp,
                            color      = StatusRejected,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)

            // ── Actions row ──────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Ref ID + date
                Column {
                    Text("Ref: ${property.referenceId.ifBlank { "--" }}", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        property.createdAt.take(10).ifBlank { "—" },
                        fontSize = 11.sp,
                        color    = TextSecondary,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = onClick,
                        shape    = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, NestXBlue.copy(alpha = 0.5f)),
                    ) {
                        Text("View", fontSize = 12.sp, color = NestXBlue)
                    }
                    OutlinedButton(
                        onClick  = { showDeleteConfirm = true },
                        shape    = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, StatusRejected.copy(alpha = 0.4f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                    ) {
                        Text("Delete", fontSize = 12.sp, color = StatusRejected)
                    }
                }
            }
        }
    }
}

// ── Approval status badge ─────────────────────────────────────────────────────

@Composable
private fun ApprovalBadge(status: ApprovalStatus) {
    val (label, bg, fg) = when (status) {
        ApprovalStatus.APPROVED -> Triple("✓ Approved", StatusApproved.copy(alpha = 0.12f), StatusApproved)
        ApprovalStatus.PENDING  -> Triple("⏳ Pending",  StatusPending.copy(alpha = 0.12f),  StatusPending)
        ApprovalStatus.REJECTED -> Triple("✗ Rejected", StatusRejected.copy(alpha = 0.12f), StatusRejected)
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            label,
            fontSize   = 10.sp,
            color      = fg,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}
