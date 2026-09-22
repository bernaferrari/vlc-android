@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import org.videolan.vlc.compose.components.VLCModalHeader
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import org.videolan.vlc.compose.components.VLCConfirmActionDialog
import org.jetbrains.compose.resources.stringResource
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.remove_playlist_track_message
import vlc_android.shared.generated.resources.delete_playlists_message
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCConnectedIconAction
import org.videolan.vlc.compose.components.VLCConnectedIconActionBar
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.components.VLCPageHeader
import org.videolan.vlc.compose.components.VLCSelectionContextBar
import org.videolan.vlc.compose.components.VLCTransientLoadingIndicator
import org.videolan.vlc.compose.components.VLCRenameItemDialog
import org.videolan.vlc.compose.components.DisplaySettingsSheet
import org.videolan.vlc.compose.components.DisplaySettingsState
import org.videolan.vlc.compose.components.VLCActionSheet
import org.videolan.vlc.compose.components.VLCActionSheetItem
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.viewmodel.PlaylistsUiState
import org.videolan.vlc.viewmodel.ViewMode

@Composable
fun PlaylistsRichPane(
    state: PlaylistsUiState,
    onCreate: (String) -> Unit,
    onOpen: (PlaylistInfo) -> Unit,
    onPlay: (PlaylistInfo) -> Unit,
    onShufflePlay: (PlaylistInfo) -> Unit = {},
    onDelete: (Long) -> Unit,
    onRename: (Long, String) -> Unit = { _, _ -> },
    onSetFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleSelect: (Long) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelection: () -> Unit = {},
    onToggleFavorites: () -> Unit = {},
    onToggleSortDesc: () -> Unit = {},
    onSetViewMode: (ViewMode) -> Unit = {},
    onPlayItem: (MediaItem) -> Unit,
    onRemoveTrack: (Int) -> Unit = {},
    onMoveTrackUp: (Int) -> Unit = {},
    onMoveTrackDown: (Int) -> Unit = {},
    onBack: () -> Unit,
    emptySymbol: MaterialIcon = MaterialSymbols.Filled.QueueMusic,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onClearActionError: () -> Unit = {},
) {
    var newName by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showPlaylistOptionsMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistInfo?>(null) }
    var deletePlaylistTarget by remember { mutableStateOf<PlaylistInfo?>(null) }
    var confirmDeleteSelection by remember { mutableStateOf(false) }
    var confirmRemoveTrackIndex by remember { mutableStateOf<Int?>(null) }
    val createFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val playlists = state.playlists
    val loading = state.loading
    val detailItems = state.openItems
    val detailName = state.openPlaylistName
    val hasVisibleContent = if (detailName != null) detailItems.isNotEmpty() else playlists.isNotEmpty()

    fun submitNewPlaylist() {
        val name = newName.trim()
        if (name.isBlank()) return
        onCreate(name)
        newName = ""
        showCreateSheet = false
        keyboardController?.hide()
    }

    LaunchedEffect(showCreateSheet) {
        if (showCreateSheet) {
            // Let the sheet finish its first measure before requesting focus; this avoids a
            // keyboard race that used to leave the field unfocused on slower devices.
            kotlinx.coroutines.yield()
            createFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
        if (detailName != null) {
            VLCPageHeader(
                title = detailName,
                navigationIcon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = ShellStrings.back(),
                onNavigate = onBack,
            )
            state.actionError?.let { error ->
                PlaylistActionError(error = error, onClear = onClearActionError)
            }
            if (detailItems.isEmpty()) {
                VLCEmptyState(
                    loading = loading,
                    text = if (loading) "" else ShellStrings.emptyPlaylist(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = VLCLayout.ListMaxWidth)
                        .align(Alignment.CenterHorizontally)
                        .weight(1f),
                    symbol = emptySymbol,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = VLCLayout.ScreenGutter,
                        end = VLCLayout.ScreenGutter,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = VLCLayout.ListMaxWidth)
                        .align(Alignment.CenterHorizontally)
                        .weight(1f),
                ) {
                    itemsIndexed(detailItems, key = { index, item -> "$index:${item.id}:${item.uri}" }) { index, item ->
                        PlaylistTrackRow(
                            item = item,
                            position = sectionListItemPosition(index, detailItems.size),
                            onPlay = onPlayItem,
                            onRemove = { confirmRemoveTrackIndex = index },
                            onMoveUp = { onMoveTrackUp(index) },
                            onMoveDown = { onMoveTrackDown(index) },
                        )
                    }
                }
            }
            confirmRemoveTrackIndex?.let { index ->
                VLCConfirmActionDialog(
                    title = ShellStrings.remove(),
                    message = stringResource(Res.string.remove_playlist_track_message),
                    target = detailItems.getOrNull(index)?.displayTitle,
                    confirmLabel = ShellStrings.remove(),
                    cancelLabel = ShellStrings.cancel(),
                    onConfirm = { onRemoveTrack(index); confirmRemoveTrackIndex = null },
                    onDismiss = { confirmRemoveTrackIndex = null },
                )
            }
            return
        }

        if (state.selection.isNotEmpty()) {
            VLCSelectionContextBar(
                title = ShellStrings.selectionCount(ShellStrings.selected(), state.selection.size),
                clearContentDescription = ShellStrings.clear(),
                onClearSelection = onClearSelection,
                modifier = Modifier.padding(horizontal = VLCLayout.ScreenGutter),
            ) {
                IconButton(onClick = { confirmDeleteSelection = true }) {
                    Icon(MaterialSymbols.Filled.Delete, contentDescription = ShellStrings.delete())
                }
            }
        } else {
            VLCPageHeader(title = ShellStrings.playlists()) {
                Box {
                VLCConnectedIconActionBar(
                    actions = listOf(
                        VLCConnectedIconAction(
                            icon = MaterialSymbols.Filled.Add,
                            contentDescription = ShellStrings.addPlaylist(),
                            onClick = {
                                newName = ""
                                showCreateSheet = true
                            },
                        ),
                        VLCConnectedIconAction(
                            icon = MaterialSymbols.Filled.MoreVert,
                            contentDescription = ShellStrings.moreOptions(),
                            onClick = { showPlaylistOptionsMenu = true },
                        ),
                    ),
                )
                }
            }
        }

        state.actionError?.let { error ->
            PlaylistActionError(error = error, onClear = onClearActionError)
        }

        when {
            state.error != null -> {
                RetryMessage(error = state.error, onRetry = onRetry)
                Box(modifier = Modifier.fillMaxWidth().weight(1f))
            }
            loading && !hasVisibleContent -> {
                VLCEmptyState(
                    loading = true,
                    text = "",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                )
            }
            playlists.isEmpty() -> {
            // This is a full library state, not a row in an otherwise scrollable list. Give it
            // the complete remaining pane so it is visually centred beneath the controls.
                VLCEmptyState(
                    loading = false,
                    text = ShellStrings.noPlaylists(),
                    actionText = ShellStrings.addPlaylist(),
                    onActionClick = { newName = ""; showCreateSheet = true },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                )
            }
            state.viewMode == ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(
                    start = VLCLayout.ScreenGutter,
                    end = VLCLayout.ScreenGutter,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(playlists, key = { it.id }) { pl ->
                    PlaylistCard(
                        playlist = pl,
                        selected = pl.id in state.selection,
                        onOpen = { onOpen(pl) },
                        onPlay = { onPlay(pl) },
                        onShuffle = { onShufflePlay(pl) },
                        onToggleSelect = { onToggleSelect(pl.id) },
                        onToggleFavorite = { onSetFavorite(pl.id, !pl.isFavorite) },
                        onRename = {
                            renameTarget = pl
                        },
                        onDelete = { deletePlaylistTarget = pl },
                    )
                }
            }
            }
            else -> {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = VLCLayout.ScreenGutter,
                    end = VLCLayout.ScreenGutter,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = VLCLayout.ListMaxWidth)
                    .align(Alignment.CenterHorizontally)
                    .weight(1f),
            ) {
                itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { index, pl ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        VLCBrowserItemRow(
                            title = pl.name,
                            subtitle = ShellStrings.itemsCount(pl.itemCount),
                            selected = pl.id in state.selection,
                            position = sectionListItemPosition(index, playlists.size),
                            onClick = {
                                if (state.selection.isNotEmpty()) onToggleSelect(pl.id)
                                else onOpen(pl)
                            },
                            onLongClick = { onToggleSelect(pl.id) },
                            artworkContent = {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        icon = MaterialSymbols.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = VLCThemeDefaults.colors.primary,
                                    )
                                    if (pl.isFavorite) {
                                        Icon(
                                            MaterialSymbols.Filled.Star,
                                            contentDescription = ShellStrings.favorites(),
                                            tint = VLCThemeDefaults.colors.primary,
                                            modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
                                        )
                                    }
                                }
                            },
                            moreActionContent = { Icon(MaterialSymbols.Filled.MoreVert, contentDescription = null) },
                            moreActionContentDescription = ShellStrings.moreOptions(),
                            onMoreClick = { menu = true },
                            primaryActionContent = { Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play()) },
                            onPrimaryActionClick = { onPlay(pl) },
                        )
                        PlaylistActionsSheet(
                            visible = menu,
                            playlist = pl,
                            onDismiss = { menu = false },
                            onPlay = { menu = false; onPlay(pl) },
                            onShuffle = { menu = false; onShufflePlay(pl) },
                            onRename = { menu = false; renameTarget = pl },
                            onToggleFavorite = { menu = false; onSetFavorite(pl.id, !pl.isFavorite) },
                            onDelete = { menu = false; deletePlaylistTarget = pl },
                        )
                    }
                }
            }
            }
        }
        }
        VLCTransientLoadingIndicator(
            loading = loading && hasVisibleContent,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        )
    }

    if (showCreateSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = {
                showCreateSheet = false
                keyboardController?.hide()
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VLCModalHeader(title = ShellStrings.newPlaylist())
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(createFocusRequester),
                    singleLine = true,
                    label = { Text(ShellStrings.newPlaylist()) },
                    leadingIcon = {
                        Icon(MaterialSymbols.Filled.QueueMusic, contentDescription = null)
                    },
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitNewPlaylist() }),
                )
                FlowRow(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = {
                        showCreateSheet = false
                        keyboardController?.hide()
                    }) { Text(ShellStrings.cancel()) }
                    Button(
                        enabled = newName.isNotBlank(),
                        onClick = { submitNewPlaylist() },
                    ) { Text(ShellStrings.addPlaylist()) }
                }
            }
        }
    }
    if (showPlaylistOptionsMenu) {
        DisplaySettingsSheet(
            state = DisplaySettingsState(
                viewMode = state.viewMode,
                onlyFavorites = state.onlyFavorites,
                sort = MediaSort.TITLE,
                sortDesc = state.sortDesc,
                availableSorts = listOf(MediaSort.TITLE),
            ),
            title = ShellStrings.displaySettings(),
            onDismiss = { showPlaylistOptionsMenu = false },
            onViewMode = onSetViewMode,
            onOnlyFavorites = { onToggleFavorites() },
            onSortDesc = { descending -> if (descending != state.sortDesc) onToggleSortDesc() },
        )
    }
    renameTarget?.let { target ->
        VLCRenameItemDialog(
            title = ShellStrings.rename(),
            initialValue = target.name,
            confirmLabel = ShellStrings.save(),
            cancelLabel = ShellStrings.cancel(),
            onConfirm = { value ->
                onRename(target.id, value)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
    if (deletePlaylistTarget != null || confirmDeleteSelection) {
        val deletingSelection = confirmDeleteSelection
        VLCConfirmActionDialog(
            title = ShellStrings.delete(),
            message = stringResource(Res.string.delete_playlists_message),
            target = if (deletingSelection) ShellStrings.itemsCount(state.selection.size) else deletePlaylistTarget?.name,
            confirmLabel = ShellStrings.delete(),
            cancelLabel = ShellStrings.cancel(),
            onConfirm = {
                if (deletingSelection) onDeleteSelection() else deletePlaylistTarget?.let { onDelete(it.id) }
                deletePlaylistTarget = null
                confirmDeleteSelection = false
            },
            onDismiss = {
                deletePlaylistTarget = null
                confirmDeleteSelection = false
            },
        )
    }
}

@Composable
private fun PlaylistActionError(error: String, onClear: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VLCLayout.ScreenGutter, vertical = 4.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onClear) { Text(ShellStrings.clear()) }
        }
    }
}

