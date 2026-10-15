package xyz.yuuki.navidroid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.Kind
import xyz.yuuki.navidroid.Song
import xyz.yuuki.navidroid.song
import xyz.yuuki.navidroid.toMediaItem
import kotlin.math.abs

/** Compose-observable view of the playback service's MediaController. */
class PlayerState(val c: MediaController) : Player.Listener {
    var song by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(Player.REPEAT_MODE_OFF)
    var from by mutableStateOf<String?>(null)
    var index by mutableStateOf(0)

    /** Timeline indices in play order after the current one (follows shuffle). */
    var upNext by mutableStateOf<List<Pair<Int, Song>>>(emptyList())

    init {
        c.addListener(this)
        sync(null)
    }

    override fun onEvents(player: Player, events: Player.Events) = sync(events)

    private fun sync(events: Player.Events?) {
        isPlaying = c.isPlaying
        shuffle = c.shuffleModeEnabled
        repeat = c.repeatMode
        from = c.playlistMetadata.title?.toString()
        index = c.currentMediaItemIndex
        if (events != null && !events.containsAny(
                Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
            )
        ) return
        song = c.currentMediaItem?.song
        val t = c.currentTimeline
        upNext = if (t.isEmpty) emptyList() else buildList {
            var i = t.getNextWindowIndex(c.currentMediaItemIndex, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
            while (i != C.INDEX_UNSET && size < 200) {
                c.getMediaItemAt(i).song?.let { add(i to it) }
                i = t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
            }
        }
    }

    fun play(songs: List<Song>, start: Int = 0, shuffled: Boolean = false, from: String? = null) {
        if (songs.isEmpty()) return
        c.shuffleModeEnabled = shuffled
        c.setMediaItems(songs.map { it.toMediaItem() }, if (shuffled) songs.indices.random() else start, 0)
        c.playlistMetadata = MediaMetadata.Builder().setTitle(from).build()
        c.prepare()
        c.play()
    }

    fun playNext(songs: List<Song>) {
        if (c.mediaItemCount == 0) play(songs) else c.addMediaItems(c.currentMediaItemIndex + 1, songs.map(Song::toMediaItem))
    }

    fun enqueue(songs: List<Song>) {
        if (c.mediaItemCount == 0) play(songs) else c.addMediaItems(songs.map(Song::toMediaItem))
    }

    fun toggle() = if (c.isPlaying) c.pause() else {
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun cycleRepeat() {
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}

@Composable
private fun rememberPosition(p: PlayerState, everyMs: Long = 250): Long {
    val pos by produceState(p.c.currentPosition, p.song?.id) {
        while (true) {
            value = p.c.currentPosition
            delay(everyMs)
        }
    }
    return pos
}

@Composable
fun MiniPlayer(p: PlayerState, onOpen: () -> Unit) {
    val song = p.song ?: return
    val color = rememberCoverColor(song.coverArt, Elevated)
    val pos = rememberPosition(p, 500)
    Box(
        Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(58.dp).clip(RoundedCornerShape(6.dp))
            .background(color).clickable(onClick = onOpen),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(song.coverArt, 120, Modifier.size(42.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ Likes.toggle(song) }) {
                val liked = Likes.isLiked(song)
                Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (liked) Green else Color.White)
            }
            IconButton(p::toggle) {
                Icon(if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/pause", Modifier.size(30.dp), tint = Color.White)
            }
        }
        val dur = song.duration * 1000f
        Box(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp).fillMaxWidth().height(2.dp)
                .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = .25f)),
        ) {
            Box(Modifier.fillMaxWidth(if (dur > 0) (pos / dur).coerceIn(0f, 1f) else 0f).height(2.dp).background(Color.White))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlaying(p: PlayerState, onClose: () -> Unit) {
    val song = p.song ?: return
    val nav = LocalNav.current
    val color = rememberCoverColor(song.coverArt)
    val lyrics = rememberLoad("lyrics:${song.id}") { App.it.api.lyrics(song.id) }
    val pos = rememberPosition(p, 200)

    Box(Modifier.fillMaxSize().background(Bg).background(Brush.verticalGradient(listOf(color, lerp(color, Bg, .6f), Bg)))) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding()) {
            item("top") {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close", Modifier.size(32.dp)) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PLAYING FROM", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = Color.White.copy(alpha = .8f))
                        Text(p.from ?: song.album, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SongMenu(song, Color.White)
                }
            }
            item("art") { SwipeArt(p, song) }
            item("info") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, modifier = Modifier.basicMarquee())
                        Text(
                            song.artist, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .7f), maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = song.artistId != null) {
                                onClose()
                                nav.go(Route.Open(Kind.Artist, song.artistId!!))
                            },
                        )
                    }
                    IconButton({ Likes.toggle(song) }) {
                        val liked = Likes.isLiked(song)
                        Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", Modifier.size(28.dp), tint = if (liked) Green else Color.White)
                    }
                }
            }
            item("seek") { Seek(p, song, pos) }
            item("controls") { Controls(p) }
            lyrics.data?.let { l -> item("lyrics") { LyricsCard(p, l, pos, color) } }
            if (p.upNext.isNotEmpty()) item("next") {
                Text("Up next", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 24.dp, top = 28.dp, bottom = 8.dp))
            }
            itemsIndexed(p.upNext, key = { _, (i, s) -> "$i:${s.id}" }) { _, (i, s) ->
                Row(
                    Modifier.fillMaxWidth().clickable { p.c.seekTo(i, 0); p.c.play() }.padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp).animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(s.coverArt, 120, Modifier.size(44.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(s.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton({ p.c.removeMediaItem(i) }) { Icon(Icons.Rounded.Close, "Remove", tint = Sub) }
                }
            }
            item("bottom") {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Album art you can swipe sideways to skip, like the big streaming apps. */
@Composable
private fun SwipeArt(p: PlayerState, song: Song) {
    val dx = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(song.id) { dx.snapTo(0f) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Cover(
            song.coverArt, 600,
            Modifier.fillMaxWidth().aspectRatio(1f)
                .graphicsLayer {
                    translationX = dx.value
                    alpha = 1f - (abs(dx.value) / size.width).coerceIn(0f, .6f)
                }
                .shadow(24.dp, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val w = size.width.toFloat()
                            scope.launch {
                                when {
                                    dx.value < -w / 4 && p.c.hasNextMediaItem() -> { dx.animateTo(-w); p.c.seekToNextMediaItem() }
                                    dx.value > w / 4 -> { dx.animateTo(w); p.c.seekToPrevious() }
                                    else -> dx.animateTo(0f)
                                }
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            scope.launch { dx.snapTo(dx.value + amount) }
                        },
                    )
                },
            RoundedCornerShape(8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Seek(p: PlayerState, song: Song, pos: Long) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val dur = (p.c.duration.takeIf { it > 0 } ?: (song.duration * 1000L)).toFloat()
    val value = dragging ?: (pos / dur).coerceIn(0f, 1f)
    Column(Modifier.padding(horizontal = 16.dp)) {
        Slider(
            value = value,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { p.c.seekTo((it * dur).toLong()) }
                dragging = null
            },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = .25f)),
            thumb = { Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White)) },
            track = { st ->
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .25f))) {
                    Box(Modifier.fillMaxWidth(st.value).height(4.dp).background(Color.White))
                }
            },
        )
        Row(Modifier.padding(horizontal = 8.dp)) {
            Text(formatTime((value * dur).toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .7f))
            Spacer(Modifier.weight(1f))
            Text(formatTime(dur.toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .7f))
        }
    }
}

