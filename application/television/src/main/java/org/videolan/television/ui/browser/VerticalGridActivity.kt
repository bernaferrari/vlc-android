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
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
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
import org.videolan.resources.util.parcelable
import org.videolan.television.databinding.TvVerticalGridBinding
import org.videolan.television.ui.MainTvActivity
import org.videolan.television.ui.TvUtil
import org.videolan.television.ui.browser.interfaces.BrowserActivityInterface
import org.videolan.television.ui.browser.interfaces.DetailsFragment
import org.videolan.television.ui.clearBackground
import org.videolan.television.ui.updateBackground
import org.videolan.television.viewmodel.MediaBrowserViewModel
import org.videolan.tools.PLAYLIST_MODE_AUDIO
import org.videolan.tools.PLAYLIST_MODE_VIDEO
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.getTvIconRes
import org.videolan.vlc.gui.helpers.loadImage
import org.videolan.vlc.interfaces.BrowserFragmentInterface
import org.videolan.vlc.providers.medialibrary.MedialibraryProvider
import org.videolan.vlc.util.generateResolutionClass
import org.videolan.vlc.viewmodels.SortableModel
import org.videolan.vlc.viewmodels.browser.TYPE_FILE
import org.videolan.vlc.viewmodels.browser.TYPE_NETWORK
import org.videolan.television.R as TvR
import org.videolan.vlc.R as VlcR

