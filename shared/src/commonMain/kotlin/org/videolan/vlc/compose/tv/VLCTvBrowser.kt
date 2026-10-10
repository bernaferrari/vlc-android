package org.videolan.vlc.compose.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols

@Immutable
data class VLCTvSortOption(val label: String, val id: Int)

/** Labels for the browser header controls; the host supplies localized strings. */
@Immutable
data class VLCTvBrowserLabels(
    val back: String? = null,
    val sort: String? = null,
    val showAsList: String? = null,
    val showAsGrid: String? = null,
    val addFavorite: String? = null,
    val removeFavorite: String? = null,
)

/** Default grid density per card shape on a 960dp-wide TV canvas. */
fun VLCTvCardShape.gridColumns(): Int = when (this) {
    VLCTvCardShape.Landscape -> 4
    VLCTvCardShape.Square -> 5
    VLCTvCardShape.Poster -> 6
}

/**
 * Full-screen library/folder browser: header with back, title and controls (favorite, sort,
 * grid/list), then either a grid of cards or a list of rows, plus loading and empty states.
 */
@Composable
fun VLCTvBrowserScreen(
    title: String,
    items: List<VLCTvItem>,
    loading: Boolean,
    inGrid: Boolean,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    emptyTitle: String = "",
    emptyMessage: String? = null,
    emptySymbol: MaterialIcon = MaterialSymbols.Filled.Folder,
    shape: VLCTvCardShape = items.firstOrNull()?.shape ?: VLCTvCardShape.Landscape,
    columns: Int = shape.gridColumns(),
    labels: VLCTvBrowserLabels = VLCTvBrowserLabels(),
    sortOptions: List<VLCTvSortOption> = emptyList(),
    favorite: Boolean? = null,
    onBack: (() -> Unit)? = null,
    onToggleDisplay: (() -> Unit)? = null,
    onSort: (Int) -> Unit = {},
    onFavorite: () -> Unit = {},
    onItemFocused: ((Int) -> Unit)? = null,
    onItemLongClick: ((Int) -> Unit)? = null,
    /** Change this (e.g. to the folder path) to move focus back to the first item. */
    focusResetKey: Any? = null,
    artwork: VLCTvArtwork = { VLCTvSymbolArtwork(it.symbol) },
) {
    // A new location (folder, category) gets its own one-shot initial focus.
    val initialFocus = remember(focusResetKey) { VLCTvInitialFocus() }
    VLCTvScreen(modifier) {
        Column(Modifier.fillMaxSize().padding(top = VLCTvLayout.OverscanVertical)) {
            VLCTvTopBar(
                title = title,
                eyebrow = eyebrow,
                subtitle = subtitle,
                onBack = onBack,
                backContentDescription = labels.back,
                modifier = Modifier.padding(horizontal = VLCTvLayout.OverscanHorizontal),
            ) {
                if (loading && items.isNotEmpty()) VLCTvActivityIndicator()
                if (favorite != null) {
                    VLCTvIconButton(
                        contentDescription = if (favorite) labels.removeFavorite else labels.addFavorite,
                        onClick = onFavorite,
                        icon = if (favorite) MaterialSymbols.Filled.Star else MaterialSymbols.Outlined.Star,
                        active = favorite,
                        filled = true,
                    )
                }
                if (sortOptions.isNotEmpty()) VLCTvSortButton(labels.sort, sortOptions, onSort)
                if (onToggleDisplay != null) {
                    VLCTvIconButton(
                        contentDescription = if (inGrid) labels.showAsList else labels.showAsGrid,
                        onClick = onToggleDisplay,
                        icon = if (inGrid) MaterialSymbols.Filled.ViewList else MaterialSymbols.Filled.GridView,
                        filled = true,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f)) {
                when {
                    items.isEmpty() && loading -> VLCTvLoading()
                    items.isEmpty() -> VLCTvEmptyState(title = emptyTitle, message = emptyMessage, symbol = emptySymbol)
                    inGrid -> LazyVerticalGrid(
                        columns = GridCells.Fixed(columns.coerceAtLeast(1)),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = VLCTvLayout.OverscanHorizontal,
                            end = VLCTvLayout.OverscanHorizontal,
                            top = 12.dp,
                            bottom = VLCTvLayout.OverscanVertical + 24.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(VLCTvLayout.CardGap),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        itemsIndexed(items, key = { index, item -> "$index-${item.key}" }) { index, item ->
                            VLCTvMediaCard(
                                item = item.copy(shape = shape),
                                width = null,
                                onClick = { onItemClick(index) },
                                onLongClick = onItemLongClick?.let { { it(index) } },
                                onFocused = onItemFocused?.let { { it(index) } },
                                modifier = if (index == 0) Modifier.vlcTvInitialFocus(initialFocus) else Modifier,
                                artwork = artwork,
                            )
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = VLCTvLayout.OverscanHorizontal,
                            end = VLCTvLayout.OverscanHorizontal,
                            top = 8.dp,
                            bottom = VLCTvLayout.OverscanVertical + 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(items, key = { index, item -> "$index-${item.key}" }) { index, item ->
                            VLCTvListRow(
                                item = item.copy(shape = shape),
                                onClick = { onItemClick(index) },
                                onLongClick = onItemLongClick?.let { { it(index) } },
                                onFocused = onItemFocused?.let { { it(index) } },
                                modifier = if (index == 0) Modifier.vlcTvInitialFocus(initialFocus) else Modifier,
                                artwork = artwork,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VLCTvSortButton(label: String?, options: List<VLCTvSortOption>, onSort: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VLCTvIconButton(
            contentDescription = label,
            onClick = { expanded = true },
            icon = MaterialSymbols.Filled.Sort,
            filled = true,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
        ) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = MaterialTheme.typography.titleMedium) },
                    onClick = {
                        expanded = false
                        onSort(option.id)
                    },
                )
            }
        }
    }
}
