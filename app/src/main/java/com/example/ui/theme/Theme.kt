package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandTeal,
    onPrimary = MidnightNavy,
    primaryContainer = BrandTealContainer,
    onPrimaryContainer = BrandTealLight,
    secondary = BrandTealLight,
    onSecondary = MidnightNavy,
    tertiary = StatusAmber,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = LightSlate,
    outline = BorderDark,
    error = StatusCoral,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0FAF3),
    onPrimaryContainer = Color(0xFF003831),
    secondary = DeepBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = DeepBlue,
    tertiary = StatusAmber,
    onTertiary = MidnightNavy,
    background = LightBackground,
    onBackground = MidnightNavy,
    surface = LightSurface,
    onSurface = MidnightNavy,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = LightBorder,
    error = StatusCoral,
    onError = Color.White
)

@Composable
fun MerchSpringTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
