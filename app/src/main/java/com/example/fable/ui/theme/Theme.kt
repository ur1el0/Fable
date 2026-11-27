package com.example.fable.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = FableDarkPrimary,
    secondary = FableSecondary,
    tertiary = FableDarkAccent,
    background = FableDarkBackground,
    surface = FableDarkSurface,
    surfaceVariant = FableDarkSurfaceVariant,
    onBackground = FableDarkText,
    onSurface = FableDarkText,
    onSurfaceVariant = FableDarkMuted,
    outline = FableDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = FablePrimary,
    secondary = FableSecondary,
    tertiary = FableAccent,
    background = FableBackground,
    surface = FableSurface,
    surfaceVariant = FableSurfaceVariant,
    onBackground = FableText,
    onSurface = FableText,
    onSurfaceVariant = FableMuted,
    outline = FableBorder
)

@Composable
fun FableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
