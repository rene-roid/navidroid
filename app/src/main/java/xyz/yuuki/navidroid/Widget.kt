package xyz.yuuki.navidroid

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable as tap
import androidx.compose.foundation.layout.Column as UiColumn
import androidx.compose.foundation.layout.Row as UiRow
import androidx.compose.foundation.layout.Spacer as UiSpacer
import androidx.compose.foundation.layout.fillMaxSize as uiFillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth as uiFillMaxWidth
import androidx.compose.foundation.layout.padding as uiPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text as UiText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import xyz.yuuki.navidroid.ui.Accent
import xyz.yuuki.navidroid.ui.Bg
import xyz.yuuki.navidroid.ui.NavidroidTheme
import xyz.yuuki.navidroid.ui.OnAccent
import xyz.yuuki.navidroid.ui.Sub
import xyz.yuuki.navidroid.ui.Surface
import xyz.yuuki.navidroid.ui.coverRequest
import java.io.File
import androidx.media3.session.R as Media3R

/** What the widgets show, kept on disk so they can still draw it once the process is gone. */
data class NowPlaying(
    val id: String,
    val title: String,
    val artist: String,
    val from: String?,
    /** A 300 px copy of the cover in filesDir. */
    val cover: String?,
    val isPlaying: Boolean,
    /** The cover's dominant colour, 0 without one. */
    val color: Int,
) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("title", title).put("artist", artist).put("from", from).put("cover", cover)
        .put("isPlaying", isPlaying).put("color", color)

    companion object {
        fun from(o: JSONObject) = NowPlaying(o.getString("id"), o.getString("title"), o.getString("artist"), o.s("from"), o.s("cover"), o.optBoolean("isPlaying"), o.optInt("color"))
    }
}

/** Asks the service to end up playing, which a start in the foreground has to. */
const val EXTRA_PLAY = "play"

private class Shown(val np: NowPlaying, val cover: Bitmap?)

private val shown = MutableStateFlow<Shown?>(null)
private val Context.snapshotFile get() = File(filesDir, "widget.json")

/** Keeps the widgets on the current song: updates them when the song or the play button changes, never for position. */
class WidgetFeed(private val app: App, private val player: Player, private val scope: CoroutineScope) : Player.Listener {
    private var job: Job? = null

    override fun onEvents(p: Player, events: Player.Events) {
        if (!events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAYLIST_METADATA_CHANGED)) return
        val song = player.currentMediaItem?.song ?: return
        val from = player.playlistMetadata.title?.toString() ?: song.album
        // Like the notification's button, so buffering between songs doesn't flip it.
        val playing = !Util.shouldShowPlayButton(player)
        job?.cancel()
        job = scope.launch {
            val old = shown.value
            val next = if (old?.np?.id == song.id) Shown(old.np.copy(from = from, isPlaying = playing), old.cover) else {
                val cover = song.coverArt?.let { cover(it) }
                val color = cover?.let { Palette.from(it).generate().getDominantColor(0) } ?: 0
                Shown(NowPlaying(song.id, song.title, song.artist, from, cover?.let { app.coverFile.path }, playing, color), cover)
            }
            if (next.np == shown.value?.np) return@launch
            // A newer event cancels this one; past here it has to finish or the file and the widgets fall behind the state.
            withContext(NonCancellable) {
                shown.value = next
                withContext(Dispatchers.IO) { app.snapshotFile.writeText(next.np.toJson().toString()) }
                NowPlayingWidget().updateAll(app)
            }
        }
    }

    private val Context.coverFile get() = File(filesDir, "widget_cover.jpg")

    /** The same request as the notification's art, so it comes out of memory; squared and shrunk for RemoteViews. */
    private suspend fun cover(id: String): Bitmap? = withContext(Dispatchers.IO) {
        val req = coverRequest(app, id, 600).memoryCacheKey("notif:$id").allowHardware(false).build()
        val full = (app.imageLoader.execute(req) as? SuccessResult)?.image?.toBitmap() ?: return@withContext null
        val side = minOf(full.width, full.height)
        val small = Bitmap.createBitmap(full, (full.width - side) / 2, (full.height - side) / 2, side, side).scale(300, 300)
        val tmp = File(app.filesDir, "widget_cover.tmp")
        tmp.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        tmp.renameTo(app.coverFile)
        small
    }
}

