package xyz.yuuki.navidroid.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import android.os.Process
import java.util.Calendar
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import org.json.JSONObject
import java.io.File
import xyz.yuuki.navidroid.Song
import xyz.yuuki.navidroid.Streamed
import xyz.yuuki.navidroid.s
import xyz.yuuki.navidroid.tile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import xyz.yuuki.navidroid.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.toArgb
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import xyz.yuuki.navidroid.ApiException
import xyz.yuuki.navidroid.Probe
import xyz.yuuki.navidroid.networkReason
import xyz.yuuki.navidroid.probeAddress
import xyz.yuuki.navidroid.Api
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.song

val fieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface, unfocusedContainerColor = Surface, disabledContainerColor = Surface,
    focusedBorderColor = Accent, unfocusedBorderColor = Color.Transparent, cursorColor = Accent,
    focusedLabelColor = Accent, unfocusedLabelColor = Sub, focusedLeadingIconColor = Accent, unfocusedLeadingIconColor = Sub,
    focusedTrailingIconColor = Sub, unfocusedTrailingIconColor = Sub, focusedSupportingTextColor = Faint, unfocusedSupportingTextColor = Faint,
)

private val fieldShape = RoundedCornerShape(14.dp)

/** A server address being typed and what answered at it; [result] is null while it is blank or being checked. */
internal class ServerField {
    var text by mutableStateOf("")
    var result by mutableStateOf<Pair<String, Probe>?>(null)
    var retries by mutableIntStateOf(0)

    /** The address to save, once a server answered there. */
    val found get() = result?.takeIf { it.second is Probe.Found }?.first
}

/** An address field that asks the address what it is as you type; [found] words the good news from the server's name. */
@Composable
internal fun ProbeField(
    f: ServerField, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, hint: String = "",
    none: String = "No Subsonic server here", found: (String) -> String = { it }, onGo: () -> Unit = {},
) {
    LaunchedEffect(f.text, f.retries) {
        f.result = null
        if (f.text.isBlank()) return@LaunchedEffect
        delay(400)
        f.result = withContext(Dispatchers.IO) { probeAddress(f.text) }
    }
    val r = f.result
    OutlinedTextField(
        f.text, { f.text = it }, modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors, enabled = enabled, singleLine = true,
        label = { StateText(label) }, leadingIcon = { Icon(Icons.Rounded.Dns, null) },
        trailingIcon = {
            when {
                f.text.isBlank() -> {}
                r == null -> CircularProgressIndicator(Modifier.size(20.dp), color = Sub, strokeWidth = 2.dp)
                r.second is Probe.Found -> Icon(Icons.Rounded.Check, null, tint = Green)
                else -> IconButton({ f.retries++ }) { Icon(Icons.Rounded.Refresh, "Check again") }
            }
        },
        supportingText = {
            val (text, color) = when {
                f.text.isBlank() -> hint to Faint
                r == null -> "Checking…" to Sub
                else -> when (val p = r.second) {
                    is Probe.Found -> found(p.name) + (if (r.first.startsWith("http://")) " • not encrypted (http)" else "") to Green
                    Probe.NotSubsonic -> none to Red
                    is Probe.Unreachable -> "Can't reach this address: ${p.reason}" to Red
                }
            }
            Text(text, color = color, style = MaterialTheme.typography.bodySmall)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
    )
}

@Composable
private fun ErrorBox(lines: List<String>) = Row(
    Modifier.fillMaxWidth().clip(fieldShape).background(Red.copy(alpha = .14f)).padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
) {
    Icon(Icons.Rounded.ErrorOutline, null, tint = Red)
    Column { lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) } }
}

