package com.ditherpal.ui.theme

import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF3B82F6),
    tertiary = Color(0xFF0EA5E9),
    background = Color(0xFF0F1419),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    error = Color(0xFFEF4444)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B82F6),
    secondary = Color(0xFF0EA5E9),
    tertiary = Color(0xFF60A5FA),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F1419),
    onSurface = Color(0xFF0F1419),
    error = Color(0xFFDC2626)
)

@Composable
fun DitherPalTheme(
    darkTheme: Boolean = isSystemInDarkMode(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
