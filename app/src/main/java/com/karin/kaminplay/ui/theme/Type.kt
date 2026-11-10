package com.karin.kaminplay.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** The three type roles. The Android layer fills them with the bundled fonts. */
@Immutable
data class KaminFonts(
    /** Condensed marquee face: wordmark, headings, poster titles. */
    val display: FontFamily,
    val body: FontFamily,
    /** Years, runtimes and small uppercase labels. */
    val mono: FontFamily,
)

val SystemKaminFonts = KaminFonts(FontFamily.SansSerif, FontFamily.SansSerif, FontFamily.Monospace)

@Immutable
data class KaminType(
    val h1: TextStyle,
    val h2: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val small: TextStyle,
    val smallItalic: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
)

fun kaminType(fonts: KaminFonts): KaminType {
    val body = TextStyle(fontFamily = fonts.body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val small = body.copy(fontSize = 13.sp, lineHeight = 19.sp)
    return KaminType(
        h1 = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 44.sp),
        h2 = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 29.sp),
        title = body.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
        body = body,
        bodyStrong = body.copy(fontWeight = FontWeight.SemiBold),
        small = small,
        smallItalic = small.copy(fontStyle = FontStyle.Italic),
        label = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em),
        mono = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    )
}

val LocalKaminFonts = staticCompositionLocalOf { SystemKaminFonts }
val LocalKaminType = staticCompositionLocalOf { kaminType(SystemKaminFonts) }
