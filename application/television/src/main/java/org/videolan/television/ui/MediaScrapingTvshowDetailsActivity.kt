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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.moviepedia.database.models.MediaImage
import org.videolan.moviepedia.database.models.MediaImageType
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
import org.videolan.moviepedia.database.models.Person
import org.videolan.moviepedia.database.models.getYear
import org.videolan.moviepedia.database.models.tvEpisodeSubtitle
import org.videolan.moviepedia.viewmodel.MediaMetadataFull
import org.videolan.moviepedia.viewmodel.MediaMetadataModel
import org.videolan.moviepedia.viewmodel.Season
import org.videolan.resources.util.getFromMl
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.generateResolutionClass

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
                    VLCTheme(darkTheme = true) {
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

@Composable
private fun TvShowDetailsScreen(
    tvShow: MediaMetadataFull?,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onEpisodeClick: (MediaMetadataWithImages) -> Unit,
    onImageClick: (MediaImage) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val metadataWithImages = tvShow?.metadata
    val metadata = metadataWithImages?.metadata
    val backdrop = metadata?.currentBackdrop

    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            RemoteArtwork(
                imageUrl = backdrop,
                placeholder = R.drawable.ic_video_big,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to colors.backgroundDefault.copy(alpha = 0.58f),
                            0.46f to colors.backgroundDefault.copy(alpha = 0.90f),
                            1f to colors.backgroundDefault
                        )
                    )
            )

            if (metadata == null) {
                LoadingState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 56.dp, top = 48.dp, end = 56.dp, bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    item(key = "overview") {
                        TvShowOverview(
                            tvShow = tvShow,
                            onResume = onResume,
                            onStartOver = onStartOver
                        )
                    }

                    tvShow.seasons.orEmpty().forEach { season ->
                        if (season.episodes.isNotEmpty()) {
                            item(key = "season-${season.seasonNumber}") {
                                SeasonSection(
                                    season = season,
                                    onEpisodeClick = onEpisodeClick
                                )
                            }
                        }
                    }

                    peopleSection(R.string.written_by, tvShow.writers.orEmpty())
                    peopleSection(R.string.casting, tvShow.actors.orEmpty())
                    peopleSection(R.string.directed_by, tvShow.directors.orEmpty())
                    peopleSection(R.string.produced_by, tvShow.producers.orEmpty())
                    peopleSection(R.string.music_by, tvShow.musicians.orEmpty())

                    val posterImages = metadataWithImages.images.filter { it.imageType == MediaImageType.POSTER }
                    if (posterImages.isNotEmpty()) {
                        item(key = "posters") {
                            ImageSection(
                                title = R.string.posters,
                                images = posterImages,
                                imageType = MediaImageType.POSTER,
                                onImageClick = onImageClick
                            )
                        }
                    }

                    val backdropImages = metadataWithImages.images.filter { it.imageType == MediaImageType.BACKDROP }
                    if (backdropImages.isNotEmpty()) {
                        item(key = "backdrops") {
                            ImageSection(
                                title = R.string.backdrops,
                                images = backdropImages,
                                imageType = MediaImageType.BACKDROP,
                                onImageClick = onImageClick
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.peopleSection(title: Int, people: List<Person>) {
    if (people.isNotEmpty()) {
        item(key = "people-$title") {
            PeopleSection(title = title, people = people)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = VLCThemeDefaults.colors.primary)
    }
}

@Composable
private fun TvShowOverview(
    tvShow: MediaMetadataFull,
    onResume: () -> Unit,
    onStartOver: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val metadata = tvShow.metadata?.metadata ?: return
    val firstResumableEpisode = tvShow.firstResumableEpisode()
    val resumeLabel = if (firstResumableEpisode == null) {
        androidx.compose.ui.res.stringResource(R.string.resume)
    } else {
        androidx.compose.ui.res.stringResource(R.string.resume_episode, firstResumableEpisode.tvEpisodeSubtitle())
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        RemoteArtwork(
            imageUrl = metadata.currentPoster,
            placeholder = R.drawable.ic_video_big,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(188.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 900.dp)
        ) {
            Text(
                text = metadata.title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metadata.getYear(),
                color = colors.fontLight,
                style = MaterialTheme.typography.titleMedium
            )
            if (metadata.summary.isNotBlank()) {
                Text(
                    text = metadata.summary,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 7,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(resumeLabel)
                }
                OutlinedButton(
                    onClick = onStartOver,
                    border = BorderStroke(1.dp, colors.primary)
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.start_over))
                }
            }
        }
    }
}

@Composable
private fun SeasonSection(
    season: Season,
    onEpisodeClick: (MediaMetadataWithImages) -> Unit
) {
    DetailSectionHeader(title = androidx.compose.ui.res.stringResource(R.string.season_number, season.seasonNumber.toString()))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 32.dp)
    ) {
        items(
            items = season.episodes,
            key = { it.metadata.moviepediaId }
        ) { episode ->
            EpisodeCard(
                item = episode,
                onClick = { onEpisodeClick(episode) }
            )
        }
    }
}

