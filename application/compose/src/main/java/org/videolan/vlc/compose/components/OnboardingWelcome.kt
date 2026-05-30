package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the static presentational parts of:
 *   application/vlc-android/res/layout/onboarding_welcome.xml
 *   (also layout-land variant has similar structure)
 *
 * Renders:
 *   - Centered branding image / logo area (via slot)
 *   - "Welcome" title (VLC.Onboarding.Title style)
 *   - Subtitle / tagline (VLC.Onboarding.Text style)
 *
 * The full onboarding flow uses Theme.VLC.Onboarding.* (which forces dark
 * onboarding_grey background). This leaf uses the dedicated
 * VLCThemeDefaults.colors.onboardingBackground when wrapped.
 *
 * The theme selection step (onboarding_theme.xml) and later scanning/permission
 * steps are more interactive and left for subsequent waves.
 *
 * @param title The welcome title string (e.g. "Welcome to VLC")
 * @param subtitle The explanatory subtitle
 * @param logoContent Composable slot for the app icon / logo (typically 120dp-ish)
 */
@Composable
fun VLCOnboardingWelcome(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    logoContent: @Composable () -> Unit = { DefaultOnboardingLogo() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp)) // approx guide at 25%

            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                logoContent()
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = subtitle,
                color = colors.fontLight,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun DefaultOnboardingLogo() {
    // Simple placeholder representing the VLC icon in onboarding.
    // Real usage passes the actual painter/vector from app resources.
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "▶",
            style = MaterialTheme.typography.displayMedium,
            color = VLCThemeDefaults.colors.primary
        )
    }
}
