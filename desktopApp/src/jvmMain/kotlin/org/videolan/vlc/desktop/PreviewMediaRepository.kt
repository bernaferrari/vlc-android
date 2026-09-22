package org.videolan.vlc.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.repository.AudioEntity
import org.videolan.vlc.repository.AudioEntityKind
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.MediaSort
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Fictional, in-memory development fixtures. Never included in Android/iOS production builds. */
internal class PreviewMediaRepository(directory: File) : MediaRepository {
    private val catalog = MutableStateFlow(buildList {
        listOf("The quiet coast", "Alpine mornings", "A day in Kyoto", "Into the forest", "After the rain", "Sunday at home").forEachIndexed { index, title ->
            add(MediaItem(
                id = index + 1L, title = title, uri = "preview://video/$index", type = MediaType.VIDEO,
                duration = (184L + index * 357L) * 1_000, width = 1920, height = 1080,
                artworkUri = previewArtwork(directory, index, false),
                lastPlayed = if (index == 0) 1_000L else 0L,
                description = "Development fixture", fileName = "$title.mp4",
            ))
        }
        listOf("Drift" to "Mira Sol", "Blue hours" to "Northbound", "Soft focus" to "June & the Pines", "Paper skies" to "Elliot Vale").forEachIndexed { albumIndex, (album, artist) ->
            listOf("First light", "A little further", "Stay awhile").forEachIndexed { trackIndex, title ->
                val id = 100L + albumIndex * 10 + trackIndex
                add(MediaItem(
                    id = id, title = title, uri = "preview://audio/$id", type = MediaType.AUDIO,
                    duration = (180L + trackIndex * 31L) * 1_000, artist = artist, album = album,
                    genre = "Ambient", trackNumber = trackIndex + 1, year = 2025,
                    artworkUri = previewArtwork(directory, albumIndex, true),
                    description = "Development fixture",
                ))
            }
        }
    })
    val items: List<MediaItem> get() = catalog.value
    override fun observeMedia(type: MediaType) = catalog.map { rows -> rows.filter { type == MediaType.ALL || it.type == type } }
    override suspend fun getMedia(id: Long) = catalog.value.find { it.id == id }
    override suspend fun getMediaByIds(ids: List<Long>) = ids.mapNotNull { id -> catalog.value.find { it.id == id } }
    override fun search(query: String, type: MediaType) = observeMedia(type).map { rows -> rows.filter { "${it.title} ${it.artist} ${it.album}".contains(query, ignoreCase = true) } }
    override fun observeRecentlyPlayed(limit: Int) = catalog.map { rows -> rows.filter { it.lastPlayed > 0 }.take(limit) }
    override suspend fun count(type: MediaType) = catalog.value.count { type == MediaType.ALL || it.type == type }
    override suspend fun markAsPlayed(id: Long) = update(id) { it.copy(seen = 1) }
    override suspend fun markAsUnplayed(id: Long) = update(id) { it.copy(seen = 0) }
    override suspend fun incrementPlayCount(id: Long) = update(id) { it.copy(playedCount = it.playedCount + 1) }
    override suspend fun setFavorite(id: Long, favorite: Boolean) = update(id) { it.copy(isFavorite = favorite) }
    private fun update(id: Long, transform: (MediaItem) -> MediaItem) { catalog.value = catalog.value.map { if (it.id == id) transform(it) else it } }
    private fun entityTitle(item: MediaItem, kind: AudioEntityKind) = when (kind) {
        AudioEntityKind.ALBUM -> item.album.orEmpty()
        AudioEntityKind.ARTIST -> item.artist.orEmpty()
        AudioEntityKind.GENRE -> item.genre.orEmpty()
    }
    override fun observeAudioEntities(kind: AudioEntityKind, sort: MediaSort, desc: Boolean, onlyFavorites: Boolean, query: String) = catalog.map { rows ->
        rows.filter { it.isAudio }.groupBy { entityTitle(it, kind) }.map { (title, tracks) ->
            AudioEntity(title.hashCode().toLong(), title, kind, tracks.size, artworkUri = tracks.first().artworkUri, subtitle = tracks.first().artist)
        }.filter { it.title.contains(query, true) }.sortedBy { it.title }.let { if (desc) it.reversed() else it }
    }
    override fun observeAudioEntityTracks(kind: AudioEntityKind, entityId: Long, sort: MediaSort, desc: Boolean, onlyFavorites: Boolean) = catalog.map { rows ->
        rows.filter { it.isAudio && entityTitle(it, kind).hashCode().toLong() == entityId }
    }
}

/** Local geometric cover fixtures avoid network requests and copyrighted sample imagery. */
private fun previewArtwork(directory: File, index: Int, album: Boolean): String {
    val file = File(directory, "${if (album) "album" else "video"}-$index.png")
    if (!file.exists()) {
        val image = BufferedImage(640, if (album) 640 else 400, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val palettes = listOf(0x869B91 to 0x314D46, 0xB8B8A4 to 0x626C60, 0xCBB19A to 0x795D52, 0x82989A to 0x334C56)
        val (sky, land) = palettes[index % palettes.size]
        graphics.color = Color(sky)
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.color = Color(0xF1D9AF)
        graphics.fillOval(420 - index * 30, 65, 90, 90)
        graphics.color = Color(land).brighter()
        graphics.fillPolygon(intArrayOf(0, 180, 390, 640, 640, 0), intArrayOf(280, 140, 270, 190, image.height, image.height), 6)
        graphics.color = Color(land)
        graphics.fillOval(-180, image.height / 2, 820, image.height)
        graphics.dispose()
        ImageIO.write(image, "png", file)
    }
    return file.toURI().toString()
}
