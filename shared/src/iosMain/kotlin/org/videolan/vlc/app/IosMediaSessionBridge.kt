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
 * iOS lock-screen / Control Center state mirror.
 *
 * MobileVLCKit owns the actual remote-command registrations because it also owns
 * decoder-specific seek, skip-interval, and playback-rate handling. Registering
 * the same commands here causes Control Center to dispatch actions twice (most
 * visibly skipping two tracks). This bridge deliberately owns only the shared
 * KMP playback state and Now Playing metadata.
 */
class IosMediaSessionBridge : MediaSessionBridge {
    private var active = false
    private var lastMeta: Map<Any?, Any?> = emptyMap()

    override fun activate() {
        if (active) return
        active = true
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.playCommand.enabled = true
        center.pauseCommand.enabled = true
        center.nextTrackCommand.enabled = true
        center.previousTrackCommand.enabled = true
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

    override fun updatePlayback(playing: Boolean, progress: Progress, rate: Float) {
        if (!active) return
        val info = lastMeta.toMutableMap()
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = NSNumber(double = progress.time / 1000.0)
        info[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(double = if (playing) rate.toDouble() else 0.0)
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
