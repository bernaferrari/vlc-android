package org.videolan.vlc.gui.helpers.hf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import org.videolan.vlc.gui.dialogs.ComposeMaterialDialogHandle
import org.videolan.vlc.gui.dialogs.showMaterialDialog
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
    var activeDialog: ComposeMaterialDialogHandle? = null

    fun unregister() {
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
            },
            onDismiss = {
                if (!waitingForPicker && !showingHelp) complete(false)
            }
        )
    }

    continuation.invokeOnCancellation { unregister() }
    launcher = activityResultRegistry.register(resultKey, ActivityResultContracts.StartActivityForResult()) { result ->
        complete(handleExtWriteResult(storage, result.resultCode, result.data))
    }
    showMainDialog()
}

private fun ComponentActivity.showSdWritePermissionDialog(
    onConfirm: () -> Unit,
    onHelp: () -> Unit,
    onDismiss: () -> Unit
): ComposeMaterialDialogHandle = showMaterialDialog(onDismiss = onDismiss) { handle ->
    AlertDialog(
        onDismissRequest = { handle.dismiss() },
        title = { Text(text = stringResource(R.string.sdcard_permission_dialog_title)) },
        text = { Text(text = stringResource(R.string.sdcard_permission_dialog_message)) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    handle.dismiss()
                }
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onHelp()
                    handle.dismiss()
                }
            ) {
                Text(text = stringResource(R.string.dialog_sd_wizard))
            }
        }
    )
}

private fun ComponentActivity.showSdWriteHelpDialog(onDismiss: () -> Unit) {
    showMaterialDialog(onDismiss = onDismiss) { handle ->
        AlertDialog(
            onDismissRequest = { handle.dismiss() },
            text = {
                Image(
                    painter = painterResource(R.drawable.img_tips_sdcard),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(
                        width = dimensionResource(R.dimen.dialog_sd_wisard_width),
                        height = dimensionResource(R.dimen.dialog_sd_wisard_height)
                    )
                )
            },
            confirmButton = {
                Button(onClick = { handle.dismiss() }) {
                    Text(text = stringResource(R.string.ok))
                }
            }
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
