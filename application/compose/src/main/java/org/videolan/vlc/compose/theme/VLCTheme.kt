package org.videolan.vlc.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * VLCTheme.kt - Expanded theme tokens for VLC Compose migration (Phase 0).
 *
 * Tokens extracted (read-only) from the real app:
 * - Palette: /vlc-android/application/resources/src/main/res/values/colors.xml
 * - Custom attrs: /vlc-android/application/resources/src/main/res/values/attrs.xml (60+ ?attr/...)
 * - Light/Dark mappings: /vlc-android/application/vlc-android/res/values/styles.xml
 *   (Theme.VLC.Apearance + Theme.VLC.Apearance.Black + variants in values-night, v21, v23 etc.)
 * - Prioritization: freq of ?attr/ in layout resources (font_default 81x, background_default 26x, etc.)
 *   + key files: audio_player.xml (player chrome/HUD), browser_item.xml (media lists),
 *     about.xml, audio_browser.xml, video_*_card.xml, empty states, dialogs,
 *     section header decorations, info_item.xml, dialog_*.xml, onboarding_*.xml.
 *
 * This enables future leaf migrations (audio browser, player, about, onboarding, lists)
 * to use correct colors without hard-coded values.
 *
 * Usage in @Composable:
 *   val colors = VLCThemeDefaults.colors   // or LocalVLCColors.current
 *   Box(Modifier.background(colors.backgroundDefault)) { ... }
 */

// ============================================================
// RAW PALETTE (from colors.xml)
// ============================================================
// Material oranges (primary brand)
private val Orange50 = Color(0xFFFFF3E0)
private val Orange100 = Color(0xFFFFDFAE)
private val Orange200 = Color(0xFFFFCA7D)
private val Orange300 = Color(0xFFFFB54C)
private val Orange400 = Color(0xFFFFA11A)
private val Orange500 = Color(0xFFFF8800)   // main accent in many dark contexts
private val Orange600 = Color(0xFFFF7D00)
private val Orange700 = Color(0xFFFF7200)
private val Orange800 = Color(0xFFFF610A)   // colorPrimary in light theme
private val Orange900 = Color(0xFFFF5014)

// Greys (extensive scale used for text, cards, dividers, dark mode)
private val Grey50 = Color(0xFFFAFAFA)
private val Grey100 = Color(0xFFF5F5F5)
private val Grey200 = Color(0xFFEEEEEE)
private val Grey300 = Color(0xFFE0E0E0)
private val Grey400 = Color(0xFFBDBDBD)
private val Grey500 = Color(0xFF9E9E9E)
private val Grey600 = Color(0xFF757575)
private val Grey700 = Color(0xFF616161)
private val Grey800 = Color(0xFF424242)
private val Grey850 = Color(0xFF323232)
private val Grey875 = Color(0xFF2A2A2A)
private val Grey900 = Color(0xFF212121)

// Core + special backgrounds
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val DarkBackground = Color(0xFF131313)          // background_default in dark
private val MiniPlayerDark = Color(0xFF121212)          // audio_header_background dark
private val PlayerBackground = Color(0xC0141414)

// Transparent variants heavily used for selections, tints, chips, HUD (from colors.xml)
private val BlackTransparent10 = Color(0x1A000000)
private val BlackTransparent20 = Color(0x33000000)
private val BlackTransparent50 = Color(0x80000000)
private val BlackTransparent60 = Color(0x99000000)
private val BlackTransparent75 = Color(0xBF000000)
private val BlackTransparent80 = Color(0xCC000000)
private val BlackTransparent90 = Color(0xE6000000)

private val WhiteTransparent10 = Color(0x1AFFFFFF)
private val WhiteTransparent20 = Color(0x33FFFFFF)
private val WhiteTransparent50 = Color(0x80FFFFFF)
private val WhiteTransparent60 = Color(0x99FFFFFF)
private val WhiteTransparent75 = Color(0xBFFFFFFF)
private val WhiteTransparent80 = Color(0xCCFFFFFF)
private val WhiteTransparent90 = Color(0xE6FFFFFF)
private val WhiteTransparent95 = Color(0xF2FFFFFF)

