@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package org.videolan.vlc.compose.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.videolan.tools.VlcSettings
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import org.videolan.vlc.compose.theme.resolveVLCThemePreference

/**
 * Spatial language for the 10-foot UI. Values are density-independent and sized for Android TV's
 * canonical 960x540dp canvas: content stays inside a generous overscan-safe frame, shelves have
 * room to breathe and every focusable element is large enough to read from the sofa.
 */
object VLCTvLayout {
    val OverscanHorizontal = 48.dp
    val OverscanVertical = 32.dp
    val ShelfGap = 32.dp
    val ShelfHeaderGap = 14.dp
    val CardGap = 20.dp
    val CardCorner = 16.dp
    val CardTitleGap = 10.dp
    val ControlHeight = 48.dp
    val IconButtonSize = 48.dp
    val ReadingWidth = 680.dp
    val FocusOutline = 3.dp
    const val FocusScale = 1.06f
    const val ButtonFocusScale = 1.05f
}

/**
 * Card proportions used on TV. Widths are chosen so a shelf always shows a partial next card,
 * which tells people at a glance that the row continues to the right.
 */
enum class VLCTvCardShape(val width: Dp, val aspectRatio: Float) {
    /** Videos, episodes and backdrops. */
    Landscape(208.dp, 16f / 9f),
    /** Movie and show posters, cast portraits. */
    Poster(128.dp, 2f / 3f),
    /** Albums, artists, playlists and folders. */
    Square(160.dp, 1f),
}

/**
 * TV appearance policy: a calm dark canvas unless the person explicitly chose Light. The accent
 * always follows the shared VLC preference, so phone and TV share one identity.
 */
fun resolveVLCTvDarkTheme(appearance: VLCThemeAppearance): Boolean = appearance != VLCThemeAppearance.Light

/** Theme boundary for every TV activity. Pass [darkTheme] only for previews and snapshots. */
@Composable
fun VLCTvTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val appearance by VlcSettings.themeAppearance.collectAsState()
    val accent by VlcSettings.themeAccent.collectAsState()
    val preference = resolveVLCThemePreference(appearance, accent)
    VLCTheme(
        darkTheme = darkTheme ?: resolveVLCTvDarkTheme(preference.appearance),
        accent = preference.accent,
        content = content,
    )
}

/**
 * Preview/snapshot hook: when a surface's [VLCTvSurface] `previewKey` matches this value it renders
 * in its focused state. Production code never provides it, so real focus always wins on devices.
 */
val LocalVLCTvPreviewFocus = compositionLocalOf<Any?> { null }

/**
 * The one focus treatment for TV: a spring-driven lift (scale), an accent outline and a soft
 * accent glow. Ripples are meaningless with a remote, so no indication is drawn. Reduced motion
 * keeps the outline and glow but drops the scale animation.
 */
@Composable
fun VLCTvSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(VLCTvLayout.CardCorner),
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    focusedScale: Float = VLCTvLayout.FocusScale,
    containerColor: Color = Color.Transparent,
    focusedContainerColor: Color = containerColor,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    focusedContentColor: Color = contentColor,
    outline: Boolean = true,
    glow: Boolean = true,
    contentDescription: String? = null,
    previewKey: Any? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val realFocus by interactionSource.collectIsFocusedAsState()
    val previewFocus = previewKey != null && LocalVLCTvPreviewFocus.current == previewKey
    val focused = realFocus || previewFocus
    val motion = LocalVLCMotion.current
    val scale by animateFloatAsState(
        targetValue = if (focused && enabled) focusedScale else 1f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "tv-focus-scale",
    )
    val container by animateColorAsState(
        targetValue = if (focused) focusedContainerColor else containerColor,
        animationSpec = tween(if (motion.reducedMotion) 0 else 140),
        label = "tv-focus-container",
    )
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (focused && glow) {
                    Modifier.shadow(18.dp, shape, clip = false, ambientColor = accent.copy(alpha = 0.55f), spotColor = accent.copy(alpha = 0.55f))
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(container)
            .then(if (focused && outline) Modifier.border(VLCTvLayout.FocusOutline, accent, shape) else Modifier)
            .then(if (onFocusChanged != null) Modifier.onFocusChanged { onFocusChanged(it.isFocused) } else Modifier)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        propagateMinConstraints = false,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (focused) focusedContentColor else contentColor) {
            content(focused)
        }
    }
}

