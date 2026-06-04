/*
 * ************************************************************************
 *  VideoPlayerShell.kt
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

package org.videolan.vlc.gui.video

import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import org.videolan.libvlc.util.VLCVideoLayout
import org.videolan.tools.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.view.PlayerOptionsPanelView
import org.videolan.vlc.gui.view.VideoHudRightOverlayView
import org.videolan.vlc.gui.view.VideoSeekOverlayView
import org.videolan.vlc.gui.view.VideoTipsHostView
import org.videolan.vlc.gui.view.VideoVerticalProgressOverlayView
import org.videolan.vlc.gui.view.createVideoHudOverlay
import org.videolan.vlc.gui.view.installVideoDelayOverlayHost
import org.videolan.vlc.gui.view.installVideoInfoOverlayHost
import org.videolan.vlc.gui.view.installVideoOrientationOverlayHost
import org.videolan.vlc.gui.view.installVideoResizeOverlayHost
import org.videolan.vlc.gui.view.installVideoScreenshotOverlayHost

internal fun VideoPlayerActivity.createVideoPlayerShell(isPrimaryDisplay: Boolean): View =
        if (isPrimaryDisplay) createPrimaryVideoPlayerShell() else createRemoteVideoPlayerShell()

private fun Context.createVideoPlayerRoot() = FrameLayout(this).apply {
    id = R.id.player_root
    layoutParams = FrameLayout.LayoutParams(matchParent, matchParent)
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    contentDescription = getString(R.string.talkback_video_player)
    keepScreenOn = true
}

private fun Context.createPrimaryVideoPlayerShell() = createVideoPlayerRoot().apply {
    addView(videoLayout(), matchFrame())
    addView(seekOverlay(), matchFrame())
    addView(verticalOverlay(R.id.player_overlay_brightness), frameWrap(Gravity.CENTER_VERTICAL or Gravity.RIGHT) {
        rightMargin = resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal)
        bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
    })
    addView(verticalOverlay(R.id.player_overlay_volume), frameWrap(Gravity.CENTER_VERTICAL or Gravity.LEFT) {
        leftMargin = resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal)
        bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
    })
    addView(View(context).apply {
        id = R.id.hud_background
        setBackgroundResource(R.drawable.gradient_hud_player)
        visibility = View.GONE
    }, FrameLayout.LayoutParams(matchParent, 130.dp, Gravity.BOTTOM))
    addView(View(context).apply {
        id = R.id.hud_right_background
        setBackgroundResource(R.drawable.gradient_title_player)
        visibility = View.GONE
    }, FrameLayout.LayoutParams(matchParent, 150.dp))
    addView(primaryUiContainer(), matchFrame())
    addView(videoPlaylistContainer(primary = true), FrameLayout.LayoutParams(matchParent, matchParent, Gravity.CENTER_HORIZONTAL))
    addView(optionsPanel(), matchFrame())
    addView(VLCComposeView(context).apply {
        id = R.id.player_resize_stub
        visibility = View.GONE
        isClickable = true
        isFocusable = false
        installVideoResizeOverlayHost()
    }, matchFrame())
    addView(VLCComposeView(context).apply {
        id = R.id.player_orientation_stub
        visibility = View.GONE
        isClickable = true
        isFocusable = false
        installVideoOrientationOverlayHost()
    }, matchFrame())
    addView(VLCComposeView(context).apply {
        id = R.id.player_screenshot_stub
        visibility = View.GONE
        isClickable = false
        isFocusable = false
        installVideoScreenshotOverlayHost { visibility = View.GONE }
    }, matchFrame())
}

private fun Context.createRemoteVideoPlayerShell() = createVideoPlayerRoot().apply {
    addView(videoLayout(), matchFrame())
    addView(VideoTipsHostView(context).apply {
        visibility = View.GONE
    }, matchFrame())
    addView(FrameLayout(context).apply {
        id = R.id.player_overlay_background
        fitsSystemWindows = true
        addView(ImageView(context).apply {
            id = R.id.renderer_background_cone
            setImageResource(R.drawable.ic_renderer_background_cone)
        }, frameWrap(Gravity.CENTER))
        addView(infoOverlay(), frameWrap(Gravity.CENTER) {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
        })
        addView(FrameLayout(context).apply {
            id = R.id.player_ui_container
            addView(loadingView(wrap = true), frameWrap(Gravity.CENTER))
        }, matchFrame())
        addView(VLCComposeView(context).apply {
            id = R.id.delay_container
            visibility = View.INVISIBLE
            isFocusable = false
            isClickable = false
            installVideoDelayOverlayHost()
        }, frameWrap(Gravity.CENTER) {
            rightMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.default_margin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
        })
        addView(seekOverlay(), matchFrame(Gravity.CENTER))
        addView(hudRightOverlay(), matchWrapFrame(Gravity.TOP or Gravity.END) {
            val overlayMargin = resources.getDimensionPixelSize(R.dimen.overlay_margin)
            leftMargin = overlayMargin
            topMargin = overlayMargin
            rightMargin = overlayMargin
            bottomMargin = overlayMargin
        })
        addView(createVideoHudOverlay(), matchFrame(Gravity.BOTTOM) {
            val overlayMargin = resources.getDimensionPixelSize(R.dimen.overlay_margin)
            leftMargin = overlayMargin
            rightMargin = overlayMargin
            bottomMargin = overlayMargin
        })
        addView(VLCComposeView(context).apply {
            id = R.id.player_resize_stub
            visibility = View.GONE
            isClickable = true
            isFocusable = false
            installVideoResizeOverlayHost()
        }, matchFrame())
        addView(VLCComposeView(context).apply {
            id = R.id.player_orientation_stub
            visibility = View.GONE
            isClickable = true
            isFocusable = false
            installVideoOrientationOverlayHost()
        }, matchFrame())
    }, matchFrame())
    addView(verticalOverlay(R.id.player_overlay_brightness), frameWrap(Gravity.CENTER_VERTICAL or Gravity.RIGHT) {
        rightMargin = resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal)
        bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
    })
    addView(verticalOverlay(R.id.player_overlay_volume), frameWrap(Gravity.CENTER_VERTICAL or Gravity.LEFT) {
        leftMargin = resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal)
        bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
    })
    addView(videoPlaylistContainer(primary = false), FrameLayout.LayoutParams(matchParent, matchParent, Gravity.CENTER_HORIZONTAL))
    addView(optionsPanel(), matchFrame())
}

private fun Context.primaryUiContainer() = FrameLayout(this).apply {
    id = R.id.player_ui_container
    fitsSystemWindows = true
    addView(VideoTipsHostView(context).apply {
        visibility = View.GONE
    }, matchFrame())
    addView(loadingView(wrap = false), FrameLayout.LayoutParams(80.dp, 80.dp, Gravity.CENTER))
    addView(infoOverlay(), frameWrap(Gravity.CENTER) {
        bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
    })
    addView(VLCComposeView(context).apply {
        id = R.id.delay_container
        visibility = View.INVISIBLE
        isFocusable = false
        isClickable = false
        installVideoDelayOverlayHost()
    }, frameWrap(Gravity.CENTER_VERTICAL or Gravity.END))
    addView(hudRightOverlay(), matchWrapFrame(Gravity.END))
    addView(createVideoHudOverlay(), matchFrame(Gravity.BOTTOM))
    addView(hingeArrow(R.id.hinge_go_left, R.drawable.ic_arrow_left), frameWrap(Gravity.CENTER_VERTICAL))
    addView(hingeArrow(R.id.hinge_go_right, R.drawable.ic_arrow_right), frameWrap(Gravity.CENTER_VERTICAL or Gravity.RIGHT))
}

private fun Context.videoLayout() = VLCVideoLayout(this).apply {
    id = R.id.video_layout
    fitsSystemWindows = false
}

private fun Context.seekOverlay() = VideoSeekOverlayView(this).apply {
    id = R.id.seekContainer
    fitsSystemWindows = false
    visibility = View.INVISIBLE
}

private fun Context.verticalOverlay(id: Int) = VideoVerticalProgressOverlayView(this).apply {
    this.id = id
    visibility = View.INVISIBLE
}

private fun Context.loadingView(wrap: Boolean) = ImageView(this).apply {
    id = R.id.player_overlay_loading
    setImageResource(R.drawable.ic_cone_o)
    visibility = View.INVISIBLE
    if (!wrap) scaleType = ImageView.ScaleType.FIT_CENTER
}

private fun Context.infoOverlay() = VLCComposeView(this).apply {
    id = R.id.player_info_stub
    visibility = View.INVISIBLE
    isClickable = false
    isFocusable = false
    installVideoInfoOverlayHost()
}

private fun Context.hudRightOverlay() = VideoHudRightOverlayView(this).apply {
    id = R.id.hud_right_overlay
    visibility = View.INVISIBLE
}

private fun Context.hingeArrow(id: Int, icon: Int) = ImageView(this).apply {
    this.id = id
    setImageResource(icon)
    isClickable = true
    isFocusable = true
    background = selectableItemBackgroundBorderless()
    visibility = View.GONE
}

private fun Context.optionsPanel() = PlayerOptionsPanelView(this).apply {
    id = R.id.options_background
    isClickable = true
    elevation = 16.dp.toFloat()
    isFocusable = false
    visibility = View.GONE
}

private fun Context.videoPlaylistContainer(primary: Boolean) = ConstraintLayout(this).apply {
    id = R.id.video_playlist_container
    maxWidth = 480.dp
    setBackgroundColor(ContextCompat.getColor(context, R.color.playerbackground))
    visibility = View.GONE

    val closeButton = ImageButton(context).apply {
        id = R.id.close_button
        setImageResource(R.drawable.ic_close_small)
        background = selectableItemBackgroundBorderless()
        contentDescription = getString(R.string.close)
        setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    }
    val searchText = playlistSearchText()
    val playlist = VLCComposeView(context).apply {
        id = R.id.video_playlist
        clipToPadding = false
        isFocusable = true
        nextFocusUpId = R.id.playlist_search_edittext
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    if (primary) {
        val header = View(context).apply {
            id = R.id.video_playlist_header
            setBackgroundColor(ContextCompat.getColor(context, R.color.playerbackground))
        }
        addView(header, constraintLayout(matchConstraint, 56.dp) {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })
        addView(closeButton, constraintLayout(wrapContent, wrapContent) {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = R.id.video_playlist_header
            marginStart = 8.dp
        })
        addView(searchText, constraintLayout(matchConstraint, matchConstraint) {
            startToEnd = R.id.close_button
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = R.id.video_playlist_header
            marginStart = 8.dp
            topMargin = 4.dp
            marginEnd = 8.dp
            bottomMargin = 4.dp
        })
        addView(playlist, constraintLayout(matchParent, matchConstraint) {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.video_playlist_header
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        })
    } else {
        addView(closeButton, constraintLayout(wrapContent, wrapContent) {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = R.id.playlist_search_text
            bottomToBottom = R.id.playlist_search_text
            marginStart = 8.dp
        })
        addView(searchText, constraintLayout(matchConstraint, wrapContent) {
            startToEnd = R.id.close_button
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 8.dp
        })
        addView(playlist, constraintLayout(matchParent, matchConstraint) {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.playlist_search_text
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 8.dp
        })
    }
}

private fun Context.playlistSearchText() = TextInputLayout(
    ContextThemeWrapper(this, R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_Dense)
).apply {
    id = R.id.playlist_search_text
    visibility = View.VISIBLE
    boxStrokeColor = ContextCompat.getColor(context, R.color.grey200)
    hintTextColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey200))
    setPadding(8.dp, 0, 8.dp, 0)
    addView(EditText(context).apply {
        id = R.id.playlist_search_edittext
        isFocusable = true
        hint = getString(R.string.search_hint)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER
        nextFocusDownId = R.id.video_playlist
    }, ViewGroup.LayoutParams(matchParent, wrapContent))
}

private fun Context.selectableItemBackgroundBorderless() = run {
    val typedValue = android.util.TypedValue()
    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
    ContextCompat.getDrawable(this, typedValue.resourceId)
}

private fun matchFrame(gravity: Int = Gravity.NO_GRAVITY, block: FrameLayout.LayoutParams.() -> Unit = {}) =
        FrameLayout.LayoutParams(matchParent, matchParent, gravity).apply(block)

private fun matchWrapFrame(gravity: Int, block: FrameLayout.LayoutParams.() -> Unit = {}) =
        FrameLayout.LayoutParams(matchParent, wrapContent, gravity).apply(block)

private fun frameWrap(gravity: Int, block: FrameLayout.LayoutParams.() -> Unit = {}) =
        FrameLayout.LayoutParams(wrapContent, wrapContent, gravity).apply(block)

private fun constraintLayout(width: Int, height: Int, block: ConstraintLayout.LayoutParams.() -> Unit = {}) =
        ConstraintLayout.LayoutParams(width, height).apply(block)

private const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
private const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT
private const val matchConstraint = 0