private val Orange800Transparent20 = Color(0x33FF610A)
private val Orange500Transparent20 = Color(0x33FF8800)

// Onboarding / special
private val OnboardingGrey = Color(0xFF011422)

// ============================================================
// VLCColorScheme - light + dark
// Semantic tokens mapped from ?attr/* + style items (traceability in comments)
// ============================================================

data class VLCColorScheme(
    // --- Material 3 core tokens (derived from VLC palette for compatibility) ---
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val error: Color,
    val onError: Color,
    val primaryContainer: Color = Color.Unspecified,
    val onPrimaryContainer: Color = Color.Unspecified,

    // --- Core VLC semantic tokens (highest priority from reconnaissance) ---
    /** ?attr/background_default (styles.xml:41 light, 235 dark) */
    val backgroundDefault: Color,
    /** ?attr/background_default_darker (used in cards, about, tips) */
    val backgroundDefaultDarker: Color,
    /** ?attr/list_title (browser_item.xml:176, many lists + selectors in res/color/) */
    val listTitle: Color,
    /** ?attr/list_subtitle (browser_item.xml:194, audio lists) */
    val listSubtitle: Color,
    /** ?attr/font_default (layouts: 81 occurrences - primary text) */
    val fontDefault: Color,
    /** ?attr/font_light (layouts: 21 occurrences - secondary/description text) */
    val fontLight: Color,
    /** ?attr/font_audio_light */
    val fontAudioLight: Color,
    /** ?attr/font_disabled */
    val fontDisabled: Color,
    /** ?attr/default_divider (audio headers, lists) */
    val defaultDivider: Color,
    /** ?attr/player_icon_color (audio_player.xml:524,578 + HUD chrome) */
    val playerIconColor: Color,
    /** ?attr/audio_menu_icon (audio_player.xml collapsed header actions) */
    val audioMenuIcon: Color,
    /** ?attr/about_text_primary (about.xml:35,87 + dark override to orange) */
    val aboutTextPrimary: Color,
    /** ?attr/card_background */
    val cardBackground: Color,
    /** ?attr/card_border */
    val cardBorder: Color,
    /** ?attr/audio_header_background (audio_player.xml:205) */
    val audioHeaderBackground: Color,
    /** ?attr/audio_header_divider */
    val audioHeaderDivider: Color,
    /** ?attr/audio_player_gradient_top/bottom start color */
    val audioPlayerGradientColor: Color,
    /** Track/background color for ?attr/audio_seek_bar */
    val audioSeekTrack: Color,
    /** Approx for ?attr/bottom_navigation_background (drawable in real, color here) */
    val bottomNavigationBackground: Color,
    /** ?attr/audio_chips_color + text (audio_player.xml:396 etc) */
    val audioChipsColor: Color,
    val audioChipsTextColor: Color,
    /** ?attr/audio_chip_background + text (audio_player.xml audio queue progress pill) */
    val audioChipBackground: Color,
    val audioChipTextColor: Color,
    /** ?attr/subtle_selection */
    val subtleSelection: Color,
    /** ?attr/primary_focus (focus/ripple highlights) */
    val primaryFocus: Color,
    /** ?attr/empty_background, empty_foreground, empty_title */
    val emptyBackground: Color,
    val emptyForeground: Color,
    val emptyTitle: Color,

    // --- Additional tokens for Wave 1 leaf Composables (section headers, onboarding) ---
    /** ?attr/header_background (section header surfaces; light=#eaffffff, dark=dark_background; TV often gradient drawable) */
    val headerBackground: Color,
    /** ?attr/audio_browser_separator (section header text color; orange accent) */
    val audioBrowserSeparator: Color,
    /** @color/onboarding_grey (onboarding_*.xml tools:background + Theme.VLC.Onboarding.* ; deep blue-grey #011422) */
    val onboardingBackground: Color,
)