/**
 * One-shot initial focus that is safe inside lazy layouts. The request is issued from the target's
 * own composition (after its focus node is attached) rather than from the screen, where a lazy item
 * may not exist yet. Once it succeeds it never steals focus again, e.g. when the data refreshes.
 */
@Stable
class VLCTvInitialFocus {
    internal val requester = FocusRequester()
    internal var consumed = false
}

@Composable
fun rememberVLCTvInitialFocus(): VLCTvInitialFocus = remember { VLCTvInitialFocus() }

/** Attaches [state] to this element and focuses it the first time it composes. */
@Composable
fun Modifier.vlcTvInitialFocus(state: VLCTvInitialFocus?): Modifier {
    if (state == null) return this
    LaunchedEffect(state) {
        if (!state.consumed) {
            // Let parents finish their first layout so bring-into-view targets the right offset.
            withFrameNanos { }
            state.consumed = runCatching { state.requester.requestFocus(FocusDirection.Enter) }.getOrDefault(false)
        }
    }
    return focusRequester(state.requester)
}

/** Visual weight of a [VLCTvButton]. */
enum class VLCTvButtonStyle { Primary, Tonal, Quiet }

/**
 * Pill button for TV action rows. Unfocused buttons stay tonal so a row of actions reads as one
 * group; the focused button turns solid accent, which is the strongest cue on a dark canvas.
 */
@Composable
fun VLCTvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: MaterialIcon? = null,
    painter: Painter? = null,
    style: VLCTvButtonStyle = VLCTvButtonStyle.Tonal,
    enabled: Boolean = true,
    previewKey: Any? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val (rest, restContent) = when (style) {
        VLCTvButtonStyle.Primary -> scheme.primary.copy(alpha = 0.22f) to scheme.onSurface
        VLCTvButtonStyle.Tonal -> scheme.onSurface.copy(alpha = 0.08f) to scheme.onSurface
        VLCTvButtonStyle.Quiet -> Color.Transparent to scheme.onSurfaceVariant
    }
    VLCTvSurface(
        onClick = onClick,
        modifier = modifier.heightIn(min = VLCTvLayout.ControlHeight).alpha(if (enabled) 1f else 0.38f),
        shape = CircleShape,
        enabled = enabled,
        focusedScale = VLCTvLayout.ButtonFocusScale,
        containerColor = rest,
        focusedContainerColor = scheme.primary,
        contentColor = restContent,
        focusedContentColor = scheme.onPrimary,
        outline = false,
        previewKey = previewKey,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PaddingValues(start = if (icon != null || painter != null) 20.dp else 26.dp, end = 26.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                painter != null -> androidx.compose.material3.Icon(painter, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Circular icon control (transport buttons, header actions, row actions). [active] tints the
 * glyph with the accent for toggles such as shuffle and repeat.
 */
@Composable
fun VLCTvIconButton(
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: MaterialIcon? = null,
    painter: Painter? = null,
    size: Dp = VLCTvLayout.IconButtonSize,
    iconSize: Dp = 24.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    filled: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    previewKey: Any? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val glyph = if (active) scheme.primary else scheme.onSurface
    VLCTvSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(size).alpha(if (enabled) 1f else 0.38f),
        shape = CircleShape,
        enabled = enabled,
        focusedScale = 1.1f,
        containerColor = if (filled) scheme.onSurface.copy(alpha = 0.10f) else Color.Transparent,
        focusedContainerColor = scheme.primary,
        contentColor = glyph,
        focusedContentColor = scheme.onPrimary,
        outline = false,
        contentDescription = contentDescription,
        previewKey = previewKey,
        onFocusChanged = onFocusChanged,
    ) {
        val iconModifier = Modifier.align(Alignment.Center).size(iconSize)
        when {
            icon != null -> Icon(icon, contentDescription = null, modifier = iconModifier)
            painter != null -> androidx.compose.material3.Icon(painter, contentDescription = null, modifier = iconModifier)
        }
    }
}
