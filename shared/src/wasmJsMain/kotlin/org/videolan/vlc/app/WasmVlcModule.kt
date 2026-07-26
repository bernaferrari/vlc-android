package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.NoOpMediaSessionBridge
import org.videolan.vlc.platform.NoOpPipController
import org.videolan.vlc.platform.NoOpRendererBridge
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.repository.FakeMediaRepository
import org.videolan.vlc.repository.FakePlaybackService
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.repository.StubHistoryRepository
import org.videolan.vlc.repository.StubPlaylistRepository

/** Browser-session preference storage. Persistence can later move to OPFS. */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val values = MutableStateFlow<Preferences>(emptyPreferences())
    private val updateMutex = Mutex()

    override val data: Flow<Preferences> = values

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        updateMutex.withLock {
            transform(values.value).also { values.value = it }
        }
}

/** Wasm uses deterministic media data and a stateful UI playback service. */
actual val platformModule: Module = module {
    single<DataStore<Preferences>> { InMemoryPreferencesDataStore() }
    single<MediaRepository> { FakeMediaRepository() }
    single<PlaylistRepository> { StubPlaylistRepository() }
    single<HistoryRepository> { StubHistoryRepository() }
    single<PlaybackService> { FakePlaybackService() }
    single<MediaSessionBridge> { NoOpMediaSessionBridge }
    single<PipController> { NoOpPipController }
    single<RendererBridge> { NoOpRendererBridge }
}
