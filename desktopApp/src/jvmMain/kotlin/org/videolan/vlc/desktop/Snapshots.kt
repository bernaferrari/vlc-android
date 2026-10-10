package org.videolan.vlc.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.videolan.tools.JvmVlcDataStoreFactory
import org.videolan.tools.KEY_SHARED_THEME_APPEARANCE
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.app.platformModule
import org.videolan.vlc.app.sharedModule
import org.videolan.vlc.compose.app.*
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.player.PlayerArtworkFallback
import org.videolan.vlc.compose.player.PlaybackSheetDestination
import org.videolan.vlc.compose.player.VideoSurfaceWithHud
import org.videolan.vlc.compose.components.DisplaySettingsSheet
import org.videolan.vlc.compose.components.DisplaySettingsState
import org.videolan.vlc.compose.components.VLCSearchScreen
import org.videolan.vlc.compose.components.VLCSearchSection
import org.videolan.vlc.compose.components.VLCSearchResultRow
import org.videolan.vlc.compose.theme.VLCAppTheme
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaylistEngine
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.viewmodel.MainTab
import java.io.File

/**
 * Development-only design review: renders shared screens headlessly to PNG.
 * Usage: `./gradlew :desktopApp:snapshots -Pfilter=audio` (writes to build/snapshots).
 */
