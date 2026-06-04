package org.videolan.vlc.gui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.format.Formatter
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.interfaces.IMediaFactory
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Artist
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.TAG_ITEM
import org.videolan.resources.VLCInstance
import org.videolan.resources.util.parcelable
import org.videolan.tools.readableSize
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCInfoItem
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.getAllTracks
import org.videolan.vlc.gui.browser.PathAdapterListener
import org.videolan.vlc.gui.browser.buildPathSegments
import org.videolan.vlc.gui.helpers.AudioUtil
import org.videolan.vlc.gui.helpers.PlayerBehavior
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.UiTools.getResourceFromAttribute
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.LocaleUtil
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.generateResolutionClass
import org.videolan.vlc.util.getModel
import org.videolan.vlc.util.getScreenWidth
import org.videolan.vlc.util.isSchemeSupported
import org.videolan.vlc.util.setLayoutMarginTop
import org.videolan.vlc.viewmodels.browser.IPathOperationDelegate
import org.videolan.vlc.viewmodels.browser.PathOperationDelegate
import java.io.File

private const val TAG_FAB_VISIBILITY = "FAB"

class InfoActivity : AudioPlayerContainerActivity(), PathAdapterListener,
        IPathOperationDelegate by PathOperationDelegate() {

    private lateinit var item: MediaLibraryItem
    private lateinit var model: InfoModel
    private lateinit var infoContent: ComposeView
    private var uiState by mutableStateOf(InfoUiState())
    private var trackRows by mutableStateOf(emptyList<InfoTrackRow>())
    private var fabVisible by mutableStateOf(true)
    override var isEdgeToEdge = false

    override fun isTransparent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        super.onCreate(savedInstanceState)

        val incoming = if (savedInstanceState != null)
            savedInstanceState.parcelable<Parcelable>(TAG_ITEM) as MediaLibraryItem?
        else
            intent.parcelable<Parcelable>(TAG_ITEM) as MediaLibraryItem?
        if (incoming == null) {
            finish()
            return
        }

        item = incoming
        if (item.id == 0L) {
            val libraryItem = Medialibrary.getInstance().getMedia((item as MediaWrapper).uri)
            if (libraryItem != null) item = libraryItem
        }
        fabVisible = savedInstanceState?.getInt(TAG_FAB_VISIBILITY)?.let { it == View.VISIBLE } ?: true
        uiState = uiState.copy(item = item, scanned = true)

        setContentView(createInfoActivityShell())
        initAudioPlayerContainerActivity()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = bars.bottom,
            )
            setLayoutMarginTop(toolbar, bars.top)
            WindowInsetsCompat.CONSUMED
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        model = getModel()

        if (item is MediaWrapper) {
            val media = item as MediaWrapper
            if (model.sizeText.value === null) model.checkFile(media)
            if (model.mediaTracks.value === null) model.parseTracks(this, media)
        }
        model.hasSubs.observe(this) { hasSubs ->
            uiState = uiState.copy(showSubtitles = hasSubs)
        }
        model.mediaTracks.observe(this) { tracks ->
            trackRows = tracks.map { it.toInfoTrackRow(resources) }
        }
        model.sizeText.observe(this) { size ->
            uiState = uiState.copy(
                showFileSize = size != -1L,
                sizeValueText = if (size != -1L) Formatter.formatFileSize(this, size) else ""
            )
        }
        model.cover.observe(this) { cover ->
            if (cover != null) {
                uiState = uiState.copy(cover = cover)
            } else {
                noCoverFallback()
            }
        }
        if (model.cover.value === null) model.getCover(item, getScreenWidth())
        updateMeta()
    }

    private fun createInfoActivityShell(): View {
        val root = CoordinatorLayout(this).apply {
            id = R.id.coordinator
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        root.addView(createInfoToolbar())

        infoContent = ComposeView(this).apply {
            id = R.id.content_placeholder
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    InfoScreen(
                        state = uiState,
                        trackRows = trackRows,
                        fabVisible = fabVisible,
                        strings = InfoStrings(
                            length = getString(R.string.length),
                            add = getString(R.string.add),
                            directoryNotScanned = getString(R.string.directory_not_scanned),
                            play = getString(R.string.play)
                        ),
                        pathAdapterListener = this@InfoActivity,
                        onAddDirectory = ::addCurrentDirectoryToMedialibrary,
                        onPlay = ::playItem
                    )
                }
            }
        }
        root.addView(
            infoContent,
            CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT).apply {
                behavior = AppBarLayout.ScrollingViewBehavior()
            }
        )

        root.addView(createInfoAudioPlayerContainer())
        root.addView(
            createAudioPlayerTipsComposeHost(),
            CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT)
        )
        return root
    }

    private fun createInfoToolbar(): AppBarLayout {
        val appBar = AppBarLayout(this).apply {
            id = R.id.appbar
            setBackgroundResource(getResourceFromAttribute(this@InfoActivity, R.attr.background_actionbar))
            elevation = 0f
            ViewCompat.setElevation(this, 0f)
        }
        val toolbar = MaterialToolbar(ContextThemeWrapper(this, R.style.Toolbar_VLC)).apply {
            id = R.id.main_toolbar
            navigationContentDescription = getString(R.string.abc_action_bar_up_description)
            navigationIcon = ContextCompat.getDrawable(context, getResourceFromAttribute(context, androidx.appcompat.R.attr.homeAsUpIndicator))
            popupTheme = getResourceFromAttribute(context, R.attr.toolbar_popup_style)
            titleMarginStart = resources.getDimensionPixelSize(R.dimen.default_margin)
        }
        appBar.addView(
            toolbar,
            AppBarLayout.LayoutParams(AppBarLayout.LayoutParams.MATCH_PARENT, actionBarSize()).apply {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            }
        )
        return appBar
    }

    private fun createInfoAudioPlayerContainer() = FrameLayout(this).apply {
        id = R.id.audio_player_container
        elevation = resources.getDimension(R.dimen.audio_player_elevation)
        visibility = View.GONE
        addView(
            context.createAudioPlayerHostView(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        addView(
            context.createAudioPlaylistTipsComposeHost(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        layoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT).apply {
            behavior = PlayerBehavior<FrameLayout>()
        }
    }

    private fun updateMeta() = lifecycleScope.launchWhenStarted {
        var length = 0L
        val tracks = item.tracks
        val nbTracks = tracks?.size ?: 0
        if (nbTracks > 0) for (media in tracks!!) length += media.length

        var nextState = uiState.copy(
            item = item,
            length = if (length > 0) length else null,
            scanned = true
        )

        if (item is MediaWrapper) {
            val media = item as MediaWrapper
            val resolution = generateResolutionClass(media.width, media.height)
            val progress = if (media.length == 0L || length == 0L) 0 else (100L * media.time / length).toInt()
            var scanned = true
            val pathMedia = if (isSchemeSupported(media.uri?.scheme)) {
                scanned = Medialibrary.getInstance().foldersList.any {
                    media.uri.toString().startsWith(it.toUri().toString())
                }
                media
            } else {
                null
            }
            nextState = nextState.copy(
                resolution = resolution,
                progress = progress,
                pathMedia = pathMedia,
                scanned = scanned,
                sizeTitleText = getString(R.string.file_size),
                sizeIcon = R.drawable.ic_storage
            )
        } else if (item.itemType == MediaLibraryItem.TYPE_ARTIST) {
            val albums = (item as Artist).albums
            val nbAlbums = albums?.size ?: 0
            nextState = nextState.copy(
                sizeTitleText = getString(R.string.albums),
                sizeValueText = nbAlbums.toString(),
                sizeIcon = R.drawable.ic_album,
                showFileSize = true,
                extraTitleText = getString(R.string.tracks),
                extraValueText = nbTracks.toString(),
                extraIcon = R.drawable.ic_song_small
            )
        } else {
            nextState = nextState.copy(
                sizeTitleText = getString(R.string.tracks),
                sizeValueText = nbTracks.toString(),
                sizeIcon = R.drawable.ic_song_small,
                showFileSize = true
            )
        }
        uiState = nextState
    }

    private fun addCurrentDirectoryToMedialibrary() {
        val media = item as? MediaWrapper ?: return
        val parent = media.uri.toString().substring(0, media.uri.toString().lastIndexOf("/"))
        MedialibraryUtils.addDir(parent, applicationContext)
        Snackbar.make(infoContent, getString(R.string.scanned_directory_added, parent.toUri().lastPathSegment), Snackbar.LENGTH_LONG).show()
        uiState = uiState.copy(scanned = true)
    }

    private fun noCoverFallback() {
        uiState = uiState.copy(cover = null)
        fabVisible = true
    }

    private fun playItem() {
        MediaUtils.playTracks(this, item, 0)
        finish()
    }

    override fun backTo(tag: String) {
    }

    override fun currentContext() = this

    override fun showRoot() = false

    override fun getPathOperationDelegate() = this

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(TAG_ITEM, item)
        outState.putInt(TAG_FAB_VISIBILITY, if (fabVisible) View.VISIBLE else View.INVISIBLE)
    }

    override fun onPlayerStateChanged(bottomSheet: View, newState: Int) {
        if (fabVisible && newState != BottomSheetBehavior.STATE_COLLAPSED && newState != BottomSheetBehavior.STATE_HIDDEN)
            fabVisible = false
        else if (!fabVisible && (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN))
            fabVisible = true
    }

    private fun actionBarSize(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
        return if (typedValue.resourceId != 0) resources.getDimensionPixelSize(typedValue.resourceId)
        else TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    }
}

@Composable
private fun InfoScreen(
    state: InfoUiState,
    trackRows: List<InfoTrackRow>,
    fabVisible: Boolean,
    strings: InfoStrings,
    pathAdapterListener: PathAdapterListener,
    onAddDirectory: () -> Unit,
    onPlay: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDefault)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                InfoHeader(state = state)
            }
            if (state.progress > 0) {
                item {
                    InfoProgress(progress = state.progress)
                }
            }
            state.pathMedia?.let { media ->
                item {
                    InfoPathBreadcrumb(
                        media = media,
                        listener = pathAdapterListener,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
            if (!state.scanned) {
                item {
                    DirectoryNotScannedRow(
                        message = strings.directoryNotScanned,
                        add = strings.add,
                        onAddDirectory = onAddDirectory
                    )
                }
            }
            state.length?.let { length ->
                item {
                    InfoMetaRow(
                        icon = R.drawable.ic_duration,
                        title = strings.length,
                        value = Tools.millisToString(length),
                        contentDescription = TalkbackUtil.millisToString(pathAdapterListener.currentContext(), length)
                    )
                }
            }
            if (state.showFileSize && state.sizeTitleText.isNotEmpty() && state.sizeValueText.isNotEmpty()) {
                item {
                    InfoMetaRow(
                        icon = state.sizeIcon,
                        title = state.sizeTitleText,
                        value = state.sizeValueText
                    )
                }
            }
            if (state.extraTitleText.isNotEmpty() && state.extraValueText.isNotEmpty()) {
                item {
                    InfoMetaRow(
                        icon = state.extraIcon,
                        title = state.extraTitleText,
                        value = state.extraValueText
                    )
                }
            }
            if (state.item?.itemType == MediaLibraryItem.TYPE_MEDIA) {
                items(trackRows) { row ->
                    VLCInfoItem(
                        title = row.title,
                        subtitle = row.subtitle,
                        leadingContent = {
                            Text(
                                text = row.leadingText,
                                color = VLCThemeDefaults.colors.fontAudioLight,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
        if (fabVisible) {
            FloatingActionButton(
                onClick = onPlay,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fab_play),
                    contentDescription = strings.play
                )
            }
        }
    }
}

@Composable
private fun InfoHeader(state: InfoUiState) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (state.cover != null) 300.dp else 144.dp)
            .background(colors.backgroundDefaultDarker)
    ) {
        state.cover?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = state.item?.title.orEmpty(),
                color = if (state.cover != null) androidx.compose.ui.graphics.Color.White else colors.fontDefault,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            state.item?.description?.takeIf { it.isNotEmpty() }?.let { description ->
                Text(
                    text = description,
                    color = if (state.cover != null) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.82f) else colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            state.resolution?.let { resolution ->
                Text(
                    text = resolution,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            if (state.showSubtitles) {
                Icon(
                    painter = painterResource(R.drawable.ic_audiosub_info),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.58f))
                        .padding(6.dp)
                        .size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoProgress(progress: Int) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(colors.defaultDivider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((progress.coerceIn(0, 100) / 100f))
                .height(4.dp)
                .background(colors.primary)
        )
    }
}

@Composable
private fun InfoPathBreadcrumb(media: MediaWrapper, listener: PathAdapterListener, modifier: Modifier = Modifier) {
    val segments = buildPathSegments(listener, media)
    LazyRow(modifier = modifier) {
        itemsIndexed(segments) { index, segment ->
            val semanticModifier = segment.contentDescription?.let { description ->
                Modifier.semantics { contentDescription = description }
            } ?: Modifier
            Text(
                text = segment.text,
                color = VLCThemeDefaults.colors.fontLight,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                modifier = semanticModifier
                    .clickable(enabled = segment.enabled) { listener.backTo(segment.tag) }
                    .focusable()
                    .padding(top = 3.dp, bottom = 4.dp)
            )
            if (index != segments.lastIndex) {
                Icon(
                    painter = painterResource(R.drawable.ic_divider),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DirectoryNotScannedRow(message: String, add: String, onAddDirectory: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onAddDirectory) {
            Text(text = add)
        }
    }
}

@Composable
private fun InfoMetaRow(
    @DrawableRes icon: Int,
    title: String,
    value: String,
    contentDescription: String? = null
) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.fontAudioLight,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column {
            Text(
                text = title,
                color = colors.fontAudioLight,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = value,
                color = colors.fontAudioLight,
                style = MaterialTheme.typography.bodyMedium,
                modifier = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
            )
        }
    }
}

private data class InfoUiState(
    val item: MediaLibraryItem? = null,
    val cover: Bitmap? = null,
    val resolution: String? = null,
    val showSubtitles: Boolean = false,
    val length: Long? = null,
    val progress: Int = 0,
    val pathMedia: MediaWrapper? = null,
    val scanned: Boolean = true,
    val showFileSize: Boolean = false,
    val sizeTitleText: String = "",
    val sizeValueText: String = "",
    @DrawableRes val sizeIcon: Int = R.drawable.ic_storage,
    val extraTitleText: String = "",
    val extraValueText: String = "",
    @DrawableRes val extraIcon: Int = R.drawable.ic_song
)

private data class InfoTrackRow(
    val title: String,
    val subtitle: String,
    val leadingText: String
)

private data class InfoStrings(
    val length: String,
    val add: String,
    val directoryNotScanned: String,
    val play: String
)

private fun IMedia.Track.toInfoTrackRow(res: android.content.res.Resources): InfoTrackRow {
    val title: String
    val textBuilder = StringBuilder()
    val leadingText: String
    when (type) {
        IMedia.Track.Type.Audio -> {
            title = res.getString(R.string.track_audio)
            leadingText = "A"
            appendCommonInfo(textBuilder, res, this)
            appendAudioInfo(textBuilder, res, this as IMedia.AudioTrack)
        }
        IMedia.Track.Type.Video -> {
            title = res.getString(R.string.track_video)
            leadingText = "V"
            appendCommonInfo(textBuilder, res, this)
            appendVideoInfo(textBuilder, res, this as IMedia.VideoTrack)
        }
        IMedia.Track.Type.Text -> {
            title = res.getString(R.string.track_text)
            leadingText = "T"
            appendCommonInfo(textBuilder, res, this)
        }
        else -> {
            title = res.getString(R.string.track_unknown)
            leadingText = "?"
        }
    }
    return InfoTrackRow(title, textBuilder.toString(), leadingText)
}

private fun appendCommonInfo(textBuilder: StringBuilder, res: android.content.res.Resources, track: IMedia.Track) {
    if (track.bitrate != 0)
        textBuilder.append(res.getString(R.string.track_bitrate_info, track.bitrate.toLong().readableSize()))
    textBuilder.append(res.getString(R.string.track_codec_info, track.codec))
    if (track.language != null && !track.language.equals("und", ignoreCase = true))
        textBuilder.append(res.getString(R.string.track_language_info, LocaleUtil.getLocaleName(track.language)))
}

private fun appendAudioInfo(textBuilder: StringBuilder, res: android.content.res.Resources, track: IMedia.AudioTrack) {
    textBuilder.append(res.getQuantityString(R.plurals.track_channels_info_quantity, track.channels, track.channels))
    textBuilder.append(res.getString(R.string.track_samplerate_info, track.rate))
}

private fun appendVideoInfo(textBuilder: StringBuilder, res: android.content.res.Resources, track: IMedia.VideoTrack) {
    val frameRate = track.frameRateNum / track.frameRateDen.toDouble()
    if (track.width != 0 && track.height != 0)
        textBuilder.append(res.getString(R.string.track_resolution_info, track.width, track.height))
    if (!frameRate.isNaN())
        textBuilder.append(res.getString(R.string.track_framerate_info, frameRate))
}

class InfoModel : ViewModel() {

    val hasSubs = MutableLiveData<Boolean>()
    val mediaTracks = MutableLiveData<List<IMedia.Track>>()
    val sizeText = MutableLiveData<Long>()
    val cover = MutableLiveData<Bitmap>()
    private val mediaFactory = FactoryManager.getFactory(IMediaFactory.factoryId) as IMediaFactory

    fun getCover(item: MediaLibraryItem?, width: Int) = viewModelScope.launch {
        item?.let { item ->
            cover.value = item.artworkMrl?.let {
                withContext(Dispatchers.IO) { AudioUtil.fetchCoverBitmap(Uri.decode(it), width) }
            } ?: (item as? MediaWrapper)?.let { media ->
                if (item.type == MediaWrapper.TYPE_VIDEO) withContext(Dispatchers.IO) { ThumbnailsProvider.getVideoThumbnail(media, width) } else null
            }
        }
    }

    fun parseTracks(context: Context, mw: MediaWrapper) = viewModelScope.launch {
        val media = withContext(Dispatchers.IO) {
            val libVlc = VLCInstance.getInstance(context)
            mediaFactory.getFromUri(libVlc, mw.uri).apply { parse() }
        }
        if (!isActive) return@launch
        val tracks = media.getAllTracks()
        val subs = tracks.asReversed().any { it.type == IMedia.Track.Type.Text }
        media.release()
        hasSubs.value = subs
        mediaTracks.value = tracks
    }

    fun checkFile(mw: MediaWrapper) = viewModelScope.launch {
        val itemFile = withContext(Dispatchers.IO) { File(Uri.decode(mw.location.substring(5))) }

        if (!withContext(Dispatchers.IO) { itemFile.exists() } || !isActive) {
            sizeText.value = -1L
            return@launch
        }
        if (mw.type == MediaWrapper.TYPE_VIDEO) checkSubtitles(itemFile)
        sizeText.value = itemFile.length()
    }

    private suspend fun checkSubtitles(itemFile: File) = withContext(Dispatchers.IO) {
        var extension: String
        var filename: String
        var videoName = Uri.decode(itemFile.name)
        val parentPath = Uri.decode(itemFile.parent)
        videoName = videoName.substring(0, videoName.lastIndexOf('.'))
        val subFolders = arrayOf("/Subtitles", "/subtitles", "/Subs", "/subs")
        var files: Array<String>? = itemFile.parentFile?.list()
        var filesLength = files?.size ?: 0
        for (subFolderName in subFolders) {
            val subFolder = File(parentPath + subFolderName)
            if (!subFolder.exists()) continue
            val subFiles = subFolder.list()
            var subFilesLength = 0
            var newFiles = arrayOfNulls<String>(0)
            if (subFiles != null) {
                subFilesLength = subFiles.size
                newFiles = arrayOfNulls(filesLength + subFilesLength)
                System.arraycopy(subFiles, 0, newFiles, 0, subFilesLength)
            }
            files?.let { System.arraycopy(it, 0, newFiles, subFilesLength, filesLength) }
            files = newFiles.filterNotNull().toTypedArray()
            filesLength = files.size
        }
        if (files != null) for (i in 0 until filesLength) {
            filename = Uri.decode(files[i])
            val index = filename.lastIndexOf('.')
            if (index <= 0) continue
            extension = filename.substring(index)
            if (index <= 0 || !org.videolan.libvlc.util.Extensions.SUBTITLES.contains(extension)) continue
            if (!isActive) return@withContext
            if (filename.startsWith(videoName)) {
                hasSubs.postValue(true)
                return@withContext
            }
        }
    }
}
