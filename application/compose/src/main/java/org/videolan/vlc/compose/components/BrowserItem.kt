package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

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
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val selectionBackground by animateColorAsState(
            targetValue = if (selected) colors.primary.copy(alpha = 0.10f) else Color.Transparent,
            animationSpec = tween(VLCMotion.DurationShort, easing = VLCMotion.Standard),
            label = "rowSelection"
        )
        val rowModifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(selectionBackground)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)

        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showArtwork) {
                BrowserArtwork(size = 48.dp, content = artworkContent)
                Spacer(modifier = Modifier.width(16.dp))
            }
            BrowserItemTexts(
                title = title,
                subtitle = subtitle,
                titleMaxLines = titleMaxLines,
                subtitleMaxLines = subtitleMaxLines,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = badgeContent
            )
            primaryActionContent?.let { content ->
                IconButton(onClick = onPrimaryActionClick) {
                    content()
                }
            }
            moreActionContent?.let { content ->
                IconButton(onClick = onMoreClick) {
                    content()
                }
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
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val borderColor by animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            animationSpec = tween(VLCMotion.DurationShort, easing = VLCMotion.Standard),
            label = "cardBorder"
        )
        Column(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .background(if (selected) colors.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer)
                .border(if (selected) 2.dp else 1.dp, borderColor, MaterialTheme.shapes.medium)
                .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrowserArtwork(size = 56.dp, content = artworkContent)
                Spacer(modifier = Modifier.width(8.dp))
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
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun BrowserItemTexts(
    title: String,
    subtitle: String?,
    titleMaxLines: Int,
    subtitleMaxLines: Int,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = colors.listTitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = colors.listSubtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = subtitleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DefaultBrowserArtworkContent() {
    Text(
        text = "*",
        color = VLCThemeDefaults.colors.primary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Preview(
    name = "VLCBrowserItemRow - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 420,
    heightDp = 84
)
@Composable
private fun VLCBrowserItemRowLightPreview() {
    VLCTheme(darkTheme = false) {
        Surface(color = VLCThemeDefaults.colors.backgroundDefault) {
            VLCBrowserItemRow(
                title = "Big Buck Bunny",
                subtitle = "Video - 1920x1080 - 9:56",
                primaryActionContent = { PreviewActionText("P") },
                moreActionContent = { PreviewActionText("M") },
                artworkContent = { PreviewArtworkText("V") }
            )
        }
    }
}

@Preview(
    name = "VLCBrowserItemRow - Dark Selected",
    showBackground = true,
    backgroundColor = 0xFF131313,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 420,
    heightDp = 84
)
@Composable
private fun VLCBrowserItemRowDarkSelectedPreview() {
    VLCTheme(darkTheme = true) {
        Surface(color = VLCThemeDefaults.colors.backgroundDefault) {
            VLCBrowserItemRow(
                title = "Selected playlist with a longer title that ellipsizes",
                subtitle = "42 tracks",
                selected = true,
                primaryActionContent = { PreviewActionText("P") },
                moreActionContent = { PreviewActionText("M") },
                artworkContent = { PreviewArtworkText("L") }
            )
        }
    }
}

@Preview(
    name = "VLCBrowserItemCard - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 220,
    heightDp = 150
)
@Composable
private fun VLCBrowserItemCardLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCBrowserItemCard(
            title = "Camera",
            subtitle = "/storage/emulated/0/DCIM",
            primaryActionContent = { PreviewActionText("P") },
            moreActionContent = { PreviewActionText("M") },
            artworkContent = { PreviewArtworkText("F") },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(
    name = "VLCBrowserItemCard - Dark Selected",
    showBackground = true,
    backgroundColor = 0xFF131313,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 220,
    heightDp = 150
)
@Composable
private fun VLCBrowserItemCardDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCBrowserItemCard(
            title = "Network share",
            subtitle = "smb://media.local",
            selected = true,
            moreActionContent = { PreviewActionText("M") },
            artworkContent = { PreviewArtworkText("N") },
            modifier = Modifier.padding(8.dp)
        )
    }
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
