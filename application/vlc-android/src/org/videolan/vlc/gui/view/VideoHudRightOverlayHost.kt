/*
 * ************************************************************************
 *  VideoHudRightOverlayHost.kt
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
import android.content.res.Configuration
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCVideoQuickAction
import org.videolan.vlc.compose.components.VLCVideoQuickActions
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private data class VideoHudRightOverlayState(
    val title: String = "",
    val showTitleWarning: Boolean = true,
    val titleTopPaddingPx: Int = 0,
    val navMenuVisible: Boolean = false,
    val screenshotVisible: Boolean = false,
    val playlistVisible: Boolean = false,
    val secondaryDisplayVisible: Boolean = false,
    @DrawableRes val secondaryDisplayIcon: Int = R.drawable.ic_player_screenshare,
    val secondaryDisplayContentDescription: String = "",
    val rendererVisible: Boolean = false,
    @DrawableRes val rendererIcon: Int = R.drawable.ic_player_renderer,
    val clockVisible: Boolean = false,
    val quickActions: List<VideoQuickActionEntry> = emptyList()
)

private data class VideoQuickActionEntry(
    @IdRes val id: Int,
    @DrawableRes val icon: Int,
    val text: String? = null,
    val contentDescription: String? = null,
    val visible: Boolean = false
)

/**
 * Compose replacement for player_hud_right.xml. The player delegates still own
 * playback state and action dispatch; this host owns the top video HUD title,
 * icon cluster, TV clock, and quick-action chip strip.
 */
