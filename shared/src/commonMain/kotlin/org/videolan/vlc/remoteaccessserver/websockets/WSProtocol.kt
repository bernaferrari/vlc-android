package org.videolan.vlc.remoteaccessserver.websockets

/**
 * Incoming WebSocket message from a remote client.
 */
data class WSIncomingMessage(
    val message: String,
    val id: Int?,
    val floatValue: Float? = null,
    val longValue: Long? = null,
    val stringValue: String? = null,
    val authTicket: String? = null
)

/**
 * Authentication ticket issued by the server.
 */
data class WSAuthTicket(
    val id: String,
    val expiration: Long
)

/**
 * Message types for the VLC remote access WebSocket protocol.
 */
enum class IncomingMessageType(private val type: String, val controlRequired: Boolean = true) {
    HELLO("hello", false),
    PLAY("play"),
    PAUSE("pause"),
    PREVIOUS("previous"),
    NEXT("next"),
    PREVIOUS10("previous10"),
    NEXT10("next10"),
    SHUFFLE("shuffle"),
    REPEAT("repeat"),
    GET_VOLUME("get-volume", false),
    SET_VOLUME("set-volume"),
    SET_PROGRESS("set-progress"),
    PLAY_CHAPTER("play-chapter"),
    SPEED("speed"),
    SLEEP_TIMER("sleep-timer"),
    SLEEP_TIMER_WAIT("sleep-timer-wait"),
    SLEEP_TIMER_RESET("sleep-timer-reset"),
    ADD_BOOKMARK("add-bookmark"),
    DELETE_BOOKMARK("delete-bookmark"),
    RENAME_BOOKMARK("rename-bookmark"),
    PLAY_MEDIA("play-media"),
    DELETE_MEDIA("delete-media"),
    MOVE_MEDIA_BOTTOM("move-media-bottom"),
    MOVE_MEDIA_TOP("move-media-top"),
    REMOTE("remote"),
    SET_BROWSER_AUDIO("set-browser-audio");

    override fun toString(): String = type

    companion object {
        fun fromString(type: String) = entries.firstOrNull { type == it.type }
    }
}
