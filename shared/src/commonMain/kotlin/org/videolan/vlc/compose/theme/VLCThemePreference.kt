package org.videolan.vlc.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import org.videolan.tools.VlcSettings

/**
 * The appearance preference is deliberately independent from the accent. This keeps a person's
 * light/dark choice stable while they explore VLC's colour options.
 */
enum class VLCThemeAppearance(val storageValue: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    fun resolveDarkTheme(systemIsDark: Boolean): Boolean = when (this) {
        System -> systemIsDark
        Light -> false
        Dark -> true
    }

    companion object {
        fun fromStorage(value: String?): VLCThemeAppearance =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

/**
 * Curated VLC accents. Orange stays first and is the product default; the alternatives are
 * intentionally restrained, dark-enough light-theme primaries with a corresponding dark value.
 * This gives iOS, Android and Web the same deterministic result without depending on Android
 * colour APIs in shared UI.
 */
enum class VLCThemeAccent(
    val storageValue: String,
    val swatchColor: Color,
    internal val lightPrimary: Color,
    internal val darkPrimary: Color,
) {
    Dynamic(
        storageValue = "dynamic",
        swatchColor = Color(0xFF6F7EAF),
        lightPrimary = Color(0xFFC44A00),
        darkPrimary = Color(0xFFFF8800),
    ),
    // VLC's default must remain unmistakably VLC on a dark surface, not a diluted peach.
    Orange("orange", Color(0xFFFF7200), Color(0xFFC44A00), Color(0xFFFF8800)),
    Amber("amber", Color(0xFFFFB300), Color(0xFF765800), Color(0xFFFFDEA7)),
    Lime("lime", Color(0xFF8ABF00), Color(0xFF4D6600), Color(0xFFC7ED85)),
    Green("green", Color(0xFF2E9D4C), Color(0xFF006E26), Color(0xFF78DC8B)),
    Teal("teal", Color(0xFF008F83), Color(0xFF006A61), Color(0xFF67D7C6)),
    Cyan("cyan", Color(0xFF008CA0), Color(0xFF006875), Color(0xFF70D8E9)),
    Blue("blue", Color(0xFF367FC4), Color(0xFF165E9C), Color(0xFFA0C9FF)),
    Indigo("indigo", Color(0xFF6270C4), Color(0xFF4956A6), Color(0xFFC1C2FF)),
    Purple("purple", Color(0xFF9568B3), Color(0xFF78558F), Color(0xFFE9B6FF)),
    Pink("pink", Color(0xFFD05A95), Color(0xFFA33A73), Color(0xFFFFB0D4)),
    ;

    fun primary(darkTheme: Boolean): Color = if (darkTheme) darkPrimary else lightPrimary

    companion object {
        val Default: VLCThemeAccent = Orange

        fun fromStorage(value: String?): VLCThemeAccent =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

data class VLCThemePreference(
    val appearance: VLCThemeAppearance = VLCThemeAppearance.System,
    val accent: VLCThemeAccent = VLCThemeAccent.Default,
)

/** The Dynamic swatch is meaningful only when Android can source wallpaper colours. */
fun availableVLCThemeAccents(): List<VLCThemeAccent> =
    if (supportsDynamicTheme()) VLCThemeAccent.entries else VLCThemeAccent.entries - VLCThemeAccent.Dynamic

/**
 * Resolves persisted input defensively. A Dynamic preference can be carried between devices, but
 * unsupported targets always retain VLC orange rather than an Android-looking faux dynamic theme.
 */
fun resolveVLCThemePreference(
    appearanceValue: String?,
    accentValue: String?,
): VLCThemePreference {
    val accent = VLCThemeAccent.fromStorage(accentValue).let { selected ->
        if (selected == VLCThemeAccent.Dynamic && !supportsDynamicTheme()) VLCThemeAccent.Default else selected
    }
    return VLCThemePreference(
        appearance = VLCThemeAppearance.fromStorage(appearanceValue),
        accent = accent,
    )
}

/**
 * App-level theme boundary, equivalent to QuietGuard's repository-backed application theme.
 * [VLCTheme] remains dependency-free for previews and isolated component tests.
 */
@Composable
fun VLCAppTheme(content: @Composable () -> Unit) {
    val appearance by VlcSettings.themeAppearance.collectAsState()
    val accent by VlcSettings.themeAccent.collectAsState()
    val preference = resolveVLCThemePreference(appearance, accent)
    VLCTheme(
        darkTheme = preference.appearance.resolveDarkTheme(systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()),
        accent = preference.accent,
        content = content,
    )
}
