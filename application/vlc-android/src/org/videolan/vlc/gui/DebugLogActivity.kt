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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
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

        LaunchedEffect(logLines.size) {
            if (logLines.isNotEmpty()) listState.scrollToItem(logLines.lastIndex)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundDefault)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebugLogButton(R.string.start_logging, startEnabled, onStartClick)
                DebugLogButton(R.string.stop_logging, stopEnabled, onStopClick)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebugLogButton(R.string.copy_to_clipboard, optionsEnabled, onCopyClick)
                DebugLogButton(R.string.dump_logcat, optionsEnabled, onSaveClick)
            }
            Button(
                onClick = onClearClick,
                enabled = optionsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.clear_log),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onInteropLabClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Open Compose Interop Lab",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(color = colors.defaultDivider)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(logLines) { line ->
                    VLCDebugLogLine(text = line)
                }
            }
        }
    }
}

@Composable
private fun RowScope.DebugLogButton(
    @StringRes textRes: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
