/*
 * *************************************************************************
 *  PreferenceSearchScreen.kt
 * **************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * ***************************************************************************
 */

package org.videolan.vlc.gui.preferences.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.viewmodels.PreferenceSearchModel
import java.util.Locale

@Composable
internal fun PreferenceSearchRoute(
        viewmodel: PreferenceSearchModel,
        showTranslationToggle: Boolean,
        onClose: () -> Unit,
        onPreferenceClick: (PreferenceItem) -> Unit
) {
    var results by remember { mutableStateOf<List<PreferenceItem>>(emptyList()) }
    var showTranslations by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context as LifecycleOwner }

    DisposableEffect(viewmodel, lifecycleOwner) {
        val filteredObserver = Observer<MutableList<PreferenceItem>> { results = it.orEmpty() }
        val translationObserver = Observer<Boolean> { showTranslations = it == true }
        viewmodel.filtered.observe(lifecycleOwner, filteredObserver)
        viewmodel.showTranslations.observe(lifecycleOwner, translationObserver)

        onDispose {
            viewmodel.filtered.removeObserver(filteredObserver)
            viewmodel.showTranslations.removeObserver(translationObserver)
        }
    }

    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query, viewmodel) {
        viewmodel.filter(query.normalizedSearchQuery())
    }

    PreferenceSearchScreen(
            query = query,
            results = results,
            showTranslations = showTranslations,
            showTranslationToggle = showTranslationToggle,
            onQueryChange = {
                query = it
            },
            onToggleTranslations = {
                viewmodel.switchTranslations(query.normalizedSearchQuery())
            },
            onClose = onClose,
            onPreferenceClick = onPreferenceClick
    )
}

@Composable
private fun PreferenceSearchScreen(
        query: String,
        results: List<PreferenceItem>,
        showTranslations: Boolean,
        showTranslationToggle: Boolean,
        onQueryChange: (String) -> Unit,
        onToggleTranslations: () -> Unit,
        onClose: () -> Unit,
        onPreferenceClick: (PreferenceItem) -> Unit
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .background(colors.backgroundDefault)
        ) {
            PreferenceSearchToolbar(
                    query = query,
                    showTranslations = showTranslations,
                    showTranslationToggle = showTranslationToggle,
                    onQueryChange = onQueryChange,
                    onToggleTranslations = onToggleTranslations,
                    onClose = onClose
            )
            HorizontalDivider(color = colors.defaultDivider)
            LazyColumn(
                    modifier = Modifier.fillMaxSize()
            ) {
                items(
                        items = results,
                        key = { item -> "${item.parentScreen}:${item.key}:${item.titleEng}" }
                ) { item ->
                    PreferenceSearchResultRow(
                            item = item,
                            query = query.normalizedSearchQuery(),
                            showTranslations = showTranslations,
                            onClick = { onPreferenceClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceSearchToolbar(
        query: String,
        showTranslations: Boolean,
        showTranslationToggle: Boolean,
        onQueryChange: (String) -> Unit,
        onToggleTranslations: () -> Unit,
        onClose: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(
                    painter = painterResource(R.drawable.ic_close_up),
                    contentDescription = stringResource(R.string.close),
                    tint = colors.fontDefault
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
        ) {
            BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.fontDefault),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                    ),
                    modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                    text = stringResource(R.string.search_prefs),
                                    color = colors.fontLight,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
            )
        }
        if (showTranslationToggle) {
            Spacer(Modifier.width(8.dp))
            IconButton(
                    onClick = onToggleTranslations,
                    modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (showTranslations) colors.primary.copy(alpha = 0.32f) else androidx.compose.ui.graphics.Color.Transparent)
            ) {
                Icon(
                        painter = painterResource(R.drawable.ic_translate),
                        contentDescription = stringResource(R.string.language),
                        tint = colors.fontDefault
                )
            }
        }
    }
}

@Composable
private fun PreferenceSearchResultRow(
        item: PreferenceItem,
        query: String,
        showTranslations: Boolean,
        onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val title = if (showTranslations) item.titleEng else item.title
    val summary = if (showTranslations) item.summaryEng else item.summary
    val category = if (showTranslations) item.categoryEng else item.category

    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
    ) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 10.dp, end = 16.dp, bottom = 10.dp)
        ) {
            Text(
                    text = title.highlightedSearchText(query),
                    color = colors.listTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            if (summary.isNotEmpty()) {
                Text(
                        text = summary.highlightedSearchText(query),
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                    text = category,
                    color = colors.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(
                color = colors.defaultDivider,
                modifier = Modifier.padding(start = 20.dp)
        )
    }
}

private fun String.normalizedSearchQuery(): String = lowercase(Locale.getDefault())

private fun String.highlightedSearchText(query: String): AnnotatedString {
    if (isEmpty() || query.isBlank()) return AnnotatedString(this)

    val start = lowercase(Locale.getDefault()).indexOf(query.lowercase(Locale.getDefault()))
    if (start == -1) return AnnotatedString(this)

    val end = (start + query.length).coerceAtMost(length)
    return buildAnnotatedString {
        append(this@highlightedSearchText)
        addStyle(
                style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                ),
                start = start,
                end = end
        )
    }
}
