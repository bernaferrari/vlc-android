package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.IosVlcDataStoreFactory
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.NoOpMediaSessionBridge
import org.videolan.vlc.platform.NoOpPipController
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.repository.PreferenceStreamRepository
import org.videolan.vlc.repository.StreamRepository

/**
 * iOS-specific Koin module.
 *
 * [IosMediaLibrary.shared] backs media + playlists + history while
 * [IosNetworkMediaRepository] adds MobileVLCKit LAN discovery to the same
 * shared browser contract.
 * Swift attaches VLCKit via [IosPlaybackService.shared.setBackend].
 */
actual val platformModule: Module = module {
    single<DataStore<Preferences>> {
        IosVlcDataStoreFactory().create()
    }
    single { IosMediaLibrary.shared }
    single { IosNetworkMediaRepository(get<IosMediaLibrary>()) }
    single<MediaRepository> { get<IosNetworkMediaRepository>() }
    single<PlaylistRepository> { get<IosMediaLibrary>() }
    single<HistoryRepository> { get<IosMediaLibrary>() }
    single<StreamRepository> { PreferenceStreamRepository(get()) }
    single<PlaybackService> { IosPlaybackService.shared }
    single<MediaSessionBridge> { IosMediaSessionBridge() }
    single<PipController> { NoOpPipController }
    single<RendererBridge> { IosRendererBridge() }
}
