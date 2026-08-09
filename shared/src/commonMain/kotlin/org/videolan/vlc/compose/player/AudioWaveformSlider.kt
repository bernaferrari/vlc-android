@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.videolan.vlc.compose.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import org.videolan.vlc.compose.theme.VLCThemeDefaults

private const val WaveformBucketCount = 72

/**
 * A fully seekable audio timeline. Local files use their measured amplitude envelope; streams and
 * unsupported codecs retain the same interaction and semantics over Material's expressive wave.
 */
@Composable
internal fun AudioWaveformSlider(
    uri: String?,
    durationMs: Long,
    valueMs: Float,
    playing: Boolean,
    scrubbing: Boolean,
    contentDescription: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loader = WaveformLoaderHolder.loader
    var waveform by remember(uri, durationMs, loader) { mutableStateOf<AudioWaveform?>(null) }
    LaunchedEffect(uri, durationMs, loader) {
        waveform = null
        if (!uri.isNullOrBlank() && durationMs > 0L) {
            waveform = try {
                loader.load(uri, durationMs, WaveformBucketCount)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    val primary = VLCThemeDefaults.colors.primary
    val duration = durationMs.coerceAtLeast(1L).toFloat()
    Slider(
        value = valueMs.coerceIn(0f, duration),
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = 0f..duration,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { this.contentDescription = contentDescription },
        thumb = {
            Box(
                modifier = Modifier
                    .size(if (scrubbing) 18.dp else 14.dp)
                    .background(primary, CircleShape),
            )
        },
        track = { sliderState ->
            val fraction = sliderState.progressFraction(duration)
            val samples = waveform?.amplitudes
            if (samples.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LinearWavyProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clearAndSetSemantics { },
                        color = primary,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        amplitude = { if (playing && !scrubbing) 1f else 0f },
                        wavelength = 28.dp,
                        waveSpeed = 28.dp,
                    )
                }
            } else {
                MeasuredWaveformTrack(
                    amplitudes = samples,
                    progressFraction = fraction,
                    activeColor = primary,
                    inactiveColor = Color.White.copy(alpha = 0.24f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                )
            }
        },
    )
}

private fun SliderState.progressFraction(duration: Float): Float =
    (value / duration).coerceIn(0f, 1f)

@Composable
private fun MeasuredWaveformTrack(
    amplitudes: List<Float>,
    progressFraction: Float,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val count = amplitudes.size.coerceAtLeast(1)
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1.dp.toPx())
        val minimumHeight = 4.dp.toPx()
        val maximumHeight = size.height * 0.78f
        val activeX = size.width * progressFraction
        amplitudes.forEachIndexed { index, amplitude ->
            val shapedAmplitude = amplitude.coerceIn(0f, 1f).pow(0.72f)
            val height = minimumHeight + (maximumHeight - minimumHeight) * shapedAmplitude
            val left = index * (barWidth + gap)
            val top = (size.height - height) / 2f
            val color = when {
                left + barWidth <= activeX -> activeColor
                left < activeX -> activeColor.copy(alpha = 0.72f)
                else -> inactiveColor
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
