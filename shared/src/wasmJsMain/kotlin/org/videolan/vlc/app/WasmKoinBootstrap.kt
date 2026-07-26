package org.videolan.vlc.app

import org.koin.core.context.startKoin

/** Starts the browser-safe shared graph used by the Wasm Compose host. */
object WasmKoinBootstrap {
    fun start() {
        if (VlcKoin.isStarted) return
        val app = startKoin {
            modules(platformModule, sharedModule)
        }
        VlcKoin.set(app.koin)
    }
}
