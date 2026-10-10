package org.videolan.vlc.kmp

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.util.Rational
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.videolan.tools.KEY_VIDEO_APP_SWITCH
import org.videolan.vlc.getSelectedVideoTrack
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.provider.Settings.System as SystemSettings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.content.getSystemService
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import org.videolan.resources.AndroidDevices
import org.videolan.tools.BRIGHTNESS_VALUE
import org.videolan.tools.ENABLE_BRIGHTNESS_GESTURE
import org.videolan.tools.ENABLE_DOUBLE_TAP_PLAY
import org.videolan.tools.ENABLE_DOUBLE_TAP_SEEK
import org.videolan.tools.ENABLE_FASTPLAY
import org.videolan.tools.ENABLE_SCALE_GESTURE
import org.videolan.tools.ENABLE_SWIPE_SEEK
import org.videolan.tools.ENABLE_VOLUME_GESTURE
import org.videolan.tools.KEY_AUDIO_BOOST
import org.videolan.tools.LAST_LOCK_ORIENTATION
import org.videolan.tools.LOCK_USE_SENSOR
import org.videolan.tools.SAVE_BRIGHTNESS
import org.videolan.tools.SCREENSHOT_MODE
import org.videolan.tools.SCREEN_ORIENTATION
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.compose.player.PlayerGestureSettings
import org.videolan.vlc.compose.player.PlayerHostControls
import org.videolan.vlc.compose.player.PlayerLevelControl
import org.videolan.vlc.compose.player.PlayerOrientationControl
import org.videolan.vlc.compose.player.PlayerRemoteCommand
import org.videolan.vlc.compose.player.PlayerScreenshotControl
import org.videolan.vlc.gui.helpers.BitmapUtil
import org.videolan.vlc.media.VideoRemoteControl
import org.videolan.vlc.util.Permissions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android effects behind the shared video player: window brightness, media volume with VLC's
 * audio boost, orientation lock, TextureView screenshots, gesture preferences and the remote
 * access commands. It also owns the per-session window state the old VideoPlayerActivity got
 * for free from being a separate window (orientation and brightness are restored on leave).
 */
internal class AndroidPlayerHostControls(private val activity: ComponentActivity) : PlayerHostControls {
    private val prefs = Settings.getInstance(activity)
    private val audioManager = activity.getSystemService<AudioManager>()

    private var videoSessionActive = false
    private var orientationBeforeVideo = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var locked = false
    /** Video track disabled by "play videos in background" while the app was stopped. */
    private var videoSentToBackground = false

