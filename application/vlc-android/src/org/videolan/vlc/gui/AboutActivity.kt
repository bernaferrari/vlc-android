package org.videolan.vlc.gui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.Image
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.tools.resIdByName
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAboutDetailRow
import org.videolan.vlc.compose.components.VLCAboutScreen
import org.videolan.vlc.compose.components.VLCAboutVersionInfo
import org.videolan.vlc.util.openLinkIfPossible
import java.security.MessageDigest

/**
 * Full Compose About screen. Replaces the former AboutFragment/about.xml path.
 */
open class AboutActivity : BaseActivity() {

    private var licenseText by mutableStateOf("")

    override fun getSnackAnchorView(overAudioPlayer: Boolean) = window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val versionInfo = buildVersionInfo()

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCAboutScreen(
                        title = getString(R.string.about),
                        appName = getString(R.string.app_name_full),
                        description = getString(R.string.about_vlc_text),
                        versionInfo = versionInfo,
                        copyright = getString(R.string.about_copyright),
                        licenseTitle = getString(R.string.about_license),
                        licenseText = licenseText,
                        websiteTitle = getString(R.string.official_website),
                        feedbackTitle = getString(R.string.send_feedback),
                        sourcesTitle = getString(R.string.sources),
                        librariesTitle = getString(R.string.libraries),
                        authorsTitle = getString(R.string.authors),
                        closeContentDescription = getString(R.string.close),
                        openLinkContentDescription = getString(R.string.talkback_open_in_browser),
                        onClose = ::finish,
                        onOpenWebsite = { openLinkIfPossible("https://www.videolan.org/vlc/") },
                        onSendFeedback = { startActivity(Intent(this@AboutActivity, FeedbackActivity::class.java)) },
                        onOpenSources = { openLinkIfPossible("https://code.videolan.org/videolan/vlc-android") },
                        onOpenLibraries = { startActivity(Intent(this@AboutActivity, LibrariesActivity::class.java)) },
                        onOpenAuthors = { startActivity(Intent(this@AboutActivity, AuthorsActivity::class.java)) },
                        onOpenLicenseLink = { openLinkIfPossible("https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt") },
                        closeIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_up),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        logoContent = {
                            Image(
                                painter = painterResource(R.drawable.ic_about_logo),
                                contentDescription = null,
                                modifier = Modifier.size(128.dp)
                            )
                        },
                        websiteIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_website),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        feedbackIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_feedback),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        sourcesIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sourcecode),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        librariesIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sources),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        authorsIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_authors),
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
            licenseText = loadGplLicense()
        }
    }

    private fun buildVersionInfo() = VLCAboutVersionInfo(
        version = BuildConfig.VLC_VERSION_NAME,
        buildDate = getString(R.string.build_time),
        changelog = getString(R.string.changelog).replace("*", "\u2022"),
        detailRows = listOf(
            VLCAboutDetailRow(getString(R.string.revision), getString(R.string.build_revision)),
            VLCAboutDetailRow(getString(R.string.is_compiled_by), getString(R.string.build_host)),
            VLCAboutDetailRow(getString(R.string.is_signed_by), getSignerName()),
            VLCAboutDetailRow("libvlcjni", getString(R.string.build_libvlc_revision)),
            VLCAboutDetailRow("VLC", getString(R.string.build_vlc_revision)),
            VLCAboutDetailRow("libvlc", BuildConfig.LIBVLC_VERSION),
            VLCAboutDetailRow(getString(R.string.remote_access_version_title), optionalString("remote_access_version")),
            VLCAboutDetailRow(getString(R.string.remote_access_hash_title), optionalString("build_remote_access_revision"))
        )
    )

    private suspend fun loadGplLicense(): String = withContext(Dispatchers.IO) {
        AppContextProvider.appResources.openRawResource(R.raw.vlc_license).bufferedReader().use {
            it.readText()
        }
    }

    private fun optionalString(name: String) = try {
        getString(resIdByName(name, "string"))
    } catch (e: Resources.NotFoundException) {
        getString(R.string.unknown)
    }

    @Suppress("DEPRECATION")
    private fun getSignerName(): String {
        val signatures =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            } else {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo
                    ?.apkContentsSigners
            }

        signatures?.forEach { signature ->
            val fingerprint = try {
                MessageDigest.getInstance("SHA1")
                    .digest(signature.toByteArray())
                    .joinToString(":") { "%02x".format(it.toInt() and 0xff).uppercase() }
            } catch (e: Exception) {
                null
            }

            when (fingerprint) {
                "AC:5A:BC:F1:99:AC:86:61:6A:79:65:CB:84:59:94:89:A5:A7:3F:86" -> return "VideoLAN nightly"
                "4D:D5:44:A7:51:D3:D5:4C:17:D8:7E:1D:D3:60:F0:C6:40:A5:C1:50" -> return "Google"
                "EE:FB:C9:81:42:83:43:BB:DD:FF:F6:B2:3B:6B:D8:71:73:51:41:0C" -> return "VideoLAN"
                "A6:07:A2:5D:03:B8:90:5B:2D:16:E6:27:D9:15:74:35:02:E7:D0:CB" -> return "Amazon"
                "40:80:86:F9:AE:A6:52:A8:61:44:70:4F:11:79:9A:CA:BA:31:C7:A0" -> return "F-Droid"
            }
        }
        return getString(R.string.unknown)
    }
}
