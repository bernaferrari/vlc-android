@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.videolan.vlc.compose.components.VLCArtworkTileShape
import org.videolan.vlc.compose.components.VLCMediaCardShape
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
    var renameText by remember { mutableStateOf("") }
    var deletePlaylistTarget by remember { mutableStateOf<PlaylistInfo?>(null) }
    var confirmDeleteSelection by remember { mutableStateOf(false) }
    var confirmRemoveTrackIndex by remember { mutableStateOf<Int?>(null) }
    val playlists = state.playlists
    val loading = state.loading
    val detailItems = state.openItems
    val detailName = state.openPlaylistName
    val hasVisibleContent = if (detailName != null) detailItems.isNotEmpty() else playlists.isNotEmpty()

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
        if (detailName != null) {
            VLCPageHeader(
                title = detailName,
                navigationIcon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = ShellStrings.back(),
                onNavigate = onBack,
                compact = true,
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
                AlertDialog(
                    onDismissRequest = { confirmRemoveTrackIndex = null },
                    title = { Text(ShellStrings.remove()) },
                    text = { Text(ShellStrings.confirmDeleteMessage()) },
                    confirmButton = {
                        TextButton(onClick = {
                            onRemoveTrack(index)
                            confirmRemoveTrackIndex = null
                        }) { Text(ShellStrings.remove()) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmRemoveTrackIndex = null }) {
                            Text(ShellStrings.cancel())
                        }
                    },
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
                            onClick = { showCreateSheet = true },
                        ),
                        VLCConnectedIconAction(
                            icon = MaterialSymbols.Filled.MoreVert,
                            contentDescription = ShellStrings.moreOptions(),
                            onClick = { showPlaylistOptionsMenu = true },
                        ),
                    ),
                )
                    DropdownMenu(
                        expanded = showPlaylistOptionsMenu,
                        onDismissRequest = { showPlaylistOptionsMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(ShellStrings.favorites()) },
                            leadingIcon = {
                                Icon(
                                    if (state.onlyFavorites) MaterialSymbols.Filled.Star else MaterialSymbols.Outlined.Star,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showPlaylistOptionsMenu = false
                                onToggleFavorites()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.sortDesc) ShellStrings.descending() else ShellStrings.ascending()) },
                            leadingIcon = { Icon(MaterialSymbols.Filled.Sort, contentDescription = null) },
                            onClick = {
                                showPlaylistOptionsMenu = false
                                onToggleSortDesc()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.viewMode == ViewMode.LIST) ShellStrings.gridView() else ShellStrings.listView()) },
                            leadingIcon = {
                                Icon(
                                    if (state.viewMode == ViewMode.LIST) MaterialSymbols.Filled.GridView else MaterialSymbols.Filled.ViewList,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showPlaylistOptionsMenu = false
                                onSetViewMode(if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                            },
                        )
                    }
                }
            }
        }

        renameTarget?.let { target ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VLCLayout.ScreenGutter, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(ShellStrings.rename()) },
                    shape = MaterialTheme.shapes.extraLarge,
                )
                TextButton(onClick = {
                    if (renameText.isNotBlank()) onRename(target.id, renameText.trim())
                    renameTarget = null
                    renameText = ""
                }) { Text(ShellStrings.save()) }
                TextButton(onClick = {
                    renameTarget = null
                    renameText = ""
                }) { Text(ShellStrings.cancel()) }
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            renameText = pl.name
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
                                if (pl.isFavorite) {
                                    Icon(
                                        MaterialSymbols.Filled.Star,
                                        contentDescription = ShellStrings.favorites(),
                                        tint = VLCThemeDefaults.colors.primary,
                                    )
                                } else {
                                    Icon(
                                        icon = MaterialSymbols.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = VLCThemeDefaults.colors.primary,
                                    )
                                }
                            },
                            moreActionContent = { Icon(MaterialSymbols.Filled.MoreVert, contentDescription = null) },
                            onMoreClick = { menu = true },
                            primaryActionContent = { Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play()) },
                            onPrimaryActionClick = { onPlay(pl) },
                        )
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(ShellStrings.play()) }, onClick = {
                                menu = false; onPlay(pl)
                            })
                            DropdownMenuItem(text = { Text(ShellStrings.shuffle()) }, onClick = {
                                menu = false; onShufflePlay(pl)
                            })
                            DropdownMenuItem(text = { Text(ShellStrings.rename()) }, onClick = {
                                menu = false
                                renameTarget = pl
                                renameText = pl.name
                            })
                            DropdownMenuItem(
                                text = { Text(if (pl.isFavorite) ShellStrings.unfavorite() else ShellStrings.favorite()) },
                                onClick = {
                                    menu = false
                                    onSetFavorite(pl.id, !pl.isFavorite)
                                },
                            )
                            DropdownMenuItem(text = { Text(ShellStrings.delete()) }, onClick = {
                                menu = false
                                deletePlaylistTarget = pl
                            })
                        }
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
            onDismissRequest = { showCreateSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(ShellStrings.newPlaylist(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(ShellStrings.newPlaylist()) },
                    shape = MaterialTheme.shapes.large,
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = { showCreateSheet = false }) { Text(ShellStrings.cancel()) }
                    TextButton(
                        enabled = newName.isNotBlank(),
                        onClick = {
                            onCreate(newName.trim())
                            newName = ""
                            showCreateSheet = false
                        },
                    ) { Text(ShellStrings.addPlaylist()) }
                }
            }
        }
    }
    if (deletePlaylistTarget != null || confirmDeleteSelection) {
        val deletingSelection = confirmDeleteSelection
        AlertDialog(
            onDismissRequest = {
                deletePlaylistTarget = null
                confirmDeleteSelection = false
            },
            title = { Text(ShellStrings.delete()) },
            text = { Text(ShellStrings.confirmDeleteMessage()) },
            confirmButton = {
                TextButton(onClick = {
                    if (deletingSelection) {
                        onDeleteSelection()
                    } else {
                        deletePlaylistTarget?.let { onDelete(it.id) }
                    }
                    deletePlaylistTarget = null
                    confirmDeleteSelection = false
                }) { Text(ShellStrings.delete()) }
            },
            dismissButton = {
                TextButton(onClick = {
                    deletePlaylistTarget = null
                    confirmDeleteSelection = false
                }) { Text(ShellStrings.cancel()) }
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
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Standard),
        label = "playlistSelection",
    )
    Surface(
        modifier = Modifier
            .clip(VLCMediaCardShape)
            .combinedClickable(onClick = onOpen, onLongClick = onToggleSelect),
        shape = VLCMediaCardShape,
        color = containerColor,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(VLCArtworkTileShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = if (playlist.isFavorite) MaterialSymbols.Filled.Star else MaterialSymbols.Filled.QueueMusic,
                    contentDescription = if (playlist.isFavorite) ShellStrings.favorites() else null,
                    tint = colors.primary,
                    modifier = Modifier.size(48.dp),
                )
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    FilledTonalIconButton(onClick = { menu = true }) {
                        Icon(MaterialSymbols.Filled.MoreVert, contentDescription = ShellStrings.moreOptions())
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(ShellStrings.play()) }, onClick = { menu = false; onPlay() })
                        DropdownMenuItem(text = { Text(ShellStrings.shuffle()) }, onClick = { menu = false; onShuffle() })
                        DropdownMenuItem(text = { Text(ShellStrings.rename()) }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(
                            text = { Text(if (playlist.isFavorite) ShellStrings.unfavorite() else ShellStrings.favorite()) },
                            onClick = { menu = false; onToggleFavorite() },
                        )
                        DropdownMenuItem(text = { Text(ShellStrings.delete()) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Text(
                playlist.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ShellStrings.itemsCount(playlist.itemCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fontLight,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onShuffle) {
                    Icon(MaterialSymbols.Filled.Shuffle, contentDescription = ShellStrings.shuffle())
                }
                FilledTonalIconButton(onClick = onPlay) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play())
                }
            }
        }
    }
}
