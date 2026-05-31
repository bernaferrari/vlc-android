package org.videolan.vlc.gui.network

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults

@Composable
fun StreamPanelContent(
    streams: List<MediaWrapper>,
    loading: Boolean,
    searchText: String,
    clipboardText: String?,
    isTv: Boolean,
    tvOverscanHorizontal: Int,
    tvOverscanVertical: Int,
    onSearchTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onStreamClicked: (MediaWrapper) -> Unit,
    onStreamLongClicked: (Int) -> Unit,
    onMoreClicked: (Int) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isTv) tvOverscanHorizontal.dpFromPx() else 0.dp,
                    top = if (isTv) tvOverscanVertical.dpFromPx() else 0.dp,
                    end = if (isTv) tvOverscanHorizontal.dpFromPx() else 0.dp,
                    bottom = if (isTv) tvOverscanVertical.dpFromPx() else 0.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChanged,
                    label = { Text(stringResource(R.string.open_mrl_dialog_msg)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSubmit,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = stringResource(R.string.play_button),
                        tint = colors.fontDefault
                    )
                }
            }
            if (clipboardText != null) {
                Text(
                    text = stringResource(R.string.copied_from_clipboard),
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(24.dp)
                )
            }
            if (isTv) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 4.dp, top = 16.dp, end = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(streams, key = { _, item -> item.uri.toString() }) { index, item ->
                        StreamHistoryItem(
                            stream = item,
                            onClick = { onStreamClicked(item) },
                            onLongClick = { onStreamLongClicked(index) },
                            onMoreClicked = { onMoreClicked(index) },
                            showDivider = false
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 4.dp, top = 16.dp, end = 4.dp, bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(streams, key = { _, item -> item.uri.toString() }) { index, item ->
                        StreamHistoryItem(
                            stream = item,
                            onClick = { onStreamClicked(item) },
                            onLongClick = { onStreamLongClicked(index) },
                            onMoreClicked = { onMoreClicked(index) },
                            showDivider = true
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StreamHistoryItem(
    stream: MediaWrapper,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClicked: () -> Unit,
    showDivider: Boolean
) {
    val colors = VLCThemeDefaults.colors
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = decode(stream.title),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = decode(stream.location),
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onMoreClicked) {
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = stringResource(R.string.more_actions),
                    tint = colors.fontDefault,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (showDivider) HorizontalDivider()
    }
}

private fun decode(value: String?): String = value?.let { Uri.decode(it) }.orEmpty()

@Composable
private fun Int.dpFromPx() = with(androidx.compose.ui.platform.LocalDensity.current) { this@dpFromPx.toDp() }
