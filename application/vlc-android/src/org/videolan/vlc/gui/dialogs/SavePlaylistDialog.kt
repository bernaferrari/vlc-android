/*
 * *************************************************************************
 *  SavePlaylist.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.dialogs

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.tools.AppScope
import org.videolan.tools.CoroutineContextProvider
import org.videolan.tools.PLAYLIST_REPLACE
import org.videolan.tools.Settings
import org.videolan.tools.isStarted
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.providers.FileBrowserProvider
import org.videolan.vlc.util.isSchemeStreaming
import org.videolan.vlc.util.isTalkbackIsEnabled
import org.videolan.vlc.viewmodels.browser.BrowserModel
import org.videolan.vlc.viewmodels.browser.TYPE_FILE
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

object SavePlaylistDialog {
    const val TAG = "VLC/SavePlaylistDialog"

    const val KEY_NEW_TRACKS = "PLAYLIST_NEW_TRACKS"
    const val KEY_FOLDER = "PLAYLIST_FROM_FOLDER"
    const val KEY_SUB_FOLDERS = "PLAYLIST_FOLDER_ADD_SUBFOLDERS"
    const val KEY_DEFAULT_TITLE = "DEFAULT_TITLE"

    const val SELECTED_PLAYLIST = "SELECTED_PLAYLIST"
}

private var isSavePlaylistComposeDialogShowing = false

fun ComponentActivity.showSavePlaylistComposeDialog(
    tracks: Array<MediaWrapper>? = null,
    folder: String? = null,
    includeSubfolders: Boolean = false,
    defaultTitle: String = ""
) {
    if (!isStarted() || isSavePlaylistComposeDialogShowing) return
    isSavePlaylistComposeDialogShowing = true
    lifecycleScope.launch {
        if (showPinIfNeeded()) {
            isSavePlaylistComposeDialogShowing = false
            return@launch
        }
        SavePlaylistComposeDialog(
            activity = this@showSavePlaylistComposeDialog,
            initialTracks = tracks,
            folder = folder,
            includeSubfolders = includeSubfolders,
            defaultTitle = defaultTitle,
            onDismissed = { isSavePlaylistComposeDialogShowing = false }
        ).show()
    }
}

private class SavePlaylistComposeDialog(
    private val activity: ComponentActivity,
    private val initialTracks: Array<MediaWrapper>?,
    private val folder: String?,
    private val includeSubfolders: Boolean,
    defaultTitle: String,
    private val onDismissed: () -> Unit
) {
    private val medialibrary = Medialibrary.getInstance()
    private val settings = Settings.getInstance(activity)
    private val coroutineContextProvider = CoroutineContextProvider()
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private val playlistNameState = mutableStateOf(defaultTitle)
    private val playlistNameErrorState = mutableStateOf<String?>(null)
    private val isLoadingState = mutableStateOf(false)
    private val filesTextState = mutableStateOf("")
    private val newTracksState = mutableStateOf(emptyArray<MediaWrapper>())
    private val playlistsState = mutableStateOf<List<Playlist>>(emptyList())
    private val selectedPlaylistIdsState = mutableStateOf<Set<Long>>(emptySet())
    private val replaceCheckedState = mutableStateOf(settings.getBoolean(PLAYLIST_REPLACE, false))
    private val replaceConfirmationState = mutableStateOf<ReplaceConfirmation?>(null)
    private val alreadyAdding = AtomicBoolean(false)
    private var playlistIterator: Iterator<Playlist>? = null
    private var nonDuplicateTracks: Array<MediaWrapper>? = null
    private var browserModel: BrowserModel? = null
    private var browserObserver: Observer<MutableList<MediaLibraryItem>>? = null
    private var folderLoadJob: Job? = null
    private var rootView: ComposeView? = null

    fun show() {
        refreshPlaylists()
        setupContent()
        dialog.show()
        configureBottomSheet()
        loadTracks()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    SavePlaylistContent(
                        playlistName = playlistNameState.value,
                        playlistNameError = playlistNameErrorState.value,
                        isLoading = isLoadingState.value,
                        filesText = filesTextState.value,
                        playlists = playlistsState.value,
                        selectedPlaylistIds = selectedPlaylistIdsState.value,
                        replaceChecked = replaceCheckedState.value,
                        replaceConfirmation = replaceConfirmationState.value,
                        saveButtonText = if (newTracksState.value.isNotEmpty()) R.string.save else R.string.add,
                        onPlaylistNameChanged = {
                            playlistNameState.value = it
                            playlistNameErrorState.value = null
                        },
                        onCreatePlaylist = ::addNewPlaylist,
                        onPlaylistClicked = ::togglePlaylistSelection,
                        onReplaceCheckedChanged = ::setReplaceChecked,
                        onSave = ::saveToSelectedPlaylists,
                        onCancelReplace = { replaceConfirmationState.value = null },
                        onConfirmReplace = { confirmation ->
                            replaceConfirmationState.value = null
                            savePlaylist(confirmation.playlist, confirmation.tracks, confirmed = true)
                        }
                    )
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            folderLoadJob?.cancel()
            folderLoadJob = null
            browserObserver?.let { observer -> browserModel?.dataset?.removeObserver(observer) }
            browserObserver = null
            browserModel?.provider?.release()
            browserModel = null
            rootView = null
            onDismissed()
        }
    }

    private fun loadTracks() {
        initialTracks?.let {
            setLoadedTracks(it)
            return
        }
        val folder = folder ?: return
        isLoadingState.value = true
        val model = BrowserModel(activity.applicationContext, folder, TYPE_FILE, showDummyCategory = false)
        browserModel = model
        if (includeSubfolders) {
            folderLoadJob = activity.lifecycleScope.launch {
                val tracks = withContext(Dispatchers.IO) {
                    (model.provider as FileBrowserProvider).browseByUrl(folder).toTypedArray()
                }
                setLoadedTracks(tracks)
                model.provider.release()
                browserModel = null
            }
        } else {
            val observer = Observer<MutableList<MediaLibraryItem>> { mediaLibraryItems ->
                val tracks = mediaLibraryItems.asSequence()
                    .filterIsInstance<MediaWrapper>()
                    .filter { it.type != MediaWrapper.TYPE_DIR }
                    .toList()
                    .toTypedArray()
                setLoadedTracks(tracks)
            }
            browserObserver = observer
            model.dataset.observe(activity, observer)
        }
    }

    private fun setLoadedTracks(tracks: Array<MediaWrapper>) {
        newTracksState.value = tracks
        isLoadingState.value = false
        filesTextState.value = activity.resources.getQuantityString(R.plurals.media_quantity, tracks.size, tracks.size)
    }

    private fun refreshPlaylists(selectPlaylist: Playlist? = null) {
        val selectedIds = selectedPlaylistIdsState.value.toMutableSet()
        selectPlaylist?.let { selectedIds.add(it.id) }
        playlistsState.value = medialibrary.getPlaylists(Playlist.Type.All, false)
            .onEach { playlist ->
                playlist.description = activity.resources.getQuantityString(
                    R.plurals.media_quantity,
                    playlist.tracksCount,
                    playlist.tracksCount
                )
            }
            .toList()
        selectedPlaylistIdsState.value = selectedIds
    }

    private fun addNewPlaylist() {
        if (alreadyAdding.getAndSet(true)) return
        val name = playlistNameState.value.trim { it <= ' ' }
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) { medialibrary.getPlaylistByName(name) }?.let {
                playlistNameErrorState.value = activity.getString(R.string.playlist_existing, it.title)
                alreadyAdding.set(false)
                return@launch
            }
            val playlist = medialibrary.createPlaylist(name, Settings.includeMissing, false)
            playlistNameState.value = ""
            refreshPlaylists(selectPlaylist = playlist)
            playlistNameErrorState.value = null
            alreadyAdding.set(false)
        }
    }

    private fun togglePlaylistSelection(playlist: Playlist) {
        selectedPlaylistIdsState.value = selectedPlaylistIdsState.value.toMutableSet().apply {
            if (!add(playlist.id)) remove(playlist.id)
        }
    }

    private fun setReplaceChecked(checked: Boolean) {
        replaceCheckedState.value = checked
        settings.putSingle(PLAYLIST_REPLACE, checked)
    }

    private fun saveToSelectedPlaylists() {
        val selectedItems = playlistsState.value.filter { selectedPlaylistIdsState.value.contains(it.id) }
        if (selectedItems.isEmpty()) return
        playlistIterator = selectedItems.iterator()
        saveToExistingPlaylists(selectedItems)
    }

    private fun processNextItem(tracks: Array<MediaWrapper>) {
        val iterator = playlistIterator
        if (iterator != null && iterator.hasNext()) {
            val item = iterator.next()
            savePlaylist(item, tracks)
        } else {
            playlistIterator = null
            dialog.dismiss()
        }
    }

    private fun savePlaylist(playlist: Playlist, tracks: Array<MediaWrapper>, confirmed: Boolean = false) {
        AppScope.launch(coroutineContextProvider.IO) {
            if (tracks.isEmpty()) return@launch
            val ids = LinkedList<Long>()
            for (mw in tracks) {
                val id = mw.id
                if (id == 0L) {
                    var media = medialibrary.getMedia(mw.uri)
                    if (media != null) {
                        ids.add(media.id)
                        media.title = mw.title
                    } else {
                        media = if (isSchemeStreaming(mw.location)) {
                            medialibrary.addStream(mw.location, mw.title)
                        } else {
                            medialibrary.addMedia(mw.location, -1L)
                        }
                        if (media != null) ids.add(media.id)
                    }
                } else {
                    ids.add(id)
                }
            }
            var resume = true
            if (replaceCheckedState.value) {
                if (confirmed) {
                    val name = playlist.title
                    playlist.delete()
                    val newPlaylist = medialibrary.createPlaylist(name, Settings.includeMissing, false)
                    newPlaylist.append(ids)
                } else {
                    resume = false
                    withContext(coroutineContextProvider.Main) {
                        replaceConfirmationState.value = ReplaceConfirmation(playlist, tracks)
                    }
                }
            } else {
                playlist.append(ids)
            }
            if (resume) withContext(coroutineContextProvider.Main) {
                if (playlistIterator == null) dialog.dismiss() else processNextItem(tracks)
            }
        }
    }

    private fun saveToExistingPlaylists(items: List<Playlist>) {
        val highlightedItemsCounts = newTracksState.value.size
        var shouldShowThreeOptions = false
        val titles = ArrayList<String>()
        val duplicationMessages = items.filter { playlist ->
            nonDuplicateTracks = getNonDuplicateTracks(playlist.tracks, newTracksState.value)
            val duplicateItemsCount = newTracksState.value.size - nonDuplicateTracks!!.size
            duplicateItemsCount != 0
        }.map { playlist ->
            nonDuplicateTracks = getNonDuplicateTracks(playlist.tracks, newTracksState.value)
            val duplicateItemsCount = newTracksState.value.size - nonDuplicateTracks!!.size
            titles.add(playlist.title)
            if (duplicateItemsCount < highlightedItemsCounts) {
                shouldShowThreeOptions = true
                if (duplicateItemsCount == 1) {
                    activity.resources.getQuantityString(R.plurals.duplicate_three_options_secondary, duplicateItemsCount, playlist.title)
                } else {
                    activity.resources.getQuantityString(R.plurals.duplicate_three_options_secondary, duplicateItemsCount, duplicateItemsCount, playlist.title)
                }
            } else {
                activity.resources.getQuantityString(R.plurals.duplicate_two_options_secondary, duplicateItemsCount, playlist.title)
            }
        }

        if (duplicationMessages.isEmpty() || replaceCheckedState.value) {
            processNextItem(newTracksState.value)
        } else {
            activity.showDuplicationWarningComposeDialog(
                shouldShowThreeOptions = shouldShowThreeOptions,
                playlistTitles = titles,
                duplicationMessages = duplicationMessages,
                onOptionSelected = { option ->
                    when (option) {
                        DuplicationWarningResult.ADD_ALL -> processNextItem(newTracksState.value)
                        DuplicationWarningResult.ADD_NEW -> nonDuplicateTracks?.let { processNextItem(it) }
                        DuplicationWarningResult.CANCEL, DuplicationWarningResult.NO_OPTION -> Unit
                    }
                }
            )
        }
    }

    private fun getNonDuplicateTracks(currentTracks: Array<MediaWrapper>, newTracks: Array<MediaWrapper>): Array<MediaWrapper> {
        return newTracks.filter { newItem ->
            currentTracks.all { currentItem ->
                !currentItem.equals(newItem)
            }
        }.toTypedArray()
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusable = false
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusableInTouchMode = false
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
}

private data class ReplaceConfirmation(
    val playlist: Playlist,
    val tracks: Array<MediaWrapper>
)

@Composable
private fun SavePlaylistContent(
    playlistName: String,
    playlistNameError: String?,
    isLoading: Boolean,
    filesText: String,
    playlists: List<Playlist>,
    selectedPlaylistIds: Set<Long>,
    replaceChecked: Boolean,
    replaceConfirmation: ReplaceConfirmation?,
    saveButtonText: Int,
    onPlaylistNameChanged: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistClicked: (Playlist) -> Unit,
    onReplaceCheckedChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancelReplace: () -> Unit,
    onConfirmReplace: (ReplaceConfirmation) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 300.dp)
                .padding(vertical = 16.dp)
        ) {
            CreatePlaylistSection(
                playlistName = playlistName,
                playlistNameError = playlistNameError,
                isLoading = isLoading,
                onPlaylistNameChanged = onPlaylistNameChanged,
                onCreatePlaylist = onCreatePlaylist
            )
            if (replaceConfirmation != null) {
                ReplacePlaylistConfirmation(
                    confirmation = replaceConfirmation,
                    onCancel = onCancelReplace,
                    onConfirm = onConfirmReplace,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                ExistingPlaylistSection(
                    isLoading = isLoading,
                    filesText = filesText,
                    playlists = playlists,
                    selectedPlaylistIds = selectedPlaylistIds,
                    replaceChecked = replaceChecked,
                    onPlaylistClicked = onPlaylistClicked,
                    onReplaceCheckedChanged = onReplaceCheckedChanged
                )
                TextButton(
                    onClick = onSave,
                    enabled = !isLoading && selectedPlaylistIds.isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(saveButtonText))
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistSection(
    playlistName: String,
    playlistNameError: String?,
    isLoading: Boolean,
    onPlaylistNameChanged: (String) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.create_new_playlist),
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = playlistName,
                onValueChange = onPlaylistNameChanged,
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                isError = playlistNameError != null,
                supportingText = playlistNameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    focusManager.clearFocus()
                    onCreatePlaylist()
                }),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onCreatePlaylist()
                },
                enabled = !isLoading,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.create))
            }
        }
    }
}

@Composable
private fun ExistingPlaylistSection(
    isLoading: Boolean,
    filesText: String,
    playlists: List<Playlist>,
    selectedPlaylistIds: Set<Long>,
    replaceChecked: Boolean,
    onPlaylistClicked: (Playlist) -> Unit,
    onReplaceCheckedChanged: (Boolean) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = if (selectedPlaylistIds.isEmpty()) {
                    stringResource(R.string.add_to_existing_playlist)
                } else {
                    stringResource(R.string.selection_count, selectedPlaylistIds.size)
                },
                color = colors.fontDefault,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        if (!isLoading && filesText.isNotEmpty()) {
            Text(
                text = filesText,
                color = colors.fontLight,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) { onReplaceCheckedChanged(!replaceChecked) }
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.replace_playlist),
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = replaceChecked,
                enabled = !isLoading,
                onCheckedChange = onReplaceCheckedChanged
            )
        }
        HorizontalDivider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 420.dp)
        ) {
            if (!isLoading && playlists.isEmpty()) {
                Text(
                    text = stringResource(R.string.noplaylist),
                    color = colors.fontLight,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            selected = selectedPlaylistIds.contains(playlist.id),
                            enabled = !isLoading,
                            onClick = { onPlaylistClicked(playlist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onClick() }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!playlist.description.isNullOrBlank()) {
                Text(
                    text = playlist.description,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Checkbox(
            checked = selected,
            enabled = enabled,
            onCheckedChange = { onClick() }
        )
    }
}

@Composable
private fun ReplacePlaylistConfirmation(
    confirmation: ReplaceConfirmation,
    onCancel: () -> Unit,
    onConfirm: (ReplaceConfirmation) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefaultDarker,
        contentColor = colors.fontDefault,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.confirm_replace_playlist, confirmation.playlist.title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = { onConfirm(confirmation) }) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

fun Medialibrary.getPlaylistByName(name: String) = getPlaylists(Playlist.Type.All, false).firstOrNull { it.title == name }
