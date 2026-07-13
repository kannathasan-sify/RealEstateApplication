package com.realestate.app.ui.mysearches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.realestate.app.data.api.SavedSearch
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySearchesScreen(
    viewModel: MySearchesViewModel,
    onBack: () -> Unit,
    onSearchClick: (String, String?) -> Unit,   // (listingType, district?)
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Searches", fontWeight = FontWeight.Bold) },
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
        containerColor = SurfaceGray,
    ) { padding ->

        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = NestXBlue) }
            }

            state.searches.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint     = BorderColor,
                            modifier = Modifier.size(64.dp),
                        )
                        Text(
                            "No saved searches yet",
                            fontSize   = 17.sp,
                            color      = TextSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "When you search for properties and save a search, it will appear here for quick access.",
                            fontSize  = 13.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        Text(
                            "${state.searches.size} saved search${if (state.searches.size != 1) "es" else ""}",
                            fontSize = 13.sp,
                            color    = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.searches, key = { it.id }) { search ->
                        SavedSearchCard(
                            search  = search,
                            onClick = {
                                onSearchClick(
                                    search.listingType ?: "all",
                                    search.filters["district"]?.toString(),
                                )
                            },
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ── Saved-search card ─────────────────────────────────────────────────────────

@Composable
private fun SavedSearchCard(
    search: SavedSearch,
    onClick: () -> Unit,
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier           = Modifier.padding(12.dp),
            verticalAlignment  = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // Thumbnail or placeholder
            Box(
                modifier         = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OnboardingBlob),
                contentAlignment = Alignment.Center,
            ) {
                if (!search.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = search.thumbnailUrl,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Default.Search, null, tint = NestXBlue, modifier = Modifier.size(26.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = search.label ?: "Saved Search",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = search.listingType?.let {
                        it.replaceFirstChar { c -> c.uppercase() }.replace("_", " ")
                    } ?: "All Types",
                    fontSize = 12.sp,
                    color    = TextSecondary,
                )

                // Show active filters summary
                val filterParts = buildList<String> {
                    search.filters["district"]?.toString()?.let { add(it) }
                    search.filters["bedrooms"]?.toString()?.let { add("$it Beds") }
                    search.filters["min_price"]?.let { mn ->
                        search.filters["max_price"]?.let { mx ->
                            add("₹${formatAmount(mn.toString())}–₹${formatAmount(mx.toString())}")
                        }
                    }
                }
                if (filterParts.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        filterParts.take(3).joinToString(" · "),
                        fontSize = 11.sp,
                        color    = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint     = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatAmount(raw: String): String = runCatching {
    val n = raw.toDouble().toLong()
    when {
        n >= 10_000_000 -> "${n / 10_000_000}Cr"
        n >= 100_000    -> "${n / 100_000}L"
        else            -> n.toString()
    }
}.getOrDefault(raw)
