package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.NoOpMediaSessionBridge
import org.videolan.vlc.platform.NoOpPipController
import org.videolan.vlc.platform.NoOpRendererBridge
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.repository.StubHistoryRepository
import org.videolan.vlc.repository.StubPlaylistRepository

/**
 * Browser media is user-owned: OPFS keeps imported files durable and an HTML media element
 * decodes them. Demo rows remain available only to explicit previews/tests, never the app graph.
 */
actual val platformModule: Module = module {
    single<DataStore<Preferences>> { BrowserPreferencesDataStore() }
    single { BrowserMediaRepository(includeDemoCatalog = false) }
    single<MediaRepository> { get<BrowserMediaRepository>() }
    single<PlaylistRepository> { StubPlaylistRepository() }
    single<HistoryRepository> { StubHistoryRepository() }
    single { BrowserPlaybackService() }
    single<PlaybackService> { get<BrowserPlaybackService>() }
    single<MediaSessionBridge> { NoOpMediaSessionBridge }
    single<PipController> { NoOpPipController }
    single<RendererBridge> { NoOpRendererBridge }
}
