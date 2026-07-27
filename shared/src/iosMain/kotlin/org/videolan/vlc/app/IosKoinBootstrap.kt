package org.videolan.vlc.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.component.get
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings

/**
 * Starts the shared Koin graph for iOS. Safe to call once from Swift `onAppear`.
 */
object IosKoinBootstrap {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var preferencesReady = false
    private val readyCallbacks = mutableListOf<() -> Unit>()

    fun start() {
        if (VlcKoin.isStarted) return
        val koinApp = startKoin {
            modules(platformModule, sharedModule)
        }
        VlcKoin.set(koinApp.koin)
        scope.launch {
            // Playback/session restoration must wait for this cache. Without it,
            // a persisted incognito choice is briefly indistinguishable from the
            // default false value during app launch.
            runCatching { VlcSettings.load(koinApp.koin.get<VlcPreferences>()) }
            preferencesReady = true
            readyCallbacks.toList().also { readyCallbacks.clear() }.forEach { it() }
        }
    }

    /** Runs on the iOS main dispatcher after persisted shared settings are available. */
    fun whenPreferencesReady(action: () -> Unit) {
        if (preferencesReady) action() else readyCallbacks += action
    }
}
