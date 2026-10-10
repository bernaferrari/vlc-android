package org.videolan.vlc.compose.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.AUDIO_RESUME_PLAYBACK
import org.videolan.tools.BROWSER_SHOW_HIDDEN_FILES
import org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE
import org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE
import org.videolan.tools.KEY_SHOW_HEADERS
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.SHOW_VIDEO_THUMBNAILS
import org.videolan.tools.VIDEO_HUD_TIMEOUT
import org.videolan.tools.VIDEO_RESUME_PLAYBACK
import org.videolan.vlc.compose.components.VLCIconChip
import org.videolan.vlc.compose.components.VLCPageHeader
import org.videolan.vlc.compose.components.vlcSelectionWash
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCAppTheme
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.settings.SettingsDeepLinks
import org.videolan.vlc.settings.SettingsGroup
import org.videolan.vlc.settings.SettingsGroupIds
import org.videolan.vlc.settings.SettingsItem
import org.videolan.vlc.settings.SettingsLink
import org.videolan.vlc.settings.SettingsNote
import org.videolan.vlc.settings.SettingsNoteTone
import org.videolan.vlc.settings.SettingsOption
import org.videolan.vlc.settings.SettingsSection
import org.videolan.vlc.settings.SettingsStepper
import org.videolan.vlc.settings.SettingsToggle
import org.videolan.vlc.viewmodel.SettingsUiState
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.SharedSettingsKeys

/*
 * The shared settings surface: one root list (theme, grouped common settings, platform sections)
 * and one renderer for every section. Platform settings arrive as declarative model items from a
 * SettingsContributor, so every target draws exactly the same rows, sheets and dialogs.
 */

private const val ROOT_LEADING_ITEMS = 2 // search field + theme picker

