/*
 * ************************************************************************
 *  VideoScreen.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
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
 * **************************************************************************
 */

package org.videolan.vlc.gui.video

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Folder
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.interfaces.media.VideoGroup
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.GROUP_VIDEOS_FOLDER
import org.videolan.resources.GROUP_VIDEOS_NAME
import org.videolan.resources.GROUP_VIDEOS_NONE
import org.videolan.resources.KEY_FOLDER
import org.videolan.resources.KEY_GROUP
import org.videolan.resources.MEDIALIBRARY_PAGE_SIZE
import org.videolan.resources.PLAYLIST_TYPE_VIDEO
import org.videolan.resources.TAG_ITEM
import org.videolan.tools.KEY_CASTING_AUDIO_ONLY
import org.videolan.tools.KEY_GROUP_VIDEOS
import org.videolan.tools.KEY_MEDIA_LAST_PLAYLIST
import org.videolan.tools.KEY_VIDEOS_CARDS
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.tools.retrieveParent
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.components.VLCRenameDialogContent
import org.videolan.vlc.compose.components.VLCBrowserItemCard
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.HeaderMediaListActivity
import org.videolan.vlc.gui.InfoActivity
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.gui.browser.KEY_JUMP_TO
import org.videolan.vlc.gui.browser.KEY_MEDIA
import org.videolan.vlc.gui.dialogs.CURRENT_SORT
import org.videolan.vlc.gui.dialogs.ComposeMaterialBottomSheetHost
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.DEFAULT_ACTIONS
import org.videolan.vlc.gui.dialogs.DISPLAY_IN_CARDS
import org.videolan.vlc.gui.dialogs.DisplaySettingsDialog
import org.videolan.vlc.gui.dialogs.ONLY_FAVS
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.VIDEO_GROUPING
import org.videolan.vlc.gui.dialogs.CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showDisplaySettingsComposeDialog
import org.videolan.vlc.gui.helpers.AudioUtil.setRingtone
import org.videolan.vlc.gui.helpers.DefaultPlaybackAction
import org.videolan.vlc.gui.helpers.DefaultPlaybackActionMediaType
import org.videolan.vlc.gui.helpers.FloatingActionButtonBehavior
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.gui.helpers.UiTools.addToGroup
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.helpers.UiTools.snacker
import org.videolan.vlc.gui.helpers.UiTools.snackerMissing
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.media.getAll
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_GROUP
import org.videolan.vlc.util.ContextOption.CTX_ADD_SHORTCUT
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_BAN_FOLDER
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_DOWNLOAD_SUBTITLES
import org.videolan.vlc.util.ContextOption.CTX_FAV_ADD
import org.videolan.vlc.util.ContextOption.CTX_FAV_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_FOLDER
import org.videolan.vlc.util.ContextOption.CTX_GROUP_SIMILAR
import org.videolan.vlc.util.ContextOption.CTX_INFORMATION
import org.videolan.vlc.util.ContextOption.CTX_MARK_ALL_AS_PLAYED
import org.videolan.vlc.util.ContextOption.CTX_MARK_ALL_AS_UNPLAYED
import org.videolan.vlc.util.ContextOption.CTX_MARK_AS_PLAYED
import org.videolan.vlc.util.ContextOption.CTX_MARK_AS_UNPLAYED
import org.videolan.vlc.util.ContextOption.CTX_PLAY
import org.videolan.vlc.util.ContextOption.CTX_PLAY_ALL
import org.videolan.vlc.util.ContextOption.CTX_PLAY_AS_AUDIO
import org.videolan.vlc.util.ContextOption.CTX_PLAY_FROM_START
import org.videolan.vlc.util.ContextOption.CTX_PLAY_NEXT
import org.videolan.vlc.util.ContextOption.CTX_PLAY_SHUFFLE
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.ContextOption.CTX_RENAME_GROUP
import org.videolan.vlc.util.ContextOption.CTX_REMOVE_GROUP
import org.videolan.vlc.util.ContextOption.CTX_SET_RINGTONE
import org.videolan.vlc.util.ContextOption.CTX_SHARE
import org.videolan.vlc.util.ContextOption.CTX_UNGROUP
import org.videolan.vlc.util.ContextOption.Companion.createCtxFolderFlags
import org.videolan.vlc.util.ContextOption.Companion.createCtxPlaylistAlbumFlags
import org.videolan.vlc.util.ContextOption.Companion.createCtxVideoFlags
import org.videolan.vlc.util.ContextOption.Companion.createCtxVideoGroupFlags
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.isMissing
import org.videolan.vlc.util.share
import org.videolan.vlc.viewmodels.DisplaySettingsViewModel
import org.videolan.vlc.viewmodels.mobile.PlaylistsViewModel
import org.videolan.vlc.viewmodels.mobile.VideoGroupingType
import org.videolan.vlc.viewmodels.mobile.VideosViewModel
import java.security.SecureRandom
import kotlin.math.min

class VideoScreenController(private val activity: MainActivity) : DefaultLifecycleObserver, CtxActionReceiver {

    private val settings = Settings.getInstance(activity)
    private val displaySettingsViewModel = ViewModelProvider(activity)[DisplaySettingsViewModel::class.java]
    private val videoViewModel = ViewModelProvider(
        activity,
        VideosViewModel.Factory(activity, initialGrouping())
    )["main-videos", VideosViewModel::class.java]
    private val playlistViewModel = ViewModelProvider(
        activity,
        PlaylistsViewModel.Factory(activity, Playlist.Type.Video)
    )["main-video-playlists", PlaylistsViewModel::class.java]

    private var currentTab by mutableIntStateOf(0)
    private var videos by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var playlists by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var videoLoading by mutableStateOf(false)
    private var playlistLoading by mutableStateOf(false)
    private var displayVideosInCards by mutableStateOf(settings.getBoolean(KEY_VIDEOS_CARDS, true))
    private var displayPlaylistsInCards by mutableStateOf(playlistViewModel.providerInCard)
    private var settingsJob: Job? = null
    private var visible = false

