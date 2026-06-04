package org.videolan.vlc.gui.dialogs

import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.BaseInputConnection
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.dialogs.PlaybackRemoteControl.shouldInterceptRemote
import org.videolan.vlc.gui.video.VideoPlayerActivity.Companion.videoRemoteFlow
import org.videolan.vlc.util.EmptyPBSCallback

internal abstract class PlaybackComposeSheetDialog(
    protected val activity: ComponentActivity,
    private val onDismiss: (() -> Unit)? = null,
    private val dismissOnServiceEnded: Boolean = true,
    private val dismissOnPlaybackEnded: Boolean = true,
    private val allowRemote: Boolean = true
) : PlaybackService.Callback by EmptyPBSCallback {
    private var rootView: ComposeView? = null
    private var bottomSheet: ComposeMaterialBottomSheetHandle? = null
    private var serviceJob: Job? = null
    private var remoteJob: Job? = null

    protected var service: PlaybackService? = null
        private set

    fun show() {
        bottomSheet = activity.showMaterialBottomSheet(onDismiss = ::cleanup) { handle ->
            rootView = handle.composeView()
            VLCTheme {
                Content()
            }
        }
        configureBottomSheet()
        startServiceTracking()
        if (allowRemote) startRemoteSupport()
    }

    protected fun dismiss() {
        bottomSheet?.dismiss()
    }

    override fun update() {
        val currentService = service ?: return
        if (currentService.playlistManager.hasCurrentMedia()) {
            onMediaChanged(currentService)
        } else if (dismissOnPlaybackEnded) {
            dismiss()
        }
    }

    protected open fun onServiceAvailable(service: PlaybackService) = Unit

    protected open fun onMediaChanged(service: PlaybackService) = Unit

    @Composable
    protected abstract fun Content()

    private fun startServiceTracking() {
        serviceJob = activity.lifecycleScope.launch {
            PlaybackService.serviceFlow.collect { onServiceChanged(it) }
        }
    }

    private fun onServiceChanged(newService: PlaybackService?) {
        if (newService === service) {
            if (newService == null && dismissOnServiceEnded) dismiss()
            return
        }
        service?.removeCallback(this)
        service = newService
        if (newService != null) {
            newService.addCallback(this)
            onServiceAvailable(newService)
        } else if (dismissOnServiceEnded) {
            dismiss()
        }
    }

    private fun startRemoteSupport() {
        shouldInterceptRemote.postValue(true)
        remoteJob = activity.lifecycleScope.launch {
            videoRemoteFlow.collect { action ->
                val keyCode = when (action) {
                    "up" -> KeyEvent.KEYCODE_DPAD_UP
                    "down" -> KeyEvent.KEYCODE_DPAD_DOWN
                    "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
                    "left" -> KeyEvent.KEYCODE_DPAD_LEFT
                    "center" -> KeyEvent.KEYCODE_DPAD_CENTER
                    "back" -> KeyEvent.KEYCODE_BACK
                    else -> null
                }
                keyCode?.let {
                    rootView?.simulateKeyPress(it)
                    videoRemoteFlow.emit(null)
                }
            }
        }
    }

    private fun cleanup() {
        if (allowRemote) shouldInterceptRemote.postValue(false)
        remoteJob?.cancel()
        remoteJob = null
        serviceJob?.cancel()
        serviceJob = null
        service?.removeCallback(this)
        service = null
        rootView = null
        bottomSheet = null
        onDismiss?.invoke()
    }

    private fun configureBottomSheet() {
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
}

private fun View.simulateKeyPress(key: Int) {
    val inputConnection = BaseInputConnection(this, true)
    val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, key)
    val upEvent = KeyEvent(KeyEvent.ACTION_UP, key)
    inputConnection.sendKeyEvent(downEvent)
    inputConnection.sendKeyEvent(upEvent)
}
