package com.realestate.app.ui.property

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.data.models.Amenity
import com.realestate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmenitiesScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedAmenities = remember { mutableStateListOf<String>() }

    val filteredAmenities = Amenity.values().filter {
        searchQuery.isBlank() || it.displayName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Amenities", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { selectedAmenities.clear() }) {
                        Text("Clear All", color = TextSecondary, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("176,351 Results", color = TextSecondary, fontSize = 14.sp)
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = PrimaryRed,
                ),
            )
            Spacer(Modifier.height(12.dp))

            // 2-column amenity list
            val chunkedAmenities = filteredAmenities.chunked(2)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(chunkedAmenities) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { index, amenity ->
                            AmenityCheckRow(
                                amenity = amenity,
                                checked = amenity.name in selectedAmenities,
                                onCheckedChange = { checked ->
                                    if (checked) selectedAmenities.add(amenity.name)
                                    else selectedAmenities.remove(amenity.name)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Divider(color = BorderColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun AmenityCheckRow(
    amenity: Amenity,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryRed,
                checkmarkColor = Color.White,
                uncheckedColor = TextSecondary,
            ),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = amenity.displayName,
            fontSize = 13.sp,
            color = if (checked) PrimaryRed else TextPrimary,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
