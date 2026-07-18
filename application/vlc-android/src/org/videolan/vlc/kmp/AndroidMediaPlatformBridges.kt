package org.videolan.vlc.kmp

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.util.Rational
import kotlinx.coroutines.launch
import org.videolan.tools.AppScope
import org.videolan.vlc.PlaybackService as AndroidPlaybackHost
import org.videolan.vlc.RendererDelegate
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Progress
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.platform.RendererInfo
import org.videolan.vlc.platform.RendererType
import org.videolan.vlc.platform.SessionActions
import java.lang.ref.WeakReference

/**
 * Bridges shared [MediaSessionBridge] to the existing Android playback host.
 * Session ownership stays in [AndroidPlaybackHost]; this keeps
 * [org.videolan.vlc.player.PlaybackController] portable.
 */
class AndroidMediaSessionBridge(
    private val appContext: Context,
) : MediaSessionBridge {
    override fun activate() {
        AndroidPlaybackHost.start(appContext)
    }

    override fun deactivate() {}

    override fun updateMetadata(item: MediaItem?) {}

    override fun updatePlayback(playing: Boolean, progress: Progress) {}

    override fun setActions(actions: SessionActions) {}
}

class AndroidPipController : PipController {
    private var activityRef: WeakReference<Activity>? = null

    fun attachActivity(activity: Activity?) {
        activityRef = activity?.let { WeakReference(it) }
    }

    override val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    override fun enterPip(): Boolean {
        val activity = activityRef?.get() ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun exitPip() {}

    override fun isInPip(): Boolean {
        val activity = activityRef?.get() ?: return false
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode
    }
}

class AndroidRendererBridge : RendererBridge {
    override fun startDiscovery() {
        AppScope.launch { runCatching { RendererDelegate.start() } }
    }

    override fun stopDiscovery() {
        runCatching { RendererDelegate.stop() }
    }

    override fun listRenderers(): List<RendererInfo> {
        return runCatching {
            RendererDelegate.renderers.getList().map { r ->
                RendererInfo(
                    id = r.name ?: r.toString(),
                    name = r.name ?: "Renderer",
                    type = RendererType.CHROMECAST,
                )
            }
        }.getOrDefault(emptyList())
    }

    override fun selectRenderer(id: String?): Boolean {
        return runCatching {
            if (id == null) {
                AndroidPlaybackHost.instance?.setRenderer(null)
                true
            } else {
                val match = RendererDelegate.renderers.getList().firstOrNull { it.name == id }
                if (match != null) {
                    AndroidPlaybackHost.instance?.setRenderer(match)
                    true
                } else false
            }
        }.getOrDefault(false)
    }

    override fun currentRendererId(): String? =
        AndroidPlaybackHost.renderer.value?.name
}
