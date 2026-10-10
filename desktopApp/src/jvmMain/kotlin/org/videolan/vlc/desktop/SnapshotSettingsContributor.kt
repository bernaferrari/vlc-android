package org.videolan.vlc.desktop

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.videolan.vlc.settings.SettingsAction
import org.videolan.vlc.settings.SettingsChoice
import org.videolan.vlc.settings.SettingsColor
import org.videolan.vlc.settings.SettingsConfirmation
import org.videolan.vlc.settings.SettingsContributor
import org.videolan.vlc.settings.SettingsGroup
import org.videolan.vlc.settings.SettingsGroupIds
import org.videolan.vlc.settings.SettingsIcons
import org.videolan.vlc.settings.SettingsLink
import org.videolan.vlc.settings.SettingsMultiChoice
import org.videolan.vlc.settings.SettingsNote
import org.videolan.vlc.settings.SettingsOption
import org.videolan.vlc.settings.SettingsSection
import org.videolan.vlc.settings.SettingsSlider
import org.videolan.vlc.settings.SettingsTextInput
import org.videolan.vlc.settings.SettingsToggle

/**
 * Design-review fixture: a platform contributor shaped like Android's, so the snapshot task can
 * render subpages and every row kind. Disabled by default — the real desktop app contributes
 * nothing and must show only the shared settings.
 */
object SnapshotSettingsContributor : SettingsContributor {
    var enabled = false
    private val values = mutableMapOf<String, Any>()
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val changes: SharedFlow<Unit> = _changes

    private fun bool(key: String, default: Boolean) = values[key] as? Boolean ?: default
    private fun string(key: String, default: String) = values[key] as? String ?: default
    private fun int(key: String, default: Int) = values[key] as? Int ?: default

    override fun onValueChange(key: String, value: Any) {
        values[key] = value
        _changes.tryEmit(Unit)
    }

    override fun rootGroups(): List<SettingsGroup> = if (!enabled) emptyList() else listOf(
        SettingsGroup(
            SettingsGroupIds.LIBRARY, null, listOf(
                SettingsAction("directories", "Media library folders", "Select which folders VLC scans for media"),
                SettingsToggle("auto_rescan", "Auto rescan", bool("auto_rescan", true), "Rescan the media library each time VLC starts"),
            )
        ),
        SettingsGroup(
            SettingsGroupIds.NETWORK, null, listOf(
                SettingsChoice(
                    "metered_connection", "Metered connections",
                    listOf(SettingsOption("0", "Ask every time"), SettingsOption("1", "Always allow"), SettingsOption("2", "Never stream")),
                    string("metered_connection", "0"),
                ),
            )
        ),
    )

    override fun sections(): List<SettingsSection> = if (!enabled) emptyList() else listOf(
        SettingsSection("interface", "Interface", SettingsIcons.Interface, emptyList(), "Language, TV interface, lists"),
        SettingsSection("video", "Video", SettingsIcons.Video, emptyList(), "Hardware acceleration, orientation, pop-up"),
        SettingsSection(
            "subtitles", "Subtitles", SettingsIcons.Subtitles, summary = "Style, encoding, preferred language",
            groups = listOf(
                SettingsGroup(
                    "general", null, listOf(
                        SettingsChoice(
                            "subtitles_presets", "Presets",
                            listOf(SettingsOption("0", "Default"), SettingsOption("1", "Small"), SettingsOption("2", "Boxed"), SettingsOption("4", "Yellow")),
                            null, summary = "Apply a ready-made style",
                        ),
                        SettingsToggle("subtitles_autoload", "Load subtitles automatically", bool("subtitles_autoload", true)),
                        SettingsChoice(
                            "subtitle_text_encoding", "Text encoding",
                            listOf(SettingsOption("", "Default (Windows-1252)"), SettingsOption("UTF-8", "Universal (UTF-8)")),
                            string("subtitle_text_encoding", ""),
                        ),
                    )
                ),
                SettingsGroup(
                    "font", "Font style", listOf(
                        SettingsChoice(
                            "subtitles_size", "Size",
                            listOf(SettingsOption("10", "Tiny"), SettingsOption("13", "Small"), SettingsOption("16", "Normal"), SettingsOption("20", "Big")),
                            string("subtitles_size", "16"),
                        ),
                        SettingsToggle("subtitles_bold", "Bold", bool("subtitles_bold", false)),
                        SettingsColor("subtitles_color", "Colour", int("subtitles_color", 0xFFFFFFFF.toInt())),
                        SettingsSlider("subtitles_color_opacity", "Opacity", int("subtitles_color_opacity", 255), 50..255, valueLabel = { "${it * 100 / 255}%" }),
                    )
                ),
                SettingsGroup(
                    "outline", "Outline", listOf(
                        SettingsToggle("subtitles_outline", "Outline", bool("subtitles_outline", true)),
                        SettingsColor("subtitles_outline_color", "Colour", int("subtitles_outline_color", 0xFF000000.toInt())),
                    )
                ),
            ),
        ),
        SettingsSection("audio", "Audio", SettingsIcons.Audio, emptyList(), "Output, ReplayGain, headset"),
        SettingsSection("remote_access", "Remote access", SettingsIcons.RemoteAccess, emptyList(), "Control VLC from a browser", infoActionKey = "remote_access_info"),
        SettingsSection(
            "advanced", "Advanced", SettingsIcons.Advanced, summary = "Performance, developer options, app data",
            groups = listOf(
                SettingsGroup(
                    "general", null, listOf(
                        SettingsLink("optional_features", "Optional features", "optional_features", "Enable experimental features"),
                        SettingsTextInput("network_caching", "Network caching value", string("network_caching", "0"), "Caching for network resources, in milliseconds", numeric = true),
                        SettingsMultiChoice(
                            "remote_access_ml_content", "Media library content",
                            listOf(SettingsOption("video", "Videos"), SettingsOption("audio", "Audio"), SettingsOption("playlists", "Playlists")),
                            setOf("video", "audio"),
                        ),
                    )
                ),
                SettingsGroup(
                    "data", "App data", listOf(
                        SettingsAction("export_settings", "Export settings", "Save your settings to a file"),
                        SettingsAction(
                            "clear_media_db", "Clear media database", "Media will be scanned again", destructive = true,
                            confirmation = SettingsConfirmation("Clear media database", "Your media library will be rebuilt.", "Clear"),
                        ),
                        SettingsAction("debug_logs", "Debug logs", external = true),
                    )
                ),
                SettingsGroup("note", null, listOf(SettingsNote("adv_note", "Changing these values restarts the media engine."))),
            ),
        ),
        SettingsSection(
            "optional_features", "Optional features", SettingsIcons.OptionalFeatures, parentId = "advanced",
            groups = listOf(SettingsGroup("flags", null, listOf(SettingsToggle("feature_x", "Experimental feature", false)))),
        ),
    )
}
