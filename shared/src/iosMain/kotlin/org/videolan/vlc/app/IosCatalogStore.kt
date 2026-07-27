package org.videolan.vlc.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.RepeatMode
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/** Durable, repository-owned storage for the iOS media catalog. */
internal interface IosCatalogStore {
    fun read(): IosCatalogSnapshot?
    fun write(snapshot: IosCatalogSnapshot)
}

/**
 * Writes a versioned snapshot through a sibling temporary file then atomically
 * replaces the prior snapshot. A corrupt or unknown snapshot is ignored rather
 * than risking a partial catalog migration.
 */
internal class FileIosCatalogStore(
    private val path: Path = "${documentsDirectory()}/vlc.catalog.v1.json".toPath(),
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : IosCatalogStore {
    override fun read(): IosCatalogSnapshot? = runCatching {
        // Okio's Native Source/Sink do not implement AutoCloseable, so Kotlin's
        // JVM-oriented use {} extension is unavailable on this target.
        val source = fileSystem.source(path).buffer()
        try {
            catalogJson.decodeFromString<IosCatalogSnapshot>(source.readUtf8())
        } finally {
            source.close()
        }.takeIf { it.schemaVersion == IosCatalogSnapshot.SCHEMA_VERSION }
    }.getOrNull()

    override fun write(snapshot: IosCatalogSnapshot) {
        runCatching {
            val temporary = "${path}.tmp".toPath()
            val sink = fileSystem.sink(temporary).buffer()
            try {
                sink.writeUtf8(catalogJson.encodeToString(snapshot))
            } finally {
                sink.close()
            }
            fileSystem.atomicMove(temporary, path)
        }
    }

    private companion object {
        fun documentsDirectory(): String {
            val paths = NSFileManager.defaultManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask,
            )
            return (paths.firstOrNull() as? NSURL)?.path ?: platform.Foundation.NSTemporaryDirectory()
        }
    }
}

@Serializable
internal data class IosCatalogSnapshot(
    val schemaVersion: Int = SCHEMA_VERSION,
    val media: List<StoredMediaItem> = emptyList(),
    val playlists: List<StoredPlaylist> = emptyList(),
    val favoritePlaylistIds: List<Long> = emptyList(),
    val history: List<StoredHistoryEntry> = emptyList(),
    val playbackSession: StoredPlaybackSession? = null,
    val nextId: Long = 10_000L,
    val nextPlaylistId: Long = 1L,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
internal data class StoredMediaItem(
    val id: Long,
    val title: String,
    val uri: String,
    val type: String,
    val duration: Long = 0L,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val artworkUri: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val mime: String? = null,
    val lastModified: Long = 0L,
    val size: Long = 0L,
    val rating: Float = 0f,
    val playedCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isFavorite: Boolean = false,
    val seen: Long = 0L,
    val present: Boolean = true,
    val fileName: String? = null,
    val description: String? = null,
)

@Serializable
internal data class StoredPlaylist(
    val id: Long,
    val name: String,
    val items: List<StoredMediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: String = RepeatMode.NONE.name,
)

@Serializable
internal data class StoredHistoryEntry(
    val item: StoredMediaItem,
    val playedAt: Long,
)

@Serializable
internal data class StoredPlaybackSession(
    val playlist: StoredPlaylist,
    val positionMs: Long = 0L,
    val volume: Int = 100,
    val rate: Float = 1f,
)

internal data class IosPlaybackSession(
    val playlist: Playlist,
    val positionMs: Long,
    val volume: Int,
    val rate: Float,
)

internal fun MediaItem.toStored(): StoredMediaItem = StoredMediaItem(
    id = id,
    title = title,
    uri = uri,
    type = type.name,
    duration = duration,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    genre = genre,
    year = year,
    trackNumber = trackNumber,
    discNumber = discNumber,
    artworkUri = artworkUri,
    width = width,
    height = height,
    mime = mime,
    lastModified = lastModified,
    size = size,
    rating = rating,
    playedCount = playedCount,
    lastPlayed = lastPlayed,
    isFavorite = isFavorite,
    seen = seen,
    present = present,
    fileName = fileName,
    description = description,
)

internal fun StoredMediaItem.toMediaItem(): MediaItem = MediaItem(
    id = id,
    title = title,
    uri = uri,
    type = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.ALL),
    duration = duration,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    genre = genre,
    year = year,
    trackNumber = trackNumber,
    discNumber = discNumber,
    artworkUri = artworkUri,
    width = width,
    height = height,
    mime = mime,
    lastModified = lastModified,
    size = size,
    rating = rating,
    playedCount = playedCount,
    lastPlayed = lastPlayed,
    isFavorite = isFavorite,
    seen = seen,
    present = present,
    fileName = fileName,
    description = description,
)

internal fun Playlist.toStored(): StoredPlaylist = StoredPlaylist(
    id = id,
    name = name,
    items = items.map(MediaItem::toStored),
    currentIndex = currentIndex,
    shuffle = shuffle,
    repeatMode = repeatMode.name,
)

internal fun StoredPlaylist.toPlaylist(): Playlist = Playlist(
    id = id,
    name = name,
    items = items.map(StoredMediaItem::toMediaItem),
    currentIndex = currentIndex,
    shuffle = shuffle,
    repeatMode = runCatching { RepeatMode.valueOf(repeatMode) }.getOrDefault(RepeatMode.NONE),
)

internal fun HistoryEntry.toStored(): StoredHistoryEntry =
    StoredHistoryEntry(item = item.toStored(), playedAt = playedAt)

internal fun StoredHistoryEntry.toHistoryEntry(): HistoryEntry =
    HistoryEntry(item = item.toMediaItem(), playedAt = playedAt)

internal fun IosPlaybackSession.toStored(): StoredPlaybackSession = StoredPlaybackSession(
    playlist = playlist.toStored(),
    positionMs = positionMs,
    volume = volume,
    rate = rate,
)

internal fun StoredPlaybackSession.toPlaybackSession(): IosPlaybackSession = IosPlaybackSession(
    playlist = playlist.toPlaylist(),
    positionMs = positionMs.coerceAtLeast(0L),
    volume = volume.coerceIn(0, 200),
    rate = rate.takeIf(Float::isFinite)?.coerceIn(0.25f, 4f) ?: 1f,
)

private val catalogJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
