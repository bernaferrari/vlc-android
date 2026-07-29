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
import org.videolan.vlc.kmp.AndroidPlayerSurface
import org.videolan.vlc.kmp.AndroidPipController
import org.videolan.vlc.kmp.VlcKmpInitializer
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.platform.PipController

/**
 * Production-quality entry for the shared Compose shell
 * (Video / Audio / Browser / Playlists / More).
 * Launch from More → "VLC Shared" or deep link for QA.
 *
 * Uses the same [VlcKoinMainShell] as iOS [org.videolan.vlc.compose.app.MainViewController].
 */
class SharedAppActivity : BaseActivity() {

    override val displayTitle = true
    // NavigationSuiteScaffold is the single owner of edge-to-edge system insets, just like the
    // MainActivity shared-shell path. Letting BaseActivity consume them first creates a second
    // bottom safe area and makes navigation geometry differ between the two Android entry points.
    override fun ownsSystemBarInsetsInCompose(): Boolean = true
    private var root: ComposeView? = null

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = root ?: window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!VlcKmpInitializer.isInitialized) {
            VlcKmpInitializer.initialize(applicationContext)
        }
        // The shared HUD dispatches PiP through the common controller; it still needs the
        // foreground activity at the Android edge to perform the system transition.
        runCatching {
            (VlcKoin.get().get<PipController>() as? AndroidPipController)?.attachActivity(this)
        }
        val hostCallbacks = AndroidShellHostCallbacks(this)
        root = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VlcKoinMainShell(
                    title = getString(org.videolan.vlc.R.string.app_name),
                    hostCallbacks = hostCallbacks,
                    playerSurface = AndroidPlayerSurface,
                )
            }
        }
        setContentView(root)
    }
}
