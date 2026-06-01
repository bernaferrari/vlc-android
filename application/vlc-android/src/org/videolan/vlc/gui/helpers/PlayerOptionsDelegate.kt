package org.videolan.vlc.gui.helpers

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.ViewStubCompat
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.resources.VLCOptions
import org.videolan.tools.KEY_AOUT
import org.videolan.tools.Settings
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.gui.AudioPlayerContainerActivity
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.dialogs.showAudioControlsSettingsComposeDialog
import org.videolan.vlc.gui.dialogs.showEqualizerComposeDialog
import org.videolan.vlc.gui.dialogs.showJumpToTimeComposeDialog
import org.videolan.vlc.gui.dialogs.showPlaybackSpeedComposeDialog
import org.videolan.vlc.gui.dialogs.showSelectChapterComposeDialog
import org.videolan.vlc.gui.dialogs.showSleepTimerComposeDialog
import org.videolan.vlc.gui.dialogs.showVideoControlsSettingsComposeDialog
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.hf.PinCodeDelegate
import org.videolan.vlc.gui.helpers.hf.checkPIN
import org.videolan.vlc.gui.view.PlayerOptionsPanelView
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.media.PlayerController
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.util.getScreenHeight
import org.videolan.vlc.util.share

private const val ACTION_AUDIO_DELAY = 2
private const val ACTION_SPU_DELAY = 3

private const val ID_PLAY_AS_AUDIO = 0L
private const val ID_SLEEP = 1L
private const val ID_JUMP_TO = 2L
private const val ID_PLAY_AS_VIDEO = 3L
private const val ID_BOOKMARK = 4L
private const val ID_CHAPTER_TITLE = 5L
private const val ID_PLAYBACK_SPEED = 6L
private const val ID_EQUALIZER = 7L
private const val ID_SAVE_PLAYLIST = 8L
private const val ID_POPUP_VIDEO = 9L
private const val ID_REPEAT = 10L
private const val ID_SHUFFLE = 11L
private const val ID_PASSTHROUGH = 12L
private const val ID_ABREPEAT = 13L
private const val ID_LOCK_PLAYER = 14L
private const val ID_VIDEO_STATS = 15L
private const val ID_SHOW_VIDEO_TIPS = 16L
private const val ID_SHOW_AUDIO_TIPS = 17L
private const val ID_SHOW_PLAYLIST_TIPS = 18L
private const val ID_VIDEO_CONTROL_SETTING = 19L
private const val ID_AUDIO_CONTROL_SETTING = 20L
private const val ID_SAFE_MODE_LOCK = 21L
private const val ID_SAFE_MODE_UNLOCK = 22L
private const val ID_SHARE = 23L
@SuppressLint("ShowToast")
class PlayerOptionsDelegate(val activity: ComponentActivity, val service: PlaybackService, private val showABReapeat:Boolean = true)  {

    private lateinit var bookmarkClickedListener: () -> Unit
    private lateinit var rootView: PlayerOptionsPanelView
    var flags: Long = 0L
    private val toast by lazy(LazyThreadSafetyMode.NONE) { Toast.makeText(activity, "", Toast.LENGTH_SHORT) }

    private val primary = activity is VideoPlayerActivity && activity.displayManager.isPrimary
    private val isChromecast = activity is VideoPlayerActivity && activity.displayManager.isOnRenderer
    private val video = activity is VideoPlayerActivity
    private val res = activity.resources
    private val settings = Settings.getInstance(activity)

