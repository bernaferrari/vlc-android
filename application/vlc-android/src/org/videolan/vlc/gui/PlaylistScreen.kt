/*
 * ************************************************************************
 *  PlaylistScreen.kt
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

package org.videolan.vlc.gui

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.MEDIALIBRARY_PAGE_SIZE
import org.videolan.resources.TAG_ITEM
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBrowserItemCard
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.dialogs.CURRENT_SORT
import org.videolan.vlc.gui.dialogs.DISPLAY_IN_CARDS
import org.videolan.vlc.gui.dialogs.ONLY_FAVS
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showDisplaySettingsComposeDialog
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.helpers.DefaultPlaybackAction
import org.videolan.vlc.gui.helpers.DefaultPlaybackActionMediaType
import org.videolan.vlc.gui.helpers.FloatingActionButtonBehavior
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
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
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_INFORMATION
import org.videolan.vlc.util.ContextOption.CTX_PLAY
import org.videolan.vlc.util.ContextOption.CTX_PLAY_AS_AUDIO
import org.videolan.vlc.util.ContextOption.CTX_PLAY_NEXT
import org.videolan.vlc.util.ContextOption.CTX_PLAY_SHUFFLE
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.ContextOption.Companion.createCtxPlaylistAlbumFlags
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.viewmodels.DisplaySettingsViewModel
import org.videolan.vlc.viewmodels.mobile.PlaylistsViewModel
import java.security.SecureRandom
import java.util.Arrays
import kotlin.math.min

class PlaylistScreenController(private val activity: MainActivity) : DefaultLifecycleObserver, org.videolan.vlc.gui.dialogs.CtxActionReceiver {

    private val settings = Settings.getInstance(activity)
    private val viewModel = ViewModelProvider(
        activity,
        PlaylistsViewModel.Factory(activity, Playlist.Type.All)
    )["main-playlists", PlaylistsViewModel::class.java]
    private val displaySettingsViewModel = ViewModelProvider(activity)[DisplaySettingsViewModel::class.java]

    private var playlists by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var loading by mutableStateOf(false)
    private var displayInCards by mutableStateOf(viewModel.providerInCard)
    private var onlyFavorites by mutableStateOf(viewModel.provider.onlyFavorites)
    private var selectedPositions by mutableStateOf<Set<Int>>(emptySet())
    private var actionMode: ActionMode? = null
    private var settingsJob: Job? = null
    private var visible = false

    init {
        activity.lifecycle.addObserver(this)
        viewModel.provider.pagedList.observe(activity) { list ->
            playlists = list?.filterNotNull().orEmpty()
            selectedPositions = selectedPositions.filter { it < playlists.size }.toSet()
            actionMode?.invalidate()
        }
        viewModel.provider.loading.observe(activity) { value ->
            loading = value == true
            activity.refreshing = loading
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        settingsJob?.cancel()
        activity.lifecycle.removeObserver(this)
    }

    fun onVisible() {
        visible = true
        activity.title = activity.getString(R.string.playlists)
        activity.setTabLayoutVisibility(false)
        hideFloatingActionButtons()
        startDisplaySettingsCollector()
        viewModel.refresh()
        activity.invalidateOptionsMenu()
    }

    fun onHidden() {
        visible = false
        actionMode?.finish()
        settingsJob?.cancel()
        settingsJob = null
    }

    fun refresh() {
        activity.reloadLibrary()
        viewModel.refresh()
    }

    fun prepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.ml_menu_sortby)?.isVisible = false
        menu.findItem(R.id.ml_menu_display_options)?.isVisible = true
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ml_menu_display_options -> {
                showDisplaySettings()
                true
            }
            else -> false
        }
    }

    @Composable
    fun Content() {
        PlaylistScreenContent(
            playlists = playlists,
            loading = loading,
            displayInCards = displayInCards,
            onlyFavorites = onlyFavorites,
            filterQuery = viewModel.filterQuery,
            selectedPositions = selectedPositions,
            onPlaylistClicked = ::onPlaylistClicked,
            onPlaylistLongClicked = ::onPlaylistLongClicked,
            onMoreClicked = ::showPlaylistContext,
            onMainActionClicked = ::onMainActionClicked
        )
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
            Medialibrary.NbMedia
        ).filter { viewModel.provider.canSortBy(it) }
        activity.showDisplaySettingsComposeDialog(
            displayInCards = viewModel.providerInCard,
            onlyFavs = viewModel.provider.onlyFavorites,
            sorts = sorts,
            currentSort = viewModel.provider.sort,
            currentSortDesc = viewModel.provider.desc,
            defaultPlaybackActions = DefaultPlaybackActionMediaType.PLAYLIST.getDefaultPlaybackActions(settings),
            defaultActionType = activity.getString(DefaultPlaybackActionMediaType.PLAYLIST.title)
        )
    }

    private fun startDisplaySettingsCollector() {
        if (settingsJob != null) return
        settingsJob = activity.lifecycleScope.launch {
            displaySettingsViewModel.settingChangeFlow.collect { change ->
                if (!visible || change.key == "init") return@collect
                when (change.key) {
                    DISPLAY_IN_CARDS -> {
                        val value = change.value as Boolean
                        viewModel.providerInCard = value
                        displayInCards = value
                        settings.putSingle(viewModel.displayModeKey, value)
                    }
                    ONLY_FAVS -> {
                        val value = change.value as Boolean
                        viewModel.provider.showOnlyFavs(value)
                        onlyFavorites = value
                        viewModel.refresh()
                    }
                    CURRENT_SORT -> {
                        @Suppress("UNCHECKED_CAST")
                        val sort = change.value as Pair<Int, Boolean>
                        viewModel.provider.sort = sort.first
                        viewModel.provider.desc = sort.second
                        viewModel.provider.saveSort()
                        viewModel.refresh()
                    }
                }
                displaySettingsViewModel.consume()
            }
        }
    }

    private fun onPlaylistClicked(position: Int, playlist: MediaLibraryItem) {
        if (actionMode != null) {
            toggleSelection(position)
            return
        }
        activity.startActivity(Intent(activity, HeaderMediaListActivity::class.java).apply {
            putExtra(TAG_ITEM, playlist)
        })
    }

    private fun onPlaylistLongClicked(position: Int, @Suppress("UNUSED_PARAMETER") playlist: MediaLibraryItem) {
        toggleSelection(position, forceSelected = true)
    }

    private fun toggleSelection(position: Int, forceSelected: Boolean = false) {
        val next = selectedPositions.toMutableSet()
        if (forceSelected) next.add(position) else if (!next.add(position)) next.remove(position)
        selectedPositions = next
        if (next.isNotEmpty() && actionMode == null) startActionMode()
        if (next.isEmpty()) actionMode?.finish() else actionMode?.invalidate()
    }

    private fun startActionMode() {
        actionMode = activity.startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.action_mode_audio_browser, menu)
                menu.findItem(R.id.action_mode_audio_add_playlist)?.isVisible = false
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val selection = selectedItems()
                if (selection.isEmpty()) {
                    mode.finish()
                    return false
                }
                mode.title = activity.getString(R.string.selection_count, selection.size)
                menu.findItem(R.id.action_mode_audio_info)?.isVisible = selection.size == 1
                menu.findItem(R.id.action_mode_audio_append)?.isVisible = PlaylistManager.hasMedia()
                menu.findItem(R.id.action_mode_audio_delete)?.isVisible = false
                menu.findItem(R.id.action_mode_audio_share)?.isVisible = false
                menu.findItem(R.id.action_mode_audio_set_song)?.isVisible = false
                menu.findItem(R.id.action_mode_go_to_folder)?.isVisible = false
                menu.findItem(R.id.action_mode_favorite_add)?.isVisible = selection.none { it.isFavorite }
                menu.findItem(R.id.action_mode_favorite_remove)?.isVisible = selection.none { !it.isFavorite }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val selection = selectedItems()
                mode.finish()
                if (selection.isNotEmpty()) activity.lifecycleScope.launch {
                    when (item.itemId) {
                        R.id.action_mode_audio_play -> MediaUtils.openList(activity, selection.tracks(), 0)
                        R.id.action_mode_audio_append -> MediaUtils.appendMedia(activity, selection.tracks())
                        R.id.action_mode_audio_info -> showInfoDialog(selection.first())
                        R.id.action_mode_favorite_add -> viewModel.changeFavorite(selection, true)
                        R.id.action_mode_favorite_remove -> viewModel.changeFavorite(selection, false)
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                selectedPositions = emptySet()
            }
        })
    }

    private fun selectedItems() = selectedPositions.sorted().mapNotNull { playlists.getOrNull(it) }

    private suspend fun List<MediaLibraryItem>.tracks() = withContext(Dispatchers.Default) {
        ArrayList<MediaWrapper>().apply {
            for (item in this@tracks) addAll(Arrays.asList(*item.tracks))
        }
    }

    private fun onMainActionClicked(position: Int, playlist: MediaLibraryItem) {
        when (DefaultPlaybackActionMediaType.PLAYLIST.getCurrentPlaybackAction(settings)) {
            DefaultPlaybackAction.PLAY -> MediaUtils.openList(activity, playlist.tracks.toList(), 0)
            DefaultPlaybackAction.ADD_TO_QUEUE -> MediaUtils.appendMedia(activity, playlist.tracks.toList())
            DefaultPlaybackAction.INSERT_NEXT -> MediaUtils.insertNext(activity, playlist.tracks)
            DefaultPlaybackAction.PLAY_ALL -> MediaUtils.openList(activity, playlist.tracks.toList(), 0)
        }
    }

    private fun showPlaylistContext(position: Int, playlist: MediaLibraryItem) {
        if (actionMode != null) return
        val flags = createCtxPlaylistAlbumFlags().apply {
            add(CTX_PLAY_AS_AUDIO)
            remove(CTX_GO_TO_ARTIST)
            if (playlist.tracksCount > 2) add(CTX_PLAY_SHUFFLE)
            if (playlist.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
        }
        showContext(activity, this, position, playlist, flags)
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        val playlist = playlists.getOrNull(position) ?: return
        when (option) {
            CTX_PLAY -> MediaUtils.playTracks(activity, playlist, 0)
            CTX_PLAY_SHUFFLE -> MediaUtils.playTracks(activity, playlist, SecureRandom().nextInt(min(playlist.tracksCount, MEDIALIBRARY_PAGE_SIZE)), true)
            CTX_PLAY_AS_AUDIO -> activity.lifecycleScope.launch(Dispatchers.IO) {
                (playlist as? Playlist)?.tracks?.let { tracks ->
                    MediaUtils.openList(activity, tracks.map {
                        it.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                        it
                    }, 0)
                }
            }
            CTX_INFORMATION -> showInfoDialog(playlist)
            CTX_DELETE -> activity.showConfirmDeleteComposeDialog(arrayListOf(playlist)) {
                activity.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { (playlist as? Playlist)?.delete() }
                    viewModel.refresh()
                }
            }
            CTX_APPEND -> MediaUtils.appendMedia(activity, playlist.tracks)
            CTX_PLAY_NEXT -> MediaUtils.insertNext(activity, playlist.tracks)
            CTX_ADD_TO_PLAYLIST -> activity.addToPlaylist(playlist.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            CTX_ADD_SHORTCUT -> activity.lifecycleScope.launch { activity.createShortcut(playlist) }
            CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch {
                withContext(Dispatchers.IO) { playlist.isFavorite = option == CTX_FAV_ADD }
                viewModel.refresh()
            }
            CTX_RENAME -> {
                if (playlist is Playlist) {
                    activity.showRenameComposeDialog(playlist) { renamedMedia, name ->
                        activity.lifecycleScope.launch { viewModel.rename(renamedMedia, name) }
                    }
                }
            }
            else -> {}
        }
    }

    private fun showInfoDialog(item: MediaLibraryItem) {
        activity.startActivity(Intent(activity, InfoActivity::class.java).apply {
            putExtra(TAG_ITEM, item)
        })
    }

    private fun hideFloatingActionButtons() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = true
            fab.hide()
        }
    }
}

@Composable
private fun PlaylistScreenContent(
    playlists: List<MediaLibraryItem>,
    loading: Boolean,
    displayInCards: Boolean,
    onlyFavorites: Boolean,
    filterQuery: String?,
    selectedPositions: Set<Int>,
    onPlaylistClicked: (Int, MediaLibraryItem) -> Unit,
    onPlaylistLongClicked: (Int, MediaLibraryItem) -> Unit,
    onMoreClicked: (Int, MediaLibraryItem) -> Unit,
    onMainActionClicked: (Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.backgroundDefault
    ) {
        when {
            loading && playlists.isEmpty() -> PlaylistEmptyState(loading = true, text = stringResource(R.string.loading))
            playlists.isEmpty() -> PlaylistEmptyState(
                loading = false,
                text = filterQuery?.let { stringResource(R.string.empty_search, it) }
                    ?: if (onlyFavorites) stringResource(R.string.nofav) else stringResource(R.string.noplaylist)
            )
            displayInCards -> PlaylistCardGrid(
                playlists = playlists,
                selectedPositions = selectedPositions,
                onPlaylistClicked = onPlaylistClicked,
                onPlaylistLongClicked = onPlaylistLongClicked,
                onMoreClicked = onMoreClicked,
                onMainActionClicked = onMainActionClicked
            )
            else -> PlaylistList(
                playlists = playlists,
                selectedPositions = selectedPositions,
                onPlaylistClicked = onPlaylistClicked,
                onPlaylistLongClicked = onPlaylistLongClicked,
                onMoreClicked = onMoreClicked,
                onMainActionClicked = onMainActionClicked
            )
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<MediaLibraryItem>,
    selectedPositions: Set<Int>,
    onPlaylistClicked: (Int, MediaLibraryItem) -> Unit,
    onPlaylistLongClicked: (Int, MediaLibraryItem) -> Unit,
    onMoreClicked: (Int, MediaLibraryItem) -> Unit,
    onMainActionClicked: (Int, MediaLibraryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VLCThemeDefaults.colors.backgroundDefault),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(playlists, key = { "${it.itemType}-${it.id}-${it.title}" }) { item ->
            val index = playlists.indexOf(item)
            PlaylistRow(
                item = item,
                selected = selectedPositions.contains(index),
                onClick = { onPlaylistClicked(index, item) },
                onLongClick = { onPlaylistLongClicked(index, item) },
                onMoreClick = { onMoreClicked(index, item) },
                onMainActionClick = { onMainActionClicked(index, item) }
            )
        }
    }
}

@Composable
private fun PlaylistCardGrid(
    playlists: List<MediaLibraryItem>,
    selectedPositions: Set<Int>,
    onPlaylistClicked: (Int, MediaLibraryItem) -> Unit,
    onPlaylistLongClicked: (Int, MediaLibraryItem) -> Unit,
    onMoreClicked: (Int, MediaLibraryItem) -> Unit,
    onMainActionClicked: (Int, MediaLibraryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VLCThemeDefaults.colors.backgroundDefault),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(playlists.chunked(2), key = { row -> row.joinToString { "${it.id}-${it.title}" } }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val index = playlists.indexOf(item)
                    PlaylistCard(
                        item = item,
                        selected = selectedPositions.contains(index),
                        modifier = Modifier.weight(1f),
                        onClick = { onPlaylistClicked(index, item) },
                        onLongClick = { onPlaylistLongClicked(index, item) },
                        onMoreClick = { onMoreClicked(index, item) },
                        onMainActionClick = { onMainActionClicked(index, item) }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlaylistRow(
    item: MediaLibraryItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    onMainActionClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    VLCBrowserItemRow(
        title = item.title.orEmpty(),
        subtitle = stringResource(R.plurals.track_quantity, item.tracksCount, item.tracksCount),
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        artworkContent = {
            PlaylistIconContent(size = 28.dp)
        },
        primaryActionContent = {
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = stringResource(R.string.play),
                tint = colors.primary
            )
        },
        onPrimaryActionClick = onMainActionClick,
        moreActionContent = {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.more),
                tint = colors.listSubtitle
            )
        },
        onMoreClick = onMoreClick
    )
}

@Composable
private fun PlaylistCard(
    item: MediaLibraryItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    onMainActionClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    VLCBrowserItemCard(
        title = item.title.orEmpty(),
        subtitle = stringResource(R.plurals.track_quantity, item.tracksCount, item.tracksCount),
        selected = selected,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        artworkContent = {
            PlaylistIconContent(size = 32.dp)
        },
        primaryActionContent = {
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = stringResource(R.string.play),
                tint = colors.primary
            )
        },
        onPrimaryActionClick = onMainActionClick,
        moreActionContent = {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.more),
                tint = colors.listSubtitle
            )
        },
        onMoreClick = onMoreClick
    )
}

@Composable
private fun PlaylistIconContent(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_playlist),
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = VLCThemeDefaults.colors.primary
    )
}

@Composable
private fun PlaylistEmptyState(loading: Boolean, text: String) {
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
        Text(
            text = text,
            color = colors.listSubtitle,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
