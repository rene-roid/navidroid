package xyz.yuuki.navidroid

import android.app.PendingIntent
import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import xyz.yuuki.navidroid.ui.coverRequest
import java.io.File
import java.io.IOException

fun Song.toMediaItem(): MediaItem {
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
                .setExtras(Bundle().apply { putString("song", toJson().toString()) })
                .build(),
        )
        .build()
}

val MediaItem.song: Song? get() = mediaMetadata.extras?.getString("song")?.let { Song.from(JSONObject(it)) }

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val scope = MainScope()
    private val queueFile by lazy { File(filesDir, "queue.json") }
    private val buttons = Handler(Looper.getMainLooper())
    private var presses = 0

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
        val player = ExoPlayer.Builder(this, renderers)
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
        player.addListener(object : Player.Listener {
            private var retries = 0

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                retries = 0
                saveQueue(player)
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

        session = MediaSession.Builder(this, player)
            .setBitmapLoader(CacheBitmapLoader(CoverLoader(app)))
            .setSessionActivity(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
            )
            .setCallback(object : MediaSession.Callback {
                // Controllers don't send URIs across the binder; every song's URI derives from its id.
                override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, items: MutableList<MediaItem>) =
                    Futures.immediateFuture(items.map { it.buildUpon().setUri(songUri(it.mediaId)).setCustomCacheKey(it.mediaId).build() })

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
            })
            .build()

        restoreQueue(player)
        scope.launch {
            while (isActive) {
                delay(15_000)
                if (player.isPlaying) saveQueue(player)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        buttons.removeCallbacksAndMessages(null)
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
        for (i in 0 until player.mediaItemCount) player.getMediaItemAt(i).song?.let { items.put(it.toJson()) }
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

    private fun restoreQueue(player: Player) {
        scope.launch {
            val o = withContext(Dispatchers.IO) { runCatching { JSONObject(queueFile.readText()) }.getOrNull() } ?: return@launch
            val songs = o.getJSONArray("items").objects().map(Song::from)
            if (songs.isEmpty() || player.mediaItemCount > 0) return@launch
            player.setMediaItems(songs.map { it.toMediaItem() }, o.optInt("index").coerceIn(0, songs.lastIndex), o.optLong("position"))
            player.shuffleModeEnabled = o.optBoolean("shuffle")
            player.repeatMode = o.optInt("repeat")
            player.playlistMetadata = MediaMetadata.Builder().setTitle(o.s("from")).build()
            player.prepare()
        }
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
 * With autoplay on, tops up the queue when its last song starts, with songs like the last few heard.
 * Offline, only downloaded ones.
 */
private class Autoplay(private val player: ExoPlayer, private val scope: CoroutineScope) : Player.Listener {
    private var asked: String? = null

    override fun onEvents(p: Player, events: Player.Events) {
        if (!events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) return
        val app = App.it
        val id = player.currentMediaItem?.mediaId ?: return
        if (!app.prefs.autoplay || player.nextMediaItemIndex != C.INDEX_UNSET || id == asked) return
        asked = id
        val t = player.currentTimeline
        val shuffle = player.shuffleModeEnabled
        fun step(i: Int, next: Boolean) =
            (if (next) t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, shuffle) else t.getPreviousWindowIndex(i, Player.REPEAT_MODE_OFF, shuffle)).takeIf { it != C.INDEX_UNSET }
        val seeds = generateSequence(player.currentMediaItemIndex) { step(it, false) }.take(5).map { player.getMediaItemAt(it).mediaId }.toList()
        val order = generateSequence(t.getFirstWindowIndex(shuffle)) { step(it, true) }.toList()
        val n = player.mediaItemCount
        val queue = (0 until n).map { player.getMediaItemAt(it).mediaId }.toSet()
        scope.launch {
            val songs = runCatching { app.api.recommendations(seeds, queue) }.getOrNull().orEmpty()
                .filter { app.api.online.value || app.offline.isDownloaded(it.id) }
            // The queue changed while asking; its own last song will ask again.
            if (songs.isEmpty() || player.currentMediaItem?.mediaId != id || player.mediaItemCount != n) return@launch
            player.addMediaItems(songs.map { it.toMediaItem() })
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