    init {
        activity.lifecycle.addObserver(this)
        observeVideoProvider()
        playlistViewModel.provider.pagedList.observe(activity) { list ->
            playlists = list?.filterNotNull().orEmpty()
        }
        playlistViewModel.provider.loading.observe(activity) { loading ->
            playlistLoading = loading == true
            activity.refreshing = videoLoading || playlistLoading
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        settingsJob?.cancel()
        activity.lifecycle.removeObserver(this)
    }

    fun onVisible() {
        visible = true
        activity.title = activity.getString(R.string.videos)
        activity.setTabLayoutVisibility(false)
        showFloatingActionButton()
        startDisplaySettingsCollector()
        videoViewModel.refresh()
        playlistViewModel.refresh()
        activity.invalidateOptionsMenu()
    }

    fun onHidden() {
        visible = false
        settingsJob?.cancel()
        settingsJob = null
        hideFloatingActionButton()
    }

    fun refresh() {
        activity.reloadLibrary()
        videoViewModel.refresh()
        playlistViewModel.refresh()
    }

    fun prepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.ml_menu_last_playlist)?.isVisible = currentTab == 0 && settings.contains(KEY_MEDIA_LAST_PLAYLIST)
        menu.findItem(R.id.ml_menu_sortby)?.isVisible = false
        menu.findItem(R.id.ml_menu_display_options)?.isVisible = true
        menu.findItem(R.id.play_all)?.isVisible = currentTab == 0 && videos.isNotEmpty()
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ml_menu_last_playlist -> {
                MediaUtils.loadlastPlaylist(activity, PLAYLIST_TYPE_VIDEO)
                true
            }
            R.id.play_all -> {
                videoViewModel.playAll(activity)
                true
            }
            R.id.ml_menu_display_options -> {
                showDisplaySettings()
                true
            }
            else -> false
        }
    }

    @Composable
    fun Content() {
        VideoScreenContent(
            selectedTab = currentTab,
            videos = videos,
            playlists = playlists,
            videoLoading = videoLoading,
            playlistLoading = playlistLoading,
            displayVideosInCards = displayVideosInCards,
            displayPlaylistsInCards = displayPlaylistsInCards,
            videoOnlyFavorites = videoViewModel.provider.onlyFavorites,
            playlistOnlyFavorites = playlistViewModel.provider.onlyFavorites,
            videoFilterQuery = videoViewModel.filterQuery,
            playlistFilterQuery = playlistViewModel.filterQuery,
            videoGroupingType = videoViewModel.groupingType,
            onTabSelected = {
                currentTab = it
                activity.invalidateOptionsMenu()
                if (it == 0) showFloatingActionButton() else hideFloatingActionButton()
            },
            onVideoClicked = ::onVideoClicked,
            onVideoMoreClicked = ::onVideoMoreClicked,
            onVideoMainActionClicked = ::onVideoMainActionClicked,
            onPlaylistClicked = ::onPlaylistClicked,
            onPlaylistMoreClicked = ::onPlaylistMoreClicked,
            onPlaylistMainActionClicked = ::onPlaylistMainActionClicked
        )
    }

    private fun initialGrouping(): VideoGroupingType {
        return when (settings.getString(KEY_GROUP_VIDEOS, null) ?: GROUP_VIDEOS_NAME) {
            GROUP_VIDEOS_NONE -> VideoGroupingType.NONE
            GROUP_VIDEOS_FOLDER -> VideoGroupingType.FOLDER
            else -> VideoGroupingType.NAME
        }
    }

    private fun observeVideoProvider() {
        videoViewModel.provider.pagedList.observe(activity) { list ->
            videos = list?.filterNotNull().orEmpty()
            if (visible && currentTab == 0) {
                showFloatingActionButton()
                activity.invalidateOptionsMenu()
            }
        }
        videoViewModel.provider.loading.observe(activity) { loading ->
            videoLoading = loading == true
            activity.refreshing = videoLoading || playlistLoading
        }
    }

    private fun showDisplaySettings() {
        if (currentTab == 1) {
            val sorts = arrayListOf(
                Medialibrary.SORT_ALPHA,
                Medialibrary.SORT_FILENAME,
                Medialibrary.SORT_ARTIST,
                Medialibrary.SORT_ALBUM,
                Medialibrary.SORT_DURATION,
                Medialibrary.SORT_RELEASEDATE,
                Medialibrary.SORT_LASTMODIFICATIONDATE,
                Medialibrary.SORT_FILESIZE,
                Medialibrary.NbMedia
            ).filter { playlistViewModel.provider.canSortBy(it) }
            activity.showDisplaySettingsComposeDialog(
                displayInCards = playlistViewModel.providerInCard,
                onlyFavs = playlistViewModel.provider.onlyFavorites,
                sorts = sorts,
                currentSort = playlistViewModel.provider.sort,
                currentSortDesc = playlistViewModel.provider.desc,
                defaultPlaybackActions = DefaultPlaybackActionMediaType.PLAYLIST.getDefaultPlaybackActions(settings),
                defaultActionType = activity.getString(DefaultPlaybackActionMediaType.PLAYLIST.title)
            )
            return
        }

        val sorts = arrayListOf(
            Medialibrary.SORT_ALPHA,
            Medialibrary.SORT_FILENAME,
            Medialibrary.SORT_ARTIST,
            Medialibrary.SORT_ALBUM,
            Medialibrary.SORT_DURATION,
            Medialibrary.SORT_RELEASEDATE,
            Medialibrary.SORT_LASTMODIFICATIONDATE,
            Medialibrary.SORT_FILESIZE,
            Medialibrary.NbMedia,
            Medialibrary.SORT_INSERTIONDATE
        ).filter { videoViewModel.provider.canSortBy(it) }
        activity.showDisplaySettingsComposeDialog(
            displayInCards = displayVideosInCards,
            onlyFavs = videoViewModel.provider.onlyFavorites,
            sorts = sorts,
            currentSort = videoViewModel.provider.sort,
            currentSortDesc = videoViewModel.provider.desc,
            videoGroup = settings.getString(KEY_GROUP_VIDEOS, GROUP_VIDEOS_NAME),
            defaultPlaybackActions = DefaultPlaybackActionMediaType.VIDEO.getDefaultPlaybackActions(settings),
            defaultActionType = activity.getString(DefaultPlaybackActionMediaType.VIDEO.title)
        )
    }

    private fun startDisplaySettingsCollector() {
        if (settingsJob != null) return
        settingsJob = activity.lifecycleScope.launch {
            displaySettingsViewModel.settingChangeFlow.collect { change ->
                if (!visible || change.key == "init") return@collect
                if (currentTab == 0) applyVideoDisplayChange(change.key, change.value)
                else applyPlaylistDisplayChange(change.key, change.value)
                displaySettingsViewModel.consume()
            }
        }
    }

    private fun applyVideoDisplayChange(key: String, value: Any) {
        when (key) {
            DISPLAY_IN_CARDS -> {
                displayVideosInCards = value as Boolean
                settings.putSingle(KEY_VIDEOS_CARDS, value)
            }
            ONLY_FAVS -> {
                videoViewModel.provider.showOnlyFavs(value as Boolean)
                videoViewModel.refresh()
            }
            CURRENT_SORT -> {
                @Suppress("UNCHECKED_CAST")
                val sort = value as Pair<Int, Boolean>
                videoViewModel.provider.sort = sort.first
                videoViewModel.provider.desc = sort.second
                videoViewModel.provider.saveSort()
                videoViewModel.refresh()
            }
            VIDEO_GROUPING -> {
                val group = value as DisplaySettingsDialog.VideoGroup
                settings.putSingle(KEY_GROUP_VIDEOS, group.value)
                videoViewModel.provider.pagedList.removeObservers(activity)
                videoViewModel.provider.loading.removeObservers(activity)
                videoViewModel.changeGroupingType(group.type)
                observeVideoProvider()
            }
            DEFAULT_ACTIONS -> settings.putSingle(DefaultPlaybackActionMediaType.VIDEO.defaultActionKey, (value as DefaultPlaybackAction).name)
        }
    }

    private fun applyPlaylistDisplayChange(key: String, value: Any) {
        when (key) {
            DISPLAY_IN_CARDS -> {
                val cards = value as Boolean
                playlistViewModel.providerInCard = cards
                displayPlaylistsInCards = cards
                settings.putSingle(playlistViewModel.displayModeKey, cards)
            }
            ONLY_FAVS -> {
                playlistViewModel.provider.showOnlyFavs(value as Boolean)
                playlistViewModel.refresh()
            }
            CURRENT_SORT -> {
                @Suppress("UNCHECKED_CAST")
                val sort = value as Pair<Int, Boolean>
                playlistViewModel.provider.sort = sort.first
                playlistViewModel.provider.desc = sort.second
                playlistViewModel.provider.saveSort()
                playlistViewModel.refresh()
            }
        }
    }

    private fun onVideoClicked(position: Int, item: MediaLibraryItem) {
        when (item) {
            is MediaWrapper -> {
                if (!item.isPresent) {
                    snackerMissing(activity)
                    return
                }
                val castAsAudio = PlaybackService.renderer.value != null && settings.getBoolean(KEY_CASTING_AUDIO_ONLY, false)
                if (castAsAudio) {
                    item.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                    PlaylistManager.playingAsAudio = true
                }
                when (DefaultPlaybackActionMediaType.VIDEO.getCurrentPlaybackAction(settings)) {
                    DefaultPlaybackAction.PLAY -> videoViewModel.playVideo(activity, item, position, forceAudio = castAsAudio)
                    DefaultPlaybackAction.ADD_TO_QUEUE -> MediaUtils.appendMedia(activity, item)
                    DefaultPlaybackAction.INSERT_NEXT -> MediaUtils.insertNext(activity, item)
                    else -> videoViewModel.playVideo(activity, item, position, forceAll = true, forceAudio = castAsAudio)
                }
            }
            is Folder -> {
                if (item.mMrl.isMissing()) return
                openVideoContainer(item)
            }
            is VideoGroup -> {
                when {
                    item.presentCount == 0 -> snackerMissing(activity)
                    item.presentCount == 1 -> videoViewModel.play(position)
                    else -> openVideoContainer(item)
                }
            }
        }
    }

    private fun onVideoMainActionClicked(position: Int, item: MediaLibraryItem) {
        when (item) {
            is MediaWrapper -> videoViewModel.playVideo(activity, item, position)
            is Folder -> videoViewModel.play(position)
            is VideoGroup -> if (item.presentCount == 0) snackerMissing(activity) else videoViewModel.play(position)
        }
    }

    private fun onVideoMoreClicked(position: Int, item: MediaLibraryItem) {
        val flags = when (item) {
            is Folder -> createCtxFolderFlags().apply {
                if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
            }
            is VideoGroup -> createCtxVideoGroupFlags().apply {
                if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
            }
            is MediaWrapper -> createCtxVideoFlags().apply {
                if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
                if (item.seen > 0) add(CTX_MARK_AS_UNPLAYED) else add(CTX_MARK_AS_PLAYED)
                if (item.time != 0L) add(CTX_PLAY_FROM_START)
                if (videoViewModel.groupingType == VideoGroupingType.NAME) addAll(CTX_ADD_GROUP, CTX_GROUP_SIMILAR)
                if (item.uri.retrieveParent() != null) add(CTX_GO_TO_FOLDER)
            }
            else -> return
        }
        showContext(activity, this, position, item, flags)
    }

    private fun onPlaylistClicked(@Suppress("UNUSED_PARAMETER") position: Int, playlist: MediaLibraryItem) {
        activity.startActivity(Intent(activity, HeaderMediaListActivity::class.java).apply {
            putExtra(TAG_ITEM, playlist)
        })
    }

    private fun onPlaylistMainActionClicked(@Suppress("UNUSED_PARAMETER") position: Int, playlist: MediaLibraryItem) {
        MediaUtils.openList(activity, playlist.tracks.toList(), 0)
    }

    private fun onPlaylistMoreClicked(position: Int, playlist: MediaLibraryItem) {
        val flags = createCtxPlaylistAlbumFlags().apply {
            add(CTX_PLAY_AS_AUDIO)
            remove(ContextOption.CTX_GO_TO_ARTIST)
            if (playlist.tracksCount > 2) add(CTX_PLAY_SHUFFLE)
            if (playlist.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
        }
        showContext(activity, this, position, playlist, flags)
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        if (currentTab == 1) {
            onPlaylistCtxAction(position, option)
            return
        }
        val item = videos.getOrNull(position) ?: return
        when (item) {
            is MediaWrapper -> when (option) {
                CTX_PLAY_FROM_START -> videoViewModel.playVideo(activity, item, position, fromStart = true)
                CTX_PLAY_AS_AUDIO -> videoViewModel.playAudio(activity, item)
                CTX_PLAY_ALL -> videoViewModel.playVideo(activity, item, position, forceAll = true)
                CTX_PLAY -> videoViewModel.play(position)
                CTX_INFORMATION -> showInfoDialog(item)
                CTX_DELETE -> confirmDelete(item) { videoViewModel.refresh() }
                CTX_APPEND -> MediaUtils.appendMedia(activity, item)
                CTX_SET_RINGTONE -> activity.setRingtone(item)
                CTX_PLAY_NEXT -> MediaUtils.insertNext(activity, item.tracks)
                CTX_DOWNLOAD_SUBTITLES -> MediaUtils.getSubs(activity, item)
                CTX_ADD_TO_PLAYLIST -> activity.addToPlaylist(item.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
                CTX_SHARE -> activity.lifecycleScope.launch { activity.share(item) }
                CTX_ADD_GROUP -> activity.addToGroup(listOf(item), true)
                CTX_GROUP_SIMILAR -> activity.lifecycleScope.launch {
                    if (!activity.showPinIfNeeded()) {
                        videoViewModel.groupSimilar(item)
                        videoViewModel.refresh()
                    }
                }
                CTX_MARK_AS_PLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsPlayed(item); videoViewModel.refresh() }
                CTX_MARK_AS_UNPLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsUnplayed(item); videoViewModel.refresh() }
                CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch(Dispatchers.IO) {
                    item.isFavorite = option == CTX_FAV_ADD
                    withContext(Dispatchers.Main) { videoViewModel.refresh() }
                }
                CTX_GO_TO_FOLDER -> showParentFolder(item)
                CTX_ADD_SHORTCUT -> activity.lifecycleScope.launch { activity.createShortcut(item) }
                else -> {}
            }
            is Folder -> when (option) {
                CTX_PLAY, CTX_PLAY_ALL -> videoViewModel.play(position)
                CTX_APPEND -> videoViewModel.append(position)
                CTX_ADD_TO_PLAYLIST -> videoViewModel.addItemToPlaylist(activity, position)
                CTX_BAN_FOLDER -> confirmBanFolder(item)
                CTX_MARK_ALL_AS_PLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsPlayed(item); videoViewModel.refresh() }
                CTX_MARK_ALL_AS_UNPLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsUnplayed(item); videoViewModel.refresh() }
                CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch(Dispatchers.IO) {
                    item.isFavorite = option == CTX_FAV_ADD
                    withContext(Dispatchers.Main) { videoViewModel.refresh() }
                }
                else -> {}
            }
            is VideoGroup -> when (option) {
                CTX_PLAY_ALL, CTX_PLAY -> videoViewModel.play(position)
                CTX_APPEND -> videoViewModel.append(position)
                CTX_ADD_TO_PLAYLIST -> videoViewModel.addItemToPlaylist(activity, position)
                CTX_ADD_GROUP -> activity.addToGroup(listOf(item).getAll(), true)
                CTX_RENAME_GROUP -> showRenameDialog(item) { name ->
                    videoViewModel.renameGroup(item, name)
                    videoViewModel.refresh()
                }
                CTX_UNGROUP -> activity.lifecycleScope.launch {
                    if (!activity.showPinIfNeeded()) {
                        videoViewModel.ungroup(item)
                        videoViewModel.refresh()
                    }
                }
                CTX_MARK_ALL_AS_PLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsPlayed(item); videoViewModel.refresh() }
                CTX_MARK_ALL_AS_UNPLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsUnplayed(item); videoViewModel.refresh() }
                CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch(Dispatchers.IO) {
                    item.isFavorite = option == CTX_FAV_ADD
                    withContext(Dispatchers.Main) { videoViewModel.refresh() }
                }
                else -> {}
            }
        }
    }

    private fun onPlaylistCtxAction(position: Int, option: ContextOption) {
        val playlist = playlists.getOrNull(position) ?: return
        when (option) {
            CTX_PLAY -> MediaUtils.playTracks(activity, playlist, 0)
            CTX_PLAY_SHUFFLE -> MediaUtils.playTracks(activity, playlist, SecureRandom().nextInt(min(playlist.tracksCount, MEDIALIBRARY_PAGE_SIZE)), true)
            CTX_PLAY_AS_AUDIO -> activity.lifecycleScope.launch(Dispatchers.IO) {
                (playlist as? Playlist)?.tracks?.let { tracks ->
                    MediaUtils.openList(activity, tracks.map {
                        it.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                        it
                    }, 0)
                }
            }
            CTX_INFORMATION -> showInfoDialog(playlist)
            CTX_DELETE -> confirmDelete(playlist) { playlistViewModel.refresh() }
            CTX_APPEND -> MediaUtils.appendMedia(activity, playlist.tracks)
            CTX_PLAY_NEXT -> MediaUtils.insertNext(activity, playlist.tracks)
            CTX_ADD_TO_PLAYLIST -> activity.addToPlaylist(playlist.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            CTX_ADD_SHORTCUT -> activity.lifecycleScope.launch { activity.createShortcut(playlist) }
            CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch {
                withContext(Dispatchers.IO) { playlist.isFavorite = option == CTX_FAV_ADD }
                playlistViewModel.refresh()
            }
            CTX_RENAME -> if (playlist is Playlist) {
                showRenameDialog(playlist) { name ->
                    activity.lifecycleScope.launch { playlistViewModel.rename(playlist, name) }
                }
            }
            else -> {}
        }
    }

    private fun openVideoContainer(item: MediaLibraryItem) {
        activity.startActivityForResult(Intent(activity, SecondaryActivity::class.java).apply {
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.VIDEO_GROUP_LIST)
            if (item is Folder) putExtra(KEY_FOLDER, item)
            else if (item is VideoGroup) putExtra(KEY_GROUP, item)
        }, SecondaryActivity.ACTIVITY_RESULT_SECONDARY)
    }

    private fun showInfoDialog(item: MediaLibraryItem) {
        activity.startActivity(Intent(activity, InfoActivity::class.java).apply {
            putExtra(TAG_ITEM, item)
        })
    }

    private fun confirmDelete(item: MediaLibraryItem, onDeleted: () -> Unit) {
        activity.showConfirmDeleteComposeDialog(arrayListOf(item)) {
            MediaUtils.deleteItem(activity, item) { failed ->
                snacker(activity, activity.getString(R.string.msg_delete_failed, failed.title))
            }
            onDeleted()
        }
    }

    private fun confirmBanFolder(folder: Folder) {
        activity.showConfirmDeleteComposeDialog(
            medias = arrayListOf(folder),
            title = activity.getString(R.string.group_ban_folder),
            description = activity.getString(R.string.ban_folder_explanation, activity.getString(R.string.medialibrary_directories)),
            buttonText = activity.getString(R.string.ban_folder),
            resultType = CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER
        ) {
            banFolder(folder)
        }
    }

    private fun banFolder(folder: Folder) {
        val path = folder.mMrl.toUri().path
        if (path == null) {
            snacker(activity, activity.getString(R.string.msg_delete_failed, folder.title))
            return
        }
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val strippedPath = path.removePrefix("file://")
            val roots = Medialibrary.getInstance().foldersList
            if (roots.any { it.removePrefix("file://") == strippedPath }) {
                withContext(Dispatchers.Main) {
                    snacker(activity, R.string.cant_ban_root)
                }
                return@launch
            }
            MedialibraryUtils.banDir(strippedPath)
            withContext(Dispatchers.Main) { videoViewModel.refresh() }
        }
    }

    private fun showRenameDialog(item: MediaLibraryItem, onRename: (String) -> Unit) {
        if (activity.showPinIfNeeded()) return
        val dialog = if (Settings.showTvUi) {
            ComposeMaterialBottomSheetHost(activity)
        } else {
            ComposeMaterialBottomSheetHost(activity)
        }
        var newName by mutableStateOf(TextFieldValue(item.title.orEmpty()))
        val rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    VLCRenameDialogContent(
                        title = activity.getString(R.string.rename),
                        mediaTitle = item.title.orEmpty(),
                        newTitleHint = activity.getString(R.string.new_title),
                        okText = activity.getString(R.string.ok),
                        newName = newName,
                        onNewNameChange = { newName = it },
                        onConfirm = {
                            val trimmedName = newName.text.trim()
                            if (trimmedName.isNotEmpty()) {
                                onRename(trimmedName)
                                dialog.dismiss()
                            }
                        }
                    )
                }
            }
        }
        dialog.setContentView(rootView)
        dialog.show()
    }

    private fun showParentFolder(media: MediaWrapper) {
        val parent = MLServiceLocator.getAbstractMediaWrapper(media.uri.retrieveParent()).apply {
            type = MediaWrapper.TYPE_DIR
        }
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, parent)
            putExtra(KEY_JUMP_TO, media)
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun showFloatingActionButton() {
        val fab = activity.findViewById<FloatingActionButton?>(R.id.fab) ?: return
        ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = videos.isEmpty()
        fab.setImageResource(R.drawable.ic_fab_play)
        fab.contentDescription = activity.getString(R.string.play)
        fab.setOnClickListener { videoViewModel.playAll(activity) }
        if (videos.isEmpty()) fab.hide() else fab.show()
        activity.findViewById<FloatingActionButton?>(R.id.fab_large)?.hide()
    }

    private fun hideFloatingActionButton() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            fab.hide()
        }
    }
}

