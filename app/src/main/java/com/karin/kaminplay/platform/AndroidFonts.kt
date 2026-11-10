package com.karin.kaminplay.platform

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.karin.kaminplay.R
import com.karin.kaminplay.ui.theme.KaminFonts

/** Bundled fonts, all under the SIL Open Font License (texts in licenses/fonts). */
val AndroidKaminFonts = KaminFonts(
    display = FontFamily(
        Font(R.font.big_shoulders_display_semibold, FontWeight.SemiBold),
        Font(R.font.big_shoulders_display_extrabold, FontWeight.ExtraBold),
    ),
    body = FontFamily(
        Font(R.font.instrument_sans_regular, FontWeight.Normal),
        Font(R.font.instrument_sans_medium, FontWeight.Medium),
        Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
    ),
    mono = FontFamily(
        Font(R.font.dm_mono_regular, FontWeight.Normal),
        Font(R.font.dm_mono_medium, FontWeight.Medium),
    ),
)
