package com.lunaiptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * Available LunaIPtv themes. Persisted via DataStore and selectable from Settings → Theme.
 * SYSTEM follows the platform dark/light setting.
 */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.DARK }

/** Map the resolved LunaIPtv tokens onto a tv-material3 M3 [ColorScheme]. */
private fun schemeFrom(c: LunaIPtvColors): ColorScheme =
    if (c.isDark) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary,
            onSecondary = c.onSecondary,
            secondaryContainer = c.secondaryContainer,
            onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary,
            onTertiary = c.onTertiary,
            tertiaryContainer = c.tertiaryContainer,
            onTertiaryContainer = c.onTertiaryContainer,
            background = c.background,
            onBackground = c.onSurface,
            surface = c.surface,
            onSurface = c.onSurface,
            surfaceVariant = c.surfaceContainerHigh,
            onSurfaceVariant = c.onSurfaceVariant,
            border = c.outline,
            error = c.favorite,
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary,
            onSecondary = c.onSecondary,
            secondaryContainer = c.secondaryContainer,
            onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary,
            onTertiary = c.onTertiary,
            tertiaryContainer = c.tertiaryContainer,
            onTertiaryContainer = c.onTertiaryContainer,
            background = c.background,
            onBackground = c.onSurface,
            surface = c.surface,
            onSurface = c.onSurface,
            surfaceVariant = c.surfaceContainerHigh,
            onSurfaceVariant = c.onSurfaceVariant,
            border = c.outline,
            error = c.favorite,
        )
    }

@Composable
fun LunaIPtvTheme(
    themeMode: ThemeMode,
    accent: AccentColor,
    systemInDarkTheme: Boolean,
    customAccent: String = "",
    animationLevel: AnimationLevel = AnimationLevel.FULL,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

    val colors = LunaIPtvColors(isDark = useDark, accent = accent, customAccent = customAccent)

    CompositionLocalProvider(
        LocalLunaIPtvColors provides colors,
        LocalThemeMode provides themeMode,
        LocalAnimationLevel provides animationLevel,
    ) {
        MaterialTheme(
            colorScheme = schemeFrom(colors),
            typography = LunaIPtvTypography,
            content = content,
        )
    }
}

/** Convenience accessor: `LunaIPtvTheme.colors.focusBorder`. */
object LunaIPtvTheme {
    val colors: LunaIPtvColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLunaIPtvColors.current
}
