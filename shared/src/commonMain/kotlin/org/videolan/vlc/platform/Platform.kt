package org.videolan.vlc.platform

/**
 * Platform identification.
 */
enum class Platform {
    ANDROID,
    IOS,
    WEB,
    JVM,
    UNKNOWN
}

/**
 * Information about the current runtime platform.
 */
data class PlatformInfo(
    val platform: Platform,
    val osVersion: String,
    val deviceModel: String,
    val isTv: Boolean = false,
    val isTablet: Boolean = false
)

/**
 * Product capabilities that differ by target, independent from the shared Compose surface.
 *
 * A capability being false is a promise: shared code must neither render its entry point nor
 * dispatch its bridge action. Runtime readiness (for example, a selected Android activity being
 * PiP-capable) remains the responsibility of the corresponding native bridge.
 */
data class VlcPlatformCapabilities(
    val nativePlayback: Boolean,
    val pictureInPicture: Boolean = false,
    val rendererSelection: Boolean = false,
    val networkBrowsing: Boolean = false,
    val remoteAccessServer: Boolean = false,
)

/** Target-owned capability declaration used by shared UI and controller guards. */
expect val platformCapabilities: VlcPlatformCapabilities

/**
 * Whether the OS requests that non-essential interface motion be removed.
 *
 * This stays a tiny platform seam: the shared theme and Nav3 graph own the
 * actual behavior, while Android/iOS/browser hosts only report their native
 * accessibility preference.
 */
expect fun prefersReducedMotion(): Boolean

/**
 * Provider for platform-specific information.
 * Each target must supply an [actual] implementation.
 */
expect object PlatformInfoProvider {
    val current: PlatformInfo
}

/**
 * Logging abstraction — common declaration.
 * Platforms route to their native log subsystems (Logcat, os_log, SLF4J).
 */
expect object VlcLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