internal fun VLCComposeView.installVideoHudRightOverlayHost() {
    val host = VideoHudRightOverlayHost(context)
    setTag(R.id.hud_right_overlay, host)
    id = R.id.hud_right_overlay
    visibility = View.INVISIBLE
    isClickable = false
    isFocusable = false
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoHudRightOverlayHost(): VideoHudRightOverlayHost =
    getTag(R.id.hud_right_overlay) as? VideoHudRightOverlayHost ?: error("Missing video HUD right overlay host")

internal class VideoHudRightOverlayHost(context: Context) {

    private var state by mutableStateOf(
        VideoHudRightOverlayState(
            secondaryDisplayContentDescription = context.getString(R.string.video_remote_enable),
            quickActions = defaultActions(context)
        )
    )
    private var onActionClick by mutableStateOf<((Int) -> Unit)?>(null)
    private var onQuickActionClick by mutableStateOf<(Int) -> Unit>({ _ -> })
    private var onQuickActionLongClick by mutableStateOf<(Int) -> Unit>({ _ -> })
    private var onQuickActionsInteraction by mutableStateOf<(() -> Unit)>({})

    fun setTitle(title: String?) {
        state = state.copy(
            title = title.orEmpty(),
            showTitleWarning = title?.startsWith("fd://") != false
        )
    }

    fun hasTitleText() = state.title.isNotEmpty()

    fun setTitleTopPadding(paddingPx: Int) {
        state = state.copy(titleTopPaddingPx = paddingPx)
    }

    fun setNavMenuVisible(visible: Boolean) {
        state = state.copy(navMenuVisible = visible)
    }

    fun setScreenshotVisible(visible: Boolean) {
        state = state.copy(screenshotVisible = visible)
    }

    fun setPlaylistVisible(visible: Boolean) {
        state = state.copy(playlistVisible = visible)
    }

    fun setSecondaryDisplay(visible: Boolean, secondary: Boolean, contentDescription: String) {
        state = state.copy(
            secondaryDisplayVisible = visible,
            secondaryDisplayIcon = if (secondary) R.drawable.ic_player_screenshare_stop else R.drawable.ic_player_screenshare,
            secondaryDisplayContentDescription = contentDescription
        )
    }

    fun setRendererVisible(visible: Boolean) {
        state = state.copy(rendererVisible = visible)
    }

    fun setRendererConnected(connected: Boolean) {
        state = state.copy(rendererIcon = if (connected) R.drawable.ic_player_renderer_on else R.drawable.ic_player_renderer)
    }

    fun setClockVisible(visible: Boolean) {
        state = state.copy(clockVisible = visible)
    }

    fun setActionVisible(@IdRes actionId: Int, visible: Boolean) {
        updateQuickAction(actionId) { it.copy(visible = visible) }
    }

    fun setActionText(@IdRes actionId: Int, text: CharSequence?) {
        updateQuickAction(actionId) { it.copy(text = text?.toString()) }
    }

    fun setActionContentDescription(@IdRes actionId: Int, contentDescription: CharSequence?) {
        updateQuickAction(actionId) { it.copy(contentDescription = contentDescription?.toString()) }
    }

    fun setActionIcon(@IdRes actionId: Int, @DrawableRes icon: Int) {
        updateQuickAction(actionId) { it.copy(icon = icon) }
    }

    fun setOnActionClickListener(listener: ((Int) -> Unit)?) {
        onActionClick = listener
    }

    fun setOnQuickActionClickListener(listener: (Int) -> Unit) {
        onQuickActionClick = listener
    }

    fun setOnQuickActionLongClickListener(listener: (Int) -> Unit) {
        onQuickActionLongClick = listener
    }

    fun setOnQuickActionsTouched(listener: () -> Unit) {
        onQuickActionsInteraction = listener
    }

    private fun updateQuickAction(@IdRes actionId: Int, transform: (VideoQuickActionEntry) -> VideoQuickActionEntry) {
        state = state.copy(
            quickActions = state.quickActions.map { action ->
                if (action.id == actionId) transform(action) else action
            }
        )
    }

    @Composable
    fun Content() {
        val current = state
        val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
        val titleTopPadding = with(LocalDensity.current) { current.titleTopPaddingPx.toDp() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            if (isPortrait) {
                HeaderIconRow(current)
                HudTitle(
                    state = current,
                    topPadding = titleTopPadding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    HeaderIconRow(current)
                    HudTitle(
                        state = current,
                        topPadding = titleTopPadding,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(start = 136.dp, end = 136.dp)
                            .widthIn(min = 0.dp)
                    )
                }
            }

            val visibleActions = current.quickActions
                .filter { it.visible }
                .map { action ->
                    VLCVideoQuickAction(
                        id = action.id,
                        icon = action.icon,
                        text = action.text,
                        contentDescription = action.contentDescription
                    )
                }
            if (visibleActions.isNotEmpty()) {
                VLCVideoQuickActions(
                    actions = visibleActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onActionClick = { id ->
                        onQuickActionsInteraction()
                        onQuickActionClick(id)
                    },
                    onActionLongClick = { id ->
                        onQuickActionsInteraction()
                        onQuickActionLongClick(id)
                    }
                )
            }
        }
    }

    @Composable
    private fun HeaderIconRow(state: VideoHudRightOverlayState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudIconButton(
                    id = R.id.player_overlay_navmenu,
                    icon = R.drawable.ic_player_navmenu,
                    visible = state.navMenuVisible
                )
                HudIconButton(
                    id = R.id.player_screenshot,
                    icon = R.drawable.ic_player_screenshot,
                    visible = state.screenshotVisible
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                HudIconButton(
                    id = R.id.video_secondary_display,
                    icon = state.secondaryDisplayIcon,
                    visible = state.secondaryDisplayVisible,
                    contentDescription = state.secondaryDisplayContentDescription
                )
                HudIconButton(
                    id = R.id.video_renderer,
                    icon = state.rendererIcon,
                    visible = state.rendererVisible,
                    contentDescription = stringResource(R.string.renderer_list_title)
                )
                HudIconButton(
                    id = R.id.playlist_toggle,
                    icon = R.drawable.ic_player_playqueue,
                    visible = state.playlistVisible,
                    contentDescription = stringResource(R.string.show_playlist)
                )
                if (state.clockVisible) {
                    HudClock(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    @Composable
    private fun HudIconButton(
        @IdRes id: Int,
        @DrawableRes icon: Int,
        visible: Boolean,
        contentDescription: String? = null
    ) {
        if (!visible) return
        IconButton(
            onClick = { onActionClick?.invoke(id) },
            enabled = onActionClick != null,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    @Composable
    private fun HudTitle(
        state: VideoHudRightOverlayState,
        topPadding: androidx.compose.ui.unit.Dp,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier.padding(top = 8.dp + topPadding, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.showTitleWarning) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(
                            enabled = onActionClick != null,
                            role = Role.Button,
                            onClick = { onActionClick?.invoke(R.id.player_overlay_title_warning) }
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning_small),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    VideoHudTitleText(text = stringResource(R.string.unknown))
                }
            } else {
                VideoHudTitleText(text = state.title)
            }
        }
    }

    @Composable
    private fun VideoHudTitleText(text: String) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = VideoHudTitleStyle
        )
    }

    @Composable
    private fun HudClock(modifier: Modifier = Modifier) {
        var now by androidx.compose.runtime.remember { mutableStateOf(Date()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = Date()
                delay(1000)
            }
        }
        Text(
            text = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(now),
            color = Color.White,
            fontSize = 18.sp,
            style = VideoHudTitleStyle,
            modifier = modifier
        )
    }

    private companion object {
        val VideoHudTitleStyle = TextStyle(
            shadow = Shadow(
                color = Color(0xB4000000),
                offset = Offset(0F, 2F),
                blurRadius = 11F
            )
        )

        fun defaultActions(context: Context) = listOf(
            VideoQuickActionEntry(
                id = R.id.orientation_quick_action,
                icon = R.drawable.ic_player_lock_portrait,
                contentDescription = context.getString(R.string.lock_orientation_description)
            ),
            VideoQuickActionEntry(
                id = R.id.playback_speed_quick_action,
                icon = R.drawable.ic_speed,
                contentDescription = context.getString(R.string.playback_speed)
            ),
            VideoQuickActionEntry(
                id = R.id.sleep_quick_action,
                icon = R.drawable.ic_sleep,
                contentDescription = context.getString(R.string.sleep_title)
            ),
            VideoQuickActionEntry(
                id = R.id.spu_delay_quick_action,
                icon = R.drawable.ic_subtitles,
                contentDescription = context.getString(R.string.spu_delay)
            ),
            VideoQuickActionEntry(
                id = R.id.audio_delay_quick_action,
                icon = R.drawable.ic_player_volume,
                contentDescription = context.getString(R.string.audio_delay)
            )
        )
    }
}
