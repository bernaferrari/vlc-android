package org.videolan.vlc.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBetaWelcomeScreen

class BetaWelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCBetaWelcomeScreen(
                        title = getString(R.string.welcome_beta_title),
                        description = getString(R.string.welcome_beta_description),
                        bugsDescription = getString(R.string.welcome_beta_description_bugs),
                        crashWarning = getString(R.string.welcome_beta_description_crashes),
                        okText = getString(R.string.ok),
                        onOk = ::finish,
                        iconContent = {
                            Image(
                                painter = painterResource(R.drawable.icon),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }
            }
        )
    }
}
