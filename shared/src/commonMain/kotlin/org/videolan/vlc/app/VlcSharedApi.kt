package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.platform.PlatformInfoProvider
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.repository.MediaRepository

/**
 * Public API surface for iOS / Swift consumption.
 *
 * Resolves dependencies from [VlcKoin]. Call the platform initializer first
 * (`IosKoinBootstrap.start()` on iOS).
 */
class VlcSharedApi {

    fun platformName(): String = PlatformInfoProvider.current.platform.name

    fun platformInfo(): String =
        "VLC on ${PlatformInfoProvider.current.platform.name} " +
            "(${PlatformInfoProvider.current.osVersion}, ${PlatformInfoProvider.current.deviceModel})"

    fun appVersion(): String = "KMP v1.0"

    fun isInitialized(): Boolean = VlcKoin.isStarted

    suspend fun getMediaCount(type: MediaType = MediaType.ALL): Int {
        return try {
            VlcKoin.get().get<MediaRepository>().count(type)
        } catch (_: Exception) {
            0
        }
    }

    suspend fun listMediaTitles(type: MediaType = MediaType.ALL, limit: Int = 50): List<String> {
        return try {
            val repo = VlcKoin.get().get<MediaRepository>()
            repo.observeMedia(type).first().take(limit).map { it.displayTitle }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun listMedia(type: MediaType = MediaType.ALL, limit: Int = 100): List<MediaItem> {
        return try {
            VlcKoin.get().get<MediaRepository>().observeMedia(type).first().take(limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun replaceLibrary(items: List<MediaItem>) {
        try {
            val repo = VlcKoin.get().get<MediaRepository>()
            if (repo is IosMediaRepositoryMarker) {
                repo.replaceAllPublic(items)
            }
        } catch (_: Exception) {
        }
    }

    fun seedDemoLibrary() {
        replaceLibrary(
            listOf(
                MediaItem(
                    id = 1L,
                    title = "Demo Audio",
                    uri = "https://streams.videolan.org/streams/mp3/sample.mp3",
                    type = MediaType.AUDIO,
                    duration = 30_000L,
                    artist = "VideoLAN",
                ),
                MediaItem(
                    id = 2L,
                    title = "Demo Video",
                    uri = "https://streams.videolan.org/streams/mp4/Mr_MrsSmith-h264_aac.mp4",
                    type = MediaType.VIDEO,
                    duration = 60_000L,
                    width = 1280,
                    height = 720,
                ),
            )
        )
    }

    suspend fun playFirst(type: MediaType = MediaType.VIDEO): Boolean {
        return try {
            val items = listMedia(type, 50)
            val first = items.firstOrNull() ?: return false
            VlcKoin.get().get<PlaybackService>().play(first, items)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun pause() {
        try {
            VlcKoin.get().get<PlaybackService>().pause()
        } catch (_: Exception) {
        }
    }

    fun resume() {
        try {
            VlcKoin.get().get<PlaybackService>().resume()
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try {
            VlcKoin.get().get<PlaybackService>().stop()
        } catch (_: Exception) {
        }
    }

    fun playbackStateFlow(): Flow<PlaybackState> {
        return try {
            VlcKoin.get().get<PlaybackService>().state
        } catch (_: Exception) {
            flowOf(PlaybackState.Idle)
        }
    }

    fun booleanPreferenceFlow(key: String, default: Boolean): Flow<Boolean> {
        return try {
            VlcKoin.get().get<VlcPreferences>().getBooleanFlow(key, default)
        } catch (_: Exception) {
            flowOf(default)
        }
    }

    suspend fun setBooleanPreference(key: String, value: Boolean) {
        try {
            VlcKoin.get().get<VlcPreferences>().putBoolean(key, value)
        } catch (_: Exception) {
        }
    }
}

/**
 * Optional capability for platform repos that accept bulk library replacement (iOS).
 */
interface IosMediaRepositoryMarker {
    fun replaceAllPublic(items: List<MediaItem>)
}
