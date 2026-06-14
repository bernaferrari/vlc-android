package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.IosVlcDataStoreFactory
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.StubMediaRepository
import org.videolan.vlc.player.PlaybackService

/**
 * iOS-specific Koin module.
 *
 * Provides the DataStore backed by iOS file system and stub repositories.
 * When VLCKit integration lands, swap [StubMediaRepository] for a real
 * VLCKit-backed implementation and add a real [PlaybackService].
 */
actual val platformModule: Module = module {
    single<DataStore<Preferences>> {
        IosVlcDataStoreFactory().create()
    }
    single<MediaRepository> { StubMediaRepository() }
    single<PlaybackService> { IosPlaybackService() }
}
