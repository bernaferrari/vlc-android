package org.videolan.vlc.gui.dialogs

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
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCFeatureWarningDialogContent
import org.videolan.vlc.util.FeatureFlag
import org.videolan.vlc.util.isTalkbackIsEnabled

/**
 * Compose-hosted optional feature warning bottom sheet.
 */
fun FragmentActivity.showFeatureFlagWarningComposeDialog(
    featureFlag: FeatureFlag,
    onAccepted: () -> Unit
) {
    FeatureWarningComposeDialog(
        activity = this,
        title = getString(featureFlag.title),
        genericWarning = getString(R.string.optional_features_warning),
        detailWarning = featureFlag.warning?.let(::getString),
        isDpadAllowed = true,
        onAccepted = onAccepted
    ).show()
}

/**
 * Compose-hosted touch-only warning bottom sheet.
 */
fun FragmentActivity.showFeatureTouchOnlyWarningComposeDialog(onAccepted: () -> Unit) {
    FeatureWarningComposeDialog(
        activity = this,
        title = getString(R.string.touch_only),
        genericWarning = getString(R.string.touch_only_description),
        detailWarning = null,
        isDpadAllowed = false,
        onAccepted = onAccepted
    ).show()
}

private class FeatureWarningComposeDialog(
    private val activity: FragmentActivity,
    private val title: String,
    private val genericWarning: String,
    private val detailWarning: String?,
    private val isDpadAllowed: Boolean,
    private val onAccepted: () -> Unit
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
                VLCFeatureWarningDialogContent(
                    title = title,
                    genericWarning = genericWarning,
                    detailWarning = detailWarning,
                    swipeText = activity.getString(
                        if (isDpadAllowed && AndroidDevices.isTv) R.string.swipe_unlock_no_touch
                        else R.string.swipe_unlock
                    ),
                    isDpadAllowed = isDpadAllowed,
                    onSwipeStart = { dialog.setCancelable(false) },
                    onSwipeStop = { dialog.setCancelable(true) },
                    onUnlock = ::unlock,
                    warningIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified
                        )
                    },
                    unlockIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_swipe_unlock),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Unspecified
                        )
                    }
                )
            }
        }
        dialog.setContentView(rootView!!)
    }

    private fun unlock() {
        dialog.dismiss()
        onAccepted()
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
