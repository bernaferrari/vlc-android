/*
 * ************************************************************************
 *  VideoPlayerScreenshotDelegate.kt
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

import android.graphics.Bitmap
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.view.videoScreenshotOverlayHost
import org.videolan.vlc.util.getScreenHeight
import org.videolan.vlc.util.share
import java.io.File

class VideoPlayerScreenshotDelegate(private val player: VideoPlayerActivity) {


    private lateinit var screenshotOverlay: VLCComposeView

    /**
     * Retrieves the Compose screenshot overlay host.
     *
     */
    private fun initScreenshot() {
        screenshotOverlay = player.findViewById(R.id.player_screenshot_stub)
        screenshotOverlay.setVisible()
    }

    /**
     * Display the screenshot that has just been taken
     *
     * @param dst the screenshot file
     * @param bitmap the screenshot bitmap
     * @param surfaceBounds the surface view bounds
     * @param width the screenshot width
     * @param height the screenshot height
     */
    fun takeScreenshot(dst: File, bitmap: Bitmap, surfaceBounds: IntArray, width: Int, height: Int) {
        initScreenshot()
        screenshotOverlay.videoScreenshotOverlayHost().showScreenshot(
            file = dst,
            bitmap = bitmap,
            surfaceBounds = surfaceBounds,
            width = width,
            height = height,
            screenHeight = player.getScreenHeight(),
            onShare = { player.share(it) }
        )

        player.handler.removeMessages(VideoPlayerActivity.FADE_OUT_SCREENSHOT)
        player.handler.sendEmptyMessageDelayed(VideoPlayerActivity.FADE_OUT_SCREENSHOT, 5000)

    }

    /**
     * Hides the screenshot UI
     *
     */
    fun hide() {
        if (::screenshotOverlay.isInitialized) screenshotOverlay.videoScreenshotOverlayHost().hideScreenshot()
    }

}
