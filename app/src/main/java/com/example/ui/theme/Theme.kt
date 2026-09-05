package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PremiumPrimary,
    onPrimary = Color.White,
    primaryContainer = PremiumSurfaceVariant,
    onPrimaryContainer = PremiumPrimaryDark,
    secondary = PremiumSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = PremiumRed,
    onTertiary = Color.White,
    tertiaryContainer = PremiumRedLight,
    onTertiaryContainer = Color(0xFF7F1D1D),
    background = PremiumBackground,
    onBackground = PremiumLightOnSurface,
    surface = PremiumSurface,
    onSurface = PremiumLightOnSurface,
    surfaceVariant = PremiumSurfaceVariant,
    onSurfaceVariant = PremiumLightOnSurfaceVariant,
    surfaceTint = PremiumSurfaceTint,
    outline = PremiumLightOutline,
    outlineVariant = PremiumLightOutlineVariant,
    error = PremiumRed,
    onError = Color.White,
    errorContainer = PremiumRedLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = PremiumDarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = PremiumDarkPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFA5B4FC),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3730A3),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFFCA5A5),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF7F1D1D),
    onTertiaryContainer = Color.White,
    background = PremiumDarkBackground,
    onBackground = PremiumDarkOnSurface,
    surface = PremiumDarkSurface,
    onSurface = PremiumDarkOnSurface,
    surfaceVariant = PremiumDarkSurfaceVariant,
    onSurfaceVariant = PremiumDarkOnSurfaceVariant,
    surfaceTint = PremiumDarkPrimary,
    outline = PremiumDarkOutline,
    outlineVariant = PremiumDarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color.Black,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}