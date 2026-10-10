package org.videolan.vlc.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.videolan.vlc.compose.icons.MaterialIcon

/*
 * Declarative description of the settings surface.
 *
 * The shared Settings pane renders these items on every platform. Common settings are described by
 * the shared view model; platform-only settings are contributed by a [SettingsContributor] that is
 * registered in the platform Koin graph. A platform that contributes nothing simply has no extra
 * sections, so desktop, iOS and web never show a dead row.
 *
 * Items carry their current value: contributors rebuild their items whenever [SettingsContributor.changes]
 * emits, which keeps one source of truth (the platform preference store) and no shadow copies.
 */

/** One selectable value of a [SettingsChoice], [SettingsMultiChoice] or [SettingsStepper]. */
data class SettingsOption(
    val value: String,
    val label: String,
    val summary: String? = null,
)

/** A confirmation shown before a value change or an action is applied. */
data class SettingsConfirmation(
    val title: String,
    val message: String,
    val confirmLabel: String,
    /** Destructive confirmations use the error colour for their confirm button. */
    val destructive: Boolean = true,
)

sealed interface SettingsItem {
    /** Preference key (or action id). Keys are unique across the whole settings surface. */
    val key: String
    val title: String
    val summary: String?
    val enabled: Boolean
    /** Extra words the in-settings search should match, e.g. an English fallback. */
    val keywords: List<String>
}

