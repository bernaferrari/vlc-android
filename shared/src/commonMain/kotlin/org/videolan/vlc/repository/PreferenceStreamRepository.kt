package org.videolan.vlc.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType

/**
 * Portable named-stream storage for targets without a native medialibrary stream table.
 *
 * Android intentionally keeps its medialibrary-backed implementation. iOS and Web use this
 * repository so the exact same shared stream UI supports add, rename, delete and playback.
 */
class PreferenceStreamRepository(
    private val dataStore: DataStore<Preferences>,
) : StreamRepository {
    override fun observeStreams(): Flow<List<MediaItem>> =
        dataStore.data.map { preferences ->
            decodeStoredStreams(preferences[STREAMS_KEY]).sortedBy(MediaItem::id)
        }

    override suspend fun addStream(title: String, uri: String): MediaItem? {
        val cleanUri = uri.trim()
        if (cleanUri.isEmpty()) return null
        var added: MediaItem? = null
        dataStore.edit { preferences ->
            val current = decodeStoredStreams(preferences[STREAMS_KEY])
            val duplicate = current.firstOrNull { it.uri == cleanUri }
            if (duplicate != null) {
                added = duplicate
                return@edit
            }
            val id = (current.maxOfOrNull(MediaItem::id) ?: 0L) + 1L
            added = MediaItem(
                id = id,
                title = title.trim().ifBlank { cleanUri },
                uri = cleanUri,
                type = MediaType.STREAM,
            )
            preferences[STREAMS_KEY] = encodeStoredStreams(current + requireNotNull(added))
        }
        return added
    }

    override suspend fun renameStream(id: Long, title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        dataStore.edit { preferences ->
            val current = decodeStoredStreams(preferences[STREAMS_KEY])
            preferences[STREAMS_KEY] = encodeStoredStreams(
                current.map { if (it.id == id) it.copy(title = cleanTitle) else it }
            )
        }
    }

    override suspend fun deleteStream(id: Long) {
        dataStore.edit { preferences ->
            val current = decodeStoredStreams(preferences[STREAMS_KEY])
            preferences[STREAMS_KEY] = encodeStoredStreams(current.filterNot { it.id == id })
        }
    }

    private companion object {
        val STREAMS_KEY = stringSetPreferencesKey("named_network_streams")
    }
}

internal fun encodeStoredStreams(items: List<MediaItem>): Set<String> =
    items.mapTo(linkedSetOf()) { item ->
        "${item.id}|${item.title.length}|${item.title}${item.uri}"
    }

internal fun decodeStoredStreams(entries: Set<String>?): List<MediaItem> =
    entries.orEmpty().mapNotNull { entry ->
        val first = entry.indexOf('|')
        val second = entry.indexOf('|', first + 1)
        if (first <= 0 || second <= first) return@mapNotNull null
        val id = entry.substring(0, first).toLongOrNull() ?: return@mapNotNull null
        val titleLength = entry.substring(first + 1, second).toIntOrNull() ?: return@mapNotNull null
        val content = entry.substring(second + 1)
        if (titleLength < 0 || titleLength >= content.length) return@mapNotNull null
        MediaItem(
            id = id,
            title = content.take(titleLength),
            uri = content.drop(titleLength),
            type = MediaType.STREAM,
        )
    }
