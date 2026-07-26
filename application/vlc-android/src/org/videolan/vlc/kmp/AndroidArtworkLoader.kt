package org.videolan.vlc.kmp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.compose.artwork.ArtworkLoader
import org.videolan.vlc.gui.helpers.AudioUtil.readCoverBitmap
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.isSchemeHttpOrHttps
import java.io.File

/**
 * Android artwork decoder using [ThumbnailsProvider] / [BitmapFactory].
 */
class AndroidArtworkLoader(
    context: Context,
) : ArtworkLoader {

    private val appContext = context.applicationContext

    override suspend fun loadBitmap(uri: String?, widthPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val raw = uri?.trim().orEmpty()
        if (raw.isEmpty()) return@withContext null
        val width = widthPx.coerceIn(48, 1024)
        decode(raw, width)?.asImageBitmap()
    }

    private suspend fun decode(raw: String, width: Int): Bitmap? {
        val decoded = runCatching { Uri.decode(raw) }.getOrDefault(raw)
        val parsed = runCatching { raw.toUri() }.getOrElse {
            runCatching { decoded.toUri() }.getOrNull()
        }
        val scheme = parsed?.scheme

        // HTTP(S) covers via existing downloader / decoder path.
        if (isSchemeHttpOrHttps(scheme) || isSchemeHttpOrHttps(decoded)) {
            readCoverBitmap(decoded, width)?.let { return it }
        }

        // Prefer ThumbnailsProvider when a MediaWrapper can be built.
        runCatching {
            val wrapper = mediaWrapperFor(raw, parsed)
            ThumbnailsProvider.obtainBitmap(wrapper, width)
        }.getOrNull()?.let { return it }

        // file:// or bare filesystem path
        val path = when {
            scheme == "file" -> Uri.decode(parsed.path ?: "")
            decoded.startsWith("file://") -> Uri.decode(decoded.removePrefix("file://"))
            decoded.startsWith("/") -> decoded
            scheme.isNullOrBlank() && !decoded.contains("://") -> decoded
            else -> null
        }
        if (!path.isNullOrBlank()) {
            readCoverBitmap(path, width)?.let { return it }
            val file = File(path)
            if (file.isFile) {
                decodeSampled(file.absolutePath, width)?.let { return it }
            }
        }

        // content:// and other resolvable URIs via ContentResolver
        if (parsed != null && !parsed.scheme.isNullOrBlank() && parsed.scheme != "file") {
            runCatching {
                appContext.contentResolver.openInputStream(parsed)?.use { input ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input, null, bounds)
                    bounds
                }?.let { bounds ->
                    appContext.contentResolver.openInputStream(parsed)?.use { input ->
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, width)
                        }
                        BitmapFactory.decodeStream(input, null, opts)
                    }
                }
            }.getOrNull()?.let { return it }
        }

        // Last resort: treat raw string as a local cover path.
        return readCoverBitmap(decoded, width)
    }

    private fun mediaWrapperFor(raw: String, parsed: Uri?): MediaWrapper {
        val uri = parsed ?: runCatching { raw.toUri() }.getOrElse { Uri.parse(raw) }
        return MLServiceLocator.getAbstractMediaWrapper(uri).apply {
            if (artworkURL.isNullOrBlank()) {
                artworkURL = raw
            }
            if (type == MediaWrapper.TYPE_ALL) {
                type = MediaWrapper.TYPE_AUDIO
            }
        }
    }

    private fun decodeSampled(path: String, width: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, width)
        }
        return BitmapFactory.decodeFile(path, opts)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int): Int {
        if (width <= 0 || height <= 0 || reqWidth <= 0) return 1
        var inSampleSize = 1
        if (width > reqWidth || height > reqWidth) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / inSampleSize >= reqWidth && halfHeight / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
