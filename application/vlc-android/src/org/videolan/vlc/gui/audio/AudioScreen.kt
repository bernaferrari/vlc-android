/*
 * ************************************************************************
 *  AudioScreen.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
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
 * **************************************************************************
 */

package org.videolan.vlc.gui.audio

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.Artist
import org.videolan.medialibrary.interfaces.media.Genre
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AppContextProvider
import org.videolan.resources.MEDIALIBRARY_PAGE_SIZE
import org.videolan.resources.PLAYLIST_TYPE_AUDIO
import org.videolan.resources.TAG_ITEM
import org.videolan.tools.KEY_ARTISTS_SHOW_ALL
import org.videolan.tools.KEY_AUDIO_CURRENT_TAB
import org.videolan.tools.KEY_AUDIO_LAST_PLAYLIST
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.tools.retrieveParent
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCRenameDialogContent
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.HeaderMediaListActivity
import org.videolan.vlc.gui.HeaderMediaListActivity.Companion.ARTIST_FROM_ALBUM
import org.videolan.vlc.gui.InfoActivity
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.browser.KEY_JUMP_TO
import org.videolan.vlc.gui.browser.KEY_MEDIA
import org.videolan.vlc.gui.dialogs.CURRENT_SORT
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.DEFAULT_ACTIONS
import org.videolan.vlc.gui.dialogs.DISPLAY_IN_CARDS
import org.videolan.vlc.gui.dialogs.ONLY_FAVS
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.SHOW_ALL_ARTISTS
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showDisplaySettingsComposeDialog
import org.videolan.vlc.gui.helpers.AudioUtil.setRingtone
import org.videolan.vlc.gui.helpers.DefaultPlaybackAction
import org.videolan.vlc.gui.helpers.DefaultPlaybackActionMediaType
import org.videolan.vlc.gui.helpers.FloatingActionButtonBehavior
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.helpers.UiTools.snacker
import org.videolan.vlc.gui.helpers.UiTools.snackerMissing
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_SHORTCUT
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_FAV_ADD
import org.videolan.vlc.util.ContextOption.CTX_FAV_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ALBUM
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ALBUM_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_FOLDER
import org.videolan.vlc.util.ContextOption.CTX_INFORMATION
import org.videolan.vlc.util.ContextOption.CTX_PLAY
import org.videolan.vlc.util.ContextOption.CTX_PLAY_ALL
import org.videolan.vlc.util.ContextOption.CTX_PLAY_AS_AUDIO
import org.videolan.vlc.util.ContextOption.CTX_PLAY_NEXT
import org.videolan.vlc.util.ContextOption.CTX_PLAY_SHUFFLE
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.ContextOption.CTX_SET_RINGTONE
import org.videolan.vlc.util.ContextOption.CTX_SHARE
import org.videolan.vlc.util.ContextOption.Companion.createCtxAudioFlags
import org.videolan.vlc.util.ContextOption.Companion.createCtxPlaylistAlbumFlags
import org.videolan.vlc.util.ContextOption.Companion.createCtxTrackFlags
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.share
import org.videolan.vlc.viewmodels.DisplaySettingsViewModel
import org.videolan.vlc.viewmodels.mobile.AudioBrowserViewModel
import java.security.SecureRandom
import kotlin.math.min

private const val MODE_ARTISTS = 0
private const val MODE_ALBUMS = 1
private const val MODE_TRACKS = 2
private const val MODE_GENRES = 3
private const val MODE_PLAYLISTS = 4
private const val MODE_TOTAL = 5
private const val AUDIO_TAG_ITEM = "ML_ITEM"

class AudioScreenController(private val activity: MainActivity) : DefaultLifecycleObserver, CtxActionReceiver {

    private val settings = Settings.getInstance(activity)
    private val viewModel = ViewModelProvider(
        activity,
        AudioBrowserViewModel.Factory(activity)
    )["main-audio", AudioBrowserViewModel::class.java]
    private val displaySettingsViewModel = ViewModelProvider(activity)[DisplaySettingsViewModel::class.java]

