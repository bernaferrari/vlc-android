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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import org.videolan.resources.util.parcelable
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.Settings
import org.videolan.tools.copy
import org.videolan.tools.dp
import org.videolan.tools.isStarted
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
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
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.getResourceFromAttribute
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.view.SwipeRefreshLayout
import org.videolan.vlc.interfaces.Filterable
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
import org.videolan.vlc.util.isOTG
import org.videolan.vlc.util.isSD
import org.videolan.vlc.util.isSchemeHttpOrHttps
import org.videolan.vlc.util.isSchemeSMB
import org.videolan.vlc.util.launchWhenStarted
import org.videolan.vlc.util.setLayoutMarginTop
import org.videolan.vlc.util.share
import org.videolan.vlc.viewmodels.PlaylistModel
import org.videolan.vlc.viewmodels.mobile.PlaylistViewModel
import org.videolan.vlc.viewmodels.mobile.getViewModel
import java.security.SecureRandom
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class HeaderMediaListActivity : AudioPlayerContainerActivity(), ActionMode.Callback, View.OnClickListener, CtxActionReceiver, Filterable, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

    private lateinit var searchView: SearchView
    private val mediaLibrary = Medialibrary.getInstance()
    private lateinit var coordinator: CoordinatorLayout
    private lateinit var trackListComposeView: ComposeView
    private lateinit var headerComposeView: ComposeView
    private var headerState by mutableStateOf(HeaderMediaListUiState())
    private var trackItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private val selectedTrackPositions = mutableStateListOf<Int>()
    private var trackUiVersion by mutableStateOf(0)
    private var currentMedia by mutableStateOf<MediaWrapper?>(null)
    private var playlistPlaying by mutableStateOf(false)
    private var inSelectionMode by mutableStateOf(false)
    private var forceNoTrackNumbers by mutableStateOf(false)
    private var actionMode: ActionMode? = null
    private var isPlaylist: Boolean = false
    private lateinit var viewModel: PlaylistViewModel
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

        contentContainer = trackListComposeView
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
            val pagedTracks = tracks as? PagedList<MediaLibraryItem>
            trackItems = pagedTracks?.toList().orEmpty()
            forceNoTrackNumbers = !isPlaylist && pagedTracks?.any { ((it as? MediaWrapper)?.trackNumber ?: 0) > 0 } == false
            selectedTrackPositions.removeAll { it !in trackItems.indices }
            menu.let { UiTools.updateSortTitles(it, viewModel.tracksProvider) }
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

        if (isPlaylist) {
            headerState = headerState.copy(showReleaseYear = false)
        }

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
        trackListComposeView = ComposeView(this).apply {
            id = R.id.songs
            setBackgroundResource(getResourceFromAttribute(this@HeaderMediaListActivity, R.attr.background_default))
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    HeaderMediaTrackList(
                        tracks = trackItems,
                        isPlaylist = isPlaylist,
                        selectedPositions = selectedTrackPositions,
                        inSelectionMode = inSelectionMode,
                        currentMedia = currentMedia,
                        playing = playlistPlaying,
                        forceNoTrackNumbers = forceNoTrackNumbers,
                        uiVersion = trackUiVersion,
                        onClick = ::onTrackClick,
                        onLongClick = ::onTrackLongClick,
                        onImageClick = ::onTrackImageClick,
                        onMoreClick = ::onTrackMoreClick,
                        onMainActionClick = ::onTrackMainActionClick,
                        onMoveFinished = ::commitPlaylistTrackMove
                    )
                }
            }
        }
        content.addView(
            trackListComposeView,
            FrameLayout.LayoutParams(defaultContentWidth(), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
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
            currentMedia = it
        }
        playlistModel.dataset.asFlow().conflate().onEach {
            playlistPlaying = playlistModel.playing
            delay(50L)
        }.launchWhenStarted(lifecycleScope)
    }

    override fun onPause() {
        super.onPause()
        playlistPlaying = false
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
                trackUiVersion++
                viewModel.refresh()
            }
            DEFAULT_ACTIONS -> {
                Settings.getInstance(this).putSingle(DefaultPlaybackActionMediaType.TRACK.defaultActionKey, (value as DefaultPlaybackAction).name)
            }
        }
    }

    private fun onTrackClick(position: Int, item: MediaLibraryItem) {
        if (actionMode != null) {
            toggleTrackSelection(position)
            invalidateActionMode()
        } else {
            if (::searchView.isInitialized && searchView.visibility == View.VISIBLE) UiTools.setKeyboardVisibility(trackListComposeView, false)
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

    private fun onTrackLongClick(position: Int, item: MediaLibraryItem) {
        toggleTrackSelection(position, true)
        if (actionMode == null) startActionMode() else invalidateActionMode()
    }


    private fun onTrackImageClick(position: Int, item: MediaLibraryItem) {
        if (actionMode != null) {
            onTrackClick(position, item)
            return
        }
        onTrackLongClick(position, item)
    }

    private fun onTrackMoreClick(position: Int, item: MediaLibraryItem) {
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

    private fun onTrackMainActionClick(position: Int, item: MediaLibraryItem) {
        MediaUtils.openList(this, listOf(*item.tracks), 0)
    }

    private fun movePlaylistTrack(from: Int, to: Int) {
        if (!isPlaylist || actionMode != null || from == to || from !in trackItems.indices || to !in trackItems.indices) return
        trackItems = trackItems.toMutableList().apply {
            add(to, removeAt(from))
        }
        selectedTrackPositions.clear()
    }

    private fun commitPlaylistTrackMove(from: Int, to: Int) {
        if (!isPlaylist || from == to || from !in trackItems.indices || to !in trackItems.indices) return
        val playlist = viewModel.playlist as? Playlist ?: return
        if (showPinIfNeeded()) {
            viewModel.refresh()
            return
        }
        movePlaylistTrack(from, to)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                playlist.move(from, to)
            }
            viewModel.refresh()
        }
    }

    private fun toggleTrackSelection(position: Int, forceSelected: Boolean = false) {
        if (position !in trackItems.indices) return
        val selected = selectedTrackPositions.contains(position)
        when {
            selected && !forceSelected -> selectedTrackPositions.remove(position)
            !selected -> selectedTrackPositions.add(position)
        }
    }

    private fun selectedTrackItems(): List<MediaLibraryItem> {
        return selectedTrackPositions.sorted().mapNotNull { trackItems.getOrNull(it) }
    }

    private fun selectedTrackIndexes(): List<Int> = selectedTrackPositions.sorted()

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
        inSelectionMode = true
        mode.menuInflater.inflate(R.menu.action_mode_audio_browser, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val selection = selectedTrackItems()
        val count = selection.size
        if (count == 0) {
            stopActionMode()
            return false
        }
        val isMedia = selection[0].itemType == MediaLibraryItem.TYPE_MEDIA
        val isSong = count == 1 && isMedia
        menu.findItem(R.id.action_mode_audio_set_song).isVisible = isSong && AndroidDevices.isPhone && !isPlaylist
        menu.findItem(R.id.action_mode_audio_info).isVisible = isSong
        menu.findItem(R.id.action_mode_audio_append).isVisible = PlaylistManager.hasMedia()
        menu.findItem(R.id.action_mode_audio_delete).isVisible = true
        menu.findItem(R.id.action_mode_audio_share).isVisible = isSong
        menu.findItem(R.id.action_mode_favorite_add).isVisible = selection.none { it.isFavorite }
        menu.findItem(R.id.action_mode_favorite_remove).isVisible = selection.none { !it.isFavorite }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (!isStarted()) return false
        val list = selectedTrackItems()
        val tracks = ArrayList<MediaWrapper>()
        list.forEach { tracks.addAll(listOf(*it.tracks)) }

        val indexes = selectedTrackIndexes()

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
        inSelectionMode = false
        actionMode = null
        selectedTrackPositions.clear()
    }

    private fun showInfoDialog(media: MediaWrapper) {
        val i = Intent(this, InfoActivity::class.java)
        i.putExtra(TAG_ITEM, media)
        startActivity(i)
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        if (position !in trackItems.indices) return
        val media = trackItems[position] as MediaWrapper? ?: return
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
                withContext(Dispatchers.Main) { trackUiVersion++ }
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
                UiTools.snackerWithCancel(this@HeaderMediaListActivity, removedMessage, action = {}) {
                    for ((key, value) in itemsRemoved) {
                        playlist.add(value, key)
                    }
                    trackUiVersion++
                }
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
        setAppBarScrollEnabled(false)
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
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
private fun HeaderMediaTrackList(
    tracks: List<MediaLibraryItem>,
    isPlaylist: Boolean,
    selectedPositions: List<Int>,
    inSelectionMode: Boolean,
    currentMedia: MediaWrapper?,
    playing: Boolean,
    forceNoTrackNumbers: Boolean,
    uiVersion: Int,
    onClick: (Int, MediaLibraryItem) -> Unit,
    onLongClick: (Int, MediaLibraryItem) -> Unit,
    onImageClick: (Int, MediaLibraryItem) -> Unit,
    onMoreClick: (Int, MediaLibraryItem) -> Unit,
    onMainActionClick: (Int, MediaLibraryItem) -> Unit,
    onMoveFinished: (Int, Int) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val listState = rememberLazyListState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDefault)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.composeDp)
        ) {
            itemsIndexed(
                items = tracks,
                key = { index, item -> "${item.itemType}-${item.id}-$index-$uiVersion" }
            ) { position, item ->
                val media = item as? MediaWrapper
                val selected = selectedPositions.contains(position)
                val isCurrent = currentMedia == item
                val isPresent = media?.isPresent ?: true
                if (!isPlaylist && media != null) {
                    HeaderAlbumTrackRow(
                        media = media,
                        position = position,
                        selected = selected,
                        inSelectionMode = inSelectionMode,
                        isCurrent = isCurrent,
                        playing = playing,
                        forceNoTrackNumbers = forceNoTrackNumbers,
                        showSelectedIcon = selected,
                        onClick = { onClick(position, item) },
                        onLongClick = { onLongClick(position, item) },
                        onMoreClick = { onMoreClick(position, item) }
                    )
                } else {
                    HeaderMediaTrackRow(
                        item = item,
                        position = position,
                        selected = selected,
                        inSelectionMode = inSelectionMode,
                        isPlaylist = isPlaylist,
                        itemCount = tracks.size,
                        listState = listState,
                        isCurrent = isCurrent,
                        playing = playing,
                        isPresent = isPresent,
                        onClick = { onClick(position, item) },
                        onLongClick = { onLongClick(position, item) },
                        onImageClick = { onImageClick(position, item) },
                        onMoreClick = { onMoreClick(position, item) },
                        onMainActionClick = { onMainActionClick(position, item) },
                        onMoveFinished = onMoveFinished
                    )
                }
            }
        }
        HeaderMediaScrollThumb(
            listState = listState,
            itemCount = tracks.size,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun HeaderMediaScrollThumb(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount <= 20) return
    val scope = rememberCoroutineScope()
    val colors = VLCThemeDefaults.colors
    var trackHeight by remember { mutableIntStateOf(0) }
    val visibleItems = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val thumbFraction = (visibleItems.toFloat() / itemCount.toFloat()).coerceIn(0.08f, 1f)
    val scrollFraction = if (itemCount <= 1) {
        0f
    } else {
        (listState.firstVisibleItemIndex.toFloat() / (itemCount - 1).toFloat()).coerceIn(0f, 1f)
    }

    BoxWithConstraints(
        modifier = modifier
            .width(24.composeDp)
            .fillMaxSize()
            .onSizeChanged { trackHeight = it.height }
            .pointerInput(itemCount, trackHeight) {
                detectDragGestures { change, _ ->
                    change.consume()
                    if (trackHeight <= 0) return@detectDragGestures
                    val fraction = (change.position.y / trackHeight.toFloat()).coerceIn(0f, 1f)
                    val target = ((itemCount - 1) * fraction).roundToInt().coerceIn(0, itemCount - 1)
                    scope.launch { listState.scrollToItem(target) }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        val maxHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val thumbHeight = max(36f, maxHeight * thumbFraction)
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        val thumbHeightDp = with(LocalDensity.current) { thumbHeight.toDp() }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = thumbOffset
                }
                .width(4.composeDp)
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(2.composeDp))
                .background(colors.listSubtitle.copy(alpha = 0.42f))
        )
    }
}

