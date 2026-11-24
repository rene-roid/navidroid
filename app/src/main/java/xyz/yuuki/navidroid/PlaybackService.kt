package xyz.yuuki.navidroid

import android.app.PendingIntent
import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.IntentCompat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import xyz.yuuki.navidroid.ui.Likes
import xyz.yuuki.navidroid.ui.coverRequest
import java.io.IOException
import kotlin.random.Random

/** [auto]: the song this one was picked to follow, when the app queued it (radio, autoplay) and not the user. [jam]: the Jam guest who added it. */
fun Song.toMediaItem(auto: String? = null, jam: String? = null): MediaItem {
    val app = App.it
    val art = coverArt?.let { id -> app.offline.coverFile(id).takeIf { it.exists() }?.let(Uri::fromFile) ?: Uri.parse(app.api.coverUrl(id, 600)) }
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(songUri(id))
        .setCustomCacheKey(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                // Some system UIs (HyperOS, Bluetooth displays) only read the display fields.
                .setDisplayTitle(title)
                .setSubtitle(artist)
                .setAlbumTitle(album)
                .setArtworkUri(art)
                .setDurationMs(duration * 1000L)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(Bundle().apply { putString("song", toJson().toString()); putString("auto", auto); putString("jam", jam) })
                .build(),
        )
        .build()
}

val MediaItem.song: Song? get() = mediaMetadata.extras?.getString("song")?.let { Song.from(JSONObject(it)) }
val MediaItem.auto: String? get() = mediaMetadata.extras?.getString("auto")
val MediaItem.jam: String? get() = mediaMetadata.extras?.getString("jam")

/** When the music stops by itself. [at]: the SystemClock.elapsedRealtime to stop at, or null to stop when the current song ends. */
data class SleepTimer(val at: Long?)

/** What the screens set and the playback service acts on, so it goes on without them. */
object Playback {
    val sleep = MutableStateFlow<SleepTimer?>(null)
    val gain by lazy { MutableStateFlow(GainMode.entries.getOrElse(App.it.prefs.replayGain) { GainMode.Off }) }

    /** Likes or unlikes the current song; what the heart in the notification and in the car sends. */
    val LIKE = SessionCommand("like", Bundle.EMPTY)

    /** Drags the song at timeline index "from" to where the one at "to" plays, and marks it "auto". */
    val MOVE = SessionCommand("move", Bundle.EMPTY)

    fun setGain(mode: GainMode) {
        gain.value = mode
        App.it.prefs.replayGain = mode.ordinal
    }
}

const val SLEEP_FADE_MS = 10_000L

/** How loud to play with [leftMs] to go before a sleep timer stops the music: it fades out over the last ten seconds. */
fun sleepFade(leftMs: Long) = (leftMs.toFloat() / SLEEP_FADE_MS).coerceIn(0f, 1f)

/** This list after its item at [from] is dragged to [to]. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().apply { add(to, removeAt(from)) }

/**
 * An order to play songs by [artists] in, as indices into it: shuffled, but with no artist twice in a row wherever
 * the songs allow it, and not opening with the artist that plays just before ([after]).
 */
fun smartShuffle(artists: List<String>, rnd: Random = Random, after: String? = null): List<Int> {
    val pool = artists.indices.shuffled(rnd).toMutableList()
    val left = artists.groupingBy { it }.eachCount().toMutableMap()
    var last = after
    return List(artists.size) {
        // An artist with more than half of what's left has to go now, or two of theirs end up together later.
        val must = left.entries.firstOrNull { it.value * 2 > pool.size && it.key != last }?.key
        val at = (if (must != null) pool.indexOfFirst { artists[it] == must } else pool.indexOfFirst { artists[it] != last }).coerceAtLeast(0)
        pool.removeAt(at).also { i ->
            last = artists[i]
            left[artists[i]] = left.getValue(artists[i]) - 1
        }
    }
}

/** Timeline indices in the order they play, which shuffle makes its own. */
fun Player.playOrder(): List<Int> {
    val t = currentTimeline
    return generateSequence(t.getFirstWindowIndex(shuffleModeEnabled).takeIf { it != C.INDEX_UNSET }) { i ->
        t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, shuffleModeEnabled).takeIf { it != C.INDEX_UNSET }
    }.toList()
}

private fun <T> CoroutineScope.future(block: suspend () -> T): ListenableFuture<T> =
    SettableFuture.create<T>().also { f -> launch { runCatching { block() }.fold(f::set, f::setException) } }

