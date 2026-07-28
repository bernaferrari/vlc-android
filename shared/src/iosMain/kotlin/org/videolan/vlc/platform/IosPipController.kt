package org.videolan.vlc.platform

/**
 * Swift-owned controller for the public VLCKit PiP drawable.  Kotlin owns the
 * product decision and invokes this narrow adapter; Swift owns UIKit and the
 * VLCKit lifetime.
 */
interface IosPipHandler {
    val isSupported: Boolean
    fun enter(): Boolean
    fun exit()
    fun isActive(): Boolean
}

object IosPipController : PipController {
    private var handler: IosPipHandler? = null

    fun setHandler(handler: IosPipHandler?) {
        this.handler = handler
    }

    override val isSupported: Boolean
        get() = handler?.isSupported == true

    override fun enterPip(): Boolean = handler?.enter() == true

    override fun exitPip() {
        handler?.exit()
    }

    override fun isInPip(): Boolean = handler?.isActive() == true
}
