package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.JvmVlcDataStoreFactory
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.repository.StubHistoryRepository
import org.videolan.vlc.repository.StubMediaRepository
import org.videolan.vlc.repository.StubPlaylistRepository
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.NoOpMediaSessionBridge
import org.videolan.vlc.platform.NoOpPipController
import org.videolan.vlc.platform.NoOpRendererBridge
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.RendererBridge
import java.io.File

/**
 * JVM-specific Koin module (for desktop / testing).
 *
 * Provides DataStore via Okio file system and stub repositories.
 */
actual val platformModule: Module = module {
    single<DataStore<Preferences>> {
        JvmVlcDataStoreFactory(File(System.getProperty("user.home"), ".vlc-shared").apply { mkdirs() }).create()
    }
    single<MediaRepository> { StubMediaRepository() }
    single<PlaylistRepository> { StubPlaylistRepository() }
    single<HistoryRepository> { StubHistoryRepository() }
    single<PlaybackService> { JvmPlaybackService() }
    single<MediaSessionBridge> { NoOpMediaSessionBridge }
    single<PipController> { NoOpPipController }
    single<RendererBridge> { NoOpRendererBridge }
}
