package org.videolan.vlc.gui.dialogs

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCPreferenceChangeWarningDialogContent
import org.videolan.vlc.util.isTalkbackIsEnabled

/**
 * Compose-hosted preference change warning bottom sheet.
 */
fun ComponentActivity.showConfirmPreferenceChangeComposeDialog(
    preferenceKey: String,
    title: String,
    warning: String,
    onAccepted: (String) -> Unit
) {
    ConfirmPreferenceChangeComposeDialog(this, preferenceKey, title, warning, onAccepted).show()
}

private class ConfirmPreferenceChangeComposeDialog(
    private val activity: ComponentActivity,
    private val preferenceKey: String,
    private val title: String,
    private val warning: String,
    private val onAccepted: (String) -> Unit
) {
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private var rootView: ComposeView? = null

    fun show() {
        setupContent()
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCPreferenceChangeWarningDialogContent(
                    title = title,
                    message = warning,
                    okText = activity.getString(R.string.ok),
                    cancelText = activity.getString(R.string.cancel),
                    onConfirm = ::confirm,
                    onCancel = { dialog.dismiss() },
                    iconContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                )
            }
        }
        dialog.setContentView(rootView!!)
    }

    private fun confirm() {
        onAccepted(preferenceKey)
        dialog.dismiss()
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusable = false
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusableInTouchMode = false
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
}
