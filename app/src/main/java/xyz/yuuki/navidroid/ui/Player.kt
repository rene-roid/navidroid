package xyz.yuuki.navidroid.ui

import android.os.Bundle
import android.os.SystemClock
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import xyz.yuuki.navidroid.Playback
import xyz.yuuki.navidroid.SleepTimer
import xyz.yuuki.navidroid.moved
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.TextStyle
import xyz.yuuki.navidroid.auto
import xyz.yuuki.navidroid.jam
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
import xyz.yuuki.navidroid.Jam
import androidx.compose.runtime.collectAsState
import xyz.yuuki.navidroid.Kind
import xyz.yuuki.navidroid.Song
import xyz.yuuki.navidroid.song
import xyz.yuuki.navidroid.toMediaItem
import kotlin.math.abs

/** A song waiting in the queue at timeline [index]. [auto]: the song it was picked to follow, when the app queued it and not the user. [jam]: the Jam guest who added it. */
data class Queued(val index: Int, val song: Song, val auto: String?, val jam: String? = null)

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

    /** What plays after the current song, in play order (follows shuffle). */
    var upNext by mutableStateOf<List<Queued>>(emptyList())

    /** What this queue played before the current song, the latest first. */
    var history by mutableStateOf<List<Queued>>(emptyList())
    var speed by mutableFloatStateOf(1f)

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
        speed = c.playbackParameters.speed
        if (events != null && !events.containsAny(
                Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
            )
        ) return
        song = c.currentMediaItem?.song
        val t = c.currentTimeline
        fun from(next: Boolean) = if (t.isEmpty) emptyList() else buildList {
            fun step(i: Int) = if (next) t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled) else t.getPreviousWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
            var i = step(c.currentMediaItemIndex)
            while (i != C.INDEX_UNSET && size < 200) {
                c.getMediaItemAt(i).run { song?.let { add(Queued(i, it, auto, jam)) } }
                i = step(i)
            }
        }
        upNext = from(next = true)
        history = from(next = false)
    }

    /** Has the service drag [q] to where [to] plays; only it can reorder a shuffled queue. */
    fun move(q: Queued, to: Queued, auto: String?) {
        c.sendCustomCommand(Playback.MOVE, Bundle().apply { putInt("from", q.index); putInt("to", to.index); putString("auto", auto) })
    }

    fun play(songs: List<Song>, start: Int = 0, shuffled: Boolean = false, from: String? = null, radio: Boolean = false) {
        if (songs.isEmpty()) return
        c.shuffleModeEnabled = shuffled
        // Before the songs, so the service knows what kind of queue they are when they arrive.
        c.playlistMetadata = MediaMetadata.Builder().setTitle(from).setExtras(Bundle().apply { putBoolean("radio", radio) }).build()
        c.setMediaItems(songs.map { it.toMediaItem() }, if (shuffled) songs.indices.random() else start, 0)
        c.prepare()
        c.play()
    }

    /** Plays [seed] at once; the service then queues songs like it behind it, and drops them if something else got started meanwhile. */
    fun playRadio(seed: Song, from: String = "${seed.title} Radio") = play(listOf(seed), from = from, radio = true)

    /** Highest index first, so the rest keep theirs while going. */
    fun remove(items: List<Queued>) = items.map { it.index }.sortedDescending().forEach(c::removeMediaItem)

    fun queue() = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).song }

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
            .background(color).swipeToSkip(p, song).clickable(onClick = onOpen),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(song.coverArt, 120, Modifier.size(42.dp))
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
    val large = largeControls
    // A song without lyrics shows its cover as usual.
    val lyricsFirst = nowPlayingStyle == 2 && (lyrics.data != null || lyrics.loading)

    val list = rememberLazyListState()
    val queue = rememberQueueUi(p, list)

    Box(Modifier.fillMaxSize().then(rememberPullToClose(onClose))) {
        Aurora(song.coverArt, null, modifier = Modifier.fillMaxSize(), plain = color)
        LazyColumn(Modifier.align(Alignment.TopCenter).widthIn(max = 640.dp).fillMaxSize().statusBarsPadding(), list) {
            item("top") {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close", Modifier.size(32.dp)) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PLAYING FROM", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = Color.White.copy(alpha = .8f))
                        Text(p.from ?: song.album, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayerMenu(p, song)
                }
            }
            if (lyricsFirst) {
                item("info") { SongInfo(song, large, cover = true) { goToArtist(song) } }
                item("words") {
                    val area = Modifier.fillMaxWidth().fillParentMaxHeight(.5f).padding(horizontal = 8.dp)
                    lyrics.data?.let { LyricLines(p, it, pos, area, 24.sp) } ?: Spacer(area)
                }
            } else {
                item("art") { SwipeArt(p, song, vinyl = nowPlayingStyle == 1) }
                item("info") { SongInfo(song, large) { goToArtist(song) } }
            }
            item("seek") { Seek(p, song, pos, large = large) }
            item("controls") { if (large) Controls(p, play = 92.dp, skip = 56.dp, side = 34.dp) else Controls(p) }
            item("jam") { JamPill(Modifier.padding(top = 4.dp)) }
            if (!lyricsFirst) lyrics.data?.let { l -> item("lyrics") { LyricsCard(p, l, pos, color) } }
            if (p.upNext.isNotEmpty() || p.history.isNotEmpty()) item("next") { QueueHeader(queue, Modifier.padding(start = 24.dp, top = 28.dp, end = 8.dp), MaterialTheme.typography.titleLarge) }
            queue(queue, start = 24.dp)
            item("bottom") {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** "Up next" or "History", the button that swaps them, and what can be done with the whole queue. */
@Composable
private fun QueueHeader(ui: QueueUi, modifier: Modifier, style: TextStyle) {
    val p = ui.p
    var open by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf<List<Song>?>(null) }
    saving?.let { NewPlaylist(it) { saving = null } }
    var jam by remember { mutableStateOf(false) }
    if (jam) JamSheet { jam = false }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (ui.history) "History" else "Up next", style = style, modifier = Modifier.weight(1f))
        IconButton({ ui.history = !ui.history }) { Icon(Icons.Rounded.History, if (ui.history) "Show up next" else "Show history", tint = if (ui.history) Accent else Sub) }
        Box {
            IconButton({ open = true }) { Icon(Icons.Rounded.MoreVert, "Queue options", tint = Sub) }
            DropdownMenu(open, { open = false }, containerColor = Elevated) {
                DropdownMenuItem({ Text("Clear queue") }, { open = false; p.remove(p.upNext) }, enabled = p.upNext.isNotEmpty())
                DropdownMenuItem({ Text("Save queue as playlist") }, { open = false; saving = p.queue() })
                DropdownMenuItem({ Text(if (Jam.session.collectAsState().value == null) "Start a Jam" else "Jam settings") }, { open = false; jam = true })
            }
        }
    }
}

