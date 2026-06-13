/*
 * ************************************************************************
 *  DisplaySettingsDialog.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.dialogs

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
import org.videolan.resources.GROUP_VIDEOS_FOLDER
import org.videolan.resources.GROUP_VIDEOS_NAME
import org.videolan.resources.GROUP_VIDEOS_NONE
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCIconChip
import org.videolan.vlc.compose.components.VLCSettingsCard
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.DefaultPlaybackAction
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.util.isTalkbackIsEnabled
import org.videolan.vlc.viewmodels.DisplaySettingsViewModel
import org.videolan.vlc.viewmodels.mobile.VideoGroupingType
import org.videolan.vlc.viewmodels.mobile.VideosViewModel

const val DISPLAY_IN_CARDS = "display_in_cards"
const val SHOW_ALL_ARTISTS = "show_all_artists"
const val VIDEO_GROUPING = "show_video_groups"
const val ONLY_FAVS = "only_favs"
const val SORTS = "sorts"
const val CURRENT_SORT = "current_sort"
const val CURRENT_SORT_DESC = "current_sort_desc"
const val SHOW_ONLY_MULTIMEDIA_FILES = "show_only_multimedia_files"
const val SHOW_HIDDEN_FILES = "show_hidden_files"
const val SHOW_TRACK_NUMBER = "show_track_number"
const val DEFAULT_ACTIONS = "default_actions"
const val DEFAULT_ACTION_TYPE = "default_action_type"

object DisplaySettingsDialog {
    /**
     * Video grouping entry
     *
     * @property value the value to be saved in the shared preferences
     * @property title the title resources to be shown
     * @property type the [VideosViewModel] type for this grouping
     */
    enum class VideoGroup(val value: String, val title: Int, val type: VideoGroupingType) {
        GROUP_BY_NAME(GROUP_VIDEOS_NAME, R.string.video_min_group_length_name, VideoGroupingType.NAME),
        GROUP_BY_FOLDER(GROUP_VIDEOS_FOLDER, R.string.video_min_group_length_folder, VideoGroupingType.FOLDER),
        NO_GROUP(GROUP_VIDEOS_NONE, R.string.video_min_group_length_disable, VideoGroupingType.NONE);

        override fun toString(): String {
            return AppContextProvider.appContext.getString(title)
        }

        companion object {
            /**
             * Retrieve a [VideoGroup] by its value
             *
             * @param value of the video group to retrieve
             * @return a [VideoGroup]
             */
            fun findByValue(value: String?): VideoGroup {
                entries.forEach { if (value == it.value) return it }
                return GROUP_BY_NAME
            }
        }
    }
}

private var isDisplaySettingsComposeDialogShowing = false

fun ComponentActivity.showDisplaySettingsComposeDialog(
    displayInCards: Boolean?,
    showAllArtists: Boolean? = null,
    onlyFavs: Boolean?,
    sorts: List<Int>,
    currentSort: Int,
    currentSortDesc: Boolean,
    videoGroup: String? = null,
    showOnlyMultimediaFiles: Boolean? = null,
    showTrackNumber: Boolean? = null,
    showHiddenFiles: Boolean? = null,
    defaultPlaybackActions: List<DefaultPlaybackAction>? = null,
    defaultActionType: String? = null
) {
    if (isDisplaySettingsComposeDialogShowing) return
    isDisplaySettingsComposeDialogShowing = true
    lifecycleScope.launch {
        if (showPinIfNeeded()) {
            isDisplaySettingsComposeDialogShowing = false
            return@launch
        }
        DisplaySettingsComposeDialog(
            activity = this@showDisplaySettingsComposeDialog,
            displayInCards = displayInCards,
            showAllArtists = showAllArtists,
            onlyFavs = onlyFavs,
            sorts = sorts,
            currentSort = currentSort,
            currentSortDesc = currentSortDesc,
            videoGroup = videoGroup,
            showOnlyMultimediaFiles = showOnlyMultimediaFiles,
            showTrackNumbers = showTrackNumber,
            showHiddenFiles = showHiddenFiles,
            defaultPlaybackActions = defaultPlaybackActions,
            defaultActionType = defaultActionType,
            onDismissed = { isDisplaySettingsComposeDialogShowing = false }
        ).show()
    }
}

