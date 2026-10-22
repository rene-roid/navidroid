package xyz.yuuki.navidroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.TextUnit
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
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

    /** Wants to play but is still fetching audio. */
    var loading by mutableStateOf(false)
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
        loading = c.playWhenReady && c.playbackState == Player.STATE_BUFFERING
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
        Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(58.dp)
            .background(Brush.horizontalGradient(listOf(lerp(color, Bg, .2f), lerp(color, Bg, .6f))), RoundedCornerShape(18.dp))
            .glass(RoundedCornerShape(18.dp)).swipeToSkip(p, song).clickable(onClick = onOpen),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(song.coverArt, 120, Modifier.size(42.dp), RoundedCornerShape(12.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ Likes.toggle(song) }) {
                val liked = Likes.isLiked(song)
                if (liked) LikedHeart(Modifier.size(24.dp), "Unlike") else Icon(Icons.Rounded.FavoriteBorder, "Like", tint = Color.White)
            }
            IconButton(p::toggle) {
                if (p.loading) CircularProgressIndicator(Modifier.size(24.dp), Color.White, 2.5.dp)
                else Icon(if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/pause", Modifier.size(30.dp), tint = Color.White)
            }
        }
        Progress(p, song, pos, Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp))
    }
}

/** Full-screen player in portrait. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlaying(p: PlayerState, onClose: () -> Unit) {
    val song = p.song ?: return
    val nav = LocalNav.current
    val goToArtist = rememberGoToArtist(onClose)
    val color = rememberCoverColor(song.coverArt)
    val lyrics = rememberLoad("lyrics:${song.id}") { App.it.api.lyrics(song.id) }
    val pos = rememberPosition(p, 200)

    Box(Modifier.fillMaxSize().then(rememberPullToClose(onClose))) {
        Aurora(song.coverArt, null, modifier = Modifier.fillMaxSize(), plain = color)
        LazyColumn(Modifier.align(Alignment.TopCenter).widthIn(max = 640.dp).fillMaxSize().statusBarsPadding()) {
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
                SongInfo(song) { goToArtist(song) }
            }
            item("seek") { Seek(p, song, pos) }
            item("controls") { Controls(p) }
            lyrics.data?.let { l -> item("lyrics") { LyricsCard(p, l, pos, color) } }
            if (p.upNext.isNotEmpty()) item("next") {
                Text("Up next", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 24.dp, top = 28.dp, bottom = 8.dp))
            }
            itemsIndexed(p.upNext, key = { _, (i, s) -> "$i:${s.id}" }) { _, (i, s) -> QueueRow(p, i, s, Modifier.animateItem(), start = 24.dp) }
            item("bottom") {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun QueueRow(p: PlayerState, i: Int, s: Song, modifier: Modifier, start: Dp, cover: Dp = 44.dp) = Row(
    modifier.fillMaxWidth().clickable { p.c.seekTo(i, 0); p.c.play() }.padding(start = start, end = 8.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Cover(s.coverArt, 120, Modifier.size(cover))
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
        Text(s.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(s.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    IconButton({ p.c.removeMediaItem(i) }) { Icon(Icons.Rounded.Close, "Remove", tint = Sub) }
}

@Composable
private fun SongInfo(song: Song, onArtist: () -> Unit) = Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
        Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, modifier = Modifier.basicMarquee())
        Text(
            song.artist, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .7f), maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(enabled = song.artistId != null, onClick = onArtist),
        )
    }
    IconButton({ Likes.toggle(song) }) {
        val liked = Likes.isLiked(song)
        if (liked) LikedHeart(Modifier.size(28.dp), "Unlike") else Icon(Icons.Rounded.FavoriteBorder, "Like", Modifier.size(28.dp), tint = Color.White)
    }
}

/** Dragging down past the top of the list moves the screen with the finger; let go far or fast enough and it closes. */
@Composable
private fun rememberPullToClose(onClose: () -> Unit): Modifier {
    var offset by remember { mutableFloatStateOf(0f) }
    val far = with(LocalDensity.current) { 120.dp.toPx() }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (offset <= 0f || available.y >= 0f) return Offset.Zero
                val used = maxOf(available.y, -offset)
                offset += used
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                offset += available.y
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset <= 0f) return Velocity.Zero
                if (offset > far || available.y > 2000f) onClose() else animate(offset, 0f) { v, _ -> offset = v }
                return available
            }
        }
    }
    return Modifier.nestedScroll(connection).graphicsLayer { translationY = offset }
}

