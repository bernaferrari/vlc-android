/*
 * ************************************************************************
 *  AudioPlayerTipsHost.kt
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
import android.util.TypedValue
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.audio.AudioPlayerTipsStep

private data class AudioPlayerTipsUiState(
    val visible: Boolean = false,
    val step: AudioPlayerTipsStep? = null,
    @StringRes val title: Int = 0,
    @StringRes val description: Int = 0,
    val isTablet: Boolean = false,
    val playlistIndicatorCenterXPx: Int = -1,
    val stopIndicatorCenterXPx: Int = -1
)

internal fun VLCComposeView.installAudioPlayerTipsHost(context: Context) {
    val host = AudioPlayerTipsHost(context)
    setTag(R.id.audioPlayerTips, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.audioPlayerTipsHost(): AudioPlayerTipsHost =
    getTag(R.id.audioPlayerTips) as? AudioPlayerTipsHost ?: error("Missing audio player tips host")

/**
 * State owner for the audio-player tips overlay. AudioTipsDelegate keeps the
 * playback/tip sequencing contract and pushes only the current visual state.
 */
internal class AudioPlayerTipsHost(context: Context) {

    private var state by mutableStateOf(AudioPlayerTipsUiState())
    private var bottomInsetPx by mutableIntStateOf(0)
    private var onDismissClick: () -> Unit = {}
    private var onNextClick: () -> Unit = {}
    private val tipsBackgroundColor = context.resolveComposeColor(R.attr.background_audio_tips)
    private val primaryColor = context.resolveComposeColor(R.attr.colorPrimary)

    fun setCallbacks(
        onDismiss: () -> Unit,
        onNext: () -> Unit
    ) {
        onDismissClick = onDismiss
        onNextClick = onNext
    }

    fun showTip(
        step: AudioPlayerTipsStep,
        @StringRes title: Int,
        @StringRes description: Int,
        isTablet: Boolean,
        playlistIndicatorCenterXPx: Int,
        stopIndicatorCenterXPx: Int
    ) {
        state = AudioPlayerTipsUiState(
            visible = true,
            step = step,
            title = title,
            description = description,
            isTablet = isTablet,
            playlistIndicatorCenterXPx = playlistIndicatorCenterXPx,
            stopIndicatorCenterXPx = stopIndicatorCenterXPx
        )
    }

    fun setBottomInset(peekHeight: Int) {
        bottomInsetPx = peekHeight.coerceAtLeast(0)
    }

    fun hideTips() {
        state = state.copy(visible = false)
    }

