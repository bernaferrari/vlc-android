package org.videolan.vlc.gui.dialogs

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCPreferenceChangeWarningDialogContent
import org.videolan.vlc.util.isTalkbackIsEnabled

const val CONFIRM_PREFERENCE_CHANGE_DIALOG_RESULT = "confirm_preference_change_dialog_result"
const val PREFERENCE_KEY = "preference_key"

/**
 * Compose-hosted preference change warning bottom sheet.
 */
fun FragmentActivity.showConfirmPreferenceChangeComposeDialog(
    preferenceKey: String,
    title: String,
    warning: String,
    onAccepted: (() -> Unit)? = null
) {
    ConfirmPreferenceChangeComposeDialog(this, preferenceKey, title, warning, onAccepted).show()
}

private class ConfirmPreferenceChangeComposeDialog(
    private val activity: FragmentActivity,
    private val preferenceKey: String,
    private val title: String,
    private val warning: String,
    private val onAccepted: (() -> Unit)?
) {
    private val dialog = if (Settings.showTvUi) {
        BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
    } else {
        BottomSheetDialog(activity)
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
                            modifier = Modifier.size(54.dp),
                            tint = Color.Unspecified
                        )
                    }
                )
            }
        }
        dialog.setContentView(rootView!!)
    }

    private fun confirm() {
        onAccepted?.invoke()
        val bundle: Bundle = bundleOf(PREFERENCE_KEY to preferenceKey)
        activity.supportFragmentManager.setFragmentResult(CONFIRM_PREFERENCE_CHANGE_DIALOG_RESULT, bundle)
        dialog.dismiss()
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (AndroidDevices.isChromeBook) behavior.isDraggable = false
        }
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
