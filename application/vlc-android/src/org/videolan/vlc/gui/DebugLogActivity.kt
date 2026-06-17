/*****************************************************************************
 * DebugLogActivity.java
 *
 * Copyright © 2013-2015 VLC authors and VideoLAN
 * Copyright © 2013 Edward Wang
 *
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
 */
package org.videolan.vlc.gui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.google.android.material.snackbar.Snackbar
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.resources.AndroidDevices
import org.videolan.vlc.DebugLogService
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCDebugLogLine
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.share
import java.io.File

class DebugLogActivity : ComponentActivity(), DebugLogService.Client.Callback {
    private lateinit var client: DebugLogService.Client
    private lateinit var snackbarAnchor: View

    private val logLines = mutableStateListOf<String>()
    private var startEnabled by mutableStateOf(false)
    private var stopEnabled by mutableStateOf(false)
    private var optionsEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = DebugLogService.Client(this, this)
        snackbarAnchor = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DebugLogScreen(
                    logLines = logLines,
                    startEnabled = startEnabled,
                    stopEnabled = stopEnabled,
                    optionsEnabled = optionsEnabled,
                    onStartClick = ::startLogging,
                    onStopClick = ::stopLogging,
                    onCopyClick = ::copyLogToClipboard,
                    onSaveClick = ::saveLogToFile,
                    onClearClick = ::clearLog,
                    onInteropLabClick = {
                        startActivity(Intent(this@DebugLogActivity, ComposeInteropLabActivity::class.java))
                    }
                )
            }
        }
        setContentView(snackbarAnchor)
    }

    override fun onDestroy() {
        client.release()
        super.onDestroy()
    }

    private fun startLogging() {
        startEnabled = false
        stopEnabled = false
        client.start()
    }

    private fun stopLogging() {
        startEnabled = false
        stopEnabled = false
        client.stop()
    }

    private fun clearLog() {
        if (::client.isInitialized) client.clear()
        logLines.clear()
        optionsEnabled = false
    }

    private fun saveLogToFile() {
        if (AndroidUtil.isOOrLater && !Permissions.canWriteStorage())
            Permissions.askWriteStoragePermission(this@DebugLogActivity, false, Runnable { client.save() })
        else
            client.save()
    }

    private fun copyLogToClipboard() {
        val text = buildString {
            logLines.forEach { line -> append(line).append('\n') }
        }

        val clipboard = applicationContext.getSystemService<ClipboardManager>()!!
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text))

        UiTools.snacker(this, R.string.copied_to_clipboard)
    }

    override fun onStarted(logList: List<String>) {
        runOnUiThread {
            startEnabled = false
            stopEnabled = true
            optionsEnabled = logList.isNotEmpty()
            logLines.clear()
            logLines.addAll(logList)
        }
    }

    override fun onStopped() {
        runOnUiThread {
            startEnabled = true
            stopEnabled = false
        }
    }

    override fun onLog(msg: String) {
        runOnUiThread {
            logLines.add(msg)
            optionsEnabled = true
        }
    }

    override fun onSaved(success: Boolean, path: String) {
        runOnUiThread {
            if (success) {
                if (AndroidDevices.isAndroidTv) {
                    Snackbar.make(
                        snackbarAnchor,
                        String.format(getString(R.string.dump_logcat_success), path),
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    UiTools.snackerConfirm(this, String.format(getString(R.string.dump_logcat_success), path), false, R.string.share) {
                        share(File(path))
                    }
                }
            } else {
                UiTools.snacker(this, R.string.dump_logcat_failure)
            }
        }
    }

    companion object {
        const val TAG = "VLC/DebugLogActivity"
    }
}

@Composable
private fun DebugLogScreen(
    logLines: List<String>,
    startEnabled: Boolean,
    stopEnabled: Boolean,
    optionsEnabled: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onCopyClick: () -> Unit,
    onSaveClick: () -> Unit,
    onClearClick: () -> Unit,
    onInteropLabClick: () -> Unit
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val listState = rememberLazyListState()
        val isLogging = stopEnabled

        LaunchedEffect(logLines.size) {
            if (logLines.isNotEmpty()) listState.scrollToItem(logLines.lastIndex)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundDefault)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with a live recording indicator.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.debug_logs),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLogging) RecordingIndicator()
            }

            // Primary capture toggle: morphs between start (accent) and stop (error).
            Button(
                onClick = if (isLogging) onStopClick else onStartClick,
                enabled = startEnabled || stopEnabled,
                shape = MaterialTheme.shapes.large,
                colors = if (isLogging) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Icon(
                    painter = painterResource(if (isLogging) R.drawable.ic_pause_player else R.drawable.ic_play),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(if (isLogging) R.string.stop_logging else R.string.start_logging),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Secondary actions on the captured log.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebugLogAction(R.string.copy_to_clipboard, R.drawable.ic_copy, optionsEnabled, onCopyClick)
                DebugLogAction(R.string.dump_logcat, R.drawable.ic_download, optionsEnabled, onSaveClick)
                DebugLogAction(R.string.clear_log, R.drawable.ic_delete, optionsEnabled, onClearClick, destructive = true)
            }

            // Console output.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = colors.fontDefault
            ) {
                if (logLines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.start_logging),
                            color = colors.fontLight,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        items(logLines) { line ->
                            VLCDebugLogLine(text = line)
                        }
                    }
                }
            }

            TextButton(
                onClick = onInteropLabClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Open Compose Interop Lab",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A softly pulsing red dot + label shown while a capture session is active. */
@Composable
private fun RecordingIndicator() {
    val transition = rememberInfiniteTransition(label = "recording")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recordingPulse"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.log_service_title),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun RowScope.DebugLogAction(
    @StringRes textRes: Int,
    iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        colors = if (destructive) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        modifier = Modifier.weight(1f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
