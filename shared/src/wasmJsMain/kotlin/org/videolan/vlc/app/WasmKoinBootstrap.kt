package org.videolan.vlc.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.component.get
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings

/** Starts the browser-safe shared graph used by the Wasm Compose host. */
object WasmKoinBootstrap {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (VlcKoin.isStarted) return
        val app = startKoin {
            modules(platformModule, sharedModule)
        }
        VlcKoin.set(app.koin)
        // Keep browser reloads in the same shared theme and media-settings state as iOS/Android.
        scope.launch {
            runCatching { VlcSettings.load(app.koin.get<VlcPreferences>()) }
        }
    }
}