/** Two steps on one card: find the server, then sign in to it. */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val servers = remember { mutableStateListOf(ServerField()) }
    var account by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    val reachable = servers.mapNotNull { it.found }.distinct()
    val canSubmit = !busy && reachable.isNotEmpty() && user.isNotBlank() && pass.isNotEmpty()

    fun submit() {
        if (!canSubmit) return
        busy = true
        errors = emptyList()
        scope.launch {
            val name = user.trim()
            // Only the salted token is kept, and only the addresses that took it.
            val results = withContext(Dispatchers.IO) {
                reachable.map { ep -> async { ep to runCatching { Api.login(ep, name, pass) } } }.awaitAll()
            }
            val ok = results.mapNotNull { (ep, r) -> r.getOrNull()?.let { ep to it } }
            if (ok.isEmpty()) {
                val why = results.map { (ep, r) -> ep to r.exceptionOrNull()!!.let { (it as? ApiException)?.message ?: "Couldn't sign in: ${networkReason(it)}" } }
                // The same answer from every address, a wrong password say, is said once.
                errors = why.map { it.second }.distinct().singleOrNull()?.let(::listOf) ?: why.map { "${it.first}: ${it.second}" }
                busy = false
                return@launch
            }
            val prefs = App.it.prefs
            prefs.endpoints = ok.map { it.first }
            prefs.username = name
            prefs.token = ok[0].second.first
            prefs.salt = ok[0].second.second
            prefs.active = ok[0].first
            App.it.api = Api(prefs, App.it.scope).apply { clearSaved() }
            App.it.scope.launch { App.it.api.pickFastest() }
            onDone()
        }
    }

    BackHandler(account && !busy) { account = false }
    Box(Modifier.fillMaxSize().background(Bg)) {
        Aurora(null, null, modifier = Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            LauncherIcon(icons[remember { currentIcon(ctx) }].res, 88.dp)
            Text("Navidroid", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
            Text(
                "Your whole library, streamed from your own server.",
                style = MaterialTheme.typography.bodyMedium, color = Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            Column(
                // Phone width on a tablet instead of fields across the whole screen.
                Modifier.widthIn(max = 440.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = .45f)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (account) "Your account" else "Your server", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Text(if (account) "Step 2 of 2" else "Step 1 of 2", style = MaterialTheme.typography.labelMedium, color = Faint)
                }
                if (!account) {
                    servers.forEachIndexed { i, f ->
                        key(f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProbeField(
                                    f, if (i == 0) "Server address" else "Another address", Modifier.weight(1f),
                                    hint = if (i == 0) "music.example.com or 192.168.1.10:4533" else "Another way to the same server",
                                ) { if (reachable.isNotEmpty()) account = true }
                                if (i > 0) IconButton({ servers.remove(f) }) { Icon(Icons.Rounded.Close, "Remove address", tint = Sub) }
                            }
                        }
                    }
                    TextButton({ servers += ServerField() }) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(18.dp), tint = Accent)
                        Spacer(Modifier.width(6.dp))
                        Text("Add another address (e.g. your home network)", color = Accent)
                    }
                    Button(
                        { account = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = reachable.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent, disabledContainerColor = Elevated, disabledContentColor = Faint),
                    ) { Text("Continue", color = LocalContentColor.current, style = MaterialTheme.typography.titleMedium) }
                } else {
                    Row(
                        Modifier.fillMaxWidth().clip(fieldShape).background(Color.White.copy(alpha = .06f)).padding(start = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Green)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(servers.firstNotNullOfOrNull { (it.result?.second as? Probe.Found)?.name } ?: "Server", style = MaterialTheme.typography.bodyLarge)
                            Text(reachable.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton({ account = false }, enabled = !busy) { Text("Change", color = Accent) }
                    }
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    OutlinedTextField(
                        user, { user = it }, Modifier.fillMaxWidth().focusRequester(focus).semantics { contentType = ContentType.Username },
                        shape = fieldShape, colors = fieldColors, enabled = !busy, singleLine = true,
                        label = { StateText("Username") }, leadingIcon = { Icon(Icons.Rounded.Person, null) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    )
                    OutlinedTextField(
                        pass, { pass = it }, Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
                        shape = fieldShape, colors = fieldColors, enabled = !busy, singleLine = true,
                        label = { StateText("Password") }, leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                        trailingIcon = {
                            IconButton({ showPass = !showPass }) {
                                Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (showPass) "Hide password" else "Show password")
                            }
                        },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                    )
                    if (errors.isNotEmpty()) ErrorBox(errors)
                    Button(
                        ::submit, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent, disabledContainerColor = Elevated, disabledContentColor = Faint),
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = OnAccent, strokeWidth = 2.dp)
                        else Text("Log in", color = LocalContentColor.current, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

private val Red = Color(0xFFF15E6C)

/** Our typography bakes in text colors, which would override the field's focused/disabled colors. */
@Composable
private fun StateText(text: String) = Text(text, color = LocalContentColor.current)

// ---- what every settings page is built from ---------------------------------------------------

internal val SettingsWidth = Modifier.widthIn(max = 640.dp).fillMaxWidth()

/** A settings page: its title, then [content] in a column no wider than a phone. */
@Composable
internal fun SettingsPage(title: String, content: LazyListScope.() -> Unit) =
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Box(SettingsWidth) { BackTitle(title) } }
        content()
        item { BottomSpacer() }
    }

/** A card holding one group of settings, under a header if it has a [title]; [action] sits at the header's end. */
@Composable
internal fun Section(
    title: String? = null, modifier: Modifier = SettingsWidth, action: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit,
) = Column(modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
    if (title != null) Row(Modifier.padding(start = 4.dp, top = 8.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp, color = Sub, modifier = Modifier.weight(1f))
        action()
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .06f)).padding(vertical = 4.dp), content = content)
}

@Composable
internal fun IconTile(icon: ImageVector, tint: Color) =
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
    }

/** Titles wrap rather than cut off, so a large font scale costs height, not words. */
@Composable
internal fun SettingRow(
    title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, icon: ImageVector? = null, tint: Color = Accent,
    subtitleLines: Int = Int.MAX_VALUE, trailing: @Composable () -> Unit = {},
) = Row(
    Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    if (icon != null) {
        IconTile(icon, tint)
        Spacer(Modifier.width(14.dp))
    }
    Column(Modifier.weight(1f).padding(end = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = subtitleLines, overflow = TextOverflow.Ellipsis) }
    }
    trailing()
}

/** A row that opens a page. */
@Composable
internal fun NavRow(title: String, subtitle: String?, icon: ImageVector? = null, tint: Color = Accent, onClick: () -> Unit) =
    SettingRow(title, subtitle, onClick, icon, tint, subtitleLines = 2) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Faint) }

@Composable
internal fun SwitchRow(title: String, subtitle: String?, checked: Boolean, icon: ImageVector? = null, tint: Color = Accent, onChange: (Boolean) -> Unit) =
    SettingRow(title, subtitle, { onChange(!checked) }, icon, tint) {
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
    }

private fun quality(br: Int) = qualities.firstOrNull { it.first == br }?.second ?: "$br kbps"

private val qualities = listOf(0 to "Original", 320 to "320 kbps", 256 to "256 kbps", 192 to "192 kbps", 160 to "160 kbps", 128 to "128 kbps", 96 to "96 kbps")

