package org.videolan.vlc.gui

import org.videolan.vlc.compose.app.AboutBundle

import org.videolan.vlc.gui.helpers.setVlcContent
import android.os.Bundle
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCLibrariesScreen
import org.videolan.vlc.compose.components.VLCLibraryLicense
import org.videolan.vlc.util.openLinkIfPossible

/**
 * Activity showing the different libraries used by VLC for Android and their licenses.
 */
class LibrariesActivity : BaseActivity() {

    private var libraries by mutableStateOf(emptyList<VLCLibraryLicense>())

    override fun getSnackAnchorView(overAudioPlayer: Boolean) = window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setVlcContent {
                    VLCLibrariesScreen(
                        title = getString(R.string.libraries),
                        libraries = libraries,
                        closeContentDescription = getString(R.string.close),
                        openLinkContentDescription = getString(R.string.talkback_open_in_browser),
                        onClose = ::finish,
                        onOpenLicenseLink = { openLinkIfPossible(it) },
                        closeIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_up),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        sourceIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sources),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        linkIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_website),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        )
        if (AndroidDevices.isTv) applyOverscanMargin(this)

        lifecycleScope.launch {
            libraries = loadLibraries()
        }
    }

    private suspend fun loadLibraries(): List<VLCLibraryLicense> = AboutBundle.libraries()
}
