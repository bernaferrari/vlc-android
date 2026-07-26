@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.components.DisplaySettingsSheet
import org.videolan.vlc.compose.components.DisplaySettingsState
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.viewmodel.toMediaSort
import org.videolan.vlc.viewmodel.toSortMode
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.viewmodel.BrowserUiState
import org.videolan.vlc.viewmodel.MediaListUiState
import org.videolan.vlc.viewmodel.PlaylistsUiState
import org.videolan.vlc.viewmodel.SortMode
import org.videolan.vlc.viewmodel.VideoGroupingMode
import org.videolan.vlc.viewmodel.ViewMode

/**
 * Rich media browser pane — grid/list, sort, multi-select, context actions, paging.
 * Used by Video and Audio tabs of [VlcMainShell].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RichMediaListPane(
    state: MediaListUiState,
    title: String,
    emptyLabel: String,
    sections: List<Pair<String, List<MediaItem>>> = emptyList(),
    pagingFlow: Flow<PagingData<MediaItem>>? = null,
    groups: List<MediaFolder> = emptyList(),
    onQuery: (String) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onPlayAll: () -> Unit,
    onPlayNext: (MediaItem) -> Unit = {},
    onAppend: (MediaItem) -> Unit = {},
    onToggleSelect: (MediaItem) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onPlaySelection: () -> Unit = {},
    onAppendSelection: () -> Unit = {},
    onFavoriteSelection: (Boolean) -> Unit = {},
    onSetViewMode: (ViewMode) -> Unit = {},
    onSetSort: (SortMode) -> Unit = {},
    onToggleSortDesc: () -> Unit = {},
    onToggleFavorites: () -> Unit = {},
    onCtx: (MediaItem, ContextOption) -> Unit = { _, _ -> },
    canHandleHostAction: (ContextOption) -> Boolean = { false },
    onOpenGroup: (MediaFolder) -> Unit = {},
    onCloseContainer: () -> Unit = {},
    onSetGroupingMode: (VideoGroupingMode) -> Unit = {},
    showGroupingToggle: Boolean = false,
    showAllArtistsToggle: Boolean = false,
    showTrackNumbersToggle: Boolean = false,
    onShowAllArtists: (Boolean) -> Unit = {},
    onShowTrackNumbers: (Boolean) -> Unit = {},
    onDefaultAction: (String) -> Unit = {},
    defaultActionOptions: List<String> = listOf("PLAY", "PLAY_ALL", "ADD_TO_QUEUE", "INSERT_NEXT"),
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val colors = VLCThemeDefaults.colors
    var showDisplaySettings by remember { mutableStateOf(false) }
    val lazyPagingItems = pagingFlow?.let { it.collectAsLazyPagingItems() }
    val usePaging = lazyPagingItems != null &&
        sections.isEmpty() &&
        groups.isEmpty() &&
        state.groupingMode == VideoGroupingMode.NONE

    Column(modifier.padding(horizontal = 12.dp)) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.containerTitle != null || state.openedEntityTitle != null) {
                    TextButton(onClick = onCloseContainer) { Text(ShellStrings.back()) }
                }
                Text(
                    state.openedEntityTitle ?: state.containerTitle ?: title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.openedEntityTitle != null) {
                    TextButton(onClick = onPlayAll) { Text(ShellStrings.playAll()) }
                }
            }
            Row {
                if (state.selection.isNotEmpty()) {
                    TextButton(onClick = onPlaySelection) { Text(ShellStrings.play()) }
                    TextButton(onClick = onAppendSelection) { Text(ShellStrings.append()) }
                    TextButton(onClick = { onFavoriteSelection(true) }) { Text("★") }
                    TextButton(onClick = onClearSelection) {
                        Text("${ShellStrings.clear()} (${state.selection.size})")
                    }
                } else {
                    TextButton(onClick = onSelectAll) { Text(ShellStrings.select()) }
                    TextButton(onClick = onToggleFavorites) {
                        Text(if (state.onlyFavorites) "★ ${ShellStrings.favorites()}" else "☆ ${ShellStrings.favorites()}")
                    }
                    TextButton(onClick = {
                        onSetViewMode(if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                    }) { Text(if (state.viewMode == ViewMode.LIST) "Grid" else "List") }
                    TextButton(onClick = { showDisplaySettings = true }) { Text(ShellStrings.displaySettings()) }
                    if (showGroupingToggle) {
                        TextButton(
                            onClick = {
                                onSetGroupingMode(
                                    when (state.groupingMode) {
                                        VideoGroupingMode.NONE -> VideoGroupingMode.NAME
                                        VideoGroupingMode.NAME -> VideoGroupingMode.FOLDER
                                        VideoGroupingMode.FOLDER -> VideoGroupingMode.NONE
                                    },
                                )
                            },
                        ) {
                            Text(
                                when (state.groupingMode) {
                                    VideoGroupingMode.NONE -> "Groups"
                                    VideoGroupingMode.NAME -> "Folders"
                                    VideoGroupingMode.FOLDER -> "Flat"
                                },
                            )
                        }
                    }
                    TextButton(onClick = onPlayAll) { Text(ShellStrings.playAll()) }
                }
            }
        }

        if (showDisplaySettings) {
            val groupingOptions = if (showGroupingToggle) {
                listOf("None", "By name", "By folder")
            } else {
                emptyList()
            }
            val selectedGrouping = when {
                !showGroupingToggle -> null
                state.groupingMode == VideoGroupingMode.NAME -> "By name"
                state.groupingMode == VideoGroupingMode.FOLDER -> "By folder"
                else -> "None"
            }
            DisplaySettingsSheet(
                state = DisplaySettingsState(
                    viewMode = state.viewMode,
                    onlyFavorites = state.onlyFavorites,
                    sort = state.sortMode.toMediaSort(),
                    sortDesc = state.sortDesc,
                    showAllArtists = if (showAllArtistsToggle) state.showAllArtists else null,
                    showTrackNumbers = if (showTrackNumbersToggle) state.showTrackNumbers else null,
                    groupingLabel = if (showGroupingToggle) "Group videos" else null,
                    groupingOptions = groupingOptions,
                    selectedGrouping = selectedGrouping,
                    defaultActionLabel = "Default action",
                    defaultActionOptions = defaultActionOptions,
                    selectedDefaultAction = state.defaultPlaybackAction,
                    availableSorts = listOf(
                        MediaSort.TITLE,
                        MediaSort.FILENAME,
                        MediaSort.ARTIST,
                        MediaSort.ALBUM,
                        MediaSort.DURATION,
                        MediaSort.RECENT,
                    ),
                ),
                onDismiss = { showDisplaySettings = false },
                onViewMode = onSetViewMode,
                onOnlyFavorites = { fav ->
                    if (fav != state.onlyFavorites) onToggleFavorites()
                },
                onSort = { sort -> onSetSort(sort.toSortMode()) },
                onSortDesc = { desc ->
                    if (desc != state.sortDesc) onToggleSortDesc()
                },
                onShowAllArtists = onShowAllArtists,
                onShowTrackNumbers = onShowTrackNumbers,
                onDefaultAction = onDefaultAction,
                onGrouping = { label ->
                    onSetGroupingMode(
                        when (label) {
                            "By name" -> VideoGroupingMode.NAME
                            "By folder" -> VideoGroupingMode.FOLDER
                            else -> VideoGroupingMode.NONE
                        },
                    )
                },
            )
        }

        androidx.compose.material3.OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            label = { Text(ShellStrings.search()) },
        )

        if (state.loading) {
            LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { error ->
            RetryMessage(error = error, onRetry = onRetry)
        }

        val countLabel = when {
            groups.isNotEmpty() -> "${groups.size} groups"
            usePaging -> {
                val n = lazyPagingItems.itemCount
                if (n > 0) "$n+ items" else "${state.count} items"
            }
            else -> "${state.count} items"
        }
        Text(
            countLabel + if (state.selection.isNotEmpty()) " · ${state.selection.size} selected" else "",
            color = colors.fontLight,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        when {
            state.groupingMode != VideoGroupingMode.NONE && groups.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(groups, key = { "g:${it.id}:${it.path}" }) { folder ->
                        VLCBrowserItemRow(
                            title = folder.title,
                            subtitle = if (folder.childCount > 0) "${folder.childCount} items" else {
                                if (folder.kind == org.videolan.vlc.model.FolderKind.MEDIA_FOLDER ||
                                    state.groupingMode == VideoGroupingMode.FOLDER
                                ) {
                                    "Folder"
                                } else {
                                    "Group"
                                }
                            },
                            onClick = { onOpenGroup(folder) },
                            artworkContent = {
                                val label = if (state.groupingMode == VideoGroupingMode.FOLDER) "DIR" else "GRP"
                                Text(label, color = colors.primary, fontWeight = FontWeight.Bold)
                            },
                        )
                    }
                }
            }
            state.groupingMode != VideoGroupingMode.NONE && !state.loading && groups.isEmpty() -> {
                VLCEmptyState(loading = false, text = emptyLabel, modifier = Modifier.fillMaxSize())
            }
            usePaging -> {
                PagedMediaBody(
                    state = state,
                    lazyPagingItems = lazyPagingItems,
                    emptyLabel = emptyLabel,
                    onPlay = onPlay,
                    onPlayNext = onPlayNext,
                    onAppend = onAppend,
                    onToggleSelect = onToggleSelect,
                    onCtx = onCtx,
                    canHandleHostAction = canHandleHostAction,
                )
            }
            !state.loading && state.items.isEmpty() && sections.isEmpty() -> {
                VLCEmptyState(loading = false, text = emptyLabel, modifier = Modifier.fillMaxSize())
            }
            else -> {
                SnapshotMediaBody(
                    state = state,
                    sections = sections,
                    onPlay = onPlay,
                    onPlayNext = onPlayNext,
                    onAppend = onAppend,
                    onToggleSelect = onToggleSelect,
                    onCtx = onCtx,
                    canHandleHostAction = canHandleHostAction,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedMediaBody(
    state: MediaListUiState,
    lazyPagingItems: LazyPagingItems<MediaItem>,
    emptyLabel: String,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onCtx: (MediaItem, ContextOption) -> Unit,
    canHandleHostAction: (ContextOption) -> Boolean,
) {
    if (lazyPagingItems.itemCount == 0 && !state.loading) {
        VLCEmptyState(loading = false, text = emptyLabel, modifier = Modifier.fillMaxSize())
        return
    }
    if (state.viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(lazyPagingItems.itemCount, key = { index ->
                val item = lazyPagingItems.peek(index)
                item?.let { "${it.id}:${it.uri}" } ?: "placeholder-$index"
            }) { index ->
                val item = lazyPagingItems[index] ?: return@items
                MediaGridCard(
                    item = item,
                    selected = item.uri in state.selection,
                    showTrackNumbers = state.showTrackNumbers,
                    onClick = {
                        if (state.selection.isNotEmpty()) onToggleSelect(item)
                        else onPlay(item)
                    },
                    onLongClick = { onToggleSelect(item) },
                    onMore = { /* card uses row menu in list mode */ },
                    onCtx = onCtx,
                    canHandleHostAction = canHandleHostAction,
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(lazyPagingItems.itemCount, key = { index ->
                val item = lazyPagingItems.peek(index)
                item?.let { "${it.id}:${it.uri}" } ?: "placeholder-$index"
            }) { index ->
                val item = lazyPagingItems[index] ?: return@items
                MediaListRow(
                    item = item,
                    selected = item.uri in state.selection,
                    selecting = state.selection.isNotEmpty(),
                    showTrackNumbers = state.showTrackNumbers,
                    onPlay = onPlay,
                    onPlayNext = onPlayNext,
                    onAppend = onAppend,
                    onToggleSelect = onToggleSelect,
                    onCtx = onCtx,
                    canHandleHostAction = canHandleHostAction,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnapshotMediaBody(
    state: MediaListUiState,
    sections: List<Pair<String, List<MediaItem>>>,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onCtx: (MediaItem, ContextOption) -> Unit,
    canHandleHostAction: (ContextOption) -> Boolean,
) {
    val colors = VLCThemeDefaults.colors
    val displaySections = if (sections.isNotEmpty()) sections else listOf("" to state.items)

    if (state.viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            displaySections.forEach { (section, items) ->
                if (section.isNotBlank()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Text(
                            section,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                items(items, key = { "${it.id}:${it.uri}" }) { item ->
                    MediaGridCard(
                        item = item,
                        selected = item.uri in state.selection,
                        showTrackNumbers = state.showTrackNumbers,
                        onClick = {
                            if (state.selection.isNotEmpty()) onToggleSelect(item)
                            else onPlay(item)
                        },
                        onLongClick = { onToggleSelect(item) },
                        onMore = {},
                        onCtx = onCtx,
                        canHandleHostAction = canHandleHostAction,
                    )
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            displaySections.forEach { (section, items) ->
                if (section.isNotBlank()) {
                    item {
                        Text(
                            section,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        )
                    }
                }
                items(items, key = { "${it.id}:${it.uri}" }) { item ->
                    MediaListRow(
                        item = item,
                        selected = item.uri in state.selection,
                        selecting = state.selection.isNotEmpty(),
                        showTrackNumbers = state.showTrackNumbers,
                        onPlay = onPlay,
                        onPlayNext = onPlayNext,
                        onAppend = onAppend,
                        onToggleSelect = onToggleSelect,
                        onCtx = onCtx,
                        canHandleHostAction = canHandleHostAction,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaListRow(
    item: MediaItem,
    selected: Boolean,
    selecting: Boolean,
    showTrackNumbers: Boolean,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onCtx: (MediaItem, ContextOption) -> Unit,
    canHandleHostAction: (ContextOption) -> Boolean,
) {
    var menu by remember { mutableStateOf(false) }
    val trackNumber = item.trackNumber.takeIf {
        showTrackNumbers && item.isAudio && it > 0
    }?.let { "#$it" }
    Box {
        VLCBrowserItemRow(
            title = item.displayTitle,
            subtitle = listOfNotNull(
                trackNumber,
                item.artist,
                item.album,
                item.description,
                formatDuration(item.duration),
            ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
            selected = selected,
            onClick = {
                if (selecting) onToggleSelect(item) else onPlay(item)
            },
            onLongClick = { onToggleSelect(item) },
            artworkContent = { MediaArtworkSlot(item) },
            moreActionContent = { Text("⋮") },
            onMoreClick = { menu = true },
        )
        MediaContextMenu(
            expanded = menu,
            onDismiss = { menu = false },
            item = item,
            onPlay = onPlay,
            onPlayNext = onPlayNext,
            onAppend = onAppend,
            onCtx = onCtx,
            canHandleHostAction = canHandleHostAction,
        )
    }
}

@Composable
private fun MediaContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    item: MediaItem,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    onCtx: (MediaItem, ContextOption) -> Unit,
    canHandleHostAction: (ContextOption) -> Boolean,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Play") }, onClick = {
            onDismiss(); onCtx(item, ContextOption.CTX_PLAY)
        })
        DropdownMenuItem(text = { Text("Play next") }, onClick = {
            onDismiss(); onPlayNext(item)
        })
        DropdownMenuItem(text = { Text("Append") }, onClick = {
            onDismiss(); onAppend(item)
        })
        DropdownMenuItem(text = { Text("Play all") }, onClick = {
            onDismiss(); onCtx(item, ContextOption.CTX_PLAY_ALL)
        })
        if (canHandleHostAction(ContextOption.CTX_ADD_TO_PLAYLIST)) {
            DropdownMenuItem(text = { Text(ShellStrings.addToPlaylist()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_ADD_TO_PLAYLIST)
            })
        }
        if (item.isVideo && canHandleHostAction(ContextOption.CTX_DOWNLOAD_SUBTITLES)) {
            DropdownMenuItem(text = { Text(ShellStrings.downloadSubtitles()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_DOWNLOAD_SUBTITLES)
            })
        }
        if (item.isAudio && canHandleHostAction(ContextOption.CTX_SET_RINGTONE)) {
            DropdownMenuItem(text = { Text(ShellStrings.setRingtone()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_SET_RINGTONE)
            })
        }
        if (item.isFavorite) {
            DropdownMenuItem(text = { Text("Remove favorite") }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_FAV_REMOVE)
            })
        } else {
            DropdownMenuItem(text = { Text("Add favorite") }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_FAV_ADD)
            })
        }
        DropdownMenuItem(text = { Text("Mark played") }, onClick = {
            onDismiss(); onCtx(item, ContextOption.CTX_MARK_AS_PLAYED)
        })
        DropdownMenuItem(text = { Text("Mark unplayed") }, onClick = {
            onDismiss(); onCtx(item, ContextOption.CTX_MARK_AS_UNPLAYED)
        })
        if (canHandleHostAction(ContextOption.CTX_SHARE)) {
            DropdownMenuItem(text = { Text(ShellStrings.share()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_SHARE)
            })
        }
        if (canHandleHostAction(ContextOption.CTX_INFORMATION)) {
            DropdownMenuItem(text = { Text("Info") }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_INFORMATION)
            })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridCard(
    item: MediaItem,
    selected: Boolean,
    showTrackNumbers: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    onCtx: (MediaItem, ContextOption) -> Unit = { _, _ -> },
    canHandleHostAction: (ContextOption) -> Boolean = { false },
) {
    val colors = VLCThemeDefaults.colors
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) colors.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            MediaArtwork(item = item, size = 96.dp)
            Box(Modifier.align(Alignment.TopEnd)) {
                TextButton(onClick = { menu = true }, modifier = Modifier.padding(0.dp)) {
                    Text("⋮")
                }
                MediaContextMenu(
                    expanded = menu,
                    onDismiss = { menu = false },
                    item = item,
                    onPlay = { onClick() },
                    onPlayNext = { media -> onCtx(media, ContextOption.CTX_PLAY_NEXT) },
                    onAppend = { media -> onCtx(media, ContextOption.CTX_APPEND) },
                    onCtx = onCtx,
                    canHandleHostAction = canHandleHostAction,
                )
            }
        }
        Text(
            item.displayTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        val trackNumber = item.trackNumber.takeIf {
            showTrackNumbers && item.isAudio && it > 0
        }?.let { "#$it" }
        val sub = listOfNotNull(trackNumber, item.artist, item.description, formatDuration(item.duration))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (sub.isNotBlank()) {
            Text(
                sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = colors.fontLight,
            )
        }
    }
}

@Composable
fun MediaArtworkSlot(item: MediaItem) {
    MediaArtwork(item = item, size = 48.dp)
}

@Composable
fun MediaTypeBadge(item: MediaItem) {
    val colors = VLCThemeDefaults.colors
    Text(
        when {
            item.isVideo -> "VID"
            item.isAudio -> "AUD"
            item.isStream -> "URL"
            else -> "•"
        },
        color = colors.primary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    if (tabs.isEmpty()) return
    PrimaryScrollableTabRow(selectedTabIndex = selected) {
        tabs.forEachIndexed { index, label ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = { Text(label) },
            )
        }
    }
}

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
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val colors = VLCThemeDefaults.colors
    val atRoot = state.currentFolder == null
    var showDisplaySettings by remember { mutableStateOf(false) }
    Column(modifier.padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.stack.isNotEmpty()) TextButton(onClick = onUp) { Text("Up") }
                Text(
                    state.currentFolder?.title ?: "Browser",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (state.selection.isNotEmpty()) {
                Row {
                    TextButton(onClick = onPlaySelection) { Text(ShellStrings.play()) }
                    TextButton(onClick = onAppendSelection) { Text(ShellStrings.append()) }
                    TextButton(onClick = onClearSelection) {
                        Text("${ShellStrings.clear()} (${state.selection.size})")
                    }
                }
            } else {
                TextButton(onClick = { showDisplaySettings = true }) {
                    Text(ShellStrings.displaySettings())
                }
            }
        }
        if (showDisplaySettings) {
            DisplaySettingsSheet(
                state = DisplaySettingsState(
                    showHiddenFiles = state.showHiddenFiles,
                    showOnlyMultimedia = state.showOnlyMultimedia,
                    defaultActionLabel = "Default action",
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
        if (state.loading) LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
        state.error?.let { error ->
            RetryMessage(error = error, onRetry = onRetry)
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (atRoot && state.favorites.isNotEmpty()) {
                item {
                    Text(
                        ShellStrings.favorites(),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
                items(state.favorites, key = { "fav:${it.id}:${it.uri}" }) { item ->
                    BrowserMediaRow(
                        item = item,
                        selected = item.uri in state.selection,
                        selecting = state.selection.isNotEmpty(),
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
                        "Storage",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
            }
            items(state.folders, key = { "f:${it.id}:${it.path}" }) { folder ->
                VLCBrowserItemRow(
                    title = folder.title + if (folder.isFavorite) " ★" else "",
                    subtitle = if (folder.childCount > 0) "${folder.childCount} items" else "Folder",
                    onClick = { onOpenFolder(folder) },
                    artworkContent = {
                        Text("DIR", color = colors.primary, fontWeight = FontWeight.Bold)
                    },
                )
            }
            if (atRoot && state.networkRoots.isNotEmpty()) {
                item {
                    Text(
                        "Network",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
                items(state.networkRoots, key = { "n:${it.id}:${it.path}" }) { folder ->
                    VLCBrowserItemRow(
                        title = folder.title,
                        subtitle = "Network",
                        onClick = { onOpenFolder(folder) },
                        artworkContent = {
                            Text("NET", color = colors.primary, fontWeight = FontWeight.Bold)
                        },
                    )
                }
            }
            items(state.media, key = { "m:${it.id}:${it.uri}" }) { item ->
                BrowserMediaRow(
                    item = item,
                    selected = item.uri in state.selection,
                    selecting = state.selection.isNotEmpty(),
                    onPlay = onPlay,
                    onPlayNext = onPlayNext,
                    onAppend = onAppend,
                    onToggleSelect = onToggleSelect,
                )
            }
            if (!state.loading &&
                state.folders.isEmpty() &&
                state.media.isEmpty() &&
                state.favorites.isEmpty() &&
                state.networkRoots.isEmpty()
            ) {
                item {
                    VLCEmptyState(loading = false, text = "Nothing here")
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
            onClick = { if (selecting) onToggleSelect(item) else onPlay(item) },
            onLongClick = { onToggleSelect(item) },
            artworkContent = { MediaArtworkSlot(item) },
            moreActionContent = { Text("⋮") },
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
private fun PlaylistTrackRow(
    item: MediaItem,
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
            onClick = { onPlay(item) },
            artworkContent = { MediaArtworkSlot(item) },
            moreActionContent = { Text("⋮") },
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
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<PlaylistInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    val playlists = state.playlists
    val loading = state.loading
    val detailItems = state.openItems
    val detailName = state.openPlaylistName

    Column(modifier.padding(12.dp)) {
        if (detailName != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(ShellStrings.back()) }
                Text(detailName, fontWeight = FontWeight.Bold)
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(detailItems, key = { index, item -> "$index:${item.id}:${item.uri}" }) { index, item ->
                    PlaylistTrackRow(
                        item = item,
                        onPlay = onPlayItem,
                        onRemove = { onRemoveTrack(index) },
                        onMoveUp = { onMoveTrackUp(index) },
                        onMoveDown = { onMoveTrackDown(index) },
                    )
                }
                if (detailItems.isEmpty()) {
                    item { Text("Empty playlist", modifier = Modifier.padding(24.dp)) }
                }
            }
            return
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("New playlist") },
            )
            TextButton(onClick = {
                if (newName.isNotBlank()) {
                    onCreate(newName.trim())
                    newName = ""
                }
            }) { Text("Add") }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row {
                TextButton(onClick = onToggleFavorites) {
                    Text(if (state.onlyFavorites) "★ ${ShellStrings.favorites()}" else "☆ ${ShellStrings.favorites()}")
                }
                TextButton(onClick = onToggleSortDesc) {
                    Text(if (state.sortDesc) "A-Z ↓" else "A-Z ↑")
                }
                TextButton(onClick = {
                    onSetViewMode(if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                }) { Text(if (state.viewMode == ViewMode.LIST) "Grid" else "List") }
            }
            if (state.selection.isNotEmpty()) {
                Row {
                    TextButton(onClick = onDeleteSelection) {
                        Text("${ShellStrings.delete()} (${state.selection.size})")
                    }
                    TextButton(onClick = onClearSelection) { Text(ShellStrings.clear()) }
                }
            }
        }

        renameTarget?.let { target ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Rename") },
                )
                TextButton(onClick = {
                    if (renameText.isNotBlank()) onRename(target.id, renameText.trim())
                    renameTarget = null
                    renameText = ""
                }) { Text("Save") }
                TextButton(onClick = {
                    renameTarget = null
                    renameText = ""
                }) { Text(ShellStrings.cancel()) }
            }
        }

        if (loading) {
            LinearProgressIndicator(
                progress = { 0f },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        state.error?.let { error ->
            RetryMessage(error = error, onRetry = onRetry)
        }

        if (state.viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
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
                        onDelete = { onDelete(pl.id) },
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(playlists, key = { it.id }) { pl ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        VLCBrowserItemRow(
                            title = (if (pl.isFavorite) "★ " else "") + pl.name,
                            subtitle = "${pl.itemCount} items",
                            selected = pl.id in state.selection,
                            onClick = {
                                if (state.selection.isNotEmpty()) onToggleSelect(pl.id)
                                else onOpen(pl)
                            },
                            onLongClick = { onToggleSelect(pl.id) },
                            artworkContent = {
                                Text(
                                    "PLS",
                                    color = VLCThemeDefaults.colors.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            moreActionContent = { Text("⋮") },
                            onMoreClick = { menu = true },
                            primaryActionContent = { Text("▶") },
                            onPrimaryActionClick = { onPlay(pl) },
                        )
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Play") }, onClick = {
                                menu = false; onPlay(pl)
                            })
                            DropdownMenuItem(text = { Text("Shuffle") }, onClick = {
                                menu = false; onShufflePlay(pl)
                            })
                            DropdownMenuItem(text = { Text("Rename") }, onClick = {
                                menu = false
                                renameTarget = pl
                                renameText = pl.name
                            })
                            DropdownMenuItem(
                                text = { Text(if (pl.isFavorite) "Unfavorite" else "Favorite") },
                                onClick = {
                                    menu = false
                                    onSetFavorite(pl.id, !pl.isFavorite)
                                },
                            )
                            DropdownMenuItem(text = { Text("Delete") }, onClick = {
                                menu = false; onDelete(pl.id)
                            })
                        }
                    }
                }
                if (!loading && playlists.isEmpty()) {
                    item {
                        VLCEmptyState(loading = false, text = "No playlists")
                    }
                }
            }
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
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) colors.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .combinedClickable(onClick = onOpen, onLongClick = onToggleSelect)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (playlist.isFavorite) "★" else "PLS",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.align(Alignment.TopEnd)) {
                TextButton(onClick = { menu = true }) { Text("⋮") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Play") }, onClick = { menu = false; onPlay() })
                    DropdownMenuItem(text = { Text("Shuffle") }, onClick = { menu = false; onShuffle() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(
                        text = { Text(if (playlist.isFavorite) "Unfavorite" else "Favorite") },
                        onClick = { menu = false; onToggleFavorite() },
                    )
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
        Text(
            playlist.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "${playlist.itemCount} items",
            style = MaterialTheme.typography.labelSmall,
            color = colors.fontLight,
        )
        Row {
            TextButton(onClick = onPlay) { Text("Play") }
            TextButton(onClick = onShuffle) { Text("Shuffle") }
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}
