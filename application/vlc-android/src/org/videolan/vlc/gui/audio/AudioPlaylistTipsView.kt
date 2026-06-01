/*
 * ************************************************************************
 *  AudioPlaylistTipsView.kt
 * *************************************************************************
 * Copyright (C) 2026 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.audio

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Permanent host for the playlist gesture tips overlay. It replaces the former
 * ViewStub payload while keeping the public audio_playlist_tips ID used by
 * AudioPlayerContainerActivity.
 */
class AudioPlaylistTipsHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val tipsView = AudioPlaylistTipsView(context)

    init {
        id = R.id.audio_playlist_tips
        visibility = View.GONE
        isClickable = true
        elevation = resources.getDimension(R.dimen.audio_player_elevation)
        addView(
            tipsView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}

private data class AudioPlaylistTipsUiState(
    val visible: Boolean = false,
    val step: AudioPlaylistTipsStep? = null,
    @StringRes val title: Int = 0,
    @StringRes val description: Int = 0,
    val isTablet: Boolean = false,
    val media: MediaWrapper? = null,
    val subtitle: String = ""
)

/**
 * Compose replacement for the playlist tips overlay. AudioPlaylistTipsDelegate
 * owns sequencing and side effects, while this view owns the visual surface.
 */
class AudioPlaylistTipsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var state by mutableStateOf(AudioPlaylistTipsUiState())
    private var onDismissClick: () -> Unit = {}
    private var onNextClick: () -> Unit = {}
    private val tipsBackgroundColor = context.resolveComposeColor(R.attr.background_audio_tips)
    private val primaryColor = context.resolveComposeColor(R.attr.colorPrimary)

    init {
        isClickable = true
    }

    fun setCallbacks(
        onDismiss: () -> Unit,
        onNext: () -> Unit
    ) {
        onDismissClick = onDismiss
        onNextClick = onNext
    }

    fun showTip(
        step: AudioPlaylistTipsStep,
        @StringRes title: Int,
        @StringRes description: Int,
        isTablet: Boolean,
        media: MediaWrapper?,
        subtitle: String
    ) {
        state = AudioPlaylistTipsUiState(
            visible = true,
            step = step,
            title = title,
            description = description,
            isTablet = isTablet,
            media = media,
            subtitle = subtitle
        )
    }

    fun hideTips() {
        state = state.copy(visible = false)
    }

    @Composable
    override fun WidgetContent() {
        val current = state
        if (!current.visible || current.step == null) return

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(tipsBackgroundColor)
        ) {
            if (current.step != AudioPlaylistTipsStep.SEEK) {
                FakeTrackRows(
                    state = current,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 72.dp)
                )
            }

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
                    .padding(start = 24.dp, end = 24.dp, top = 104.dp, bottom = 104.dp)
            )

            if (current.step == AudioPlaylistTipsStep.SEEK) {
                SeekControls(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            TipIndicators(
                state = current,
                maxWidth = maxWidth,
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
                text = stringResource(R.string.playlist_tips),
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
        state: AudioPlaylistTipsUiState,
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
                Text(text = stringResource(if (state.step == AudioPlaylistTipsStep.SEEK) R.string.close else R.string.next_step))
            }
        }
    }

    @Composable
    private fun FakeTrackRows(
        state: AudioPlaylistTipsUiState,
        modifier: Modifier
    ) {
        val media = state.media ?: return
        Column(modifier = modifier) {
            repeat(11) { index ->
                val highlighted = index == 1 || (state.step == AudioPlaylistTipsStep.REARRANGE && index == 2)
                AudioPlaylistMediaItem(
                    media = media,
                    subtitle = state.subtitle,
                    showTrackNumbers = false,
                    showReorderButtons = state.isTablet,
                    showDeleteButton = true,
                    stopAfterThis = false,
                    current = index == 2,
                    playing = state.step == AudioPlaylistTipsStep.REARRANGE && index == 2,
                    masked = !highlighted,
                    tipsOverlayColor = tipsBackgroundColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun SeekControls(modifier: Modifier) {
        Column(modifier = modifier) {
            AnimatedTimeline(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 0.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlIcon(R.drawable.ic_shuffle_audio, R.string.shuffle_title)
                Spacer(Modifier.width(8.dp))
                ControlIcon(R.drawable.ic_previous, R.string.previous)
                Spacer(Modifier.width(8.dp))
                ControlIcon(R.drawable.ic_play_player, R.string.play, compact = true)
                Spacer(Modifier.width(8.dp))
                ControlIcon(R.drawable.ic_next, R.string.next)
                Spacer(Modifier.width(8.dp))
                ControlIcon(R.drawable.ic_repeat_audio, R.string.repeat_title)
            }
        }
    }

    @Composable
    private fun ControlIcon(
        drawable: Int,
        @StringRes contentDescription: Int,
        compact: Boolean = false
    ) {
        Icon(
            painter = painterResource(drawable),
            contentDescription = stringResource(contentDescription),
            tint = VLCThemeDefaults.colors.playerIconColor,
            modifier = Modifier
                .size(if (compact) 40.dp else 48.dp)
                .padding(if (compact) 4.dp else 8.dp)
        )
    }

    @Composable
    private fun AnimatedTimeline(modifier: Modifier) {
        val transition = rememberInfiniteTransition(label = "playlist tips seek")
        val progress by transition.animateFloat(
            initialValue = 0.41F,
            targetValue = 0.66F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400),
                repeatMode = RepeatMode.Reverse
            ),
            label = "playlist tips seek progress"
        )
        BoxWithConstraints(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VLCThemeDefaults.colors.fontLight.copy(alpha = 0.45F))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(1.dp)
                    .background(VLCThemeDefaults.colors.primary)
            )
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth * progress) - 7.dp)
                    .size(14.dp)
                    .background(VLCThemeDefaults.colors.primary, CircleShape)
            )
        }
    }

    @Composable
    private fun TipIndicators(
        state: AudioPlaylistTipsUiState,
        maxWidth: Dp,
        primaryColor: Color,
        backgroundColor: Color
    ) {
        when (state.step) {
            AudioPlaylistTipsStep.REMOVE -> {
                if (state.isTablet) {
                    TapIndicatorAt(
                        offsetX = maxWidth - 76.dp,
                        offsetY = 128.dp,
                        primaryColor = primaryColor
                    )
                } else {
                    HorizontalSwipeIndicator(
                        primaryColor = primaryColor,
                        backgroundColor = backgroundColor
                    )
                }
            }
            AudioPlaylistTipsStep.REARRANGE -> {
                if (state.isTablet) {
                    TapIndicatorAt(
                        offsetX = maxWidth - 172.dp,
                        offsetY = 184.dp,
                        primaryColor = primaryColor
                    )
                } else {
                    DragIndicator(
                        offsetX = (maxWidth / 2) - 24.dp,
                        offsetY = 184.dp,
                        primaryColor = primaryColor
                    )
                }
            }
            AudioPlaylistTipsStep.SEEK -> {
                TapIndicatorAt(
                    offsetX = (maxWidth / 2) - 88.dp,
                    offsetY = Dp.Unspecified,
                    primaryColor = primaryColor,
                    alignBottom = true
                )
                TapIndicatorAt(
                    offsetX = (maxWidth / 2) + 40.dp,
                    offsetY = Dp.Unspecified,
                    primaryColor = primaryColor,
                    alignBottom = true
                )
            }
            null -> Unit
        }
    }

    @Composable
    private fun HorizontalSwipeIndicator(
        primaryColor: Color,
        backgroundColor: Color
    ) {
        val transition = rememberInfiniteTransition(label = "playlist tips swipe")
        val offset by transition.animateFloat(
            initialValue = 0F,
            targetValue = 54F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "playlist tips swipe offset"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 112.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box {
                Image(
                    painter = painterResource(R.drawable.tips_gesture_horizontal),
                    contentDescription = null
                )
                PulsingTapIndicator(
                    size = 40.dp,
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
    private fun DragIndicator(
        offsetX: Dp,
        offsetY: Dp,
        primaryColor: Color
    ) {
        val transition = rememberInfiniteTransition(label = "playlist tips drag")
        val offset by transition.animateFloat(
            initialValue = 0F,
            targetValue = -48F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "playlist tips drag offset"
        )
        TapIndicatorAt(
            offsetX = offsetX,
            offsetY = offsetY + offset.dp,
            primaryColor = primaryColor
        )
    }

    @Composable
    private fun TapIndicatorAt(
        offsetX: Dp,
        offsetY: Dp,
        primaryColor: Color,
        alignBottom: Boolean = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = offsetX.coerceAtLeast(0.dp),
                    top = if (alignBottom) 0.dp else offsetY.coerceAtLeast(0.dp),
                    bottom = if (alignBottom) 24.dp else 0.dp
                ),
            contentAlignment = if (alignBottom) Alignment.BottomStart else Alignment.TopStart
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
        val transition = rememberInfiniteTransition(label = "playlist tips tap")
        val scale by transition.animateFloat(
            initialValue = 0.9F,
            targetValue = 1.12F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse
            ),
            label = "playlist tips tap scale"
        )
        val alpha by transition.animateFloat(
            initialValue = 0.55F,
            targetValue = 1F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse
            ),
            label = "playlist tips tap alpha"
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
}

private fun Context.resolveComposeColor(attr: Int): Color {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return if (typedValue.resourceId != 0) Color(ContextCompat.getColor(this, typedValue.resourceId)) else Color(typedValue.data)
}