private class DisplaySettingsComposeDialog(
    private val activity: ComponentActivity,
    displayInCards: Boolean?,
    showAllArtists: Boolean?,
    onlyFavs: Boolean?,
    private val sorts: List<Int>,
    currentSort: Int,
    currentSortDesc: Boolean,
    videoGroup: String?,
    showOnlyMultimediaFiles: Boolean?,
    showTrackNumbers: Boolean?,
    showHiddenFiles: Boolean?,
    defaultPlaybackActions: List<DefaultPlaybackAction>?,
    private val defaultActionType: String?,
    private val onDismissed: () -> Unit
) {
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private val displaySettingsViewModel = ViewModelProvider(activity)[DisplaySettingsViewModel::class.java]
    private val displayInCardsState = mutableStateOf(displayInCards)
    private val showAllArtistsState = mutableStateOf(showAllArtists)
    private val onlyFavsState = mutableStateOf(onlyFavs)
    private val showOnlyMultimediaFilesState = mutableStateOf(showOnlyMultimediaFiles)
    private val showTrackNumbersState = mutableStateOf(showTrackNumbers)
    private val showHiddenFilesState = mutableStateOf(showHiddenFiles)
    private val videoGroupState = mutableStateOf(
        videoGroup?.let { DisplaySettingsDialog.VideoGroup.findByValue(it) }
    )
    private val currentSortState = mutableStateOf(currentSort)
    private val currentSortDescState = mutableStateOf(currentSortDesc)
    private val sortLockedState = mutableStateOf(displaySettingsViewModel.lockSortFlow.value)
    private val defaultPlaybackActionsState = defaultPlaybackActions.orEmpty()
    private val currentDefaultActionState = mutableStateOf(defaultPlaybackActions?.find { it.selected } ?: defaultPlaybackActions?.firstOrNull())
    private var rootView: ComposeView? = null
    private var sortLockJob: Job? = null

    fun show() {
        setupContent()
        sortLockJob = activity.lifecycleScope.launch {
            displaySettingsViewModel.lockSortFlow.collect { locked ->
                sortLockedState.value = locked
            }
        }
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    DisplaySettingsContent(
                        title = activity.getString(R.string.display_settings),
                        displayInCards = displayInCardsState.value,
                        showAllArtists = showAllArtistsState.value,
                        onlyFavs = onlyFavsState.value,
                        showOnlyMultimediaFiles = showOnlyMultimediaFilesState.value,
                        showTrackNumbers = showTrackNumbersState.value,
                        showHiddenFiles = showHiddenFilesState.value,
                        selectedVideoGroup = videoGroupState.value,
                        defaultPlaybackActions = defaultPlaybackActionsState,
                        selectedDefaultAction = currentDefaultActionState.value,
                        defaultActionType = defaultActionType,
                        sorts = sorts,
                        currentSort = currentSortState.value,
                        currentSortDesc = currentSortDescState.value,
                        sortLocked = sortLockedState.value,
                        onDisplayModeClicked = ::toggleDisplayMode,
                        onShowAllArtistsChanged = ::setShowAllArtists,
                        onOnlyFavsChanged = ::setOnlyFavs,
                        onShowOnlyMultimediaFilesChanged = ::setShowOnlyMultimediaFiles,
                        onShowTrackNumbersChanged = ::setShowTrackNumbers,
                        onShowHiddenFilesChanged = ::setShowHiddenFiles,
                        onVideoGroupChanged = ::setVideoGroup,
                        onDefaultActionChanged = ::setDefaultAction,
                        onSortChanged = ::setSort
                    )
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            sortLockJob?.cancel()
            sortLockJob = null
            rootView = null
            onDismissed()
        }
    }

    private fun toggleDisplayMode() {
        val next = !(displayInCardsState.value ?: return)
        displayInCardsState.value = next
        send(DISPLAY_IN_CARDS, next)
    }

    private fun setShowAllArtists(checked: Boolean) {
        showAllArtistsState.value = checked
        send(SHOW_ALL_ARTISTS, checked)
    }

    private fun setOnlyFavs(checked: Boolean) {
        onlyFavsState.value = checked
        send(ONLY_FAVS, checked)
    }

    private fun setShowOnlyMultimediaFiles(checked: Boolean) {
        showOnlyMultimediaFilesState.value = checked
        send(SHOW_ONLY_MULTIMEDIA_FILES, checked)
    }

    private fun setShowTrackNumbers(checked: Boolean) {
        showTrackNumbersState.value = checked
        send(SHOW_TRACK_NUMBER, checked)
    }

    private fun setShowHiddenFiles(checked: Boolean) {
        showHiddenFilesState.value = checked
        send(SHOW_HIDDEN_FILES, checked)
    }

    private fun setVideoGroup(videoGroup: DisplaySettingsDialog.VideoGroup) {
        if (videoGroup == videoGroupState.value) return
        videoGroupState.value = videoGroup
        send(VIDEO_GROUPING, videoGroup)
        dialog.dismiss()
    }

    private fun setDefaultAction(defaultPlaybackAction: DefaultPlaybackAction) {
        if (defaultPlaybackAction == currentDefaultActionState.value) return
        currentDefaultActionState.value = defaultPlaybackAction
        send(DEFAULT_ACTIONS, defaultPlaybackAction)
    }

    private fun setSort(sort: Int, desc: Boolean) {
        currentSortState.value = sort
        currentSortDescState.value = desc
        send(CURRENT_SORT, Pair(sort, desc))
    }

    private fun send(key: String, value: Any) {
        activity.lifecycleScope.launch { displaySettingsViewModel.send(key, value) }
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusable = false
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusableInTouchMode = false
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplaySettingsContent(
    title: String,
    displayInCards: Boolean?,
    showAllArtists: Boolean?,
    onlyFavs: Boolean?,
    showOnlyMultimediaFiles: Boolean?,
    showTrackNumbers: Boolean?,
    showHiddenFiles: Boolean?,
    selectedVideoGroup: DisplaySettingsDialog.VideoGroup?,
    defaultPlaybackActions: List<DefaultPlaybackAction>,
    selectedDefaultAction: DefaultPlaybackAction?,
    defaultActionType: String?,
    sorts: List<Int>,
    currentSort: Int,
    currentSortDesc: Boolean,
    sortLocked: Boolean,
    onDisplayModeClicked: () -> Unit,
    onShowAllArtistsChanged: (Boolean) -> Unit,
    onOnlyFavsChanged: (Boolean) -> Unit,
    onShowOnlyMultimediaFilesChanged: (Boolean) -> Unit,
    onShowTrackNumbersChanged: (Boolean) -> Unit,
    onShowHiddenFilesChanged: (Boolean) -> Unit,
    onVideoGroupChanged: (DisplaySettingsDialog.VideoGroup) -> Unit,
    onDefaultActionChanged: (DefaultPlaybackAction) -> Unit,
    onSortChanged: (Int, Boolean) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 2.dp)
            )
            displayInCards?.let { inCards ->
                DisplayModeSelector(
                    inCards = inCards,
                    onModeSelected = { wantCards -> if (wantCards != inCards) onDisplayModeClicked() }
                )
            }
            val optionRows = buildList<@Composable () -> Unit> {
                showAllArtists?.let { value ->
                    add { SwitchSettingRow(R.string.artists_show_all_title, R.drawable.ic_sort_artist, value, onShowAllArtistsChanged) }
                }
                showOnlyMultimediaFiles?.let { value ->
                    add { SwitchSettingRow(R.string.browser_show_all_title, R.drawable.ic_multimedia, value, onShowOnlyMultimediaFilesChanged) }
                }
                showTrackNumbers?.let { value ->
                    add { SwitchSettingRow(R.string.albums_show_track_numbers, R.drawable.ic_sort_track, value, onShowTrackNumbersChanged) }
                }
                showHiddenFiles?.let { value ->
                    add { SwitchSettingRow(R.string.browser_show_hidden_files_title, R.drawable.ic_hidden, value, onShowHiddenFilesChanged) }
                }
                onlyFavs?.let { value ->
                    add { SwitchSettingRow(R.string.show_only_favs, R.drawable.ic_favorite, value, onOnlyFavsChanged) }
                }
                selectedVideoGroup?.let { group ->
                    add {
                        EnumSettingRow(
                            title = R.string.video_min_group_length_title,
                            icon = R.drawable.ic_group_display,
                            options = DisplaySettingsDialog.VideoGroup.entries.toList(),
                            selected = group,
                            optionText = { option -> option.toString() },
                            onOptionSelected = onVideoGroupChanged
                        )
                    }
                }
                if (defaultPlaybackActions.isNotEmpty() && selectedDefaultAction != null) {
                    add {
                        EnumSettingRow(
                            title = R.string.default_playback_action,
                            icon = R.drawable.ic_play,
                            options = defaultPlaybackActions,
                            selected = selectedDefaultAction,
                            optionText = { option -> option.toString() },
                            onOptionSelected = onDefaultActionChanged,
                            subtitle = defaultActionType
                        )
                    }
                }
            }
            SettingsCard(rows = optionRows)
            if (sorts.isNotEmpty()) {
                SectionTitle(text = R.string.sortby)
                val sortRows = sorts.map { sort ->
                    @Composable {
                        SortSettingRow(
                            sort = sort,
                            currentSort = currentSort,
                            currentSortDesc = currentSortDesc,
                            enabled = !sortLocked,
                            onSortChanged = onSortChanged
                        )
                    }
                }
                SettingsCard(
                    rows = sortRows,
                    modifier = Modifier.alpha(if (sortLocked) 0.6f else 1f)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    val colors = VLCThemeDefaults.colors
    Text(
        text = AppContextProvider.appContext.getString(text),
        color = colors.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 28.dp, end = 24.dp, top = 6.dp, bottom = 0.dp)
    )
}

