package org.videolan.vlc.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose replacement for the bookmark marker overlays previously populated by
 * BookmarkListDelegate with dynamic ImageViews and ConstraintSet guidelines:
 *   - application/vlc-android/res/layout/audio_player.xml @id/bookmark_marker_container
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/bookmark_marker_container
 *   - application/vlc-android/src/org/videolan/vlc/gui/view/VideoHudOverlayView.kt @id/bookmark_marker_container
 *
 * Marker fractions are normalized against the current media duration by the
 * app-side delegate so this leaf only owns drawing the existing bookmark glyph
 * shape at stable timeline positions.
 */
@Composable
fun VLCBookmarkMarkers(
    markerFractions: List<Float>,
    modifier: Modifier = Modifier,
    markerColor: Color = VLCThemeDefaults.colors.playerIconColor
) {
    val height = if (markerFractions.isEmpty()) 0.dp else 16.dp
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        markerFractions.forEach { fraction ->
            drawBookmarkMarker(
                fraction = fraction.coerceIn(0f, 1f),
                color = markerColor
            )
        }
    }
}

private fun DrawScope.drawBookmarkMarker(fraction: Float, color: Color) {
    val markerSize = 16.dp.toPx()
    val halfMarker = markerSize / 2f
    val centerX = if (size.width <= markerSize) {
        size.width / 2f
    } else {
        (size.width * fraction).coerceIn(halfMarker, size.width - halfMarker)
    }
    val left = centerX - halfMarker
    val top = (size.height - markerSize) / 2f
    val scale = markerSize / 24f

    val path = Path().apply {
        moveTo(left + 9f * scale, top + 4f * scale)
        cubicTo(
            left + 7.8919f * scale,
            top + 4f * scale,
            left + 7f * scale,
            top + 4.892f * scale,
            left + 7f * scale,
            top + 6f * scale
        )
        lineTo(left + 7f * scale, top + 20f * scale)
        lineTo(left + 12f * scale, top + 16.9863f * scale)
        lineTo(left + 17f * scale, top + 20f * scale)
        lineTo(left + 17f * scale, top + 6f * scale)
        cubicTo(
            left + 17f * scale,
            top + 4.892f * scale,
            left + 16.108f * scale,
            top + 4f * scale,
            left + 15f * scale,
            top + 4f * scale
        )
        lineTo(left + 9f * scale, top + 4f * scale)
        close()
    }
    drawPath(path = path, color = color)
}