/** What a queue list keeps between frames: whether it shows History, and the row being dragged up or down Up next. */
private class QueueUi(val p: PlayerState, val list: LazyListState) {
    var history by mutableStateOf(false)

    /** Up next as dragged so far. It stays up after the drop until the player reports the move, so rows don't jump back in between. */
    var dragged by mutableStateOf<List<Queued>?>(null)

    /** The timeline index of the row under the finger. */
    var dragging by mutableStateOf<Int?>(null)

    /** Where the finger holds that row, from the top of the list. */
    var top by mutableFloatStateOf(0f)
    var height = 0

    val rows get() = dragged ?: p.upNext

    fun start(q: Queued, key: String) {
        val me = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        dragged = p.upNext
        dragging = q.index
        top = me.offset.toFloat()
        height = me.size
    }

    fun offsetOf(key: String) = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.offset

    /** The row trades places with a neighbour once its middle is past the neighbour's. */
    fun settle() {
        val rows = dragged ?: return
        val at = rows.indexOfFirst { it.index == dragging }.takeIf { it >= 0 } ?: return
        val keys = queueKeys(rows)
        val items = list.layoutInfo.visibleItemsInfo
        val mid = top + height / 2
        fun middle(n: Int) = items.firstOrNull { it.key == keys.getOrNull(n) }?.let { it.offset + it.size / 2 }
        val to = when {
            middle(at + 1)?.let { mid > it } == true -> at + 1
            middle(at - 1)?.let { mid < it } == true -> at - 1
            else -> return
        }
        // The list holds on to its first visible row, and would scroll away with it when that's one of the two.
        list.requestScrollToItem(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
        dragged = draggedTo(rows, at, to)
    }

    fun drop() {
        val q = p.upNext.firstOrNull { it.index == dragging }
        val moved = dragged?.firstOrNull { it.index == dragging }
        val to = p.upNext.getOrNull(dragged.orEmpty().indexOf(moved))
        dragging = null
        if (q == null || moved == null || to == null || to == q) dragged = null else p.move(q, to, moved.auto)
    }
}

@Composable
private fun rememberQueueUi(p: PlayerState, list: LazyListState): QueueUi {
    val ui = remember(p, list) { QueueUi(p, list) }
    // The queue changed: the move arrived, or something else made the dragged copy stale.
    LaunchedEffect(p.upNext) {
        ui.dragged = null
        ui.dragging = null
    }
    // Holding a row past either end scrolls the list under it.
    LaunchedEffect(ui.dragging != null) {
        while (ui.dragging != null) {
            withFrameNanos {}
            val view = list.layoutInfo
            val past = minOf(ui.top - view.viewportStartOffset, 0f) + maxOf(ui.top + ui.height - view.viewportEndOffset, 0f)
            if (past != 0f) {
                list.scrollBy(past.coerceIn(-30f, 30f))
                ui.settle()
            }
        }
    }
    return ui
}

/** Up next after the song at [from] is dragged to [to]. It takes the marker of the song now before it, so what autoplay added stays in one run, and a song dragged above that run becomes the user's. */
fun draggedTo(rows: List<Queued>, from: Int, to: Int): List<Queued> =
    rows.moved(from, to).let { l -> l.mapIndexed { n, q -> if (n == to) q.copy(auto = l.getOrNull(n - 1)?.auto) else q } }

/** A key per row that a song keeps when the queue reorders: its id, numbered when it's queued more than once. */
fun queueKeys(rows: List<Queued>): List<String> {
    val seen = HashMap<String, Int>()
    return rows.map { "${it.song.id}#${seen.merge(it.song.id, 1, Int::plus)}" }
}

/** The queue's rows, with a line above the first of each run of songs the app added by itself; or the songs already played. */
private fun LazyListScope.queue(ui: QueueUi, start: Dp, cover: Dp = 44.dp) {
    val p = ui.p
    if (ui.history) {
        if (p.history.isEmpty()) item { Hint("Nothing played yet") }
        items(p.history, key = { "h${it.index}" }) { q -> QueueRow(p, q, start, cover, Modifier.animateItem()) {} }
        return
    }
    val rows = ui.rows
    val keys = queueKeys(rows)
    itemsIndexed(rows, key = { n, _ -> keys[n] }) { n, q ->
        val key = keys[n]
        val held = q.index == ui.dragging
        val haptics = LocalHapticFeedback.current
        // What the gesture reads when it starts, long after it was set up.
        val row by rememberUpdatedState(q to key)
        Column(if (held) Modifier.zIndex(1f).graphicsLayer { translationY = ui.offsetOf(key)?.let { ui.top - it } ?: 0f } else Modifier.animateItem()) {
            if (q.auto != null && q.auto != rows.getOrNull(n - 1)?.auto) Row(Modifier.fillMaxWidth().padding(start = start, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (q.auto.isEmpty()) "Autoplay" else "Autoplay · songs like ${q.auto}", style = MaterialTheme.typography.labelLarge, color = Sub,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                TextButton({ p.remove(p.upNext.filter { it.auto != null }) }) { Text("Clear", color = Sub) }
            }
            QueueRow(p, q, start, cover, if (held) Modifier.background(Color.White.copy(alpha = .12f)) else Modifier) {
                IconButton({ p.c.removeMediaItem(q.index) }) { Icon(Icons.Rounded.Close, "Remove", tint = Sub) }
                Icon(
                    Icons.Rounded.DragHandle, "Hold and drag to reorder", tint = Sub,
                    modifier = Modifier.size(40.dp).padding(8.dp).pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                ui.start(row.first, row.second)
                            },
                            onDragEnd = ui::drop, onDragCancel = ui::drop,
                        ) { change, by ->
                            change.consume()
                            if (ui.dragging != null) {
                                ui.top += by.y
                                ui.settle()
                            }
                        }
                    },
                )
            }
        }
    }
}