    @Composable
    fun Content() {
        val current = state
        if (!current.visible || current.step == null) return

        val density = LocalDensity.current
        val bottomInset = with(density) { bottomInsetPx.toDp() }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomInset)
                    .background(tipsBackgroundColor)
            )
            Header(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp, end = 16.dp)
            )
            TipCopy(
                state = current,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 72.dp, bottom = bottomInset + 72.dp)
            )
            TipIndicators(
                state = current,
                bottomInset = bottomInset,
                primaryColor = primaryColor,
                backgroundColor = tipsBackgroundColor
            )
        }
    }

    @Composable
    private fun Header(modifier: Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismissClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_small),
                    contentDescription = stringResource(R.string.close),
                    tint = VLCThemeDefaults.colors.fontDefault
                )
            }
            Text(
                text = stringResource(R.string.audio_player_tips),
                color = VLCThemeDefaults.colors.fontDefault,
                fontSize = 20.sp,
                modifier = Modifier
                    .weight(1F)
                    .padding(start = 8.dp)
            )
        }
    }

    @Composable
    private fun TipCopy(
        state: AudioPlayerTipsUiState,
        modifier: Modifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(state.title),
                color = VLCThemeDefaults.colors.fontDefault,
                fontFamily = FontFamily.SansSerif,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(state.description),
                color = VLCThemeDefaults.colors.fontLight,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(
                onClick = onNextClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VLCThemeDefaults.colors.primary,
                    contentColor = VLCThemeDefaults.colors.onPrimary
                )
            ) {
                Text(text = stringResource(if (state.step == AudioPlayerTipsStep.HOLD_STOP) R.string.close else R.string.next_step))
            }
        }
    }

    @Composable
    private fun TipIndicators(
        state: AudioPlayerTipsUiState,
        bottomInset: Dp,
        primaryColor: Color,
        backgroundColor: Color
    ) {
        when (state.step) {
            AudioPlayerTipsStep.SWIPE_NEXT -> {
                if (state.isTablet) {
                    TapIndicatorAt(
                        centerXPx = state.playlistIndicatorCenterXPx,
                        fallbackStart = 48.dp,
                        bottomPadding = bottomInset,
                        primaryColor = primaryColor
                    )
                } else {
                    HorizontalSwipeIndicator(
                        bottomPadding = bottomInset,
                        primaryColor = primaryColor,
                        backgroundColor = backgroundColor
                    )
                }
            }
            AudioPlayerTipsStep.TAP_PLAYLIST -> {
                TapIndicatorAt(
                    centerXPx = state.playlistIndicatorCenterXPx,
                    fallbackStart = 48.dp,
                    bottomPadding = bottomInset,
                    primaryColor = primaryColor
                )
            }
            AudioPlayerTipsStep.HOLD_STOP -> {
                TapIndicatorAt(
                    centerXPx = state.stopIndicatorCenterXPx,
                    fallbackStart = 0.dp,
                    bottomPadding = bottomInset,
                    primaryColor = primaryColor,
                    alignEnd = state.stopIndicatorCenterXPx < 0
                )
            }
            null -> Unit
        }
    }

    @Composable
    private fun HorizontalSwipeIndicator(
        bottomPadding: Dp,
        primaryColor: Color,
        backgroundColor: Color
    ) {
        val transition = rememberInfiniteTransition(label = "audio tips swipe")
        val offset by transition.animateFloat(
            initialValue = 0F,
            targetValue = 54F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "audio tips swipe offset"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, bottom = bottomIndicatorPadding(bottomPadding)),
            contentAlignment = Alignment.BottomStart
        ) {
            Box {
                Image(
                    painter = painterResource(R.drawable.tips_gesture_horizontal_small),
                    contentDescription = null
                )
                PulsingTapIndicator(
                    size = 24.dp,
                    primaryColor = primaryColor,
                    fillColor = backgroundColor,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = offset.dp)
                )
            }
        }
    }

    @Composable
    private fun TapIndicatorAt(
        centerXPx: Int,
        fallbackStart: Dp,
        bottomPadding: Dp,
        primaryColor: Color,
        alignEnd: Boolean = false
    ) {
        val density = LocalDensity.current
        val start = if (centerXPx >= 0) with(density) { centerXPx.toDp() - 24.dp } else fallbackStart
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (alignEnd) 0.dp else start.coerceAtLeast(0.dp),
                    end = if (alignEnd) 11.dp else 0.dp,
                    bottom = bottomIndicatorPadding(bottomPadding)
                ),
            contentAlignment = if (alignEnd) Alignment.BottomEnd else Alignment.BottomStart
        ) {
            PulsingTapIndicator(
                size = 48.dp,
                primaryColor = primaryColor,
                fillColor = primaryColor.copy(alpha = 0.12F)
            )
        }
    }

    @Composable
    private fun PulsingTapIndicator(
        size: Dp,
        primaryColor: Color,
        fillColor: Color,
        modifier: Modifier = Modifier
    ) {
        val transition = rememberInfiniteTransition(label = "audio tips tap")
        val scale by transition.animateFloat(
            initialValue = 0.9F,
            targetValue = 1.12F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse
            ),
            label = "audio tips tap scale"
        )
        val alpha by transition.animateFloat(
            initialValue = 0.55F,
            targetValue = 1F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse
            ),
            label = "audio tips tap alpha"
        )
        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .alpha(alpha)
                .background(fillColor, CircleShape)
                .border(2.dp, primaryColor, CircleShape)
        )
    }

    private fun bottomIndicatorPadding(bottomInset: Dp) = if (bottomInset > 72.dp) bottomInset - 72.dp else 16.dp
}

private fun Context.resolveComposeColor(attr: Int): Color {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return if (typedValue.resourceId != 0) Color(ContextCompat.getColor(this, typedValue.resourceId)) else Color(typedValue.data)
}