/** List/Grid switch rendered as an M3 segmented control - the most legible way to surface a binary view mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayModeSelector(
    inCards: Boolean,
    onModeSelected: (Boolean) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SegmentedButton(
            selected = !inCards,
            onClick = { onModeSelected(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_view_list),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = {
                Text(
                    text = AppContextProvider.appContext.getString(R.string.display_in_list),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
        SegmentedButton(
            selected = inCards,
            onClick = { onModeSelected(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_view_grid),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = {
                Text(
                    text = AppContextProvider.appContext.getString(R.string.display_in_grid),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}

/** Groups related rows inside a single rounded, tonal container with hairline inset dividers between them. */
@Composable
private fun SettingsCard(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    VLCSettingsCard(rows = rows, modifier = modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingRow(
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    role: Role? = null,
    containerColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (containerColor.isSpecified) Modifier.background(containerColor) else Modifier)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = role, onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
private fun SwitchSettingRow(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(
        onClick = { onCheckedChange(!checked) },
        role = Role.Switch
    ) {
        SettingIcon(icon = icon)
        SettingText(title = title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> EnumSettingRow(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    options: List<T>,
    selected: T,
    optionText: (T) -> String,
    onOptionSelected: (T) -> Unit,
    subtitle: String? = null
) {
    val colors = VLCThemeDefaults.colors
    var expanded by remember(title, selected) { mutableStateOf(false) }
    SettingRow(onClick = { expanded = true }) {
        SettingIcon(icon = icon)
        SettingText(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = optionText(selected),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 128.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_collapse_arrow),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionText(option)) },
                        onClick = {
                            expanded = false
                            onOptionSelected(option)
                        }
                    )
                }
            }
        }
    }
}

