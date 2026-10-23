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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
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
import xyz.yuuki.navidroid.Api
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.normalizeEndpoint
import xyz.yuuki.navidroid.song

val fieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface, unfocusedContainerColor = Surface, disabledContainerColor = Surface,
    focusedBorderColor = Accent, unfocusedBorderColor = Color.Transparent, cursorColor = Accent,
    focusedLabelColor = Accent, unfocusedLabelColor = Sub, focusedLeadingIconColor = Accent, unfocusedLeadingIconColor = Sub,
    focusedTrailingIconColor = Sub, unfocusedTrailingIconColor = Sub, focusedSupportingTextColor = Faint, unfocusedSupportingTextColor = Faint,
)

private val fieldShape = RoundedCornerShape(14.dp)

@Composable
fun LoginScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var servers by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSubmit = !busy && servers.isNotBlank() && user.isNotBlank() && pass.isNotEmpty()
    val spin by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)))

    fun submit() {
        if (!canSubmit) return
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
            App.it.api = Api(prefs, App.it.scope).apply { clearSaved() }
            App.it.scope.launch { App.it.api.pickFastest() }
            onDone()
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Box(
            Modifier.fillMaxWidth().height(400.dp)
                .background(Brush.radialGradient(listOf(Accent.copy(alpha = .35f), Accent.copy(alpha = .08f), Color.Transparent))),
        )
        Column(
            // Phone width on a tablet instead of fields across the whole screen.
            Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(200.dp)) {
                Image(painterResource(R.drawable.ic_record), null, Modifier.matchParentSize().graphicsLayer { rotationZ = spin })
                Image(painterResource(R.drawable.ic_play_label), null, Modifier.matchParentSize())
            }
            Text("Navidroid", style = MaterialTheme.typography.displaySmall, modifier = Modifier.offset(y = (-28).dp))
            Text(
                "Your whole library, streamed from your own Navidrome server.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Sub), textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-20).dp).padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    servers, { servers = it }, Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors, enabled = !busy,
                    label = { StateText("Server addresses") }, placeholder = { StateText("music.example.com\n192.168.1.10:4533") }, minLines = 2,
                    leadingIcon = { Icon(Icons.Rounded.Dns, null) },
                    supportingText = { StateText("One per line. The fastest one is used, and the app switches if it goes down.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                )
                OutlinedTextField(
                    user, { user = it }, Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
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
            }
            error?.let {
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(fieldShape).background(Red.copy(alpha = .14f)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = Red)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                ::submit, Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp), enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent, disabledContainerColor = Elevated, disabledContentColor = Faint),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = OnAccent, strokeWidth = 2.dp)
                else Text("Log in", color = LocalContentColor.current, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private val Red = Color(0xFFF15E6C)

/** Our typography bakes in text colors, which would override the field's focused/disabled colors. */
@Composable
private fun StateText(text: String) = Text(text, color = LocalContentColor.current)

/** A header and a card holding one group of settings; [action] sits at the header's end. */
@Composable
private fun Section(title: String, modifier: Modifier, action: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) = Column(modifier.padding(horizontal = 16.dp)) {
    Row(Modifier.padding(start = 4.dp, top = 20.dp, bottom = 6.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.W700, letterSpacing = 1.sp, color = Sub, modifier = Modifier.weight(1f))
        action()
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .06f)).padding(vertical = 4.dp), content = content)
}

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

private val storageColors = listOf(Color(0xFF509BF5), Color(0xFF8D67AB), Color(0xFF27856A), Color(0xFFF59B23))

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

private fun quality(br: Int) = qualities.firstOrNull { it.first == br }?.second ?: "$br kbps"

private val qualities = listOf(0 to "Original", 320 to "320 kbps", 256 to "256 kbps", 192 to "192 kbps", 160 to "160 kbps", 128 to "128 kbps", 96 to "96 kbps")

@Composable
private fun ChoiceRow(title: String, value: Int, options: List<Pair<Int, String>> = qualities, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(title, options.firstOrNull { it.first == value }?.second ?: "$value kbps", { open = true })
        DropdownMenu(open, { open = false }, containerColor = Elevated) {
            options.forEach { (v, label) ->
                DropdownMenuItem({ Text(label) }, { onChange(v); open = false }, trailingIcon = { if (v == value) Icon(Icons.Rounded.Check, null, tint = Accent) })
            }
        }
    }
}

@Composable
private fun NextArrow() = Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Sub)

