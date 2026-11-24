package xyz.yuuki.navidroid

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.compose.runtime.remember
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.imageLoader
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.yuuki.navidroid.ui.Accent
import xyz.yuuki.navidroid.ui.OnAccent
import xyz.yuuki.navidroid.ui.Surface
import xyz.yuuki.navidroid.ui.coverRequest
import java.io.File
import androidx.media3.session.R as Media3R

/** The collection a playlist widget plays, per widget in its Glance state. [COVER] is a file in filesDir, saved when it was picked. */
internal val PW_KIND = stringPreferencesKey("pw_kind")
internal val PW_ID = stringPreferencesKey("pw_id")
internal val PW_TITLE = stringPreferencesKey("pw_title")
internal val PW_COVER = stringPreferencesKey("pw_cover")
internal val PW_COLOR = intPreferencesKey("pw_color")

internal fun Context.playlistCover(widget: Int) = File(filesDir, "playlist_widget_$widget.jpg")

/** Saves a square 300 px copy of the cover for RemoteViews; false when it can't be fetched. */
internal suspend fun saveCover(ctx: Context, cover: String, to: File): Boolean = withContext(Dispatchers.IO) {
    val full = (ctx.imageLoader.execute(coverRequest(ctx, cover, 600).allowHardware(false).build()) as? SuccessResult)?.image?.toBitmap() ?: return@withContext false
    val side = minOf(full.width, full.height)
    val small = Bitmap.createBitmap(full, (full.width - side) / 2, (full.height - side) / 2, side, side).scale(300, 300)
    to.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    true
}

class PlaylistWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Single

    // Only reads what the config activity saved: no network here, the cover is a file.
    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        // The state is read inside the composition: the session that drew the empty widget is still running when the choice is saved,
        // and update() only recomposes it, so a value read once up here would stay the empty one.
        val prefs = currentState<Preferences>()
        val path = prefs[PW_COVER]
        // A 300 px file, small enough to decode in place; keyed on the pick since reconfiguring rewrites the same path.
        val cover = remember(path, prefs[PW_ID]) { path?.let(BitmapFactory::decodeFile) }
        PlaylistContent(context, prefs, cover)
    }
}

class PlaylistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = PlaylistWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { context.playlistCover(it).delete() }
    }
}

@androidx.compose.runtime.Composable
private fun PlaylistContent(ctx: Context, prefs: Preferences, cover: Bitmap?) {
    val kind = prefs[PW_KIND]?.let { runCatching { Kind.valueOf(it) }.getOrNull() }
    val id = prefs[PW_ID]
    val title = prefs[PW_TITLE]
    if (kind == null || id == null || title == null) {
        Box(GlanceModifier.fillMaxSize().shape(Surface, null).clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
            Text("Tap to open navidroid", GlanceModifier.padding(12.dp), TextStyle(ColorProvider(Color.White), 14.sp))
        }
        return
    }
    // Different data per target: the PendingIntents would otherwise be one, and extras don't tell them apart.
    fun go(a: ShortcutAction) = actionStartActivity(a.intent(ctx).setData(Uri.parse("navidroid://widget/${Uri.encode(a.spec)}")))
    Box(
        GlanceModifier.fillMaxSize().shape(Color(prefs[PW_COLOR] ?: Surface.toArgb()), null).clickable(go(ShortcutAction.Open(kind, id))),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (cover != null) Image(ImageProvider(cover), null, GlanceModifier.fillMaxSize(), ContentScale.Crop)
        Row(GlanceModifier.fillMaxWidth().background(Color(0xB3000000)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, GlanceModifier.defaultWeight(), TextStyle(ColorProvider(Color.White), 13.sp, FontWeight.Bold), maxLines = 2)
            Box(GlanceModifier.size(40.dp).shape(Accent, 20.dp).clickable(go(ShortcutAction.Play(kind, id))), contentAlignment = Alignment.Center) {
                Image(ImageProvider(Media3R.drawable.media3_icon_play), "Play", GlanceModifier.size(24.dp), colorFilter = ColorFilter.tint(ColorProvider(OnAccent)))
            }
        }
    }
}
