package org.videolan.vlc.app

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Full in-process iOS media library: media catalog, folder tree, playlists, history.
 * Populated by Documents scan, Files/Photos import (Swift), and demo seed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMediaLibrary : MediaRepository, PlaylistRepository, HistoryRepository, IosMediaRepositoryMarker {

    private val items = MutableStateFlow<List<MediaItem>>(emptyList())
    private val folders = MutableStateFlow<List<MediaFolder>>(emptyList())
    private val folderMedia = MutableStateFlow<Map<Long, List<Long>>>(emptyMap()) // folderId -> media ids
    private val playlists = MutableStateFlow<Map<Long, Playlist>>(emptyMap())
    private val history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private var nextId = 10_000L
    private var nextFolderId = 1L
    private var nextPlaylistId = 1L

    init {
        runCatching { scanDocumentsFolder() }
        ensureRootFolder()
    }

    // --- MediaRepository ---

    override fun replaceAllPublic(media: List<MediaItem>) = replaceAll(media)

    fun snapshot(): List<MediaItem> = items.value

    fun replaceAll(media: List<MediaItem>) {
        items.value = media
        rebuildFolderIndex()
    }

    fun upsert(media: MediaItem) {
        val without = items.value.filterNot { it.uri == media.uri }
        items.value = without + media
        rebuildFolderIndex()
    }

    fun removeByUri(uri: String) {
        items.value = items.value.filterNot { it.uri == uri }
        rebuildFolderIndex()
    }

    fun clear() {
        items.value = emptyList()
        folderMedia.value = emptyMap()
        history.value = emptyList()
    }

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> =
        items.map { list -> list.filterByType(type) }

    override suspend fun getMedia(id: Long): MediaItem? =
        items.value.firstOrNull { it.id == id }

    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> {
        val set = ids.toSet()
        return items.value.filter { it.id in set }
    }

    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> =
        items.map { list ->
            list.filterByType(type).filter {
                it.title.contains(query, ignoreCase = true) ||
                    (it.artist?.contains(query, ignoreCase = true) == true) ||
                    (it.album?.contains(query, ignoreCase = true) == true)
            }
        }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> =
        history.map { entries -> entries.take(limit.coerceAtLeast(0)).map { it.item } }

    override suspend fun count(type: MediaType): Int =
        items.value.filterByType(type).size

    override suspend fun markAsPlayed(id: Long) {
        val item = getMedia(id) ?: return
        val updated = item.copy(lastPlayed = currentTimeMs(), playedCount = item.playedCount + 1)
        upsert(updated)
        val entry = HistoryEntry(updated, playedAt = updated.lastPlayed)
        history.value = listOf(entry) + history.value.filterNot { it.item.id == id || it.item.uri == item.uri }
    }

    override suspend fun incrementPlayCount(id: Long) = markAsPlayed(id)

    override fun observeFolders(parentId: Long?): Flow<List<MediaFolder>> =
        folders.map { all ->
            if (parentId == null) all.filter { it.isRoot }
            else {
                // Children: folders whose path is direct child of parent path
                val parent = all.firstOrNull { it.id == parentId } ?: return@map emptyList()
                all.filter { f ->
                    !f.isRoot && f.path != parent.path &&
                        f.path.startsWith(parent.path.trimEnd('/') + "/") &&
                        f.path.removePrefix(parent.path.trimEnd('/') + "/").count { it == '/' } == 0
                }
            }
        }

    override fun observeFolderMedia(folderId: Long): Flow<List<MediaItem>> =
        folderMedia.map { map ->
            val ids = map[folderId].orEmpty().toSet()
            items.value.filter { it.id in ids }
        }

    // --- PlaylistRepository ---

    override fun observePlaylists(): Flow<List<PlaylistInfo>> =
        playlists.map { map ->
            map.values.map {
                PlaylistInfo(
                    id = it.id,
                    name = it.name,
                    itemCount = it.items.size,
                    duration = it.items.sumOf { m -> m.duration },
                )
            }
        }

    override suspend fun getPlaylist(id: Long): Playlist? = playlists.value[id]

    override suspend fun createPlaylist(name: String): Playlist {
        val id = nextPlaylistId++
        val pl = Playlist(id, name)
        playlists.value = playlists.value + (id to pl)
        return pl
    }

    override suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>) {
        val current = playlists.value[playlistId] ?: return
        playlists.value = playlists.value + (playlistId to current.copy(items = current.items + items))
    }

    override suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>) {
        val current = playlists.value[playlistId] ?: return
        val set = itemIds.toSet()
        playlists.value = playlists.value + (playlistId to current.copy(items = current.items.filterNot { it.id in set }))
    }

    override suspend fun deletePlaylist(id: Long) {
        playlists.value = playlists.value - id
    }

    override suspend fun renamePlaylist(id: Long, name: String) {
        val current = playlists.value[id] ?: return
        playlists.value = playlists.value + (id to current.copy(name = name))
    }

    // --- HistoryRepository ---

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> =
        history.map { it.take(limit) }

    override suspend fun addToHistory(item: MediaItem) {
        markAsPlayed(item.id)
    }

    override suspend fun clearHistory() {
        history.value = emptyList()
    }

    override suspend fun removeHistoryEntry(id: Long) {
        history.value = history.value.filterNot { it.item.id == id }
    }

    // --- Scan ---

    private fun ensureRootFolder() {
        if (folders.value.none { it.isRoot }) {
            val docs = documentsPath() ?: "Documents"
            folders.value = listOf(
                MediaFolder(id = nextFolderId++, title = "On My iPhone", path = docs, uri = "file://$docs", isRoot = true)
            )
        }
    }

    private fun scanDocumentsFolder() {
        val docs = documentsPath() ?: return
        val fm = NSFileManager.defaultManager
        val names = fm.contentsOfDirectoryAtPath(docs, error = null) as? List<*> ?: return
        val found = mutableListOf<MediaItem>()
        for (nameAny in names) {
            val name = nameAny as? String ?: continue
            val lower = name.lowercase()
            val type = when {
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") ||
                    lower.endsWith(".avi") || lower.endsWith(".m4v") || lower.endsWith(".webm") -> MediaType.VIDEO
                lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".m4a") ||
                    lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".ogg") -> MediaType.AUDIO
                else -> continue
            }
            found += MediaItem(
                id = nextId++,
                title = name.substringBeforeLast('.'),
                uri = "file://$docs/$name",
                type = type,
            )
        }
        if (found.isNotEmpty()) replaceAll(found)
    }

    private fun rebuildFolderIndex() {
        ensureRootFolder()
        val root = folders.value.firstOrNull { it.isRoot } ?: return
        // Group media by parent directory
        val byDir = linkedMapOf<String, MutableList<MediaItem>>()
        for (item in items.value) {
            val path = item.uri.removePrefix("file://")
            val dir = path.substringBeforeLast('/', missingDelimiterValue = root.path)
            byDir.getOrPut(dir) { mutableListOf() }.add(item)
        }
        val folderList = mutableListOf(root)
        val mediaMap = mutableMapOf<Long, List<Long>>()
        // Root media (files directly in documents)
        mediaMap[root.id] = byDir[root.path]?.map { it.id }.orEmpty()
        for ((dir, media) in byDir) {
            if (dir == root.path) continue
            val title = dir.substringAfterLast('/')
            val existing = folders.value.firstOrNull { it.path == dir }
            val folder = existing ?: MediaFolder(
                id = nextFolderId++,
                title = title.ifBlank { dir },
                path = dir,
                uri = "file://$dir",
                childCount = media.size,
                isRoot = false,
            )
            if (existing == null) folderList += folder
            else folderList += existing.copy(childCount = media.size)
            mediaMap[folder.id] = media.map { it.id }
        }
        // Keep unique by path
        folders.value = folderList.distinctBy { it.path }
        folderMedia.value = mediaMap
    }

    private fun documentsPath(): String? {
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String
    }

    private fun List<MediaItem>.filterByType(type: MediaType): List<MediaItem> =
        when (type) {
            MediaType.ALL -> this
            else -> filter { it.type == type }
        }

    private fun currentTimeMs(): Long = 0L // set by platform on play; history uses markAsPlayed timestamps

    companion object {
        val shared: IosMediaLibrary by lazy { IosMediaLibrary() }
    }
}

/** Back-compat alias used by Swift / older call sites. */
typealias IosMediaRepository = IosMediaLibrary