@Composable
internal fun SettingsRootPane(
    modifier: Modifier,
    vm: SettingsViewModel,
    onOpenSection: (String) -> Unit,
    selectedSectionId: String? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val contributed by vm.contributed.collectAsStateWithLifecycle()
    val highlightedKey by vm.highlightedKey.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val shared = sharedSettingsGroups(state)
    val rootGroups = remember(shared, contributed) { mergeGroups(shared, contributed.rootGroups) }
    val rootSections = contributed.sections.filter { it.parentId == null }
    val callbacks = rememberRowCallbacks(vm, onOpenSection)
    val listState = rememberLazyListState()
    RevealHighlight(vm, highlightedKey, listState) { key ->
        rootGroups.indexOfFirst { group -> group.items.any { it.key == key } }
            .takeIf { it >= 0 }?.plus(ROOT_LEADING_ITEMS)
    }

    SettingsSurface(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = VLCLayout.ScreenGutter),
            contentPadding = SettingsListPadding,
            verticalArrangement = Arrangement.spacedBy(VLCLayout.SectionGap),
        ) {
            item(key = "search") {
                SettingsSearchField(query = query, onQueryChange = { query = it })
            }
            if (query.isBlank()) {
                item(key = "theme") {
                    ThemePicker(
                        appearance = state.themeAppearance,
                        accent = state.themeAccent,
                        onAppearanceChange = vm::setThemeAppearance,
                        onAccentChange = vm::setThemeAccent,
                    )
                }
                items(rootGroups, key = { "group-${it.id}" }) { group ->
                    SettingsGroupBlock(group.title, group.items, highlightedKey, callbacks)
                }
                if (rootSections.isNotEmpty()) {
                    item(key = "sections") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsGroupTitle(ShellStrings.moreSettings())
                            SettingsCard(rootSections.map { section ->
                                { SectionNavigationRow(section, section.id == selectedSectionId) { onOpenSection(section.id) } }
                            })
                        }
                    }
                }
            } else {
                val results = searchSettings(query, rootGroups, contributed.sections)
                if (results.isEmpty()) {
                    item(key = "no-results") {
                        Text(
                            ShellStrings.settingsNoResults(query.trim()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        )
                    }
                } else {
                    items(results, key = { "result-${it.id}" }) { group ->
                        SettingsGroupBlock(group.title, group.items, highlightedKey, callbacks)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsSectionPane(
    modifier: Modifier,
    vm: SettingsViewModel,
    sectionId: String,
    onOpenSection: (String) -> Unit,
) {
    val contributed by vm.contributed.collectAsStateWithLifecycle()
    val highlightedKey by vm.highlightedKey.collectAsStateWithLifecycle()
    val section = contributed.section(sectionId)
    val callbacks = rememberRowCallbacks(vm, onOpenSection)
    val listState = rememberLazyListState()
    val groups = section?.groups.orEmpty().filter { it.items.isNotEmpty() }
    RevealHighlight(vm, highlightedKey, listState) { key ->
        groups.indexOfFirst { group -> group.items.any { it.key == key } }.takeIf { it >= 0 }
    }
    SettingsSurface(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = VLCLayout.ScreenGutter),
            contentPadding = SettingsListPadding,
            verticalArrangement = Arrangement.spacedBy(VLCLayout.SectionGap),
        ) {
            items(groups, key = { it.id }) { group ->
                SettingsGroupBlock(group.title, group.items, highlightedKey, callbacks)
            }
        }
    }
}

/** Header action for a section that offers an explanation (for example Android Auto). */
@Composable
internal fun SettingsSectionInfoAction(vm: SettingsViewModel, sectionId: String) {
    val contributed by vm.contributed.collectAsStateWithLifecycle()
    val section = contributed.section(sectionId) ?: return
    if (section.infoActionKey == null) return
    IconButton(onClick = { vm.infoAction(section) }) {
        Icon(MaterialSymbols.Filled.Info, contentDescription = section.infoActionLabel ?: ShellStrings.info())
    }
}

/** Shown in the detail pane of a wide layout before a section is chosen. */
@Composable
internal fun SettingsSectionPlaceholder() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            VLCIconChip(size = 56.dp) { tint -> Icon(MaterialSymbols.Filled.Settings, contentDescription = null, tint = tint) }
            Text(
                ShellStrings.selectSettingsSection(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Dialogs and snackbars of the settings surface; composed once per host. */
@Composable
internal fun SettingsOverlays(vm: SettingsViewModel, snackbarHostState: SnackbarHostState) {
    val pending by vm.pendingConfirmation.collectAsStateWithLifecycle()
    pending?.let { request ->
        SettingsConfirmationDialog(
            confirmation = request.confirmation,
            onConfirm = vm::confirmPending,
            onDismiss = vm::dismissPending,
        )
    }
    LaunchedEffect(vm) {
        vm.messages.collect { message ->
            launch {
                val result = snackbarHostState.showSnackbar(message.text, actionLabel = message.actionLabel, withDismissAction = message.actionLabel == null)
                if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
            }
        }
    }
}

/**
 * Settings without the main shell, for hosts that open Settings on their own (Android TV and
 * external entry points). Keeps its own section stack and the same row renderer. The host provides
 * the theme (Android TV forces its dark surface).
 */
@Composable
fun VlcSettingsScreen(
    vm: SettingsViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    initialKey: String? = null,
) {
    run {
        val stack = remember { mutableStateListOf<String>() }
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(initialKey) {
            if (initialKey != null) {
                stack.clear()
                stack.addAll(vm.sectionPathFor(initialKey))
                vm.highlight(initialKey)
            }
        }
        LaunchedEffect(stack.toList()) { vm.onOpenSectionsChanged(stack.toSet()) }
        fun open(id: String) = scope.launch { if (vm.canOpenSection(id)) stack.add(id) }
        fun back() = if (stack.isEmpty()) onExit() else stack.removeAt(stack.lastIndex).let { }
        HandleShellBackPress(enabled = true, onBack = ::back)
        val contributed by vm.contributed.collectAsStateWithLifecycle()
        val current = stack.lastOrNull()
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = VLCThemeDefaults.colors.backgroundDefault,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            VLCUtilityPane(Modifier.padding(padding)) {
                Column(Modifier.fillMaxSize()) {
                    VLCPageHeader(
                        title = current?.let { contributed.section(it)?.title } ?: ShellStrings.settings(),
                        navigationIcon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                        navigationContentDescription = ShellStrings.back(),
                        onNavigate = ::back,
                        actions = { if (current != null) SettingsSectionInfoAction(vm, current) },
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (current == null) {
                            SettingsRootPane(Modifier.fillMaxSize(), vm, onOpenSection = { open(it) })
                        } else {
                            SettingsSectionPane(Modifier.fillMaxSize(), vm, current, onOpenSection = { open(it) })
                        }
                    }
                }
            }
        }
        SettingsOverlays(vm, snackbar)
    }
}

@Composable
private fun SettingsSurface(modifier: Modifier, content: @Composable () -> Unit) {
    VLCUtilityPane(modifier = modifier) {
        Surface(modifier = Modifier.fillMaxSize(), color = VLCThemeDefaults.colors.backgroundDefault) {
            content()
        }
    }
}

@Composable
private fun rememberRowCallbacks(vm: SettingsViewModel, onOpenSection: (String) -> Unit) =
    remember(vm, onOpenSection) {
        SettingsRowCallbacks(
            onChange = vm::change,
            onAction = vm::action,
            onOpenSection = onOpenSection,
        )
    }

/** Scrolls a deep-linked row into view, washes it briefly, then forgets it. */
@Composable
private fun RevealHighlight(
    vm: SettingsViewModel,
    key: String?,
    listState: LazyListState,
    indexOf: (String) -> Int?,
) {
    LaunchedEffect(key) {
        val target = key ?: return@LaunchedEffect
        val index = indexOf(target) ?: return@LaunchedEffect
        listState.animateScrollToItem(index)
        delay(2_400)
        vm.clearHighlight(target)
    }
}

@Composable
private fun SectionNavigationRow(section: SettingsSection, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VLCLayout.RowHeight)
            .vlcSelectionWash(selected)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VLCIconChip(selected = selected, containerColor = if (selected) Color.Unspecified else MaterialTheme.colorScheme.secondaryContainer) { tint ->
            section.icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(section.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            section.summary?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(MaterialSymbols.AutoMirrored.Filled.ChevronForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val PlaybackSpeedChoices = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 6f, 8f)
private val PlaybackSpeedOptions = PlaybackSpeedChoices.map { SettingsOption(value = it.toString(), label = "${it}×") }
private val HudTimeoutOptions = (1..10).map { SettingsOption(value = it.toString(), label = "${it}s") }

private fun nearestSpeed(rate: Float): String =
    PlaybackSpeedChoices.minBy { kotlin.math.abs(it - rate) }.toString()

/** The common settings every platform shows, expressed in the shared model. */
@Composable
private fun sharedSettingsGroups(state: SettingsUiState): List<SettingsGroup> {
    val playback = SettingsGroup(
        id = SettingsGroupIds.PLAYBACK,
        title = ShellStrings.playback(),
        items = listOf(
            SettingsToggle(AUDIO_RESUME_PLAYBACK, ShellStrings.resumeAudio(), state.audioResume, ShellStrings.audioResumeSummary()),
            SettingsToggle(VIDEO_RESUME_PLAYBACK, ShellStrings.resumeVideo(), state.videoResume, ShellStrings.videoResumeSummary()),
            SettingsStepper(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, ShellStrings.defaultAudioPlaybackSpeed(), PlaybackSpeedOptions, nearestSpeed(state.defaultAudioPlaybackSpeed)),
            SettingsStepper(KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE, ShellStrings.defaultVideoPlaybackSpeed(), PlaybackSpeedOptions, nearestSpeed(state.defaultVideoPlaybackSpeed)),
            SettingsToggle(PLAYBACK_HISTORY, ShellStrings.playbackHistory(), state.playbackHistory, ShellStrings.playbackHistorySummary()),
            SettingsToggle(KEY_INCOGNITO, ShellStrings.incognito(), state.incognito, ShellStrings.incognitoSummary()),
            SettingsStepper(VIDEO_HUD_TIMEOUT, ShellStrings.videoHudTimeout(), HudTimeoutOptions, state.videoHudTimeoutSeconds.toString()),
        ),
    )
    val privacy = if (state.appLock.supported) SettingsGroup(
        id = SettingsGroupIds.PRIVACY,
        title = ShellStrings.privacy(),
        items = buildList {
            add(SettingsToggle(SharedSettingsKeys.APP_LOCK, ShellStrings.appLock(), state.appLock.enabled, ShellStrings.appLockSummary()))
            if (state.appLock.enabled && state.appLock.biometricsAvailable) {
                add(SettingsToggle(SharedSettingsKeys.APP_LOCK_BIOMETRICS, ShellStrings.useBiometrics(), state.appLock.biometricsEnabled, ShellStrings.useBiometricsSummary()))
            }
        },
    ) else null
    val library = SettingsGroup(
        id = SettingsGroupIds.LIBRARY,
        title = ShellStrings.library(),
        items = listOf(
            SettingsToggle(SHOW_VIDEO_THUMBNAILS, ShellStrings.videoThumbnails(), state.showVideoThumbs, ShellStrings.videoThumbnailsSummary()),
            SettingsToggle(KEY_SHOW_HEADERS, ShellStrings.showHeaders(), state.showHeaders, ShellStrings.showHeadersSummary()),
            SettingsToggle(ALBUMS_SHOW_TRACK_NUMBER, ShellStrings.showTrackNumbers(), state.showTrackNumbers),
        ),
    )
    val browser = SettingsGroup(
        id = SettingsGroupIds.BROWSER,
        title = ShellStrings.browser(),
        items = listOf(
            SettingsToggle(BROWSER_SHOW_HIDDEN_FILES, ShellStrings.showHiddenFiles(), state.showHiddenFiles, ShellStrings.showHiddenFilesSummary()),
            SettingsToggle(BROWSER_SHOW_ONLY_MULTIMEDIA, ShellStrings.multimediaFilesOnly(), state.showOnlyMultimedia, ShellStrings.multimediaFilesOnlySummary()),
        ),
    )
    val remoteStatus = when {
        !state.supportsRemoteAccess -> null
        state.remoteAccessStarting -> SettingsNote(SharedSettingsKeys.REMOTE_ACCESS_STATUS, ShellStrings.remoteAccessStarting())
        state.remoteAccessAddress != null -> SettingsNote(
            SharedSettingsKeys.REMOTE_ACCESS_STATUS,
            "${ShellStrings.remoteAccessUploadAddress()}\n${state.remoteAccessAddress}",
            SettingsNoteTone.Accent,
        )
        state.remoteAccessError != null -> SettingsNote(SharedSettingsKeys.REMOTE_ACCESS_STATUS, state.remoteAccessError, SettingsNoteTone.Error)
        else -> null
    }
    val network = SettingsGroup(
        id = SettingsGroupIds.NETWORK,
        title = ShellStrings.network(),
        items = buildList {
            if (state.supportsNetworkBrowsing) add(SettingsToggle(KEY_BROWSE_NETWORK, ShellStrings.browseNetwork(), state.browseNetwork))
            if (state.supportsRemoteAccess) add(SettingsToggle(KEY_ENABLE_REMOTE_ACCESS, ShellStrings.remoteAccessServer(), state.remoteAccess))
            remoteStatus?.let(::add)
        },
    )
    return listOfNotNull(playback, privacy, library, browser, network)
}

/** Platform groups extend shared groups with the same id; new groups follow the shared ones. */
private fun mergeGroups(shared: List<SettingsGroup>, contributed: List<SettingsGroup>): List<SettingsGroup> {
    val merged = shared.toMutableList()
    for (group in contributed) {
        val index = merged.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            merged[index] = merged[index].copy(items = merged[index].items + group.items)
        } else {
            merged += group
        }
    }
    return merged.filter { it.items.isNotEmpty() }
}

private fun SettingsItem.matches(terms: List<String>): Boolean {
    if (this is SettingsNote) return false
    val haystack = buildString {
        append(title).append(' ')
        summary?.let { append(it).append(' ') }
        keywords.forEach { append(it).append(' ') }
    }
    return terms.all { haystack.contains(it, ignoreCase = true) }
}

/** Live search across every visible row; results keep their own controls so they can be used directly. */
private fun searchSettings(query: String, rootGroups: List<SettingsGroup>, sections: List<SettingsSection>): List<SettingsGroup> {
    val terms = query.trim().split(' ').filter { it.isNotBlank() }
    if (terms.isEmpty()) return emptyList()
    val seen = mutableSetOf<String>()
    val results = mutableListOf<SettingsGroup>()
    val sectionLinks = sections.filter { section -> terms.all { section.title.contains(it, ignoreCase = true) } }
        .map { SettingsLink("open-${it.id}", it.title, it.id, it.summary) }
    if (sectionLinks.isNotEmpty()) results += SettingsGroup("sections", null, sectionLinks)
    rootGroups.forEach { group ->
        val matches = group.items.filter { it.matches(terms) && seen.add(it.key) }
        if (matches.isNotEmpty()) results += SettingsGroup("root-${group.id}", group.title, matches)
    }
    sections.forEach { section ->
        section.groups.forEach { group ->
            val matches = group.items.filter { it.matches(terms) && seen.add(it.key) }
            if (matches.isNotEmpty()) {
                val crumb = listOfNotNull(section.title, group.title?.takeIf { it != section.title }).joinToString(" › ")
                results += SettingsGroup("${section.id}-${group.id}", crumb, matches)
            }
        }
    }
    return results
}

/** Requests from outside the shell (other screens, notifications) to show a setting. */
internal val settingsDeepLinkRequests get() = SettingsDeepLinks.pending