    private var currentTab by mutableIntStateOf(viewModel.currentTab.coerceIn(0, MODE_TOTAL - 1))
    private var itemsByTab by mutableStateOf(List(MODE_TOTAL) { emptyList<MediaLibraryItem>() })
    private var loadingByTab by mutableStateOf(List(MODE_TOTAL) { false })
    private var displayInCardsByTab by mutableStateOf(viewModel.providersInCard.toList())
    private var settingsJob: Job? = null
    private var visible = false

    init {
        activity.lifecycle.addObserver(this)
        viewModel.providers.forEachIndexed { index, provider ->
            provider.pagedList.observe(activity) { list ->
                updateItems(index, list?.filterNotNull().orEmpty())
            }
            provider.loading.observe(activity) { loading ->
                updateLoading(index, loading == true)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        settingsJob?.cancel()
        activity.lifecycle.removeObserver(this)
    }

    fun onVisible() {
        visible = true
        activity.title = activity.getString(R.string.audio)
        activity.setTabLayoutVisibility(false)
        startDisplaySettingsCollector()
        if (viewModel.showResumeCard) {
            activity.proposeCard()
            viewModel.showResumeCard = false
        }
        showFloatingActionButton()
        viewModel.refresh()
        activity.invalidateOptionsMenu()
    }

    fun onHidden() {
        visible = false
        settingsJob?.cancel()
        settingsJob = null
        hideFloatingActionButtons()
    }

    fun refresh() {
        activity.reloadLibrary()
        viewModel.refresh()
    }

    fun prepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.ml_menu_last_playlist)?.isVisible = settings.contains(KEY_AUDIO_LAST_PLAYLIST)
        menu.findItem(R.id.ml_menu_sortby)?.isVisible = false
        menu.findItem(R.id.ml_menu_display_options)?.isVisible = true
        menu.findItem(R.id.play_all)?.isVisible = false
        menu.findItem(R.id.shuffle_all)?.isVisible = currentTab == MODE_TRACKS && itemsByTab[MODE_TRACKS].size > 2
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ml_menu_last_playlist -> {
                MediaUtils.loadlastPlaylist(activity, PLAYLIST_TYPE_AUDIO)
                true
            }
            R.id.shuffle_all -> {
                MediaUtils.playAll(activity, viewModel.tracksProvider, 0, true)
                true
            }
            R.id.ml_menu_display_options -> {
                showDisplaySettings()
                true
            }
            else -> false
        }
    }

    @Composable
    fun Content() {
        AudioScreenContent(
            selectedTab = currentTab,
            tabs = audioTabs(),
            items = itemsByTab[currentTab],
            loading = loadingByTab[currentTab],
            displayInCards = displayInCardsByTab[currentTab],
            onlyFavorites = viewModel.providers[currentTab].onlyFavorites,
            filterQuery = viewModel.filterQuery,
            onTabSelected = ::selectTab,
            onItemClicked = ::onItemClicked,
            onMoreClicked = ::onMoreClicked,
            onMainActionClicked = ::onMainActionClicked
        )
    }

    private fun audioTabs() = listOf(
        activity.getString(R.string.artists),
        activity.getString(R.string.albums),
        activity.getString(R.string.tracks),
        activity.getString(R.string.genres),
        activity.getString(R.string.playlists)
    )

    private fun updateItems(index: Int, items: List<MediaLibraryItem>) {
        itemsByTab = itemsByTab.toMutableList().also { it[index] = items }
        if (visible && index == currentTab) {
            showFloatingActionButton()
            activity.invalidateOptionsMenu()
        }
    }

    private fun updateLoading(index: Int, loading: Boolean) {
        loadingByTab = loadingByTab.toMutableList().also { it[index] = loading }
        if (index == currentTab) activity.refreshing = loading
    }

    private fun selectTab(index: Int) {
        currentTab = index
        viewModel.currentTab = index
        settings.putSingle(KEY_AUDIO_CURRENT_TAB, index)
        showFloatingActionButton()
        activity.invalidateOptionsMenu()
    }

    private fun startDisplaySettingsCollector() {
        if (settingsJob != null) return
        settingsJob = activity.lifecycleScope.launch {
            displaySettingsViewModel.settingChangeFlow.collect { change ->
                if (!visible || change.key == "init") return@collect
                applyDisplayChange(change.key, change.value)
                displaySettingsViewModel.consume()
            }
        }
    }

    private fun applyDisplayChange(key: String, value: Any) {
        when (key) {
            DISPLAY_IN_CARDS -> {
                val cards = value as Boolean
                viewModel.providersInCard[currentTab] = cards
                displayInCardsByTab = displayInCardsByTab.toMutableList().also { it[currentTab] = cards }
                settings.putSingle(viewModel.displayModeKeys[currentTab], cards)
            }
            SHOW_ALL_ARTISTS -> {
                settings.putSingle(KEY_ARTISTS_SHOW_ALL, value as Boolean)
                viewModel.artistsProvider.showAll = value
                viewModel.refresh()
            }
            ONLY_FAVS -> {
                viewModel.providers[currentTab].showOnlyFavs(value as Boolean)
                viewModel.providers[currentTab].refresh()
            }
            CURRENT_SORT -> {
                @Suppress("UNCHECKED_CAST")
                val sort = value as Pair<Int, Boolean>
                viewModel.providers[currentTab].sort = sort.first
                viewModel.providers[currentTab].desc = sort.second
                viewModel.providers[currentTab].saveSort()
                viewModel.refresh()
            }
            DEFAULT_ACTIONS -> settings.putSingle(defaultActionMediaType().defaultActionKey, (value as DefaultPlaybackAction).name)
        }
    }

    private fun showDisplaySettings() {
        val sorts = arrayListOf(
            Medialibrary.SORT_ALPHA,
            Medialibrary.SORT_FILENAME,
            Medialibrary.SORT_ARTIST,
            Medialibrary.SORT_ALBUM,
            Medialibrary.SORT_DURATION,
            Medialibrary.SORT_RELEASEDATE,
            Medialibrary.SORT_LASTMODIFICATIONDATE,
            Medialibrary.SORT_FILESIZE,
            Medialibrary.NbMedia,
            Medialibrary.SORT_INSERTIONDATE
        ).filter { viewModel.providers[currentTab].canSortBy(it) }
        activity.showDisplaySettingsComposeDialog(
            displayInCards = viewModel.providersInCard[currentTab],
            showAllArtists = if (currentTab == MODE_ARTISTS) settings.getBoolean(KEY_ARTISTS_SHOW_ALL, false) else null,
            onlyFavs = viewModel.providers[currentTab].onlyFavorites,
            sorts = sorts,
            currentSort = viewModel.providers[currentTab].sort,
            currentSortDesc = viewModel.providers[currentTab].desc,
            defaultPlaybackActions = defaultActionMediaType().getDefaultPlaybackActions(settings),
            defaultActionType = activity.getString(defaultActionMediaType().title)
        )
    }

    private fun onItemClicked(position: Int, item: MediaLibraryItem) {
        if (item.itemType == MediaLibraryItem.TYPE_MEDIA) {
            if (item is MediaWrapper && !item.isPresent) {
                snackerMissing(activity)
                return
            }
            onMainActionClicked(position, item)
            return
        }
        when (item.itemType) {
            MediaLibraryItem.TYPE_ARTIST, MediaLibraryItem.TYPE_GENRE -> openAlbumsSongs(item)
            MediaLibraryItem.TYPE_ALBUM, MediaLibraryItem.TYPE_PLAYLIST -> openHeaderList(item)
        }
    }

    private fun onMainActionClicked(position: Int, item: MediaLibraryItem) {
        when (defaultActionMediaType().getCurrentPlaybackAction(settings)) {
            DefaultPlaybackAction.PLAY -> MediaUtils.openList(activity, item.tracks.toList(), 0)
            DefaultPlaybackAction.ADD_TO_QUEUE -> MediaUtils.appendMedia(activity, item.tracks)
            DefaultPlaybackAction.INSERT_NEXT -> MediaUtils.insertNext(activity, item.tracks)
            DefaultPlaybackAction.PLAY_ALL -> {
                if (item.itemType == MediaLibraryItem.TYPE_MEDIA) MediaUtils.playAll(activity, viewModel.tracksProvider, position, false)
                else MediaUtils.openList(activity, item.tracks.toList(), 0)
            }
        }
    }

    private fun onMoreClicked(position: Int, item: MediaLibraryItem) {
        val flags = when (item.itemType) {
            MediaLibraryItem.TYPE_MEDIA -> createCtxTrackFlags().apply {
                if ((item as? MediaWrapper)?.isFavorite == true) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
                val media = item as? MediaWrapper
                if (media != null && media.artistId != media.albumArtistId) add(CTX_GO_TO_ALBUM_ARTIST)
            }
            MediaLibraryItem.TYPE_ARTIST -> createCtxAudioFlags().apply {
                if (item.tracksCount > 2) add(CTX_PLAY_SHUFFLE)
                if ((item as? Artist)?.isFavorite == true) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
            }
            MediaLibraryItem.TYPE_ALBUM -> createCtxPlaylistAlbumFlags().apply {
                remove(CTX_RENAME)
                if (item.tracksCount > 2) add(CTX_PLAY_SHUFFLE)
                if ((item as? Album)?.isFavorite == true) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
            }
            MediaLibraryItem.TYPE_GENRE -> createCtxAudioFlags().apply {
                if (item.tracksCount > 2) add(CTX_PLAY_SHUFFLE)
                if ((item as? Genre)?.isFavorite == true) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
            }
            MediaLibraryItem.TYPE_PLAYLIST -> createCtxPlaylistAlbumFlags().apply {
                add(CTX_PLAY_AS_AUDIO)
                remove(CTX_GO_TO_ALBUM_ARTIST)
                remove(CTX_GO_TO_ARTIST)
                if (item.tracksCount > 2) add(CTX_PLAY_SHUFFLE)
                if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
            }
            else -> createCtxAudioFlags()
        }
        showContext(activity, this, position, item, flags)
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        val item = itemsByTab[currentTab].getOrNull(position) ?: return
        when (option) {
            CTX_PLAY -> MediaUtils.playTracks(activity, item, 0)
            CTX_PLAY_ALL -> if (item.itemType == MediaLibraryItem.TYPE_MEDIA) MediaUtils.playAll(activity, viewModel.tracksProvider, position, false) else MediaUtils.openList(activity, item.tracks.toList(), 0)
            CTX_PLAY_SHUFFLE -> MediaUtils.playTracks(activity, item, SecureRandom().nextInt(min(item.tracksCount, MEDIALIBRARY_PAGE_SIZE)), true)
            CTX_PLAY_AS_AUDIO -> activity.lifecycleScope.launch(Dispatchers.IO) {
                (item as? Playlist)?.tracks?.let { tracks ->
                    MediaUtils.openList(activity, tracks.map {
                        it.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                        it
                    }, 0)
                }
            }
            CTX_INFORMATION -> showInfoDialog(item)
            CTX_GO_TO_ALBUM -> (item as? MediaWrapper)?.album?.let { openHeaderList(it) }
            CTX_GO_TO_ARTIST -> openArtist(item, albumArtist = false)
            CTX_GO_TO_ALBUM_ARTIST -> openArtist(item, albumArtist = true)
            CTX_DELETE -> confirmDelete(item) { viewModel.refresh() }
            CTX_APPEND -> MediaUtils.appendMedia(activity, item.tracks)
            CTX_PLAY_NEXT -> MediaUtils.insertNext(activity, item.tracks)
            CTX_ADD_TO_PLAYLIST -> activity.addToPlaylist(item.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            CTX_SET_RINGTONE -> (item as? MediaWrapper)?.let { activity.setRingtone(it) }
            CTX_SHARE -> (item as? MediaWrapper)?.let { media -> activity.lifecycleScope.launch { activity.share(media) } }
            CTX_GO_TO_FOLDER -> (item as? MediaWrapper)?.let { showParentFolder(it) }
            CTX_ADD_SHORTCUT -> activity.lifecycleScope.launch { activity.createShortcut(item) }
            CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch {
                withContext(Dispatchers.IO) { item.isFavorite = option == CTX_FAV_ADD }
                viewModel.refresh()
            }
            CTX_RENAME -> if (item is Playlist) {
                showRenameDialog(item) { name ->
                    activity.lifecycleScope.launch {
                        withContext(Dispatchers.IO) { item.setName(name) }
                        viewModel.refresh()
                    }
                }
            }
            else -> {}
        }
    }

    private fun openAlbumsSongs(item: MediaLibraryItem) {
        activity.startActivity(Intent(activity, SecondaryActivity::class.java).apply {
            putExtra(SecondaryActivity.KEY_FRAGMENT, SecondaryActivity.ALBUMS_SONGS)
            putExtra(TAG_ITEM, item)
        })
    }

    private fun openHeaderList(item: MediaLibraryItem) {
        activity.startActivity(Intent(activity, HeaderMediaListActivity::class.java).apply {
            putExtra(TAG_ITEM, item)
        })
    }

    private fun openArtist(item: MediaLibraryItem, albumArtist: Boolean) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val artist = when {
                albumArtist && item is MediaWrapper -> item.albumArtist
                item is Album -> item.retrieveAlbumArtist()
                item is MediaWrapper -> item.artist
                else -> null
            } ?: return@launch
            withContext(Dispatchers.Main) {
                activity.startActivity(Intent(activity, SecondaryActivity::class.java).apply {
                    putExtra(SecondaryActivity.KEY_FRAGMENT, SecondaryActivity.ALBUMS_SONGS)
                    putExtra(AUDIO_TAG_ITEM, artist)
                    putExtra(ARTIST_FROM_ALBUM, true)
                    flags = flags or Intent.FLAG_ACTIVITY_NO_HISTORY
                })
            }
        }
    }

    private fun showInfoDialog(item: MediaLibraryItem) {
        activity.startActivity(Intent(activity, InfoActivity::class.java).apply {
            putExtra(TAG_ITEM, item)
        })
    }

    private fun confirmDelete(item: MediaLibraryItem, onDeleted: () -> Unit) {
        activity.showConfirmDeleteComposeDialog(arrayListOf(item)) {
            MediaUtils.deleteItem(activity, item) { failed ->
                snacker(activity, activity.getString(R.string.msg_delete_failed, failed.title))
            }
            onDeleted()
        }
    }

    private fun showParentFolder(media: MediaWrapper) {
        val parent = MLServiceLocator.getAbstractMediaWrapper(media.uri.retrieveParent()).apply {
            type = MediaWrapper.TYPE_DIR
        }
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, parent)
            putExtra(KEY_JUMP_TO, media)
            putExtra(SecondaryActivity.KEY_FRAGMENT, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun showRenameDialog(item: MediaLibraryItem, onRename: (String) -> Unit) {
        if (activity.showPinIfNeeded()) return
        val dialog = if (Settings.showTvUi) {
            BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
        } else {
            BottomSheetDialog(activity)
        }
        var newName by mutableStateOf(TextFieldValue(item.title.orEmpty()))
        val rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    VLCRenameDialogContent(
                        title = activity.getString(R.string.rename),
                        mediaTitle = item.title.orEmpty(),
                        newTitleHint = activity.getString(R.string.new_title),
                        okText = activity.getString(R.string.ok),
                        newName = newName,
                        onNewNameChange = { newName = it },
                        onConfirm = {
                            val trimmedName = newName.text.trim()
                            if (trimmedName.isNotEmpty()) {
                                onRename(trimmedName)
                                dialog.dismiss()
                            }
                        }
                    )
                }
            }
        }
        dialog.setContentView(rootView)
        dialog.show()
    }

    private fun showFloatingActionButton() {
        val fab = activity.findViewById<FloatingActionButton?>(R.id.fab) ?: return
        val visible = currentTab == MODE_TRACKS && itemsByTab[MODE_TRACKS].size > 2
        ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = !visible
        fab.setImageResource(R.drawable.ic_fab_shuffle)
        fab.contentDescription = activity.getString(R.string.shuffle_play)
        fab.setOnClickListener { MediaUtils.playAll(activity, viewModel.tracksProvider, 0, true) }
        if (visible) fab.show() else fab.hide()
        activity.findViewById<FloatingActionButton?>(R.id.fab_large)?.hide()
    }

    private fun hideFloatingActionButtons() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            fab.hide()
        }
    }

    private fun defaultActionMediaType() = when (currentTab) {
        MODE_ARTISTS -> DefaultPlaybackActionMediaType.ARTIST
        MODE_ALBUMS -> DefaultPlaybackActionMediaType.ALBUM
        MODE_TRACKS -> DefaultPlaybackActionMediaType.TRACK
        MODE_GENRES -> DefaultPlaybackActionMediaType.GENRE
        else -> DefaultPlaybackActionMediaType.PLAYLIST
    }
}

