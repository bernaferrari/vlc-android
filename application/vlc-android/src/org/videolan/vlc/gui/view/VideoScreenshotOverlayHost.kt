/*
 * ************************************************************************
 *  VideoScreenshotOverlayHost.kt
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

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import java.io.File
import kotlin.math.roundToInt

private data class VideoScreenshotOverlayState(
    val token: Int,
    val file: File,
    val bitmap: Bitmap,
    val surfaceLeftPx: Int,
    val surfaceTopPx: Int,
    val sourceWidthPx: Int,
    val sourceHeightPx: Int,
    val screenHeightPx: Int,
    val onShare: (File) -> Unit
)

/**
 * Compose replacement for the former video screenshot overlay XML. The video
 * screenshot delegate supplies a bitmap and this host owns the flash,
 * thumbnail, share action, and fade-out UI.
 */
internal fun VLCComposeView.installVideoScreenshotOverlayHost(onHidden: () -> Unit) {
    val host = VideoScreenshotOverlayHost(onHidden)
    setTag(R.id.player_screenshot_stub, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoScreenshotOverlayHost(): VideoScreenshotOverlayHost =
    getTag(R.id.player_screenshot_stub) as? VideoScreenshotOverlayHost ?: error("Missing video screenshot overlay host")

internal class VideoScreenshotOverlayHost(private val onHidden: () -> Unit) {
    private var overlayState by mutableStateOf<VideoScreenshotOverlayState?>(null)
    private var screenshotToken by mutableIntStateOf(0)
    private var hideToken by mutableIntStateOf(0)

    fun showScreenshot(
        file: File,
        bitmap: Bitmap,
        surfaceBounds: IntArray,
        width: Int,
        height: Int,
        screenHeight: Int,
        onShare: (File) -> Unit
    ) {
        screenshotToken += 1
        overlayState = VideoScreenshotOverlayState(
            token = screenshotToken,
            file = file,
            bitmap = bitmap,
            surfaceLeftPx = surfaceBounds.getOrElse(0) { 0 },
            surfaceTopPx = surfaceBounds.getOrElse(1) { 0 },
            sourceWidthPx = width.coerceAtLeast(1),
            sourceHeightPx = height.coerceAtLeast(1),
            screenHeightPx = screenHeight,
            onShare = onShare
        )
    }

    fun hideScreenshot() {
        if (overlayState != null) hideToken += 1
    }

    @Composable
    fun Content() {
        val state = overlayState ?: return
        val density = LocalDensity.current
        val startX = state.surfaceLeftPx.toFloat()
        val startY = state.surfaceTopPx.toFloat()
        val thumbWidthPx = with(density) { 150.dp.toPx() }
        val paddingPx = with(density) { 16.dp.toPx() }
        val bottomOffsetPx = with(density) { 48.dp.toPx() }
        val actionYOffsetPx = with(density) { 96.dp.toPx() }
        val thumbnailPaddingPx = with(density) { 6.dp.toPx() }
        val thumbnailPaddingDoublePx = with(density) { 12.dp.toPx() }
        val ratio = thumbWidthPx / state.sourceWidthPx.toFloat()
        val thumbHeightPx = state.sourceHeightPx.toFloat() * ratio
        val targetY = state.screenHeightPx.toFloat() - thumbHeightPx - bottomOffsetPx

        val x = remember(state.token) { Animatable(startX) }
        val y = remember(state.token) { Animatable(startY) }
        val scale = remember(state.token) { Animatable(1F) }
        val alpha = remember(state.token) { Animatable(1F) }
        val chromeAlpha = remember(state.token) { Animatable(0F) }
        val flashAlpha = remember(state.token) { Animatable(0F) }

        LaunchedEffect(state.token) {
            coroutineScope {
                launch { flashAlpha.animateTo(1F, tween(durationMillis = 90)) }
                launch {
                    x.animateTo(
                        targetValue = paddingPx,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    y.animateTo(
                        targetValue = targetY,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    scale.animateTo(
                        targetValue = ratio,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                }
            }
            flashAlpha.animateTo(0F, tween(durationMillis = 220))
            chromeAlpha.animateTo(1F, tween(durationMillis = 180))
        }

        LaunchedEffect(hideToken) {
            if (hideToken == 0) return@LaunchedEffect
            coroutineScope {
                launch { y.animateTo(y.value + with(density) { 200.dp.toPx() }, tween(durationMillis = 220)) }
                launch { alpha.animateTo(0F, tween(durationMillis = 220)) }
                launch { chromeAlpha.animateTo(0F, tween(durationMillis = 160)) }
            }
            overlayState = null
            onHidden()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (flashAlpha.value > 0F) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.75F * flashAlpha.value))
                )
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (x.value - thumbnailPaddingPx).roundToInt(),
                            (y.value - thumbnailPaddingPx).roundToInt()
                        )
                    }
                    .size(
                        width = with(density) { (state.sourceWidthPx * scale.value + thumbnailPaddingDoublePx).toDp() },
                        height = with(density) { (state.sourceHeightPx * scale.value + thumbnailPaddingDoublePx).toDp() }
                    )
                    .graphicsLayer { this.alpha = chromeAlpha.value }
                    .background(Color.White, RoundedCornerShape(6.dp))
            )
            Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                    .size(
                        width = with(density) { state.sourceWidthPx.toDp() },
                        height = with(density) { state.sourceHeightPx.toDp() }
                    )
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0F, 0F)
                        this.alpha = alpha.value
                    }
                    .clip(RoundedCornerShape(4.dp))
            )
            ScreenshotActions(
                state = state,
                alpha = chromeAlpha.value,
                yPx = state.screenHeightPx.toFloat() - actionYOffsetPx
            )
        }
    }

    @Composable
    private fun ScreenshotActions(
        state: VideoScreenshotOverlayState,
        alpha: Float,
        yPx: Float
    ) {
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .offset { IntOffset(with(density) { 16.dp.toPx() }.roundToInt(), yPx.roundToInt()) }
                .size(width = 250.dp, height = 56.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(Color.White, RoundedCornerShape(6.dp))
        ) {
            IconButton(
                onClick = { state.onShare(state.file) },
                modifier = Modifier
                    .offset { IntOffset(with(density) { 194.dp.toPx() }.roundToInt(), with(density) { 6.dp.toPx() }.roundToInt()) }
                    .size(44.dp)
                    .background(Color(0xFFFF610A), RoundedCornerShape(4.dp))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
