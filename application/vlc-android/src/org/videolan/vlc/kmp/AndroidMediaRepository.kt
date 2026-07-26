package org.videolan.vlc.kmp

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.MediaBrowser
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.Artist
import org.videolan.medialibrary.interfaces.media.Folder
import org.videolan.medialibrary.interfaces.media.Genre
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.AndroidDevices
import org.videolan.resources.VLCInstance
import org.videolan.tools.KEY_ARTISTS_SHOW_ALL
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.tools.NetworkMonitor
import org.videolan.tools.Settings
import org.videolan.vlc.ExternalMonitor
import org.videolan.vlc.R
import org.videolan.vlc.VlcMigrationHelper
import org.videolan.vlc.gui.helpers.hf.OtgAccess
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.repository.AudioEntity
import org.videolan.vlc.repository.AudioEntityKind
import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.repository.BrowserListing
import org.videolan.vlc.repository.ContainerKind
import org.videolan.vlc.repository.ListPagingSource
import org.videolan.vlc.repository.MEDIA_PAGE_SIZE
import org.videolan.vlc.repository.MediaQuery
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.convertFavorites
import java.io.File

/**
 * Android implementation of [MediaRepository] backed by the VLC medialibrary JNI.
 *
 * Bridges the shared KMP domain interface to the existing Android medialibrary.
 * Created lazily once the medialibrary is initialized.
 */
