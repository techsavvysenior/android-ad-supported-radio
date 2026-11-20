package com.example.opendial.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// "Late-night radio": deep navy, an ember accent for what's live, amber and teal as supporting hues.
val Ember = Color(0xFFFF7A59)
val Amber = Color(0xFFFFB547)
val Teal = Color(0xFF4FD1C5)

private val Dark = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF2A0D05),
    primaryContainer = Color(0xFF4A1D12),
    onPrimaryContainer = Color(0xFFFFDBD1),
    secondary = Amber,
    onSecondary = Color(0xFF2B1A00),
    secondaryContainer = Color(0xFF3D2A0A),
    onSecondaryContainer = Color(0xFFFFE1B3),
    tertiary = Teal,
    onTertiary = Color(0xFF00201D),
    background = Color(0xFF0B0F1A),
    onBackground = Color(0xFFEEF1F8),
    surface = Color(0xFF0B0F1A),
    onSurface = Color(0xFFEEF1F8),
    surfaceVariant = Color(0xFF1E2740),
    onSurfaceVariant = Color(0xFFA3ADC6),
    surfaceContainerLowest = Color(0xFF080B14),
    surfaceContainerLow = Color(0xFF10172A),
    surfaceContainer = Color(0xFF141C31),
    surfaceContainerHigh = Color(0xFF1A233B),
    surfaceContainerHighest = Color(0xFF212B46),
    outline = Color(0xFF3A4560),
    outlineVariant = Color(0xFF283250),
)

private val Light = lightColorScheme(
    primary = Color(0xFFD9502E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3B0D02),
    secondary = Color(0xFFB87400),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1B3),
    onSecondaryContainer = Color(0xFF2B1A00),
    tertiary = Color(0xFF0F8C80),
    background = Color(0xFFFBF8F6),
    onBackground = Color(0xFF141826),
    surface = Color(0xFFFBF8F6),
    onSurface = Color(0xFF141826),
    surfaceVariant = Color(0xFFEDE7E3),
    onSurfaceVariant = Color(0xFF5A6275),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F2EF),
    surfaceContainer = Color(0xFFF2ECE8),
    surfaceContainerHigh = Color(0xFFECE5E1),
    surfaceContainerHighest = Color(0xFFE6DFDB),
    outline = Color(0xFFC9C0BA),
    outlineVariant = Color(0xFFE0D8D3),
)

private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
)

/** Off in screenshot tests, so infinite animations don't keep the test clock busy. */
val LocalAnimationsEnabled = compositionLocalOf { true }

/** Off in screenshot tests, which have no network for station logos. */
val LocalLoadArtwork = compositionLocalOf { true }

@Composable
fun OpenDialTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
