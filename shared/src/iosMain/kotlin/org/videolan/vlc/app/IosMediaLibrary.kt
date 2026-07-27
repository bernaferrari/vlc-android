package org.videolan.vlc.app

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
import platform.Foundation.NSURL
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.time.Clock

/**
 * The iOS implementation of the shared repositories.
 *
 * Its catalog is durable, while the folder tree is deliberately derived from the
 * current media URI set. That keeps persisted state platform-neutral and avoids
 * stale folder identifiers after files move on disk.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMediaLibrary private constructor(
    private val catalogStore: IosCatalogStore,
) : MediaRepository, PlaylistRepository, HistoryRepository, IosMediaRepositoryMarker {

    private val items = MutableStateFlow<List<MediaItem>>(emptyList())
    private val folders = MutableStateFlow<List<MediaFolder>>(emptyList())
    private val folderMedia = MutableStateFlow<Map<Long, List<Long>>>(emptyMap())
    private val playlists = MutableStateFlow<Map<Long, Playlist>>(emptyMap())
    private val favoritePlaylistIds = MutableStateFlow<Set<Long>>(emptySet())
    private val history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private var savedPlaybackSession: IosPlaybackSession? = null
    private var nextId = FIRST_MEDIA_ID
    private var nextFolderId = 1L
    private var nextPlaylistId = 1L

    init {
        restore()
        runCatching { scanDocumentsFolder() }
        ensureRootFolder()
    }

    // --- MediaRepository ---

    override fun replaceAllPublic(items: List<MediaItem>) = replaceAll(items)

    fun snapshot(): List<MediaItem> = items.value

    internal fun playbackSession(): IosPlaybackSession? = savedPlaybackSession

    internal fun savePlaybackSession(
        playlist: Playlist,
        positionMs: Long,
        volume: Int,
        rate: Float,
    ) {
        savedPlaybackSession = IosPlaybackSession(
            playlist = playlist,
            positionMs = positionMs.coerceAtLeast(0L),
            volume = volume.coerceIn(0, 200),
            rate = rate.takeIf(Float::isFinite)?.coerceIn(0.25f, 4f) ?: 1f,
        )
        persist()
    }

    internal fun clearPlaybackSession() {
        if (savedPlaybackSession == null) return
        savedPlaybackSession = null
        persist()
    }

    fun replaceAll(media: List<MediaItem>) {
        items.value = normalizeMedia(media)
        rebuildFolderIndex()
        persist()
    }

    /**
     * URI is the stable identity for a locally imported file. Swift assigns an
     * ephemeral id while scanning, so retaining the existing id prevents
     * duplicate library rows and broken playlist/history references after launch.
     */
    fun upsert(media: MediaItem) {
        val existing = items.value.firstOrNull { it.uri == media.uri }
        val assigned = when {
            existing != null -> media.copy(id = existing.id)
            items.value.any { it.id == media.id } -> media.copy(id = reserveMediaId())
            else -> media
        }
        items.value = items.value.filterNot { it.uri == assigned.uri } + assigned
        nextId = maxOf(nextId, assigned.id + 1L)
        refreshPlaylistMetadata()
        rebuildFolderIndex()
        persist()
    }

    /**
     * Applies filesystem/decoder-derived fields after a native scan without overwriting metadata
     * VLC owns (favorites, history, rating, artwork, and descriptions). Swift uses this when
     * AVFoundation finishes inspecting a local file after it is already visible in the catalog.
     */
    fun mergeScannedMetadata(media: MediaItem) {
        val existing = items.value.firstOrNull { it.uri == media.uri }
        if (existing == null) {
            upsert(media)
            return
        }
        val merged = media.copy(
            id = existing.id,
            artworkUri = existing.artworkUri ?: media.artworkUri,
            description = existing.description ?: media.description,
            rating = existing.rating,
            playedCount = existing.playedCount,
            lastPlayed = existing.lastPlayed,
            isFavorite = existing.isFavorite,
            seen = existing.seen,
        )
        items.value = items.value.map { if (it.uri == merged.uri) merged else it }
        refreshPlaylistMetadata()
        rebuildFolderIndex()
        persist()
    }

    fun removeByUri(uri: String) {
        if (items.value.none { it.uri == uri }) return
        // Keep playlist/history entries: users expect an unavailable local track
        // to retain its position and to return if the file is restored later.
        items.value = items.value.filterNot { it.uri == uri }
        rebuildFolderIndex()
        persist()
    }

    fun clear() {
        items.value = emptyList()
        playlists.value = emptyMap()
        favoritePlaylistIds.value = emptySet()
        history.value = emptyList()
        savedPlaybackSession = null
        rebuildFolderIndex()
        persist()
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
        items.value = items.value.map { if (it.id == id) updated else it }
        refreshPlaylistMetadata()
        history.value = listOf(HistoryEntry(updated, playedAt = updated.lastPlayed)) +
            history.value.filterNot { it.item.id == id || it.item.uri == item.uri }
        rebuildFolderIndex()
        persist()
    }

    override suspend fun incrementPlayCount(id: Long) = markAsPlayed(id)

    override suspend fun markAsUnplayed(id: Long) {
        val item = getMedia(id) ?: return
        upsert(item.copy(playedCount = 0, seen = 0L, lastPlayed = 0L))
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        val item = getMedia(id)
        if (item != null) {
            upsert(item.copy(isFavorite = favorite))
            return
        }
        if (playlists.value.containsKey(id)) {
            favoritePlaylistIds.value = favoritePlaylistIds.value.let { ids ->
                if (favorite) ids + id else ids - id
            }
            persist()
        }
    }

    override fun observeFolders(parentId: Long?): Flow<List<MediaFolder>> =
        folders.map { all ->
            if (parentId == null) all.filter { it.isRoot }
            else {
                val parent = all.firstOrNull { it.id == parentId } ?: return@map emptyList()
                all.filter { folder ->
                    !folder.isRoot && folder.path != parent.path &&
                        folder.path.startsWith(parent.path.trimEnd('/') + "/") &&
                        folder.path.removePrefix(parent.path.trimEnd('/') + "/").count { it == '/' } == 0
                }
            }
        }

    override fun observeFolderMedia(folderId: Long): Flow<List<MediaItem>> =
        combine(folderMedia, items) { mediaByFolder, currentItems ->
            val ids = mediaByFolder[folderId].orEmpty().toSet()
            currentItems.filter { it.id in ids }
        }

    // --- PlaylistRepository ---

    override fun observePlaylists(): Flow<List<PlaylistInfo>> =
        combine(playlists, favoritePlaylistIds) { map, favorites ->
            map.values.map {
                PlaylistInfo(
                    id = it.id,
                    name = it.name,
                    itemCount = it.items.size,
                    duration = it.items.sumOf { media -> media.duration },
                    isFavorite = it.id in favorites,
                )
            }
        }

    override suspend fun getPlaylist(id: Long): Playlist? = playlists.value[id]

    override suspend fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(nextPlaylistId++, name)
        playlists.value = playlists.value + (playlist.id to playlist)
        persist()
        return playlist
    }

    override suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>) {
        val current = playlists.value[playlistId] ?: return
        playlists.value = playlists.value + (playlistId to current.copy(items = current.items + items))
        persist()
    }

    override suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>) {
        val current = playlists.value[playlistId] ?: return
        val ids = itemIds.toSet()
        playlists.value = playlists.value + (playlistId to current.copy(items = current.items.filterNot { it.id in ids }))
        persist()
    }

    override suspend fun removeFromPlaylistAt(playlistId: Long, index: Int) {
        val current = playlists.value[playlistId] ?: return
        if (index !in current.items.indices) return
        playlists.value = playlists.value + (playlistId to current.copy(items = current.items.filterIndexed { i, _ -> i != index }))
        persist()
    }

    override suspend fun moveInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val current = playlists.value[playlistId] ?: return
        val reordered = current.items.toMutableList()
        if (fromIndex !in reordered.indices || toIndex !in reordered.indices || fromIndex == toIndex) return
        reordered.add(toIndex, reordered.removeAt(fromIndex))
        playlists.value = playlists.value + (playlistId to current.copy(items = reordered))
        persist()
    }

    override suspend fun deletePlaylist(id: Long) {
        if (!playlists.value.containsKey(id)) return
        playlists.value = playlists.value - id
        favoritePlaylistIds.value = favoritePlaylistIds.value - id
        persist()
    }

    override suspend fun renamePlaylist(id: Long, name: String) {
        val current = playlists.value[id] ?: return
        playlists.value = playlists.value + (id to current.copy(name = name))
        persist()
    }

    // --- HistoryRepository ---

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> =
        history.map { it.take(limit.coerceAtLeast(0)) }

    override suspend fun addToHistory(item: MediaItem) {
        markAsPlayed(item.id)
    }

    override suspend fun clearHistory() {
        history.value = emptyList()
        persist()
    }

    override suspend fun removeHistoryEntry(id: Long) {
        val filtered = history.value.filterNot { it.item.id == id }
        if (filtered == history.value) return
        history.value = filtered
        persist()
    }

    // --- Local file reconciliation ---

    /** Called by the Swift Documents scan; it never removes remote/stream items. */
    fun reconcileLocalDocuments(media: List<MediaItem>) {
        documentsPath()?.let { reconcileLocalFiles(media, listOf(it)) } ?: media.forEach(::upsert)
    }

    /**
     * Reconciles a complete scan under [roots]. Metadata owned by VLC is retained
     * when the scanner supplies the same URI, while playlist and history snapshots
     * are intentionally not pruned when a file temporarily disappears.
     */
    internal fun reconcileLocalFiles(media: List<MediaItem>, roots: List<String>) {
        val normalizedRoots = roots.map(::normalizedPath).filter(String::isNotBlank)
        if (normalizedRoots.isEmpty()) {
            media.forEach(::upsert)
            return
        }
        val scannedByUri = media.associateBy { it.uri }
        val retained = items.value.filterNot { item ->
            item.uri.isUnderAny(normalizedRoots) && item.uri !in scannedByUri
        }
        val retainedByUri = retained.associateBy { it.uri }
        val occupiedIds = retained.mapTo(mutableSetOf()) { it.id }
        var candidateId = nextId
        fun reserveId(): Long {
            while (candidateId in occupiedIds) candidateId++
            return candidateId++.also(occupiedIds::add)
        }
        val reconciled = scannedByUri.values.map { fresh ->
            val existing = retainedByUri[fresh.uri]
            val assignedId = when {
                existing != null -> existing.id
                fresh.id >= FIRST_MEDIA_ID && fresh.id !in occupiedIds -> fresh.id.also(occupiedIds::add)
                else -> reserveId()
            }
            fresh.copy(
                id = assignedId,
                artworkUri = existing?.artworkUri ?: fresh.artworkUri,
                description = existing?.description ?: fresh.description,
                rating = existing?.rating ?: fresh.rating,
                playedCount = existing?.playedCount ?: fresh.playedCount,
                lastPlayed = existing?.lastPlayed ?: fresh.lastPlayed,
                isFavorite = existing?.isFavorite ?: fresh.isFavorite,
                seen = existing?.seen ?: fresh.seen,
            )
        }
        items.value = retained.filterNot { it.uri in scannedByUri } + reconciled
        nextId = maxOf(candidateId, (items.value.maxOfOrNull { it.id } ?: FIRST_MEDIA_ID - 1L) + 1L)
        refreshPlaylistMetadata()
        rebuildFolderIndex()
        persist()
    }

    // --- Scan and persistence ---

    private fun restore() {
        val snapshot = catalogStore.read() ?: return
        items.value = normalizeMedia(snapshot.media.map(StoredMediaItem::toMediaItem))
        playlists.value = snapshot.playlists
            .map(StoredPlaylist::toPlaylist)
            .associateBy(Playlist::id)
        favoritePlaylistIds.value = snapshot.favoritePlaylistIds.toSet().intersect(playlists.value.keys)
        history.value = snapshot.history.map(StoredHistoryEntry::toHistoryEntry)
        savedPlaybackSession = snapshot.playbackSession?.toPlaybackSession()
        nextId = maxOf(snapshot.nextId, (items.value.maxOfOrNull { it.id } ?: FIRST_MEDIA_ID - 1L) + 1L)
        nextPlaylistId = maxOf(snapshot.nextPlaylistId, (playlists.value.keys.maxOrNull() ?: 0L) + 1L)
        refreshPlaylistMetadata()
        rebuildFolderIndex()
    }

    private fun persist() {
        catalogStore.write(
            IosCatalogSnapshot(
                media = items.value.map(MediaItem::toStored),
                playlists = playlists.value.values.map(Playlist::toStored),
                favoritePlaylistIds = favoritePlaylistIds.value.sorted(),
                history = history.value.map(HistoryEntry::toStored),
                playbackSession = savedPlaybackSession?.toStored(),
                nextId = nextId,
                nextPlaylistId = nextPlaylistId,
            )
        )
    }

    private fun scanDocumentsFolder() {
        val documents = documentsPath() ?: return
        // Match the Swift rescan: a Documents catalog can contain user-created
        // subfolders, so a top-level-only scan must not treat nested files as
        // deleted during cold-start reconciliation.
        val names = NSFileManager.defaultManager.subpathsOfDirectoryAtPath(documents, error = null) ?: return
        val found = names.mapNotNull { name ->
            val fileName = name as? String ?: return@mapNotNull null
            val type = fileName.mediaTypeOrNull() ?: return@mapNotNull null
            // Match Swift URL.absoluteString from the document picker. Constructing file:// URLs
            // manually leaves spaces and non-ASCII characters unescaped, which would make a cold
            // scan look like a different file and discard its saved favorite/history metadata.
            val uri = canonicalIosFileUri("$documents/$fileName") ?: return@mapNotNull null
            MediaItem(
                id = 0L,
                title = fileName.substringBeforeLast('.'),
                uri = uri,
                type = type,
            )
        }
        reconcileLocalFiles(found, listOf(documents))
    }

    private fun normalizeMedia(media: List<MediaItem>): List<MediaItem> {
        val occupied = mutableSetOf<Long>()
        return media.distinctBy(MediaItem::uri).map { item ->
            val id = if (item.id in occupied) reserveMediaId() else item.id
            occupied += id
            item.copy(id = id)
        }.also { normalized ->
            nextId = maxOf(nextId, (normalized.maxOfOrNull { it.id } ?: FIRST_MEDIA_ID - 1L) + 1L)
        }
    }

    private fun refreshPlaylistMetadata() {
        val canonicalByUri = items.value.associateBy(MediaItem::uri)
        playlists.value = playlists.value.mapValues { (_, playlist) ->
            playlist.copy(items = playlist.items.map { saved -> canonicalByUri[saved.uri] ?: saved })
        }
    }

    private fun ensureRootFolder() {
        if (folders.value.none { it.isRoot }) {
            val documents = documentsPath() ?: "Documents"
            folders.value = listOf(
                MediaFolder(
                    id = nextFolderId++,
                    title = "On My iPhone",
                    path = documents,
                    uri = "file://$documents",
                    isRoot = true,
                )
            )
        }
    }

    private fun rebuildFolderIndex() {
        ensureRootFolder()
        val root = folders.value.firstOrNull { it.isRoot } ?: return
        val byDirectory = linkedMapOf<String, MutableList<MediaItem>>()
        items.value.forEach { item ->
            val path = normalizedPath(item.uri)
            val directory = path.substringBeforeLast('/', missingDelimiterValue = root.path)
            byDirectory.getOrPut(directory) { mutableListOf() } += item
        }
        val folderList = mutableListOf(root)
        val mediaMap = mutableMapOf<Long, List<Long>>()
        mediaMap[root.id] = byDirectory[root.path]?.map(MediaItem::id).orEmpty()
        byDirectory.forEach { (directory, media) ->
            if (directory == root.path) return@forEach
            val existing = folders.value.firstOrNull { it.path == directory }
            val folder = existing ?: MediaFolder(
                id = nextFolderId++,
                title = directory.substringAfterLast('/').ifBlank { directory },
                path = directory,
                uri = "file://$directory",
            )
            folderList += folder.copy(childCount = media.size)
            mediaMap[folder.id] = media.map(MediaItem::id)
        }
        folders.value = folderList.distinctBy(MediaFolder::path)
        folderMedia.value = mediaMap
    }

    private fun reserveMediaId(): Long = nextId++

    private fun documentsPath(): String? = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String

    private fun List<MediaItem>.filterByType(type: MediaType): List<MediaItem> = when (type) {
        MediaType.ALL -> this
        else -> filter { it.type == type }
    }

    private fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()

    companion object {
        private const val FIRST_MEDIA_ID = 10_000L

        val shared: IosMediaLibrary by lazy { IosMediaLibrary(FileIosCatalogStore()) }

        internal fun forTesting(store: IosCatalogStore): IosMediaLibrary = IosMediaLibrary(store)
    }
}

private fun String.mediaTypeOrNull(): MediaType? = when (substringAfterLast('.', "").lowercase()) {
    "mp4", "mkv", "mov", "avi", "m4v", "webm" -> MediaType.VIDEO
    "mp3", "flac", "m4a", "aac", "wav", "ogg" -> MediaType.AUDIO
    else -> null
}

/** Shared identity form for both UIKit's picker and the Kotlin Documents rescan. */
internal fun canonicalIosFileUri(path: String): String? = NSURL.fileURLWithPath(path).absoluteString

private fun normalizedPath(value: String): String = value.removePrefix("file://").trimEnd('/')

private fun String.isUnderAny(roots: List<String>): Boolean {
    val path = normalizedPath(this)
    return roots.any { root -> path == root || path.startsWith("$root/") }
}
