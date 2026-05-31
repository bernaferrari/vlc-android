/*
 * *************************************************************************
 *  FilePickerActivity.java
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

package org.videolan.vlc.gui.browser

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.resources.util.parcelable
import org.videolan.tools.livedata.LiveDataset
import org.videolan.tools.removeFileScheme
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.providers.FilePickerProvider
import org.videolan.vlc.providers.PickerType
import org.videolan.vlc.repository.DirectoryRepository
import org.videolan.vlc.util.FileUtils
import kotlin.reflect.jvm.jvmName

const val EXTRA_MRL = "sub_mrl"

/**
 * Full Compose file picker for subtitles, soundfonts, settings, and equalizer imports.
 *
 * This Activity owns the picker state directly and reuses [FilePickerProvider] so file
 * filtering, root discovery, and result semantics stay aligned with the legacy fragment path.
 */
class FilePickerActivity : BaseActivity() {
    private val dataset = LiveDataset<MediaLibraryItem>()
    private var provider: FilePickerProvider? = null
    private var loadingObserver: Observer<Boolean>? = null
    private var pickerType = PickerType.SUBTITLE
    private var currentMedia by mutableStateOf<MediaWrapper?>(null)
    private var currentMrl by mutableStateOf<String?>(null)
    private var isRootDirectory by mutableStateOf(true)
    private var pickerItems by mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private var isLoading by mutableStateOf(false)

    override fun getSnackAnchorView(overAudioPlayer:Boolean): View? = findViewById(android.R.id.content)

    /**
     * Forces the dark theme if the dialog is opened from the VideoPlayerActivity
     */
    override fun forcedTheme() =
        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO && callingActivity?.className == VideoPlayerActivity::class.jvmName)
            R.style.Theme_VLC_PickerDialog_Dark
        else null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickerType = PickerType.entries.getOrElse(intent.getIntExtra(KEY_PICKER_TYPE, PickerType.SUBTITLE.ordinal)) { PickerType.SUBTITLE }
        currentMedia = intent.parcelable<MediaWrapper>(KEY_MEDIA)?.takeUnless { media ->
            val scheme = media.uri?.scheme
            media.uri == null || scheme == "http" || scheme == "content" || scheme == "fd"
        }
        currentMrl = currentMedia?.location ?: intent.dataString
        isRootDirectory = defineIsRoot(currentMrl)

        dataset.observe(this) { pickerItems = it.orEmpty() }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    FilePickerScreen(
                        title = getPickerTitle(),
                        pickerType = pickerType,
                        items = pickerItems,
                        isLoading = isLoading,
                        isRootDirectory = isRootDirectory,
                        onBack = ::browseUp,
                        onClose = ::finish,
                        onItemClick = ::onItemClick
                    )
                }
            }
        }
        setContentView(
            composeView,
            ViewGroup.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.file_picker_width),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        window.attributes.gravity = Gravity.BOTTOM
        bindProvider(currentMrl)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                browseUp()
            }
        })
    }

    private fun bindProvider(url: String?) {
        loadingObserver?.let { observer -> provider?.loading?.removeObserver(observer) }
        provider?.stop()
        provider?.release()
        dataset.value = mutableListOf()
        isLoading = true
        title = getPickerTitle()

        provider = FilePickerProvider(this, dataset, url, pickerType = pickerType).also { newProvider ->
            val observer = Observer<Boolean> { loading -> isLoading = loading == true }
            loadingObserver = observer
            newProvider.loading.observe(this, observer)
        }
    }

    private fun onItemClick(item: MediaLibraryItem) {
        val media = item as? MediaWrapper ?: return
        if (media.type == MediaWrapper.TYPE_DIR) {
            provider?.saveList(media)
            browseTo(media, media.location)
        } else {
            val result = Intent(Intent.ACTION_PICK).putExtra(EXTRA_MRL, media.location)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun browseTo(media: MediaWrapper?, mrl: String?) {
        currentMedia = media
        currentMrl = mrl
        isRootDirectory = defineIsRoot(mrl)
        bindProvider(mrl)
    }

    private fun browseUp() {
        when {
            isRootDirectory -> finish()
            currentMrl?.removeFileScheme() == AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY -> browseTo(null, null)
            currentMrl != null -> {
                val parent = FileUtils.getParent(currentMrl)?.toUri()?.let { uri ->
                    MLServiceLocator.getAbstractMediaWrapper(uri)
                }
                browseTo(parent, parent?.location)
            }
            else -> finish()
        }
    }

    private fun defineIsRoot(mrl: String?) = mrl?.run {
        if (startsWith("file")) {
            val path = removeFileScheme()
            val rootDirectories = runBlocking(Dispatchers.IO) {
                DirectoryRepository.getInstance(this@FilePickerActivity).getMediaDirectories()
            }
            for (directory in rootDirectories) if (path.startsWith(directory)) return false
            return true
        } else length < 7
    } != false

    private fun getPickerTitle(): String = if (isRootDirectory) {
        getString(R.string.directories)
    } else {
        currentMedia?.uri?.toString() ?: currentMrl.orEmpty()
    }

    override fun onDestroy() {
        loadingObserver?.let { observer -> provider?.loading?.removeObserver(observer) }
        provider?.stop()
        provider?.release()
        provider = null
        super.onDestroy()
    }
}

@Composable
private fun FilePickerScreen(
    title: String,
    pickerType: PickerType,
    items: List<MediaLibraryItem>,
    isLoading: Boolean,
    isRootDirectory: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onItemClick: (MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefaultDarker,
        contentColor = colors.fontDefault,
        modifier = Modifier
            .fillMaxSize()
            .heightIn(min = 320.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(start = 4.dp, end = 4.dp)
            ) {
                if (isRootDirectory) {
                    Spacer(modifier = Modifier.size(48.dp))
                } else {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.previous),
                            tint = colors.fontDefault
                        )
                    }
                }
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading && items.isNotEmpty()) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(20.dp)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_small),
                        contentDescription = stringResource(R.string.close),
                        tint = colors.fontDefault
                    )
                }
            }
            HorizontalDivider(color = colors.defaultDivider)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    items.isEmpty() && isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    items.isEmpty() -> Text(
                        text = stringResource(R.string.no_subs_found),
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items) { item ->
                            FilePickerRow(
                                item = item,
                                pickerType = pickerType,
                                onClick = { onItemClick(item) }
                            )
                            HorizontalDivider(color = colors.defaultDivider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilePickerRow(
    item: MediaLibraryItem,
    pickerType: PickerType,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val media = item as? MediaWrapper
    val isDirectory = media?.type == MediaWrapper.TYPE_DIR
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(filePickerIcon(isDirectory, pickerType)),
            contentDescription = null,
            tint = if (isDirectory) colors.primary else colors.fontLight,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.takeUnless { it.isNullOrBlank() } ?: media?.uri?.lastPathSegment.orEmpty(),
                color = colors.listTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val description = media?.location ?: item.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    color = colors.listSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun filePickerIcon(isDirectory: Boolean, pickerType: PickerType) = when {
    isDirectory -> R.drawable.ic_folder
    pickerType == PickerType.SUBTITLE -> R.drawable.ic_subtitles_file
    pickerType == PickerType.SOUNDFONT -> R.drawable.ic_menu_audio
    pickerType == PickerType.EQUALIZER -> R.drawable.ic_equalizer
    else -> R.drawable.ic_settings
}
