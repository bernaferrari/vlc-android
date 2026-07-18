/*
 * RemoteAccessClientActivity.kt — control another VLC instance over the LAN.
 */
package org.videolan.vlc.remoteaccessserver.gui.remoteaccess

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.remoteaccessclient.RemoteAccessClient
import org.videolan.vlc.remoteaccessclient.RemoteAccessClientConfig
import org.videolan.vlc.remoteaccessclient.RemoteAccessClientException
import org.videolan.vlc.remoteaccessserver.websockets.IncomingMessageType
import org.videolan.vlc.R as VR

private const val PREF_LAST_REMOTE_URL = "remote_access_client_last_url"

/**
 * In-app controller for a **remote** VLC remote-access server.
 * Uses [RemoteAccessClient] (OTP → session cookie → library / playback).
 */
class RemoteAccessClientActivity : BaseActivity() {

    override val displayTitle = true
    private var rootView: ComposeView? = null

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = rootView ?: window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings.getInstance(this)
        rootView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    RemoteAccessClientScreen(
                        initialUrl = settings.getString(PREF_LAST_REMOTE_URL, "https://192.168.1.1:8443")
                            ?: "https://192.168.1.1:8443",
                        onSaveUrl = { settings.putSingle(PREF_LAST_REMOTE_URL, it) },
                        onBack = ::finish
                    )
                }
            }
        }
        setContentView(rootView)
    }
}

private data class RemoteVideoRow(val id: Long, val title: String, val artist: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteAccessClientScreen(
    initialUrl: String,
    onSaveUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var url by remember { mutableStateOf(initialUrl) }
    var otp by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Disconnected") }
    var busy by remember { mutableStateOf(false) }
    val videos = remember { mutableStateListOf<RemoteVideoRow>() }
    var client by remember { mutableStateOf<RemoteAccessClient?>(null) }

    DisposableEffect(Unit) {
        onDispose { client?.close() }
    }

    fun runOp(label: String, block: suspend (RemoteAccessClient) -> Unit) {
        scope.launch {
            busy = true
            try {
                val c = client ?: throw RemoteAccessClientException("Connect first")
                withContext(Dispatchers.IO) { block(c) }
            } catch (t: Throwable) {
                status = "$label failed: ${t.message}"
                snackbar.showSnackbar(status)
            } finally {
                busy = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(VR.string.remote_access_client_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(VR.string.close)) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(VR.string.remote_access_client_blurb),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(VR.string.remote_access_client_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                client?.close()
                                val c = RemoteAccessClient(RemoteAccessClientConfig(url.trim()))
                                client = c
                                onSaveUrl(url.trim())
                                val ok = withContext(Dispatchers.IO) { c.ping() }
                                status = if (ok) {
                                    "Reachable — enter OTP from the host device"
                                } else {
                                    "No response"
                                }
                                snackbar.showSnackbar(status)
                            } catch (t: Throwable) {
                                status = t.message ?: "Connect failed"
                                snackbar.showSnackbar(status)
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text(stringResource(VR.string.remote_access_client_connect)) }
                Button(
                    enabled = !busy && client != null,
                    onClick = {
                        runOp("OTP challenge") { c ->
                            c.requestOtpChallenge()
                            status = "Challenge sent — check host notification / screen"
                        }
                    }
                ) { Text(stringResource(VR.string.remote_access_client_request_otp)) }
            }
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text(stringResource(VR.string.remote_access_client_otp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = !busy && client != null && otp.isNotBlank(),
                onClick = {
                    runOp("Verify OTP") { c ->
                        val ok = c.verifyOtp(otp.trim())
                        status = if (ok) "Authenticated" else "OTP rejected"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(VR.string.remote_access_client_verify_otp)) }

            Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))


            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    enabled = !busy && client != null,
                    onClick = { runOp("play") { it.play() } },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(VR.string.play)) }
                Button(
                    enabled = !busy && client != null,
                    onClick = { runOp("pause") { it.pause() } },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(VR.string.pause)) }
                Button(
                    enabled = !busy && client != null,
                    onClick = { runOp("next") { it.next() } },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(VR.string.next)) }
            }
            Button(
                enabled = !busy && client != null,
                onClick = {
                    runOp("video-list") { c ->
                        val json = c.getVideoListJson()
                        val rows = parseVideoList(json)
                        videos.clear()
                        videos.addAll(rows)
                        status = "Loaded ${rows.size} videos"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(VR.string.remote_access_client_load_videos)) }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                items(videos, key = { it.id }) { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runOp("play-media") { c ->
                                    c.sendPlaybackEvent(
                                        IncomingMessageType.PLAY_MEDIA,
                                        id = row.id.toInt()
                                    )
                                    status = "Play requested: ${row.title}"
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(row.title, style = MaterialTheme.typography.titleSmall)
                            row.artist?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseVideoList(json: String): List<RemoteVideoRow> {
    return try {
        val root = JSONObject(json)
        val arr: JSONArray = when {
            root.has("list") -> root.getJSONArray("list")
            root.has("items") -> root.getJSONArray("items")
            else -> JSONArray(json)
        }
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optLong("id", o.optLong("mediaId", i.toLong()))
                val title = o.optString("title", o.optString("name", "Item $id"))
                val artist = o.optString("artist").takeIf { it.isNotBlank() }
                add(RemoteVideoRow(id, title, artist))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