private data class MediaSortAction(val label: String, val sort: Int)

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class VerticalGridActivity : BaseTvActivity(), BrowserActivityInterface {

    private var fragment: BrowserFragmentInterface? = null
    private var binding: TvVerticalGridBinding? = null

    private var mediaModel: MediaBrowserViewModel? = null
    private var backgroundManager: BackgroundManager? = null
    private var selectedMediaItem: MediaLibraryItem? = null
    private var mediaItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var mediaLoading by mutableStateOf(true)
    private var mediaInGrid by mutableStateOf(true)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getLongExtra(MainTvActivity.BROWSER_TYPE, -1)
        val mediaRoute = mediaRoute(type)
        if (mediaRoute != null) {
            setupMediaBrowser(mediaRoute.first, mediaRoute.second)
        } else {
            setupFragmentBrowser(savedInstanceState, type)
        }
    }

    override fun onStart() {
        super.onStart()
        backgroundManager?.let { clearBackground(this, it) }
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
                    VLCTheme(darkTheme = true) {
                        TvMediaBrowserScreen(
                            title = mediaTitle(category),
                            items = mediaItems,
                            loading = mediaLoading,
                            empty = !mediaLoading && mediaItems.isEmpty(),
                            inGrid = mediaInGrid,
                            columns = mediaColumnCount(category),
                            sortActions = mediaSortActions(model),
                            onBack = ::finish,
                            onToggleDisplay = {
                                mediaInGrid = !mediaInGrid
                                Settings.getInstance(this@VerticalGridActivity).putSingle(displayPrefId, mediaInGrid)
                            },
                            onSort = model::sort,
                            onItemFocused = ::onMediaItemFocused,
                            onItemClicked = ::onMediaItemClicked
                        )
                    }
                }
            }
        )
    }

    private fun setupFragmentBrowser(savedInstanceState: Bundle?, type: Long) {
        binding = TvVerticalGridBinding.inflate(LayoutInflater.from(this))
        setContentView(binding!!.root)
        if (savedInstanceState != null) return

        fragment = when (type) {
            HEADER_NETWORK -> {
                var uri = intent.data
                if (uri == null) uri = intent.parcelable(KEY_URI)
                val item = if (uri == null) null else MLServiceLocator.getAbstractMediaWrapper(uri)
                if (item != null && intent.hasExtra(FAVORITE_TITLE)) item.title = intent.getStringExtra(FAVORITE_TITLE)
                FileBrowserTvFragment.newInstance(TYPE_NETWORK, item, item === null)
            }
            HEADER_MOVIES, HEADER_TV_SHOW -> MediaScrapingBrowserTvFragment.newInstance(type)
            HEADER_DIRECTORIES -> FileBrowserTvFragment.newInstance(TYPE_FILE, intent.data?.let { MLServiceLocator.getAbstractMediaWrapper(it) }, true)
            else -> {
                finish()
                return
            }
        }

        if (fragment == null && BuildConfig.BETA) android.util.Log.i(TAG, "Fragment not initialized: $type")
        supportFragmentManager.beginTransaction()
            .add(TvR.id.tv_fragment_placeholder, fragment as Fragment)
            .commit()
    }

    override fun refresh() {
        mediaModel?.refresh() ?: fragment?.refresh()
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

        val fragment = fragment
        if (fragment != null) {
            if (fragment is DetailsFragment && (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_BUTTON_Y || keyCode == KeyEvent.KEYCODE_Y)) {
                fragment.showDetails()
                return true
            }
            try {
                if ((supportFragmentManager.fragments[0] as? OnKeyPressedListener)?.onKeyPressed(keyCode) == true) {
                    return true
                }
            } catch (e: IndexOutOfBoundsException) {
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun showProgress(show: Boolean) {
        lifecycleScope.launch {
            val binding = binding ?: return@launch
            binding.tvFragmentEmpty.visibility = View.GONE
            binding.tvFragmentProgress.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun updateEmptyView(empty: Boolean) {
        lifecycleScope.launch {
            binding?.tvFragmentEmpty?.visibility = if (empty) View.VISIBLE else View.GONE
        }
    }

    private fun onMediaItemFocused(item: MediaLibraryItem) {
        selectedMediaItem = item
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

    private fun mediaTitle(category: Long) = when (category) {
        CATEGORY_SONGS -> getString(VlcR.string.tracks)
        CATEGORY_ALBUMS -> getString(VlcR.string.albums)
        CATEGORY_ARTISTS -> getString(VlcR.string.artists)
        CATEGORY_GENRES -> getString(VlcR.string.genres)
        CATEGORY_PLAYLISTS -> getString(VlcR.string.playlists)
        else -> getString(VlcR.string.video)
    }

    private fun mediaColumnCount(category: Long) = when (category) {
        CATEGORY_VIDEOS -> resources.getInteger(TvR.integer.tv_videos_col_count)
        else -> resources.getInteger(TvR.integer.tv_songs_col_count)
    }

    private fun mediaDisplayPrefId(category: Long) = "display_tv_media_$category"

    private fun mediaSortActions(model: SortableModel) = buildList {
        if (model.canSortByName()) add(MediaSortAction(getString(VlcR.string.sortby_name), Medialibrary.SORT_ALPHA))
        if (model.canSortByFileNameName()) add(MediaSortAction(getString(VlcR.string.sortby_filename), Medialibrary.SORT_FILENAME))
        if (model.canSortByDuration()) add(MediaSortAction(getString(VlcR.string.sortby_length), Medialibrary.SORT_DURATION))
        if (model.canSortByInsertionDate()) add(MediaSortAction(getString(VlcR.string.sortby_date_insertion), Medialibrary.SORT_INSERTIONDATE))
        if (model.canSortByReleaseDate()) add(MediaSortAction(getString(VlcR.string.sortby_date_release), Medialibrary.SORT_RELEASEDATE))
        if (model.canSortByLastModified()) add(MediaSortAction(getString(VlcR.string.sortby_last_modified_date), Medialibrary.SORT_LASTMODIFICATIONDATE))
        if (model.canSortByArtist()) add(MediaSortAction(getString(VlcR.string.sortby_artist_name), Medialibrary.SORT_ARTIST))
        if (model.canSortByAlbum()) add(MediaSortAction(getString(VlcR.string.sortby_album_name), Medialibrary.SORT_ALBUM))
    }

    interface OnKeyPressedListener {
        /**
         * a key has been pressed
         * @param keyCode the pressed key
         * @return true if the event has been intercepted
         */
        fun onKeyPressed(keyCode: Int): Boolean
    }

    companion object {
        private const val TAG = "VerticalGridActivity"
    }
}

@Composable
private fun TvMediaBrowserScreen(
    title: String,
    items: List<MediaLibraryItem>,
    loading: Boolean,
    empty: Boolean,
    inGrid: Boolean,
    columns: Int,
    sortActions: List<MediaSortAction>,
    onBack: () -> Unit,
    onToggleDisplay: () -> Unit,
    onSort: (Int) -> Unit,
    onItemFocused: (MediaLibraryItem) -> Unit,
    onItemClicked: (MediaLibraryItem) -> Unit
) {
    val firstFocusRequester = remember { FocusRequester() }
    var firstFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(items.firstOrNull()?.id) {
        if (!firstFocusRequested && items.isNotEmpty()) {
            firstFocusRequester.requestFocus()
            firstFocusRequested = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF2011422),
                        Color(0xE6011422),
                        Color(0xF2011422)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 56.dp, top = 32.dp, end = 48.dp, bottom = 40.dp)
        ) {
            TvMediaBrowserHeader(
                title = title,
                inGrid = inGrid,
                sortActions = sortActions,
                onBack = onBack,
                onToggleDisplay = onToggleDisplay,
                onSort = onSort
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (empty) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(VlcR.string.empty_directory),
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else if (inGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns.coerceAtLeast(1)),
                    contentPadding = PaddingValues(bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items, key = { index, item -> "${index}-${item.itemType}-${item.id}-${item.title}" }) { index, item ->
                        TvMediaGridCard(
                            item = item,
                            firstFocusRequester = firstFocusRequester.takeIf { index == 0 },
                            onFocused = onItemFocused,
                            onClick = { onItemClicked(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items, key = { index, item -> "${index}-${item.itemType}-${item.id}-${item.title}" }) { index, item ->
                        TvMediaListRow(
                            item = item,
                            firstFocusRequester = firstFocusRequester.takeIf { index == 0 },
                            onFocused = onItemFocused,
                            onClick = { onItemClicked(item) }
                        )
                    }
                }
            }
        }
        if (loading) {
            CircularProgressIndicator(
                color = VLCThemeDefaults.colors.primary,
                trackColor = Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            )
        }
    }
}

@Composable
private fun TvMediaBrowserHeader(
    title: String,
    inGrid: Boolean,
    sortActions: List<MediaSortAction>,
    onBack: () -> Unit,
    onToggleDisplay: () -> Unit,
    onSort: (Int) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(VlcR.drawable.ic_arrow_back),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { sortExpanded = true }) {
                Icon(
                    painter = painterResource(VlcR.drawable.ic_tv_browser_sort),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                sortActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = {
                            sortExpanded = false
                            onSort(action.sort)
                        }
                    )
                }
            }
        }
        IconButton(onClick = onToggleDisplay) {
            Icon(
                painter = painterResource(if (inGrid) VlcR.drawable.ic_tv_browser_list else VlcR.drawable.ic_tv_browser_grid),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TvMediaGridCard(
    item: MediaLibraryItem,
    firstFocusRequester: FocusRequester?,
    onFocused: (MediaLibraryItem) -> Unit,
    onClick: () -> Unit
) {
    TvMediaItemSurface(
        item = item,
        width = 192.dp,
        imageHeight = if (item is MediaWrapper && item.type == MediaWrapper.TYPE_VIDEO) 120.dp else 168.dp,
        firstFocusRequester = firstFocusRequester,
        onFocused = onFocused,
        onClick = onClick
    )
}

@Composable
private fun TvMediaListRow(
    item: MediaLibraryItem,
    firstFocusRequester: FocusRequester?,
    onFocused: (MediaLibraryItem) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)
    val background = if (focused) Color(0xFF34434E) else Color(0xFF1A2C38)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(firstFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .fillMaxWidth()
            .height(92.dp)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused) 3.dp else 1.dp, if (focused) VLCThemeDefaults.colors.primary else Color.White.copy(alpha = 0.08f)),
                shape
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(item)
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        TvMediaImage(item = item, modifier = Modifier.size(width = 116.dp, height = 68.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.mediaSubtitle(),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        item.mediaBadge()?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun TvMediaItemSurface(
    item: MediaLibraryItem,
    width: Dp,
    imageHeight: Dp,
    firstFocusRequester: FocusRequester?,
    onFocused: (MediaLibraryItem) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)
    val background = if (focused) Color(0xFF34434E) else Color(0xFF1A2C38)
    Column(
        modifier = Modifier
            .then(firstFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .width(width)
            .height(imageHeight + 70.dp)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused) 3.dp else 1.dp, if (focused) VLCThemeDefaults.colors.primary else Color.White.copy(alpha = 0.08f)),
                shape
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(item)
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .background(Color.Black.copy(alpha = 0.24f))
        ) {
            TvMediaImage(item = item, modifier = Modifier.fillMaxSize())
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = item.title.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.mediaSubtitle(),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvMediaImage(item: MediaLibraryItem, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setImageResource(getTvIconRes(item))
            imageView.contentDescription = item.title
            loadImage(imageView, item, tv = true, card = true)
        }
    )
}

private fun MediaLibraryItem.mediaSubtitle(): String {
    val media = this as? MediaWrapper
    return when {
        media?.type == MediaWrapper.TYPE_VIDEO && media.time != 0L -> org.videolan.medialibrary.Tools.getProgressText(media)
        else -> description.orEmpty()
    }
}

private fun MediaLibraryItem.mediaBadge(): String? {
    val media = this as? MediaWrapper ?: return null
    return if (media.type == MediaWrapper.TYPE_VIDEO) generateResolutionClass(media.width, media.height) else null
}