class PlaybackService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    /** The two things that turn the player down: the current song's ReplayGain and a sleep timer running out. */
    private var loudness: ReplayGain? = null
    private var fade = 1f
    private var liked: Boolean? = null
    private val scope = MainScope()
    private val queueFile by lazy { App.it.dir("queue.json") }
    private val buttons = Handler(Looper.getMainLooper())
    private var presses = 0
    private var restoring: Job? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as App
        // The aurora listens to the decoded audio on its way to the speaker.
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context).setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(Levels))).build()
        }
        player = ExoPlayer.Builder(this, renderers)
            // Transcoded streams have no length; seek them by bitrate instead.
            .setMediaSourceFactory(DefaultMediaSourceFactory(app.offline.playback, DefaultExtractorsFactory().setConstantBitrateSeekingAlwaysEnabled(true)))
            // Start after half a second of audio instead of 2.5 s; songs are cached or preloaded most of the time.
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(50_000, 50_000, 500, 2_000).build())
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(Scrobbler(player, scope))
        player.addListener(Autoplay(player, scope))
        player.addListener(WidgetFeed(app, player, scope))
        player.addListener(object : Player.Listener {
            private var retries = 0

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                retries = 0
                saveQueue(player)
                loudness = item?.song?.gain
                setVolume()
                showLike()
                // A queue that starts out shuffled got its order from the player; the shuffle button comes by below.
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && player.shuffleModeEnabled) shuffleSmartly()
            }

            override fun onShuffleModeEnabledChanged(on: Boolean) {
                if (on) shuffleSmartly()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) Playback.sleep.value = null
            }

            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) {
                    val next = player.nextMediaItemIndex
                    val upNext = if (next != C.INDEX_UNSET) player.getMediaItemAt(next) else null
                    app.offline.preload(upNext?.song)
                    app.offline.compact(setOfNotNull(player.currentMediaItem?.mediaId, upNext?.mediaId))
                }
            }

            // A dead endpoint mid-song: switch to the next one and carry on from the same spot.
            override fun onPlayerError(error: PlaybackException) {
                if (retries++ >= 3) return
                scope.launch {
                    app.api.pickFastest()
                    player.prepare()
                    player.play()
                }
            }
        })

        // Stopped on a network error: carry on as soon as an endpoint answers again.
        scope.launch {
            app.api.online.collect { if (it && player.playerError != null) player.prepare() }
        }

        scope.launch { Playback.sleep.collectLatest(::sleep) }
        scope.launch { Playback.gain.collect { setVolume() } }
        // The heart follows likes made in the app too.
        scope.launch { snapshotFlow { player.currentMediaItem?.song?.let(Likes::isLiked) }.collect { showLike() } }

        session = MediaLibrarySession.Builder(
            this, player,
            object : MediaLibrarySession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo) =
                    MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller).setAvailableSessionCommands(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon().add(Playback.LIKE).add(Playback.MOVE).build(),
                    ).build()

                override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                    when (command) {
                        Playback.LIKE -> player.currentMediaItem?.song?.let {
                            Likes.toggle(it)
                            showLike()
                        }
                        Playback.MOVE -> move(args.getInt("from"), args.getInt("to"), args.getString("auto"))
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                // The app's own songs arrive whole but for their URI, which controllers don't send across the binder and
                // derives from the id. A browser sends only the id of a place in the tree, or what someone said to play.
                override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, items: MutableList<MediaItem>): ListenableFuture<List<MediaItem>> {
                    fun own(it: MediaItem) = it.buildUpon().setUri(songUri(it.mediaId)).setCustomCacheKey(it.mediaId).build()
                    fun asked(it: MediaItem) = Node.parse(it.mediaId) != null || it.requestMetadata.searchQuery != null
                    if (items.none(::asked)) return Futures.immediateFuture(items.map(::own))
                    return scope.future {
                        items.flatMap { item ->
                            val track = Node.parse(item.mediaId) as? Node.Track
                            when {
                                track != null -> listOf(item.song ?: app.api.song(track.songId))
                                asked(item) -> browsePlay(app, item)?.songs.orEmpty()
                                else -> return@flatMap listOf(own(item))
                            }.map { it.toMediaItem() }
                        }
                    }
                }

                override fun onSetMediaItems(
                    session: MediaSession, controller: MediaSession.ControllerInfo, items: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long,
                ): ListenableFuture<MediaItemsWithStartPosition> {
                    val one = items.singleOrNull()?.takeIf { Node.parse(it.mediaId) != null || it.requestMetadata.searchQuery != null }
                        ?: return super.onSetMediaItems(session, controller, items, startIndex, startPositionMs)
                    return scope.future {
                        val asked = browsePlay(app, one)?.takeIf { it.songs.isNotEmpty() } ?: throw IOException("Nothing to play for ${one.mediaId}")
                        player.playlistMetadata = MediaMetadata.Builder().setTitle(asked.from).build()
                        MediaItemsWithStartPosition(asked.songs.map { it.toMediaItem() }, asked.start, 0)
                    }
                }

                override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?) =
                    Futures.immediateFuture(
                        LibraryResult.ofItem(
                            browseRoot(),
                            // A car shows playlists and albums as a grid of covers, songs as a list.
                            LibraryParams.Builder().setExtras(
                                Bundle().apply {
                                    putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
                                    putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
                                },
                            ).build(),
                        ),
                    )

                override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String) =
                    scope.future { runCatching { browseItem(app, mediaId) }.getOrNull()?.let { LibraryResult.ofItem(it, null) } ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE) }

                override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?) =
                    scope.future {
                        runCatching { browseChildren(app, parentId) }.fold(
                            { if (it == null) LibraryResult.ofError(SessionError.ERROR_BAD_VALUE) else LibraryResult.ofItemList(it.page(page, pageSize), params) },
                            { LibraryResult.ofError(SessionError.ERROR_IO) },
                        )
                    }

                override fun onSearch(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?): ListenableFuture<LibraryResult<Void>> {
                    scope.launch {
                        val found = runCatching { browseSearch(app, query) }.getOrDefault(emptyList())
                        searches[query] = found
                        session.notifySearchResultChanged(browser, query, found.size, params)
                    }
                    return Futures.immediateFuture(LibraryResult.ofVoid())
                }

                override fun onGetSearchResult(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?) =
                    scope.future { LibraryResult.ofItemList((searches[query] ?: runCatching { browseSearch(app, query) }.getOrDefault(emptyList())).page(page, pageSize), params) }

                // Headset button: one press toggles, two skip to the next song, three go back.
                override fun onMediaButtonEvent(session: MediaSession, controller: MediaSession.ControllerInfo, intent: Intent): Boolean {
                    val key = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return false
                    if (key.keyCode != KeyEvent.KEYCODE_HEADSETHOOK && key.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) return false
                    if (controller.controllerVersion != MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION) return false
                    if (key.action == KeyEvent.ACTION_DOWN && key.repeatCount == 0) {
                        presses++
                        buttons.removeCallbacksAndMessages(null)
                        buttons.postDelayed({
                            when (presses) {
                                1 -> Util.handlePlayPauseButtonAction(player)
                                2 -> player.seekToNext()
                                else -> player.seekToPrevious()
                            }
                            presses = 0
                        }, 400)
                    }
                    return true
                }
            },
        )
            .setBitmapLoader(CacheBitmapLoader(CoverLoader(app)))
            .setSessionActivity(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
            )
            .build()

        Jam.player = player
        restoring = restoreQueue(player)
        scope.launch {
            while (isActive) {
                delay(15_000)
                if (player.isPlaying) saveQueue(player)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /** What a browser last searched for, kept for when it comes back for the hits. */
    private val searches = object : LinkedHashMap<String, List<MediaItem>>() {
        override fun removeEldestEntry(eldest: Map.Entry<String, List<MediaItem>>) = size > 8
    }

    private fun setVolume() {
        player.volume = replayVolume(loudness, Playback.gain.value) * fade
    }

    /** Runs [t] until it stops the music or another takes its place; whichever way it ends, the volume comes back. */
    private suspend fun sleep(t: SleepTimer?) {
        if (t == null) return
        // The player stops itself at the end of the song, exactly where the next one would start.
        player.pauseAtEndOfMediaItems = t.at == null
        try {
            while (player.playbackState != Player.STATE_ENDED) {
                val left = t.at?.let { it - SystemClock.elapsedRealtime() }
                    ?: (player.duration.takeIf { it != C.TIME_UNSET } ?: player.mediaMetadata.durationMs)?.let { ((it - player.currentPosition) / player.playbackParameters.speed).toLong() }
                    ?: Long.MAX_VALUE
                if (t.at != null && left <= 0) {
                    player.pause()
                    break
                }
                fade = sleepFade(left)
                setVolume()
                delay(if (left > SLEEP_FADE_MS + 1_000) 1_000 else 50)
            }
            Playback.sleep.value = null
        } finally {
            player.pauseAtEndOfMediaItems = false
            fade = 1f
            setVolume()
        }
    }

    private fun showLike() {
        val on = player.currentMediaItem?.song?.let(Likes::isLiked)
        if (on == liked) return
        liked = on
        session?.setMediaButtonPreferences(
            if (on == null) emptyList() else listOf(
                CommandButton.Builder(if (on) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
                    .setDisplayName(if (on) "Remove from Liked Songs" else "Add to Liked Songs").setSessionCommand(Playback.LIKE).build(),
            ),
        )
    }

    /** The current song first, then the rest of the user's with their artists spread out, then what autoplay added, as it was. */
    private fun shuffleSmartly() {
        val now = player.currentMediaItemIndex
        val items = List(player.mediaItemCount, player::getMediaItemAt)
        if (now !in items.indices) return
        fun artist(i: Int) = items[i].mediaMetadata.artist?.toString().orEmpty()
        val (autos, own) = items.indices.filter { it != now }.partition { items[it].auto != null }
        val order = listOf(now) + smartShuffle(own.map(::artist), after = artist(now)).map { own[it] } + autos
        player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), System.nanoTime()))
    }

    private fun move(from: Int, to: Int, auto: String?) {
        val n = player.mediaItemCount
        if (from !in 0 until n || to !in 0 until n || from == to) return
        val order = player.playOrder()
        val item = player.getMediaItemAt(from)
        if (item.auto != auto) item.song?.let { player.replaceMediaItem(from, it.toMediaItem(auto, item.jam)) }
        // A shuffled queue plays in an order of its own: the song keeps its place in the list and moves in that order.
        if (player.shuffleModeEnabled) player.setShuffleOrder(DefaultShuffleOrder(order.moved(order.indexOf(from), order.indexOf(to)).toIntArray(), System.nanoTime()))
        else player.moveMediaItem(from, to)
    }

    // A widget button can be what starts the process, and would then get here ahead of the saved queue.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            restoring?.join()
            super.onStartCommand(intent, flags, startId)
            if (intent?.getBooleanExtra(EXTRA_PLAY, false) == true) Util.handlePlayButtonAction(session?.player)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        buttons.removeCallbacksAndMessages(null)
        Playback.sleep.value = null
        Jam.stop()
        Jam.player = null
        session?.run {
            saveQueue(player)
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private fun saveQueue(player: Player) {
        val items = JSONArray()
        for (i in 0 until player.mediaItemCount) player.getMediaItemAt(i).run { song?.let { items.put(it.toJson().put("auto", auto).put("jam", jam)) } }
        val json = JSONObject()
            .put("items", items)
            .put("index", player.currentMediaItemIndex)
            .put("position", player.currentPosition)
            .put("shuffle", player.shuffleModeEnabled)
            .put("repeat", player.repeatMode)
            .put("from", player.playlistMetadata.title?.toString())
            .toString()
        scope.launch(Dispatchers.IO) { queueFile.writeText(json) }
    }

    private fun restoreQueue(player: Player) =
        scope.launch {
            val o = withContext(Dispatchers.IO) { runCatching { JSONObject(queueFile.readText()) }.getOrNull() } ?: return@launch
            val songs = o.getJSONArray("items").objects().map { Song.from(it).toMediaItem(it.s("auto"), it.s("jam")) }
            if (songs.isEmpty() || player.mediaItemCount > 0) return@launch
            player.setMediaItems(songs, o.optInt("index").coerceIn(0, songs.lastIndex), o.optLong("position"))
            player.shuffleModeEnabled = o.optBoolean("shuffle")
            player.repeatMode = o.optInt("repeat")
            player.playlistMetadata = MediaMetadata.Builder().setTitle(o.s("from")).build()
            player.prepare()
        }
}