data class SettingsToggle(
    override val key: String,
    override val title: String,
    val checked: Boolean,
    override val summary: String? = null,
    override val enabled: Boolean = true,
    /** Asked before turning the switch on. */
    val confirmOn: SettingsConfirmation? = null,
    /** Asked before turning the switch off. */
    val confirmOff: SettingsConfirmation? = null,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** Single choice presented as a sheet of radio rows. The summary defaults to the selected label. */
data class SettingsChoice(
    override val key: String,
    override val title: String,
    val options: List<SettingsOption>,
    val selected: String?,
    override val summary: String? = null,
    override val enabled: Boolean = true,
    override val keywords: List<String> = emptyList(),
) : SettingsItem {
    val selectedLabel: String? get() = options.firstOrNull { it.value == selected }?.label
}

/** Multiple choice presented as a sheet of check rows, committed with one Done action. */
data class SettingsMultiChoice(
    override val key: String,
    override val title: String,
    val options: List<SettingsOption>,
    val selected: Set<String>,
    override val summary: String? = null,
    override val enabled: Boolean = true,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** A short ordered list of values nudged with a connected −/+ control. */
data class SettingsStepper(
    override val key: String,
    override val title: String,
    val options: List<SettingsOption>,
    val selected: String,
    override val summary: String? = null,
    override val enabled: Boolean = true,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** A continuous integer range. The value is committed when the drag ends. */
data class SettingsSlider(
    override val key: String,
    override val title: String,
    val value: Int,
    val range: IntRange,
    val step: Int = 1,
    val valueLabel: (Int) -> String = { it.toString() },
    override val summary: String? = null,
    override val enabled: Boolean = true,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** Free text edited in a dialog. When [summary] is null the current value is shown instead. */
data class SettingsTextInput(
    override val key: String,
    override val title: String,
    val value: String,
    override val summary: String? = null,
    val placeholder: String? = null,
    val numeric: Boolean = false,
    val multiline: Boolean = false,
    val maxLength: Int? = null,
    override val enabled: Boolean = true,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** An opaque RGB colour (alpha is ignored) chosen from a palette or a hex code. */
data class SettingsColor(
    override val key: String,
    override val title: String,
    val color: Int,
    val palette: List<Int> = DefaultSettingsPalette,
    override val summary: String? = null,
    override val enabled: Boolean = true,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** A one-shot command, optionally confirmed. */
data class SettingsAction(
    override val key: String,
    override val title: String,
    override val summary: String? = null,
    override val enabled: Boolean = true,
    val icon: MaterialIcon? = null,
    val destructive: Boolean = false,
    val confirmation: SettingsConfirmation? = null,
    /** Opens something outside settings (another screen or system page). */
    val external: Boolean = false,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** Opens another settings section. */
data class SettingsLink(
    override val key: String,
    override val title: String,
    val sectionId: String,
    override val summary: String? = null,
    override val enabled: Boolean = true,
    override val keywords: List<String> = emptyList(),
) : SettingsItem

/** Static explanatory text (or live status) inside a group. */
data class SettingsNote(
    override val key: String,
    val text: String,
    val tone: SettingsNoteTone = SettingsNoteTone.Neutral,
    override val keywords: List<String> = emptyList(),
) : SettingsItem {
    override val title: String get() = text
    override val summary: String? get() = null
    override val enabled: Boolean get() = true
}

enum class SettingsNoteTone { Neutral, Accent, Error }

data class SettingsGroup(
    /** Groups with the same id are merged, so a platform can extend a shared group. */
    val id: String,
    val title: String?,
    val items: List<SettingsItem>,
)

data class SettingsSection(
    val id: String,
    val title: String,
    val icon: MaterialIcon?,
    val groups: List<SettingsGroup>,
    val summary: String? = null,
    /** Child sections are only reachable through a [SettingsLink] and do not appear on the root. */
    val parentId: String? = null,
    /** Optional header action, e.g. an explanation of the feature. Dispatched to [SettingsContributor.onAction]. */
    val infoActionKey: String? = null,
    val infoActionLabel: String? = null,
)

/** Transient feedback shown in the settings snackbar. */
data class SettingsMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** Ids of the shared root groups that platform contributors may extend. */
object SettingsGroupIds {
    const val PLAYBACK = "playback"
    const val PRIVACY = "privacy"
    const val LIBRARY = "library"
    const val BROWSER = "browser"
    const val NETWORK = "network"
}

/**
 * Platform extension point for the shared settings surface (registered in Koin as
 * `single<SettingsContributor>`; several may coexist).
 */
interface SettingsContributor {
    /** Emits whenever a value behind this contributor's items changed. */
    val changes: Flow<Unit> get() = emptyFlow()

    /** Snackbar feedback for platform side effects. */
    val messages: Flow<SettingsMessage> get() = emptyFlow()

    /** Groups shown on the root page. A group whose id matches a shared group extends it. */
    fun rootGroups(): List<SettingsGroup> = emptyList()

    /** Sections; those without a [SettingsSection.parentId] get a row on the root page. */
    fun sections(): List<SettingsSection> = emptyList()

    /** Persists a value produced by one of this contributor's items, then runs its side effects. */
    fun onValueChange(key: String, value: Any) = Unit

    /** Runs one of this contributor's [SettingsAction]s or its section info action. */
    fun onAction(key: String) = Unit

    /** Called after any settings value changed, including shared ones, for platform side effects. */
    fun afterValueChange(key: String, value: Any) = Unit

    /** Optional confirmation for a shared value change (for example a destructive toggle). */
    fun confirmationFor(key: String, value: Any): SettingsConfirmation? = null

    /** Gate for entering Settings at all (for example a parental PIN). */
    suspend fun canOpenSettings(): Boolean = true

    /** Gate for one section. */
    suspend fun canOpenSection(id: String): Boolean = true

    fun onSectionOpened(id: String) = Unit
    fun onSectionClosed(id: String) = Unit

    /** The user left the settings surface; flush deferred side effects here. */
    fun onSettingsClosed() = Unit
}

val DefaultSettingsPalette: List<Int> = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFFFC107.toInt(),
    0xFF8BC34A.toInt(),
    0xFF00BCD4.toInt(),
    0xFF2196F3.toInt(),
    0xFFE91E63.toInt(),
    0xFF9E9E9E.toInt(),
    0xFF000000.toInt(),
)
