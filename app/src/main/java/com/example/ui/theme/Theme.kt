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
    primary = BcaBlue,
    onPrimary = Color.White,
    primaryContainer = BcaSky,
    onPrimaryContainer = BcaNavy,
    secondary = BcaBlueLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEDFB),
    onSecondaryContainer = Color(0xFF0A3A6B),
    tertiary = BcaRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = BcaRedDark,
    background = SlateLightBackground,
    onBackground = SlateLightOnSurface,
    surface = SlateLightSurface,
    onSurface = SlateLightOnSurface,
    surfaceVariant = Color(0xFFE6ECF3),
    onSurfaceVariant = Color(0xFF4A5A70),
    surfaceTint = BcaBlue,
    outline = Color(0xFF7A8AA0),
    outlineVariant = Color(0xFFD6DEE8),
    error = BcaRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = BcaRedDark
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DB1E8),
    onPrimary = Color(0xFF062342),
    primaryContainer = Color(0xFF0A3A6B),
    onPrimaryContainer = Color(0xFFD3E7F7),
    secondary = Color(0xFF8FC4EC),
    onSecondary = Color(0xFF072C50),
    secondaryContainer = Color(0xFF1B3A5E),
    onSecondaryContainer = Color(0xFFD3E7F7),
    tertiary = Color(0xFFFF8A82),
    onTertiary = Color(0xFF5A110C),
    tertiaryContainer = Color(0xFF7A241E),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = SlateDarkBackground,
    onBackground = SlateDarkOnSurface,
    surface = SlateDarkSurface,
    onSurface = SlateDarkOnSurface,
    surfaceVariant = Color(0xFF24303F),
    onSurfaceVariant = Color(0xFFB7C4D4),
    surfaceTint = Color(0xFF6DB1E8),
    outline = Color(0xFF8A9BAE),
    outlineVariant = Color(0xFF2E3B4C),
    error = Color(0xFFFF8A82),
    onError = Color(0xFF5A110C),
    errorContainer = Color(0xFF7A241E),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false so the BCA brand palette is consistent on every device.
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
