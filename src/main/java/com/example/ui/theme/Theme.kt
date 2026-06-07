package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyanColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = Color.Black,
    secondary = SecondaryCyan,
    onSecondary = Color.Black,
    tertiary = TertiaryCyan,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = OnDarkTextPrimary,
    onSurface = OnDarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkTextSecondary
)

private val MagentaColorScheme = darkColorScheme(
    primary = PrimaryMagenta,
    onPrimary = Color.White,
    secondary = SecondaryMagenta,
    onSecondary = Color.White,
    tertiary = TertiaryMagenta,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = OnDarkTextPrimary,
    onSurface = OnDarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkTextSecondary
)

private val NeonColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color.Black,
    secondary = SecondaryNeon,
    onSecondary = Color.White,
    tertiary = TertiaryNeon,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = OnDarkTextPrimary,
    onSurface = OnDarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkTextSecondary
)

private val AmberColorScheme = darkColorScheme(
    primary = PrimaryAmber,
    onPrimary = Color.Black,
    secondary = SecondaryAmber,
    onSecondary = Color.Black,
    tertiary = TertiaryAmber,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = OnDarkTextPrimary,
    onSurface = OnDarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkTextSecondary
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Neon",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Cyan" -> CyanColorScheme
        "Magenta" -> MagentaColorScheme
        "Neon" -> NeonColorScheme
        "Amber" -> AmberColorScheme
        else -> NeonColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
