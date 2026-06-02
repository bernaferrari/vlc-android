/*
 * ************************************************************************
 *  NextTvActivity.kt
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

/*****************************************************************************
 * SearchActivity.java
 *
 * Copyright © 2014-2015 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
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
 */
package org.videolan.television.ui

import android.annotation.TargetApi
import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.moviepedia.MediaScraper
import org.videolan.moviepedia.models.identify.MoviepediaMedia
import org.videolan.moviepedia.models.resolver.ResolverMedia
import org.videolan.moviepedia.viewmodel.MediaScrapingModel
import org.videolan.resources.util.parcelable
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.television.util.manageHttpException
import org.videolan.tools.NetworkMonitor
import org.videolan.tools.getLocaleLanguages
import org.videolan.tools.HttpImageLoader
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCSearchResultRow
import org.videolan.vlc.compose.components.VLCSearchScreen
import org.videolan.vlc.compose.components.VLCSearchSection
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.util.isSchemeHttpOrHttps

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class MediaScrapingTvActivity : BaseTvActivity() {

    private var query by mutableStateOf("")
    private var results by mutableStateOf(emptyList<ResolverMedia>())
    private var showEmpty by mutableStateOf(false)
    private var autoFocus by mutableStateOf(false)
    private lateinit var viewModel: MediaScrapingModel
    private lateinit var media: MediaWrapper
    private val speechRecognitionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { speechQuery ->
                autoFocus = false
                submitQuery(speechQuery)
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        media = intent.parcelable(MEDIA) ?: savedInstanceState?.parcelable(MEDIA) ?: run {
            finish()
            return
        }
        viewModel = ViewModelProvider(this)[media.uri.path ?: "", MediaScrapingModel::class.java]
        viewModel.apiResult.observe(this) {
            results = it.getAllResults().filterIsInstance<ResolverMedia>()
            showEmpty = results.isEmpty()
        }
        viewModel.exceptionLiveData.observe(this) { exception ->
            exception ?: return@observe
            manageHttpException(exception)
            lifecycleScope.launch {
                NetworkMonitor.getInstance(this@MediaScrapingTvActivity).connectionFlow.first { it.connected }
                refresh()
            }
        }

        val actionQuery = when (intent.action) {
            Intent.ACTION_SEARCH, GOOGLE_SEARCH_ACTION -> intent.getStringExtra(SearchManager.QUERY)
            else -> null
        }
        val initialTextQuery = actionQuery?.takeIf { it.length >= MIN_QUERY_LENGTH }
        query = initialTextQuery ?: media.title.orEmpty()

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCSearchScreen(
                        query = query,
                        hint = getString(R.string.search_hint),
                        emptyText = getString(R.string.no_result),
                        backContentDescription = getString(R.string.close),
                        clearContentDescription = getString(R.string.clear),
                        sections = results.toSearchSections(),
                        showEmpty = showEmpty,
                        autoFocus = autoFocus,
                        onQueryChange = ::onQueryChanged,
                        onSearchAction = {
                            UiTools.setKeyboardVisibility(window.decorView, false)
                            submitQuery(query)
                        },
                        onBack = ::finish,
                        onClear = ::clearQuery,
                        onResultClick = ::onResultClick,
                        backIconContent = { SearchIcon(R.drawable.ic_arrow_back) },
                        clearIconContent = { SearchIcon(R.drawable.ic_close_small) },
                        thumbnailContent = { _, rowIndex, _ ->
                            MediaScrapingResultThumbnail(results.getOrNull(rowIndex))
                        }
                    )
                }
            }
        )
        if (initialTextQuery == null) refresh() else submitQuery(initialTextQuery)
    }

    override fun refresh() {
        if (::viewModel.isInitialized && ::media.isInitialized) viewModel.search(media.uri)
    }

    private fun onQueryChanged(value: String) {
        query = value
        if (value.isBlank()) {
            results = emptyList()
            showEmpty = false
        }
    }

    private fun clearQuery() {
        query = ""
        results = emptyList()
        showEmpty = false
        autoFocus = true
    }

    private fun submitQuery(value: String) {
        query = value
        if (value.length < MIN_QUERY_LENGTH) return
        results = emptyList()
        showEmpty = false
        viewModel.search(value)
    }

    private fun onResultClick(sectionIndex: Int, rowIndex: Int) {
        if (sectionIndex != 0) return
        val item = results.getOrNull(rowIndex) as? MoviepediaMedia ?: return
        lifecycleScope.launch {
            val exception = withContext(Dispatchers.IO) {
                try {
                    MediaScraper.saveMediaMetadata(this@MediaScrapingTvActivity, media, item)
                    null
                } catch (e: Exception) {
                    e
                }
            }
            exception?.let { manageHttpException(it) }
            finish()
        }
    }

    override fun onSearchRequested(): Boolean {
        startSpeechRecognition()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            startSpeechRecognition()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startSpeechRecognition() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.search_hint))
        }
        try {
            speechRecognitionLauncher.launch(speechIntent)
        } catch (e: ActivityNotFoundException) {
            autoFocus = true
        }
    }

    private fun List<ResolverMedia>.toSearchSections(): List<VLCSearchSection> {
        if (isEmpty()) return emptyList()
        return listOf(
            VLCSearchSection(
                title = getString(R.string.moviepedia_result),
                rows = map { it.toSearchRow() }
            )
        )
    }

    private fun ResolverMedia.toSearchRow() = VLCSearchResultRow(
        id = "${mediaType()}-${mediaId()}-${title()}",
        title = title(),
        description = getCardSubtitle(),
        thumbnailWide = false
    )

    companion object {
        const val MEDIA: String = "MEDIA"
        private const val TAG = "VLC/MediaScrapingTvActivity"
        private const val GOOGLE_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION"
        private const val MIN_QUERY_LENGTH = 3
    }
}

@Composable
private fun SearchIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun MediaScrapingResultThumbnail(item: ResolverMedia?) {
    val context = LocalContext.current
    val languages = remember(context) { context.getLocaleLanguages() }
    val imageUri = remember(item, languages) { item?.imageUri(languages) }
    val bitmap by remoteBitmap(imageUri)

    val poster = bitmap
    if (poster == null) {
        Image(
            painter = painterResource(R.drawable.ic_video_big),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Image(
            bitmap = poster.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun remoteBitmap(imageUri: Uri?) = produceState<Bitmap?>(initialValue = null, imageUri) {
    val url = imageUri?.toString()
    value = null
    value = if (!url.isNullOrEmpty() && isSchemeHttpOrHttps(imageUri.scheme)) {
        HttpImageLoader.downloadBitmap(url)
    } else {
        null
    }
}