/** A row showing one of [options], picked from a menu. */
@Composable
internal fun ChoiceRow(
    title: String, value: Int, options: List<Pair<Int, String>> = qualities, icon: ImageVector? = null, tint: Color = Accent, onChange: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(title, options.firstOrNull { it.first == value }?.second ?: "$value kbps", { open = true }, icon, tint) {
            Icon(Icons.Rounded.UnfoldMore, null, tint = Faint)
        }
        DropdownMenu(open, { open = false }, containerColor = Elevated) {
            options.forEach { (v, label) ->
                DropdownMenuItem({ Text(label) }, { onChange(v); open = false }, trailingIcon = { if (v == value) Icon(Icons.Rounded.Check, null, tint = Accent) })
            }
        }
    }
}

@Composable
private fun AuroraSwitch(t: Toggle) = SwitchRow(t.title, t.subtitle, t.on) { t.on = it }

private val storageColors = listOf(Color(0xFF509BF5), Color(0xFF8D67AB), Color(0xFF27856A), Color(0xFFF59B23))

/** Downloads, streamed songs, covers, saved info. */
private fun storageBytes(ctx: Context) = App.it.run { listOf(offline.bytesUsed(), offline.streamBytes(), ctx.imageLoader.diskCache?.size ?: 0L, api.savedBytes()) }

/** Bytes this app moved since the 1st, on Wi-Fi and on mobile data; null where Android won't say (mobile before Android 10). */
@Suppress("DEPRECATION")
private fun monthTraffic(ctx: Context): Pair<Long?, Long?> {
    val stats = ctx.getSystemService(NetworkStatsManager::class.java)
    val start = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    fun sum(type: Int) = runCatching {
        stats.queryDetailsForUid(type, null, start, System.currentTimeMillis(), Process.myUid()).use { s ->
            val b = NetworkStats.Bucket()
            var total = 0L
            while (s.hasNextBucket()) {
                s.getNextBucket(b)
                total += b.rxBytes + b.txBytes
            }
            total
        }
    }.getOrNull()
    return sum(ConnectivityManager.TYPE_WIFI) to sum(ConnectivityManager.TYPE_MOBILE)
}

private val themes = listOf(
    "Blue" to Blue, "Green" to Green, "Purple" to Purple, "Pink" to Color(0xFFEC4899),
    "Amber" to Color(0xFFF59E0B), "Teal" to Color(0xFF14B8A6), "Red" to Color(0xFFEF4444),
)

private fun themeName() = themes.firstOrNull { it.second == Accent }?.first ?: "Custom"

private fun setAccent(c: Color) {
    Accent = c
    App.it.prefs.accent = c.toArgb()
}

private val rainbow = Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))

/** A launcher entry in the manifest; Aurora is on unless another was picked. */
private class AppIcon(val alias: String, val name: String, val res: Int)

private val icons = listOf(
    AppIcon("IconAurora", "Aurora", R.mipmap.ic_launcher),
    AppIcon("IconMoonrise", "Moonrise", R.mipmap.ic_launcher_moonrise),
    AppIcon("IconPeaks", "Peaks", R.mipmap.ic_launcher_peaks),
    AppIcon("IconEclipse", "Eclipse", R.mipmap.ic_launcher_eclipse),
    AppIcon("IconBlue", "Classic", R.mipmap.ic_launcher_blue),
    AppIcon("IconGreen", "Classic green", R.mipmap.ic_launcher_green),
)

private fun iconComponent(ctx: Context, i: Int) = ComponentName(ctx, "xyz.yuuki.navidroid.${icons[i].alias}")

