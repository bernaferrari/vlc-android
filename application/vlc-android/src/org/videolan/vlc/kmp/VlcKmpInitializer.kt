package org.videolan.vlc.kmp

import android.content.Context
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.tools.AndroidVlcDataStoreFactory
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.app.VlcApp
import org.videolan.vlc.app.VlcAppContainerBuilder
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.videolan.vlc.model.MediaItem

/**
 * Initializes the KMP VlcApp container on Android.
 *
 * Call [initialize] once during Application.onCreate() after the medialibrary
 * and PlaylistManager are available.
 *
 * This wires the shared KMP architecture into the existing Android VLC engine:
 *   - MediaRepository  → AndroidMediaRepository (JNI medialibrary)
 *   - PlaybackService  → AndroidPlaybackService (PlaylistManager + libVLC)
 *   - Preferences      → VlcPreferences (DataStore-backed)
 */
object VlcKmpInitializer {

    private var initialized = false

    fun initialize(
        context: Context,
        medialibrary: Medialibrary,
        playlistManager: PlaylistManager
    ) {
        if (initialized) return
        initialized = true

        val preferences = VlcPreferences(AndroidVlcDataStoreFactory(context).create())
        val mediaRepository = AndroidMediaRepository(medialibrary)
        val playbackService = AndroidPlaybackService(playlistManager)

        val container = VlcAppContainerBuilder()
            .preferences(preferences)
            .mediaRepository(mediaRepository)
            .playlistRepository(StubPlaylistRepository())
            .historyRepository(StubHistoryRepository())
            .playbackService(playbackService)
            .build()

        VlcApp.init(container)
    }
}

/**
 * Temporary stub playlist repository — will be wired to the medialibrary's
 * playlist API in a follow-up.
 */
private class StubPlaylistRepository : PlaylistRepository {
    override fun observePlaylists(): Flow<List<org.videolan.vlc.model.Playlist>> = flow { emit(emptyList()) }
    override suspend fun getPlaylist(id: Long) = null
    override suspend fun createPlaylist(name: String) = org.videolan.vlc.model.Playlist(0, name)
    override suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>) {}
    override suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>) {}
    override suspend fun deletePlaylist(id: Long) {}
    override suspend fun renamePlaylist(id: Long, name: String) {}
}

/**
 * Temporary stub history repository — will be wired to the medialibrary's
 * history API in a follow-up.
 */
private class StubHistoryRepository : HistoryRepository {
    override fun observeHistory(limit: Int): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override suspend fun addToHistory(item: MediaItem) {}
    override suspend fun clearHistory() {}
    override suspend fun removeHistoryEntry(id: Long) {}
}
