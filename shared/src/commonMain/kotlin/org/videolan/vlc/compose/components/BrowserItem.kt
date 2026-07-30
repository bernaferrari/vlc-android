package org.videolan.vlc.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * A row's position inside a visual section. Segmented lists use a deliberately
 * asymmetric silhouette: soft outside corners and compact corners where items
 * meet, so a long library reads as a calm collection rather than a pile of pills.
 */
enum class VLCListItemPosition {
    Single,
    First,
    Middle,
    Last,
}

/** The shared QuietGuard-inspired outer/inner geometry for segmented rows. */
fun VLCListItemPosition.segmentShape() = when {
    this == VLCListItemPosition.Single -> RoundedCornerShape(16.dp)
    this == VLCListItemPosition.First -> RoundedCornerShape(16.dp, 16.dp, 4.dp, 4.dp)
    this == VLCListItemPosition.Last -> RoundedCornerShape(4.dp, 4.dp, 16.dp, 16.dp)
    else -> RoundedCornerShape(4.dp)
}

/** Restrained media-grid geometry, shared by media and playlist cards. */
val VLCMediaCardShape = RoundedCornerShape(18.dp)

/** Artwork is intentionally a little tighter than its containing media card. */
val VLCArtworkTileShape = RoundedCornerShape(14.dp)

/**
 * Shared Compose row for the core media-browser list item pattern.
 *
 * Traceability: this replaces the former media-browser row/card XML patterns,
 * including audio browser, album track, MRL, and history variants.
 *
 * The former XML combined a stable media icon/artwork box, title/subtitle text,
 * selection background, and trailing actions. This leaf owns that layout and
 * theme behavior while keeping artwork, badges, and action icons as slots so
 * app modules can provide their drawables or async thumbnail hosts. Audio album
 * track rows can hide the artwork slot and reuse the same title/action layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCBrowserItemRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String? = null,
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 1,
    position: VLCListItemPosition = VLCListItemPosition.Single,
    showArtwork: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    artworkContent: @Composable BoxScope.() -> Unit = { DefaultBrowserArtworkContent() },
    badgeContent: @Composable RowScope.() -> Unit = {},
    primaryActionContent: (@Composable () -> Unit)? = null,
    onPrimaryActionClick: () -> Unit = {},
    moreActionContent: (@Composable () -> Unit)? = null,
    onMoreClick: () -> Unit = {}
) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Standard),
        label = "rowContainer",
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        colors.listTitle
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        // Selection is a color state, not a different component. Preserving the group silhouette
        // stops a selected item from becoming an isolated pill in the middle of a section.
        shape = position.segmentShape(),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showArtwork) {
                BrowserArtwork(size = 48.dp, content = artworkContent)
                Spacer(modifier = Modifier.width(16.dp))
            }
            BrowserItemTexts(
                title = title,
                subtitle = subtitle,
                titleColor = contentColor,
                subtitleColor = if (selected) contentColor.copy(alpha = 0.72f) else colors.listSubtitle,
                titleMaxLines = titleMaxLines,
                subtitleMaxLines = subtitleMaxLines,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = badgeContent,
            )
            primaryActionContent?.let { content ->
                IconButton(onClick = onPrimaryActionClick) { content() }
            }
            moreActionContent?.let { content ->
                IconButton(onClick = onMoreClick) { content() }
            }
        }
    }
}

/**
 * Shared Compose card variant for media-browser grid/list-card layouts.
 *
 * Mirrors the former media-browser card branch: selected state, rounded media
 * tile, action row, title, and one-line subtitle. Artwork and action icons are
 * slots for app-side drawable/resource ownership.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCBrowserItemCard(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String? = null,
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 1,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    artworkContent: @Composable BoxScope.() -> Unit = { DefaultBrowserArtworkContent() },
    badgeContent: @Composable RowScope.() -> Unit = {},
    primaryActionContent: (@Composable () -> Unit)? = null,
    onPrimaryActionClick: () -> Unit = {},
    moreActionContent: (@Composable () -> Unit)? = null,
    onMoreClick: () -> Unit = {}
) {
    val motion = LocalVLCMotion.current
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Standard),
        label = "cardContainer",
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = VLCMediaCardShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrowserArtwork(size = 64.dp, content = artworkContent)
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    content = badgeContent
                )
                primaryActionContent?.let { content ->
                    IconButton(onClick = onPrimaryActionClick, modifier = Modifier.size(36.dp)) {
                        content()
                    }
                }
                moreActionContent?.let { content ->
                    IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                        content()
                    }
                }
            }
            BrowserItemTexts(
                title = title,
                subtitle = subtitle,
                titleColor = contentColor,
                subtitleColor = if (selected) {
                    contentColor.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                titleMaxLines = titleMaxLines,
                subtitleMaxLines = subtitleMaxLines
            )
        }
    }
}

@Composable
private fun BrowserArtwork(
    size: Dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(VLCArtworkTileShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun BrowserItemTexts(
    title: String,
    subtitle: String?,
    titleColor: androidx.compose.ui.graphics.Color,
    subtitleColor: androidx.compose.ui.graphics.Color,
    titleMaxLines: Int,
    subtitleMaxLines: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = subtitleColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = subtitleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DefaultBrowserArtworkContent() {
    Icon(
        icon = MaterialSymbols.Filled.VideoLibrary,
        contentDescription = null,
        tint = VLCThemeDefaults.colors.primary,
    )
}

@Composable
private fun PreviewArtworkText(text: String) {
    Text(
        text = text,
        color = VLCThemeDefaults.colors.primary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PreviewActionText(text: String) {
    Text(
        text = text,
        color = VLCThemeDefaults.colors.primary,
        style = MaterialTheme.typography.labelLarge
    )
}
