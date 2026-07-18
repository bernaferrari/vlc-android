@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.viewmodel.MediaListUiState
import org.videolan.vlc.viewmodel.SortMode
import org.videolan.vlc.viewmodel.ViewMode

/**
 * Rich media browser pane — grid/list, sort, multi-select, context actions.
 * Used by Video and Audio tabs of [VlcMainShell].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RichMediaListPane(
    state: MediaListUiState,
    title: String,
    emptyLabel: String,
    sections: List<Pair<String, List<MediaItem>>> = emptyList(),
    onQuery: (String) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onPlayAll: () -> Unit,
    onPlayNext: (MediaItem) -> Unit = {},
    onAppend: (MediaItem) -> Unit = {},
    onToggleSelect: (MediaItem) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSetViewMode: (ViewMode) -> Unit = {},
    onSetSort: (SortMode) -> Unit = {},
    onCtx: (MediaItem, ContextOption) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = VLCThemeDefaults.colors
    var showSort by remember { mutableStateOf(false) }
    Column(modifier.padding(horizontal = 12.dp)) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row {
                if (state.selection.isNotEmpty()) {
                    TextButton(onClick = onClearSelection) { Text("Clear (${state.selection.size})") }
                } else {
                    TextButton(onClick = onSelectAll) { Text("Select") }
                }
                TextButton(onClick = {
                    onSetViewMode(if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                }) { Text(if (state.viewMode == ViewMode.LIST) "Grid" else "List") }
                Box {
                    TextButton(onClick = { showSort = true }) { Text("Sort") }
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    onSetSort(mode)
                                    showSort = false
                                }
                            )
                        }
                    }
                }
                if (state.items.isNotEmpty()) {
                    TextButton(onClick = onPlayAll) { Text("Play all") }
                }
            }
        }

        androidx.compose.material3.OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            label = { Text("Search") },
        )

        if (state.loading) {
            LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }

        Text(
            "${state.count} items" + if (state.selection.isNotEmpty()) " · ${state.selection.size} selected" else "",
            color = colors.fontLight,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (!state.loading && state.items.isEmpty()) {
            VLCEmptyState(loading = false, text = emptyLabel, modifier = Modifier.fillMaxSize())
            return@RichMediaListPane
        }

        val displaySections = if (sections.isNotEmpty()) sections else listOf("" to state.items)

        if (state.viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
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
                            onClick = {
                                if (state.selection.isNotEmpty()) onToggleSelect(item)
                                else onPlay(item)
                            },
                            onLongClick = { onToggleSelect(item) },
                            onMore = { onCtx(item, ContextOption.CTX_PLAY) },
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
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
                        var menu by remember { mutableStateOf(false) }
                        Box {
                            VLCBrowserItemRow(
                                title = item.displayTitle,
                                subtitle = listOfNotNull(item.artist, item.album, formatDuration(item.duration))
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · ")
                                    .ifBlank { null },
                                selected = item.uri in state.selection,
                                onClick = {
                                    if (state.selection.isNotEmpty()) onToggleSelect(item)
                                    else onPlay(item)
                                },
                                onLongClick = { onToggleSelect(item) },
                                artworkContent = {
                                    MediaTypeBadge(item)
                                },
                                moreActionContent = { Text("⋮") },
                                onMoreClick = { menu = true },
                            )
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Play") }, onClick = { menu = false; onPlay(item) })
                                DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; onPlayNext(item) })
                                DropdownMenuItem(text = { Text("Append") }, onClick = { menu = false; onAppend(item) })
                                DropdownMenuItem(text = { Text("Info") }, onClick = {
                                    menu = false
                                    onCtx(item, ContextOption.CTX_INFORMATION)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridCard(
    item: MediaItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = VLCThemeDefaults.colors
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            MediaTypeBadge(item)
        }
        Text(
            item.displayTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        val sub = listOfNotNull(item.artist, formatDuration(item.duration)).joinToString(" · ")
        if (sub.isNotBlank()) {
            Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = colors.fontLight)
        }
    }
}

@Composable
fun MediaTypeBadge(item: MediaItem) {
    val colors = VLCThemeDefaults.colors
    Text(
        when {
            item.isVideo -> "VID"
            item.isAudio -> "AUD"
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
    folders: List<MediaFolder>,
    media: List<MediaItem>,
    title: String,
    loading: Boolean,
    canGoUp: Boolean,
    onUp: () -> Unit,
    onOpenFolder: (MediaFolder) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onAppend: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VLCThemeDefaults.colors
    Column(modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canGoUp) TextButton(onClick = onUp) { Text("Up") }
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
        if (loading) LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(folders, key = { "f:${it.id}:${it.path}" }) { folder ->
                VLCBrowserItemRow(
                    title = folder.title,
                    subtitle = if (folder.childCount > 0) "${folder.childCount} items" else "Folder",
                    onClick = { onOpenFolder(folder) },
                    artworkContent = {
                        Text("DIR", color = colors.primary, fontWeight = FontWeight.Bold)
                    },
                )
            }
            items(media, key = { "m:${it.id}:${it.uri}" }) { item ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    VLCBrowserItemRow(
                        title = item.displayTitle,
                        subtitle = formatDuration(item.duration),
                        onClick = { onPlay(item) },
                        artworkContent = { MediaTypeBadge(item) },
                        moreActionContent = { Text("⋮") },
                        onMoreClick = { menu = true },
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Play") }, onClick = { menu = false; onPlay(item) })
                        DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; onPlayNext(item) })
                        DropdownMenuItem(text = { Text("Append") }, onClick = { menu = false; onAppend(item) })
                    }
                }
            }
            if (!loading && folders.isEmpty() && media.isEmpty()) {
                item {
                    VLCEmptyState(loading = false, text = "Nothing here")
                }
            }
        }
    }
}

@Composable
fun PlaylistsRichPane(
    playlists: List<PlaylistInfo>,
    loading: Boolean,
    detailItems: List<MediaItem>,
    detailName: String?,
    onCreate: (String) -> Unit,
    onOpen: (PlaylistInfo) -> Unit,
    onPlay: (PlaylistInfo) -> Unit,
    onDelete: (Long) -> Unit,
    onPlayItem: (MediaItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var newName by remember { mutableStateOf("") }
    Column(modifier.padding(12.dp)) {
        if (detailName != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back") }
                Text(detailName, fontWeight = FontWeight.Bold)
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(detailItems, key = { it.id }) { item ->
                    VLCBrowserItemRow(
                        title = item.displayTitle,
                        subtitle = item.artist,
                        onClick = { onPlayItem(item) },
                        artworkContent = { MediaTypeBadge(item) },
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
        if (loading) LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(playlists, key = { it.id }) { pl ->
                VLCBrowserItemRow(
                    title = pl.name,
                    subtitle = "${pl.itemCount} items",
                    onClick = { onOpen(pl) },
                    onLongClick = { onPlay(pl) },
                    artworkContent = {
                        Text("PLS", color = VLCThemeDefaults.colors.primary, fontWeight = FontWeight.Bold)
                    },
                    moreActionContent = { Text("Del") },
                    onMoreClick = { onDelete(pl.id) },
                    primaryActionContent = { Text("▶") },
                    onPrimaryActionClick = { onPlay(pl) },
                )
            }
            if (!loading && playlists.isEmpty()) {
                item {
                    VLCEmptyState(loading = false, text = "No playlists")
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
