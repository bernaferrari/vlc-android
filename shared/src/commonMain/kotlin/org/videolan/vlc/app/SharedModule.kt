package org.videolan.vlc.app

import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.repository.StubHistoryRepository
import org.videolan.vlc.repository.StubPlaylistRepository

/**
 * Shared Koin module — registered on every platform.
 *
 * Declares bindings that are platform-neutral:
 *   - [VlcPreferences] wraps the platform-provided [DataStore]
 *   - Stub repositories as defaults (overridden by platform modules)
 *
 * Platform-specific modules (Android, iOS) are provided via [platformModule]
 * (expect/actual) and supply concrete implementations of DataStore,
 * MediaRepository, PlaybackService, etc.
 */
val sharedModule: Module = module {
    single<VlcPreferences> { VlcPreferences(get()) }

    // Default stub repos — platform modules override these
    single<PlaylistRepository> { StubPlaylistRepository() }
    single<HistoryRepository> { StubHistoryRepository() }
}
