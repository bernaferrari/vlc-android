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
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.leanback.app.BackgroundManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.DummyItem
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
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
import org.videolan.tools.RESULT_RESCAN
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.Settings
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.RecommendationsService
import org.videolan.vlc.ScanProgress
import org.videolan.vlc.StartActivity
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.icons.VlcCone
import org.videolan.vlc.compose.tv.VLCTvCardShape
import org.videolan.vlc.compose.tv.VLCTvHomeScreen
import org.videolan.vlc.compose.tv.VLCTvItem
import org.videolan.vlc.compose.tv.VLCTvShelfModel
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.gui.dialogs.showUpdateComposeDialog
import org.videolan.vlc.gui.helpers.UiTools.showDonations
import org.videolan.vlc.gui.helpers.hf.PinCodeDelegate
import org.videolan.vlc.gui.helpers.hf.StoragePermissionsDelegate
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.AutoUpdate
import org.videolan.vlc.util.LifecycleAwareScheduler
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.SchedulerCallback
import org.videolan.vlc.util.Util
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
    val symbol: MaterialIcon
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
                    VLCTvTheme {
                        MainTvHomeContent(
                            rows = homeRows(),
                            appName = getString(VlcR.string.app_name),
                            searchLabel = getString(VlcR.string.search),
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
        if (pinUnlocked) add(GenericCardItem(ID_PIN_LOCK, getString(VlcR.string.lock_with_pin_short), "", MaterialSymbols.Filled.Lock))
        add(GenericCardItem(ID_SETTINGS, getString(VlcR.string.preferences), "", MaterialSymbols.Filled.Settings))
        if (remoteAccessEnabled) add(GenericCardItem(ID_REMOTE_ACCESS, getString(VlcR.string.remote_access), "", MaterialSymbols.Filled.Devices))
        if (Permissions.canReadStorage(this@MainTvActivity)) add(GenericCardItem(ID_REFRESH, getString(VlcR.string.refresh), "", MaterialSymbols.Filled.Refresh))
        add(GenericCardItem(ID_ABOUT_TV, getString(VlcR.string.about), "${getString(VlcR.string.app_name_full)} ${BuildConfig.VLC_VERSION_NAME}", MaterialSymbols.Filled.Info))
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

private fun HomeRow.toEntries() = TvEntries(items) { item ->
    when (item) {
        is GenericCardItem -> VLCTvItem(key = "generic-${item.id}", title = item.title, subtitle = item.content, symbol = item.symbol, shape = VLCTvCardShape.Square)
        is MediaMetadataWithImages -> item.toTvItem()
        is MediaLibraryItem -> item.toTvItem()
        else -> VLCTvItem(key = item.hashCode().toString(), title = item.toString())
    }
}

@Composable
private fun MainTvHomeContent(
    rows: List<HomeRow>,
    appName: String,
    searchLabel: String,
    incognitoMode: Boolean,
    showSearch: Boolean,
    loading: Boolean,
    onSearch: () -> Unit,
    onItemFocused: (Any) -> Unit,
    onItemClicked: (Long, Any) -> Unit
) {
    val entries = remember(rows) { rows.map { it.toEntries() } }
    val shelves = remember(entries) {
        rows.mapIndexed { index, row ->
            VLCTvShelfModel(key = "${row.id}-${row.title}", title = row.title, items = entries[index].items, isShortcutShelf = row.id == HEADER_MISC)
        }
    }
    val sources = remember(entries) { entries.flatMap { it.sourcesByKey.entries }.associate { it.key to it.value } }
    VLCTvHomeScreen(
        appName = appName,
        shelves = shelves,
        searchLabel = searchLabel,
        onSearch = onSearch.takeIf { showSearch },
        loading = loading,
        onItemClick = { shelf, index -> entries[shelf][index]?.let { onItemClicked(rows[shelf].id, it) } },
        onItemFocused = { shelf, index -> entries[shelf][index]?.let(onItemFocused) },
        brand = {
            if (incognitoMode) {
                Icon(painterResource(VlcR.drawable.ic_incognito), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(44.dp))
            } else {
                Image(VlcCone, contentDescription = null, modifier = Modifier.size(44.dp))
            }
        },
        artwork = { item -> TvSourceArtwork(sources[item.key], item) },
    )
}
