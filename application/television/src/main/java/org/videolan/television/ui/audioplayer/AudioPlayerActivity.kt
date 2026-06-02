/*****************************************************************************
 * AudioPlayerActivity.java
 *
 * Copyright © 2014-2015 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan.television.ui.audioplayer

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.medialibrary.interfaces.media.Bookmark
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.AndroidDevices
import org.videolan.resources.util.parcelableList
import org.videolan.television.R
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.Settings
import org.videolan.tools.dp
import org.videolan.tools.formatRateString
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.dialogs.showEqualizerComposeDialog
import org.videolan.vlc.gui.dialogs.showPlaybackSpeedComposeDialog
import org.videolan.vlc.gui.dialogs.showSleepTimerComposeDialog
import org.videolan.vlc.gui.helpers.AudioUtil
import org.videolan.vlc.gui.helpers.BookmarkListDelegate
import org.videolan.vlc.gui.helpers.BookmarkMarkerHost
import org.videolan.vlc.gui.helpers.KeycodeListener
import org.videolan.vlc.gui.helpers.MediaComparators
import org.videolan.vlc.gui.helpers.PlayerKeyListenerDelegate
import org.videolan.vlc.gui.helpers.PlayerOption
import org.videolan.vlc.gui.helpers.PlayerOptionsDelegate
import org.videolan.vlc.gui.helpers.PlayerOptionsDelegateCallback
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.view.BookmarkPanelHost
import org.videolan.vlc.gui.view.BookmarkPanelItem
import org.videolan.vlc.gui.view.PlayerOptionsPanelHost
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.media.PlaylistManager.Companion.hasMedia
import org.videolan.vlc.util.getScreenWidth
import org.videolan.vlc.viewmodels.BookmarkModel
import org.videolan.vlc.viewmodels.PlayerState
import org.videolan.vlc.viewmodels.PlaylistModel
import kotlin.math.absoluteValue
import org.videolan.vlc.R as VlcR

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class AudioPlayerActivity : BaseTvActivity(),KeycodeListener, PlaybackService.Callback, PlayerOptionsDelegateCallback  {

    private lateinit var views: TvAudioPlayerViews
    private var lastMove: Long = 0
    private var shuffling = false
    private var currentCoverArt: String? = null
    private lateinit var model: PlaylistModel
    private var settings: SharedPreferences? = null
    private var optionsDelegate: PlayerOptionsDelegate? = null
    lateinit var bookmarkModel: BookmarkModel
    private lateinit var bookmarkListDelegate: BookmarkListDelegate
    private var timelineDragging = false
    private val playerKeyListenerDelegate: PlayerKeyListenerDelegate by lazy(LazyThreadSafetyMode.NONE) { PlayerKeyListenerDelegate(this@AudioPlayerActivity) }
    var playbackStarted = false
    private var service: PlaybackService? = null
    private var playlistItems by mutableStateOf<List<MediaWrapper>>(emptyList())
    private var selectedPlaylistItem by mutableIntStateOf(-1)
    private var playlistPlaying by mutableStateOf(false)
    private var playlistScrollTarget by mutableIntStateOf(-1)
    private var playlistFocusEnabled by mutableStateOf(true)
    private var quickActionsState by mutableStateOf(TvAudioQuickActionsState())
    private var quickActionsFocusEnabled by mutableStateOf(true)
    private var transportControlsState by mutableStateOf(TvAudioTransportControlsState())
    private var trackInfoState by mutableStateOf(TvAudioTrackInfoState())
    private var progressLabelsState by mutableStateOf(TvAudioProgressLabelsState())
    private var artworkState by mutableStateOf(TvAudioArtworkState())
    private var timelineState by mutableStateOf(TvAudioTimelineState())
    private var optionsPanelState by mutableStateOf(TvAudioPlayerOptionsPanelState())
    private var bookmarksPanelState by mutableStateOf(TvAudioBookmarksPanelState())
    private var bookmarkMarkersState by mutableStateOf(TvAudioBookmarkMarkersState())
    private val tvOptionsPanelHost = object : PlayerOptionsPanelHost {
        var onDismissClick: () -> Unit = {}
        var onOptionClick: (PlayerOption) -> Unit = {}

        override val visible: Boolean
            get() = optionsPanelState.visible

        override fun show() {
            optionsPanelState = optionsPanelState.copy(visible = true)
            if (::views.isInitialized) views.playerOptionsPanel.visibility = View.VISIBLE
        }

        override fun hide() {
            optionsPanelState = optionsPanelState.copy(visible = false)
            if (::views.isInitialized) views.playerOptionsPanel.visibility = View.GONE
        }

        override fun setOptions(options: List<PlayerOption>) {
            optionsPanelState = optionsPanelState.copy(options = options)
        }

        override fun setOnDismissClickListener(listener: () -> Unit) {
            onDismissClick = listener
        }

        override fun setOnOptionClickListener(listener: (PlayerOption) -> Unit) {
            onOptionClick = listener
        }

        override fun requestInitialFocus() {
            optionsPanelState = optionsPanelState.copy(focusRequestToken = optionsPanelState.focusRequestToken + 1)
        }

        override fun setOptionIcon(optionId: Long, icon: Int, contentDescription: String?) {
            optionsPanelState = optionsPanelState.copy(
                options = optionsPanelState.options.map { option ->
                    if (option.id == optionId) option.copy(icon = icon, contentDescription = contentDescription ?: option.contentDescription)
                    else option
                }
            )
        }
    }
    private val tvBookmarksPanelHost = object : BookmarkPanelHost {
        var onCloseClick: () -> Unit = {}
        var onAddBookmarkClick: () -> Unit = {}
        var onPreviousBookmarkClick: () -> Unit = {}
        var onNextBookmarkClick: () -> Unit = {}
        var onRewindClick: () -> Unit = {}
        var onForwardClick: () -> Unit = {}
        var onRewindLongClick: () -> Unit = {}
        var onForwardLongClick: () -> Unit = {}
        var onBookmarkClick: (Bookmark) -> Unit = {}
        var onBookmarkRenameClick: (Bookmark) -> Unit = {}
        var onBookmarkDeleteClick: (Bookmark) -> Unit = {}

        override val visible: Boolean
            get() = bookmarksPanelState.visible

        override fun show() {
            bookmarksPanelState = bookmarksPanelState.copy(visible = true)
            if (::views.isInitialized) views.bookmarksPanel.visibility = View.VISIBLE
        }

        override fun hide() {
            bookmarksPanelState = bookmarksPanelState.copy(visible = false)
            if (::views.isInitialized) views.bookmarksPanel.visibility = View.GONE
        }

        override fun setBookmarks(bookmarks: List<BookmarkPanelItem>) {
            bookmarksPanelState = bookmarksPanelState.copy(bookmarks = bookmarks)
        }

        override fun setJumpDelay(jumpDelay: Int, rewindDescription: String, forwardDescription: String) {
            bookmarksPanelState = bookmarksPanelState.copy(
                jumpDelayText = jumpDelay.toString(),
                rewindContentDescription = rewindDescription,
                forwardContentDescription = forwardDescription
            )
        }

        override fun setProgressTop(y: Float) {
            bookmarksPanelState = bookmarksPanelState.copy(progressTopPx = y)
        }

        @Suppress("DEPRECATION")
        override fun announceBookmarkAdded(message: String) {
            if (::views.isInitialized) views.bookmarksPanel.announceForAccessibility(message)
        }

        override fun sendAddBookmarkAccessibilityEvent() {
            requestPanelFocus()
        }

        override fun requestPanelFocus() {
            bookmarksPanelState = bookmarksPanelState.copy(addBookmarkFocusToken = bookmarksPanelState.addBookmarkFocusToken + 1)
        }

        override fun setOnCloseClickListener(listener: () -> Unit) {
            onCloseClick = listener
        }

        override fun setOnAddBookmarkClickListener(listener: () -> Unit) {
            onAddBookmarkClick = listener
        }

        override fun setOnPreviousBookmarkClickListener(listener: () -> Unit) {
            onPreviousBookmarkClick = listener
        }

        override fun setOnNextBookmarkClickListener(listener: () -> Unit) {
            onNextBookmarkClick = listener
        }

        override fun setOnRewindClickListener(listener: () -> Unit) {
            onRewindClick = listener
        }

        override fun setOnForwardClickListener(listener: () -> Unit) {
            onForwardClick = listener
        }

        override fun setOnRewindLongClickListener(listener: () -> Unit) {
            onRewindLongClick = listener
        }

        override fun setOnForwardLongClickListener(listener: () -> Unit) {
            onForwardLongClick = listener
        }

        override fun setOnBookmarkClickListener(listener: (Bookmark) -> Unit) {
            onBookmarkClick = listener
        }

        override fun setOnBookmarkRenameClickListener(listener: (Bookmark) -> Unit) {
            onBookmarkRenameClick = listener
        }

        override fun setOnBookmarkDeleteClickListener(listener: (Bookmark) -> Unit) {
            onBookmarkDeleteClick = listener
        }
    }
    private val tvBookmarkMarkerHost = object : BookmarkMarkerHost {
        override fun show() {
            bookmarkMarkersState = bookmarkMarkersState.copy(visible = true)
        }

        override fun hide() {
            bookmarkMarkersState = bookmarkMarkersState.copy(visible = false)
        }

        override fun setMarkerFractions(fractions: List<Float>) {
            bookmarkMarkersState = bookmarkMarkersState.copy(markerFractions = fractions)
        }

        override fun clearMarkers() {
            bookmarkMarkersState = bookmarkMarkersState.copy(markerFractions = emptyList())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings.getInstance(this)
        model = ViewModelProvider(this)[PlaylistModel::class.java]

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCTheme(darkTheme = true) {
                        TvAudioPlayerScreen { refs ->
                            if (!::views.isInitialized) {
                                views = refs
                                initializeAudioPlayerScreen()
                            }
                        }
                    }
                }
            }
        )
    }

    private fun initializeAudioPlayerScreen() {
        views.background.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.background.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioBackground(
                    state = artworkState,
                    overlayColor = Color(views.background.context.resolveThemeColor(R.attr.audio_player_background_tint)),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        views.albumCover.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.albumCover.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioAlbumCover(state = artworkState, modifier = Modifier.fillMaxSize())
            }
        }
        views.playlist.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.playlist.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioPlaylist(
                    items = playlistItems,
                    selectedItem = selectedPlaylistItem,
                    playing = playlistPlaying,
                    focusEnabled = playlistFocusEnabled,
                    scrollTarget = playlistScrollTarget,
                    onItemClick = ::playPlaylistItem,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        views.quickActions.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.quickActions.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioQuickActions(
                    state = quickActionsState,
                    focusEnabled = quickActionsFocusEnabled,
                    onSpeedClick = ::showPlaybackSpeedComposeDialog,
                    onSpeedLongClick = {
                        model.service?.setRate(1F, true)
                        showChips()
                    },
                    onSleepClick = ::showSleepTimerComposeDialog,
                    onSleepLongClick = {
                        model.service?.setSleepTimer(null)
                        showChips()
                    }
                )
            }
        }
        syncQuickActionsFocusability()
        views.transportControls.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.transportControls.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioTransportControls(
                    state = transportControlsState,
                    onShuffleClick = { setShuffleMode(!shuffling) },
                    onPreviousClick = ::previous,
                    onPlayPauseClick = ::togglePlayPause,
                    onNextClick = ::next,
                    onRepeatClick = ::switchRepeatMode,
                    onMoreClick = ::showAdvancedOptionsPanel
                )
            }
        }
        updateTransportControlsState(playing = false)
        views.trackInfo.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.trackInfo.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioTrackInfo(state = trackInfoState, modifier = Modifier.fillMaxSize())
            }
        }
        views.progressLabels.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.progressLabels.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioProgressLabels(state = progressLabelsState, modifier = Modifier.fillMaxSize())
            }
        }
        views.mediaProgress.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.mediaProgress.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioTimeline(
                    state = timelineState,
                    onUserDragStarted = { timelineDragging = true },
                    onUserProgressChange = ::onTimelineUserProgressChanged,
                    onUserDragStopped = { timelineDragging = false }
                )
            }
        }
        views.bookmarkMarkerContainer.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.bookmarkMarkerContainer.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioBookmarkMarkers(
                    state = bookmarkMarkersState
                )
            }
        }
        views.bookmarksPanel.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.bookmarksPanel.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioBookmarksPanel(
                    state = bookmarksPanelState,
                    onCloseClick = { tvBookmarksPanelHost.onCloseClick() },
                    onAddBookmarkClick = { tvBookmarksPanelHost.onAddBookmarkClick() },
                    onPreviousBookmarkClick = { tvBookmarksPanelHost.onPreviousBookmarkClick() },
                    onNextBookmarkClick = { tvBookmarksPanelHost.onNextBookmarkClick() },
                    onRewindClick = { tvBookmarksPanelHost.onRewindClick() },
                    onForwardClick = { tvBookmarksPanelHost.onForwardClick() },
                    onRewindLongClick = { tvBookmarksPanelHost.onRewindLongClick() },
                    onForwardLongClick = { tvBookmarksPanelHost.onForwardLongClick() },
                    onBookmarkClick = { tvBookmarksPanelHost.onBookmarkClick(it) },
                    onBookmarkRenameClick = { tvBookmarksPanelHost.onBookmarkRenameClick(it) },
                    onBookmarkDeleteClick = { tvBookmarksPanelHost.onBookmarkDeleteClick(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        views.playerOptionsPanel.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        views.playerOptionsPanel.setContent {
            VLCTheme(darkTheme = true) {
                TvAudioPlayerOptionsPanel(
                    state = optionsPanelState,
                    onDismissClick = { tvOptionsPanelHost.onDismissClick() },
                    onOptionClick = { tvOptionsPanelHost.onOptionClick(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        model.progress.observe(this) { progress ->
            progressLabelsState = TvAudioProgressLabelsState(progress.timeText, progress.lengthText)
            val max = progress.length.toInt()
            val currentProgress = if (timelineDragging) timelineState.progress else progress.time.toInt()
            updateTimelineState(currentProgress, max)
        }
        model.dataset.observe(this) { mediaWrappers ->
            if (mediaWrappers != null) {
                playlistItems = mediaWrappers
                selectedPlaylistItem = -1
                updatePlaylistSelection()
            }
            updateRepeatMode()
        }
        model.speed.observe(this) { showChips() }
        PlaybackService.playerSleepTime.observe(this) {
            showChips()
        }
        model.playerState.observe(this) { playerState -> update(playerState) }
        val position = intent.getIntExtra(MEDIA_POSITION, 0)
        if (intent.hasExtra(MEDIA_PLAYLIST))
            intent.getLongExtra(MEDIA_PLAYLIST, -1L).let { MediaUtils.openPlaylist(this, it, position) }
        else
            intent.parcelableList<MediaWrapper>(MEDIA_LIST)?.let { MediaUtils.openList(this, it, position) }
        bookmarkModel = BookmarkModel.get(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (optionsDelegate?.isShowing() == true) {
                    optionsDelegate?.hide()
                    return
                }
                if (::bookmarkListDelegate.isInitialized && bookmarkListDelegate.visible) {
                    bookmarkListDelegate.hide()
                    return
                }
                finish()
            }
        })
        PlaybackService.serviceFlow.onEach { onServiceChanged(it) }.launchIn(MainScope())
        PlaylistManager.showAudioPlayer.observe(this) { showPlayer ->
            if (!showPlayer && playbackStarted) finish()
        }
    }

    private fun onServiceChanged(it: PlaybackService?) {
        it?.addCallback(this)
        service = it
    }

    override fun onDestroy() {
        super.onDestroy()
        optionsDelegate = null
        service?.removeCallback(this)
    }

    private fun onTimelineUserProgressChanged(progress: Int) {
        updateTimelineState(progress, timelineState.max)
        model.setTime(progress.toLong())
    }

    private fun updateTimelineState(progress: Int, max: Int) {
        val safeMax = max.coerceAtLeast(0)
        val safeProgress = progress.coerceIn(0, safeMax.coerceAtLeast(1))
        timelineState = TvAudioTimelineState(
            progress = safeProgress,
            max = safeMax,
            contentDescription = getString(
                VlcR.string.talkback_out_of,
                TalkbackUtil.millisToString(this, safeProgress.toLong()),
                TalkbackUtil.millisToString(this, safeMax.toLong())
            )
        )
    }

    private fun showChips() {
        val speed = model.speed.value
        quickActionsState = TvAudioQuickActionsState(
            speedText = speed?.takeIf { it != 1.0F }?.formatRateString(),
            sleepText = PlaybackService.playerSleepTime.value?.let { DateFormat.getTimeFormat(this).format(it.time) },
            speedUsesGlobalRate = settings?.getBoolean(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL, false) == true
        )
        syncQuickActionsFocusability()
    }

    private fun syncQuickActionsFocusability() {
        if (::views.isInitialized) views.quickActions.isFocusable = quickActionsFocusEnabled && quickActionsState.hasVisibleActions
    }

    override fun refresh() {}

    fun update(state: PlayerState?) {
        if (state == null) return

        playlistPlaying = state.playing
        updateTransportControlsState(state.playing)
        updatePlaylistSelection()

        val mw = model.currentMediaWrapper
        lifecycleScope.launch {
            if (model.switchToVideo()) {
                finish()
                return@launch
            }
            trackInfoState = TvAudioTrackInfoState(state.title.orEmpty(), state.artist.orEmpty())
            if (mw == null || currentCoverArt == mw.artworkMrl) return@launch
            currentCoverArt = mw.artworkMrl
            updateBackground()
        }
    }

    private fun updateBackground() = lifecycleScope.launchWhenStarted {
        val artworkMrl = currentCoverArt
        val width = if (views.albumCover.width > 0) views.albumCover.width else this@AudioPlayerActivity.getScreenWidth()
        val cover = withContext(Dispatchers.IO) { AudioUtil.readCoverBitmap(Uri.decode(artworkMrl), width) }
        val background = withContext(Dispatchers.IO) { cover?.let { UiTools.blurBitmap(it) } }
        if (artworkMrl != currentCoverArt) return@launchWhenStarted
        artworkState = TvAudioArtworkState(cover = cover, background = background)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (playerKeyListenerDelegate.onKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun stop() {
        model.stop()
        finish()
    }

    override fun seek(delta: Int) {
        val time = model.getTime().toInt() + delta
        if (time < 0 || time > model.length) return
        model.setTime(time.toLong())
    }

    override fun isReady() = true

    override fun isReadyForDirectional() = true

    override fun showAdvancedOptions() {
        showAdvancedOptionsPanel()
    }

    override fun previous() {
        model.previous(false)
    }

    override fun next() {
        model.next()
    }

    override fun togglePlayPause() {
        model.togglePlayPause()
    }

    override fun showEqualizer() {
        showEqualizerComposeDialog()
    }

    override fun increaseRate() {
        model.service?.increaseRate()
    }

    override fun decreaseRate() {
        model.service?.decreaseRate()
    }

    override fun resetRate() {
        model.service?.resetRate()
    }

    override fun bookmark() {
        bookmarkModel.addBookmark(this)
        UiTools.snackerConfirm(this, getString(org.videolan.vlc.R.string.bookmark_added), confirmMessage = org.videolan.vlc.R.string.show) {
            showBookmarks()
        }
    }

    private fun playPlaylistItem(position: Int) {
        selectedPlaylistItem = position
        playlistScrollTarget = position
        model.play(position)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        //Check for a joystick event
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK || event.action != MotionEvent.ACTION_MOVE)
            return false

        val inputDevice = event.device

        val dpadx = event.getAxisValue(MotionEvent.AXIS_HAT_X).absoluteValue
        val dpady = event.getAxisValue(MotionEvent.AXIS_HAT_Y).absoluteValue
        if (inputDevice == null || dpadx == 1.0f || dpady == 1.0f) return false

        val x = AndroidDevices.getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X)

        if (x.absoluteValue > 0.3f && System.currentTimeMillis() - lastMove > JOYSTICK_INPUT_DELAY) {
            seek(if (x > 0.0f) 10000 else -10000)
            lastMove = System.currentTimeMillis()
            return true
        }
        return true
    }

    private fun showAdvancedOptionsPanel() {
        if (optionsDelegate == null) {
            val service = model.service ?: return
            optionsDelegate = PlayerOptionsDelegate(this, service, false)
            optionsDelegate?.setPanelHost(tvOptionsPanelHost)
            optionsDelegate?.setBookmarkClickedListener {
                lifecycleScope.launch { if (!showPinIfNeeded()) showBookmarks() }
            }
        }
        optionsDelegate?.show()
    }

    private fun updateTransportControlsState(playing: Boolean = playlistPlaying) {
        val repeatType = model.repeatType
        transportControlsState = TvAudioTransportControlsState(
            shuffleIcon = if (shuffling) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_audio,
            shuffleContentDescription = getString(if (shuffling) org.videolan.vlc.R.string.shuffle_on else org.videolan.vlc.R.string.shuffle),
            shuffleActive = shuffling,
            playPauseIcon = if (playing) R.drawable.ic_pause_player else R.drawable.ic_play_player,
            playPauseContentDescription = getString(if (playing) org.videolan.vlc.R.string.pause else org.videolan.vlc.R.string.play),
            repeatIcon = when (repeatType) {
                PlaybackStateCompat.REPEAT_MODE_ALL -> R.drawable.ic_repeat_all_audio
                PlaybackStateCompat.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_audio
                else -> R.drawable.ic_repeat_audio
            },
            repeatContentDescription = getString(
                when (repeatType) {
                    PlaybackStateCompat.REPEAT_MODE_ALL -> R.string.repeat_all
                    PlaybackStateCompat.REPEAT_MODE_ONE -> R.string.repeat_single
                    else -> R.string.repeat_none
                }
            ),
            repeatActive = repeatType != PlaybackStateCompat.REPEAT_MODE_NONE
        )
    }

    /**
     * Show the bookmarks and initialize the delegate if needed
     */
    private fun showBookmarks() {
        model.service?.let {
            if (!this::bookmarkListDelegate.isInitialized) {
                bookmarkListDelegate = BookmarkListDelegate(this, it, bookmarkModel, false)
                bookmarkListDelegate.setPanelHost(tvBookmarksPanelHost)
                bookmarkListDelegate.visibilityListener = {
                    if (bookmarkListDelegate.visible) bookmarkListDelegate.requestFocus()
                    playlistFocusEnabled = !bookmarkListDelegate.visible
                    quickActionsFocusEnabled = !bookmarkListDelegate.visible
                    views.playlist.isFocusable = !bookmarkListDelegate.visible
                    syncQuickActionsFocusability()
                }
                bookmarkListDelegate.seekListener = { forward, long ->
                    model.jump(forward, long, this)
                }
                bookmarkListDelegate.setMarkerHost(tvBookmarkMarkerHost)
            }
            bookmarkListDelegate.show()
        }
    }

    private fun setShuffleMode(shuffle: Boolean) {
        shuffling = shuffle
        val medias = model.medias?.toMutableList() ?: return
        if (shuffle)
            medias.shuffle()
        else
            medias.sortWith(MediaComparators.BY_TRACK_NUMBER)
        model.load(medias, 0)
        updateTransportControlsState()
    }

    private fun updateRepeatMode() {
        if (model.repeatType != PlaybackStateCompat.REPEAT_MODE_ALL &&
            model.repeatType != PlaybackStateCompat.REPEAT_MODE_ONE
        ) model.repeatType = PlaybackStateCompat.REPEAT_MODE_NONE
        updateTransportControlsState()
    }

    private fun switchRepeatMode() {
        when (model.repeatType) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> {
                model.repeatType = PlaybackStateCompat.REPEAT_MODE_ALL
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                model.repeatType = PlaybackStateCompat.REPEAT_MODE_ONE
            }
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                model.repeatType = PlaybackStateCompat.REPEAT_MODE_NONE
            }
        }
        updateTransportControlsState()
    }

    private fun updatePlaylistSelection() {
        val position = model.currentMediaPosition
        if (position < 0 || position >= playlistItems.size || selectedPlaylistItem == position) return
        selectedPlaylistItem = position
        playlistScrollTarget = position
    }

    companion object {
        const val TAG = "VLC/AudioPlayerActivity"

        const val MEDIA_LIST = "media_list"
        const val MEDIA_PLAYLIST = "media_playlist"
        const val MEDIA_POSITION = "media_position"

        //PAD navigation
        private const val JOYSTICK_INPUT_DELAY = 300
    }

    override fun update() { }

    override fun onMediaEvent(event: IMedia.Event) { }

    override fun onMediaPlayerEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Playing -> {
                playbackStarted = true
            }
        }
    }

    override fun onResumeToVideoClick() {
        model.currentMediaWrapper?.let {
            if (PlaybackService.hasRenderer()) VideoPlayerActivity.startOpened(
                this,
                it.uri, model.currentMediaPosition
            )
            else if (hasMedia()) {
                it.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { model.switchToVideo() }
                finish()
            }
        }
    }
}

