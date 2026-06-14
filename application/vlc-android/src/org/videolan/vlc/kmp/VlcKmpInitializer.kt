package org.videolan.vlc.kmp

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.videolan.medialibrary.interfaces.Medialibrary
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
 * Call [initialize] once during Application.onCreate() after the medialibrary
 * and PlaylistManager are available.
 *
 *   - MediaRepository  → AndroidMediaRepository (JNI medialibrary)
 *   - PlaybackService  → AndroidPlaybackService (PlaylistManager + libVLC)
 *   - Preferences      → VlcPreferences (DataStore-backed)
 */
object VlcKmpInitializer {

    private var initialized = false

    fun initialize(
        context: Context,
        medialibrary: Medialibrary,
        playlistManager: PlaylistManager
    ) {
        if (initialized) return
        initialized = true

        // Module capturing runtime objects created during app startup
        val androidAppModule = module {
            single { medialibrary }
            single { playlistManager }
            single<MediaRepository> { AndroidMediaRepository(get()) }
            single<PlaybackService> { AndroidPlaybackService(get()) }
        }

        startKoin {
            androidContext(context)
            modules(platformModule, sharedModule, androidAppModule)
        }

        // Wire the Koin instance into the cross-platform holder (for iOS/Swift API)
        VlcKoin.set(org.koin.core.context.GlobalContext.get())
    }
}
