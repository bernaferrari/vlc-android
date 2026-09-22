@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.artwork.MediaArtworkUri
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCListItemPosition
import org.videolan.vlc.compose.components.VLCSelectionCheckIndicator
import org.videolan.vlc.compose.components.highlightedSearchText
import org.videolan.vlc.compose.components.segmentShape
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption
import kotlinx.coroutines.launch
import org.videolan.vlc.viewmodel.PlayerUiState
import org.videolan.vlc.viewmodel.MediaListUiState

private data class MediaAction(
    val label: String,
    val icon: MaterialIcon,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

private data class MediaActionSection(
    val title: String,
    val actions: List<MediaAction>,
)

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
    if (!expanded) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = VLCThemeDefaults.colors
    fun dismissThen(action: () -> Unit): () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
            action()
        }
    }

    val playbackActions = listOf(
        MediaAction(ShellStrings.play(), MaterialSymbols.Filled.PlayArrow, dismissThen { onPlay(item) }),
        MediaAction(ShellStrings.playNext(), MaterialSymbols.Filled.SkipNext, dismissThen { onPlayNext(item) }),
        MediaAction(ShellStrings.append(), MaterialSymbols.Filled.QueueMusic, dismissThen { onAppend(item) }),
        MediaAction(ShellStrings.playAll(), MaterialSymbols.Filled.PlayArrow, dismissThen { onCtx(item, ContextOption.CTX_PLAY_ALL) }),
    )
    val manageActions = buildList {
        if (canHandleHostAction(ContextOption.CTX_ADD_TO_PLAYLIST)) {
            add(MediaAction(ShellStrings.addToPlaylist(), MaterialSymbols.Filled.QueueMusic, dismissThen {
                onCtx(item, ContextOption.CTX_ADD_TO_PLAYLIST)
            }))
        }
        add(
            MediaAction(
                label = if (item.isFavorite) ShellStrings.removeFavorite() else ShellStrings.addFavorite(),
                icon = MaterialSymbols.Filled.Star,
                onClick = dismissThen {
                    onCtx(item, if (item.isFavorite) ContextOption.CTX_FAV_REMOVE else ContextOption.CTX_FAV_ADD)
                },
            ),
        )
        add(MediaAction(ShellStrings.markPlayed(), MaterialSymbols.Filled.CheckCircle, dismissThen {
            onCtx(item, ContextOption.CTX_MARK_AS_PLAYED)
        }))
        add(MediaAction(ShellStrings.markUnplayed(), MaterialSymbols.Filled.Undo, dismissThen {
            onCtx(item, ContextOption.CTX_MARK_AS_UNPLAYED)
        }))
    }
    val moreActions = buildList {
        if (item.isVideo && canHandleHostAction(ContextOption.CTX_DOWNLOAD_SUBTITLES)) {
            add(MediaAction(ShellStrings.downloadSubtitles(), MaterialSymbols.Filled.Description, dismissThen {
                onCtx(item, ContextOption.CTX_DOWNLOAD_SUBTITLES)
            }))
        }
        if (item.isAudio && canHandleHostAction(ContextOption.CTX_SET_RINGTONE)) {
            add(MediaAction(ShellStrings.setRingtone(), MaterialSymbols.Filled.MusicNote, dismissThen {
                onCtx(item, ContextOption.CTX_SET_RINGTONE)
            }))
        }
        if (canHandleHostAction(ContextOption.CTX_SHARE)) {
            add(MediaAction(ShellStrings.share(), MaterialSymbols.Filled.IosShare, dismissThen {
                onCtx(item, ContextOption.CTX_SHARE)
            }))
        }
        if (canHandleHostAction(ContextOption.CTX_INFORMATION)) {
            add(MediaAction(ShellStrings.info(), MaterialSymbols.Filled.Info, dismissThen {
                onCtx(item, ContextOption.CTX_INFORMATION)
            }))
        }
        if (item.isLocallyRenamable() && canHandleHostAction(ContextOption.CTX_RENAME)) {
            add(MediaAction(ShellStrings.rename(), MaterialSymbols.Filled.Edit, dismissThen {
                onCtx(item, ContextOption.CTX_RENAME)
            }))
        }
        if (item.isLocallyDeletable() && canHandleHostAction(ContextOption.CTX_DELETE)) {
            add(MediaAction(ShellStrings.delete(), MaterialSymbols.Filled.Delete, dismissThen {
                onCtx(item, ContextOption.CTX_DELETE)
            }, destructive = true))
        }
    }
    val sections = listOfNotNull(
        MediaActionSection(ShellStrings.playback(), playbackActions),
        MediaActionSection(ShellStrings.mediaActionsManage(), manageActions).takeIf { it.actions.isNotEmpty() },
        MediaActionSection(ShellStrings.moreActions(), moreActions).takeIf { it.actions.isNotEmpty() },
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundDefault,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VLCLayout.SheetHorizontalPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaArtworkSlot(item)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (item.isVideo) ShellStrings.video() else ShellStrings.audio(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.fontLight,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = VLCLayout.SheetBottomPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                sections.forEachIndexed { sectionIndex, section ->
                    item(key = "media-action-section-$sectionIndex") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                            if (sectionIndex == 0) {
                                // Playback is the frequent path. Four compact icon-over-label
                                // actions keep the sheet scannable instead of making the user
                                // scroll through four full-width rows before reaching management.
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    section.actions.forEach { action ->
                                        MediaActionCompactCell(action)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(VLCLayout.GroupGap)) {
                                    section.actions.forEachIndexed { actionIndex, action ->
                                        val position = when {
                                            section.actions.size == 1 -> VLCListItemPosition.Single
                                            actionIndex == 0 -> VLCListItemPosition.First
                                            actionIndex == section.actions.lastIndex -> VLCListItemPosition.Last
                                            else -> VLCListItemPosition.Middle
                                        }
                                        MediaActionSheetRow(action, position)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.MediaActionCompactCell(action: MediaAction) {
    val contentColor = if (action.destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 72.dp)
            .clip(VLCListItemPosition.Single.segmentShape())
            .clickable(role = Role.Button, onClick = action.onClick),
        shape = VLCListItemPosition.Single.segmentShape(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon = action.icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = contentColor)
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaActionSheetRow(action: MediaAction, position: VLCListItemPosition) {
    val shape = position.segmentShape()
    val contentColor = if (action.destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = action.onClick,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                icon = action.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor,
            )
            Text(
                text = action.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridCard(
    item: MediaItem,
    selected: Boolean,
    artworkAspectRatio: Float = 16f / 9f,
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
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Standard),
        label = "mediaGridSelection",
    )
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .semantics { this.selected = selected }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(artworkAspectRatio)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                MediaArtwork(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                    size = 220.dp,
                    showFallbackContainer = false,
                    fillMaxSizeArtwork = true,
                )
                if (selected) {
                    VLCSelectionCheckIndicator(modifier = Modifier.fillMaxSize())
                }
                val duration = formatDuration(item.duration)
                if (duration.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.64f),
                        contentColor = Color.White,
                    ) {
                        Text(
                            text = duration,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(top = 9.dp, bottom = 4.dp, start = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        highlightedSearchText(item.displayTitle, searchQuery),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val sub = mediaSecondaryText(item, showTrackNumbers, includeDuration = false)
                    if (sub.isNotBlank()) {
                        Text(
                            highlightedSearchText(sub, searchQuery),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.fontLight,
                        )
                    }
                }
                // Keep the thumbnail unobstructed and preserve a full touch target for actions.
                IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        MaterialSymbols.Filled.MoreVert,
                        contentDescription = ShellStrings.moreOptions(),
                        modifier = Modifier.size(20.dp),
                        tint = colors.fontLight,
                    )
                }
            }
        }
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

/** An active session is resumable only while its known position is inside the video. */
internal fun shouldShowContinueWatching(library: MediaListUiState, player: PlayerUiState): Boolean =
    library.containerId == null && library.containerTitle == null && library.openedEntityTitle == null &&
        library.groupingMode == org.videolan.vlc.viewmodel.VideoGroupingMode.NONE &&
        library.query.isBlank() && !library.onlyFavorites && library.selection.isEmpty() &&
        player.hasMedia && player.hasVideoOutput && player.uri.isNotBlank() &&
        player.progress.time > 0L && player.progress.time < player.progress.length

@Composable
internal fun ContinueWatchingCard(
    state: PlayerUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = Color.White,
    ) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            MediaArtworkUri(
                uri = state.artworkUri?.takeIf(String::isNotBlank) ?: state.uri,
                modifier = Modifier.fillMaxSize(),
                size = 400.dp,
                showFallbackContainer = false,
                fillMaxSizeArtwork = true,
                fallback = {
                    Icon(
                        MaterialSymbols.Filled.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.4f))),
                ),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    ShellStrings.resumeVideo(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = state.title.ifBlank {
                                state.queue.getOrNull(state.currentQueueIndex)?.displayTitle
                                    ?: state.uri.substringAfterLast('/')
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${formatDuration(state.progress.time)} / ${formatDuration(state.progress.length)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { state.progress.progressPercent },
                modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
        }
    }
}
