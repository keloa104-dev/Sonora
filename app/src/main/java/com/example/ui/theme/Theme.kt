package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AccentColorOption
import com.example.model.DarkModeOption

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = PrimaryViolet,
    secondary = SecondaryPink,
    onSecondary = Color(0xFF332D41),
    tertiary = Color(0xFFFFB4AB),
    background = DeepBackground,
    onBackground = OnSurfaceWhite,
    surface = SurfaceDark,
    onSurface = OnSurfaceWhite,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMuted,
    outline = GlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = PrimaryViolet,
    onSecondary = Color.White,
    tertiary = SecondaryPink,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = Color(0xFFCBD5E1)
)

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.5f
}

fun getAccentColor(option: AccentColorOption, isDark: Boolean): Color {
    return when (option) {
        AccentColorOption.PURPLE -> if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
        AccentColorOption.CYAN -> if (isDark) Color(0xFF80DEEA) else Color(0xFF00838F)
        AccentColorOption.GREEN -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
        AccentColorOption.ORANGE -> if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100)
        AccentColorOption.ROSE -> if (isDark) Color(0xFFF48FB1) else Color(0xFFC2185B)
        AccentColorOption.BLUE -> if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
        AccentColorOption.RED -> if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
        else -> Color(option.hexCode)
    }
}

@Composable
fun MusicPlayerTheme(
    darkModeOption: DarkModeOption = DarkModeOption.DARK,
    accentColorOption: AccentColorOption = AccentColorOption.PURPLE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkModeOption) {
        DarkModeOption.SYSTEM -> isSystemInDarkTheme()
        DarkModeOption.DARK -> true
        DarkModeOption.LIGHT -> false
    }

    val accentPrimary = getAccentColor(accentColorOption, darkTheme)
    val onPrimaryColor = if (isLightColor(accentPrimary)) Color.Black else Color.White
    val isBotanical = accentColorOption.category != "Vibrant"

    val colorScheme = if (darkTheme) {
        if (isBotanical && accentColorOption.category == "Deep Shades") {
            // Earthy dark scheme
            DarkColorScheme.copy(
                primary = accentPrimary,
                onPrimary = onPrimaryColor,
                secondary = accentPrimary,
                onSecondary = onPrimaryColor,
                primaryContainer = accentPrimary.copy(alpha = 0.3f),
                onPrimaryContainer = accentPrimary,
                background = Color(0xFF1B1917),
                surface = Color(0xFF262320),
                surfaceVariant = Color(0xFF332F2B)
            )
        } else {
            DarkColorScheme.copy(
                primary = accentPrimary,
                onPrimary = onPrimaryColor,
                secondary = accentPrimary,
                onSecondary = onPrimaryColor,
                primaryContainer = accentPrimary.copy(alpha = 0.25f),
                onPrimaryContainer = accentPrimary
            )
        }
    } else {
        if (isBotanical) {
            // Earthy, muted paper/terracotta daytime scheme as suggested for Android 17
            LightColorScheme.copy(
                primary = accentPrimary,
                onPrimary = onPrimaryColor,
                secondary = accentPrimary,
                onSecondary = onPrimaryColor,
                primaryContainer = accentPrimary.copy(alpha = 0.2f),
                onPrimaryContainer = accentPrimary,
                background = Color(0xFFF4F0E8), // Sandy earthy paper
                surface = Color(0xFFEAE4D8),    // Natural limestone tone
                surfaceVariant = Color(0xFFDFD8C9),
                onBackground = Color(0xFF2B2620),
                onSurface = Color(0xFF2B2620)
            )
        } else {
            LightColorScheme.copy(
                primary = accentPrimary,
                onPrimary = onPrimaryColor,
                secondary = accentPrimary,
                onSecondary = onPrimaryColor,
                primaryContainer = accentPrimary.copy(alpha = 0.2f),
                onPrimaryContainer = accentPrimary
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