@Composable
private fun PlaylistActionsSheet(
    visible: Boolean,
    playlist: PlaylistInfo,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    VLCActionSheet(
        visible = visible,
        title = playlist.name,
        subtitle = ShellStrings.itemsCount(playlist.itemCount),
        headerIcon = MaterialSymbols.Filled.QueueMusic,
        actions = listOf(
            VLCActionSheetItem(ShellStrings.play(), MaterialSymbols.Filled.PlayArrow, onClick = onPlay),
            VLCActionSheetItem(ShellStrings.shuffle(), MaterialSymbols.Filled.Shuffle, onClick = onShuffle),
            VLCActionSheetItem(ShellStrings.rename(), MaterialSymbols.Filled.Edit, onClick = onRename),
            VLCActionSheetItem(
            if (playlist.isFavorite) ShellStrings.unfavorite() else ShellStrings.favorite(),
            MaterialSymbols.Filled.Star,
                onClick = onToggleFavorite,
            ),
            VLCActionSheetItem(
                ShellStrings.delete(),
                MaterialSymbols.Filled.Delete,
                destructive = true,
                onClick = onDelete,
            ),
        ),
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCard(
    playlist: PlaylistInfo,
    selected: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onToggleSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    var menu by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Standard),
        label = "playlistSelection",
    )
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .semantics { this.selected = selected }
            .combinedClickable(
                role = Role.Button,
                onClick = onOpen,
                onLongClick = onToggleSelect,
            ),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = MaterialSymbols.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                if (playlist.isFavorite) {
                    Icon(
                        icon = MaterialSymbols.Filled.Star,
                        contentDescription = ShellStrings.favorites(),
                        tint = colors.primary,
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(20.dp),
                    )
                }
                FilledTonalIconButton(
                    onClick = onPlay,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play())
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f).padding(top = 9.dp, start = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        playlist.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        ShellStrings.itemsCount(playlist.itemCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.fontLight,
                    )
                }
                IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        MaterialSymbols.Filled.MoreVert,
                        contentDescription = ShellStrings.moreOptions(),
                        tint = colors.fontLight,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
    PlaylistActionsSheet(
        visible = menu,
        playlist = playlist,
        onDismiss = { menu = false },
        onPlay = { menu = false; onPlay() },
        onShuffle = { menu = false; onShuffle() },
        onRename = { menu = false; onRename() },
        onToggleFavorite = { menu = false; onToggleFavorite() },
        onDelete = { menu = false; onDelete() },
    )
}
