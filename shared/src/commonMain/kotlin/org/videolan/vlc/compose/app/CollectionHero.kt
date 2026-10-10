package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.model.MediaItem

/**
 * Editorial header for a collection (album, artist, genre, playlist): one artwork, one title,
 * one quiet metadata line, then the two actions people reach for first. It scrolls with the list
 * so the tracks always remain the focus once someone starts browsing.
 */
@Composable
internal fun CollectionHero(
    eyebrow: String,
    title: String,
    subtitle: String?,
    items: List<MediaItem>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    circularArtwork: Boolean = false,
    fallbackIcon: MaterialIcon = MaterialSymbols.Filled.MusicNote,
) {
    val cover = items.firstOrNull { !it.artworkUri.isNullOrBlank() } ?: items.firstOrNull()
    val shape = if (circularArtwork) CircleShape else RoundedCornerShape(20.dp)
    val totalDuration = items.sumOf { it.duration.coerceAtLeast(0L) }
    val meta = buildList {
        add(ShellStrings.tracksCount(items.size))
        if (totalDuration > 0L) add(ShellStrings.totalDuration(totalDuration))
    }.joinToString(" · ")
    Column(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(128.dp)
                    .shadow(12.dp, shape, clip = false, ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f), spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f))
                    .clip(shape),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    MediaArtwork(item = cover, contentDescription = null, size = 128.dp, fillMaxSizeArtwork = true)
                } else {
                    Icon(fallbackIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.takeIf(String::isNotBlank)?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (items.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (items.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val padding = PaddingValues(horizontal = 20.dp)
                Button(onClick = onPlay, modifier = Modifier.weight(1f).heightIn(min = 52.dp), contentPadding = padding) {
                    Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(ShellStrings.play(), style = MaterialTheme.typography.titleSmall)
                }
                FilledTonalButton(onClick = onShuffle, modifier = Modifier.weight(1f).heightIn(min = 52.dp), contentPadding = padding) {
                    Icon(MaterialSymbols.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(ShellStrings.shufflePlay(), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
