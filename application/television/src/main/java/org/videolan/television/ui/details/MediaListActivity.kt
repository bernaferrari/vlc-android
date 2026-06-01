package org.videolan.television.ui.details

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.leanback.app.BackgroundManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.ITEM
import org.videolan.resources.util.parcelable
import org.videolan.television.R
import org.videolan.television.ui.TvUtil
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.television.ui.dialogs.ConfirmationTvActivity
import org.videolan.television.ui.dialogs.ConfirmationTvActivity.Companion.CONFIRMATION_DIALOG_TEXT
import org.videolan.television.ui.dialogs.ConfirmationTvActivity.Companion.CONFIRMATION_DIALOG_TITLE
import org.videolan.television.ui.updateBackground
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.helpers.getTvIconRes
import org.videolan.vlc.gui.helpers.loadImage
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.ModelsHelper.getDiscNumberString
import org.videolan.vlc.viewmodels.mobile.PlaylistViewModel

class MediaListActivity : BaseTvActivity() {

    override fun refresh() {}

    private lateinit var item: MediaLibraryItem
    private lateinit var backgroundManager: BackgroundManager
    private lateinit var viewModel: PlaylistViewModel
    private var lateSelectedItem: MediaLibraryItem? = null
    private var tracks by mutableStateOf<List<MediaWrapper>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        item = (savedInstanceState?.parcelable(ITEM) ?: intent.parcelable<Parcelable>(ITEM)) as MediaLibraryItem
        tracks = item.tracks.toList()

        val rootView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme(darkTheme = true) {
                    TvMediaListScreen(
                        item = item,
                        tracks = tracks,
                        onPlayAll = ::playAll,
                        onAppendAll = { MediaUtils.appendMedia(this@MediaListActivity, item.tracks) },
                        onInsertNextAll = { MediaUtils.insertNext(this@MediaListActivity, item.tracks) },
                        onAddAllToPlaylist = { addToPlaylist(item.tracks, SavePlaylistDialog.KEY_NEW_TRACKS) },
                        onDeletePlaylist = ::requestPlaylistDeletion,
                        onPlayTrack = ::playTrack,
                        onInsertTrackNext = ::insertTrackNext,
                        onAppendTrack = ::appendTrack,
                        onAddTrackToPlaylist = ::addTrackToPlaylist,
                        onMoveTrackUp = ::moveTrackUp,
                        onMoveTrackDown = ::moveTrackDown,
                        onRemoveTrack = ::removeTrack,
                        onTrackFocused = ::onTrackFocused
                    )
                }
            }
        }
        setContentView(rootView)

        backgroundManager = BackgroundManager.getInstance(this)
        if (!backgroundManager.isAttached) {
            backgroundManager.attachToView(rootView)
        }

        if (item.itemType == MediaLibraryItem.TYPE_PLAYLIST) {
            viewModel = getViewModel(item)
            viewModel.tracksProvider.pagedList.observe(this) { updatedTracks ->
                if (updatedTracks != null) tracks = updatedTracks
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_DELETE_PLAYLIST && resultCode == ConfirmationTvActivity.ACTION_ID_POSITIVE && ::viewModel.isInitialized) {
            (viewModel.playlist as Playlist).delete()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.updateBackground(this, backgroundManager, item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ITEM, item)
    }

    private fun playAll() {
        if (item is Playlist) TvUtil.playPlaylist(this, item as Playlist)
        else TvUtil.playMedia(this, item.tracks.toMutableList())
    }

    private fun requestPlaylistDeletion() {
        if (!showPinIfNeeded()) {
            val intent = Intent(this, ConfirmationTvActivity::class.java)
            intent.putExtra(CONFIRMATION_DIALOG_TITLE, getString(R.string.validation_delete_playlist))
            intent.putExtra(CONFIRMATION_DIALOG_TEXT, getString(R.string.validation_delete_playlist_text))
            startActivityForResult(intent, REQUEST_DELETE_PLAYLIST)
        }
    }

    private fun playTrack(position: Int) {
        if (item is Playlist) TvUtil.playPlaylist(this, item as Playlist, position)
        else TvUtil.playMedia(this, item.tracks.toList(), position)
    }

    private fun insertTrackNext(position: Int) {
        tracks.getOrNull(position)?.let { MediaUtils.insertNext(this, it) }
    }

    private fun appendTrack(position: Int) {
        tracks.getOrNull(position)?.let { MediaUtils.appendMedia(this, it) }
    }

    private fun addTrackToPlaylist(position: Int) {
        tracks.getOrNull(position)?.let { addToPlaylist(arrayOf(it), SavePlaylistDialog.KEY_NEW_TRACKS) }
    }

    private fun moveTrackUp(position: Int) {
        if (!showPinIfNeeded() && ::viewModel.isInitialized && position > 0) {
            (viewModel.playlist as Playlist).move(position, position - 1)
        }
    }

    private fun moveTrackDown(position: Int) {
        if (!showPinIfNeeded() && ::viewModel.isInitialized && position < tracks.lastIndex) {
            (viewModel.playlist as Playlist).move(position, position + 1)
        }
    }

    private fun removeTrack(position: Int) {
        if (!showPinIfNeeded() && ::viewModel.isInitialized) {
            (viewModel.playlist as Playlist).remove(position)
        }
    }

    private fun onTrackFocused(track: MediaLibraryItem) {
        if (track != lateSelectedItem) lifecycleScope.updateBackground(this, backgroundManager, track)
        lateSelectedItem = track
    }

    companion object {
        private const val REQUEST_DELETE_PLAYLIST = 1
    }
}

