/*****************************************************************************
 * AudioPlayer.kt
 *
 * Copyright © 2011-2019 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp as composeDp
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.MediaPlayer
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
import org.videolan.resources.TAG_ITEM
import org.videolan.tools.AUDIO_HINGE_ON_RIGHT
import org.videolan.tools.AUDIO_PLAY_PROGRESS_MODE
import org.videolan.tools.KEY_AUDIO_PLAYER_SHOW_COVER
import org.videolan.tools.KEY_AUDIO_SHOW_BOOKMARK_MARKERS
import org.videolan.tools.KEY_AUDIO_SHOW_BOOkMARK_BUTTONS
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.KEY_SHOW_TRACK_INFO
import org.videolan.tools.PREF_PLAYLIST_TIPS_SHOWN
import org.videolan.tools.PREF_RESTORE_VIDEO_TIPS_SHOWN
import org.videolan.tools.RESTORE_BACKGROUND_VIDEO
import org.videolan.tools.SHOW_REMAINING_TIME
import org.videolan.tools.Settings
import org.videolan.tools.copy
import org.videolan.tools.dp
import org.videolan.tools.formatRateString
import org.videolan.tools.hasRtl
import org.videolan.tools.isStarted
import org.videolan.tools.markBidi
import org.videolan.tools.putSingle
import org.videolan.tools.retrieveParent
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioHeaderActionButton
import org.videolan.vlc.compose.components.VLCAudioHeaderBackground
import org.videolan.vlc.compose.components.VLCAudioHeaderDivider
import org.videolan.vlc.compose.components.VLCAudioHeaderPlayPauseButton
import org.videolan.vlc.compose.components.VLCAudioHeaderTimeLabel
import org.videolan.vlc.compose.components.VLCAudioHeaderTransportButton
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPill
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPillState
import org.videolan.vlc.compose.components.VLCAudioPlayerChips
import org.videolan.vlc.compose.components.VLCAudioPlayerChipsState
import org.videolan.vlc.compose.components.VLCAudioResumeVideoHint
import org.videolan.vlc.compose.components.VLCAudioSeekDelayLabel
import org.videolan.vlc.compose.components.VLCAudioSeekHudButton
import org.videolan.vlc.compose.components.VLCAudioTrackInfoText
import org.videolan.vlc.compose.components.VLCAudioTrackInfoTextStyle
import org.videolan.vlc.compose.components.VLCAudioTimelineTimeLabel
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.databinding.AudioPlayerBinding
import org.videolan.vlc.gui.AudioPlayerContainerActivity
import org.videolan.vlc.gui.HeaderMediaListActivity
import org.videolan.vlc.gui.HeaderMediaListActivity.Companion.ARTIST_FROM_ALBUM
import org.videolan.vlc.gui.InfoActivity
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.browser.KEY_JUMP_TO
import org.videolan.vlc.gui.browser.KEY_MEDIA
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showPlaybackSpeedComposeDialog
import org.videolan.vlc.gui.dialogs.showSleepTimerComposeDialog
import org.videolan.vlc.gui.helpers.AudioUtil.setRingtone
import org.videolan.vlc.gui.helpers.BookmarkListDelegate
import org.videolan.vlc.gui.helpers.PlayerOptionsDelegate
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.isTablet
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.gui.view.AbRepeatControlsView
import org.videolan.vlc.gui.view.AudioMediaSwitcher
import org.videolan.vlc.gui.view.AudioMediaSwitcher.AudioMediaSwitcherListener
import org.videolan.vlc.gui.view.PlayerTimelineSeekBarView
import org.videolan.vlc.manageAbRepeatStep
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.media.PlaylistManager.Companion.hasMedia
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_FAV_ADD
import org.videolan.vlc.util.ContextOption.CTX_FAV_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ALBUM
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_FOLDER
import org.videolan.vlc.util.ContextOption.CTX_INFORMATION
import org.videolan.vlc.util.ContextOption.CTX_REMOVE_FROM_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_SET_RINGTONE
import org.videolan.vlc.util.ContextOption.CTX_SHARE
import org.videolan.vlc.util.ContextOption.CTX_STOP_AFTER_THIS
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.util.LocaleUtil
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.util.launchWhenStarted
import org.videolan.vlc.util.share
import org.videolan.vlc.viewmodels.BookmarkModel
import org.videolan.vlc.viewmodels.PlaybackProgress
import org.videolan.vlc.viewmodels.PlaylistModel
import java.text.DateFormat.getTimeInstance
import kotlin.math.absoluteValue

private const val TAG = "VLC/AudioPlayer"
private const val SEARCH_TIMEOUT_MILLIS = 10000L

private data class AudioPlaylistSwitchState(
        @DrawableRes val icon: Int = R.drawable.ic_playlist_audio_on,
        val contentDescription: String = "",
        val usePrimaryTint: Boolean = true
)

private data class AudioPlayPauseState(
        @DrawableRes val icon: Int = R.drawable.ic_play_player,
        val contentDescription: String = ""
)

private data class AudioTransportState(
        @DrawableRes val icon: Int,
        val contentDescription: String = ""
)

private data class AudioSeekHudState(
        val delayText: String = "",
        val rewindContentDescription: String = "",
        val forwardContentDescription: String = ""
)

@Composable
private fun AudioPlayerChipIcon(@DrawableRes drawable: Int) {
    Icon(
            painter = painterResource(drawable),
            contentDescription = null,
            tint = VLCThemeDefaults.colors.audioChipsTextColor,
            modifier = Modifier.size(18.composeDp)
    )
}

@Composable
private fun AudioPlayerHeaderIcon(@DrawableRes drawable: Int, usePrimaryTint: Boolean = false) {
    val colors = VLCThemeDefaults.colors
    Icon(
            painter = painterResource(drawable),
            contentDescription = null,
            tint = if (usePrimaryTint) colors.primary else colors.audioMenuIcon,
            modifier = Modifier.size(24.composeDp)
    )
}

@Composable
private fun AudioPlayerPlayPauseIcon(@DrawableRes drawable: Int, size: Dp = 38.composeDp) {
    Icon(
            painter = painterResource(drawable),
            contentDescription = null,
            tint = VLCThemeDefaults.colors.playerIconColor,
            modifier = Modifier.size(size)
    )
}

@Composable
private fun AudioPlayerTransportIcon(@DrawableRes drawable: Int, size: Dp = 32.composeDp) {
    Icon(
            painter = painterResource(drawable),
            contentDescription = null,
            tint = VLCThemeDefaults.colors.playerIconColor,
            modifier = Modifier.size(size)
    )
}

class AudioPlayer(
        val activity: AudioPlayerContainerActivity,
        container: ViewGroup,
        savedInstanceState: Bundle?
) : IAudioPlayerAnimator by AudioPlayerAnimator() {

    private lateinit var binding: AudioPlayerBinding
    private lateinit var settings: SharedPreferences
    private val handler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
    lateinit var playlistModel: PlaylistModel
    lateinit var bookmarkModel: BookmarkModel
    private lateinit var optionsDelegate: PlayerOptionsDelegate
    lateinit var bookmarkListDelegate: BookmarkListDelegate
    private var playlistItems by mutableStateOf<List<MediaWrapper>>(emptyList())
    private var playlistCurrentIndex by mutableStateOf(-1)
    private var playlistPlaying by mutableStateOf(false)
    private var playlistShowTrackNumbers by mutableStateOf(false)
    private var playlistShowReorderButtons by mutableStateOf(true)
    private var playlistFiltering by mutableStateOf(false)
    private var playlistStopAfter by mutableStateOf(-1)
    private var playlistScrollSerial = 0
    private var playlistScrollRequest by mutableStateOf<AudioPlaylistScrollRequest?>(null)
    private var audioPlayerChipsState by mutableStateOf(VLCAudioPlayerChipsState())
    private var audioQueueProgressPillState by mutableStateOf(VLCAudioQueueProgressPillState())
    private var audioHeaderTimeText by mutableStateOf("")
    private var audioPlaylistSwitchState by mutableStateOf(AudioPlaylistSwitchState())
    private var audioPlayPauseState by mutableStateOf(AudioPlayPauseState())
    private var audioShuffleState by mutableStateOf(AudioTransportState(R.drawable.ic_shuffle_audio))
    private var audioRepeatState by mutableStateOf(AudioTransportState(R.drawable.ic_repeat_audio))
    private var audioSeekHudState by mutableStateOf(AudioSeekHudState())
    private var audioTrackTitleText by mutableStateOf("")
    private var audioTrackSubtitleText by mutableStateOf("")
    private var audioTrackDetailText by mutableStateOf("")
    private var audioTimelineTimeText by mutableStateOf("")
    private var audioTimelineLengthText by mutableStateOf("")
    private var resumeVideoHintVisible by mutableStateOf(false)

    val lifecycle: Lifecycle
        get() = activity.lifecycle

    val resources: Resources
        get() = activity.resources

    private val context: Context?
        get() = activity

    private val lifecycleScope
        get() = activity.lifecycleScope

    private val viewLifecycleOwner: LifecycleOwner
        get() = activity

    val isVisible: Boolean
        get() = binding.root.isShown

    private var showRemainingTime = false
    private var previewingSeek = false
    private var playerState = 0

    private var abRepeatControlsActive = false
    private var audioPlayProgressMode:Boolean = false
    private var lastEndsAt = -1L
    private var isDragging = false
    private var currentChapters: Pair<MediaWrapper,  List<MediaPlayer.Chapter>?>? = null

    init {
        savedInstanceState?.let {
            playerState = it.getInt("player_state")
            showRemainingTime = it.getBoolean("show_remaining_time")
        }
        settings = Settings.getInstance(activity)
        playlistModel = PlaylistModel.get(activity)
        bookmarkModel = BookmarkModel.get(activity)
        binding = AudioPlayerBinding.inflate(LayoutInflater.from(activity), container, true)
        setupAnimator(binding)
        lifecycleScope.launch(Dispatchers.Main) {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(activity)
                        .windowLayoutInfo(activity)
                        .collect { layoutInfo ->
                            foldingFeature = layoutInfo.displayFeatures.firstOrNull() as? FoldingFeature
                        }
            }
        }
        setupView()
        registerObservers()
    }

    fun requireActivity(): AudioPlayerContainerActivity = activity

    fun requireContext(): Context = activity

    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String = activity.getString(resId, *formatArgs)

    fun startActivity(intent: Intent) = activity.startActivity(intent)

    private fun registerObservers() {
        playlistModel.progress.observe(viewLifecycleOwner) { it?.let { updateProgress(it) } }
        playlistModel.speed.observe(viewLifecycleOwner) { showChips() }
        playlistModel.filteringState.observe(viewLifecycleOwner) {
            playlistFiltering = it
            playlistShowReorderButtons = !it
        }
        playlistModel.dataset.asFlow().conflate().onEach {
            doUpdate()
            updateAudioPlaylistItems(it)
            delay(50L)
        }.launchWhenStarted(lifecycleScope)
        PlaybackService.playerSleepTime.observe(viewLifecycleOwner) {
            showChips()
        }
        Settings.setAudioControlsChangeListener {
            lifecycleScope.launchWhenStarted {
                doUpdate()
            }
        }
        lifecycleScope.launchWhenStarted {
            PlaylistManager.repeating.collect {
                updateRepeatMode()
            }
        }
    }

    private fun setupView() {
        binding.audioMediaSwitcher.setAudioMediaSwitcherListener(headerMediaSwitcherListener)
        binding.coverMediaSwitcher.setAudioMediaSwitcherListener(coverMediaSwitcherListener)
        binding.playlistSearchText.onQueryChanged = ::onSearchQueryChanged
        setupAudioPlaylistQueue()
        binding.header.setOnClickListener {
            val activity = activity as AudioPlayerContainerActivity
            activity.slideUpOrDownAudioPlayer()
        }

        binding.next.setOnTouchListener(LongSeekListener(true))
        binding.previous.setOnTouchListener(LongSeekListener(false))

        binding.timeline.setOnTimelineSeekChangeListener(timelineListener)

        onSlide(0f)
        playlistModel.service?.playlistManager?.abRepeat?.observe(viewLifecycleOwner) { abvalues ->
            binding.abRepeatMarkerGuidelineContainer.setMarkerPositions(
                    start = abvalues.start,
                    stop = abvalues.stop,
                    length = playlistModel.service!!.playlistManager.player.getLength()
            )
            refreshAbRepeatStep()
        }
        playlistModel.service?.playlistManager?.abRepeatOn?.observe(viewLifecycleOwner) {
            abRepeatControlsActive = it
            binding.abRepeatMarkerGuidelineContainer.visibility = if (it) View.VISIBLE else View.GONE
            binding.audioPlayProgress.visibility = if (!shouldHidePlayProgress()) View.VISIBLE else View.GONE

            refreshAbRepeatStep()
        }
        Settings.audioShowTrackNumbers.observe(viewLifecycleOwner) { showTrackNumbers ->
            playlistShowTrackNumbers = showTrackNumbers
        }

        binding.abRepeatContainer.setOnClickListener {
            playlistModel.service?.playlistManager?.setABRepeatValue(playlistModel.service?.playlistManager?.getCurrentMedia(), binding.timeline.progress.toLong())
        }

        audioPlayProgressMode = Settings.getInstance(requireActivity()).getBoolean(AUDIO_PLAY_PROGRESS_MODE, false)
        audioHeaderTimeText = getString(R.string.time_0)
        audioTimelineTimeText = getString(R.string.time_0)
        audioTimelineLengthText = getString(R.string.time_0)
        setupAudioHeaderDecorations()
        setupAudioHeaderTime()
        setupAudioTimelineTimeLabels()
        setupAudioHeaderActions()
        setupAudioHeaderPlayPause()
        setupAudioHeaderTransportControls()
        setupAudioPlayerTransportControls()
        setupAudioSeekHudControls()
        setupAudioChapterControls()
        setupAudioHingeControls()
        setupAudioTrackInfoTexts()
        setupAudioQueueProgressPill()
        setupResumeVideoHint()
        setupPlaybackChips()

        setBottomMargin()

        bookmarkModel.dataset.observe(requireActivity()) {
            lifecycleScope.launch {
                doUpdate()
            }
        }
    }

    fun onDestroy() {
        Settings.removeAudioControlsChangeListener()
        currentChapters = null
    }

    fun setBottomMargin() {
        (binding.playPause.layoutParams as? ConstraintLayout.LayoutParams)?.let {
            val audioPlayerContainerActivity = (requireActivity() as AudioPlayerContainerActivity)
            if (audioPlayerContainerActivity is MainActivity && !audioPlayerContainerActivity.isTablet()) it.bottomMargin = 8.dp + audioPlayerContainerActivity.bottomInset
        }
    }

    fun isTablet() = requireActivity().isTablet()

    private fun setupAudioPlaylistQueue() {
        val showInlineActions = isTablet() || AndroidDevices.isTv
        binding.songsList.setContent {
            VLCTheme {
                AudioPlaylistQueue(
                    items = playlistItems,
                    currentIndex = playlistCurrentIndex,
                    playing = playlistPlaying,
                    showTrackNumbers = playlistShowTrackNumbers,
                    showReorderButtons = playlistShowReorderButtons,
                    showInlineActions = showInlineActions,
                    stopAfter = playlistStopAfter,
                    scrollRequest = playlistScrollRequest,
                    onScrollRequestConsumed = { playlistScrollRequest = null },
                    onPlayItem = ::playPlaylistItem,
                    onShowContext = ::showPlaylistContext,
                    onDismissItem = ::removePlaylistItem,
                    onMoveItem = ::movePlaylistItem
                )
            }
        }
    }

    private fun updateAudioPlaylistItems(items: List<MediaWrapper>) {
        playlistItems = items.toList()
        playlistPlaying = playlistModel.playing
        playlistStopAfter = playlistModel.service?.playlistManager?.stopAfter ?: -1
        requestPlaylistSelection(playlistModel.selection)
    }

    private fun requestPlaylistSelection(position: Int) {
        playlistCurrentIndex = position
        if (position < 0) return
        if (playlistModel.lastActionWasEdit) {
            playlistModel.lastActionWasEdit = false
            return
        }
        requestPlaylistScroll(position)
    }

    private fun requestPlaylistScroll(position: Int) {
        if (position !in playlistItems.indices) return
        playlistScrollRequest = AudioPlaylistScrollRequest(position, ++playlistScrollSerial)
    }

    private fun playPlaylistItem(position: Int, item: MediaWrapper) {
        clearSearch()
        playlistModel.play(playlistModel.getPlaylistPosition(position, item))
    }

    private fun removePlaylistItem(position: Int, item: MediaWrapper) {
        if (position !in playlistItems.indices) return
        val message = String.format(getString(R.string.remove_playlist_item), item.title)
        val originalPosition = playlistModel.getOriginalPosition(position)
        UiTools.snackerWithCancel(requireActivity(), message, overAudioPlayer = true, action = {}) {
            playlistModel.insertMedia(originalPosition, item)
        }
        playlistModel.remove(position)
    }

    private fun movePlaylistItem(from: Int, to: Int) {
        if (playlistFiltering || from !in playlistItems.indices || to !in playlistItems.indices) return
        val nextItems = playlistItems.toMutableList()
        val movedItem = nextItems.removeAt(from)
        nextItems.add(to, movedItem)
        playlistItems = nextItems
        playlistCurrentIndex = when (playlistCurrentIndex) {
            from -> to
            to -> from
            else -> playlistCurrentIndex
        }
        playlistModel.move(from, if (to > from) to + 1 else to)
    }

    private fun setupAudioHeaderDecorations() {
        binding.headerBackground.setContent {
            VLCTheme {
                VLCAudioHeaderBackground()
            }
        }
        binding.headerDivider.setContent {
            VLCTheme {
                VLCAudioHeaderDivider()
            }
        }
    }

    private fun setupAudioHeaderTime() {
        binding.headerTime.setContent {
            VLCTheme {
                VLCAudioHeaderTimeLabel(
                    text = audioHeaderTimeText,
                    onClick = { toggleRemainingTimeMode() }
                )
            }
        }
    }

    private fun setupAudioTimelineTimeLabels() {
        binding.time.setContent {
            VLCTheme {
                VLCAudioTimelineTimeLabel(
                    text = audioTimelineTimeText,
                    onClick = { toggleRemainingTimeMode() }
                )
            }
        }
        binding.length.setContent {
            VLCTheme {
                VLCAudioTimelineTimeLabel(
                    text = audioTimelineLengthText,
                    onClick = { toggleRemainingTimeMode() }
                )
            }
        }
    }

    private fun setupAudioHeaderActions() {
        binding.abRepeatReset.setContent {
            VLCTheme {
                VLCAudioHeaderActionButton(
                    contentDescription = getString(R.string.ab_repeat_reset),
                    onClick = { onABRepeatResetClick(binding.abRepeatReset) }
                ) {
                    AudioPlayerHeaderIcon(R.drawable.ic_abrepeat_reset_marker_audio)
                }
            }
        }
        binding.abRepeatStop.setContent {
            VLCTheme {
                VLCAudioHeaderActionButton(
                    contentDescription = getString(R.string.ab_repeat_stop),
                    onClick = { onABRepeatStopClick(binding.abRepeatStop) }
                ) {
                    AudioPlayerHeaderIcon(R.drawable.ic_abrepeat_reset_audio)
                }
            }
        }
        binding.playlistSearch.setContent {
            VLCTheme {
                VLCAudioHeaderActionButton(
                    contentDescription = getString(R.string.search),
                    onClick = { onSearchClick(binding.playlistSearch) }
                ) {
                    AudioPlayerHeaderIcon(R.drawable.ic_search_audio)
                }
            }
        }
        binding.playlistSwitch.setContent {
            VLCTheme {
                val state = audioPlaylistSwitchState
                VLCAudioHeaderActionButton(
                    contentDescription = state.contentDescription,
                    onClick = { onPlaylistSwitchClick(binding.playlistSwitch) }
                ) {
                    AudioPlayerHeaderIcon(state.icon, usePrimaryTint = state.usePrimaryTint)
                }
            }
        }
        binding.advFunction.setContent {
            VLCTheme {
                VLCAudioHeaderActionButton(
                    contentDescription = getString(R.string.advanced),
                    onClick = { showAdvancedOptions(binding.advFunction) }
                ) {
                    AudioPlayerHeaderIcon(R.drawable.ic_overflow_audio)
                }
            }
        }
        updatePlaylistSwitchChrome(isShowingCover())
    }

    private fun setupAudioHeaderPlayPause() {
        binding.headerPlayPause.setContent {
            VLCTheme {
                val state = audioPlayPauseState
                VLCAudioHeaderPlayPauseButton(
                    contentDescription = state.contentDescription,
                    onClick = { onPlayPauseClick(binding.headerPlayPause) },
                    onLongClick = { onStopClick(binding.headerPlayPause) }
                ) {
                    AudioPlayerPlayPauseIcon(state.icon)
                }
            }
        }
    }

    private fun setupAudioHeaderTransportControls() {
        binding.headerShuffle.setContent {
            VLCTheme {
                val state = audioShuffleState
                VLCAudioHeaderTransportButton(
                    contentDescription = state.contentDescription,
                    onClick = { onShuffleClick(binding.headerShuffle) }
                ) {
                    AudioPlayerTransportIcon(state.icon)
                }
            }
        }
        binding.headerPrevious.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.previous),
                    onClick = { onPreviousClick(binding.headerPrevious) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_previous)
                }
            }
        }
        binding.headerLargePlayPause.setContent {
            VLCTheme {
                val state = audioPlayPauseState
                VLCAudioHeaderTransportButton(
                    contentDescription = state.contentDescription,
                    size = 56.composeDp,
                    onClick = { onPlayPauseClick(binding.headerLargePlayPause) },
                    onLongClick = { onStopClick(binding.headerLargePlayPause) }
                ) {
                    AudioPlayerPlayPauseIcon(state.icon, size = 48.composeDp)
                }
            }
        }
        binding.headerNext.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.next),
                    onClick = { onNextClick(binding.headerNext) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_next)
                }
            }
        }
        binding.headerRepeat.setContent {
            VLCTheme {
                val state = audioRepeatState
                VLCAudioHeaderTransportButton(
                    contentDescription = state.contentDescription,
                    onClick = { onRepeatClick(binding.headerRepeat) }
                ) {
                    AudioPlayerTransportIcon(state.icon)
                }
            }
        }
    }

    private fun setupAudioPlayerTransportControls() {
        binding.shuffle.setContent {
            VLCTheme {
                val state = audioShuffleState
                VLCAudioHeaderTransportButton(
                    contentDescription = state.contentDescription,
                    onClick = { onShuffleClick(binding.shuffle) }
                ) {
                    AudioPlayerTransportIcon(state.icon)
                }
            }
        }
        binding.previous.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.previous),
                    onClick = { onPreviousClick(binding.previous) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_previous)
                }
            }
        }
        binding.playPause.setContent {
            VLCTheme {
                val state = audioPlayPauseState
                VLCAudioHeaderTransportButton(
                    contentDescription = state.contentDescription,
                    size = 56.composeDp,
                    onClick = { onPlayPauseClick(binding.playPause) },
                    onLongClick = { onStopClick(binding.playPause) }
                ) {
                    AudioPlayerPlayPauseIcon(state.icon, size = 48.composeDp)
                }
            }
        }
        binding.next.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.next),
                    onClick = { onNextClick(binding.next) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_next)
                }
            }
        }
        binding.repeat.setContent {
            VLCTheme {
                val state = audioRepeatState
                VLCAudioHeaderTransportButton(
                    contentDescription = state.contentDescription,
                    onClick = { onRepeatClick(binding.repeat) }
                ) {
                    AudioPlayerTransportIcon(state.icon)
                }
            }
        }
    }

    private fun setupAudioSeekHudControls() {
        binding.audioRewindBookmark.setContent {
            VLCTheme {
                VLCAudioSeekHudButton(
                    contentDescription = getString(R.string.previous_bookmark),
                    onClick = { onPreviousBookmark(binding.audioRewindBookmark) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_player_bookmark_previous)
                }
            }
        }
        binding.audioRewind10.setContent {
            VLCTheme {
                val state = audioSeekHudState
                VLCAudioSeekHudButton(
                    contentDescription = state.rewindContentDescription,
                    onClick = { onJumpBack(binding.audioRewind10) },
                    onLongClick = { onJumpBackLong(binding.audioRewind10) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_player_rewind_10)
                }
            }
        }
        binding.audioRewindText.setContent {
            VLCTheme {
                VLCAudioSeekDelayLabel(text = audioSeekHudState.delayText)
            }
        }
        binding.audioForward10.setContent {
            VLCTheme {
                val state = audioSeekHudState
                VLCAudioSeekHudButton(
                    contentDescription = state.forwardContentDescription,
                    onClick = { onJumpForward(binding.audioForward10) },
                    onLongClick = { onJumpForwardLong(binding.audioForward10) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_player_forward_10)
                }
            }
        }
        binding.audioForwardText.setContent {
            VLCTheme {
                VLCAudioSeekDelayLabel(text = audioSeekHudState.delayText)
            }
        }
        binding.audioForwardBookmark.setContent {
            VLCTheme {
                VLCAudioSeekHudButton(
                    contentDescription = getString(R.string.next_bookmark),
                    onClick = { onNextBookmark(binding.audioForwardBookmark) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_player_bookmark_next)
                }
            }
        }
        updateAudioSeekHudState()
    }

    private fun setupAudioChapterControls() {
        binding.previousChapter?.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.previous),
                    size = 40.composeDp,
                    onClick = { coverMediaSwitcherListener.onChapterSwitching(false) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_chevron_left, size = 24.composeDp)
                }
            }
        }
        binding.nextChapter?.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.next),
                    size = 40.composeDp,
                    onClick = { coverMediaSwitcherListener.onChapterSwitching(true) }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_chevron_right, size = 24.composeDp)
                }
            }
        }
    }

    private fun setupAudioHingeControls() {
        binding.hingeGoLeft.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.audio_hinge_go_left),
                    size = 40.composeDp,
                    onClick = {
                        Settings.getInstance(requireActivity()).putSingle(AUDIO_HINGE_ON_RIGHT, false)
                        manageHinge()
                    }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_arrow_left, size = 24.composeDp)
                }
            }
        }
        binding.hingeGoRight.setContent {
            VLCTheme {
                VLCAudioHeaderTransportButton(
                    contentDescription = getString(R.string.audio_hinge_go_right),
                    size = 40.composeDp,
                    onClick = {
                        Settings.getInstance(requireActivity()).putSingle(AUDIO_HINGE_ON_RIGHT, true)
                        manageHinge()
                    }
                ) {
                    AudioPlayerTransportIcon(R.drawable.ic_arrow_right, size = 24.composeDp)
                }
            }
        }
    }

    private fun setupAudioTrackInfoTexts() {
        binding.songTitle?.setContent {
            VLCTheme {
                VLCAudioTrackInfoText(
                    text = audioTrackTitleText,
                    style = VLCAudioTrackInfoTextStyle.Title,
                    onClick = { coverMediaSwitcherListener.onTextClicked() }
                )
            }
        }
        binding.songSubtitle?.setContent {
            VLCTheme {
                VLCAudioTrackInfoText(
                    text = audioTrackSubtitleText,
                    style = VLCAudioTrackInfoTextStyle.Subtitle,
                    onClick = { coverMediaSwitcherListener.onTextClicked() }
                )
            }
        }
        binding.songTrackInfo?.setContent {
            VLCTheme {
                VLCAudioTrackInfoText(
                    text = audioTrackDetailText,
                    style = VLCAudioTrackInfoTextStyle.Detail
                )
            }
        }
    }

    private fun updateAudioSeekHudState() {
        val delayText = Settings.audioJumpDelay.toString()
        audioSeekHudState = AudioSeekHudState(
                delayText = delayText,
                rewindContentDescription = getString(R.string.talkback_action_rewind, delayText),
                forwardContentDescription = getString(R.string.talkback_action_forward, delayText)
        )
    }

    private fun updateAudioPlayPause(playing: Boolean, contentDescription: String) {
        audioPlayPauseState = AudioPlayPauseState(
                icon = if (playing) R.drawable.ic_pause_player else R.drawable.ic_play_player,
                contentDescription = contentDescription
        )
    }

    fun updatePlaylistSwitchChrome(showCover: Boolean, announce: Boolean = false) {
        val text = getString(if (showCover) R.string.hide_playlist else R.string.show_playlist)
        audioPlaylistSwitchState = AudioPlaylistSwitchState(
                icon = if (showCover) R.drawable.ic_playlist_audio else R.drawable.ic_playlist_audio_on,
                contentDescription = text,
                usePrimaryTint = !showCover
        )
        if (announce) binding.playlistSwitch.announceForAccessibility(text)
    }

    private fun setupAudioQueueProgressPill() {
        binding.audioPlayProgress.setContent {
            VLCTheme {
                VLCAudioQueueProgressPill(
                    state = audioQueueProgressPillState,
                    onClick = { toggleAudioPlayProgressMode() }
                )
            }
        }
    }

    private fun setupResumeVideoHint() {
        binding.resumeVideoHint.setContent {
            VLCTheme {
                if (resumeVideoHintVisible) {
                    VLCAudioResumeVideoHint(
                            message = getString(R.string.return_to_video),
                            onClick = {
                                hideResumeVideoHint()
                                onResumeToVideoClick()
                            }
                    )
                }
            }
        }
    }

    private fun showResumeVideoHint() {
        resumeVideoHintVisible = true
        binding.resumeVideoHint.setVisible()
        lifecycleScope.launch {
            delay(4000L)
            hideResumeVideoHint()
        }
    }

    private fun hideResumeVideoHint() {
        if (!::binding.isInitialized) return
        resumeVideoHintVisible = false
        binding.resumeVideoHint.setGone()
    }

    private fun toggleAudioPlayProgressMode() {
        val activity = activity ?: return
        audioPlayProgressMode = !audioPlayProgressMode
        Settings.getInstance(activity).putSingle(AUDIO_PLAY_PROGRESS_MODE, audioPlayProgressMode)
        playlistModel.progress.value?.let { updateProgress(it) }
    }

    private fun toggleRemainingTimeMode() {
        val context = context ?: return
        showRemainingTime = !showRemainingTime
        Settings.getInstance(context).edit().putBoolean(SHOW_REMAINING_TIME, showRemainingTime).apply()
        playlistModel.progress.value?.let { updateProgress(it) }
    }

    private fun setupPlaybackChips() {
        binding.playbackChips.setContent {
            VLCTheme {
                val state = audioPlayerChipsState
                VLCAudioPlayerChips(
                    state = state,
                    speedIconContent = {
                        AudioPlayerChipIcon(if (state.speedUsesGlobalRate) R.drawable.ic_speed_all else R.drawable.ic_speed)
                    },
                    sleepIconContent = { AudioPlayerChipIcon(R.drawable.ic_sleep) },
                    onSpeedClick = { activity.showPlaybackSpeedComposeDialog() },
                    onSpeedLongClick = {
                        playlistModel.service?.setRate(1F, true)
                        showChips()
                    },
                    onSleepClick = { activity.showSleepTimerComposeDialog() },
                    onSleepLongClick = {
                        playlistModel.service?.setSleepTimer(null)
                        showChips()
                    }
                )
            }
        }
    }

    fun showChips() {
        val context = context ?: return
        val speedText = playlistModel.speed.value
                ?.takeUnless { it == 1.0F }
                ?.formatRateString()
        val sleepText = PlaybackService.playerSleepTime.value
                ?.let { DateFormat.getTimeFormat(context).format(it.time) }
        val newState = VLCAudioPlayerChipsState(
                speedText = speedText,
                sleepText = sleepText,
                speedUsesGlobalRate = settings.getBoolean(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL, false)
        )
        audioPlayerChipsState = newState

        if (!newState.hasVisibleChips) {
            binding.playbackChips.setGone()
        } else {
            binding.playbackChips.setVisible()
        }
    }

    fun onResume() {
        onStateChanged(playerState)
        showRemainingTime = Settings.getInstance(requireContext()).getBoolean(SHOW_REMAINING_TIME, false)
        val restoreVideoTipCount = settings.getInt(PREF_RESTORE_VIDEO_TIPS_SHOWN, 0)
        val forceRestoreVideo = settings.getBoolean(RESTORE_BACKGROUND_VIDEO, false)
        playlistModel.service?.let {
            if (!it.isVideoPlaying && it.videoTracksCount > 0)
                if ( !forceRestoreVideo && restoreVideoTipCount < 4) {
                    showResumeVideoHint()
                    settings.putSingle(PREF_RESTORE_VIDEO_TIPS_SHOWN, restoreVideoTipCount + 1)
                } else if (forceRestoreVideo && !PlaylistManager.playingAsAudio) {
                    onResumeToVideoClick()
                }
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("player_state", playerState)
        outState.putBoolean("show_remaining_time", showRemainingTime)
    }

    private val ctxReceiver: CtxActionReceiver = object : CtxActionReceiver {
        override fun onCtxAction(position: Int, option: ContextOption) {
            val media = playlistItems.getOrNull(position) ?: return
            when (option) {
                CTX_SET_RINGTONE -> activity.setRingtone(media)
                CTX_ADD_TO_PLAYLIST -> {
                    requireActivity().addToPlaylist(listOf(media))
                }
                CTX_REMOVE_FROM_PLAYLIST -> {
                    removePlaylistItem(position, media)
                }
                CTX_STOP_AFTER_THIS -> {
                    val pos = if (playlistModel.service?.playlistManager?.stopAfter != position) position else -1
                    playlistModel.stopAfter(pos)
                    playlistStopAfter = pos
                }
                CTX_INFORMATION -> showInfoDialog(media)
                CTX_GO_TO_FOLDER -> showParentFolder(media)
                CTX_GO_TO_ALBUM -> {
                    val i = Intent(requireActivity(), HeaderMediaListActivity::class.java)
                    i.putExtra(TAG_ITEM, media.album)
                    startActivity(i)
                }
                CTX_GO_TO_ARTIST -> lifecycleScope.launch(Dispatchers.IO) {
                    val i = Intent(requireActivity(), SecondaryActivity::class.java)
                    i.putExtra(SecondaryActivity.KEY_FRAGMENT, SecondaryActivity.ALBUMS_SONGS)
                    i.putExtra(TAG_ITEM, media.artist)
                    i.putExtra(ARTIST_FROM_ALBUM, true)
                    i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
                    startActivity(i)
                }
                CTX_FAV_ADD, CTX_FAV_REMOVE -> lifecycleScope.launch {
                    media.isFavorite = option == CTX_FAV_ADD
                    playlistItems = playlistItems.toList()
                }
                CTX_SHARE -> lifecycleScope.launch { (requireActivity() as AppCompatActivity).share(media) }
                else -> {}
            }
        }
    }

    private fun showInfoDialog(media: MediaWrapper) {
        val i = Intent(requireActivity(), InfoActivity::class.java)
        i.putExtra(TAG_ITEM, media)
        startActivity(i)
    }

    private fun showParentFolder(media: MediaWrapper) {
        val parent = MLServiceLocator.getAbstractMediaWrapper(media.uri.retrieveParent()).apply {
            type = MediaWrapper.TYPE_DIR
        }
        startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, parent)
            putExtra(KEY_JUMP_TO, media)
            putExtra(SecondaryActivity.KEY_FRAGMENT, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun showPlaylistContext(position: Int, item: MediaWrapper) {
        if (position !in playlistItems.indices) return
        val flags = FlagSet(ContextOption::class.java).apply {
            addAll(CTX_GO_TO_FOLDER, CTX_INFORMATION, CTX_REMOVE_FROM_PLAYLIST, CTX_STOP_AFTER_THIS)
            if (item.uri?.scheme != "content") addAll(CTX_ADD_TO_PLAYLIST, CTX_SET_RINGTONE, CTX_SHARE)
            if (item.album != null) add(CTX_GO_TO_ALBUM)
            if (item.artist != null) add(CTX_GO_TO_ARTIST)
            if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
        }
        showContext(activity, ctxReceiver, position, item, flags)
    }

    private suspend fun doUpdate() {
        if (isVisible && playlistModel.switchToVideo()) return
        updatePlayPause()
        updateShuffleMode()
        updateRepeatMode()
        binding.audioMediaSwitcher.updateMedia(playlistModel.service)
        binding.coverMediaSwitcher.updateMedia(playlistModel.service)
        playlistModel.service?.currentMediaWrapper?.let {
            binding.audioMediaSwitcher.contentDescription = getString(R.string.talkback_audio_player,TalkbackUtil.getAudioTrack(requireActivity(), it))
            binding.trackInfoContainer?.contentDescription = getString(R.string.talkback_audio_player,TalkbackUtil.getAudioTrack(requireActivity(), it))
        }

        val chapter = playlistModel.service?.getCurrentChapter()
        if (chapter.isNullOrEmpty()) {
            binding.nextChapter?.visibility = View.GONE
            binding.previousChapter?.visibility = View.GONE
        } else {
            binding.nextChapter?.visibility = View.VISIBLE
            binding.previousChapter?.visibility = View.VISIBLE
        }

        if (isShowingCover() && !bookmarkModel.dataset.isEmpty() && settings.getBoolean(KEY_AUDIO_SHOW_BOOkMARK_BUTTONS, true)) {
            binding.audioForwardBookmark.setVisible()
            binding.audioRewindBookmark.setVisible()
        } else {
            binding.audioForwardBookmark.setGone()
            binding.audioRewindBookmark.setGone()
        }
        if (!::bookmarkListDelegate.isInitialized || !bookmarkListDelegate.visible) {
            if (settings.getBoolean(KEY_AUDIO_SHOW_BOOKMARK_MARKERS, true))
                bookmarkModel.service?.let { service ->
                    binding.bookmarkMarkerContainer.setVisible()
                    BookmarkListDelegate.showBookmarks(binding.bookmarkMarkerContainer, service, bookmarkModel.dataset.getList())
                }
            else binding.bookmarkMarkerContainer.clearMarkers()
            if (isShowingCover()) {
                binding.audioForward10.setVisible()
                binding.audioRewind10.setVisible()
            }
        } else {
            binding.audioForwardBookmark.setGone()
            binding.audioRewindBookmark.setGone()
            binding.audioForward10.setGone()
            binding.audioRewind10.setGone()
        }

        audioTrackTitleText = if (!chapter.isNullOrEmpty()) chapter else playlistModel.title.orEmpty()
        audioTrackSubtitleText = if (!chapter.isNullOrEmpty()) TextUtils.separatedString(playlistModel.title, playlistModel.artist) else TextUtils.separatedString(playlistModel.artist, playlistModel.album)
        audioTrackDetailText = playlistModel.service?.trackInfo().orEmpty()
        binding.songTrackInfo?.visibility = if (Settings.showAudioTrackInfo) View.VISIBLE else View.GONE

        updateAudioSeekHudState()
        updateBackground()

    }

    private fun updatePlayPause() {
        val ctx = context ?: return
        val playing = playlistModel.playing
        val text = ctx.getString(if (playing) R.string.pause else R.string.play)

        updateAudioPlayPause(playing, text)

        playlistPlaying = playing
    }

    private var wasShuffling = false
    private fun updateShuffleMode() {
        val ctx = context ?: return
        binding.shuffle.visibility = if (playlistModel.canShuffle) View.VISIBLE else View.INVISIBLE
        val shuffling = playlistModel.shuffling
        val icon = if (shuffling) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_audio
        val text = ctx.getString(if (shuffling) R.string.shuffle_on else R.string.shuffle)
        if (wasShuffling == shuffling && audioShuffleState.contentDescription == text) return
        audioShuffleState = AudioTransportState(icon, text)
        wasShuffling = shuffling
    }

    private var previousRepeatType = -1
    private fun updateRepeatMode() {
        val ctx = context ?: return
        val repeatType = playlistModel.repeatType
        val icon: Int
        val text: String
        when (repeatType) {
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                icon = R.drawable.ic_repeat_one_audio
                text = ctx.getString(R.string.repeat_single)
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                icon = R.drawable.ic_repeat_all_audio
                text = ctx.getString(R.string.repeat_all)
            }
            else -> {
                icon = R.drawable.ic_repeat_audio
                text = ctx.getString(R.string.repeat_none)
            }
        }
        if (previousRepeatType == repeatType && audioRepeatState.contentDescription == text) return
        audioRepeatState = AudioTransportState(icon, text)
        previousRepeatType = repeatType
    }

    /**
     * Updates the text views in the player with the current progress
     * It includes the time, the length and the progress pill text and content description
     *
     * @param progress the progress to be displayed
     */
    private fun updateProgress(progress: PlaybackProgress) {
        if (playlistModel.currentMediaPosition == -1) return
        audioTimelineLengthText = if (showRemainingTime) Tools.millisToString(progress.time - progress.length) else progress.lengthText
        binding.timeline.max = progress.length.toInt()
        binding.progressBar.max = progress.length.toInt()

        if (!previewingSeek) {
            val displayTime = progress.timeText
            audioHeaderTimeText = if (showRemainingTime) Tools.millisToString(progress.time - progress.length) else displayTime
            audioTimelineTimeText = displayTime
            if (!isDragging) binding.timeline.progress = progress.time.toInt()
            binding.progressBar.progress = progress.time.toInt()
        }

        lifecycleScope.launchWhenStarted {
            val text:Pair<String, String> = withContext(Dispatchers.Default) {
                val medias = playlistModel.medias ?: return@withContext Pair("", "")
                withContext(Dispatchers.Main) { if (!shouldHidePlayProgress()) binding.audioPlayProgress.setVisible() else binding.audioPlayProgress.setGone() }
                if (playlistModel.currentMediaPosition == -1) return@withContext Pair("", "")
                val elapsedTracksTime = playlistModel.previousTotalTime ?: return@withContext Pair("", "")
                val progressTime = elapsedTracksTime + progress.time
                val totalTime = playlistModel.getTotalTime()
                val progressTimeText = Tools.millisToString(
                        if (showRemainingTime && totalTime > 0) totalTime - progressTime else progressTime,
                        false,
                        true,
                        false
                )
                val totalTimeText = Tools.millisToString(totalTime, false, false, false)
                val totalTimeDescription = TalkbackUtil.millisToString(requireActivity(), totalTime)
                val progressTimeDescription =  TalkbackUtil.millisToString(requireActivity(), if (showRemainingTime && totalTime > 0) totalTime - progressTime else progressTime)
                val currentProgressText = if (progressTimeText.isNullOrEmpty()) "0:00" else progressTimeText

                val isRtlLocale = LocaleUtil.isRtl()
                val size = if (playlistModel.service?.playlistManager?.stopAfter != -1 ) (playlistModel.service?.playlistManager?.stopAfter ?: 0) + 1 else medias.size
                val textTrack = getString(R.string.track_index, "${playlistModel.currentMediaPosition + 1} / $size".let {
                    if (isRtlLocale) it.markBidi(true) else it
                })
                val textTrackDescription = getString(R.string.talkback_track_index, "${playlistModel.currentMediaPosition + 1}", "$size")

                val textProgress = if (audioPlayProgressMode) {
                    val endsAt = System.currentTimeMillis() + totalTime - progressTime
                    if ((lastEndsAt - endsAt).absoluteValue > 1) lastEndsAt = endsAt
                    getString(
                            R.string.audio_queue_progress_finished,
                            getTimeInstance(java.text.DateFormat.MEDIUM).format(lastEndsAt).let {
                                if (isRtlLocale) it.markBidi(true) else it
                            }
                    )
                } else
                    if (showRemainingTime && totalTime > 0) getString(
                            R.string.audio_queue_progress_remaining,
                            currentProgressText
                    )
                    else getString(
                            R.string.audio_queue_progress,
                            if (totalTimeText.isNullOrEmpty()) currentProgressText else "$currentProgressText / $totalTimeText".let {
                                if (isRtlLocale) it.markBidi(true) else it
                            }
                    )
                val textDescription = if (audioPlayProgressMode) {
                    val endsAt = System.currentTimeMillis() + totalTime - progressTime
                    if ((lastEndsAt - endsAt).absoluteValue > 1) lastEndsAt = endsAt
                    getString(
                            R.string.audio_queue_progress_finished,
                            getTimeInstance(java.text.DateFormat.MEDIUM).format(lastEndsAt).let {
                                if (isRtlLocale) it.markBidi(true) else it
                            }
                    )
                } else
                    if (showRemainingTime && totalTime > 0) getString(
                            R.string.audio_queue_progress_remaining,
                            progressTimeDescription
                    )
                    else getString(
                            R.string.audio_queue_progress,
                            if (totalTimeText.isNullOrEmpty()) progressTimeDescription else getString(R.string.talkback_out_of, progressTimeDescription, totalTimeDescription)
                    )

                val finalTextTrack = if (isRtlLocale && !textTrack.hasRtl()) textTrack.markBidi(true) else textTrack
                val finalTextProgress = if (isRtlLocale && !textProgress.hasRtl()) textProgress.markBidi(true) else textProgress
                Pair("$finalTextTrack  ${TextUtils.SEPARATOR}  $finalTextProgress", "$textTrackDescription. $textDescription")
            }
            audioQueueProgressPillState = VLCAudioQueueProgressPillState(
                    text = text.first,
                    contentDescription = text.second
            )
            binding.audioPlayProgress.contentDescription = text.second
        }
    }

    private fun shouldHidePlayProgress() = abRepeatControlsActive || areBookmarksVisible() || playlistModel.medias?.size ?: 0 < 2

    fun onTimeLabelClick(@Suppress("UNUSED_PARAMETER") view: View) {
        toggleRemainingTimeMode()
    }

    fun onJumpBack(@Suppress("UNUSED_PARAMETER") view: View) {
        playlistModel.jump(forward = false, long = false, requireActivity())
    }

    fun onJumpBackLong(@Suppress("UNUSED_PARAMETER") view: View):Boolean {
        playlistModel.jump(forward = false, long = true, requireActivity())
        return true
    }

    fun onJumpForward(@Suppress("UNUSED_PARAMETER") view: View) {
        playlistModel.jump(forward = true, long = false, requireActivity())
    }

    fun onJumpForwardLong(@Suppress("UNUSED_PARAMETER") view: View):Boolean {
        playlistModel.jump(forward = true, long = true, requireActivity())
        return true
    }

    fun onPreviousBookmark(@Suppress("UNUSED_PARAMETER") view: View) {
        val bookmark = if (LocaleUtil.isRtl()) bookmarkModel.findNext() else bookmarkModel.findPrevious()
        bookmark?.let {
            bookmarkModel.service?.setTime(it.time)
        }
    }

    fun onNextBookmark(@Suppress("UNUSED_PARAMETER") view: View) {
        val bookmark = if (LocaleUtil.isRtl()) bookmarkModel.findPrevious() else bookmarkModel.findNext()
        bookmark?.let {
            bookmarkModel.service?.setTime(it.time)
        }
    }

    fun onPlayPauseClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        if (playlistModel.service?.isPausable == false) {
            UiTools.snackerConfirm(requireActivity(), getString(R.string.stop_unpaubale), true) {
                playlistModel.stop()
            }
            return
        }
        playlistModel.togglePlayPause()
    }

    fun onStopClick(@Suppress("UNUSED_PARAMETER") view: View?): Boolean {
        playlistModel.stop()
        activity.closeMiniPlayer()
        return true
    }

    fun onNextClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        if (!playlistModel.next()) UiTools.snacker(requireActivity(), R.string.lastsong, true)
    }

    fun onPreviousClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        if (!playlistModel.previous()) UiTools.snacker(requireActivity(),  R.string.firstsong)
    }

    fun onRepeatClick(@Suppress("UNUSED_PARAMETER") view: View) {
        playlistModel.repeatType = when (playlistModel.repeatType) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ALL
            PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ONE
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        }
        updateRepeatMode()
    }

    fun onPlaylistSwitchClick(@Suppress("UNUSED_PARAMETER") view: View) {
        switchShowCover()
        settings.putSingle(KEY_AUDIO_PLAYER_SHOW_COVER, isShowingCover())
    }

    fun onShuffleClick(@Suppress("UNUSED_PARAMETER") view: View) {
        playlistModel.shuffle()
        updateShuffleMode()
    }

    fun onResumeToVideoClick() {
        playlistModel.currentMediaWrapper?.let {
            if (PlaybackService.hasRenderer()) VideoPlayerActivity.startOpened(requireActivity(),
                    it.uri, playlistModel.currentMediaPosition)
            else if (hasMedia()) {
                it.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { playlistModel.switchToVideo() }
            }
        }
    }

    fun showAdvancedOptions(@Suppress("UNUSED_PARAMETER") v: View?) {
        if (!isVisible) return
        if (!this::optionsDelegate.isInitialized) {
            val service = playlistModel.service ?: return
            val activity = activity as? AppCompatActivity ?: return
            optionsDelegate = PlayerOptionsDelegate(activity, service)
            optionsDelegate.setBookmarkClickedListener {
                lifecycleScope.launch { if (!activity.showPinIfNeeded()) showBookmarks() }
            }
        }
        optionsDelegate.show()
    }

    /**
     * Show bookmark and initialize the delegate if needed
     */
    fun showBookmarks() {
        val service = playlistModel.service ?: return
        if (!this::bookmarkListDelegate.isInitialized) {
            bookmarkListDelegate = BookmarkListDelegate(requireActivity(), service, bookmarkModel, false)
            bookmarkListDelegate.visibilityListener = {
                binding.audioPlayProgress.visibility = if (shouldHidePlayProgress()) View.GONE else View.VISIBLE
                lifecycleScope.launch {
                    doUpdate()
                }
            }
            bookmarkListDelegate.seekListener = { forward, long ->
                playlistModel.jump(forward , long, requireActivity())
            }
            bookmarkListDelegate.markerContainer = binding.bookmarkMarkerContainer
        }
        bookmarkListDelegate.show()
        bookmarkListDelegate.setProgressHeight(binding.time.y)
    }

    fun onSearchClick(@Suppress("UNUSED_PARAMETER") v: View) {
        if (isShowingCover()) onPlaylistSwitchClick(binding.playlistSwitch)
        manageSearchVisibilities(true)
        binding.playlistSearchText.requestSearchFocus()
        handler.postDelayed(hideSearchRunnable, SEARCH_TIMEOUT_MILLIS)
    }

    fun onABRepeatStopClick(@Suppress("UNUSED_PARAMETER") v: View) {
        playlistModel.service?.playlistManager?.resetABRepeatValues(playlistModel.service?.playlistManager?.getCurrentMedia())
        playlistModel.service?.playlistManager?.clearABRepeat()
    }

    fun onABRepeatResetClick(@Suppress("UNUSED_PARAMETER") v: View) {
        playlistModel.service?.playlistManager?.resetABRepeatValues(playlistModel.service?.playlistManager?.getCurrentMedia())
    }

    fun backPressed(): Boolean {
        if (this::optionsDelegate.isInitialized && optionsDelegate.isShowing()) {
            optionsDelegate.hide()
            return true
        }
        if (areBookmarksVisible()) {
            bookmarkListDelegate.hide()
            return true
        }
        return clearSearch()
    }

    fun areBookmarksVisible() = ::bookmarkListDelegate.isInitialized && bookmarkListDelegate.visible

    fun clearSearch(): Boolean {
        if (this::playlistModel.isInitialized) playlistModel.filter(null)
        return hideSearchField()
    }

    private fun hideSearchField(): Boolean {
        if (binding.playlistSearchText.visibility != View.VISIBLE) return false
        binding.playlistSearchText.clearSearchText()
        UiTools.setKeyboardVisibility(binding.playlistSearchText, false)
        manageSearchVisibilities(false)
        return true
    }

    private fun onSearchQueryChanged(query: String) {
        val length = query.length
        if (length > 0) {
            playlistModel.filter(query)
            handler.removeCallbacks(hideSearchRunnable)
        } else {
            playlistModel.filter(null)
            hideSearchField()
        }
    }

    private inner class LongSeekListener(var forward: Boolean) : View.OnTouchListener {
        var length = -1L

        var possibleSeek = 0
        var vibrated = false

        @RequiresPermission(Manifest.permission.VIBRATE)
        var seekRunnable: Runnable = object : Runnable {
            override fun run() {
                if (!vibrated) {
                    AppContextProvider.appContext.getSystemService<Vibrator>()?.vibrate(80)
                    vibrated = true
                }

                if (forward) {
                    if (length <= 0 || possibleSeek < length) possibleSeek += 4000
                } else {
                    if (possibleSeek > 4000) possibleSeek -= 4000
                    else if (possibleSeek <= 4000) possibleSeek = 0
                }

                audioTimelineTimeText = Tools.millisToString(possibleSeek.toLong())
                binding.timeline.progress = possibleSeek
                binding.progressBar.progress = possibleSeek
                handler.postDelayed(this, 50)
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    possibleSeek = playlistModel.getTime().toInt()
                    previewingSeek = true
                    vibrated = false
                    length = playlistModel.length
                    handler.postDelayed(seekRunnable, 1000)
                    return false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(seekRunnable)
                    previewingSeek = false
                    if (event.eventTime - event.downTime >= 1000L) {
                        playlistModel.setTime(possibleSeek.toLong().coerceAtLeast(0L).coerceAtMost(playlistModel.length))
                        v.isPressed = false
                        return true
                    }
                    return false
                }
            }
            return false
        }
    }

    private fun showPlaylistTips() {
        activity.showTipViewIfNeeded(R.id.audio_playlist_tips, PREF_PLAYLIST_TIPS_SHOWN)
    }

    fun onStateChanged(newState: Int) {
        playerState = newState
        when (newState) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
                backPressed()
                onSlide(0f)
            }
            BottomSheetBehavior.STATE_EXPANDED -> {
                onSlide(1f)
                showPlaylistTips()
                requestPlaylistSelection(playlistModel.currentMediaPosition)
            }
        }
    }

    private var timelineListener = object : PlayerTimelineSeekBarView.Listener {

        override fun onStopTrackingTouch(progress: Int) {
            playlistModel.setTime(progress.toLong())
            isDragging = false
        }

        override fun onStartTrackingTouch() {
            isDragging = true
        }

        override fun onProgressChanged(progress: Int, fromUser: Boolean) {
            if (fromUser) {
                playlistModel.setTime(progress.toLong(), true)
                val displayTime = Tools.millisToString(progress.toLong())
                audioTimelineTimeText = displayTime
                audioHeaderTimeText = displayTime
                binding.timeline.forceAccessibilityUpdate()
            }
        }
    }

    private val headerMediaSwitcherListener = object : AudioMediaSwitcherListener {

        override fun onMediaSwitching() {}

        override fun onMediaSwitched(position: Int) {
            when (position) {
                AudioMediaSwitcherListener.PREVIOUS_MEDIA -> playlistModel.previous(true)
                AudioMediaSwitcherListener.NEXT_MEDIA -> playlistModel.next()
            }
        }

        override fun onTouchClick() {
            val activity = activity as AudioPlayerContainerActivity
            activity.slideUpOrDownAudioPlayer()
        }

        override fun onTouchLongClick() {
            val trackInfo = playlistModel.title ?: return

            if (playlistModel.videoTrackCount > 0) onResumeToVideoClick()
            else {
                requireActivity().copy("VLC - song name", trackInfo)
                UiTools.snacker(requireActivity(), R.string.track_info_copied_to_clipboard)
            }
        }

        override fun onTouchDown() {}

        override fun onTouchUp() {}

        override fun onTextClicked() { }

        override fun onChapterSwitching(next: Boolean) { }
    }

    private val coverMediaSwitcherListener = object : AudioMediaSwitcherListener by AudioMediaSwitcher.EmptySwitcherListener {


        override fun onMediaSwitching() {
            (activity as? AudioPlayerContainerActivity)?.playerBehavior?.lock(true)
        }

        override fun onMediaSwitched(position: Int) {
            when (position) {
                AudioMediaSwitcherListener.PREVIOUS_MEDIA -> playlistModel.previous(true)
                AudioMediaSwitcherListener.NEXT_MEDIA -> playlistModel.next()
            }
            (activity as? AudioPlayerContainerActivity)?.playerBehavior?.lock(false)
        }

        override fun onTextClicked() {
            Settings.getInstance(requireActivity()).putSingle(KEY_SHOW_TRACK_INFO, !Settings.showAudioTrackInfo)
            Settings.showAudioTrackInfo = !Settings.showAudioTrackInfo
            lifecycleScope.launch { doUpdate() }
        }

        override fun onChapterSwitching(next: Boolean) {
            playlistModel.service?.let { service ->
                service.currentMediaWrapper?.let { media ->
                    if (currentChapters?.first?.uri != media.uri) {
                        playlistModel.service?.getChapters(-1)?.let {
                            currentChapters = Pair(media, it.toList())
                        }
                    }
                }
            }

            currentChapters?.second?.let { chapters ->
                playlistModel.service?.let { service ->
                    val chapterIdx = playlistModel.service!!.chapterIdx
                    if (!next) {
                        val chapter = chapters[service.chapterIdx]
                        if (chapter.timeOffset + 5000 > service.getTime())
                            playlistModel.service!!.chapterIdx = chapterIdx.plus(-1).coerceAtLeast(0)
                        else
                            playlistModel.service!!.chapterIdx = chapterIdx
                    } else if (chapterIdx != chapters.size - 1)
                        playlistModel.service!!.chapterIdx = chapterIdx.plus(1).coerceAtMost(chapters.size - 1)
                }
            }
        }
    }

    fun refreshAbRepeatStep() {
        playlistModel.service?.manageAbRepeatStep(binding.abRepeatReset, binding.abRepeatStop, binding.abRepeatContainer) { markerText ->
            (binding.abRepeatContainer as AbRepeatControlsView).setMarkerText(markerText)
        }
    }

    fun update() {
        lifecycleScope.launch {
            if (activity.isStarted()) doUpdate()
        }
    }

    private val hideSearchRunnable by lazy(LazyThreadSafetyMode.NONE) {
        Runnable {
            hideSearchField()
            playlistModel.filter(null)
        }
    }
}