@Composable
private fun AudioScreenContent(
    selectedTab: Int,
    tabs: List<String>,
    items: List<MediaLibraryItem>,
    loading: Boolean,
    displayInCards: Boolean,
    onlyFavorites: Boolean,
    filterQuery: String?,
    onTabSelected: (Int) -> Unit,
    onItemClicked: (Int, MediaLibraryItem) -> Unit,
    onMoreClicked: (Int, MediaLibraryItem) -> Unit,
    onMainActionClicked: (Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDefault)
    ) {
        AudioTabs(tabs = tabs, selectedTab = selectedTab, onTabSelected = onTabSelected)
        AudioGridOrList(
            items = items,
            loading = loading,
            displayInCards = displayInCards,
            emptyText = emptyAudioText(onlyFavorites, filterQuery),
            onClick = onItemClicked,
            onMoreClick = onMoreClicked,
            onMainActionClick = onMainActionClicked
        )
    }
}

@Composable
private fun AudioTabs(tabs: List<String>, selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.headerBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index
            TextButton(
                onClick = { onTabSelected(index) },
                modifier = Modifier.background(if (selected) colors.subtleSelection else colors.headerBackground, RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = title,
                    color = if (selected) colors.primary else colors.listTitle,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AudioGridOrList(
    items: List<MediaLibraryItem>,
    loading: Boolean,
    displayInCards: Boolean,
    emptyText: String,
    onClick: (Int, MediaLibraryItem) -> Unit,
    onMoreClick: (Int, MediaLibraryItem) -> Unit,
    onMainActionClick: (Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(modifier = Modifier.fillMaxSize(), color = colors.backgroundDefault) {
        when {
            loading && items.isEmpty() -> AudioEmptyState(loading = true, text = stringResource(R.string.loading))
            items.isEmpty() -> AudioEmptyState(loading = false, text = emptyText)
            displayInCards -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(
                    items.chunked(2),
                    key = { rowIndex, row -> "$rowIndex-${row.joinToString { "${it.itemType}-${it.id}-${it.title}" }}" }
                ) { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { columnIndex, item ->
                            val index = rowIndex * 2 + columnIndex
                            AudioCard(
                                item = item,
                                modifier = Modifier.weight(1f),
                                onClick = { onClick(index, item) },
                                onMoreClick = { onMoreClick(index, item) },
                                onMainActionClick = { onMainActionClick(index, item) }
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(items, key = { index, item -> "$index-${item.itemType}-${item.id}-${item.title}" }) { index, item ->
                    AudioRow(
                        item = item,
                        onClick = { onClick(index, item) },
                        onMoreClick = { onMoreClick(index, item) },
                        onMainActionClick = { onMainActionClick(index, item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioRow(
    item: MediaLibraryItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onMainActionClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AudioIcon(item)
        Spacer(modifier = Modifier.width(16.dp))
        AudioTexts(item = item, modifier = Modifier.weight(1f))
        IconButton(onClick = onMainActionClick) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.play), tint = colors.primary)
        }
        IconButton(onClick = onMoreClick) {
            Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more), tint = colors.listSubtitle)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioCard(
    item: MediaLibraryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onMainActionClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.backgroundDefaultDarker)
            .border(1.dp, colors.listSubtitle.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AudioIcon(item, large = true)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onMainActionClick, modifier = Modifier.size(36.dp)) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.play), tint = colors.primary)
            }
            IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more), tint = colors.listSubtitle)
            }
        }
        AudioTexts(item = item)
    }
}

@Composable
private fun AudioTexts(item: MediaLibraryItem, modifier: Modifier = Modifier) {
    val colors = VLCThemeDefaults.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = item.title.orEmpty(),
            color = colors.listTitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = audioSubtitle(item),
            color = colors.listSubtitle,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AudioIcon(item: MediaLibraryItem, large: Boolean = false) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier
            .size(if (large) 48.dp else 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundDefault),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(audioIcon(item)),
            contentDescription = null,
            modifier = Modifier.size(if (large) 32.dp else 28.dp),
            tint = colors.primary
        )
    }
}

