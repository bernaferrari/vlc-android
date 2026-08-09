package org.videolan.vlc.kmp

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.videolan.vlc.compose.player.AudioWaveform
import org.videolan.vlc.compose.player.WaveformLoader

/** Decodes local audio into a small, real amplitude envelope and caches it on disk. */
class AndroidWaveformLoader(context: Context) : WaveformLoader {
    private val appContext = context.applicationContext
    private val cacheDirectory = File(appContext.cacheDir, "audio-waveforms")
    private val memoryCache = object : LinkedHashMap<String, AudioWaveform>(12, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AudioWaveform>?): Boolean =
            size > 12
    }

    override suspend fun load(uri: String, durationMs: Long, bucketCount: Int): AudioWaveform? =
        withContext(Dispatchers.IO) {
            if (bucketCount !in 32..256 || !isLocalSource(uri)) return@withContext null
            val key = cacheKey(uri, durationMs, bucketCount)
            synchronized(memoryCache) { memoryCache[key] }?.let { return@withContext it }
            readDiskCache(key, bucketCount)?.let { cached ->
                synchronized(memoryCache) { memoryCache[key] = cached }
                return@withContext cached
            }

            val waveform = decode(uri, durationMs, bucketCount) ?: return@withContext null
            synchronized(memoryCache) { memoryCache[key] = waveform }
            writeDiskCache(key, waveform)
            waveform
        }

    private fun isLocalSource(raw: String): Boolean {
        val scheme = runCatching { Uri.parse(raw).scheme?.lowercase() }.getOrNull()
        return scheme.isNullOrBlank() || scheme == "file" || scheme == "content"
    }

    private fun cacheKey(raw: String, durationMs: Long, bucketCount: Int): String {
        val parsed = runCatching { Uri.parse(raw) }.getOrNull()
        val fingerprint = when (parsed?.scheme?.lowercase()) {
            "file" -> parsed.path?.let(::File)?.takeIf(File::isFile)?.let {
                "${it.length()}:${it.lastModified()}"
            }
            "content" -> contentFingerprint(parsed)
            null, "" -> File(raw).takeIf(File::isFile)?.let {
                "${it.length()}:${it.lastModified()}"
            }
            else -> null
        }.orEmpty()
        val source = "$raw|$durationMs|$bucketCount|$fingerprint"
        return MessageDigest.getInstance("SHA-256")
            .digest(source.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun contentFingerprint(uri: Uri): String? = runCatching {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE, "last_modified"),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val modifiedIndex = cursor.getColumnIndex("last_modified")
            val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
            val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else -1L
            "$size:$modified"
        }
    }.getOrNull()

    private fun readDiskCache(key: String, bucketCount: Int): AudioWaveform? = runCatching {
        val values = File(cacheDirectory, "$key.wave")
            .takeIf(File::isFile)
            ?.readText()
            ?.split(',')
            ?.map(String::toFloat)
            ?: return@runCatching null
        if (values.size != bucketCount || values.any { it !in 0f..1f }) null
        else AudioWaveform(values)
    }.getOrNull()

    private fun writeDiskCache(key: String, waveform: AudioWaveform) {
        runCatching {
            cacheDirectory.mkdirs()
            val target = File(cacheDirectory, "$key.wave")
            val temporary = File(cacheDirectory, "$key.tmp")
            temporary.writeText(waveform.amplitudes.joinToString(","))
            if (!temporary.renameTo(target)) {
                target.writeText(temporary.readText())
                temporary.delete()
            }
            cacheDirectory.listFiles()
                ?.sortedByDescending(File::lastModified)
                ?.drop(24)
                ?.forEach(File::delete)
        }
    }

    private suspend fun decode(raw: String, suppliedDurationMs: Long, bucketCount: Int): AudioWaveform? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            setDataSource(extractor, raw)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return null
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = inputFormat.longOrNull(MediaFormat.KEY_DURATION)
                ?: suppliedDurationMs.takeIf { it > 0L }?.times(1_000L)
                ?: return null
            if (durationUs <= 0L) return null

            extractor.selectTrack(trackIndex)
            val decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }
            codec = decoder
            val sums = DoubleArray(bucketCount)
            val weights = IntArray(bucketCount)
            val info = MediaCodec.BufferInfo()
            var inputFinished = false
            var outputFinished = false
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            val startedAt = System.nanoTime()

            while (!outputFinished && (System.nanoTime() - startedAt) < MaxDecodeNanos) {
                currentCoroutineContext().ensureActive()
                if (!inputFinished) {
                    val inputIndex = decoder.dequeueInputBuffer(CodecTimeoutUs)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex) ?: continue
                        input.clear()
                        val sampleSize = extractor.readSampleData(input, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputFinished = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(info, CodecTimeoutUs)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        pcmEncoding = outputFormat.intOrNull(MediaFormat.KEY_PCM_ENCODING)
                            ?: AudioFormat.ENCODING_PCM_16BIT
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        if (info.size > 0) {
                            val output = decoder.getOutputBuffer(outputIndex)
                            val amplitude = output?.rms(info.offset, info.size, pcmEncoding)
                            if (amplitude != null) {
                                val bucket = ((info.presentationTimeUs.toDouble() / durationUs) * bucketCount)
                                    .toInt()
                                    .coerceIn(0, bucketCount - 1)
                                sums[bucket] += amplitude
                                weights[bucket]++
                            }
                        }
                        outputFinished = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            if (!outputFinished) return null

            val measured = List(bucketCount) { index ->
                if (weights[index] == 0) null else (sums[index] / weights[index]).toFloat()
            }
            normalizeAndInterpolate(measured)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun setDataSource(extractor: MediaExtractor, raw: String) {
        val uri = Uri.parse(raw)
        when (uri.scheme?.lowercase()) {
            "content" -> extractor.setDataSource(appContext, uri, null)
            "file" -> extractor.setDataSource(requireNotNull(uri.path))
            null, "" -> extractor.setDataSource(raw)
            else -> error("Unsupported waveform source")
        }
    }

    private fun normalizeAndInterpolate(measured: List<Float?>): AudioWaveform? {
        if (measured.none { it != null }) return null
        val filled = measured.mapIndexed { index, value ->
            value ?: run {
                val previous = (index - 1 downTo 0).firstNotNullOfOrNull { measured[it] }
                val next = (index + 1 until measured.size).firstNotNullOfOrNull { measured[it] }
                when {
                    previous != null && next != null -> (previous + next) / 2f
                    previous != null -> previous
                    next != null -> next
                    else -> 0f
                }
            }
        }
        val nonZero = filled.filter { it > 0f }.sorted()
        val reference = nonZero.getOrNull((nonZero.lastIndex * 0.9f).toInt())
            ?.coerceAtLeast(nonZero.lastOrNull().orEmpty() * 0.35f)
            ?: return null
        if (reference <= 0f) return null
        return AudioWaveform(filled.map { (it / reference).coerceIn(0f, 1f) })
    }

    private fun Float?.orEmpty(): Float = this ?: 0f

    private fun MediaFormat.longOrNull(key: String): Long? =
        if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

    private fun MediaFormat.intOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private fun ByteBuffer.rms(offset: Int, byteCount: Int, encoding: Int): Double? {
        val buffer = duplicate().order(ByteOrder.nativeOrder()).apply {
            position(offset.coerceIn(0, limit()))
            limit((offset + byteCount).coerceIn(position(), limit()))
        }
        var energy = 0.0
        var count = 0
        when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> while (buffer.remaining() >= 4) {
                val sample = buffer.float.coerceIn(-1f, 1f).toDouble()
                energy += sample * sample
                count++
            }
            AudioFormat.ENCODING_PCM_8BIT -> while (buffer.hasRemaining()) {
                val sample = ((buffer.get().toInt() and 0xFF) - 128) / 128.0
                energy += sample * sample
                count++
            }
            else -> while (buffer.remaining() >= 2) {
                val sample = buffer.short / 32768.0
                energy += abs(sample) * abs(sample)
                count++
            }
        }
        return if (count == 0) null else sqrt(energy / count)
    }

    private companion object {
        const val CodecTimeoutUs = 10_000L
        const val MaxDecodeNanos = 15_000_000_000L
    }
}
