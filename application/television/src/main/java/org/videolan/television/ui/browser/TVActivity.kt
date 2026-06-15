package org.videolan.television.ui.browser

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.HEADER_STREAM
import org.videolan.television.ui.MainTvActivity
import org.videolan.tools.copy
import org.videolan.tools.isValidUrl
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.network.StreamPanelContent
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_SHORTCUT
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_COPY
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.viewmodels.StreamsModel

class TVActivity : BaseTvActivity() {

    private var streamsModel: StreamsModel? = null
    private var streamsRoot: View? = null
    private var streamsSearchText: MutableState<String>? = null
    private var streamsClipboardText: MutableState<String?>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getLongExtra(MainTvActivity.BROWSER_TYPE, -1)
        if (type != HEADER_STREAM) {
            finish()
            return
        }
        setupStreamsContent()
    }

    override fun onStart() {
        super.onStart()
        streamsModel?.refresh()
    }

    override fun onResume() {
        super.onResume()
        checkStreamsClipboard()
    }

    private fun setupStreamsContent() {
        val model = ViewModelProvider(this, StreamsModel.Factory(this))[StreamsModel::class.java]
        streamsModel = model
        val itemsState = mutableStateOf<List<MediaWrapper>>(emptyList())
        val loadingState = mutableStateOf(false)
        val searchTextState = mutableStateOf(model.observableSearchText.get().orEmpty())
        val clipboardTextState = mutableStateOf<String?>(null)
        streamsSearchText = searchTextState
        streamsClipboardText = clipboardTextState

        setContentView(
            ComposeView(this).apply {
                streamsRoot = this
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme(darkTheme = true) {
                        StreamPanelContent(
                            streams = itemsState.value,
                            loading = loadingState.value,
                            searchText = searchTextState.value,
                            clipboardText = clipboardTextState.value,
                            isTv = true,
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
            }
        )

        model.dataset.observe(this) { itemsState.value = it.orEmpty() }
        model.loading.observe(this) { loadingState.value = it == true }
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
        streamsRoot?.let { UiTools.setKeyboardVisibility(it, false) }
    }

    private fun showStreamContext(position: Int) {
        val media = streamsModel?.dataset?.get(position) ?: return
        val flags = FlagSet(ContextOption.entries.toList()).apply {
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
            CTX_RENAME -> showRenameComposeDialog(media) { renamedMedia, name ->
                model.rename(renamedMedia as MediaWrapper, name)
            }
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

    override fun refresh() {
        streamsModel?.refresh()
    }
}
