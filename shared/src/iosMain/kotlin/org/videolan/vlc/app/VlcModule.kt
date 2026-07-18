package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.IosVlcDataStoreFactory
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.repository.MediaRepository

/**
 * iOS-specific Koin module.
 *
 * Uses [IosMediaRepository] / [IosPlaybackService] process singletons so Swift
 * can attach VLCKit backends and feed library items without re-resolving Koin.
 */
actual val platformModule: Module = module {
    single<DataStore<Preferences>> {
        IosVlcDataStoreFactory().create()
    }
    single<MediaRepository> { IosMediaRepository.shared }
    single<PlaybackService> { IosPlaybackService.shared }
}