private fun load(ctx: Context): Shown? = runCatching {
    // A process that just started isn't playing, whatever was last written; the service says so itself when it is.
    // ponytail: a widget drawn before a crash or force stop keeps its pause button until the app next runs.
    val np = NowPlaying.from(JSONObject(ctx.snapshotFile.readText())).copy(isPlaying = false)
    Shown(np, np.cover?.let(BitmapFactory::decodeFile))
}.getOrNull()

private val SMALL = DpSize(100.dp, 100.dp)
private val WIDE = DpSize(200.dp, 48.dp)
private val LARGE = DpSize(270.dp, 130.dp)
private val TALL = DpSize(200.dp, 230.dp)
private val BG = intPreferencesKey("bg")
private val CONTROLS = booleanPreferencesKey("controls")
private val backgrounds = listOf("Cover colour", "Dark", "Translucent")

class NowPlayingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (shown.value == null) shown.compareAndSet(null, withContext(Dispatchers.IO) { load(context) })
        provideContent {
            // Collected rather than read once: a running session recomposes on update() without coming back through here.
            val s by shown.collectAsState()
            val prefs = currentState<Preferences>()
            Content(s, prefs[BG] ?: 0, prefs[CONTROLS] ?: true)
        }
    }
}

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NowPlayingWidget()
}

@Composable
private fun Content(s: Shown?, bg: Int, controls: Boolean) {
    val np = s?.np
    val cover = s?.cover?.let { ImageProvider(it) }
    val color = when (bg) {
        1 -> Bg
        2 -> Color(0x99000000)
        else -> np?.color?.takeIf { it != 0 }?.let { Color(ColorUtils.blendARGB(it, AndroidColor.BLACK, .45f)) } ?: Surface
    }
    val size = LocalSize.current
    Box(GlanceModifier.fillMaxSize().shape(color, null).clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
        when {
            np == null -> Row(GlanceModifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.mipmap.ic_launcher), null, GlanceModifier.size(40.dp))
                Text(LocalContext.current.getString(R.string.widget_empty), GlanceModifier.padding(start = 10.dp), TextStyle(ColorProvider(Color.White), 14.sp))
            }
            size == SMALL -> Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                if (cover != null) Image(cover, np.title, GlanceModifier.fillMaxSize(), ContentScale.Crop)
                else Titles(np, GlanceModifier.fillMaxSize().padding(12.dp))
                if (controls) PlayPause(np, 44.dp, GlanceModifier.padding(10.dp))
            }
            size == WIDE -> Row(GlanceModifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Fit with a free width keeps it square at whatever height the row gets.
                if (cover != null) Image(cover, null, GlanceModifier.fillMaxHeight().cornerRadius(8.dp))
                Titles(np, GlanceModifier.defaultWeight().padding(horizontal = 10.dp))
                if (controls) Controls(np, 32.dp)
            }
            size == LARGE -> Row(GlanceModifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // A fixed size: this layout also gets heights where a cover as tall as the widget would leave the text no room.
                if (cover != null) Image(cover, null, GlanceModifier.size(106.dp).cornerRadius(12.dp))
                Column(GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                    Titles(np, from = true)
                    if (controls) {
                        Spacer(GlanceModifier.size(8.dp))
                        Controls(np, 36.dp)
                    }
                }
            }
            else -> Column(GlanceModifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (cover != null) Image(cover, null, GlanceModifier.defaultWeight().fillMaxWidth().cornerRadius(12.dp))
                Titles(np, GlanceModifier.fillMaxWidth().padding(vertical = 8.dp), from = true)
                if (controls) Controls(np, 40.dp)
            }
        }
    }
}

@Composable
private fun Titles(np: NowPlaying, modifier: GlanceModifier = GlanceModifier, from: Boolean = false) = Column(modifier) {
    if (from && np.from != null) Text("PLAYING FROM ${np.from.uppercase()}", style = TextStyle(ColorProvider(Sub), 10.sp, FontWeight.Medium), maxLines = 1)
    Text(np.title, style = TextStyle(ColorProvider(Color.White), 15.sp, FontWeight.Bold), maxLines = 1)
    Text(np.artist, style = TextStyle(ColorProvider(Sub), 13.sp), maxLines = 1)
}

