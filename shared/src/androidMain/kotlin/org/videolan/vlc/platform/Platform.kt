package org.videolan.vlc.platform

import android.os.Build

actual object PlatformInfoProvider {
    actual val current: PlatformInfo = PlatformInfo(
        platform = Platform.ANDROID,
        osVersion = "${Build.VERSION.SDK_INT}",
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
        isTv = false, // Set at runtime via UI mode check
        isTablet = false // Set at runtime via configuration
    )
}

actual object VlcLogger {
    actual fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }

    actual fun i(tag: String, message: String) {
        android.util.Log.i(tag, message)
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) android.util.Log.w(tag, message, throwable)
        else android.util.Log.w(tag, message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) android.util.Log.e(tag, message, throwable)
        else android.util.Log.e(tag, message)
    }
}
