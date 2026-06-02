/*
 * *************************************************************************
 *  HeaderMediaListActivity.kt
 * **************************************************************************
 *  Copyright © 2017 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp as composeDp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.medialibrary.Tools
import org.videolan.resources.AndroidDevices
import org.videolan.resources.MEDIALIBRARY_PAGE_SIZE
import org.videolan.resources.TAG_ITEM
import org.videolan.resources.UPDATE_REORDER
import org.videolan.resources.util.parcelable
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.Settings
import org.videolan.tools.copy
import org.videolan.tools.dp
import org.videolan.tools.isStarted
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.audio.AudioAlbumTracksAdapter
import org.videolan.vlc.gui.audio.AudioBrowserAdapter
import org.videolan.vlc.gui.dialogs.CURRENT_SORT
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.DEFAULT_ACTIONS
import org.videolan.vlc.gui.dialogs.SHOW_TRACK_NUMBER
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showDisplaySettingsComposeDialog
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.helpers.AudioUtil
import org.videolan.vlc.gui.helpers.AudioUtil.setRingtone
import org.videolan.vlc.gui.helpers.DefaultPlaybackAction
import org.videolan.vlc.gui.helpers.DefaultPlaybackActionMediaType
import org.videolan.vlc.gui.helpers.ExpandStateAppBarLayoutBehavior
import org.videolan.vlc.gui.helpers.SwipeDragItemTouchHelperCallback
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.getResourceFromAttribute
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
// WAVE 1 section header host (compose-2l4.1.4 / compose-95d): this activity also
// adds the Decoration (the true interop site for VLCSectionHeader). See the
// Decoration .kt files for full docs + rollback. (Phone audio/playlist paths prioritized.)
import org.videolan.vlc.gui.view.RecyclerSectionItemDecoration
import org.videolan.vlc.gui.view.FastScroller
import org.videolan.vlc.gui.view.SwipeRefreshLayout
import org.videolan.vlc.interfaces.Filterable
import org.videolan.vlc.interfaces.IEventsHandler
import org.videolan.vlc.interfaces.IListEventsHandler
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_SHORTCUT
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_COPY
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_FAV_ADD
import org.videolan.vlc.util.ContextOption.CTX_FAV_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ALBUM_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_INFORMATION
import org.videolan.vlc.util.ContextOption.CTX_PLAY_ALL
import org.videolan.vlc.util.ContextOption.CTX_PLAY_NEXT
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.ContextOption.CTX_SET_RINGTONE
import org.videolan.vlc.util.ContextOption.CTX_SHARE
import org.videolan.vlc.util.ContextOption.Companion.createCtxPlaylistItemFlags
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.getScreenWidth
import org.videolan.vlc.util.isSchemeHttpOrHttps
import org.videolan.vlc.util.launchWhenStarted
import org.videolan.vlc.util.setLayoutMarginTop
import org.videolan.vlc.util.share
import org.videolan.vlc.viewmodels.PlaylistModel
import org.videolan.vlc.viewmodels.mobile.PlaylistViewModel
import org.videolan.vlc.viewmodels.mobile.getViewModel
import java.security.SecureRandom
import kotlin.math.min

open class HeaderMediaListActivity : AudioPlayerContainerActivity(), IEventsHandler<MediaLibraryItem>, IListEventsHandler, ActionMode.Callback, View.OnClickListener, CtxActionReceiver, Filterable, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

    private var lastDismissedPosition: Int = -1
    private lateinit var searchView: SearchView
    private lateinit var itemTouchHelperCallback: SwipeDragItemTouchHelperCallback
    private lateinit var audioBrowserAdapter: AudioBrowserAdapter
    private val mediaLibrary = Medialibrary.getInstance()
    private lateinit var coordinator: CoordinatorLayout
    private lateinit var songs: RecyclerView
    private lateinit var browserFastScroller: FastScroller
    private lateinit var headerComposeView: ComposeView
    private var headerState by mutableStateOf(HeaderMediaListUiState())
    private var actionMode: ActionMode? = null
    private var isPlaylist: Boolean = false
    private lateinit var viewModel: PlaylistViewModel
    private var itemTouchHelper: ItemTouchHelper? = null
    override fun isTransparent() = true
    override var isEdgeToEdge = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        super.onCreate(savedInstanceState)

        setContentView(createHeaderMediaListShell())
        initAudioPlayerContainerActivity()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = bars.bottom,
            )

            setLayoutMarginTop(toolbar, bars.top)
            WindowInsetsCompat.CONSUMED
        }

        contentContainer = songs
        originalBottomPadding = contentContainer.paddingBottom
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val playlist = if (savedInstanceState != null)
            savedInstanceState.parcelable<Parcelable>(TAG_ITEM) as MediaLibraryItem?
        else
            intent.parcelable<Parcelable>(TAG_ITEM) as MediaLibraryItem?
        if (playlist == null) {
            finish()
            return
        }
        isPlaylist = playlist.itemType == MediaLibraryItem.TYPE_PLAYLIST
        headerState = headerState.copy(
            item = playlist,
            isPlaylist = isPlaylist,
            showReleaseYear = !isPlaylist,
            favorite = playlist.isFavorite
        )
        viewModel = getViewModel(playlist)
        viewModel.tracksProvider.pagedList.observe(this) { tracks ->
            @Suppress("UNCHECKED_CAST")
            (tracks as? PagedList<MediaLibraryItem>)?.let { audioBrowserAdapter.submitList(it) }
            menu.let { UiTools.updateSortTitles(it, viewModel.tracksProvider) }
            if (::itemTouchHelperCallback.isInitialized) itemTouchHelperCallback.swipeEnabled = true
        }

        viewModel.playlistLiveData.observe(this) { playlist ->
            var nextState = headerState.copy(
                favorite = playlist?.isFavorite == true,
                totalDuration = playlist?.tracks?.sumOf { it.length } ?: 0
            )

            if (playlist is Album) {
                val releaseYear = playlist.releaseYear
                nextState = nextState.copy(
                    releaseYear = if (releaseYear > 0) releaseYear.toString() else "",
                    showReleaseYear = releaseYear > 0
                )
            }
            headerState = nextState
        }

        viewModel.tracksProvider.liveHeaders.observe(this) {
            songs.invalidateItemDecorations()
        }

        if (isPlaylist) {
            audioBrowserAdapter = AudioBrowserAdapter(MediaLibraryItem.TYPE_MEDIA, this, this, isPlaylist)
            itemTouchHelperCallback = SwipeDragItemTouchHelperCallback(audioBrowserAdapter, lockedInSafeMode = Settings.safeMode)
            itemTouchHelperCallback.swipeAttemptListener = {
                lifecycleScope.launch { showPinIfNeeded() }
            }
            itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
            itemTouchHelper!!.attachToRecyclerView(songs)
            headerState = headerState.copy(showReleaseYear = false)
        } else {
            audioBrowserAdapter = AudioAlbumTracksAdapter(MediaLibraryItem.TYPE_MEDIA, this, this)
            songs.addItemDecoration(RecyclerSectionItemDecoration(resources.getDimensionPixelSize(R.dimen.recycler_section_header_height), true, viewModel.tracksProvider))

        }
        songs.layoutManager = LinearLayoutManager(this)
        songs.adapter = audioBrowserAdapter

        val context = this
        lifecycleScope.launch {
            var showBackground = true
            val cover = withContext(Dispatchers.IO) {
                val width = context.getScreenWidth()
                if (!playlist.artworkMrl.isNullOrEmpty()) {
                    AudioUtil.fetchCoverBitmap(Uri.decode(playlist.artworkMrl), width)
                } else if (playlist is Album) {
                    showBackground = false
                    UiTools.getDefaultAlbumDrawableBig(this@HeaderMediaListActivity).bitmap
                } else {
                    ThumbnailsProvider.getPlaylistOrGenreImage("playlist:${playlist.id}_$width", playlist.tracks.toList(), width)
                }
            }
            if (cover != null) {
                headerState = headerState.copy(cover = cover, showBackground = showBackground)
                appBarLayout.setExpanded(true, true)
            }
        }

        audioBrowserAdapter.areSectionsEnabled = false
        browserFastScroller.attachToCoordinator(appBarLayout, coordinator, null)
        browserFastScroller.setRecyclerView(songs, viewModel.tracksProvider)
    }

    private fun createHeaderMediaListShell(): View {
        coordinator = CoordinatorLayout(this).apply {
            id = R.id.coordinator
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        coordinator.addView(createHeaderAppBar())
        coordinator.addView(createTrackListContainer(), CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
        })
        coordinator.addView(createAudioPlayerContainer())
        coordinator.addView(
            createAudioPlayerTipsComposeHost(),
            CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT)
        )
        return coordinator
    }

    private fun createHeaderAppBar(): AppBarLayout {
        val appBar = AppBarLayout(this).apply {
            id = R.id.appbar
            setBackgroundResource(getResourceFromAttribute(this@HeaderMediaListActivity, R.attr.background_default))
        }
        val collapsingToolbar = CollapsingToolbarLayout(ContextThemeWrapper(this, R.style.Toolbar_VLC)).apply {
            isTitleEnabled = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        headerComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    HeaderMediaListHeader(
                        state = headerState,
                        strings = HeaderMediaListStrings(
                            play = getString(R.string.play),
                            shuffle = getString(R.string.shuffle_all_title),
                            addToPlaylist = getString(R.string.add_to_playlist),
                            favorite = getString(if (headerState.favorite) R.string.favorites_remove else R.string.favorites_add)
                        ),
                        onPlay = ::playCurrentList,
                        onShuffle = ::shuffleCurrentList,
                        onAddToPlaylist = ::addCurrentListToPlaylist,
                        onFavorite = ::toggleFavorite,
                        onArtistClick = ::openAlbumArtist
                    )
                }
            }
        }
        collapsingToolbar.addView(
            headerComposeView,
            CollapsingToolbarLayout.LayoutParams(defaultContentWidth(), CollapsingToolbarLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
            }
        )

        val shellToolbar = MaterialToolbar(ContextThemeWrapper(this, R.style.Toolbar_VLC)).apply {
            id = R.id.main_toolbar
            navigationContentDescription = getString(R.string.abc_action_bar_up_description)
            navigationIcon = ContextCompat.getDrawable(context, getResourceFromAttribute(context, androidx.appcompat.R.attr.homeAsUpIndicator))
            popupTheme = getResourceFromAttribute(context, R.attr.toolbar_popup_style)
            titleMarginStart = resources.getDimensionPixelSize(R.dimen.default_margin)
        }
        collapsingToolbar.addView(
            shellToolbar,
            CollapsingToolbarLayout.LayoutParams(CollapsingToolbarLayout.LayoutParams.MATCH_PARENT, actionBarSize()).apply {
                collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PIN
            }
        )

        appBar.addView(
            collapsingToolbar,
            AppBarLayout.LayoutParams(AppBarLayout.LayoutParams.MATCH_PARENT, AppBarLayout.LayoutParams.WRAP_CONTENT).apply {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            }
        )
        appBar.layoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT).apply {
            behavior = ExpandStateAppBarLayoutBehavior(this@HeaderMediaListActivity, null)
        }
        return appBar
    }

    private fun createTrackListContainer(): View {
        val swipeLayout = SwipeRefreshLayout(this).apply {
            isEnabled = false
        }
        val content = FrameLayout(this)
        songs = RecyclerView(this).apply {
            id = R.id.songs
            setBackgroundResource(getResourceFromAttribute(this@HeaderMediaListActivity, R.attr.background_default))
            clipToPadding = false
            isVerticalScrollBarEnabled = true
            setPadding(0, 0, 0, 64.dp)
        }
        content.addView(
            songs,
            FrameLayout.LayoutParams(defaultContentWidth(), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
        )
        browserFastScroller = FastScroller(this).apply {
            id = R.id.browser_fast_scroller
        }
        content.addView(
            browserFastScroller,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END)
        )
        swipeLayout.addView(
            content,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        return swipeLayout
    }

    private fun createAudioPlayerContainer() = FrameLayout(this).apply {
        id = R.id.audio_player_container
        elevation = resources.getDimension(R.dimen.audio_player_elevation)
        visibility = View.GONE
        addView(
            context.createAudioPlayerHostView(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        addView(
            context.createAudioPlaylistTipsComposeHost(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        layoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT).apply {
            behavior = org.videolan.vlc.gui.helpers.PlayerBehavior<FrameLayout>()
        }
    }

    private fun defaultContentWidth(): Int {
        val width = resources.getDimensionPixelSize(R.dimen.default_content_width)
        return if (width > 0) width else ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun actionBarSize(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
        return if (typedValue.resourceId != 0) resources.getDimensionPixelSize(typedValue.resourceId)
        else TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    }

    private fun playCurrentList() {
        if (::viewModel.isInitialized) MediaUtils.playTracks(this, viewModel.tracksProvider, 0)
    }

    private fun shuffleCurrentList() {
        viewModel.playlist?.let { playlist ->
            val trackCount = min(playlist.tracksCount, MEDIALIBRARY_PAGE_SIZE)
            if (trackCount > 0) MediaUtils.playTracks(this, playlist, SecureRandom().nextInt(trackCount), true)
        }
    }

    private fun addCurrentListToPlaylist() {
        viewModel.playlist?.let { addToPlaylist(it.tracks.toList()) }
    }

    private fun toggleFavorite() {
        lifecycleScope.launch {
            viewModel.toggleFavorite()
        }
    }

    private fun openAlbumArtist() {
        if (viewModel.playlist !is Album) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val artist = (viewModel.playlist as Album).retrieveAlbumArtist()
                val i = Intent(this@HeaderMediaListActivity, SecondaryActivity::class.java)
                i.putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.ALBUMS_SONGS)
                i.putExtra(TAG_ITEM, artist)
                i.putExtra(ARTIST_FROM_ALBUM, true)
                i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
                startActivity(i)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val playlistModel = PlaylistModel.get(this)
        PlaylistManager.currentPlayedMedia.observe(this) {
            audioBrowserAdapter.currentMedia = it
        }
        playlistModel.dataset.asFlow().conflate().onEach {
            audioBrowserAdapter.setCurrentlyPlaying(playlistModel.playing)
            delay(50L)
        }.launchWhenStarted(lifecycleScope)
        audioBrowserAdapter.setModel(playlistModel)
    }

    override fun onPause() {
        super.onPause()
        audioBrowserAdapter.setCurrentlyPlaying(false)
    }

    override fun onStop() {
        super.onStop()
        stopActionMode()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        viewModel.playlist?.let {
            outState.putParcelable(TAG_ITEM, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.playlist_option, menu)
        if (!isPlaylist) {
            menu.findItem(R.id.ml_menu_display_options).isVisible = true
        }
        val searchItem = menu.findItem(R.id.ml_menu_filter)
        searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_in_list_hint)
        searchView.setOnQueryTextListener(this)
        val query = getFilterQuery()
        if (!query.isNullOrEmpty()) {
            searchView.post {
                searchItem.expandActionView()
                searchView.clearFocus()
                UiTools.setKeyboardVisibility(searchView, false)
                searchView.setQuery(query, false)
            }
        }
        searchItem.setOnActionExpandListener(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ml_menu_display_options -> {
                //filter all sorts and keep only applicable ones
                val sorts = arrayListOf(Medialibrary.TrackId, Medialibrary.SORT_ALPHA, Medialibrary.SORT_FILENAME, Medialibrary.SORT_ARTIST, Medialibrary.SORT_ALBUM, Medialibrary.SORT_DURATION, Medialibrary.SORT_RELEASEDATE, Medialibrary.SORT_LASTMODIFICATIONDATE, Medialibrary.SORT_FILESIZE, Medialibrary.NbMedia).filter {
                    viewModel.canSortBy(it)
                }
                //Open the display settings Bottom sheet
                showDisplaySettingsComposeDialog(
                    displayInCards = null,
                    onlyFavs = null,
                    sorts = sorts,
                    showTrackNumber = Settings.showTrackNumber,
                    currentSort = viewModel.tracksProvider.sort,
                    currentSortDesc = viewModel.tracksProvider.desc,
                    defaultPlaybackActions = DefaultPlaybackActionMediaType.TRACK.getDefaultPlaybackActions(Settings.getInstance(this)),
                    defaultActionType = getString(DefaultPlaybackActionMediaType.TRACK.title)
                )
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onDisplaySettingChanged(key: String, value: Any) {
        when (key) {
            CURRENT_SORT -> {
                @Suppress("UNCHECKED_CAST") val sort = value as Pair<Int, Boolean>
                viewModel.desc = sort.second
                viewModel.sort(sort.first)
            }
            SHOW_TRACK_NUMBER -> {
                val checked = value as Boolean
                Settings.getInstance(this).putSingle(ALBUMS_SHOW_TRACK_NUMBER, checked)
                Settings.showTrackNumber = checked
                audioBrowserAdapter.notifyDataSetChanged()
                viewModel.refresh()
            }
            DEFAULT_ACTIONS -> {
                Settings.getInstance(this).putSingle(DefaultPlaybackActionMediaType.TRACK.defaultActionKey, (value as DefaultPlaybackAction).name)
            }
        }
    }

    override fun onClick(v: View, position: Int, item: MediaLibraryItem) {
        if (actionMode != null) {
            audioBrowserAdapter.multiSelectHelper.toggleSelection(position)
            invalidateActionMode()
        } else {
            if (searchView.visibility == View.VISIBLE) UiTools.setKeyboardVisibility(v, false)
            if (isPlaylist)
                MediaUtils.playTracks(this, viewModel.tracksProvider, position)
            else
                when(DefaultPlaybackActionMediaType.TRACK.getCurrentPlaybackAction(Settings.getInstance(this))) {
                    DefaultPlaybackAction.PLAY -> MediaUtils.openList(this, listOf(*item.tracks), 0)
                    DefaultPlaybackAction.ADD_TO_QUEUE -> MediaUtils.appendMedia(this, listOf(*item.tracks))
                    DefaultPlaybackAction.INSERT_NEXT -> MediaUtils.insertNext(this, listOf(*item.tracks).toTypedArray())
                    DefaultPlaybackAction.PLAY_ALL -> MediaUtils.playTracks(this, viewModel.tracksProvider, position)
                }
        }
    }

    override fun onLongClick(v: View, position: Int, item: MediaLibraryItem): Boolean {
        audioBrowserAdapter.multiSelectHelper.toggleSelection(position, true)
        if (actionMode == null) startActionMode() else invalidateActionMode()
        return true
    }


    override fun onImageClick(v: View, position: Int, item: MediaLibraryItem) {
        if (actionMode != null) {
            onClick(v, position, item)
            return
        }
        onLongClick(v, position, item)
    }

    override fun onCtxClick(v: View, position: Int, item: MediaLibraryItem) {
        if (actionMode == null) {
            (item as? MediaWrapper)?.let { media ->
                val flags = createCtxPlaylistItemFlags().apply {
                    if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
                    if (media.type == MediaWrapper.TYPE_STREAM || (media.type == MediaWrapper.TYPE_ALL && isSchemeHttpOrHttps(media.uri.scheme)))
                        addAll(CTX_COPY, CTX_RENAME)
                    if (media.type == MediaWrapper.TYPE_AUDIO) {
                        add(CTX_GO_TO_ARTIST)
                        if (BuildConfig.DEBUG) Log.d("CtxPrep", "Artist id is: ${media.artistId}, album artist is: ${media.albumArtistId}")
                        if (media.artistId != media.albumArtistId) add(CTX_GO_TO_ALBUM_ARTIST)
                    }
                    else add(CTX_SHARE)
                }
                showContext(this, this, position, media, flags)
            }
        }
    }

    override fun onUpdateFinished(adapter: RecyclerView.Adapter<*>) {}

    override fun onItemFocused(v: View, item: MediaLibraryItem) {}

    override fun onRemove(position: Int, item: MediaLibraryItem) {
        lastDismissedPosition = position
        val tracks = ArrayList(listOf(*item.tracks))
        lifecycleScope.launch {  removeFromPlaylist(tracks, ArrayList(listOf(position))) }
    }

    override fun onMove(oldPosition: Int, newPosition: Int) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Moving item from $oldPosition to $newPosition")
        (viewModel.playlist as Playlist).move(oldPosition, newPosition)

    }

    override fun onMainActionClick(v: View, position: Int, item: MediaLibraryItem) {
        MediaUtils.openList(this, listOf(*item.tracks), 0)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper!!.startDrag(viewHolder)
    }

    private fun startActionMode() {
        actionMode = startSupportActionMode(this)
    }

    private fun stopActionMode() = actionMode?.let {
        it.finish()
        onDestroyActionMode(it)
    }

    private fun invalidateActionMode() {
        if (actionMode != null)
            actionMode!!.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        audioBrowserAdapter.multiSelectHelper.toggleActionMode(true, audioBrowserAdapter.itemCount)
        mode.menuInflater.inflate(R.menu.action_mode_audio_browser, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = audioBrowserAdapter.multiSelectHelper.getSelectionCount()
        if (count == 0) {
            stopActionMode()
            return false
        }
        val isMedia = audioBrowserAdapter.multiSelectHelper.getSelection()[0].itemType == MediaLibraryItem.TYPE_MEDIA
        val isSong = count == 1 && isMedia
        menu.findItem(R.id.action_mode_audio_set_song).isVisible = isSong && AndroidDevices.isPhone && !isPlaylist
        menu.findItem(R.id.action_mode_audio_info).isVisible = isSong
        menu.findItem(R.id.action_mode_audio_append).isVisible = PlaylistManager.hasMedia()
        menu.findItem(R.id.action_mode_audio_delete).isVisible = true
        menu.findItem(R.id.action_mode_audio_share).isVisible = isSong
        menu.findItem(R.id.action_mode_favorite_add).isVisible = audioBrowserAdapter.multiSelectHelper.getSelection().none { it.isFavorite }
        menu.findItem(R.id.action_mode_favorite_remove).isVisible = audioBrowserAdapter.multiSelectHelper.getSelection().none { !it.isFavorite }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (!isStarted()) return false
        val list = audioBrowserAdapter.multiSelectHelper.getSelection()
        val tracks = ArrayList<MediaWrapper>()
        list.forEach { tracks.addAll(listOf(*it.tracks)) }

        val indexes = audioBrowserAdapter.multiSelectHelper.selectionMap

        when (item.itemId) {
            R.id.action_mode_audio_play -> MediaUtils.openList(this, tracks, 0)
            R.id.action_mode_audio_append -> MediaUtils.appendMedia(this, tracks)
            R.id.action_mode_audio_add_playlist -> addToPlaylist(tracks)
            R.id.action_mode_audio_info -> showInfoDialog(list[0] as MediaWrapper)
            R.id.action_mode_audio_share -> lifecycleScope.launch { share(list.map { it as MediaWrapper }) }
            R.id.action_mode_audio_set_song -> setRingtone(list[0] as MediaWrapper)
            R.id.action_mode_audio_delete -> lifecycleScope.launch { if (isPlaylist) removeFromPlaylist(tracks, indexes.toMutableList()) else removeItems(tracks) }
            R.id.action_mode_favorite_add -> lifecycleScope.launch { viewModel.changeFavorite(tracks, true) }
            R.id.action_mode_favorite_remove -> lifecycleScope.launch { viewModel.changeFavorite(tracks, false) }
            else -> return false
        }
        stopActionMode()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        audioBrowserAdapter.multiSelectHelper.toggleActionMode(false, audioBrowserAdapter.itemCount)
        actionMode = null
        audioBrowserAdapter.multiSelectHelper.clearSelection()
    }

    private fun showInfoDialog(media: MediaWrapper) {
        val i = Intent(this, InfoActivity::class.java)
        i.putExtra(TAG_ITEM, media)
        startActivity(i)
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        if (position >= audioBrowserAdapter.itemCount) return
        val media = audioBrowserAdapter.getItem(position) as MediaWrapper? ?: return
        when (option) {
            CTX_INFORMATION -> showInfoDialog(media)
            CTX_DELETE -> lifecycleScope.launch { removeItem(position, media) }
            CTX_APPEND -> MediaUtils.appendMedia(this, media.tracks)
            CTX_PLAY_NEXT -> MediaUtils.insertNext(this, media.tracks)
            CTX_PLAY_ALL -> MediaUtils.playTracks(this, viewModel.tracksProvider, position, false)
            CTX_ADD_TO_PLAYLIST -> addToPlaylist(media.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            CTX_SET_RINGTONE -> setRingtone(media)
            CTX_SHARE -> lifecycleScope.launch { share(media) }
            CTX_RENAME -> {
                showRenameComposeDialog(media) { renamedMedia, name ->
                    lifecycleScope.launch { viewModel.rename(renamedMedia as MediaWrapper, name) }
                }
            }
            CTX_COPY -> {
                copy(media.title, media.location)
                Snackbar.make(window.decorView.findViewById(android.R.id.content), R.string.url_copied_to_clipboard, Snackbar.LENGTH_LONG).show()
            }
            CTX_FAV_ADD, CTX_FAV_REMOVE -> lifecycleScope.launch {
                media.isFavorite = option == CTX_FAV_ADD
                withContext(Dispatchers.Main) { audioBrowserAdapter.notifyItemChanged(position) }
            }
            CTX_ADD_SHORTCUT -> lifecycleScope.launch { createShortcut(media) }
            CTX_GO_TO_ARTIST -> lifecycleScope.launch(Dispatchers.IO) {
                val artist = if (media is Album) media.retrieveAlbumArtist() else (media as MediaWrapper).artist
                val i = Intent(this@HeaderMediaListActivity, SecondaryActivity::class.java)
                i.putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.ALBUMS_SONGS)
                i.putExtra(TAG_ITEM, artist)
                i.putExtra(ARTIST_FROM_ALBUM, true)
                i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
                startActivity(i)
            }
            CTX_GO_TO_ALBUM_ARTIST -> lifecycleScope.launch(Dispatchers.IO) {
                val artist = (media as MediaWrapper).albumArtist
                val i = Intent(this@HeaderMediaListActivity, SecondaryActivity::class.java)
                i.putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.ALBUMS_SONGS)
                i.putExtra(TAG_ITEM, artist)
                i.putExtra(ARTIST_FROM_ALBUM, true)
                i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
                startActivity(i)
            }
            else -> {}
        }

    }

    private suspend fun removeItem(position: Int, media: MediaWrapper) {
        if (isPlaylist) {
            removeFromPlaylist(listOf(media), listOf(position))
        } else {
            removeItems(listOf(media))
        }
    }

    private fun removeItems(items: List<MediaWrapper>) {
        showConfirmDeleteComposeDialog(ArrayList(items)) {
            deleteItems(items)
        }
    }

    private fun deleteItems(items: List<MediaWrapper>) {
        lifecycleScope.launch {
            for (item in items) {
                val deleteAction = kotlinx.coroutines.Runnable {
                    lifecycleScope.launch {
                        MediaUtils.deleteItem(this@HeaderMediaListActivity, item) {
                            UiTools.snacker(this@HeaderMediaListActivity, getString(R.string.msg_delete_failed, it.title))
                        }
                        if (isStarted()) viewModel.refresh()
                    }
                }
                if (Permissions.checkWritePermission(this@HeaderMediaListActivity, item, deleteAction)) deleteAction.run()
            }
        }
    }

    override fun onClick(v: View) {
        playCurrentList()
    }

    private fun removeFromPlaylist(list: List<MediaWrapper>, indexes: List<Int>) {
        if (!showPinIfNeeded()) {
            val itemsRemoved = HashMap<Int, Long>()
            val playlist = viewModel.playlist as? Playlist
                    ?: return

            itemTouchHelperCallback.swipeEnabled = false
            lifecycleScope.launchWhenStarted {
                val tracks = withContext(Dispatchers.IO) { playlist.tracks }
                viewModel.playlist?.let { playlist ->
                    withContext(Dispatchers.IO) {
                        for ((index, playlistIndex) in indexes.sortedBy { it }.withIndex()) {
                            val trueIndex = playlist.tracks.indexOf(list[index])
                            itemsRemoved[trueIndex] = tracks[playlistIndex].id
                            (playlist as Playlist).remove(trueIndex)
                        }
                    }
                }
                var removedMessage = if (indexes.size > 1) getString(R.string.removed_from_playlist_anonymous) else getString(R.string.remove_playlist_item, list.first().title)
                UiTools.snackerWithCancel(this@HeaderMediaListActivity, removedMessage, action = {
                    lastDismissedPosition = -1
                }) {
                    for ((key, value) in itemsRemoved) {
                        playlist.add(value, key)
                        if (lastDismissedPosition != -1) {
                            audioBrowserAdapter.notifyItemChanged(lastDismissedPosition)
                            lastDismissedPosition = -1
                        }
                    }
                }
            }
        } else {
            if (lastDismissedPosition != -1) {
                audioBrowserAdapter.notifyItemChanged(lastDismissedPosition)
                lastDismissedPosition = -1
            }
        }
    }

    companion object {

        const val ARTIST_FROM_ALBUM = "ARTIST_FROM_ALBUM"
        const val TAG = "VLC/PlaylistActivity"
    }

    override fun getFilterQuery() = viewModel.filterQuery

    override fun enableSearchOption() = true

    override fun filter(query: String) = viewModel.filter(query)

    override fun restoreList() = viewModel.restore()

    override fun setSearchVisibility(visible: Boolean) {}

    override fun allowedToExpand() = true

    override fun onQueryTextSubmit(query: String?) = false

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText?.length  == 0)
            restoreList()
        else
            filter(newText ?: "")
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        appBarLayout.setExpanded(false, true)
        audioBrowserAdapter.stopReorder = true
        audioBrowserAdapter.notifyItemRangeChanged(0, audioBrowserAdapter.itemCount, UPDATE_REORDER)
        setAppBarScrollEnabled(false)
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        audioBrowserAdapter.stopReorder = false
        audioBrowserAdapter.notifyItemRangeChanged(0, audioBrowserAdapter.itemCount, UPDATE_REORDER)
        setAppBarScrollEnabled(true)
        return true
    }

    private fun setAppBarScrollEnabled(enabled: Boolean) {
        ((appBarLayout.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? ExpandStateAppBarLayoutBehavior)?.scrollEnabled = enabled
    }
}

private data class HeaderMediaListUiState(
    val item: MediaLibraryItem? = null,
    val isPlaylist: Boolean = false,
    val cover: Bitmap? = null,
    val showBackground: Boolean = false,
    val totalDuration: Long = 0L,
    val releaseYear: String = "",
    val showReleaseYear: Boolean = false,
    val favorite: Boolean = false
)

private data class HeaderMediaListStrings(
    val play: String,
    val shuffle: String,
    val addToPlaylist: String,
    val favorite: String
)

@Composable
private fun HeaderMediaListHeader(
    state: HeaderMediaListUiState,
    strings: HeaderMediaListStrings,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onFavorite: () -> Unit,
    onArtistClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val item = state.item
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(246.composeDp)
            .background(colors.backgroundDefaultDarker)
    ) {
        if (state.showBackground) {
            state.cover?.let { cover ->
                Image(
                    bitmap = cover.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.20f),
                                    colors.backgroundDefault.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.composeDp, end = 16.composeDp, bottom = 16.composeDp, top = 86.composeDp),
            verticalAlignment = Alignment.Top
        ) {
            HeaderCover(
                cover = state.cover,
                fallback = if (state.isPlaylist) R.drawable.ic_song_big else R.drawable.ic_album,
                modifier = Modifier.size(128.composeDp)
            )
            Spacer(modifier = Modifier.width(16.composeDp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item?.title.orEmpty(),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics {
                        contentDescription = item?.title.orEmpty()
                    }
                )
                val description = item?.description.orEmpty()
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 6.composeDp)
                            .clickable(enabled = item is Album, onClick = onArtistClick)
                    )
                }
                if (state.showReleaseYear && state.releaseYear.isNotEmpty()) {
                    Text(
                        text = state.releaseYear,
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.composeDp)
                    )
                }
                if (state.totalDuration > 0) {
                    Text(
                        text = Tools.millisToTextLarge(state.totalDuration),
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.composeDp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.composeDp),
                    modifier = Modifier.padding(top = 10.composeDp)
                ) {
                    Button(onClick = onPlay) {
                        Text(strings.play)
                    }
                    HeaderMediaIconButton(
                        icon = R.drawable.ic_album_shuffle,
                        contentDescription = strings.shuffle,
                        onClick = onShuffle
                    )
                    HeaderMediaIconButton(
                        icon = R.drawable.ic_album_addtoplaylist,
                        contentDescription = strings.addToPlaylist,
                        onClick = onAddToPlaylist
                    )
                    HeaderMediaIconButton(
                        icon = if (state.favorite) R.drawable.ic_header_media_favorite else R.drawable.ic_header_media_favorite_outline,
                        contentDescription = strings.favorite,
                        onClick = onFavorite,
                        tint = if (state.favorite) colors.primary else colors.fontDefault
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCover(cover: Bitmap?, @DrawableRes fallback: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.composeDp))
            .background(VLCThemeDefaults.colors.backgroundDefault),
        contentAlignment = Alignment.Center
    ) {
        if (cover != null) {
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                painter = painterResource(fallback),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.fontLight,
                modifier = Modifier.size(64.composeDp)
            )
        }
    }
}

@Composable
private fun HeaderMediaIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = VLCThemeDefaults.colors.fontDefault
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.composeDp)
            .clip(RoundedCornerShape(20.composeDp))
            .background(VLCThemeDefaults.colors.subtleSelection)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.composeDp)
        )
    }
}