@Composable
private fun Controls(np: NowPlaying, size: Dp) = Row(verticalAlignment = Alignment.CenterVertically) {
    val ctx = LocalContext.current
    val white = ColorFilter.tint(ColorProvider(Color.White))
    Image(ImageProvider(Media3R.drawable.media3_icon_previous), "Previous", GlanceModifier.size(size).padding(4.dp).clickable(press(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS, np.isPlaying)), colorFilter = white)
    PlayPause(np, size, GlanceModifier.padding(horizontal = 6.dp))
    Image(ImageProvider(Media3R.drawable.media3_icon_next), "Next", GlanceModifier.size(size).padding(4.dp).clickable(press(ctx, KeyEvent.KEYCODE_MEDIA_NEXT, np.isPlaying)), colorFilter = white)
}

@Composable
private fun PlayPause(np: NowPlaying, size: Dp, modifier: GlanceModifier = GlanceModifier) = Box(modifier) {
    val key = if (np.isPlaying) KeyEvent.KEYCODE_MEDIA_PAUSE else KeyEvent.KEYCODE_MEDIA_PLAY
    Box(GlanceModifier.size(size).shape(Accent, size / 2).clickable(press(LocalContext.current, key, np.isPlaying)), contentAlignment = Alignment.Center) {
        Image(
            ImageProvider(if (np.isPlaying) Media3R.drawable.media3_icon_pause else Media3R.drawable.media3_icon_play), if (np.isPlaying) "Pause" else "Play",
            GlanceModifier.size(size * .6f), colorFilter = ColorFilter.tint(ColorProvider(OnAccent)),
        )
    }
}

/**
 * A rounded background, with the system's widget radius when [radius] is null.
 * ponytail: before Android 12 nothing clips to it, so a cover filling the widget keeps square corners there; round the bitmap if that matters.
 */
private fun GlanceModifier.shape(color: Color, radius: Dp?) = when {
    Build.VERSION.SDK_INT < 31 -> background(ImageProvider(R.drawable.widget_bg), colorFilter = ColorFilter.tint(ColorProvider(color)))
    radius == null -> background(color).cornerRadius(android.R.dimen.system_app_widget_background_radius)
    else -> background(color).cornerRadius(radius)
}

/**
 * A media key sent straight to the service, the way Media3's own notification buttons do it, which also starts the service when
 * the process is gone. Explicit play and pause keys, because play/pause goes through the headset's press counting.
 */
private fun press(ctx: Context, key: Int, playing: Boolean) = actionStartService(
    // The data only tells the PendingIntents apart; extras don't.
    Intent(Intent.ACTION_MEDIA_BUTTON, Uri.parse("navidroid://widget/$key/$playing"), ctx, PlaybackService::class.java)
        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, key))
        .putExtra(EXTRA_PLAY, !playing),
    // While playing the service is up and in the foreground, and a plain start reaches it. Otherwise it may be gone, and from the
    // background only a foreground start is let through, which then has to play: skipping while paused starts the song.
    isForegroundService = !playing,
)

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        val widget = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if (widget == AppWidgetManager.INVALID_APPWIDGET_ID) return finish()
        val id = GlanceAppWidgetManager(this).getGlanceIdBy(widget)
        setContent {
            NavidroidTheme {
                var bg by remember { mutableIntStateOf(0) }
                var controls by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    val prefs = getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, id)
                    bg = prefs[BG] ?: 0
                    controls = prefs[CONTROLS] ?: true
                }
                UiColumn(Modifier.uiFillMaxSize().background(Bg).safeDrawingPadding().uiPadding(24.dp)) {
                    UiText("Now playing widget", style = MaterialTheme.typography.headlineMedium)
                    UiText("Background", Modifier.uiPadding(top = 24.dp, bottom = 4.dp), color = Sub, style = MaterialTheme.typography.titleSmall)
                    backgrounds.forEachIndexed { i, name ->
                        UiRow(Modifier.uiFillMaxWidth().tap { bg = i }, verticalAlignment = UiAlignment.CenterVertically) {
                            RadioButton(bg == i, onClick = { bg = i })
                            UiText(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    UiRow(Modifier.uiFillMaxWidth().uiPadding(top = 16.dp).tap { controls = !controls }, verticalAlignment = UiAlignment.CenterVertically) {
                        UiText("Show controls", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(controls, onCheckedChange = { controls = it })
                    }
                    UiSpacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                updateAppWidgetState(this@WidgetConfigActivity, id) {
                                    it[BG] = bg
                                    it[CONTROLS] = controls
                                }
                                NowPlayingWidget().update(this@WidgetConfigActivity, id)
                                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget))
                                finish()
                            }
                        },
                        Modifier.uiFillMaxWidth(),
                    ) { UiText("Done", style = MaterialTheme.typography.labelLarge, color = OnAccent) }
                }
            }
        }
    }
}
