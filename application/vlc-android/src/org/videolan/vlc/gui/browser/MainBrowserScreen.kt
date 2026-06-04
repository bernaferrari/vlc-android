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
import android.content.DialogInterface
import android.net.Uri
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.medialibrary.media.MediaWrapperImpl
import org.videolan.medialibrary.media.Storage
import org.videolan.resources.AndroidDevices
import org.videolan.resources.util.getFromMl
import org.videolan.tools.BROWSER_DISPLAY_IN_CARDS
import org.videolan.tools.BROWSER_SHOW_HIDDEN_FILES
import org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.tools.KEY_MEDIALIBRARY_SCAN
import org.videolan.tools.KEY_NAVIGATOR_SCREEN_UNSTABLE
import org.videolan.tools.KEY_QUICK_PLAY
import org.videolan.tools.KEY_QUICK_PLAY_DEFAULT
import org.videolan.tools.KeyHelper
import org.videolan.tools.ML_SCAN_ON
import org.videolan.tools.NetworkMonitor
import org.videolan.tools.Settings
import org.videolan.tools.containsPath
import org.videolan.tools.putSingle
import org.videolan.tools.removeFileScheme
import org.videolan.tools.sanitizePath
import org.videolan.tools.stripTrailingSlash
import org.videolan.resources.util.canReadStorage
import org.videolan.vlc.MediaParsingService
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBrowserItemCard
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.dialogs.CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER
import org.videolan.vlc.gui.dialogs.CONFIRM_DELETE_DIALOG_RESULT_DEFAULT_VALUE
import org.videolan.vlc.gui.dialogs.CURRENT_SORT
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.DEFAULT_ACTIONS
import org.videolan.vlc.gui.dialogs.DISPLAY_IN_CARDS
import org.videolan.vlc.gui.dialogs.SHOW_HIDDEN_FILES
import org.videolan.vlc.gui.dialogs.SHOW_ONLY_MULTIMEDIA_FILES
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showDisplaySettingsComposeDialog
import org.videolan.vlc.gui.dialogs.showNetworkServerComposeDialog
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.helpers.DefaultPlaybackAction
import org.videolan.vlc.gui.helpers.DefaultPlaybackActionMediaType
import org.videolan.vlc.gui.helpers.FloatingActionButtonBehavior
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylistAsync
import org.videolan.vlc.gui.helpers.UiTools.showMediaInfo
import org.videolan.vlc.gui.helpers.UiTools.snacker
import org.videolan.vlc.gui.helpers.hf.OTG_SCHEME
import org.videolan.vlc.gui.helpers.hf.OtgAccess
import org.videolan.vlc.gui.helpers.hf.requestOtgRoot
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.repository.DirectoryRepository
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_SCANNED
import org.videolan.vlc.util.ContextOption.CTX_ADD_FOLDER_AND_SUB_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_ADD_FOLDER_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_BAN_FOLDER
import org.videolan.vlc.util.ContextOption.CTX_CUSTOM_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_DOWNLOAD_SUBTITLES
import org.videolan.vlc.util.ContextOption.CTX_FAV_ADD
import org.videolan.vlc.util.ContextOption.CTX_FAV_EDIT
import org.videolan.vlc.util.ContextOption.CTX_FAV_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_INFORMATION
import org.videolan.vlc.util.ContextOption.CTX_PLAY
import org.videolan.vlc.util.ContextOption.CTX_PLAY_ALL
import org.videolan.vlc.util.ContextOption.CTX_PLAY_AS_AUDIO
import org.videolan.vlc.util.ContextOption.CTX_PLAY_NEXT
import org.videolan.vlc.util.ContextOption.CTX_QUICK_PLAY
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.isSchemeFavoriteEditable
import org.videolan.vlc.util.isSchemeNetwork
import org.videolan.vlc.viewmodels.DisplaySettingsViewModel
import org.videolan.vlc.viewmodels.browser.BrowserFavoritesModel
import org.videolan.vlc.viewmodels.browser.BrowserModel
import org.videolan.vlc.viewmodels.browser.NetworkModel
import org.videolan.vlc.viewmodels.browser.TYPE_FILE
import org.videolan.vlc.viewmodels.browser.TYPE_NETWORK
import org.videolan.vlc.viewmodels.browser.TYPE_STORAGE
import java.io.File

