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
 * PlayerRoute. Playback requests are held until the route-owned [VLCVideoLayout]
 * is ready, so one player and one native output remain authoritative.
 */
internal object SharedVideoSurfaceRegistry {
    internal enum class VideoPlaybackRoute {
        DEFERRED,
        INLINE,
    }

    private var layout: VLCVideoLayout? = null
    private var displayManager: DisplayManager? = null
    private var attachedPlayer: MediaPlayer? = null
    private var pendingPlayback: (() -> Unit)? = null

    fun requestInlinePlayback() {
        // A previous Android popup owns a second vout and can remain visible while the shared
        // player route is being composed. The shared shell is the sole video owner now.
        AndroidPlaybackHost.instance?.removePopup()
    }

    fun activateHost() {
        AndroidPlaybackHost.instance?.removePopup()
    }

    fun deactivateHost() {
        pendingPlayback = null
        detachSurface()
    }

    /**
     * Chooses exactly one native video owner. A request is either sent through the attached
     * shared output or held until the route creates it; it never opens a second activity/output.
     */
    fun routeVideoPlayback(manager: PlaylistManager, index: Int): VideoPlaybackRoute {
        if (hasAttachedSurface()) return VideoPlaybackRoute.INLINE
        pendingPlayback = {
            manager.launch {
                manager.playIndex(index, forceInline = true)
            }
        }
        return VideoPlaybackRoute.DEFERRED
    }

    fun attachSurface(surface: VLCVideoLayout) {
        if (layout !== surface) {
            detachSurface()
            layout = surface
        }
        attachToLivePlayer()
        // The service can start a frame after Compose has created the view. Keep the request
        // queued until the vout is really attached; replaying earlier would immediately queue a
        // second request and leave playback waiting for a future lifecycle event.
        pendingPlayback?.takeIf { hasAttachedSurface() }?.let { replay ->
            pendingPlayback = null
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
            true,
            false,
            false,
        )
        // TextureView is deliberately used for the shared Compose route. A SurfaceView is
        // positioned by SurfaceFlinger outside the Compose hierarchy and can be left offset
        // after a Nav3 transition or a window-inset change.
        player.attachViews(surface, displayManager, true, true)
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
