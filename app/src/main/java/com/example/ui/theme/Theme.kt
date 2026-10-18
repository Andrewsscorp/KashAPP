package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.core.ui.adaptive.LocalKashAdaptiveInfo
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo

private val LightColorScheme = lightColorScheme(
    primary = KashBlue,
    onPrimary = Color.White,
    primaryContainer = KashSurfaceTint,
    onPrimaryContainer = KashNavy,
    secondary = KashCyan,
    onSecondary = Color.White,
    background = KashLightBg,
    onBackground = KashTextPrimary,
    surface = KashCardBg,
    onSurface = KashTextPrimary,
    surfaceVariant = KashLightBg,
    onSurfaceVariant = KashTextSecondary,
    outline = KashBorder,
    error = KashError
)

private val DarkColorScheme = darkColorScheme(
    primary = KashCyan,
    onPrimary = KashNavy,
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = KashCyanLight,
    onSecondary = KashNavy,
    background = Color(0xFF090D16),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171)
)

@Composable
fun KashAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val adaptiveInfo = rememberKashAdaptiveInfo()

    CompositionLocalProvider(
        LocalKashAdaptiveInfo provides adaptiveInfo
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