@Composable
private fun HeaderPlaylistDragHandle(
    position: Int,
    itemCount: Int,
    listState: LazyListState,
    onMoveFinished: (Int, Int) -> Unit
) {
    var originalPosition by remember(position) { mutableIntStateOf(position) }
    var dragOffset by remember(position) { mutableStateOf(0f) }
    Icon(
        painter = painterResource(R.drawable.ic_move_media),
        contentDescription = stringResource(R.string.move_up),
        tint = VLCThemeDefaults.colors.listSubtitle,
        modifier = Modifier
            .graphicsLayer {
                translationY = dragOffset
            }
            .pointerInput(position, listState) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        originalPosition = position
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        val rowHeight = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == originalPosition }
                            ?.size
                            ?.toFloat()
                            ?.coerceAtLeast(1f)
                            ?: 64f
                        val target = (originalPosition + (dragOffset / rowHeight).roundToInt()).coerceIn(0, itemCount - 1)
                        onMoveFinished(originalPosition, target)
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                    }
                )
            }
    )
}

@Composable
private fun HeaderMediaTrackRow(
    item: MediaLibraryItem,
    position: Int,
    selected: Boolean,
    inSelectionMode: Boolean,
    isPlaylist: Boolean,
    itemCount: Int,
    listState: LazyListState,
    isCurrent: Boolean,
    playing: Boolean,
    isPresent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onImageClick: () -> Unit,
    onMoreClick: () -> Unit,
    onMainActionClick: () -> Unit,
    onMoveFinished: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val colors = VLCThemeDefaults.colors
    VLCBrowserItemRow(
        title = item.title.orEmpty(),
        subtitle = item.description?.toString(),
        selected = selected,
        contentDescription = (item as? MediaWrapper)?.let { TalkbackUtil.getAudioTrack(context, it) },
        onClick = onClick,
        onLongClick = onLongClick,
        artworkContent = {
            HeaderTrackArtwork(
                selected = selected,
                isCurrent = isCurrent,
                playing = playing,
                enabled = isPresent,
                onImageClick = onImageClick
            )
        },
        badgeContent = {
            HeaderAudioBadges(item = item, isPresent = isPresent)
        },
        primaryActionContent = if (isPlaylist && !inSelectionMode) {
            {
                HeaderPlaylistDragHandle(
                    position = position,
                    itemCount = itemCount,
                    listState = listState,
                    onMoveFinished = onMoveFinished
                )
            }
        } else if (isPresent && !inSelectionMode) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    tint = colors.primary
                )
            }
        } else null,
        onPrimaryActionClick = onMainActionClick,
        moreActionContent = if (isPresent && !inSelectionMode) {
            { HeaderAudioMoreIcon() }
        } else null,
        onMoreClick = onMoreClick
    )
}

