package org.videolan.vlc.compose.app

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.repository.AudioEntity
import org.videolan.vlc.repository.AudioEntityKind
import org.videolan.vlc.viewmodel.MainTab

/** Typed, serializable Navigation 3 destinations for the VLC shell. */
sealed interface VlcShellRoute : NavKey

@Serializable
data object VideoRoute : VlcShellRoute

@Serializable
data object AudioRoute : VlcShellRoute

@Serializable
data object BrowserRoute : VlcShellRoute

@Serializable
data object PlaylistsRoute : VlcShellRoute

@Serializable
data object MoreRoute : VlcShellRoute

@Serializable
data object PlayerRoute : VlcShellRoute

@Serializable
data object SettingsRoute : VlcShellRoute

@Serializable
enum class VideoContainerRouteKind { FOLDER, VIDEO_GROUP }

/** A video group or folder, retained in the saved stack as primitives. */
@Serializable
data class VideoContainerRoute(
    val id: Long,
    val title: String,
    val kind: VideoContainerRouteKind,
) : VlcShellRoute {
    fun toMediaFolder(): MediaFolder = MediaFolder(
        id = id,
        title = title,
        path = "",
        kind = if (kind == VideoContainerRouteKind.VIDEO_GROUP) FolderKind.VIDEO_GROUP else FolderKind.MEDIA_FOLDER,
    )
}

@Serializable
enum class AudioEntityRouteKind { ARTIST, ALBUM, GENRE }

/** An audio entity reconstructed by id when its route is restored. */
@Serializable
data class AudioEntityRoute(
    val id: Long,
    val title: String,
    val kind: AudioEntityRouteKind,
) : VlcShellRoute {
    fun toAudioEntity(): AudioEntity = AudioEntity(
        id = id,
        title = title,
        kind = kind.toRepositoryKind(),
    )
}

/** Serializable browser folder data; paths are needed to rebuild a URI stack. */
@Serializable
data class BrowserFolderKey(
    val id: Long,
    val title: String,
    val path: String,
    val uri: String,
    val childCount: Int,
    val isRoot: Boolean,
    val isFavorite: Boolean,
    val kind: BrowserFolderRouteKind,
)

@Serializable
enum class BrowserFolderRouteKind { STORAGE, FAVORITE, NETWORK, VIDEO_GROUP, MEDIA_FOLDER }

@Serializable
data class BrowserFolderRoute(val stack: List<BrowserFolderKey>) : VlcShellRoute {
    init {
        require(stack.isNotEmpty()) { "A browser folder route needs at least one folder" }
    }

    fun toMediaFolders(): List<MediaFolder> = stack.map(BrowserFolderKey::toMediaFolder)

    companion object {
        fun from(folders: List<MediaFolder>): BrowserFolderRoute =
            BrowserFolderRoute(folders.map(MediaFolder::toBrowserFolderKey))
    }
}

/** A playlist detail can be restored without serializing its media items. */
@Serializable
data class PlaylistDetailRoute(
    val id: Long,
    val name: String,
) : VlcShellRoute {
    fun toPlaylistInfo(): PlaylistInfo = PlaylistInfo(id = id, name = name)
}

fun MainTab.toVlcShellRoute(): VlcShellRoute = when (this) {
    MainTab.VIDEO -> VideoRoute
    MainTab.AUDIO -> AudioRoute
    MainTab.BROWSER -> BrowserRoute
    MainTab.PLAYLISTS -> PlaylistsRoute
    MainTab.MORE -> MoreRoute
}

fun VlcShellRoute.toMainTabOrNull(): MainTab? = when (this) {
    VideoRoute -> MainTab.VIDEO
    is VideoContainerRoute -> MainTab.VIDEO
    AudioRoute -> MainTab.AUDIO
    is AudioEntityRoute -> MainTab.AUDIO
    BrowserRoute -> MainTab.BROWSER
    is BrowserFolderRoute -> MainTab.BROWSER
    PlaylistsRoute -> MainTab.PLAYLISTS
    is PlaylistDetailRoute -> MainTab.PLAYLISTS
    MoreRoute -> MainTab.MORE
    PlayerRoute, SettingsRoute -> null
}

