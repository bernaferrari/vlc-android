/*
 * ************************************************************************
 *  VideoHudOverlayView.kt
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
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.videolan.tools.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView

/**
 * Direct host factory for the video player's bottom HUD. The leaf controls are
 * Compose-backed views; this plain root replaces the old player_hud.xml shell
 * while preserving the same stable IDs for the player delegates.
 */
internal fun Context.createVideoHudOverlay(): ConstraintLayout =
    ConstraintLayout(this).apply {
        id = R.id.progress_overlay
        setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
        visibility = View.INVISIBLE
        addHudChildren(
            smallSideMargin = resources.getDimensionPixelSize(R.dimen.small_margins_sides),
            largeCenterMargin = resources.getDimensionPixelSize(R.dimen.large_margins_center)
        )
    }

private fun ConstraintLayout.addHudChildren(
    smallSideMargin: Int,
    largeCenterMargin: Int
) {
        addView(VLCComposeView(context).apply {
            id = R.id.stats_container
            setBackgroundResource(R.drawable.rounded_corners)
            visibility = View.GONE
            installVideoStatsOverlayHost()
        }, hudLayout(0, 0) {
            topToTop = PARENT_ID
            bottomToTop = R.id.player_overlay_seekbar
            startToStart = PARENT_ID
            endToEnd = PARENT_ID
            topMargin = 24.dp
            bottomMargin = 8.dp
        })

        addView(VLCComposeView(context).apply {
            id = R.id.ab_repeat_container
            visibility = View.GONE
            installAbRepeatControlsHost()
        }, hudLayout {
            bottomToTop = R.id.player_overlay_time
            startToStart = PARENT_ID
            marginStart = 16.dp
            bottomMargin = 8.dp
        })

        addView(hudIconButton(
            id = R.id.ab_repeat_reset,
            icon = R.drawable.ic_abrepeat_reset_marker_circle,
            contentDescription = R.string.ab_repeat_reset,
            visible = View.GONE
        ), hudLayout {
            bottomToTop = R.id.player_overlay_time
            startToStart = PARENT_ID
            marginStart = 16.dp
            topMargin = 4.dp
            bottomMargin = 8.dp
            verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
        })

        addView(hudIconButton(
            id = R.id.ab_repeat_stop,
            icon = R.drawable.ic_abrepeat_reset_circle,
            contentDescription = R.string.ab_repeat_stop,
            visible = View.GONE
        ), hudLayout {
            bottomToTop = R.id.player_overlay_time
            startToEnd = R.id.ab_repeat_reset
            marginStart = 16.dp
            topMargin = 4.dp
            bottomMargin = 8.dp
        })

        addView(hudIconButton(
            id = R.id.fast_seek_warning,
            icon = R.drawable.ic_abrepeat_warning,
            contentDescription = R.string.ab_repeat_fastseek_warning,
            visible = View.GONE
        ), hudLayout {
            topToTop = R.id.ab_repeat_stop
            bottomToBottom = R.id.ab_repeat_stop
            startToEnd = R.id.ab_repeat_stop
            marginStart = 16.dp
        })

        addView(VLCComposeView(context).apply {
            id = R.id.bookmarks_background
            setBackgroundFromAttr(R.attr.bookmark_background)
            fitsSystemWindows = true
            isFocusable = false
            visibility = View.GONE
            installBookmarksPanelHost()
        }, hudLayout(0, 0) {
            topToTop = PARENT_ID
            bottomToBottom = PARENT_ID
            startToStart = PARENT_ID
            endToEnd = PARENT_ID
        })

        addView(timeLabel(R.id.player_overlay_time), hudLayout(0, WRAP_CONTENT) {
            bottomToTop = R.id.player_overlay_seekbar
            leftToLeft = PARENT_ID
            rightToLeft = R.id.player_overlay_length
            marginStart = 24.dp
            horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_SPREAD
        })

        addView(timeLabel(R.id.player_overlay_length), hudLayout(0, WRAP_CONTENT) {
            bottomToTop = R.id.player_overlay_seekbar
            leftToRight = R.id.player_overlay_time
            rightToRight = PARENT_ID
            marginEnd = 24.dp
        })

        addView(VideoTimelineSeekBarView(context).apply {
            id = R.id.player_overlay_seekbar
            isFocusable = true
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, 0, 0, 0)
        }, hudLayout(0, WRAP_CONTENT) {
            bottomToTop = R.id.player_overlay_play
            startToStart = PARENT_ID
            endToEnd = PARENT_ID
        })

        addView(VLCComposeView(context).apply {
            id = R.id.bookmark_marker_container
            setPadding(8.dp, 0, 8.dp, 0)
            installBookmarkMarkerContainerHost()
        }, hudLayout(0, WRAP_CONTENT) {
            bottomToBottom = R.id.player_overlay_seekbar
            startToStart = R.id.player_overlay_seekbar
            endToEnd = R.id.player_overlay_seekbar
            bottomMargin = 16.dp
        })

        addView(VLCComposeView(context).apply {
            id = R.id.ab_repeat_marker_guideline_container
            clipToPadding = false
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(8.dp, 0, 8.dp, 0)
            visibility = View.GONE
            installAbRepeatMarkerContainerHost()
        }, hudLayout(0, WRAP_CONTENT) {
            bottomToTop = R.id.player_overlay_seekbar
            startToStart = R.id.player_overlay_seekbar
            endToEnd = R.id.player_overlay_seekbar
        })

        addView(hudIconButton(
            id = R.id.player_overlay_tracks,
            icon = R.drawable.ic_player_audiotrack,
            contentDescription = R.string.tracks
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToStart = PARENT_ID
            endToStart = R.id.orientation_toggle
            marginStart = smallSideMargin
            horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_SPREAD_INSIDE
        })

        addView(hudIconButton(
            id = R.id.orientation_toggle,
            icon = R.drawable.ic_player_rotate,
            contentDescription = R.string.lock_orientation_description
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToEnd = R.id.player_overlay_tracks
            endToStart = R.id.player_space_left
            marginStart = smallSideMargin
        })

        addView(Space(context).apply {
            id = R.id.player_space_left
        }, hudLayout(0, WRAP_CONTENT) {
            topToTop = R.id.playlist_previous
            bottomToBottom = R.id.playlist_previous
            startToEnd = R.id.orientation_toggle
            endToStart = R.id.playlist_previous
        })

        addView(hudIconButton(
            id = R.id.playlist_previous,
            icon = R.drawable.ic_player_previous,
            contentDescription = R.string.previous,
            visible = View.GONE,
            longClickable = true
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToEnd = R.id.player_space_left
            endToStart = R.id.player_overlay_rewind
            marginEnd = largeCenterMargin
        })

        addView(hudIconButton(
            id = R.id.player_overlay_rewind,
            icon = R.drawable.ic_player_rewind_10,
            contentDescription = R.string.playback_rewind,
            visible = View.GONE,
            longClickable = true
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToEnd = R.id.playlist_previous
            endToStart = R.id.player_overlay_play
            marginEnd = largeCenterMargin
        })

        addView(seekJumpLabel(R.id.player_overlay_rewind_text), hudLayout {
            topToTop = R.id.player_overlay_rewind
            bottomToBottom = R.id.player_overlay_rewind
            startToStart = R.id.player_overlay_rewind
            endToEnd = R.id.player_overlay_rewind
        })

        addView(hudIconButton(
            id = R.id.player_overlay_play,
            icon = R.drawable.ic_pause_player,
            contentDescription = R.string.play
        ), hudLayout {
            bottomToBottom = PARENT_ID
            startToEnd = R.id.player_overlay_rewind
            endToStart = R.id.player_overlay_forward
        })

        addView(hudIconButton(
            id = R.id.player_overlay_forward,
            icon = R.drawable.ic_player_forward_10,
            contentDescription = R.string.playback_forward,
            visible = View.GONE,
            longClickable = true
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToEnd = R.id.player_overlay_play
            endToStart = R.id.playlist_next
            marginStart = largeCenterMargin
        })

        addView(seekJumpLabel(R.id.player_overlay_forward_text), hudLayout {
            topToTop = R.id.player_overlay_forward
            bottomToBottom = R.id.player_overlay_forward
            startToStart = R.id.player_overlay_forward
            endToEnd = R.id.player_overlay_forward
        })

        addView(hudIconButton(
            id = R.id.playlist_next,
            icon = R.drawable.ic_player_next,
            contentDescription = R.string.next,
            visible = View.GONE,
            longClickable = true
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToEnd = R.id.player_overlay_forward
            endToStart = R.id.player_space_right
            marginStart = largeCenterMargin
        })

        addView(Space(context).apply {
            id = R.id.player_space_right
        }, hudLayout(0, WRAP_CONTENT) {
            topToTop = R.id.playlist_next
            bottomToBottom = R.id.playlist_next
            startToEnd = R.id.playlist_next
            endToStart = R.id.player_resize
        })

        addView(hudIconButton(
            id = R.id.player_resize,
            icon = R.drawable.ic_player_ratio,
            contentDescription = R.string.aspect_ratio
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToEnd = R.id.player_space_right
            endToStart = R.id.player_overlay_adv_function
            marginEnd = smallSideMargin
        })

        addView(hudIconButton(
            id = R.id.player_overlay_adv_function,
            icon = R.drawable.ic_player_more,
            contentDescription = R.string.advanced
        ), hudLayout {
            topToTop = R.id.player_overlay_play
            bottomToBottom = R.id.player_overlay_play
            startToEnd = R.id.player_resize
            endToEnd = PARENT_ID
            marginEnd = smallSideMargin
        })

        addView(VLCComposeView(context).apply {
            id = R.id.swipe_to_unlock
            visibility = View.GONE
            installSwipeToUnlockHost()
        }, hudLayout(WRAP_CONTENT, 0) {
            topToBottom = R.id.player_overlay_seekbar
            bottomToBottom = PARENT_ID
            startToStart = PARENT_ID
            endToEnd = PARENT_ID
        })
    }

