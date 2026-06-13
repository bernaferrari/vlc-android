package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/about_feedback_activity.xml
 *
 * FeedbackActivity keeps ownership of side effects: mail client detection,
 * DebugLogService, support email intents, settings navigation, and links.
 */
@Composable
fun VLCFeedbackScreen(
    title: String,
    feedbackForumTitle: String,
    feedbackForumSummary: String,
    readDocTitle: String,
    readDocSummary: String,
    emailSupportTitle: String,
    emailSupportSummary: String,
    rateTitle: String,
    rateSummary: String,
    feedbackTypeLabel: String,
    feedbackTypeEntries: List<String>,
    selectedFeedbackTypeIndex: Int,
    subject: String,
    subjectLabel: String,
    message: String,
    messageLabel: String,
    showForumCard: Boolean,
    showDocCard: Boolean,
    showRateCard: Boolean,
    showEmailSupportForm: Boolean,
    showEmailWarning: Boolean,
    emailWarningTitle: String,
    emailWarningExplanation: String,
    tryAnywayText: String,
    openSettingsText: String,
    showIncludes: Boolean,
    includeMedialibrary: Boolean,
    includeMedialibraryText: String,
    medialibraryWarning: String,
    includeLogs: Boolean,
    includeLogsText: String,
    logsWarning: String,
    sendText: String,
    sendEnabled: Boolean,
    closeContentDescription: String,
    onClose: () -> Unit,
    onFeedbackForum: () -> Unit,
    onReadDoc: () -> Unit,
    onEmailSupport: () -> Unit,
    onRate: () -> Unit,
    onTryAnyway: () -> Unit,
    onOpenSettings: () -> Unit,
    onFeedbackTypeSelected: (Int) -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onIncludeMedialibraryChange: (Boolean) -> Unit,
    onIncludeLogsChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    closeIconContent: @Composable () -> Unit = { DefaultFeedbackIconPlaceholder() },
    forumIconContent: @Composable () -> Unit = { DefaultFeedbackIconPlaceholder() },
    docIconContent: @Composable () -> Unit = { DefaultFeedbackIconPlaceholder() },
    emailIconContent: @Composable () -> Unit = { DefaultFeedbackIconPlaceholder() },
    rateIconContent: @Composable () -> Unit = { DefaultFeedbackIconPlaceholder() },
    warningIconContent: @Composable () -> Unit = { DefaultFeedbackIconPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(colors.backgroundDefault),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.semantics {
                            contentDescription = closeContentDescription
                        }
                    ) {
                        CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                            closeIconContent()
                        }
                    }

                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 54.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showForumCard) {
                        FeedbackActionCard(
                            title = feedbackForumTitle,
                            summary = feedbackForumSummary,
                            iconContent = forumIconContent,
                            onClick = onFeedbackForum
                        )
                    }

                    if (showDocCard) {
                        FeedbackActionCard(
                            title = readDocTitle,
                            summary = readDocSummary,
                            iconContent = docIconContent,
                            onClick = onReadDoc
                        )
                    }

                    FeedbackActionCard(
                        title = emailSupportTitle,
                        summary = emailSupportSummary,
                        iconContent = emailIconContent,
                        onClick = onEmailSupport,
                        bodyContent = {
                            if (showEmailWarning) {
                                EmailWarningPanel(
                                    title = emailWarningTitle,
                                    explanation = emailWarningExplanation,
                                    tryAnywayText = tryAnywayText,
                                    openSettingsText = openSettingsText,
                                    warningIconContent = warningIconContent,
                                    onTryAnyway = onTryAnyway,
                                    onOpenSettings = onOpenSettings
                                )
                            }

                            if (showEmailSupportForm) {
                                FeedbackForm(
                                    feedbackTypeLabel = feedbackTypeLabel,
                                    feedbackTypeEntries = feedbackTypeEntries,
                                    selectedFeedbackTypeIndex = selectedFeedbackTypeIndex,
                                    subject = subject,
                                    subjectLabel = subjectLabel,
                                    message = message,
                                    messageLabel = messageLabel,
                                    showIncludes = showIncludes,
                                    includeMedialibrary = includeMedialibrary,
                                    includeMedialibraryText = includeMedialibraryText,
                                    medialibraryWarning = medialibraryWarning,
                                    includeLogs = includeLogs,
                                    includeLogsText = includeLogsText,
                                    logsWarning = logsWarning,
                                    sendText = sendText,
                                    sendEnabled = sendEnabled,
                                    onFeedbackTypeSelected = onFeedbackTypeSelected,
                                    onSubjectChange = onSubjectChange,
                                    onMessageChange = onMessageChange,
                                    onIncludeMedialibraryChange = onIncludeMedialibraryChange,
                                    onIncludeLogsChange = onIncludeLogsChange,
                                    onSend = onSend
                                )
                            }
                        }
                    )

                    if (showRateCard) {
                        FeedbackActionCard(
                            title = rateTitle,
                            summary = rateSummary,
                            iconContent = rateIconContent,
                            onClick = onRate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackActionCard(
    title: String,
    summary: String,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bodyContent: @Composable (() -> Unit)? = null
) {
    val colors = VLCThemeDefaults.colors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .focusable(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = colors.fontDefault
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                        iconContent()
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = summary,
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            bodyContent?.invoke()
        }
    }
}

@Composable
private fun EmailWarningPanel(
    title: String,
    explanation: String,
    tryAnywayText: String,
    openSettingsText: String,
    warningIconContent: @Composable () -> Unit,
    onTryAnyway: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = VLCThemeDefaults.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        DividerLine()

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.primary) {
                    warningIconContent()
                }
            }

            Text(
                text = title,
                color = colors.primary,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = explanation,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onTryAnyway) {
                Text(tryAnywayText, color = colors.fontLight)
            }
            TextButton(onClick = onOpenSettings) {
                Text(openSettingsText)
            }
        }
    }
}