    init {
        activity.addOnUserLeaveHintListener(::onUserLeave)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = onAppVisible()
            override fun onStop(owner: LifecycleOwner) = onAppHidden()
        })
    }

    override val gestures: PlayerGestureSettings
        get() = PlayerGestureSettings(
            volume = prefs.getBoolean(ENABLE_VOLUME_GESTURE, true),
            brightness = prefs.getBoolean(ENABLE_BRIGHTNESS_GESTURE, true),
            swipeSeek = prefs.getBoolean(ENABLE_SWIPE_SEEK, true),
            doubleTapSeek = prefs.getBoolean(ENABLE_DOUBLE_TAP_SEEK, true),
            doubleTapPlay = prefs.getBoolean(ENABLE_DOUBLE_TAP_PLAY, true),
            pinchZoom = prefs.getBoolean(ENABLE_SCALE_GESTURE, true),
            // Fast play stays opt-in, and unavailable while casting, exactly like the old player.
            fastPlay = prefs.getBoolean(ENABLE_FASTPLAY, false) && PlaybackService.renderer.value == null,
            fastPlayRate = Settings.fastplaySpeed,
            doubleTapSeekSeconds = Settings.videoDoubleTapJumpDelay,
            keySeekSeconds = Settings.videoJumpDelay,
        )

    override val brightness: PlayerLevelControl = object : PlayerLevelControl {
        override fun read(): Float {
            val current = activity.window.attributes.screenBrightness
            if (current >= 0f) return current
            val resolver = activity.contentResolver
            val automatic = SystemSettings.getInt(
                resolver,
                SystemSettings.SCREEN_BRIGHTNESS_MODE,
                SystemSettings.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
            ) == SystemSettings.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            // Automatic brightness has no readable level; start in the middle like VLC did.
            return if (automatic) 0.5f else SystemSettings.getInt(resolver, SystemSettings.SCREEN_BRIGHTNESS, 128) / 255f
        }

        override fun write(level: Float) {
            val value = level.coerceIn(0.01f, 1f)
            setWindowBrightness(value)
            if (prefs.getBoolean(SAVE_BRIGHTNESS, false)) prefs.putSingle(BRIGHTNESS_VALUE, value)
        }
    }

    override val volume: PlayerLevelControl? = audioManager?.let { manager ->
        object : PlayerLevelControl {
            private val streamMax get() = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

            override val max: Float
                get() = if (prefs.getBoolean(KEY_AUDIO_BOOST, true)) 2f else 1f

            override fun read(): Float {
                val playerVolume = PlaybackService.instance?.volume ?: 100
                if (playerVolume > 100) return playerVolume / 100f
                return manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / streamMax
            }

            override fun write(level: Float) {
                val service = PlaybackService.instance
                try {
                    if (level <= 1f) {
                        service?.setVolume(100)
                        val target = Math.round(level * streamMax)
                        if (target != manager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                            // No system UI: the shared pill is the feedback.
                            manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                        }
                    } else {
                        if (manager.getStreamVolume(AudioManager.STREAM_MUSIC) != streamMax) {
                            manager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMax, 0)
                        }
                        service?.setVolume(Math.round(level * 100f))
                    }
                } catch (error: SecurityException) {
                    // Do Not Disturb can forbid volume changes; keep the gesture harmless.
                    Log.w(TAG, "Volume change refused", error)
                }
            }
        }
    }

    override val orientation: PlayerOrientationControl = object : PlayerOrientationControl {
        override val isLocked: Boolean get() = locked

        override fun setLocked(locked: Boolean) {
            this@AndroidPlayerHostControls.locked = locked
            if (locked) {
                val current = currentSensorOrientation()
                prefs.putSingle(LAST_LOCK_ORIENTATION, current)
                requestOrientation(current)
            } else {
                requestOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
            }
        }
    }

    override val screenshot: PlayerScreenshotControl?
        get() = if (prefs.getString(SCREENSHOT_MODE, "0") in arrayOf("1", "3")) screenshotControl else null

    private val screenshotControl = PlayerScreenshotControl {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Permissions.canWriteStorage(activity)) {
            Permissions.askWriteStoragePermission(activity, false) {}
            return@PlayerScreenshotControl false
        }
        val bitmap = SharedVideoSurfaceRegistry.captureFrame() ?: return@PlayerScreenshotControl false
        withContext(Dispatchers.IO) {
            runCatching {
                val directory = AndroidDevices.MediaFolders.EXTERNAL_PUBLIC_SCREENSHOTS_URI_DIRECTORY.toFile()
                directory.mkdirs()
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val destination = File(directory, "vlc_$stamp.jpg")
                BitmapUtil.saveOnDisk(bitmap, destination.absolutePath, publish = true, context = activity)
            }.onFailure { Log.w(TAG, "Screenshot failed", it) }.getOrDefault(false)
        }
    }

    override val remoteCommands: Flow<PlayerRemoteCommand> =
        VideoRemoteControl.actions.filterNotNull().mapNotNull { action ->
            VideoRemoteControl.actions.value = null
            when (action) {
                "up" -> PlayerRemoteCommand.UP
                "down" -> PlayerRemoteCommand.DOWN
                "left" -> PlayerRemoteCommand.LEFT
                "right" -> PlayerRemoteCommand.RIGHT
                "center" -> PlayerRemoteCommand.CENTER
                "back" -> PlayerRemoteCommand.BACK
                "skip-next" -> PlayerRemoteCommand.NEXT
                "skip-previous" -> PlayerRemoteCommand.PREVIOUS
                "pip" -> PlayerRemoteCommand.PICTURE_IN_PICTURE
                "play", "pause" -> PlayerRemoteCommand.PLAY_PAUSE
                else -> null
            }
        }

    /**
     * KEY_VIDEO_APP_SWITCH "2": leaving the app with video playing enters Picture-in-Picture,
     * like the old player did from its own window.
     */
    private fun onUserLeave() {
        if (!videoSessionActive || appSwitchMode() != "2") return
        val service = PlaybackService.instance ?: return
        if (!service.isPlaying || PlaybackService.hasRenderer()) return
        if (!AndroidDevices.hasPiP || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val builder = PictureInPictureParams.Builder()
            service.playlistManager.player.mediaplayer.getSelectedVideoTrack()?.let { track ->
                val width = track.getWidth()
                val height = track.getHeight()
                if (width > 0 && height > 0 && width.toFloat() / height in 0.418410f..2.39f) {
                    builder.setAspectRatio(Rational(width, height))
                }
            }
            activity.enterPictureInPictureMode(builder.build())
        }.onFailure { Log.w(TAG, "Picture-in-Picture refused", it) }
    }

    /**
     * Leaving the app (not rotating, not PiP): "1" keeps the audio playing without decoding video,
     * the default pauses so nothing plays unseen.
     */
    private fun onAppHidden() {
        if (!videoSessionActive || activity.isChangingConfigurations) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode) return
        val service = PlaybackService.instance ?: return
        if (!service.isPlaying || PlaybackService.hasRenderer()) return
        when (appSwitchMode()) {
            "1" -> {
                service.playlistManager.setVideoTrackEnabled(false)
                videoSentToBackground = true
            }
            "2" -> Unit // PiP was requested on the leave hint; keep playing if it was refused.
            else -> service.pause()
        }
    }

    private fun onAppVisible() {
        if (!videoSentToBackground) return
        videoSentToBackground = false
        // Back in the player: bring the picture back on the same native player.
        if (videoSessionActive) PlaybackService.instance?.playlistManager?.setVideoTrackEnabled(true)
    }

    private fun appSwitchMode(): String = prefs.getString(KEY_VIDEO_APP_SWITCH, "0") ?: "0"

    /** Video became visible: apply the user's orientation preference and saved brightness. */
    fun onVideoSessionChanged(active: Boolean) {
        if (active == videoSessionActive) return
        videoSessionActive = active
        if (active) {
            orientationBeforeVideo = activity.requestedOrientation
            applyOrientationPreference()
            if (prefs.getBoolean(SAVE_BRIGHTNESS, false)) {
                prefs.getFloat(BRIGHTNESS_VALUE, -1f).takeIf { it >= 0f }?.let(::setWindowBrightness)
            }
        } else {
            locked = false
            requestOrientation(orientationBeforeVideo)
            // The library follows the system brightness again.
            setWindowBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    private fun applyOrientationPreference() {
        val sensor = prefs.getBoolean(LOCK_USE_SENSOR, true)
        val setting = prefs.getString(SCREEN_ORIENTATION, "99")?.toIntOrNull() ?: 99
        val orientation = when (setting) {
            99 -> null
            101 -> if (sensor) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            102 -> if (sensor) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            103 -> if (sensor) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            98 -> prefs.getInt(LAST_LOCK_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            else -> currentSensorOrientation()
        }
        locked = orientation != null
        if (orientation != null) {
            prefs.putSingle(LAST_LOCK_ORIENTATION, orientation)
            requestOrientation(orientation)
        }
    }

    private fun currentSensorOrientation(): Int =
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

    private fun requestOrientation(orientation: Int) {
        runCatching { activity.requestedOrientation = orientation }
            .onFailure { Log.w(TAG, "Orientation request refused", it) }
    }

    private fun setWindowBrightness(value: Float) {
        val attributes = activity.window.attributes
        if (attributes.screenBrightness == value) return
        attributes.screenBrightness = value
        activity.window.attributes = attributes
    }

    private companion object {
        const val TAG = "VLC/PlayerHost"
    }
}
