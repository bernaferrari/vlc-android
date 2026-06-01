package org.videolan.vlc.gui.audio

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.R
import org.videolan.vlc.media.MediaUtils

internal data class AudioPlaylistScrollRequest(val index: Int, val serial: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioPlaylistQueue(
    items: List<MediaWrapper>,
    currentIndex: Int,
    playing: Boolean,
    showTrackNumbers: Boolean,
    showReorderButtons: Boolean,
    showInlineActions: Boolean,
    stopAfter: Int,
    scrollRequest: AudioPlaylistScrollRequest?,
    onScrollRequestConsumed: () -> Unit,
    onPlayItem: (position: Int, item: MediaWrapper) -> Unit,
    onShowContext: (position: Int, item: MediaWrapper) -> Unit,
    onDismissItem: (position: Int, item: MediaWrapper) -> Unit,
    onMoveItem: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    bottomPaddingOverride: Dp? = null
) {
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val bottomPadding = bottomPaddingOverride ?: if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        dimensionResource(R.dimen.listview_bottom_padding)
    } else {
        68.dp
    }

    LaunchedEffect(scrollRequest, items.size) {
        val request = scrollRequest ?: return@LaunchedEffect
        if (request.index in items.indices) listState.scrollToItem(request.index)
        onScrollRequestConsumed()
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.id}:${item.uri}:$index" }
            ) { index, item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            onDismissItem(index, item)
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { AudioPlaylistDismissBackground() },
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true
                ) {
                    AudioPlaylistMediaItem(
                        media = item,
                        subtitle = MediaUtils.getMediaSubtitle(item),
                        showTrackNumbers = showTrackNumbers,
                        showReorderButtons = showReorderButtons && showInlineActions,
                        showDeleteButton = showInlineActions,
                        stopAfterThis = stopAfter == index,
                        current = currentIndex == index,
                        playing = playing,
                        onClick = { onPlayItem(index, item) },
                        onMoveUpClick = { onMoveItem(index, index - 1) },
                        onMoveDownClick = { onMoveItem(index, index + 1) },
                        onDeleteClick = { onDismissItem(index, item) },
                        onMoreClick = { onShowContext(index, item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPlaylistDismissBackground() {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE53935).copy(alpha = 0.22f))
            .padding(horizontal = 24.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_playlist_delete),
            contentDescription = null,
            tint = Color(0xFFE53935)
        )
    }
}
