package org.videolan.vlc.app

import org.koin.core.Koin

/**
 * Cross-platform Koin instance holder.
 *
 * Koin's [GlobalContext] is JVM-only in Koin 4.x, so we provide our own
 * global holder that works on iOS/Native. Each platform's startup code
 * calls [set] after [startKoin] (Android) or [koinApplication] (iOS).
 */
object VlcKoin {
    private var _koin: Koin? = null

    fun set(koin: Koin) {
        _koin = koin
    }

    fun getOrNull(): Koin? = _koin

    fun get(): Koin = _koin ?: error("VlcKoin not initialized — call startKoin first")

    val isStarted: Boolean get() = _koin != null
}
