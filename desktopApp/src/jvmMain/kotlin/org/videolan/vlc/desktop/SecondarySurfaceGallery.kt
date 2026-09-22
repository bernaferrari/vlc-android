package org.videolan.vlc.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import org.videolan.vlc.compose.components.*
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCAppTheme
import org.videolan.vlc.compose.player.VideoSurfaceWithHud
import org.videolan.vlc.compose.player.PlayerArtworkFallback
import org.videolan.vlc.model.Progress
import org.videolan.vlc.viewmodel.PlayerUiState

/** Fixtures only: production components with local state, no file/network/account side effects. */
internal enum class QASurface(val label: String) {
    Rename("Rename"), Warning("Preference warning"), FeatureWarning("Feature warning"),
    Duplicates("Duplicate media"), Renderer("Renderer picker"), Equalizer("Equalizer editor"),
    Pin("PIN entry"), Otp("Pairing code"), Update("Update"), WhatsNew("What’s new"),
    ExternalDevice("External device"), Widget("Widget guide"), AutoInfo("Android Auto guide"),
    Player("Playback tools"), About("About"), Authors("Authors"), Libraries("Libraries"), Feedback("Feedback"),
}

@Composable
internal fun SecondarySurfaceGallery(
    surface: QASurface,
    previewSize: DpSize,
    fontScale: Float,
    onClose: () -> Unit,
) {
    // A native resizable window supplies finite height. Production content owns its scroll area;
    // wrapping it in another vertical scroll would break its lazy lists and weighted content.
    DialogWindow(
        onCloseRequest = onClose,
        title = "QA · ${surface.label}",
        state = rememberDialogState(size = DpSize(
            previewSize.width.coerceAtMost(680.dp), previewSize.height.coerceAtMost(800.dp),
        )),
        resizable = true,
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            VLCAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    key(surface) { GalleryContent(surface, onClose) }
                }
            }
        }
    }
}

