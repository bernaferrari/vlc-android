package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.platform.PlatformInfoProvider
import org.videolan.vlc.repository.MediaRepository

/**
 * Public API surface for iOS / Swift consumption.
 *
 * Resolves dependencies from [VlcKoin]. The app must call the platform
 * initializer (VlcKmpInitializer on Android, startKoin on iOS) before use.
 *
 * ```swift
 * let api = VlcSharedApi()
 * print(api.platformInfo())
 * ```
 */
class VlcSharedApi {

    fun platformName(): String = PlatformInfoProvider.current.platform.name

    fun platformInfo(): String =
        "VLC on ${PlatformInfoProvider.current.platform.name} " +
        "(${PlatformInfoProvider.current.osVersion}, ${PlatformInfoProvider.current.deviceModel})"

    fun appVersion(): String = "KMP v1.0"

    fun isInitialized(): Boolean = VlcKoin.isStarted

    /**
     * Returns the number of media items of the given type (suspend).
     */
    suspend fun getMediaCount(type: MediaType = MediaType.ALL): Int {
        return try {
            VlcKoin.get().get<MediaRepository>().count(type)
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Get preference value as a Flow for reactive UI.
     */
    fun booleanPreferenceFlow(key: String, default: Boolean): Flow<Boolean> {
        return try {
            VlcKoin.get().get<VlcPreferences>().getBooleanFlow(key, default)
        } catch (_: Exception) {
            flowOf(default)
        }
    }

    /**
     * Set a boolean preference (suspend).
     */
    suspend fun setBooleanPreference(key: String, value: Boolean) {
        try {
            VlcKoin.get().get<VlcPreferences>().putBoolean(key, value)
        } catch (_: Exception) {
            // Koin not started yet
        }
    }
}
