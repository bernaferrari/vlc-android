/*****************************************************************************
 * OtgAccess.kt
 *****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.gui.helpers.hf

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.R

const val TAG = "OtgAccess"

const val OTG_SCHEME = "otg"

object OtgAccess {
    val otgRoot = MutableStateFlow<Uri?>(null)
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun ComponentActivity.requestOtgRoot() {
    lifecycleScope.launch(Dispatchers.Main.immediate) {
        val resultKey = "$TAG:${System.nanoTime()}"
        var launcher: ActivityResultLauncher<Intent>? = null
        lateinit var lifecycleObserver: LifecycleEventObserver
        fun unregister() {
            launcher?.unregister()
            launcher = null
            lifecycle.removeObserver(lifecycleObserver)
        }
        lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) unregister()
        }
        lifecycle.addObserver(lifecycleObserver)
        launcher = activityResultRegistry.register(resultKey, ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { OtgAccess.otgRoot.value = it }
            unregister()
        }
        AlertDialog.Builder(this@requestOtgRoot)
            .setTitle(resources.getString(R.string.allow_otg))
            .setMessage(resources.getString(R.string.allow_otg_description))
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    launcher?.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                } catch (_: ActivityNotFoundException) {
                    unregister()
                }
            }
            .setOnCancelListener {
                unregister()
            }
            .show()
    }
}

@WorkerThread
fun getDocumentFiles(context: Context, path: String) : List<MediaWrapper>? {
    val rootUri = OtgAccess.otgRoot.value ?: return null
    var documentFile = DocumentFile.fromTreeUri(context, rootUri)

    val parts = path.substringAfterLast(':').split("/".toRegex()).dropLastWhile { it.isEmpty() }
    for (part in parts) {
        if (part == "") continue
        documentFile = documentFile?.findFile(part)
    }

    if (documentFile == null) {
        Log.w(TAG, "Failed to find file")
        return null
    }

    // we have the end point DocumentFile, list the files inside it and return
    val list = mutableListOf<MediaWrapper>()
    for (file in documentFile.listFiles()) {
        if (file.exists() && file.canRead()) {
            if (file.name?.startsWith(".") == true) continue
            val mw = MLServiceLocator.getAbstractMediaWrapper(file.uri).apply {
                type = when {
                    file.isDirectory -> MediaWrapper.TYPE_DIR
                    file.type?.startsWith("video") == true -> MediaWrapper.TYPE_VIDEO
                    file.type?.startsWith("audio") == true -> MediaWrapper.TYPE_AUDIO
                    else -> type
                }
                title = file.name
            }
            list.add(mw)
        }
    }
    return list
}
