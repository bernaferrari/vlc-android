/*****************************************************************************
 * MainTvActivity.java
 *
 * Copyright © 2014-2018 VLC authors, VideoLAN and VideoLabs
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
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.leanback.app.BackgroundManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Folder
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.DummyItem
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
import org.videolan.moviepedia.database.models.tvEpisodeSubtitle
import org.videolan.resources.AndroidDevices
import org.videolan.resources.CATEGORY
import org.videolan.resources.CATEGORY_NOW_PLAYING
import org.videolan.resources.CATEGORY_NOW_PLAYING_PAUSED
import org.videolan.resources.CATEGORY_NOW_PLAYING_PIP
import org.videolan.resources.CATEGORY_NOW_PLAYING_PIP_PAUSED
import org.videolan.resources.HEADER_CATEGORIES
import org.videolan.resources.HEADER_HISTORY
import org.videolan.resources.HEADER_MISC
import org.videolan.resources.HEADER_NETWORK
import org.videolan.resources.HEADER_NOW_PLAYING
import org.videolan.resources.HEADER_PERMISSION
import org.videolan.resources.HEADER_PLAYLISTS
import org.videolan.resources.HEADER_RECENTLY_ADDED
import org.videolan.resources.HEADER_RECENTLY_PLAYED
import org.videolan.resources.HEADER_VIDEO
import org.videolan.resources.ID_ABOUT_TV
import org.videolan.resources.ID_PIN_LOCK
import org.videolan.resources.ID_REFRESH
import org.videolan.resources.ID_REMOTE_ACCESS
import org.videolan.resources.ID_SETTINGS
import org.videolan.resources.ID_SPONSOR
import org.videolan.television.ui.audioplayer.AudioPlayerActivity
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.television.ui.browser.VerticalGridActivity
import org.videolan.television.ui.preferences.PreferencesActivity
import org.videolan.television.viewmodel.MainTvModel
import org.videolan.tools.KEY_SHOW_UPDATE
import org.videolan.tools.HttpImageLoader
import org.videolan.tools.RESULT_RESCAN
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.Settings
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.RecommendationsService
import org.videolan.vlc.ScanProgress
import org.videolan.vlc.StartActivity
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.dialogs.showUpdateComposeDialog
import org.videolan.vlc.gui.helpers.UiTools.showDonations
import org.videolan.vlc.gui.helpers.getTvIconRes
import org.videolan.vlc.gui.helpers.hf.PinCodeDelegate
import org.videolan.vlc.gui.helpers.hf.StoragePermissionsDelegate
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.AutoUpdate
import org.videolan.vlc.util.LifecycleAwareScheduler
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.SchedulerCallback
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.Util
import org.videolan.vlc.util.isSchemeHttpOrHttps
import org.videolan.vlc.R as VlcR

private data class HomeRow(
    val id: Long,
    val title: String,
    val items: List<Any>,
    val poster: Boolean = false
)

private data class GenericCardItem(
    val id: Long,
    val title: String,
    val content: String,
    val icon: Int,
    val color: Int
)

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class MainTvActivity : BaseTvActivity(), StoragePermissionsDelegate.CustomActionController, SchedulerCallback, PlaybackService.Callback {

    private lateinit var model: MainTvModel
    lateinit var scheduler: LifecycleAwareScheduler

    private var backgroundManager: BackgroundManager? = null
    private var selectedItem: Any? = null
    private var service: PlaybackService? = null

    private var nowPlayingItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var recentlyPlayedItems by mutableStateOf<List<MediaMetadataWithImages>>(emptyList())
    private var recentlyAddedItems by mutableStateOf<List<MediaMetadataWithImages>>(emptyList())
    private var videoItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var audioCategoryItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var historyItems by mutableStateOf<List<MediaWrapper>>(emptyList())
    private var playlistItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var favoriteItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var browserItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var pinUnlocked by mutableStateOf(PinCodeDelegate.pinUnlocked.value == true)
    private var remoteAccessEnabled by mutableStateOf(Settings.remoteAccessEnabled.value == true)
    private var incognitoMode by mutableStateOf(Settings.incognitoMode)
    private var loadingVisible by mutableStateOf(false)

    override fun onTaskTriggered(id: String, data: Bundle) {
        when (id) {
            SHOW_LOADING -> loadingVisible = true
            HIDE_LOADING -> {
                scheduler.cancelAction(SHOW_LOADING)
                loadingVisible = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduler = LifecycleAwareScheduler(this)
        model = ViewModelProvider(this, MainTvModel.Factory(application))[MainTvModel::class.java]

        Util.checkCpuCompatibility(this)

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme(darkTheme = true) {
                        MainTvHomeScreen(
                            rows = homeRows(),
                            appName = getString(VlcR.string.app_name),
                            incognitoMode = incognitoMode,
                            showSearch = AndroidDevices.hasPlayServices,
                            loading = loadingVisible,
                            onSearch = ::openSearch,
                            onItemFocused = ::onItemFocused,
                            onItemClicked = ::onItemClicked
                        )
                    }
                }
            }
        )
        backgroundManager = BackgroundManager.getInstance(this).apply { attach(window) }

        registerDatasets()
        PlaybackService.serviceFlow.onEach { onServiceChanged(it) }.launchIn(lifecycleScope)

        lifecycleScope.launch {
            AutoUpdate.clean(this@MainTvActivity.application)
            if (!Settings.getInstance(this@MainTvActivity).getBoolean(KEY_SHOW_UPDATE, true)) return@launch
            AutoUpdate.checkUpdate(this@MainTvActivity.application) { url, date ->
                showUpdateComposeDialog(url, date)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (selectedItem is MediaWrapper) lifecycleScope.updateBackground(this, backgroundManager, selectedItem)
        model.refresh()
    }

    override fun onResume() {
        super.onResume()
        incognitoMode = Settings.incognitoMode
    }

    override fun onStop() {
        super.onStop()
        if (AndroidDevices.isAndroidTv && !org.videolan.libvlc.util.AndroidUtil.isOOrLater) {
            startService(Intent(this, RecommendationsService::class.java))
        }
    }

    override fun onDestroy() {
        service?.removeCallback(this)
        service = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_RESULT_PREFERENCES) {
            when (resultCode) {
                RESULT_RESCAN -> this.reloadLibrary()
                RESULT_RESTART, RESULT_RESTART_APP -> {
                    val intent = Intent(this, if (resultCode == RESULT_RESTART_APP) StartActivity::class.java else MainTvActivity::class.java)
                    finish()
                    startActivity(intent)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
            showDetails()
        } else super.onKeyDown(keyCode, event)
    }

    override fun onParsingServiceStarted() {
        scheduler.startAction(SHOW_LOADING)
    }

    override fun onParsingServiceProgress(scanProgress: ScanProgress?) {
        if (!loadingVisible && Medialibrary.getInstance().isWorking)
            scheduler.startAction(SHOW_LOADING)
    }

    override fun onParsingServiceFinished() {
        if (!Medialibrary.getInstance().isWorking)
            scheduler.scheduleAction(HIDE_LOADING, 500)
    }

    override fun onStorageAccessGranted() {
        refresh()
    }

    override fun refresh() {
        this.reloadLibrary()
    }

    override fun update() {
    }

    override fun onMediaEvent(event: IMedia.Event) {
    }

    override fun onMediaPlayerEvent(event: MediaPlayer.Event) {
        model.updateNowPlaying()
    }

    private fun registerDatasets() {
        model.nowPlaying.observe(this) { nowPlayingItems = it.orEmpty() }
        model.recentlyPlayed.observe(this) { recentlyPlayedItems = it.orEmpty() }
        model.recentlyAdded.observe(this) { recentlyAddedItems = it.orEmpty() }
        model.videos.observe(this) { videoItems = it.orEmpty() }
        model.audioCategories.observe(this) { audioCategoryItems = it.orEmpty() }
        model.history.observe(this) { historyItems = it.orEmpty() }
        model.playlist.observe(this) { playlistItems = it.orEmpty() }
        model.favoritesList.observe(this) { favoriteItems = it.orEmpty() }
        model.browsers.observe(this) { browserItems = it.orEmpty() }
        PinCodeDelegate.pinUnlocked.observe(this) { pinUnlocked = it == true }
        Settings.remoteAccessEnabled.observe(this) { remoteAccessEnabled = it == true }
    }

    private fun onServiceChanged(newService: PlaybackService?) {
        if (newService === service) return
        service?.removeCallback(this)
        service = newService
        newService?.addCallback(this)
        model.updateNowPlaying()
    }

    private fun homeRows(): List<HomeRow> = buildList {
        if (nowPlayingItems.isNotEmpty()) add(HomeRow(HEADER_NOW_PLAYING, getString(VlcR.string.music_now_playing), nowPlayingItems))
        if (recentlyPlayedItems.isNotEmpty()) add(HomeRow(HEADER_RECENTLY_PLAYED, getString(VlcR.string.recently_played), recentlyPlayedItems, poster = true))
        if (recentlyAddedItems.isNotEmpty()) add(HomeRow(HEADER_RECENTLY_ADDED, getString(VlcR.string.recently_added), recentlyAddedItems, poster = true))
        add(HomeRow(HEADER_VIDEO, getString(VlcR.string.video), videoItems))
        add(HomeRow(HEADER_CATEGORIES, getString(VlcR.string.audio), audioCategoryItems))
        if (playlistItems.isNotEmpty()) add(HomeRow(HEADER_PLAYLISTS, getString(VlcR.string.playlists), playlistItems))
        if (historyItems.isNotEmpty()) add(HomeRow(HEADER_HISTORY, getString(VlcR.string.history), historyItems))
        if (favoriteItems.isNotEmpty()) add(HomeRow(HEADER_PLAYLISTS, getString(VlcR.string.favorites), favoriteItems))
        add(HomeRow(HEADER_NETWORK, getString(VlcR.string.browsing), browserItems))
        add(HomeRow(HEADER_MISC, getString(VlcR.string.other), miscItems()))
    }

    private fun miscItems(): List<GenericCardItem> = buildList {
        if (pinUnlocked) add(GenericCardItem(ID_PIN_LOCK, getString(VlcR.string.lock_with_pin_short), "", VlcR.drawable.ic_pin_lock_big, VlcR.color.tv_card_content_dark))
        add(GenericCardItem(ID_SETTINGS, getString(VlcR.string.preferences), "", VlcR.drawable.ic_settings_big, VlcR.color.tv_card_content_dark))
        if (remoteAccessEnabled) add(GenericCardItem(ID_REMOTE_ACCESS, getString(VlcR.string.remote_access), "", VlcR.drawable.ic_remote_access_big, VlcR.color.tv_card_content_dark))
        if (Permissions.canReadStorage(this@MainTvActivity)) add(GenericCardItem(ID_REFRESH, getString(VlcR.string.refresh), "", VlcR.drawable.ic_scan_big, VlcR.color.tv_card_content_dark))
        add(GenericCardItem(ID_ABOUT_TV, getString(VlcR.string.about), "${getString(VlcR.string.app_name_full)} ${BuildConfig.VLC_VERSION_NAME}", VlcR.drawable.ic_info_big, VlcR.color.tv_card_content_dark))
    }

    private fun openSearch() {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    private fun onItemFocused(item: Any) {
        selectedItem = item
        lifecycleScope.updateBackground(this, backgroundManager, item)
    }

    private fun onItemClicked(rowId: Long, item: Any) {
        when (rowId) {
            HEADER_CATEGORIES -> {
                val dummyItem = item as? DummyItem ?: return
                if (dummyItem.id == HEADER_PERMISSION) {
                    model.open(this, item)
                } else {
                    val intent = Intent(this, VerticalGridActivity::class.java)
                    intent.putExtra(BROWSER_TYPE, HEADER_CATEGORIES)
                    intent.putExtra(CATEGORY, dummyItem.id)
                    startActivity(intent)
                }
            }
            HEADER_MISC -> {
                when ((item as? GenericCardItem)?.id) {
                    ID_SETTINGS -> startActivityForResult(Intent(this, PreferencesActivity::class.java), ACTIVITY_RESULT_PREFERENCES)
                    ID_REFRESH -> if (!Medialibrary.getInstance().isWorking) reloadLibrary()
                    ID_ABOUT_TV -> startActivity(Intent(this, AboutActivity::class.java))
                    ID_SPONSOR -> showDonations()
                    ID_PIN_LOCK -> PinCodeDelegate.pinUnlocked.postValue(false)
                    ID_REMOTE_ACCESS -> startActivity(Intent(this, StartActivity::class.java).apply { action = "vlc.remoteaccess.share" })
                }
            }
            HEADER_NOW_PLAYING -> {
                val dummyItem = item as? DummyItem ?: return
                when (dummyItem.id) {
                    CATEGORY_NOW_PLAYING, CATEGORY_NOW_PLAYING_PAUSED -> startActivity(Intent(this, AudioPlayerActivity::class.java))
                    CATEGORY_NOW_PLAYING_PIP, CATEGORY_NOW_PLAYING_PIP_PAUSED -> startActivity(Intent(this, VideoPlayerActivity::class.java))
                }
            }
            else -> model.open(this, item)
        }
    }

    private fun showDetails(): Boolean {
        val media = selectedItem as? MediaWrapper ?: return false
        if (media.type != MediaWrapper.TYPE_DIR) return false
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra(EXTRA_MEDIA, media)
        intent.putExtra(EXTRA_ITEM, MediaItemDetails(media.title, media.artistName, media.albumName, media.location, media.artworkURL))
        startActivity(intent)
        return true
    }

    companion object {

        const val ACTIVITY_RESULT_PREFERENCES = 1

        const val BROWSER_TYPE = "browser_type"

        const val TAG = "VLC/MainTvActivity"
        private const val SHOW_LOADING = "show_loading"
        private const val HIDE_LOADING = "hide_loading"
    }
}

@Composable
private fun MainTvHomeScreen(
    rows: List<HomeRow>,
    appName: String,
    incognitoMode: Boolean,
    showSearch: Boolean,
    loading: Boolean,
    onSearch: () -> Unit,
    onItemFocused: (Any) -> Unit,
    onItemClicked: (Long, Any) -> Unit
) {
    val firstFocusRequester = remember { FocusRequester() }
    var firstFocusRequested by remember { mutableStateOf(false) }
    val contentRowsReady = rows.any { it.id != HEADER_MISC && it.items.isNotEmpty() }
    val firstFocusableKey = rows.firstOrNull { it.id != HEADER_MISC && it.items.isNotEmpty() }
        ?.items
        ?.firstOrNull()
        ?.homeKey()

    LaunchedEffect(firstFocusableKey) {
        if (!firstFocusRequested && contentRowsReady && firstFocusableKey != null) {
            firstFocusRequester.requestFocus()
            firstFocusRequested = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF2011422),
                        Color(0xE6011422),
                        Color(0xF2011422)
                    )
                )
            )
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 56.dp, top = 32.dp, end = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                MainTvHeader(
                    appName = appName,
                    incognitoMode = incognitoMode,
                    showSearch = showSearch,
                    onSearch = onSearch
                )
            }
            items(rows, key = { "${it.id}-${it.title}" }) { row ->
                MainTvRow(
                    row = row,
                    firstFocusRequester = firstFocusRequester,
                    firstFocusableKey = firstFocusableKey,
                    onItemFocused = onItemFocused,
                    onItemClicked = onItemClicked
                )
            }
        }
        if (loading) {
            CircularProgressIndicator(
                color = VLCThemeDefaults.colors.primary,
                trackColor = Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 128.dp)
                    .size(36.dp)
            )
        }
    }
}

@Composable
private fun MainTvHeader(
    appName: String,
    incognitoMode: Boolean,
    showSearch: Boolean,
    onSearch: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(if (incognitoMode) VlcR.drawable.ic_incognito else VlcR.drawable.icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = appName,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showSearch) {
            IconButton(onClick = onSearch) {
                Icon(
                    painter = painterResource(VlcR.drawable.ic_search),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MainTvRow(
    row: HomeRow,
    firstFocusRequester: FocusRequester,
    firstFocusableKey: String?,
    onItemFocused: (Any) -> Unit,
    onItemClicked: (Long, Any) -> Unit
) {
    Column {
        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            itemsIndexed(
                items = row.items,
                key = { index, item -> "${row.id}-$index-${item.homeKey()}" }
            ) { _, item ->
                val firstCardModifier = if (item.homeKey() == firstFocusableKey) Modifier.focusRequester(firstFocusRequester) else Modifier
                MainTvCard(
                    item = item,
                    poster = row.poster,
                    onFocused = onItemFocused,
                    onClick = { onItemClicked(row.id, item) },
                    modifier = firstCardModifier
                )
            }
        }
    }
}

@Composable
private fun MainTvCard(
    item: Any,
    poster: Boolean,
    onFocused: (Any) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val colors = VLCThemeDefaults.colors
    val shape = RoundedCornerShape(4.dp)
    val cardWidth = if (poster) 128.dp else 192.dp
    val imageHeight = if (poster) 184.dp else 120.dp
    val cardHeight = imageHeight + 68.dp
    val background = if (focused) Color(0xFF34434E) else Color(0xFF1A2C38)

    Column(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused) 3.dp else 1.dp, if (focused) colors.primary else Color.White.copy(alpha = 0.08f)),
                shape
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(item)
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .background(Color.Black.copy(alpha = 0.24f))
        ) {
            MainTvCardImage(item = item)
            MainTvNowPlayingBadge(item = item)
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = item.homeTitle(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.homeSubtitle(),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MainTvCardImage(item: Any) {
    when (item) {
        is GenericCardItem -> Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(58.dp)
        )
        is MediaMetadataWithImages -> MainTvRemotePoster(item.metadata.currentPoster)
        is MediaLibraryItem -> MainTvMediaArtwork(item)
        else -> Icon(
            painter = painterResource(VlcR.drawable.ic_default_cone),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(58.dp)
        )
    }
}

@Composable
private fun MainTvRemotePoster(imageUrl: String) {
    val imageUri = remember(imageUrl) { imageUrl.toUri() }
    val bitmap by mainTvRemoteBitmap(imageUri)
    MainTvArtworkImage(bitmap = bitmap, fallback = VlcR.drawable.ic_people_big)
}

@Composable
private fun MainTvMediaArtwork(item: MediaLibraryItem) {
    val context = LocalContext.current
    val imageWidth = remember(context) {
        context.resources.getDimensionPixelSize(VlcR.dimen.tv_grid_card_thumb_width)
    }
    val bitmap by produceState<Bitmap?>(initialValue = null, item, imageWidth, Settings.showVideoThumbs) {
        value = null
        value = if (item.shouldUseFallbackArtworkOnly()) {
            null
        } else {
            ThumbnailsProvider.obtainBitmap(item, imageWidth)
        }
    }

    MainTvArtworkImage(bitmap = bitmap, fallback = getTvIconRes(item))
}

@Composable
private fun mainTvRemoteBitmap(imageUri: Uri?) = produceState<Bitmap?>(initialValue = null, imageUri) {
    val url = imageUri?.toString()
    value = null
    value = if (!url.isNullOrEmpty() && isSchemeHttpOrHttps(imageUri.scheme)) {
        HttpImageLoader.downloadBitmap(url)
    } else {
        null
    }
}

@Composable
private fun MainTvArtworkImage(bitmap: Bitmap?, fallback: Int) {
    val cover = bitmap
    if (cover == null) {
        Image(
            painter = painterResource(fallback),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Image(
            bitmap = cover.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun MediaLibraryItem.shouldUseFallbackArtworkOnly(): Boolean {
    if (Settings.showVideoThumbs) return false
    val isVideoMedia = this is MediaWrapper && type == MediaWrapper.TYPE_VIDEO
    return isVideoMedia || itemType == MediaLibraryItem.TYPE_VIDEO_GROUP || this is Folder
}

@Composable
private fun MainTvNowPlayingBadge(item: Any) {
    val dummyItem = item as? DummyItem ?: return
    val icon = when (dummyItem.id) {
        CATEGORY_NOW_PLAYING, CATEGORY_NOW_PLAYING_PIP -> VlcR.drawable.anim_now_playing
        CATEGORY_NOW_PLAYING_PAUSED, CATEGORY_NOW_PLAYING_PIP_PAUSED -> VlcR.drawable.ic_now_playing_paused
        else -> return
    }
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun Any.homeKey(): String = when (this) {
    is GenericCardItem -> "generic-$id"
    is MediaMetadataWithImages -> "metadata-${metadata.mlId}-${metadata.title}"
    is MediaLibraryItem -> "$itemType-$id-$title"
    else -> hashCode().toString()
}

private fun Any.homeTitle(): String = when (this) {
    is GenericCardItem -> title
    is MediaMetadataWithImages -> metadata.title.orEmpty()
    is MediaLibraryItem -> title.orEmpty()
    else -> toString()
}

private fun Any.homeSubtitle(): String = when (this) {
    is GenericCardItem -> content
    is MediaMetadataWithImages -> tvEpisodeSubtitle()
    is MediaWrapper -> {
        Tools.setMediaDescription(this)
        description.orEmpty()
    }
    is MediaLibraryItem -> description.orEmpty()
    else -> ""
}
