package org.videolan.vlc.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.artwork.MediaArtworkUri
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.LocalVLCTvPreviewFocus
import org.videolan.vlc.compose.tv.VLCTvAction
import org.videolan.vlc.compose.tv.VLCTvBrowserScreen
import org.videolan.vlc.compose.tv.VLCTvCardShape
import org.videolan.vlc.compose.tv.VLCTvConfirmationScreen
import org.videolan.vlc.compose.tv.VLCTvDetailsScreen
import org.videolan.vlc.compose.tv.VLCTvHomeScreen
import org.videolan.vlc.compose.tv.VLCTvItem
import org.videolan.vlc.compose.tv.VLCTvOnboardingPage
import org.videolan.vlc.compose.tv.VLCTvOnboardingScreen
import org.videolan.vlc.compose.tv.VLCTvShelfModel
import org.videolan.vlc.compose.tv.VLCTvSortOption
import org.videolan.vlc.compose.tv.VLCTvSymbolArtwork
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.compose.tv.VLCTvTrack
import org.videolan.vlc.compose.tv.VLCTvTrackAction
import org.videolan.vlc.compose.tv.VLCTvTrackListScreen
import org.videolan.vlc.model.MediaItem

/** Android TV's canonical canvas is 960x540dp; render it at 1.5x (1440x810px). */
internal val TvCanvas = 960 to 540
internal const val TvScale = 1.5f

/** Fixture catalogue mapped onto the shared TV models, artwork drawn from the preview repository. */
internal class TvFixtures(private val media: List<MediaItem>) {
    private val videos = media.filter { !it.isAudio }
    private val tracks = media.filter { it.isAudio }
    private val albums = tracks.groupBy { it.album.orEmpty() }
    private val artworkByKey = HashMap<String, String?>()

    private fun register(key: String, artwork: String?) = key.also { artworkByKey[key] = artwork }

    fun videoItems(prefix: String = "video", shape: VLCTvCardShape = VLCTvCardShape.Landscape) = videos.mapIndexed { index, item ->
        VLCTvItem(
            key = register("$prefix-${item.id}", item.artworkUri),
            title = item.title,
            subtitle = formatDuration(item.duration),
            badge = if (index % 3 == 0) "4K" else "1080p",
            progress = listOf(0.35f, 0f, 0.8f, 0f, 0.1f, 0f)[index % 6],
            shape = shape,
        )
    }

    fun albumItems() = albums.entries.map { (album, items) ->
        VLCTvItem(key = register("album-$album", items.first().artworkUri), title = album, subtitle = items.first().artist, shape = VLCTvCardShape.Square)
    }

    fun shortcuts() = listOf(
        VLCTvItem("settings", "Settings", symbol = MaterialSymbols.Filled.Settings, shape = VLCTvCardShape.Square),
        VLCTvItem("remote", "Remote access", symbol = MaterialSymbols.Filled.Devices, shape = VLCTvCardShape.Square),
        VLCTvItem("refresh", "Refresh", symbol = MaterialSymbols.Filled.Refresh, shape = VLCTvCardShape.Square),
        VLCTvItem("about", "About", subtitle = "VLC 4.0", symbol = MaterialSymbols.Filled.Info, shape = VLCTvCardShape.Square),
    )

    fun audioCategories() = listOf(
        VLCTvItem("artists", "Artists", symbol = MaterialSymbols.Filled.Groups, shape = VLCTvCardShape.Square),
        VLCTvItem("albums", "Albums", symbol = MaterialSymbols.Filled.VideoLibrary, shape = VLCTvCardShape.Square),
        VLCTvItem("genres", "Genres", symbol = MaterialSymbols.Filled.Tune, shape = VLCTvCardShape.Square),
        VLCTvItem("tracks", "Tracks", symbol = MaterialSymbols.Filled.MusicNote, shape = VLCTvCardShape.Square),
        VLCTvItem("playlists", "Playlists", symbol = MaterialSymbols.Filled.QueueMusic, shape = VLCTvCardShape.Square),
    )

    fun folders() = listOf("Movies", "Series", "Music", "Downloads", "Camera", "Podcasts").map {
        VLCTvItem("folder-$it", it, subtitle = "${it.length + 3} items", symbol = MaterialSymbols.Filled.Folder)
    }

    val artwork: @Composable androidx.compose.foundation.layout.BoxScope.(VLCTvItem) -> Unit = { item ->
        val uri = artworkByKey[item.key]
        if (uri == null) {
            VLCTvSymbolArtwork(item.symbol)
        } else {
            MediaArtworkUri(
                uri = uri,
                modifier = Modifier.fillMaxSize(),
                fillMaxSizeArtwork = true,
                showFallbackContainer = false,
                fallback = { VLCTvSymbolArtwork(item.symbol ?: MaterialSymbols.Filled.VideoLibrary) },
            )
        }
    }

