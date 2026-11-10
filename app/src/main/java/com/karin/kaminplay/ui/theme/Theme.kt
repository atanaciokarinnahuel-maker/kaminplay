package com.karin.kaminplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

@Composable
fun KaminTheme(darkTheme: Boolean, fonts: KaminFonts = SystemKaminFonts, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkKaminColors else LightKaminColors
    val type = remember(fonts) { kaminType(fonts) }
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.accentInk,
            background = colors.bg, onBackground = colors.fg,
            surface = colors.surface, onSurface = colors.fg,
            surfaceVariant = colors.surface2, onSurfaceVariant = colors.muted,
            outline = colors.line,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.accentInk,
            background = colors.bg, onBackground = colors.fg,
            surface = colors.surface, onSurface = colors.fg,
            surfaceVariant = colors.surface2, onSurfaceVariant = colors.muted,
            outline = colors.line,
        )
    }
    CompositionLocalProvider(
        LocalKaminColors provides colors,
        LocalKaminFonts provides fonts,
        LocalKaminType provides type,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Shorthand: `Kamin.colors.fg`, `Kamin.type.h2`. */
object Kamin {
    val colors: KaminColors
        @Composable @ReadOnlyComposable get() = LocalKaminColors.current
    val type: KaminType
        @Composable @ReadOnlyComposable get() = LocalKaminType.current
    val fonts: KaminFonts
        @Composable @ReadOnlyComposable get() = LocalKaminFonts.current
}
