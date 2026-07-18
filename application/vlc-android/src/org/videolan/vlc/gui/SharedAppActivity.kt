/*
 * SharedAppActivity — hosts the multiplatform VlcSharedApp shell on Android.
 */
package org.videolan.vlc.gui

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.videolan.vlc.compose.app.VlcMainShell
import org.videolan.vlc.kmp.VlcKmpInitializer

/**
 * Production-quality entry for the shared Compose shell (library / player / settings).
 * Launch from More → "VLC Shared" or deep link for QA.
 *
 * Uses the same [VlcSharedApp] as iOS [MainViewController].
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
        root = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VlcMainShell(title = getString(org.videolan.vlc.R.string.app_name))
            }
        }
        setContentView(root)
    }
}
