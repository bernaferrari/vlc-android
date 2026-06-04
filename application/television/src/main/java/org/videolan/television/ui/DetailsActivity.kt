/*****************************************************************************
 * DetailsActivity.java
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

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.moviepedia.database.models.MediaImage
import org.videolan.moviepedia.database.models.MediaImageType
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
import org.videolan.moviepedia.database.models.Person
import org.videolan.moviepedia.database.models.subtitle
import org.videolan.moviepedia.viewmodel.MediaMetadataFull
import org.videolan.moviepedia.viewmodel.MediaMetadataModel
import org.videolan.resources.ACTION_REMOTE_STOP
import org.videolan.resources.FAVORITE_TITLE
import org.videolan.resources.HEADER_DIRECTORIES
import org.videolan.resources.HEADER_NETWORK
import org.videolan.resources.util.parcelable
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.television.ui.browser.VerticalGridActivity
import org.videolan.tools.HttpImageLoader
import org.videolan.tools.retrieveParent
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.DialogActivity
import org.videolan.vlc.gui.DialogActivity.Companion.EXTRA_MEDIA as DIALOG_EXTRA_MEDIA
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.helpers.AudioUtil
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.convertFavorites
import org.videolan.vlc.util.isSchemeFile
import org.videolan.vlc.util.isSchemeNetwork

private const val ID_PLAY = 1
private const val ID_NEXT_EPISODE = 2
private const val ID_LISTEN = 3
private const val ID_FAVORITE_ADD = 4
private const val ID_FAVORITE_DELETE = 5
private const val ID_BROWSE = 6
private const val ID_DL_SUBS = 7
private const val ID_PLAY_FROM_START = 8
private const val ID_PLAYLIST = 9
private const val ID_GET_INFO = 10
private const val ID_REMOVE_FROM_HISTORY = 12
private const val ID_NAVIGATE_PARENT = 13
private const val ID_FAVORITE_EDIT = 14
private const val ID_DELETE = 15
const val EXTRA_FROM_HISTORY = "from_history"
const val EXTRA_ITEM = "item"
const val EXTRA_MEDIA = "media"

class DetailsActivity : BaseTvActivity() {
    private val viewModel: MediaItemDetailsModel by viewModels()
    private lateinit var browserFavRepository: BrowserFavRepository
    private lateinit var mediaMetadataModel: MediaMetadataModel
    private var fromHistory by mutableStateOf(false)
    private var favoriteExists by mutableStateOf(false)
    private var mediaMetadata by mutableStateOf<MediaMetadataFull?>(null, neverEqualPolicy())
    private var nextEpisode by mutableStateOf<MediaMetadataWithImages?>(null, neverEqualPolicy())
    private var coverBitmap by mutableStateOf<Bitmap?>(null, neverEqualPolicy())
    private var backgroundBitmap by mutableStateOf<Bitmap?>(null, neverEqualPolicy())
    private var actions by mutableStateOf<List<DetailAction>>(emptyList(), neverEqualPolicy())

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browserFavRepository = BrowserFavRepository.getInstance(this)
        viewModel.mediaStarted = false

        val extras = intent.extras ?: savedInstanceState ?: run {
            finish()
            return
        }
        viewModel.mediaItemDetails = extras.parcelable(EXTRA_ITEM) ?: run {
            finish()
            return
        }
        val hasMedia = extras.containsKey(EXTRA_MEDIA)
        fromHistory = extras.getBoolean(EXTRA_FROM_HISTORY, false)
        val media = (extras.parcelable<Parcelable>(EXTRA_MEDIA)
            ?: MLServiceLocator.getAbstractMediaWrapper(AndroidUtil.LocationToUri(viewModel.mediaItemDetails.location))) as MediaWrapper

        viewModel.media = media
        if (!hasMedia) viewModel.media.setDisplayTitle(viewModel.mediaItemDetails.title)
        title = viewModel.media.title

        mediaMetadataModel = ViewModelProvider(this, MediaMetadataModel.Factory(this, mlId = media.id))[media.uri.path ?: "", MediaMetadataModel::class.java]
        mediaMetadataModel.updateLiveData.observe(this) {
            mediaMetadata = it
            title = it.metadata?.metadata?.title ?: viewModel.media.title
            loadBackdrop(it)
        }
        mediaMetadataModel.nextEpisode.observe(this) {
            nextEpisode = it
            rebuildActions()
        }
        viewModel.browserFavUpdated.observe(this) { newMedia ->
            startActivity(Intent(this, DetailsActivity::class.java).apply {
                putExtra(EXTRA_MEDIA, newMedia)
                putExtra(EXTRA_ITEM, MediaItemDetails(newMedia.title, newMedia.artistName, newMedia.albumName, newMedia.location, newMedia.artworkURL))
            })
            finish()
        }

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme(darkTheme = true) {
                        MediaItemDetailsScreen(
                            item = viewModel.mediaItemDetails,
                            media = viewModel.media,
                            metadataFull = mediaMetadata,
                            coverBitmap = coverBitmap,
                            backgroundBitmap = backgroundBitmap,
                            actions = actions,
                            onActionClick = ::handleAction,
                            onImageClick = mediaMetadataModel::updateMetadataImage
                        )
                    }
                }
            }
        )
        loadOverview()
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.mediaStarted) sendBroadcast(Intent(ACTION_REMOTE_STOP))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_ITEM, viewModel.mediaItemDetails)
        outState.putParcelable(EXTRA_MEDIA, viewModel.media)
        outState.putBoolean(EXTRA_FROM_HISTORY, fromHistory)
        super.onSaveInstanceState(outState)
    }

    override fun refresh() {}

    private fun loadOverview() {
        lifecycleScope.launch {
            val media = viewModel.media
            coverBitmap = when {
                media.type == MediaWrapper.TYPE_AUDIO || media.type == MediaWrapper.TYPE_VIDEO -> {
                    withContext(Dispatchers.IO) { AudioUtil.readCoverBitmap(viewModel.mediaItemDetails.artworkUrl, 512) }
                }
                media.type == MediaWrapper.TYPE_ALL -> {
                    withContext(Dispatchers.IO) { AudioUtil.fetchCoverBitmap(media.uri.toString(), 512) }
                }
                else -> null
            }
            favoriteExists = viewModel.mediaItemDetails.location?.toUri()?.let { browserFavRepository.browserFavExists(it) } ?: false
            if (mediaMetadata?.metadata?.metadata?.currentBackdrop.isNullOrEmpty()) loadBackdrop()
            rebuildActions()
        }
    }

    private fun rebuildActions() {
        if (!::browserFavRepository.isInitialized || !::mediaMetadataModel.isInitialized) return
        val media = viewModel.media
        val res = resources
        val next = nextEpisode
        actions = buildList {
            val isDir = media.type == MediaWrapper.TYPE_DIR
            if (isDir) {
                add(DetailAction(ID_BROWSE, res.getString(R.string.browse_folder)))
                val canSave = FileUtils.canSave(media)
                if (canSave) add(DetailAction(if (favoriteExists) ID_FAVORITE_DELETE else ID_FAVORITE_ADD, res.getString(if (favoriteExists) R.string.favorites_remove else R.string.favorites_add)))
                if (media.uri.scheme.isSchemeNetwork() && favoriteExists) add(DetailAction(ID_FAVORITE_EDIT, res.getString(R.string.favorites_edit)))
            } else if (media.type == MediaWrapper.TYPE_AUDIO) {
                addHistoryAndParentActions()
                add(DetailAction(ID_PLAY, res.getString(R.string.play)))
                add(DetailAction(ID_LISTEN, res.getString(R.string.listen)))
                add(DetailAction(ID_PLAYLIST, res.getString(R.string.add_to_playlist)))
                if (media.uri.scheme.isSchemeFile()) add(DetailAction(ID_DELETE, res.getString(R.string.delete)))
            } else if (media.type == MediaWrapper.TYPE_VIDEO) {
                addHistoryAndParentActions()
                add(DetailAction(ID_PLAY, res.getString(R.string.play)))
                if (next != null) add(DetailAction(ID_NEXT_EPISODE, res.getString(R.string.next_episode)))
                add(DetailAction(ID_PLAY_FROM_START, res.getString(R.string.play_from_start)))
                if (FileUtils.canWrite(media.uri)) add(DetailAction(ID_DL_SUBS, res.getString(R.string.download_subtitles)))
                add(DetailAction(ID_PLAYLIST, res.getString(R.string.add_to_playlist)))
                if (BuildConfig.DEBUG) add(DetailAction(ID_GET_INFO, res.getString(R.string.find_metadata)))
                if (media.uri.scheme.isSchemeFile()) add(DetailAction(ID_DELETE, res.getString(R.string.delete)))
            } else if (media.type == MediaWrapper.TYPE_ALL) {
                if (media.uri.retrieveParent() != null) add(DetailAction(ID_NAVIGATE_PARENT, res.getString(R.string.go_to_folder)))
            }
        }
    }

    private fun MutableList<DetailAction>.addHistoryAndParentActions() {
        val media = viewModel.media
        val res = resources
        if (fromHistory) add(DetailAction(ID_REMOVE_FROM_HISTORY, res.getString(R.string.remove_from_history)))
        if (media.uri.retrieveParent() != null) add(DetailAction(ID_NAVIGATE_PARENT, res.getString(R.string.go_to_folder)))
    }

    private fun handleAction(action: DetailAction) {
        when (action.id) {
            ID_LISTEN -> {
                MediaUtils.openMedia(this, viewModel.media)
                viewModel.mediaStarted = true
            }
            ID_PLAY -> {
                viewModel.mediaStarted = false
                TvUtil.playMedia(this, viewModel.media)
                finish()
            }
            ID_REMOVE_FROM_HISTORY -> {
                lifecycleScope.launch {
                    fromHistory = withContext(Dispatchers.IO) { !viewModel.media.removeFromHistory() }
                    rebuildActions()
                }
            }
            ID_NAVIGATE_PARENT -> {
                viewModel.media.uri.retrieveParent()?.let { item ->
                    startActivity(Intent(this, VerticalGridActivity::class.java).apply {
                        putExtra(MainTvActivity.BROWSER_TYPE, if ("file" == item.scheme) HEADER_DIRECTORIES else HEADER_NETWORK)
                        putExtra(FAVORITE_TITLE, item.lastPathSegment)
                        data = item
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
            ID_DELETE -> delete()
            ID_PLAYLIST -> addToPlaylist(arrayListOf(viewModel.media))
            ID_FAVORITE_ADD -> addFavorite()
            ID_FAVORITE_EDIT -> {
                viewModel.listenForNetworkFav = true
                startActivity(Intent(this, DialogActivity::class.java).setAction(DialogActivity.KEY_SERVER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).apply {
                    putExtra(DIALOG_EXTRA_MEDIA, viewModel.media)
                })
            }
            ID_FAVORITE_DELETE -> deleteFavorite()
            ID_BROWSE -> TvUtil.openMedia(this, viewModel.media)
            ID_DL_SUBS -> MediaUtils.getSubs(this, viewModel.media)
            ID_PLAY_FROM_START -> {
                viewModel.mediaStarted = false
                VideoPlayerActivity.start(this, viewModel.media.uri, true)
                finish()
            }
            ID_GET_INFO -> startActivity(Intent(this, MediaScrapingTvActivity::class.java).apply { putExtra(MediaScrapingTvActivity.MEDIA, viewModel.media) })
            ID_NEXT_EPISODE -> nextEpisode?.media?.let {
                TvUtil.showMediaDetail(this, it)
                finish()
            }
        }
    }

    private fun addFavorite() {
        val uri = viewModel.mediaItemDetails.location?.toUri() ?: return
        val local = "file" == uri.scheme
        lifecycleScope.launch {
            if (local) {
                browserFavRepository.addLocalFavItem(uri, viewModel.mediaItemDetails.title.orEmpty(), viewModel.mediaItemDetails.artworkUrl)
            } else {
                browserFavRepository.addNetworkFavItem(uri, viewModel.mediaItemDetails.title.orEmpty(), viewModel.mediaItemDetails.artworkUrl)
            }
            favoriteExists = true
            rebuildActions()
            Toast.makeText(this@DetailsActivity, R.string.favorite_added, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFavorite() {
        val uri = viewModel.mediaItemDetails.location?.toUri() ?: return
        lifecycleScope.launch {
            browserFavRepository.deleteBrowserFav(uri)
            favoriteExists = false
            rebuildActions()
            Toast.makeText(this@DetailsActivity, R.string.favorite_removed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun delete() {
        if (!Permissions.canWriteStorage(this)) {
            Permissions.askWriteStoragePermission(this, false) {
                delete()
            }
            return
        }
        showConfirmDeleteComposeDialog(arrayListOf(viewModel.media), listener = ::deleteConfirmed)
    }

    private fun deleteConfirmed() {
        var preventFinish = false
        MediaUtils.deleteItem(this, viewModel.media) {
            onDeleteFailed(it)
            preventFinish = true
        }
        if (!preventFinish) finish()
    }

    private fun onDeleteFailed(item: MediaLibraryItem) {
        Toast.makeText(this, getString(R.string.msg_delete_failed, item.title), Toast.LENGTH_LONG).show()
    }

    private fun loadBackdrop(mediaMetadataFull: MediaMetadataFull? = null) {
        val backdrop = mediaMetadataFull?.metadata?.metadata?.currentBackdrop
        lifecycleScope.launch {
            backgroundBitmap = withContext(Dispatchers.IO) {
                when {
                    !backdrop.isNullOrEmpty() -> HttpImageLoader.downloadBitmap(backdrop)?.let { UiTools.blurBitmap(it) }
                    coverBitmap != null -> UiTools.blurBitmap(coverBitmap!!)
                    viewModel.media.type == MediaWrapper.TYPE_AUDIO || viewModel.media.type == MediaWrapper.TYPE_VIDEO -> {
                        AudioUtil.readCoverBitmap(viewModel.mediaItemDetails.artworkUrl, 512)?.let { UiTools.blurBitmap(it) }
                    }
                    else -> null
                }
            }
        }
    }
}

private data class DetailAction(val id: Int, val label: String)

@Composable
private fun MediaItemDetailsScreen(
    item: MediaItemDetails,
    media: MediaWrapper,
    metadataFull: MediaMetadataFull?,
    coverBitmap: Bitmap?,
    backgroundBitmap: Bitmap?,
    actions: List<DetailAction>,
    onActionClick: (DetailAction) -> Unit,
    onImageClick: (MediaImage) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val metadataWithImages = metadataFull?.metadata

    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            backgroundBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to colors.backgroundDefault.copy(alpha = 0.60f),
                            0.48f to colors.backgroundDefault.copy(alpha = 0.90f),
                            1f to colors.backgroundDefault
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 56.dp, top = 48.dp, end = 56.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item(key = "overview") {
                    MediaItemOverview(
                        item = item,
                        media = media,
                        metadataWithImages = metadataWithImages,
                        coverBitmap = coverBitmap,
                        actions = actions,
                        onActionClick = onActionClick
                    )
                }

                metadataFull?.let { details ->
                    peopleSection(R.string.written_by, details.writers.orEmpty())
                    peopleSection(R.string.casting, details.actors.orEmpty())
                    peopleSection(R.string.directed_by, details.directors.orEmpty())
                    peopleSection(R.string.produced_by, details.producers.orEmpty())
                    peopleSection(R.string.music_by, details.musicians.orEmpty())
                    metadataWithImages?.images.orEmpty().filter { it.imageType == MediaImageType.POSTER }.takeIf { it.isNotEmpty() }?.let { posters ->
                        item(key = "posters") {
                            DetailImageSection(
                                title = R.string.posters,
                                images = posters,
                                poster = true,
                                onImageClick = onImageClick
                            )
                        }
                    }
                    metadataWithImages?.images.orEmpty().filter { it.imageType == MediaImageType.BACKDROP }.takeIf { it.isNotEmpty() }?.let { backdrops ->
                        item(key = "backdrops") {
                            DetailImageSection(
                                title = R.string.backdrops,
                                images = backdrops,
                                poster = false,
                                onImageClick = onImageClick
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.peopleSection(title: Int, people: List<Person>) {
    if (people.isEmpty()) return
    item(key = "people-$title") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailSectionHeader(title)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(people, key = { it.moviepediaId }) { person ->
                    DetailPersonCard(person)
                }
            }
        }
    }
}

@Composable
private fun MediaItemOverview(
    item: MediaItemDetails,
    media: MediaWrapper,
    metadataWithImages: MediaMetadataWithImages?,
    coverBitmap: Bitmap?,
    actions: List<DetailAction>,
    onActionClick: (DetailAction) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val metadata = metadataWithImages?.metadata
    val title = metadata?.title ?: item.title.orEmpty()
    val subtitle = metadataWithImages?.subtitle() ?: item.subTitle.orEmpty()
    val body = when {
        metadata != null -> metadata.summary
        item.body == null -> Uri.decode(item.location.orEmpty())
        else -> item.body + "\n" + Uri.decode(item.location.orEmpty())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.Top
    ) {
        OverviewArtwork(
            media = media,
            posterUrl = metadata?.currentPoster,
            coverBitmap = coverBitmap,
            modifier = Modifier.width(220.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.fontLight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (body.isNotEmpty()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.fontDefault,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 760.dp)
                )
            }
            if (actions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(actions, key = { it.id }) { action ->
                        DetailActionButton(action, onClick = { onActionClick(action) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewArtwork(
    media: MediaWrapper,
    posterUrl: String?,
    coverBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val placeholder = when {
        media.type == MediaWrapper.TYPE_DIR && media.uri.scheme == "file" -> R.drawable.ic_folder_big
        media.type == MediaWrapper.TYPE_DIR -> R.drawable.ic_network_big
        else -> R.drawable.ic_default_cone
    }
    Box(
        modifier = modifier
            .width(220.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(4.dp))
            .background(VLCThemeDefaults.colors.cardBackground),
        contentAlignment = Alignment.Center
    ) {
        when {
            !posterUrl.isNullOrEmpty() -> DetailRemoteArtwork(
                imageUrl = posterUrl,
                placeholder = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            coverBitmap != null -> Image(
                bitmap = coverBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Icon(
                painter = painterResource(placeholder),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

@Composable
private fun DetailActionButton(action: DetailAction, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = VLCThemeDefaults.colors.primary,
            contentColor = Color.White
        ),
        modifier = Modifier.height(44.dp)
    ) {
        Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailImageSection(
    title: Int,
    images: List<MediaImage>,
    poster: Boolean,
    onImageClick: (MediaImage) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailSectionHeader(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(images, key = { "${it.mediaId}-${it.url}" }) { image ->
                DetailImageCard(
                    image = image,
                    poster = poster,
                    onClick = { onImageClick(image) }
                )
            }
        }
    }
}

@Composable
private fun DetailPersonCard(person: Person) {
    Surface(
        color = VLCThemeDefaults.colors.cardBackground.copy(alpha = 0.94f),
        contentColor = VLCThemeDefaults.colors.fontDefault,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, VLCThemeDefaults.colors.cardBorder),
        modifier = Modifier
            .width(128.dp)
            .focusable()
    ) {
        Column {
            DetailRemoteArtwork(
                imageUrl = person.image,
                placeholder = R.drawable.ic_people_big,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun DetailImageCard(image: MediaImage, poster: Boolean, onClick: () -> Unit) {
    Surface(
        color = VLCThemeDefaults.colors.cardBackground.copy(alpha = 0.94f),
        contentColor = VLCThemeDefaults.colors.fontDefault,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, VLCThemeDefaults.colors.cardBorder),
        modifier = Modifier
            .width(if (poster) 112.dp else 224.dp)
            .focusable()
            .clickable(onClick = onClick)
    ) {
        DetailRemoteArtwork(
            imageUrl = image.url,
            placeholder = if (poster) R.drawable.ic_video_big else R.drawable.ic_default_cone,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (poster) 2f / 3f else 16f / 9f)
        )
    }
}

@Composable
private fun DetailSectionHeader(title: Int) {
    Text(
        text = androidx.compose.ui.res.stringResource(title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = VLCThemeDefaults.colors.fontDefault
    )
}

@Composable
private fun DetailRemoteArtwork(
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

class MediaItemDetailsModel(context: Application) : AndroidViewModel(context), CoroutineScope by MainScope() {
    lateinit var mediaItemDetails: MediaItemDetails
    lateinit var media: MediaWrapper
    var mediaStarted = false
    private val repository = BrowserFavRepository.getInstance(context)
    val browserFavUpdated: MediatorLiveData<MediaWrapper> = MediatorLiveData()
    private val oldList = ArrayList<MediaWrapper>()
    var listenForNetworkFav = false

    @OptIn(ObsoleteCoroutinesApi::class)
    private val updateActor = actor<MediaWrapper>(capacity = Channel.CONFLATED) {
        for (entry in channel) {
            browserFavUpdated.value = entry
        }
    }

    init {
        browserFavUpdated.addSource(repository.networkFavs.asLiveData()) { favList ->
            val convertFavorites = convertFavorites(favList)
            if (oldList.isEmpty()) oldList.addAll(convertFavorites)
            if (listenForNetworkFav) {
                convertFavorites.forEach { media ->
                    if (oldList.none { it.uri == media.uri && it.title == media.title }) {
                        oldList.clear()
                        oldList.addAll(convertFavorites)
                        listenForNetworkFav = false
                        updateActor.trySend(media)
                    }
                }
            }
        }
    }
}
