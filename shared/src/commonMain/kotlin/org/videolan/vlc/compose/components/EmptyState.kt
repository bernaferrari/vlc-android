package org.videolan.vlc.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols

/** The semantic treatment of a shared loading, empty, or recoverable-error state. */
enum class VLCStatePlaceholderTone { Neutral, Error }

/**
 * Shared state surface used throughout the library and utility screens. A state is expressed as a
 * title, optional explanation, and actions instead of each feature inventing its own retry row.
 * The surface itself remains intentionally calm: media emptiness is normal, while errors gain a
 * restrained semantic cue rather than an alarming full-screen dialog.
 */
@Composable
fun VLCStatePlaceholder(
    title: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    message: String? = null,
    icon: Painter? = null,
    symbol: MaterialIcon = MaterialSymbols.Filled.VideoLibrary,
    loading: Boolean = false,
    tone: VLCStatePlaceholderTone = VLCStatePlaceholderTone.Neutral,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    VLCTheme {
        Box(
            modifier = modifier.padding(
                horizontal = if (compact) 16.dp else 24.dp,
                vertical = if (compact) 24.dp else 40.dp,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (loading) {
                    LoadingIndicator(text = title)
                    return@Column
                }
                PlaceholderIcon(icon = icon, symbol = symbol, tone = tone)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    color = if (tone == VLCStatePlaceholderTone.Error) MaterialTheme.colorScheme.error
                        else VLCThemeDefaults.colors.fontDefault,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                message?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = VLCThemeDefaults.colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                if (actionText != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = onActionClick) { Text(actionText) }
                }
                if (secondaryActionText != null && onSecondaryActionClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onSecondaryActionClick) { Text(secondaryActionText) }
                }
            }
        }
    }
}

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
    symbol: MaterialIcon = MaterialSymbols.Filled.VideoLibrary,
    compact: Boolean = false,
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    VLCStatePlaceholder(
        title = text,
        modifier = modifier,
        icon = icon,
        symbol = symbol,
        loading = loading,
        actionText = actionText,
        onActionClick = onActionClick.takeIf { actionText != null },
        compact = compact,
    )
}

@Composable
private fun LoadingIndicator(text: String) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    val pulse = if (motion.reducedMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "vlcLoadingPulse")
        val alpha by transition.animateFloat(
            initialValue = .64f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(motion.durationMedium),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "vlcLoadingPulseAlpha",
        )
        alpha
    }

    if (text.isBlank()) {
        CircularProgressIndicator(modifier = Modifier.alpha(pulse), color = colors.primary)
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.alpha(pulse), color = colors.primary)
        Spacer(modifier = Modifier.width(12.dp))
        LoadingText(text = text)
    }
}

@Composable
private fun LoadingText(text: String) {
    val motion = LocalVLCMotion.current
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(motion.reducedMotion) {
        if (motion.reducedMotion) return@LaunchedEffect
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
            text = if (motion.reducedMotion) "" else ".".repeat(dotCount),
            color = VLCThemeDefaults.colors.listSubtitle,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
private fun PlaceholderIcon(icon: Painter?, symbol: MaterialIcon, tone: VLCStatePlaceholderTone) {
    // Empty libraries need a quiet, destination-specific cue, not a floating gray badge that
    // reads like an error. The symbol keeps each tab recognisable without adding another surface
    // to an intentionally blank state.
    if (tone == VLCStatePlaceholderTone.Error) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(symbol, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    } else if (icon != null) {
        Image(
            painter = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(40.dp),
        )
    } else {
        Icon(
            icon = symbol,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
    }
}
