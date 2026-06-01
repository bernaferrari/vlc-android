package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.medialibrary.interfaces.media.Bookmark
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioSeekDelayLabel
import org.videolan.vlc.compose.components.VLCAudioSeekHudButton
import org.videolan.vlc.compose.components.VLCBookmarkRow
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults

data class BookmarkPanelItem(
    val bookmark: Bookmark,
    val id: Long,
    val title: String,
    val timeText: String,
    val timeContentDescription: String
)

/**
 * Compose replacement for bookmarks.xml. The existing ViewStub hosts keep the
 * same ID and bounds while this view owns the former toolbar, RecyclerView row
 * list, empty state, overflow menu, and bottom seek/bookmark controls.
 */
class BookmarksPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var panelBookmarks by mutableStateOf<List<BookmarkPanelItem>>(emptyList())
    private var jumpDelayText by mutableStateOf(Settings.audioJumpDelay.toString())
    private var rewindContentDescription by mutableStateOf("")
    private var forwardContentDescription by mutableStateOf("")
    private var progressTopPx by mutableStateOf(-1f)
    private var addBookmarkFocusToken by mutableStateOf(0)
    private var onCloseClick: () -> Unit = {}
    private var onAddBookmarkClick: () -> Unit = {}
    private var onPreviousBookmarkClick: () -> Unit = {}
    private var onNextBookmarkClick: () -> Unit = {}
    private var onRewindClick: () -> Unit = {}
    private var onForwardClick: () -> Unit = {}
    private var onRewindLongClick: () -> Unit = {}
    private var onForwardLongClick: () -> Unit = {}
    private var onBookmarkClick: (Bookmark) -> Unit = {}
    private var onBookmarkRenameClick: (Bookmark) -> Unit = {}
    private var onBookmarkDeleteClick: (Bookmark) -> Unit = {}

    init {
        isClickable = true
        isFocusable = false
    }

    fun setBookmarks(bookmarks: List<BookmarkPanelItem>) {
        panelBookmarks = bookmarks
    }

    fun setJumpDelay(
        jumpDelay: Int,
        rewindDescription: String,
        forwardDescription: String
    ) {
        jumpDelayText = jumpDelay.toString()
        rewindContentDescription = rewindDescription
        forwardContentDescription = forwardDescription
    }

    fun setProgressTop(y: Float) {
        progressTopPx = y
    }

    @Suppress("DEPRECATION")
    fun announceBookmarkAdded(message: String) {
        announceForAccessibility(message)
    }

    fun sendAddBookmarkAccessibilityEvent() {
        addBookmarkFocusToken += 1
    }

    fun setOnCloseClickListener(listener: () -> Unit) {
        onCloseClick = listener
    }

    fun setOnAddBookmarkClickListener(listener: () -> Unit) {
        onAddBookmarkClick = listener
    }

    fun setOnPreviousBookmarkClickListener(listener: () -> Unit) {
        onPreviousBookmarkClick = listener
    }

    fun setOnNextBookmarkClickListener(listener: () -> Unit) {
        onNextBookmarkClick = listener
    }

    fun setOnRewindClickListener(listener: () -> Unit) {
        onRewindClick = listener
    }

    fun setOnForwardClickListener(listener: () -> Unit) {
        onForwardClick = listener
    }

    fun setOnRewindLongClickListener(listener: () -> Unit) {
        onRewindLongClick = listener
    }

    fun setOnForwardLongClickListener(listener: () -> Unit) {
        onForwardLongClick = listener
    }

    fun setOnBookmarkClickListener(listener: (Bookmark) -> Unit) {
        onBookmarkClick = listener
    }

    fun setOnBookmarkRenameClickListener(listener: (Bookmark) -> Unit) {
        onBookmarkRenameClick = listener
    }

    fun setOnBookmarkDeleteClickListener(listener: (Bookmark) -> Unit) {
        onBookmarkDeleteClick = listener
    }

    @Composable
    override fun WidgetContent() {
        val density = LocalDensity.current
        val addBookmarkFocusRequester = remember { FocusRequester() }

        LaunchedEffect(addBookmarkFocusToken) {
            if (addBookmarkFocusToken > 0) {
                runCatching { addBookmarkFocusRequester.requestFocus() }
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val controlsBottomPadding = controlsBottomPadding(density.run { progressTopPx.toDp() }, maxHeight)
            val listBottomPadding = controlsBottomPadding + 72.dp

            Box(Modifier.fillMaxSize()) {
                BookmarkHeader(
                    addBookmarkFocusRequester = addBookmarkFocusRequester,
                    onCloseClick = onCloseClick,
                    onAddBookmarkClick = onAddBookmarkClick
                )

                if (panelBookmarks.isEmpty()) {
                    BookmarkEmptyState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = controlsBottomPadding)
                    )
                } else {
                    BookmarkList(
                        bookmarks = panelBookmarks,
                        contentPadding = PaddingValues(top = 16.dp, bottom = listBottomPadding),
                        onBookmarkClick = onBookmarkClick,
                        onBookmarkRenameClick = onBookmarkRenameClick,
                        onBookmarkDeleteClick = onBookmarkDeleteClick
                    )
                }

                BookmarkControls(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = controlsBottomPadding),
                    jumpDelayText = jumpDelayText,
                    rewindContentDescription = rewindContentDescription,
                    forwardContentDescription = forwardContentDescription,
                    onPreviousBookmarkClick = onPreviousBookmarkClick,
                    onNextBookmarkClick = onNextBookmarkClick,
                    onRewindClick = onRewindClick,
                    onForwardClick = onForwardClick,
                    onRewindLongClick = onRewindLongClick,
                    onForwardLongClick = onForwardLongClick
                )
            }
        }
    }

    private fun controlsBottomPadding(progressTop: Dp, maxHeight: Dp): Dp {
        if (progressTopPx <= 0f) return 8.dp
        val distanceFromBottom = maxHeight - progressTop
        return (if (distanceFromBottom < 0.dp) 0.dp else distanceFromBottom) + 8.dp
    }
}