    fun firstVideoArtwork() = videos.first().artworkUri
    fun album() = albums.entries.first()
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

@Composable
private fun TvFrame(dark: Boolean, focusKey: Any?, content: @Composable () -> Unit) {
    VLCTvTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalVLCTvPreviewFocus provides focusKey, content = content)
    }
}

internal fun tvShots(media: List<MediaItem>): List<Triple<String, Pair<Int, Int>, @Composable () -> Unit>> {
    val fx = TvFixtures(media)
    val videos = fx.videoItems()
    val shelves = listOf(
        VLCTvShelfModel("continue", "Continue watching", fx.videoItems("continue").filter { (it.progress ?: 0f) > 0f }),
        VLCTvShelfModel("video", "Video", videos),
        VLCTvShelfModel("audio", "Audio", fx.audioCategories()),
        VLCTvShelfModel("albums", "Recently added albums", fx.albumItems()),
        VLCTvShelfModel("browse", "Browsing", fx.folders().take(4)),
        VLCTvShelfModel("other", "Other", fx.shortcuts(), isShortcutShelf = true),
    )
    val sorts = listOf(VLCTvSortOption("Name", 1), VLCTvSortOption("Duration", 2))
    val (albumName, albumTracks) = fx.album()
    val shots = mutableListOf<Triple<String, Pair<Int, Int>, @Composable () -> Unit>>()
    for (dark in listOf(false, true)) {
        val theme = if (dark) "dark" else "light"
        shots += Triple("tv-home-$theme", TvCanvas) {
            TvFrame(dark, "continue-1") {
                VLCTvHomeScreen(appName = "VLC", shelves = shelves, onItemClick = { _, _ -> }, searchLabel = "Search", onSearch = {}, artwork = fx.artwork)
            }
        }
        shots += Triple("tv-home-scrolled-$theme", TvCanvas) {
            TvFrame(dark, "about") {
                VLCTvHomeScreen(appName = "VLC", shelves = shelves.drop(4), onItemClick = { _, _ -> }, onSearch = {}, loading = true, artwork = fx.artwork)
            }
        }
        shots += Triple("tv-browser-grid-$theme", TvCanvas) {
            TvFrame(dark, videos[1].key) {
                VLCTvBrowserScreen(
                    title = "Video", eyebrow = "Library", items = videos + fx.videoItems("more"),
                    loading = false, inGrid = true, onItemClick = {}, onBack = {}, onToggleDisplay = {}, sortOptions = sorts, artwork = fx.artwork,
                )
            }
        }
        shots += Triple("tv-browser-list-$theme", TvCanvas) {
            TvFrame(dark, "folder-Series") {
                VLCTvBrowserScreen(
                    title = "Media", subtitle = "smb  ›  nas  ›  Media", items = fx.folders() + videos.take(3),
                    loading = true, inGrid = false, favorite = true, onItemClick = {}, onBack = {}, onToggleDisplay = {}, sortOptions = sorts, artwork = fx.artwork,
                )
            }
        }
        shots += Triple("tv-browser-albums-$theme", TvCanvas) {
            TvFrame(dark, null) {
                VLCTvBrowserScreen(title = "Albums", eyebrow = "Audio", items = fx.albumItems() + fx.albumItems(), loading = false, inGrid = true, onItemClick = {}, onBack = {}, onToggleDisplay = {}, sortOptions = sorts, artwork = fx.artwork)
            }
        }
        shots += Triple("tv-browser-empty-$theme", TvCanvas) {
            TvFrame(dark, null) {
                VLCTvBrowserScreen(title = "Downloads", subtitle = "file  ›  sdcard  ›  Downloads", items = emptyList(), loading = false, inGrid = true, onItemClick = {}, onBack = {}, emptyTitle = "This folder is empty", emptyMessage = "Media you copy here will show up automatically.")
            }
        }
        shots += Triple("tv-details-$theme", TvCanvas) {
            TvFrame(dark, "action-1") {
                VLCTvDetailsScreen(
                    title = "The quiet coast",
                    eyebrow = "Movie",
                    subtitle = "A slow journey along the northern shore",
                    metadata = listOf("2025", "1h 42m", "4K", "HDR"),
                    description = "Two old friends retrace the coastal road they travelled as teenagers, stopping in harbours and lighthouses that have barely changed while their own lives have.",
                    actions = listOf(
                        VLCTvAction(1, "Resume", MaterialSymbols.Filled.PlayArrow, primary = true),
                        VLCTvAction(2, "Play from start", MaterialSymbols.Filled.Refresh),
                        VLCTvAction(3, "Add to playlist", MaterialSymbols.Filled.QueueMusic),
                        VLCTvAction(4, "Delete", MaterialSymbols.Filled.Delete),
                    ),
                    onAction = {},
                    shelves = listOf(VLCTvShelfModel("more", "More videos", videos.drop(1))),
                    artworkShape = VLCTvCardShape.Poster,
                    backdrop = { MediaArtworkUri(uri = fx.firstVideoArtwork(), modifier = Modifier.fillMaxSize().blur(24.dp), fillMaxSizeArtwork = true, showFallbackContainer = false, fallback = {}) },
                    heroArtwork = { MediaArtworkUri(uri = fx.firstVideoArtwork(), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, fillMaxSizeArtwork = true, showFallbackContainer = false, fallback = {}) },
                    artwork = fx.artwork,
                )
            }
        }
        shots += Triple("tv-details-show-$theme", TvCanvas) {
            TvFrame(dark, "episode-2") {
                val episodes = fx.videoItems("episode", VLCTvCardShape.Poster).mapIndexed { i, item ->
                    item.copy(key = "episode-$i", title = "Episode ${i + 1}", subtitle = "S01E0${i + 1}", badge = null, statusIcon = if (i < 2) MaterialSymbols.Filled.CheckCircle else null)
                }
                VLCTvDetailsScreen(
                    title = "Northern lights", eyebrow = "TV shows", metadata = listOf("2024"),
                    description = "A small fishing town prepares for the longest night of the year.",
                    actions = listOf(VLCTvAction(1, "Resume S01E03", MaterialSymbols.Filled.PlayArrow, primary = true), VLCTvAction(2, "Start over", MaterialSymbols.Filled.Refresh)),
                    onAction = {}, requestInitialFocus = false,
                    shelves = listOf(
                        VLCTvShelfModel("s1", "Season 1", episodes),
                        VLCTvShelfModel("cast", "Cast", listOf("Ada North", "Jonas Vik", "Mia Holm", "Per Lund").map { VLCTvItem("p-$it", it, symbol = org.videolan.vlc.compose.tv.ExtraSymbols.Person, shape = VLCTvCardShape.Poster) }),
                    ),
                    heroArtwork = { MediaArtworkUri(uri = fx.firstVideoArtwork(), modifier = Modifier.fillMaxSize(), fillMaxSizeArtwork = true, showFallbackContainer = false, fallback = {}) },
                    artwork = fx.artwork,
                )
            }
        }
        shots += Triple("tv-details-folder-$theme", TvCanvas) {
            TvFrame(dark, null) {
                VLCTvDetailsScreen(
                    title = "Series", eyebrow = "Folder", subtitle = "smb://nas/Media/Series",
                    actions = listOf(VLCTvAction(1, "Browse folder", MaterialSymbols.Filled.Folder, primary = true), VLCTvAction(2, "Add to favorites", MaterialSymbols.Outlined.Star)),
                    onAction = {}, artworkShape = VLCTvCardShape.Square,
                    heroArtwork = { VLCTvSymbolArtwork(MaterialSymbols.Filled.Folder) },
                )
            }
        }
        shots += Triple("tv-tracklist-$theme", TvCanvas) {
            TvFrame(dark, "track-1") {
                VLCTvTrackListScreen(
                    title = albumName, eyebrow = "Album", subtitle = albumTracks.first().artist, metadata = "${albumTracks.size} tracks · 9:51",
                    tracks = (albumTracks + albumTracks).mapIndexed { i, t -> VLCTvTrack("track-$i", t.title, t.artist, formatDuration(t.duration)) },
                    actions = listOf(
                        VLCTvAction(1, "Play", MaterialSymbols.Filled.PlayArrow, primary = true),
                        VLCTvAction(2, "Play next", MaterialSymbols.Filled.SkipNext),
                        VLCTvAction(3, "Append", MaterialSymbols.Filled.QueueMusic),
                        VLCTvAction(4, "Add to playlist", MaterialSymbols.Filled.Add),
                    ),
                    onAction = {}, onTrackClick = {}, requestInitialFocus = false,
                    trackActions = listOf(
                        VLCTvTrackAction(1, "Move up", MaterialSymbols.Filled.ArrowUpward),
                        VLCTvTrackAction(2, "Move down", MaterialSymbols.Filled.ArrowDownward),
                        VLCTvTrackAction(3, "Play next", MaterialSymbols.Filled.SkipNext),
                        VLCTvTrackAction(4, "Remove", MaterialSymbols.Filled.Close),
                    ),
                    isTrackActionEnabled = { id, index -> !(id == 1 && index == 0) },
                    heroArtwork = { MediaArtworkUri(uri = albumTracks.first().artworkUri, modifier = Modifier.fillMaxSize(), fillMaxSizeArtwork = true, showFallbackContainer = false, fallback = {}) },
                    trackArtwork = { i -> MediaArtworkUri(uri = albumTracks[i % albumTracks.size].artworkUri, modifier = Modifier.fillMaxSize(), fillMaxSizeArtwork = true, showFallbackContainer = false, fallback = {}) },
                )
            }
        }
        shots += Triple("tv-confirm-$theme", TvCanvas) {
            TvFrame(dark, "positive") {
                VLCTvConfirmationScreen(
                    title = "Delete this playlist?", message = "\"Slow mornings\" will be removed. The songs stay in your library.",
                    positiveLabel = "Yes", negativeLabel = "No", onPositive = {}, onNegative = {},
                    symbol = MaterialSymbols.Filled.Delete, destructive = true,
                )
            }
        }
        val pages = listOf(
            VLCTvOnboardingPage("Welcome to VLC", "VLC plays your movies, shows and music, from this device and from your network."),
            VLCTvOnboardingPage("Find your media", "Allow VLC to read the media on this device so it can build your library.", MaterialSymbols.Filled.Folder),
            VLCTvOnboardingPage("You're all set", "Your library is being scanned. New files will appear automatically.", MaterialSymbols.Filled.CheckCircle),
        )
        listOf(0, 2).forEach { page ->
            shots += Triple("tv-onboarding-$page-$theme", TvCanvas) {
                TvFrame(dark, "forward") {
                    VLCTvOnboardingScreen(pages = pages, currentPage = page, previousLabel = "Previous", nextLabel = "Next", finishLabel = "Start VLC", onPrevious = {}, onNext = {}, onFinish = {})
                }
            }
        }
    }
    shots += tvPlayerShots(media)
    return shots
}

