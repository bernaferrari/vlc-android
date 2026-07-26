package org.videolan.vlc.util

/**
 * What a primary list click does — mirrors Android DefaultPlaybackAction.
 */
enum class DefaultPlaybackAction {
    PLAY,
    PLAY_ALL,
    ADD_TO_QUEUE,
    INSERT_NEXT;

    companion object {
        fun fromName(name: String?): DefaultPlaybackAction =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PLAY
    }
}

object DefaultPlaybackKeys {
    const val VIDEO = "default_playback_action_video"
    const val TRACK = "default_playback_action_track"
    const val ARTIST = "default_playback_action_artist"
    const val ALBUM = "default_playback_action_album"
    const val GENRE = "default_playback_action_genre"
    const val PLAYLIST = "default_playback_action_playlist"
    const val FILE = "default_playback_action_file"
}
