package org.videolan.vlc.gui.helpers.hf

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.videolan.tools.Settings
import org.videolan.vlc.gui.DialogActivity
import org.videolan.vlc.gui.PinCodeActivity
import org.videolan.vlc.gui.PinCodeReason
import org.videolan.vlc.gui.video.VideoPlayerActivity
import kotlin.coroutines.resume

object PinCodeDelegate {
    internal const val TAG = "VLC/PinCode"
    val pinUnlocked = MutableLiveData(false)
}

suspend fun ComponentActivity.checkPIN(unlock:Boolean = false) : Boolean = withContext(Dispatchers.Main.immediate) {
    if (this@checkPIN is VideoPlayerActivity) this@checkPIN.waitingForPin = true
    if (!Settings.safeMode) return@withContext true
    suspendCancellableCoroutine { continuation ->
        if (this@checkPIN is DialogActivity) this@checkPIN.preventFinish()
        val resultKey = "${PinCodeDelegate.TAG}:${System.nanoTime()}"
        var pinCodeResult: ActivityResultLauncher<Intent>? = null
        fun unregister() {
            pinCodeResult?.unregister()
            pinCodeResult = null
        }
        pinCodeResult = activityResultRegistry.register(resultKey, ActivityResultContracts.StartActivityForResult()) { result ->
            val granted = result.resultCode == Activity.RESULT_OK
            if (granted && unlock) PinCodeDelegate.pinUnlocked.postValue(true)
            if (continuation.isActive) continuation.resume(granted)
            unregister()
            (this@checkPIN as? DialogActivity)?.finish()
        }
        continuation.invokeOnCancellation { unregister() }
        try {
            pinCodeResult?.launch(PinCodeActivity.getIntent(this@checkPIN, PinCodeReason.UNLOCK))
        } catch (_: RuntimeException) {
            unregister()
            (this@checkPIN as? DialogActivity)?.finish()
            if (continuation.isActive) continuation.resume(false)
        }
    }
}
