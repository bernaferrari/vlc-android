/*
 * ************************************************************************
 *  VideoVerticalProgressOverlayHost.kt
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

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme

private data class VideoVerticalProgressState(
    @StringRes val title: Int = R.string.brightness,
    @DrawableRes val icon: Int = R.drawable.ic_player_brightness,
    val value: Int = 0,
    val labelAfterProgress: Boolean = false,
    val isDoubleRange: Boolean = false
)

/**
 * Compose replacement for player_overlay_brightness.xml and
 * player_overlay_volume.xml. The delegate owns gesture/service state; this
 * host owns only the value label, icon, and vertical progress drawing.
 */
internal fun VLCComposeView.installVideoVerticalProgressOverlayHost() {
    val host = VideoVerticalProgressOverlayHost(context)
    setTag(id, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoVerticalProgressOverlayHost(): VideoVerticalProgressOverlayHost =
    getTag(id) as? VideoVerticalProgressOverlayHost ?: error("Missing video vertical progress overlay host")

internal class VideoVerticalProgressOverlayHost(context: Context) {

    private var state by mutableStateOf(VideoVerticalProgressState())
    private val boostColor = Color(ContextCompat.getColor(context, R.color.orange700))

    fun showBrightness(value: Int) {
        state = VideoVerticalProgressState(
            title = R.string.brightness,
            icon = R.drawable.ic_player_brightness,
            value = value.coerceIn(0, 100),
            labelAfterProgress = false,
            isDoubleRange = false
        )
    }

    fun showVolume(value: Int, isDoubleRange: Boolean) {
        state = VideoVerticalProgressState(
            title = R.string.volume,
            icon = R.drawable.ic_player_volume,
            value = value.coerceIn(0, if (isDoubleRange) 200 else 100),
            labelAfterProgress = true,
            isDoubleRange = isDoubleRange
        )
    }

    @Composable
    fun Content() {
        val current = state
        if (current.labelAfterProgress) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ProgressColumn(current, boostColor)
                OverlayTitle(current.title, modifier = Modifier.padding(start = 24.dp, end = 16.dp))
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OverlayTitle(current.title, modifier = Modifier.padding(start = 16.dp, end = 24.dp))
                ProgressColumn(current, boostColor)
            }
        }
    }
}

@Composable
private fun OverlayTitle(
    @StringRes title: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(title),
        color = Color.White,
        fontSize = 16.sp,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ProgressColumn(
    state: VideoVerticalProgressState,
    boostColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${state.value}%",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(8.dp)
        )
        VerticalProgressBar(
            value = state.value,
            isDoubleRange = state.isDoubleRange,
            boostColor = boostColor,
            modifier = Modifier
                .width(16.dp)
                .size(width = 16.dp, height = 120.dp)
        )
        Icon(
            painter = painterResource(state.icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(top = 16.dp)
                .size(32.dp)
        )
    }
}

@Composable
private fun VerticalProgressBar(
    value: Int,
    isDoubleRange: Boolean,
    boostColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val trackWidth = 8.dp.toPx()
        val yOffset = 4.dp.toPx()
        val left = (size.width - trackWidth) / 2F
        val top = yOffset
        val bottom = size.height - yOffset
        val height = bottom - top
        val radius = trackWidth / 2F
        val percent = if (isDoubleRange) value / 200F else value / 100F
        val clampedPercent = percent.coerceIn(0F, 1F)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.5F),
            topLeft = Offset(left, top),
            size = Size(trackWidth, height),
            cornerRadius = CornerRadius(radius, radius)
        )

        if (clampedPercent > 0F) {
            val progressTop = top + height * (1F - clampedPercent)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, progressTop),
                size = Size(trackWidth, bottom - progressTop),
                cornerRadius = CornerRadius(radius, radius)
            )

            if (isDoubleRange && clampedPercent > 0.5F) {
                val boostBottom = top + height * 0.5F
                drawRoundRect(
                    color = boostColor,
                    topLeft = Offset(left, progressTop),
                    size = Size(trackWidth, boostBottom - progressTop),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }
}
