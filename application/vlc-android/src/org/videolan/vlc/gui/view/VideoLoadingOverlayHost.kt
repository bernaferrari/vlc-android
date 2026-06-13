/*****************************************************************************
 * VideoLoadingOverlayHost.kt
 *
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
 */

package org.videolan.vlc.gui.view

import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView

internal interface VideoLoadingOverlayHost {
    fun show()
    fun hide()
}

internal fun VLCComposeView.installVideoLoadingOverlayHost() {
    val host = VideoLoadingOverlayController(this)
    setTag(R.id.player_overlay_loading, host)
    visibility = View.INVISIBLE
    isClickable = false
    isFocusable = false
    setContent { VideoLoadingOverlay(visible = host.visible) }
}

internal fun VLCComposeView.videoLoadingOverlayHost(): VideoLoadingOverlayHost =
        getTag(R.id.player_overlay_loading) as? VideoLoadingOverlayHost ?: error("Missing video loading overlay host")

private class VideoLoadingOverlayController(private val view: VLCComposeView) : VideoLoadingOverlayHost {
    var visible by mutableStateOf(false)
        private set

    override fun show() {
        visible = true
        view.visibility = View.VISIBLE
    }

    override fun hide() {
        visible = false
        view.visibility = View.INVISIBLE
    }
}

@Composable
private fun VideoLoadingOverlay(visible: Boolean) {
    if (!visible) return

    val transition = rememberInfiniteTransition(label = "video-loading")
    val rotation by transition.animateFloat(
            initialValue = 0F,
            targetValue = 360F,
            animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
            ),
            label = "video-loading-rotation"
    )

    Box(contentAlignment = Alignment.Center) {
        Image(
                painter = painterResource(R.drawable.ic_cone_o),
                contentDescription = null,
                modifier = Modifier
                        .size(80.dp)
                        .rotate(rotation)
        )
    }
}