class SecondaryVideoScreenController(
    private val activity: SecondaryActivity,
    private val folder: Folder?,
    private val group: VideoGroup?
) : DefaultLifecycleObserver, CtxActionReceiver {

    private val settings = Settings.getInstance(activity)
    private val displaySettingsViewModel = ViewModelProvider(activity)[DisplaySettingsViewModel::class.java]
    private val videoViewModel = ViewModelProvider(
        activity,
        VideosViewModel.Factory(activity, VideoGroupingType.NONE, folder, group)
    )["secondary-videos-${folder?.id ?: 0L}-${group?.id ?: 0L}", VideosViewModel::class.java]

    private var videos by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var videoLoading by mutableStateOf(false)
    private var displayInCards by mutableStateOf(settings.getBoolean(KEY_VIDEOS_CARDS, true))
    private var settingsJob: Job? = null
    private var visible = false

    init {
        activity.lifecycle.addObserver(this)
        videoViewModel.provider.pagedList.observe(activity) { list ->
            videos = list?.filterNotNull().orEmpty()
            if (visible) {
                showFloatingActionButton()
                activity.invalidateOptionsMenu()
            }
        }
        videoViewModel.provider.loading.observe(activity) { loading ->
            videoLoading = loading == true
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        settingsJob?.cancel()
        hideFloatingActionButton()
        activity.lifecycle.removeObserver(this)
    }

    fun onVisible() {
        if (visible) return
        visible = true
        activity.supportActionBar?.title = folder?.displayTitle ?: group?.displayTitle ?: activity.getString(R.string.videos)
        activity.setTabLayoutVisibility(false)
        showFloatingActionButton()
        startDisplaySettingsCollector()
        videoViewModel.refresh()
        activity.invalidateOptionsMenu()
    }

    fun onHidden() {
        if (!visible) return
        visible = false
        settingsJob?.cancel()
        settingsJob = null
        hideFloatingActionButton()
    }

    fun refresh() {
        videoViewModel.refresh()
    }

    fun prepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.ml_menu_last_playlist)?.isVisible = settings.contains(KEY_MEDIA_LAST_PLAYLIST)
        menu.findItem(R.id.rename_group)?.isVisible = group != null
        menu.findItem(R.id.ungroup)?.isVisible = group != null
        menu.findItem(R.id.ml_menu_sortby)?.isVisible = false
        menu.findItem(R.id.ml_menu_display_options)?.isVisible = true
        menu.findItem(R.id.play_all)?.isVisible = videos.isNotEmpty()
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ml_menu_last_playlist -> {
                MediaUtils.loadlastPlaylist(activity, PLAYLIST_TYPE_VIDEO)
                true
            }
            R.id.rename_group -> {
                group?.let { renameGroup(it) }
                true
            }
            R.id.ungroup -> {
                group?.let { videoGroup ->
                    activity.lifecycleScope.launch {
                        if (!activity.showPinIfNeeded()) {
                            videoViewModel.ungroup(videoGroup)
                            activity.finish()
                        }
                    }
                }
                true
            }
            R.id.play_all -> {
                videoViewModel.playAll(activity)
                true
            }
            R.id.ml_menu_display_options -> {
                showDisplaySettings()
                true
            }
            else -> false
        }
    }

    @Composable
    fun Content() {
        MediaGridOrList(
            items = videos,
            loading = videoLoading,
            displayInCards = displayInCards,
            emptyText = emptyVideoText(videoViewModel.provider.onlyFavorites, videoViewModel.filterQuery),
            itemSubtitle = { videoSubtitle(it, VideoGroupingType.NONE) },
            icon = { videoIcon(it) },
            onClick = ::onVideoClicked,
            onMoreClick = ::onVideoMoreClicked,
            onMainActionClick = ::onVideoMainActionClicked
        )
    }

    private fun showDisplaySettings() {
        val sorts = arrayListOf(
            Medialibrary.SORT_ALPHA,
            Medialibrary.SORT_FILENAME,
            Medialibrary.SORT_ARTIST,
            Medialibrary.SORT_ALBUM,
            Medialibrary.SORT_DURATION,
            Medialibrary.SORT_RELEASEDATE,
            Medialibrary.SORT_LASTMODIFICATIONDATE,
            Medialibrary.SORT_FILESIZE,
            Medialibrary.NbMedia,
            Medialibrary.SORT_INSERTIONDATE
        ).filter { videoViewModel.provider.canSortBy(it) }
        activity.showDisplaySettingsComposeDialog(
            displayInCards = displayInCards,
            onlyFavs = videoViewModel.provider.onlyFavorites,
            sorts = sorts,
            currentSort = videoViewModel.provider.sort,
            currentSortDesc = videoViewModel.provider.desc,
            videoGroup = null,
            defaultPlaybackActions = DefaultPlaybackActionMediaType.VIDEO.getDefaultPlaybackActions(settings),
            defaultActionType = activity.getString(DefaultPlaybackActionMediaType.VIDEO.title)
        )
    }

    private fun startDisplaySettingsCollector() {
        if (settingsJob != null) return
        settingsJob = activity.lifecycleScope.launch {
            displaySettingsViewModel.settingChangeFlow.collect { change ->
                if (!visible || change.key == "init") return@collect
                applyDisplayChange(change.key, change.value)
                displaySettingsViewModel.consume()
            }
        }
    }

    private fun applyDisplayChange(key: String, value: Any) {
        when (key) {
            DISPLAY_IN_CARDS -> {
                displayInCards = value as Boolean
                settings.putSingle(KEY_VIDEOS_CARDS, value)
            }
            ONLY_FAVS -> {
                videoViewModel.provider.showOnlyFavs(value as Boolean)
                videoViewModel.refresh()
            }
            CURRENT_SORT -> {
                @Suppress("UNCHECKED_CAST")
                val sort = value as Pair<Int, Boolean>
                videoViewModel.provider.sort = sort.first
                videoViewModel.provider.desc = sort.second
                videoViewModel.provider.saveSort()
                videoViewModel.refresh()
            }
            DEFAULT_ACTIONS -> settings.putSingle(DefaultPlaybackActionMediaType.VIDEO.defaultActionKey, (value as DefaultPlaybackAction).name)
        }
    }

    private fun onVideoClicked(position: Int, item: MediaLibraryItem) {
        when (item) {
            is MediaWrapper -> {
                if (!item.isPresent) {
                    snackerMissing(activity)
                    return
                }
                val castAsAudio = PlaybackService.renderer.value != null && settings.getBoolean(KEY_CASTING_AUDIO_ONLY, false)
                if (castAsAudio) {
                    item.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                    PlaylistManager.playingAsAudio = true
                }
                when (DefaultPlaybackActionMediaType.VIDEO.getCurrentPlaybackAction(settings)) {
                    DefaultPlaybackAction.PLAY -> videoViewModel.playVideo(activity, item, position, forceAudio = castAsAudio)
                    DefaultPlaybackAction.ADD_TO_QUEUE -> MediaUtils.appendMedia(activity, item)
                    DefaultPlaybackAction.INSERT_NEXT -> MediaUtils.insertNext(activity, item)
                    else -> videoViewModel.playVideo(activity, item, position, forceAll = true, forceAudio = castAsAudio)
                }
            }
            is Folder -> {
                if (item.mMrl.isMissing()) return
                openVideoContainer(item)
            }
            is VideoGroup -> {
                when {
                    item.presentCount == 0 -> snackerMissing(activity)
                    item.presentCount == 1 -> videoViewModel.play(position)
                    else -> openVideoContainer(item)
                }
            }
        }
    }

    private fun onVideoMainActionClicked(position: Int, item: MediaLibraryItem) {
        when (item) {
            is MediaWrapper -> videoViewModel.playVideo(activity, item, position)
            is Folder -> videoViewModel.play(position)
            is VideoGroup -> if (item.presentCount == 0) snackerMissing(activity) else videoViewModel.play(position)
        }
    }

    private fun onVideoMoreClicked(position: Int, item: MediaLibraryItem) {
        val flags = when (item) {
            is Folder -> createCtxFolderFlags().apply {
                if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
            }
            is VideoGroup -> {
                if (item.presentCount == 0) {
                    snackerMissing(activity)
                    return
                }
                createCtxVideoGroupFlags().apply {
                    if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
                }
            }
            is MediaWrapper -> createCtxVideoFlags().apply {
                if (item.isFavorite) add(CTX_FAV_REMOVE) else add(CTX_FAV_ADD)
                if (item.seen > 0) add(CTX_MARK_AS_UNPLAYED) else add(CTX_MARK_AS_PLAYED)
                if (item.time != 0L) add(CTX_PLAY_FROM_START)
                if (group != null) add(CTX_REMOVE_GROUP)
                if (item.uri.retrieveParent() != null) add(CTX_GO_TO_FOLDER)
            }
            else -> return
        }
        showContext(activity, this, position, item, flags)
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        val item = videos.getOrNull(position) ?: return
        when (item) {
            is MediaWrapper -> when (option) {
                CTX_PLAY_FROM_START -> videoViewModel.playVideo(activity, item, position, fromStart = true)
                CTX_PLAY_AS_AUDIO -> videoViewModel.playAudio(activity, item)
                CTX_PLAY_ALL -> videoViewModel.playVideo(activity, item, position, forceAll = true)
                CTX_PLAY -> videoViewModel.play(position)
                CTX_INFORMATION -> showInfoDialog(item)
                CTX_DELETE -> confirmDelete(item) { videoViewModel.refresh() }
                CTX_APPEND -> MediaUtils.appendMedia(activity, item)
                CTX_SET_RINGTONE -> activity.setRingtone(item)
                CTX_PLAY_NEXT -> MediaUtils.insertNext(activity, item.tracks)
                CTX_DOWNLOAD_SUBTITLES -> MediaUtils.getSubs(activity, item)
                CTX_ADD_TO_PLAYLIST -> activity.addToPlaylist(item.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
                CTX_SHARE -> activity.lifecycleScope.launch { activity.share(item) }
                CTX_REMOVE_GROUP -> {
                    videoViewModel.removeFromGroup(item)
                    videoViewModel.refresh()
                }
                CTX_ADD_GROUP -> activity.addToGroup(listOf(item), true)
                CTX_GROUP_SIMILAR -> activity.lifecycleScope.launch {
                    if (!activity.showPinIfNeeded()) {
                        videoViewModel.groupSimilar(item)
                        videoViewModel.refresh()
                    }
                }
                CTX_MARK_AS_PLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsPlayed(item); videoViewModel.refresh() }
                CTX_MARK_AS_UNPLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsUnplayed(item); videoViewModel.refresh() }
                CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch(Dispatchers.IO) {
                    item.isFavorite = option == CTX_FAV_ADD
                    withContext(Dispatchers.Main) { videoViewModel.refresh() }
                }
                CTX_GO_TO_FOLDER -> showParentFolder(item)
                CTX_ADD_SHORTCUT -> activity.lifecycleScope.launch { activity.createShortcut(item) }
                else -> {}
            }
            is Folder -> when (option) {
                CTX_PLAY, CTX_PLAY_ALL -> videoViewModel.play(position)
                CTX_APPEND -> videoViewModel.append(position)
                CTX_ADD_TO_PLAYLIST -> videoViewModel.addItemToPlaylist(activity, position)
                CTX_BAN_FOLDER -> confirmBanFolder(item)
                CTX_MARK_ALL_AS_PLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsPlayed(item); videoViewModel.refresh() }
                CTX_MARK_ALL_AS_UNPLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsUnplayed(item); videoViewModel.refresh() }
                CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch(Dispatchers.IO) {
                    item.isFavorite = option == CTX_FAV_ADD
                    withContext(Dispatchers.Main) { videoViewModel.refresh() }
                }
                else -> {}
            }
            is VideoGroup -> when (option) {
                CTX_PLAY_ALL, CTX_PLAY -> videoViewModel.play(position)
                CTX_APPEND -> videoViewModel.append(position)
                CTX_ADD_TO_PLAYLIST -> videoViewModel.addItemToPlaylist(activity, position)
                CTX_ADD_GROUP -> activity.addToGroup(listOf(item).getAll(), true)
                CTX_RENAME_GROUP -> renameGroup(item)
                CTX_UNGROUP -> activity.lifecycleScope.launch {
                    if (!activity.showPinIfNeeded()) {
                        videoViewModel.ungroup(item)
                        videoViewModel.refresh()
                    }
                }
                CTX_MARK_ALL_AS_PLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsPlayed(item); videoViewModel.refresh() }
                CTX_MARK_ALL_AS_UNPLAYED -> activity.lifecycleScope.launch { videoViewModel.markAsUnplayed(item); videoViewModel.refresh() }
                CTX_FAV_ADD, CTX_FAV_REMOVE -> activity.lifecycleScope.launch(Dispatchers.IO) {
                    item.isFavorite = option == CTX_FAV_ADD
                    withContext(Dispatchers.Main) { videoViewModel.refresh() }
                }
                else -> {}
            }
        }
    }

    private fun openVideoContainer(item: MediaLibraryItem) {
        activity.startActivityForResult(Intent(activity, SecondaryActivity::class.java).apply {
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.VIDEO_GROUP_LIST)
            if (item is Folder) putExtra(KEY_FOLDER, item)
            else if (item is VideoGroup) putExtra(KEY_GROUP, item)
        }, SecondaryActivity.ACTIVITY_RESULT_SECONDARY)
    }

    private fun showInfoDialog(item: MediaLibraryItem) {
        activity.startActivity(Intent(activity, InfoActivity::class.java).apply {
            putExtra(TAG_ITEM, item)
        })
    }

    private fun confirmDelete(item: MediaLibraryItem, onDeleted: () -> Unit) {
        activity.showConfirmDeleteComposeDialog(arrayListOf(item)) {
            MediaUtils.deleteItem(activity, item) { failed ->
                snacker(activity, activity.getString(R.string.msg_delete_failed, failed.title))
            }
            onDeleted()
        }
    }

    private fun confirmBanFolder(folder: Folder) {
        activity.showConfirmDeleteComposeDialog(
            medias = arrayListOf(folder),
            title = activity.getString(R.string.group_ban_folder),
            description = activity.getString(R.string.ban_folder_explanation, activity.getString(R.string.medialibrary_directories)),
            buttonText = activity.getString(R.string.ban_folder),
            resultType = CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER
        ) {
            banFolder(folder)
        }
    }

    private fun banFolder(folder: Folder) {
        val path = folder.mMrl.toUri().path
        if (path == null) {
            snacker(activity, activity.getString(R.string.msg_delete_failed, folder.title))
            return
        }
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val strippedPath = path.removePrefix("file://")
            val roots = Medialibrary.getInstance().foldersList
            if (roots.any { it.removePrefix("file://") == strippedPath }) {
                withContext(Dispatchers.Main) {
                    snacker(activity, R.string.cant_ban_root)
                }
                return@launch
            }
            MedialibraryUtils.banDir(strippedPath)
            withContext(Dispatchers.Main) { videoViewModel.refresh() }
        }
    }

    private fun renameGroup(item: VideoGroup) {
        showRenameDialog(item) { name ->
            videoViewModel.renameGroup(item, name)
            activity.supportActionBar?.title = name
            videoViewModel.refresh()
        }
    }

    private fun showRenameDialog(item: MediaLibraryItem, onRename: (String) -> Unit) {
        if (activity.showPinIfNeeded()) return
        val dialog = if (Settings.showTvUi) {
            ComposeMaterialBottomSheetHost(activity)
        } else {
            ComposeMaterialBottomSheetHost(activity)
        }
        var newName by mutableStateOf(TextFieldValue(item.title.orEmpty()))
        val rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    VLCRenameDialogContent(
                        title = activity.getString(R.string.rename),
                        mediaTitle = item.title.orEmpty(),
                        newTitleHint = activity.getString(R.string.new_title),
                        okText = activity.getString(R.string.ok),
                        newName = newName,
                        onNewNameChange = { newName = it },
                        onConfirm = {
                            val trimmedName = newName.text.trim()
                            if (trimmedName.isNotEmpty()) {
                                onRename(trimmedName)
                                dialog.dismiss()
                            }
                        }
                    )
                }
            }
        }
        dialog.setContentView(rootView)
        dialog.show()
    }

    private fun showParentFolder(media: MediaWrapper) {
        val parent = MLServiceLocator.getAbstractMediaWrapper(media.uri.retrieveParent()).apply {
            type = MediaWrapper.TYPE_DIR
        }
        activity.startActivity(Intent(activity.applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, parent)
            putExtra(KEY_JUMP_TO, media)
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
        })
    }

    private fun showFloatingActionButton() {
        val fab = activity.findViewById<FloatingActionButton?>(R.id.fab) ?: return
        ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = videos.isEmpty()
        fab.setImageResource(R.drawable.ic_fab_play)
        fab.contentDescription = activity.getString(R.string.play)
        fab.setOnClickListener { videoViewModel.playAll(activity) }
        if (videos.isEmpty()) fab.hide() else fab.show()
        activity.findViewById<FloatingActionButton?>(R.id.fab_large)?.hide()
    }

    private fun hideFloatingActionButton() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            fab.hide()
        }
    }
}

