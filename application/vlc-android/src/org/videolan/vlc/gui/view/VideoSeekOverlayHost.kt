/*
 * ************************************************************************
 *  VideoSeekOverlayHost.kt
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

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme

private enum class VideoSeekDirection {
    Rewind,
    Forward
}

private data class VideoSeekOverlayState(
    val seekDirection: VideoSeekDirection = VideoSeekDirection.Forward,
    val seekText: String = "",
    val seekVisible: Boolean = false,
    val seekToken: Int = 0,
    val hideImmediately: Boolean = false,
    val fastPlayVisible: Boolean = false,
    val fastPlayTitle: String = "",
    val tv: Boolean = false
)

/**
 * Compose replacement for player_overlay_seek.xml. VideoTouchDelegate owns
 * gesture timing and playback seeks; this host owns the transient visual
 * overlay for double-tap seek and tap-and-hold fast play.
 */
internal fun VLCComposeView.installVideoSeekOverlayHost() {
    val host = VideoSeekOverlayHost(this)
    setTag(R.id.seekContainer, host)
    isClickable = false
    isFocusable = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoSeekOverlayHost(): VideoSeekOverlayHost =
    getTag(R.id.seekContainer) as? VideoSeekOverlayHost ?: error("Missing video seek overlay host")

internal class VideoSeekOverlayHost(private val view: VLCComposeView) {

    private var state by mutableStateOf(VideoSeekOverlayState())
    private var seekToken by mutableIntStateOf(0)

    fun showSeek(seekForward: Boolean, text: String, tv: Boolean) {
        view.visibility = View.VISIBLE
        state = state.copy(
            seekDirection = if (seekForward) VideoSeekDirection.Forward else VideoSeekDirection.Rewind,
            seekText = text,
            seekVisible = true,
            seekToken = ++seekToken,
            hideImmediately = false,
            tv = tv
        )
    }

    fun hideSeekOverlay(immediate: Boolean) {
        state = state.copy(
            seekVisible = false,
            seekText = if (immediate) "" else state.seekText,
            hideImmediately = immediate
        )
    }

    fun showFastPlay(title: String) {
        view.visibility = View.VISIBLE
        state = state.copy(
            fastPlayVisible = true,
            fastPlayTitle = title
        )
    }

    fun hideFastPlay() {
        state = state.copy(fastPlayVisible = false)
    }

    @Composable
    fun Content() {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                SeekDimBackground(visible = state.seekVisible)
                SeekSideOverlay(state)
                FastPlayOverlay(
                    visible = state.fastPlayVisible,
                    title = state.fastPlayTitle,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun SeekDimBackground(visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.35F else 0F,
        animationSpec = tween(durationMillis = 200),
        label = "seekDimAlpha"
    )
    if (alpha > 0F) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(colorResource(R.color.playerbackground))
        )
    }
}

@Composable
private fun SeekSideOverlay(state: VideoSeekOverlayState) {
    val visibleAlpha by animateFloatAsState(
        targetValue = if (state.seekVisible) 1F else 0F,
        animationSpec = tween(durationMillis = if (state.hideImmediately) 0 else 300),
        label = "seekSideAlpha"
    )
    if (visibleAlpha == 0F && state.seekText.isBlank()) return

    val forward = state.seekDirection == VideoSeekDirection.Forward
    val alignment = if (forward) Alignment.CenterEnd else Alignment.CenterStart
    val horizontalPadding = if (forward) Modifier.padding(end = if (state.tv) 24.dp else 32.dp) else Modifier.padding(start = if (state.tv) 24.dp else 32.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.tv) {
            TvSeekPanel(
                forward = forward,
                text = state.seekText,
                token = state.seekToken,
                alpha = visibleAlpha,
                modifier = Modifier
                    .align(alignment)
                    .then(horizontalPadding)
            )
        } else {
            PhoneSeekSideBackground(
                forward = forward,
                alpha = visibleAlpha,
                token = state.seekToken,
                modifier = Modifier
                    .align(if (forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
            )
            SeekTextAndArrows(
                forward = forward,
                text = state.seekText,
                tv = false,
                token = state.seekToken,
                alpha = visibleAlpha,
                modifier = Modifier
                    .align(alignment)
                    .then(horizontalPadding)
            )
        }
    }
}

@Composable
private fun PhoneSeekSideBackground(
    forward: Boolean,
    alpha: Float,
    token: Int,
    modifier: Modifier = Modifier
) {
    val revealProgress = remember { Animatable(0F) }
    val darkColor = colorResource(R.color.blacktransparent)
    val rippleColor = colorResource(R.color.ripple_white)

    LaunchedEffect(token) {
        if (token == 0) return@LaunchedEffect
        revealProgress.snapTo(0F)
        revealProgress.animateTo(1F, animationSpec = tween(durationMillis = 750, easing = LinearEasing))
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val sideWidth = size.width / 3F
        val start = if (forward) size.width - sideWidth else 0F
        val end = if (forward) size.width else sideWidth
        clipRect(left = start, top = 0F, right = end, bottom = size.height) {
            val centerX = if (forward) end + sideWidth else start - sideWidth
            val centerY = size.height / 2F
            drawCircle(
                color = darkColor.copy(alpha = darkColor.alpha * alpha),
                radius = sideWidth * 2F,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
            drawCircle(
                color = rippleColor.copy(alpha = rippleColor.alpha * alpha),
                radius = sideWidth * 2F * revealProgress.value,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}

@Composable
private fun TvSeekPanel(
    forward: Boolean,
    text: String,
    token: Int,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val shape = if (forward) {
        RoundedCornerShape(topStart = 75.dp, bottomStart = 75.dp)
    } else {
        RoundedCornerShape(topEnd = 75.dp, bottomEnd = 75.dp)
    }
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(colorResource(R.color.playerbackground), shape)
            .padding(horizontal = 32.dp, vertical = 12.dp)
    ) {
        SeekTextAndArrows(
            forward = forward,
            text = text,
            tv = true,
            token = token,
            alpha = 1F
        )
    }
}

@Composable
private fun SeekTextAndArrows(
    forward: Boolean,
    text: String,
    tv: Boolean,
    token: Int,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val firstAlpha = remember { Animatable(0F) }
    val secondAlpha = remember { Animatable(0F) }
    val icon = if (forward) {
        if (tv) R.drawable.ic_half_seek_forward_tv else R.drawable.ic_half_seek_forward
    } else {
        if (tv) R.drawable.ic_half_seek_rewind_tv else R.drawable.ic_half_seek_rewind
    }

    LaunchedEffect(token) {
        if (token == 0) return@LaunchedEffect
        firstAlpha.snapTo(1F)
        secondAlpha.snapTo(0F)
        launch { firstAlpha.animateTo(0F, animationSpec = tween(durationMillis = 500, easing = LinearEasing)) }
        launch {
            secondAlpha.animateTo(
                0F,
                animationSpec = keyframes {
                    durationMillis = 750
                    0F at 0
                    1F at 375
                    0F at 750
                }
            )
        }
    }

    Column(
        modifier = modifier.alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(if (tv) 24.dp else 16.dp)
                    .alpha(if (forward) firstAlpha.value else secondAlpha.value)
            )
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(if (tv) 24.dp else 16.dp)
                    .alpha(if (forward) secondAlpha.value else firstAlpha.value)
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = if (tv) 28.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun FastPlayOverlay(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1F else 0F,
        animationSpec = tween(durationMillis = 200),
        label = "fastPlayAlpha"
    )
    if (alpha == 0F && !visible) return

    val transition = rememberInfiniteTransition(label = "fastPlayArrows")
    val firstArrowAlpha by transition.animateFloat(
        initialValue = 1F,
        targetValue = 0F,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 750
                1F at 0
                0F at 375
                0F at 750
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "fastPlayFirstArrowAlpha"
    )
    val secondArrowAlpha by transition.animateFloat(
        initialValue = 0F,
        targetValue = 0F,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 750
                0F at 0
                1F at 375
                0F at 750
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "fastPlaySecondArrowAlpha"
    )

    Row(
        modifier = modifier
            .alpha(alpha)
            .background(colorResource(R.color.playerbackground), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Icon(
            painter = painterResource(R.drawable.ic_half_seek_forward),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(16.dp)
                .alpha(firstArrowAlpha)
        )
        Icon(
            painter = painterResource(R.drawable.ic_half_seek_forward),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp)
                .alpha(secondArrowAlpha)
        )
    }
}
