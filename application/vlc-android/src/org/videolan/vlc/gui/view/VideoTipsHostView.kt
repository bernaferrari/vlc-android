/*
 * ************************************************************************
 *  VideoTipsHostView.kt
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
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 */

package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.gui.video.VideoPlayerTipsStep

enum class VideoTipsControl(
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int,
    @StringRes val selectedTitle: Int,
    @StringRes val selectedDescription: Int
) {
    TRACKS(R.drawable.ic_player_audiotrack, R.string.tracks, R.string.tips_audio_sub, R.string.tap),
    ORIENTATION(R.drawable.ic_player_rotate, R.string.lock_orientation, R.string.lock_orientation, R.string.lock_orientation_description),
    PLAY(R.drawable.ic_play_player, R.string.play, R.string.play, R.string.tips_play_description),
    RATIO(R.drawable.ic_player_ratio, R.string.aspect_ratio, R.string.aspect_ratio, R.string.aspect_ratio_description),
    ADVANCED(R.drawable.ic_player_more, R.string.advanced_options, R.string.advanced_options, R.string.advanced_options_description)
}

private data class VideoTipsUiState(
    val visible: Boolean = false,
    val step: VideoPlayerTipsStep? = null,
    @StringRes val title: Int = R.string.tips_player_controls,
    @StringRes val description: Int = R.string.tips_player_controls_description,
    @StringRes val nextButtonText: Int = R.string.next_step,
    val selectedControl: VideoTipsControl? = null
)

/**
 * Compose replacement for player_tips.xml. VideoTipsDelegate keeps the
 * tutorial sequencing contract and pushes the current step/control state here.
 */
class VideoTipsHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var state by mutableStateOf(VideoTipsUiState())
    private var onDismissClick: () -> Unit = {}
    private var onNextClick: () -> Unit = {}
    private var onControlClick: (VideoTipsControl) -> Unit = {}
    private val fontColor = context.resolveComposeColor(R.attr.font_default)
    private val primaryColor = context.resolveComposeColor(R.attr.colorPrimary)

    init {
        id = R.id.player_overlay_tips
        visibility = View.GONE
        isClickable = true
        setOnTouchListener { _, _ -> true }
    }

    fun setCallbacks(
        onDismiss: () -> Unit,
        onNext: () -> Unit,
        onControl: (VideoTipsControl) -> Unit
    ) {
        onDismissClick = onDismiss
        onNextClick = onNext
        onControlClick = onControl
    }

    fun showStep(
        step: VideoPlayerTipsStep,
        @StringRes title: Int,
        @StringRes description: Int,
        @StringRes nextButtonText: Int,
        selectedControl: VideoTipsControl? = null
    ) {
        visibility = View.VISIBLE
        state = VideoTipsUiState(
            visible = true,
            step = step,
            title = title,
            description = description,
            nextButtonText = nextButtonText,
            selectedControl = selectedControl
        )
    }

    fun updateControlCopy(
        selectedControl: VideoTipsControl?,
        @StringRes title: Int,
        @StringRes description: Int
    ) {
        state = state.copy(
            selectedControl = selectedControl,
            title = title,
            description = description
        )
    }

    fun hideTips() {
        state = state.copy(visible = false)
        visibility = View.GONE
    }

    @Composable
    override fun WidgetContent() {
        val current = state
        if (!current.visible || current.step == null) return

        VideoTipsOverlay(
            state = current,
            fontColor = fontColor,
            primaryColor = primaryColor,
            onDismiss = onDismissClick,
            onNext = onNextClick,
            onControlClick = onControlClick
        )
    }
}

@Composable
private fun VideoTipsOverlay(
    state: VideoTipsUiState,
    fontColor: Color,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onNext: () -> Unit,
    onControlClick: (VideoTipsControl) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8F))
    ) {
        VideoTipsHeader(
            fontColor = fontColor,
            onDismiss = onDismiss,
            modifier = Modifier.align(Alignment.TopStart)
        )

        when (state.step) {
            VideoPlayerTipsStep.CONTROLS -> VideoTipsControlsRow(
                selectedControl = state.selectedControl,
                fontColor = fontColor,
                onControlClick = onControlClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = 20.dp)
            )
            VideoPlayerTipsStep.BRIGHTNESS -> VideoTipsVerticalSwipe(
                icon = R.drawable.ic_player_brightness,
                alignProgressToEnd = true,
                primaryColor = primaryColor,
                modifier = Modifier.fillMaxSize()
            )
            VideoPlayerTipsStep.VOLUME -> VideoTipsVerticalSwipe(
                icon = R.drawable.ic_player_volume,
                alignProgressToEnd = false,
                primaryColor = primaryColor,
                modifier = Modifier.fillMaxSize()
            )
            VideoPlayerTipsStep.PAUSE -> VideoTipsDoubleTap(
                showSeek = false,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-96).dp)
            )
            VideoPlayerTipsStep.SEEK_TAP -> VideoTipsSeekTap(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = (-96).dp)
            )
            VideoPlayerTipsStep.SEEK -> VideoTipsHorizontalSwipe(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-116).dp)
            )
            null -> Unit
        }

        VideoTipsCopy(
            state = state,
            fontColor = fontColor,
            primaryColor = primaryColor,
            onNext = onNext,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 80.dp, bottom = 104.dp)
        )
    }
}

