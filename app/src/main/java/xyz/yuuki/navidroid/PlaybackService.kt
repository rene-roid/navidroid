package xyz.yuuki.navidroid

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
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
import java.io.File

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

    override fun onCreate() {
        super.onCreate()
        val app = application as App
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(app.offline.playback))
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(Scrobbler(player, scope))
        player.addListener(object : Player.Listener {
            private var retries = 0

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                retries = 0
                saveQueue(player)
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

        session = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
            )
            .setCallback(object : MediaSession.Callback {
                // Controllers don't send URIs across the binder; every song's URI derives from its id.
                override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, items: MutableList<MediaItem>) =
                    Futures.immediateFuture(items.map { it.buildUpon().setUri(songUri(it.mediaId)).setCustomCacheKey(it.mediaId).build() })
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