@Composable
private fun VideoScreenContent(
    selectedTab: Int,
    videos: List<MediaLibraryItem>,
    playlists: List<MediaLibraryItem>,
    videoLoading: Boolean,
    playlistLoading: Boolean,
    displayVideosInCards: Boolean,
    displayPlaylistsInCards: Boolean,
    videoOnlyFavorites: Boolean,
    playlistOnlyFavorites: Boolean,
    videoFilterQuery: String?,
    playlistFilterQuery: String?,
    videoGroupingType: VideoGroupingType,
    onTabSelected: (Int) -> Unit,
    onVideoClicked: (Int, MediaLibraryItem) -> Unit,
    onVideoMoreClicked: (Int, MediaLibraryItem) -> Unit,
    onVideoMainActionClicked: (Int, MediaLibraryItem) -> Unit,
    onPlaylistClicked: (Int, MediaLibraryItem) -> Unit,
    onPlaylistMoreClicked: (Int, MediaLibraryItem) -> Unit,
    onPlaylistMainActionClicked: (Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDefault)
    ) {
        VideoTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)
        if (selectedTab == 0) {
            MediaGridOrList(
                items = videos,
                loading = videoLoading,
                displayInCards = displayVideosInCards,
                emptyText = emptyVideoText(videoOnlyFavorites, videoFilterQuery),
                itemSubtitle = { videoSubtitle(it, videoGroupingType) },
                icon = { videoIcon(it) },
                onClick = onVideoClicked,
                onMoreClick = onVideoMoreClicked,
                onMainActionClick = onVideoMainActionClicked
            )
        } else {
            MediaGridOrList(
                items = playlists,
                loading = playlistLoading,
                displayInCards = displayPlaylistsInCards,
                emptyText = playlistFilterQuery?.let { stringResource(R.string.empty_search, it) }
                    ?: if (playlistOnlyFavorites) stringResource(R.string.nofav) else stringResource(R.string.noplaylist),
                itemSubtitle = { stringResource(R.plurals.track_quantity, it.tracksCount, it.tracksCount) },
                icon = { R.drawable.ic_playlist },
                onClick = onPlaylistClicked,
                onMoreClick = onPlaylistMoreClicked,
                onMainActionClick = onPlaylistMainActionClicked
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val colors = VLCThemeDefaults.colors
    val tabs = listOf(stringResource(R.string.videos), stringResource(R.string.playlists))
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
        containerColor = colors.backgroundDefault,
        contentColor = colors.primary,
        edgePadding = 12.dp
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index
            Tab(
                selected = selected,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                selectedContentColor = colors.primary,
                unselectedContentColor = colors.listSubtitle
            )
        }
    }
}