/** One song of the queue; tapping it jumps there. */
@Composable
private fun QueueRow(p: PlayerState, q: Queued, start: Dp, cover: Dp, modifier: Modifier, trailing: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().clickable { p.c.seekTo(q.index, 0); p.c.play() }.padding(start = start, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(q.song.coverArt, 120, Modifier.size(cover))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(q.song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (q.jam != null) "${q.song.artist} · Added by ${q.jam}" else q.song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

private val SLEEP_MINUTES = listOf(5, 15, 30, 45, 60)
private val SPEEDS = listOf(.5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun speedLabel(x: Float) = "${x.toString().removeSuffix(".0")}x"

/** How long until the sleep timer stops the music, in ms; null when none is set. */
@Composable
private fun rememberSleepLeft(p: PlayerState): Long? {
    val timer = Playback.sleep.collectAsState().value ?: return null
    return produceState(0L, timer, p.song?.id, p.speed) {
        while (true) {
            value = timer.at?.let { it - SystemClock.elapsedRealtime() } ?: ((p.c.duration.takeIf { it > 0 } ?: ((p.song?.duration ?: 0) * 1000L)) - p.c.currentPosition).div(p.speed).toLong()
            delay(500)
        }
    }.value
}

/** The song's menu, with the sleep timer and the playback speed after it. */
@Composable
private fun PlayerMenu(p: PlayerState, song: Song) {
    var sleep by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(false) }
    val left = rememberSleepLeft(p)
    SongMenu(song, Color.White) { close ->
        HorizontalDivider()
        DropdownMenuItem({ Text(if (left == null) "Sleep timer" else "Sleep timer · ${formatTime(left)} left") }, { close(); sleep = true }, leadingIcon = { Icon(Icons.Rounded.Bedtime, null, tint = if (left == null) Color.White else Accent) })
        DropdownMenuItem({ Text("Speed · ${speedLabel(p.speed)}") }, { close(); speed = true }, leadingIcon = { Icon(Icons.Rounded.Speed, null, tint = if (p.speed == 1f) Color.White else Accent) })
    }
    if (sleep) {
        val timer = Playback.sleep.collectAsState().value
        PickSheet(
            "Sleep timer", if (left == null) "Stops the music by itself, fading out at the end" else "Stops in ${formatTime(left)}",
            SLEEP_MINUTES.map { if (it == 60) "1 hour" else "$it minutes" } + "End of this song" + "Off",
            when {
                timer == null -> SLEEP_MINUTES.size + 1
                timer.at == null -> SLEEP_MINUTES.size
                else -> -1
            },
            { sleep = false },
        ) { n -> Playback.sleep.value = SLEEP_MINUTES.getOrNull(n)?.let { SleepTimer(SystemClock.elapsedRealtime() + it * 60_000L) } ?: SleepTimer(null).takeIf { n == SLEEP_MINUTES.size } }
    }
    if (speed) PickSheet("Playback speed", "Until the app closes", SPEEDS.map(::speedLabel), SPEEDS.indexOf(p.speed), { speed = false }) { p.c.setPlaybackSpeed(SPEEDS[it]) }
}

/** A sheet to pick one of [options] from; [picked] has the tick. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickSheet(title: String, note: String, options: List<String>, picked: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) = ModalBottomSheet(onDismiss, containerColor = Elevated) {
    Column(Modifier.padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
        Text(note, style = MaterialTheme.typography.bodySmall, color = Sub, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp))
        options.forEachIndexed { n, option ->
            Row(Modifier.fillMaxWidth().clickable { onPick(n); onDismiss() }.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(option, style = MaterialTheme.typography.bodyLarge, color = if (n == picked) Accent else Color.White, modifier = Modifier.weight(1f))
                if (n == picked) Icon(Icons.Rounded.Check, null, tint = Accent)
            }
        }
    }
}

@Composable
private fun SongInfo(song: Song, large: Boolean = false, cover: Boolean = false, onArtist: () -> Unit) = Row(
    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = if (cover) 12.dp else 0.dp), verticalAlignment = Alignment.CenterVertically,
) {
    if (cover) {
        Cover(song.coverArt, 300, Modifier.size(if (large) 80.dp else 64.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.width(14.dp))
    }
    Column(Modifier.weight(1f)) {
        Text(song.title, style = MaterialTheme.typography.run { if (large) displaySmall else headlineSmall }, maxLines = 1, modifier = Modifier.basicMarquee())
        Text(
            song.artist, style = MaterialTheme.typography.run { if (large) titleLarge else bodyLarge }, color = Color.White.copy(alpha = .7f), maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(enabled = song.artistId != null, onClick = onArtist),
        )
    }
    val heart = if (large) 38.dp else 28.dp
    IconButton({ Likes.toggle(song) }, Modifier.size(heart + 20.dp)) {
        val liked = Likes.isLiked(song)
        if (liked) LikedHeart(Modifier.size(heart), "Unlike") else Icon(Icons.Rounded.FavoriteBorder, "Like", Modifier.size(heart), tint = Color.White)
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
private fun SwipeArt(p: PlayerState, song: Song, vinyl: Boolean = false) = Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
    val square = Modifier.fillMaxWidth().aspectRatio(1f).swipeToSkip(p, song)
    if (vinyl) Record(song.coverArt, p.isPlaying, square.shadow(24.dp, CircleShape))
    else Cover(song.coverArt, 600, square.shadow(24.dp, RoundedCornerShape(8.dp)), RoundedCornerShape(8.dp))
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
    val list = rememberLazyListState()
    val queue = rememberQueueUi(p, list)
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
                PlayerMenu(p, song)
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
            showQueue -> LazyColumn(m.then(fade), list, contentPadding = PaddingValues(vertical = 8.dp)) {
                item("next") { QueueHeader(queue, Modifier.padding(start = 16.dp, end = 4.dp), MaterialTheme.typography.titleSmall) }
                if (p.upNext.isEmpty() && !queue.history) item { Hint("Nothing queued") }
                queue(queue, start = 16.dp, cover = 40.dp)
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
private fun Seek(p: PlayerState, song: Song, pos: Long, inset: Dp = 16.dp, inline: Boolean = false, large: Boolean = false) {
    val time = MaterialTheme.typography.run { if (large) titleMedium else labelSmall }
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
            thumb = { Box(Modifier.size(if (large) 22.dp else 14.dp).clip(CircleShape).background(Color.White)) },
            track = { st ->
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .25f))) {
                    Box(Modifier.fillMaxWidth(st.value).height(4.dp).background(Color.White))
                }
            },
        )
    }
    val elapsed = @Composable { Text(formatTime((value * dur).toLong()), style = time, color = Color.White.copy(alpha = .7f)) }
    val total = @Composable { Text(formatTime(dur.toLong()), style = time, color = Color.White.copy(alpha = .7f)) }
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
private fun Controls(
    p: PlayerState, play: Dp = 68.dp, inset: Dp = 16.dp, vertical: Dp = 8.dp, modifier: Modifier = Modifier, skip: Dp = 40.dp, side: Dp = 24.dp,
) = Row(
    modifier.fillMaxWidth().padding(horizontal = inset, vertical = vertical),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    ShuffleButton(p, side)
    SkipButton(p, next = false, skip)
    PlayPause(p, play)
    SkipButton(p, next = true, skip)
    RepeatButton(p, side)
}

@Composable
private fun ShuffleButton(p: PlayerState, size: Dp = 24.dp) = IconButton({ p.c.shuffleModeEnabled = !p.shuffle }, Modifier.size(size + 24.dp)) {
    Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(size), tint = if (p.shuffle) Accent else Color.White)
}

@Composable
private fun RepeatButton(p: PlayerState, size: Dp = 24.dp) = IconButton(p::cycleRepeat, Modifier.size(size + 24.dp)) {
    Icon(
        if (p.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", Modifier.size(size),
        tint = if (p.repeat == Player.REPEAT_MODE_OFF) Color.White else Accent,
    )
}

@Composable
private fun SkipButton(p: PlayerState, next: Boolean, icon: Dp) = IconButton({ if (next) p.c.seekToNext() else p.c.seekToPrevious() }, Modifier.size(maxOf(56.dp, icon + 16.dp))) {
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
