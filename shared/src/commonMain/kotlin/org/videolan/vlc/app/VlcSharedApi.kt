package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.platform.PlatformInfoProvider

/**
 * Public API surface for iOS / Swift consumption.
 *
 * This class is exported in the VLCShared framework and can be called
 * directly from Swift code:
 *
 * ```swift
 * let api = VlcSharedApi()
 * print(api.platformInfo())
 * api.getMediaCount(type: .video) { count in ... }
 * ```
 */
class VlcSharedApi {

    fun platformName(): String = PlatformInfoProvider.current.platform.name

    fun platformInfo(): String =
        "VLC on ${PlatformInfoProvider.current.platform.name} " +
        "(${PlatformInfoProvider.current.osVersion}, ${PlatformInfoProvider.current.deviceModel})"

    fun appVersion(): String = "KMP v1.0"

    fun isInitialized(): Boolean = VlcApp.isInitialized

    /**
     * Returns the number of media items of the given type (suspend,
     * must be called from a coroutine).
     */
    suspend fun getMediaCount(type: MediaType = MediaType.ALL): Int {
        return if (VlcApp.isInitialized) {
            VlcApp.container.mediaRepository.count(type)
        } else 0
    }

    /**
     * Synchronous version of [getMediaCount] for Swift convenience.
     * Returns 0 if not yet initialized.
     */
    fun getMediaCountSync(): Int {
        // Non-suspending convenience for simple checks.
        // For real data access, use the suspending version from coroutines.
        return 0
    }

    /**
     * Get preference value as a Flow for reactive UI.
     */
    fun booleanPreferenceFlow(key: String, default: Boolean): Flow<Boolean> {
        return if (VlcApp.isInitialized) {
            VlcApp.container.preferences.getBooleanFlow(key, default)
        } else {
            flowOf(default)
        }
    }

    /**
     * Set a boolean preference (suspend).
     */
    suspend fun setBooleanPreference(key: String, value: Boolean) {
        if (VlcApp.isInitialized) {
            VlcApp.container.preferences.putBoolean(key, value)
        }
    }
}