/** Light scheme - derived from Theme.VLC.Apearance (MaterialComponents.Light.NoActionBar parent) */
private val LightVLCColors = VLCColorScheme(
    // M3
    primary = Orange800,
    onPrimary = White,
    secondary = Orange800,
    onSecondary = Grey50,
    background = White,
    onBackground = Grey900,
    surface = White,
    onSurface = Grey900,
    error = Color(0xFFBB0000),
    onError = White,

    // VLC semantics (light)
    backgroundDefault = White,                    // styles.xml:41
    backgroundDefaultDarker = Grey200,            // :57
    listTitle = Black,                            // via res/color/list_title.xml + style:68
    listSubtitle = Grey600,                       // via res/color/list_subtitle.xml
    fontDefault = Grey900,                        // :63
    fontLight = Grey700,                          // :64
    fontAudioLight = Grey700,                     // :66
    fontDisabled = Color(0x80616161),             // grey700transparent
    defaultDivider = BlackTransparent20,          // :45
    playerIconColor = Black,                      // :92
    audioMenuIcon = BlackTransparent50,           // :47
    aboutTextPrimary = Black,                     // :108 (light)
    cardBackground = White,                       // :93
    cardBorder = Grey300,                         // :96
    audioHeaderBackground = White,                // :43
    audioHeaderDivider = Grey400,                 // :44
    audioPlayerGradientColor = WhiteTransparent80, // :105/:106 via gradient_audio_player_* drawables
    audioSeekTrack = BlackTransparent20,          // :49 audio_seekbar background/secondaryProgress
    bottomNavigationBackground = White,           // approx (real is drawable)
    audioChipsColor = BlackTransparent75,         // :72
    audioChipsTextColor = WhiteTransparent60,     // :73
    audioChipBackground = BlackTransparent75,     // :102 rounded_corners_audio
    audioChipTextColor = WhiteTransparent60,      // :103
    subtleSelection = BlackTransparent10,         // :46
    primaryFocus = Orange800Transparent20,        // :79
    emptyBackground = Grey50,                     // :98
    emptyForeground = Grey300,                    // :97
    emptyTitle = Grey850,                         // :104
    // Wave 1 leaf tokens (light)
    headerBackground = Color(0xEAFFFFFF),         // whitetransparent_ea from colors.xml + styles:56
    audioBrowserSeparator = Orange800,            // styles.xml:61
    onboardingBackground = OnboardingGrey,        // colors.xml:127 + onboarding styles
)

