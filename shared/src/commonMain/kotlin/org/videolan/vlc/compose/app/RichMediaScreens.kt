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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.viewmodel.toMediaSort
import org.videolan.vlc.viewmodel.toSortMode
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.components.VLCIndexScrollTarget
import org.videolan.vlc.compose.components.VLCIndexedFastScroller
import org.videolan.vlc.compose.components.VLCListItemPosition
import org.videolan.vlc.compose.components.vlcIndexLabel
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
 * A library whose first result has not arrived is not a filtered list.  Treat
 * it like a dedicated loading/empty surface instead of briefly exposing
 * controls that cannot operate on anything.  This is deliberately data-only
 * so every host (and its tests) gets the same first-launch hierarchy.
 */
internal fun shouldUseEmptyMediaPresentation(
    state: MediaListUiState,
    sections: List<Pair<String, List<MediaItem>>>,
    groups: List<MediaFolder>,
    pagingItemCount: Int,
): Boolean =
    state.error == null &&
        state.query.isBlank() &&
        !state.onlyFavorites &&
        state.selection.isEmpty() &&
        state.items.isEmpty() &&
        state.count == 0 &&
        state.containerTitle == null &&
        state.openedEntityTitle == null &&
        sections.isEmpty() &&
        groups.isEmpty()

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
    onRescan: () -> Unit = {},
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
    emptyActionText: String? = null,
    onEmptyAction: () -> Unit = {},
    emptySymbol: MaterialIcon = MaterialSymbols.Filled.VideoLibrary,
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
    val useEmptyPresentation = shouldUseEmptyMediaPresentation(
        state = state,
        sections = sections,
        groups = groups,
        pagingItemCount = lazyPagingItems?.itemCount ?: 0,
    )

    Column(modifier.padding(horizontal = 16.dp)) {
        if (!useEmptyPresentation) {
            // Keep the hierarchy deliberate: identity first, then a compact action strip.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.containerTitle != null || state.openedEntityTitle != null) {
                    IconButton(onClick = onCloseContainer) {
                        Icon(
                            icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ShellStrings.back(),
                        )
                    }
                }
                Text(
                    state.openedEntityTitle ?: state.containerTitle ?: title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (state.selection.isNotEmpty()) {
            // Text actions can grow with translations and selection counts. Wrapping keeps the
            // shared action strip touchable on narrow phones instead of clipping its last action.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalIconButton(onClick = onPlaySelection) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play())
                }
                TextButton(onClick = onAppendSelection) { Text(ShellStrings.append()) }
                IconButton(onClick = { onFavoriteSelection(true) }) {
                    Icon(MaterialSymbols.Filled.Star, contentDescription = ShellStrings.favorites())
                }
                TextButton(onClick = onClearSelection) {
                    Text(ShellStrings.selectionCount(ShellStrings.clear(), state.selection.size))
                }
            }
        } else if (!useEmptyPresentation) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSelectAll) {
                    Icon(MaterialSymbols.Filled.SelectAll, contentDescription = ShellStrings.select())
                }
                IconButton(onClick = onToggleFavorites) {
                    Icon(
                        icon = if (state.onlyFavorites) MaterialSymbols.Filled.Star else MaterialSymbols.Outlined.Star,
                        contentDescription = ShellStrings.favorites(),
                    )
                }
                IconButton(onClick = {
                    onSetViewMode(if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                }) {
                    Icon(
                        icon = if (state.viewMode == ViewMode.LIST) MaterialSymbols.Filled.GridView else MaterialSymbols.Filled.ViewList,
                        contentDescription = if (state.viewMode == ViewMode.LIST) ShellStrings.gridView() else ShellStrings.listView(),
                    )
                }
                IconButton(onClick = { showDisplaySettings = true }) {
                    Icon(MaterialSymbols.Filled.Tune, contentDescription = ShellStrings.displaySettings())
                }
                if (state.supportsRescan) {
                    TextButton(onClick = onRescan) { Text(ShellStrings.refresh()) }
                }
                FilledTonalIconButton(onClick = onPlayAll) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.playAll())
                }
            }
        }

        if (showDisplaySettings) {
            val groupingOptions = if (showGroupingToggle) {
                VideoGroupingMode.entries.map(VideoGroupingMode::name)
            } else {
                emptyList()
            }
            val selectedGrouping = when {
                !showGroupingToggle -> null
                else -> state.groupingMode.name
            }
            DisplaySettingsSheet(
                state = DisplaySettingsState(
                    viewMode = state.viewMode,
                    onlyFavorites = state.onlyFavorites,
                    sort = state.sortMode.toMediaSort(),
                    sortDesc = state.sortDesc,
                    showAllArtists = if (showAllArtistsToggle) state.showAllArtists else null,
                    showTrackNumbers = if (showTrackNumbersToggle) state.showTrackNumbers else null,
                    groupingLabel = if (showGroupingToggle) ShellStrings.groupVideos() else null,
                    groupingOptions = groupingOptions,
                    selectedGrouping = selectedGrouping,
                    defaultActionLabel = ShellStrings.defaultAction(),
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
                        VideoGroupingMode.entries.firstOrNull { it.name == label } ?: VideoGroupingMode.NONE,
                    )
                },
            )
        }

        if (!useEmptyPresentation) {
            androidx.compose.material3.OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
                label = { Text(ShellStrings.search()) },
                shape = MaterialTheme.shapes.extraLarge,
            )
        }

        if (state.loading && !useEmptyPresentation) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { error ->
            RetryMessage(error = error, onRetry = onRetry)
        }

        if (!useEmptyPresentation) {
            val countLabel = when {
                groups.isNotEmpty() -> ShellStrings.groupsCount(groups.size)
                usePaging -> {
                    val n = lazyPagingItems.itemCount
                    if (n > 0) ShellStrings.itemsPlusCount(n) else ShellStrings.itemsCount(state.count)
                }
                else -> ShellStrings.itemsCount(state.count)
            }
            Text(
                if (state.selection.isNotEmpty()) {
                    ShellStrings.selectedItemsSummary(countLabel, state.selection.size)
                } else {
                    countLabel
                },
                color = colors.fontLight,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        when {
            useEmptyPresentation -> {
                VLCEmptyState(
                    loading = state.loading,
                    // During first load the spinner is sufficient; do not imply that no media
                    // exists until the repository has delivered its first empty result.
                    text = if (state.loading) "" else emptyLabel,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                    actionText = emptyActionText.takeIf { !state.loading },
                    onActionClick = onEmptyAction,
                )
            }
            state.groupingMode != VideoGroupingMode.NONE && groups.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(groups, key = { "g:${it.id}:${it.path}" }) { folder ->
                        VLCBrowserItemRow(
                            title = folder.title,
                            subtitle = if (folder.childCount > 0) ShellStrings.itemsCount(folder.childCount) else {
                                if (folder.kind == org.videolan.vlc.model.FolderKind.MEDIA_FOLDER ||
                                    state.groupingMode == VideoGroupingMode.FOLDER
                                ) {
                                    ShellStrings.folder()
                                } else {
                                    ShellStrings.group()
                                }
                            },
                            onClick = { onOpenGroup(folder) },
                            artworkContent = {
                                Icon(
                                    icon = if (state.groupingMode == VideoGroupingMode.FOLDER) {
                                        MaterialSymbols.Filled.Folder
                                    } else {
                                        MaterialSymbols.Filled.VideoLibrary
                                    },
                                    contentDescription = null,
                                    tint = colors.primary,
                                )
                            },
                        )
                    }
                }
            }
            state.groupingMode != VideoGroupingMode.NONE && !state.loading && groups.isEmpty() -> {
                VLCEmptyState(
                    loading = false,
                    text = emptyLabel,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                    actionText = emptyActionText,
                    onActionClick = onEmptyAction,
                )
            }
            usePaging -> {
                PagedMediaBody(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    state = state,
                    lazyPagingItems = lazyPagingItems,
                    emptyLabel = emptyLabel,
                    emptySymbol = emptySymbol,
                    emptyActionText = emptyActionText,
                    onEmptyAction = onEmptyAction,
                    onPlay = onPlay,
                    onPlayNext = onPlayNext,
                    onAppend = onAppend,
                    onToggleSelect = onToggleSelect,
                    onCtx = onCtx,
                    canHandleHostAction = canHandleHostAction,
                )
            }
            !state.loading && state.items.isEmpty() && sections.isEmpty() -> {
                VLCEmptyState(
                    loading = false,
                    text = emptyLabel,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                    actionText = emptyActionText,
                    onActionClick = onEmptyAction,
                )
            }
            else -> {
                SnapshotMediaBody(
                    modifier = Modifier.fillMaxWidth().weight(1f),
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
    modifier: Modifier,
    state: MediaListUiState,
    lazyPagingItems: LazyPagingItems<MediaItem>,
    emptyLabel: String,
    emptySymbol: MaterialIcon,
    emptyActionText: String?,
    onEmptyAction: () -> Unit,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onCtx: (MediaItem, ContextOption) -> Unit,
    canHandleHostAction: (ContextOption) -> Boolean,
) {
    if (lazyPagingItems.itemCount == 0 && !state.loading) {
        VLCEmptyState(
            loading = false,
            text = emptyLabel,
            modifier = modifier,
            symbol = emptySymbol,
            actionText = emptyActionText,
            onActionClick = onEmptyAction,
        )
        return
    }
    if (state.viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier,
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
                    onCtx = onCtx,
                    canHandleHostAction = canHandleHostAction,
                )
            }
        }
    } else {
        val listState = rememberLazyListState()
        val indexTargets = buildList {
            repeat(lazyPagingItems.itemCount) { index ->
                lazyPagingItems.peek(index)?.let { add(VLCIndexScrollTarget(index, it.displayTitle)) }
            }
        }
        val hasFastScroller = indexTargets.size >= 24
        Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                // Dedicated space keeps the index clear of a song's overflow action.
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = if (hasFastScroller) 48.dp else 16.dp,
                    bottom = 80.dp,
                ),
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
                        position = pagedListItemPosition(
                            current = item.displayTitle,
                            previous = index.takeIf { it > 0 }?.let(lazyPagingItems::peek)?.displayTitle,
                            next = (index + 1).takeIf { it < lazyPagingItems.itemCount }
                                ?.let(lazyPagingItems::peek)
                                ?.displayTitle,
                        ),
                    )
                }
            }
            VLCIndexedFastScroller(
                targets = indexTargets,
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            )
        }
    }
}

