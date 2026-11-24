package xyz.yuuki.navidroid.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.Jam
import xyz.yuuki.navidroid.JamOrder
import xyz.yuuki.navidroid.JamSong
import xyz.yuuki.navidroid.JamState
import xyz.yuuki.navidroid.SkipVotes
import xyz.yuuki.navidroid.Song
import xyz.yuuki.navidroid.jamBase
import xyz.yuuki.navidroid.list
import java.io.IOException

private fun people(n: Int) = if (n == 1) "1 guest" else "$n guests"

private val accentButton @Composable get() = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent, disabledContainerColor = Tile, disabledContentColor = Faint)

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) = Box(
    Modifier.clip(CircleShape).background(if (on) Accent else Tile).clickable(onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) OnAccent else Color.White) }

// ---- hosting --------------------------------------------------------------------------------

/** On the player: starts a Jam, or while one runs shows its name and how many joined. Opens the Jam sheet. */
@Composable
fun JamPill(modifier: Modifier = Modifier) {
    val session by Jam.session.collectAsStateWithLifecycle()
    val settings by Jam.settings.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    if (open) JamSheet { open = false }
    val live = session
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(CircleShape).background(if (live != null) Accent else Color.White.copy(alpha = .12f)).clickable { open = true }
                .heightIn(min = 40.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tint = if (live != null) OnAccent else Color.White
            Icon(Icons.Rounded.Groups, null, Modifier.size(20.dp), tint = tint)
            Text(
                if (live != null) "${settings.name} · ${people(live.guests.size)}" else "Start a Jam", style = MaterialTheme.typography.labelLarge, color = tint,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The host's side of a Jam: its link while it runs, and its rules, which apply as they change. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamSheet(onDismiss: () -> Unit) {
    val session by Jam.session.collectAsStateWithLifecycle()
    val s by Jam.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(s.name) }
    val live = session
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Elevated) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (live == null) "Start a Jam" else "Your Jam", style = MaterialTheme.typography.titleLarge)
            if (live == null) Text("Friends on your Wi-Fi open a link and pick songs for your queue. They don't need the app.", style = MaterialTheme.typography.bodySmall)
            else {
                Text(live.url, style = MaterialTheme.typography.bodyLarge, color = Accent)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Join my Jam and pick the music: ${live.url}")
                            ctx.startActivity(Intent.createChooser(send, "Share the Jam"))
                        },
                        Modifier.weight(1f).heightIn(min = 48.dp), colors = accentButton,
                    ) {
                        Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                        Text("Share", Modifier.padding(start = 8.dp), color = LocalContentColor.current, style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        {
                            ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Jam link", live.url))
                            Toast.makeText(ctx, "Link copied", Toast.LENGTH_SHORT).show()
                        },
                        Modifier.weight(1f).heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Tile, contentColor = Color.White),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                        Text("Copy", Modifier.padding(start = 8.dp), color = LocalContentColor.current, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(
                    if (live.guests.isEmpty()) "Nobody has joined yet. Guests need to be on the same Wi-Fi as this phone."
                    else "${people(live.guests.size)}: ${live.guests.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedTextField(
                name, { name = it.take(30); Jam.update(s.copy(name = name.trim().ifEmpty { "Jam" })) }, Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors,
                label = { Text("Jam name") }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            )
            Row(Modifier.fillMaxWidth().clickable { Jam.update(s.copy(canAdd = !s.canAdd)) }, verticalAlignment = Alignment.CenterVertically) {
                Text("Guests can add songs", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(s.canAdd, { Jam.update(s.copy(canAdd = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
            }
            if (s.canAdd) {
                Column {
                    Row {
                        Text("Songs each guest can have waiting", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(if (s.maxPerGuest == 0) "No limit" else "${s.maxPerGuest}", style = MaterialTheme.typography.bodyLarge, color = Accent)
                    }
                    // 1 to 10, and one notch further for no limit.
                    Slider(
                        if (s.maxPerGuest == 0) 11f else s.maxPerGuest.toFloat(), { v -> Jam.update(s.copy(maxPerGuest = Math.round(v).let { if (it > 10) 0 else it })) },
                        valueRange = 1f..11f, steps = 9, colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent),
                    )
                }
                Text("Guests' songs play", style = MaterialTheme.typography.bodyLarge)
                Column {
                    JamOrder.entries.forEach { o ->
                        Row(Modifier.fillMaxWidth().clickable { Jam.update(s.copy(order = o)) }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(s.order == o, { Jam.update(s.copy(order = o)) })
                            Text(o.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Text("Guests' votes it takes to skip a song", style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkipVotes.entries.forEach { v -> Chip(v.label, s.skip == v) { Jam.update(s.copy(skip = v)) } }
            }

            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (live == null) Button({ scope.launch { error = Jam.start() } }, Modifier.fillMaxWidth().heightIn(min = 52.dp), colors = accentButton) {
                Text("Start Jam", color = LocalContentColor.current, style = MaterialTheme.typography.titleMedium)
            } else TextButton({ Jam.stop(); onDismiss() }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("End Jam", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---- joining --------------------------------------------------------------------------------

/** Asks for a Jam's link and opens it. */
@Composable
fun JoinJam(onDone: () -> Unit) {
    val nav = LocalNav.current
    var link by remember { mutableStateOf("") }
    val base = jamBase(link)
    fun join() {
        if (base == null) return
        onDone()
        nav.go(Route.Jam(base))
    }
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Elevated,
        title = { Text("Join a Jam") },
        text = {
            OutlinedTextField(
                link, { link = it }, Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors,
                placeholder = { Text("Paste the Jam link") }, isError = link.isNotBlank() && base == null,
                supportingText = { Text(if (link.isNotBlank() && base == null) "That isn't a Jam link" else "The host shares it from their player. You need to be on the same Wi-Fi.") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { join() }),
            )
        },
        confirmButton = { TextButton(::join, enabled = base != null) { Text("Join", color = if (base != null) Accent else Faint) } },
        dismissButton = { TextButton(onDone) { Text("Cancel", color = Sub) } },
    )
}

private class JamError(val code: Int, message: String) : IOException(message)

/** The id each Jam's host gave this phone, so leaving the page and coming back is the same guest. */
private val guestIds = HashMap<String, String>()

private suspend fun jamCall(base: String, path: String, post: Boolean, params: List<Pair<String, String?>>): JSONObject = withContext(Dispatchers.IO) {
    val url = "$base/$path".toHttpUrl().newBuilder().apply { params.forEach { (k, v) -> if (v != null) addQueryParameter(k, v) } }.build()
    val req = Request.Builder().url(url).apply { if (post) post(ByteArray(0).toRequestBody()) }.build()
    App.it.api.http.newCall(req).execute().use { r ->
        val o = runCatching { JSONObject(r.body!!.string()) }.getOrElse { JSONObject() }
        if (!r.isSuccessful) throw JamError(r.code, o.optString("error").ifEmpty { "Something went wrong" })
        o
    }
}

@Composable
private fun JamCover(base: String, id: String?, size: Dp) = AsyncImage(
    model = id?.let { "$base/cover/".toHttpUrl().newBuilder().addPathSegment(it).build().toString() }, contentDescription = null, contentScale = ContentScale.Crop,
    modifier = Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(Elevated),
)

@Composable
private fun JamRow(base: String, song: Song, by: String?, trailing: @Composable () -> Unit = {}) = Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    JamCover(base, song.coverArt, 48.dp)
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
        Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (by != null) "${song.artist} · Added by $by" else song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    trailing()
}

/** Someone else's Jam, from the guest's side: what's playing there, what's next, and a search of the host's library to add from. */
@Composable
fun JamScreen(base: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var id by remember { mutableStateOf(guestIds[base]) }
    var state by remember { mutableStateOf<JamState?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var ended by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(displayName()) }
    var q by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Song>()) }

    suspend fun call(path: String, post: Boolean = false, vararg params: Pair<String, String?>) = jamCall(base, path, post, listOf("g" to id) + params)
    suspend fun refresh() {
        try {
            state = JamState.from(call("state"))
            problem = null
        } catch (e: JamError) {
            if (e.code == 401) { guestIds.remove(base); id = null } else if (e.code == 403) ended = true else problem = e.message
        } catch (_: IOException) {
            problem = "Can't reach the host. Are you on the same Wi-Fi?"
        }
    }
    /** Something the guest asked for: the host's answer shows as it is, and the page catches up at once. */
    fun act(path: String, done: String? = null, vararg params: Pair<String, String?>) = scope.launch {
        val said = try {
            call(path, true, *params)
            done
        } catch (e: IOException) {
            if (e is JamError) e.message else "Can't reach the host"
        }
        said?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
        refresh()
    }
    LaunchedEffect(id, ended) {
        while (id != null && !ended) {
            refresh()
            delay(2_500)
        }
    }
    LaunchedEffect(q, id) {
        if (q.isBlank() || id == null) { results = emptyList(); return@LaunchedEffect }
        delay(300)
        try {
            results = call("search", false, "q" to q.trim()).list("songs").map(Song::from)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    val st = state
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
        item("title") {
            BackTitle(st?.name ?: "Join a Jam")
        }
        when {
            ended -> item { Text("This Jam has ended. Ask the host for a new link.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp)) }
            id == null -> item("join") {
                fun join() = scope.launch {
                    try {
                        val g = jamCall(base, "join", true, listOf("name" to name)).getString("id")
                        guestIds[base] = g
                        id = g
                    } catch (e: IOException) {
                        problem = if (e is JamError) e.message else "Can't reach the host. Are you on the same Wi-Fi?"
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("At ${base.toHttpUrl().run { "$host:$port" }}. Your name shows next to the songs you add.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        name, { name = it.take(24) }, Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors, label = { Text("Your name") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { join() }),
                    )
                    problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    Button({ join() }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = name.isNotBlank(), colors = accentButton) {
                        Text("Join", color = LocalContentColor.current, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            st == null -> item { problem?.let { ErrorText(it) } ?: Loading() }
            else -> {
                item("now") {
                    Column {
                        Text(
                            "You are ${st.you} · ${people(st.guests.size)}" + if (st.guests.size > 1) ": ${st.guests.joinToString()}" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
                        val now = st.now
                        Row(
                            Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (now == null) Text("Nothing is playing. Add a song to start.", style = MaterialTheme.typography.bodySmall)
                            else {
                                JamCover(base, now.song.coverArt, 88.dp)
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(now.song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(now.song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    now.by?.let { Text("Added by $it", style = MaterialTheme.typography.bodySmall, color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    // Runs on between polls while the host plays.
                                    val pos by produceState(st.positionMs, st.positionMs, st.playing, now.song.id) {
                                        val since = SystemClock.elapsedRealtime()
                                        value = st.positionMs
                                        while (st.playing) {
                                            value = st.positionMs + SystemClock.elapsedRealtime() - since
                                            delay(500)
                                        }
                                    }
                                    val dur = now.song.duration * 1000f
                                    LinearProgressIndicator(
                                        { if (dur > 0) (pos / dur).coerceIn(0f, 1f) else 0f }, Modifier.padding(top = 10.dp).fillMaxWidth(),
                                        color = Color.White, trackColor = Color.White.copy(alpha = .2f), drawStopIndicator = {},
                                    )
                                }
                            }
                        }
                        if (st.votesNeeded > 0 && now != null) Button(
                            { act("skip") }, Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 48.dp), enabled = !st.voted,
                            colors = ButtonDefaults.buttonColors(containerColor = Tile, contentColor = Color.White, disabledContainerColor = Tile, disabledContentColor = Sub),
                        ) {
                            Text("${if (st.voted) "You voted to skip" else "Vote to skip"} · ${st.votes} of ${st.votesNeeded}", color = LocalContentColor.current, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                val full = st.maxPerGuest > 0 && st.mine >= st.maxPerGuest
                val queued = st.queue.map { it.song.id }.toSet()
                item("search") {
                    Column {
                        Text("Add a song", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
                        OutlinedTextField(
                            q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors, placeholder = { Text("Search the host's songs") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }) { Icon(Icons.Rounded.Close, "Clear") } },
                        )
                        if (!st.canAdd || full) Text(
                            if (!st.canAdd) "The host turned off adding songs for now." else "You have ${st.mine} of ${st.maxPerGuest} songs waiting. Add more once one has played.",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                items(results, key = { "hit:${it.id}" }) { s ->
                    JamRow(base, s, null) {
                        val added = s.id in queued
                        TextButton({ act("add", "Added ${s.title}", "id" to s.id) }, enabled = st.canAdd && !full && !added) {
                            Text(if (added) "Added" else "Add", color = if (st.canAdd && !full && !added) Accent else Faint, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                item("next") { Text("Up next", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)) }
                if (st.queue.isEmpty()) item { Text("Nothing queued yet.", style = MaterialTheme.typography.bodySmall) }
                items(st.queue, key = { "q:${it.index}:${it.song.id}" }) { x: JamSong ->
                    JamRow(base, x.song, x.by) {
                        if (x.by == st.you) IconButton({ act("remove", null, "index" to "${x.index}", "id" to x.song.id) }) { Icon(Icons.Rounded.Close, "Remove", tint = Sub) }
                    }
                }
            }
        }
        item("end") { BottomSpacer() }
    }
}