    fun setup() {
        if (!this::rootView.isInitialized || PlayerController.playbackState == PlaybackStateCompat.STATE_STOPPED) return
        val options = mutableListOf<PlayerOption>()
        if (video) options.add(PlayerOption(ID_LOCK_PLAYER, R.drawable.ic_lock_player, res.getString(R.string.lock)))
        options.add(PlayerOption(ID_SLEEP, R.drawable.ic_sleep, res.getString(R.string.sleep_title)))
        if (!isChromecast) options.add(PlayerOption(ID_PLAYBACK_SPEED, R.drawable.ic_speed, res.getString(R.string.playback_speed)))
        options.add(PlayerOption(ID_JUMP_TO, R.drawable.ic_jumpto, res.getString(R.string.jump_to_time)))
        options.add(PlayerOption(ID_EQUALIZER, R.drawable.ic_equalizer, res.getString(R.string.equalizer)))
        if (video) {
            if (primary && !Settings.showTvUi && service.audioTracksCount > 0)
                options.add(PlayerOption(ID_PLAY_AS_AUDIO, R.drawable.ic_playasaudio_on, res.getString(R.string.play_as_audio)))
            if (primary && AndroidDevices.pipAllowed && !AndroidDevices.isDex(activity))
                options.add(PlayerOption(ID_POPUP_VIDEO, R.drawable.ic_popup_dim, res.getString(R.string.ctx_pip_title)))
            if (primary)
                options.add(PlayerOption(ID_REPEAT, R.drawable.ic_repeat, res.getString(R.string.repeat_title)))
            if (service.canShuffle()) options.add(PlayerOption(ID_SHUFFLE, R.drawable.ic_player_shuffle, res.getString(R.string.shuffle_title)))
            options.add(PlayerOption(ID_VIDEO_STATS, R.drawable.ic_video_stats, res.getString(R.string.video_information)))
        } else {
            if (service.videoTracksCount > 0) options.add(PlayerOption(ID_PLAY_AS_VIDEO, R.drawable.ic_playasaudio_off, res.getString(R.string.play_as_video)))
        }
        val chaptersCount = service.getChapters(-1)?.size ?: 0
        if (chaptersCount > 1) options.add(PlayerOption(ID_CHAPTER_TITLE, R.drawable.ic_chapter, res.getString(R.string.go_to_chapter)))
        if (::bookmarkClickedListener.isInitialized) options.add(PlayerOption(ID_BOOKMARK, R.drawable.ic_bookmark, res.getString(R.string.bookmarks)))
        if (showABReapeat) options.add(PlayerOption(ID_ABREPEAT, R.drawable.ic_abrepeat, res.getString(R.string.ab_repeat)))
        options.add(PlayerOption(ID_SAVE_PLAYLIST, R.drawable.ic_addtoplaylist, res.getString(R.string.playlist_save)))
        if (service.playlistManager.player.canDoPassthrough() && settings.getString(KEY_AOUT, "0") != "2")
            options.add(PlayerOption(ID_PASSTHROUGH, R.drawable.ic_passthrough, res.getString(R.string.audio_digital_title)))

        if (video) {
            if (PinCodeDelegate.pinUnlocked.value == true) options.add(PlayerOption(ID_SAFE_MODE_LOCK, R.drawable.ic_pin_lock, res.getString(R.string.lock_with_pin)))
            if (Settings.safeMode && PinCodeDelegate.pinUnlocked.value == false) options.add(PlayerOption(ID_SAFE_MODE_UNLOCK, R.drawable.ic_pin_unlock, res.getString(R.string.unlock_with_pin)))
            options.add(PlayerOption(ID_VIDEO_CONTROL_SETTING, R.drawable.ic_video_controls, res.getString(R.string.control_setting)))
        } else if (!Settings.showTvUi) {
            options.add(PlayerOption(ID_SHARE, R.drawable.ic_share, res.getString(R.string.share_track_info)))
        }

        if (!Settings.showTvUi) {
            if (video) {
                options.add(PlayerOption(ID_SHOW_VIDEO_TIPS, R.drawable.ic_videotips, res.getString(R.string.tips_title)))
            } else {
                options.add(PlayerOption(ID_AUDIO_CONTROL_SETTING, R.drawable.ic_audio_controls, res.getString(R.string.control_setting)))
                options.add(PlayerOption(ID_SHOW_AUDIO_TIPS, R.drawable.ic_audiotips, res.getString(R.string.audio_player_tips)))
                options.add(PlayerOption(ID_SHOW_PLAYLIST_TIPS, R.drawable.ic_playlisttips, res.getString(R.string.playlist_tips)))
            }
        }
        rootView.setOptions(options)
        if (options.any { it.id == ID_REPEAT }) updateRepeatOption()
        if (options.any { it.id == ID_SHUFFLE }) updateShuffleOption()
    }

