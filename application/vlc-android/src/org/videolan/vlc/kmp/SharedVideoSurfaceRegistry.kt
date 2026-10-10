package org.videolan.vlc.kmp

import android.app.Activity
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.resources.AppContextProvider
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
        /** Android TV: VideoPlayerActivity owns the vout and replays the request itself. */
        LEGACY_ACTIVITY,
    }

    /** True while the shared PlayerRoute is composed (audio artwork or video). */
    @Volatile
    private var hostActive = false

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
        hostActive = true
        AndroidPlaybackHost.instance?.removePopup()
    }

    fun deactivateHost() {
        hostActive = false
        pendingPlayback = null
        detachSurface()
    }

    internal fun currentLayout(): VLCVideoLayout? = layout

    /** True when the shared route currently owns the native video output. */
    fun ownsVideoOutput(): Boolean = hasAttachedSurface()

    /**
     * Chooses exactly one native video owner. A request is either sent through the attached
     * shared output or held until the route creates it; it never opens a second activity/output.
     */
    fun routeVideoPlayback(manager: PlaylistManager, index: Int): VideoPlaybackRoute {
        if (!SharedPlayerLauncher.usesSharedPlayer()) return VideoPlaybackRoute.LEGACY_ACTIVITY
        if (hasAttachedSurface()) return VideoPlaybackRoute.INLINE
        pendingPlayback = {
            manager.launch {
                manager.playIndex(index, forceInline = true)
            }
        }
        // Nobody would ever compose the route for a request started outside the player (an
        // external intent, a shortcut, a widget): show it, or the request would wait forever.
        if (!hostActive) SharedPlayerLauncher.open(AppContextProvider.appContext)
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

/** Current frame of the shared route's TextureView, or null when no video is shown. */
internal fun SharedVideoSurfaceRegistry.captureFrame(): android.graphics.Bitmap? {
    val root = currentLayout() ?: return null
    fun find(view: android.view.View): android.view.TextureView? = when (view) {
        is android.view.TextureView -> view
        is android.view.ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) }
        else -> null
    }
    val texture = find(root)?.takeIf { it.isAvailable } ?: return null
    return texture.bitmap
}