private fun currentIcon(ctx: Context) =
    icons.indices.firstOrNull { ctx.packageManager.getComponentEnabledSetting(iconComponent(ctx, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED } ?: 0

/** Enables the chosen launcher entry before disabling the rest, so the app never has no icon. */
private fun setIcon(ctx: Context, chosen: Int) = icons.indices.sortedBy { it != chosen }.forEach {
    ctx.packageManager.setComponentEnabledSetting(
        iconComponent(ctx, it),
        if (it == chosen) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
}

/** Before Aurora was the default, the classic icon was; whoever picked one keeps it instead of getting two. */
fun migrateIcon(ctx: Context) {
    if (ctx.packageManager.getComponentEnabledSetting(iconComponent(ctx, 0)) != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) return
    icons.indices.drop(1).firstOrNull { ctx.packageManager.getComponentEnabledSetting(iconComponent(ctx, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
        ?.let { setIcon(ctx, it) }
}

/** The launcher icon as the launcher shows it, in this phone's icon shape. */
@Composable
private fun LauncherIcon(res: Int, size: Dp) {
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bmp = remember(res, px) { ctx.getDrawable(res)!!.toBitmap(px, px).asImageBitmap() }
    Image(bmp, null, Modifier.size(size))
}

@Composable
private fun ColorDialog(initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val c = Color.hsv(hue, sat, value)
    val sliderColors = SliderDefaults.colors(thumbColor = c, activeTrackColor = c, inactiveTrackColor = Color.White.copy(alpha = .15f))
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Elevated,
        title = { Text("Custom colour") },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(c))
                Text("Hue", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall)
                Slider(hue, { hue = it }, valueRange = 0f..360f, colors = sliderColors)
                Text("Saturation", style = MaterialTheme.typography.bodySmall)
                Slider(sat, { sat = it }, colors = sliderColors)
                Text("Brightness", style = MaterialTheme.typography.bodySmall)
                Slider(value, { value = it }, valueRange = .3f..1f, colors = sliderColors)
            }
        },
        confirmButton = { TextButton({ onPick(c); onDismiss() }) { Text("Use", color = c) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = Color.White) } },
    )
}

private const val REPO = "https://github.com/rene-roid/navidroid"

private fun onOff(on: Boolean) = if (on) "on" else "off"

private fun appVersion(ctx: Context) = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName

/** A row of the settings home: the page it opens, and a line saying how things there stand right now. */
private class Category(val title: String, val icon: ImageVector, val tint: Color, val route: Route, val summary: @Composable () -> String)

/** The settings home, one card per group. */
private val categories = listOf(
    listOf(
        Category("Playback", Icons.Rounded.GraphicEq, Green, Route.Playback) {
            App.it.prefs.run { "${quality(streamBitrateWifi)} on Wi-Fi • ${quality(streamBitrateMobile)} on mobile data • Autoplay ${onOff(autoplay)}" }
        },
        Category("Downloads", Icons.Rounded.DownloadForOffline, Color(0xFF509BF5), Route.DownloadSettings) {
            val pins by App.it.offline.pins.collectAsStateWithLifecycle()
            App.it.prefs.run { "${quality(downloadBitrate)} • ${if (wifiOnlyDownloads) "Wi-Fi only" else "Any network"} • ${pins.size} kept on this phone" }
        },
    ),
    listOf(
        Category("Appearance", Icons.Rounded.Palette, Purple, Route.Appearance) {
            val ctx = LocalContext.current
            "${themeName()} theme • ${icons[remember { currentIcon(ctx) }].name} icon • Aurora ${onOff(auroraOn.on)}"
        },
        Category("Library", Icons.Rounded.LibraryMusic, Color(0xFFF59E0B), Route.HiddenPlaylists) {
            Hidden.playlists.size.let { if (it == 0) "No hidden playlists" else "$it hidden playlists" }
        },
    ),
    listOf(
        Category("Server & connection", Icons.Rounded.Dns, Color(0xFF14B8A6), Route.Server) {
            val n = App.it.prefs.endpoints.size
            "${App.it.api.active.substringAfter("://")} in use • ${if (n == 1) "1 address" else "$n addresses"}"
        },
        Category("Storage", Icons.Rounded.Storage, Color(0xFFEC4899), Route.Storage) {
            val ctx = LocalContext.current
            val used by produceState<Long?>(null) { value = withContext(Dispatchers.IO) { storageBytes(ctx).sum() } }
            used?.let { "${formatBytes(it)} on this phone" } ?: "Counting…"
        },
    ),
    listOf(
        Category("About", Icons.Rounded.Info, Sub, Route.About) { "Version ${appVersion(LocalContext.current)}" },
    ),
)

/** The address in use and whether the addresses are being raced right now. */
private class Connection(val active: String, val checking: Boolean, val recheck: () -> Unit)

/** Races the addresses when first shown, and again on [Connection.recheck]. */
@Composable
private fun rememberConnection(): Connection {
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(App.it.api.active) }
    var checking by remember { mutableStateOf(false) }
    fun recheck() {
        checking = true
        scope.launch {
            App.it.api.pickFastest()
            active = App.it.api.active
            checking = false
        }
    }
    LaunchedEffect(Unit) { recheck() }
    return Connection(active, checking, ::recheck)
}

/** Who is logged in and where, and how the connection is doing. */
@Composable
private fun AccountCard(onLogout: () -> Unit) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val conn = rememberConnection()
    val latency by App.it.api.latency.collectAsStateWithLifecycle()
    val server by produceState<Probe?>(null, conn.active) { value = withContext(Dispatchers.IO) { Api.probe(conn.active) } }
    var confirm by remember { mutableStateOf(false) }
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)!!.use { i -> avatarFile.outputStream().use { i.copyTo(it) } } }
            }
            avatarVersion++
        }
    }
    val where = (server as? Probe.Found)?.name ?: conn.active.substringAfter("://")
    Column(
        SettingsWidth.padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Accent.copy(alpha = .3f), Color.White.copy(alpha = .06f)))).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The avatar's initial is sized by the text style around it.
            ProvideTextStyle(MaterialTheme.typography.headlineMedium) {
                Avatar(72.dp) { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(displayName(), style = MaterialTheme.typography.headlineSmall)
                Text("${App.it.prefs.username} on $where", style = MaterialTheme.typography.bodyMedium, color = Sub)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val ms = latency[conn.active]
                    val known = conn.active in latency
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (ms != null) Green else if (known) Red else Faint))
                    Text(
                        if (ms != null) "Connected • $ms ms" else if (known) "Can't reach the server" else "Checking…",
                        Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ nav.go(Route.Server) }, Modifier.weight(1f)) {
                Icon(Icons.Rounded.Dns, null, Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Server", color = Color.White)
            }
            OutlinedButton({ confirm = true }, Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Log out", color = Color.White)
            }
        }
    }
    if (confirm) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirm = false },
        containerColor = Elevated,
        title = { Text("Log out?") },
        text = { Text("You'll need the server's address and your password to come back. Downloads stay on this phone.") },
        confirmButton = { TextButton({ confirm = false; onLogout() }) { Text("Log out", color = Red) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancel", color = Color.White) } },
    )
}

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val nav = LocalNav.current
    SettingsPage("Settings") {
        item { AccountCard(onLogout) }
        items(categories) { group ->
            Section { group.forEach { c -> NavRow(c.title, c.summary(), c.icon, c.tint) { nav.go(c.route) } } }
        }
    }
}

