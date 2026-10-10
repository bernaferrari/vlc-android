package org.videolan.vlc.compose.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.icons.VlcCone
import org.videolan.vlc.compose.theme.LocalVLCMotion

/**
 * Full-screen TV confirmation (delete playlist, open network settings...). Focus starts on the
 * positive action, both actions are large pills and Back cancels at the activity level.
 */
@Composable
fun VLCTvConfirmationScreen(
    title: String,
    message: String,
    positiveLabel: String,
    negativeLabel: String,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    modifier: Modifier = Modifier,
    symbol: MaterialIcon = MaterialSymbols.Filled.Info,
    destructive: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val positiveFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { positiveFocus.requestFocus() } }
    VLCTvScreen(modifier) {
        Box(Modifier.fillMaxSize().padding(horizontal = VLCTvLayout.OverscanHorizontal, vertical = VLCTvLayout.OverscanVertical), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(scheme.surfaceContainer)
                    .padding(horizontal = 44.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val tint = if (destructive) scheme.error else scheme.primary
                Box(Modifier.size(64.dp).background(tint.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(symbol, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, textAlign = TextAlign.Center)
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(message, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    VLCTvButton(
                        text = positiveLabel,
                        icon = MaterialSymbols.Filled.CheckCircle,
                        style = VLCTvButtonStyle.Primary,
                        onClick = onPositive,
                        modifier = Modifier.focusRequester(positiveFocus),
                        previewKey = "positive",
                    )
                    VLCTvButton(text = negativeLabel, icon = MaterialSymbols.Filled.Close, onClick = onNegative, previewKey = "negative")
                }
            }
        }
    }
}

/** Copy for one onboarding page; the host supplies localized strings. */
data class VLCTvOnboardingPage(val title: String, val message: String, val symbol: MaterialIcon? = null)

/**
 * First-run flow for TV: the VLC cone (or the page symbol), a headline, a short explanation,
 * a progress indicator and Previous/Next actions. Focus always lands on the forward action.
 */
@Composable
fun VLCTvOnboardingScreen(
    pages: List<VLCTvOnboardingPage>,
    currentPage: Int,
    previousLabel: String,
    nextLabel: String,
    finishLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val page = pages.getOrNull(currentPage) ?: return
    val last = currentPage == pages.lastIndex
    val forwardFocus = remember { FocusRequester() }
    LaunchedEffect(currentPage) { runCatching { forwardFocus.requestFocus() } }
    VLCTvScreen(modifier) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = VLCTvLayout.OverscanHorizontal * 2, vertical = VLCTvLayout.OverscanVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(56.dp),
        ) {
            Box(
                Modifier.size(220.dp).background(scheme.primary.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val symbol = page.symbol
                if (symbol == null) {
                    Image(VlcCone, contentDescription = null, modifier = Modifier.size(150.dp))
                } else {
                    Icon(symbol, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(96.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                VLCTvPageIndicator(count = pages.size, current = currentPage)
                Spacer(Modifier.height(20.dp))
                Text(page.title, style = MaterialTheme.typography.displaySmall, color = scheme.onSurface)
                Spacer(Modifier.height(14.dp))
                Text(page.message, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, color = scheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 520.dp))
                Spacer(Modifier.height(36.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    VLCTvButton(
                        text = if (last) finishLabel else nextLabel,
                        icon = if (last) MaterialSymbols.Filled.CheckCircle else MaterialSymbols.AutoMirrored.Filled.ChevronForward,
                        style = VLCTvButtonStyle.Primary,
                        onClick = if (last) onFinish else onNext,
                        modifier = Modifier.focusRequester(forwardFocus),
                        previewKey = "forward",
                    )
                    if (currentPage > 0) {
                        VLCTvButton(text = previousLabel, icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack, onClick = onPrevious, style = VLCTvButtonStyle.Quiet)
                    }
                }
            }
        }
    }
}

@Composable
private fun VLCTvPageIndicator(count: Int, current: Int) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalVLCMotion.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 32.dp else 10.dp, tween(if (motion.reducedMotion) 0 else 220), label = "tv-page-width")
            val color by animateColorAsState(
                if (index <= current) scheme.primary else scheme.onSurface.copy(alpha = 0.24f),
                tween(if (motion.reducedMotion) 0 else 220),
                label = "tv-page-color",
            )
            Box(Modifier.width(width).height(10.dp).clip(CircleShape).background(color))
        }
    }
}

