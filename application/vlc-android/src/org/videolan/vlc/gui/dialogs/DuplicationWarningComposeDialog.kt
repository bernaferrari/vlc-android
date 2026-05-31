package org.videolan.vlc.gui.dialogs

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCDuplicationWarningDialogContent
import org.videolan.vlc.util.isTalkbackIsEnabled

object DuplicationWarningResult {
    const val REQUEST_KEY = "REQUEST_KEY"
    const val OPTION_KEY = "option"

    const val NO_OPTION = -1
    const val ADD_ALL = 0
    const val ADD_NEW = 1
    const val CANCEL = 2
}

/**
 * Compose-hosted duplicate playlist warning bottom sheet.
 */
fun FragmentActivity.showDuplicationWarningComposeDialog(
    shouldShowThreeOptions: Boolean,
    playlistTitles: List<String>,
    duplicationMessages: List<String>
) {
    DuplicationWarningComposeDialog(this, shouldShowThreeOptions, playlistTitles, duplicationMessages).show()
}

private class DuplicationWarningComposeDialog(
    private val activity: FragmentActivity,
    private val shouldShowThreeOptions: Boolean,
    private val playlistTitles: List<String>,
    private val duplicationMessages: List<String>
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
                VLCDuplicationWarningDialogContent(
                    title = activity.getString(R.string.message_primary_default),
                    message = buildMessage(),
                    cancelText = activity.getString(R.string.cancel),
                    addText = activity.getString(R.string.add_button),
                    addAllText = activity.getString(R.string.add_all_button),
                    addNewOnlyText = activity.getString(R.string.add_new_only_button),
                    showThreeOptions = shouldShowThreeOptions,
                    onCancel = { publishResultAndDismiss(DuplicationWarningResult.CANCEL) },
                    onAddAll = { publishResultAndDismiss(DuplicationWarningResult.ADD_ALL) },
                    onAddNew = { publishResultAndDismiss(DuplicationWarningResult.ADD_NEW) }
                )
            }
        }
        dialog.setContentView(rootView!!)
    }

    private fun buildMessage(): AnnotatedString {
        return buildAnnotatedString {
            duplicationMessages.forEachIndexed { index, message ->
                val messageStart = length
                append(message)
                playlistTitles.getOrNull(index)?.let { playlistTitle ->
                    val searchTitle = "\"$playlistTitle\""
                    val titleStart = message.indexOf(searchTitle)
                    if (titleStart >= 0) {
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            messageStart + titleStart,
                            messageStart + titleStart + searchTitle.length
                        )
                    }
                }
                append('\n')
            }
        }
    }

    private fun publishResultAndDismiss(option: Int) {
        val bundle: Bundle = bundleOf(DuplicationWarningResult.OPTION_KEY to option)
        activity.supportFragmentManager.setFragmentResult(DuplicationWarningResult.REQUEST_KEY, bundle)
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