@Composable
private fun PeopleSection(title: Int, people: List<Person>) {
    DetailSectionHeader(title = androidx.compose.ui.res.stringResource(title))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 32.dp)
    ) {
        items(
            items = people,
            key = { it.moviepediaId }
        ) { person ->
            PersonCard(person = person)
        }
    }
}

@Composable
private fun ImageSection(
    title: Int,
    images: List<MediaImage>,
    imageType: MediaImageType,
    onImageClick: (MediaImage) -> Unit
) {
    DetailSectionHeader(title = androidx.compose.ui.res.stringResource(title))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 32.dp)
    ) {
        items(
            items = images,
            key = { "${it.mediaId}-${it.url}" }
        ) { image ->
            ImageChoiceCard(
                image = image,
                imageType = imageType,
                onClick = { onImageClick(image) }
            )
        }
    }
}

@Composable
private fun DetailSectionHeader(title: String) {
    Text(
        text = title,
        color = VLCThemeDefaults.colors.primary,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun EpisodeCard(
    item: MediaMetadataWithImages,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val media = item.media
    val progress = media?.let {
        if (it.length > 0 && it.displayTime > 0) it.displayTime.toFloat() / it.length.toFloat() else 0f
    } ?: 0f
    Surface(
        color = colors.backgroundDefaultDarker,
        contentColor = colors.fontDefault,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(156.dp)
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(10.dp)) {
            Box {
                RemoteArtwork(
                    imageUrl = item.metadata.currentPoster,
                    placeholder = R.drawable.ic_video_big,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(4.dp))
                )
                if ((media?.seen ?: 0L) > 0L) {
                    Icon(
                        painter = painterResource(R.drawable.ic_seen_normal),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(colors.primary, RoundedCornerShape(bottomStart = 4.dp))
                            .padding(5.dp)
                            .size(18.dp)
                    )
                }
            }
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            } else {
                Spacer(Modifier.height(3.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.metadata.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.tvEpisodeSubtitle(),
                color = colors.fontLight,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            media?.let {
                val resolution = generateResolutionClass(it.width, it.height)
                if (!resolution.isNullOrBlank()) {
                    Text(
                        text = resolution,
                        color = colors.primary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonCard(person: Person) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefaultDarker,
        contentColor = colors.fontDefault,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(132.dp)
            .focusable()
    ) {
        Column(Modifier.padding(10.dp)) {
            RemoteArtwork(
                imageUrl = person.image,
                placeholder = R.drawable.ic_people_big,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImageChoiceCard(
    image: MediaImage,
    imageType: MediaImageType,
    onClick: () -> Unit
) {
    val width = if (imageType == MediaImageType.POSTER) 132.dp else 220.dp
    val aspectRatio = if (imageType == MediaImageType.POSTER) 2f / 3f else 16f / 9f
    RemoteArtwork(
        imageUrl = image.url,
        placeholder = R.drawable.ic_video_big,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(width)
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(6.dp))
            .focusable()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun RemoteArtwork(
    imageUrl: String?,
    placeholder: Int,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    TvRemoteArtworkImage(
        imageUrl = imageUrl,
        placeholder = placeholder,
        modifier = modifier,
        contentScale = contentScale
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
