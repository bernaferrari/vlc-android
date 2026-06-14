package org.videolan.vlc.util

/**
 * Playback capabilities matching Android's PlaybackStateCompat action constants.
 *
 * The Long values are hardcoded from PlaybackStateCompat to avoid the Android
 * dependency — they are part of the media session protocol and must not change.
 */
@Suppress("unused")
enum class PlaybackAction(private val capability: Long) : Flag {
    ACTION_STOP(1L),
    ACTION_PAUSE(2L),
    ACTION_PLAY(4L),
    ACTION_REWIND(8L),
    ACTION_SKIP_TO_PREVIOUS(16L),
    ACTION_SKIP_TO_NEXT(32L),
    ACTION_FAST_FORWARD(64L),
    ACTION_SET_RATING(128L),
    ACTION_SEEK_TO(256L),
    ACTION_PLAY_PAUSE(512L),
    ACTION_PLAY_FROM_MEDIA_ID(1024L),
    ACTION_PLAY_FROM_SEARCH(2048L),
    ACTION_SKIP_TO_QUEUE_ITEM(4096L),
    ACTION_PLAY_FROM_URI(8192L),
    ACTION_PREPARE(16384L),
    ACTION_PREPARE_FROM_MEDIA_ID(32768L),
    ACTION_PREPARE_FROM_SEARCH(65536L),
    ACTION_PREPARE_FROM_URI(131072L),
    ACTION_SET_REPEAT_MODE(262144L),
    ACTION_SET_CAPTIONING_ENABLED(524288L),
    ACTION_SET_SHUFFLE_MODE(1048576L),
    ACTION_SET_PLAYBACK_SPEED(2097152L);

    override fun toLong() = this.capability

    companion object {
        fun createBaseActions() = FlagSet(PlaybackAction.entries).apply {
            addAll(
                ACTION_PLAY_PAUSE, ACTION_PLAY_FROM_MEDIA_ID,
                ACTION_PLAY_FROM_SEARCH, ACTION_SKIP_TO_QUEUE_ITEM,
                ACTION_PLAY_FROM_URI
            )
        }

        fun createActivePlaybackActions() = createBaseActions().apply {
            addAll(
                ACTION_REWIND, ACTION_FAST_FORWARD,
                ACTION_SEEK_TO, ACTION_SET_PLAYBACK_SPEED
            )
        }
    }
}
