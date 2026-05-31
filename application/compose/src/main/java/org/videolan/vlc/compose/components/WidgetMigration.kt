package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_widget_migration.xml
 *
 * The app module owns the BottomSheetDialog host and supplies the widget preview
 * drawable. This content mirrors the informational prompt without requiring a
 * Fragment or DataBinding layout.
 */
@Composable
fun VLCWidgetMigrationDialogContent(
    title: String,
    startDescription: String,
    whatsNextTitle: String,
    endDescription: String,
    modifier: Modifier = Modifier,
    previewContent: @Composable () -> Unit = { WidgetPreviewPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefault)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 32.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Text(
                    text = startDescription,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    previewContent()
                }

                Text(
                    text = whatsNextTitle,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 32.dp, end = 16.dp)
                )

                Text(
                    text = endDescription,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun WidgetPreviewPlaceholder() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(VLCThemeDefaults.colors.primaryFocus)
    )
}

@Preview(name = "Widget Migration", showBackground = true)
@Composable
private fun VLCWidgetMigrationDialogContentPreview() {
    VLCWidgetMigrationDialogContent(
        title = "We updated our widgets!",
        startDescription = "Good news! We released improved widgets.\nThey offer more responsiveness and customization",
        whatsNextTitle = "What's next?",
        endDescription = "We detected that you already have widgets on your launcher. Unfortunately, they are not compatible with our new improved ones.\nDepending on your launcher, you may have to remove the old ones to replace them."
    )
}

@Preview(name = "Widget Migration Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCWidgetMigrationDialogContentDarkPreview() {
    VLCWidgetMigrationDialogContent(
        title = "We updated our widgets!",
        startDescription = "Good news! We released improved widgets.\nThey offer more responsiveness and customization",
        whatsNextTitle = "What's next?",
        endDescription = "We detected that you already have widgets on your launcher. Unfortunately, they are not compatible with our new improved ones.\nDepending on your launcher, you may have to remove the old ones to replace them."
    )
}
