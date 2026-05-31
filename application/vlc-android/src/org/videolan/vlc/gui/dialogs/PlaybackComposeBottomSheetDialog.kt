package org.videolan.vlc.gui.dialogs

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.BaseInputConnection
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.dialogs.VLCBottomSheetDialogFragment.Companion.shouldInterceptRemote
import org.videolan.vlc.gui.video.VideoPlayerActivity.Companion.videoRemoteFlow
import org.videolan.vlc.util.EmptyPBSCallback

internal abstract class PlaybackComposeBottomSheetDialog(
    protected val activity: FragmentActivity,
    private val onDismiss: (() -> Unit)? = null,
    private val dismissOnServiceEnded: Boolean = true,
    private val dismissOnPlaybackEnded: Boolean = true,
    private val allowRemote: Boolean = true
) : PlaybackService.Callback by EmptyPBSCallback {
    private val dialog = if (Settings.showTvUi) {
        BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
    } else {
        BottomSheetDialog(activity)
    }
    private var rootView: ComposeView? = null
    private var serviceJob: Job? = null
    private var remoteJob: Job? = null

    protected var service: PlaybackService? = null
        private set

    fun show() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    Content()
                }
            }
        }
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(rootView!!)
        dialog.setOnShowListener { configureBottomSheet() }
        dialog.setOnDismissListener { cleanup() }
        dialog.show()
        startServiceTracking()
        if (allowRemote) startRemoteSupport()
    }

    protected fun dismiss() {
        dialog.dismiss()
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
        onDismiss?.invoke()
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
        }
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
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (AndroidDevices.isChromeBook) behavior.isDraggable = false
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