/** Album art you can swipe sideways to skip, like the big streaming apps. */
@Composable
private fun SwipeArt(p: PlayerState, song: Song) = Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
    Cover(
        song.coverArt, 600,
        Modifier.fillMaxWidth().aspectRatio(1f).swipeToSkip(p, song).shadow(24.dp, RoundedCornerShape(20.dp)),
        RoundedCornerShape(20.dp),
    )
}

/** Drag sideways to go to the next or previous song; the content follows the finger and fades. */
@Composable
private fun Modifier.swipeToSkip(p: PlayerState, song: Song): Modifier {
    val dx = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(song.id) { dx.snapTo(0f) }
    return graphicsLayer {
        translationX = dx.value
        alpha = 1f - (abs(dx.value) / size.width).coerceIn(0f, .6f)
    }.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = {
                val w = size.width.toFloat()
                scope.launch {
                    when {
                        dx.value < -w / 4 && p.c.hasNextMediaItem() -> { dx.animateTo(-w); p.c.seekToNextMediaItem() }
                        dx.value > w / 4 && p.c.hasPreviousMediaItem() -> { dx.animateTo(w); p.c.seekToPreviousMediaItem() }
                        else -> dx.animateTo(0f)
                    }
                }
            },
            onDragCancel = { scope.launch { dx.animateTo(0f) } },
            onHorizontalDrag = { change, amount ->
                change.consume()
                scope.launch { dx.snapTo(dx.value + amount) }
            },
        )
    }
}

/**
 * Landscape player: a floating card beside the page. The cover sits next to the title, the controls under it,
 * and lyrics or the queue fill the rest. On a tablet the tabs move to the top and the controls to the bottom, where
 * they stay, and a song without lyrics shows its cover big in their place.
 */
