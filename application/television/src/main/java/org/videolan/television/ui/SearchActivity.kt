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
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.Genre
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.medialibrary.media.SearchAggregate
import org.videolan.resources.util.getFromMl
import org.videolan.tools.Settings
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCSearchResultRow
import org.videolan.vlc.compose.components.VLCSearchScreen
import org.videolan.vlc.compose.components.VLCSearchSection
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.loadImage
import org.videolan.vlc.util.generateResolutionClass

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class SearchActivity : BaseTvActivity() {

    private var query by mutableStateOf("")
    private var sections by mutableStateOf(emptyList<SearchSectionItems>())
    private var showEmpty by mutableStateOf(false)
    private var autoFocus by mutableStateOf(true)
    private var searchJob: Job? = null
    private val speechRecognitionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { speechQuery ->
                autoFocus = false
                onQueryChanged(speechQuery)
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialQuery = when (intent.action) {
            Intent.ACTION_SEARCH, GOOGLE_SEARCH_ACTION -> intent.getStringExtra(SearchManager.QUERY)
            else -> null
        }.orEmpty()
        if (initialQuery.isNotEmpty()) {
            autoFocus = false
            query = initialQuery
            performSearch(initialQuery)
        }
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
                        sections = sections.map { it.section },
                        showEmpty = showEmpty,
                        autoFocus = autoFocus,
                        onQueryChange = ::onQueryChanged,
                        onSearchAction = { UiTools.setKeyboardVisibility(window.decorView, false) },
                        onBack = ::finish,
                        onClear = {
                            onQueryChanged("")
                            autoFocus = true
                        },
                        onResultClick = ::onResultClick,
                        backIconContent = { SearchIcon(R.drawable.ic_arrow_back) },
                        clearIconContent = { SearchIcon(R.drawable.ic_close_small) },
                        thumbnailContent = { sectionIndex, rowIndex, row ->
                            SearchResultThumbnail(
                                item = sections.getOrNull(sectionIndex)?.items?.getOrNull(rowIndex),
                                thumbnailWide = row.thumbnailWide
                            )
                        }
                    )
                }
            }
        )
    }

    override fun refresh() { }

    private fun onQueryChanged(value: String) {
        query = value
        if (value.isBlank()) {
            searchJob?.cancel()
            sections = emptyList()
            showEmpty = false
        } else {
            performSearch(value)
        }
    }

    private fun performSearch(value: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            val searchAggregate = getFromMl { search(value, Settings.includeMissing, false) }
            sections = searchAggregate.toSections()
            showEmpty = searchAggregate.isEmpty
        }
    }

    private fun onResultClick(sectionIndex: Int, rowIndex: Int) {
        val item = sections.getOrNull(sectionIndex)?.items?.getOrNull(rowIndex) ?: return
        if (item is MediaWrapper) TvUtil.openMedia(this, item)
        else TvUtil.openAudioCategory(this, item)
        finish()
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

    private fun SearchAggregate.toSections() = listOf(
        SearchSectionItems(getString(R.string.videos), videos?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.songs), tracks?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.artists), artists?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.albums), albums?.filterNotNull().orEmpty()),
        SearchSectionItems(getString(R.string.genres), genres?.filterNotNull().orEmpty())
    ).filter { it.items.isNotEmpty() }

    private inner class SearchSectionItems(
        title: String,
        val items: List<MediaLibraryItem>
    ) {
        val section = VLCSearchSection(
            title = title,
            rows = items.map { it.toSearchRow() }
        )
    }

    private fun MediaLibraryItem.toSearchRow(): VLCSearchResultRow {
        val isVideo = this is MediaWrapper && type == MediaWrapper.TYPE_VIDEO
        return VLCSearchResultRow(
            id = "${itemType}-${id}-${title}",
            title = title.orEmpty(),
            description = searchDescription(),
            thumbnailWide = isVideo
        )
    }

    private fun MediaLibraryItem.searchDescription(): String? = when {
        (this as? MediaWrapper)?.type == MediaWrapper.TYPE_VIDEO -> {
            if (length > 0) {
                val resolution = generateResolutionClass(width, height)
                if (resolution != null) "${Tools.millisToString(length)}  •  $resolution"
                else Tools.millisToString(length)
            } else null
        }
        this is Playlist || this is Genre -> getString(R.string.track_number, tracksCount)
        else -> description
    }

    companion object {
        private const val TAG = "VLC/SearchActivity"
        private const val GOOGLE_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION"
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
private fun SearchResultThumbnail(item: MediaLibraryItem?, thumbnailWide: Boolean) {
    val imageWidth = with(LocalDensity.current) { (if (thumbnailWide) 100.dp else 48.dp).roundToPx() }

    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { imageView ->
            if (item == null) {
                imageView.setImageDrawable(null)
                return@AndroidView
            }
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setImageDrawable(UiTools.getDefaultCover(imageView.context, item))
            loadImage(imageView, item, imageWidth)
        }
    )
}
