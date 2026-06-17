/*
 * ************************************************************************
 *  RemoteAccessShareActivity.kt
 * *************************************************************************
 * Copyright © 2025 VLC authors and VideoLAN
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

package org.videolan.vlc.remoteaccessserver.gui.remoteaccess

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.resources.ACTION_START_SERVER
import org.videolan.resources.ACTION_STOP_SERVER
import org.videolan.resources.AndroidDevices
import org.videolan.resources.REMOTE_ACCESS_ONBOARDING
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.tools.copy
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.remoteaccessserver.R
import org.videolan.vlc.remoteaccessserver.RemoteAccessServer
import org.videolan.vlc.remoteaccessserver.ServerStatus
import org.videolan.vlc.util.UrlUtils
import org.videolan.vlc.util.share

class RemoteAccessShareActivity : BaseActivity() {

    override val displayTitle = true

    private var rootView: ComposeView? = null
    private var serverStatus by mutableStateOf(ServerStatus.NOT_INIT)
    private var serverLinks by mutableStateOf(emptyList<String>())
    private var serverConnections by mutableStateOf(emptyList<RemoteAccessServer.RemoteAccessConnection>())
    private var snackbarMessage by mutableStateOf<String?>(null)
    private var qrDialogLink by mutableStateOf<String?>(null)

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = rootView ?: window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val remoteAccessServer = RemoteAccessServer.getInstance(applicationContext)
        rootView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    RemoteAccessShareScreen(
                        serverStatus = serverStatus,
                        links = serverLinks,
                        connections = serverConnections,
                        snackbarMessage = snackbarMessage,
                        qrDialogLink = qrDialogLink,
                        onSnackbarShown = { snackbarMessage = null },
                        onClose = ::finish,
                        onShowOnboarding = ::showOnboarding,
                        onToggleServer = { toggleServer(remoteAccessServer) },
                        onCopyLink = ::copyRemoteAccessLink,
                        onShareLink = { share(getString(R.string.remote_access), it) },
                        onShowQr = { qrDialogLink = it },
                        onDismissQr = { qrDialogLink = null }
                    )
                }
            }
        }
        setContentView(rootView)
        if (AndroidDevices.isTv) applyOverscanMargin(this)

        remoteAccessServer.serverStatus.observe(this) { status ->
            serverStatus = status
            serverLinks = if (status == ServerStatus.STARTED) remoteAccessServer.getServerAddresses() else emptyList()
        }
        remoteAccessServer.serverConnections.observe(this) { connections ->
            serverConnections = connections
        }
    }

    private fun toggleServer(remoteAccessServer: RemoteAccessServer) {
        val action = if (remoteAccessServer.serverStatus.value == ServerStatus.STARTED) ACTION_STOP_SERVER else ACTION_START_SERVER
        sendBroadcast(Intent(action).apply { `package` = packageName })
    }

    private fun copyRemoteAccessLink(link: String) {
        copy("VLC for Android Remote Access link", link)
        snackbarMessage = getString(R.string.url_copied_to_clipboard)
    }

    private fun showOnboarding() {
        startActivity(Intent(Intent.ACTION_VIEW).apply { setClassName(this@RemoteAccessShareActivity, REMOTE_ACCESS_ONBOARDING) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteAccessShareScreen(
    serverStatus: ServerStatus,
    links: List<String>,
    connections: List<RemoteAccessServer.RemoteAccessConnection>,
    snackbarMessage: String?,
    qrDialogLink: String?,
    onSnackbarShown: () -> Unit,
    onClose: () -> Unit,
    onShowOnboarding: () -> Unit,
    onToggleServer: () -> Unit,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onShowQr: (String) -> Unit,
    onDismissQr: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSnackbarShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.backgroundDefault,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.remote_access),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_up),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShowOnboarding) {
                        Icon(
                            painter = painterResource(R.drawable.ic_information),
                            contentDescription = stringResource(R.string.ra_remote_access)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundDefault,
                    titleContentColor = colors.fontDefault,
                    navigationIconContentColor = colors.fontDefault,
                    actionIconContentColor = colors.fontDefault
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RemoteAccessStatusCard(
                    serverStatus = serverStatus,
                    onToggleServer = onToggleServer
                )
            }
            if (serverStatus == ServerStatus.STARTED) {
                item {
                    SectionTitle(text = stringResource(R.string.remote_access_links))
                }
                items(links, key = { it }) { link ->
                    RemoteAccessLinkRow(
                        link = link,
                        onShowQr = onShowQr,
                        onShareLink = onShareLink,
                        onCopyLink = onCopyLink
                    )
                }
                item {
                    SectionTitle(text = stringResource(R.string.remote_access_connections))
                }
                items(connections, key = { it.ip }) { connection ->
                    RemoteAccessConnectionRow(connection.ip)
                }
            }
        }
    }

    qrDialogLink?.let { link ->
        RemoteAccessQrDialog(
            link = link,
            onDismiss = onDismissQr
        )
    }
}

@Composable
private fun RemoteAccessStatusCard(
    serverStatus: ServerStatus,
    onToggleServer: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val started = serverStatus == ServerStatus.STARTED
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(text = stringResource(R.string.remote_access_status))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor(serverStatus, colors))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(statusText(serverStatus)),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onToggleServer,
                    enabled = started || serverStatus == ServerStatus.STOPPED,
                    shape = MaterialTheme.shapes.large,
                    colors = if (started) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    }
                ) {
                    Text(stringResource(if (started) R.string.stop else R.string.start))
                }
            }
        }
    }
}

private fun statusColor(status: ServerStatus, colors: org.videolan.vlc.compose.theme.VLCColorScheme): Color = when (status) {
    ServerStatus.STARTED -> Color(0xFF4CAF50)
    ServerStatus.ERROR -> colors.error
    ServerStatus.CONNECTING, ServerStatus.STOPPING -> colors.primary
    ServerStatus.STOPPED, ServerStatus.NOT_INIT -> colors.fontLight
}

@Composable
private fun RemoteAccessLinkRow(
    link: String,
    onShowQr: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onCopyLink: (String) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = link,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onShowQr(link) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_qr_code),
                    contentDescription = stringResource(R.string.remote_access)
                )
            }
            IconButton(onClick = { onShareLink(link) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.share)
                )
            }
            IconButton(onClick = { onCopyLink(link) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = stringResource(R.string.url_copied_to_clipboard)
                )
            }
        }
    }
}

@Composable
private fun RemoteAccessConnectionRow(ip: String) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = ip,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = VLCThemeDefaults.colors
    Text(
        text = text,
        color = colors.listTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun RemoteAccessQrDialog(
    link: String,
    onDismiss: () -> Unit
) {
    val qrBitmap: Bitmap = remember(link) { UrlUtils.generateQRCode(link, 512) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.remote_access_notification, link),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

private fun statusText(status: ServerStatus) = when (status) {
    ServerStatus.NOT_INIT -> R.string.remote_access_notification_not_init
    ServerStatus.STARTED -> R.string.remote_access_active
    ServerStatus.STOPPED -> R.string.remote_access_notification_stopped
    ServerStatus.CONNECTING -> R.string.remote_access_notification_connecting
    ServerStatus.ERROR -> R.string.remote_access_notification_error
    ServerStatus.STOPPING -> R.string.remote_access_notification_stopping
}
