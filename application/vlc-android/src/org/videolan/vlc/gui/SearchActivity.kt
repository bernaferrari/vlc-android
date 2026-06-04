package org.videolan.vlc.gui

import android.app.SearchManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.Genre
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.medialibrary.media.SearchAggregate
import org.videolan.resources.util.getFromMl
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCSearchResultRow
import org.videolan.vlc.compose.components.VLCSearchScreen
import org.videolan.vlc.compose.components.VLCSearchSection
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.generateResolutionClass

open class SearchActivity : BaseActivity() {

    private var query by mutableStateOf("")
    private var sections by mutableStateOf(emptyList<SearchSectionItems>())
    private var showEmpty by mutableStateOf(false)
    private var autoFocus by mutableStateOf(true)
    private var searchJob: Job? = null

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = findViewById(android.R.id.content)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialQuery = when (intent.action) {
            Intent.ACTION_SEARCH, "com.google.android.gms.actions.SEARCH_ACTION" -> intent.getStringExtra(SearchManager.QUERY)
            else -> null
        }.orEmpty()

        if (initialQuery.isNotEmpty()) {
            autoFocus = false
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            query = initialQuery
            performSearch(initialQuery)
        }

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCSearchScreen(
                        query = query,
                        hint = getString(R.string.search_hint),
                        emptyText = getString(R.string.search_no_result),
                        backContentDescription = getString(R.string.close),
                        clearContentDescription = getString(R.string.clear),
                        sections = sections.map { it.section },
                        showEmpty = showEmpty,
                        autoFocus = autoFocus,
                        onQueryChange = ::onQueryChanged,
                        onSearchAction = { UiTools.setKeyboardVisibility(window.decorView, false) },
                        onBack = ::finish,
                        onClear = {
                            onQueryChanged("")
                            autoFocus = true
                        },
                        onResultClick = ::onResultClick,
                        backIconContent = { SearchIcon(R.drawable.ic_arrow_back) },
                        clearIconContent = { SearchIcon(R.drawable.ic_close_small) },
                        thumbnailContent = { sectionIndex, rowIndex, row ->
                            SearchResultThumbnail(
                                item = sections.getOrNull(sectionIndex)?.items?.getOrNull(rowIndex),
                                thumbnailWide = row.thumbnailWide
                            )
                        }
                    )
                }
            }
        )
    }

    private fun onQueryChanged(value: String) {
        query = value
        if (value.isEmpty()) {
            searchJob?.cancel()
            sections = emptyList()
            showEmpty = false
        } else {
            performSearch(value)
        }
    }

    private fun performSearch(value: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            val searchAggregate = getFromMl { search(value, Settings.includeMissing, false) }
            sections = searchAggregate.toSections()
            showEmpty = searchAggregate.isEmpty
        }
    }

    private fun onResultClick(sectionIndex: Int, rowIndex: Int) {
        val item = sections.getOrNull(sectionIndex)?.items?.getOrNull(rowIndex) ?: return
        MediaUtils.playTracks(this@SearchActivity, item, 0)
        finish()
    }

    private fun SearchAggregate.toSections() = listOf(
        SearchSectionItems(getString(R.string.videos), videos?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.tracks), tracks?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.albums), albums?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.artists), artists?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.genres), genres?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.playlists), playlists?.filterNotNull().orEmpty())
    ).filter { it.items.isNotEmpty() }

    private inner class SearchSectionItems(
        title: String,
        val items: List<MediaLibraryItem>
    ) {
        val section = VLCSearchSection(
            title = title,
            rows = items.map { it.toSearchRow() }
        )
    }

    private fun MediaLibraryItem.toSearchRow(): VLCSearchResultRow {
        val isVideo = this is MediaWrapper && type == MediaWrapper.TYPE_VIDEO
        return VLCSearchResultRow(
            id = "${itemType}-${id}-${title}",
            title = title.orEmpty(),
            description = searchDescription(),
            thumbnailWide = isVideo
        )
    }

    private fun MediaLibraryItem.searchDescription(): String? = when {
        (this as? MediaWrapper)?.type == MediaWrapper.TYPE_VIDEO -> {
            if (length > 0) {
                val resolution = generateResolutionClass(width, height)
                if (resolution != null) "${Tools.millisToString(length)}  •  $resolution"
                else Tools.millisToString(length)
            } else null
        }
        this is Playlist || this is Genre -> getString(R.string.track_number, tracksCount)
        else -> description
    }

    companion object {

        const val TAG = "VLC/SearchActivity"
    }
}

@Composable
private fun SearchIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun SearchResultThumbnail(item: MediaLibraryItem?, thumbnailWide: Boolean) {
    if (item == null) return
    VlcMediaImage(
        item = item,
        width = if (thumbnailWide) 100.dp else 48.dp,
        fallbackPainter = painterResource(item.defaultSearchCoverResource),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
        reloadKey = Settings.showVideoThumbs,
        thumbnailLoader = ::loadSearchThumbnail
    )
}

private suspend fun loadSearchThumbnail(item: MediaLibraryItem, imageWidth: Int): Bitmap? {
    if (!Settings.showVideoThumbs && item is MediaWrapper && item.type == MediaWrapper.TYPE_VIDEO) return null
    if ((item.itemType == MediaLibraryItem.TYPE_PLAYLIST || item.itemType == MediaLibraryItem.TYPE_GENRE) && imageWidth > 0) {
        val tracks = withContext(Dispatchers.IO) { item.tracks.toList() }
        val key = if (item is MediaWrapper && item.type == MediaWrapper.TYPE_PLAYLIST) "playlist" else "genre"
        return ThumbnailsProvider.getPlaylistOrGenreImage("$key:${item.id}_$imageWidth", tracks, imageWidth)
    }
    return ThumbnailsProvider.obtainBitmap(item, imageWidth)
}

private val MediaLibraryItem.defaultSearchCoverResource: Int
    get() = when (itemType) {
        MediaLibraryItem.TYPE_ARTIST -> R.drawable.ic_no_artist
        MediaLibraryItem.TYPE_ALBUM -> R.drawable.ic_no_album
        MediaLibraryItem.TYPE_MEDIA -> {
            if ((this as? MediaWrapper)?.type == MediaWrapper.TYPE_VIDEO) R.drawable.ic_no_thumbnail_1610 else R.drawable.ic_no_song
        }
        else -> R.drawable.ic_no_song
    }
