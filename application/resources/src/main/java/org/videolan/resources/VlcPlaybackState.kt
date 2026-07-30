/*
 * Copyright © 2026 VLC authors and VideoLAN
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.videolan.resources

/**
 * VLC's platform-neutral playback contract.
 *
 * Media3 exposes readiness and play-when-ready separately, while the native
 * libVLC engine exposes a concise event state. Keeping this small contract at
 * the engine boundary makes the state shareable and lets Android's Media3
 * adapter perform the one required translation.
 */
object VlcPlaybackState {
    const val NONE = 0
    const val STOPPED = 1
    const val PAUSED = 2
    const val PLAYING = 3
    const val CONNECTING = 8
    const val ERROR = 7

    const val REPEAT_NONE = 0
    const val REPEAT_ONE = 1
    const val REPEAT_ALL = 2
    const val REPEAT_GROUP = 3

    const val SHUFFLE_NONE = 0
    const val SHUFFLE_ALL = 1
    const val SHUFFLE_GROUP = 2

    // Names retained at the native-engine boundary while callers move to the
    // Media3 Player state model. They intentionally do not reintroduce a
    // support-library dependency.
    const val STATE_NONE = NONE
    const val STATE_STOPPED = STOPPED
    const val STATE_PAUSED = PAUSED
    const val STATE_PLAYING = PLAYING
    const val STATE_CONNECTING = CONNECTING
    const val STATE_ERROR = ERROR
    const val REPEAT_MODE_NONE = REPEAT_NONE
    const val REPEAT_MODE_ONE = REPEAT_ONE
    const val REPEAT_MODE_ALL = REPEAT_ALL
    const val REPEAT_MODE_GROUP = REPEAT_GROUP
    const val SHUFFLE_MODE_NONE = SHUFFLE_NONE
    const val SHUFFLE_MODE_ALL = SHUFFLE_ALL
    const val SHUFFLE_MODE_GROUP = SHUFFLE_GROUP

    const val ACTION_STOP = 1L shl 0
    const val ACTION_PLAY_PAUSE = 1L shl 9
    const val ERROR_CODE_NOT_SUPPORTED = 1

    /** Transitional command snapshot consumed by the Android Media3 bridge. */
    class Builder {
        fun setActions(value: Long) = apply { }
        fun setState(state: Int, position: Long, speed: Float) = apply { }
        fun setActiveQueueItemId(value: Long) = apply { }
        fun setExtras(value: android.os.Bundle) = apply { }
        fun setErrorMessage(code: Int, message: CharSequence) = apply { }
        fun addCustomAction(action: CustomAction) = apply { }
        fun addCustomAction(action: String, name: CharSequence, icon: Int) = apply { }
        fun build() = PublishedState
    }

    object PublishedState

    class CustomAction private constructor() {
        class Builder(
            private val action: String,
            private val name: CharSequence,
            private val icon: Int,
        ) {
            fun setExtras(value: android.os.Bundle) = apply { }
            fun build() = CustomAction()
        }
    }
}
