package org.videolan.vlc.app

import org.koin.core.context.startKoin

/**
 * Starts the shared Koin graph for iOS. Safe to call once from Swift `onAppear`.
 */
object IosKoinBootstrap {
    fun start() {
        if (VlcKoin.isStarted) return
        val koinApp = startKoin {
            modules(platformModule, sharedModule)
        }
        VlcKoin.set(koinApp.koin)
    }
}