@Composable
private fun VideoTipsHeader(
    fontColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                painter = painterResource(R.drawable.ic_close_small),
                contentDescription = stringResource(R.string.close),
                tint = fontColor
            )
        }
        Text(
            text = stringResource(R.string.tips_title),
            color = fontColor,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun VideoTipsCopy(
    state: VideoTipsUiState,
    fontColor: Color,
    primaryColor: Color,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(state.title),
            color = fontColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(state.description),
            color = Color.White.copy(alpha = 0.5F),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Text(text = stringResource(state.nextButtonText))
        }
    }
}

@Composable
private fun VideoTipsControlsRow(
    selectedControl: VideoTipsControl?,
    fontColor: Color,
    onControlClick: (VideoTipsControl) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoTipsControl.entries.forEach { control ->
            VideoTipsControlButton(
                control = control,
                selected = selectedControl == control,
                fontColor = fontColor,
                onClick = { onControlClick(control) }
            )
        }
    }
}

@Composable
private fun VideoTipsControlButton(
    control: VideoTipsControl,
    selected: Boolean,
    fontColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Image(
                    painter = painterResource(R.drawable.tips_tap),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp)
                )
            } else {
                PulsingTapMarker(size = 52.dp)
            }
            Icon(
                painter = painterResource(control.icon),
                contentDescription = stringResource(control.contentDescription),
                tint = fontColor,
                modifier = Modifier.size(if (control == VideoTipsControl.PLAY) 44.dp else 32.dp)
            )
        }
    }
}

@Composable
private fun VideoTipsVerticalSwipe(
    @DrawableRes icon: Int,
    alignProgressToEnd: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "videoTipsVerticalSwipe")
    val offset by transition.animateFloat(
        initialValue = 30F,
        targetValue = -30F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoTipsVerticalSwipeOffset"
    )
    val value = (50 - (offset * 2 / 3).toInt()).coerceIn(0, 100)

    Box(modifier = modifier) {
        VideoTipsGesture(
            horizontal = false,
            tapOffsetY = offset.dp,
            modifier = Modifier
                .align(if (alignProgressToEnd) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 32.dp)
        )
        VideoTipsProgress(
            value = value,
            icon = icon,
            primaryColor = primaryColor,
            modifier = Modifier
                .align(if (alignProgressToEnd) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun VideoTipsProgress(
    value: Int,
    @DrawableRes icon: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value%",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(120.dp)
                .border(1.dp, Color.White.copy(alpha = 0.5F), RoundedCornerShape(8.dp))
                .padding(2.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value / 100F)
                    .background(primaryColor, RoundedCornerShape(6.dp))
            )
        }
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(top = 16.dp)
                .size(32.dp)
        )
    }
}

@Composable
private fun VideoTipsHorizontalSwipe(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "videoTipsHorizontalSwipe")
    val offset by transition.animateFloat(
        initialValue = -42F,
        targetValue = 42F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoTipsHorizontalSwipeOffset"
    )
    VideoTipsGesture(
        horizontal = true,
        tapOffsetX = offset.dp,
        modifier = modifier
    )
}

@Composable
private fun VideoTipsGesture(
    horizontal: Boolean,
    modifier: Modifier = Modifier,
    tapOffsetX: Dp = 0.dp,
    tapOffsetY: Dp = 0.dp
) {
    Box(
        modifier = modifier.size(if (horizontal) 160.dp else 96.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(if (horizontal) R.drawable.tips_gesture_horizontal else R.drawable.tips_gesture),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.tips_tap),
            contentDescription = null,
            modifier = Modifier
                .offset(x = tapOffsetX, y = tapOffsetY)
                .size(40.dp)
        )
    }
}

@Composable
private fun VideoTipsSeekTap(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoTipsDoubleTap(
            showSeek = true,
            forward = false,
            modifier = Modifier.padding(start = 24.dp)
        )
        VideoTipsDoubleTap(
            showSeek = true,
            forward = true,
            modifier = Modifier.padding(end = 24.dp)
        )
    }
}

@Composable
private fun VideoTipsDoubleTap(
    showSeek: Boolean,
    modifier: Modifier = Modifier,
    forward: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "videoTipsDoubleTap")
    val alpha by transition.animateFloat(
        initialValue = 0F,
        targetValue = 1F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoTipsDoubleTapAlpha"
    )
    val scale by transition.animateFloat(
        initialValue = 0.9F,
        targetValue = 1F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoTipsDoubleTapScale"
    )

    Box(
        modifier = modifier.size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.tips_double_tap),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
        if (showSeek) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) {
                    Icon(
                        painter = painterResource(if (forward) R.drawable.ic_half_seek_forward else R.drawable.ic_half_seek_rewind),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingTapMarker(size: Dp) {
    val transition = rememberInfiniteTransition(label = "videoTipsTapMarker")
    val alpha by transition.animateFloat(
        initialValue = 0.35F,
        targetValue = 0.8F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoTipsTapMarkerAlpha"
    )
    val scale by transition.animateFloat(
        initialValue = 0.9F,
        targetValue = 1.08F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoTipsTapMarkerScale"
    )
    Image(
        painter = painterResource(R.drawable.tips_tap),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .alpha(alpha)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    )
}

private fun Context.resolveComposeColor(attr: Int): Color {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return if (typedValue.resourceId != 0) Color(ContextCompat.getColor(this, typedValue.resourceId)) else Color(typedValue.data)
}
