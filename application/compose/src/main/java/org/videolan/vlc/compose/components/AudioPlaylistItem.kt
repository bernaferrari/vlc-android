package org.videolan.vlc.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the legacy audio/video playlist row.
 *
 * The app-side queue hosts supply media artwork and action drawables
 * because those resources and async thumbnail helpers live in :vlc-android.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCAudioPlaylistItem(
    title: String,
    subtitle: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    trackNumberText: String = "",
    showTrackNumber: Boolean = false,
    showReorderButtons: Boolean = false,
    showDeleteButton: Boolean = false,
    stopAfterThis: Boolean = false,
    current: Boolean = false,
    video: Boolean = false,
    marqueeTitle: Boolean = false,
    masked: Boolean = false,
    tipsOverlayColor: Color = Color.Transparent,
    onClick: () -> Unit = {},
    onMoveUpClick: () -> Unit = {},
    onMoveDownClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    coverContent: @Composable BoxScope.() -> Unit,
    playingContent: @Composable BoxScope.() -> Unit,
    stopAfterContent: @Composable () -> Unit,
    moveUpContent: @Composable () -> Unit,
    moveDownContent: @Composable () -> Unit,
    deleteContent: @Composable () -> Unit,
    moreContent: @Composable () -> Unit
) {
    VLCTheme {
        val titleModifier = if (marqueeTitle) Modifier.basicMarquee(iterations = 1) else Modifier
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics { this.contentDescription = contentDescription }
                    .padding(start = 8.dp)
            ) {
                PlaylistCover(
                    video = video,
                    current = current,
                    coverContent = coverContent,
                    playingContent = playingContent
                )

                if (showTrackNumber && trackNumberText.isNotBlank()) {
                    Text(
                        text = trackNumberText,
                        color = VLCThemeDefaults.colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        modifier = Modifier
                            .width(28.dp)
                            .padding(start = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = VLCThemeDefaults.colors.fontDefault,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = if (marqueeTitle) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = titleModifier.padding(horizontal = 8.dp)
                    )
                    if (subtitle.isNotEmpty() || stopAfterThis) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = subtitle,
                                color = VLCThemeDefaults.colors.fontAudioLight,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (stopAfterThis) {
                                Spacer(Modifier.width(4.dp))
                                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    stopAfterContent()
                                }
                            }
                        }
                    }
                }

                if (showReorderButtons) {
                    PlaylistActionButton(onClick = onMoveUpClick, content = moveUpContent)
                    PlaylistActionButton(onClick = onMoveDownClick, content = moveDownContent)
                }
                if (showDeleteButton) {
                    PlaylistActionButton(onClick = onDeleteClick, content = deleteContent)
                }
                PlaylistActionButton(onClick = onMoreClick, content = moreContent)
            }

            if (masked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(tipsOverlayColor)
                )
            }
        }
    }
}

@Composable
private fun PlaylistCover(
    video: Boolean,
    current: Boolean,
    coverContent: @Composable BoxScope.() -> Unit,
    playingContent: @Composable BoxScope.() -> Unit
) {
    val width = if (video) 77.dp else 48.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(width)
            .padding(vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = width, height = 48.dp)
                .clip(MaterialTheme.shapes.small)
        ) {
            if (current) playingContent() else coverContent()
        }
    }
}

@Composable
private fun PlaylistActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        content()
    }
}

@Preview(
    name = "VLCAudioPlaylistItem - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 420,
    heightDp = 80
)
@Composable
fun VLCAudioPlaylistItemLightPreview() {
    VLCTheme(darkTheme = false) {
        PlaylistItemPreviewContent(current = false)
    }
}

@Preview(
    name = "VLCAudioPlaylistItem - Dark Current",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 420,
    heightDp = 80
)
@Composable
fun VLCAudioPlaylistItemDarkPreview() {
    VLCTheme(darkTheme = true) {
        PlaylistItemPreviewContent(current = true)
    }
}

@Composable
private fun PlaylistItemPreviewContent(current: Boolean) {
    VLCAudioPlaylistItem(
        title = "Symphony No. 1",
        subtitle = "Beethoven",
        contentDescription = "Symphony No. 1, Beethoven",
        trackNumberText = "1.",
        showTrackNumber = true,
        showReorderButtons = true,
        showDeleteButton = true,
        stopAfterThis = current,
        current = current,
        coverContent = { PreviewSquare(48.dp) },
        playingContent = { PreviewSquare(32.dp) },
        stopAfterContent = { Text("S", color = VLCThemeDefaults.colors.playerIconColor) },
        moveUpContent = { Text("^", color = VLCThemeDefaults.colors.playerIconColor) },
        moveDownContent = { Text("v", color = VLCThemeDefaults.colors.playerIconColor) },
        deleteContent = { Text("x", color = VLCThemeDefaults.colors.playerIconColor) },
        moreContent = { Text("...", color = VLCThemeDefaults.colors.playerIconColor) }
    )
}

@Composable
private fun BoxScope.PreviewSquare(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(VLCThemeDefaults.colors.fontAudioLight)
            .align(Alignment.Center)
    )
}
