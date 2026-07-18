package org.videolan.vlc.app

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Progress
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.SessionActions
import platform.Foundation.NSNumber
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter

/**
 * iOS lock-screen / Control Center now-playing integration.
 * Requires UIBackgroundModes: audio in Info.plist.
 */
class IosMediaSessionBridge : MediaSessionBridge {
    private var active = false
    private var lastMeta: Map<Any?, Any?> = emptyMap()

    override fun activate() {
        active = true
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.playCommand.enabled = true
        center.pauseCommand.enabled = true
        center.nextTrackCommand.enabled = true
        center.previousTrackCommand.enabled = true

        center.playCommand.addTargetWithHandler { _ ->
            IosPlaybackService.shared.resume()
            platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
        }
        center.pauseCommand.addTargetWithHandler { _ ->
            IosPlaybackService.shared.pause()
            platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
        }
        center.nextTrackCommand.addTargetWithHandler { _ ->
            IosPlaybackService.shared.next()
            platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
        }
        center.previousTrackCommand.addTargetWithHandler { _ ->
            IosPlaybackService.shared.previous()
            platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
        }
    }

    override fun deactivate() {
        active = false
        lastMeta = emptyMap()
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    override fun updateMetadata(item: MediaItem?) {
        if (!active) return
        if (item == null) {
            lastMeta = emptyMap()
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
            return
        }
        val info = mutableMapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to item.displayTitle,
        )
        item.artist?.let { info[MPMediaItemPropertyArtist] = it }
        if (item.duration > 0) {
            info[MPMediaItemPropertyPlaybackDuration] = NSNumber(double = item.duration / 1000.0)
        }
        lastMeta = info
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    override fun updatePlayback(playing: Boolean, progress: Progress) {
        if (!active) return
        val info = lastMeta.toMutableMap()
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = NSNumber(double = progress.time / 1000.0)
        info[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(double = if (playing) 1.0 else 0.0)
        if (progress.length > 0) {
            info[MPMediaItemPropertyPlaybackDuration] = NSNumber(double = progress.length / 1000.0)
        }
        lastMeta = info
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    override fun setActions(actions: SessionActions) {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.playCommand.enabled = actions.play
        center.pauseCommand.enabled = actions.pause
        center.nextTrackCommand.enabled = actions.skipNext
        center.previousTrackCommand.enabled = actions.skipPrevious
    }
}
