package org.videolan.vlc.gui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.util.isOTG
import org.videolan.vlc.util.isSD
import org.videolan.vlc.util.isSchemeNetwork

@Composable
fun HistoryScreenContent(
    items: List<MediaWrapper>,
    loading: Boolean,
    selectedPositions: Set<Int>,
    onRefresh: () -> Unit,
    onItemClicked: (Int, MediaWrapper) -> Unit,
    onItemLongClicked: (Int, MediaWrapper) -> Unit,
    onRemoveClicked: (Int, MediaWrapper) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundDefault)
        ) {
            when {
                items.isNotEmpty() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                    ) {
                        itemsIndexed(items, key = { index, media -> "${media.id}:${media.uri}:$index" }) { index, media ->
                            HistoryMediaRow(
                                media = media,
                                selected = index in selectedPositions,
                                selectionMode = selectedPositions.isNotEmpty(),
                                onClick = { onItemClicked(index, media) },
                                onLongClick = { onItemLongClicked(index, media) },
                                onRemove = { onRemoveClicked(index, media) }
                            )
                        }
                    }
                }
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    HistoryEmptyState(onRefresh = onRefresh, modifier = Modifier.align(Alignment.Center))
                }
            }
            if (loading && items.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .size(28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryMediaRow(
    media: MediaWrapper,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val enabledAlpha = if (media.isPresent) 1f else 0.48f
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) colors.subtleSelection else colors.backgroundDefault)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
        ) {
            HistoryMediaArt(media = media, modifier = Modifier.alpha(enabledAlpha))
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .alpha(enabledAlpha)
            ) {
                Text(
                    text = media.title.orEmpty(),
                    color = colors.listTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!media.description.isNullOrEmpty()) {
                    Text(
                        text = media.description.orEmpty(),
                        color = colors.listSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = onRemove, enabled = !selectionMode) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.remove_from_history),
                    tint = if (selectionMode) colors.fontDisabled else colors.fontLight
                )
            }
        }
        HorizontalDivider(color = colors.defaultDivider)
    }
}

@Composable
private fun HistoryMediaArt(media: MediaWrapper, modifier: Modifier = Modifier) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = modifier
            .width(80.dp)
            .aspectRatio(if (media.type == MediaWrapper.TYPE_VIDEO) 16f / 10f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.backgroundDefaultDarker),
        contentAlignment = Alignment.Center
    ) {
        VlcMediaImage(
            item = media,
            width = 80.dp,
            fallbackPainter = painterResource(if (media.type == MediaWrapper.TYPE_VIDEO) R.drawable.ic_video_big else R.drawable.ic_song_big),
            fallbackModifier = Modifier.size(36.dp),
            fallbackColorFilter = ColorFilter.tint(colors.fontLight),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        HistoryBadge(media = media, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun HistoryBadge(media: MediaWrapper, modifier: Modifier = Modifier) {
    val icon = when {
        !media.isPresent -> R.drawable.ic_emoji_absent
        media.uri.scheme.isSchemeNetwork() -> R.drawable.ic_emoji_network
        media.uri.isSD() -> R.drawable.ic_emoji_sd
        media.uri.isOTG() -> R.drawable.ic_emoji_otg
        else -> null
    } ?: return
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
            .padding(3.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun HistoryEmptyState(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VLCThemeDefaults.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(24.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = colors.emptyForeground,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = stringResource(R.string.nohistory),
            color = colors.emptyTitle,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        TextButton(onClick = onRefresh, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = stringResource(R.string.refresh), color = colors.primary)
        }
    }
}
