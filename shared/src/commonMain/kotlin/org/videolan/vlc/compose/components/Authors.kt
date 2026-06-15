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
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

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
    closeIconContent: @Composable () -> Unit = { DefaultAuthorsIconPlaceholder() },
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
                Row(
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 54.dp)
                ) {
                    itemsIndexed(authors) { index, author ->
                        AuthorRow(
                            author = author,
                            authorIconContent = authorIconContent,
                            showDivider = index > 0
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
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) VLCSettingsCardDivider(startInset = 56.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .focusable()
                .padding(vertical = 6.dp),
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
private fun DefaultAuthorsIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}
