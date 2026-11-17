package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealNeon,
    onPrimary = TealDeep,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealNeon,
    secondary = TealNeonDark,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TealNeon,
    tertiary = PriorityMedium,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF2E4E55),
    outlineVariant = Color(0xFF1E353A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008975),
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF00382E),
    secondary = TealNeonDark,
    onSecondary = Color.Black,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = PriorityMedium,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFBDD9D4),
    outlineVariant = Color(0xFFE0EFEF)
)

@Composable
fun PlanKaroTheme(
    darkTheme: Boolean = true, // Default to gorgeous dark theme where #00E5BE glows stunningly
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