private fun ConstraintLayout.hudIconButton(
    @IdRes id: Int,
    @DrawableRes icon: Int,
    @StringRes contentDescription: Int,
    visible: Int = View.VISIBLE,
    longClickable: Boolean = false
) = VLCComposeView(context).apply {
    this.id = id
    setBackgroundFromAttr(android.R.attr.selectableItemBackgroundBorderless)
    isClickable = true
    isFocusable = true
    isLongClickable = longClickable
    this.contentDescription = context.getString(contentDescription)
    visibility = visible
    installVideoHudIconButtonHost(
        icon = icon,
        contentDescription = context.getString(contentDescription)
    )
}

private fun ConstraintLayout.timeLabel(@IdRes id: Int) = VLCComposeView(context).apply {
    this.id = id
    isClickable = true
    isFocusable = true
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    nextFocusUpId = R.id.ab_repeat_container
    installVideoTimelineTimeLabelHost(alignEnd = id == R.id.player_overlay_length)
}

private fun ConstraintLayout.seekJumpLabel(@IdRes id: Int) = VLCComposeView(context).apply {
    this.id = id
    visibility = View.GONE
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    installVideoHudSeekJumpLabelHost()
}

private fun hudLayout(
    width: Int = WRAP_CONTENT,
    height: Int = WRAP_CONTENT,
    block: ConstraintLayout.LayoutParams.() -> Unit
): ConstraintLayout.LayoutParams {
    return ConstraintLayout.LayoutParams(width, height).apply(block)
}

private fun View.setBackgroundFromAttr(@AttrRes attr: Int) {
    val typedValue = TypedValue()
    if (!context.theme.resolveAttribute(attr, typedValue, true)) return
    if (typedValue.resourceId != 0) {
        setBackgroundResource(typedValue.resourceId)
    } else {
        setBackgroundColor(typedValue.data)
    }
}

private const val PARENT_ID = ConstraintLayout.LayoutParams.PARENT_ID
private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