class MainBrowserScreenController(
    private val activity: MainActivity,
    forEspresso: ArrayList<MediaLibraryItem>?
) : CtxActionReceiver, DefaultLifecycleObserver {

    private val settings = Settings.getInstance(activity)
    private val browserFavRepository = BrowserFavRepository.getInstance(activity)
    private val networkMonitor = NetworkMonitor.getInstance(activity)
    private val displayInListKey = "main_browser_display_mode"

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
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
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
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
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
                UiTools.addBrowserFileActionModeMenu(menu)
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

class SecondaryStorageBrowserScreenController(
    private val activity: SecondaryActivity,
    private val onboarding: Boolean
) : CtxActionReceiver, DefaultLifecycleObserver {

    private val settings = Settings.getInstance(activity)
    private val networkMonitor = NetworkMonitor.getInstance(activity)
    private val localViewModel = ViewModelProvider(
        activity,
        BrowserModel.Factory(activity, url = null, type = TYPE_STORAGE)
    )["secondary-storage-local", BrowserModel::class.java]
    private val networkViewModel = ViewModelProvider(
        activity,
        NetworkModel.Factory(activity, url = null)
    )["secondary-storage-network", NetworkModel::class.java]
    private val directoryRepository = DirectoryRepository.getInstance(activity)

    private var localItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var networkItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var localLoading by mutableStateOf(false)
    private var networkLoading by mutableStateOf(false)
    private var mediaDirectories by mutableStateOf<List<String>>(emptyList())
    private var customDirectories by mutableStateOf<List<String>>(emptyList())
    private var bannedFolders by mutableStateOf<List<String>>(emptyList())
    private var contextItems: List<MediaLibraryItem> = emptyList()
    private var loaded = false
    private var visible = false
    private var addDirectoryDialog: AlertDialog? = null

    private val rootsCallback = object : org.videolan.medialibrary.interfaces.RootsEventsCb {
        override fun onRootBanned(entryPoint: String, success: Boolean) = refreshStorageState()
        override fun onRootUnbanned(entryPoint: String, success: Boolean) = refreshStorageState()
        override fun onRootAdded(entryPoint: String, success: Boolean) = refreshStorageState()
        override fun onRootRemoved(entrypoint: String, success: Boolean) = refreshStorageState()
        override fun onDiscoveryStarted() {}
        override fun onDiscoveryProgress(entryPoint: String) {}
        override fun onDiscoveryCompleted() = refreshStorageState()
        override fun onDiscoveryFailed(entryPoint: String) {}
    }

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
        networkViewModel.dataset.observe(activity) { items ->
            networkItems = items.orEmpty().filterIsInstance<MediaWrapper>().filter { it.uri?.scheme == "smb" }
        }
        networkViewModel.loading.observe(activity) { loading ->
            networkLoading = loading == true
            updateRefreshing()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        addDirectoryDialog?.dismiss()
        activity.lifecycle.removeObserver(this)
    }

    fun onVisible() {
        if (visible) return
        visible = true
        activity.supportActionBar?.setTitle(if (onboarding) R.string.medialibrary_directories else R.string.directories_summary)
        activity.setTabLayoutVisibility(false)
        hideFloatingActionButtons()
        Medialibrary.getInstance().addRootsEventsCb(rootsCallback)
        if (!loaded) {
            loaded = true
            refresh()
        } else {
            refreshStorageState()
        }
        activity.invalidateOptionsMenu()
    }

    fun onHidden() {
        if (!visible) return
        visible = false
        Medialibrary.getInstance().removeRootsEventsCb(rootsCallback)
        addDirectoryDialog?.dismiss()
    }

    fun refresh() {
        localViewModel.browseRoot()
        networkViewModel.browseRoot()
        refreshStorageState()
        updateRefreshing()
    }

    fun prepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.ml_menu_custom_dir)?.isVisible = !onboarding
        menu.findItem(R.id.ml_menu_refresh)?.isVisible = false
        menu.findItem(R.id.ml_menu_add_playlist)?.isVisible = false
        menu.findItem(R.id.ml_menu_display_options)?.isVisible = false
        menu.findItem(R.id.ml_menu_filter)?.isVisible = false
        menu.findItem(R.id.play_all)?.isVisible = false
        menu.findItem(R.id.shuffle_all)?.isVisible = false
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.ml_menu_custom_dir) return false
        showAddDirectoryDialog()
        return true
    }

    @Composable
    fun Content() {
        StorageRootScreenContent(
            localItems = localItems,
            networkItems = networkItems,
            localLoading = localLoading,
            networkLoading = networkLoading,
            networkConnected = networkMonitor.connected,
            lanAllowed = networkMonitor.lanAllowed,
            storageState = ::storageState,
            isCustomDirectory = ::isCustomDirectory,
            onItemClicked = ::openStorage,
            onCheckedChange = ::onCheckedChange,
            onMoreClicked = ::onMoreClicked
        )
    }

    private fun showAddDirectoryDialog() {
        val input = AppCompatEditText(activity).apply {
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        addDirectoryDialog = AlertDialog.Builder(activity)
            .setTitle(R.string.add_custom_path)
            .setMessage(R.string.add_custom_path_description)
            .setView(input)
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->
                val path = input.text.toString().trim { it <= ' ' }
                val file = File(path)
                if (!file.exists() || !file.isDirectory) {
                    snacker(activity, activity.getString(R.string.directorynotfound, path))
                    return@OnClickListener
                }
                activity.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { localViewModel.addCustomDirectory(file.canonicalPath).join() }
                    localViewModel.browseRoot()
                    refreshStorageState()
                }
            })
            .show()
    }

    private fun openStorage(item: MediaLibraryItem) {
        val media = item.asStorageMediaWrapper() ?: return
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, media)
            putExtra(KEY_IN_MEDIALIB, storageState(item) == ToggleableState.On)
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun onCheckedChange(item: MediaLibraryItem, checked: Boolean) {
        val mrl = item.storageMrl() ?: return
        if (checked && mrl.contains("file://") && !canReadStorage(activity)) {
            Permissions.showStoragePermissionDialog(activity, false)
            return
        }
        if (onboarding) {
            val path = mrl.sanitizePath()
            MediaParsingService.preselectedStorages.removeAll { it.startsWith(path) }
            if (checked) MediaParsingService.preselectedStorages.add(path)
            refreshStorageState()
            return
        }
        if (checked) {
            MedialibraryUtils.addDir(mrl, activity.applicationContext)
            if (settings.getInt(KEY_MEDIALIBRARY_SCAN, -1) != ML_SCAN_ON) settings.putSingle(KEY_MEDIALIBRARY_SCAN, ML_SCAN_ON)
        } else {
            MedialibraryUtils.removeDir(mrl)
        }
        refreshStorageState()
    }

    private fun onMoreClicked(sectionItems: List<MediaLibraryItem>, position: Int, item: MediaLibraryItem) {
        if (!isCustomDirectory(item)) return
        contextItems = sectionItems
        showContext(activity, this, position, item, FlagSet(ContextOption::class.java).apply { add(CTX_CUSTOM_REMOVE) })
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        if (option != CTX_CUSTOM_REMOVE) return
        val item = contextItems.getOrNull(position) ?: return
        val path = item.storagePath()?.stripTrailingSlash() ?: return
        localViewModel.deleteCustomDirectory(path)
        localViewModel.remove(item)
        refreshStorageState()
        activity.updateLib()
    }

    private fun refreshStorageState() {
        activity.lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) {
                if (!Medialibrary.getInstance().isInitiated) {
                    MediaParsingService.preselectedStorages.toList()
                } else {
                    Medialibrary.getInstance().foldersList.toList()
                }
            }
            val custom = withContext(Dispatchers.IO) {
                directoryRepository.getCustomDirectories().map { it.path.stripTrailingSlash() }
            }
            val banned = withContext(Dispatchers.IO) {
                Medialibrary.getInstance().bannedFolders().toList()
            }
            mediaDirectories = folders.map { Uri.decode(it.removeFileScheme()) }
            customDirectories = custom
            bannedFolders = banned
        }
    }

    private fun storageState(item: MediaLibraryItem): ToggleableState {
        val storagePath = item.storagePath() ?: return ToggleableState.Off
        val mrl = item.storageMrl() ?: return ToggleableState.Off
        if (MedialibraryUtils.isBanned(mrl, bannedFolders)) return ToggleableState.Off
        if (mediaDirectories.containsPath(storagePath)) return ToggleableState.On
        if (mediaDirectories.any { it.sanitizePath().startsWith(storagePath.sanitizePath()) }) return ToggleableState.Indeterminate
        return ToggleableState.Off
    }

    private fun isCustomDirectory(item: MediaLibraryItem): Boolean {
        return item.storagePath()?.stripTrailingSlash()?.let { customDirectories.contains(it) } == true
    }

    private fun updateRefreshing() {
        if (visible) activity.invalidateOptionsMenu()
    }

    private fun hideFloatingActionButtons() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = true
            fab.hide()
        }
    }

    private fun MediaLibraryItem.asStorageMediaWrapper(): MediaWrapper? {
        return when (this) {
            is Storage -> MLServiceLocator.getAbstractMediaWrapper(uri)
            is MediaWrapper -> MLServiceLocator.getAbstractMediaWrapper(uri)
            else -> null
        }?.apply { type = MediaWrapper.TYPE_DIR }
    }

    private fun MediaLibraryItem.storageMrl(): String? {
        return when (this) {
            is Storage -> uri.toString()
            is MediaWrapper -> uri.toString()
            else -> null
        }
    }

    private fun MediaLibraryItem.storagePath(): String? {
        val uri = when (this) {
            is Storage -> uri
            is MediaWrapper -> uri
            else -> return null
        }
        val raw = if (uri.scheme == "file") uri.path.orEmpty() else Uri.decode(uri.toString())
        return if (raw.endsWith("/")) raw else "$raw/"
    }
}

