package org.videolan.vlc.kmp

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.vlc.PlaybackService as AndroidPlaybackHost
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.app.platformModule
import org.videolan.vlc.app.sharedModule
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.repository.MediaRepository

/**
 * Initializes the Koin DI graph on Android, wiring the shared KMP
 * architecture into the existing Android VLC engine.
 *
 * Call [initialize] once the medialibrary singleton is available (Application
 * startup or [Medialibrary.OnMedialibraryReadyListener]). PlaylistManager is
 * resolved lazily from the running [AndroidPlaybackHost] because it is only
 * constructed in that service's onCreate.
 *
 *   - MediaRepository  → AndroidMediaRepository (JNI medialibrary)
 *   - PlaybackService  → AndroidPlaybackService (PlaylistManager + libVLC)
 *   - Preferences      → VlcPreferences (DataStore-backed)
 */
object VlcKmpInitializer {

    @Volatile
    private var initialized = false

    /**
     * @param context application context
     * @param medialibrary initiated medialibrary instance
     * @param playlistManager optional explicit manager; when null, [AndroidPlaybackService]
     *   resolves the live manager from [AndroidPlaybackHost.instance]
     */
    @JvmOverloads
    fun initialize(
        context: Context,
        medialibrary: Medialibrary = Medialibrary.getInstance(),
        playlistManager: PlaylistManager? = AndroidPlaybackHost.instance?.playlistManager
    ) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true

            val appContext = context.applicationContext
            val managerProvider: () -> PlaylistManager? = {
                playlistManager ?: AndroidPlaybackHost.instance?.playlistManager
            }

            val androidAppModule = module {
                single { medialibrary }
                single<MediaRepository> { AndroidMediaRepository(get()) }
                single<PlaybackService> { AndroidPlaybackService(managerProvider) }
            }

            if (GlobalContext.getOrNull() == null) {
                startKoin {
                    androidContext(appContext)
                    modules(platformModule, sharedModule, androidAppModule)
                }
            } else {
                loadKoinModules(androidAppModule)
            }

            VlcKoin.set(GlobalContext.get())
        }
    }

    val isInitialized: Boolean get() = initialized
}