/** Dark scheme - derived from Theme.VLC.Apearance.Black (and values-night parent) */
private val DarkVLCColors = VLCColorScheme(
    // M3
    primary = Orange500,
    onPrimary = White,
    secondary = Orange600,
    onSecondary = Grey50,
    background = DarkBackground,
    onBackground = Grey50,
    surface = DarkBackground,
    onSurface = Grey50,
    error = Color(0xFFBB0000),
    onError = White,

    // VLC semantics (dark)
    backgroundDefault = DarkBackground,           // :235
    backgroundDefaultDarker = Grey875,            // :251
    listTitle = White,                            // via res/color/list_title_dark.xml + style:262
    listSubtitle = Grey400,                       // via list_subtitle_dark + overrides
    fontDefault = Grey50,                         // :257
    fontLight = Grey600,                          // :258
    fontAudioLight = Grey400,                     // :259
    fontDisabled = Color(0x80757575),             // grey600transparent variant
    defaultDivider = WhiteTransparent20,          // :239
    playerIconColor = White,                      // :286
    audioMenuIcon = WhiteTransparent50,           // :241
    aboutTextPrimary = Orange500,                 // :303 (dark)
    cardBackground = Black,                       // :287
    cardBorder = Grey800,                         // :291
    audioHeaderBackground = MiniPlayerDark,       // :237
    audioHeaderDivider = Black,                   // :238
    audioPlayerGradientColor = BlackTransparent80, // :300/:301 via gradient_audio_player_*_dark drawables
    audioSeekTrack = Color(0x33EEEEEE),           // :243 audio_seekbar_black background/secondaryProgress
    bottomNavigationBackground = DarkBackground,  // approx (real: bottom_navigation_background_dark drawable)
    audioChipsColor = WhiteTransparent90,         // :266
    audioChipsTextColor = BlackTransparent60,     // :267
    audioChipBackground = WhiteTransparent75,     // :297 rounded_corners_audio_dark
    audioChipTextColor = BlackTransparent60,      // :298
    subtleSelection = WhiteTransparent10,         // :240
    primaryFocus = Orange500Transparent20,        // :273
    emptyBackground = Grey900,                    // :293
    emptyForeground = Grey800,                    // :292
    emptyTitle = Grey300,                         // :299
    // Wave 1 leaf tokens (dark)
    headerBackground = DarkBackground,            // styles.xml:250
    audioBrowserSeparator = Orange500,            // styles.xml:255
    onboardingBackground = OnboardingGrey,        // colors.xml:127
)

// CompositionLocal for access to full VLC semantic tokens (beyond MaterialTheme.colorScheme)
val LocalVLCColors = staticCompositionLocalOf { LightVLCColors }

// Backwards-compatible object (expanded). Components can use VLCThemeDefaults.colors.xxx
val LocalVLCTheme = staticCompositionLocalOf { VLCThemeDefaults }

object VLCThemeDefaults {
    val colors: VLCColorScheme
        @Composable
        get() = LocalVLCColors.current

    // Legacy single-token access (will be removed as components migrate)
    val primary: Color @Composable get() = colors.primary
}

// ============================================================
// Typography (VLC flavor - matches usages of sans-serif-black, medium titles in styles)
// ============================================================
val VLCTypography = Typography(
    // Titles / headers often use black weight in VLC (see Tooltip, About.Title, VLC.CtxTitle)
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    // Body / list items
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    // Subtitles / secondary (font_light style)
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = Grey700, // will be overridden by explicit usage of our tokens
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    ),
)

// Basic shapes (4dp cards from VLCCardView + VLCCardView.NoShadow in styles.xml:805)
val VLCShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // matches VLCCardView corner radius in styles.xml
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
)

// Helper to build M3 ColorScheme from our richer VLCColorScheme
private fun buildColorScheme(vlc: VLCColorScheme, dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = vlc.primary,
            onPrimary = vlc.onPrimary,
            secondary = vlc.secondary,
            onSecondary = vlc.onSecondary,
            background = vlc.backgroundDefault,
            onBackground = vlc.fontDefault,
            surface = vlc.backgroundDefault,
            onSurface = vlc.fontDefault,
            error = vlc.error,
            onError = vlc.onError,
        )
    } else {
        lightColorScheme(
            primary = vlc.primary,
            onPrimary = vlc.onPrimary,
            secondary = vlc.secondary,
            onSecondary = vlc.onSecondary,
            background = vlc.backgroundDefault,
            onBackground = vlc.fontDefault,
            surface = vlc.backgroundDefault,
            onSurface = vlc.fontDefault,
            error = vlc.error,
            onError = vlc.onError,
        )
    }

@Composable
fun VLCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val vlcColors = if (darkTheme) DarkVLCColors else LightVLCColors
    val colorScheme = buildColorScheme(vlcColors, darkTheme)

    CompositionLocalProvider(
        LocalVLCColors provides vlcColors,
        LocalVLCTheme provides VLCThemeDefaults
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VLCTypography,
            shapes = VLCShapes,
            content = content
        )
    }
}
