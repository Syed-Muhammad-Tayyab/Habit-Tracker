package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Extra design tokens that Material's colour scheme does not cover. */
@Immutable
data class HabitColors(
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val card: Color,
    val cardStroke: Color,
    val track: Color,
    val accent: Color,
    val accentSoft: Color,
    val navBar: Color,
    val shadow: Color,
    val isDark: Boolean
) {
    val backgroundBrush: Brush
        get() = Brush.verticalGradient(listOf(bgTop, bgMid, bgBottom))
}

private val LightHabitColors = HabitColors(
    bgTop = Color(0xFFD3EBF8),
    bgMid = Color(0xFFEAF3F8),
    bgBottom = Color(0xFFFBF1E8),
    card = Color(0xE6FFFFFF),
    cardStroke = Color(0xFFFFFFFF),
    track = Color(0xFFE3EBF1),
    accent = BrandBlue,
    accentSoft = BrandBlueSoft,
    navBar = Color(0xFFFFFFFF),
    shadow = Color(0x1A1E5A80),
    isDark = false
)

private val DarkHabitColors = HabitColors(
    bgTop = Color(0xFF10202D),
    bgMid = DarkBg,
    bgBottom = Color(0xFF0A0E14),
    card = Color(0xE616202B),
    cardStroke = Color(0xFF263544),
    track = Color(0xFF2A3947),
    accent = DarkPrimary,
    accentSoft = Color(0xFF123247),
    navBar = Color(0xFF111A23),
    shadow = Color(0x66000000),
    isDark = true
)

private val AmberHabitColors = HabitColors(
    bgTop = Color(0xFF2B1A10),
    bgMid = AmberBg,
    bgBottom = Color(0xFF120B08),
    card = Color(0xE62A1C15),
    cardStroke = Color(0xFF4F3324),
    track = Color(0xFF4A3426),
    accent = AmberPrimary,
    accentSoft = Color(0xFF4A2A15),
    navBar = Color(0xFF21150F),
    shadow = Color(0x66000000),
    isDark = true
)

val LocalHabitColors = staticCompositionLocalOf { LightHabitColors }

private val LightPalette = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = Color(0xFF0B5A82),
    secondary = BrandGreen,
    onSecondary = Color.White,
    tertiary = BrandOrange,
    background = Color(0xFFEAF3F8),
    onBackground = LightText,
    surface = Color.White,
    onSurface = LightText,
    surfaceVariant = Color(0xFFEFF4F8),
    onSurfaceVariant = LightTextDim,
    outline = LightBorder
)

private val DarkPalette = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF06131C),
    primaryContainer = Color(0xFF123247),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF5ED79A),
    onSecondary = Color(0xFF07140E),
    tertiary = Color(0xFFF7A07F),
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextDim,
    outline = DarkBorder
)

private val AmberPalette = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF1B0B04),
    primaryContainer = Color(0xFF5A2209),
    onPrimaryContainer = Color(0xFFFFD8BF),
    secondary = Color(0xFFFFB703),
    onSecondary = Color(0xFF1E1000),
    tertiary = Color(0xFF6FDB9A),
    background = AmberBg,
    onBackground = AmberText,
    surface = AmberSurface,
    onSurface = AmberText,
    surfaceVariant = AmberSurfaceVariant,
    onSurfaceVariant = AmberTextDim,
    outline = AmberBorder
)

@Composable
fun HabitTrackerTheme(
    themeKey: String = "light",
    content: @Composable () -> Unit
) {
    val (scheme, extras) = when (themeKey) {
        "black" -> DarkPalette to DarkHabitColors
        "orange" -> AmberPalette to AmberHabitColors
        else -> LightPalette to LightHabitColors
    }

    CompositionLocalProvider(LocalHabitColors provides extras) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}

/** Shortcut: `habitColors.card` inside composables. */
val habitColors: HabitColors
    @androidx.compose.runtime.Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = LocalHabitColors.current
