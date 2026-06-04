/*
 * ************************************************************************
 *  VideoDelayDelegate.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import org.videolan.tools.AUDIO_DELAY_GLOBAL
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.tools.setInvisible
import org.videolan.tools.setVisible
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.view.VideoDelayOverlayAction
import org.videolan.vlc.gui.view.videoDelayOverlayHost
import org.videolan.vlc.gui.video.VideoPlayerActivity.Companion.KEY_BLUETOOTH_DELAY
import org.videolan.vlc.interfaces.IPlaybackSettingsController
import org.videolan.vlc.media.DelayValues
import org.videolan.vlc.util.isTalkbackIsEnabled

private const val DELAY_DEFAULT_VALUE = 50000L

/**
 * Delegate for delay management.
 *
 * @property player the player activity instance the delegate is attached to
 */
class VideoDelayDelegate(private val player: VideoPlayerActivity) : IPlaybackSettingsController {
    var playbackSetting: IPlaybackSettingsController.DelayState = IPlaybackSettingsController.DelayState.OFF

    private var spuDelay = 0L
    private var audioDelay = 0L

    private lateinit var delayOverlay: VLCComposeView

    /**
     * Instantiate all the views, set their click listeners and shows the view.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showDelayControls() {
        player.touchDelegate.clearTouchAction()
        if (!player.displayManager.isPrimary) player.overlayDelegate.showOverlayTimeout(VideoPlayerActivity.OVERLAY_INFINITE)
        player.overlayDelegate.info.setInvisible()
        val overlay = getDelayOverlay()
        initPlaybackSettingInfo()
        overlay.videoDelayOverlayHost().show(
            title = currentTitle(),
            value = currentValue(),
            firstButtonText = if (playbackSetting == IPlaybackSettingsController.DelayState.AUDIO) player.getString(R.string.audio_delay_start) else player.getString(R.string.subtitle_delay_first),
            secondButtonText = if (playbackSetting == IPlaybackSettingsController.DelayState.AUDIO) player.getString(R.string.audio_delay_end) else player.getString(R.string.subtitle_delay_end),
            showApplyAll = playbackSetting == IPlaybackSettingsController.DelayState.AUDIO,
            showApplyBluetooth = playbackSetting == IPlaybackSettingsController.DelayState.AUDIO && (player.audiomanager.isBluetoothA2dpOn || player.audiomanager.isBluetoothScoOn)
        )
        overlay.setVisible()
        overlay.videoDelayOverlayHost().requestPlusFocus()
        if (player.displayManager.isPrimary) player.overlayDelegate.hideOverlay(fromUser = true, forceTalkback = true)
    }

    /**
     * Initialize the whole delay view state
     *
     */
    private fun initPlaybackSettingInfo() {
        player.overlayDelegate.initInfoOverlay()
        if (::delayOverlay.isInitialized) delayOverlay.videoDelayOverlayHost().updateDelayInfo(currentTitle(), currentValue())
    }

    /**
     * Click listener for all the views
     *
     */
    private fun onDelayAction(action: VideoDelayOverlayAction) {
        when (action) {
            VideoDelayOverlayAction.Decrease -> delayAudioOrSpu(-DELAY_DEFAULT_VALUE, delayState = playbackSetting)
            VideoDelayOverlayAction.Increase -> delayAudioOrSpu(DELAY_DEFAULT_VALUE, delayState = playbackSetting)
            VideoDelayOverlayAction.MarkStart -> if (player.service?.playlistManager?.delayValue?.value?.start ?: -1L == -1L) {
                player.service?.playlistManager?.setDelayValue(System.currentTimeMillis(), true)
                if (player.service?.playlistManager?.delayValue?.value?.stop == -1L) getDelayOverlay().videoDelayOverlayHost().requestSecondButtonFocus()
            } else {
                player.service?.playlistManager?.setDelayValue(-1L, true)
            }
            VideoDelayOverlayAction.MarkStop -> if (player.service?.playlistManager?.delayValue?.value?.stop ?: -1L == -1L) {
                player.service?.playlistManager?.setDelayValue(System.currentTimeMillis(), false)
                if (player.service?.playlistManager?.delayValue?.value?.start == -1L) getDelayOverlay().videoDelayOverlayHost().requestFirstButtonFocus()
            } else {
                player.service?.playlistManager?.setDelayValue(-1L, false)
            }
            VideoDelayOverlayAction.Reset -> {
                if (playbackSetting == IPlaybackSettingsController.DelayState.AUDIO) player.service?.setAudioDelay(0) else player.service?.setSpuDelay(0)
                if (::delayOverlay.isInitialized) delayOverlay.videoDelayOverlayHost().updateDelayInfo(currentTitle(), "0 ms")
                player.service?.playlistManager?.resetDelayValues()
            }
            VideoDelayOverlayAction.ApplyAll -> {
                player.service?.let {
                    Settings.getInstance(player).putSingle(AUDIO_DELAY_GLOBAL, it.audioDelay)
                    UiTools.snacker(player, player.getString(R.string.audio_delay_global, "${it.audioDelay / 1000L}"))
                }
            }
            VideoDelayOverlayAction.ApplyBluetooth -> {
                player.service?.let {
                    Settings.getInstance(player).putSingle(KEY_BLUETOOTH_DELAY, it.audioDelay)
                    UiTools.snacker(player, player.getString(R.string.audio_delay_bt, "${it.audioDelay / 1000L}"))
                }
            }
            VideoDelayOverlayAction.Close -> endPlaybackSetting()

        }
    }