@Composable
private fun TvAudioPlayerScreen(onReady: (TvAudioPlayerViews) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            createTvAudioPlayerViews(context).also(onReady).root
        }
    )
}

private data class TvAudioPlayerViews(
    val root: ConstraintLayout,
    val background: ComposeView,
    val playlist: ComposeView,
    val albumCover: ComposeView,
    val trackInfo: ComposeView,
    val quickActions: ComposeView,
    val transportControls: ComposeView,
    val playerOptionsPanel: ComposeView,
    val bookmarksPanel: ComposeView,
    val bookmarkMarkerContainer: ComposeView,
    val mediaProgress: ComposeView,
    val progressLabels: ComposeView
)

private fun createTvAudioPlayerViews(context: Context): TvAudioPlayerViews {
    val root = ConstraintLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    val background = ComposeView(context).apply {
        id = R.id.background
    }
    root.addView(background, matchConstraints().apply {
        leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
    })

    root.addView(Guideline(context).apply { id = R.id.guideline8 }, ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.WRAP_CONTENT,
        ConstraintLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        orientation = ConstraintLayout.LayoutParams.VERTICAL
        guidePercent = 0.65F
    })

    val mediaProgress = ComposeView(context).apply {
        id = R.id.media_progress
        isFocusable = true
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        nextFocusUpId = R.id.playlist
        nextFocusDownId = R.id.button_play
    }
    root.addView(mediaProgress, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
        leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        bottomToTop = R.id.button_play
        marginStart = context.resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal_progressbar)
        marginEnd = context.resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal_progressbar)
    })

    val playlist = ComposeView(context).apply {
        id = R.id.playlist
        isFocusable = true
        nextFocusLeftId = R.id.button_play
        nextFocusRightId = R.id.playlist
        nextFocusUpId = R.id.playlist
        nextFocusDownId = R.id.playlist
    }
    root.addView(playlist, matchConstraints().apply {
        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        bottomToBottom = R.id.media_progress
        startToStart = R.id.guideline8
        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        bottomMargin = 17.dp
    })

    val quickActions = ComposeView(context).apply {
        id = R.id.playback_speed_quick_action
        isFocusable = true
        nextFocusDownId = R.id.media_progress
    }
    root.addView(quickActions, wrapContent().apply {
        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        startToStart = R.id.media_time
        topMargin = context.resources.getDimensionPixelSize(R.dimen.tv_overscan_vertical)
    })

    root.addView(Barrier(context).apply {
        id = R.id.barrier
        type = Barrier.BOTTOM
        setReferencedIds(intArrayOf(R.id.playback_speed_quick_action))
    }, wrapContent())

    val albumCover = ComposeView(context).apply {
        id = R.id.album_cover
    }
    root.addView(albumCover, matchConstraints().apply {
        topToBottom = R.id.barrier
        bottomToTop = R.id.media_title
        startToStart = R.id.media_progress
        endToStart = R.id.guideline8
        dimensionRatio = "1"
        topMargin = 16.dp
    })

    val trackInfo = ComposeView(context).apply {
        id = R.id.media_title
    }
    root.addView(trackInfo, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
        topToBottom = R.id.album_cover
        bottomToTop = R.id.media_time
        startToStart = R.id.media_progress
        endToStart = R.id.guideline8
        topMargin = 16.dp
        bottomMargin = 16.dp
    })

    val bookmarksPanel = ComposeView(context).apply {
        id = VlcR.id.bookmarks_background
        visibility = View.GONE
        isFocusable = false
    }
    root.addView(bookmarksPanel, ConstraintLayout.LayoutParams(500.dp, 0).apply {
        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        bottomToBottom = R.id.media_progress
        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        topMargin = context.resources.getDimensionPixelSize(R.dimen.tv_overscan_vertical)
        bottomMargin = 17.dp
    })

    val bookmarkMarkerContainer = ComposeView(context).apply {
        id = VlcR.id.bookmark_marker_container
    }
    root.addView(bookmarkMarkerContainer, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
        bottomToBottom = R.id.media_progress
        startToStart = R.id.media_progress
        endToEnd = R.id.media_progress
        bottomMargin = 16.dp
    })

    val progressLabels = ComposeView(context).apply {
        id = R.id.media_time
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    root.addView(progressLabels, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
        bottomToTop = R.id.media_progress
        startToStart = R.id.media_progress
        endToEnd = R.id.media_progress
        marginStart = 16.dp
        marginEnd = 16.dp
    })

    val transportControls = ComposeView(context).apply {
        id = R.id.button_play
        isFocusable = true
        nextFocusDownId = R.id.playlist
    }
    root.addView(transportControls, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        startToStart = R.id.media_progress
        endToEnd = R.id.media_progress
        bottomMargin = context.resources.getDimensionPixelSize(R.dimen.tv_overscan_vertical)
    })

    val playerOptionsPanel = ComposeView(context).apply {
        id = VlcR.id.options_background
        visibility = View.GONE
        isClickable = true
        isFocusable = false
        elevation = 16.dp.toFloat()
    }
    root.addView(playerOptionsPanel, matchConstraints().apply {
        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
    })

    return TvAudioPlayerViews(
        root = root,
        background = background,
        playlist = playlist,
        albumCover = albumCover,
        trackInfo = trackInfo,
        quickActions = quickActions,
        transportControls = transportControls,
        playerOptionsPanel = playerOptionsPanel,
        bookmarksPanel = bookmarksPanel,
        bookmarkMarkerContainer = bookmarkMarkerContainer,
        mediaProgress = mediaProgress,
        progressLabels = progressLabels
    )
}

private fun matchConstraints() = ConstraintLayout.LayoutParams(0, 0)

private fun wrapContent() = ConstraintLayout.LayoutParams(
    ConstraintLayout.LayoutParams.WRAP_CONTENT,
    ConstraintLayout.LayoutParams.WRAP_CONTENT
)

private fun Context.resolveThemeColor(attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return if (typedValue.resourceId != 0) ContextCompat.getColor(this, typedValue.resourceId) else typedValue.data
}
