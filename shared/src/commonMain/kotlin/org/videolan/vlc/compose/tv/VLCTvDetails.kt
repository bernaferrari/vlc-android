@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.videolan.vlc.compose.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.MaterialIcon

/** One action in a details hero. [primary] actions get the accent tint even when unfocused. */
@Immutable
data class VLCTvAction(
    val id: Int,
    val label: String,
    val icon: MaterialIcon? = null,
    val primary: Boolean = false,
)

/**
 * Details hero: large artwork beside an eyebrow, title, subtitle, a quiet metadata line, a short
 * synopsis and the action row. Initial focus goes to the first action.
 */
@Composable
fun VLCTvDetailsHero(
    title: String,
    actions: List<VLCTvAction>,
    onAction: (VLCTvAction) -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    metadata: List<String> = emptyList(),
    description: String? = null,
    artworkShape: VLCTvCardShape = VLCTvCardShape.Poster,
    requestInitialFocus: Boolean = true,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val initialFocus = rememberVLCTvInitialFocus()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VLCTvLayout.OverscanHorizontal),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (artwork != null) {
            val artworkWidth = when (artworkShape) {
                VLCTvCardShape.Poster -> 196.dp
                VLCTvCardShape.Square -> 232.dp
                VLCTvCardShape.Landscape -> 320.dp
            }
            val shape = RoundedCornerShape(VLCTvLayout.CardCorner)
            Box(
                modifier = Modifier
                    .width(artworkWidth)
                    .aspectRatio(artworkShape.aspectRatio)
                    .shadow(24.dp, shape, clip = false, ambientColor = scheme.scrim.copy(alpha = 0.5f), spotColor = scheme.scrim.copy(alpha = 0.5f))
                    .clip(shape)
                    .background(scheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
                content = artwork,
            )
        }
        Column(Modifier.weight(1f)) {
            eyebrow?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val meta = metadata.filter { it.isNotBlank() }
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    meta.forEach { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(scheme.onSurface.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface.copy(alpha = 0.86f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = VLCTvLayout.ReadingWidth),
                )
            }
            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                // Actions wrap instead of scrolling: every choice stays visible inside the safe area.
                FlowRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    actions.forEachIndexed { index, action ->
                        VLCTvButton(
                            text = action.label,
                            icon = action.icon,
                            style = if (action.primary) VLCTvButtonStyle.Primary else VLCTvButtonStyle.Tonal,
                            onClick = { onAction(action) },
                            modifier = if (index == 0 && requestInitialFocus) Modifier.vlcTvInitialFocus(initialFocus) else Modifier,
                            previewKey = "action-${action.id}",
                        )
                    }
                }
            }
        }
    }
}

/**
 * Complete details page: optional blurred backdrop, the hero, then shelves (seasons, cast,
 * artwork choices). Used for media files, folders, movies and TV shows.
 */
@Composable
fun VLCTvDetailsScreen(
    title: String,
    actions: List<VLCTvAction>,
    onAction: (VLCTvAction) -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    metadata: List<String> = emptyList(),
    description: String? = null,
    artworkShape: VLCTvCardShape = VLCTvCardShape.Poster,
    shelves: List<VLCTvShelfModel> = emptyList(),
    onShelfItemClick: (shelfIndex: Int, itemIndex: Int) -> Unit = { _, _ -> },
    loading: Boolean = false,
    requestInitialFocus: Boolean = true,
    backdrop: (@Composable BoxScope.() -> Unit)? = null,
    heroArtwork: (@Composable BoxScope.() -> Unit)? = null,
    artwork: VLCTvArtwork = { VLCTvSymbolArtwork(it.symbol) },
) {
    VLCTvScreen(modifier, backdrop = backdrop) {
        if (loading) {
            VLCTvLoading()
            return@VLCTvScreen
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = VLCTvLayout.OverscanVertical + 16.dp, bottom = VLCTvLayout.OverscanVertical + 24.dp),
            verticalArrangement = Arrangement.spacedBy(VLCTvLayout.ShelfGap),
        ) {
            item(key = "hero") {
                VLCTvDetailsHero(
                    title = title,
                    actions = actions,
                    onAction = onAction,
                    eyebrow = eyebrow,
                    subtitle = subtitle,
                    metadata = metadata,
                    description = description,
                    artworkShape = artworkShape,
                    requestInitialFocus = requestInitialFocus,
                    artwork = heroArtwork,
                )
            }
            itemsIndexed(shelves, key = { _, shelf -> shelf.key }) { shelfIndex, shelf ->
                if (shelf.items.isNotEmpty()) {
                    VLCTvItemShelf(
                        title = shelf.title,
                        items = shelf.items,
                        onItemClick = { onShelfItemClick(shelfIndex, it) },
                        artwork = artwork,
                    )
                }
            }
        }
    }
}