fun main(args: Array<String>) {
    val filter = args.firstOrNull()?.takeIf { it.isNotBlank() }
    val out = File(System.getProperty("snapshots.dir") ?: "build/snapshots").apply { mkdirs() }
    val dataDirectory = File(System.getProperty("java.io.tmpdir"), "vlc-compose-snapshots-${System.nanoTime()}").apply { mkdirs() }
    val media = PreviewMediaRepository(dataDirectory)
    val app = startKoin {
        modules(platformModule, sharedModule, module {
            single<MediaRepository> { media }
            single<PlaybackService> { PlaylistEngine() }
            single<DataStore<Preferences>> { JvmVlcDataStoreFactory(dataDirectory).create() }
        })
    }
    VlcKoin.set(app.koin)
    val playback = app.koin.get<PlaybackService>()
    runBlocking {
        VlcSettings.load(app.koin.get<VlcPreferences>())
        val playlists = app.koin.get<PlaylistRepository>()
        val playlist = playlists.createPlaylist("Slow mornings")
        playlists.addToPlaylist(playlist.id, media.items.filter { it.isAudio }.take(6))
        playlists.createPlaylist("Road trip").also { playlists.addToPlaylist(it.id, media.items.filter { m -> m.isAudio }.drop(4).take(5)) }
        app.koin.get<HistoryRepository>().addToHistory(media.items.first())
        app.koin.get<HistoryRepository>().addToHistory(media.items.first { it.isAudio })
    }
    fun playAudio() = runBlocking { playback.play(media.items.first { it.isAudio }, media.items.filter { it.isAudio }); playback.pause() }
    fun playVideo() = runBlocking { playback.play(media.items.first { !it.isAudio }, media.items.filter { !it.isAudio }); playback.pause() }

    val phone = 412 to 892
    val wide = 1200 to 800
    data class Shot(val name: String, val size: Pair<Int, Int> = phone, val setup: () -> Unit = {}, val content: @Composable () -> Unit)

    fun shell(routes: List<VlcShellRoute>): @Composable () -> Unit = {
        VlcKoinMainShell(
            initialRoutes = routes,
            initialTab = routes.first().toMainTabOrNull() ?: MainTab.VIDEO,
            playerSurface = { state, _ ->
                val item = state.queue.getOrNull(state.currentQueueIndex)
                if (state.hasVideoOutput && item != null) {
                    MediaArtwork(item = item, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, fillMaxSizeArtwork = true)
                } else PlayerArtworkFallback(state)
            },
        )
    }
    val video = media.items.first { !it.isAudio }
    val routeShots = listOf(
        "video" to listOf(VideoRoute),
        "audio" to listOf(AudioRoute),
        "audio-album" to listOf(AudioRoute, AudioEntityRoute("Drift".hashCode().toLong(), "Drift", AudioEntityRouteKind.ALBUM)),
        "audio-artist" to listOf(AudioRoute, AudioEntityRoute("Mira Sol".hashCode().toLong(), "Mira Sol", AudioEntityRouteKind.ARTIST)),
        "browse" to listOf(BrowserRoute),
        "playlists" to listOf(PlaylistsRoute),
        "playlist-detail" to listOf(PlaylistsRoute, PlaylistDetailRoute(1, "Slow mornings")),
        "more" to listOf(MoreRoute),
        "settings" to listOf(MoreRoute, SettingsRoute),
        "about" to listOf(MoreRoute, AboutRoute),
        "about-libraries" to listOf(MoreRoute, AboutRoute, AboutLibrariesRoute),
        "about-authors" to listOf(MoreRoute, AboutRoute, AboutAuthorsRoute),
    )
    val shots = buildList {
        for (dark in listOf(false, true)) {
            val theme = if (dark) "dark" else "light"
            routeShots.forEach { (name, routes) ->
                add(Shot("$name-$theme", setup = { theme(dark); playAudio() }, content = shell(routes)))
            }
            add(Shot("player-audio-$theme", setup = { theme(dark); playAudio() }, content = shell(listOf(AudioRoute, PlayerRoute))))
            add(Shot("player-video-$theme", setup = { theme(dark); playVideo() }, content = shell(listOf(VideoRoute, PlayerRoute))))
            add(Shot("wide-video-$theme", wide, setup = { theme(dark); playAudio() }, content = shell(listOf(VideoRoute))))
            add(Shot("wide-audio-album-$theme", wide, setup = { theme(dark); playAudio() }, content = shell(listOf(AudioRoute, AudioEntityRoute("Drift".hashCode().toLong(), "Drift", AudioEntityRouteKind.ALBUM)))))
            add(Shot("wide-settings-$theme", wide, setup = { theme(dark); playAudio() }, content = shell(listOf(MoreRoute, SettingsRoute))))
            add(Shot("tall-settings-$theme", 412 to 3000, setup = { theme(dark); playAudio() }, content = shell(listOf(MoreRoute, SettingsRoute))))
            add(Shot("settings-system-$theme", setup = { theme(dark); VlcSettings.updateStringCache(KEY_SHARED_THEME_APPEARANCE, "system"); VlcSettings.updateStringCache(org.videolan.tools.KEY_SHARED_THEME_ACCENT, "teal") }, content = shell(listOf(MoreRoute, SettingsRoute))))
            add(Shot("tall-more-$theme", 412 to 1400, setup = { theme(dark); playAudio() }, content = shell(listOf(MoreRoute))))
            add(Shot("wide-player-audio-$theme", wide, setup = { theme(dark); playAudio() }, content = shell(listOf(AudioRoute, PlayerRoute))))
            listOf(PlaybackSheetDestination.SPEED, PlaybackSheetDestination.QUEUE, PlaybackSheetDestination.TOOLS).forEach { sheet ->
                add(Shot("sheet-${sheet.name.lowercase()}-$theme", setup = { theme(dark) }) {
                    VLCAppTheme { HudFixture(media.items.filter { it.isAudio }, sheet, video = false) }
                })
            }
            add(Shot("sheet-video-tools-$theme", setup = { theme(dark) }) {
                VLCAppTheme { HudFixture(media.items.filter { !it.isAudio }, PlaybackSheetDestination.TOOLS, video = true) }
            })
            add(Shot("sheet-display-$theme", setup = { theme(dark) }) {
                VLCAppTheme {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        DisplaySettingsSheet(
                            state = DisplaySettingsState(showTrackNumbers = true, showAllArtists = false, defaultActionLabel = "Default action", defaultActionOptions = listOf("PLAY", "PLAY_ALL", "ADD_TO_QUEUE"), selectedDefaultAction = "PLAY"),
                            onDismiss = {},
                        )
                    }
                }
            })
            add(Shot("search-$theme", setup = { theme(dark) }) {
                VLCAppTheme {
                    VLCSearchScreen(
                        query = "light", hint = "Search", emptyText = "No results", backContentDescription = "Back", clearContentDescription = "Clear",
                        sections = listOf(
                            VLCSearchSection("Tracks", listOf(VLCSearchResultRow("1", "First light", "Mira Sol · Drift", false), VLCSearchResultRow("2", "First light", "Northbound · Blue hours", false))),
                            VLCSearchSection("Videos", listOf(VLCSearchResultRow("3", "Alpine mornings", "9:01", true))),
                        ),
                        showEmpty = false, autoFocus = false, onQueryChange = {}, onSearchAction = {}, onBack = {}, onClear = {}, onResultClick = { _, _ -> },
                    )
                }
            })
            org.videolan.vlc.compose.components.VLCOnboardingStep.entries.forEachIndexed { index, step ->
                add(Shot("onboarding-${step.name.lowercase()}-$theme", setup = { theme(dark) }) {
                    VLCAppTheme {
                        org.videolan.vlc.compose.components.VLCOnboardingScreen(
                            step = step, stepIndex = minOf(index, 3).let { if (index >= 2) it - 1 + (if (step == org.videolan.vlc.compose.components.VLCOnboardingStep.AccessDenied) 0 else 1) else it }.coerceAtMost(3), stepCount = 4,
                            scanStorages = true, onSkip = {}, onNext = {}, onGrantPermission = {}, onScanStoragesChanged = {}, onCustomizeScan = {},
                        )
                    }
                })
            }
            add(Shot("onboarding-wide-$theme", 900 to 520, setup = { theme(dark) }) {
                VLCAppTheme {
                    org.videolan.vlc.compose.components.VLCOnboardingScreen(
                        step = org.videolan.vlc.compose.components.VLCOnboardingStep.LibraryScan, stepIndex = 2, stepCount = 4,
                        scanStorages = true, onSkip = {}, onNext = {}, onGrantPermission = {}, onScanStoragesChanged = {}, onCustomizeScan = {},
                    )
                }
            })
            QASurface.entries.forEach { surface ->
                add(Shot("qa-${surface.name.lowercase()}-$theme", 412 to 760, setup = { theme(dark) }) {
                    VLCAppTheme {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            key(surface) { GalleryContent(surface) {} }
                        }
                    }
                })
            }
        }
    }.filter { filter == null || filter.split(',').any { f -> it.name.contains(f) } }

    for (shot in shots) {
        shot.setup()
        val (w, h) = shot.size
        val scale = 2f
        runBlocking(Dispatchers.Main) {
            val scene = ImageComposeScene((w * scale).toInt(), (h * scale).toInt(), Density(scale), coroutineContext = coroutineContext, content = shot.content)
            var t = 0L
            repeat(40) {
                scene.render(t)
                t += 50_000_000L
                withContext(Dispatchers.IO) { Thread.sleep(15) }
            }
            val image = scene.render(t)
            File(out, "${shot.name}.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            scene.close()
        }
        println("wrote ${shot.name}")
    }
    app.close()
    System.exit(0)
}

@Composable
private fun HudFixture(queue: List<org.videolan.vlc.model.MediaItem>, sheet: PlaybackSheetDestination, video: Boolean) {
    val item = queue.first()
    VideoSurfaceWithHud(
        title = item.title, subtitle = item.artist.orEmpty(), playing = true,
        progress = org.videolan.vlc.model.Progress(time = 72_000L, length = item.duration),
        queue = queue, currentQueueIndex = 0, hasVideoOutput = video,
        onTogglePlay = {}, onSeek = {}, onNext = {}, onPrevious = {}, onClose = {},
        initialSheet = sheet,
    ) { _ ->
        if (video) MediaArtwork(item = item, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, fillMaxSizeArtwork = true)
        else PlayerArtworkFallback(org.videolan.vlc.viewmodel.PlayerUiState(title = item.title, subtitle = item.artist.orEmpty(), queue = queue))
    }
}

private fun theme(dark: Boolean) {
    VlcSettings.updateStringCache(KEY_SHARED_THEME_APPEARANCE, if (dark) "dark" else "light")
    VlcSettings.updateStringCache(org.videolan.tools.KEY_SHARED_THEME_ACCENT, "orange")
}
