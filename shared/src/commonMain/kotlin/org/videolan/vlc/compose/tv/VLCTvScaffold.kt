@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package org.videolan.vlc.compose.tv

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.LocalVLCMotion

/**
 * Root canvas for every TV screen: the theme background with a faint accent wash from the top
 * left, which gives depth on large panels without competing with artwork. [backdrop] can paint
 * blurred artwork underneath; it is automatically scrimmed so text stays legible.
 */
@Composable
fun VLCTvScreen(
    modifier: Modifier = Modifier,
    backdrop: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        if (backdrop != null) {
            backdrop()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to scheme.background.copy(alpha = 0.96f),
                            0.55f to scheme.background.copy(alpha = 0.78f),
                            1f to scheme.background.copy(alpha = 0.40f),
                        ),
                    ),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to scheme.background.copy(alpha = 0.55f),
                            1f to scheme.background,
                        ),
                    ),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(scheme.primary.copy(alpha = 0.08f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset.Zero,
                            radius = 1400f,
                        ),
                    ),
            )
        }
        content()
    }
}

/**
 * Screen header: an optional back control, an eyebrow (category), a large title, an optional
 * subtitle (path, count...) and trailing actions. Sits inside the overscan frame.
 */
@Composable
fun VLCTvTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            VLCTvIconButton(
                contentDescription = backContentDescription,
                onClick = onBack,
                icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                filled = true,
            )
            Spacer(Modifier.width(18.dp))
        }
        Column(Modifier.weight(1f)) {
            eyebrow?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    maxLines = 1,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Shelf title with an optional quiet count, aligned to the overscan edge. */
@Composable
fun VLCTvShelfHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

/**
 * A horizontally scrolling shelf. Content bleeds to the screen edge while the first card aligns
 * with the overscan margin, and focus returns to the last focused card when the shelf is re-entered.
 */
@Composable
fun VLCTvShelf(
    title: String?,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            VLCTvShelfHeader(title, Modifier.padding(horizontal = VLCTvLayout.OverscanHorizontal), trailing)
            Spacer(Modifier.height(VLCTvLayout.ShelfHeaderGap))
        }
        LazyRow(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxWidth().focusRestorer(),
            // Vertical padding leaves room for the focus lift and glow, which would otherwise clip.
            contentPadding = PaddingValues(horizontal = VLCTvLayout.OverscanHorizontal, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(VLCTvLayout.CardGap),
            content = content,
        )
    }
}

/** Convenience shelf of [VLCTvMediaCard]s. */
@Composable
fun VLCTvItemShelf(
    title: String?,
    items: List<VLCTvItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onItemFocused: ((Int) -> Unit)? = null,
    onItemLongClick: ((Int) -> Unit)? = null,
    initialFocus: VLCTvInitialFocus? = null,
    /** Render compact [VLCTvShortcutTile]s instead of media cards. */
    shortcuts: Boolean = false,
    artwork: VLCTvArtwork = { VLCTvSymbolArtwork(it.symbol) },
) {
    VLCTvShelf(title = title, modifier = modifier, trailing = trailing) {
        itemsIndexed(items, key = { index, item -> "$index-${item.key}" }) { index, item ->
            if (shortcuts) {
                VLCTvShortcutTile(
                    item = item,
                    onClick = { onItemClick(index) },
                    onFocused = onItemFocused?.let { { it(index) } },
                    modifier = if (index == 0) Modifier.vlcTvInitialFocus(initialFocus) else Modifier,
                )
                return@itemsIndexed
            }
            VLCTvMediaCard(
                item = item,
                onClick = { onItemClick(index) },
                onLongClick = onItemLongClick?.let { { it(index) } },
                onFocused = onItemFocused?.let { { it(index) } },
                modifier = if (index == 0) Modifier.vlcTvInitialFocus(initialFocus) else Modifier,
                artwork = artwork,
            )
        }
    }
}

/** Centered loading indicator with an optional caption. */
@Composable
fun VLCTvLoading(modifier: Modifier = Modifier, text: String? = null) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp),
        )
        text?.let {
            Spacer(Modifier.height(18.dp))
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Small spinner shown in a header while a background scan or refresh runs. */
@Composable
fun VLCTvActivityIndicator(modifier: Modifier = Modifier) {
    val motion = LocalVLCMotion.current
    val alpha = if (motion.reducedMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "tv-activity")
        val value by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "tv-activity-alpha",
        )
        value
    }
    CircularProgressIndicator(
        modifier = modifier.size(28.dp).alpha(alpha),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        strokeWidth = 3.dp,
    )
}

/**
 * Empty or error state sized for the 10-foot UI: a tinted symbol, a headline and an optional
 * explanation, centered in the available space.
 */
@Composable
fun VLCTvEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    symbol: MaterialIcon = MaterialSymbols.Filled.VideoLibrary,
    action: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize().padding(VLCTvLayout.OverscanHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .background(scheme.primary.copy(alpha = 0.14f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = VLCTvLayout.ReadingWidth),
        )
        message?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = VLCTvLayout.ReadingWidth),
            )
        }
        if (action != null) {
            Spacer(Modifier.height(28.dp))
            action()
        }
    }
}
