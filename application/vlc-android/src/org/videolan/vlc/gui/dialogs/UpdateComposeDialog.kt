package org.videolan.vlc.gui.dialogs

import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.tools.KEY_SHOW_UPDATE
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCUpdateDialogContent
import org.videolan.vlc.util.AutoUpdate
import org.videolan.vlc.util.isTalkbackIsEnabled
import java.text.DateFormat
import java.util.Date

/**
 * Compose-hosted nightly update bottom sheet.
 */
fun FragmentActivity.showUpdateComposeDialog(
    updateURL: String,
    updateDate: Date,
    newInstall: Boolean = false
) {
    UpdateComposeDialog(this, updateURL, updateDate, newInstall).show()
}

private class UpdateComposeDialog(
    private val activity: FragmentActivity,
    private val updateURL: String,
    private val updateDate: Date,
    private val newInstall: Boolean
) {
    private val settings = Settings.getInstance(activity)
    private val dialog = if (Settings.showTvUi) {
        BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
    } else {
        BottomSheetDialog(activity)
    }
    private var rootView: ComposeView? = null
    private var isDownloading by mutableStateOf(false)
    private var neverAskAgain by mutableStateOf(!settings.getBoolean(KEY_SHOW_UPDATE, true))

    fun show() {
        setupContent()
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCUpdateDialogContent(
                    title = if (newInstall) activity.getString(R.string.install_nightly_title) else activity.getString(R.string.update_title),
                    description = if (newInstall) {
                        activity.getString(R.string.install_text)
                    } else {
                        "${activity.getString(R.string.update_text)}\n\n${activity.getString(R.string.install_text)}"
                    },
                    nightlyVersion = activity.getString(
                        R.string.nightly_version,
                        DateFormat.getDateInstance(DateFormat.SHORT).format(updateDate),
                        getAbi()
                    ),
                    neverAskAgainText = activity.getString(R.string.never_ask_again),
                    neverAskAgain = neverAskAgain,
                    openInBrowserText = activity.getString(R.string.open_in_browser),
                    installText = activity.getString(R.string.install),
                    showInstall = BuildConfig.DEBUG,
                    isDownloading = isDownloading,
                    onNeverAskAgainChange = ::updateNeverAskAgain,
                    onOpenInBrowser = ::openInBrowser,
                    onInstall = ::downloadAndInstall,
                    iconContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_update),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                )
            }
        }
        dialog.setContentView(rootView!!)
    }

    private fun updateNeverAskAgain(checked: Boolean) {
        neverAskAgain = checked
        settings.putSingle(KEY_SHOW_UPDATE, !checked)
    }

    private fun openInBrowser() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, updateURL.toUri()))
    }

    private fun downloadAndInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivityForResult(
                Intent(ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData("package:${activity.packageName}".toUri()),
                1
            )
            return
        }
        activity.lifecycleScope.launch {
            AutoUpdate.downloadAndInstall(activity.application, updateURL) { loading ->
                isDownloading = loading
            }
        }
    }

    private fun getAbi(): String {
        val arch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS[0]
        } else {
            Build.CPU_ABI
        }
        return mapOf(
            "armeabi-v7a" to "armv7",
            "arm64-v8a" to "arm64",
            "x86" to "x86",
            "x86_64" to "x86_64"
        )[arch].orEmpty()
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