/**
 * Notification and lock screen art through Coil, so it shares the app's disk cache and the
 * downloaded covers, and still loads after the endpoint baked into the artwork URL went away.
 */
private class CoverLoader(private val app: App) : BitmapLoader {
    private val fallback = DataSourceBitmapLoader.Builder(app).build()

    override fun supportsMimeType(mimeType: String) = fallback.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray) = fallback.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val id = uri.getQueryParameter("id")?.takeIf { uri.path?.endsWith("/getCoverArt") == true }
        val req = (if (id != null) coverRequest(app, id, 600).memoryCacheKey("notif:$id") else ImageRequest.Builder(app).data(uri))
            .allowHardware(false)
            .build()
        val f = SettableFuture.create<Bitmap>()
        app.scope.launch {
            val bmp = (app.imageLoader.execute(req) as? SuccessResult)?.image?.toBitmap()
            if (bmp != null) f.set(bmp) else f.setException(IOException("No cover for $uri"))
        }
        return f
    }
}

/**
 * Keeps the music going when the queue's last song starts. A song radio (a lone song the user tapped) gets songs
 * like it; with autoplay on, any other queue gets songs like the last few heard, unless it repeats, which never ends.
 * Offline, only downloaded ones.
 */
private class Autoplay(private val player: ExoPlayer, private val scope: CoroutineScope) : Player.Listener {
    private var asked: String? = null
    private var autos = 0

