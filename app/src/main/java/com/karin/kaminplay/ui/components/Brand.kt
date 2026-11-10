package com.karin.kaminplay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.ui.theme.Kamin

/** Row of film perforations. With the wordmark it is the one loud element of the app. */
@Composable
fun PerforationStrip(modifier: Modifier = Modifier) {
    val color = Kamin.colors.line
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val hole = 13.dp.toPx()
        val step = 24.dp.toPx()
        val radius = CornerRadius(3.dp.toPx())
        var x = 0f
        while (x < size.width) {
            drawRoundRect(color, Offset(x, 0f), Size(minOf(hole, size.width - x), size.height), radius)
            x += step
        }
    }
}

/** "KAMINPLAY" sized to fill the available width, up to [maxSize]. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, maxSize: Dp = 132.dp) {
    val colors = Kamin.colors
    val display = Kamin.fonts.display
    BoxWithConstraints(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "KAMINPLAY"; heading() }) {
        // Nine condensed capitals take about 4.4 em, so this is the size that fills the row.
        val sizeDp = minOf(maxSize, maxWidth / 4.4f)
        val size = with(LocalDensity.current) { sizeDp.toSp() }
        val style = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = size, lineHeight = size)
        Row {
            Text("KAMIN", style = style, color = colors.fg, maxLines = 1, softWrap = false)
            Text("PLAY", style = style, color = colors.accentText, maxLines = 1, softWrap = false)
        }
    }
}
