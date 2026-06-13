package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/activity_beta_welcome.xml
 *
 * This is intentionally a full dialog Activity surface rather than an interop leaf:
 * BetaWelcomeActivity can drop DataBinding and the XML layout entirely while keeping
 * its existing dialog theme, localized text, VLC icon slot, and finish-on-OK behavior.
 */
@Composable
fun VLCBetaWelcomeScreen(
    title: String,
    description: String,
    bugsDescription: String,
    crashWarning: String,
    okText: String,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit = { DefaultBetaWelcomeIcon() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier
                .width(300.dp)
                .fillMaxHeight(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        iconContent()
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = description,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = bugsDescription,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = crashWarning,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = okText)
                }
            }
        }
    }
}

@Composable
private fun DefaultBetaWelcomeIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(VLCThemeDefaults.colors.primary)
    )
}

@Preview(name = "Beta Welcome Light", showBackground = true)
@Composable
private fun VLCBetaWelcomeScreenPreview() {
    VLCBetaWelcomeScreen(
        title = "VLC Beta",
        description = "Welcome to the VLC Beta program.\nPlease note that this version may contain bugs.",
        bugsDescription = "If you encounter bugs, please report them so we can improve VLC.",
        crashWarning = "This version may crash or behave unexpectedly.",
        okText = "OK",
        onOk = {}
    )
}

@Preview(name = "Beta Welcome Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCBetaWelcomeScreenDarkPreview() {
    VLCBetaWelcomeScreen(
        title = "VLC Beta",
        description = "Welcome to the VLC Beta program.\nPlease note that this version may contain bugs.",
        bugsDescription = "If you encounter bugs, please report them so we can improve VLC.",
        crashWarning = "This version may crash or behave unexpectedly.",
        okText = "OK",
        onOk = {}
    )
}
