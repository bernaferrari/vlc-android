package org.videolan.vlc.platform

actual object PlatformInfoProvider {
    actual val current: PlatformInfo = PlatformInfo(
        platform = Platform.UNKNOWN,
        osVersion = "WebAssembly",
        deviceModel = "Browser",
    )
}

actual object VlcLogger {
    actual fun d(tag: String, message: String) = Unit
    actual fun i(tag: String, message: String) = Unit
    actual fun w(tag: String, message: String, throwable: Throwable?) = Unit
    actual fun e(tag: String, message: String, throwable: Throwable?) = Unit
}