    fun show() {
        activity.findViewById<ViewStubCompat>(R.id.player_options_stub)?.let {
            rootView = it.inflate() as PlayerOptionsPanelView
            rootView.setOnOptionClickListener(::onClick)
            rootView.setOnDismissClickListener { hide() }
        }
        val windowInfoLayout = if (activity is VideoPlayerActivity) activity.windowLayoutInfo else if (activity is BaseActivity) activity.windowLayoutInfo else null
        val foldingFeature = windowInfoLayout?.displayFeatures?.firstOrNull() as? FoldingFeature
        if (foldingFeature?.isSeparating == true && foldingFeature.occlusionType == FoldingFeature.OcclusionType.FULL && foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) {
            val halfScreenSize = activity.getScreenHeight() - foldingFeature.bounds.bottom
            val lp = (rootView.layoutParams as ViewGroup.MarginLayoutParams)
            lp.height = halfScreenSize
            if (lp is FrameLayout.LayoutParams) lp.gravity = Gravity.BOTTOM
            rootView.layoutParams = lp
        } else {
             val lp = (rootView.layoutParams as ViewGroup.MarginLayoutParams)
            lp.height = FrameLayout.LayoutParams.MATCH_PARENT
            if (lp is FrameLayout.LayoutParams) lp.gravity = Gravity.BOTTOM
            rootView.layoutParams = lp
        }
        setup()
        rootView.visibility = View.VISIBLE
        if (Settings.showTvUi) {
            activity.lifecycleScope.launch {
                delay(100L)
                rootView.requestInitialFocus()
            }
        }
    }

    fun hide() {
        rootView.visibility = View.GONE
    }

    fun setBookmarkClickedListener(listener:()->Unit) {
        this.bookmarkClickedListener = listener
    }

    fun onClick(option: PlayerOption) {
        when (option.id) {
            ID_SLEEP -> {
                showPlaybackDialog(ID_SLEEP)
            }
            ID_PLAY_AS_AUDIO -> (activity as VideoPlayerActivity).switchToAudioMode(true)
            ID_PLAY_AS_VIDEO -> {
                when {
                    activity is PlayerOptionsDelegateCallback -> activity.onResumeToVideoClick()
                }
            }
            ID_POPUP_VIDEO -> {
                (activity as VideoPlayerActivity).switchToPopup()
                val startMain = Intent(Intent.ACTION_MAIN)
                startMain.addCategory(Intent.CATEGORY_HOME)
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(startMain)
                hide()
            }
            ID_REPEAT -> setRepeatMode()
            ID_SHUFFLE -> {
                service.shuffle()
                setShuffle()
            }
            ID_PASSTHROUGH -> togglePassthrough()
            ID_ABREPEAT -> {
                hide()
                service.playlistManager.toggleABRepeat()
            }
            ID_LOCK_PLAYER -> {
                hide()
                (activity as VideoPlayerActivity).toggleLock()
            }
            ID_VIDEO_STATS -> {
                hide()
                service.playlistManager.toggleStats()
            }
            ID_SHOW_VIDEO_TIPS -> {
                hide()
                (activity as VideoPlayerActivity).tipsDelegate.init()
            }
            ID_SHOW_AUDIO_TIPS -> {
                hide()
                val audioPlayerContainerActivity = activity as AudioPlayerContainerActivity
                audioPlayerContainerActivity.findViewById<ViewStubCompat>(R.id.audio_player_tips)?.let {
                    audioPlayerContainerActivity.tipsDelegate.init(it)
                }
            }
            ID_SHOW_PLAYLIST_TIPS -> {
                hide()
                val audioPlayerContainerActivity = activity as AudioPlayerContainerActivity
                audioPlayerContainerActivity.findViewById<ViewStubCompat>(R.id.audio_playlist_tips)?.let {
                    audioPlayerContainerActivity.playlistTipsDelegate.init(it)
                }
            }
            ID_BOOKMARK -> {
                hide()
                bookmarkClickedListener.invoke()
            }
            ID_VIDEO_CONTROL_SETTING -> {
                hide()
                activity.showVideoControlsSettingsComposeDialog()
            }
            ID_SHARE -> {
                hide()
                service.playlistManager.getCurrentMedia()?.let { media ->
                    val trackInfo = buildString {
                        var started = false
                        if (media.title.isNotBlank()) {
                            append(media.title)
                            started = true
                        }
                        if (media.albumName.isNotBlank()) {
                            if (started) append(" ${TextUtils.SEPARATOR} ")
                            started = true
                            append(media.albumName)
                        }
                        if (media.artistName.isNotBlank()) {
                            if (started) append(" ${TextUtils.SEPARATOR} ")
                            append(media.artistName)
                        }
                    }
                    activity.share("", activity.getString(R.string.share_track, trackInfo))
                }
            }
            ID_AUDIO_CONTROL_SETTING -> {
                hide()
                activity.showAudioControlsSettingsComposeDialog()
            }
            ID_SAFE_MODE_LOCK -> {
                hide()
                PinCodeDelegate.pinUnlocked.postValue(false)
                (activity as? VideoPlayerActivity)?.overlayDelegate?.showOverlay()
                UiTools.snacker(activity, R.string.safe_mode_enabled)
            }

            ID_SAFE_MODE_UNLOCK -> {
                hide()
                activity.lifecycleScope.launch { activity.checkPIN(true) }
            }
            else -> showPlaybackDialog(option.id)
        }
    }

