/*
 * ************************************************************************
 *  MainBrowserScreen.kt
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

package org.videolan.vlc.gui.browser

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.medialibrary.media.MediaWrapperImpl
import org.videolan.medialibrary.media.Storage
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.tools.KEY_NAVIGATOR_SCREEN_UNSTABLE
import org.videolan.tools.NetworkMonitor
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showNetworkServerComposeDialog
import org.videolan.vlc.gui.helpers.FloatingActionButtonBehavior
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylistAsync
import org.videolan.vlc.gui.helpers.UiTools.showMediaInfo
import org.videolan.vlc.gui.helpers.hf.OTG_SCHEME
import org.videolan.vlc.gui.helpers.hf.OtgAccess
import org.videolan.vlc.gui.helpers.hf.requestOtgRoot
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_FOLDER_AND_SUB_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_ADD_FOLDER_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_FAV_ADD
import org.videolan.vlc.util.ContextOption.CTX_FAV_EDIT
import org.videolan.vlc.util.ContextOption.CTX_FAV_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_PLAY
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.isSchemeFavoriteEditable
import org.videolan.vlc.viewmodels.browser.BrowserFavoritesModel
import org.videolan.vlc.viewmodels.browser.BrowserModel
import org.videolan.vlc.viewmodels.browser.NetworkModel
import org.videolan.vlc.viewmodels.browser.TYPE_FILE

class MainBrowserScreenController(
    private val activity: MainActivity,
    forEspresso: ArrayList<MediaLibraryItem>?
) : CtxActionReceiver, DefaultLifecycleObserver {

    private val settings = Settings.getInstance(activity)
    private val browserFavRepository = BrowserFavRepository.getInstance(activity)
    private val networkMonitor = NetworkMonitor.getInstance(activity)
    private val displayInListKey = "main_browser_fragment_display_mode"

    private val localViewModel = ViewModelProvider(
        activity,
        BrowserModel.Factory(activity, url = null, type = TYPE_FILE)
    )["main-browser-local", BrowserModel::class.java]
    private val favoritesViewModel = ViewModelProvider(
        activity,
        BrowserFavoritesFactory(activity)
    )["main-browser-favorites", BrowserFavoritesModel::class.java]
    private val networkViewModel = ViewModelProvider(
        activity,
        NetworkModel.Factory(activity, url = null, mocked = forEspresso)
    )["main-browser-network", NetworkModel::class.java]

    private var localItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var favoriteItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var networkItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var localLoading by mutableStateOf(false)
    private var favoritesLoading by mutableStateOf(false)
    private var networkLoading by mutableStateOf(false)
    private var displayInList by mutableStateOf(settings.getBoolean(displayInListKey, false))
    private var selectedItems by mutableStateOf<Set<MediaWrapper>>(emptySet())
    private var loaded = false
    private var requiringOtg = false
    private var actionMode: ActionMode? = null
    private var contextItems: List<MediaLibraryItem> = emptyList()

    init {
        activity.lifecycle.addObserver(this)
        localViewModel.dataset.observe(activity) { localItems = it?.toList().orEmpty() }
        localViewModel.loading.observe(activity) { loading ->
            localLoading = loading == true
            updateRefreshing()
        }
        localViewModel.getDescriptionUpdate().observe(activity) {
            localItems = localViewModel.dataset.value?.toList().orEmpty()
        }
        favoritesViewModel.favorites.observe(activity) { favoriteItems = it?.toList().orEmpty() }
        favoritesViewModel.provider.loading.observe(activity) { loading ->
            favoritesLoading = loading == true
            updateRefreshing()
        }
        favoritesViewModel.provider.descriptionUpdate.observe(activity) {
            favoriteItems = favoritesViewModel.favorites.value?.toList().orEmpty()
        }
        networkViewModel.dataset.observe(activity) { networkItems = it?.toList().orEmpty() }
        networkViewModel.loading.observe(activity) { loading ->
            networkLoading = loading == true
            updateRefreshing()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!requiringOtg || OtgAccess.otgRoot.value == null) return
        val otgMedia = MediaWrapperImpl("otg://".toUri()).apply {
            title = activity.getString(R.string.otg_device_title)
        }
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, otgMedia)
            putExtra(SecondaryActivity.KEY_FRAGMENT, SecondaryActivity.FILE_BROWSER)
        })
        requiringOtg = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activity.lifecycle.removeObserver(this)
    }

    fun onVisible() {
        settings.edit { putBoolean(KEY_NAVIGATOR_SCREEN_UNSTABLE, true) }
        activity.title = activity.getString(R.string.browse)
        activity.setTabLayoutVisibility(false)
        hideFloatingActionButtons()
        if (!loaded) {
            loaded = true
            localViewModel.browseRoot()
            favoritesViewModel.provider.refresh()
            networkViewModel.browseRoot()
        }
        activity.invalidateOptionsMenu()
    }

    fun onHidden() {
        settings.edit { putBoolean(KEY_NAVIGATOR_SCREEN_UNSTABLE, false) }
        actionMode?.finish()
    }

    fun refresh() {
        localViewModel.refresh()
        favoritesViewModel.provider.refresh()
        if (settings.getBoolean(KEY_BROWSE_NETWORK, true)) networkViewModel.refresh()
        updateRefreshing()
    }

    fun prepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.ml_menu_display_grid)?.isVisible = displayInList
        menu.findItem(R.id.ml_menu_display_list)?.isVisible = !displayInList
        menu.findItem(R.id.add_server_favorite)?.isVisible = true
        menu.findItem(R.id.browse_network)?.let {
            it.isVisible = true
            it.isChecked = settings.getBoolean(KEY_BROWSE_NETWORK, true)
        }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ml_menu_display_list, R.id.ml_menu_display_grid -> {
                displayInList = item.itemId == R.id.ml_menu_display_list
                settings.putSingle(displayInListKey, displayInList)
                activity.invalidateOptionsMenu()
                true
            }
            R.id.browse_network -> {
                val enabled = !item.isChecked
                item.isChecked = enabled
                settings.putSingle(KEY_BROWSE_NETWORK, enabled)
                if (!enabled) {
                    networkViewModel.stop()
                    networkViewModel.dataset.clear()
                    networkItems = emptyList()
                } else {
                    networkViewModel.refresh()
                }
                true
            }
            R.id.add_server_favorite -> {
                activity.showNetworkServerComposeDialog(null)
                true
            }
            else -> false
        }
    }

    @Composable
    fun Content() {
        MainBrowserScreenContent(
            displayInList = displayInList,
            localItems = localItems,
            favoriteItems = favoriteItems,
            networkItems = networkItems,
            localLoading = localLoading,
            favoritesLoading = favoritesLoading,
            networkLoading = networkLoading,
            canReadStorage = Permissions.canReadStorage(activity),
            networkEnabled = settings.getBoolean(KEY_BROWSE_NETWORK, true),
            networkConnected = networkMonitor.connected,
            lanAllowed = networkMonitor.lanAllowed,
            localModel = localViewModel,
            networkModel = networkViewModel,
            selectedItems = selectedItems,
            onItemClicked = ::onItemClicked,
            onItemLongClicked = ::onItemLongClicked,
            onMoreClicked = ::onMoreClicked
        )
    }

    private fun onItemClicked(item: MediaLibraryItem) {
        val media = item.asMediaWrapper() ?: return
        if (actionMode != null) {
            if (media.isMultiSelectable()) toggleSelection(media)
            return
        }
        if (media.location == "otg://" && OtgAccess.otgRoot.value == null) {
            requiringOtg = true
            activity.requestOtgRoot()
            return
        }
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, media)
            putExtra(SecondaryActivity.KEY_FRAGMENT, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun onItemLongClicked(section: MainBrowserSection, position: Int, item: MediaLibraryItem) {
        val media = item.asMediaWrapper() ?: return
        if (media.isMultiSelectable()) {
            toggleSelection(media, forceSelected = true)
        } else {
            onMoreClicked(section, position, item)
        }
    }

    private fun toggleSelection(media: MediaWrapper, forceSelected: Boolean = false) {
        val next = selectedItems.toMutableSet()
        if (forceSelected) next.add(media) else if (!next.add(media)) next.remove(media)
        selectedItems = next
        if (next.isNotEmpty() && actionMode == null) startActionMode()
        if (next.isEmpty()) actionMode?.finish() else actionMode?.invalidate()
    }

    private fun startActionMode() {
        actionMode = activity.startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.action_mode_browser_file, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val selection = selectedItems.toList()
                if (selection.isEmpty()) {
                    mode.finish()
                    return false
                }
                mode.title = activity.getString(R.string.selection_count, selection.size)
                menu.findItem(R.id.action_mode_file_info)?.isVisible = selection.size == 1
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val selection = selectedItems.toList()
                if (selection.isNotEmpty()) {
                    when (item.itemId) {
                        R.id.action_mode_file_play -> MediaUtils.openList(activity, selection, 0)
                        R.id.action_mode_file_append -> MediaUtils.appendMedia(activity, selection)
                        R.id.action_mode_file_add_playlist -> activity.addToPlaylist(selection)
                        R.id.action_mode_file_info -> activity.showMediaInfo(selection.first())
                        else -> {
                            mode.finish()
                            return false
                        }
                    }
                }
                mode.finish()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                selectedItems = emptySet()
            }
        })
    }

    private fun onMoreClicked(section: MainBrowserSection, position: Int, item: MediaLibraryItem) {
        if (actionMode != null || item.itemType != MediaLibraryItem.TYPE_MEDIA) return
        val media = item.asMediaWrapper() ?: return
        activity.lifecycleScope.launch {
            if (media.uri.scheme == "content" || media.uri.scheme == OTG_SCHEME) return@launch
            val flags = FlagSet(ContextOption::class.java).apply {
                val isEmpty = section.model?.isFolderEmpty(media) != false
                if (!isEmpty) add(CTX_PLAY)
                val isFileBrowser = section.isFile && media.uri.scheme == "file"
                val favExists = withContext(Dispatchers.IO) { browserFavRepository.browserFavExists(media.uri) }
                if (favExists) {
                    if (media.uri.scheme.isSchemeFavoriteEditable() && withContext(Dispatchers.IO) { browserFavRepository.isFavNetwork(media.uri) }) {
                        addAll(CTX_FAV_EDIT, CTX_FAV_REMOVE)
                    } else {
                        add(CTX_FAV_REMOVE)
                    }
                } else {
                    add(CTX_FAV_ADD)
                }
                if (isFileBrowser) {
                    if (localViewModel.provider.hasMedias(media)) add(CTX_ADD_FOLDER_PLAYLIST)
                    if (localViewModel.provider.hasSubfolders(media)) add(CTX_ADD_FOLDER_AND_SUB_PLAYLIST)
                }
            }
            if (flags.isNotEmpty()) {
                contextItems = section.items
                showContext(activity, this@MainBrowserScreenController, position, media, flags)
            }
        }
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        val media = contextItems.getOrNull(position) as? MediaWrapper ?: return
        when (option) {
            CTX_PLAY -> MediaUtils.openMedia(activity, media)
            CTX_FAV_ADD -> activity.lifecycleScope.launch {
                if (media.uri.scheme == "file") browserFavRepository.addLocalFavItem(media.uri, media.title, media.artworkURL)
                else browserFavRepository.addNetworkFavItem(media.uri, media.title, media.artworkURL)
            }
            CTX_FAV_REMOVE -> activity.lifecycleScope.launch(Dispatchers.IO) { browserFavRepository.deleteBrowserFav(media.uri) }
            CTX_ADD_FOLDER_PLAYLIST -> activity.addToPlaylistAsync(media.uri.toString(), false, media.title)
            CTX_ADD_FOLDER_AND_SUB_PLAYLIST -> activity.addToPlaylistAsync(media.uri.toString(), true, media.title)
            CTX_FAV_EDIT -> activity.showNetworkServerComposeDialog(media)
            else -> {}
        }
    }

    private fun updateRefreshing() {
        activity.refreshing = localLoading || favoritesLoading || networkLoading
    }

    private fun hideFloatingActionButtons() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = true
            fab.hide()
        }
    }

    private fun MediaLibraryItem.asMediaWrapper(): MediaWrapper? {
        return when (this) {
            is MediaWrapper -> this
            is Storage -> MLServiceLocator.getAbstractMediaWrapper(uri).apply { type = MediaWrapper.TYPE_DIR }
            else -> null
        }
    }

    private fun MediaWrapper.isMultiSelectable() =
        type == MediaWrapper.TYPE_AUDIO || type == MediaWrapper.TYPE_VIDEO || type == MediaWrapper.TYPE_DIR

    private class BrowserFavoritesFactory(private val activity: MainActivity) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BrowserFavoritesModel(activity.applicationContext) as T
        }
    }
}

private data class MainBrowserSection(
    val title: String,
    val items: List<MediaLibraryItem>,
    val loading: Boolean,
    val emptyText: String,
    val isFile: Boolean,
    val model: BrowserModel?,
    val visibleWhenEmpty: Boolean = true
)

@Composable
private fun MainBrowserScreenContent(
    displayInList: Boolean,
    localItems: List<MediaLibraryItem>,
    favoriteItems: List<MediaLibraryItem>,
    networkItems: List<MediaLibraryItem>,
    localLoading: Boolean,
    favoritesLoading: Boolean,
    networkLoading: Boolean,
    canReadStorage: Boolean,
    networkEnabled: Boolean,
    networkConnected: Boolean,
    lanAllowed: Boolean,
    localModel: BrowserModel,
    networkModel: BrowserModel,
    selectedItems: Set<MediaWrapper>,
    onItemClicked: (MediaLibraryItem) -> Unit,
    onItemLongClicked: (MainBrowserSection, Int, MediaLibraryItem) -> Unit,
    onMoreClicked: (MainBrowserSection, Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val sections = listOf(
        MainBrowserSection(
            title = stringResource(R.string.favorites),
            items = favoriteItems,
            loading = favoritesLoading,
            emptyText = stringResource(R.string.no_favorite),
            isFile = true,
            model = null,
            visibleWhenEmpty = false
        ),
        MainBrowserSection(
            title = stringResource(R.string.browser_storages),
            items = if (canReadStorage) localItems else emptyList(),
            loading = canReadStorage && localLoading,
            emptyText = if (canReadStorage) stringResource(R.string.nomedia) else stringResource(R.string.permission_media),
            isFile = true,
            model = localModel
        ),
        MainBrowserSection(
            title = stringResource(R.string.network_browsing),
            items = networkItems,
            loading = networkLoading || (networkEnabled && networkConnected && lanAllowed && networkItems.isEmpty()),
            emptyText = networkEmptyText(networkEnabled, networkConnected, lanAllowed),
            isFile = false,
            model = networkModel
        )
    ).filter { it.visibleWhenEmpty || it.items.isNotEmpty() || it.loading }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.backgroundDefault
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundDefault),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            sections.forEach { section ->
                item(key = "${section.title}-header") {
                    Text(
                        text = section.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.headerBackground)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = colors.listTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (section.loading && section.items.isEmpty()) {
                    item(key = "${section.title}-loading") {
                        BrowserEmptyState(loading = true, text = stringResource(R.string.loading))
                    }
                } else if (section.items.isEmpty()) {
                    item(key = "${section.title}-empty") {
                        BrowserEmptyState(loading = false, text = section.emptyText)
                    }
                } else if (displayInList) {
                    items(section.items, key = { "${section.title}-${it.stableBrowserKey()}" }) { item ->
                        val position = section.items.indexOf(item)
                        BrowserListRow(
                            item = item,
                            selected = item is MediaWrapper && selectedItems.contains(item),
                            onClick = { onItemClicked(item) },
                            onLongClick = { onItemLongClicked(section, position, item) },
                            onMoreClick = { onMoreClicked(section, position, item) }
                        )
                    }
                } else {
                    items(section.items.chunked(2), key = { row -> "${section.title}-${row.joinToString { it.stableBrowserKey() }}" }) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val position = section.items.indexOf(item)
                                BrowserCard(
                                    item = item,
                                    selected = item is MediaWrapper && selectedItems.contains(item),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onItemClicked(item) },
                                    onLongClick = { onItemLongClicked(section, position, item) },
                                    onMoreClick = { onMoreClicked(section, position, item) }
                                )
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                item(key = "${section.title}-spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun networkEmptyText(enabled: Boolean, connected: Boolean, lanAllowed: Boolean): String {
    return when {
        !enabled -> stringResource(R.string.network_disabled)
        !connected -> stringResource(R.string.network_connection_needed)
        lanAllowed -> stringResource(R.string.network_shares_discovery)
        else -> stringResource(R.string.network_connection_needed)
    }
}

@Composable
private fun BrowserEmptyState(loading: Boolean, text: String) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (loading) CircularProgressIndicator(color = colors.primary)
        Text(
            text = text,
            color = colors.listSubtitle,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserListRow(
    item: MediaLibraryItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.subtleSelection else colors.backgroundDefault)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrowserItemIcon(item)
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title.orEmpty(),
                color = colors.listTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitleText()?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = colors.listSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onMoreClick) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.more),
                tint = colors.listSubtitle
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserCard(
    item: MediaLibraryItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.subtleSelection else colors.backgroundDefaultDarker)
            .border(1.dp, colors.listSubtitle.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrowserItemIcon(item, large = true)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = stringResource(R.string.more),
                    tint = colors.listSubtitle
                )
            }
        }
        Text(
            text = item.title.orEmpty(),
            color = colors.listTitle,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        item.subtitleText()?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = colors.listSubtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BrowserItemIcon(item: MediaLibraryItem, large: Boolean = false) {
    val colors = VLCThemeDefaults.colors
    val icon = when (val media = item as? MediaWrapper) {
        null -> R.drawable.ic_folder
        else -> when (media.type) {
            MediaWrapper.TYPE_AUDIO -> R.drawable.ic_song
            MediaWrapper.TYPE_VIDEO -> R.drawable.ic_video
            MediaWrapper.TYPE_SUBTITLE -> R.drawable.ic_subtitles
            MediaWrapper.TYPE_DIR -> R.drawable.ic_folder
            else -> R.drawable.ic_unknown
        }
    }
    Box(
        modifier = Modifier
            .size(if (large) 48.dp else 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundDefault),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(if (large) 32.dp else 28.dp),
            tint = colors.primary
        )
    }
}

private fun MediaLibraryItem.subtitleText(): String? {
    val descriptionText = description?.toString()
    if (!descriptionText.isNullOrBlank()) return descriptionText
    return when (this) {
        is MediaWrapper -> location
        is Storage -> uri.toString()
        else -> null
    }
}

private fun MediaLibraryItem.stableBrowserKey(): String {
    return when (this) {
        is MediaWrapper -> location ?: uri.toString()
        is Storage -> uri.toString()
        else -> "$itemType-${title.orEmpty()}-${hashCode()}"
    }
}
