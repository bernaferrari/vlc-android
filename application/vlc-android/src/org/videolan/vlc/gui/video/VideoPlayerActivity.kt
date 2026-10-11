/*
 * VideoPlayerActivity.kt
 *
 * Copyright © 2011-2026 VLC authors and VideoLAN
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
package org.videolan.vlc.gui.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.BaseContextWrappingDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.media.MediaWrapperImpl
import org.videolan.resources.AppContextProvider
import org.videolan.resources.EXIT_PLAYER
import org.videolan.resources.PLAY_EXTRA_FROM_START
import org.videolan.resources.PLAY_EXTRA_ITEM_LOCATION
import org.videolan.resources.PLAY_EXTRA_ITEM_TITLE
import org.videolan.resources.PLAY_EXTRA_OPENED_POSITION
import org.videolan.resources.PLAY_FROM_SERVICE
import org.videolan.resources.PLAY_FROM_VIDEOGRID
import org.videolan.resources.util.parcelable
import org.videolan.tools.InProcessEvents
import org.videolan.tools.KEY_VIDEO_MATCH_FRAME_RATE
import org.videolan.tools.Settings
import org.videolan.resources.buildPkgString
import org.videolan.tools.getContextWithLocale
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.compose.player.VlcPlayerScreen
import org.videolan.vlc.compose.theme.VLCAppTheme
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.gui.browser.EXTRA_MRL
import org.videolan.vlc.gui.browser.FilePickerActivity
import org.videolan.vlc.gui.browser.KEY_MEDIA
import org.videolan.vlc.gui.dialogs.PlaybackRemoteControl
import org.videolan.vlc.kmp.AndroidPlayerHostControls
import org.videolan.vlc.kmp.AndroidTvPlayerSurface
import org.videolan.vlc.kmp.ExternalVideoLauncher
import org.videolan.vlc.kmp.SharedPlayerLauncher
import org.videolan.vlc.kmp.SharedVideoSurfaceRegistry
import org.videolan.vlc.media.VideoRemoteControl
import org.videolan.vlc.repository.SlaveRepository
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.FrameRateManager
import org.videolan.vlc.util.Util
import org.videolan.vlc.util.isSchemeFile
import org.videolan.vlc.util.isSchemeNetwork
import org.videolan.vlc.viewmodel.PlayerViewModel

/**
 * Full-screen host of the shared Compose video player.
 *
 * Android TV shows every video here, with the ten-foot chrome of the shared player
 * ([VlcPlayerScreen] in TV mode). On phones video normally plays inside MainActivity's player
 * route; this activity only serves apps that start VLC for a result, so they get the playback
 * position back. The class name is public API: other apps address it explicitly with the extras
 * documented on [ExternalVideoLauncher], and receive [EXTRA_POSITION], [EXTRA_DURATION] and
 * [EXTRA_URI] in the result.
 *
 * It owns no playback logic: the shared view model drives PlaybackService, and the decoder view is
 * the same [SharedVideoSurfaceRegistry] island as on phones, so there is still exactly one vout.
 */
open class VideoPlayerActivity : AppCompatActivity() {

    private val tv by lazy(LazyThreadSafetyMode.NONE) { Settings.showTvUi }
    private lateinit var hostControls: AndroidPlayerHostControls
    private val viewModel: PlayerViewModel by lazy(LazyThreadSafetyMode.NONE) { VlcKoin.get().get<PlayerViewModel>() }
    private var service: PlaybackService? = null
    private var serviceReady by mutableStateOf(false)
    private var pendingLaunch: Intent? = null
    private var stopPlaybackOnFinish = false
    private var frameRateMatchedUri: String? = null
    private var subtitleCallback: ((String) -> Unit)? = null
    private var baseContextWrappingDelegate: AppCompatDelegate? = null