@Composable
private fun TvMediaListScreen(
    item: MediaLibraryItem,
    tracks: List<MediaWrapper>,
    onPlayAll: () -> Unit,
    onAppendAll: () -> Unit,
    onInsertNextAll: () -> Unit,
    onAddAllToPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onInsertTrackNext: (Int) -> Unit,
    onAppendTrack: (Int) -> Unit,
    onAddTrackToPlaylist: (Int) -> Unit,
    onMoveTrackUp: (Int) -> Unit,
    onMoveTrackDown: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onTrackFocused: (MediaWrapper) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VLCThemeDefaults.colors.backgroundDefault)
    ) {
        TvMediaListHeader(
            item = item,
            totalTime = Tools.millisToString(tracks.sumOf { it.length }),
            showDelete = item.itemType == MediaLibraryItem.TYPE_PLAYLIST,
            onPlayAll = onPlayAll,
            onAppendAll = onAppendAll,
            onInsertNextAll = onInsertNextAll,
            onAddAllToPlaylist = onAddAllToPlaylist,
            onDeletePlaylist = onDeletePlaylist
        )
        LazyColumn(
            contentPadding = PaddingValues(top = 40.dp, bottom = 150.dp),
            modifier = Modifier
                .width(800.dp)
                .weight(1f)
                .align(Alignment.CenterHorizontally)
        ) {
            itemsIndexed(
                items = tracks,
                key = { index, track -> "${track.id}-${track.uri}-$index" }
            ) { position, track ->
                TvMediaListRow(
                    track = track,
                    position = position,
                    itemType = item.itemType,
                    lastPosition = tracks.lastIndex,
                    onPlay = { onPlayTrack(position) },
                    onInsertNext = { onInsertTrackNext(position) },
                    onAppend = { onAppendTrack(position) },
                    onAddToPlaylist = { onAddTrackToPlaylist(position) },
                    onMoveUp = { onMoveTrackUp(position) },
                    onMoveDown = { onMoveTrackDown(position) },
                    onRemove = { onRemoveTrack(position) },
                    onFocused = { onTrackFocused(track) }
                )
                HorizontalDivider(color = colorResource(R.color.tv_card_content_darker))
            }
        }
    }
}

@Composable
private fun TvMediaListHeader(
    item: MediaLibraryItem,
    totalTime: String,
    showDelete: Boolean,
    onPlayAll: () -> Unit,
    onAppendAll: () -> Unit,
    onInsertNextAll: () -> Unit,
    onAddAllToPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { playFocusRequester.requestFocus() }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(colorResource(R.color.tv_card_content_darker))
            .padding(
                start = dimensionResource(R.dimen.tv_overscan_horizontal),
                end = dimensionResource(R.dimen.tv_overscan_horizontal),
                bottom = 8.dp
            )
    ) {
        TvMediaImage(
            item = item,
            modifier = Modifier.size(90.dp)
        )
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
                .height(90.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.description?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    color = VLCThemeDefaults.colors.listSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = totalTime,
                color = VLCThemeDefaults.colors.listSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            TvCircleIconButton(
                icon = R.drawable.ic_tv_list_play,
                contentDescription = stringResource(R.string.play),
                modifier = Modifier.focusRequester(playFocusRequester),
                onClick = onPlayAll
            )
            if (showDelete) {
                TvCircleIconButton(
                    icon = R.drawable.ic_tv_list_delete,
                    contentDescription = stringResource(R.string.delete),
                    onClick = onDeletePlaylist
                )
            }
            TvCircleIconButton(
                icon = R.drawable.ic_tv_list_playnext,
                contentDescription = stringResource(R.string.insert_next),
                onClick = onInsertNextAll
            )
            TvCircleIconButton(
                icon = R.drawable.ic_tv_list_append,
                contentDescription = stringResource(R.string.append),
                onClick = onAppendAll
            )
            TvCircleIconButton(
                icon = R.drawable.ic_tv_list_addtoplaylist,
                contentDescription = stringResource(R.string.add_to_playlist),
                onClick = onAddAllToPlaylist
            )
        }
    }
}