@Composable
private fun HeaderAlbumTrackRow(
    media: MediaWrapper,
    position: Int,
    selected: Boolean,
    inSelectionMode: Boolean,
    isCurrent: Boolean,
    playing: Boolean,
    forceNoTrackNumbers: Boolean,
    showSelectedIcon: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    val showTrackNumberColumn = isCurrent || (Settings.showTrackNumber && !forceNoTrackNumbers) || showSelectedIcon
    val showTrackNumberText = !isCurrent && Settings.showTrackNumber && !forceNoTrackNumbers
    val trackNumberText = if (showTrackNumberText && media.trackNumber > 0) "${media.trackNumber}." else ""
    val trackNumberContentDescription = media
        .takeIf { showTrackNumberText && it.trackNumber > 0 }
        ?.let { TalkbackUtil.getTrackNumber(context, it) }
    VLCBrowserItemRow(
        title = media.title.orEmpty(),
        subtitle = MediaUtils.getMediaSubtitle(media),
        selected = selected,
        contentDescription = TalkbackUtil.getAudioTrack(context, media),
        titleMaxLines = 1,
        showArtwork = showTrackNumberColumn,
        onClick = onClick,
        onLongClick = onLongClick,
        artworkContent = {
            HeaderAlbumTrackLeadingContent(
                trackNumberText = trackNumberText,
                trackNumberContentDescription = trackNumberContentDescription,
                showTrackNumber = showTrackNumberText,
                selected = selected,
                isCurrent = isCurrent,
                playing = playing
            )
        },
        badgeContent = {
            HeaderAudioBadges(item = media, isPresent = media.isPresent)
        },
        moreActionContent = if (media.isPresent && !inSelectionMode) {
            { HeaderAudioMoreIcon() }
        } else null,
        onMoreClick = onMoreClick
    )
}

