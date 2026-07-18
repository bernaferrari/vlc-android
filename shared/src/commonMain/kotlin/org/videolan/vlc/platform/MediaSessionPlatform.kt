package org.videolan.vlc.platform

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Progress

/**
 * Background playback / lock-screen / headset integration.
 * Android → MediaSessionCompat; iOS → MPNowPlayingInfoCenter + AVAudioSession.
 */
interface MediaSessionBridge {
    fun activate()
    fun deactivate()
    fun updateMetadata(item: MediaItem?)
    fun updatePlayback(playing: Boolean, progress: Progress)
    fun setActions(actions: SessionActions)
}

data class SessionActions(
    val play: Boolean = true,
    val pause: Boolean = true,
    val stop: Boolean = true,
    val seek: Boolean = true,
    val skipNext: Boolean = true,
    val skipPrevious: Boolean = true,
)

/**
 * Picture-in-Picture control surface.
 * Android → enterPictureInPictureMode; iOS → AVPictureInPictureController when available.
 */
interface PipController {
    val isSupported: Boolean
    fun enterPip(): Boolean
    fun exitPip()
    fun isInPip(): Boolean
}

/**
 * External renderer / cast (Chromecast, AirPlay discovery hooks).
 * Platforms fill discovery; shared UI only lists [RendererInfo].
 */
data class RendererInfo(
    val id: String,
    val name: String,
    val type: RendererType,
)

enum class RendererType { CHROMECAST, AIRPLAY, DLNA, OTHER }

interface RendererBridge {
    fun startDiscovery()
    fun stopDiscovery()
    fun listRenderers(): List<RendererInfo>
    fun selectRenderer(id: String?): Boolean
    fun currentRendererId(): String?
}

/** No-op defaults for platforms / tests without session features. */
object NoOpMediaSessionBridge : MediaSessionBridge {
    override fun activate() {}
    override fun deactivate() {}
    override fun updateMetadata(item: MediaItem?) {}
    override fun updatePlayback(playing: Boolean, progress: Progress) {}
    override fun setActions(actions: SessionActions) {}
}

object NoOpPipController : PipController {
    override val isSupported: Boolean = false
    override fun enterPip(): Boolean = false
    override fun exitPip() {}
    override fun isInPip(): Boolean = false
}

object NoOpRendererBridge : RendererBridge {
    override fun startDiscovery() {}
    override fun stopDiscovery() {}
    override fun listRenderers(): List<RendererInfo> = emptyList()
    override fun selectRenderer(id: String?): Boolean = false
    override fun currentRendererId(): String? = null
}
