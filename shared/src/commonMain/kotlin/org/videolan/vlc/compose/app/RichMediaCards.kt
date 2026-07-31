@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCMediaCardShape
import org.videolan.vlc.compose.components.highlightedSearchText
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption

@Composable
internal fun MediaContextMenu(
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
    val motion = LocalVLCMotion.current
    var menu by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Standard),
        label = "mediaGridSelection",
    )
    Surface(
        modifier = Modifier
            .clip(VLCMediaCardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        // A media grid repeats this shape many times. The large token still feels expressive
        // without turning dense libraries into a field of oversized pills.
        shape = VLCMediaCardShape,
        color = containerColor,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        val duration = formatDuration(item.duration)
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center,
            ) {
                // The card owns the clipping. Artwork fills the entire media well so a real
                // thumbnail reads as content, not as a smaller card nested inside another card.
                MediaArtwork(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                    size = 220.dp,
                    showFallbackContainer = false,
                    fillMaxSizeArtwork = true,
                )
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    IconButton(
                        onClick = { menu = true },
                        modifier = Modifier
                            .size(40.dp),
                    ) {
                        Icon(
                            MaterialSymbols.Filled.MoreVert,
                            contentDescription = ShellStrings.moreOptions(),
                            tint = Color.White,
                        )
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
                if (duration.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = Color.Black.copy(alpha = 0.64f),
                        contentColor = Color.White,
                    ) {
                        Text(
                            text = duration,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    highlightedSearchText(item.displayTitle, searchQuery),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                val sub = mediaSecondaryText(item, showTrackNumbers, includeDuration = false)
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
}

/** A single de-duplicated metadata line shared by grid and list presentations. */
internal fun mediaSecondaryText(
    item: MediaItem,
    showTrackNumbers: Boolean,
    includeDuration: Boolean = true,
): String {
    val trackNumber = item.trackNumber.takeIf {
        showTrackNumbers && item.isAudio && it > 0
    }?.let { "#$it" }
    val durationValue = formatDuration(item.duration)
    val duration = durationValue.takeIf { includeDuration }
    return listOfNotNull(trackNumber, item.artist, item.album, item.description, duration)
        .map(String::trim)
        .filter { it.isNotBlank() && it != durationValue }
        .distinct()
        .joinToString(" · ")
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
