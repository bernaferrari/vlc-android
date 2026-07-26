package org.videolan.vlc.compose.artwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.MediaItem

/**
 * Platform artwork decoder. Android uses ThumbnailsProvider / file URIs;
 * iOS/JVM return null and the UI falls back to type badges.
 */
interface ArtworkLoader {
    suspend fun loadBitmap(uri: String?, widthPx: Int): ImageBitmap?
}

object NoOpArtworkLoader : ArtworkLoader {
    override suspend fun loadBitmap(uri: String?, widthPx: Int): ImageBitmap? = null
}

/**
 * Injectable holder so Android can register a real loader without expect/actual
 * Compose entry points (keeps commonMain free of platform image frameworks).
 */
object ArtworkLoaderHolder {
    var loader: ArtworkLoader = NoOpArtworkLoader

    fun install(loader: ArtworkLoader) {
        this.loader = loader
    }
}

@Composable
fun MediaArtwork(
    item: MediaItem,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = { DefaultArtworkFallback(item) },
) {
    MediaArtworkUri(
        uri = item.artworkUri,
        favorite = item.isFavorite,
        contentDescription = item.displayTitle,
        modifier = modifier,
        size = size,
        contentScale = contentScale,
        fallback = fallback,
    )
}

@Composable
fun MediaArtworkUri(
    uri: String?,
    favorite: Boolean = false,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit,
) {
    val loader = ArtworkLoaderHolder.loader
    val requestedWidthPx = with(LocalDensity.current) {
        size.roundToPx().coerceIn(48, 512)
    }
    var bitmap by remember(uri, requestedWidthPx) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri, requestedWidthPx, loader) {
        bitmap = null
        if (!uri.isNullOrBlank()) {
            bitmap = runCatching { loader.loadBitmap(uri, requestedWidthPx) }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            fallback()
        }
        if (favorite) {
            Icon(
                icon = MaterialSymbols.Filled.Star,
                contentDescription = null,
                tint = VLCThemeDefaults.colors.primary,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
fun DefaultArtworkFallback(item: MediaItem) {
    val colors = VLCThemeDefaults.colors
    if (item.isFavorite && item.artworkUri.isNullOrBlank()) {
        Icon(
            icon = MaterialSymbols.Filled.Star,
            contentDescription = null,
            tint = colors.primary,
        )
    } else {
        Text(
            when {
                item.isVideo -> "VID"
                item.isAudio -> "AUD"
                item.isStream -> "URL"
                item.isDirectory -> "DIR"
                else -> "•"
            },
            color = colors.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
