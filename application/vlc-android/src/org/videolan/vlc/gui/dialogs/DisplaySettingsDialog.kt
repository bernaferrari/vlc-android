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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = title,
                color = colors.primary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
            displayInCards?.let {
                ActionSettingRow(
                    title = if (it) R.string.display_in_list else R.string.display_in_grid,
                    icon = if (it) R.drawable.ic_view_list else R.drawable.ic_view_grid,
                    onClick = onDisplayModeClicked
                )
            }
            showAllArtists?.let {
                ToggleSettingRow(
                    title = R.string.artists_show_all_title,
                    icon = R.drawable.ic_sort_artist,
                    checked = it,
                    onCheckedChange = onShowAllArtistsChanged
                )
            }
            showOnlyMultimediaFiles?.let {
                ToggleSettingRow(
                    title = R.string.browser_show_all_title,
                    icon = R.drawable.ic_multimedia,
                    checked = it,
                    onCheckedChange = onShowOnlyMultimediaFilesChanged
                )
            }
            showTrackNumbers?.let {
                ToggleSettingRow(
                    title = R.string.albums_show_track_numbers,
                    icon = R.drawable.ic_sort_track,
                    checked = it,
                    onCheckedChange = onShowTrackNumbersChanged
                )
            }
            showHiddenFiles?.let {
                ToggleSettingRow(
                    title = R.string.browser_show_hidden_files_title,
                    icon = R.drawable.ic_hidden,
                    checked = it,
                    onCheckedChange = onShowHiddenFilesChanged
                )
            }
            onlyFavs?.let {
                ToggleSettingRow(
                    title = R.string.show_only_favs,
                    icon = R.drawable.ic_favorite,
                    checked = it,
                    onCheckedChange = onOnlyFavsChanged
                )
            }
            selectedVideoGroup?.let {
                EnumSettingRow(
                    title = R.string.video_min_group_length_title,
                    subtitle = null,
                    icon = R.drawable.ic_group_display,
                    options = DisplaySettingsDialog.VideoGroup.entries.toList(),
                    selected = it,
                    optionText = { option -> option.toString() },
                    onOptionSelected = onVideoGroupChanged
                )
            }
            if (defaultPlaybackActions.isNotEmpty() && selectedDefaultAction != null) {
                EnumSettingRow(
                    title = R.string.default_playback_action,
                    subtitle = defaultActionType,
                    icon = R.drawable.ic_play,
                    options = defaultPlaybackActions,
                    selected = selectedDefaultAction,
                    optionText = { option -> option.toString() },
                    onOptionSelected = onDefaultActionChanged
                )
            }
            if (sorts.isNotEmpty()) {
                SectionTitle(text = R.string.sortby)
                sorts.forEach { sort ->
                    SortSettingRow(
                        sort = sort,
                        currentSort = currentSort,
                        currentSortDesc = currentSortDesc,
                        enabled = !sortLocked,
                        onSortChanged = onSortChanged
                    )
                }
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
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun ActionSettingRow(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit
) {
    SettingRowFrame(
        onClick = onClick,
        leadingContent = {
            SettingIcon(icon = icon)
        },
        textContent = {
            SettingText(title = title)
        },
        trailingContent = {}
    )
}

@Composable
private fun ToggleSettingRow(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRowFrame(
        onClick = { onCheckedChange(!checked) },
        role = Role.Checkbox,
        leadingContent = {
            SettingIcon(icon = icon)
        },
        textContent = {
            SettingText(title = title)
        },
        trailingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun <T> EnumSettingRow(
    @StringRes title: Int,
    subtitle: String?,
    @DrawableRes icon: Int,
    options: List<T>,
    selected: T,
    optionText: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember(title, selected) { mutableStateOf(false) }
    SettingRowFrame(
        onClick = { expanded = true },
        leadingContent = {
            SettingIcon(icon = icon)
        },
        textContent = {
            SettingText(
                title = title,
                subtitle = subtitle ?: optionText(selected)
            )
        },
        trailingContent = {
            Box(modifier = Modifier.width(156.dp)) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = optionText(selected),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    )
}

@Composable
private fun SortSettingRow(
    sort: Int,
    currentSort: Int,
    currentSortDesc: Boolean,
    enabled: Boolean,
    onSortChanged: (Int, Boolean) -> Unit
) {
    val metadata = sortMetadata(sort)
    val selectedSort = sort == currentSort || currentSort == Medialibrary.SORT_DEFAULT && sort == Medialibrary.SORT_ALPHA
    SettingRowFrame(
        enabled = enabled,
        onClick = null,
        leadingContent = {
            SettingIcon(icon = metadata.icon, enabled = enabled)
        },
        textContent = {
            SettingText(title = metadata.title, enabled = enabled)
        },
        trailingContent = {
            Column(
                modifier = Modifier.width(144.dp)
            ) {
                SortDirectionButton(
                    text = metadata.ascending,
                    selected = selectedSort && !currentSortDesc,
                    enabled = enabled,
                    onClick = { onSortChanged(sort, false) }
                )
                SortDirectionButton(
                    text = metadata.descending,
                    selected = selectedSort && currentSortDesc,
                    enabled = enabled,
                    onClick = { onSortChanged(sort, true) }
                )
            }
        }
    )
}

@Composable
private fun SortDirectionButton(
    @StringRes text: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.6f)
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = AppContextProvider.appContext.getString(text),
            color = when {
                !enabled -> colors.fontDisabled
                selected -> colors.primary
                else -> colors.fontDefault
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingRowFrame(
    enabled: Boolean = true,
    onClick: (() -> Unit)?,
    role: Role? = null,
    leadingContent: @Composable () -> Unit,
    textContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = role) { onClick() } else Modifier)
            .focusable(enabled)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            leadingContent()
            Box(modifier = Modifier.weight(1f)) {
                textContent()
            }
            trailingContent()
        }
        HorizontalDivider(
            color = colors.defaultDivider,
            modifier = Modifier.padding(start = 72.dp)
        )
    }
}

@Composable
private fun SettingIcon(
    @DrawableRes icon: Int,
    enabled: Boolean = true
) {
    val colors = VLCThemeDefaults.colors
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = if (enabled) colors.primary else colors.fontDisabled,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun SettingText(
    @StringRes title: Int,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val colors = VLCThemeDefaults.colors
    Column {
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