@Composable
private fun TvMediaListRow(
    track: MediaWrapper,
    position: Int,
    itemType: Int,
    lastPosition: Int,
    onPlay: () -> Unit,
    onInsertNext: () -> Unit,
    onAppend: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onFocused: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(false) }
    val playlistActions = itemType != MediaLibraryItem.TYPE_ALBUM
    val subtitle = track.getDiscNumberString()?.let { disc ->
        listOf(track.artistName, disc).filter { it.isNotEmpty() }.joinToString(" · ")
    } ?: track.artistName

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colorResource(R.color.tv_card_content_dark))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onFocusChanged {
                    controlsVisible = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .focusable()
                .clickable(onClick = onPlay)
        ) {
            Box(contentAlignment = Alignment.Center) {
                TvMediaImage(
                    item = track,
                    modifier = Modifier.size(48.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_tv_list_play_big),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (controlsVisible) 0.46f else 0f))
                        .padding(8.dp)
                        .alpha(if (controlsVisible) 1f else 0f)
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(start = 8.dp, end = 16.dp)
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = track.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = VLCThemeDefaults.colors.listSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = Tools.millisToString(track.length),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier
                    .width(88.dp)
                    .padding(end = 8.dp)
            )
        }
        if (playlistActions) {
            TvRowActionButton(
                icon = R.drawable.ic_tv_list_moveup,
                contentDescription = stringResource(R.string.move_up),
                visible = controlsVisible,
                enabled = position > 0,
                onFocusChanged = { focused ->
                    controlsVisible = focused
                    if (focused) onFocused()
                },
                onClick = onMoveUp
            )
            TvRowActionButton(
                icon = R.drawable.ic_tv_list_movedown,
                contentDescription = stringResource(R.string.move_down),
                visible = controlsVisible,
                enabled = position < lastPosition,
                onFocusChanged = { focused ->
                    controlsVisible = focused
                    if (focused) onFocused()
                },
                onClick = onMoveDown
            )
        }
        TvRowActionButton(
            icon = R.drawable.ic_tv_list_playnext,
            contentDescription = stringResource(R.string.insert_next),
            visible = controlsVisible,
            onFocusChanged = { focused ->
                controlsVisible = focused
                if (focused) onFocused()
            },
            onClick = onInsertNext
        )
        TvRowActionButton(
            icon = R.drawable.ic_tv_list_append,
            contentDescription = stringResource(R.string.append),
            visible = controlsVisible,
            onFocusChanged = { focused ->
                controlsVisible = focused
                if (focused) onFocused()
            },
            onClick = onAppend
        )
        TvRowActionButton(
            icon = R.drawable.ic_tv_list_addtoplaylist,
            contentDescription = stringResource(R.string.add_to_playlist),
            visible = controlsVisible,
            onFocusChanged = { focused ->
                controlsVisible = focused
                if (focused) onFocused()
            },
            onClick = onAddToPlaylist
        )
        if (playlistActions) {
            TvRowActionButton(
                icon = R.drawable.ic_tv_list_removefromplaylist,
                contentDescription = stringResource(R.string.remove_from_playlist),
                visible = controlsVisible,
                onFocusChanged = { focused ->
                    controlsVisible = focused
                    if (focused) onFocused()
                },
                onClick = onRemove
            )
        }
    }
}

@Composable
private fun TvCircleIconButton(
    icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (focused) VLCThemeDefaults.colors.primary else Color.White)
            .border(2.dp, if (focused) Color.White else Color.Transparent, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TvRowActionButton(
    icon: Int,
    contentDescription: String,
    visible: Boolean,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    TvCircleIconButton(
        icon = icon,
        contentDescription = contentDescription,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .padding(end = 8.dp)
            .alpha(if (visible && enabled) 1f else 0f)
            .onFocusChanged {
                onFocusChanged(it.isFocused)
            }
    )
}

@Composable
private fun TvMediaImage(item: MediaLibraryItem, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setImageResource(getTvIconRes(item))
            imageView.contentDescription = item.title
            loadImage(imageView, item, tv = true, card = true)
        }
    )
}

internal fun MediaListActivity.getViewModel(playlist: MediaLibraryItem) = ViewModelProvider(this, PlaylistViewModel.Factory(this, playlist))[PlaylistViewModel::class.java]
