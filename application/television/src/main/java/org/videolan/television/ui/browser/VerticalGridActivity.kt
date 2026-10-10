/*****************************************************************************
 * VerticalGridActivity.java
 *
 * Copyright © 2014-2016 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan.television.ui.browser

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.leanback.app.BackgroundManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.Dialog
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.moviepedia.database.models.MediaMetadataType
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
import org.videolan.moviepedia.database.models.subtitle
import org.videolan.moviepedia.provider.MediaScrapingProvider
import org.videolan.resources.CATEGORY
import org.videolan.resources.CATEGORY_ALBUMS
import org.videolan.resources.CATEGORY_ARTISTS
import org.videolan.resources.CATEGORY_GENRES
import org.videolan.resources.CATEGORY_PLAYLISTS
import org.videolan.resources.CATEGORY_SONGS
import org.videolan.resources.CATEGORY_VIDEOS
import org.videolan.resources.FAVORITE_TITLE
import org.videolan.resources.HEADER_CATEGORIES
import org.videolan.resources.HEADER_DIRECTORIES
import org.videolan.resources.HEADER_MOVIES
import org.videolan.resources.HEADER_NETWORK
import org.videolan.resources.HEADER_PLAYLISTS
import org.videolan.resources.HEADER_TV_SHOW
import org.videolan.resources.HEADER_VIDEO
import org.videolan.resources.ITEM
import org.videolan.resources.KEY_URI
import org.videolan.resources.util.getFromMl
import org.videolan.resources.util.parcelable
import org.videolan.television.ui.MainTvActivity
import org.videolan.television.ui.MediaScrapingTvshowDetailsActivity
import org.videolan.television.ui.TvUtil
import org.videolan.television.ui.TV_SHOW_ID
import org.videolan.television.ui.TvEntries
import org.videolan.television.ui.toTvItem
import org.videolan.television.ui.clearBackground
import org.videolan.television.ui.updateBackground
import org.videolan.television.viewmodel.MediaBrowserViewModel
import org.videolan.television.viewmodel.MediaScrapingBrowserViewModel
import org.videolan.tools.PLAYLIST_MODE_AUDIO
import org.videolan.tools.PLAYLIST_MODE_VIDEO
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.VLCTvBrowserLabels
import org.videolan.vlc.compose.tv.VLCTvBrowserScreen
import org.videolan.vlc.compose.tv.VLCTvCardShape
import org.videolan.vlc.compose.tv.VLCTvSortOption
import org.videolan.vlc.compose.tv.VLCTvSymbols
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.compose.tv.gridColumns
import org.videolan.vlc.providers.medialibrary.MedialibraryProvider
import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.viewmodels.SortableModel
import org.videolan.vlc.viewmodels.browser.BrowserModel
import org.videolan.vlc.viewmodels.browser.NetworkModel
import org.videolan.vlc.viewmodels.browser.TYPE_FILE
import org.videolan.vlc.viewmodels.browser.TYPE_NETWORK
import org.videolan.vlc.R as VlcR

private data class FileBrowserEntry(val category: Long, val item: MediaLibraryItem?, val rootLevel: Boolean)

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class VerticalGridActivity : BaseTvActivity() {

    private var mediaModel: MediaBrowserViewModel? = null
    private var moviepediaModel: MediaScrapingBrowserViewModel? = null
    private var fileModel: BrowserModel? = null
    private var backgroundManager: BackgroundManager? = null
    private var selectedMediaItem: MediaLibraryItem? = null
    private var selectedMoviepediaItem: MediaMetadataWithImages? = null
    private var selectedFileItem: MediaLibraryItem? = null
    private var mediaItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var mediaLoading by mutableStateOf(true)
    private var mediaInGrid by mutableStateOf(true)
    private var moviepediaItems by mutableStateOf<List<MediaMetadataWithImages>>(emptyList())
    private var moviepediaLoading by mutableStateOf(true)
    private var moviepediaInGrid by mutableStateOf(true)
    private var fileItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var fileLoading by mutableStateOf(true)
    private var fileInGrid by mutableStateOf(true)
    private var fileTitle by mutableStateOf("")
    private var fileSubtitle by mutableStateOf<String?>(null)
    private var fileFavoriteVisible by mutableStateOf(false)
    private var fileFavorite by mutableStateOf(false)
    private var fileEntry: FileBrowserEntry? = null
    private val fileBackStack = mutableListOf<FileBrowserEntry>()
    private val browserFavRepository by lazy(LazyThreadSafetyMode.NONE) { BrowserFavRepository.getInstance(this) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getLongExtra(MainTvActivity.BROWSER_TYPE, -1)
        val mediaRoute = mediaRoute(type)
        if (mediaRoute != null) {
            setupMediaBrowser(mediaRoute.first, mediaRoute.second)
        } else if (type == HEADER_MOVIES || type == HEADER_TV_SHOW) {
            setupMoviepediaBrowser(type)
        } else {
            val fileRoute = fileRoute(type)
            if (fileRoute != null) {
                setupFileBrowser(savedInstanceState, fileRoute)
            } else {
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        backgroundManager?.let { clearBackground(this, it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fileEntry?.let { entry ->
            outState.putLong(CATEGORY, entry.category)
            outState.putBoolean(FILE_BROWSER_ROOT_LEVEL, entry.rootLevel)
            (entry.item as? Parcelable)?.let { outState.putParcelable(ITEM, it) }
        }
        super.onSaveInstanceState(outState)
    }

    override fun dialogCanceled(dialog: Dialog?) {
        when (dialog) {
            is Dialog.ErrorMessage -> {
                Snackbar.make(window.decorView, "${dialog.title}: ${dialog.text}", Snackbar.LENGTH_LONG).show()
                if (fileModel != null) goBackFromFileBrowser()
            }
            is Dialog.LoginDialog -> if (fileModel != null) goBackFromFileBrowser()
            else -> super.dialogCanceled(dialog)
        }
    }

    private fun mediaRoute(type: Long): Pair<Long, MediaLibraryItem?>? = when (type) {
        HEADER_VIDEO -> CATEGORY_VIDEOS to null
        HEADER_PLAYLISTS -> CATEGORY_PLAYLISTS to null
        HEADER_CATEGORIES -> {
            val category = intent.getLongExtra(CATEGORY, CATEGORY_SONGS)
            when (category) {
                CATEGORY_SONGS,
                CATEGORY_ALBUMS,
                CATEGORY_ARTISTS,
                CATEGORY_GENRES -> category to intent.parcelable<Parcelable>(ITEM) as? MediaLibraryItem
                else -> null
            }
        }
        else -> null
    }

    private fun fileRoute(type: Long): FileBrowserEntry? = when (type) {
        HEADER_NETWORK -> {
            var uri = intent.data
            if (uri == null) uri = intent.parcelable(KEY_URI)
            val item = uri?.let { MLServiceLocator.getAbstractMediaWrapper(it) }
            if (item != null && intent.hasExtra(FAVORITE_TITLE)) item.title = intent.getStringExtra(FAVORITE_TITLE)
            FileBrowserEntry(TYPE_NETWORK, item, item == null)
        }
        HEADER_DIRECTORIES -> FileBrowserEntry(TYPE_FILE, intent.data?.let { MLServiceLocator.getAbstractMediaWrapper(it) }, true)
        else -> null
    }

    private fun setupMediaBrowser(category: Long, parent: MediaLibraryItem?) {
        val displayPrefId = mediaDisplayPrefId(category)
        mediaInGrid = Settings.getInstance(this).getBoolean(displayPrefId, true)
        backgroundManager = BackgroundManager.getInstance(this).apply { attach(window) }
        val model = ViewModelProvider(this, MediaBrowserViewModel.Factory(this, category, parent))[MediaBrowserViewModel::class.java]
        mediaModel = model
        model.nbColumns = mediaColumnCount(category)

        val provider = model.provider as MedialibraryProvider<*>
        provider.pagedList.observe(this) { pagedList ->
            mediaItems = pagedList?.filterNotNull().orEmpty()
            mediaLoading = false
        }
        provider.loading.observe(this) { loading ->
            mediaLoading = loading == true
        }
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTvTheme {
                        val shape = if (category == CATEGORY_VIDEOS) VLCTvCardShape.Landscape else VLCTvCardShape.Square
                        val entries = remember(mediaItems) { TvEntries(mediaItems) { it.toTvItem(shape) } }
                        VLCTvBrowserScreen(
                            title = mediaTitle(category),
                            eyebrow = parent?.title ?: getString(if (category == CATEGORY_VIDEOS) VlcR.string.video else VlcR.string.audio),
                            items = entries.items,
                            loading = mediaLoading,
                            inGrid = mediaInGrid,
                            shape = shape,
                            labels = browserLabels(),
                            emptyTitle = getString(VlcR.string.nomedia),
                            emptySymbol = if (category == CATEGORY_VIDEOS) MaterialSymbols.Filled.VideoLibrary else MaterialSymbols.Filled.MusicNote,
                            sortOptions = mediaSortActions(model),
                            onBack = ::finish,
                            onToggleDisplay = {
                                mediaInGrid = !mediaInGrid
                                Settings.getInstance(this@VerticalGridActivity).putSingle(displayPrefId, mediaInGrid)
                            },
                            onSort = model::sort,
                            onItemFocused = { entries[it]?.let(::onMediaItemFocused) },
                            onItemClick = { entries[it]?.let(::onMediaItemClicked) },
                            artwork = entries.artwork,
                        )
                    }
                }
            }
        )
    }

    private fun setupMoviepediaBrowser(category: Long) {
        val displayPrefId = moviepediaDisplayPrefId(category)
        moviepediaInGrid = Settings.getInstance(this).getBoolean(displayPrefId, true)
        backgroundManager = BackgroundManager.getInstance(this).apply { attach(window) }
        val model = ViewModelProvider(this, MediaScrapingBrowserViewModel.Factory(this, category))[MediaScrapingBrowserViewModel::class.java]
        moviepediaModel = model
        model.nbColumns = moviepediaColumnCount()

        val provider = model.provider as MediaScrapingProvider
        provider.pagedList.observe(this) { pagedList ->
            moviepediaItems = pagedList?.filterNotNull().orEmpty()
            moviepediaLoading = false
        }
        provider.loading.observe(this) { loading ->
            moviepediaLoading = loading == true
        }
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTvTheme {
                        val entries = remember(moviepediaItems) { TvEntries(moviepediaItems) { it.toTvItem(it.subtitle().takeIf(String::isNotBlank)) } }
                        VLCTvBrowserScreen(
                            title = moviepediaTitle(category),
                            eyebrow = getString(VlcR.string.video),
                            items = entries.items,
                            loading = moviepediaLoading,
                            inGrid = moviepediaInGrid,
                            shape = VLCTvCardShape.Poster,
                            labels = browserLabels(),
                            emptyTitle = getString(VlcR.string.nomedia),
                            emptySymbol = if (category == HEADER_TV_SHOW) VLCTvSymbols.Tv else VLCTvSymbols.Movie,
                            sortOptions = mediaSortActions(model),
                            onBack = ::finish,
                            onToggleDisplay = {
                                moviepediaInGrid = !moviepediaInGrid
                                Settings.getInstance(this@VerticalGridActivity).putSingle(displayPrefId, moviepediaInGrid)
                            },
                            onSort = model::sort,
                            onItemFocused = { entries[it]?.let(::onMoviepediaItemFocused) },
                            onItemClick = { entries[it]?.let(::openMoviepediaItem) },
                            artwork = entries.artwork,
                        )
                    }
                }
            }
        )
    }

    private fun setupFileBrowser(savedInstanceState: Bundle?, initialEntry: FileBrowserEntry) {
        val restoredEntry = savedInstanceState?.let {
            FileBrowserEntry(
                it.getLong(CATEGORY, initialEntry.category),
                it.parcelable<Parcelable>(ITEM) as? MediaLibraryItem ?: initialEntry.item,
                it.getBoolean(FILE_BROWSER_ROOT_LEVEL, initialEntry.rootLevel)
            )
        } ?: initialEntry
        val displayPrefId = fileDisplayPrefId(restoredEntry.category)
        fileInGrid = Settings.getInstance(this).getBoolean(displayPrefId, true)
        backgroundManager = BackgroundManager.getInstance(this).apply { attach(window) }
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTvTheme {
                        val entries = remember(fileItems) { TvEntries(fileItems) { it.toTvItem(VLCTvCardShape.Landscape) } }
                        VLCTvBrowserScreen(
                            title = fileTitle,
                            eyebrow = getString(if (fileEntry?.category == TYPE_NETWORK) VlcR.string.network_browsing else VlcR.string.directories),
                            subtitle = fileSubtitle,
                            items = entries.items,
                            loading = fileLoading,
                            inGrid = fileInGrid,
                            shape = VLCTvCardShape.Landscape,
                            labels = browserLabels(),
                            emptyTitle = fileEmptyText(),
                            emptySymbol = if (fileEntry?.category == TYPE_NETWORK) VLCTvSymbols.Cloud else MaterialSymbols.Filled.Folder,
                            sortOptions = fileModel?.let { mediaSortActions(it) }.orEmpty(),
                            favorite = fileFavorite.takeIf { fileFavoriteVisible },
                            onBack = ::goBackFromFileBrowser,
                            onToggleDisplay = {
                                fileInGrid = !fileInGrid
                                Settings.getInstance(this@VerticalGridActivity).putSingle(displayPrefId, fileInGrid)
                            },
                            onSort = { sort -> fileModel?.sort(sort) },
                            onFavorite = ::toggleFileFavorite,
                            onItemFocused = { entries[it]?.let(::onFileItemFocused) },
                            onItemClick = { entries[it]?.let(::onFileItemClicked) },
                            focusResetKey = fileSubtitle ?: fileTitle,
                            artwork = entries.artwork,
                        )
                    }
                }
            }
        )
        showFileBrowser(restoredEntry, addToBackStack = false)
    }

    override fun refresh() {
        mediaModel?.refresh() ?: moviepediaModel?.refresh() ?: fileModel?.refresh()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val mediaModel = mediaModel
        if (mediaModel != null) {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_BUTTON_Y || keyCode == KeyEvent.KEYCODE_Y) {
                (selectedMediaItem as? MediaWrapper)?.let {
                    TvUtil.showMediaDetail(this, it)
                    return true
                }
            }
            return super.onKeyDown(keyCode, event)
        }
        if (moviepediaModel != null) {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_BUTTON_Y || keyCode == KeyEvent.KEYCODE_Y) {
                selectedMoviepediaItem?.let {
                    openMoviepediaItem(it)
                    return true
                }
            }
            return super.onKeyDown(keyCode, event)
        }
        if (fileModel != null) {
            if (keyCode == KeyEvent.KEYCODE_BOOKMARK) {
                toggleFileFavorite()
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_BACK && fileBackStack.isNotEmpty()) {
                goBackFromFileBrowser()
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_BUTTON_Y || keyCode == KeyEvent.KEYCODE_Y) {
                (selectedFileItem as? MediaWrapper)?.takeIf { it.type != MediaWrapper.TYPE_DIR }?.let {
                    TvUtil.showMediaDetail(this, it)
                    return true
                }
            }
            return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onMediaItemFocused(item: MediaLibraryItem) {
        selectedMediaItem = item
        backgroundManager?.let { lifecycleScope.updateBackground(this, it, item) }
    }

    private fun onMoviepediaItemFocused(item: MediaMetadataWithImages) {
        selectedMoviepediaItem = item
        backgroundManager?.let { lifecycleScope.updateBackground(this, it, item) }
    }

    private fun onFileItemFocused(item: MediaLibraryItem) {
        selectedFileItem = item
        backgroundManager?.let { lifecycleScope.updateBackground(this, it, item) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun onMediaItemClicked(item: MediaLibraryItem) {
        val model = mediaModel ?: return
        lifecycleScope.launch {
            if (model.category == CATEGORY_VIDEOS && !Settings.getInstance(this@VerticalGridActivity).getBoolean(PLAYLIST_MODE_VIDEO, Settings.tvUI)) {
                TvUtil.playMedia(this@VerticalGridActivity, item as MediaWrapper)
            } else if (model.category == CATEGORY_SONGS && !Settings.getInstance(this@VerticalGridActivity).getBoolean(PLAYLIST_MODE_AUDIO, Settings.tvUI)) {
                TvUtil.playMedia(this@VerticalGridActivity, item as MediaWrapper)
            } else {
                TvUtil.openMediaFromPaged(this@VerticalGridActivity, item, model.provider as MedialibraryProvider<out MediaLibraryItem>)
            }
        }
    }

    private fun onFileItemClicked(item: MediaLibraryItem) {
        val media = item as? MediaWrapper ?: return
        val model = fileModel ?: return
        media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
        if (media.type == MediaWrapper.TYPE_DIR) {
            model.saveList(media)
            val currentEntry = fileEntry ?: return
            showFileBrowser(FileBrowserEntry(currentEntry.category, media, rootLevel = false), addToBackStack = true)
        } else {
            TvUtil.openMedia(this, item, model)
        }
    }

    private fun showFileBrowser(entry: FileBrowserEntry, addToBackStack: Boolean) {
        val previousModel = fileModel
        if (addToBackStack) fileEntry?.let { fileBackStack.add(it) }
        fileEntry = entry
        fileTitle = fileBrowserTitle(entry)
        fileSubtitle = fileBrowserSubtitle(entry)
        fileLoading = true
        fileItems = emptyList()
        selectedFileItem = null
        updateFileFavoriteState(entry.item)

        val url = (entry.item as? MediaWrapper)?.location
        val model = if (entry.category == TYPE_NETWORK) {
            ViewModelProvider(this, NetworkModel.Factory(this, url))["tv-file-network-${url ?: "root"}", NetworkModel::class.java]
        } else {
            ViewModelProvider(this, BrowserModel.Factory(this, url, entry.category))["tv-file-${entry.category}-${url ?: "root"}", BrowserModel::class.java]
        }
        previousModel?.let {
            it.dataset.removeObservers(this)
            it.loading.removeObservers(this)
            it.getDescriptionUpdate().removeObservers(this)
            it.provider.liveHeaders.removeObservers(this)
        }
        fileModel = model
        model.currentItem = entry.item
        model.nbColumns = fileColumnCount()
        model.dataset.observe(this) { items ->
            fileItems = items?.toList().orEmpty()
            fileLoading = false
        }
        model.loading.observe(this) { loading ->
            fileLoading = loading == true
        }
        model.getDescriptionUpdate().observe(this) {
            fileItems = model.dataset.value?.toList().orEmpty()
        }
        model.provider.liveHeaders.observe(this) {
            fileItems = model.dataset.value?.toList().orEmpty()
        }
        if (entry.item == null) {
            model.browseRoot()
        } else if (entry.category == TYPE_NETWORK || model.dataset.value.isNullOrEmpty()) {
            model.refresh()
        }
    }

    private fun goBackFromFileBrowser() {
        val previous = fileBackStack.removeLastOrNull()
        if (previous == null) {
            finish()
        } else {
            showFileBrowser(previous, addToBackStack = false)
        }
    }

    private fun updateFileFavoriteState(item: MediaLibraryItem?) {
        val media = item as? MediaWrapper
        fileFavoriteVisible = media != null
        fileFavorite = false
        if (media == null) return
        lifecycleScope.launch {
            fileFavorite = withContext(Dispatchers.IO) { browserFavRepository.browserFavExists(media.uri) }
        }
    }

    private fun toggleFileFavorite() {
        val media = fileEntry?.item as? MediaWrapper ?: return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (browserFavRepository.browserFavExists(media.uri)) {
                    browserFavRepository.deleteBrowserFav(media.uri)
                } else if (media.uri.scheme == "file") {
                    browserFavRepository.addLocalFavItem(media.uri, media.title, media.artworkURL)
                } else {
                    browserFavRepository.addNetworkFavItem(media.uri, media.title, media.artworkURL)
                }
            }
            updateFileFavoriteState(media)
        }
    }

    private fun mediaTitle(category: Long) = when (category) {
        CATEGORY_SONGS -> getString(VlcR.string.tracks)
        CATEGORY_ALBUMS -> getString(VlcR.string.albums)
        CATEGORY_ARTISTS -> getString(VlcR.string.artists)
        CATEGORY_GENRES -> getString(VlcR.string.genres)
        CATEGORY_PLAYLISTS -> getString(VlcR.string.playlists)
        else -> getString(VlcR.string.video)
    }

    private fun mediaColumnCount(category: Long) =
        (if (category == CATEGORY_VIDEOS) VLCTvCardShape.Landscape else VLCTvCardShape.Square).gridColumns()

    private fun mediaDisplayPrefId(category: Long) = "display_tv_media_$category"

    private fun fileBrowserTitle(entry: FileBrowserEntry): String {
        val media = entry.item as? MediaWrapper
        if (media != null) {
            return media.title.takeIf { !it.isNullOrBlank() }
                ?: FileUtils.getFileNameFromPath(media.location).takeIf { it.isNotBlank() }
                ?: getString(if (entry.category == TYPE_NETWORK) VlcR.string.network_browsing else VlcR.string.directories)
        }
        return getString(if (entry.category == TYPE_NETWORK) VlcR.string.network_browsing else VlcR.string.directories)
    }

    private fun fileBrowserSubtitle(entry: FileBrowserEntry): String? {
        val location = (entry.item as? MediaWrapper)?.location?.takeIf { it.isNotBlank() } ?: return null
        return Uri.decode(location).replace("://", "/").split('/').filter { it.isNotBlank() }.joinToString("  ›  ")
    }

    private fun fileEmptyText() = when (fileEntry?.category) {
        TYPE_NETWORK -> getString(VlcR.string.network_empty)
        else -> getString(VlcR.string.empty_directory)
    }

    private fun fileColumnCount() = VLCTvCardShape.Landscape.gridColumns()

    private fun fileDisplayPrefId(category: Long) = "display_tv_file_$category"

    private fun moviepediaTitle(category: Long) = when (category) {
        HEADER_TV_SHOW -> getString(VlcR.string.header_tvshows)
        HEADER_MOVIES -> getString(VlcR.string.header_movies)
        else -> getString(VlcR.string.video)
    }

    private fun moviepediaColumnCount() = VLCTvCardShape.Poster.gridColumns()

    private fun moviepediaDisplayPrefId(category: Long) = "display_tv_moviepedia_$category"

    private fun openMoviepediaItem(item: MediaMetadataWithImages) {
        when (item.metadata.type) {
            MediaMetadataType.TV_SHOW -> {
                val intent = Intent(this, MediaScrapingTvshowDetailsActivity::class.java)
                intent.putExtra(TV_SHOW_ID, item.metadata.moviepediaId)
                startActivity(intent)
            }
            else -> item.metadata.mlId?.let {
                lifecycleScope.launch {
                    val media = getFromMl { getMedia(it) }
                    TvUtil.showMediaDetail(this@VerticalGridActivity, media)
                }
            }
        }
    }

    private fun mediaSortActions(model: SortableModel) = buildList {
        if (model.canSortByName()) add(VLCTvSortOption(getString(VlcR.string.sortby_name), Medialibrary.SORT_ALPHA))
        if (model.canSortByFileNameName()) add(VLCTvSortOption(getString(VlcR.string.sortby_filename), Medialibrary.SORT_FILENAME))
        if (model.canSortByDuration()) add(VLCTvSortOption(getString(VlcR.string.sortby_length), Medialibrary.SORT_DURATION))
        if (model.canSortByInsertionDate()) add(VLCTvSortOption(getString(VlcR.string.sortby_date_insertion), Medialibrary.SORT_INSERTIONDATE))
        if (model.canSortByReleaseDate()) add(VLCTvSortOption(getString(VlcR.string.sortby_date_release), Medialibrary.SORT_RELEASEDATE))
        if (model.canSortByLastModified()) add(VLCTvSortOption(getString(VlcR.string.sortby_last_modified_date), Medialibrary.SORT_LASTMODIFICATIONDATE))
        if (model.canSortByArtist()) add(VLCTvSortOption(getString(VlcR.string.sortby_artist_name), Medialibrary.SORT_ARTIST))
        if (model.canSortByAlbum()) add(VLCTvSortOption(getString(VlcR.string.sortby_album_name), Medialibrary.SORT_ALBUM))
    }

    private fun browserLabels() = VLCTvBrowserLabels(
        back = getString(VlcR.string.close),
        sort = getString(VlcR.string.sortby),
        showAsList = getString(VlcR.string.display_in_list),
        showAsGrid = getString(VlcR.string.display_in_grid),
        addFavorite = getString(VlcR.string.favorites_add),
        removeFavorite = getString(VlcR.string.favorites_remove),
    )

    companion object {
        private const val FILE_BROWSER_ROOT_LEVEL = "rootLevel"
    }
}

