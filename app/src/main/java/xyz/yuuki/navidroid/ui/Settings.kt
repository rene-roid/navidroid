package xyz.yuuki.navidroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import coil3.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.yuuki.navidroid.Api
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.normalizeEndpoint
import xyz.yuuki.navidroid.song

private val fieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White, unfocusedBorderColor = Faint, cursorColor = Green, focusedLabelColor = Color.White,
)

@Composable
fun LoginScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var servers by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(64.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Bg))
        }
        Text("Log in to Navidrome", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Add every address your server answers on, one per line. The app uses whichever responds fastest and switches if one goes down.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            servers, { servers = it }, Modifier.fillMaxWidth(), label = { Text("Server addresses") },
            placeholder = { Text("music.example.com\n192.168.1.10:4533") }, minLines = 2, colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        )
        OutlinedTextField(
            user, { user = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true, colors = fieldColors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        )
        OutlinedTextField(
            pass, { pass = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, colors = fieldColors,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        error?.let { Text(it, color = Color(0xFFF15E6C), style = MaterialTheme.typography.bodyMedium) }
        Button(
            enabled = !busy && servers.isNotBlank() && user.isNotBlank() && pass.isNotEmpty(),
            onClick = {
                busy = true
                error = null
                scope.launch {
                    val eps = servers.lines().filter { it.isNotBlank() }.map(::normalizeEndpoint).distinct()
                    // Credentials are checked on the first endpoint that accepts them; only the salted token is kept.
                    var last: Exception? = null
                    val ok = withContext(Dispatchers.IO) {
                        eps.firstNotNullOfOrNull { ep ->
                            try { ep to Api.login(ep, user.trim(), pass) } catch (e: Exception) { last = e; null }
                        }
                    }
                    if (ok == null) {
                        error = (last as? xyz.yuuki.navidroid.ApiException)?.message ?: "Couldn't reach any of those addresses"
                        busy = false
                        return@launch
                    }
                    val prefs = App.it.prefs
                    prefs.endpoints = eps
                    prefs.username = user.trim()
                    prefs.token = ok.second.first
                    prefs.salt = ok.second.second
                    prefs.active = ok.first
                    App.it.api = Api(prefs, App.it.scope)
                    App.it.scope.launch { App.it.api.pickFastest() }
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp) else Text("Log in", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Group(title: String) = Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 8.dp))

@Composable
private fun SettingRow(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) = Row(
    Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
    trailing()
}

private val qualities = listOf(0 to "Original", 320 to "320 kbps", 256 to "256 kbps", 192 to "192 kbps", 128 to "128 kbps", 96 to "96 kbps")

@Composable
private fun QualityRow(title: String, value: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(title, qualities.firstOrNull { it.first == value }?.second ?: "$value kbps", { open = true })
        DropdownMenu(open, { open = false }, containerColor = Elevated) {
            qualities.forEach { (v, label) ->
                DropdownMenuItem({ Text(label) }, { onChange(v); open = false }, trailingIcon = { if (v == value) Icon(Icons.Rounded.Check, null, tint = Green) })
            }
        }
    }
}

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val app = App.it
    val prefs = app.prefs
    val scope = rememberCoroutineScope()
    var endpoints by remember { mutableStateOf(prefs.endpoints) }
    var newEndpoint by remember { mutableStateOf("") }
    val latency by app.api.latency.collectAsStateWithLifecycle()
    var active by remember { mutableStateOf(app.api.active) }
    var checking by remember { mutableStateOf(false) }
    var wifi by remember { mutableIntStateOf(prefs.streamBitrateWifi) }
    var mobile by remember { mutableIntStateOf(prefs.streamBitrateMobile) }
    var dl by remember { mutableIntStateOf(prefs.downloadBitrate) }
    var wifiOnly by remember { mutableStateOf(prefs.wifiOnlyDownloads) }

    fun recheck() {
        checking = true
        scope.launch {
            app.api.pickFastest()
            active = app.api.active
            checking = false
        }
    }
    LaunchedEffect(Unit) { recheck() }

    LazyColumn(Modifier.fillMaxSize()) {
        item { BackTitle("Settings") }
        item {
            SettingRow(displayName(), "Logged in as ${prefs.username}") {
                OutlinedButton(onLogout) { Text("Log out", color = Color.White) }
            }
        }
        item {
            Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Group("Servers") }
                if (checking) CircularProgressIndicator(Modifier.padding(top = 20.dp).size(20.dp), color = Green, strokeWidth = 2.dp)
                else IconButton(::recheck, Modifier.padding(top = 20.dp)) { Icon(Icons.Rounded.Refresh, "Test all") }
            }
        }
        items(endpoints, key = { it }) { ep ->
            val ms = latency[ep]
            SettingRow(
                ep,
                when {
                    ep !in latency -> "Testing…"
                    ms == null -> "Unreachable"
                    ep == active -> "In use • $ms ms"
                    else -> "$ms ms"
                },
            ) {
                if (ep == active) Icon(Icons.Rounded.Check, "In use", tint = Green)
                if (endpoints.size > 1) IconButton({
                    endpoints = endpoints - ep
                    prefs.endpoints = endpoints
                    recheck()
                }) { Icon(Icons.Rounded.Close, "Remove", tint = Sub) }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    newEndpoint, { newEndpoint = it }, Modifier.weight(1f), singleLine = true, colors = fieldColors,
                    placeholder = { Text("Add another address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                IconButton({
                    if (newEndpoint.isNotBlank()) {
                        endpoints = (endpoints + normalizeEndpoint(newEndpoint)).distinct()
                        prefs.endpoints = endpoints
                        newEndpoint = ""
                        recheck()
                    }
                }) { Icon(Icons.Rounded.Add, "Add") }
            }
        }

        item { Group("Streaming quality") }
        item { QualityRow("On Wi-Fi", wifi) { wifi = it; prefs.streamBitrateWifi = it } }
        item { QualityRow("On mobile data", mobile) { mobile = it; prefs.streamBitrateMobile = it } }

        item { Group("Downloads") }
        item { QualityRow("Download quality", dl) { dl = it; prefs.downloadBitrate = it } }
        item {
            SettingRow("Download on Wi-Fi only", "Pinned playlists wait for Wi-Fi") {
                Switch(wifiOnly, { wifiOnly = it; app.offline.setWifiOnly(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Green))
            }
        }
        item { SettingRow("Manage downloads", null, { nav.go(Route.Downloads) }) }
        item {
            SettingRow("Clear image cache", "Covers download again as you browse", {
                ctx.imageLoader.memoryCache?.clear()
                ctx.imageLoader.diskCache?.clear()
            })
        }
        item { Spacer(Modifier.height(BottomSpace)) }
    }
}

@Composable
fun DownloadsScreen() {
    val nav = LocalNav.current
    val offline = App.it.offline
    val pins by offline.pins.collectAsStateWithLifecycle()
    val states by offline.states.collectAsStateWithLifecycle()
    val syncing by offline.syncing.collectAsStateWithLifecycle()
    var paused by remember { mutableStateOf(offline.manager.downloadsPaused) }
    var confirmClear by remember { mutableStateOf(false) }
    var used by remember { mutableLongStateOf(0L) }
    // Media3 doesn't push progress, so poll it while this screen is open.
    val active by produceState(emptyList<Download>()) {
        while (true) {
            value = offline.manager.currentDownloads.filter { it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED }
            used = withContext(Dispatchers.IO) { offline.bytesUsed() }
            delay(1000)
        }
    }
    val done = states.count { it.value == Download.STATE_COMPLETED }
    val failed = states.count { it.value == Download.STATE_FAILED }
    val tiles = remember(pins, states.size) { pinnedTiles(pins) }

    LazyColumn(Modifier.fillMaxSize()) {
        item { BackTitle("Downloads") }
        item {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).padding(16.dp)) {
                Text("$done songs • ${formatBytes(used)}", style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        append(if (active.isEmpty()) "Everything is up to date" else "${active.size} left to download")
                        if (failed > 0) append(" • $failed failed, retried on next sync")
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        { offline.sync() }, enabled = !syncing,
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                    ) {
                        if (syncing) CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp) else Icon(Icons.Rounded.Sync, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (syncing) "Syncing" else "Sync now")
                    }
                    OutlinedButton({
                        if (paused) offline.resume() else offline.pause()
                        paused = !paused
                    }) {
                        Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(if (paused) "Resume" else "Pause", color = Color.White)
                    }
                }
            }
        }
        item {
            Text("Kept on this phone", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp))
            Text(
                "Pinned playlists re-sync when you open the app: new songs download, removed ones are deleted.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (tiles.isEmpty()) item {
            Text("Nothing yet. Tap the download button on any playlist or album.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
        }
        items(tiles, key = { "${it.kind}:${it.id}" }) { t ->
            val c = offline.snapshot(t.kind, t.id)
            val total = c?.songs?.size ?: 0
            val have = c?.songs?.count { states[it.id] == Download.STATE_COMPLETED } ?: 0
            Row(Modifier.fillMaxWidth().clickable { nav.open(t) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(56.dp), 120)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$have of $total downloaded", style = MaterialTheme.typography.bodySmall)
                    if (have < total) LinearProgressIndicator(
                        progress = { have / total.toFloat() }, Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp),
                        color = Green, trackColor = Color.White.copy(alpha = .1f),
                    )
                }
                IconButton({ offline.unpin(t.kind, t.id) }) { Icon(Icons.Rounded.Close, "Remove download", tint = Sub) }
            }
        }
        if (active.isNotEmpty()) {
            item { Text("In progress", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)) }
            items(active.take(50), key = { "dl" + it.request.id }) { d ->
                val s = runCatching { xyz.yuuki.navidroid.Song.from(org.json.JSONObject(String(d.request.data))) }.getOrNull()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(s?.coverArt, 120, Modifier.size(40.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(s?.title ?: d.request.id, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        LinearProgressIndicator(
                            progress = { (d.percentDownloaded / 100f).coerceIn(0f, 1f) }, Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp),
                            color = Green, trackColor = Color.White.copy(alpha = .1f),
                        )
                    }
                }
            }
        }
        if (tiles.isNotEmpty()) item {
            HorizontalDivider(Modifier.padding(top = 24.dp), color = Elevated)
            TextButton({ confirmClear = true }, Modifier.padding(8.dp)) { Text("Remove all downloads", color = Color(0xFFF15E6C)) }
        }
        item { Spacer(Modifier.height(BottomSpace)) }
    }
    if (confirmClear) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmClear = false },
        containerColor = Elevated,
        title = { Text("Remove all downloads?") },
        text = { Text("Everything you downloaded is deleted from this phone. Your playlists stay on the server.") },
        confirmButton = { TextButton({ offline.unpinAll(); confirmClear = false }) { Text("Remove", color = Color(0xFFF15E6C)) } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel", color = Color.White) } },
    )
}