@Composable
fun PlaybackSettingsScreen() {
    val prefs = App.it.prefs
    var wifi by remember { mutableIntStateOf(prefs.streamBitrateWifi) }
    var mobile by remember { mutableIntStateOf(prefs.streamBitrateMobile) }
    var autoplay by remember { mutableStateOf(prefs.autoplay) }
    SettingsPage("Playback") {
        item {
            Section("Streaming quality") {
                ChoiceRow("On Wi-Fi", wifi, icon = Icons.Rounded.Wifi, tint = Green) { wifi = it; prefs.streamBitrateWifi = it }
                ChoiceRow("On mobile data", mobile, icon = Icons.Rounded.SignalCellularAlt, tint = Green) { mobile = it; prefs.streamBitrateMobile = it }
            }
        }
        item {
            Section("Queue") {
                SwitchRow("Autoplay", "When the queue ends, keep playing songs like the last ones", autoplay, Icons.Rounded.AllInclusive, Green) {
                    autoplay = it
                    prefs.autoplay = it
                }
            }
        }
    }
}

@Composable
fun DownloadSettingsScreen() {
    val nav = LocalNav.current
    val app = App.it
    val blue = Color(0xFF509BF5)
    var dl by remember { mutableIntStateOf(app.prefs.downloadBitrate) }
    var wifiOnly by remember { mutableStateOf(app.prefs.wifiOnlyDownloads) }
    val pins by app.offline.pins.collectAsStateWithLifecycle()
    SettingsPage("Downloads") {
        item {
            Section {
                ChoiceRow("Download quality", dl, icon = Icons.Rounded.HighQuality, tint = blue) { dl = it; app.prefs.downloadBitrate = it; app.offline.sync() }
                SwitchRow("Download on Wi-Fi only", "Pinned playlists wait for Wi-Fi", wifiOnly, Icons.Rounded.Wifi, blue) { wifiOnly = it; app.offline.setWifiOnly(it) }
            }
        }
        item {
            Section {
                NavRow("Manage downloads", "${pins.size} playlists and albums kept on this phone", Icons.Rounded.DownloadForOffline, blue) { nav.go(Route.Downloads) }
            }
        }
    }
}