    // A new queue asks afresh, even when it ends in the song the old one did.
    override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
        asked = null
        autos = 0
    }

    override fun onEvents(p: Player, events: Player.Events) {
        if (!events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) return
        val app = App.it
        val current = player.currentMediaItem ?: return
        val id = current.mediaId
        val n = player.mediaItemCount
        val had = autos
        autos = (0 until n).count { player.getMediaItemAt(it).auto != null }
        // The user cleared what was added for them: this song doesn't put it back.
        if (autos < had) asked = id
        val radio = n == 1 && player.playlistMetadata.extras?.getBoolean("radio") == true
        if (!(radio || app.prefs.autoplay && player.nextMediaItemIndex == C.INDEX_UNSET) || id == asked) return
        asked = id
        val t = player.currentTimeline
        val shuffle = player.shuffleModeEnabled
        fun step(i: Int, next: Boolean) =
            (if (next) t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, shuffle) else t.getPreviousWindowIndex(i, Player.REPEAT_MODE_OFF, shuffle)).takeIf { it != C.INDEX_UNSET }
        val seeds = generateSequence(player.currentMediaItemIndex) { step(it, false) }.take(5).map { player.getMediaItemAt(it).mediaId }.toList()
        val order = generateSequence(t.getFirstWindowIndex(shuffle)) { step(it, true) }.toList()
        val queue = (0 until n).map { player.getMediaItemAt(it).mediaId }.toSet()
        val seed = current.song
        scope.launch {
            val songs = runCatching { if (radio && seed != null) app.api.songRadio(seed, queue) else app.api.recommendations(seeds, queue) }.getOrNull().orEmpty()
                .filter { app.api.online.value || app.offline.isDownloaded(it.id) }
            // The queue changed while asking; its own last song will ask again.
            if (songs.isEmpty() || player.currentMediaItem?.mediaId != id || player.mediaItemCount != n) return@launch
            player.addMediaItems(songs.map { it.toMediaItem(auto = current.mediaMetadata.title?.toString().orEmpty()) })
            // Added songs would otherwise be shuffled in among the ones already played.
            if (shuffle) player.setShuffleOrder(DefaultShuffleOrder((order + (n until n + songs.size)).toIntArray(), System.nanoTime()))
        }
    }
}

