package xyz.yuuki.navidroid

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import xyz.yuuki.navidroid.ui.Accent
import xyz.yuuki.navidroid.ui.Bg
import xyz.yuuki.navidroid.ui.NavidroidTheme
import xyz.yuuki.navidroid.ui.OnAccent
import xyz.yuuki.navidroid.ui.Sub
import xyz.yuuki.navidroid.ui.TileCover

/** Picks what a playlist widget plays: Liked Songs, then the user's playlists, then the generated mixes. */
class PlaylistWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        val widget = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if (widget == AppWidgetManager.INVALID_APPWIDGET_ID) return finish()
        val id = GlanceAppWidgetManager(this).getGlanceIdBy(widget)
        setContent {
            NavidroidTheme {
                var choices by remember { mutableStateOf<List<Pair<String, List<Tile>>>?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                var saving by remember { mutableStateOf(false) }
                var tick by remember { mutableIntStateOf(0) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(tick) {
                    error = null
                    try {
                        val playlists = App.it.api.playlists().map(Playlist::tile)
                        choices = listOf(
                            "" to listOf(likedTile(null)),
                            "Your playlists" to playlists.filter { it.mix == null },
                            "Made for you" to playlists.filter { it.mix != null },
                        ).filter { it.second.isNotEmpty() }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.message ?: "Couldn't reach the server"
                    }
                }
                fun pick(t: Tile) {
                    saving = true
                    scope.launch {
                        val file = playlistCover(widget)
                        val saved = t.coverArt?.let { runCatching { saveCover(this@PlaylistWidgetConfigActivity, it, file) }.getOrDefault(false) } == true
                        if (!saved) file.delete()
                        updateAppWidgetState(this@PlaylistWidgetConfigActivity, id) {
                            it[PW_KIND] = t.kind.name
                            it[PW_ID] = t.id
                            it[PW_TITLE] = t.title
                            if (saved) it[PW_COVER] = file.path else it.remove(PW_COVER)
                            it[PW_COLOR] = t.mix?.let { m -> Color(m.color).toArgb() } ?: if (t.kind == Kind.Liked) 0xFF5038A0.toInt() else 0xFF1F1F1F.toInt()
                        }
                        PlaylistWidget().update(this@PlaylistWidgetConfigActivity, id)
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget))
                        finish()
                    }
                }
                Column(Modifier.fillMaxSize().background(Bg).safeDrawingPadding()) {
                    Text("Playlist widget", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineMedium)
                    when {
                        saving || (choices == null && error == null) -> Center { CircularProgressIndicator(color = Accent) }
                        choices == null -> Center {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Text(error!!, color = Sub, style = MaterialTheme.typography.bodyMedium)
                                Button({ tick++ }, Modifier.padding(top = 16.dp)) { Text("Retry", color = OnAccent) }
                            }
                        }
                        else -> LazyColumn(Modifier.fillMaxSize()) {
                            choices!!.forEach { (header, tiles) ->
                                if (header.isNotEmpty()) item(header) { Text(header, Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp), color = Sub, style = MaterialTheme.typography.titleSmall) }
                                items(tiles, key = { "${it.kind}:${it.id}" }) { t ->
                                    Row(Modifier.fillMaxWidth().clickable { pick(t) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(44.dp), 120)
                                        Text(t.title, Modifier.padding(start = 16.dp), maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Center(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
