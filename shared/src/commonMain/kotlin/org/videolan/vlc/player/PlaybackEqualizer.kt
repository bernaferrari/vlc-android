package org.videolan.vlc.player

/** Portable equalizer state. Amplification values use LibVLC's -20 dB…20 dB range. */
data class PlaybackEqualizer(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val presets: List<PlaybackEqualizerPreset> = emptyList(),
    val selectedPresetId: String? = null,
    val preampDb: Float = 0f,
    val bands: List<PlaybackEqualizerBand> = emptyList(),
)

data class PlaybackEqualizerPreset(
    val id: String,
    val label: String,
)

data class PlaybackEqualizerBand(
    val index: Int,
    val label: String,
    val amplificationDb: Float,
)

internal fun Float.coerceEqualizerDb(): Float = takeIf(Float::isFinite)?.coerceIn(-20f, 20f) ?: 0f
