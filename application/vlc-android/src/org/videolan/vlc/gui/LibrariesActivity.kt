package org.videolan.vlc.gui

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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
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
                setContent {
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

    /**
     * Load libraries from the JSON raw resource and adapt them for the Compose screen.
     */
    private suspend fun loadLibraries(): List<VLCLibraryLicense> = withContext(Dispatchers.IO) {
        val jsonData = AppContextProvider.appResources.openRawResource(R.raw.libraries).bufferedReader().use {
            it.readText()
        }

        val moshi = Moshi.Builder().build()
        val jsonAdapter: JsonAdapter<Licenses> = moshi.adapter(Licenses::class.java)
        val rawLibraries = jsonAdapter.fromJson(jsonData)!!

        rawLibraries.libraries.mapNotNull { library ->
            rawLibraries.licenses.firstOrNull { it.id == library.license }?.let { license ->
                VLCLibraryLicense(
                    title = library.title,
                    copyright = library.copyright,
                    licenseTitle = license.name,
                    licenseDescription = license.description,
                    licenseLink = license.link
                )
            }
        }
    }
}

data class Licenses(
        @Json(name = "libraries")
        val libraries: List<Library>,
        @Json(name = "licenses")
        val licenses: List<License>
)

data class License(
        @Json(name = "description")
        val description: String,
        @Json(name = "id")
        val id: String,
        @Json(name = "link")
        val link: String,
        @Json(name = "name")
        val name: String
)

data class Library(
        @Json(name = "copyright")
        val copyright: String,
        @Json(name = "license")
        val license: String,
        @Json(name = "title")
        val title: String
)
