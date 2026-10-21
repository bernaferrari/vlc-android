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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
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
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.VLCTvAction
import org.videolan.vlc.compose.tv.VLCTvCardShape
import org.videolan.vlc.compose.tv.VLCTvDetailsScreen
import org.videolan.vlc.compose.tv.VLCTvShelfModel
import org.videolan.vlc.compose.icons.ExtraSymbols
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.util.generateResolutionClass
import org.videolan.medialibrary.Tools
import org.videolan.moviepedia.database.models.getYear
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
                    VLCTvTheme {
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

private fun DetailAction.symbol(): MaterialIcon = when (id) {
    ID_PLAY, ID_LISTEN -> MaterialSymbols.Filled.PlayArrow
    ID_NEXT_EPISODE -> MaterialSymbols.Filled.SkipNext
    ID_PLAY_FROM_START -> MaterialSymbols.Filled.Refresh
    ID_FAVORITE_ADD -> MaterialSymbols.Outlined.Star
    ID_FAVORITE_DELETE -> MaterialSymbols.Filled.Star
    ID_FAVORITE_EDIT -> MaterialSymbols.Filled.Edit
    ID_BROWSE -> MaterialSymbols.Filled.Folder
    ID_DL_SUBS -> ExtraSymbols.Subtitles
    ID_PLAYLIST -> MaterialSymbols.Filled.QueueMusic
    ID_GET_INFO -> MaterialSymbols.Filled.Search
    ID_REMOVE_FROM_HISTORY -> MaterialSymbols.Filled.History
    ID_NAVIGATE_PARENT -> MaterialSymbols.Filled.Folder
    ID_DELETE -> MaterialSymbols.Filled.Delete
    else -> MaterialSymbols.Filled.PlayArrow
}

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
    val metadataWithImages = metadataFull?.metadata
    val metadata = metadataWithImages?.metadata
    val title = metadata?.title ?: item.title.orEmpty()
    val subtitle = metadataWithImages?.subtitle() ?: item.subTitle
    val location = Uri.decode(item.location.orEmpty())
    val body = when {
        metadata != null -> metadata.summary
        item.body.isNullOrBlank() -> location
        else -> item.body + "\n" + location
    }
    val eyebrow = stringResource(
        when (media.type) {
            MediaWrapper.TYPE_VIDEO -> R.string.video
            MediaWrapper.TYPE_AUDIO -> R.string.audio
            MediaWrapper.TYPE_DIR -> if (media.uri.scheme == "file") R.string.directories else R.string.network_browsing
            else -> R.string.app_name
        }
    )
    val chips = buildList {
        metadata?.getYear()?.takeIf { it.isNotBlank() }?.let(::add)
        if (media.length > 0) add(Tools.millisToString(media.length))
        if (media.type == MediaWrapper.TYPE_VIDEO) generateResolutionClass(media.width, media.height)?.let(::add)
    }
    val tvActions = remember(actions) { actions.mapIndexed { index, action -> VLCTvAction(action.id, action.label, action.symbol(), primary = index == 0) } }
    val context = androidx.compose.ui.platform.LocalContext.current
    val shelves = remember(metadataFull) { metadataFull?.let { detailShelves(context, it) }.orEmpty() }
    val posterUrl = metadata?.currentPoster
    val shape = when {
        !posterUrl.isNullOrEmpty() -> VLCTvCardShape.Poster
        media.type == MediaWrapper.TYPE_VIDEO -> VLCTvCardShape.Landscape
        else -> VLCTvCardShape.Square
    }
    VLCTvDetailsScreen(
        title = title,
        eyebrow = eyebrow,
        subtitle = subtitle,
        metadata = chips,
        description = body,
        actions = tvActions,
        onAction = { action -> actions.firstOrNull { it.id == action.id }?.let(onActionClick) },
        artworkShape = shape,
        shelves = shelves.map { it.first },
        onShelfItemClick = { shelf, index -> (shelves.getOrNull(shelf)?.second?.get(index) as? MediaImage)?.let(onImageClick) },
        backdrop = backgroundBitmap?.let { bitmap ->
            { Image(bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        },
        heroArtwork = {
            if (!posterUrl.isNullOrEmpty()) TvRemoteArtworkImage(posterUrl, media.tvSymbol())
            else TvArtworkImage(coverBitmap, media.tvSymbol())
        },
        artwork = { tvItem -> TvSourceArtwork(shelves.firstNotNullOfOrNull { it.second.sourcesByKey[tvItem.key] }, tvItem) },
    )
}

/** Cast and artwork choices shared by media and TV show details. */
internal fun detailShelves(context: android.content.Context, details: MediaMetadataFull): List<Pair<VLCTvShelfModel, TvEntries<Any>>> {
    val shelves = mutableListOf<Pair<VLCTvShelfModel, TvEntries<Any>>>()
    fun people(title: Int, people: List<Person>?) {
        if (people.isNullOrEmpty()) return
        val entries = TvEntries<Any>(people) { (it as Person).toTvItem() }
        shelves += VLCTvShelfModel("people-$title", context.getString(title), entries.items) to entries
    }
    people(R.string.written_by, details.writers)
    people(R.string.casting, details.actors)
    people(R.string.directed_by, details.directors)
    people(R.string.produced_by, details.producers)
    people(R.string.music_by, details.musicians)
    val images = details.metadata?.images.orEmpty()
    listOf(MediaImageType.POSTER to R.string.posters, MediaImageType.BACKDROP to R.string.backdrops).forEach { (type, title) ->
        val list = images.filter { it.imageType == type }
        if (list.isNotEmpty()) {
            val entries = TvEntries<Any>(list) { (it as MediaImage).toTvItem() }
            shelves += VLCTvShelfModel("images-$title", context.getString(title), entries.items) to entries
        }
    }
    return shelves
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
