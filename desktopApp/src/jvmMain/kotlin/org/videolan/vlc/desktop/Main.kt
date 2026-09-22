package org.videolan.vlc.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.player.PlayerArtworkFallback
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.videolan.tools.JvmVlcDataStoreFactory
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.app.platformModule
import org.videolan.vlc.app.sharedModule
import org.videolan.vlc.compose.app.VlcKoinMainShell
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaylistEngine
import java.io.File

/** Development host for inspecting the real shared UI; JVM playback is intentionally unavailable. */
fun main() {
    val dataDirectory = File(System.getProperty("java.io.tmpdir"), "vlc-compose-desktop-preview").apply { mkdirs() }
    val media = PreviewMediaRepository(dataDirectory)
    val developmentModule = module {
        single<MediaRepository> { media }
        single<PlaybackService> { PlaylistEngine() }
        // Override the JVM default so visual review never touches real VLC preferences.
        single<DataStore<Preferences>> { JvmVlcDataStoreFactory(dataDirectory).create() }
    }
    val app = startKoin { modules(platformModule, sharedModule, developmentModule) }
    VlcKoin.set(app.koin)
    runBlocking {
        VlcSettings.load(app.koin.get<VlcPreferences>())
        val playlists = app.koin.get<PlaylistRepository>()
        val playlist = playlists.createPlaylist("Slow mornings")
        playlists.addToPlaylist(playlist.id, media.items.filter { it.isAudio }.take(6))
        app.koin.get<HistoryRepository>().addToHistory(media.items.first())
        app.koin.get<PlaybackService>().play(media.items.first { it.isAudio }, media.items.filter { it.isAudio })
        app.koin.get<PlaybackService>().pause()
    }
    application {
        val windowState = rememberWindowState(width = 430.dp, height = 860.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = "VLC · Design preview",
            state = windowState,
        ) {
            MenuBar {
                Menu("Preview") {
                    Item("Phone · 430 × 860", onClick = { windowState.size = DpSize(430.dp, 860.dp) })
                    Item("Narrow · 320 × 700", onClick = { windowState.size = DpSize(320.dp, 700.dp) })
                    Item("Landscape · 850 × 400", onClick = { windowState.size = DpSize(850.dp, 400.dp) })
                    Item("Wide · 1100 × 800", onClick = { windowState.size = DpSize(1100.dp, 800.dp) })
                }
            }
            VlcKoinMainShell(
                playerSurface = { state, _ ->
                    val item = state.queue.getOrNull(state.currentQueueIndex)
                    if (state.hasVideoOutput && item != null) {
                        MediaArtwork(
                            item = item,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fillMaxSizeArtwork = true,
                        )
                    } else {
                        PlayerArtworkFallback(state)
                    }
                },
            )
        }
    }
    app.close()
}