class SecondaryFileBrowserScreenController(
    private val activity: SecondaryActivity,
    private val currentMedia: MediaWrapper,
    private val jumpTo: MediaWrapper?,
    private val storageBrowser: Boolean,
    private val scannedDirectory: Boolean
) : CtxActionReceiver, DefaultLifecycleObserver {

    private val settings = Settings.getInstance(activity)
    private val browserFavRepository = BrowserFavRepository.getInstance(activity)
    private val networkMonitor = NetworkMonitor.getInstance(activity)
    private val displaySettingsViewModel = ViewModelProvider(activity)[DisplaySettingsViewModel::class.java]
    private val browserType = when {
        storageBrowser -> TYPE_STORAGE
        currentMedia.uri.scheme.isSchemeNetwork() -> TYPE_NETWORK
        else -> TYPE_FILE
    }
    private val isStorageBrowser = browserType == TYPE_STORAGE
    private val isNetworkBrowser = browserType == TYPE_NETWORK
    private val isLocalFileBrowser = browserType == TYPE_FILE && currentMedia.uri.scheme == "file"
    private val viewModel: BrowserModel = when (browserType) {
        TYPE_NETWORK -> ViewModelProvider(
            activity,
            NetworkModel.Factory(activity, currentMedia.location)
        )["secondary-file-browser-network-${currentMedia.location}", NetworkModel::class.java]
        else -> ViewModelProvider(
            activity,
            BrowserModel.Factory(activity, url = currentMedia.location, type = browserType)
        )["secondary-file-browser-$browserType-${currentMedia.location}", BrowserModel::class.java]
    }

    private var items by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var loading by mutableStateOf(false)
    private var displayInCards by mutableStateOf(settings.getBoolean(BROWSER_DISPLAY_IN_CARDS, false))
    private var selectedItems by mutableStateOf<Set<MediaWrapper>>(emptySet())
    private var mediaDirectories by mutableStateOf<List<String>>(emptyList())
    private var bannedFolders by mutableStateOf<List<String>>(emptyList())
    private var loaded = false
    private var visible = false
    private var contextItems: List<MediaLibraryItem> = emptyList()
    private var actionMode: ActionMode? = null
    private var addDirectoryDialog: AlertDialog? = null

    private val rootsCallback = object : org.videolan.medialibrary.interfaces.RootsEventsCb {
        override fun onRootBanned(entryPoint: String, success: Boolean) = refreshStorageState()
        override fun onRootUnbanned(entryPoint: String, success: Boolean) = refreshStorageState()
        override fun onRootAdded(entryPoint: String, success: Boolean) = refreshStorageState()
        override fun onRootRemoved(entrypoint: String, success: Boolean) = refreshStorageState()
        override fun onDiscoveryStarted() {}
        override fun onDiscoveryProgress(entryPoint: String) {}
        override fun onDiscoveryCompleted() = refreshStorageState()
        override fun onDiscoveryFailed(entryPoint: String) {}
    }

    init {
        activity.lifecycle.addObserver(this)
        viewModel.dataset.observe(activity) { dataset ->
            items = dataset?.toList().orEmpty()
            selectedItems = selectedItems.filter { selected -> items.any { it is MediaWrapper && it.location == selected.location } }.toSet()
            actionMode?.invalidate()
            activity.invalidateOptionsMenu()
        }
        viewModel.loading.observe(activity) { isLoading ->
            loading = isLoading == true
            updateRefreshing()
        }
        viewModel.getDescriptionUpdate().observe(activity) {
            items = viewModel.dataset.value?.toList().orEmpty()
        }
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                displaySettingsViewModel.settingChangeFlow.collect { change ->
                    if (!visible || change.key == "init") return@collect
                    onDisplaySettingChanged(change.key, change.value)
                    displaySettingsViewModel.consume()
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        addDirectoryDialog?.dismiss()
        actionMode?.finish()
        activity.lifecycle.removeObserver(this)
    }

    fun onVisible() {
        if (visible) return
        visible = true
        activity.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_up)
        activity.supportActionBar?.title = title()
        activity.supportActionBar?.subtitle = subtitle()
        activity.setTabLayoutVisibility(false)
        hideFloatingActionButtons()
        if (isStorageBrowser) {
            Medialibrary.getInstance().addRootsEventsCb(rootsCallback)
            refreshStorageState()
        }
        if (!loaded) loaded = true else refresh()
        activity.invalidateOptionsMenu()
    }

    fun onHidden() {
        if (!visible) return
        visible = false
        actionMode?.finish()
        addDirectoryDialog?.dismiss()
        if (isStorageBrowser) Medialibrary.getInstance().removeRootsEventsCb(rootsCallback)
        viewModel.stop()
    }

    fun refresh() {
        if (isNetworkBrowser && !networkMonitor.connected) {
            viewModel.dataset.clear()
        } else {
            viewModel.refresh()
        }
        if (isStorageBrowser) refreshStorageState()
        updateRefreshing()
    }

    fun hideRenderers() = isStorageBrowser

    fun prepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.ml_menu_display_options)?.isVisible = !isStorageBrowser
        menu.findItem(R.id.ml_menu_filter)?.isVisible = false
        menu.findItem(R.id.ml_menu_sortby)?.isVisible = false
        menu.findItem(R.id.ml_menu_sortby_media_number)?.isVisible = false
        menu.findItem(R.id.ml_menu_add_playlist)?.isVisible = !isStorageBrowser
        menu.findItem(R.id.folder_add_playlist)?.isVisible = !isStorageBrowser && currentFolderHasMedias()
        menu.findItem(R.id.play_all)?.isVisible = !isStorageBrowser && playableItems().isNotEmpty()
        menu.findItem(R.id.ml_menu_custom_dir)?.isVisible = isStorageBrowser
        menu.findItem(R.id.ml_menu_refresh)?.isVisible = !isStorageBrowser && Permissions.canReadStorage(activity)
        menu.findItem(R.id.ml_menu_save)?.let { item ->
            item.isVisible = !isStorageBrowser
            activity.lifecycleScope.launch {
                val isFavorite = browserFavRepository.browserFavExists(currentMedia.uri)
                item.setIcon(if (isFavorite) R.drawable.ic_fav_remove else R.drawable.ic_fav_add)
                item.setTitle(if (isFavorite) R.string.favorites_remove else R.string.favorites_add)
            }
        }
        menu.findItem(R.id.ml_menu_scan)?.let { item ->
            activity.lifecycleScope.launch {
                val canScan = !isStorageBrowser &&
                        (currentMedia.uri.scheme == "file" || currentMedia.uri.scheme == "smb") &&
                        withContext(Dispatchers.IO) { !MedialibraryUtils.isScanned(currentMedia.uri.toString()) }
                item.isVisible = canScan
            }
        }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ml_menu_display_options -> {
                showDisplaySettings()
                true
            }
            R.id.ml_menu_save -> {
                toggleCurrentFavorite()
                true
            }
            R.id.ml_menu_scan -> {
                addToScannedFolders(currentMedia)
                item.isVisible = false
                true
            }
            R.id.folder_add_playlist -> {
                activity.addToPlaylistAsync(currentMedia.uri.toString(), false, currentMedia.title)
                true
            }
            R.id.subfolders_add_playlist -> {
                activity.addToPlaylistAsync(currentMedia.uri.toString(), true, currentMedia.title)
                true
            }
            R.id.play_all -> {
                playAll(null)
                true
            }
            R.id.ml_menu_custom_dir -> {
                showAddDirectoryDialog()
                true
            }
            else -> false
        }
    }

    @Composable
    fun Content() {
        if (isStorageBrowser) {
            StorageFolderScreenContent(
                items = items,
                loading = loading,
                storageState = ::storageState,
                checkEnabled = !scannedDirectory,
                onItemClicked = ::openStorageDirectory,
                onItemLongClicked = ::toggleStorageBan,
                onCheckedChange = ::onStorageCheckedChange
            )
        } else {
            NestedBrowserScreenContent(
                displayInCards = displayInCards,
                items = items,
                loading = loading,
                emptyText = emptyText(),
                jumpToLocation = jumpTo?.location,
                selectedItems = selectedItems,
                onItemClicked = ::onItemClicked,
                onItemLongClicked = ::onItemLongClicked,
                onMoreClicked = ::onMoreClicked
            )
        }
    }

    fun onRenameResult(media: MediaLibraryItem, name: String) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val mw = media as? MediaWrapper ?: return@launch
            val file = mw.uri.path?.let { File(it) } ?: return@launch
            if (file.exists()) file.parentFile?.let { parent -> file.renameTo(File(parent, name)) }
            withContext(Dispatchers.Main) { viewModel.refresh() }
        }
    }

    fun onDeleteResult(items: List<MediaLibraryItem>, type: Int) {
        if (type == CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER) {
            banFolders(items)
            return
        }
        items.forEach { item ->
            MediaUtils.deleteItem(activity, item) { failed ->
                snacker(activity, activity.getString(R.string.msg_delete_failed, failed.title))
            }
            viewModel.remove(item)
        }
    }

    private fun title(): String {
        val mrl = currentMedia.location
        return when {
            isStorageBrowser && currentMedia.title.isNullOrBlank() -> activity.getString(R.string.directories_summary)
            AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY == mrl.removeFileScheme() -> activity.getString(R.string.internal_memory)
            !currentMedia.title.isNullOrBlank() -> currentMedia.title
            else -> FileUtils.getFileNameFromPath(mrl)
        }
    }

    private fun subtitle(): String? {
        if (isStorageBrowser) return null
        var mrl = currentMedia.location.removeFileScheme()
        if (mrl.isBlank()) return null
        if (isLocalFileBrowser && mrl.startsWith(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY)) {
            mrl = activity.getString(R.string.internal_memory) + mrl.substring(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY.length)
        }
        return Uri.decode(mrl).replace("://".toRegex(), " ").replace("/".toRegex(), " > ")
    }

    private fun emptyText(): String {
        return when {
            isNetworkBrowser && !networkMonitor.connected -> activity.getString(R.string.network_connection_needed)
            isNetworkBrowser -> activity.getString(R.string.network_empty)
            !Permissions.canReadStorage(activity) -> activity.getString(R.string.permission_media)
            else -> activity.getString(R.string.nomedia)
        }
    }

    private fun onItemClicked(position: Int, item: MediaLibraryItem) {
        val media = item.asMediaWrapper() ?: return
        if (KeyHelper.isShiftPressed || KeyHelper.isCtrlPressed) {
            onItemLongClicked(position, item)
            return
        }
        if (actionMode != null) {
            if (media.isMultiSelectable()) toggleSelection(media)
            return
        }
        media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
        if (media.type == MediaWrapper.TYPE_DIR) openDirectory(media) else playMedia(position, media)
    }

    private fun onItemLongClicked(position: Int, item: MediaLibraryItem) {
        val media = item.asMediaWrapper() ?: return
        if (media.isMultiSelectable()) {
            toggleSelection(media, forceSelected = true)
        } else {
            onMoreClicked(position, item)
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
                UiTools.addBrowserFileActionModeMenu(menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val selection = selectedItems.toList()
                if (selection.isEmpty()) {
                    mode.finish()
                    return false
                }
                mode.title = activity.getString(R.string.selection_count, selection.size)
                val single = selection.size == 1
                val type = selection.firstOrNull()?.type ?: -1
                menu.findItem(R.id.action_mode_file_info)?.isVisible = single &&
                        isLocalFileBrowser &&
                        (type == MediaWrapper.TYPE_AUDIO || type == MediaWrapper.TYPE_VIDEO)
                menu.findItem(R.id.action_mode_file_append)?.isVisible = PlaylistManager.hasMedia()
                menu.findItem(R.id.action_mode_file_delete)?.isVisible = isLocalFileBrowser
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val selection = selectedItems.toList()
                if (selection.isNotEmpty()) {
                    when (item.itemId) {
                        R.id.action_mode_file_play -> activity.lifecycleScope.launch { MediaUtils.openList(activity, selection.map { getMediaWithMeta(it) }, 0) }
                        R.id.action_mode_file_append -> activity.lifecycleScope.launch { MediaUtils.appendMedia(activity, selection.map { getMediaWithMeta(it) }) }
                        R.id.action_mode_file_add_playlist -> activity.addToPlaylist(selection)
                        R.id.action_mode_file_info -> activity.showMediaInfo(selection.first())
                        R.id.action_mode_file_delete -> removeItems(selection)
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

    private fun onMoreClicked(position: Int, item: MediaLibraryItem) {
        if (actionMode != null || item.itemType != MediaLibraryItem.TYPE_MEDIA) return
        val media = item.asMediaWrapper() ?: return
        activity.lifecycleScope.launch {
            if (media.uri.scheme == "content" || media.uri.scheme == OTG_SCHEME) return@launch
            val flags = FlagSet(ContextOption::class.java).apply {
                add(CTX_RENAME)
                if (isLocalFileBrowser) {
                    add(CTX_DELETE)
                    if (settings.getBoolean(KEY_QUICK_PLAY, false)) add(CTX_QUICK_PLAY)
                }
                if (media.type == MediaWrapper.TYPE_DIR) {
                    if (isLocalFileBrowser) add(CTX_BAN_FOLDER)
                    if (!viewModel.isFolderEmpty(media)) add(CTX_PLAY)
                    if (isLocalFileBrowser || isNetworkBrowser) {
                        if (browserFavRepository.browserFavExists(media.uri)) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
                    }
                    if (isLocalFileBrowser && !MedialibraryUtils.isScanned(media.uri.toString())) add(CTX_ADD_SCANNED)
                    if (isLocalFileBrowser) {
                        add(CTX_APPEND)
                        if (viewModel.provider.hasMedias(media)) add(CTX_ADD_FOLDER_PLAYLIST)
                        if (viewModel.provider.hasSubfolders(media)) add(CTX_ADD_FOLDER_AND_SUB_PLAYLIST)
                    }
                } else {
                    val isVideo = media.type == MediaWrapper.TYPE_VIDEO
                    val isAudio = media.type == MediaWrapper.TYPE_AUDIO
                    val isMedia = isVideo || isAudio
                    if (isMedia) addAll(CTX_ADD_TO_PLAYLIST, CTX_APPEND, CTX_INFORMATION, CTX_PLAY_ALL)
                    if (!isAudio && isMedia) add(CTX_PLAY_AS_AUDIO)
                    if (!isMedia) add(CTX_PLAY)
                    if (isVideo) add(CTX_DOWNLOAD_SUBTITLES)
                    if ((isVideo || media.isPodcast) && media.seen > 0L) add(ContextOption.CTX_MARK_AS_UNPLAYED)
                    if ((isVideo || media.isPodcast) && media.seen == 0L) add(ContextOption.CTX_MARK_AS_PLAYED)
                }
                add(CTX_PLAY_NEXT)
            }
            if (flags.isNotEmpty()) {
                contextItems = items
                showContext(activity, this@SecondaryFileBrowserScreenController, position, media, flags)
            }
        }
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        val media = contextItems.getOrNull(position) as? MediaWrapper ?: return
        when (option) {
            CTX_PLAY -> activity.lifecycleScope.launch { MediaUtils.openMedia(activity, getMediaWithMeta(media)) }
            CTX_QUICK_PLAY -> activity.lifecycleScope.launch {
                val mediaWithMeta = getMediaWithMeta(media)
                mediaWithMeta.addFlags(MediaWrapper.MEDIA_NO_PARSE)
                MediaUtils.openMedia(activity, mediaWithMeta)
            }
            CTX_PLAY_ALL -> {
                media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                playAll(media)
            }
            CTX_APPEND -> activity.lifecycleScope.launch { MediaUtils.appendMedia(activity, getMediaWithMeta(media)) }
            CTX_PLAY_NEXT -> activity.lifecycleScope.launch { MediaUtils.insertNext(activity, getMediaWithMeta(media)) }
            CTX_DELETE -> removeItems(listOf(media))
            CTX_RENAME -> activity.showRenameComposeDialog(media, true, ::onRenameResult)
            CTX_INFORMATION -> activity.showMediaInfo(media)
            CTX_PLAY_AS_AUDIO -> activity.lifecycleScope.launch {
                media.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                MediaUtils.openMedia(activity, getMediaWithMeta(media))
            }
            CTX_ADD_TO_PLAYLIST -> activity.addToPlaylist(media.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            CTX_DOWNLOAD_SUBTITLES -> MediaUtils.getSubs(activity, media)
            CTX_FAV_ADD -> activity.lifecycleScope.launch {
                if (media.uri.scheme == "file") browserFavRepository.addLocalFavItem(media.uri, media.title, media.artworkURL)
                else browserFavRepository.addNetworkFavItem(media.uri, media.title, media.artworkURL)
                activity.invalidateOptionsMenu()
            }
            CTX_FAV_REMOVE -> activity.lifecycleScope.launch {
                browserFavRepository.deleteBrowserFav(media.uri)
                activity.invalidateOptionsMenu()
            }
            CTX_ADD_SCANNED -> addToScannedFolders(media)
            CTX_BAN_FOLDER -> activity.showConfirmDeleteComposeDialog(
                medias = arrayListOf(media),
                title = activity.getString(R.string.group_ban_folder),
                description = activity.getString(R.string.ban_folder_explanation, activity.getString(R.string.medialibrary_directories)),
                buttonText = activity.getString(R.string.ban_folder),
                resultType = CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER
            ) {
                onDeleteResult(listOf(media), CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER)
            }
            CTX_ADD_FOLDER_PLAYLIST -> activity.addToPlaylistAsync(media.uri.toString(), false, media.title)
            CTX_ADD_FOLDER_AND_SUB_PLAYLIST -> activity.addToPlaylistAsync(media.uri.toString(), true, media.title)
            ContextOption.CTX_MARK_AS_UNPLAYED -> {
                media.setPlayCount(0L)
                media.seen = 0L
                items = viewModel.dataset.value?.toList().orEmpty()
            }
            ContextOption.CTX_MARK_AS_PLAYED -> {
                media.setPlayCount(media.seen + 1L)
                media.seen += 1L
                items = viewModel.dataset.value?.toList().orEmpty()
            }
            else -> {}
        }
    }

    private fun playMedia(position: Int, media: MediaWrapper) {
        activity.lifecycleScope.launch {
            val mediaWithMeta = getMediaWithMeta(media).applyQuickPlayDefault()
            when (DefaultPlaybackActionMediaType.FILE.getCurrentPlaybackAction(settings)) {
                DefaultPlaybackAction.PLAY -> MediaUtils.openMedia(activity, mediaWithMeta)
                DefaultPlaybackAction.ADD_TO_QUEUE -> MediaUtils.appendMedia(activity, mediaWithMeta)
                DefaultPlaybackAction.INSERT_NEXT -> MediaUtils.insertNext(activity, mediaWithMeta)
                DefaultPlaybackAction.PLAY_ALL -> openPlayableList(media, position)
            }
        }
    }

    private fun openDirectory(media: MediaWrapper) {
        viewModel.saveList(media)
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, media)
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun openStorageDirectory(item: MediaLibraryItem) {
        val media = item.asStorageMediaWrapper() ?: return
        viewModel.saveList(media)
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, media)
            putExtra(KEY_IN_MEDIALIB, scannedDirectory || storageState(item) == ToggleableState.On)
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun onStorageCheckedChange(item: MediaLibraryItem, checked: Boolean) {
        if (scannedDirectory) return
        val mrl = item.storageMrl() ?: return
        if (checked && mrl.contains("file://") && !canReadStorage(activity)) {
            Permissions.showStoragePermissionDialog(activity, false)
            return
        }
        if (checked) {
            MedialibraryUtils.addDir(mrl, activity.applicationContext)
            if (settings.getInt(KEY_MEDIALIBRARY_SCAN, -1) != ML_SCAN_ON) settings.putSingle(KEY_MEDIALIBRARY_SCAN, ML_SCAN_ON)
        } else {
            MedialibraryUtils.removeDir(mrl)
        }
        refreshStorageState()
    }

    private fun toggleStorageBan(item: MediaLibraryItem) {
        val path = item.storagePath()?.stripTrailingSlash() ?: return
        activity.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.toggleBanState(path)
            refreshStorageState()
        }
    }

    private fun showDisplaySettings() {
        activity.showDisplaySettingsComposeDialog(
            displayInCards = displayInCards,
            onlyFavs = null,
            sorts = arrayListOf(Medialibrary.SORT_ALPHA, Medialibrary.SORT_FILENAME),
            currentSort = viewModel.provider.sort.takeIf { it != 0 } ?: Medialibrary.SORT_FILENAME,
            currentSortDesc = viewModel.provider.desc,
            showOnlyMultimediaFiles = settings.getBoolean(BROWSER_SHOW_ONLY_MULTIMEDIA, false),
            showHiddenFiles = settings.getBoolean(BROWSER_SHOW_HIDDEN_FILES, true),
            defaultPlaybackActions = DefaultPlaybackActionMediaType.FILE.getDefaultPlaybackActions(settings),
            defaultActionType = activity.getString(R.string.files)
        )
    }

    private fun onDisplaySettingChanged(key: String, value: Any) {
        when (key) {
            DISPLAY_IN_CARDS -> {
                val inCards = value as Boolean
                settings.putSingle(BROWSER_DISPLAY_IN_CARDS, inCards)
                displayInCards = inCards
            }
            CURRENT_SORT -> {
                @Suppress("UNCHECKED_CAST") val sort = value as Pair<Int, Boolean>
                viewModel.desc = sort.second
                viewModel.sort = sort.first
                viewModel.provider.desc = sort.second
                viewModel.provider.sort = sort.first
                viewModel.refresh()
                viewModel.saveSort()
            }
            SHOW_HIDDEN_FILES -> {
                val showHidden = value as Boolean
                settings.putSingle(BROWSER_SHOW_HIDDEN_FILES, showHidden)
                Settings.showHiddenFiles = showHidden
                viewModel.refresh()
            }
            SHOW_ONLY_MULTIMEDIA_FILES -> {
                settings.putSingle(BROWSER_SHOW_ONLY_MULTIMEDIA, value as Boolean)
                viewModel.updateShowAllFiles(value)
            }
            DEFAULT_ACTIONS -> settings.putSingle(DefaultPlaybackActionMediaType.FILE.defaultActionKey, (value as DefaultPlaybackAction).name)
        }
    }

    private fun toggleCurrentFavorite() = activity.lifecycleScope.launch {
        if (browserFavRepository.browserFavExists(currentMedia.uri)) browserFavRepository.deleteBrowserFav(currentMedia.uri)
        else if (currentMedia.uri.scheme == "file") browserFavRepository.addLocalFavItem(currentMedia.uri, currentMedia.title, currentMedia.artworkURL)
        else browserFavRepository.addNetworkFavItem(currentMedia.uri, currentMedia.title, currentMedia.artworkURL)
        activity.invalidateOptionsMenu()
    }

    private fun addToScannedFolders(media: MediaWrapper) {
        MedialibraryUtils.addDir(media.uri.toString(), activity.applicationContext)
        snacker(activity, activity.getString(R.string.scanned_directory_added, media.uri.toString().toUri().lastPathSegment))
    }

    private fun showAddDirectoryDialog() {
        val input = AppCompatEditText(activity).apply {
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        addDirectoryDialog = AlertDialog.Builder(activity)
            .setTitle(R.string.add_custom_path)
            .setMessage(R.string.add_custom_path_description)
            .setView(input)
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->
                val path = input.text.toString().trim { it <= ' ' }
                val file = File(path)
                if (!file.exists() || !file.isDirectory) {
                    snacker(activity, activity.getString(R.string.directorynotfound, path))
                    return@OnClickListener
                }
                activity.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { viewModel.addCustomDirectory(file.canonicalPath).join() }
                    viewModel.refresh()
                    refreshStorageState()
                }
            })
            .show()
    }

    private fun removeItems(items: List<MediaLibraryItem>) {
        activity.showConfirmDeleteComposeDialog(ArrayList(items)) {
            onDeleteResult(items, CONFIRM_DELETE_DIALOG_RESULT_DEFAULT_VALUE)
        }
    }

    private fun banFolders(items: List<MediaLibraryItem>) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            items.filterIsInstance<MediaWrapper>().forEach { item ->
                val path = item.uri.path ?: return@forEach
                val strippedPath = path.removePrefix("file://")
                for (root in Medialibrary.getInstance().foldersList) {
                    if (root.removePrefix("file://") == strippedPath) {
                        withContext(Dispatchers.Main) {
                            snacker(activity, activity.getString(R.string.cant_ban_root))
                        }
                        return@launch
                    }
                }
                MedialibraryUtils.banDir(strippedPath)
            }
            refreshStorageState()
        }
    }

    private fun currentFolderHasMedias() = playableItems().isNotEmpty() || viewModel.provider.hasMedias(currentMedia)

    private fun playableItems() = items.filterIsInstance<MediaWrapper>()
        .filter { it.type == MediaWrapper.TYPE_AUDIO || it.type == MediaWrapper.TYPE_VIDEO }

    private fun playAll(media: MediaWrapper?) {
        activity.lifecycleScope.launch {
            openPlayableList(media, 0)
        }
    }

    private suspend fun openPlayableList(media: MediaWrapper?, fallbackPosition: Int) {
        val sourceItems = withContext(Dispatchers.IO) {
            if (viewModel.url?.startsWith("file") == true) viewModel.provider.browseUrl(viewModel.url!!) else viewModel.dataset.getList()
        }
        val playable = sourceItems.filterIsInstance<MediaWrapper>()
            .filter { it.type == MediaWrapper.TYPE_AUDIO || it.type == MediaWrapper.TYPE_VIDEO }
            .map { getMediaWithMeta(it).applyQuickPlayDefault() }
        if (playable.isEmpty()) return
        val position = media?.let { selected ->
            playable.indexOfFirst { it.location == selected.location }.takeIf { it >= 0 }
        } ?: fallbackPosition.coerceIn(0, (playable.size - 1).coerceAtLeast(0))
        MediaUtils.openList(activity, playable, position, shuffle = PlaylistManager.shuffling.value)
    }

    private suspend fun getMediaWithMeta(media: MediaWrapper): MediaWrapper {
        return activity.getFromMl { getMedia(media.uri) ?: media }
    }

    private fun MediaWrapper.applyQuickPlayDefault(): MediaWrapper {
        if (this@SecondaryFileBrowserScreenController.settings.getBoolean(KEY_QUICK_PLAY_DEFAULT, false)) addFlags(MediaWrapper.MEDIA_NO_PARSE)
        return this
    }

    private fun MediaLibraryItem.asMediaWrapper(): MediaWrapper? {
        return when (this) {
            is MediaWrapper -> this
            is Storage -> MLServiceLocator.getAbstractMediaWrapper(uri).apply { type = MediaWrapper.TYPE_DIR }
            else -> null
        }
    }

    private fun MediaLibraryItem.asStorageMediaWrapper(): MediaWrapper? {
        return when (this) {
            is Storage -> MLServiceLocator.getAbstractMediaWrapper(uri)
            is MediaWrapper -> MLServiceLocator.getAbstractMediaWrapper(uri)
            else -> null
        }?.apply { type = MediaWrapper.TYPE_DIR }
    }

    private fun MediaWrapper.isMultiSelectable() =
        type == MediaWrapper.TYPE_AUDIO || type == MediaWrapper.TYPE_VIDEO || type == MediaWrapper.TYPE_DIR

    private fun refreshStorageState() {
        activity.lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) {
                if (!Medialibrary.getInstance().isInitiated) {
                    MediaParsingService.preselectedStorages.toList()
                } else {
                    Medialibrary.getInstance().foldersList.toList()
                }
            }
            val banned = withContext(Dispatchers.IO) {
                Medialibrary.getInstance().bannedFolders().toList()
            }
            mediaDirectories = folders.map { Uri.decode(it.removeFileScheme()) }
            bannedFolders = banned
        }
    }

    private fun storageState(item: MediaLibraryItem): ToggleableState {
        val storagePath = item.storagePath() ?: return ToggleableState.Off
        val mrl = item.storageMrl() ?: return ToggleableState.Off
        if (MedialibraryUtils.isBanned(mrl, bannedFolders)) return ToggleableState.Off
        if (scannedDirectory || mediaDirectories.containsPath(storagePath)) return ToggleableState.On
        if (mediaDirectories.any { it.sanitizePath().startsWith(storagePath.sanitizePath()) }) return ToggleableState.Indeterminate
        return ToggleableState.Off
    }

    private fun MediaLibraryItem.storageMrl(): String? {
        return when (this) {
            is Storage -> uri.toString()
            is MediaWrapper -> uri.toString()
            else -> null
        }
    }

    private fun MediaLibraryItem.storagePath(): String? {
        val uri = when (this) {
            is Storage -> uri
            is MediaWrapper -> uri
            else -> return null
        }
        val raw = if (uri.scheme == "file") uri.path.orEmpty() else Uri.decode(uri.toString())
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    private fun updateRefreshing() {
        activity.invalidateOptionsMenu()
    }

    private fun hideFloatingActionButtons() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = true
            fab.hide()
        }
    }
}

