package org.videolan.vlc.app

import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.tools.VlcPreferences

/**
 * Dependency injection container for the VLC shared module.
 *
 * Platforms create a concrete instance by supplying platform-specific
 * implementations of each repository and service. Shared code (ViewModels,
 * use cases, UI) reads dependencies from this container — never from platform
 * singletons — keeping the architecture clean and testable.
 */
class VlcAppContainer(
    val preferences: VlcPreferences,
    val mediaRepository: MediaRepository,
    val playlistRepository: PlaylistRepository,
    val historyRepository: HistoryRepository,
    val playbackService: PlaybackService,
)

/**
 * Builder for [VlcAppContainer] — platform code calls this at app launch.
 */
class VlcAppContainerBuilder {
    private var preferences: VlcPreferences? = null
    private var mediaRepository: MediaRepository? = null
    private var playlistRepository: PlaylistRepository? = null
    private var historyRepository: HistoryRepository? = null
    private var playbackService: PlaybackService? = null

    fun preferences(prefs: VlcPreferences) = apply { preferences = prefs }
    fun mediaRepository(repo: MediaRepository) = apply { mediaRepository = repo }
    fun playlistRepository(repo: PlaylistRepository) = apply { playlistRepository = repo }
    fun historyRepository(repo: HistoryRepository) = apply { historyRepository = repo }
    fun playbackService(service: PlaybackService) = apply { playbackService = service }

    fun build(): VlcAppContainer {
        return VlcAppContainer(
            preferences = preferences ?: error("preferences not set"),
            mediaRepository = mediaRepository ?: error("mediaRepository not set"),
            playlistRepository = playlistRepository ?: error("playlistRepository not set"),
            historyRepository = historyRepository ?: error("historyRepository not set"),
            playbackService = playbackService ?: error("playbackService not set"),
        )
    }
}

/**
 * Singleton holder for the app container.
 * Set once at app launch via [VlcAppContainerBuilder.build()].
 */
object VlcApp {
    lateinit var container: VlcAppContainer
        private set

    fun init(container: VlcAppContainer) {
        this.container = container
    }

    val isInitialized: Boolean get() = ::container.isInitialized
}
