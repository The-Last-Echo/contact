package com.thelastecho.contact.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.thelastecho.contact.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF456B58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E9D8),
    onPrimaryContainer = Color(0xFF10281B),
    secondary = Color(0xFF735A42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2E1CE),
    onSecondaryContainer = Color(0xFF291A0D),
    background = Color(0xFFF7F6F2),
    onBackground = Color(0xFF1B1C19),
    surface = Color(0xFFF7F6F2),
    onSurface = Color(0xFF1B1C19),
    surfaceVariant = Color(0xFFE8E6DF),
    onSurfaceVariant = Color(0xFF494A44),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D0B5),
    onPrimary = Color(0xFF173724),
    primaryContainer = Color(0xFF2E513A),
    onPrimaryContainer = Color(0xFFC4E8CE),
    secondary = Color(0xFFE2C4A5),
    onSecondary = Color(0xFF402C17),
    secondaryContainer = Color(0xFF59432B),
    onSecondaryContainer = Color(0xFFFFDCB8),
    background = Color(0xFF151714),
    onBackground = Color(0xFFE4E4DE),
    surface = Color(0xFF151714),
    onSurface = Color(0xFFE4E4DE),
    surfaceVariant = Color(0xFF42443E),
    onSurfaceVariant = Color(0xFFC4C7BE),
)

@Composable
fun ContactTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}
