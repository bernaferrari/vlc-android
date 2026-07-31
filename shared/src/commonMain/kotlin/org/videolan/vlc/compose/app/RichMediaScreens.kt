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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
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
import org.videolan.vlc.compose.components.VLCTransientLoadingIndicator
import org.videolan.vlc.compose.components.vlcIndexLabel
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.viewmodel.MediaListUiState
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

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
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
            // Every root library uses the same PageHeader baseline and connected action group.
            // The detail route only changes the title and adds the standard Nav3 back action.
            VLCPageHeader(
                title = state.openedEntityTitle ?: state.containerTitle ?: title,
                navigationIcon = MaterialSymbols.AutoMirrored.Filled.ArrowBack.takeIf { isDetail },
                navigationContentDescription = ShellStrings.back().takeIf { isDetail },
                onNavigate = onCloseContainer.takeIf { isDetail },
                compact = isDetail,
                horizontalPadding = 0.dp,
            ) {
                Box {
                    VLCConnectedIconActionBar(
                        actions = listOf(
                            VLCConnectedIconAction(
                                icon = if (isSearchOpen) MaterialSymbols.Filled.Close else MaterialSymbols.Filled.Search,
                                contentDescription = if (isSearchOpen) ShellStrings.clear() else ShellStrings.search(),
                                onClick = {
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) {
                                        onQuery("")
                                        focusManager.clearFocus()
                                    }
                                },
                            ),
                            VLCConnectedIconAction(
                                icon = MaterialSymbols.Filled.MoreVert,
                                contentDescription = ShellStrings.moreOptions(),
                                onClick = { showLibraryMenu = true },
                            ),
                        ),
                    )
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

        state.error?.let { error ->
            RetryMessage(error = error, onRetry = onRetry)
        }

            }
        }

        when {
            state.error != null -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f))
            }
            showLoadingPlaceholder -> {
                VLCEmptyState(
                    loading = true,
                    text = "",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    symbol = emptySymbol,
                )
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
            else -> {
                MediaBody(
                    state = state,
                    sections = sections,
                    groups = groups,
                    usePaging = usePaging,
                    lazyPagingItems = lazyPagingItems,
                    searchQuery = state.query,
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
                    onOpenGroup = onOpenGroup,
                )
            }
        }
    }
    VLCTransientLoadingIndicator(
        loading = state.loading && hasVisibleContent && state.query.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter),
    )
}
}

/*
 * Keep the body dispatch in one composable so the outer pane can own a non-layout-shifting
 * loading rail. The implementation below is intentionally unchanged from the previous body
 * branches; it only moves them behind the stable loading contract.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.MediaBody(
    state: MediaListUiState,
    sections: List<Pair<String, List<MediaItem>>>,
    groups: List<MediaFolder>,
    usePaging: Boolean,
    lazyPagingItems: LazyPagingItems<MediaItem>?,
    searchQuery: String,
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
    onOpenGroup: (MediaFolder) -> Unit,
) {
    when {
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
                                ) ShellStrings.folder() else ShellStrings.group()
                            },
                            searchQuery = searchQuery,
                            position = sectionListItemPosition(index, groups.size),
                            onClick = { onOpenGroup(folder) },
                            artworkContent = {
                                Icon(
                                    icon = if (state.groupingMode == VideoGroupingMode.FOLDER) {
                                        MaterialSymbols.Filled.Folder
                                    } else MaterialSymbols.Filled.VideoLibrary,
                                    contentDescription = null,
                                    tint = VLCThemeDefaults.colors.primary,
                                )
                            },
                        )
                    }
                }
            }
        }
        state.groupingMode != VideoGroupingMode.NONE && !state.loading -> {
            VLCEmptyState(
                loading = false,
                text = emptyLabel,
                modifier = Modifier.fillMaxWidth().weight(1f),
                symbol = emptySymbol,
                actionText = emptyActionText,
                onActionClick = onEmptyAction,
            )
        }
        usePaging && lazyPagingItems != null -> {
            PagedMediaBody(
                modifier = Modifier.fillMaxWidth().weight(1f),
                state = state,
                searchQuery = searchQuery,
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
                searchQuery = searchQuery,
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


fun formatDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}