@Composable
private fun AudioEmptyState(loading: Boolean, text: String) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) CircularProgressIndicator(color = colors.primary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = text, color = colors.listSubtitle, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun emptyAudioText(onlyFavorites: Boolean, filterQuery: String?): String {
    return when {
        !Permissions.canReadStorage(AppContextProvider.appContext) -> stringResource(R.string.permission_media)
        !Permissions.canReadAudios(AppContextProvider.appContext) -> stringResource(R.string.permission_media)
        filterQuery != null -> stringResource(R.string.empty_search, filterQuery)
        onlyFavorites -> stringResource(R.string.nofav)
        else -> stringResource(R.string.nomedia)
    }
}

@Composable
private fun audioSubtitle(item: MediaLibraryItem): String {
    return when (item) {
        is Artist -> stringResource(R.plurals.albums_quantity, item.albumsCount, item.albumsCount)
        is Genre -> stringResource(R.plurals.track_quantity, item.tracksCount, item.tracksCount)
        else -> item.description?.toString().takeUnless { it.isNullOrBlank() }
            ?: stringResource(R.plurals.track_quantity, item.tracksCount, item.tracksCount)
    }
}

private fun audioIcon(item: MediaLibraryItem): Int {
    return when (item.itemType) {
        MediaLibraryItem.TYPE_ARTIST -> R.drawable.ic_artist_big
        MediaLibraryItem.TYPE_ALBUM -> R.drawable.ic_album_big
        MediaLibraryItem.TYPE_MEDIA -> R.drawable.ic_song_big
        MediaLibraryItem.TYPE_GENRE -> R.drawable.ic_genre_big
        MediaLibraryItem.TYPE_PLAYLIST -> R.drawable.ic_playlist_big
        else -> R.drawable.ic_song_big
    }
}