    /**
     * Delay audio or spu for the video
     *
     * @param delta the delay to set
     * @param fromCustom does the delay change come from the custom buttons? If so, the delay should always be added to the previous one
     */
    fun delayAudioOrSpu(delta: Long, fromCustom: Boolean = false, delayState: IPlaybackSettingsController.DelayState) {
        if (delayState == IPlaybackSettingsController.DelayState.OFF) return
        player.service?.let { service ->
            val currentDelay = if (delayState == IPlaybackSettingsController.DelayState.SUBS) service.spuDelay else service.audioDelay
            val delay = currentDelay + when {
                // Comes from plus or minus buttons. We try to round it if needed
                !fromCustom && currentDelay % delta != 0L -> delta - (currentDelay % delta)
                else -> delta
            }
            player.overlayDelegate.initInfoOverlay()
            if (delayState == IPlaybackSettingsController.DelayState.SUBS) service.setSpuDelay(delay) else service.setAudioDelay(delay)
            if (::delayOverlay.isInitialized) {
                delayOverlay.videoDelayOverlayHost().updateDelayInfo(
                    title = player.getString(if (delayState == IPlaybackSettingsController.DelayState.SUBS) R.string.spu_delay else R.string.audio_delay),
                    value = "${delay / 1000L} ms"
                )
            }
            if (delayState == IPlaybackSettingsController.DelayState.SUBS) spuDelay = delay else audioDelay = delay
            if (!player.isPlaybackSettingActive) {
                playbackSetting = delayState
                showDelayControls()
            }
        }
    }


    /**
     * Set [playbackSetting] to the right value and shows the view
     *
     */
    override fun showAudioDelaySetting() {
        playbackSetting = IPlaybackSettingsController.DelayState.AUDIO
        showDelayControls()
    }

    /**
     * Set [playbackSetting] to the right value and shows the view
     *
     */
    override fun showSubsDelaySetting() {
        playbackSetting = IPlaybackSettingsController.DelayState.SUBS
        showDelayControls()
    }

    /**
     * Close the view and remove the listeners
     *
     */
    override fun endPlaybackSetting() {
        if (playbackSetting == IPlaybackSettingsController.DelayState.OFF) return
        player.service?.let { service ->
            service.saveMediaMeta()
            playbackSetting = IPlaybackSettingsController.DelayState.OFF
            if (::delayOverlay.isInitialized) delayOverlay.setInvisible()
            player.overlayDelegate.overlayInfo.setInvisible()
            service.playlistManager.delayValue.value = DelayValues()
            player.overlayDelegate.focusPlayPause()
        }
        if (player.isTalkbackIsEnabled()) player.overlayDelegate.showOverlay()
    }

    /**
     * Setup the delay values when the livedata has changed
     *
     * @param delayValues the new values for the delay
     * @param service the playback service instance
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun delayChanged(delayValues: DelayValues, service: PlaybackService) {
        var hasChanged = false
        if (delayValues.start != -1L && delayValues.stop != -1L) {
            val oldDelay = if (playbackSetting == IPlaybackSettingsController.DelayState.SUBS) service.spuDelay else service.audioDelay
            delayAudioOrSpu(delayValues.start * 1000 - delayValues.stop * 1000, fromCustom = true, delayState = playbackSetting)
            hasChanged = oldDelay != if (playbackSetting == IPlaybackSettingsController.DelayState.SUBS) service.spuDelay else service.audioDelay
            service.playlistManager.delayValue.postValue(DelayValues())
        }
        if (!::delayOverlay.isInitialized) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            delayOverlay.videoDelayOverlayHost().updateDelayMarkers(
                firstMarked = delayValues.start != -1L,
                secondMarked = delayValues.stop != -1L,
                flashInfo = hasChanged
            )
        }
    }

    private fun getDelayOverlay(): VLCComposeView {
        if (!::delayOverlay.isInitialized) {
            delayOverlay = player.findViewById(R.id.delay_container)
            delayOverlay.videoDelayOverlayHost().onAction = ::onDelayAction
        }
        return delayOverlay
    }

    private fun currentTitle() = when (playbackSetting) {
        IPlaybackSettingsController.DelayState.AUDIO -> player.getString(R.string.audio_delay)
        IPlaybackSettingsController.DelayState.SUBS -> player.getString(R.string.spu_delay)
        else -> ""
    }

    private fun currentValue() = when (playbackSetting) {
        IPlaybackSettingsController.DelayState.AUDIO -> "${player.service!!.audioDelay / 1000L} ms"
        IPlaybackSettingsController.DelayState.SUBS -> "${player.service!!.spuDelay / 1000L} ms"
        else -> "0"
    }
}
