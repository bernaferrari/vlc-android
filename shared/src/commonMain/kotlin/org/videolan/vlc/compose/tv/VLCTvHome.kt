package org.videolan.vlc.compose.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.icons.VlcCone

/** One horizontal shelf on the TV home screen. */
@Immutable
data class VLCTvShelfModel(
    val key: String,
    val title: String,
    val items: List<VLCTvItem>,
    /** Shelves that only hold shortcuts (Settings, About...) never receive initial focus. */
    val isShortcutShelf: Boolean = false,
)

/**
 * TV home: brand header with search, then one shelf per library section. Initial focus lands on
 * the first real media card once content arrives (never on a shortcut), exactly once.
 */
@Composable
fun VLCTvHomeScreen(
    appName: String,
    shelves: List<VLCTvShelfModel>,
    onItemClick: (shelfIndex: Int, itemIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    searchLabel: String? = null,
    onSearch: (() -> Unit)? = null,
    loading: Boolean = false,
    onItemFocused: ((shelfIndex: Int, itemIndex: Int) -> Unit)? = null,
    brand: @Composable () -> Unit = { Image(VlcCone, contentDescription = null, modifier = Modifier.size(44.dp)) },
    artwork: VLCTvArtwork = { VLCTvSymbolArtwork(it.symbol) },
) {
    val initialFocus = rememberVLCTvInitialFocus()
    val firstContentShelf = shelves.indexOfFirst { !it.isShortcutShelf && it.items.isNotEmpty() }
    VLCTvScreen(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = VLCTvLayout.OverscanVertical, bottom = VLCTvLayout.OverscanVertical + 24.dp),
            verticalArrangement = Arrangement.spacedBy(VLCTvLayout.ShelfGap - 10.dp),
        ) {
            item(key = "header") {
                VLCTvHomeHeader(
                    appName = appName,
                    loading = loading,
                    searchLabel = searchLabel,
                    onSearch = onSearch,
                    brand = brand,
                )
            }
            itemsIndexed(shelves, key = { _, shelf -> shelf.key }) { shelfIndex, shelf ->
                if (shelf.items.isNotEmpty()) {
                    VLCTvItemShelf(
                        title = shelf.title,
                        items = shelf.items,
                        onItemClick = { onItemClick(shelfIndex, it) },
                        onItemFocused = onItemFocused?.let { callback -> { callback(shelfIndex, it) } },
                        initialFocus = if (shelfIndex == firstContentShelf) initialFocus else null,
                        shortcuts = shelf.isShortcutShelf,
                        artwork = artwork,
                    )
                }
            }
        }
    }
}

@Composable
private fun VLCTvHomeHeader(
    appName: String,
    loading: Boolean,
    searchLabel: String?,
    onSearch: (() -> Unit)?,
    brand: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VLCTvLayout.OverscanHorizontal)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        brand()
        Spacer(Modifier.width(14.dp))
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        if (loading) {
            VLCTvActivityIndicator()
            Spacer(Modifier.width(20.dp))
        }
        if (onSearch != null) {
            if (searchLabel != null) {
                VLCTvButton(text = searchLabel, onClick = onSearch, icon = MaterialSymbols.Filled.Search)
            } else {
                VLCTvIconButton(contentDescription = null, onClick = onSearch, icon = MaterialSymbols.Filled.Search, filled = true)
            }
        }
    }
}
