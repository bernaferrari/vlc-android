package org.videolan.vlc.app

import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.player.PlaybackController

/**
 * Shared Koin module — registered on every platform.
 *
 * Platform modules supply MediaRepository, PlaybackService, PlaylistRepository,
 * HistoryRepository, and session/PiP/renderer bridges.
 */
val sharedModule: Module = module {
    single<VlcPreferences> { VlcPreferences(get()) }

    // Thin façade — prefer this over Android PlaylistManager god-objects.
    factory { PlaybackController(service = get()) }
}
