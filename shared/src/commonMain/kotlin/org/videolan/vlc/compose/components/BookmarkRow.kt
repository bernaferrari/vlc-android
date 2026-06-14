package org.videolan.vlc.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the bookmark list row from:
 *   - application/vlc-android/res/layout/bookmark_item.xml
 *
 * The app-side panel supplies the overflow icon drawable because those
 * drawables live in :application:vlc-android resources.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCBookmarkRow(
    title: String,
    timeText: String,
    timeContentDescription: String,
    moreContentDescription: String,
    modifier: Modifier = Modifier,
    marqueeTitle: Boolean = false,
    onClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    moreIconContent: @Composable () -> Unit
) {
    VLCTheme {
        val titleModifier = if (marqueeTitle) Modifier.basicMarquee(iterations = 1) else Modifier

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = title,
                    color = VLCThemeDefaults.colors.fontDefault,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = if (marqueeTitle) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = titleModifier
                )
                Text(
                    text = timeText,
                    color = VLCThemeDefaults.colors.fontAudioLight,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { contentDescription = timeContentDescription }
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .semantics { contentDescription = moreContentDescription }
            ) {
                moreIconContent()
            }
        }
    }
}
