package org.videolan.vlc.platform

actual val platformCapabilities = VlcPlatformCapabilities(
    // The iOS host links MobileVLCKit through Swift Package Manager.
    nativePlayback = true,
    rendererSelection = true,
    // Native MobileVLCKit discovery and folder parsing is bridged by Swift.
    networkBrowsing = true,
    // Network.framework powers authenticated local Wi-Fi transfer uploads.
    remoteAccessServer = true,
)

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