/**
 * One sort criterion as a single selectable row. The active sort is washed in the accent tint and
 * shows a direction arrow + label; tapping it flips ascending/descending, tapping another switches to it.
 */
@Composable
private fun SortSettingRow(
    sort: Int,
    currentSort: Int,
    currentSortDesc: Boolean,
    enabled: Boolean,
    onSortChanged: (Int, Boolean) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val metadata = sortMetadata(sort)
    val selected = sort == currentSort || (currentSort == Medialibrary.SORT_DEFAULT && sort == Medialibrary.SORT_ALPHA)
    SettingRow(
        enabled = enabled,
        role = Role.Button,
        containerColor = if (selected) colors.primary.copy(alpha = 0.08f) else Color.Unspecified,
        onClick = { if (selected) onSortChanged(sort, !currentSortDesc) else onSortChanged(sort, false) }
    ) {
        SettingIcon(icon = metadata.icon, enabled = enabled, selected = selected)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppContextProvider.appContext.getString(metadata.title),
                color = when {
                    !enabled -> colors.fontDisabled
                    selected -> colors.primary
                    else -> colors.fontDefault
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) {
                Text(
                    text = AppContextProvider.appContext.getString(
                        if (currentSortDesc) metadata.descending else metadata.ascending
                    ),
                    color = if (enabled) colors.fontLight else colors.fontDisabled,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_collapse_arrow),
                contentDescription = null,
                tint = if (enabled) colors.primary else colors.fontDisabled,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (currentSortDesc) 0f else 180f)
            )
        }
    }
}