@Composable
private fun GalleryContent(surface: QASurface, close: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue("A quiet morning by the sea")) }
    var checked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinSuccess by remember { mutableStateOf(false) }
    var renderer by remember { mutableStateOf("living-room") }
    when (surface) {
        QASurface.Rename -> VLCRenameDialogContent(
            title = "Rename media", mediaTitle = "A quiet morning by the sea.mp4", newTitleHint = "New name",
            okText = "Rename", newName = name, onNewNameChange = { name = it }, onConfirm = close,
        )
        QASurface.Warning -> VLCPreferenceChangeWarningDialogContent(
            title = "Restart playback?", message = "Your new audio output will apply when playback restarts. Your current position will be kept.",
            okText = "Restart", cancelText = "Cancel", onConfirm = close, onCancel = close,
        )
        QASurface.FeatureWarning -> VLCFeatureWarningDialogContent(
            title = "Advanced playback", genericWarning = "Changing these options may affect playback on this device.",
            detailWarning = "You can restore the defaults in Settings at any time.", swipeText = "Slide to continue",
            isDpadAllowed = true, onSwipeStart = {}, onSwipeStop = {}, onUnlock = close,
        )
        QASurface.Duplicates -> VLCDuplicationWarningDialogContent(
            title = "Some tracks are already here", message = AnnotatedString("3 of these tracks are already in Slow mornings."),
            cancelText = "Cancel", addText = "Add", addAllText = "Add all", addNewOnlyText = "Add new only",
            showThreeOptions = true, onCancel = close, onAddAll = close, onAddNew = close,
        )
        QASurface.Renderer -> VLCRendererPickerDialogContent(
            title = "Play on a device", renderers = listOf(
                VLCRendererUiItem("living-room", "Living room TV", renderer == "living-room", true),
                VLCRendererUiItem("studio", "Studio speakers", renderer == "studio", false),
                VLCRendererUiItem("bedroom", "Bedroom television with a longer device name", renderer == "bedroom", true),
            ), disconnectText = "Disconnect", showDisconnect = renderer.isNotEmpty(),
            onRendererSelected = { renderer = it.id }, onDisconnect = { renderer = "" },
            rendererIcon = { _, tint -> Icon(MaterialSymbols.Filled.Devices, null, tint = tint ?: MaterialTheme.colorScheme.onSurface) },
        )
        QASurface.Equalizer -> EqualizerFixture(close)
        QASurface.Player -> {
            var playing by remember { mutableStateOf(false) }
            var time by remember { mutableStateOf(72_000L) }
            val progress = Progress(time = time, length = 240_000L)
            VideoSurfaceWithHud(
                title = "A quiet morning", subtitle = "Studio sessions", playing = playing, progress = progress,
                onTogglePlay = { playing = !playing }, onSeek = { time = it }, onNext = {}, onPrevious = {}, onClose = close,
                surface = { PlayerArtworkFallback(PlayerUiState(title = "A quiet morning", subtitle = "Studio sessions", progress = progress)) },
            )
        }
        QASurface.Pin -> VLCPinCodeScreen(
            reasonText = "Keep your media private", title = "Enter your PIN", pin = pin,
            showPinEntry = !pinSuccess, showSuccess = pinSuccess, successText = "Unlocked",
            showVirtualKeyboard = true, nextText = "Unlock", cancelText = "Cancel", deleteContentDescription = "Delete digit",
            nextEnabled = pin.length >= 4, showCancel = true, onPinChange = { pin = it.take(6) },
            onDigit = { pin = (pin + it).take(6) }, onBackspace = { pin = pin.dropLast(1) },
            onNext = { pinSuccess = true }, onCancel = close,
        )
        QASurface.Otp -> VLCOTPCodeScreen(
            title = "Connect to VLC", subtitle = "Enter this code on your other device to allow access.",
            cancelText = "Cancel", code = "482719", onCancel = close,
        )
        QASurface.Update -> VLCUpdateDialogContent(
            title = "A new VLC is available", description = "Playback improvements, a refreshed library, and fixes for network streams.",
            nightlyVersion = "4.0 · September 22", neverAskAgainText = "Don’t ask again", neverAskAgain = checked,
            openInBrowserText = "Release notes", installText = "Install", showInstall = true, isDownloading = false,
            onNeverAskAgainChange = { checked = it }, onOpenInBrowser = {}, onInstall = close,
        )
        QASurface.WhatsNew -> VLCWhatsNewDialogContent(
            title = "What’s new", items = listOf(
                VLCWhatsNewItem("library", "Your library, refreshed", "Find your videos, music, and playlists in a quieter, clearer library.", "Explore library"),
                VLCWhatsNewItem("playback", "More room for your music", "Large artwork and familiar playback controls keep your media in focus.", "Open player"),
                VLCWhatsNewItem("privacy", "Made for your media", "Keep local files organized and manage your privacy from Settings.", "Open settings"),
            ), neverShowAgainText = "Don’t show again", neverShowAgain = checked,
            onNeverShowAgainChange = { checked = it }, onItemAction = { close() },
        )
        QASurface.ExternalDevice -> VLCExternalDeviceDialogContent(
            title = "USB drive connected", message = "Browse this drive or add its media to your library.",
            browseText = "Browse", scanText = "Add to library", cancelText = "Cancel", showScan = true,
            onBrowse = close, onScan = close, onCancel = close,
        )
        QASurface.Widget -> VLCWidgetExplanationDialogContent(
            title = "Playback on your home screen", sizeText = "Choose the widget size that works for you.",
            resizeText = "Touch and hold the widget to resize it.", endText = "Your controls are always close by.",
            nextText = "Next", closeText = "Done", sizePreviewCount = 3, onClose = close,
            sizePreviewContent = { index, modifier -> WidgetSample(modifier, "Widget ${index + 1}") },
            resizePreviewContent = { WidgetSample(it, "Drag to resize") },
            tapIconContent = { Icon(MaterialSymbols.Filled.PlayArrow, null, modifier = it) },
            themeIconContent = { Icon(MaterialSymbols.Filled.Palette, null, modifier = it) },
        )
        QASurface.AutoInfo -> VLCAutoInfoDialogContent(
            title = "VLC on Android Auto", podcastModeTitle = "Listen at your pace",
            podcastModeText = "Use podcast controls to skip forward or go back while listening.",
            voiceControlTitle = "Keep your attention on the road", voiceControlText = "Ask your car’s voice assistant to play music in VLC.",
        )
        QASurface.About -> VLCAboutScreen(
            title = "About VLC", appName = "VLC media player", description = "A free, open source media player for everyone.",
            versionInfo = VLCAboutVersionInfo("4.0 preview", "September 22, 2026", "A refreshed media experience.", emptyList()),
            copyright = "© VideoLAN and contributors", licenseTitle = "GNU General Public License",
            licenseText = "VLC is free software, distributed under the GNU General Public License.",
            websiteTitle = "Website", feedbackTitle = "Send feedback", sourcesTitle = "Source code",
            librariesTitle = "Open source libraries", authorsTitle = "Contributors", closeContentDescription = "Close",
            openLinkContentDescription = "Open link", onClose = close, onOpenWebsite = {}, onSendFeedback = {},
            onOpenSources = {}, onOpenLibraries = {}, onOpenAuthors = {}, onOpenLicenseLink = {},
        )
        QASurface.Authors -> VLCAuthorsScreen(
            title = "Contributors", authors = listOf("VideoLAN contributors", "Jean-Baptiste Kempf", "Rémi Denis-Courmont", "Geoffrey Métais", "Community translators"),
            closeContentDescription = "Close", onClose = close,
        )
        QASurface.Libraries -> VLCLibrariesScreen(
            title = "Open source libraries", libraries = listOf(
                VLCLibraryLicense("Kotlin", "JetBrains and contributors", "Apache License 2.0", "Licensed under the Apache License, Version 2.0.", "https://kotlinlang.org"),
                VLCLibraryLicense("libVLC", "VideoLAN and contributors", "GNU LGPL 2.1", "The media engine behind VLC playback.", "https://www.videolan.org"),
            ), closeContentDescription = "Close", openLinkContentDescription = "Open license", onClose = close, onOpenLicenseLink = {},
        )
        QASurface.Feedback -> FeedbackFixture(close)
    }
}

