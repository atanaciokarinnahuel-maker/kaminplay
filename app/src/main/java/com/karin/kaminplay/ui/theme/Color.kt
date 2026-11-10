package com.karin.kaminplay.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.karin.kaminplay.core.Tint

@Immutable
data class KaminColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val fg: Color,
    val muted: Color,
    /** Fill for the primary action. */
    val accent: Color,
    /** Text and icons placed on [accent]. */
    val accentInk: Color,
    /** Accent used as text or outline on [bg]. */
    val accentText: Color,
    val amber: Color,
    val blue: Color,
    val rose: Color,
    val green: Color,
    val violet: Color,
    /** Posters keep their own dark palette in both themes. */
    val posterDark: Color,
    val posterLight: Color,
) {
    fun tint(tint: Tint): Color = when (tint) {
        Tint.AMBER -> amber
        Tint.BLUE -> blue
        Tint.ROSE -> rose
        Tint.GREEN -> green
        Tint.VIOLET -> violet
    }
}

val DarkKaminColors = KaminColors(
    isDark = true,
    bg = Color(0xFF14111A),
    surface = Color(0xFF1E1A26),
    surface2 = Color(0xFF2A2434),
    line = Color(0xFF3B3348),
    fg = Color(0xFFF1EBE0),
    muted = Color(0xFFA8A0B4),
    accent = Color(0xFFF0B23E),
    accentInk = Color(0xFF221604),
    accentText = Color(0xFFF0B23E),
    amber = Color(0xFFE9A93A),
    blue = Color(0xFF5F93DC),
    rose = Color(0xFFDC7890),
    green = Color(0xFF72B88D),
    violet = Color(0xFFA08AD8),
    posterDark = Color(0xFF0D0B12),
    posterLight = Color(0xFFF6EFE2),
)

val LightKaminColors = DarkKaminColors.copy(
    isDark = false,
    bg = Color(0xFFEEECF3),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE3E0EB),
    line = Color(0xFFCDC8D8),
    fg = Color(0xFF1B1722),
    muted = Color(0xFF5D566B),
    accent = Color(0xFFE7A52C),
    accentText = Color(0xFF8A5800),
)

val LocalKaminColors = staticCompositionLocalOf { DarkKaminColors }
