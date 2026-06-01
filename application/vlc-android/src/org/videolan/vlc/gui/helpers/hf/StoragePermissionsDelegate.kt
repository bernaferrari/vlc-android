/*
 * *************************************************************************
 *  StoragePermissionsDelegate.kt
 * **************************************************************************
 *  Copyright © 2017-2018 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.helpers.hf

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.resources.AndroidDevices
import org.videolan.resources.EXTRA_FIRST_RUN
import org.videolan.resources.EXTRA_UPGRADE
import org.videolan.resources.SCHEME_PACKAGE
import org.videolan.resources.util.isExternalStorageManager
import org.videolan.resources.util.startMedialibrary
import org.videolan.tools.INITIAL_PERMISSION_ASKED
import org.videolan.tools.KEY_TV_ONBOARDING_DONE
import org.videolan.tools.Settings
import org.videolan.tools.isCallable
import org.videolan.tools.putSingle
import org.videolan.vlc.StartActivity
import org.videolan.vlc.gui.dialogs.showPermissionListComposeDialog
import org.videolan.vlc.gui.onboarding.ONBOARDING_DONE_KEY
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.Permissions.canReadStorage
import videolan.org.commontools.LiveEvent
import kotlin.coroutines.resume

class StoragePermissionsDelegate private constructor() {

    interface CustomActionController {
        fun onStorageAccessGranted()
    }

    companion object {
        const val TAG = "VLC/StorageHF"
        val storageAccessGranted = LiveEvent<Boolean>()
        private var permissionRationaleShown = false

        fun ComponentActivity.askStoragePermission(write: Boolean, cb: Runnable?) {
            val intent = intent
            val upgrade = intent?.getBooleanExtra(EXTRA_UPGRADE, false) == true
            val firstRun = upgrade && intent.getBooleanExtra(EXTRA_FIRST_RUN, false)
            val settings = Settings.getInstance(this)
            lifecycleScope.launch {
                val granted = getStoragePermission(write)
                val onboardingDone = withContext(Dispatchers.IO) {
                    if (AndroidDevices.isTv)
                        settings.getBoolean(KEY_TV_ONBOARDING_DONE, false)
                    else
                        settings.getBoolean(ONBOARDING_DONE_KEY, false)
                }
                if (granted && onboardingDone) {
                    (cb ?: getAction(this@askStoragePermission, firstRun, upgrade)).run()
                }
            }
        }

        suspend fun ComponentActivity.getStoragePermission(write: Boolean = false, withDialog:Boolean = true, onlyMedia:Boolean = false) : Boolean = withContext(Dispatchers.Main.immediate) {
            if (isFinishing) return@withContext false
            Settings.getInstance(this@getStoragePermission).putSingle(INITIAL_PERMISSION_ASKED, true)
            when {
                !AndroidUtil.isMarshMallowOrLater -> true
                shouldRequestReadOrAllAccess(onlyMedia) -> requestStorageAccess(write = false, withDialog = withDialog, onlyMedia = onlyMedia)
                write && !Permissions.canWriteStorage(this@getStoragePermission) -> requestStorageAccess(write = true, withDialog = withDialog, onlyMedia = onlyMedia)
                else -> true
            }
        }

        private fun ComponentActivity.shouldRequestReadOrAllAccess(onlyMedia: Boolean) = if (onlyMedia) {
            !canReadStorage(this)
        } else {
            !canReadStorage(this) || !Permissions.hasAllAccess(this)
        }

        private suspend fun ComponentActivity.requestStorageAccess(write: Boolean, withDialog: Boolean, onlyMedia: Boolean) : Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !onlyMedia && !write) {
                val uri = Uri.fromParts(SCHEME_PACKAGE, packageName, null)
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                if (intent.isCallable(this)) {
                    if (withDialog && this !is StartActivity) {
                        showPermissionListComposeDialog()
                        return false
                    }
                    return requestAllFilesAccess(intent)
                }
            }

            val permission = if (write) Manifest.permission.WRITE_EXTERNAL_STORAGE else Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                if (!write) storageAccessGranted.value = true
                return true
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission) && !permissionRationaleShown) {
                Permissions.showStoragePermissionDialog(this, false)
                permissionRationaleShown = true
                return false
            }
            return requestRuntimeStoragePermission(permission, write)
        }

        private suspend fun ComponentActivity.requestRuntimeStoragePermission(permission: String, write: Boolean) : Boolean = suspendCancellableCoroutine { continuation ->
            val resultKey = "$TAG:${System.nanoTime()}"
            var requestPermissionLauncher: ActivityResultLauncher<String>? = null

            fun unregister() {
                requestPermissionLauncher?.unregister()
                requestPermissionLauncher = null
            }

            requestPermissionLauncher = activityResultRegistry.register(resultKey, ActivityResultContracts.RequestPermission()) { isGranted ->
                val delay = System.currentTimeMillis() - Permissions.timeAsked
                if (delay < 300) {
                    Permissions.showAppSettingsPage(this)
                    if (continuation.isActive) continuation.resume(false)
                    unregister()
                    return@register
                }
                val granted = if (write) {
                    isGranted
                } else {
                    isGranted || isExternalStorageManager()
                }
                if (!write) storageAccessGranted.value = granted
                if (continuation.isActive) continuation.resume(granted)
                unregister()
            }

            continuation.invokeOnCancellation { unregister() }
            try {
                Permissions.timeAsked = System.currentTimeMillis()
                requestPermissionLauncher?.launch(permission)
            } catch (_: RuntimeException) {
                unregister()
                if (continuation.isActive) continuation.resume(false)
            }
        }

        private suspend fun ComponentActivity.requestAllFilesAccess(intent: Intent) : Boolean = suspendCancellableCoroutine { continuation ->
            val resultKey = "$TAG:all:${System.nanoTime()}"
            var settingsLauncher: ActivityResultLauncher<Intent>? = null

            fun unregister() {
                settingsLauncher?.unregister()
                settingsLauncher = null
            }

            settingsLauncher = activityResultRegistry.register(resultKey, ActivityResultContracts.StartActivityForResult()) {
                val granted = Permissions.hasAllAccess(this)
                storageAccessGranted.value = granted
                if (continuation.isActive) continuation.resume(granted)
                unregister()
            }

            continuation.invokeOnCancellation { unregister() }
            try {
                settingsLauncher?.launch(intent)
            } catch (_: RuntimeException) {
                unregister()
                if (continuation.isActive) continuation.resume(false)
            }
        }

        private fun getAction(activity: ComponentActivity, firstRun: Boolean, upgrade: Boolean) = Runnable {
            if (activity is CustomActionController) activity.onStorageAccessGranted()
            else activity.startMedialibrary(firstRun, upgrade, true)
        }

        suspend fun ComponentActivity.getWritePermission(uri: Uri) = if (uri.path?.startsWith(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY) == true) {
            if (AndroidUtil.isOOrLater && !Permissions.canWriteStorage()) getStoragePermission(true)
            else withContext(Dispatchers.IO) { FileUtils.canWrite(uri) }
        } else getExtWritePermission(uri)

    }
}
