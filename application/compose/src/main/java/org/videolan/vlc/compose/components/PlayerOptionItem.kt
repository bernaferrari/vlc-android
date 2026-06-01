package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the playback options row from:
 *   - application/vlc-android/res/layout/player_option_item.xml
 *
 * The app-side host supplies the icon drawable because playback option icons
 * live in :application:vlc-android resources. The surrounding RecyclerView and
 * BrowseFrameLayout keep existing focus, TV, and panel behavior during the
 * migration.
 */
@Composable
fun VLCPlayerOptionItem(
    title: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    VLCTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .width(224.dp)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                color = VLCThemeDefaults.colors.listTitle,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(
    name = "VLCPlayerOptionItem - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 240,
    heightDp = 64
)
@Composable
fun VLCPlayerOptionItemLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCPlayerOptionItem(title = "Playback speed") {
            PreviewPlayerOptionIcon()
        }
    }
}

@Preview(
    name = "VLCPlayerOptionItem - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 240,
    heightDp = 64
)
@Composable
fun VLCPlayerOptionItemDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCPlayerOptionItem(title = "Audio delay") {
            PreviewPlayerOptionIcon()
        }
    }
}

@Composable
private fun PreviewPlayerOptionIcon() {
    Box(
        Modifier
            .size(24.dp)
            .background(VLCThemeDefaults.colors.playerIconColor)
    )
}