/** Positions alphabetic targets after any visible section headers in [SnapshotMediaBody]. */
internal fun mediaIndexScrollTargets(sections: List<Pair<String, List<MediaItem>>>): List<VLCIndexScrollTarget> {
    var lazyIndex = 0
    return buildList {
        sections.forEach { (section, items) ->
            if (section.isNotBlank()) lazyIndex++
            items.forEach { item ->
                add(VLCIndexScrollTarget(itemIndex = lazyIndex++, labelSource = item.displayTitle))
            }
        }
    }
}

internal fun sectionListItemPosition(index: Int, size: Int): VLCListItemPosition = when {
    size <= 1 -> VLCListItemPosition.Single
    index == 0 -> VLCListItemPosition.First
    index == size - 1 -> VLCListItemPosition.Last
    else -> VLCListItemPosition.Middle
}

/**
 * Paging only knows its immediate neighbours. That is enough to preserve the
 * QuietGuard-style grouped silhouette for the loaded portion of an alphabetic
 * library without making paging wait for every item to arrive.
 */
internal fun pagedListItemPosition(
    current: String,
    previous: String?,
    next: String?,
): VLCListItemPosition {
    val label = vlcIndexLabel(current)
    val followsDifferentSection = previous?.let(::vlcIndexLabel) != label
    val precedesDifferentSection = next?.let(::vlcIndexLabel) != label
    return when {
        followsDifferentSection && precedesDifferentSection -> VLCListItemPosition.Single
        followsDifferentSection -> VLCListItemPosition.First
        precedesDifferentSection -> VLCListItemPosition.Last
        else -> VLCListItemPosition.Middle
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnapshotMediaBody(
    modifier: Modifier,
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier,
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
                        onCtx = onCtx,
                        canHandleHostAction = canHandleHostAction,
                    )
                }
            }
        }
    } else {
        val listState = rememberLazyListState()
        val indexTargets = remember(displaySections) { mediaIndexScrollTargets(displaySections) }
        val hasFastScroller = indexTargets.size >= 24
        Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = if (hasFastScroller) 48.dp else 16.dp,
                    bottom = 80.dp,
                ),
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
                    items.forEachIndexed { index, media ->
                        item(key = "${media.id}:${media.uri}") {
                            MediaListRow(
                                item = media,
                                selected = media.uri in state.selection,
                                selecting = state.selection.isNotEmpty(),
                                showTrackNumbers = state.showTrackNumbers,
                                onPlay = onPlay,
                                onPlayNext = onPlayNext,
                                onAppend = onAppend,
                                onToggleSelect = onToggleSelect,
                                onCtx = onCtx,
                                canHandleHostAction = canHandleHostAction,
                                position = sectionListItemPosition(index, items.size),
                            )
                        }
                    }
                }
            }
            VLCIndexedFastScroller(
                targets = indexTargets,
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            )
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
    position: VLCListItemPosition,
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
            position = position,
            onClick = {
                if (selecting) onToggleSelect(item) else onPlay(item)
            },
            onLongClick = { onToggleSelect(item) },
            artworkContent = { MediaArtworkSlot(item) },
            moreActionContent = { Icon(MaterialSymbols.Filled.MoreVert, contentDescription = null) },
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
        DropdownMenuItem(text = { Text(ShellStrings.play()) }, onClick = {
            onDismiss(); onCtx(item, ContextOption.CTX_PLAY)
        })
        DropdownMenuItem(text = { Text(ShellStrings.playNext()) }, onClick = {
            onDismiss(); onPlayNext(item)
        })
        DropdownMenuItem(text = { Text(ShellStrings.append()) }, onClick = {
            onDismiss(); onAppend(item)
        })
        DropdownMenuItem(text = { Text(ShellStrings.playAll()) }, onClick = {
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
            DropdownMenuItem(text = { Text(ShellStrings.removeFavorite()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_FAV_REMOVE)
            })
        } else {
            DropdownMenuItem(text = { Text(ShellStrings.addFavorite()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_FAV_ADD)
            })
        }
        DropdownMenuItem(text = { Text(ShellStrings.markPlayed()) }, onClick = {
            onDismiss(); onCtx(item, ContextOption.CTX_MARK_AS_PLAYED)
        })
        DropdownMenuItem(text = { Text(ShellStrings.markUnplayed()) }, onClick = {
            onDismiss(); onCtx(item, ContextOption.CTX_MARK_AS_UNPLAYED)
        })
        if (canHandleHostAction(ContextOption.CTX_SHARE)) {
            DropdownMenuItem(text = { Text(ShellStrings.share()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_SHARE)
            })
        }
        if (canHandleHostAction(ContextOption.CTX_INFORMATION)) {
            DropdownMenuItem(text = { Text(ShellStrings.info()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_INFORMATION)
            })
        }
        if (item.isLocallyRenamable() && canHandleHostAction(ContextOption.CTX_RENAME)) {
            DropdownMenuItem(text = { Text(ShellStrings.rename()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_RENAME)
            })
        }
        if (item.isLocallyDeletable() && canHandleHostAction(ContextOption.CTX_DELETE)) {
            DropdownMenuItem(text = { Text(ShellStrings.delete()) }, onClick = {
                onDismiss(); onCtx(item, ContextOption.CTX_DELETE)
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
    onCtx: (MediaItem, ContextOption) -> Unit = { _, _ -> },
    canHandleHostAction: (ContextOption) -> Boolean = { false },
) {
    val colors = VLCThemeDefaults.colors
    var menu by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        // A media grid repeats this shape many times. The large token still feels expressive
        // without turning dense libraries into a field of oversized pills.
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                // The card already owns the artwork well. Avoid a second dark rounded tile when
                // a thumbnail is unavailable; a single type symbol reads as intentional.
                MediaArtwork(item = item, size = 112.dp, showFallbackContainer = false)
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    FilledTonalIconButton(onClick = { menu = true }) {
                        Icon(MaterialSymbols.Filled.MoreVert, contentDescription = ShellStrings.moreOptions())
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
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
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
    emptySymbol: MaterialIcon = MaterialSymbols.Filled.Folder,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val colors = VLCThemeDefaults.colors
    val atRoot = state.currentFolder == null
    var showDisplaySettings by remember { mutableStateOf(false) }
    Column(modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.stack.isNotEmpty()) {
                IconButton(onClick = onUp) {
                    Icon(
                        icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                        contentDescription = ShellStrings.up(),
                    )
                }
            }
            Text(
                state.currentFolder?.title ?: ShellStrings.browser(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.selection.isEmpty()) {
                IconButton(onClick = { showDisplaySettings = true }) {
                    Icon(
                        icon = MaterialSymbols.Filled.Tune,
                        contentDescription = ShellStrings.displaySettings(),
                    )
                }
            }
        }
        if (state.selection.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalIconButton(onClick = onPlaySelection) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play())
                }
                TextButton(onClick = onAppendSelection) { Text(ShellStrings.append()) }
                TextButton(onClick = onClearSelection) {
                    Text(ShellStrings.selectionCount(ShellStrings.clear(), state.selection.size))
                }
            }
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
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.error?.let { error ->
            RetryMessage(error = error, onRetry = onRetry)
        }
        val isEmpty = state.folders.isEmpty() &&
            state.media.isEmpty() &&
            state.favorites.isEmpty() &&
            state.networkRoots.isEmpty()
        if (!state.loading && isEmpty) {
            VLCEmptyState(
                loading = false,
                text = ShellStrings.nothingHere(),
                modifier = Modifier.fillMaxWidth().weight(1f),
                symbol = emptySymbol,
            )
        } else LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            // A two-pixel join makes related rows read as one asymmetric group rather than a
            // vertical pile of independent cards. Section labels create the intentional gaps.
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
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
            artworkContent = { MediaArtworkSlot(item) },
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
) {
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<PlaylistInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    val playlists = state.playlists
    val loading = state.loading
    val detailItems = state.openItems
    val detailName = state.openPlaylistName

    Column(modifier.padding(horizontal = 16.dp)) {
        if (detailName != null) {
            Row(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                        contentDescription = ShellStrings.back(),
                    )
                }
                Text(
                    detailName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (detailItems.isEmpty()) {
                VLCEmptyState(
                    loading = false,
                    text = ShellStrings.emptyPlaylist(),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    itemsIndexed(detailItems, key = { index, item -> "$index:${item.id}:${item.uri}" }) { index, item ->
                        PlaylistTrackRow(
                            item = item,
                            onPlay = onPlayItem,
                            onRemove = { onRemoveTrack(index) },
                            onMoveUp = { onMoveTrackUp(index) },
                            onMoveDown = { onMoveTrackDown(index) },
                        )
                    }
                }
            }
            return
        }

        Row(
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(ShellStrings.newPlaylist()) },
                shape = MaterialTheme.shapes.extraLarge,
            )
            FilledTonalIconButton(onClick = {
                if (newName.isNotBlank()) {
                    onCreate(newName.trim())
                    newName = ""
                }
            }) {
                Icon(MaterialSymbols.Filled.Add, contentDescription = ShellStrings.addPlaylist())
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.selection.isNotEmpty()) {
                FilledTonalIconButton(onClick = onDeleteSelection) {
                    Icon(MaterialSymbols.Filled.Delete, contentDescription = ShellStrings.delete())
                }
                TextButton(onClick = onClearSelection) {
                    Text(ShellStrings.selectionCount(ShellStrings.clear(), state.selection.size))
                }
            } else {
                IconButton(onClick = onToggleFavorites) {
                    Icon(
                        icon = if (state.onlyFavorites) MaterialSymbols.Filled.Star else MaterialSymbols.Outlined.Star,
                        contentDescription = ShellStrings.favorites(),
                    )
                }
                IconButton(onClick = onToggleSortDesc) {
                    Icon(
                        icon = MaterialSymbols.Filled.Sort,
                        contentDescription = if (state.sortDesc) ShellStrings.descending() else ShellStrings.ascending(),
                    )
                }
                IconButton(onClick = {
                    onSetViewMode(if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                }) {
                    Icon(
                        icon = if (state.viewMode == ViewMode.LIST) MaterialSymbols.Filled.GridView else MaterialSymbols.Filled.ViewList,
                        contentDescription = if (state.viewMode == ViewMode.LIST) ShellStrings.gridView() else ShellStrings.listView(),
                    )
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

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        state.error?.let { error ->
            RetryMessage(error = error, onRetry = onRetry)
        }

        if (!loading && playlists.isEmpty()) {
            // This is a full library state, not a row in an otherwise scrollable list. Give it
            // the complete remaining pane so it is visually centred beneath the controls.
            VLCEmptyState(
                loading = false,
                text = ShellStrings.noPlaylists(),
                modifier = Modifier.fillMaxWidth().weight(1f),
                symbol = emptySymbol,
            )
        } else if (state.viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
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
                        onDelete = { onDelete(pl.id) },
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(playlists, key = { it.id }) { pl ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        VLCBrowserItemRow(
                            title = pl.name,
                            subtitle = ShellStrings.itemsCount(pl.itemCount),
                            selected = pl.id in state.selection,
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
                                menu = false; onDelete(pl.id)
                            })
                        }
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
    Surface(
        modifier = Modifier.combinedClickable(onClick = onOpen, onLongClick = onToggleSelect),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(MaterialTheme.shapes.large)
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

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}
