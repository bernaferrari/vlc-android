package org.videolan.vlc.gui.dialogs

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCRenameDialogContent
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.util.isTalkbackIsEnabled

/**
 * Compose-hosted rename bottom sheet.
 */
fun ComponentActivity.showRenameComposeDialog(
    media: MediaLibraryItem,
    isFile: Boolean = false,
    onRenamed: (MediaLibraryItem, String) -> Unit
) {
    if (showPinIfNeeded()) return
    RenameComposeDialog(this, media, isFile, onRenamed).show()
}

private class RenameComposeDialog(
    private val activity: ComponentActivity,
    private val media: MediaLibraryItem,
    private val renameFile: Boolean,
    private val onRenamed: (MediaLibraryItem, String) -> Unit
) {
    private val dialog = if (Settings.showTvUi) {
        BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
    } else {
        BottomSheetDialog(activity)
    }
    private val name = if (renameFile && media is MediaWrapper) media.fileName else media.title
    private var newName by mutableStateOf(initialNameFieldValue(name, renameFile))
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
                VLCRenameDialogContent(
                    title = activity.getString(R.string.rename),
                    mediaTitle = name,
                    newTitleHint = activity.getString(R.string.new_title),
                    okText = activity.getString(R.string.ok),
                    newName = newName,
                    onNewNameChange = { newName = it },
                    onConfirm = ::confirm
                )
            }
        }
        dialog.setContentView(rootView!!)
    }

    private fun confirm() {
        if (newName.text.isEmpty()) return
        onRenamed(media, newName.text)
        dialog.dismiss()
    }

    @Suppress("DEPRECATION")
    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        dialog.window?.setSoftInputMode(softInputMode)
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

    private fun initialNameFieldValue(name: String, renameFile: Boolean): TextFieldValue {
        val extensionIndex = name.indexOfLast { it == '.' }
        val selectionEnd = if (extensionIndex != -1 && renameFile) extensionIndex else name.length
        return TextFieldValue(name, selection = TextRange(0, selectionEnd))
    }
}
