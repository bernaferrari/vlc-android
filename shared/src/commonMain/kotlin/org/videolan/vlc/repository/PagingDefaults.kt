package org.videolan.vlc.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.PlaylistInfo

/**
 * Live paging for repositories without a native paged source (iOS, Web, desktop). Each catalog
 * emission republishes the list, so metadata enrichment (artwork, durations, tags) and imports
 * appear immediately instead of after the next launch.
 */
internal fun MediaRepository.defaultPagedMedia(query: MediaQuery): Flow<PagingData<MediaItem>> =
    mediaSource(query).map { PagingData.from(it) }

internal fun PlaylistRepository.defaultPagedPlaylists(
    sort: MediaSort,
    desc: Boolean,
    onlyFavorites: Boolean,
    query: String,
): Flow<PagingData<PlaylistInfo>> {
    return Pager(
        config = PagingConfig(
            pageSize = MEDIA_PAGE_SIZE,
            enablePlaceholders = false,
            initialLoadSize = MEDIA_PAGE_SIZE,
        ),
        pagingSourceFactory = {
            ListPagingSource {
                var list = observePlaylists().first()
                if (onlyFavorites) list = list.filter { it.isFavorite }
                if (query.isNotBlank()) {
                    list = list.filter { it.name.contains(query, ignoreCase = true) }
                }
                list = when (sort) {
                    MediaSort.DURATION -> list.sortedBy { it.duration }
                    MediaSort.TRACK_COUNT -> list.sortedBy { it.itemCount }
                    else -> list.sortedBy { it.name.lowercase() }
                }
                if (desc) list = list.reversed()
                list
            }
        },
    ).flow
}

private fun MediaRepository.mediaSource(query: MediaQuery): Flow<List<MediaItem>> {
    val q = query.query.trim()
    val source = if (q.isEmpty()) {
        when (query.containerKind) {
            ContainerKind.FOLDER -> query.containerId?.let { observeFolderMedia(it) } ?: flowOf(emptyList())
            ContainerKind.VIDEO_GROUP -> query.containerId?.let { observeVideoGroupMedia(it) } ?: flowOf(emptyList())
            ContainerKind.NONE -> observeMedia(query.type)
        }
    } else {
        search(q, query.type)
    }
    return source.map { items ->
        var list = if (query.onlyFavorites) items.filter { it.isFavorite } else items
        list = sortMedia(list, query.sort)
        if (query.desc) list.reversed() else list
    }
}

internal fun sortMedia(items: List<MediaItem>, sort: MediaSort): List<MediaItem> = when (sort) {
    MediaSort.DEFAULT, MediaSort.TITLE -> items.sortedBy { it.displayTitle.lowercase() }
    MediaSort.FILENAME -> items.sortedBy { (it.fileName ?: it.displayTitle).lowercase() }
    MediaSort.ARTIST -> items.sortedWith(
        compareBy({ it.artist?.lowercase().orEmpty() }, { it.displayTitle.lowercase() })
    )
    MediaSort.ALBUM -> items.sortedWith(
        compareBy({ it.album?.lowercase().orEmpty() }, { it.trackNumber }, { it.displayTitle.lowercase() })
    )
    MediaSort.DURATION -> items.sortedBy { it.duration }
    MediaSort.RELEASE_DATE -> items.sortedBy { it.year }
    MediaSort.LAST_MODIFIED -> items.sortedBy { it.lastModified }
    MediaSort.INSERTION_DATE, MediaSort.RECENT -> items.sortedBy { it.lastPlayed }
    MediaSort.FILE_SIZE -> items.sortedBy { it.size }
    MediaSort.TRACK_COUNT -> items.sortedBy { it.playedCount }
}

/**
 * Simple in-memory [PagingSource] over a suspend snapshot loader.
 * Used as the default for platforms without native paged ML queries.
 */
class ListPagingSource<T : Any>(
    /** Optional platform hook used to invalidate stale page snapshots. */
    registerInvalidation: ((invalidate: () -> Unit) -> (() -> Unit))? = null,
    private val loader: suspend () -> List<T>,
) : PagingSource<Int, T>() {
    private val unregisterInvalidation = registerInvalidation?.invoke(::invalidate)

    init {
        registerInvalidatedCallback { unregisterInvalidation?.invoke() }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(page.data.size) ?: page.nextKey?.minus(page.data.size)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try {
            val offset = params.key ?: 0
            val all = loader()
            val from = offset.coerceAtMost(all.size)
            val to = (from + params.loadSize).coerceAtMost(all.size)
            val slice = if (from < to) all.subList(from, to) else emptyList()
            LoadResult.Page(
                data = slice,
                prevKey = if (from == 0) null else (from - params.loadSize).coerceAtLeast(0),
                nextKey = if (to >= all.size) null else to,
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }
}

/** Convenience map for full-list flows that still want page-sized emissions. */
fun <T> Flow<List<T>>.takePage(limit: Int): Flow<List<T>> = map { it.take(limit) }

fun MediaType.matches(item: MediaItem): Boolean =
    this == MediaType.ALL || item.type == this