/** Ten-foot video player chrome, one shot per focus state (`-Pfilter=tv-player`). */
private fun tvPlayerShots(media: List<MediaItem>): List<Triple<String, Pair<Int, Int>, @Composable () -> Unit>> {
    val videos = media.filter { !it.isAudio }
    val tracks = media.filter { it.isAudio }
    val keys = org.videolan.vlc.compose.player.TvPlayerFocusKeys
    val shots = mutableListOf<Triple<String, Pair<Int, Int>, @Composable () -> Unit>>()
    fun player(name: String, focus: Any?, dark: Boolean = true, size: Pair<Int, Int> = TvCanvas, queue: List<MediaItem> = videos, video: Boolean = true, rate: Float = 1f, live: Boolean = false, feedback: org.videolan.vlc.compose.player.PlayerGestureFeedback? = null) {
        shots += Triple(name, size) {
            TvFrame(dark, focus) { TvPlayerFixture(queue, video, rate, live, feedback) }
        }
    }
    player("tv-player-play-dark", keys.PLAY)
    player("tv-player-play-light", keys.PLAY, dark = false)
    player("tv-player-timeline-dark", keys.TIMELINE)
    player("tv-player-tracks-dark", keys.TRACKS)
    player("tv-player-speed-dark", keys.SPEED, rate = 1.5f)
    player("tv-player-next-dark", keys.NEXT)
    player("tv-player-live-dark", keys.PLAY, live = true)
    player("tv-player-seek-dark", null, feedback = org.videolan.vlc.compose.player.PlayerGestureFeedback.TapSeek(org.videolan.vlc.compose.player.SeekBurst(true, 72_000L, 30_000L, 3, 0L), androidx.compose.ui.geometry.Offset.Unspecified))
    player("tv-player-audio-dark", keys.PLAY, queue = tracks, video = false)
    player("tv-player-720p-dark", keys.PLAY, size = 853 to 480)
    return shots
}

