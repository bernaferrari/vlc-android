/*
 * *************************************************************************
 *  SecondaryActivity.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.libvlc.Dialog
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.resources.KEY_ANIMATED
import org.videolan.resources.KEY_FOLDER
import org.videolan.resources.KEY_GROUP
import org.videolan.resources.TAG_ITEM
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.resources.util.parcelable
import org.videolan.tools.KEY_AUDIO_LAST_PLAYLIST
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.KEY_MEDIA_LAST_PLAYLIST
import org.videolan.tools.KeyHelper
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.RESULT_RESCAN
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.Settings
import org.videolan.tools.copy
import org.videolan.tools.isValidUrl
import org.videolan.tools.retrieveParent
import org.videolan.vlc.R
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.audio.SecondaryAudioScreenController
import org.videolan.vlc.gui.browser.FileBrowserFragment
import org.videolan.vlc.gui.browser.KEY_JUMP_TO
import org.videolan.vlc.gui.browser.KEY_MEDIA
import org.videolan.vlc.gui.browser.MLStorageBrowserFragment
import org.videolan.vlc.gui.browser.NetworkBrowserFragment
import org.videolan.vlc.gui.dialogs.CONFIRM_DELETE_DIALOG_RESULT
import org.videolan.vlc.gui.dialogs.CONFIRM_RENAME_DIALOG_RESULT
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.RENAME_DIALOG_MEDIA
import org.videolan.vlc.gui.dialogs.RENAME_DIALOG_NEW_NAME
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.network.StreamPanelContent
import org.videolan.vlc.gui.video.SecondaryVideoScreenController
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_SHORTCUT
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_COPY
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.DialogDelegate
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.util.IDialogManager
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.isSchemeNetwork
import org.videolan.vlc.viewmodels.HistoryModel
import org.videolan.vlc.viewmodels.StreamsModel

class SecondaryActivity : ContentActivity(), IDialogManager {

    private var fragment: Fragment? = null
    private var streamsModel: StreamsModel? = null
    private var streamsRoot: View? = null
    private var streamsSearchText: MutableState<String>? = null
    private var streamsClipboardText: MutableState<String?>? = null
    private var historyModel: HistoryModel? = null
    private var historyItems: MutableState<List<MediaWrapper>>? = null
    private var historySelection: MutableState<Set<Int>>? = null
    private var historyActionMode: ActionMode? = null
    private var historyCleanMenuItem: MenuItem? = null
    private var videoGroupController: SecondaryVideoScreenController? = null
    private var audioAlbumsSongsController: SecondaryAudioScreenController? = null
    override val displayTitle = true
    private val dialogsDelegate = DialogDelegate()
    val isOnboarding:Boolean
    get() {
        return intent.getStringExtra(KEY_FRAGMENT) == STORAGE_BROWSER_ONBOARDING
    }


    override fun forcedTheme() =
        if (intent.getStringExtra(KEY_FRAGMENT) == STORAGE_BROWSER_ONBOARDING) R.style.Theme_VLC_Onboarding
        else null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
        setContentView(R.layout.secondary)
        initAudioPlayerContainerActivity()

        if (isOnboarding) WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val fph = findViewById<View>(R.id.fragment_placeholder)
        val params = fph.layoutParams as CoordinatorLayout.LayoutParams

        if (AndroidDevices.isTv) {
            applyOverscanMargin(this)
            params.topMargin = resources.getDimensionPixelSize(UiTools.getResourceFromAttribute(this, R.attr.actionBarSize))
        } else
            params.behavior = AppBarLayout.ScrollingViewBehavior()
        fph.requestLayout()

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (supportFragmentManager.findFragmentById(R.id.fragment_placeholder) == null) {
            val fragmentId = intent.getStringExtra(KEY_FRAGMENT)
            if (fragmentId == STREAMS) {
                setupStreamsContent(fph as ViewGroup)
            } else if (fragmentId == HISTORY) {
                setupHistoryContent(fph as ViewGroup)
            } else if (fragmentId == VIDEO_GROUP_LIST) {
                setupVideoGroupContent(fph as ViewGroup)
            } else if (fragmentId == ALBUMS_SONGS) {
                setupAudioAlbumsSongsContent(fph as ViewGroup)
            } else {
                fragmentId?.let { fetchSecondaryFragment(it) }
                if (fragment == null) {
                    finish()
                    return
                }
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_placeholder, fragment!!)
                    .commit()
            }
        }
        dialogsDelegate.observeDialogs(this, this)
        if (intent.getBooleanExtra(KEY_ANIMATED, false)) supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_up)
    }

    override fun onStart() {
        super.onStart()
        streamsModel?.refresh()
        historyModel?.refresh()
        videoGroupController?.onVisible()
        audioAlbumsSongsController?.onVisible()
        when (intent.getStringExtra(KEY_FRAGMENT)) {
            STREAMS -> supportActionBar?.setTitle(R.string.streams)
            HISTORY -> supportActionBar?.setTitle(R.string.history)
        }
    }

    override fun onStop() {
        videoGroupController?.onHidden()
        audioAlbumsSongsController?.onHidden()
        super.onStop()
    }

    override fun fireDialog(dialog: Dialog) {
        DialogActivity.dialog = dialog
        startActivity(Intent(DialogActivity.KEY_DIALOG, null, this, DialogActivity::class.java))
    }

    override fun dialogCanceled(dialog: Dialog?) {}

    override fun onResume() {
        if (!intent.getBooleanExtra(KEY_ANIMATED, false)) overridePendingTransition(0, 0)
        super.onResume()
        checkStreamsClipboard()
    }

    override fun onPause() {
        if (!intent.getBooleanExtra(KEY_ANIMATED, false) && isFinishing)
            overridePendingTransition(0, 0)
        super.onPause()
    }

    override fun finish() {
        super.finish()
        if (intent.getBooleanExtra(KEY_ANIMATED, false)) overridePendingTransition(R.anim.no_animation, R.anim.slide_out_bottom)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_RESULT_SECONDARY) {
            if (resultCode == RESULT_RESCAN) {
                this.reloadLibrary()
                videoGroupController?.refresh()
                audioAlbumsSongsController?.refresh()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        if (isHistoryContent()) {
            setupHistoryFilterMenu(menu)
            menuInflater.inflate(R.menu.fragment_option_history, menu)
            historyCleanMenuItem = menu.findItem(R.id.ml_menu_clean)
            updateHistoryCleanMenuVisibility()
        }
        videoGroupController?.prepareOptionsMenu(menu)
        audioAlbumsSongsController?.prepareOptionsMenu(menu)
        return result
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.ml_menu_refresh)?.isVisible = Permissions.canReadStorage(this)
        menu?.findItem(R.id.incognito_mode)?.isChecked = Settings.getInstance(this).getBoolean(KEY_INCOGNITO, false)
        updateHistoryCleanMenuVisibility(menu?.findItem(R.id.ml_menu_clean))
        menu?.let { videoGroupController?.prepareOptionsMenu(it) }
        menu?.let { audioAlbumsSongsController?.prepareOptionsMenu(it) }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.ml_menu_clean -> {
                if (isHistoryContent()) {
                    showConfirmDeleteComposeDialog(
                        title = getString(R.string.clear_playback_history),
                        description = getString(R.string.clear_history_message),
                        buttonText = getString(R.string.clear_history)
                    )
                    return true
                }
            }
            R.id.ml_menu_refresh -> {
                if (Permissions.canReadStorage(this)) {
                    val ml = Medialibrary.getInstance()
                    if (!ml.isWorking) reloadLibrary()
                    videoGroupController?.refresh()
                    audioAlbumsSongsController?.refresh()
                }
                return true
            }
            R.id.incognito_mode -> {
                lifecycleScope.launch {
                    if (!UiTools.updateIncognitoMode(this@SecondaryActivity, item)) return@launch
                }
                return true
            }
        }
        if (videoGroupController?.onOptionsItemSelected(item) == true) return true
        if (audioAlbumsSongsController?.onOptionsItemSelected(item) == true) return true
        return super.onOptionsItemSelected(item)
    }

    override fun hideRenderers() = intent.getStringExtra(KEY_FRAGMENT) == STORAGE_BROWSER_ONBOARDING

    private fun setupStreamsContent(container: ViewGroup) {
        val model = ViewModelProvider(this, StreamsModel.Factory(this))[StreamsModel::class.java]
        streamsModel = model
        val itemsState = mutableStateOf<List<MediaWrapper>>(emptyList())
        val loadingState = mutableStateOf(false)
        val searchTextState = mutableStateOf(model.observableSearchText.get().orEmpty())
        val clipboardTextState = mutableStateOf<String?>(null)
        streamsSearchText = searchTextState
        streamsClipboardText = clipboardTextState

        container.removeAllViews()
        ComposeView(this).apply {
            streamsRoot = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    StreamPanelContent(
                        streams = itemsState.value,
                        loading = loadingState.value,
                        searchText = searchTextState.value,
                        clipboardText = clipboardTextState.value,
                        isTv = AndroidDevices.isTv,
                        tvOverscanHorizontal = resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal),
                        tvOverscanVertical = resources.getDimensionPixelSize(R.dimen.tv_overscan_vertical),
                        onSearchTextChanged = {
                            searchTextState.value = it
                            model.observableSearchText.set(it)
                        },
                        onSubmit = { playStreamUri(searchTextState.value) },
                        onStreamClicked = ::playStream,
                        onStreamLongClicked = ::showStreamContext,
                        onMoreClicked = ::showStreamContext
                    )
                }
            }
            container.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        model.dataset.observe(this) { itemsState.value = it.orEmpty() }
        model.loading.observe(this) { loadingState.value = it == true }
        supportFragmentManager.setFragmentResultListener(CONFIRM_RENAME_DIALOG_RESULT, this) { _, bundle ->
            val media = bundle.parcelable<MediaWrapper>(RENAME_DIALOG_MEDIA) ?: return@setFragmentResultListener
            val name = bundle.getString(RENAME_DIALOG_NEW_NAME) ?: return@setFragmentResultListener
            model.rename(media, name)
        }
        PlaybackService.lastError.observe(this) {
            if (it != null && streamsModel != null) {
                searchTextState.value = it
                model.observableSearchText.set(it)
                PlaybackService.lastError.value = null
            }
        }
        checkStreamsClipboard()
        model.refresh()
    }

    private fun checkStreamsClipboard() {
        val model = streamsModel ?: return
        try {
            val clipBoardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = clipBoardManager?.primaryClip?.getItemAt(0)?.text?.toString()
            if (text.isValidUrl()) {
                streamsSearchText?.value = text
                streamsClipboardText?.value = text
                model.observableSearchText.set(text)
            }
        } catch (e: Exception) {
        }
    }

    private fun playStreamUri(uriText: String): Boolean {
        val trimmed = uriText.trim()
        if (trimmed.isEmpty()) return false
        playStream(MLServiceLocator.getAbstractMediaWrapper(trimmed.toUri()))
        streamsSearchText?.value = ""
        streamsModel?.observableSearchText?.set("")
        return true
    }

    private fun playStream(media: MediaWrapper) {
        media.type = MediaWrapper.TYPE_STREAM
        if (media.uri.scheme?.startsWith("rtsp") == true) VideoPlayerActivity.start(this, media.uri)
        else MediaUtils.openMedia(this, media)
        invalidateOptionsMenu()
        streamsRoot?.let { UiTools.setKeyboardVisibility(it, false) }
    }

    private fun showStreamContext(position: Int) {
        val media = streamsModel?.dataset?.get(position) ?: return
        val flags = FlagSet(ContextOption::class.java).apply {
            addAll(CTX_ADD_SHORTCUT, CTX_ADD_TO_PLAYLIST, CTX_APPEND, CTX_COPY, CTX_DELETE, CTX_RENAME)
        }
        showContext(this, object : CtxActionReceiver {
            override fun onCtxAction(position: Int, option: ContextOption) {
                onStreamContextAction(position, option)
            }
        }, position, media, flags)
    }

    private fun onStreamContextAction(position: Int, option: ContextOption) {
        val model = streamsModel ?: return
        val media = model.dataset.get(position)
        when (option) {
            CTX_RENAME -> showRenameComposeDialog(media)
            CTX_APPEND -> MediaUtils.appendMedia(this, media)
            CTX_ADD_TO_PLAYLIST -> addToPlaylist(media.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            CTX_COPY -> {
                copy(media.title, media.location)
                Snackbar.make(window.decorView.findViewById<View>(android.R.id.content), R.string.url_copied_to_clipboard, Snackbar.LENGTH_LONG).show()
            }
            CTX_DELETE -> {
                model.deletingMedia = media
                UiTools.snackerWithCancel(this, getString(R.string.stream_deleted), action = { model.delete() }) {
                    model.deletingMedia = null
                    model.refresh()
                }
                model.refresh()
            }
            CTX_ADD_SHORTCUT -> lifecycleScope.launch { createShortcut(media) }
            else -> {}
        }
    }

    private fun setupHistoryContent(container: ViewGroup) {
        val model = ViewModelProvider(this, HistoryModel.Factory(this))[HistoryModel::class.java]
        historyModel = model
        val itemsState = mutableStateOf<List<MediaWrapper>>(emptyList())
        val loadingState = mutableStateOf(false)
        val selectionState = mutableStateOf<Set<Int>>(emptySet())
        historyItems = itemsState
        historySelection = selectionState

        container.removeAllViews()
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    HistoryScreenContent(
                        items = itemsState.value,
                        loading = loadingState.value,
                        selectedPositions = selectionState.value,
                        onRefresh = { model.refresh() },
                        onItemClicked = ::onHistoryItemClicked,
                        onItemLongClicked = ::onHistoryItemLongClicked,
                        onRemoveClicked = ::removeHistoryItem
                    )
                }
            }
            container.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        model.dataset.observe(this) { list ->
            itemsState.value = list.orEmpty()
            selectionState.value = selectionState.value.filter { it < itemsState.value.size }.toSet()
            updateHistoryCleanMenuVisibility()
            historyActionMode?.invalidate()
        }
        model.loading.observe(this) { loadingState.value = it == true }
        supportFragmentManager.setFragmentResultListener(CONFIRM_DELETE_DIALOG_RESULT, this) { _, _ ->
            clearHistory()
            finish()
        }
        model.refresh()
    }

    private fun setupVideoGroupContent(container: ViewGroup) {
        val controller = videoGroupController ?: SecondaryVideoScreenController(
            activity = this,
            folder = intent.parcelable(KEY_FOLDER),
            group = intent.parcelable(KEY_GROUP)
        ).also { videoGroupController = it }

        container.removeAllViews()
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    controller.Content()
                }
            }
            container.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        controller.onVisible()
    }

    private fun setupAudioAlbumsSongsContent(container: ViewGroup) {
        val media = intent.parcelable<MediaLibraryItem>(TAG_ITEM)
        if (media == null) {
            finish()
            return
        }
        val controller = audioAlbumsSongsController ?: SecondaryAudioScreenController(
            activity = this,
            parent = media,
            fromAlbums = intent.getBooleanExtra(HeaderMediaListActivity.ARTIST_FROM_ALBUM, false)
        ).also { audioAlbumsSongsController = it }

        container.removeAllViews()
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    controller.Content()
                }
            }
            container.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        controller.onVisible()
    }

    private fun setupHistoryFilterMenu(menu: Menu) {
        val item = menu.findItem(R.id.ml_menu_filter) ?: return
        val model = historyModel ?: return
        item.isVisible = true
        val searchView = item.actionView as? SearchView ?: return
        searchView.queryHint = getString(R.string.search_in_list_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty()
                if (query.isEmpty()) model.restore() else model.filter(query)
                return true
            }
        })
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                model.restore()
                return true
            }
        })
        model.filterQuery?.takeIf { it.isNotEmpty() }?.let { query ->
            searchView.post {
                item.expandActionView()
                searchView.clearFocus()
                UiTools.setKeyboardVisibility(searchView, false)
                searchView.setQuery(query, false)
            }
        }
    }

    private fun updateHistoryCleanMenuVisibility(item: MenuItem? = historyCleanMenuItem) {
        item?.isVisible = isHistoryContent() &&
                Settings.getInstance(this).getBoolean(PLAYBACK_HISTORY, true) &&
                historyItems?.value.orEmpty().isNotEmpty()
    }

    private fun isHistoryContent() = intent.getStringExtra(KEY_FRAGMENT) == HISTORY

    private fun onHistoryItemClicked(position: Int, item: MediaWrapper) {
        if (KeyHelper.isShiftPressed && historyActionMode == null) {
            onHistoryItemLongClicked(position, item)
            return
        }
        if (historyActionMode != null) {
            toggleHistorySelection(position)
            return
        }
        if (position != 0) historyModel?.moveUp(item)
        MediaUtils.openMedia(this, item)
    }

    private fun onHistoryItemLongClicked(position: Int, @Suppress("UNUSED_PARAMETER") item: MediaWrapper) {
        toggleHistorySelection(position, forceSelected = true)
    }

    private fun toggleHistorySelection(position: Int, forceSelected: Boolean = false) {
        val state = historySelection ?: return
        val selection = state.value.toMutableSet()
        if (forceSelected) selection.add(position)
        else if (!selection.add(position)) selection.remove(position)
        state.value = selection
        if (selection.isNotEmpty() && historyActionMode == null) startHistoryActionMode()
        if (selection.isEmpty()) historyActionMode?.finish() else historyActionMode?.invalidate()
    }

    private fun startHistoryActionMode() {
        historyActionMode = startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.action_mode_history, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val selection = selectedHistoryItems()
                if (selection.isEmpty()) {
                    mode.finish()
                    return false
                }
                mode.title = getString(R.string.selection_count, selection.size)
                menu.findItem(R.id.action_history_info).isVisible = selection.size == 1
                menu.findItem(R.id.action_history_append).isVisible = PlaylistManager.hasMedia()
                menu.findItem(R.id.action_go_to_folder).isVisible = selection.size == 1 && selection.first().uri.retrieveParent() != null
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val selection = selectedHistoryItems()
                if (selection.isNotEmpty()) {
                    when (item.itemId) {
                        R.id.action_history_play -> MediaUtils.openList(this@SecondaryActivity, selection, 0)
                        R.id.action_history_append -> MediaUtils.appendMedia(this@SecondaryActivity, selection)
                        R.id.action_history_info -> showHistoryInfoDialog(selection.first())
                        R.id.action_go_to_folder -> showHistoryParentFolder(selection.first())
                        else -> {
                            mode.finish()
                            return false
                        }
                    }
                }
                mode.finish()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                historyActionMode = null
                historySelection?.value = emptySet()
            }
        })
    }

    private fun selectedHistoryItems(): List<MediaWrapper> {
        val items = historyItems?.value.orEmpty()
        return historySelection?.value.orEmpty().sorted().mapNotNull { items.getOrNull(it) }
    }

    private fun removeHistoryItem(@Suppress("UNUSED_PARAMETER") position: Int, item: MediaWrapper) {
        if (!showPinIfNeeded()) historyModel?.removeFromHistory(item)
    }

    private fun clearHistory() {
        Medialibrary.getInstance().clearHistory(Medialibrary.HISTORY_TYPE_GLOBAL)
        historyModel?.clearHistory()
        Settings.getInstance(this).edit().remove(KEY_AUDIO_LAST_PLAYLIST).remove(KEY_MEDIA_LAST_PLAYLIST).apply()
        updateHistoryCleanMenuVisibility()
    }

    private fun showHistoryInfoDialog(media: MediaWrapper) {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra(TAG_ITEM, media)
        startActivity(intent)
    }

    private fun showHistoryParentFolder(media: MediaWrapper) {
        val parent = MLServiceLocator.getAbstractMediaWrapper(media.uri.retrieveParent()).apply {
            type = MediaWrapper.TYPE_DIR
        }
        startActivity(Intent(applicationContext, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, parent)
            putExtra(KEY_JUMP_TO, media)
            putExtra(KEY_FRAGMENT, FILE_BROWSER)
        })
    }

    private fun fetchSecondaryFragment(id: String) {
        when (id) {
            ALBUMS_SONGS -> {
                throw IllegalArgumentException("Audio albums/songs routes are hosted by Compose.")
            }
            VIDEO_GROUP_LIST -> {
                throw IllegalArgumentException("Video group routes are hosted by Compose.")
            }
            STORAGE_BROWSER, STORAGE_BROWSER_ONBOARDING -> {
                fragment = MLStorageBrowserFragment.newInstance(id == STORAGE_BROWSER_ONBOARDING)
                setResult(RESULT_RESTART)
            }
            FILE_BROWSER -> {
                (intent.parcelable(KEY_MEDIA) as? MediaWrapper)?.let { media ->
                    fragment = if (media.uri.scheme.isSchemeNetwork()) NetworkBrowserFragment()
                    else FileBrowserFragment()
                    fragment?.apply { arguments = bundleOf(KEY_MEDIA to media, KEY_JUMP_TO to intent.parcelable(KEY_JUMP_TO)) }
                }
            }
            else -> throw IllegalArgumentException("Wrong fragment id.")
        }
    }

    companion object {
        const val TAG = "VLC/SecondaryActivity"

        const val ACTIVITY_RESULT_SECONDARY = 3

        const val KEY_FRAGMENT = "fragment"

        const val ALBUMS_SONGS = "albumsSongs"
        const val STREAMS = "streams"
        const val HISTORY = "history"
        const val VIDEO_GROUP_LIST = "videoGroupList"
        const val STORAGE_BROWSER = "storage_browser"
        const val STORAGE_BROWSER_ONBOARDING = "storage_browser_onboarding"
        const val FILE_BROWSER = "file_browser"
    }
}
