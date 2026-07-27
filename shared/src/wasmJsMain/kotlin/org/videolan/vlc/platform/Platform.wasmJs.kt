package org.videolan.vlc.platform

actual val platformCapabilities = VlcPlatformCapabilities(
    // Browser-native HTML media decoding backs the same shared playback contract.
    nativePlayback = true,
)

actual fun prefersReducedMotion(): Boolean = js(
    "globalThis.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches === true",
)

actual object PlatformInfoProvider {
    actual val current: PlatformInfo = PlatformInfo(
        platform = Platform.WEB,
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
