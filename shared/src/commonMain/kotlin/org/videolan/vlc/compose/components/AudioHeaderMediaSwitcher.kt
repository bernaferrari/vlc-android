package org.videolan.vlc.compose.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

enum class VLCAudioMediaSwitchTarget {
    Previous,
    Current,
    Next
}

data class VLCAudioHeaderMediaSwitcherItem(
    val target: VLCAudioMediaSwitchTarget,
    val title: String,
    val artist: String,
    val cover: ImageBitmap?
)

data class VLCAudioHeaderMediaSwitcherState(
    val items: List<VLCAudioHeaderMediaSwitcherItem> = emptyList(),
    val currentPage: Int = 0,
    val marquee: Boolean = false
)

/**
 * Compose replacement for the collapsed audio-player HeaderMediaSwitcher ViewGroup
 *
 * The host still owns playback-service data collection and previous/next
 * callbacks. This leaf owns the header page layout, swipe paging, tap, and
 * long-press behavior
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCAudioHeaderMediaSwitcher(
    state: VLCAudioHeaderMediaSwitcherState,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onMediaSwitching: () -> Unit = {},
    onMediaSwitched: (VLCAudioMediaSwitchTarget) -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    VLCTheme {
        if (state.items.isEmpty()) {
            Spacer(modifier.fillMaxSize())
            return@VLCTheme
        }

        val currentPage = state.currentPage.coerceIn(0, state.items.lastIndex)
        val pagerState = rememberPagerState(
            initialPage = currentPage,
            pageCount = { state.items.size }
        )
        var suppressSwitchCallbacks by remember { mutableStateOf(false) }
        var lastHostPage by remember(state.items, currentPage) { mutableIntStateOf(currentPage) }

        LaunchedEffect(state.items, currentPage) {
            suppressSwitchCallbacks = true
            pagerState.scrollToPage(currentPage)
            lastHostPage = currentPage
            suppressSwitchCallbacks = false
        }

        LaunchedEffect(pagerState, state.items) {
            snapshotFlow { pagerState.isScrollInProgress }
                .collect { scrolling ->
                    if (scrolling) onMediaSwitching()
                }
        }

        LaunchedEffect(pagerState, state.items) {
            snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
                .collect { (page, scrolling) ->
                    if (!scrolling && !suppressSwitchCallbacks && page != lastHostPage) {
                        lastHostPage = page
                        state.items.getOrNull(page)?.let { item ->
                            onMediaSwitched(item.target)
                        }
                    }
                }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier
                .fillMaxSize()
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .semantics { this.contentDescription = contentDescription }
        ) { page ->
            HeaderSwitcherPage(
                item = state.items[page],
                marquee = state.marquee
            )
        }
    }
}

@Composable
private fun HeaderSwitcherPage(
    item: VLCAudioHeaderMediaSwitcherItem,
    marquee: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.cover?.let { cover ->
            Image(
                bitmap = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            HeaderSwitcherText(
                text = item.title,
                marquee = marquee,
                fontSize = 16,
                fontWeight = FontWeight.Medium
            )
            if (item.artist.isNotEmpty()) {
                HeaderSwitcherText(
                    text = item.artist,
                    marquee = marquee,
                    fontSize = 14,
                    color = VLCThemeDefaults.colors.fontAudioLight
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderSwitcherText(
    text: String,
    marquee: Boolean,
    fontSize: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    color: androidx.compose.ui.graphics.Color = VLCThemeDefaults.colors.fontDefault
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = if (marquee) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (marquee) modifier.basicMarquee(iterations = 1) else modifier
    )
}

