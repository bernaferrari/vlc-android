package org.videolan.vlc.platform

actual val platformCapabilities = VlcPlatformCapabilities(
    // Wasm currently ships the shared catalog/player UI, not a browser decode bridge.
    nativePlayback = false,
)

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
