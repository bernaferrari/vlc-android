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
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.tools.NOTIFICATION_PERMISSION_ASKED
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCNotificationPermissionDialogContent
import org.videolan.vlc.gui.helpers.hf.NotificationDelegate.Companion.getNotificationPermission
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.isTalkbackIsEnabled

/**
 * Compose-hosted notification permission bottom sheet.
 */
private fun ComponentActivity.showNotificationPermissionComposeDialog() {
    NotificationPermissionComposeDialog(this).show()
}

private class NotificationPermissionComposeDialog(private val activity: ComponentActivity) {
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
                VLCNotificationPermissionDialogContent(
                    title = activity.getString(R.string.notification_permission),
                    explanation = activity.getString(R.string.notification_permission_explanation),
                    okText = activity.getString(R.string.ok),
                    onOk = { dialog.dismiss() },
                    iconContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_permission_notification),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Unspecified
                        )
                    }
                )
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            Settings.getInstance(activity).edit {
                putBoolean(NOTIFICATION_PERMISSION_ASKED, true)
            }
            activity.lifecycleScope.launch { activity.getNotificationPermission() }
        }
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

object NotificationPermissionManager {
    fun launchIfNeeded(activity: ComponentActivity): Boolean {
        if (!Permissions.canSendNotifications(activity) && !Settings.getInstance(activity).getBoolean(NOTIFICATION_PERMISSION_ASKED, false)) {
            activity.showNotificationPermissionComposeDialog()
            return true
        }
        return false
    }
}
