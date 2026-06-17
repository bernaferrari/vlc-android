package org.videolan.vlc.platform

/**
 * Platform-agnostic application services abstraction.
 *
 * Replaces `android.content.Context` for shared code that needs access to
 * string resources, file system, or preferences without Android dependencies.
 *
 * On Android this is backed by `AppContextProvider.appContext`.
 * On iOS/JVM this is provided via expect/actual or dependency injection.
 */
interface PlatformServices {
    /**
     * Get a string resource by key name (e.g. "ok", "cancel").
     * Falls back to [key] itself if not found.
     */
    fun getString(key: String): String

    /**
     * Get a formatted string resource by key name with arguments.
     */
    fun getString(key: String, vararg args: Any): String

    /**
     * Application files directory (persistent storage).
     */
    val filesDir: String

    /**
     * Application cache directory (temporary storage).
     */
    val cacheDir: String

    /**
     * Application package name / bundle identifier.
     */
    val packageName: String

    companion object {
        /**
         * Global instance, set at platform startup.
         * On Android: `PlatformServices.instance = AndroidPlatformServices(appContext)`
         */
        lateinit var instance: PlatformServices
    }
}
