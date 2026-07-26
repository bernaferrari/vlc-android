/*
 * SharedAppActivity — hosts the multiplatform VlcMainShell on Android.
 */
package org.videolan.vlc.gui

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.videolan.vlc.compose.app.VlcKoinMainShell
import org.videolan.vlc.kmp.AndroidShellHostCallbacks
import org.videolan.vlc.kmp.VlcKmpInitializer

/**
 * Production-quality entry for the shared Compose shell
 * (Video / Audio / Browser / Playlists / More).
 * Launch from More → "VLC Shared" or deep link for QA.
 *
 * Uses the same [VlcKoinMainShell] as iOS [org.videolan.vlc.compose.app.MainViewController].
 */
class SharedAppActivity : BaseActivity() {

    override val displayTitle = true
    private var root: ComposeView? = null

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = root ?: window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!VlcKmpInitializer.isInitialized) {
            VlcKmpInitializer.initialize(applicationContext)
        }
        val hostCallbacks = AndroidShellHostCallbacks(this)
        root = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VlcKoinMainShell(
                    title = getString(org.videolan.vlc.R.string.app_name),
                    hostCallbacks = hostCallbacks,
                )
            }
        }
        setContentView(root)
    }
}