@Composable
private fun Controls(p: PlayerState) = Row(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    IconButton({ p.c.shuffleModeEnabled = !p.shuffle }) {
        Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (p.shuffle) Green else Color.White)
    }
    IconButton({ p.c.seekToPrevious() }, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(40.dp)) }
    Box(Modifier.size(68.dp).clip(CircleShape).background(Color.White).clickable(onClick = p::toggle), contentAlignment = Alignment.Center) {
        Icon(if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/pause", Modifier.size(40.dp), tint = Color.Black)
    }
    IconButton({ p.c.seekToNext() }, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(40.dp)) }
    IconButton(p::cycleRepeat) {
        Icon(
            if (p.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat",
            tint = if (p.repeat == Player.REPEAT_MODE_OFF) Color.White else Green,
        )
    }
}

@Composable
private fun LyricsCard(p: PlayerState, l: xyz.yuuki.navidroid.Lyrics, pos: Long, color: Color) {
    val current = if (l.synced) l.lines.indexOfLast { it.ms <= pos } else -1
    val list = rememberLazyListState()
    LaunchedEffect(current) { if (current >= 0) list.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 20.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(lerp(color, Color.White, .12f)).padding(16.dp),
    ) {
        Text("Lyrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.height(320.dp), state = list, userScrollEnabled = true) {
            itemsIndexed(l.lines) { i, line ->
                Text(
                    line.text.ifBlank { "♪" },
                    fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.W700, textAlign = TextAlign.Start,
                    color = when {
                        !l.synced -> Color.White
                        i == current -> Color.White
                        i < current -> Color.White.copy(alpha = .55f)
                        else -> Color.Black.copy(alpha = .55f)
                    },
                    modifier = Modifier.fillMaxWidth().clickable(enabled = l.synced) { p.c.seekTo(line.ms) }.padding(vertical = 5.dp),
                )
            }
        }
    }
}
