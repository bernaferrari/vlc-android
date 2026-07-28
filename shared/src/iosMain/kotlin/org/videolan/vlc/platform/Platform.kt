package org.videolan.vlc.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

actual val platformCapabilities: VlcPlatformCapabilities
    get() = VlcPlatformCapabilities(
        // The iOS host links VLCKit through Swift Package Manager.
        nativePlayback = true,
        // The public VLCKit PiP drawable is linked by the iOS host. Keeping
        // readiness in IosPipController prevents a call before VLCKit hands us
        // its window controller.
        pictureInPicture = IosPipController.isSupported,
        rendererSelection = true,
        // Native VLCKit discovery and folder parsing is bridged by Swift.
        networkBrowsing = true,
        // Network.framework powers authenticated local Wi-Fi transfer uploads.
        remoteAccessServer = true,
    )

actual fun prefersReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()

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
