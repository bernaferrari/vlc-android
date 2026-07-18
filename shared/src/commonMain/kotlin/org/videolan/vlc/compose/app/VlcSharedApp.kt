package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.viewmodel.LibraryTab
import org.videolan.vlc.viewmodel.LibraryViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel

enum class SharedDest {
    LIBRARY, PLAYER, SETTINGS
}

/**
 * Full multiplatform app shell shared by Android (lab / optional main) and iOS.
 * Backed by [LibraryViewModel], [PlayerViewModel], [SettingsViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VlcSharedApp(
    modifier: Modifier = Modifier,
    libraryVm: LibraryViewModel = remember { LibraryViewModel() },
    playerVm: PlayerViewModel = remember { PlayerViewModel() },
    settingsVm: SettingsViewModel = remember { SettingsViewModel() },
    title: String = "VLC",
) {
    DisposableEffect(libraryVm, playerVm, settingsVm) {
        onDispose {
            libraryVm.onCleared()
            playerVm.onCleared()
            settingsVm.onCleared()
        }
    }

    VLCTheme {
        val colors = VLCThemeDefaults.colors
        var dest by remember { mutableStateOf(SharedDest.LIBRARY) }
        val playerState by playerVm.state.collectAsState()

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = colors.backgroundDefault,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (dest) {
                                SharedDest.LIBRARY -> title
                                SharedDest.PLAYER -> "Now Playing"
                                SharedDest.SETTINGS -> "Settings"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                )
            },
            bottomBar = {
                Column {
                    if (playerState.hasMedia && dest != SharedDest.PLAYER) {
                        MiniPlayerBar(
                            title = playerState.title.ifBlank { "Not playing" },
                            subtitle = playerState.subtitle,
                            playing = playerState.playing,
                            progress = playerState.progress.progressPercent,
                            onExpand = { dest = SharedDest.PLAYER },
                            onToggle = playerVm::togglePlayPause,
                            onNext = playerVm::next,
                        )
                    }
                    NavigationBar {
                        NavigationBarItem(
                            selected = dest == SharedDest.LIBRARY,
                            onClick = { dest = SharedDest.LIBRARY },
                            icon = { Text("Lib") },
                            label = { Text("Library") },
                        )
                        NavigationBarItem(
                            selected = dest == SharedDest.PLAYER,
                            onClick = { dest = SharedDest.PLAYER },
                            icon = { Text("Play") },
                            label = { Text("Player") },
                        )
                        NavigationBarItem(
                            selected = dest == SharedDest.SETTINGS,
                            onClick = { dest = SharedDest.SETTINGS },
                            icon = { Text("Set") },
                            label = { Text("Settings") },
                        )
                    }
                }
            }
        ) { padding ->
            val contentModifier = Modifier.padding(padding).fillMaxSize()
            when (dest) {
                SharedDest.LIBRARY -> LibraryPane(
                    modifier = contentModifier,
                    vm = libraryVm,
                    onPlay = { item, list ->
                        playerVm.play(item, list)
                        dest = SharedDest.PLAYER
                    }
                )
                SharedDest.PLAYER -> PlayerPane(modifier = contentModifier, vm = playerVm)
                SharedDest.SETTINGS -> SettingsPane(modifier = contentModifier, vm = settingsVm)
            }
        }
    }
}

@Composable
private fun LibraryPane(
    modifier: Modifier = Modifier,
    vm: LibraryViewModel,
    onPlay: (MediaItem, List<MediaItem>) -> Unit,
) {
    val state by vm.state.collectAsState()
    val colors = VLCThemeDefaults.colors
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            singleLine = true,
            label = { Text("Search") },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LibraryTab.entries.forEach { tab ->
                FilterChip(
                    selected = state.tab == tab,
                    onClick = { vm.selectTab(tab) },
                    label = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }
        Text(
            "${state.count} items",
            style = MaterialTheme.typography.labelMedium,
            color = colors.fontLight,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.items, key = { "${it.id}:${it.uri}" }) { item ->
                MediaRow(
                    item = item,
                    onClick = { onPlay(item, state.items) },
                )
            }
        }
    }
}

@Composable
private fun MediaRow(item: MediaItem, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    item.isVideo -> "VID"
                    item.isAudio -> "AUD"
                    else -> "•"
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.listTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val sub = listOfNotNull(item.artist, item.album).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.listSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PlayerPane(
    modifier: Modifier = Modifier,
    vm: PlayerViewModel,
) {
    val state by vm.state.collectAsState()
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text("♪", style = MaterialTheme.typography.displayLarge, color = colors.primary)
        }
        Text(
            state.title.ifBlank { "Nothing playing" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.subtitle.isNotBlank()) {
            Text(state.subtitle, color = colors.fontLight, style = MaterialTheme.typography.bodyMedium)
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        val length = state.progress.length.coerceAtLeast(1L)
        Slider(
            value = state.progress.time.toFloat().coerceIn(0f, length.toFloat()),
            onValueChange = { vm.seekTo(it.toLong()) },
            valueRange = 0f..length.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.progress.time), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(state.progress.length), style = MaterialTheme.typography.labelMedium)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = vm::toggleShuffle) {
                Text(if (state.shuffle) "Shuffle*" else "Shuffle")
            }
            TextButton(onClick = vm::previous) { Text("Prev") }
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.primary,
                modifier = Modifier
                    .size(72.dp)
                    .clickable(onClick = vm::togglePlayPause),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        if (state.playing) "Pause" else "Play",
                        color = colors.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            TextButton(onClick = vm::next) { Text("Next") }
            TextButton(onClick = vm::cycleRepeat) {
                Text(
                    when (state.repeatMode) {
                        RepeatMode.NONE -> "Rep"
                        RepeatMode.ALL -> "RepA"
                        RepeatMode.ONE -> "Rep1"
                    }
                )
            }
        }
        TextButton(onClick = vm::stop) { Text("Stop") }
    }
}

@Composable
private fun SettingsPane(
    modifier: Modifier = Modifier,
    vm: SettingsViewModel,
) {
    val state by vm.state.collectAsState()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        item {
            Text(
                "Playback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item { SettingsToggle("Resume audio", state.audioResume, vm::setAudioResume) }
        item { SettingsToggle("Resume video", state.videoResume, vm::setVideoResume) }
        item { SettingsToggle("Playback history", state.playbackHistory, vm::setPlaybackHistory) }
        item { SettingsToggle("Incognito", state.incognito, vm::setIncognito) }
        item {
            Text(
                "Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        item { SettingsToggle("Video thumbnails", state.showVideoThumbs, vm::setShowVideoThumbs) }
        item {
            Text(
                "Network",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        item { SettingsToggle("Remote access server", state.remoteAccess, vm::setRemoteAccess) }
    }
}

@Composable
private fun SettingsToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun MiniPlayerBar(
    title: String,
    subtitle: String,
    playing: Boolean,
    progress: Float,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.audioHeaderBackground)
            .clickable(onClick = onExpand)
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(2.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.fontLight
                    )
                }
            }
            TextButton(onClick = onToggle) { Text(if (playing) "Pause" else "Play") }
            TextButton(onClick = onNext) { Text("Next") }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