@Composable
fun DockedPlayer(p: PlayerState, modifier: Modifier = Modifier) {
    val song = p.song ?: return
    val goToArtist = rememberGoToArtist()
    val color = rememberCoverColor(song.coverArt)
    val lyrics = rememberLoad("lyrics:${song.id}") { App.it.api.lyrics(song.id) }
    val pos = rememberPosition(p, 200)
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val tablet = isTablet()
    // A phone in landscape is too short for the full player and a panel, so a panel opens over a compact player.
    var open by rememberSaveable { mutableStateOf(false) }
    val bigCover = tablet && !showQueue && lyrics.data == null && !lyrics.loading

    val head = @Composable {
        Row(Modifier.padding(start = 14.dp, top = if (tablet) 12.dp else 14.dp, end = 4.dp, bottom = 4.dp)) {
            AnimatedVisibility(!bigCover) {
                val size = if (tablet) 72.dp else 88.dp
                Cover(song.coverArt, 300, Modifier.padding(end = 14.dp).size(size).swipeToSkip(p, song).shadow(12.dp, RoundedCornerShape(10.dp)), RoundedCornerShape(10.dp))
            }
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(
                    "PLAYING FROM ${(p.from ?: song.album).uppercase()}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                    letterSpacing = 1.sp, color = Color.White.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, fontSize = 18.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp).basicMarquee())
                Text(
                    song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = song.artistId != null) { goToArtist(song) },
                )
            }
            // Side by side on a tablet, where the head is only as tall as its small cover.
            val actions = @Composable {
                HeartButton(Likes.isLiked(song), 24.dp) { Likes.toggle(song) }
                SongMenu(song, Color.White)
            }
            if (tablet) Row { actions() } else Column { actions() }
        }
    }
    val tabs = @Composable {
        Row(
            Modifier.padding(start = 12.dp, end = 12.dp, top = if (tablet) 12.dp else 8.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .25f)).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            @Composable
            fun seg(label: String, on: Boolean, onClick: () -> Unit) = Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) Color.White.copy(alpha = .14f) else Color.Transparent)
                    .clickable(onClick = onClick).padding(vertical = if (tablet) 8.dp else 6.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W700, color = if (on) Color.White else Sub) }
            @Composable
            fun pick(label: String, queue: Boolean) {
                val on = showQueue == queue && (tablet || open)
                seg(label, on) {
                    showQueue = queue
                    open = !on
                }
            }
            pick("Lyrics", false)
            pick("Up next", true)
        }
    }
    // Rows fade out at the top and bottom edges instead of being cut off.
    val fade = Modifier.fillMaxWidth().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .08f to Color.Black, .88f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    }
    val panel = @Composable { m: Modifier ->
        val l = lyrics.data
        when {
            showQueue -> LazyColumn(m.then(fade), contentPadding = PaddingValues(vertical = 8.dp)) {
                if (p.upNext.isEmpty()) item { Hint("Nothing queued") }
                itemsIndexed(p.upNext, key = { _, (i, s) -> "$i:${s.id}" }) { _, (i, s) -> QueueRow(p, i, s, Modifier.animateItem(), start = 16.dp, cover = 40.dp) }
            }
            bigCover -> BoxWithConstraints(m.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight) - 32.dp
                Cover(song.coverArt, 600, Modifier.size(side).swipeToSkip(p, song).shadow(24.dp, RoundedCornerShape(14.dp)), RoundedCornerShape(14.dp))
            }
            l == null -> Box(m.then(fade)) { Hint(if (lyrics.loading) "" else "No lyrics for this song") }
            else -> LyricLines(p, l, pos, m.then(fade), if (tablet) 22.sp else 18.sp)
        }
    }

    Column(
        modifier.clip(RoundedCornerShape(if (tablet) 22.dp else 18.dp))
            .background(Brush.verticalGradient(0f to color, .55f to lerp(Bg, color, .25f), 1f to Surface)),
    ) {
        if (tablet) {
            tabs()
            head()
            panel(Modifier.weight(1f))
            Seek(p, song, pos, inset = 12.dp)
            Controls(p, play = 60.dp, inset = 8.dp, vertical = 0.dp)
            Spacer(Modifier.height(8.dp))
        } else if (open) {
            tabs()
            Row(Modifier.padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(song.coverArt, 120, Modifier.size(48.dp).swipeToSkip(p, song), RoundedCornerShape(8.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.basicMarquee())
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                PlayPause(p, 40.dp)
                SkipButton(p, next = true, 30.dp)
            }
            Progress(p, song, pos, Modifier.padding(horizontal = 14.dp))
            panel(Modifier.weight(1f))
        } else {
            tabs()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                head()
                Seek(p, song, pos, inset = 16.dp, inline = true)
                Controls(p, play = 52.dp, inset = 8.dp, vertical = 0.dp)
            }
        }
    }
}

/** Tablet in portrait: the player as a bar under the page. Tap it for the full-screen player. */
@Composable
fun PlayerBar(p: PlayerState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val song = p.song ?: return
    val color = rememberCoverColor(song.coverArt, Elevated)
    val pos = rememberPosition(p, 500)
    Box(
        modifier.clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(0f to color, .5f to Surface))
            .clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(song.coverArt, 120, Modifier.size(60.dp).swipeToSkip(p, song), RoundedCornerShape(10.dp))
            Column(Modifier.width(190.dp).padding(start = 14.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            HeartButton(Likes.isLiked(song), 24.dp) { Likes.toggle(song) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Controls(p, play = 48.dp, inset = 0.dp, vertical = 0.dp, modifier = Modifier.widthIn(max = 340.dp)) }
        }
        Progress(p, song, pos, Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp))
    }
}

/** A thin, untouchable progress line along the bottom of the mini player and the bar. */
@Composable
private fun Progress(p: PlayerState, song: Song, pos: Long, modifier: Modifier) {
    val dur = (p.c.duration.takeIf { it > 0 } ?: (song.duration * 1000L)).toFloat()
    Box(modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = .25f))) {
        Box(Modifier.fillMaxWidth((pos / dur).coerceIn(0f, 1f)).height(2.dp).background(Color.White))
    }
}

