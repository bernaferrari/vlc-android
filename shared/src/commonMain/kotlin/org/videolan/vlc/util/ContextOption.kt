package org.videolan.vlc.util

/**
 * Context menu action flags for media items.
 *
 * Used by the UI to build context menus on both Android and iOS.
 */
enum class ContextOption : Flag {
    CTX_ADD_FOLDER_AND_SUB_PLAYLIST,
    CTX_ADD_FOLDER_PLAYLIST,
    CTX_ADD_GROUP,
    CTX_ADD_SCANNED,
    CTX_ADD_SHORTCUT,
    CTX_ADD_TO_PLAYLIST,
    CTX_APPEND,
    CTX_BAN_FOLDER,
    CTX_COPY,
    CTX_CUSTOM_REMOVE,
    CTX_DELETE,
    CTX_DOWNLOAD_SUBTITLES,
    CTX_FAV_ADD,
    CTX_FAV_EDIT,
    CTX_FAV_REMOVE,
    CTX_FIND_METADATA,
    CTX_GO_TO_FOLDER,
    CTX_GROUP_SIMILAR,
    CTX_INFORMATION,
    CTX_MARK_ALL_AS_PLAYED,
    CTX_MARK_ALL_AS_UNPLAYED,
    CTX_MARK_AS_PLAYED,
    CTX_MARK_AS_UNPLAYED,
    CTX_PLAY,
    CTX_PLAY_ALL,
    CTX_PLAY_AS_AUDIO,
    CTX_PLAY_FROM_START,
    CTX_PLAY_NEXT,
    CTX_PLAY_SHUFFLE,
    CTX_REMOVE_FROM_PLAYLIST,
    CTX_REMOVE_GROUP,
    CTX_RENAME,
    CTX_RENAME_GROUP,
    CTX_SET_RINGTONE,
    CTX_SHARE,
    CTX_STOP_AFTER_THIS,
    CTX_UNGROUP,
    CTX_GO_TO_ALBUM,
    CTX_GO_TO_ARTIST,
    CTX_GO_TO_ALBUM_ARTIST,
    CTX_QUICK_PLAY;

    override fun toLong() = 1L shl this.ordinal

    companion object {
        private fun createBaseFlags() = FlagSet(ContextOption.entries).apply {
            addAll(CTX_ADD_SHORTCUT, CTX_ADD_TO_PLAYLIST, CTX_APPEND)
        }

        fun createCtxVideoFlags() = createBaseFlags().apply {
            addAll(CTX_DELETE, CTX_DOWNLOAD_SUBTITLES, CTX_INFORMATION)
            addAll(CTX_PLAY, CTX_PLAY_ALL, CTX_PLAY_AS_AUDIO, CTX_PLAY_NEXT)
            addAll(CTX_SET_RINGTONE, CTX_SHARE)
        }

        fun createCtxTrackFlags() = createBaseFlags().apply {
            addAll(CTX_DELETE, CTX_GO_TO_FOLDER, CTX_INFORMATION, CTX_GO_TO_ALBUM, CTX_GO_TO_ARTIST, CTX_PLAY_ALL, CTX_PLAY_NEXT)
            addAll(CTX_SET_RINGTONE, CTX_SHARE)
        }

        fun createCtxAudioFlags() = createBaseFlags().apply {
            addAll(CTX_INFORMATION, CTX_PLAY, CTX_PLAY_NEXT)
        }

        fun createCtxPlaylistAlbumFlags() = createCtxAudioFlags().apply {
            addAll(CTX_DELETE, CTX_RENAME, CTX_GO_TO_ARTIST)
        }

        fun createCtxPlaylistItemFlags() = createBaseFlags().apply {
            addAll(CTX_PLAY_ALL, CTX_DELETE, CTX_INFORMATION, CTX_PLAY_NEXT, CTX_SET_RINGTONE)
        }

        fun createCtxVideoGroupFlags() = createBaseFlags().apply {
            remove(CTX_ADD_SHORTCUT)
            addAll(CTX_ADD_GROUP, CTX_MARK_ALL_AS_PLAYED, CTX_MARK_ALL_AS_UNPLAYED, CTX_PLAY_ALL, CTX_RENAME_GROUP, CTX_UNGROUP)
        }

        fun createCtxFolderFlags() = createBaseFlags().apply {
            remove(CTX_ADD_SHORTCUT)
            addAll(CTX_BAN_FOLDER, CTX_MARK_ALL_AS_PLAYED, CTX_MARK_ALL_AS_UNPLAYED, CTX_PLAY_ALL)
        }
    }
}
