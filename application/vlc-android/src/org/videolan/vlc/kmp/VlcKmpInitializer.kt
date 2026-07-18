package org.videolan.vlc.kmp

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.tools.AppScope
import org.videolan.tools.Settings
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.putSingle
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.PlaybackService as AndroidPlaybackHost
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.app.platformModule
import org.videolan.vlc.app.sharedModule
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.PlaylistRepository

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
                single<PlaylistRepository> { AndroidPlaylistRepository(get()) }
                single<HistoryRepository> { AndroidHistoryRepository(get()) }
                single<PlaybackService> { AndroidPlaybackService(managerProvider) }
                single<MediaSessionBridge> { AndroidMediaSessionBridge(appContext) }
                single<PipController> { AndroidPipController() }
                single<RendererBridge> { AndroidRendererBridge() }
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
            wireSettingsBridge(appContext)
        }
    }

    /**
     * Attach SharedPreferences → DataStore dual-write and seed DataStore once
     * from the legacy prefs so VlcSettings/VlcPreferences stay aligned.
     */
    private fun wireSettingsBridge(appContext: Context) {
        val koin = GlobalContext.getOrNull() ?: return
        val vlcPrefs = runCatching { koin.get<VlcPreferences>() }.getOrNull() ?: return
        Settings.attachDataStoreBridge(vlcPrefs)
        Settings.hydrateVlcSettingsCache()
        SettingsWriteBridge.onBoolean = { key, value ->
            try {
                Settings.getInstance(appContext).putSingle(key, value)
            } catch (_: Exception) {
            }
        }
        AppScope.launch(Dispatchers.IO) {
            try {
                seedDataStoreFromSharedPreferences(
                    Settings.getInstance(appContext),
                    vlcPrefs
                )
                VlcSettings.load(vlcPrefs)
                VlcSettings.loadPostMigration(vlcPrefs)
            } catch (_: Exception) {
                // SharedPreferences remains the Android source of truth.
            }
        }
    }

    private suspend fun seedDataStoreFromSharedPreferences(
        shared: SharedPreferences,
        vlcPrefs: VlcPreferences
    ) {
        // Only seed keys that VlcSettings cares about; avoid dumping entire prefs.
        val keys = listOf(
            org.videolan.tools.SHOW_VIDEO_THUMBNAILS,
            org.videolan.tools.PREF_TV_UI,
            org.videolan.tools.LIST_TITLE_ELLIPSIZE,
            org.videolan.tools.VIDEO_HUD_TIMEOUT,
            org.videolan.tools.KEY_INCLUDE_MISSING,
            org.videolan.tools.KEY_SHOW_HEADERS,
            org.videolan.tools.KEY_SHOW_TRACK_INFO,
            org.videolan.tools.KEY_VIDEO_JUMP_DELAY,
            org.videolan.tools.KEY_VIDEO_LONG_JUMP_DELAY,
            org.videolan.tools.KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY,
            org.videolan.tools.KEY_AUDIO_JUMP_DELAY,
            org.videolan.tools.KEY_AUDIO_LONG_JUMP_DELAY,
            org.videolan.tools.KEY_AUDIO_SHOW_TRACK_NUMBERS,
            org.videolan.tools.BROWSER_SHOW_HIDDEN_FILES,
            org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER,
            org.videolan.tools.TV_FOLDERS_FIRST,
            org.videolan.tools.KEY_INCOGNITO,
            org.videolan.tools.KEY_SAFE_MODE,
            org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS,
            org.videolan.tools.FASTPLAY_SPEED,
            org.videolan.tools.AUDIO_RESUME_PLAYBACK,
            org.videolan.tools.VIDEO_RESUME_PLAYBACK,
            org.videolan.tools.PLAYBACK_HISTORY,
            org.videolan.tools.KEY_AUDIO_FORCE_SHUFFLE,
            org.videolan.tools.KEY_SAVE_INDIVIDUAL_AUDIO_DELAY,
            org.videolan.tools.KEY_ENABLE_HEADSET_DETECTION,
            org.videolan.tools.KEY_ENABLE_PLAY_ON_HEADSET_INSERTION,
            org.videolan.tools.KEY_ALWAYS_FAST_SEEK,
            org.videolan.tools.LOCKSCREEN_COVER,
            org.videolan.tools.RESTORE_BACKGROUND_VIDEO,
            org.videolan.tools.SHOW_REMAINING_TIME,
            org.videolan.tools.KEY_VIDEO_APP_SWITCH,
            org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL,
            org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL,
        )
        for (key in keys) {
            if (!shared.contains(key)) continue
            val value = shared.all[key] ?: continue
            when (value) {
                is Boolean, is Int, is Long, is Float, is String -> vlcPrefs.put(key, value)
                is Set<*> -> @Suppress("UNCHECKED_CAST")
                vlcPrefs.put(key, value as Set<String>)
            }
        }
    }

    val isInitialized: Boolean get() = initialized
}