@Composable
private fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = Sub, modifier = Modifier.padding(16.dp))

@Composable
private fun LyricLines(p: PlayerState, l: xyz.yuuki.navidroid.Lyrics, pos: Long, modifier: Modifier, size: TextUnit = 18.sp) {
    val current = if (l.synced) l.lines.indexOfLast { it.ms <= pos } else -1
    val list = rememberLazyListState()
    LaunchedEffect(current) { if (current >= 0) list.animateScrollToItem((current - 1).coerceAtLeast(0)) }
    LazyColumn(modifier, state = list, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
        itemsIndexed(l.lines) { i, line ->
            Text(
                line.text.ifBlank { "♪" },
                fontSize = size, lineHeight = size * 1.33f, fontWeight = FontWeight.W700,
                color = when {
                    !l.synced || i == current -> Color.White
                    i < current -> Color.White.copy(alpha = .65f)
                    else -> Color.White.copy(alpha = .4f)
                },
                modifier = Modifier.fillMaxWidth().clickable(enabled = l.synced) { p.c.seekTo(line.ms) }.padding(vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Seek(p: PlayerState, song: Song, pos: Long, inset: Dp = 16.dp, inline: Boolean = false) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val dur = (p.c.duration.takeIf { it > 0 } ?: (song.duration * 1000L)).toFloat()
    val value = dragging ?: (pos / dur).coerceIn(0f, 1f)
    val slider = @Composable { m: Modifier ->
        Slider(
            value = value,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { p.c.seekTo((it * dur).toLong()) }
                dragging = null
            },
            modifier = m,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = .25f)),
            thumb = { Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White)) },
            track = { st ->
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .25f))) {
                    Box(Modifier.fillMaxWidth(st.value).height(4.dp).background(Color.White))
                }
            },
        )
    }
    val elapsed = @Composable { Text(formatTime((value * dur).toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .7f)) }
    val total = @Composable { Text(formatTime(dur.toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .7f)) }
    // Inline puts the times either side of the bar, for the short landscape card.
    if (inline) Row(Modifier.padding(horizontal = inset), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        elapsed()
        slider(Modifier.weight(1f))
        total()
    } else Column(Modifier.padding(horizontal = inset)) {
        slider(Modifier)
        Row(Modifier.padding(horizontal = 8.dp)) {
            elapsed()
            Spacer(Modifier.weight(1f))
            total()
        }
    }
}

@Composable
private fun Controls(p: PlayerState, play: Dp = 68.dp, inset: Dp = 16.dp, vertical: Dp = 8.dp, modifier: Modifier = Modifier) = Row(
    modifier.fillMaxWidth().padding(horizontal = inset, vertical = vertical),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    ShuffleButton(p)
    SkipButton(p, next = false, 40.dp)
    PlayPause(p, play)
    SkipButton(p, next = true, 40.dp)
    RepeatButton(p)
}

@Composable
private fun ShuffleButton(p: PlayerState) = IconButton({ p.c.shuffleModeEnabled = !p.shuffle }) {
    Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (p.shuffle) Accent else Color.White)
}

@Composable
private fun RepeatButton(p: PlayerState) = IconButton(p::cycleRepeat) {
    Icon(
        if (p.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat",
        tint = if (p.repeat == Player.REPEAT_MODE_OFF) Color.White else Accent,
    )
}

@Composable
private fun SkipButton(p: PlayerState, next: Boolean, icon: Dp) = IconButton({ if (next) p.c.seekToNext() else p.c.seekToPrevious() }, Modifier.size(56.dp)) {
    Icon(if (next) Icons.Rounded.SkipNext else Icons.Rounded.SkipPrevious, if (next) "Next" else "Previous", Modifier.size(icon))
}

@Composable
private fun PlayPause(p: PlayerState, size: Dp) = Box(Modifier.size(size).clip(CircleShape).background(Color.White).clickable(onClick = p::toggle), contentAlignment = Alignment.Center) {
    if (p.loading) CircularProgressIndicator(Modifier.size(size * .45f), Color.Black, 3.dp)
    else Icon(if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/pause", Modifier.size(size * .6f), tint = Color.Black)
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