/** Returns the root tab represented by a Navigation 3 back stack. */
fun List<VlcShellRoute>.activeTab(): MainTab =
    asReversed().firstNotNullOfOrNull(VlcShellRoute::toMainTabOrNull) ?: MainTab.VIDEO

/** Restores typed routes across process recreation on every supported target. */
val vlcShellNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(VideoRoute::class, VideoRoute.serializer())
            subclass(AudioRoute::class, AudioRoute.serializer())
            subclass(BrowserRoute::class, BrowserRoute.serializer())
            subclass(PlaylistsRoute::class, PlaylistsRoute.serializer())
            subclass(MoreRoute::class, MoreRoute.serializer())
            subclass(PlayerRoute::class, PlayerRoute.serializer())
            subclass(SettingsRoute::class, SettingsRoute.serializer())
            subclass(VideoContainerRoute::class, VideoContainerRoute.serializer())
            subclass(AudioEntityRoute::class, AudioEntityRoute.serializer())
            subclass(BrowserFolderRoute::class, BrowserFolderRoute.serializer())
            subclass(PlaylistDetailRoute::class, PlaylistDetailRoute.serializer())
        }
    }
}

fun MediaFolder.toVideoContainerRoute(): VideoContainerRoute = VideoContainerRoute(
    id = id,
    title = title,
    kind = if (kind == FolderKind.VIDEO_GROUP) VideoContainerRouteKind.VIDEO_GROUP else VideoContainerRouteKind.FOLDER,
)

fun MediaItem.toAudioEntityRoute(): AudioEntityRoute? {
    val kind = when {
        uri.startsWith("artist://") -> AudioEntityRouteKind.ARTIST
        uri.startsWith("album://") -> AudioEntityRouteKind.ALBUM
        uri.startsWith("genre://") -> AudioEntityRouteKind.GENRE
        else -> return null
    }
    val entityId = uri.substringAfter("://").toLongOrNull() ?: return null
    return AudioEntityRoute(id = entityId, title = title, kind = kind)
}

private fun BrowserFolderKey.toMediaFolder(): MediaFolder = MediaFolder(
    id = id,
    title = title,
    path = path,
    uri = uri,
    childCount = childCount,
    isRoot = isRoot,
    isFavorite = isFavorite,
    kind = kind.toFolderKind(),
)

private fun MediaFolder.toBrowserFolderKey(): BrowserFolderKey = BrowserFolderKey(
    id = id,
    title = title,
    path = path,
    uri = uri,
    childCount = childCount,
    isRoot = isRoot,
    isFavorite = isFavorite,
    kind = kind.toRouteKind(),
)

private fun AudioEntityRouteKind.toRepositoryKind(): AudioEntityKind = when (this) {
    AudioEntityRouteKind.ARTIST -> AudioEntityKind.ARTIST
    AudioEntityRouteKind.ALBUM -> AudioEntityKind.ALBUM
    AudioEntityRouteKind.GENRE -> AudioEntityKind.GENRE
}

private fun BrowserFolderRouteKind.toFolderKind(): FolderKind = when (this) {
    BrowserFolderRouteKind.STORAGE -> FolderKind.STORAGE
    BrowserFolderRouteKind.FAVORITE -> FolderKind.FAVORITE
    BrowserFolderRouteKind.NETWORK -> FolderKind.NETWORK
    BrowserFolderRouteKind.VIDEO_GROUP -> FolderKind.VIDEO_GROUP
    BrowserFolderRouteKind.MEDIA_FOLDER -> FolderKind.MEDIA_FOLDER
}

private fun FolderKind.toRouteKind(): BrowserFolderRouteKind = when (this) {
    FolderKind.STORAGE -> BrowserFolderRouteKind.STORAGE
    FolderKind.FAVORITE -> BrowserFolderRouteKind.FAVORITE
    FolderKind.NETWORK -> BrowserFolderRouteKind.NETWORK
    FolderKind.VIDEO_GROUP -> BrowserFolderRouteKind.VIDEO_GROUP
    FolderKind.MEDIA_FOLDER -> BrowserFolderRouteKind.MEDIA_FOLDER
}
