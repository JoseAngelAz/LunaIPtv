package com.lunaiptv.phone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lunaiptv.ui.theme.AccentColor

private fun accentColor(accent: AccentColor, customHex: String, isDark: Boolean): Color {
    val custom = parseCustomHex(customHex)
    if (custom != null) return custom
    return if (isDark) Color(accent.primaryDark) else Color(accent.primaryLight)
}

private fun parseCustomHex(hex: String): Color? {
    val s = hex.trim().removePrefix("#")
    if (s.length != 6 && s.length != 8) return null
    return runCatching {
        if (s.length == 6) Color((0xFF000000L or s.toLong(16)).toInt())
        else Color(s.toLong(16).toInt())
    }.getOrNull()
}

@Composable
fun LunaIPtvPhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.TEAL,
    customAccent: String = "",
    content: @Composable () -> Unit,
) {
    val primary = accentColor(accentColor, customAccent, darkTheme)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            background = Color(0xFF0A0E27),
            surface = Color(0xFF111529),
            surfaceVariant = Color(0xFF1A1E35),
            onSurface = Color(0xFFE2E2E9),
            onSurfaceVariant = Color(0xFFC4C6D0),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.12f),
            onPrimaryContainer = primary,
            background = Color(0xFFF8F9FC),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE8EAF0),
            onSurface = Color(0xFF1A1C20),
            onSurfaceVariant = Color(0xFF44474F),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
