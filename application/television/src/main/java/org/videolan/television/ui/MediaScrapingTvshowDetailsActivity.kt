/*
 * ************************************************************************
 *  MoviepediaTvshowDetailsActivity.kt
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
package org.videolan.television.ui

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.moviepedia.database.models.MediaImage
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
import org.videolan.moviepedia.database.models.getYear
import org.videolan.moviepedia.database.models.tvEpisodeSubtitle
import org.videolan.moviepedia.viewmodel.MediaMetadataFull
import org.videolan.moviepedia.viewmodel.MediaMetadataModel
import org.videolan.resources.util.getFromMl
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.vlc.R
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.VLCTvAction
import org.videolan.vlc.compose.tv.VLCTvDetailsScreen
import org.videolan.vlc.compose.tv.VLCTvShelfModel
import org.videolan.vlc.compose.tv.VLCTvSymbols
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.media.MediaUtils

class MediaScrapingTvshowDetailsActivity : BaseTvActivity() {
    private lateinit var viewModel: MediaMetadataModel
    private lateinit var showId: String
    private var tvShow by mutableStateOf<MediaMetadataFull?>(null, neverEqualPolicy())

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showId = intent.extras?.getString(TV_SHOW_ID) ?: savedInstanceState?.getString(TV_SHOW_ID) ?: run {
            finish()
            return
        }
        viewModel = ViewModelProvider(this, MediaMetadataModel.Factory(this, showId = showId))[showId, MediaMetadataModel::class.java]
        viewModel.updateLiveData.observe(this) {
            tvShow = it
            title = it.metadata?.metadata?.title.orEmpty()
        }
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTvTheme {
                        TvShowDetailsScreen(
                            tvShow = tvShow,
                            onResume = ::resumeShow,
                            onStartOver = ::startOver,
                            onEpisodeClick = ::openEpisodeDetails,
                            onImageClick = viewModel::updateMetadataImage
                        )
                    }
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TV_SHOW_ID, showId)
        super.onSaveInstanceState(outState)
    }

    override fun refresh() {}

    private fun resumeShow() {
        val medias = viewModel.provider.getResumeMedias(tvShow?.seasons)
        if (medias.isNotEmpty()) MediaUtils.openList(this, medias, 0)
    }

    private fun startOver() {
        val medias = viewModel.provider.getAllMedias(tvShow?.seasons)
        if (medias.isNotEmpty()) TvUtil.playMedia(this, medias)
    }

    private fun openEpisodeDetails(item: MediaMetadataWithImages) {
        lifecycleScope.launch {
            val media = item.media ?: item.metadata.mlId?.let { getFromMl { getMedia(it) } }
            media?.let { TvUtil.showMediaDetail(this@MediaScrapingTvshowDetailsActivity, it) }
        }
    }
}

const val TV_SHOW_ID = "TV_SHOW_ID"

private const val ACTION_RESUME = 1
private const val ACTION_START_OVER = 2

@Composable
private fun TvShowDetailsScreen(
    tvShow: MediaMetadataFull?,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onEpisodeClick: (MediaMetadataWithImages) -> Unit,
    onImageClick: (MediaImage) -> Unit
) {
    val context = LocalContext.current
    val metadata = tvShow?.metadata?.metadata
    val firstResumableEpisode = tvShow?.firstResumableEpisode()
    val resumeLabel = if (firstResumableEpisode == null) stringResource(R.string.resume)
    else stringResource(R.string.resume_episode, firstResumableEpisode.tvEpisodeSubtitle())
    val actions = listOf(
        VLCTvAction(ACTION_RESUME, resumeLabel, MaterialSymbols.Filled.PlayArrow, primary = true),
        VLCTvAction(ACTION_START_OVER, stringResource(R.string.start_over), MaterialSymbols.Filled.Refresh),
    )
    val shelves = remember(tvShow) {
        if (tvShow == null) return@remember emptyList()
        val seasons = tvShow.seasons.orEmpty().filter { it.episodes.isNotEmpty() }.map { season ->
            val entries = TvEntries<Any>(season.episodes) { (it as MediaMetadataWithImages).toTvItem() }
            VLCTvShelfModel("season-${season.seasonNumber}", context.getString(R.string.season_number, season.seasonNumber.toString()), entries.items) to entries
        }
        seasons + detailShelves(context, tvShow)
    }
    VLCTvDetailsScreen(
        title = metadata?.title.orEmpty(),
        eyebrow = stringResource(R.string.header_tvshows),
        metadata = listOfNotNull(metadata?.getYear()?.takeIf { it.isNotBlank() }),
        description = metadata?.summary,
        actions = actions,
        onAction = { if (it.id == ACTION_RESUME) onResume() else onStartOver() },
        loading = metadata == null,
        shelves = shelves.map { it.first },
        onShelfItemClick = { shelf, index ->
            when (val source = shelves.getOrNull(shelf)?.second?.get(index)) {
                is MediaMetadataWithImages -> onEpisodeClick(source)
                is MediaImage -> onImageClick(source)
            }
        },
        backdrop = metadata?.currentBackdrop?.takeIf { it.isNotBlank() }?.let { url ->
            { TvRemoteArtworkImage(url, VLCTvSymbols.Tv, Modifier.fillMaxSize()) }
        },
        heroArtwork = { TvRemoteArtworkImage(metadata?.currentPoster, VLCTvSymbols.Tv) },
        artwork = { item -> TvSourceArtwork(shelves.firstNotNullOfOrNull { it.second.sourcesByKey[item.key] }, item) },
    )
}

private fun MediaMetadataFull.firstResumableEpisode(): MediaMetadataWithImages? {
    seasons?.forEach { season ->
        season.episodes.forEach { episode ->
            episode.media?.let { media ->
                if (media.seen < 1) return episode
            }
        }
    }
    return null
}
