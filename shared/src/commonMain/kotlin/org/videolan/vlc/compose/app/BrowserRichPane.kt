@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.DisplaySettingsSheet
import org.videolan.vlc.compose.components.DisplaySettingsState
import org.videolan.vlc.compose.components.VLCConnectedIconAction
import org.videolan.vlc.compose.components.VLCConnectedIconActionBar
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.components.VLCListItemPosition
import org.videolan.vlc.compose.components.VLCPageHeader
import org.videolan.vlc.compose.components.VLCSelectionContextBar
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.viewmodel.BrowserUiState

@Composable
fun BrowserRichPane(
    state: BrowserUiState,
    onUp: () -> Unit,
    onOpenFolder: (MediaFolder) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onPlaySelection: () -> Unit = {},
    onAppendSelection: () -> Unit = {},
    onDefaultAction: (String) -> Unit = {},
    onShowHiddenFiles: (Boolean) -> Unit = {},
    onShowOnlyMultimedia: (Boolean) -> Unit = {},
    emptySymbol: MaterialIcon = MaterialSymbols.Filled.Folder,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val colors = VLCThemeDefaults.colors
    val atRoot = state.currentFolder == null
    var showDisplaySettings by remember { mutableStateOf(false) }
    Column(modifier) {
        if (state.selection.isNotEmpty()) {
            VLCSelectionContextBar(
                title = ShellStrings.selectionCount(ShellStrings.selected(), state.selection.size),
                clearContentDescription = ShellStrings.clear(),
                onClearSelection = onClearSelection,
                modifier = Modifier.padding(horizontal = VLCLayout.ScreenGutter),
            ) {
                IconButton(onClick = onPlaySelection) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play())
                }
                IconButton(onClick = onAppendSelection) {
                    Icon(MaterialSymbols.Filled.QueueMusic, contentDescription = ShellStrings.append())
                }
            }
        } else VLCPageHeader(
            title = state.currentFolder?.title ?: ShellStrings.browser(),
            navigationIcon = MaterialSymbols.AutoMirrored.Filled.ArrowBack.takeIf { state.stack.isNotEmpty() },
            navigationContentDescription = ShellStrings.up(),
            onNavigate = onUp.takeIf { state.stack.isNotEmpty() },
            compact = state.currentFolder != null,
        ) {
            VLCConnectedIconActionBar(
                actions = listOf(
                    VLCConnectedIconAction(
                        icon = MaterialSymbols.Filled.Tune,
                        contentDescription = ShellStrings.displaySettings(),
                        onClick = { showDisplaySettings = true },
                    ),
                ),
            )
        }
        if (showDisplaySettings) {
            DisplaySettingsSheet(
                state = DisplaySettingsState(
                    showHiddenFiles = state.showHiddenFiles,
                    showOnlyMultimedia = state.showOnlyMultimedia,
                    defaultActionLabel = ShellStrings.defaultAction(),
                    defaultActionOptions = listOf("PLAY", "PLAY_ALL", "ADD_TO_QUEUE", "INSERT_NEXT"),
                    selectedDefaultAction = state.defaultPlaybackAction,
                    supportsViewMode = false,
                    supportsFavorites = false,
                    supportsSorting = false,
                ),
                onDismiss = { showDisplaySettings = false },
                onDefaultAction = onDefaultAction,
                onShowHiddenFiles = onShowHiddenFiles,
                onShowOnlyMultimedia = onShowOnlyMultimedia,
            )
        }
        val isEmpty = state.folders.isEmpty() &&
            state.media.isEmpty() &&
            state.favorites.isEmpty() &&
            state.networkRoots.isEmpty()
        when {
            state.error != null -> {
                RetryMessage(error = state.error, onRetry = onRetry)
                Box(modifier = Modifier.fillMaxWidth().weight(1f))
            }
            state.loading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Box(modifier = Modifier.fillMaxWidth().weight(1f))
            }
            isEmpty -> VLCEmptyState(
                    loading = false,
                    text = ShellStrings.nothingHere(),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                )
            else -> LazyColumn(
            contentPadding = PaddingValues(
                start = VLCLayout.ScreenGutter,
                end = VLCLayout.ScreenGutter,
                bottom = 24.dp,
            ),
            // A two-pixel join makes related rows read as one asymmetric group rather than a
            // vertical pile of independent cards. Section labels create the intentional gaps.
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = VLCLayout.ListMaxWidth)
                .align(Alignment.CenterHorizontally)
                .weight(1f),
            ) {
            if (atRoot && state.favorites.isNotEmpty()) {
                item {
                    Text(
                        ShellStrings.favorites(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, end = 4.dp, bottom = 6.dp),
                    )
                }
                itemsIndexed(state.favorites, key = { _, item -> "fav:${item.id}:${item.uri}" }) { index, item ->
                    BrowserMediaRow(
                        item = item,
                        selected = item.uri in state.selection,
                        selecting = state.selection.isNotEmpty(),
                        position = sectionListItemPosition(index, state.favorites.size),
                        onPlay = onPlay,
                        onPlayNext = onPlayNext,
                        onAppend = onAppend,
                        onToggleSelect = onToggleSelect,
                    )
                }
            }
            if (atRoot && state.folders.isNotEmpty()) {
                item {
                    Text(
                        ShellStrings.storage(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, end = 4.dp, bottom = 6.dp),
                    )
                }
            }
            itemsIndexed(state.folders, key = { _, folder -> "f:${folder.id}:${folder.path}" }) { index, folder ->
                VLCBrowserItemRow(
                    title = folder.title,
                    subtitle = if (folder.childCount > 0) ShellStrings.itemsCount(folder.childCount) else ShellStrings.folder(),
                    position = if (atRoot) {
                        sectionListItemPosition(index, state.folders.size)
                    } else {
                        sectionListItemPosition(index, state.folders.size + state.media.size)
                    },
                    onClick = { onOpenFolder(folder) },
                    artworkContent = {
                        // Folder identity stays stable here. Favourites already live in their
                        // own section above, so a second star is visual noise rather than useful
                        // state information.
                        Icon(
                            icon = MaterialSymbols.Filled.Folder,
                            contentDescription = null,
                            tint = colors.primary,
                        )
                    },
                )
            }
            if (atRoot && state.networkRoots.isNotEmpty()) {
                item {
                    Text(
                        ShellStrings.network(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, end = 4.dp, bottom = 6.dp),
                    )
                }
                itemsIndexed(state.networkRoots, key = { _, folder -> "n:${folder.id}:${folder.path}" }) { index, folder ->
                    VLCBrowserItemRow(
                        title = folder.title,
                        subtitle = ShellStrings.network(),
                        position = sectionListItemPosition(index, state.networkRoots.size),
                        onClick = { onOpenFolder(folder) },
                        artworkContent = {
                            Icon(
                                icon = MaterialSymbols.Filled.Devices,
                                contentDescription = null,
                                tint = colors.primary,
                            )
                        },
                    )
                }
            }
            itemsIndexed(state.media, key = { _, item -> "m:${item.id}:${item.uri}" }) { index, item ->
                BrowserMediaRow(
                    item = item,
                    selected = item.uri in state.selection,
                    selecting = state.selection.isNotEmpty(),
                    position = if (atRoot) {
                        sectionListItemPosition(index, state.media.size)
                    } else {
                        sectionListItemPosition(index + state.folders.size, state.folders.size + state.media.size)
                    },
                    onPlay = onPlay,
                    onPlayNext = onPlayNext,
                    onAppend = onAppend,
                    onToggleSelect = onToggleSelect,
                )
            }
            }
        }
    }
}