@Composable
private fun TvPlayerFixture(
    queue: List<MediaItem>,
    video: Boolean,
    rate: Float,
    live: Boolean,
    feedback: org.videolan.vlc.compose.player.PlayerGestureFeedback?,
) {
    val item = queue.first()
    val length = if (live) 0L else item.duration.takeIf { it > 0 } ?: 5_412_000L
    org.videolan.vlc.compose.player.VideoSurfaceWithHud(
        title = item.title,
        subtitle = if (video) "Season 1 · Episode 3" else item.artist.orEmpty(),
        playing = true,
        progress = org.videolan.vlc.model.Progress(time = if (live) 0L else length / 3, length = length),
        rate = rate,
        queue = queue,
        currentQueueIndex = 0,
        hasVideoOutput = video,
        onTogglePlay = {}, onSeek = {}, onNext = {}, onPrevious = {}, onClose = {},
        previewFeedback = feedback,
        tvMode = true,
        tvAutoFocus = false,
    ) { _ ->
        if (video) {
            org.videolan.vlc.compose.artwork.MediaArtwork(item = item, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, fillMaxSizeArtwork = true)
        } else {
            org.videolan.vlc.compose.player.TvPlayerArtworkStage(
                org.videolan.vlc.viewmodel.PlayerUiState(title = item.title, subtitle = item.artist.orEmpty(), queue = queue),
            )
        }
    }
}
