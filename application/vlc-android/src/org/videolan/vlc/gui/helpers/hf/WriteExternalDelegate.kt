package org.videolan.vlc.gui.helpers.hf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.videolan.resources.AndroidDevices
import org.videolan.tools.AppScope
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.VlcMigrationHelper
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.util.FileUtils
import kotlin.coroutines.resume

class WriteExternalDelegate private constructor() {
    companion object {
        internal const val TAG = "VLC/WriteExternal"

        fun askForExtWrite(activity: ComponentActivity, uri: Uri, cb: Runnable? = null) {
            AppScope.launch {
                if (activity.getExtWritePermission(uri)) cb?.run()
            }
        }

        fun needsWritePermission(uri: Uri) : Boolean {
            val path = uri.path ?: return false
            return VlcMigrationHelper.isLolliPopOrLater && ("file" == uri.scheme || uri.scheme == null)
                    && path.isNotEmpty() && path.startsWith('/')
                    && !path.startsWith(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY)
                    && FileUtils.findFile(uri)?.canWrite() != true
        }
    }
}

suspend fun ComponentActivity.getExtWritePermission(uri: Uri) : Boolean = withContext(Dispatchers.Main.immediate) {
    if (!WriteExternalDelegate.needsWritePermission(uri)) return@withContext true
    val storage = FileUtils.getMediaStorage(uri) ?: return@withContext false
    requestExtWritePermission(storage)
}

private suspend fun ComponentActivity.requestExtWritePermission(storage: String) : Boolean = suspendCancellableCoroutine { continuation ->
    val resultKey = "${WriteExternalDelegate.TAG}:${System.nanoTime()}"
    var launcher: ActivityResultLauncher<Intent>? = null
    var activeDialog: AppCompatDialog? = null

    fun unregister() {
        activeDialog?.setOnDismissListener(null)
        activeDialog?.dismiss()
        activeDialog = null
        launcher?.unregister()
        launcher = null
    }

    fun complete(granted: Boolean) {
        if (continuation.isActive) continuation.resume(granted)
        unregister()
    }

    fun showMainDialog() {
        var waitingForPicker = false
        var showingHelp = false
        activeDialog = showSdWritePermissionDialog(
            onConfirm = {
                waitingForPicker = true
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, storage.toUri())
                }
                try {
                    launcher?.launch(intent)
                } catch (_: RuntimeException) {
                    complete(false)
                }
            },
            onHelp = {
                showingHelp = true
                showSdWriteHelpDialog {
                    if (continuation.isActive) showMainDialog()
                }
            }
        ).apply {
            setOnCancelListener {
                complete(false)
            }
            setOnDismissListener {
                if (!waitingForPicker && !showingHelp) complete(false)
            }
        }
    }

    continuation.invokeOnCancellation { unregister() }
    launcher = activityResultRegistry.register(resultKey, ActivityResultContracts.StartActivityForResult()) { result ->
        complete(handleExtWriteResult(storage, result.resultCode, result.data))
    }
    showMainDialog()
}

private fun ComponentActivity.showSdWritePermissionDialog(
    onConfirm: () -> Unit,
    onHelp: () -> Unit
): AppCompatDialog {
    val dialog = AppCompatDialog(this)
    dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.setContentView(
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    val colors = VLCThemeDefaults.colors
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.sdcard_permission_dialog_title),
                                color = colors.fontDefault,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = stringResource(R.string.sdcard_permission_dialog_message),
                                color = colors.fontDefault,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        onHelp()
                                        dialog.dismiss()
                                    }
                                ) {
                                    Text(text = stringResource(R.string.dialog_sd_wizard))
                                }
                                Button(
                                    onClick = {
                                        onConfirm()
                                        dialog.dismiss()
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(text = stringResource(R.string.ok))
                                }
                            }
                        }
                    }
                }
            }
        }
    )
    dialog.show()
    return dialog
}

private fun ComponentActivity.showSdWriteHelpDialog(onDismiss: () -> Unit) {
    AppCompatDialog(this).apply {
        setContentView(createSdWriteHelpView())
        setOnDismissListener { onDismiss() }
        show()
    }
}

private fun ComponentActivity.createSdWriteHelpView() = ComposeView(this).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    setContent {
        Image(
            painter = painterResource(R.drawable.img_tips_sdcard),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(
                width = dimensionResource(R.dimen.dialog_sd_wisard_width),
                height = dimensionResource(R.dimen.dialog_sd_wisard_height)
            )
        )
    }
}

private fun ComponentActivity.handleExtWriteResult(storage: String, resultCode: Int, data: Intent?) : Boolean {
    if (resultCode != Activity.RESULT_OK) return false
    val treeUri = data?.data ?: return false
    return saveExtWritePermission(this, storage, treeUri)
}

private fun saveExtWritePermission(context: Context, storage: String, treeUri: Uri) : Boolean {
    Settings.getInstance(context).putSingle("tree_uri_$storage", treeUri.toString())
    val treeFile = DocumentFile.fromTreeUri(context, treeUri)
    val contentResolver = context.contentResolver

    for (uriPermission in contentResolver.persistedUriPermissions) {
        val file = DocumentFile.fromTreeUri(context, uriPermission.uri)
        if (treeFile?.name == file?.name) {
            contentResolver.releasePersistableUriPermission(uriPermission.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            return false
        }
    }

    contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    return true
}
