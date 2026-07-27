package org.videolan.vlc.player

/**
 * Decoder-backed video adjustment values shared by the player sheet.
 *
 * The ranges are the documented MobileVLCKit/libVLC adjust-filter ranges. A backend advertises
 * [PlaybackVideoAdjust.supported] only when it can apply these values to its active decoder.
 */
enum class VideoAdjustParameter(
    val label: String,
    val minimum: Float,
    val maximum: Float,
    val defaultValue: Float,
) {
    BRIGHTNESS("Brightness", 0f, 2f, 1f),
    CONTRAST("Contrast", 0f, 2f, 1f),
    HUE("Hue", -180f, 180f, 0f),
    SATURATION("Saturation", 0f, 3f, 1f),
    GAMMA("Gamma", 0f, 10f, 1f),
}

data class PlaybackVideoAdjust(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val brightness: Float = VideoAdjustParameter.BRIGHTNESS.defaultValue,
    val contrast: Float = VideoAdjustParameter.CONTRAST.defaultValue,
    val hue: Float = VideoAdjustParameter.HUE.defaultValue,
    val saturation: Float = VideoAdjustParameter.SATURATION.defaultValue,
    val gamma: Float = VideoAdjustParameter.GAMMA.defaultValue,
) {
    fun value(parameter: VideoAdjustParameter): Float = when (parameter) {
        VideoAdjustParameter.BRIGHTNESS -> brightness
        VideoAdjustParameter.CONTRAST -> contrast
        VideoAdjustParameter.HUE -> hue
        VideoAdjustParameter.SATURATION -> saturation
        VideoAdjustParameter.GAMMA -> gamma
    }

    fun withValue(parameter: VideoAdjustParameter, value: Float): PlaybackVideoAdjust = when (parameter) {
        VideoAdjustParameter.BRIGHTNESS -> copy(brightness = value)
        VideoAdjustParameter.CONTRAST -> copy(contrast = value)
        VideoAdjustParameter.HUE -> copy(hue = value)
        VideoAdjustParameter.SATURATION -> copy(saturation = value)
        VideoAdjustParameter.GAMMA -> copy(gamma = value)
    }
}

fun VideoAdjustParameter.coerce(value: Float): Float =
    value.takeIf(Float::isFinite)?.coerceIn(minimum, maximum) ?: defaultValue