class AndroidMediaRepository(
    private val medialibrary: Medialibrary,
    context: Context,
) : MediaRepository {

    private val appContext = context.applicationContext
    private val browserFavRepository = BrowserFavRepository.getInstance(appContext)
    private val networkMonitor = NetworkMonitor.getInstance(appContext)
    private val settings = Settings.getInstance(appContext)

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> = mediaCallbackFlow {
        queryMedia(type)
    }

    override fun observeMediaPaged(query: MediaQuery): Flow<PagingData<MediaItem>> {
        // Native paged path for plain video/audio lists and video-group children.
        val useNative = when {
            query.containerKind == ContainerKind.FOLDER -> false
            query.containerKind == ContainerKind.VIDEO_GROUP && query.containerId != null -> true
            query.containerKind == ContainerKind.NONE &&
                (query.type == MediaType.VIDEO || query.type == MediaType.AUDIO) -> true
            else -> false
        }
        if (!useNative) {
            return Pager(
                config = pagingConfig(),
                pagingSourceFactory = {
                    ListPagingSource(registerInvalidation = ::registerMediaInvalidation) {
                        withContext(Dispatchers.IO) { loadMediaSnapshot(query) }
                    }
                },
            ).flow
        }

        return Pager(
            config = pagingConfig(),
            pagingSourceFactory = {
                OffsetMediaPagingSource(
                    loadPage = { loadSize, offset ->
                        loadPagedMedia(query, loadSize, offset)
                    },
                    totalCount = { countPagedMedia(query) },
                    registerInvalidation = ::registerMediaInvalidation,
                )
            },
        ).flow
    }

    override suspend fun getMedia(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        medialibrary.getMedia(id)?.toMediaItem()
    }

    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
        ids.mapNotNull { id -> medialibrary.getMedia(id)?.toMediaItem() }
    }

    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> = mediaCallbackFlow {
        if (query.isBlank()) emptyList() else querySearch(query, type)
    }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> = mediaCallbackFlow {
        val history = medialibrary.history(Medialibrary.HISTORY_TYPE_LOCAL) ?: emptyArray()
        history.asList()
            .take(limit.coerceAtLeast(0))
            .map { it.toMediaItem() }
    }

    override suspend fun count(type: MediaType): Int = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext 0
        when (type) {
            MediaType.AUDIO -> medialibrary.audioCount
            MediaType.VIDEO -> medialibrary.videoCount
            MediaType.ALL -> medialibrary.audioCount + medialibrary.videoCount
            else -> 0
        }
    }

    override suspend fun markAsPlayed(id: Long) {
        withContext(Dispatchers.IO) {
            medialibrary.getMedia(id)?.markAsPlayed()
        }
    }

    override suspend fun markAsUnplayed(id: Long) {
        withContext(Dispatchers.IO) {
            medialibrary.getMedia(id)?.setPlayCount(0L)
        }
    }

    override suspend fun incrementPlayCount(id: Long) {
        withContext(Dispatchers.IO) {
            val media = medialibrary.getMedia(id) ?: return@withContext
            val next = media.playCount + 1L
            media.playCount = next
            if (media.seen == 0L) media.seen = 1L
            // markAsPlayed also updates ML history / last-played bookkeeping
            media.markAsPlayed()
        }
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        withContext(Dispatchers.IO) {
            medialibrary.getMedia(id)?.setFavorite(favorite)
        }
    }

    private fun queryMedia(
        type: MediaType,
        sort: MediaSort = MediaSort.DEFAULT,
        desc: Boolean = false,
        onlyFavorites: Boolean = false,
    ): List<MediaItem> {
        if (!medialibrary.isInitiated) return emptyList()
        val includeMissing = Settings.includeMissing
        val mlSort = sort.toMlSort()
        return when (type) {
            MediaType.AUDIO -> medialibrary.getAudio(mlSort, desc, includeMissing, onlyFavorites)
            MediaType.VIDEO -> medialibrary.getVideos(mlSort, desc, includeMissing, onlyFavorites)
            MediaType.ALL -> {
                val audio = medialibrary.getAudio(mlSort, desc, includeMissing, onlyFavorites)
                val video = medialibrary.getVideos(mlSort, desc, includeMissing, onlyFavorites)
                arrayOf(*audio, *video)
            }
            else -> emptyArray()
        }.map { it.toMediaItem() }
    }

    private fun querySearch(query: String, type: MediaType): List<MediaItem> {
        if (!medialibrary.isInitiated) return emptyList()
        val includeMissing = Settings.includeMissing
        val results: Array<MediaWrapper> = when (type) {
            MediaType.AUDIO -> medialibrary.searchAudio(
                query, Medialibrary.SORT_DEFAULT, false, includeMissing, false, Int.MAX_VALUE, 0
            ) ?: emptyArray()
            MediaType.VIDEO -> medialibrary.searchVideo(
                query, Medialibrary.SORT_DEFAULT, false, includeMissing, false, Int.MAX_VALUE, 0
            ) ?: emptyArray()
            else -> medialibrary.searchMedia(query) ?: emptyArray()
        }
        return results.map { it.toMediaItem() }
    }

    private fun loadPagedMedia(query: MediaQuery, loadSize: Int, offset: Int): List<MediaItem> {
        if (!medialibrary.isInitiated) return emptyList()
        val includeMissing = Settings.includeMissing
        val sort = query.sort.toMlSort()
        val desc = query.desc
        val onlyFavorites = query.onlyFavorites
        val q = query.query.trim()

        if (query.containerKind == ContainerKind.VIDEO_GROUP) {
            val groupId = query.containerId ?: return emptyList()
            val group = medialibrary.getVideoGroup(groupId) ?: return emptyList()
            val page = if (q.isEmpty()) {
                group.media(sort, desc, includeMissing, onlyFavorites, loadSize, offset)
            } else {
                group.searchTracks(q, sort, desc, includeMissing, onlyFavorites, loadSize, offset)
            }
            return page.map { it.toMediaItem() }
        }

        return when (query.type) {
            MediaType.VIDEO -> {
                val page = if (q.isEmpty()) {
                    medialibrary.getPagedVideos(sort, desc, includeMissing, onlyFavorites, loadSize, offset)
                } else {
                    medialibrary.searchVideo(q, sort, desc, includeMissing, onlyFavorites, loadSize, offset)
                        ?: emptyArray()
                }
                page.map { it.toMediaItem() }
            }
            MediaType.AUDIO -> {
                val page = if (q.isEmpty()) {
                    medialibrary.getPagedAudio(sort, desc, includeMissing, onlyFavorites, loadSize, offset)
                } else {
                    medialibrary.searchAudio(q, sort, desc, includeMissing, onlyFavorites, loadSize, offset)
                        ?: emptyArray()
                }
                page.map { it.toMediaItem() }
            }
            else -> emptyList()
        }
    }

    private fun countPagedMedia(query: MediaQuery): Int {
        if (!medialibrary.isInitiated) return 0
        val q = query.query.trim()
        if (query.containerKind == ContainerKind.VIDEO_GROUP) {
            val groupId = query.containerId ?: return 0
            val group = medialibrary.getVideoGroup(groupId) ?: return 0
            return if (q.isEmpty()) group.mediaCount() else group.searchTracksCount(q)
        }
        if (query.containerKind == ContainerKind.FOLDER) {
            val folderId = query.containerId ?: return 0
            val mlFolder = runCatching {
                medialibrary.getFolder(Folder.TYPE_FOLDER_VIDEO, folderId)
            }.getOrNull()
            if (mlFolder != null) {
                return if (q.isEmpty()) {
                    mlFolder.mediaCount(Folder.TYPE_FOLDER_VIDEO)
                } else {
                    mlFolder.searchTracksCount(q, Folder.TYPE_FOLDER_VIDEO)
                }
            }
            // Storage-root path filter is snapshot-only; paging falls back to full list size.
            return loadMediaSnapshot(query).size
        }
        return when (query.type) {
            MediaType.VIDEO -> if (q.isEmpty()) medialibrary.videoCount else medialibrary.getVideoCount(q)
            MediaType.AUDIO -> if (q.isEmpty()) medialibrary.audioCount else medialibrary.getAudioCount(q)
            else -> 0
        }
    }

    private fun loadMediaSnapshot(query: MediaQuery): List<MediaItem> {
        val q = query.query.trim()
        var list = when (query.containerKind) {
            ContainerKind.FOLDER -> {
                val folderId = query.containerId ?: return emptyList()
                // Media-library video folder (grouping by folder).
                if (medialibrary.isInitiated) {
                    val mlFolder = runCatching {
                        medialibrary.getFolder(Folder.TYPE_FOLDER_VIDEO, folderId)
                    }.getOrNull()
                    if (mlFolder != null) {
                        val count = mlFolder.mediaCount(Folder.TYPE_FOLDER_VIDEO).coerceAtLeast(0)
                        when {
                            count == 0 -> emptyList()
                            q.isEmpty() -> mlFolder.media(
                                Folder.TYPE_FOLDER_VIDEO,
                                query.sort.toMlSort(),
                                query.desc,
                                Settings.includeMissing,
                                query.onlyFavorites,
                                count,
                                0,
                            ).map { it.toMediaItem() }
                            else -> mlFolder.searchTracks(
                                q,
                                Folder.TYPE_FOLDER_VIDEO,
                                query.sort.toMlSort(),
                                query.desc,
                                Settings.includeMissing,
                                query.onlyFavorites,
                                count,
                                0,
                            ).map { it.toMediaItem() }
                        }
                    } else {
                        // Storage root path filter fallback.
                        val root = storageRootPath(folderId) ?: return emptyList()
                        queryMedia(query.type, query.sort, query.desc, query.onlyFavorites)
                            .filter { it.uri.contains(root) }
                    }
                } else {
                    val root = storageRootPath(folderId) ?: return emptyList()
                    queryMedia(query.type, query.sort, query.desc, query.onlyFavorites)
                        .filter { it.uri.contains(root) }
                }
            }
            ContainerKind.VIDEO_GROUP -> {
                val groupId = query.containerId ?: return emptyList()
                val group = medialibrary.getVideoGroup(groupId) ?: return emptyList()
                val count = group.mediaCount().coerceAtLeast(0)
                if (count == 0) emptyList()
                else group.media(
                    query.sort.toMlSort(),
                    query.desc,
                    Settings.includeMissing,
                    query.onlyFavorites,
                    count,
                    0,
                ).map { it.toMediaItem() }
            }
            ContainerKind.NONE -> {
                if (q.isEmpty()) {
                    queryMedia(query.type, query.sort, query.desc, query.onlyFavorites)
                } else {
                    var results = querySearch(q, query.type)
                    if (query.onlyFavorites) results = results.filter { it.isFavorite }
                    results
                }
            }
        }
        if (query.containerKind != ContainerKind.NONE || q.isNotEmpty()) {
            // Native getAudio/getVideos already honor sort/onlyFavorites; search path may need filter.
            if (query.onlyFavorites && query.containerKind == ContainerKind.FOLDER) {
                list = list.filter { it.isFavorite }
            }
        }
        return list
    }

    /**
     * Emits [query] immediately and again whenever the medialibrary reports media changes.
     */
    private fun <T> libraryCallbackFlow(query: () -> List<T>): Flow<List<T>> = callbackFlow {
        fun emitLatest() {
            try {
                trySend(query())
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        val mediaCb = object : Medialibrary.MediaCb {
            override fun onMediaAdded() = emitLatest()
            override fun onMediaModified() = emitLatest()
            override fun onMediaDeleted(id: LongArray?) = emitLatest()
            override fun onMediaConvertedToExternal(id: LongArray?) = emitLatest()
        }
        val historyCb = object : Medialibrary.HistoryCb {
            override fun onHistoryModified() = emitLatest()
        }
        medialibrary.addMediaCb(mediaCb)
        medialibrary.addHistoryCb(historyCb)
        emitLatest()

        awaitClose {
            medialibrary.removeMediaCb(mediaCb)
            medialibrary.removeHistoryCb(historyCb)
        }
    }.flowOn(Dispatchers.IO)

    private fun mediaCallbackFlow(query: () -> List<MediaItem>): Flow<List<MediaItem>> =
        libraryCallbackFlow(query)

    /**
     * Native paging queries are point-in-time snapshots.  Tie each PagingSource to
     * medialibrary changes so favorite, scan and deletion operations refresh the
     * visible Compose list instead of leaving it stale until the user changes a filter.
     */
    private fun registerMediaInvalidation(invalidate: () -> Unit): () -> Unit {
        val callback = object : Medialibrary.MediaCb {
            override fun onMediaAdded() = invalidate()
            override fun onMediaModified() = invalidate()
            override fun onMediaDeleted(id: LongArray?) = invalidate()
            override fun onMediaConvertedToExternal(id: LongArray?) = invalidate()
        }
        medialibrary.addMediaCb(callback)
        return { medialibrary.removeMediaCb(callback) }
    }

    override fun observeFolders(parentId: Long?): Flow<List<MediaFolder>> {
        if (parentId != null) {
            // Children of storage roots are browsed via browseUri(file://...).
            return flowOf(emptyList())
        }
        // Rebuild roots whenever OTG document-tree grant changes.
        return OtgAccess.otgRoot
            .map { buildStorageRoots() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    override fun observeFolderMedia(folderId: Long): Flow<List<MediaItem>> = mediaCallbackFlow {
        if (medialibrary.isInitiated) {
            val mlFolder = runCatching {
                medialibrary.getFolder(Folder.TYPE_FOLDER_VIDEO, folderId)
            }.getOrNull()
            if (mlFolder != null) {
                val count = mlFolder.mediaCount(Folder.TYPE_FOLDER_VIDEO).coerceAtLeast(0)
                return@mediaCallbackFlow if (count == 0) {
                    emptyList()
                } else {
                    mlFolder.media(
                        Folder.TYPE_FOLDER_VIDEO,
                        Medialibrary.SORT_DEFAULT,
                        false,
                        Settings.includeMissing,
                        false,
                        count,
                        0,
                    ).map { it.toMediaItem() }
                }
            }
        }
        val root = storageRootPath(folderId) ?: return@mediaCallbackFlow emptyList()
        queryMedia(MediaType.ALL).filter { it.uri.contains(root) }
    }

    override fun observeVideoFolders(
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
    ): Flow<List<MediaFolder>> = libraryCallbackFlow {
        if (!medialibrary.isInitiated) return@libraryCallbackFlow emptyList()
        val folders = runCatching {
            medialibrary.getFolders(
                Folder.TYPE_FOLDER_VIDEO,
                sort.toMlSort(),
                desc,
                Settings.includeMissing,
                onlyFavorites,
                Int.MAX_VALUE,
                0,
            )
        }.getOrNull() ?: return@libraryCallbackFlow emptyList()
        folders.map { folder ->
            val mrl = folder.mMrl.orEmpty()
            MediaFolder(
                id = folder.id,
                title = folder.title.orEmpty().ifBlank {
                    mrl.substringAfterLast('/').ifBlank { mrl }
                },
                path = mrl,
                uri = mrl,
                childCount = folder.mediaCount(Folder.TYPE_FOLDER_VIDEO),
                isRoot = false,
                isFavorite = folder.isFavorite,
                kind = FolderKind.MEDIA_FOLDER,
            )
        }
    }

    override fun observeVideoGroups(
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
    ): Flow<List<MediaFolder>> = callbackFlow {
        fun load(): List<MediaFolder> {
            if (!medialibrary.isInitiated) return emptyList()
            val groups = runCatching {
                medialibrary.getVideoGroups(
                    sort.toMlSort(),
                    desc,
                    Settings.includeMissing,
                    onlyFavorites,
                    Int.MAX_VALUE,
                    0,
                )
            }.getOrNull() ?: return emptyList()
            return groups.map { group ->
                MediaFolder(
                    id = group.id,
                    title = group.title.orEmpty(),
                    path = group.title.orEmpty(),
                    uri = "videogroup://${group.id}",
                    childCount = group.mediaCount(),
                    isRoot = false,
                    isFavorite = group.isFavorite,
                    kind = FolderKind.VIDEO_GROUP,
                )
            }
        }

        fun emitLatest() {
            try {
                trySend(load())
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        val mediaCb = object : Medialibrary.MediaCb {
            override fun onMediaAdded() = emitLatest()
            override fun onMediaModified() = emitLatest()
            override fun onMediaDeleted(id: LongArray?) = emitLatest()
            override fun onMediaConvertedToExternal(id: LongArray?) = emitLatest()
        }
        medialibrary.addMediaCb(mediaCb)
        emitLatest()
        awaitClose { medialibrary.removeMediaCb(mediaCb) }
    }.flowOn(Dispatchers.IO)

    override fun observeVideoGroupMedia(groupId: Long): Flow<List<MediaItem>> = mediaCallbackFlow {
        if (!medialibrary.isInitiated) return@mediaCallbackFlow emptyList()
        val group = medialibrary.getVideoGroup(groupId) ?: return@mediaCallbackFlow emptyList()
        val count = group.mediaCount().coerceAtLeast(0)
        if (count == 0) emptyList()
        else group.media(
            Medialibrary.SORT_DEFAULT,
            false,
            Settings.includeMissing,
            false,
            count,
            0,
        ).map { it.toMediaItem() }
    }

    override fun observeBrowserFavorites(): Flow<List<MediaItem>> =
        browserFavRepository.getFavDao()
            .map { favs ->
                convertFavorites(favs).map { wrapper ->
                    wrapper.toMediaItem().copy(
                        type = MediaType.DIR,
                        isFavorite = true,
                        title = wrapper.title.orEmpty().ifBlank {
                            wrapper.uri?.lastPathSegment ?: wrapper.uri?.toString().orEmpty()
                        },
                        uri = wrapper.uri?.toString().orEmpty(),
                        artworkUri = wrapper.artworkURL,
                    )
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun observeNetworkRoots(): Flow<List<MediaFolder>> {
        if (!settings.getBoolean(KEY_BROWSE_NETWORK, true)) {
            return flowOf(emptyList())
        }
        return combine(
            browserFavRepository.networkFavs,
            networkMonitor.connectionFlow,
        ) { favs, _ ->
            val folders = mutableListOf<MediaFolder>()
            convertFavorites(favs).forEach { wrapper ->
                val path = wrapper.uri?.toString().orEmpty()
                if (path.isBlank()) return@forEach
                folders += MediaFolder(
                    id = path.hashCode().toLong(),
                    title = wrapper.title.orEmpty().ifBlank {
                        wrapper.uri?.lastPathSegment ?: path
                    },
                    path = path,
                    uri = path,
                    isRoot = true,
                    isFavorite = true,
                    kind = FolderKind.NETWORK,
                )
            }
            if (networkMonitor.lanAllowed) {
                folders += MediaFolder(
                    id = LOCAL_NETWORK_FOLDER_ID,
                    title = "Local network",
                    path = "smb://",
                    uri = "smb://",
                    isRoot = true,
                    kind = FolderKind.NETWORK,
                )
            }
            folders
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override fun browseUri(uri: String): Flow<BrowserListing> = callbackFlow {
        var target = uri.trim()
        // OTG placeholder: without a SAF grant emit empty so the shell can request access;
        // with a grant, browse the granted document-tree root instead.
        if (target == "otg://" || target.equals("otg:", ignoreCase = true)) {
            val granted = OtgAccess.otgRoot.value
            if (granted == null) {
                trySend(BrowserListing())
                awaitClose { }
                return@callbackFlow
            }
            target = granted.toString()
        }

        val libVlc = runCatching { VLCInstance.getInstance(appContext) }.getOrNull()
        if (libVlc == null) {
            trySend(BrowserListing())
            awaitClose { }
            return@callbackFlow
        }

        val folders = mutableListOf<MediaFolder>()
        val media = mutableListOf<MediaItem>()
        var browser: MediaBrowser? = null

        val listener = object : MediaBrowser.EventListener {
            override fun onMediaAdded(index: Int, item: IMedia) {
                try {
                    item.retain()
                    val wrapper = try {
                        MLServiceLocator.getAbstractMediaWrapper(item)
                    } catch (e: Exception) {
                        Log.w(TAG, "browseUri: failed to wrap media", e)
                        null
                    }
                    runCatching { item.release() }
                    if (wrapper == null) return

                    val path = wrapper.uri?.toString().orEmpty()
                    if (path.isBlank()) return

                    if (wrapper.type == MediaWrapper.TYPE_DIR) {
                        folders += MediaFolder(
                            id = path.hashCode().toLong(),
                            title = wrapper.title.orEmpty().ifBlank {
                                wrapper.uri?.lastPathSegment ?: path
                            },
                            path = path,
                            uri = path,
                            isRoot = false,
                            kind = folderKindForUri(path),
                        )
                    } else {
                        val resolved = if (
                            medialibrary.isInitiated &&
                            (wrapper.type == MediaWrapper.TYPE_AUDIO || wrapper.type == MediaWrapper.TYPE_VIDEO)
                        ) {
                            runCatching { medialibrary.getMedia(wrapper.uri) }.getOrNull() ?: wrapper
                        } else {
                            wrapper
                        }
                        media += resolved.toMediaItem()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "browseUri: onMediaAdded failed", e)
                }
            }

            override fun onBrowseEnd() {
                trySend(BrowserListing(folders = folders.toList(), media = media.toList()))
                close()
            }

            override fun onMediaRemoved(index: Int, item: IMedia) = Unit
        }

        try {
            browser = MediaBrowser(libVlc, listener)
            val showOnlyMultimedia = settings.getBoolean(
                org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA,
                false,
            )
            if (!showOnlyMultimedia) {
                browser?.setIgnoreFileTypes(".")
            }
            val isNetworkRoot = target.isEmpty() ||
                target == "smb://" ||
                target == "smb:///" ||
                target.equals("smb:", ignoreCase = true)
            if (isNetworkRoot) {
                browser?.discoverNetworkShares()
            } else {
                var flags = MediaBrowser.Flag.Interact
                if (Settings.showHiddenFiles) {
                    flags = flags or MediaBrowser.Flag.ShowHiddenFiles
                }
                browser?.browse(target.toUri(), flags)
            }
        } catch (e: Exception) {
            Log.w(TAG, "browseUri: start failed for $uri", e)
            trySend(BrowserListing())
            close()
        }

        awaitClose {
            runCatching { browser?.changeEventListener(null) }
            runCatching { browser?.release() }
            browser = null
        }
    }.flowOn(Dispatchers.IO)

    override fun observeAudioEntities(
        kind: AudioEntityKind,
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
        query: String,
    ): Flow<List<AudioEntity>> = libraryCallbackFlow {
        loadAudioEntities(kind, sort, desc, onlyFavorites, query)
    }

    override fun observeAudioEntityTracks(
        kind: AudioEntityKind,
        entityId: Long,
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
    ): Flow<List<MediaItem>> = mediaCallbackFlow {
        loadAudioEntityTracks(kind, entityId, sort, desc, onlyFavorites)
    }

    private fun loadAudioEntities(
        kind: AudioEntityKind,
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
        query: String,
    ): List<AudioEntity> {
        if (!medialibrary.isInitiated) return emptyList()
        val includeMissing = Settings.includeMissing
        val mlSort = sort.toMlSort()
        val q = query.trim()
        return when (kind) {
            AudioEntityKind.ARTIST -> {
                val showAll = settings.getBoolean(KEY_ARTISTS_SHOW_ALL, false)
                val artists = if (q.isEmpty()) {
                    medialibrary.getArtists(showAll, mlSort, desc, includeMissing, onlyFavorites)
                } else {
                    medialibrary.searchArtist(q, mlSort, desc, includeMissing, onlyFavorites, Int.MAX_VALUE, 0)
                        ?: emptyArray()
                }
                artists.map { it.toAudioEntity() }
            }
            AudioEntityKind.ALBUM -> {
                val albums = if (q.isEmpty()) {
                    medialibrary.getAlbums(mlSort, desc, includeMissing, onlyFavorites)
                } else {
                    medialibrary.searchAlbum(q, mlSort, desc, includeMissing, onlyFavorites, Int.MAX_VALUE, 0)
                        ?: emptyArray()
                }
                albums.map { it.toAudioEntity() }
            }
            AudioEntityKind.GENRE -> {
                val genres = if (q.isEmpty()) {
                    medialibrary.getGenres(mlSort, desc, includeMissing, onlyFavorites)
                } else {
                    medialibrary.searchGenre(q, mlSort, desc, includeMissing, onlyFavorites, Int.MAX_VALUE, 0)
                        ?: emptyArray()
                }
                genres.map { it.toAudioEntity() }
            }
        }
    }

    private fun loadAudioEntityTracks(
        kind: AudioEntityKind,
        entityId: Long,
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
    ): List<MediaItem> {
        if (!medialibrary.isInitiated) return emptyList()
        val includeMissing = Settings.includeMissing
        val mlSort = sort.toMlSort()
        val tracks: Array<MediaWrapper> = when (kind) {
            AudioEntityKind.ARTIST -> {
                val artist = medialibrary.getArtist(entityId) ?: return emptyList()
                artist.getTracks(mlSort, desc, includeMissing, onlyFavorites) ?: emptyArray()
            }
            AudioEntityKind.ALBUM -> {
                val album = medialibrary.getAlbum(entityId) ?: return emptyList()
                album.getTracks(mlSort, desc, includeMissing, onlyFavorites) ?: emptyArray()
            }
            AudioEntityKind.GENRE -> {
                val genre = medialibrary.getGenre(entityId) ?: return emptyList()
                genre.getTracks(mlSort, desc, includeMissing, onlyFavorites) ?: emptyArray()
            }
        }
        return tracks.map { it.toMediaItem() }
    }

    private fun Artist.toAudioEntity(): AudioEntity = AudioEntity(
        id = id,
        title = title.orEmpty(),
        kind = AudioEntityKind.ARTIST,
        trackCount = tracksCount,
        albumCount = albumsCount,
        artworkUri = artworkMrl,
        subtitle = null,
        isFavorite = isFavorite,
    )

    private fun Album.toAudioEntity(): AudioEntity = AudioEntity(
        id = id,
        title = title.orEmpty(),
        kind = AudioEntityKind.ALBUM,
        trackCount = tracksCount,
        albumCount = 0,
        artworkUri = artworkMrl,
        subtitle = albumArtist,
        isFavorite = isFavorite,
    )

    private fun Genre.toAudioEntity(): AudioEntity = AudioEntity(
        id = id,
        title = title.orEmpty(),
        kind = AudioEntityKind.GENRE,
        trackCount = tracksCount,
        albumCount = 0,
        artworkUri = artworkMrl,
        subtitle = null,
        isFavorite = isFavorite,
    )

    private fun buildStorageRoots(): List<MediaFolder> {
        val roots = mutableListOf<MediaFolder>()
        val primary = AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY
        if (AndroidDevices.showInternalStorage() && File(primary).let { it.exists() && it.canRead() }) {
            roots += MediaFolder(
                id = primary.hashCode().toLong(),
                title = appContext.getString(R.string.internal_memory),
                path = primary,
                uri = "file://$primary",
                isRoot = true,
                kind = FolderKind.STORAGE,
            )
        }
        for (path in AndroidDevices.externalStorageDirectories) {
            val file = File(path)
            if (!file.exists() || !file.canRead()) continue
            val leaf = path.substringAfterLast('/').ifBlank { path }
            val tagged = FileUtils.getStorageTag(leaf)
            val title = when {
                !tagged.isNullOrBlank() -> tagged
                path.contains('-') || leaf.contains('-') -> "SD card"
                else -> leaf
            }
            roots += MediaFolder(
                id = path.hashCode().toLong(),
                title = title,
                path = path,
                uri = "file://$path",
                isRoot = true,
                kind = FolderKind.STORAGE,
            )
        }
        // OTG root when document-tree access is granted, otherwise a placeholder when USB is present.
        val otgUri = OtgAccess.otgRoot.value?.toString()
        if (!otgUri.isNullOrBlank()) {
            roots += MediaFolder(
                id = otgUri.hashCode().toLong(),
                title = appContext.getString(R.string.otg_device_title),
                path = otgUri,
                uri = otgUri,
                isRoot = true,
                kind = FolderKind.STORAGE,
            )
        } else if (VlcMigrationHelper.isLolliPopOrLater && !ExternalMonitor.devices.isEmpty()) {
            val otgPlaceholder = "otg://"
            roots += MediaFolder(
                id = otgPlaceholder.hashCode().toLong(),
                title = appContext.getString(R.string.otg_device_title),
                path = otgPlaceholder,
                uri = otgPlaceholder,
                isRoot = true,
                kind = FolderKind.STORAGE,
            )
        }
        return roots
    }

    private fun storageRootPath(folderId: Long): String? {
        val primary = AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY
        if (primary.hashCode().toLong() == folderId) return primary
        AndroidDevices.externalStorageDirectories.firstOrNull {
            it.hashCode().toLong() == folderId
        }?.let { return it }
        val otgUri = OtgAccess.otgRoot.value?.toString()
        if (otgUri != null && otgUri.hashCode().toLong() == folderId) return otgUri
        if ("otg://".hashCode().toLong() == folderId) return "otg://"
        return null
    }
}

private const val LOCAL_NETWORK_FOLDER_ID = -1L
private const val TAG = "AndroidMediaRepository"

private fun folderKindForUri(uri: String): FolderKind {
    val scheme = uri.substringBefore(':', missingDelimiterValue = "")
        .lowercase()
        .ifBlank {
            // Bare filesystem paths from MediaBrowser.
            if (uri.startsWith('/')) "file" else ""
        }
    return when (scheme) {
        "file", "content", "otg" -> FolderKind.STORAGE
        else -> FolderKind.NETWORK
    }
}

private fun pagingConfig() = PagingConfig(
    pageSize = MEDIA_PAGE_SIZE,
    enablePlaceholders = false,
    initialLoadSize = MEDIA_PAGE_SIZE,
)

internal fun MediaSort.toMlSort(): Int = when (this) {
    MediaSort.DEFAULT -> Medialibrary.SORT_DEFAULT
    MediaSort.TITLE -> Medialibrary.SORT_ALPHA
    MediaSort.FILENAME -> Medialibrary.SORT_FILENAME
    MediaSort.ARTIST -> Medialibrary.SORT_ARTIST
    MediaSort.ALBUM -> Medialibrary.SORT_ALBUM
    MediaSort.DURATION -> Medialibrary.SORT_DURATION
    MediaSort.RELEASE_DATE -> Medialibrary.SORT_RELEASEDATE
    MediaSort.LAST_MODIFIED -> Medialibrary.SORT_LASTMODIFICATIONDATE
    MediaSort.INSERTION_DATE, MediaSort.RECENT -> Medialibrary.SORT_INSERTIONDATE
    MediaSort.FILE_SIZE -> Medialibrary.SORT_FILESIZE
    MediaSort.TRACK_COUNT -> Medialibrary.SORT_PLAYCOUNT
}

/**
 * Offset-based [PagingSource] over medialibrary getPaged* APIs.
 */
private class OffsetMediaPagingSource(
    private val loadPage: (loadSize: Int, offset: Int) -> List<MediaItem>,
    private val totalCount: () -> Int,
    registerInvalidation: ((invalidate: () -> Unit) -> (() -> Unit))? = null,
) : PagingSource<Int, MediaItem>() {

    private val unregisterInvalidation = registerInvalidation?.invoke(::invalidate)

    init {
        registerInvalidatedCallback { unregisterInvalidation?.invoke() }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(state.config.pageSize)
            ?: page.nextKey?.minus(state.config.pageSize)?.coerceAtLeast(0)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        return try {
            val offset = params.key ?: 0
            val loadSize = params.loadSize
            val data = withContext(Dispatchers.IO) { loadPage(loadSize, offset) }
            val total = withContext(Dispatchers.IO) { totalCount() }
            val nextOffset = offset + data.size
            LoadResult.Page(
                data = data,
                prevKey = if (offset <= 0) null else (offset - loadSize).coerceAtLeast(0),
                nextKey = if (data.isEmpty() || nextOffset >= total) null else nextOffset,
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }
}
