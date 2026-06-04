/*
 * ************************************************************************
 *  VideoStatsOverlayHost.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 */

package org.videolan.vlc.gui.view

import android.content.res.Configuration
import android.graphics.Paint
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

data class VideoStatEntry(val title: String, val value: String)

data class VideoStatTrackInfo(val title: String, val entries: List<VideoStatEntry>)

private data class VideoStatLine(
    val index: Int,
    val title: String,
    @ColorInt val color: Int,
    val points: List<Pair<Long, Float>> = emptyList()
)

/**
 * Compose-backed replacement for the video HUD statistics panel.
 * The delegate keeps ownership of media/stat sampling and feeds immutable
 * values into this host while the panel owns layout, scrolling, rows, graph,
 * legend, and close affordance rendering.
 */
internal fun VLCComposeView.installVideoStatsOverlayHost() {
    val host = VideoStatsOverlayHost()
    setTag(R.id.stats_container, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoStatsOverlayHost(): VideoStatsOverlayHost =
    getTag(R.id.stats_container) as? VideoStatsOverlayHost ?: error("Missing video stats overlay host")

internal class VideoStatsOverlayHost {

    private var lines by mutableStateOf<List<VideoStatLine>>(emptyList())
    private var tracks by mutableStateOf<List<VideoStatTrackInfo>>(emptyList())
    private var closeClick by mutableStateOf<(() -> Unit)?>(null)
    private var scrollingCallback by mutableStateOf<(() -> Unit)?>(null)
    private var idleCallback by mutableStateOf<(() -> Unit)?>(null)

    fun addLine(index: Int, title: String, @ColorInt color: Int) {
        if (lines.any { it.index == index }) return
        lines = lines + VideoStatLine(index, title, color)
    }

    fun addData(index: Int, value: Pair<Long, Float>) {
        lines = lines.map { line ->
            if (line.index == index) line.copy(points = (line.points + value).takeLast(MAX_POINTS))
            else line
        }
    }

    fun clearPlot() {
        lines = lines.map { it.copy(points = emptyList()) }
    }

    fun setTrackInfo(trackInfo: List<VideoStatTrackInfo>) {
        tracks = trackInfo
    }

    fun setOnCloseClickListener(listener: (() -> Unit)?) {
        closeClick = listener
    }

    fun setScrollCallbacks(scrolling: () -> Unit, idle: () -> Unit) {
        scrollingCallback = scrolling
        idleCallback = idle
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Content() {
        val scrollState = rememberScrollState()
        val configuration = LocalConfiguration.current
        val largeLayout = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp >= 600

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE,
                            MotionEvent.ACTION_SCROLL -> scrollingCallback?.invoke()
                            MotionEvent.ACTION_CANCEL,
                            MotionEvent.ACTION_UP -> idleCallback?.invoke()
                        }
                        false
                    }
                    .verticalScroll(
                        state = scrollState,
                        flingBehavior = ScrollableDefaults.flingBehavior()
                    )
                    .padding(start = 8.dp, top = 8.dp, end = 56.dp, bottom = 8.dp)
            ) {
                if (largeLayout) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrackInfoColumn(
                            tracks = tracks,
                            modifier = Modifier.weight(1F)
                        )
                        StatsGraphColumn(
                            lines = lines,
                            modifier = Modifier.width(266.dp)
                        )
                    }
                } else {
                    TrackInfoColumn(tracks = tracks, modifier = Modifier.fillMaxWidth())
                    StatsGraphColumn(lines = lines, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Image(
                painter = painterResource(R.drawable.ic_close_small),
                contentDescription = stringResource(R.string.close),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clickable { closeClick?.invoke() }
                    .padding(12.dp)
            )
        }
    }

    companion object {
        private const val MAX_POINTS = 30
    }
}

