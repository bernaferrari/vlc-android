package org.videolan.vlc.kmp

import android.app.Activity
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.DisplayManager
import org.videolan.libvlc.util.VLCVideoLayout
import org.videolan.vlc.PlaybackService as AndroidPlaybackHost
import org.videolan.vlc.media.PlaylistManager

/**
 * Coordinates the one native Android video output with the shared Compose
 * PlayerRoute. The legacy playlist manager decides to launch VideoPlayerActivity
 * before Compose has had a chance to create a view; this registry holds that
 * first request until the route-owned [VLCVideoLayout] is ready instead.
 */
internal object SharedVideoSurfaceRegistry {
    private var hostActive = false
    private var inlinePlaybackRequested = false
    private var layout: VLCVideoLayout? = null
    private var displayManager: DisplayManager? = null
    private var attachedPlayer: MediaPlayer? = null
    private var pendingPlayback: (() -> Unit)? = null

    fun requestInlinePlayback() {
        inlinePlaybackRequested = true
    }

    fun activateHost() {
        hostActive = true
    }

    fun deactivateHost() {
        hostActive = false
        inlinePlaybackRequested = false
        pendingPlayback = null
        detachSurface()
    }

    /**
     * Returns true when a legacy video launch must wait for the shared surface.
     * The pending [PlaylistManager.playIndex] call is replayed once attachment
     * has completed, preserving the existing decoder and playlist machinery.
     */
    fun deferLegacyVideoPlayback(manager: PlaylistManager, index: Int): Boolean {
        if (!hostActive && !inlinePlaybackRequested) return false
        if (hasAttachedSurface()) return false
        pendingPlayback = { manager.launch { manager.playIndex(index) } }
        return true
    }

    fun attachSurface(surface: VLCVideoLayout) {
        if (layout !== surface) {
            detachSurface()
            layout = surface
        }
        attachToLivePlayer()
        pendingPlayback?.let { replay ->
            pendingPlayback = null
            inlinePlaybackRequested = false
            replay()
        }
    }

    fun releaseSurface(surface: VLCVideoLayout) {
        if (layout === surface) detachSurface()
    }

    private fun hasAttachedSurface(): Boolean =
        attachedPlayer?.vlcVout?.areViewsAttached() == true

    private fun attachToLivePlayer() {
        val surface = layout ?: return
        val activity = surface.context as? Activity ?: return
        val player = AndroidPlaybackHost.instance?.mediaplayer ?: return
        if (player.isReleased) return
        if (player.vlcVout.areViewsAttached()) player.vlcVout.detachViews()
        displayManager?.release()
        displayManager = DisplayManager(
            activity,
            AndroidPlaybackHost.renderer,
            false,
            false,
            false,
        )
        player.attachViews(surface, displayManager, true, false)
        attachedPlayer = player
    }

    private fun detachSurface() {
        attachedPlayer?.let { player ->
            if (!player.isReleased && player.vlcVout.areViewsAttached()) {
                player.vlcVout.detachViews()
            }
        }
        attachedPlayer = null
        displayManager?.release()
        displayManager = null
        layout = null
    }
}