@Composable
private fun BrowserMediaRow(
    item: MediaItem,
    selected: Boolean,
    selecting: Boolean,
    position: VLCListItemPosition,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        VLCBrowserItemRow(
            title = item.displayTitle,
            subtitle = formatDuration(item.duration),
            selected = selected,
            position = position,
            onClick = { if (selecting) onToggleSelect(item) else onPlay(item) },
            onLongClick = { onToggleSelect(item) },
            artworkContent = {
                if (item.isDirectory && item.isFavorite) {
                    FavoriteDirectoryArtwork()
                } else {
                    MediaArtworkSlot(item)
                }
            },
            moreActionContent = { Icon(MaterialSymbols.Filled.MoreVert, contentDescription = null) },
            onMoreClick = { menu = true },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(ShellStrings.play()) }, onClick = { menu = false; onPlay(item) })
            DropdownMenuItem(text = { Text(ShellStrings.insertNext()) }, onClick = { menu = false; onPlayNext(item) })
            DropdownMenuItem(text = { Text(ShellStrings.append()) }, onClick = { menu = false; onAppend(item) })
        }
    }
}

@Composable
private fun FavoriteDirectoryArtwork() {
    val tint = VLCThemeDefaults.colors.primary
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = MaterialSymbols.Outlined.Folder,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(30.dp),
        )
        Icon(
            icon = MaterialSymbols.Outlined.Star,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(16.dp),
        )
    }
}

@Composable
internal fun PlaylistTrackRow(
    item: MediaItem,
    position: VLCListItemPosition,
    onPlay: (MediaItem) -> Unit,
    onRemove: (MediaItem) -> Unit,
    onMoveUp: (MediaItem) -> Unit = {},
    onMoveDown: (MediaItem) -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        VLCBrowserItemRow(
            title = item.displayTitle,
            subtitle = item.artist,
            position = position,
            onClick = { onPlay(item) },
            artworkContent = { MediaArtworkSlot(item) },
            moreActionContent = { Icon(MaterialSymbols.Filled.MoreVert, contentDescription = null) },
            onMoreClick = { menu = true },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(ShellStrings.play()) }, onClick = { menu = false; onPlay(item) })
            DropdownMenuItem(
                text = { Text(ShellStrings.moveUp()) },
                onClick = { menu = false; onMoveUp(item) },
            )
            DropdownMenuItem(
                text = { Text(ShellStrings.moveDown()) },
                onClick = { menu = false; onMoveDown(item) },
            )
            DropdownMenuItem(
                text = { Text(ShellStrings.remove()) },
                onClick = { menu = false; onRemove(item) },
            )
        }
    }
}
