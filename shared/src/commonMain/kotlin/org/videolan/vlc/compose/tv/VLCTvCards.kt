package org.videolan.vlc.compose.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon

/**
 * Platform-neutral description of one TV tile. Hosts map their own media models (medialibrary
 * items, scraped metadata, settings shortcuts) onto this and draw the artwork through a slot, so
 * the layout itself can be previewed and reused anywhere.
 */
@Immutable
data class VLCTvItem(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    /** Short technical tag such as "1080p" or "4K", drawn as a chip on the artwork. */
    val badge: String? = null,
    /** Playback progress in 0..1, drawn as a thin accent bar along the artwork's bottom edge. */
    val progress: Float? = null,
    /** Symbol used when the tile is an action (Settings, About...) or has no artwork at all. */
    val symbol: MaterialIcon? = null,
    /** Small status glyph drawn in the artwork corner, e.g. "now playing" or "watched". */
    val statusIcon: MaterialIcon? = null,
    val shape: VLCTvCardShape = VLCTvCardShape.Landscape,
)

/** Artwork renderer supplied by the host: Android loads thumbnails, previews draw fixtures. */
typealias VLCTvArtwork = @Composable BoxScope.(item: VLCTvItem) -> Unit

/** Default artwork: a quiet tonal block with the item's symbol, used for actions and fallbacks. */
@Composable
fun VLCTvSymbolArtwork(symbol: MaterialIcon?, modifier: Modifier = Modifier, focused: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = if (focused) 0.24f else 0.12f),
                        scheme.surfaceContainerHigh,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (symbol != null) {
            Icon(symbol, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(40.dp))
        }
    }
}

/**
 * Large media card: artwork carries the focus lift and outline, title and subtitle sit beneath it
 * (Google TV / Apple TV convention) so text never fights the image. The title brightens with focus.
 */
@Composable
fun VLCTvMediaCard(
    item: VLCTvItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Fixed card width; `null` fills the parent (grid cells). */
    width: Dp? = item.shape.width,
    onLongClick: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    showText: Boolean = item.title.isNotBlank(),
    artwork: VLCTvArtwork = { VLCTvSymbolArtwork(it.symbol) },
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())) {
        VLCTvSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(item.shape.aspectRatio),
            containerColor = scheme.surfaceContainerHigh,
            contentDescription = item.title,
            previewKey = item.key,
            onFocusChanged = { if (it) onFocused?.invoke() },
        ) {
            artwork(item)
            VLCTvArtworkDecorations(item)
        }
        if (showText) {
            Spacer(Modifier.height(VLCTvLayout.CardTitleGap))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Compact tile for shortcuts (Settings, About, Refresh...): symbol and label live inside the tile,
 * so utility rows stay visibly lighter than media shelves.
 */
@Composable
fun VLCTvShortcutTile(
    item: VLCTvItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    VLCTvSurface(
        onClick = onClick,
        modifier = modifier.widthIn(min = 180.dp, max = 280.dp).height(88.dp),
        containerColor = scheme.onSurface.copy(alpha = 0.07f),
        focusedContainerColor = scheme.primary,
        contentColor = scheme.onSurface,
        focusedContentColor = scheme.onPrimary,
        outline = false,
        contentDescription = item.title,
        previewKey = item.key,
        onFocusChanged = { if (it) onFocused?.invoke() },
    ) { focused ->
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item.symbol?.let {
                Icon(it, contentDescription = null, tint = if (focused) scheme.onPrimary else scheme.primary, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (focused) scheme.onPrimary.copy(alpha = 0.8f) else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Badge, status glyph and progress bar layered over artwork in a consistent position. */
@Composable
fun BoxScope.VLCTvArtworkDecorations(item: VLCTvItem) {
    val scheme = MaterialTheme.colorScheme
    item.badge?.takeIf { it.isNotBlank() }?.let { badge ->
        Text(
            text = badge,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
    item.statusIcon?.let { icon ->
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
    item.progress?.takeIf { it > 0f }?.let { progress ->
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(scheme.primary),
            )
        }
    }
}

/**
 * Wide list row for the browser's list mode and for search-like result lists: artwork thumb,
 * two lines of text and an optional trailing tag. The whole row lifts on focus.
 */
@Composable
fun VLCTvListRow(
    item: VLCTvItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    artwork: VLCTvArtwork = { VLCTvSymbolArtwork(it.symbol) },
) {
    val scheme = MaterialTheme.colorScheme
    VLCTvSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 84.dp),
        focusedScale = 1.02f,
        containerColor = scheme.onSurface.copy(alpha = 0.05f),
        focusedContainerColor = scheme.surfaceContainerHigh,
        contentDescription = item.title,
        previewKey = item.key,
        onFocusChanged = { if (it) onFocused?.invoke() },
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val thumbWidth = when (item.shape) {
                VLCTvCardShape.Landscape -> 112.dp
                VLCTvCardShape.Poster -> 44.dp
                VLCTvCardShape.Square -> 64.dp
            }
            Box(
                Modifier
                    .width(thumbWidth)
                    .aspectRatio(item.shape.aspectRatio)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surfaceContainerHighest),
            ) {
                artwork(item)
                item.progress?.takeIf { it > 0f }?.let {
                    VLCTvArtworkDecorations(item.copy(badge = null, statusIcon = null))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item.statusIcon?.let { Icon(it, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp)) }
            item.badge?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.onSurface.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}