/**
 * Reports "now playing" when a song starts and a scrobble once half of it (or 4 minutes) has
 * actually been heard, like Navidrome's web player. Works the same for downloaded songs; when
 * offline the scrobble is queued with its original timestamp and sent later.
 */
private class Scrobbler(private val player: Player, scope: CoroutineScope) : Player.Listener {
    private val api get() = App.it.api
    private var id: String? = null
    private var durationMs = 0L
    private var startedAt = 0L
    private var heardMs = 0L
    private var since = 0L
    private var announced = false
    private var submitted = false

    init {
        scope.launch {
            while (isActive) {
                delay(5_000)
                tick()
            }
        }
    }

    override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
        tick()
        id = item?.mediaId
        durationMs = item?.mediaMetadata?.durationMs ?: 0
        heardMs = 0
        announced = false
        submitted = false
        tick()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (id == null) onMediaItemTransition(player.currentMediaItem, 0) else tick()
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        if (since > 0) heardMs += now - since
        since = if (player.isPlaying) now else 0
        val song = id ?: return
        if (player.isPlaying && !announced) {
            announced = true
            startedAt = System.currentTimeMillis()
            api.nowPlaying(song)
        }
        val need = if (durationMs > 0) minOf(durationMs / 2, 240_000) else 240_000
        if (!submitted && heardMs >= need) {
            submitted = true
            api.scrobble(song, startedAt)
        }
    }
}
