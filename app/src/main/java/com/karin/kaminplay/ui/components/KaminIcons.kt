package com.karin.kaminplay.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small original icon set on a 24 x 24 grid, so the app needs no icon library. */
object KaminIcons {
    private fun stroked(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathBuilder = block,
                )
            }
            .build()

    private fun filled(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply { path(fill = SolidColor(Color.Black), pathBuilder = block) }
            .build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
        arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
    }

    val Home: ImageVector = stroked("Home") {
        moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f)
        moveTo(6f, 10f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 10f)
    }
    val Search: ImageVector = stroked("Search") {
        circle(11f, 11f, 6.5f)
        moveTo(16f, 16f); lineTo(20.5f, 20.5f)
    }
    /** Two viewers side by side: the watch-party tab. */
    val Rooms: ImageVector = stroked("Rooms") {
        circle(8.5f, 9f, 3f)
        circle(16.5f, 10f, 2.4f)
        moveTo(3f, 19f); arcToRelative(5.5f, 5.5f, 0f, false, true, 11f, 0f)
        moveTo(15f, 15.2f); arcToRelative(4.6f, 4.6f, 0f, false, true, 6f, 3.8f)
    }
    val Profile: ImageVector = stroked("Profile") {
        circle(12f, 8.5f, 3.5f)
        moveTo(5f, 20f); arcToRelative(7f, 7f, 0f, false, true, 14f, 0f)
    }
    val Back: ImageVector = stroked("Back") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    val Close: ImageVector = stroked("Close") {
        moveTo(6f, 6f); lineTo(18f, 18f)
        moveTo(18f, 6f); lineTo(6f, 18f)
    }
    val External: ImageVector = stroked("External") {
        moveTo(14f, 5f); lineTo(19f, 5f); lineTo(19f, 10f)
        moveTo(19f, 5f); lineTo(11f, 13f)
        moveTo(11f, 7f); lineTo(6f, 7f); lineTo(6f, 18f); lineTo(17f, 18f); lineTo(17f, 13f)
    }
    val Play: ImageVector = filled("Play") { moveTo(8f, 5f); lineTo(8f, 19f); lineTo(19f, 12f); close() }
}