private data class StorageRootSection(
    val title: String,
    val items: List<MediaLibraryItem>,
    val loading: Boolean,
    val emptyText: String,
    val visibleWhenEmpty: Boolean = true
)

@Composable
private fun StorageRootScreenContent(
    localItems: List<MediaLibraryItem>,
    networkItems: List<MediaLibraryItem>,
    localLoading: Boolean,
    networkLoading: Boolean,
    networkConnected: Boolean,
    lanAllowed: Boolean,
    storageState: (MediaLibraryItem) -> ToggleableState,
    isCustomDirectory: (MediaLibraryItem) -> Boolean,
    onItemClicked: (MediaLibraryItem) -> Unit,
    onCheckedChange: (MediaLibraryItem, Boolean) -> Unit,
    onMoreClicked: (List<MediaLibraryItem>, Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val sections = listOf(
        StorageRootSection(
            title = stringResource(R.string.browser_storages),
            items = localItems,
            loading = localLoading,
            emptyText = stringResource(R.string.nomedia)
        ),
        StorageRootSection(
            title = stringResource(R.string.network_browsing),
            items = networkItems,
            loading = networkLoading || (networkConnected && lanAllowed && networkItems.isEmpty()),
            emptyText = networkEmptyText(enabled = true, connected = networkConnected, lanAllowed = lanAllowed)
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
                when {
                    section.loading && section.items.isEmpty() -> item(key = "${section.title}-loading") {
                        VLCEmptyState(
                            loading = true,
                            text = stringResource(R.string.loading),
                            modifier = Modifier.fillMaxWidth(),
                            compact = true
                        )
                    }
                    section.items.isEmpty() -> item(key = "${section.title}-empty") {
                        VLCEmptyState(
                            loading = false,
                            text = section.emptyText,
                            modifier = Modifier.fillMaxWidth(),
                            compact = true
                        )
                    }
                    else -> items(section.items, key = { "${section.title}-${it.stableBrowserKey()}" }) { item ->
                        val position = section.items.indexOf(item)
                        StorageRootRow(
                            item = item,
                            state = storageState(item),
                            customDirectory = isCustomDirectory(item),
                            onClick = { onItemClicked(item) },
                            onCheckedChange = { onCheckedChange(item, it) },
                            onMoreClick = { onMoreClicked(section.items, position, item) }
                        )
                    }
                }
                item(key = "${section.title}-spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StorageRootRow(
    item: MediaLibraryItem,
    state: ToggleableState,
    customDirectory: Boolean,
    checkEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = onClick,
    onCheckedChange: (Boolean) -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundDefault)
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TriStateCheckbox(
            state = state,
            onClick = if (checkEnabled) ({ onCheckedChange(state != ToggleableState.On) }) else null
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
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
        }
        IconButton(onClick = onMoreClick, enabled = customDirectory) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.more),
                tint = if (customDirectory) colors.listSubtitle else colors.listSubtitle.copy(alpha = 0f)
            )
        }
    }
}

