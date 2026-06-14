package org.videolan.vlc.compose.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

data class VLCAudioCoverMediaSwitcherItem(
    val target: VLCAudioMediaSwitchTarget,
    val title: String,
    val subtitle: String,
    val trackInfo: String,
    val cover: ImageBitmap?,
    val showChapterButtons: Boolean
)

data class VLCAudioCoverMediaSwitcherState(
    val items: List<VLCAudioCoverMediaSwitcherItem> = emptyList(),
    val currentPage: Int = 0,
    val showTrackInfo: Boolean = false,
    val marquee: Boolean = false
)

/**
 * Compose replacement for the full-cover audio-player CoverMediaSwitcher
 *
 * The host owns PlaybackService data collection and action callbacks. This
 * leaf owns cover/title/track-info layout, chapter buttons, and horizontal
 * paging between previous/current/next media
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCAudioCoverMediaSwitcher(
    state: VLCAudioCoverMediaSwitcherState,
    modifier: Modifier = Modifier,
    onMediaSwitching: () -> Unit = {},
    onMediaSwitched: (VLCAudioMediaSwitchTarget) -> Unit = {},
    onTextClick: () -> Unit = {},
    onPreviousChapterClick: () -> Unit = {},
    onNextChapterClick: () -> Unit = {},
    fallbackCoverContent: @Composable (ContentScale, Modifier) -> Unit = { _, fallbackModifier ->
        Spacer(fallbackModifier)
    },
    previousChapterIcon: @Composable () -> Unit = {},
    nextChapterIcon: @Composable () -> Unit = {}
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
        var userScrollWasInProgress by remember { mutableStateOf(false) }

        LaunchedEffect(state.items, currentPage) {
            suppressSwitchCallbacks = true
            pagerState.scrollToPage(currentPage)
            userScrollWasInProgress = false
            suppressSwitchCallbacks = false
        }

        LaunchedEffect(pagerState, state.items) {
            snapshotFlow { pagerState.isScrollInProgress }
                .collect { scrolling ->
                    if (suppressSwitchCallbacks) return@collect
                    if (scrolling) {
                        userScrollWasInProgress = true
                        onMediaSwitching()
                    } else if (userScrollWasInProgress) {
                        userScrollWasInProgress = false
                        state.items.getOrNull(pagerState.currentPage)?.let { item ->
                            onMediaSwitched(item.target)
                        }
                    }
                }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize()
        ) { page ->
            CoverSwitcherPage(
                item = state.items[page],
                showTrackInfo = state.showTrackInfo,
                marquee = state.marquee,
                onTextClick = onTextClick,
                onPreviousChapterClick = onPreviousChapterClick,
                onNextChapterClick = onNextChapterClick,
                fallbackCoverContent = fallbackCoverContent,
                previousChapterIcon = previousChapterIcon,
                nextChapterIcon = nextChapterIcon
            )
        }
    }
}

@Composable
private fun CoverSwitcherPage(
    item: VLCAudioCoverMediaSwitcherItem,
    showTrackInfo: Boolean,
    marquee: Boolean,
    onTextClick: () -> Unit,
    onPreviousChapterClick: () -> Unit,
    onNextChapterClick: () -> Unit,
    fallbackCoverContent: @Composable (ContentScale, Modifier) -> Unit,
    previousChapterIcon: @Composable () -> Unit,
    nextChapterIcon: @Composable () -> Unit
) {
    // TODO: Replace with multiplatform screen info (BoxWithConstraints or LocalWindowInfo)
    val isLandscape = false
    val isLargeLandscape = false

    if (isLandscape) {
        CoverSwitcherLandscapePage(
            item = item,
            large = isLargeLandscape,
            fallbackCoverContent = fallbackCoverContent
        )
    } else {
        CoverSwitcherPortraitPage(
            item = item,
            showTrackInfo = showTrackInfo,
            marquee = marquee,
            onTextClick = onTextClick,
            onPreviousChapterClick = onPreviousChapterClick,
            onNextChapterClick = onNextChapterClick,
            fallbackCoverContent = fallbackCoverContent,
            previousChapterIcon = previousChapterIcon,
            nextChapterIcon = nextChapterIcon
        )
    }
}

@Composable
private fun CoverSwitcherLandscapePage(
    item: VLCAudioCoverMediaSwitcherItem,
    large: Boolean,
    fallbackCoverContent: @Composable (ContentScale, Modifier) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val horizontalPadding = if (large) 48.dp else 0.dp
        val verticalPadding = if (large) 40.dp else 0.dp
        val coverSize = minOf(
            (maxWidth - horizontalPadding).coerceAtLeast(64.dp),
            (maxHeight - verticalPadding).coerceAtLeast(64.dp)
        )

        CoverArt(
            cover = item.cover,
            contentScale = if (large) ContentScale.Crop else ContentScale.Fit,
            elevation = if (large) 12.dp else 16.dp,
            fallbackCoverContent = fallbackCoverContent,
            modifier = Modifier.size(coverSize)
        )
    }
}

@Composable
private fun CoverSwitcherPortraitPage(
    item: VLCAudioCoverMediaSwitcherItem,
    showTrackInfo: Boolean,
    marquee: Boolean,
    onTextClick: () -> Unit,
    onPreviousChapterClick: () -> Unit,
    onNextChapterClick: () -> Unit,
    fallbackCoverContent: @Composable (ContentScale, Modifier) -> Unit,
    previousChapterIcon: @Composable () -> Unit,
    nextChapterIcon: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val textReserve = if (showTrackInfo && item.trackInfo.isNotEmpty()) 160.dp else 128.dp
        val coverSize = minOf(
            maxWidth,
            (maxHeight - textReserve).coerceAtLeast(96.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CoverArt(
                cover = item.cover,
                contentScale = ContentScale.Crop,
                elevation = 16.dp,
                fallbackCoverContent = fallbackCoverContent,
                modifier = Modifier.size(coverSize)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (item.showChapterButtons) {
                    ChapterButton(onClick = onPreviousChapterClick) {
                        previousChapterIcon()
                    }
                }
                CoverSwitcherText(
                    text = item.title,
                    marquee = marquee,
                    color = VLCThemeDefaults.colors.fontDefault,
                    fontSize = 24,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onTextClick)
                )
                if (item.showChapterButtons) {
                    ChapterButton(onClick = onNextChapterClick) {
                        nextChapterIcon()
                    }
                }
            }

            if (item.subtitle.isNotEmpty()) {
                CoverSwitcherText(
                    text = item.subtitle,
                    marquee = marquee,
                    color = VLCThemeDefaults.colors.fontAudioLight,
                    fontSize = 14,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable(onClick = onTextClick)
                )
            }

            if (showTrackInfo && item.trackInfo.isNotEmpty()) {
                CoverSwitcherText(
                    text = item.trackInfo,
                    marquee = marquee,
                    color = VLCThemeDefaults.colors.fontAudioLight,
                    fontSize = 12,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CoverArt(
    cover: ImageBitmap?,
    contentScale: ContentScale,
    elevation: androidx.compose.ui.unit.Dp,
    fallbackCoverContent: @Composable (ContentScale, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation, RectangleShape, clip = false)
            .clip(RectangleShape)
    ) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            fallbackCoverContent(contentScale, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ChapterButton(
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoverSwitcherText(
    text: String,
    marquee: Boolean,
    color: androidx.compose.ui.graphics.Color,
    fontSize: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        maxLines = 1,
        overflow = if (marquee) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (marquee) modifier.basicMarquee(iterations = 1) else modifier
    )
}
