@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.components.DisplaySettingsSheet
import org.videolan.vlc.compose.components.DisplaySettingsState
import org.videolan.vlc.compose.components.VLCConnectedIconAction
import org.videolan.vlc.compose.components.VLCConnectedIconActionBar
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
import org.videolan.vlc.compose.components.VLCPageHeader
import org.videolan.vlc.compose.components.VLCSelectionContextBar
import org.videolan.vlc.compose.components.VLCArtworkTileShape
import org.videolan.vlc.compose.components.VLCMediaCardShape
import org.videolan.vlc.compose.components.highlightedSearchText
import org.videolan.vlc.compose.components.vlcIndexLabel
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout
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

private val MediaScreenGutter = VLCLayout.ScreenGutter
private val MediaGridGap = 12.dp
private val FastScrollerContentClearance = VLCLayout.FastScrollerClearance

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
 * Query changes briefly put the repository into a loading state. Keep the previous result on
 * screen while that replacement is being resolved; dropping the body to a blank box is
 * especially noticeable when the user closes search and returns to the library.
 */
internal fun hasVisibleMediaContent(
    state: MediaListUiState,
    sections: List<Pair<String, List<MediaItem>>>,
    groups: List<MediaFolder>,
    pagingItemCount: Int,
): Boolean =
    state.items.isNotEmpty() ||
        sections.any { it.second.isNotEmpty() } ||
        groups.isNotEmpty() ||
        pagingItemCount > 0

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
    headerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val colors = VLCThemeDefaults.colors
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showLibraryMenu by remember { mutableStateOf(false) }
    var isSearchOpen by rememberSaveable { mutableStateOf(state.query.isNotBlank()) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager: FocusManager = LocalFocusManager.current
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
    val hasVisibleContent = hasVisibleMediaContent(
        state = state,
        sections = sections,
        groups = groups,
        pagingItemCount = lazyPagingItems?.itemCount ?: 0,
    )
    val showLoadingPlaceholder = state.loading && !useEmptyPresentation && !hasVisibleContent

    Column(modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .widthIn(max = VLCLayout.ListMaxWidth)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = MediaScreenGutter),
            ) {
        if (!useEmptyPresentation && state.selection.isNotEmpty()) {
            VLCSelectionContextBar(
                title = ShellStrings.selectionCount(ShellStrings.selected(), state.selection.size),
                clearContentDescription = ShellStrings.clear(),
                onClearSelection = onClearSelection,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onPlaySelection) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = ShellStrings.play())
                }
                IconButton(onClick = onAppendSelection) {
                    Icon(MaterialSymbols.Filled.QueueMusic, contentDescription = ShellStrings.append())
                }
                IconButton(onClick = { onFavoriteSelection(true) }) {
                    Icon(MaterialSymbols.Filled.Star, contentDescription = ShellStrings.favorites())
                }
            }
        } else if (!useEmptyPresentation) {
            val isDetail = state.containerTitle != null || state.openedEntityTitle != null
            // Screen identity comes before filters and controls. This mirrors the quiet hierarchy
            // used by SDKMonitor and avoids the old VLC pattern of tabs floating above the title.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isDetail) 12.dp else 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isDetail) {
                    VLCConnectedIconActionBar(
                        actions = listOf(
                            VLCConnectedIconAction(
                                icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                                contentDescription = ShellStrings.back(),
                                onClick = onCloseContainer,
                            ),
                        ),
                    )
                }
                Text(
                    state.openedEntityTitle ?: state.containerTitle ?: title,
                    fontWeight = FontWeight.Bold,
                    style = if (isDetail) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) {
                            onQuery("")
                            focusManager.clearFocus()
                        }
                    },
                ) {
                    Icon(
                        icon = if (isSearchOpen) MaterialSymbols.Filled.Close else MaterialSymbols.Filled.Search,
                        contentDescription = if (isSearchOpen) ShellStrings.clear() else ShellStrings.search(),
                    )
                }
                // Keep the full 48dp target, but optically align the three-dot glyph with the
                // shared screen gutter rather than leaving it inset by the button's inner space.
                Box(modifier = Modifier.offset(x = 8.dp, y = (-4).dp)) {
                    IconButton(onClick = { showLibraryMenu = true }) {
                        Icon(
                            icon = MaterialSymbols.Filled.MoreVert,
                            contentDescription = ShellStrings.moreOptions(),
                        )
                    }
                    DropdownMenu(
                        expanded = showLibraryMenu,
                        onDismissRequest = { showLibraryMenu = false },
                    ) {
                            if (emptyActionText != null) {
                                DropdownMenuItem(
                                    text = { Text(emptyActionText) },
                                    leadingIcon = {
                                        Icon(MaterialSymbols.Filled.Add, contentDescription = null)
                                    },
                                    onClick = {
                                        showLibraryMenu = false
                                        onEmptyAction()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(ShellStrings.playAll()) },
                                leadingIcon = {
                                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = null)
                                },
                                onClick = {
                                    showLibraryMenu = false
                                    onPlayAll()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(ShellStrings.select()) },
                                leadingIcon = {
                                    Icon(MaterialSymbols.Filled.SelectAll, contentDescription = null)
                                },
                                onClick = {
                                    showLibraryMenu = false
                                    onSelectAll()
                                },
                            )
                            if (state.supportsRescan) {
                                DropdownMenuItem(
                                    text = { Text(ShellStrings.refresh()) },
                                    leadingIcon = {
                                        Icon(MaterialSymbols.Filled.Refresh, contentDescription = null)
                                    },
                                    onClick = {
                                        showLibraryMenu = false
                                        onRescan()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(ShellStrings.displaySettings()) },
                                leadingIcon = {
                                    Icon(MaterialSymbols.Filled.Tune, contentDescription = null)
                                },
                                onClick = {
                                    showLibraryMenu = false
                                    showDisplaySettings = true
                                },
                            )
                    }
                }
            }
            headerContent?.invoke()
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

        AnimatedVisibility(
            visible = !useEmptyPresentation && state.selection.isEmpty() && isSearchOpen,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .padding(bottom = 8.dp),
                singleLine = true,
                placeholder = { Text(ShellStrings.search()) },
                leadingIcon = {
                    Icon(MaterialSymbols.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(MaterialSymbols.Filled.Close, contentDescription = ShellStrings.clear())
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = MaterialTheme.shapes.extraLarge,
            )
        }

        LaunchedEffect(isSearchOpen) {
            if (isSearchOpen) searchFocusRequester.requestFocus()
        }
        HandleShellBackPress(enabled = isSearchOpen) {
            isSearchOpen = false
            onQuery("")
            focusManager.clearFocus()
        }

        when {
            state.error != null ->
                RetryMessage(error = state.error, onRetry = onRetry)
            state.loading && !useEmptyPresentation ->
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

            }
        }

        when {
            state.error != null || showLoadingPlaceholder -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f))
            }
            useEmptyPresentation -> {
                VLCEmptyState(
                    loading = state.loading,
                    // During first load the spinner is sufficient; do not imply that no media
                    // exists until the repository has delivered its first empty result.
                    text = if (state.loading) "" else emptyLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = VLCLayout.ListMaxWidth)
                        .align(Alignment.CenterHorizontally)
                        .weight(1f),
                    symbol = emptySymbol,
                    actionText = emptyActionText.takeIf { !state.loading },
                    onActionClick = onEmptyAction,
                )
            }
            state.groupingMode != VideoGroupingMode.NONE && groups.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(VLCLayout.GroupGap),
                        modifier = Modifier
                            .widthIn(max = VLCLayout.ListMaxWidth)
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = MediaScreenGutter),
                    ) {
                        itemsIndexed(groups, key = { _, folder -> "g:${folder.id}:${folder.path}" }) { index, folder ->
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
                                searchQuery = state.query,
                                position = sectionListItemPosition(index, groups.size),
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
                    searchQuery = state.query,
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
                    searchQuery = state.query,
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
    searchQuery: String,
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
            contentPadding = PaddingValues(
                start = MediaScreenGutter,
                end = MediaScreenGutter,
                bottom = 80.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(MediaGridGap),
            verticalArrangement = Arrangement.spacedBy(MediaGridGap),
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
                    searchQuery = searchQuery,
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
                lazyPagingItems.peek(index)?.let { item ->
                    mediaFastScrollLabelSource(item, state.sortMode)?.let { label ->
                        add(VLCIndexScrollTarget(index, label))
                    }
                }
            }
        }
        val hasFastScroller = indexTargets.size >= 24
        Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                // Dedicated space keeps the index clear of a song's overflow action.
                contentPadding = PaddingValues(
                    start = MediaScreenGutter,
                    end = if (hasFastScroller) {
                        MediaScreenGutter + FastScrollerContentClearance
                    } else {
                        MediaScreenGutter
                    },
                    bottom = 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(VLCLayout.GroupGap),
                modifier = Modifier
                    .widthIn(max = VLCLayout.ListMaxWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
            ) {
                items(lazyPagingItems.itemCount, key = { index ->
                    val item = lazyPagingItems.peek(index)
                    item?.let { "${it.id}:${it.uri}" } ?: "placeholder-$index"
                }) { index ->
                    val item = lazyPagingItems[index] ?: return@items
                    MediaListRow(
                        item = item,
                        selected = item.uri in state.selection,
                        searchQuery = searchQuery,
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
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
        }
    }
}

/** Positions alphabetic targets after any visible section headers in [SnapshotMediaBody]. */
internal fun mediaIndexScrollTargets(
    sections: List<Pair<String, List<MediaItem>>>,
    sortMode: SortMode = SortMode.TITLE,
): List<VLCIndexScrollTarget> {
    var lazyIndex = 0
    return buildList {
        sections.forEach { (section, items) ->
            if (section.isNotBlank()) lazyIndex++
            items.forEach { item ->
                val labelSource = mediaFastScrollLabelSource(item, sortMode)
                if (labelSource != null) {
                    add(VLCIndexScrollTarget(itemIndex = lazyIndex, labelSource = labelSource))
                }
                lazyIndex++
            }
        }
    }
}

/** Uses the field that actually controls list order; non-alphabetic sorts do not show an A–Z index. */
internal fun mediaFastScrollLabelSource(item: MediaItem, sortMode: SortMode): String? = when (sortMode) {
    SortMode.TITLE -> item.displayTitle
    SortMode.FILENAME -> item.fileName ?: item.displayTitle
    SortMode.ARTIST -> item.artist.orEmpty()
    SortMode.ALBUM -> item.album.orEmpty()
    SortMode.DURATION,
    SortMode.RECENT,
    -> null
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
    searchQuery: String,
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
            contentPadding = PaddingValues(
                start = MediaScreenGutter,
                end = MediaScreenGutter,
                bottom = 80.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(MediaGridGap),
            verticalArrangement = Arrangement.spacedBy(MediaGridGap),
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
                        searchQuery = searchQuery,
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
        val indexTargets = remember(displaySections, state.sortMode) {
            mediaIndexScrollTargets(displaySections, state.sortMode)
        }
        val hasFastScroller = indexTargets.size >= 24
        Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = MediaScreenGutter,
                    end = if (hasFastScroller) {
                        MediaScreenGutter + FastScrollerContentClearance
                    } else {
                        MediaScreenGutter
                    },
                    bottom = 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(VLCLayout.GroupGap),
                modifier = Modifier
                    .widthIn(max = VLCLayout.ListMaxWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
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
                                searchQuery = searchQuery,
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
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaListRow(
    item: MediaItem,
    selected: Boolean,
    searchQuery: String,
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
            searchQuery = searchQuery,
            titleMaxLines = 1,
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
    searchQuery: String = "",
    showTrackNumbers: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCtx: (MediaItem, ContextOption) -> Unit = { _, _ -> },
    canHandleHostAction: (ContextOption) -> Boolean = { false },
) {
    val colors = VLCThemeDefaults.colors
    var menu by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .clip(VLCMediaCardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        // A media grid repeats this shape many times. The large token still feels expressive
        // without turning dense libraries into a field of oversized pills.
        shape = VLCMediaCardShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(VLCArtworkTileShape)
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
                highlightedSearchText(item.displayTitle, searchQuery),
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
                    highlightedSearchText(sub, searchQuery),
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
                modifier = Modifier.padding(
                    start = VLCLayout.ScreenGutter,
                    top = 12.dp,
                    end = VLCLayout.ScreenGutter,
                    bottom = 8.dp,
                ),
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
private fun PlaylistTrackRow(
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

    Column(modifier) {
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
                    loading = false,
                    text = ShellStrings.emptyPlaylist(),
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
                modifier = Modifier.padding(
                    start = VLCLayout.ScreenGutter,
                    top = 12.dp,
                    end = VLCLayout.ScreenGutter,
                    bottom = 8.dp,
                ),
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
            loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Box(modifier = Modifier.fillMaxWidth().weight(1f))
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
    var menu by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .clip(VLCMediaCardShape)
            .combinedClickable(onClick = onOpen, onLongClick = onToggleSelect),
        shape = VLCMediaCardShape,
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

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}