@Composable
private fun BoxScope.HeaderTrackArtwork(
    selected: Boolean,
    isCurrent: Boolean,
    playing: Boolean,
    enabled: Boolean,
    onImageClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier
            .size(40.composeDp)
            .clip(RoundedCornerShape(4.composeDp))
            .background(colors.backgroundDefaultDarker)
            .clickable(onClick = onImageClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_song),
            contentDescription = null,
            tint = colors.listSubtitle.copy(alpha = if (enabled) 1f else 0.45f),
            modifier = Modifier.size(24.composeDp)
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_video_grid_check),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.composeDp)
                    .size(18.composeDp)
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.composeDp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(5.composeDp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (playing) R.drawable.ic_pause_player else R.drawable.ic_play_player),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.composeDp)
                )
            }
        }
    }
}

@Composable
private fun HeaderAlbumTrackLeadingContent(
    trackNumberText: String,
    trackNumberContentDescription: String?,
    showTrackNumber: Boolean,
    selected: Boolean,
    isCurrent: Boolean,
    playing: Boolean
) {
    Box(
        modifier = Modifier.size(40.composeDp),
        contentAlignment = Alignment.Center
    ) {
        when {
            selected -> Icon(
                painter = painterResource(R.drawable.ic_video_grid_check),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.composeDp)
            )
            isCurrent -> Icon(
                painter = painterResource(if (playing) R.drawable.ic_pause_player else R.drawable.ic_play_player),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.primary,
                modifier = Modifier.size(24.composeDp)
            )
            showTrackNumber -> Text(
                text = trackNumberText,
                color = VLCThemeDefaults.colors.listTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                modifier = if (trackNumberContentDescription != null) {
                    Modifier.semantics { contentDescription = trackNumberContentDescription }
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
private fun HeaderAudioBadges(item: MediaLibraryItem?, isPresent: Boolean) {
    if (item?.isFavorite == true) {
        HeaderAudioBadgeIcon(R.drawable.ic_emoji_favorite)
    }
    val media = item as? MediaWrapper
    when {
        !isPresent -> HeaderAudioBadgeIcon(R.drawable.ic_emoji_absent)
        media?.uri?.scheme.isSchemeSMB() -> HeaderAudioBadgeIcon(R.drawable.ic_emoji_network)
        media?.uri?.isSD() == true -> HeaderAudioBadgeIcon(R.drawable.ic_emoji_sd)
        media?.uri?.isOTG() == true -> HeaderAudioBadgeIcon(R.drawable.ic_emoji_otg)
    }
}

@Composable
private fun HeaderAudioBadgeIcon(icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier
            .padding(3.composeDp)
            .size(16.composeDp)
    )
}

@Composable
private fun HeaderAudioMoreIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_more),
        contentDescription = stringResource(R.string.more),
        tint = VLCThemeDefaults.colors.listSubtitle
    )
}

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
