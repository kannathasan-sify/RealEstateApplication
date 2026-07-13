package com.realestate.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.PrimaryRed
import com.realestate.app.ui.theme.TextPrimary
import com.realestate.app.ui.theme.TextSecondary
import com.realestate.app.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceRangeSlider(
    minPrice: Float,
    maxPrice: Float,
    absoluteMin: Float = 0f,
    absoluteMax: Float = 10_000_000f,
    onRangeChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var range by remember(minPrice, maxPrice) { mutableStateOf(minPrice..maxPrice) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Min Price", fontSize = 11.sp, color = TextSecondary)
                Text(
                    CurrencyFormatter.short(range.start.toLong()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Max Price", fontSize = 11.sp, color = TextSecondary)
                Text(
                    CurrencyFormatter.short(range.endInclusive.toLong()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        RangeSlider(
            value = range,
            onValueChange = { range = it },   // local state only — no parent notification on every frame
            onValueChangeFinished = { onRangeChange(range.start, range.endInclusive) },
            valueRange = absoluteMin..absoluteMax,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = PrimaryRed, activeTrackColor = PrimaryRed),
        )
    }
}
