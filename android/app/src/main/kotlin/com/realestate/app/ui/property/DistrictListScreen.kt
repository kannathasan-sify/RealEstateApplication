package com.realestate.app.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.TamilNaduData
import com.realestate.app.ui.theme.*

/** Full-screen grid of Tamil Nadu districts, leading to PropertyListScreen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictListScreen(
    listingType: String,
    onDistrictClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val title = when (listingType) {
        "rent"        -> "Rent — Choose District"
        "sale"        -> "Buy — Choose District"
        "new_project" -> "New Projects — Choose District"
        "land"        -> "Land / Plots — Choose District"
        else          -> "All Tamil Nadu Districts"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite),
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp),
        ) {
            // Featured districts — image cards
            items(TamilNaduData.featuredDistricts) { district ->
                DistrictGridCard(
                    summary = district,
                    onClick = { onDistrictClick(district.name) },
                )
            }

            // Remaining districts — text-only cards
            val shownNames = TamilNaduData.featuredDistricts.map { it.name }
            val remaining  = TamilNaduData.districts.filter { it !in shownNames }
            items(remaining) { name ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clickable { onDistrictClick(name) },
                    shape  = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(name, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DistrictGridCard(
    summary: TamilNaduData.DistrictSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier  = Modifier.fillMaxWidth().height(110.dp).clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = summary.imageUrl,
                contentDescription = summary.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.42f)))
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(summary.name,                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${summary.propertyCount} listings", color = Color.White.copy(0.85f), fontSize = 11.sp)
            }
        }
    }
}
