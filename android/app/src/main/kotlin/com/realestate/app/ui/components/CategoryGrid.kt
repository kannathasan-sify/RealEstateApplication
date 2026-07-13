package com.realestate.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

data class CategoryItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

val homeCategories = listOf(
    CategoryItem("Property for Rent",   Icons.Filled.Apartment,    "rent"),
    CategoryItem("Property for Sale",   Icons.Filled.Home,         "sale"),
    CategoryItem("Off-Plan Properties", Icons.Filled.Business,     "off_plan"),
    CategoryItem("Rooms for Rent",      Icons.Filled.MeetingRoom,  "rooms"),
    CategoryItem("Motors",              Icons.Filled.DirectionsCar,"motors"),
    CategoryItem("Jobs",                Icons.Filled.Work,         "jobs"),
)

// Chunk once at definition time — avoids recomputation on every recomposition
private val categoryRows = homeCategories.chunked(3)

@Composable
fun CategoryGrid(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categoryRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    CategoryCell(
                        item = item,
                        onClick = { onCategoryClick(item.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill remaining cells so columns stay even
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    item: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(item.icon, contentDescription = item.label, tint = PrimaryRed, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.label,
                fontSize = 11.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp,
            )
        }
    }
}