@Composable
private fun FeedbackForm(
    feedbackTypeLabel: String,
    feedbackTypeEntries: List<String>,
    selectedFeedbackTypeIndex: Int,
    subject: String,
    subjectLabel: String,
    message: String,
    messageLabel: String,
    showIncludes: Boolean,
    includeMedialibrary: Boolean,
    includeMedialibraryText: String,
    medialibraryWarning: String,
    includeLogs: Boolean,
    includeLogsText: String,
    logsWarning: String,
    sendText: String,
    sendEnabled: Boolean,
    onFeedbackTypeSelected: (Int) -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onIncludeMedialibraryChange: (Boolean) -> Unit,
    onIncludeLogsChange: (Boolean) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DividerLine()

        FeedbackTypeSelector(
            label = feedbackTypeLabel,
            entries = feedbackTypeEntries,
            selectedIndex = selectedFeedbackTypeIndex,
            onSelected = onFeedbackTypeSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = subject,
            onValueChange = onSubjectChange,
            label = { Text(subjectLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text(messageLabel) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        if (showIncludes) {
            LabeledCheckbox(
                checked = includeLogs,
                text = includeLogsText,
                onCheckedChange = onIncludeLogsChange
            )
            if (includeLogs) WarningBox(logsWarning)

            LabeledCheckbox(
                checked = includeMedialibrary,
                text = includeMedialibraryText,
                onCheckedChange = onIncludeMedialibraryChange
            )
            if (includeMedialibrary) WarningBox(medialibraryWarning)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onSend,
                enabled = sendEnabled
            ) {
                Text(sendText)
            }
        }
    }
}

@Composable
private fun FeedbackTypeSelector(
    label: String,
    entries: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    var expanded by remember { mutableStateOf(false) }
    val selected = entries.getOrNull(selectedIndex).orEmpty()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { expanded = true }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                color = colors.fontLight,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = selected,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entries.forEachIndexed { index, entry ->
                DropdownMenuItem(
                    text = { Text(entry) },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    }
                )
            }
        }
    }
}

@Composable
private fun LabeledCheckbox(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun WarningBox(text: String) {
    val colors = VLCThemeDefaults.colors

    Text(
        text = text,
        color = colors.fontDefault,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.primaryFocus)
            .padding(8.dp)
    )
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(VLCThemeDefaults.colors.defaultDivider)
    )
}

@Composable
private fun DefaultFeedbackIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}

@Preview(name = "Feedback Light", showBackground = true)
@Composable
private fun VLCFeedbackScreenPreview() {
    VLCFeedbackScreen(
        title = "Send feedback",
        feedbackForumTitle = "Feedback forum",
        feedbackForumSummary = "Send feedback / Get some help",
        readDocTitle = "Read documentation",
        readDocSummary = "Get some help",
        emailSupportTitle = "Contact the support team by email",
        emailSupportSummary = "Send feedback / Get some help / Report a bug",
        rateTitle = "Rate us",
        rateSummary = "Google Play",
        feedbackTypeLabel = "Feedback type",
        feedbackTypeEntries = listOf("Get some help", "Feedback / feature request", "Report a bug", "Crash"),
        selectedFeedbackTypeIndex = 2,
        subject = "Playback issue",
        subjectLabel = "Subject",
        message = "Describe the problem here.",
        messageLabel = "Body",
        showForumCard = true,
        showDocCard = true,
        showRateCard = true,
        showEmailSupportForm = true,
        showEmailWarning = false,
        emailWarningTitle = "No email client installed",
        emailWarningExplanation = "Enable remote access or try anyway.",
        tryAnywayText = "Try anyway",
        openSettingsText = "Open settings",
        showIncludes = true,
        includeMedialibrary = true,
        includeMedialibraryText = "Include media library",
        medialibraryWarning = "Your media database will be attached.",
        includeLogs = true,
        includeLogsText = "Include logs",
        logsWarning = "Logs may contain private information.",
        sendText = "Send",
        sendEnabled = true,
        closeContentDescription = "Close",
        onClose = {},
        onFeedbackForum = {},
        onReadDoc = {},
        onEmailSupport = {},
        onRate = {},
        onTryAnyway = {},
        onOpenSettings = {},
        onFeedbackTypeSelected = {},
        onSubjectChange = {},
        onMessageChange = {},
        onIncludeMedialibraryChange = {},
        onIncludeLogsChange = {},
        onSend = {}
    )
}

@Preview(name = "Feedback Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCFeedbackScreenDarkPreview() {
    VLCFeedbackScreenPreview()
}