@Composable
private fun BookmarkHeader(
    addBookmarkFocusRequester: FocusRequester,
    onCloseClick: () -> Unit,
    onAddBookmarkClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(VLCThemeDefaults.colors.backgroundDefault)
            .padding(horizontal = 8.dp)
    ) {
        IconButton(onClick = onCloseClick) {
            Icon(
                painter = painterResource(R.drawable.ic_close_small),
                contentDescription = stringResource(R.string.close),
                tint = VLCThemeDefaults.colors.fontDefault
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.bookmarks_title),
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
        IconButton(
            onClick = onAddBookmarkClick,
            modifier = Modifier.focusRequester(addBookmarkFocusRequester)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.add_bookmark),
                tint = VLCThemeDefaults.colors.fontDefault
            )
        }
    }
}

@Composable
private fun BookmarkList(
    bookmarks: List<BookmarkPanelItem>,
    contentPadding: PaddingValues,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkRenameClick: (Bookmark) -> Unit,
    onBookmarkDeleteClick: (Bookmark) -> Unit
) {
    var expandedBookmarkId by remember { mutableStateOf<Long?>(null) }
    val moreActions = stringResource(R.string.more_actions)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 68.dp),
        contentPadding = contentPadding
    ) {
        items(
            items = bookmarks,
            key = { item -> "${item.id}:${item.timeText}" }
        ) { item ->
            VLCBookmarkRow(
                title = item.title,
                timeText = item.timeText,
                timeContentDescription = item.timeContentDescription,
                moreContentDescription = moreActions,
                marqueeTitle = Settings.listTitleEllipsize == 4,
                onClick = { onBookmarkClick(item.bookmark) },
                onMoreClick = { expandedBookmarkId = item.id }
            ) {
                Box {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = null,
                        tint = VLCThemeDefaults.colors.fontDefault
                    )
                    DropdownMenu(
                        expanded = expandedBookmarkId == item.id,
                        onDismissRequest = { expandedBookmarkId = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            onClick = {
                                expandedBookmarkId = null
                                onBookmarkRenameClick(item.bookmark)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                expandedBookmarkId = null
                                onBookmarkDeleteClick(item.bookmark)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkEmptyState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_empty),
            contentDescription = null,
            tint = VLCThemeDefaults.colors.fontDefault,
            modifier = Modifier.size(62.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_bookmark),
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun BookmarkControls(
    jumpDelayText: String,
    rewindContentDescription: String,
    forwardContentDescription: String,
    onPreviousBookmarkClick: () -> Unit,
    onNextBookmarkClick: () -> Unit,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onRewindLongClick: () -> Unit,
    onForwardLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        VLCAudioSeekHudButton(
            contentDescription = stringResource(R.string.previous_bookmark),
            onClick = onPreviousBookmarkClick
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_player_bookmark_previous),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor
            )
        }
        Box(contentAlignment = Alignment.Center) {
            VLCAudioSeekHudButton(
                contentDescription = rewindContentDescription,
                onClick = onRewindClick,
                onLongClick = onRewindLongClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_player_rewind_10),
                    contentDescription = null,
                    tint = VLCThemeDefaults.colors.playerIconColor
                )
            }
            VLCAudioSeekDelayLabel(text = jumpDelayText)
        }
        Box(contentAlignment = Alignment.Center) {
            VLCAudioSeekHudButton(
                contentDescription = forwardContentDescription,
                onClick = onForwardClick,
                onLongClick = onForwardLongClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_player_forward_10),
                    contentDescription = null,
                    tint = VLCThemeDefaults.colors.playerIconColor
                )
            }
            VLCAudioSeekDelayLabel(text = jumpDelayText)
        }
        VLCAudioSeekHudButton(
            contentDescription = stringResource(R.string.next_bookmark),
            onClick = onNextBookmarkClick
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_player_bookmark_next),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor
            )
        }
    }
}
