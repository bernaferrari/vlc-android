package org.videolan.vlc.app

import org.koin.core.module.Module
import org.koin.dsl.module
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.platform.NoOpRemoteAccessServerController
import org.videolan.vlc.platform.AppLockController
import org.videolan.vlc.platform.NoOpAppLockController
import org.videolan.vlc.platform.RemoteAccessServerController
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.repository.StreamRepository
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.LibraryViewModel
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel

/**
 * Presentation definitions are factories: a composed shell owns and clears its
 * own view models, while repositories and the playback controller remain app-wide.
 */
private val presentationModule: Module = module {
    factory {
        VideoListViewModel(
            repo = get<MediaRepository>(),
            player = get<PlaybackController>(),
            prefs = getOrNull(),
        )
    }
    factory {
        AudioListViewModel(
            repo = get<MediaRepository>(),
            playlists = get<PlaylistRepository>(),
            player = get<PlaybackController>(),
            prefs = getOrNull(),
        )
    }
    factory {
        BrowserViewModel(
            repo = get<MediaRepository>(),
            player = get<PlaybackController>(),
            prefs = getOrNull(),
        )
    }
    factory {
        PlaylistsViewModel(
            repo = get<PlaylistRepository>(),
            player = get<PlaybackController>(),
            prefs = getOrNull(),
        )
    }
    factory {
        MoreHubViewModel(
            history = get<HistoryRepository>(),
            media = get<MediaRepository>(),
            streamsRepo = getOrNull<StreamRepository>(),
            player = get<PlaybackController>(),
        )
    }
    factory { PlayerViewModel(playback = get<PlaybackService>(), controller = get()) }
    factory {
        SettingsViewModel(
            prefs = get(),
            remoteAccessServer = getOrNull<RemoteAccessServerController>() ?: NoOpRemoteAccessServerController,
            appLock = getOrNull<AppLockController>() ?: NoOpAppLockController,
        )
    }
    factory {
        LibraryViewModel(
            mediaRepository = get<MediaRepository>(),
            playback = get<PlaybackService>(),
        )
    }
}

/**
 * Shared Koin module — registered on every platform.
 *
 * Platform modules supply MediaRepository, PlaybackService, PlaylistRepository,
 * HistoryRepository, and session/PiP/renderer bridges.
 */
val sharedModule: Module = module {
    includes(presentationModule)
    single<VlcPreferences> { VlcPreferences(get()) }

    // Playback/session/history ownership is app-wide; every screen must share one controller.
    single { PlaybackController(service = get()) }
}
