package org.videolan.vlc.platform

/**
 * Platform identification.
 */
enum class Platform {
    ANDROID,
    IOS,
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