@Composable
fun ServerScreen() {
    val prefs = App.it.prefs
    val conn = rememberConnection()
    val latency by App.it.api.latency.collectAsStateWithLifecycle()
    var endpoints by remember { mutableStateOf(prefs.endpoints) }
    var adding by remember { mutableStateOf(false) }
    val info by produceState<Probe?>(null, conn.active) { value = withContext(Dispatchers.IO) { Api.probe(conn.active) } }
    fun save(list: List<String>) {
        endpoints = list
        prefs.endpoints = list
        conn.recheck()
    }
    SettingsPage("Server & connection") {
        item {
            Section("Addresses", action = {
                if (conn.checking) CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), color = Accent, strokeWidth = 2.dp)
                else IconButton(conn.recheck) { Icon(Icons.Rounded.Refresh, "Check again") }
            }) {
                endpoints.forEachIndexed { i, ep ->
                    val ms = latency[ep]
                    val testing = ep !in latency
                    SettingRow(
                        ep,
                        listOfNotNull("Preferred".takeIf { i == 0 && endpoints.size > 1 }, if (testing) "Checking…" else ms?.let { "$it ms" } ?: "Unreachable").joinToString(" • "),
                        if (i > 0) ({ save(listOf(ep) + (endpoints - ep)) }) else null,
                        if (testing) Icons.Rounded.MoreHoriz else if (ms != null) Icons.Rounded.Check else Icons.Rounded.CloudOff,
                        if (testing) Sub else if (ms != null) Green else Red,
                    ) {
                        if (ep == conn.active) Text(
                            "In use", Modifier.clip(CircleShape).background(Accent.copy(alpha = .22f)).padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        if (endpoints.size > 1) IconButton({ save(endpoints - ep) }) { Icon(Icons.Rounded.Close, "Remove $ep", tint = Sub) }
                    }
                }
                SettingRow("Add address", "Another way to the same server, like your home network", { adding = true }, Icons.Rounded.Add)
            }
        }
        item {
            Text(
                "The preferred address is used whenever it answers, otherwise the fastest of the others. Tap an address to prefer it.",
                SettingsWidth.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Section("Server") {
                val found = info as? Probe.Found
                // "…" while asking, "—" when it did not answer.
                fun value(of: (Probe.Found) -> String) = found?.let(of) ?: if (info == null) "…" else "—"
                SettingRow("Type") { Text(value { it.type.replaceFirstChar(Char::uppercase) }, color = Sub) }
                SettingRow("Version") { Text(value { it.version.ifEmpty { "Unknown" } }, color = Sub) }
                SettingRow("OpenSubsonic", "Extras like synced lyrics and several artists per song") { Text(value { if (it.openSubsonic) "Yes" else "No" }, color = Sub) }
            }
        }
    }
    if (adding) AddAddressSheet(endpoints, { adding = false }) {
        adding = false
        save(endpoints + it)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAddressSheet(known: List<String>, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val field = remember { ServerField() }
    val found = field.found
    ModalBottomSheet(onDismiss, containerColor = Elevated) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add address", style = MaterialTheme.typography.titleLarge)
            Text("Another way to reach the same server. It is checked as you type.", style = MaterialTheme.typography.bodySmall)
            ProbeField(field, "Server address", hint = "music.example.com or 192.168.1.10:4533", none = "No server at this address", found = { "$it found" }) {
                if (found != null && found !in known) onAdd(found)
            }
            if (found != null && found in known) Text("That address is already on your list.", style = MaterialTheme.typography.bodySmall)
            Button(
                { onAdd(found!!) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = found != null && found !in known,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent, disabledContainerColor = Tile, disabledContentColor = Faint),
            ) { Text("Add", color = LocalContentColor.current, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun StorageScreen() {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val app = App.it
    val scope = rememberCoroutineScope()
    var cleared by remember { mutableIntStateOf(0) }
    val storage by produceState(listOf(0L, 0L, 0L, 0L), cleared) { value = withContext(Dispatchers.IO) { storageBytes(ctx) } }
    val traffic by produceState<Pair<Long?, Long?>>(null to null) { value = withContext(Dispatchers.IO) { monthTraffic(ctx) } }
    fun clear(action: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) { action() }
            cleared++
        }
    }
    SettingsPage("Storage") {
        item {
            Section {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("${formatBytes(storage.sum())} on this phone", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = .1f))) {
                        storage.forEachIndexed { i, b -> if (b > 0) Box(Modifier.weight(b.toFloat()).fillMaxHeight().background(storageColors[i])) }
                    }
                }
                // Tapping one shows what's inside.
                @Composable
                fun part(i: Int, title: String, about: String, open: Route, trailing: @Composable () -> Unit) =
                    Row(Modifier.clickable { nav.go(open) }, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(start = 16.dp).size(10.dp).clip(CircleShape).background(storageColors[i]))
                        Box(Modifier.weight(1f)) { SettingRow(title, "${formatBytes(storage[i])} • $about", trailing = trailing) }
                    }
                part(0, "Downloads", "Pinned playlists and albums", Route.Downloads) {
                    TextButton({ nav.go(Route.Downloads) }) { Text("Manage", color = Accent) }
                }
                part(1, "Song cache", "Songs heard or lined up next, up to 1 GB", Route.SongCache) {
                    TextButton({ clear(app.offline::clearStreamCache) }) { Text("Clear", color = Accent) }
                }
                part(2, "Image cache", "Covers, fetched again as you browse", Route.ImageCache) {
                    TextButton({
                        ctx.imageLoader.memoryCache?.clear()
                        clear { ctx.imageLoader.diskCache?.clear() }
                    }) { Text("Clear", color = Accent) }
                }
                part(3, "Saved info", "Albums, artists and playlists, to open fast and offline", Route.SavedInfo) {
                    TextButton({ clear { app.api.clearSaved() } }) { Text("Clear", color = Accent) }
                }
            }
        }
        item {
            Section("Data used this month") {
                val (wifiBytes, mobileBytes) = traffic
                SettingRow("Wi-Fi", "Streaming at ${quality(app.prefs.streamBitrateWifi)}", icon = Icons.Rounded.Wifi, tint = Sub) { Text(wifiBytes?.let(::formatBytes) ?: "—") }
                SettingRow("Mobile data", "Streaming at ${quality(app.prefs.streamBitrateMobile)}", icon = Icons.Rounded.SignalCellularAlt, tint = Sub) {
                    Text(mobileBytes?.let(::formatBytes) ?: "—")
                }
            }
        }
        item {
            Text(
                "Counted by Android, so it can lag by an hour or two.", SettingsWidth.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    val links = LocalUriHandler.current
    // No browser to open it with is not worth a crash.
    fun open(url: String) = runCatching { links.openUri(url) }
    SettingsPage("About") {
        item {
            Column(SettingsWidth.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LauncherIcon(icons[remember { currentIcon(ctx) }].res, 80.dp)
                Text("Navidroid", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.headlineSmall)
                Text("Version ${appVersion(ctx)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Section {
                NavRow("Source code", REPO.substringAfter("://"), Icons.Rounded.Code, Sub) { open(REPO) }
                NavRow("Report a problem", "Open an issue on GitHub", Icons.Rounded.BugReport, Sub) { open("$REPO/issues") }
                SettingRow(
                    "Licenses", "Navidroid is MIT licensed. It is built on Jetpack Compose, Media3, OkHttp and Coil (Apache 2.0) and set in Figtree (OFL).",
                    { open("$REPO/blob/main/LICENSE") }, Icons.Rounded.Gavel, Sub,
                )
            }
        }
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
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                    ) {
                        if (syncing) CircularProgressIndicator(Modifier.size(16.dp), color = OnAccent, strokeWidth = 2.dp) else Icon(Icons.Rounded.Sync, null, Modifier.size(18.dp))
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
                        color = Accent, trackColor = Color.White.copy(alpha = .1f),
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
                            color = Accent, trackColor = Color.White.copy(alpha = .1f),
                        )
                    }
                }
            }
        }
        if (tiles.isNotEmpty()) item {
            HorizontalDivider(Modifier.padding(top = 24.dp), color = Elevated)
            TextButton({ confirmClear = true }, Modifier.padding(8.dp)) { Text("Remove all downloads", color = Color(0xFFF15E6C)) }
        }
        item { BottomSpacer() }
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

/** A round choice with its name under it; [fill] paints the circle. */
@Composable
private fun Swatch(name: String, fill: Brush, selected: Boolean, onClick: () -> Unit) = Column(
    Modifier.width(72.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box(Modifier.size(48.dp).border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape).padding(5.dp).clip(CircleShape).background(fill))
    Text(name, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Sub, textAlign = TextAlign.Center, maxLines = 2)
}

@Composable
private fun Swatches(content: @Composable () -> Unit) =
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { content() }

private fun setPalette(i: Int) {
    auroraPalette = i
    App.it.prefs.auroraPalette = i
}

private fun setCustom(i: Int, c: Color) {
    auroraCustom = auroraCustom.toMutableList().also { it[i] = c }
    App.it.prefs.auroraCustom = auroraCustom.joinToString(",") { it.toArgb().toString() }
}

