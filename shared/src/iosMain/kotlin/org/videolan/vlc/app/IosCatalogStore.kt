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
import org.videolan.vlc.player.PlaybackRate
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/** Durable, repository-owned storage for the iOS media catalog. */
internal interface IosCatalogStore {
    fun read(): IosCatalogSnapshot?
    fun write(snapshot: IosCatalogSnapshot)
}

internal class IosCatalogStorageException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

/**
 * Writes a versioned snapshot through a sibling temporary file then atomically
 * replaces the prior snapshot. The last known-good primary is retained as a
 * backup. Corruption and write failures are surfaced instead of being mistaken
 * for a first launch, which would otherwise overwrite recoverable user metadata
 * with an empty catalog.
 */
internal class FileIosCatalogStore(
    private val path: Path = defaultCatalogPath(),
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val legacyPath: Path? = legacyCatalogPath().takeIf { path == defaultCatalogPath() },
) : IosCatalogStore {
    private val temporaryPath = "$path.tmp".toPath()
    private val backupPath = "$path.backup".toPath()
    private val backupTemporaryPath = "$path.backup.tmp".toPath()
    private var recoveredFromBackup = false

    override fun read(): IosCatalogSnapshot? {
        try {
            migrateLegacyCatalog()
            val primaryExists = fileSystem.exists(path)
            val backupExists = fileSystem.exists(backupPath)
            if (!primaryExists && !backupExists) return null

            val primary = if (primaryExists) runCatching { readSnapshot(path) } else null
            primary?.getOrNull()?.let {
                recoveredFromBackup = false
                return it
            }

            val backup = if (backupExists) runCatching { readSnapshot(backupPath) } else null
            backup?.getOrNull()?.let {
                recoveredFromBackup = true
                return it
            }

            val cause = primary?.exceptionOrNull() ?: backup?.exceptionOrNull()
            throw IosCatalogStorageException(
                message = "The VLC iOS media catalog and its recovery copy could not be read",
                cause = cause,
            )
        } catch (failure: Throwable) {
            if (failure is IosCatalogStorageException) throw failure
            throw IosCatalogStorageException("The VLC iOS media catalog could not be opened", failure)
        }
    }

    override fun write(snapshot: IosCatalogSnapshot) {
        try {
            path.parent?.let { fileSystem.createDirectories(it) }
            writeText(temporaryPath, catalogJson.encodeToString(snapshot))
            if (fileSystem.exists(path) && !recoveredFromBackup) {
                fileSystem.copy(path, backupTemporaryPath)
                fileSystem.atomicMove(backupTemporaryPath, backupPath)
            }
            fileSystem.atomicMove(temporaryPath, path)
            recoveredFromBackup = false
        } catch (failure: Throwable) {
            runCatching { fileSystem.delete(temporaryPath, mustExist = false) }
            runCatching { fileSystem.delete(backupTemporaryPath, mustExist = false) }
            if (failure is IosCatalogStorageException) throw failure
            throw IosCatalogStorageException("The VLC iOS media catalog could not be saved", failure)
        }
    }

    private fun migrateLegacyCatalog() {
        val legacy = legacyPath ?: return
        if (fileSystem.exists(path) || !fileSystem.exists(legacy)) return
        path.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.copy(legacy, temporaryPath)
        fileSystem.atomicMove(temporaryPath, path)
        fileSystem.delete(legacy, mustExist = false)
    }

    private fun readSnapshot(sourcePath: Path): IosCatalogSnapshot {
        // Okio's Native Source/Sink do not implement AutoCloseable, so Kotlin's
        // JVM-oriented use {} extension is unavailable on this target.
        val source = fileSystem.source(sourcePath).buffer()
        val snapshot = try {
            catalogJson.decodeFromString<IosCatalogSnapshot>(source.readUtf8())
        } finally {
            source.close()
        }
        if (snapshot.schemaVersion !in 1..IosCatalogSnapshot.SCHEMA_VERSION) {
            throw IosCatalogStorageException(
                "Unsupported VLC iOS catalog schema ${snapshot.schemaVersion}",
            )
        }
        return snapshot
    }

    private fun writeText(target: Path, value: String) {
        val sink = fileSystem.sink(target).buffer()
        try {
            sink.writeUtf8(value)
        } finally {
            sink.close()
        }
    }

    private companion object {
        fun defaultCatalogPath(): Path =
            "${applicationSupportDirectory()}/VLC/vlc.catalog.v1.json".toPath()

        fun legacyCatalogPath(): Path =
            "${documentsDirectory()}/vlc.catalog.v1.json".toPath()

        fun applicationSupportDirectory(): String {
            val paths = NSFileManager.defaultManager.URLsForDirectory(
                NSApplicationSupportDirectory,
                NSUserDomainMask,
            )
            return (paths.firstOrNull() as? NSURL)?.path ?: platform.Foundation.NSTemporaryDirectory()
        }

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
    val bookmarks: List<StoredPlaybackBookmark> = emptyList(),
    val nextId: Long = 10_000L,
    val nextPlaylistId: Long = 1L,
) {
    companion object {
        const val SCHEMA_VERSION = 2
    }
}

@Serializable
internal data class StoredPlaybackBookmark(
    val mediaUri: String,
    val id: String,
    val timeMs: Long,
    val title: String,
)

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
    rate = PlaybackRate.normalize(rate),
)

private val catalogJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