/**
 * Leading icon rendered inside a rounded tonal chip. The active sort morphs the chip to a filled
 * accent fill - the expressive cue that makes the current selection read instantly.
 */
@Composable
private fun SettingIcon(
    @DrawableRes icon: Int,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    VLCIconChip(selected = selected, enabled = enabled) { tint ->
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SettingText(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val colors = VLCThemeDefaults.colors
    Column(modifier = modifier) {
        Text(
            text = AppContextProvider.appContext.getString(title),
            color = if (enabled) colors.fontDefault else colors.fontDisabled,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = if (enabled) colors.fontLight else colors.fontDisabled,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private data class SortMetadata(
    @StringRes val title: Int,
    @StringRes val ascending: Int,
    @StringRes val descending: Int,
    @DrawableRes val icon: Int
)

private fun sortMetadata(sort: Int): SortMetadata {
    return when (sort) {
        Medialibrary.TrackId -> SortMetadata(R.string.sortby_track, R.string.ascending, R.string.descending, R.drawable.ic_sort_track)
        Medialibrary.SORT_ALPHA -> SortMetadata(R.string.sortby_name, R.string.sort_alpha_asc, R.string.sort_alpha_desc, R.drawable.ic_sort_alpha)
        Medialibrary.SORT_FILENAME -> SortMetadata(R.string.sortby_filename, R.string.sort_alpha_asc, R.string.sort_alpha_desc, R.drawable.ic_sort_filename)
        Medialibrary.SORT_ARTIST -> SortMetadata(R.string.sortby_artist_name, R.string.sort_alpha_asc, R.string.sort_alpha_desc, R.drawable.ic_sort_artist)
        Medialibrary.SORT_DURATION -> SortMetadata(R.string.sortby_length, R.string.sortby_length_asc, R.string.sortby_length_desc, R.drawable.ic_sort_length)
        Medialibrary.SORT_INSERTIONDATE -> SortMetadata(R.string.sortby_date_insertion, R.string.sort_date_asc, R.string.sort_date_desc, R.drawable.ic_medialibrary_date)
        Medialibrary.SORT_LASTMODIFICATIONDATE -> SortMetadata(R.string.sortby_date_last_modified, R.string.sort_date_asc, R.string.sort_date_desc, R.drawable.ic_medialibrary_scan)
        Medialibrary.SORT_ALBUM -> SortMetadata(R.string.sortby_album_name, R.string.sort_alpha_asc, R.string.sort_alpha_desc, R.drawable.ic_sort_album)
        Medialibrary.SORT_RELEASEDATE -> SortMetadata(R.string.sortby_date_release, R.string.sort_date_asc, R.string.sort_date_desc, R.drawable.ic_sort_date)
        Medialibrary.SORT_FILESIZE -> SortMetadata(R.string.file_size, R.string.ascending, R.string.descending, R.drawable.ic_sort_number)
        Medialibrary.NbMedia -> SortMetadata(R.string.sortby_number, R.string.sortby_number_asc, R.string.sortby_number_desc, R.drawable.ic_sort_number)
        else -> throw IllegalStateException("Unsupported sort: $sort")
    }
}
