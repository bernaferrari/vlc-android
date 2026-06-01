package org.videolan.vlc.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Shared Compose empty/loading surface.
 *
 * Traceability:
 * - Replaces the duplicated private empty-state composables in the phone audio,
 *   video, browser, and playlist screens.
 * - Replaces the presentational role of the now-unused legacy XML
 *   empty/loading view.
 *
 * App modules pass drawable painters and localized strings so this leaf stays
 * reusable inside the resource-free :application:compose module.
 */
@Composable
fun VLCEmptyState(
    loading: Boolean,
    text: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    icon: Painter? = null,
    compact: Boolean = false,
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    VLCTheme {
        Column(
            modifier = modifier
                .padding(if (compact) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                loading -> LoadingIndicator(text = text)
                else -> {
                    EmptyIcon(icon = icon)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = text,
                        color = VLCThemeDefaults.colors.listSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    if (actionText != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onActionClick) {
                            Text(actionText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator(text: String) {
    val colors = VLCThemeDefaults.colors

    if (text.isBlank()) {
        CircularProgressIndicator(color = colors.primary)
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = colors.primary)
        Spacer(modifier = Modifier.width(12.dp))
        LoadingText(text = text)
    }
}

@Composable
private fun LoadingText(text: String) {
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            dotCount = (dotCount + 1) % 4
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = VLCThemeDefaults.colors.listSubtitle,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = ".".repeat(dotCount),
            color = VLCThemeDefaults.colors.listSubtitle,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
private fun EmptyIcon(icon: Painter?) {
    if (icon != null) {
        Image(
            painter = icon,
            contentDescription = null
        )
    } else {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(96.dp)
                .background(VLCThemeDefaults.colors.emptyForeground, CircleShape)
        )
    }
}

@Preview(
    name = "VLCEmptyState - Loading Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 260
)
@Composable
private fun VLCEmptyStateLoadingLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCEmptyState(loading = true, text = "Loading")
    }
}

@Preview(
    name = "VLCEmptyState - Empty Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 260
)
@Composable
private fun VLCEmptyStateEmptyDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCEmptyState(
            loading = false,
            text = "No media found",
            actionText = "Browse"
        )
    }
}
