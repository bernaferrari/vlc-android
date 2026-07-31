package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/about_authors_activity.xml
 * - application/vlc-android/res/layout/about_authors_item.xml
 *
 * The Activity keeps ownership of loading R.raw.authors and supplying app drawables.
 * This composable owns the complete visible screen: toolbar, close action, and
 * author rows. It lets AuthorsActivity retire the XML row/layout pair as part of
 * the full Compose migration.
 *
 * Material 3 Expressive redesign: a contacts-style roster — each contributor sits in
 * a tonal accent avatar disc (their initial, or the supplied icon) with an inset
 * hairline divider between rows, replacing the former flat icon + wide-gap rows.
 */
@Composable
fun VLCAuthorsScreen(
    title: String,
    authors: List<String>,
    closeContentDescription: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    closeIconContent: @Composable () -> Unit = { DefaultAuthorsCloseIcon() },
    authorIconContent: (@Composable () -> Unit)? = null
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showHeader) Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(colors.backgroundDefault),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.semantics {
                            contentDescription = closeContentDescription
                        }
                    ) {
                        CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                            closeIconContent()
                        }
                    }

                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = VLCLayout.ScreenGutter,
                        end = VLCLayout.ScreenGutter,
                        top = 8.dp,
                        bottom = 54.dp,
                    ),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(VLCLayout.GroupGap),
                ) {
                    itemsIndexed(authors) { index, author ->
                        AuthorRow(
                            author = author,
                            authorIconContent = authorIconContent,
                            position = when {
                                authors.size == 1 -> VLCListItemPosition.Single
                                index == 0 -> VLCListItemPosition.First
                                index == authors.lastIndex -> VLCListItemPosition.Last
                                else -> VLCListItemPosition.Middle
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorRow(
    author: String,
    authorIconContent: (@Composable () -> Unit)?,
    position: VLCListItemPosition,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = position.segmentShape(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VLCLayout.RowHeight)
                .focusable()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthorAvatar(author = author, authorIconContent = authorIconContent)

            Spacer(Modifier.width(16.dp))

            Text(
                text = author,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AuthorAvatar(
    author: String,
    authorIconContent: (@Composable () -> Unit)?
) {
    VLCIconChip {
        if (authorIconContent != null) {
            authorIconContent()
        } else {
            Text(
                text = author.firstInitial(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

private fun String.firstInitial(): String =
    trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

@Composable
private fun DefaultAuthorsCloseIcon() = Icon(
    icon = MaterialSymbols.Filled.Close,
    contentDescription = null,
)
