package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.JvmVlcDataStoreFactory
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.StubMediaRepository
import org.videolan.vlc.player.PlaybackService
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
    single<PlaybackService> { JvmPlaybackService() }
}