    private val subtitlePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = subtitleCallback
        subtitleCallback = null
        val mrl = result.data?.getStringExtra(EXTRA_MRL) ?: return@registerForActivityResult
        callback?.invoke(mrl)
        service?.currentMediaWrapper?.let { media ->
            SlaveRepository.getInstance(this).saveSlave(media.location, IMedia.Slave.Type.Subtitle, 2, mrl)
        }
    }

    override fun getApplicationContext(): Context =
        super.getApplicationContext().getContextWithLocale(AppContextProvider.locale)

    override fun getDelegate() = baseContextWrappingDelegate
        ?: BaseContextWrappingDelegate(super.getDelegate()).apply { baseContextWrappingDelegate = this }

    override fun createConfigurationContext(overrideConfiguration: Configuration) =
        super.createConfigurationContext(overrideConfiguration).getContextWithLocale(AppContextProvider.locale)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Util.checkCpuCompatibility(this)
        volumeControlStream = AudioManager.STREAM_MUSIC
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        hostControls = AndroidPlayerHostControls(this, tv = tv)
        hostControls.onVideoSessionChanged(true)
        // This window is the video host from now on: a playback request made while it is being
        // composed waits for its surface instead of launching another host.
        SharedVideoSurfaceRegistry.activateHost()
        if (savedInstanceState == null) pendingLaunch = intent

        setContent {
            PlayerTheme(tv) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    if (serviceReady) {
                        VlcPlayerScreen(
                            viewModel = viewModel,
                            onClose = ::exitOK,
                            modifier = Modifier.fillMaxSize(),
                            hostControls = hostControls,
                            tvMode = tv,
                            showSubtitleImport = true,
                            onImportSubtitle = ::pickSubtitles,
                            surface = AndroidTvPlayerSurface,
                        )
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Predictive back (target SDK 36) no longer delivers KEYCODE_BACK to the window.
                // Replay it into the player so Back still hides the chrome before leaving.
                if (!replayBackIntoPlayer()) exitOK()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlaybackService.serviceFlow.collect(::onServiceChanged)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                InProcessEvents.actions(PLAY_FROM_SERVICE, EXIT_PLAYER).collect { event ->
                    if (event.action == PLAY_FROM_SERVICE) onNewIntent(event) else exitOK()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.map { it.playing }.distinctUntilChanged().collect(::keepScreenOn)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state
                    .map { state -> state.uri.takeIf { state.playing && state.hasVideoOutput } }
                    .distinctUntilChanged()
                    .collect { uri -> if (uri != null) matchFrameRate(uri) }
            }
        }
        if (tv) lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Remote access sends the same commands as the physical remote: replay them as key
                // presses so they follow focus, hide the chrome on Back, and so on.
                VideoRemoteControl.actions.filterNotNull().collect { action ->
                    // An open playback dialog handles the remote itself.
                    if (PlaybackRemoteControl.shouldInterceptRemote.value == true) return@collect
                    VideoRemoteControl.actions.value = null
                    dispatchRemoteAction(action)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val current = service
        if (current != null) handleLaunch(current, intent) else pendingLaunch = intent
    }

    override fun onStart() {
        super.onStart()
        Medialibrary.getInstance().pauseBackgroundOperations()
        PlaybackService.start(this)
    }

    override fun onStop() {
        super.onStop()
        Medialibrary.getInstance().resumeBackgroundOperations()
        keepScreenOn(false)
    }

    override fun onDestroy() {
        if (isFinishing) {
            if (stopPlaybackOnFinish) service?.takeIf { it.hasMedia() }?.stop(video = true)
            hostControls.onVideoSessionChanged(false)
            SharedVideoSurfaceRegistry.deactivateHost()
        }
        viewModel.onCleared()
        super.onDestroy()
    }

    private fun onServiceChanged(service: PlaybackService?) {
        if (service == null && this.service != null && !isFinishing) {
            this.service = null
            serviceReady = false
            exit(RESULT_CONNECTION_FAILED)
            return
        }
        this.service = service
        serviceReady = service != null
        if (service != null) pendingLaunch?.let { launch ->
            pendingLaunch = null
            handleLaunch(service, launch)
        }
    }

    /**
     * A launch either names media to play (an explicit URI, or an index of the current queue) or
     * just asks to show what is playing; in that case the registry replays any pending request as
     * soon as this window's surface is attached.
     */
    private fun handleLaunch(service: PlaybackService, intent: Intent) {
        SharedPlayerLauncher.handleIntent(intent)
        val extras = intent.extras
        val openedPosition = extras?.getInt(PLAY_EXTRA_OPENED_POSITION, -1) ?: -1
        val location = extras?.parcelable<Uri>(PLAY_EXTRA_ITEM_LOCATION) ?: intent.data
        when {
            service.isValidIndex(openedPosition) -> {
                if (openedPosition != service.currentMediaPosition || !service.isPlaying) service.playIndex(openedPosition)
            }
            location != null -> ExternalVideoLauncher.load(applicationContext, intent)
        }
        // Consumed: a recreation must not start the media again.
        intent.removeExtra(PLAY_EXTRA_OPENED_POSITION)
        intent.removeExtra(PLAY_EXTRA_ITEM_LOCATION)
        intent.data = null
    }

    private fun pickSubtitles(onPicked: (String) -> Unit) {
        val uri = service?.currentMediaWrapper?.uri
        val folder = uri?.takeIf { it.scheme.isSchemeFile() || it.scheme.isSchemeNetwork() }
            ?.let { FileUtils.getParent(it.toString())?.toUri() }
            ?.let { MediaWrapperImpl(it) }
        subtitleCallback = onPicked
        subtitlePicker.launch(Intent(this, FilePickerActivity::class.java).putExtra(KEY_MEDIA, folder))
    }

    private fun keepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** "Match content frame rate": switch the display mode to the video's frame rate, once per item. */
    private fun matchFrameRate(uri: String) {
        if (uri == frameRateMatchedUri) return
        if (!Settings.getInstance(this).getBoolean(KEY_VIDEO_MATCH_FRAME_RATE, false)) return
        val service = service ?: return
        val surface = SharedVideoSurfaceRegistry.currentSurfaceView() ?: return
        frameRateMatchedUri = uri
        runCatching { FrameRateManager(this, service).matchFrameRate(surface, window) }
    }

    /**
     * Sends Back to the Compose content only (not the window, which would route an unhandled Back
     * to this callback again). Returns true when the player consumed it.
     */
    private fun replayBackIntoPlayer(): Boolean {
        val content = findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) ?: return false
        val now = SystemClock.uptimeMillis()
        val down = content.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0))
        content.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0))
        return down
    }

    private fun dispatchRemoteAction(action: String) {
        val keyCode = when (action) {
            "up" -> KeyEvent.KEYCODE_DPAD_UP
            "down" -> KeyEvent.KEYCODE_DPAD_DOWN
            "left" -> KeyEvent.KEYCODE_DPAD_LEFT
            "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
            "center" -> KeyEvent.KEYCODE_DPAD_CENTER
            "back" -> KeyEvent.KEYCODE_BACK
            "skip-next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "skip-previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "play", "pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            else -> return
        }
        val now = SystemClock.uptimeMillis()
        dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    /** Leaves the player, telling a calling app where playback stopped. */
    open fun exit(resultCode: Int) {
        if (isFinishing) return
        val result = Intent(ACTION_RESULT)
        service?.let { service ->
            service.currentMediaWrapper?.uri?.let { uri ->
                if (AndroidUtil.isNougatOrLater) result.putExtra(EXTRA_URI, uri.toString()) else result.data = uri
                result.putExtra(EXTRA_POSITION, service.getTime())
                result.putExtra(EXTRA_DURATION, service.length)
            }
        }
        setResult(resultCode, result)
        // Leaving the video player stops the video, as it always has: nothing keeps decoding unseen.
        stopPlaybackOnFinish = true
        finish()
    }

    fun exitOK() = exit(RESULT_OK)

    companion object {
        private val ACTION_RESULT = "player.result".buildPkgString()
        const val EXTRA_POSITION = "extra_position"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_URI = "extra_uri"
        /** Kept for callers; every launch is handled the same way now. */
        const val FROM_EXTERNAL = "from_external"
        private const val RESULT_CONNECTION_FAILED = Activity.RESULT_FIRST_USER + 1

        fun start(context: Context, uri: Uri) {
            start(context, uri, null, false, -1)
        }

        fun start(context: Context, uri: Uri, fromStart: Boolean) {
            start(context, uri, null, fromStart, -1)
        }

        fun start(context: Context, uri: Uri, title: String) {
            start(context, uri, title, false, -1)
        }

        private fun start(context: Context, uri: Uri, title: String?, fromStart: Boolean, openedPosition: Int) {
            context.startActivity(getIntent(PLAY_FROM_VIDEOGRID, context, uri, title, fromStart, openedPosition), Util.getFullScreenBundle())
        }

        fun getIntent(action: String, context: Context, uri: Uri, title: String?, fromStart: Boolean, openedPosition: Int): Intent {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.action = action
            intent.putExtra(PLAY_EXTRA_ITEM_LOCATION, uri)
            intent.putExtra(PLAY_EXTRA_ITEM_TITLE, title)
            intent.putExtra(PLAY_EXTRA_FROM_START, fromStart)
            if (openedPosition != -1 || context !is Activity) {
                if (openedPosition != -1) intent.putExtra(PLAY_EXTRA_OPENED_POSITION, openedPosition)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return intent
        }
    }
}

@androidx.compose.runtime.Composable
private fun PlayerTheme(tv: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
    if (tv) VLCTvTheme(content = content) else VLCAppTheme { VLCTheme(darkTheme = true, content = content) }
}
