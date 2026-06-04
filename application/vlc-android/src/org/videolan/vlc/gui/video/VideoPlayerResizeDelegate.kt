/*
 * ************************************************************************
 *  VideoPlayerResizeDelegate.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 *
 *
 */

package org.videolan.vlc.gui.video

import android.view.View
import android.view.WindowManager
import androidx.core.content.edit
import org.videolan.libvlc.MediaPlayer
import org.videolan.tools.ALLOW_FOLD_AUTO_LAYOUT
import org.videolan.tools.DISPLAY_UNDER_NOTCH
import org.videolan.tools.Settings
import org.videolan.tools.VIDEO_RATIO
import org.videolan.tools.putSingle
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.view.videoResizeOverlayHost

class VideoPlayerResizeDelegate(private val player: VideoPlayerActivity) {
    private val overlayDelegate: VideoPlayerOverlayDelegate
        get() = player.overlayDelegate
    private lateinit var resizeMainView: VLCComposeView

    /**
     * Check if the resize overlay is currently shown
     * @return true if it's shown
     */
    fun isShowing() = ::resizeMainView.isInitialized && resizeMainView.visibility == View.VISIBLE

    /**
     * Show the resize overlay. Inflate it if it's not yet
     */
    fun showResizeOverlay() {
        val settings = Settings.getInstance(player)
        val resizeView = player.findViewById<VLCComposeView>(R.id.player_resize_stub) ?: return
        val displayUnderNotch = settings.getInt(
            DISPLAY_UNDER_NOTCH,
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        )
        resizeMainView = resizeView
        resizeMainView.videoResizeOverlayHost().bind(
            selectedScale = player.service?.mediaplayer?.videoScale ?: MediaPlayer.ScaleType.SURFACE_BEST_FIT,
            showFoldSection = player.overlayDelegate.foldingFeature != null,
            foldChecked = settings.getBoolean(ALLOW_FOLD_AUTO_LAYOUT, true),
            showNotchSection = player.hasPhysicalNotch,
            notchChecked = displayUnderNotch == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
            onDismiss = { hideResizeOverlay() },
            onFoldCheckedChange = { isChecked ->
                settings.edit { putBoolean(ALLOW_FOLD_AUTO_LAYOUT, isChecked) }
                player.overlayDelegate.manageHinge()
            },
            onNotchCheckedChange = { isChecked ->
                val cutoutsAttributes = if (isChecked) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
                }
                player.window.attributes.layoutInDisplayCutoutMode = cutoutsAttributes
                settings.edit { putInt(DISPLAY_UNDER_NOTCH, cutoutsAttributes) }
                // needed to apply the new pref
                overlayDelegate.showOverlay(true)
                overlayDelegate.hideOverlay(true)
            },
            onScaleSelected = { scale -> setVideoScale(scale) }
        )
        resizeMainView.setVisible()
        if (Settings.showTvUi) resizeMainView.videoResizeOverlayHost().requestInitialFocus()
    }

    /**
     * Hide the overlay
     */
    fun hideResizeOverlay() {
        if (::resizeMainView.isInitialized) resizeMainView.setGone()
    }

    /**
     * Resize the video layout to a aspect ratio. It uses the next aspect ratio in line to loop in the different aspect ratio of  [MediaPlayer.ScaleType.getMainScaleTypes()]
     */
    fun resizeVideo() = player.service?.run {
        val currentScaleIndex = MediaPlayer.ScaleType.getMainScaleTypes().indexOf(mediaplayer.videoScale) + 1
        val nextScale = MediaPlayer.ScaleType.getMainScaleTypes()[currentScaleIndex.coerceAtLeast(0) % MediaPlayer.ScaleType.getMainScaleTypes().size]
        setVideoScale(nextScale)
        player.handler.sendEmptyMessage(VideoPlayerActivity.SHOW_INFO)
    }

    /**
     * Resize the video layout to a given aspect ratio
     * @param scale the new aspect ratio
     */
    fun setVideoScale(scale: MediaPlayer.ScaleType) = player.service?.run {
        mediaplayer.videoScale = scale
        when (scale) {
            MediaPlayer.ScaleType.SURFACE_BEST_FIT -> overlayDelegate.showInfo(R.string.surface_best_fit, 1000, R.string.resize_tip)
            MediaPlayer.ScaleType.SURFACE_FIT_SCREEN -> overlayDelegate.showInfo(R.string.surface_fit_screen, 1000, R.string.resize_tip)
            MediaPlayer.ScaleType.SURFACE_FILL -> overlayDelegate.showInfo(R.string.surface_fill, 1000, R.string.resize_tip)
            MediaPlayer.ScaleType.SURFACE_16_9 -> overlayDelegate.showInfo("16:9", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_4_3 -> overlayDelegate.showInfo("4:3", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_16_10 -> overlayDelegate.showInfo("16:10", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_2_1 -> overlayDelegate.showInfo("2:1", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_221_1 -> overlayDelegate.showInfo("2.21:1", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_235_1 -> overlayDelegate.showInfo("2.35:1", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_239_1 -> overlayDelegate.showInfo("2.39:1", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_5_4 -> overlayDelegate.showInfo("5:4", 1000, player.getString(R.string.resize_tip))
            MediaPlayer.ScaleType.SURFACE_ORIGINAL -> overlayDelegate.showInfo(R.string.surface_original, 1000, R.string.resize_tip)
        }
        settings.putSingle(VIDEO_RATIO, scale.ordinal)
    }

    /**
     * display the resize overlay and hide everything else
     */
    fun displayResize(): Boolean {
        if (player.service?.hasRenderer() == true) return false
        showResizeOverlay()
        overlayDelegate.hideOverlay(true)
        return true
    }
}
