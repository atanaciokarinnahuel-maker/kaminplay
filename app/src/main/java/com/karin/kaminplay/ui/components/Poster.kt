package com.karin.kaminplay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.core.Film
import com.karin.kaminplay.core.Motif
import com.karin.kaminplay.ui.theme.Kamin

/**
 * Typographic poster generated from the film's data: genre tint, an abstract motif, the year
 * and the title. Everything scales with the poster's own width, so it works at any size.
 */
@Composable
fun Poster(film: Film, modifier: Modifier = Modifier) {
    val colors = Kamin.colors
    val fonts = Kamin.fonts
    val tint = colors.tint(film.genre.tint)
    val dark = colors.posterDark
    BoxWithConstraints(
        modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(0f to lerp(tint, dark, 0.38f), 0.78f to dark, 1f to dark))
            .clearAndSetSemantics { },
    ) {
        val width = maxWidth
        val density = LocalDensity.current
        fun pct(percent: Float) = with(density) { (width * (percent / 100f)).toSp() }

        Canvas(Modifier.fillMaxSize()) {
            drawMotif(film.motif, tint)
            // Fade the motif out towards the title block.
            drawRect(Brush.verticalGradient(0.42f to dark.copy(alpha = 0f), 0.72f to dark, 1f to dark))
        }
        Text(
            text = film.year.toString(),
            style = TextStyle(fontFamily = fonts.mono, fontSize = pct(8.5f), lineHeight = pct(10f)),
            color = colors.posterLight,
            modifier = Modifier.align(Alignment.TopStart).padding(start = width * 0.08f, top = width * 0.07f),
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(width * 0.08f),
            verticalArrangement = Arrangement.spacedBy(width * 0.03f),
        ) {
            val titleSize = posterTitlePercent(film.title)
            Text(
                text = film.title.uppercase(),
                style = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.ExtraBold, fontSize = pct(titleSize), lineHeight = pct(titleSize * 0.96f)),
                color = colors.posterLight,
                maxLines = 4,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = film.director,
                style = TextStyle(fontFamily = fonts.body, fontSize = pct(7.5f), lineHeight = pct(9f)),
                color = colors.posterLight.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Title size as a percentage of the poster width: the largest size at which the title,
 * wrapped word by word, fits in three lines. Assumes about 0.47 em per condensed capital.
 */
internal fun posterTitlePercent(title: String): Float {
    val words = title.split(' ').filter { it.isNotEmpty() }
    for (size in 26 downTo 10) {
        val charsPerLine = (84f / (0.47f * size)).toInt()
        var lines = 1
        var used = 0
        var fits = true
        for (word in words) {
            if (word.length > charsPerLine) { fits = false; break }
            val needed = if (used == 0) word.length else used + 1 + word.length
            if (needed <= charsPerLine) {
                used = needed
            } else {
                lines++
                used = word.length
            }
        }
        if (fits && lines <= 3) return size.toFloat()
    }
    return 10f
}

/** Motifs are drawn on a 100 x 150 grid and scaled to the poster. Abstract shapes only. */
private fun DrawScope.drawMotif(motif: Motif, color: Color) {
    val u = size.width / 100f
    fun rect(x: Float, y: Float, w: Float, h: Float, alpha: Float) =
        drawRect(color.copy(alpha = alpha), Offset(x * u, y * u), Size(w * u, h * u))
    fun frame(x: Float, y: Float, w: Float, h: Float, stroke: Float, alpha: Float) =
        drawRect(color.copy(alpha = alpha), Offset(x * u, y * u), Size(w * u, h * u), style = Stroke(stroke * u))
    fun disc(cx: Float, cy: Float, r: Float, alpha: Float) =
        drawCircle(color.copy(alpha = alpha), r * u, Offset(cx * u, cy * u))
    fun ring(cx: Float, cy: Float, r: Float, stroke: Float, alpha: Float) =
        drawCircle(color.copy(alpha = alpha), r * u, Offset(cx * u, cy * u), style = Stroke(stroke * u))
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Float, alpha: Float) =
        drawLine(color.copy(alpha = alpha), Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = stroke * u)

    when (motif) {
        Motif.LUNA -> {
            disc(68f, 46f, 30f, 0.55f); disc(58f, 38f, 6f, 0.5f); disc(78f, 56f, 4f, 0.5f)
        }
        Motif.ESCALERA -> {
            rect(52f, 20f, 12f, 70f, 0.3f); rect(64f, 34f, 12f, 56f, 0.42f)
            rect(76f, 48f, 12f, 42f, 0.55f); rect(88f, 62f, 12f, 28f, 0.7f)
        }
        Motif.RELOJ -> {
            ring(64f, 48f, 28f, 4f, 0.7f)
            line(64f, 48f, 64f, 28f, 4f, 0.8f); line(64f, 48f, 80f, 56f, 4f, 0.8f)
        }
        Motif.RIEL -> {
            line(40f, 96f, 62f, 14f, 3f, 0.7f); line(96f, 96f, 70f, 14f, 3f, 0.7f)
            line(46f, 76f, 90f, 76f, 3f, 0.5f); line(52f, 54f, 83f, 54f, 3f, 0.5f); line(57f, 34f, 77f, 34f, 3f, 0.5f)
        }
        Motif.CIUDAD -> {
            rect(30f, 50f, 14f, 46f, 0.35f); rect(46f, 26f, 16f, 70f, 0.55f)
            rect(64f, 40f, 12f, 56f, 0.4f); rect(78f, 14f, 14f, 82f, 0.65f)
        }
        Motif.OJO -> {
            drawOval(color.copy(alpha = 0.7f), Offset(28f * u, 29f * u), Size(68f * u, 38f * u), style = Stroke(4f * u))
            disc(62f, 48f, 11f, 0.7f)
        }
        Motif.ONDAS -> {
            ring(64f, 52f, 12f, 3f, 0.8f); ring(64f, 52f, 24f, 3f, 0.55f); ring(64f, 52f, 36f, 3f, 0.35f)
        }
        Motif.PERSIANA -> {
            rect(34f, 22f, 60f, 6f, 0.7f); rect(34f, 36f, 60f, 6f, 0.6f); rect(34f, 50f, 60f, 6f, 0.5f)
            rect(34f, 64f, 60f, 6f, 0.4f); rect(34f, 78f, 60f, 6f, 0.3f)
        }
        Motif.VENTANA -> {
            frame(44f, 20f, 44f, 60f, 4f, 0.7f)
            line(66f, 20f, 66f, 80f, 3f, 0.6f); line(44f, 50f, 88f, 50f, 3f, 0.6f)
        }
    }
}