@Composable
private fun MediaGridOrList(
    items: List<MediaLibraryItem>,
    loading: Boolean,
    displayInCards: Boolean,
    emptyText: String,
    itemSubtitle: @Composable (MediaLibraryItem) -> String,
    icon: (MediaLibraryItem) -> Int,
    onClick: (Int, MediaLibraryItem) -> Unit,
    onMoreClick: (Int, MediaLibraryItem) -> Unit,
    onMainActionClick: (Int, MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(modifier = Modifier.fillMaxSize(), color = colors.backgroundDefault) {
        when {
            loading && items.isEmpty() -> VLCEmptyState(loading = true, text = stringResource(R.string.loading))
            items.isEmpty() -> VLCEmptyState(loading = false, text = emptyText)
            displayInCards -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(
                    items.chunked(2),
                    key = { rowIndex, row -> "$rowIndex-${row.joinToString { "${it.itemType}-${it.id}-${it.title}" }}" }
                ) { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { columnIndex, item ->
                            val index = rowIndex * 2 + columnIndex
                            MediaCard(
                                item = item,
                                subtitle = itemSubtitle(item),
                                icon = icon(item),
                                modifier = Modifier.weight(1f),
                                onClick = { onClick(index, item) },
                                onMoreClick = { onMoreClick(index, item) },
                                onMainActionClick = { onMainActionClick(index, item) }
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(items, key = { index, item -> "$index-${item.itemType}-${item.id}-${item.title}" }) { index, item ->
                    MediaRow(
                        item = item,
                        subtitle = itemSubtitle(item),
                        icon = icon(item),
                        onClick = { onClick(index, item) },
                        onMoreClick = { onMoreClick(index, item) },
                        onMainActionClick = { onMainActionClick(index, item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaRow(
    item: MediaLibraryItem,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onMainActionClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    VLCBrowserItemRow(
        title = item.title.orEmpty(),
        subtitle = subtitle,
        onClick = onClick,
        onLongClick = onMoreClick,
        artworkContent = {
            MediaArtworkContent(item = item, fallbackIcon = icon, artworkSize = 40.dp, fallbackSize = 28.dp)
        },
        primaryActionContent = {
            Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.play), tint = colors.primary)
        },
        onPrimaryActionClick = onMainActionClick,
        moreActionContent = {
            Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more), tint = colors.listSubtitle)
        },
        onMoreClick = onMoreClick
    )
}

@Composable
private fun MediaCard(
    item: MediaLibraryItem,
    subtitle: String,
    icon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onMainActionClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    VLCBrowserItemCard(
        title = item.title.orEmpty(),
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onMoreClick,
        artworkContent = {
            MediaArtworkContent(item = item, fallbackIcon = icon, artworkSize = 48.dp, fallbackSize = 32.dp)
        },
        primaryActionContent = {
            Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.play), tint = colors.primary)
        },
        onPrimaryActionClick = onMainActionClick,
        moreActionContent = {
            Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more), tint = colors.listSubtitle)
        },
        onMoreClick = onMoreClick
    )
}

@Composable
private fun MediaArtworkContent(item: MediaLibraryItem, fallbackIcon: Int, artworkSize: Dp, fallbackSize: Dp) {
    VlcMediaImage(
        item = item,
        width = artworkSize,
        fallbackPainter = painterResource(fallbackIcon),
        fallbackModifier = Modifier.size(fallbackSize),
        fallbackColorFilter = ColorFilter.tint(VLCThemeDefaults.colors.primary),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun emptyVideoText(onlyFavorites: Boolean, filterQuery: String?): String {
    return when {
        !Permissions.canReadStorage(org.videolan.resources.AppContextProvider.appContext) -> stringResource(R.string.permission_media)
        !Permissions.canReadVideos(org.videolan.resources.AppContextProvider.appContext) -> stringResource(R.string.permission_media)
        filterQuery != null -> stringResource(R.string.empty_search, filterQuery)
        onlyFavorites -> stringResource(R.string.nofav)
        else -> stringResource(R.string.nomedia)
    }
}

@Composable
private fun videoSubtitle(item: MediaLibraryItem, groupingType: VideoGroupingType): String {
    return when (item) {
        is MediaWrapper -> item.description?.toString().takeUnless { it.isNullOrBlank() } ?: item.fileName ?: item.location.orEmpty()
        is Folder -> stringResource(R.plurals.videos_quantity, item.tracksCount, item.tracksCount)
        is VideoGroup -> stringResource(R.plurals.videos_quantity, item.tracksCount, item.tracksCount)
        else -> if (groupingType == VideoGroupingType.NONE) "" else item.description?.toString().orEmpty()
    }
}

private fun videoIcon(item: MediaLibraryItem): Int {
    return when (item) {
        is Folder -> R.drawable.ic_folder
        is VideoGroup -> R.drawable.ic_video
        is MediaWrapper -> R.drawable.ic_video
        else -> R.drawable.ic_unknown
    }
}
