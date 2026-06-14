package org.videolan.vlc.platform

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual object PlatformInfoProvider {
    actual val current: PlatformInfo = PlatformInfo(
        platform = Platform.IOS,
        osVersion = "iOS",
        deviceModel = "iOS Device"
    )
}

actual object VlcLogger {
    actual fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    actual fun i(tag: String, message: String) {
        println("I/$tag: $message")
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        println("W/$tag: $message ${throwable?.let { it.message } ?: ""}")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("E/$tag: $message ${throwable?.let { it.message } ?: ""}")
    }
}
