package org.videolan.vlc.gui.dialogs

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.vlc.compose.theme.VLCTheme

class ComposeMaterialDialogHandle internal constructor(
    private val parent: ViewGroup,
    private val composeView: ComposeView,
    private val visible: MutableState<Boolean>,
    private val onDismiss: (() -> Unit)? = null
) {
    val isShowing: Boolean
        get() = visible.value

    fun dismiss() {
        if (!visible.value) return
        visible.value = false
        parent.removeView(composeView)
        onDismiss?.invoke()
    }
}

class ComposeMaterialBottomSheetHandle internal constructor(
    private val parent: ViewGroup,
    private val composeView: ComposeView,
    private val visible: MutableState<Boolean>,
    private val onDismiss: (() -> Unit)? = null
) {
    val isShowing: Boolean
        get() = visible.value

    fun dismiss() {
        if (!visible.value) return
        visible.value = false
        parent.removeView(composeView)
        onDismiss?.invoke()
    }

    internal fun composeView(): ComposeView = composeView
}

internal class ComposeMaterialBottomSheetHost(private val activity: Activity) {
    private var contentView: View? = null
    private var handle: ComposeMaterialBottomSheetHandle? = null
    private var onShow: (() -> Unit)? = null
    private var onDismiss: (() -> Unit)? = null

    val isShowing: Boolean
        get() = handle?.isShowing == true

    val window: Window?
        get() = null

    fun setContentView(view: View) {
        contentView = view
    }

    fun setCancelable(cancelable: Boolean) = Unit

    fun setCanceledOnTouchOutside(cancel: Boolean) = Unit

    fun setOnDismissListener(listener: () -> Unit) {
        onDismiss = listener
    }

    fun setOnShowListener(listener: () -> Unit) {
        onShow = listener
    }

    fun <T : View> findViewById(id: Int): T? = null

    fun show() {
        if (isShowing) return
        val view = requireNotNull(contentView) { "Bottom sheet content must be set before show()" }
        handle = activity.showMaterialBottomSheet(
            onDismiss = {
                onDismiss?.invoke()
                handle = null
            }
        ) {
            AndroidView(factory = { view })
        }
        onShow?.invoke()
    }

    fun dismiss() {
        handle?.dismiss()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun Context.showMaterialDialog(
    onDismiss: (() -> Unit)? = null,
    content: @Composable (ComposeMaterialDialogHandle) -> Unit
): ComposeMaterialDialogHandle {
    val activity = requireNotNull(findActivity()) { "Material Compose dialogs require an Activity context" }
    val parent = activity.window.decorView as ViewGroup
    val visible = mutableStateOf(true)
    lateinit var handle: ComposeMaterialDialogHandle
    val composeView = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }
    handle = ComposeMaterialDialogHandle(parent, composeView, visible, onDismiss)
    composeView.setContent {
        VLCTheme {
            if (visible.value) content(handle)
        }
    }
    parent.addView(
        composeView,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    )
    return handle
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun Context.showMaterialBottomSheet(
    onDismiss: (() -> Unit)? = null,
    content: @Composable (ComposeMaterialBottomSheetHandle) -> Unit
): ComposeMaterialBottomSheetHandle {
    val activity = requireNotNull(findActivity()) { "Material Compose bottom sheets require an Activity context" }
    val parent = activity.window.decorView as ViewGroup
    val visible = mutableStateOf(true)
    lateinit var handle: ComposeMaterialBottomSheetHandle
    val composeView = ComposeView(activity).apply {
        isFocusable = true
        isFocusableInTouchMode = true
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }
    handle = ComposeMaterialBottomSheetHandle(parent, composeView, visible, onDismiss)
    composeView.setContent {
        VLCTheme {
            if (visible.value) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                LaunchedEffect(Unit) {
                    composeView.requestFocus()
                }
                ModalBottomSheet(
                    onDismissRequest = { handle.dismiss() },
                    sheetState = sheetState
                ) {
                    content(handle)
                }
            }
        }
    }
    parent.addView(
        composeView,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    )
    return handle
}

fun Context.showSimpleComposeDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {},
    cancelable: Boolean = true
): ComposeMaterialDialogHandle = showMaterialDialog { handle ->
    AlertDialog(
        onDismissRequest = {
            if (cancelable) handle.dismiss()
        },
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    handle.dismiss()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = dismissText?.let { text ->
            {
                TextButton(
                    onClick = {
                        onDismiss()
                        handle.dismiss()
                    }
                ) {
                    Text(text)
                }
            }
        }
    )
}

fun Activity.showSimpleTextInputComposeDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    initialValue: String = "",
    onConfirm: (String) -> Boolean
): ComposeMaterialDialogHandle = showMaterialDialog { handle ->
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = { handle.dismiss() },
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = message)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onConfirm(value.trim { it <= ' ' })) handle.dismiss()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = { handle.dismiss() }) {
                Text(dismissText)
            }
        }
    )
}

fun Context.showSimpleBitmapComposeDialog(
    title: String,
    message: String,
    bitmap: Bitmap,
    confirmText: String
) {
    showMaterialDialog { handle ->
        AlertDialog(
            onDismissRequest = { handle.dismiss() },
            title = { Text(text = title) },
            text = {
                Column {
                    Text(text = message)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .size(256.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { handle.dismiss() }) {
                    Text(confirmText)
                }
            }
        )
    }
}

fun Context.showSingleActionComposeDialog(
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit,
    onCancel: () -> Unit
) {
    showMaterialDialog { handle ->
        AlertDialog(
            onDismissRequest = {
                onCancel()
                handle.dismiss()
            },
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = {
                Button(
                    onClick = {
                        onAction()
                        handle.dismiss()
                    }
                ) {
                    Text(actionText)
                }
            }
        )
    }
}
