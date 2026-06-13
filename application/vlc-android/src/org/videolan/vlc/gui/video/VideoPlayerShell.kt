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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp as composeDp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.videolan.libvlc.util.VLCVideoLayout
import org.videolan.tools.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.view.createVideoHudOverlay
import org.videolan.vlc.gui.view.installPlayerOptionsPanelHost
import org.videolan.vlc.gui.view.installVideoHudRightOverlayHost
import org.videolan.vlc.gui.view.installVideoDelayOverlayHost
import org.videolan.vlc.gui.view.installVideoInfoOverlayHost
import org.videolan.vlc.gui.view.installVideoLoadingOverlayHost
import org.videolan.vlc.gui.view.installVideoOrientationOverlayHost
import org.videolan.vlc.gui.view.installVideoPlaylistSearchHost
import org.videolan.vlc.gui.view.installVideoResizeOverlayHost
import org.videolan.vlc.gui.view.installVideoScreenshotOverlayHost
import org.videolan.vlc.gui.view.installVideoSeekOverlayHost
import org.videolan.vlc.gui.view.installVideoTipsHost
import org.videolan.vlc.gui.view.installVideoVerticalProgressOverlayHost

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
    addView(gradientDrawableHost(R.id.hud_background, R.drawable.gradient_hud_player), FrameLayout.LayoutParams(matchParent, 130.dp, Gravity.BOTTOM))
    addView(gradientDrawableHost(R.id.hud_right_background, R.drawable.gradient_title_player), FrameLayout.LayoutParams(matchParent, 150.dp))
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
    addView(VLCComposeView(context).apply {
        id = R.id.player_overlay_tips
        visibility = View.GONE
        installVideoTipsHost()
    }, matchFrame())
    addView(FrameLayout(context).apply {
        id = R.id.player_overlay_background
        fitsSystemWindows = true
        addView(staticIconHost(R.id.renderer_background_cone, R.drawable.ic_renderer_background_cone, tint = false, sizeDp = null), frameWrap(Gravity.CENTER))
        addView(infoOverlay(), frameWrap(Gravity.CENTER) {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
        })
        addView(FrameLayout(context).apply {
            id = R.id.player_ui_container
            addView(loadingView(), frameWrap(Gravity.CENTER))
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
    addView(VLCComposeView(context).apply {
        id = R.id.player_overlay_tips
        visibility = View.GONE
        installVideoTipsHost()
    }, matchFrame())
    addView(loadingView(), FrameLayout.LayoutParams(80.dp, 80.dp, Gravity.CENTER))
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

private fun Context.seekOverlay() = VLCComposeView(this).apply {
    id = R.id.seekContainer
    fitsSystemWindows = false
    visibility = View.INVISIBLE
    installVideoSeekOverlayHost()
}

private fun Context.verticalOverlay(id: Int) = VLCComposeView(this).apply {
    this.id = id
    visibility = View.INVISIBLE
    isClickable = false
    isFocusable = false
    installVideoVerticalProgressOverlayHost()
}

private fun Context.loadingView() = VLCComposeView(this).apply {
    id = R.id.player_overlay_loading
    installVideoLoadingOverlayHost()
}

private fun Context.gradientDrawableHost(id: Int, drawable: Int) = VLCComposeView(this).apply {
    this.id = id
    visibility = View.GONE
    setContent {
        Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
        )
    }
}

private fun Context.infoOverlay() = VLCComposeView(this).apply {
    id = R.id.player_info_stub
    visibility = View.INVISIBLE
    isClickable = false
    isFocusable = false
    installVideoInfoOverlayHost()
}

private fun Context.hudRightOverlay() = VLCComposeView(this).apply {
    id = R.id.hud_right_overlay
    visibility = View.INVISIBLE
    installVideoHudRightOverlayHost()
}

private fun Context.hingeArrow(id: Int, icon: Int) = staticIconHost(id, icon, tint = true, sizeDp = 48).apply {
    this.id = id
    isClickable = true
    isFocusable = true
    background = selectableItemBackgroundBorderless()
    visibility = View.GONE
}

private fun Context.staticIconHost(id: Int, icon: Int, tint: Boolean, sizeDp: Int?) = VLCComposeView(this).apply {
    this.id = id
    setContent {
        VLCTheme {
            Box(
                    modifier = sizeDp?.let { Modifier.size(it.composeDp) } ?: Modifier,
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = if (tint) VLCThemeDefaults.colors.playerIconColor else androidx.compose.ui.graphics.Color.Unspecified
                )
            }
        }
    }
}

private fun Context.optionsPanel() = VLCComposeView(this).apply {
    id = R.id.options_background
    isClickable = true
    elevation = 16.dp.toFloat()
    isFocusable = false
    visibility = View.GONE
    installPlayerOptionsPanelHost()
}

private fun Context.videoPlaylistContainer(primary: Boolean) = ConstraintLayout(this).apply {
    id = R.id.video_playlist_container
    maxWidth = 480.dp
    visibility = View.GONE

    val background = playlistBackground()
    val closeButton = playlistCloseButton()
    val searchText = playlistSearchText()
    val playlist = VLCComposeView(context).apply {
        id = R.id.video_playlist
        clipToPadding = false
        isFocusable = true
        nextFocusUpId = R.id.playlist_search_text
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    addView(background, constraintLayout(matchConstraint, matchConstraint) {
        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
    })

    if (primary) {
        val header = playlistHeaderBackground()
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

private fun Context.playlistBackground() = VLCComposeView(this).apply {
    setContent {
        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(R.color.playerbackground))
        )
    }
}

private fun Context.playlistSearchText() = VLCComposeView(this).apply {
    id = R.id.playlist_search_text
    visibility = View.VISIBLE
    setPadding(8.dp, 0, 8.dp, 0)
    isFocusable = true
    nextFocusDownId = R.id.video_playlist
    installVideoPlaylistSearchHost(getString(R.string.search_hint))
}

private fun Context.playlistCloseButton() = VLCComposeView(this).apply {
    id = R.id.close_button
    background = selectableItemBackgroundBorderless()
    contentDescription = getString(R.string.close)
    setContent {
        VLCTheme {
            Box(
                    modifier = Modifier
                            .size(48.composeDp)
                            .padding(8.composeDp),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        painter = painterResource(R.drawable.ic_close_small),
                        contentDescription = null,
                        tint = VLCThemeDefaults.colors.playerIconColor
                )
            }
        }
    }
}

private fun Context.playlistHeaderBackground() = VLCComposeView(this).apply {
    id = R.id.video_playlist_header
    setContent {
        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(R.color.playerbackground))
        )
    }
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
