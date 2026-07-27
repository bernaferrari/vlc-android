package org.videolan.vlc.player

/** Decoder-backed crop geometries supported by both LibVLC and MobileVLCKit. */
enum class VideoCropMode(val label: String, val geometry: String?) {
    ORIGINAL("Original", null),
    RATIO_1_1("1:1", "1:1"),
    RATIO_4_3("4:3", "4:3"),
    RATIO_16_9("16:9", "16:9"),
    RATIO_16_10("16:10", "16:10"),
    RATIO_21_9("21:9", "21:9"),
}

data class PlaybackVideoCrop(
    val supported: Boolean = false,
    val mode: VideoCropMode = VideoCropMode.ORIGINAL,
)