private val auroraDefault get() = auroraOn.on && auroraDetails.all { it.on } && auroraMode == 0 && auroraPalette == 0 &&
    auroraBrightness == 1f && auroraCustom == auroraPalettes[0].second

private fun resetAurora() {
    auroraOn.on = true
    auroraDetails.forEach { it.on = true }
    auroraMode = 0
    App.it.prefs.auroraColors = 0
    setPalette(0)
    auroraCustom = auroraPalettes[0].second
    App.it.prefs.auroraCustom = null
    auroraBrightness = 1f
    App.it.prefs.auroraBrightness = 100
}

/** Shown in a section's header while it differs from how the app ships. */
@Composable
private fun ResetButton(show: Boolean, onClick: () -> Unit) {
    if (show) TextButton(onClick) { Text("Reset", color = Accent) }
}

@Composable
fun AppearanceScreen() {
    val ctx = LocalContext.current
    val prefs = App.it.prefs
    var icon by remember { mutableIntStateOf(currentIcon(ctx)) }
    // What the colour dialog is editing: the theme, or one of the custom aurora colours.
    var editing by remember { mutableStateOf<Pair<Color, (Color) -> Unit>?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    fun resetIcon() {
        icon = 0
        setIcon(ctx, 0)
    }
    val wide = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Box(wide) { BackTitle("Appearance") } }
        item {
            Section("Theme", wide, action = { ResetButton(Accent != Blue) { setAccent(Blue) } }) {
                Text("Buttons, sliders and highlights", Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall)
                Swatches {
                    themes.forEach { (name, c) -> Swatch(name, SolidColor(c), Accent == c) { setAccent(c) } }
                    val custom = themes.none { it.second == Accent }
                    Swatch("Custom", if (custom) SolidColor(Accent) else rainbow, custom) { editing = Accent to ::setAccent }
                }
            }
        }
        item {
            Section("App icon", wide, action = { ResetButton(icon != 0, ::resetIcon) }) {
                FlowRow(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    icons.forEachIndexed { i, ic ->
                        Column(
                            Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).clickable { icon = i; setIcon(ctx, i) }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.border(2.dp, if (i == icon) Accent else Color.Transparent, RoundedCornerShape(18.dp)).padding(5.dp)) { LauncherIcon(ic.res, 56.dp) }
                            Text(ic.name, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelMedium, color = if (i == icon) Color.White else Sub, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
                Text(
                    "Your launcher may take a few seconds to show the new one.", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
        item {
            Section("Aurora", wide, action = { ResetButton(!auroraDefault, ::resetAurora) }) {
                AuroraSwitch(auroraOn)
                if (auroraOn.on) {
                    Text("Your palette", Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.bodyLarge)
                    Text("Shown when no cover is colouring the sky, like right now", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                    Swatches {
                        auroraPalettes.forEachIndexed { i, (name, colors) -> Swatch(name, Brush.sweepGradient(colors + colors[0]), auroraPalette == i) { setPalette(i) } }
                        Swatch("Custom", Brush.sweepGradient(auroraCustom + auroraCustom[0]), auroraPalette == auroraPalettes.size) { setPalette(auroraPalettes.size) }
                    }
                    if (auroraPalette == auroraPalettes.size) Row(
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Tap to change", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        auroraCustom.forEachIndexed { i, c ->
                            Box(Modifier.size(40.dp).clip(CircleShape).background(c).clickable { editing = c to { setCustom(i, it) } })
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 33) Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("Brightness", style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            auroraBrightness, { auroraBrightness = it }, valueRange = .4f..1.8f,
                            onValueChangeFinished = { prefs.auroraBrightness = (auroraBrightness * 100).toInt() },
                            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Color.White.copy(alpha = .15f)),
                        )
                    }
                    ChoiceRow("Colours come from", auroraMode, auroraModes) { auroraMode = it; prefs.auroraColors = it }
                    auroraDetails.forEach { AuroraSwitch(it) }
                }
            }
        }
        if (Accent != Blue || icon != 0 || !auroraDefault) item {
            Box(wide.padding(8.dp)) {
                TextButton({ confirmReset = true }) { Text("Reset all to defaults", color = Color(0xFFF15E6C)) }
            }
        }
        item { BottomSpacer() }
    }
    editing?.let { (c, pick) -> ColorDialog(c, { editing = null }, pick) }
    if (confirmReset) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = Elevated,
        title = { Text("Reset appearance?") },
        text = { Text("Back to the blue theme, the Aurora icon and the classic aurora with everything on. Your custom colours are cleared.") },
        confirmButton = {
            TextButton({
                setAccent(Blue)
                resetIcon()
                resetAurora()
                confirmReset = false
            }) { Text("Reset", color = Color(0xFFF15E6C)) }
        },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel", color = Color.White) } },
    )
}

@Composable
fun HiddenPlaylistsScreen() {
    val playlists = rememberLoad("playlists") { App.it.api.playlists() }
    val hidden = playlists.data.orEmpty().filter { it.id in Hidden.playlists }
    val wide = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Box(wide) { BackTitle("Hidden playlists") } }
        item {
            Text(
                if (hidden.isEmpty() && !playlists.loading) "Nothing hidden. To hide playlists, long-press them in your library and tap Hide."
                else "They stay on the server, just out of your home and library on this phone.",
                wide.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
            )
        }
        items(hidden, key = { it.id }) { p ->
            val t = p.tile()
            Row(wide.padding(start = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(48.dp), 120)
                Box(Modifier.weight(1f)) {
                    SettingRow(t.title, if (p.mix != null) "Mix" else "Playlist • ${p.owner}") {
                        TextButton({ Hidden.set(listOf(p.id), false) }) { Text("Unhide", color = Accent) }
                    }
                }
            }
        }
        item { BottomSpacer() }
    }
}

