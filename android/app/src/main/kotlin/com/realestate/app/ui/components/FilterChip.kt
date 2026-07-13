package com.realestate.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.BorderColor
import com.realestate.app.ui.theme.OnboardingBlob
import com.realestate.app.ui.theme.PrimaryRed
import com.realestate.app.ui.theme.TextPrimary
import com.realestate.app.ui.theme.TextSecondary

@Composable
fun RealEstateFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor   = if (selected) OnboardingBlob else Color.White
    val textColor = if (selected) PrimaryRed else TextPrimary
    val borderColor = if (selected) PrimaryRed else BorderColor

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
