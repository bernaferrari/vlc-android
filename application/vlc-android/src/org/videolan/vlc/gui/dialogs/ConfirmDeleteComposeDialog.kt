package org.videolan.vlc.gui.dialogs

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCDialogConfirmDelete
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.util.isTalkbackIsEnabled

const val CONFIRM_DELETE_DIALOG_RESULT_DEFAULT_VALUE = 0
const val CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER = 1

private var isConfirmDeleteComposeDialogShowing = false

fun ComponentActivity.showConfirmDeleteComposeDialog(
    medias: ArrayList<MediaLibraryItem> = arrayListOf(),
    title: String = "",
    description: String = "",
    buttonText: String = "",
    resultType: Int = CONFIRM_DELETE_DIALOG_RESULT_DEFAULT_VALUE,
    listener: (() -> Unit)? = null
) {
    if (isConfirmDeleteComposeDialogShowing) return
    isConfirmDeleteComposeDialogShowing = true
    lifecycleScope.launch {
        if (showPinIfNeeded()) {
            isConfirmDeleteComposeDialogShowing = false
            return@launch
        }
        ConfirmDeleteComposeDialog(
            activity = this@showConfirmDeleteComposeDialog,
            mediaList = medias,
            title = title,
            description = description,
            buttonText = buttonText,
            resultType = resultType,
            listener = listener,
            onDismissed = { isConfirmDeleteComposeDialogShowing = false }
        ).show()
    }
}

private class ConfirmDeleteComposeDialog(
    private val activity: ComponentActivity,
    private val mediaList: ArrayList<MediaLibraryItem>,
    private val title: String,
    private val description: String,
    private val buttonText: String,
    private val resultType: Int,
    private val listener: (() -> Unit)?,
    private val onDismissed: () -> Unit
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
                VLCTheme {
                    val colors = VLCThemeDefaults.colors
                    Surface(
                        color = colors.backgroundDefault,
                        contentColor = colors.fontDefault,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            VLCDialogConfirmDelete(
                                title = computeTitle(),
                                message = description.ifEmpty { activity.getString(R.string.confirm_delete_message) },
                                iconContent = {
                                    if (resultType == CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_warning_medium),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = Color.Unspecified
                                        )
                                    } else {
                                        DeleteAnimation()
                                    }
                                }
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            ) {
                                TextButton(onClick = { dialog.dismiss() }) {
                                    Text(activity.getString(R.string.cancel))
                                }
                                Button(
                                    onClick = ::confirm,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(buttonText.ifEmpty { activity.getString(R.string.delete_forever) })
                                }
                            }
                        }
                    }
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            rootView = null
            onDismissed()
        }
    }

    private fun computeTitle(): String {
        return when {
            mediaList.isEmpty() || resultType == CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER -> title.ifEmpty { activity.getString(R.string.confirm_delete) }
            mediaList.size > 1 && mediaList.filterIsInstance<MediaWrapper>().size == mediaList.size -> {
                val nbFiles = mediaList.filter { it is MediaWrapper && it.type != MediaWrapper.TYPE_DIR }.size
                val nbFolders = mediaList.filter { it is MediaWrapper && it.type == MediaWrapper.TYPE_DIR }.size
                when {
                    nbFiles == 0 -> activity.getString(R.string.confirm_delete_folders, nbFolders)
                    nbFolders == 0 -> activity.getString(R.string.confirm_delete_files, nbFiles)
                    else -> activity.getString(R.string.confirm_delete_folders_and_files, nbFolders, nbFiles)
                }
            }
            mediaList[0] is MediaWrapper -> {
                val media = mediaList[0] as MediaWrapper
                activity.getString(
                    if (media.type == MediaWrapper.TYPE_DIR) R.string.confirm_delete_folder else R.string.confirm_delete,
                    media.title
                )
            }
            mediaList[0] is Album -> activity.getString(R.string.confirm_delete_album, mediaList[0].title)
            mediaList[0] is Playlist -> activity.getString(R.string.confirm_delete_playlist, mediaList[0].title)
            else -> activity.getString(R.string.confirm_delete_several_media, mediaList.size)
        }
    }

    private fun confirm() {
        listener?.invoke()
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

    @androidx.compose.runtime.Composable
    private fun DeleteAnimation() {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    val anim = AnimatedVectorDrawableCompat.create(context, R.drawable.anim_delete)
                    setImageDrawable(anim)
                    anim?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable?) {
                            anim?.start()
                            super.onAnimationEnd(drawable)
                        }
                    })
                    anim?.start()
                }
            },
            modifier = Modifier.size(40.dp)
        )
    }
}
