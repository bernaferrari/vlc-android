package org.videolan.vlc.app

import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.platform.RendererInfo
import org.videolan.vlc.platform.RendererType

/** Portable renderer value exported by the Swift MobileVLCKit adapter. */
data class VlcKitRendererInfo(
    val id: String,
    val name: String,
    val type: String,
)

/** Swift implementation owns the transient VLCRendererItem instances. */
interface VlcKitRendererBackend {
    fun startRendererDiscovery()
    fun stopRendererDiscovery()
    fun renderers(): List<VlcKitRendererInfo>
    fun selectRenderer(id: String?): Boolean
    fun currentRendererId(): String?
}

object IosRendererController {
    private var backend: VlcKitRendererBackend? = null

    fun setBackend(backend: VlcKitRendererBackend?) {
        this.backend?.stopRendererDiscovery()
        this.backend = backend
    }

    fun startDiscovery() = backend?.startRendererDiscovery()
    fun stopDiscovery() = backend?.stopRendererDiscovery()
    fun renderers(): List<VlcKitRendererInfo> = backend?.renderers().orEmpty()
    fun selectRenderer(id: String?): Boolean = backend?.selectRenderer(id) ?: false
    fun currentRendererId(): String? = backend?.currentRendererId()
}

/** iOS MobileVLCKit renderer bridge consumed by the shared renderer picker. */
class IosRendererBridge : RendererBridge {
    override fun startDiscovery() {
        IosRendererController.startDiscovery()
    }
    override fun stopDiscovery() {
        IosRendererController.stopDiscovery()
    }
    override fun listRenderers(): List<RendererInfo> = IosRendererController.renderers().map {
        RendererInfo(id = it.id, name = it.name, type = it.type.toRendererType())
    }
    override fun selectRenderer(id: String?): Boolean = IosRendererController.selectRenderer(id)
    override fun currentRendererId(): String? = IosRendererController.currentRendererId()
}

private fun String.toRendererType(): RendererType = when (lowercase()) {
    "chromecast" -> RendererType.CHROMECAST
    "airplay" -> RendererType.AIRPLAY
    "dlna", "upnp" -> RendererType.DLNA
    else -> RendererType.OTHER
}