    private fun showPlaybackDialog(id: Long) {
        when (id) {
            ID_PLAYBACK_SPEED -> {
                activity.showPlaybackSpeedComposeDialog {
                    (activity as? VideoPlayerActivity)?.overlayDelegate?.dimStatusBar(true)
                }
                hide()
                return
            }
            ID_JUMP_TO -> {
                activity.showJumpToTimeComposeDialog {
                    (activity as? VideoPlayerActivity)?.overlayDelegate?.dimStatusBar(true)
                }
                hide()
                return
            }
            ID_SLEEP -> {
                activity.showSleepTimerComposeDialog {
                    (activity as? VideoPlayerActivity)?.overlayDelegate?.dimStatusBar(true)
                }
                hide()
                return
            }
            ID_CHAPTER_TITLE -> {
                activity.showSelectChapterComposeDialog {
                    (activity as? VideoPlayerActivity)?.overlayDelegate?.dimStatusBar(true)
                }
                hide()
                return
            }
            ID_EQUALIZER -> {
                activity.showEqualizerComposeDialog(warnBeforeSettings = activity is VideoPlayerActivity) {
                    (activity as? VideoPlayerActivity)?.overlayDelegate?.dimStatusBar(true)
                }
                hide()
                return
            }
            ID_SAVE_PLAYLIST -> {
                activity.addToPlaylist(service.media)
                hide()
                return
            }
            else -> return
        }
    }

    private fun setRepeatMode() {
        when (service.repeatType) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> {
                service.repeatType = PlaybackStateCompat.REPEAT_MODE_ONE
                updateRepeatOption()
            }
            PlaybackStateCompat.REPEAT_MODE_ONE -> if (service.hasPlaylist()) {
                service.repeatType = PlaybackStateCompat.REPEAT_MODE_ALL
                updateRepeatOption()
            } else {
                service.repeatType = PlaybackStateCompat.REPEAT_MODE_NONE
                updateRepeatOption()
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                service.repeatType = PlaybackStateCompat.REPEAT_MODE_NONE
                updateRepeatOption()
            }
        }
    }

    private fun setShuffle() {
        updateShuffleOption()
    }

    private fun updateShuffleOption() {
        rootView.setOptionIcon(
            ID_SHUFFLE,
            if (service.isShuffling) R.drawable.ic_shuffle_on_48dp else R.drawable.ic_player_shuffle,
            rootView.context.getString(if (service.isShuffling) R.string.shuffle_on else R.string.shuffle)
        )
    }

    private fun updateRepeatOption() {
        rootView.setOptionIcon(
            ID_REPEAT,
            when (service.repeatType) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                PlaybackStateCompat.REPEAT_MODE_ALL -> R.drawable.ic_repeat_all
                else -> R.drawable.ic_repeat
            },
            rootView.context.getString(when (service.repeatType) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> R.string.repeat_single
                PlaybackStateCompat.REPEAT_MODE_ALL -> R.string.repeat_all
                else -> R.string.repeat_none
            })
        )
    }

    private fun togglePassthrough() {
        val enabled = !VLCOptions.isAudioDigitalOutputEnabled(settings)
        if (service.setAudioDigitalOutputEnabled(enabled)) {
            rootView.setOptionIcon(
                ID_PASSTHROUGH,
                if (enabled) R.drawable.ic_passthrough_on else UiTools.getResourceFromAttribute(activity, R.attr.ic_passthrough)
            )
            VLCOptions.setAudioDigitalOutputEnabled(settings, enabled)
            toast.setText(res.getString(if (enabled) R.string.audio_digital_output_enabled else R.string.audio_digital_output_disabled))
        } else
            toast.setText(R.string.audio_digital_failed)
        toast.show()
    }

    fun isShowing() = rootView.visibility == View.VISIBLE
}

interface PlayerOptionsDelegateCallback {
    fun onResumeToVideoClick()
}

data class PlayerOption(val id: Long, val icon: Int, val title: String, val contentDescription: String = title)
