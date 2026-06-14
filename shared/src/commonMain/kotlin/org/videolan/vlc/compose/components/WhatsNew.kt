package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

data class VLCWhatsNewItem(
    val id: String,
    val title: String,
    val body: String,
    val actionText: String
)

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_whats_new.xml
 *
 * The app module owns the BottomSheetDialog host and settings/navigation side
 * effects. This content renders the release notes without requiring a DataBinding
 * layout.
 */
@Composable
fun VLCWhatsNewDialogContent(
    title: String,
    items: List<VLCWhatsNewItem>,
    neverShowAgainText: String,
    neverShowAgain: Boolean,
    onNeverShowAgainChange: (Boolean) -> Unit,
    onItemAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    titleIconContent: @Composable () -> Unit = { WhatsNewIconPlaceholder() },
    itemIconContent: @Composable (VLCWhatsNewItem) -> Unit = { WhatsNewIconPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefault),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "title") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                titleIconContent()
                            }
                        }
                        Text(
                            text = title,
                            color = colors.fontDefault,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        )
                    }
                }

                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    WhatsNewCard(
                        item = item,
                        onAction = { onItemAction(item.id) },
                        iconContent = { itemIconContent(item) }
                    )
                }

                item(key = "never_again") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = neverShowAgain,
                            onCheckedChange = onNeverShowAgainChange
                        )
                        Text(
                            text = neverShowAgainText,
                            color = colors.fontDefault,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsNewCard(
    item: VLCWhatsNewItem,
    onAction: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VLCIconChip { iconContent() }
                Text(
                    text = item.title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = item.body,
                color = colors.fontLight,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onAction) {
                    Text(item.actionText)
                }
            }
        }
    }
}

@Composable
private fun WhatsNewIconPlaceholder() {
    Spacer(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}

private val MockWhatsNewItems = listOf(
    VLCWhatsNewItem(
        id = "equalizer",
        title = "Equalizer",
        body = "The equalizer has been reworked from scratch. It's easier to use, lets you disable the default presets and allows you to backup, restore and share your presets.",
        actionText = "Show in settings"
    ),
    VLCWhatsNewItem(
        id = "backup",
        title = "Backup and restore",
        body = "We improved the backup and restore settings feature. It now lets you backup more settings, including the equalizer presets.",
        actionText = "Show in settings"
    ),
    VLCWhatsNewItem(
        id = "auto",
        title = "Android Auto",
        body = "Android Auto settings have moved to the mobile application.",
        actionText = "Show in settings"
    )
)
