package org.videolan.television.ui.details

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import org.videolan.television.ui.TvMediaArtworkImage
import org.videolan.television.ui.TvUtil
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.television.ui.dialogs.ConfirmationTvActivity
import org.videolan.television.ui.dialogs.ConfirmationTvActivity.Companion.CONFIRMATION_DIALOG_TEXT
import org.videolan.television.ui.dialogs.ConfirmationTvActivity.Companion.CONFIRMATION_DIALOG_TITLE
import org.videolan.television.ui.updateBackground
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.VLCTvAction
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.compose.tv.VLCTvTrack
import org.videolan.vlc.compose.tv.VLCTvTrackAction
import org.videolan.vlc.compose.tv.VLCTvTrackListScreen
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
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
                VLCTvTheme {
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
            intent.putExtra(ConfirmationTvActivity.CONFIRMATION_DIALOG_DESTRUCTIVE, true)
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

private const val ACTION_PLAY = 1
private const val ACTION_INSERT_NEXT = 2
private const val ACTION_APPEND = 3
private const val ACTION_ADD_TO_PLAYLIST = 4
private const val ACTION_DELETE = 5
private const val ACTION_MOVE_UP = 6
private const val ACTION_MOVE_DOWN = 7
private const val ACTION_REMOVE = 8

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
    val isPlaylist = item.itemType == MediaLibraryItem.TYPE_PLAYLIST
    val actions = buildList {
        add(VLCTvAction(ACTION_PLAY, stringResource(R.string.play), MaterialSymbols.Filled.PlayArrow, primary = true))
        add(VLCTvAction(ACTION_INSERT_NEXT, stringResource(R.string.insert_next), MaterialSymbols.Filled.SkipNext))
        add(VLCTvAction(ACTION_APPEND, stringResource(R.string.append), MaterialSymbols.Filled.QueueMusic))
        add(VLCTvAction(ACTION_ADD_TO_PLAYLIST, stringResource(R.string.add_to_playlist), MaterialSymbols.Filled.Add))
        if (isPlaylist) add(VLCTvAction(ACTION_DELETE, stringResource(R.string.delete), MaterialSymbols.Filled.Delete))
    }
    val trackActions = buildList {
        if (isPlaylist) {
            add(VLCTvTrackAction(ACTION_MOVE_UP, stringResource(R.string.move_up), MaterialSymbols.Filled.ArrowUpward))
            add(VLCTvTrackAction(ACTION_MOVE_DOWN, stringResource(R.string.move_down), MaterialSymbols.Filled.ArrowDownward))
        }
        add(VLCTvTrackAction(ACTION_INSERT_NEXT, stringResource(R.string.insert_next), MaterialSymbols.Filled.SkipNext))
        add(VLCTvTrackAction(ACTION_APPEND, stringResource(R.string.append), MaterialSymbols.Filled.QueueMusic))
        add(VLCTvTrackAction(ACTION_ADD_TO_PLAYLIST, stringResource(R.string.add_to_playlist), MaterialSymbols.Filled.Add))
        if (isPlaylist) add(VLCTvTrackAction(ACTION_REMOVE, stringResource(R.string.remove_from_playlist), MaterialSymbols.Filled.Close))
    }
    val rows = remember(tracks) {
        tracks.mapIndexed { index, track ->
            val subtitle = track.getDiscNumberString()?.let { disc ->
                listOf(track.artistName, disc).filter { !it.isNullOrEmpty() }.joinToString(" · ")
            } ?: track.artistName
            VLCTvTrack(key = "${track.id}-${track.uri}-$index", title = track.title, subtitle = subtitle, duration = Tools.millisToString(track.length))
        }
    }
    val totalTime = Tools.millisToString(tracks.sumOf { it.length })
    VLCTvTrackListScreen(
        title = item.title.orEmpty(),
        eyebrow = stringResource(if (isPlaylist) R.string.playlist else R.string.album),
        subtitle = item.description,
        metadata = "${pluralStringResource(R.plurals.track_quantity, tracks.size, tracks.size)} · $totalTime",
        tracks = rows,
        actions = actions,
        onAction = { action ->
            when (action.id) {
                ACTION_PLAY -> onPlayAll()
                ACTION_INSERT_NEXT -> onInsertNextAll()
                ACTION_APPEND -> onAppendAll()
                ACTION_ADD_TO_PLAYLIST -> onAddAllToPlaylist()
                ACTION_DELETE -> onDeletePlaylist()
            }
        },
        onTrackClick = onPlayTrack,
        trackActions = trackActions,
        isTrackActionEnabled = { id, index ->
            when (id) {
                ACTION_MOVE_UP -> index > 0
                ACTION_MOVE_DOWN -> index < tracks.lastIndex
                else -> true
            }
        },
        onTrackAction = { id, index ->
            when (id) {
                ACTION_MOVE_UP -> onMoveTrackUp(index)
                ACTION_MOVE_DOWN -> onMoveTrackDown(index)
                ACTION_INSERT_NEXT -> onInsertTrackNext(index)
                ACTION_APPEND -> onAppendTrack(index)
                ACTION_ADD_TO_PLAYLIST -> onAddTrackToPlaylist(index)
                ACTION_REMOVE -> onRemoveTrack(index)
            }
        },
        onTrackFocused = { index -> tracks.getOrNull(index)?.let(onTrackFocused) },
        heroArtwork = { TvMediaArtworkImage(item, contentDescription = item.title) },
        trackArtwork = { index -> tracks.getOrNull(index)?.let { TvMediaArtworkImage(it) } },
    )
}

internal fun MediaListActivity.getViewModel(playlist: MediaLibraryItem) = ViewModelProvider(this, PlaylistViewModel.Factory(this, playlist))[PlaylistViewModel::class.java]