/** The figures on top of a cache page. */
@Composable
private fun CacheSummary(title: String, about: String) =
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .06f)).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(about, style = MaterialTheme.typography.bodySmall)
    }

@Composable
fun SongCacheScreen() {
    val songs by produceState<List<Streamed>?>(null) { value = withContext(Dispatchers.IO) { App.it.offline.streamed() } }
    LazyColumn(Modifier.fillMaxSize()) {
        item { BackTitle("Song cache") }
        songs?.let { list ->
            item {
                CacheSummary(
                    "${list.size} songs • ${formatBytes(list.sumOf { it.bytes })}",
                    "Last heard first. Songs you play stay here so replays cost no data, and the next ones are fetched ahead " +
                        "so they start at once; ${list.count { !it.whole }} are only that first bit. On Wi-Fi, songs above download quality " +
                        "are swapped for a copy at it, so more fit.",
                )
            }
            items(list, key = { it.key }) { CachedSong(it) }
        }
        item { BottomSpacer() }
    }
}

@Composable
private fun CachedSong(c: Streamed) {
    // One small request per song; the answer is kept in saved info like any other.
    val song by produceState<Song?>(null, c.id) { value = runCatching { Song.from(App.it.api.call("getSong", "id" to c.id).getJSONObject("song")) }.getOrNull() }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(song?.coverArt, 120, Modifier.size(48.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(song?.title ?: c.id, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(song?.artist, (if (c.whole) "" else "First ") + formatBytes(c.bytes), quality(c.bitrate)).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ImageCacheScreen() {
    val ctx = LocalContext.current
    // Coil keeps each image as <key>.1 beside its headers in <key>.0; a cover's pointer to its picture has an empty .1.
    val files by produceState<List<File>?>(null) {
        value = withContext(Dispatchers.IO) {
            ctx.imageLoader.diskCache?.directory?.toFile()?.listFiles { f -> f.name.endsWith(".1") && f.length() > 0 }?.sortedByDescending { it.lastModified() }.orEmpty()
        }
    }
    LazyVerticalGrid(
        GridCells.Adaptive(96.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { BackTitle("Image cache") }
        files?.let { list ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(bottom = 8.dp)) {
                    CacheSummary("${list.size} images • ${formatBytes(list.sumOf { it.length() })}", "Covers and artist photos, newest first, up to 512 MB.")
                }
            }
            items(list, key = { it.name }) { f ->
                AsyncImage(f, null, Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(Tile), contentScale = ContentScale.Crop)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { BottomSpacer() }
    }
}

private val savedKinds = mapOf(
    "album" to "Album", "artist" to "Artist", "playlist" to "Playlist", "song" to "Song", "playlists" to "Your playlists",
    "albumList2" to "Album shelf", "artists" to "All artists", "starred2" to "Liked", "searchResult3" to "Search",
    "genres" to "Genres", "songsByGenre" to "Genre", "similarSongs2" to "Similar songs", "topSongs" to "Top songs",
    "artistInfo2" to "Artist info", "lyricsList" to "Lyrics", "randomSongs" to "Random songs", "mix" to "Today's mix",
)

/** What a saved server answer is about: its kind, and the name of the thing or a count of what's in it. */
private fun describeSaved(f: File): Pair<String, String?> = runCatching {
    val r = JSONObject(f.readText()).getJSONObject("subsonic-response")
    val k = r.keys().asSequence().first { r.opt(it) is JSONObject }
    val o = r.getJSONObject(k)
    (savedKinds[k] ?: k) to (o.s("name") ?: o.s("title")
        ?: o.keys().asSequence().mapNotNull { a -> o.optJSONArray(a)?.let { "${it.length()} × $a" } }.joinToString().ifEmpty { null })
}.getOrDefault("Unreadable" to null)

@Composable
fun SavedInfoScreen() {
    val files by produceState<List<File>?>(null) { value = withContext(Dispatchers.IO) { App.it.api.savedFiles() } }
    var open by remember { mutableStateOf<File?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { BackTitle("Saved info") }
        files?.let { list ->
            item {
                CacheSummary(
                    "${list.size} answers • ${formatBytes(list.sumOf { it.length() })}",
                    "The server's last answers, so pages open at once and work offline. Newest first; tap one to read it.",
                )
            }
            items(list, key = { it.name }) { f ->
                val about by produceState<Pair<String, String?>?>(null, f) { value = withContext(Dispatchers.IO) { describeSaved(f) } }
                SettingRow(
                    about?.let { it.second ?: it.first } ?: "…",
                    listOfNotNull(about?.first?.takeIf { about?.second != null }, formatBytes(f.length()), DateUtils.getRelativeTimeSpanString(f.lastModified()))
                        .joinToString(" • "),
                    { open = f },
                )
            }
        }
        item { BottomSpacer() }
    }
    open?.let { f ->
        val text by produceState<String?>(null, f) {
            value = withContext(Dispatchers.IO) { runCatching { JSONObject(f.readText()).toString(2) }.getOrElse { "Couldn't read it" }.take(50_000) }
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = null },
            containerColor = Elevated,
            title = { Text(describeSaved(f).let { it.second ?: it.first }, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Text(
                    text ?: "…", Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp, color = Sub,
                )
            },
            confirmButton = { TextButton({ open = null }) { Text("Close", color = Accent) } },
        )
    }
}