@Composable
private fun TrackInfoColumn(
    tracks: List<VideoStatTrackInfo>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        tracks.forEach { track ->
            Text(
                text = track.title,
                color = colorResource(R.color.orange500),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            track.entries.forEach { entry ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = entry.title,
                        color = VLCThemeDefaults.colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1F)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entry.value,
                        color = VLCThemeDefaults.colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1F)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGraphColumn(
    lines: List<VideoStatLine>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.video_information),
            color = colorResource(R.color.orange500),
            style = MaterialTheme.typography.titleMedium
        )
        StatsGraph(
            lines = lines,
            modifier = Modifier
                .padding(top = 16.dp)
                .size(width = 250.dp, height = 140.dp)
        )
        GraphLegend(
            lines = lines,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun StatsGraph(
    lines: List<VideoStatLine>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gridColor = Color.White
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor.toArgb()
        textSize = with(density) { 10.sp.toPx() }
    }

    Canvas(modifier = modifier) {
        val plottedLines = lines.map { it to it.points.sortedBy { point -> point.first } }
        val points = plottedLines.flatMap { it.second }
        val maxY = points.maxOfOrNull { it.second } ?: 0F
        val maxX = points.maxOfOrNull { it.first } ?: 0L
        val minX = points.minOfOrNull { it.first } ?: 0L
        if (maxY <= 0F || maxX <= minX) return@Canvas

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText("${String.format("%.0f", maxY)} kb/s", 10.dp.toPx(), 10.dp.toPx(), textPaint)
            val center = roundedByUnit(maxY / 2F)
            val centerY = size.height * ((maxY - center) / maxY)
            canvas.nativeCanvas.drawText("${String.format("%.0f", center)} kb/s", 10.dp.toPx(), centerY - 2.dp.toPx(), textPaint)

            var index = maxX - 1000
            while (index > minX) {
                val x = (size.width * ((index - minX).toDouble() / (maxX - minX).toDouble())).toFloat()
                val formattedText = "${String.format("%.0f", roundedByUnit((index - maxX).toFloat()) / 1000)}s"
                canvas.nativeCanvas.drawText(formattedText, x - (textPaint.measureText(formattedText) / 2), size.height, textPaint)
                index -= 1000
            }
        }

        val center = roundedByUnit(maxY / 2F)
        val centerY = size.height * ((maxY - center) / maxY)
        drawLine(gridColor, Offset(0F, centerY), Offset(size.width, centerY), strokeWidth = 1.dp.toPx())

        var index = maxX - 1000
        while (index > minX) {
            val x = (size.width * ((index - minX).toDouble() / (maxX - minX).toDouble())).toFloat()
            drawLine(gridColor, Offset(x, 0F), Offset(x, size.height - 12.dp.toPx()), strokeWidth = 1.dp.toPx())
            index -= 1000
        }

        plottedLines.forEach { (line, sortedPoints) ->
            sortedPoints.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = Color(line.color),
                    start = graphOffset(start, maxY, minX, maxX, size.width, size.height),
                    end = graphOffset(end, maxY, minX, maxX, size.width, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun GraphLegend(
    lines: List<VideoStatLine>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        lines.forEach { line ->
            Row {
                Text(
                    text = line.title,
                    color = Color(line.color),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${String.format("%.0f", line.points.maxByOrNull { it.first }?.second ?: 0F)} kb/s",
                    color = VLCThemeDefaults.colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun graphOffset(
    point: Pair<Long, Float>,
    maxY: Float,
    minX: Long,
    maxX: Long,
    width: Float,
    height: Float
): Offset {
    return Offset(
        x = (width * ((point.first - minX).toDouble() / (maxX - minX).toDouble())).toFloat(),
        y = height * ((maxY - point.second) / maxY)
    )
}

private fun roundedByUnit(number: Float): Float {
    if (number == 0F) return 0F
    val length = log10(kotlin.math.abs(number).toDouble()).toInt()
    return (round(number / (10.0.pow(length.toDouble()))) * (10.0.pow(length.toDouble()))).toFloat()
}