@Composable
private fun StorageFolderScreenContent(
    items: List<MediaLibraryItem>,
    loading: Boolean,
    storageState: (MediaLibraryItem) -> ToggleableState,
    checkEnabled: Boolean,
    onItemClicked: (MediaLibraryItem) -> Unit,
    onItemLongClicked: (MediaLibraryItem) -> Unit,
    onCheckedChange: (MediaLibraryItem, Boolean) -> Unit
) {
    val colors = VLCThemeDefaults.colors
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
            when {
                loading && items.isEmpty() -> item(key = "storage-loading") {
                    VLCEmptyState(
                        loading = true,
                        text = stringResource(R.string.loading),
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                }
                items.isEmpty() -> item(key = "storage-empty") {
                    VLCEmptyState(
                        loading = false,
                        text = stringResource(R.string.nomedia),
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                }
                else -> items(items, key = { it.stableBrowserKey() }) { item ->
                    StorageRootRow(
                        item = item,
                        state = storageState(item),
                        customDirectory = false,
                        checkEnabled = checkEnabled,
                        onClick = { onItemClicked(item) },
                        onLongClick = { onItemLongClicked(item) },
                        onCheckedChange = { onCheckedChange(item, it) },
                        onMoreClick = {}
                    )
                }
            }
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
                        VLCEmptyState(
                            loading = true,
                            text = stringResource(R.string.loading),
                            modifier = Modifier.fillMaxWidth(),
                            compact = true
                        )
                    }
                } else if (section.items.isEmpty()) {
                    item(key = "${section.title}-empty") {
                        VLCEmptyState(
                            loading = false,
                            text = section.emptyText,
                            modifier = Modifier.fillMaxWidth(),
                            compact = true
                        )
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
private fun NestedBrowserScreenContent(
    displayInCards: Boolean,
    items: List<MediaLibraryItem>,
    loading: Boolean,
    emptyText: String,
    jumpToLocation: String?,
    selectedItems: Set<MediaWrapper>,
    onItemClicked: (Int, MediaLibraryItem) -> Unit,
    onItemLongClicked: (Int, MediaLibraryItem) -> Unit,
    onMoreClicked: (Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val listState = rememberLazyListState()
    LaunchedEffect(items, jumpToLocation, displayInCards) {
        val target = jumpToLocation ?: return@LaunchedEffect
        val position = items.indexOfFirst { it is MediaWrapper && it.location == target }
        if (position >= 0) listState.scrollToItem(if (displayInCards) position / 2 else position)
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.backgroundDefault
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundDefault),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            when {
                loading && items.isEmpty() -> item(key = "nested-loading") {
                    VLCEmptyState(
                        loading = true,
                        text = stringResource(R.string.loading),
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                }
                items.isEmpty() -> item(key = "nested-empty") {
                    VLCEmptyState(
                        loading = false,
                        text = emptyText,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                }
                !displayInCards -> items(items, key = { it.stableBrowserKey() }) { item ->
                    val position = items.indexOf(item)
                    BrowserListRow(
                        item = item,
                        selected = item is MediaWrapper && selectedItems.contains(item),
                        onClick = { onItemClicked(position, item) },
                        onLongClick = { onItemLongClicked(position, item) },
                        onMoreClick = { onMoreClicked(position, item) }
                    )
                }
                else -> items(items.chunked(2), key = { row -> row.joinToString { it.stableBrowserKey() } }) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            val position = items.indexOf(item)
                            BrowserCard(
                                item = item,
                                selected = item is MediaWrapper && selectedItems.contains(item),
                                modifier = Modifier.weight(1f),
                                onClick = { onItemClicked(position, item) },
                                onLongClick = { onItemLongClicked(position, item) },
                                onMoreClick = { onMoreClicked(position, item) }
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
private fun BrowserListRow(
    item: MediaLibraryItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    VLCBrowserItemRow(
        title = item.title.orEmpty(),
        subtitle = item.subtitleText(),
        selected = selected,
        titleMaxLines = 1,
        onClick = onClick,
        onLongClick = onLongClick,
        artworkContent = { BrowserItemIcon(item) },
        moreActionContent = { BrowserMoreActionIcon() },
        onMoreClick = onMoreClick
    )
}

@Composable
private fun BrowserCard(
    item: MediaLibraryItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    VLCBrowserItemCard(
        title = item.title.orEmpty(),
        subtitle = item.subtitleText(),
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        artworkContent = { BrowserItemIcon(item, large = true) },
        moreActionContent = { BrowserMoreActionIcon() },
        onMoreClick = onMoreClick
    )
}

@Composable
private fun BrowserMoreActionIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_more),
        contentDescription = stringResource(R.string.more),
        tint = VLCThemeDefaults.colors.listSubtitle
    )
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
    val artworkSize = if (large) 48.dp else 40.dp
    if (item is MediaWrapper) {
        VlcMediaImage(
            item = item,
            width = artworkSize,
            fallbackPainter = painterResource(icon),
            fallbackColorFilter = ColorFilter.tint(colors.primary),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    Box(
        modifier = Modifier
            .size(artworkSize)
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
