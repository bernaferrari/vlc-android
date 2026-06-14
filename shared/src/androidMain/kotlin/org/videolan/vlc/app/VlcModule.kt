package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.AndroidVlcDataStoreFactory

/**
 * Android-specific Koin module.
 *
 * Provides the [DataStore<Preferences>] backed by Android's preferencesDataStore.
 * MediaRepository and PlaybackService are registered from the app module
 * (VlcKmpInitializer) because they depend on runtime objects (Medialibrary,
 * PlaylistManager) created during Application startup.
 */
actual val platformModule: Module = module {
    single<DataStore<Preferences>> {
        AndroidVlcDataStoreFactory(get()).create()
    }
}
