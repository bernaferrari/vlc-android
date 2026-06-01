/*
 * ************************************************************************
 *  NotificationDelegate.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.helpers.hf

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

class NotificationDelegate private constructor() {

    companion object {
        const val TAG = "VLC/NotificationDelegate"
        private var notificationAccessGranted = false

        suspend fun ComponentActivity.getNotificationPermission() : Boolean = withContext(Dispatchers.Main.immediate) {
            if (isFinishing) return@withContext false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@withContext true
            if (notificationAccessGranted || ContextCompat.checkSelfPermission(this@getNotificationPermission, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationAccessGranted = true
                return@withContext true
            }
            suspendCancellableCoroutine { continuation ->
                val resultKey = "$TAG:${System.nanoTime()}"
                var requestPermissionLauncher: ActivityResultLauncher<String>? = null
                fun unregister() {
                    requestPermissionLauncher?.unregister()
                    requestPermissionLauncher = null
                }
                requestPermissionLauncher = activityResultRegistry.register(resultKey, ActivityResultContracts.RequestPermission()) { isGranted ->
                    notificationAccessGranted = isGranted
                    if (continuation.isActive) continuation.resume(isGranted)
                    unregister()
                }
                continuation.invokeOnCancellation { unregister() }
                try {
                    requestPermissionLauncher?.launch(POST_NOTIFICATIONS)
                } catch (_: RuntimeException) {
                    unregister()
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        }
    }
}