@Composable
private fun AuroraSwitch(t: Toggle) = SettingRow(t.title, t.subtitle, { t.on = !t.on }) {
    Switch(t.on, { t.on = it }, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
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
    var autoplay by remember { mutableStateOf(prefs.autoplay) }
    val icon = remember { currentIcon(ctx) }
    var cleared by remember { mutableIntStateOf(0) }
    // Downloads, streamed songs, covers, saved info.
    val storage by produceState(listOf(0L, 0L, 0L, 0L), cleared) {
        value = withContext(Dispatchers.IO) { listOf(app.offline.bytesUsed(), app.offline.streamBytes(), ctx.imageLoader.diskCache?.size ?: 0L, app.api.savedBytes()) }
    }
    val traffic by produceState<Pair<Long?, Long?>>(null to null) { value = withContext(Dispatchers.IO) { monthTraffic(ctx) } }

    fun recheck() {
        checking = true
        scope.launch {
            app.api.pickFastest()
            active = app.api.active
            checking = false
        }
    }
    LaunchedEffect(Unit) { recheck() }
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)!!.use { i -> avatarFile.outputStream().use { i.copyTo(it) } } }
            }
            avatarVersion++
        }
    }

    val switchColors = SwitchDefaults.colors(checkedTrackColor = Accent)
    val wide = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Box(wide) { BackTitle("Settings") } }
        item {
            Section("Account", wide) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(48.dp) { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    Box(Modifier.weight(1f)) {
                        SettingRow(displayName(), "Logged in as ${prefs.username}") {
                            OutlinedButton(onLogout) { Text("Log out", color = Color.White) }
                        }
                    }
                }
            }
        }
        item {
            Section("Server", wide, action = {
                if (checking) CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), color = Accent, strokeWidth = 2.dp)
                else IconButton(::recheck) { Icon(Icons.Rounded.Refresh, "Test all") }
            }) {
                endpoints.forEach { ep ->
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
                        if (ep == active) Icon(Icons.Rounded.Check, "In use", tint = Accent)
                        if (endpoints.size > 1) IconButton({
                            endpoints = endpoints - ep
                            prefs.endpoints = endpoints
                            recheck()
                        }) { Icon(Icons.Rounded.Close, "Remove", tint = Sub) }
                    }
                }
                Row(Modifier.padding(start = 16.dp, end = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
        }
        item {
            Section("Playback", wide) {
                ChoiceRow("Streaming quality on Wi-Fi", wifi) { wifi = it; prefs.streamBitrateWifi = it }
                ChoiceRow("Streaming quality on mobile data", mobile) { mobile = it; prefs.streamBitrateMobile = it }
                SettingRow("Autoplay", "When the queue ends, keep playing songs like the last ones", { autoplay = !autoplay; prefs.autoplay = autoplay }) {
                    Switch(autoplay, { autoplay = it; prefs.autoplay = it }, colors = switchColors)
                }
            }
        }
        item {
            Section("Downloads", wide) {
                ChoiceRow("Download quality", dl) { dl = it; prefs.downloadBitrate = it; app.offline.sync() }
                SettingRow("Download on Wi-Fi only", "Pinned playlists wait for Wi-Fi", { wifiOnly = !wifiOnly; app.offline.setWifiOnly(wifiOnly) }) {
                    Switch(wifiOnly, { wifiOnly = it; app.offline.setWifiOnly(it) }, colors = switchColors)
                }
                SettingRow("Manage downloads", "See what's kept on this phone", { nav.go(Route.Downloads) }) { NextArrow() }
            }
        }
        item {
            Section("Personalise", wide) {
                SettingRow(
                    "Appearance", "${themeName()} theme • ${icons[icon].name} icon • Aurora ${if (auroraOn.on) "on" else "off"}", { nav.go(Route.Appearance) },
                ) {
                    LauncherIcon(icons[icon].res, 32.dp)
                    NextArrow()
                }
                SettingRow("Hidden playlists", Hidden.playlists.size.let { if (it == 0) "None hidden" else "$it hidden" }, { nav.go(Route.HiddenPlaylists) }) { NextArrow() }
            }
        }
        item {
            Section("Storage", wide) {
                fun clear(action: () -> Unit) {
                    scope.launch {
                        withContext(Dispatchers.IO) { action() }
                        cleared++
                    }
                }
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
            Section("Data used this month", wide) {
                val (wifiBytes, mobileBytes) = traffic
                SettingRow("Wi-Fi", "Streaming at ${quality(wifi)}") { Text(wifiBytes?.let(::formatBytes) ?: "—") }
                SettingRow("Mobile data", "Streaming at ${quality(mobile)}") { Text(mobileBytes?.let(::formatBytes) ?: "—") }
                Text(
                    "Counted by Android, so it can lag by an hour or two.", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
        item {
            Section("About", wide) {
                SettingRow("Navidroid", "Version ${remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }}")
            }
        }
        item { BottomSpacer() }
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
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .06f)).padding(16.dp)) {
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
    "artistInfo2" to "Artist info", "lyricsList" to "Lyrics", "randomSongs" to "Random songs",
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
