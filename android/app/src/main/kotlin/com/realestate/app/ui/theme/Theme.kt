package com.realestate.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = NestXBlue,
    onPrimary          = Color.White,
    primaryContainer   = BannerBlue,
    onPrimaryContainer = NestXBlueDark,
    secondary          = NestXBlueAccent,
    onSecondary        = Color.White,
    secondaryContainer = OnboardingBlob,
    tertiary           = NestXBlueLight,
    background         = BackgroundWhite,
    surface            = BackgroundWhite,
    surfaceVariant     = SurfaceGray,
    onBackground       = TextPrimary,
    onSurface          = TextPrimary,
    onSurfaceVariant   = TextSecondary,
    outline            = BorderColor,
)

@Composable
fun RealEstateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content,
    )
}
