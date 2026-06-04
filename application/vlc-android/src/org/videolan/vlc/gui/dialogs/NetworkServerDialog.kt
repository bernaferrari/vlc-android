package org.videolan.vlc.gui.dialogs

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.AndroidDevices
import org.videolan.tools.AppScope
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.DialogActivity
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.util.isTalkbackIsEnabled
import java.util.Locale

object NetworkServerDialog {
    const val FTP_DEFAULT_PORT = "21"
    const val FTPS_DEFAULT_PORT = "990"
    const val FTPES_DEFAULT_PORT = "21"
    const val SFTP_DEFAULT_PORT = "22"
    const val HTTP_DEFAULT_PORT = "80"
    const val HTTPS_DEFAULT_PORT = "443"
}

private var isNetworkServerComposeDialogShowing = false

fun ComponentActivity.showNetworkServerComposeDialog(
    server: MediaWrapper? = null,
    finishOnDismiss: Boolean = false
) {
    if (isNetworkServerComposeDialogShowing) return
    isNetworkServerComposeDialogShowing = true
    lifecycleScope.launch {
        if (showPinIfNeeded()) {
            isNetworkServerComposeDialogShowing = false
            finishNetworkServerDialogActivityIfNeeded(finishOnDismiss)
            return@launch
        }
        NetworkServerComposeDialog(
            activity = this@showNetworkServerComposeDialog,
            server = server,
            finishOnDismiss = finishOnDismiss,
            onDismissed = { isNetworkServerComposeDialogShowing = false }
        ).show()
    }
}

