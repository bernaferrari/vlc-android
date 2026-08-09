package org.videolan.vlc.compose.player

/** A measured, normalized amplitude envelope for one audio source. */
data class AudioWaveform(
    val amplitudes: List<Float>,
) {
    init {
        require(amplitudes.all { it in 0f..1f })
    }
}

/**
 * Platform waveform extraction. Implementations decode real audio samples and may cache the
 * resulting envelope; unsupported sources return null so the UI can use its honest generic track.
 */
fun interface WaveformLoader {
    suspend fun load(uri: String, durationMs: Long, bucketCount: Int): AudioWaveform?
}

private val NoOpWaveformLoader = WaveformLoader { _, _, _ -> null }

object WaveformLoaderHolder {
    var loader: WaveformLoader = NoOpWaveformLoader
        private set

    fun install(loader: WaveformLoader) {
        this.loader = loader
    }
}
