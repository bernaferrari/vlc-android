package org.videolan.vlc.gui.dialogs

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.videolan.medialibrary.interfaces.media.Bookmark
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCRenameDialogContent
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.util.isTalkbackIsEnabled

const val RENAME_DIALOG_MEDIA = "RENAME_DIALOG_MEDIA"
const val RENAME_DIALOG_FILE = "RENAME_DIALOG_FILE"
const val RENAME_DIALOG_NEW_NAME = "RENAME_DIALOG_NEW_NAME"
const val CONFIRM_RENAME_DIALOG_RESULT = "CONFIRM_RENAME_DIALOG_RESULT"
const val CONFIRM_BOOKMARK_RENAME_DIALOG_RESULT = "CONFIRM_BOOKMARK_RENAME_DIALOG_RESULT"
const val CONFIRM_PLAYLIST_RENAME_DIALOG_RESULT = "CONFIRM_PLAYLIST_RENAME_DIALOG_RESULT"

/**
 * Compose-hosted rename bottom sheet.
 */
fun FragmentActivity.showRenameComposeDialog(media: MediaLibraryItem, isFile: Boolean = false) {
    if (showPinIfNeeded()) return
    RenameComposeDialog(this, media, isFile).show()
}

private class RenameComposeDialog(
    private val activity: FragmentActivity,
    private val media: MediaLibraryItem,
    private val renameFile: Boolean
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
        val key = when (media) {
            is Bookmark -> CONFIRM_BOOKMARK_RENAME_DIALOG_RESULT
            is Playlist -> CONFIRM_PLAYLIST_RENAME_DIALOG_RESULT
            else -> CONFIRM_RENAME_DIALOG_RESULT
        }
        val bundle: Bundle = bundleOf(RENAME_DIALOG_MEDIA to media, RENAME_DIALOG_NEW_NAME to newName.text)
        activity.supportFragmentManager.setFragmentResult(key, bundle)
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