private class NetworkServerComposeDialog(
    private val activity: ComponentActivity,
    server: MediaWrapper?,
    private val finishOnDismiss: Boolean,
    private val onDismissed: () -> Unit
) {
    private val browserFavRepository = BrowserFavRepository.getInstance(activity)
    private val protocols = activity.resources.getStringArray(R.array.server_protocols).toList()
    private val editingUri: Uri? = server?.uri
    private val editingName = server?.title.orEmpty()
    private val initialProtocol = editingUri?.scheme
        ?.uppercase(Locale.getDefault())
        ?.takeIf { protocols.contains(it) }
        ?: protocols.firstOrNull()
        ?: "FTP"
    private val selectedProtocolState = mutableStateOf(initialProtocol)
    private val addressState = mutableStateOf(editingUri?.host.orEmpty())
    private val usernameState = mutableStateOf(editingUri?.userInfo.orEmpty())
    private val folderState = mutableStateOf(editingUri?.path.orEmpty())
    private val serverNameState = mutableStateOf(editingName)
    private val portState = mutableStateOf(
        editingUri?.port?.takeIf { it != -1 }?.toString() ?: getPortForProtocol(initialProtocol)
    )
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private var rootView: ComposeView? = null

    fun show() {
        setupContent()
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    NetworkServerContent(
                        protocols = protocols,
                        selectedProtocol = selectedProtocolState.value,
                        address = addressState.value,
                        port = portState.value,
                        username = usernameState.value,
                        folder = folderState.value,
                        serverName = serverNameState.value,
                        onProtocolSelected = ::setProtocol,
                        onAddressChanged = { addressState.value = it },
                        onPortChanged = { portState.value = it },
                        onUsernameChanged = ::setUsername,
                        onFolderChanged = { folderState.value = it },
                        onServerNameChanged = { serverNameState.value = it },
                        onCancel = dialog::dismiss,
                        onSave = ::saveServer
                    )
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            (activity as? MainActivity)?.forceRefresh()
            rootView = null
            onDismissed()
            activity.finishNetworkServerDialogActivityIfNeeded(finishOnDismiss)
        }
    }

    private fun setProtocol(protocol: String) {
        if (protocol == selectedProtocolState.value) return
        selectedProtocolState.value = protocol
        portState.value = getPortForProtocol(protocol)
    }

    private fun setUsername(username: String) {
        usernameState.value = username
        if (selectedProtocolState.value == "SFTP") {
            folderState.value = "/home/$username"
        }
    }

    private fun saveServer() {
        val address = addressState.value
        val name = serverNameState.value.ifEmpty { address }
        val uri = buildNetworkUrl(
            protocol = selectedProtocolState.value,
            address = address,
            port = portState.value,
            username = usernameState.value,
            folder = folderState.value
        ).toUri()
        AppScope.launch {
            editingUri?.let { browserFavRepository.deleteBrowserFav(it) }
            browserFavRepository.addNetworkFavItem(uri, name, null)
            dialog.dismiss()
        }
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
private fun NetworkServerContent(
    protocols: List<String>,
    selectedProtocol: String,
    address: String,
    port: String,
    username: String,
    folder: String,
    serverName: String,
    onProtocolSelected: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onFolderChanged: (String) -> Unit,
    onServerNameChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val protocolRules = protocolRules(selectedProtocol)
    val url = buildNetworkUrl(
        protocol = selectedProtocol,
        address = address,
        port = port,
        username = username,
        folder = folder
    )
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.server_add_title),
                color = colors.fontDefault,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
            Text(
                text = url,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ProtocolDropdown(
                    protocols = protocols,
                    selectedProtocol = selectedProtocol,
                    onProtocolSelected = onProtocolSelected,
                    modifier = Modifier.weight(1f)
                )
                if (protocolRules.portEnabled) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = onPortChanged,
                        label = { Text(stringResource(R.string.server_port)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(112.dp)
                    )
                }
            }
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChanged,
                label = { Text(stringResource(protocolRules.addressHint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
            if (protocolRules.userEnabled) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChanged,
                    label = { Text(stringResource(R.string.server_username_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                )
            }
            OutlinedTextField(
                value = folder,
                onValueChange = onFolderChanged,
                label = { Text(stringResource(R.string.server_folder_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
            OutlinedTextField(
                value = serverName,
                onValueChange = onServerNameChanged,
                label = { Text(stringResource(R.string.server_servername_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp)
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = onSave,
                    enabled = address.isNotEmpty(),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun ProtocolDropdown(
    protocols: List<String>,
    selectedProtocol: String,
    onProtocolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(selectedProtocol) { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedProtocol,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            protocols.forEach { protocol ->
                DropdownMenuItem(
                    text = { Text(protocol) },
                    onClick = {
                        expanded = false
                        onProtocolSelected(protocol)
                    }
                )
            }
        }
    }
}

private data class ProtocolRules(
    @StringRes val addressHint: Int,
    val userEnabled: Boolean = true,
    val portEnabled: Boolean = true
)

private fun protocolRules(protocol: String): ProtocolRules {
    return when (protocol) {
        "SMB" -> ProtocolRules(
            addressHint = R.string.server_share_hint,
            userEnabled = false
        )
        "NFS" -> ProtocolRules(
            addressHint = R.string.server_share_hint,
            userEnabled = false,
            portEnabled = false
        )
        else -> ProtocolRules(addressHint = R.string.server_domain_hint)
    }
}

private fun buildNetworkUrl(
    protocol: String,
    address: String,
    port: String,
    username: String,
    folder: String
): String {
    val rules = protocolRules(protocol)
    return buildString {
        append(protocol.lowercase(Locale.getDefault()))
            .append("://")
        if (rules.userEnabled && username.isNotEmpty()) {
            append(username).append('@')
        }
        append(address)
        if (needPort(rules.portEnabled, port)) {
            append(':').append(port)
        }
        if (folder.isNotEmpty()) {
            if (!folder.startsWith("/")) append('/')
            append(folder)
        }
    }
}

private fun needPort(portEnabled: Boolean, port: String): Boolean {
    if (!portEnabled || port.isEmpty()) return false
    return when (port) {
        NetworkServerDialog.FTP_DEFAULT_PORT,
        NetworkServerDialog.SFTP_DEFAULT_PORT,
        NetworkServerDialog.HTTP_DEFAULT_PORT,
        NetworkServerDialog.HTTPS_DEFAULT_PORT -> false
        else -> true
    }
}

private fun getPortForProtocol(protocol: String): String {
    return when (protocol) {
        "FTP" -> NetworkServerDialog.FTP_DEFAULT_PORT
        "FTPS" -> NetworkServerDialog.FTPS_DEFAULT_PORT
        "FTPES" -> NetworkServerDialog.FTPES_DEFAULT_PORT
        "SFTP" -> NetworkServerDialog.SFTP_DEFAULT_PORT
        "HTTP" -> NetworkServerDialog.HTTP_DEFAULT_PORT
        "HTTPS" -> NetworkServerDialog.HTTPS_DEFAULT_PORT
        else -> ""
    }
}

private fun ComponentActivity.finishNetworkServerDialogActivityIfNeeded(finishOnDismiss: Boolean) {
    if (finishOnDismiss && this is DialogActivity && !isFinishing) finish()
}
