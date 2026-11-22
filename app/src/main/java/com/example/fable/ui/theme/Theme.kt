package com.example.fable.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.fable.domain.model.AppAppearance

private val DarkColorScheme = darkColorScheme(
    primary = FableDarkPrimary,
    onPrimary = Color(0xFF091336),
    primaryContainer = Color(0xFF1F2D5C),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = FableDarkSecondary,
    onSecondary = Color(0xFF121A28),
    secondaryContainer = Color(0xFF1C2534),
    onSecondaryContainer = Color(0xFFDCE5F2),
    tertiary = FableDarkAccent,
    onTertiary = Color(0xFF3A1205),
    tertiaryContainer = Color(0xFF4A2418),
    onTertiaryContainer = Color(0xFFFFDCD0),
    background = FableDarkBackground,
    surface = FableDarkSurface,
    surfaceVariant = FableDarkSurfaceVariant,
    onBackground = FableDarkText,
    onSurface = FableDarkText,
    onSurfaceVariant = FableDarkMuted,
    outline = FableDarkBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF4B1519),
    onErrorContainer = Color(0xFFFECACA)
)

private val LightColorScheme = lightColorScheme(
    primary = FablePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E6FF),
    onPrimaryContainer = Color(0xFF0D1D6E),
    secondary = FableSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E9F7),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = FableAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDE8E0),
    onTertiaryContainer = Color(0xFF5B2412),
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
    appAppearance: AppAppearance = AppAppearance.SYSTEM,
    darkTheme: Boolean = when (appAppearance) {
        AppAppearance.SYSTEM -> isSystemInDarkTheme()
        AppAppearance.LIGHT -> false
        AppAppearance.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
