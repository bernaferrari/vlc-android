/*
 * ************************************************************************
 *  NextActivity.kt
 * *************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 * **************************************************************************
 *
 *
 */

package org.videolan.moviepedia.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.moviepedia.R
import org.videolan.moviepedia.models.resolver.ResolverMedia
import org.videolan.moviepedia.viewmodel.MediaScrapingModel
import org.videolan.resources.MOVIEPEDIA_MEDIA
import org.videolan.resources.util.parcelable
import org.videolan.tools.HttpImageLoader
import org.videolan.tools.getLocaleLanguages
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.applyTheme
import org.videolan.vlc.util.isSchemeHttpOrHttps

open class MediaScrapingActivity : BaseActivity() {

    private lateinit var viewModel: MediaScrapingModel
    private lateinit var media: MediaWrapper
    private var rootView: ComposeView? = null
    private var query by mutableStateOf("")
    private var results by mutableStateOf(emptyList<ResolverMedia>())
    private var loading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = rootView ?: findViewById(android.R.id.content)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()

        intent.parcelable<MediaWrapper>(MOVIEPEDIA_MEDIA)?.let {
            media = it
        }
        if (!::media.isInitialized) {
            finish()
            return
        }

        query = media.title ?: ""
        loading = true
        viewModel = ViewModelProvider(this)[media.uri.path ?: "", MediaScrapingModel::class.java]
        rootView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    MediaScrapingScreen(
                        query = query,
                        results = results,
                        loading = loading,
                        errorMessage = errorMessage,
                        onQueryChange = { query = it },
                        onSearch = ::performSearch,
                        onBack = ::finish,
                        onItemClick = { finish() }
                    )
                }
            }
        }
        setContentView(rootView)

        viewModel.apiResult.observe(this) { result ->
            loading = false
            errorMessage = null
            results = result.getAllResults().filterNotNull()
        }
        viewModel.exceptionLiveData.observe(this) { exception ->
            loading = false
            errorMessage = exception?.localizedMessage ?: exception?.message
        }
        viewModel.search(media.uri)
    }

    private fun performSearch(query: String) {
        rootView?.let { UiTools.setKeyboardVisibility(it, false) }
        loading = true
        errorMessage = null
        viewModel.search(query)
    }
}

@Composable
private fun MediaScrapingScreen(
    query: String,
    results: List<ResolverMedia>,
    loading: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onItemClick: (ResolverMedia) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDefault)
    ) {
        SearchHeader(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onBack = onBack
        )
        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
        when {
            loading && results.isEmpty() -> MoviepediaLoading()
            errorMessage != null -> MoviepediaMessage(errorMessage)
            !loading && results.isEmpty() -> MoviepediaMessage(stringResource(R.string.search_no_result))
            else -> {
                Text(
                    text = stringResource(R.string.moviepedia_result),
                    color = colors.listTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { item ->
                        MoviepediaResultCard(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.close)
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(stringResource(R.string.moviepedia_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearch(query)
                }
            )
        )
    }
}

@Composable
private fun MoviepediaLoading() {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colors.primary)
    }
}

@Composable
private fun MoviepediaMessage(message: String) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun MoviepediaResultCard(
    item: ResolverMedia,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            MoviepediaPoster(item)
            Text(
                text = item.title(),
                color = colors.listTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
            )
            item.year()?.takeIf { it.isNotBlank() }?.let { year ->
                Text(
                    text = year,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 16.dp)
                )
            } ?: Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MoviepediaPoster(item: ResolverMedia) {
    val context = LocalContext.current
    val languages = remember(context) { context.getLocaleLanguages() }
    val imageUri = remember(item, languages) { item.imageUri(languages)?.let { android.net.Uri.parse(it.asString()) } }
    val bitmap by remoteBitmap(imageUri)
    val colors = VLCThemeDefaults.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .background(colors.backgroundDefaultDarker),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_movie_placeholder),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                alpha = 0.7f
            )
        }
    }
}

@Composable
private fun remoteBitmap(imageUri: Uri?) = produceState<Bitmap?>(initialValue = null, imageUri) {
    val url = imageUri?.toString()
    value = if (!url.isNullOrEmpty() && isSchemeHttpOrHttps(imageUri.scheme)) {
        HttpImageLoader.downloadBitmap(url)
    } else {
        null
    }
}