@Composable
private fun WidgetSample(modifier: Modifier, title: String) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(MaterialSymbols.Filled.MusicNote, null)
            Text(title, modifier = Modifier.weight(1f))
            Icon(MaterialSymbols.Filled.PlayArrow, null)
        }
    }
}

@Composable
private fun EqualizerFixture(close: () -> Unit) {
    val initial = remember { VLCEqualizerPreset(1, "Warm listening", -1, false, true, 0f,
        listOf("60 Hz", "170 Hz", "310 Hz", "600 Hz", "1 kHz", "3 kHz", "6 kHz", "12 kHz", "14 kHz", "16 kHz")
            .mapIndexed { index, label -> VLCEqualizerBand(index, label, 0f) }) }
    var preset by remember { mutableStateOf(initial) }
    var enabled by remember { mutableStateOf(true) }
    var snap by remember { mutableStateOf(false) }
    VLCEqualizerEditorDialogContent(
        state = VLCEqualizerEditorState(listOf(preset), preset, enabled, snap, true, true, preset.name, null),
        strings = VLCEqualizerSettingsStrings("Equalizer", "Preferences", "Close", "Show equalizer", "Import", "More actions",
            "Show all", "Hide all", "Export all", "Import all", "Enable preset", "Disable preset", "Delete", "Export",
            "Preset name", "Cancel", "Enable equalizer", "Add preset", "Edit preset", "Undo", "Preamp", "Snap bands", "Done"),
        onDismiss = close, onOpenSettings = {}, onEqualizerEnabledChange = { enabled = it }, onAddEqualizer = {},
        onSelectPreset = {}, onEditPreset = {}, onUndo = { preset = initial }, onDelete = close,
        onNameChange = { preset = preset.copy(name = it) }, onNameFocusChange = {},
        onPreampChange = { preset = preset.copy(preamp = it) },
        onBandChange = { index, value -> preset = preset.copy(bands = preset.bands.map { if (it.index == index) it.copy(value = value) else it }) },
        onBandChangeFinished = {}, onSnapBandsChange = { snap = it },
    )
}

@Composable
private fun FeedbackFixture(close: () -> Unit) {
    var subject by remember { mutableStateOf("A note about my library") }
    var message by remember { mutableStateOf("") }
    var feedbackType by remember { mutableStateOf(0) }
    var includeLogs by remember { mutableStateOf(false) }
    var includeLibrary by remember { mutableStateOf(false) }
    VLCFeedbackScreen(
        title = "Send feedback", feedbackForumTitle = "Community forum", feedbackForumSummary = "Ask questions and share ideas.",
        readDocTitle = "Read the documentation", readDocSummary = "Find help with playback and your library.",
        emailSupportTitle = "Contact support", emailSupportSummary = "Describe what happened.", rateTitle = "Rate VLC", rateSummary = "Share your experience.",
        feedbackTypeLabel = "Feedback type", feedbackTypeEntries = listOf("Feedback", "Bug report", "Feature request"),
        selectedFeedbackTypeIndex = feedbackType, subject = subject, subjectLabel = "Subject", message = message, messageLabel = "Message",
        showForumCard = true, showDocCard = true, showRateCard = true, showEmailSupportForm = true,
        showEmailWarning = false, emailWarningTitle = "Email unavailable", emailWarningExplanation = "Set up an email app to continue.",
        tryAnywayText = "Try anyway", openSettingsText = "Settings", showIncludes = true, includeMedialibrary = includeLibrary,
        includeMedialibraryText = "Include media library", medialibraryWarning = "May include file names.", includeLogs = includeLogs,
        includeLogsText = "Include logs", logsWarning = "Logs may contain media paths.", sendText = "Send", sendEnabled = message.isNotBlank(),
        closeContentDescription = "Close", onClose = close, onFeedbackForum = {}, onReadDoc = {}, onEmailSupport = {}, onRate = {},
        onTryAnyway = {}, onOpenSettings = {}, onFeedbackTypeSelected = { feedbackType = it }, onSubjectChange = { subject = it },
        onMessageChange = { message = it }, onIncludeMedialibraryChange = { includeLibrary = it }, onIncludeLogsChange = { includeLogs = it }, onSend = close,
    )
}
