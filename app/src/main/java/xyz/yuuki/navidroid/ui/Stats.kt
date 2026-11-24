package xyz.yuuki.navidroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.Kind
import xyz.yuuki.navidroid.Ranked
import xyz.yuuki.navidroid.Stats

private val GRADIENTS = listOf(
    0xFFE8115B to 0xFF5038A0, 0xFF1E8A6E to 0xFF0D73EC, 0xFFF59B23 to 0xFFE8115B,
    0xFF7358FF to 0xFF1E3264, 0xFFAF2896 to 0xFFF59B23, 0xFF148A08 to 0xFF1E3264,
)

private fun brush(i: Int) = GRADIENTS[i % GRADIENTS.size].let { Brush.linearGradient(listOf(Color(it.first), Color(it.second))) }

private fun listened(minutes: Long) = when {
    minutes >= 60 * 24 -> "${minutes / (60 * 24)}d ${minutes / 60 % 24}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

@Composable
private fun StatCard(i: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(brush(i)).padding(20.dp)) { content() }

@Composable
private fun Big(value: String, label: String, modifier: Modifier = Modifier) = Column(modifier) {
    Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp, color = Color.White.copy(alpha = .8f))
    Text(value, autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(18.sp, 40.sp), fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun RankRow(rank: Int, cover: String?, title: String, sub: String, round: Boolean, onClick: () -> Unit) = androidx.compose.foundation.layout.Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text("$rank", Modifier.width(28.dp), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = .7f))
    Cover(cover, 120, Modifier.size(48.dp), if (round) CircleShape else RoundedCornerShape(6.dp))
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun plays(n: Int) = "$n play${if (n == 1) "" else "s"}"

@Composable
fun StatsScreen() {
    val nav = LocalNav.current
    val player = LocalPlayer.current
    val load = rememberLoad("stats") { App.it.api.stats() }
    val s: Stats? = load.data
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackTitle("Your stats") }
        if (s == null) item { if (load.error != null) ErrorText(load.error) else Loading() }
        else if (s.plays == 0) item { Text("Nothing played yet. Listen to some music and come back.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
        else {
            item {
                StatCard(0, Modifier.fillMaxWidth()) {
                    Big("%,d".format(s.plays), "Songs played")
                    Spacer(Modifier.height(12.dp))
                    Big(listened(s.minutes), "Time listened")
                }
            }
            if (s.genre != null || s.decade != null) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    s.genre?.let { StatCard(1, Modifier.weight(1f)) { Big(it, "Top genre") } }
                    s.decade?.let { StatCard(2, Modifier.weight(1f)) { Big("${it}s", "Top decade") } }
                }
            }
            if (s.artists.isNotEmpty()) item {
                StatCard(3, Modifier.fillMaxWidth()) {
                    Big("Top artists", "Most played")
                    s.artists.forEachIndexed { i, a -> RankRow(i + 1, a.coverArt, a.name, plays(a.plays), true) { nav.go(Route.Open(Kind.Artist, a.id)) } }
                }
            }
            if (s.albums.isNotEmpty()) item {
                StatCard(4, Modifier.fillMaxWidth()) {
                    Big("Top albums", "Most played")
                    s.albums.forEachIndexed { i, a: Ranked -> RankRow(i + 1, a.coverArt, a.name, plays(a.plays), false) { nav.go(Route.Open(Kind.Album, a.id)) } }
                }
            }
            if (s.songs.isNotEmpty()) item {
                StatCard(5, Modifier.fillMaxWidth()) {
                    Big("Top songs", "Most played")
                    s.songs.forEachIndexed { i, song -> RankRow(i + 1, song.coverArt, song.title, "${song.artist} • ${plays(song.playCount)}", false) { player?.play(s.songs, from = "Your top songs") } }
                }
            }
            if (s.discovered.isNotEmpty()) item {
                StatCard(1, Modifier.fillMaxWidth()) {
                    Big("${s.discovered.size}", "Albums discovered this year")
                    s.discovered.take(5).forEachIndexed { i, a -> RankRow(i + 1, a.coverArt, a.name, a.artist, false) { nav.go(Route.Open(Kind.Album, a.id)) } }
                }
            }
        }
        item { BottomSpacer() }
    }
}
