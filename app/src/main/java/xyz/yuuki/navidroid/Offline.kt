package xyz.yuuki.navidroid

import android.app.Notification
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
import coil3.imageLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import xyz.yuuki.navidroid.ui.coverRequest
import java.util.concurrent.Executors

/** Song URIs are resolved against whichever endpoint is active when the bytes are actually fetched. */
fun songUri(id: String, downloadBitrate: Int? = null): Uri =
    Uri.parse("navidroid://song/$id" + (downloadBitrate?.let { "?dl=$it" } ?: ""))

data class Pin(val kind: Kind, val id: String) {
    override fun toString() = "$kind:$id"

    companion object {
        fun parse(s: String) = s.split(':', limit = 2).let { Pin(Kind.valueOf(it[0]), it[1]) }
    }
}

/**
 * Downloads live in one Media3 cache keyed by song id, so the player reads a downloaded song
 * from disk no matter which endpoint it was fetched from. Pinned collections (playlists, albums,
 * Liked Songs) are re-fetched on [sync]: new songs get downloaded and dropped ones get removed,
 * which keeps the rotating NaviBeat/Explo mixes current.
 */
class Offline(private val app: App) {
    private val db = StandaloneDatabaseProvider(app)
    val cache = SimpleCache(File(app.filesDir, "media"), NoOpCacheEvictor(), db)
    /** Songs heard or preloaded while streaming; the oldest go first. */
    private val streamCache = SimpleCache(File(app.cacheDir, "stream"), LeastRecentlyUsedCacheEvictor(1L shl 30), db)
    private val covers = File(app.filesDir, "covers").apply { mkdirs() }
    private val snapshots = File(app.filesDir, "collections").apply { mkdirs() }

    private val upstream = ResolvingDataSource.Factory(
        // The API's client: songs reuse its warm connections instead of a fresh TCP and TLS handshake each.
        OkHttpDataSource.Factory(app.api.http),
    ) { spec ->
        val u = spec.uri
        if (u.scheme != "navidroid") spec
        else spec.withUri(Uri.parse(
            u.getQueryParameter("dl")?.let { app.api.downloadUrl(u.lastPathSegment!!, it.toInt()) }
                ?: app.api.streamUrl(u.lastPathSegment!!, u.getQueryParameter("br")?.toInt() ?: app.streamBitrate),
        ))
    }

    /**
     * Keyed by bitrate too, so a song cached on mobile data doesn't stick at that quality on Wi-Fi.
     * "mp3" leaves behind the entries cut short by the old length estimates.
     */
    private val streamed = CacheDataSource.Factory()
        .setCache(streamCache)
        .setCacheKeyFactory { "${it.key ?: it.uri}@${app.streamBitrate}mp3" }
        .setUpstreamDataSourceFactory(upstream)

    /** Reads downloaded songs from disk, then songs already streamed or preloaded, and streams the rest. */
    val playback: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(streamed)
        .setCacheWriteDataSinkFactory(null)

    private var preloading: Pair<String, CacheWriter>? = null

    /** Fetches [song] into the stream cache in the background, so skipping to it starts instantly, even offline. */
    fun preload(song: Song?) {
        if (song == null || song.id == preloading?.first || isDownloaded(song.id)) return
        preloading?.second?.cancel()
        val writer = CacheWriter(streamed.createDataSource(), DataSpec.Builder().setUri(songUri(song.id)).setKey(song.id).build(), null, null)
        preloading = song.id to writer
        app.scope.launch(Dispatchers.IO) { runCatching { writer.cache() } }
        // The notification, mini player and full player covers.
        for (size in listOf(120, 600, 1000)) song.coverArt?.let { app.imageLoader.enqueue(coverRequest(app, it, size).build()) }
    }

    @Volatile private var warming: CacheWriter? = null
    @Volatile private var warmGen = 0

    /**
     * Caches the opening of the first few [songs] of a screen the user just opened, so tapping one
     * plays at once while the rest streams in behind it.
     */
    // ponytail: first 5 songs, 512 KB each (~30 s at 128 kbps); warm around the scroll position if people tap deeper.
    fun warm(songs: List<Song>) {
        val gen = ++warmGen
        warming?.cancel()
        val todo = songs.take(5).filterNot { isDownloaded(it.id) }
        app.scope.launch(Dispatchers.IO) {
            for (s in todo) {
                val w = CacheWriter(streamed.createDataSource(), DataSpec.Builder().setUri(songUri(s.id)).setKey(s.id).setLength(512 * 1024).build(), null, null)
                warming = w
                if (gen != warmGen) return@launch
                runCatching { w.cache() }
            }
        }
    }

    val manager = DownloadManager(app, db, cache, upstream, Executors.newFixedThreadPool(4)).apply {
        maxParallelDownloads = 4
        requirements = requirementsFor(app.prefs.wifiOnlyDownloads)
    }
    val notifications by lazy { DownloadNotificationHelper(app, CHANNEL) }

    /** Download state per song id. */
    val states = MutableStateFlow<Map<String, Int>>(emptyMap())
    val pins = MutableStateFlow(app.prefs.pins.map(Pin::parse))
    val syncing = MutableStateFlow(false)
    private val ready = CompletableDeferred<Unit>()

    init {
        manager.addListener(object : DownloadManager.Listener {
            override fun onInitialized(m: DownloadManager) = reload()
            override fun onDownloadChanged(m: DownloadManager, d: Download, e: Exception?) = states.update { it + (d.request.id to d.state) }
            override fun onDownloadRemoved(m: DownloadManager, d: Download) = states.update { it - d.request.id }
        })
    }

    private fun reload() {
        val map = HashMap<String, Int>()
        manager.downloadIndex.getDownloads().use { while (it.moveToNext()) map[it.download.request.id] = it.download.state }
        states.value = map
        ready.complete(Unit)
    }

    fun isDownloaded(id: String) = states.value[id] == Download.STATE_COMPLETED

    fun setWifiOnly(on: Boolean) {
        app.prefs.wifiOnlyDownloads = on
        manager.requirements = requirementsFor(on)
    }

    private fun requirementsFor(wifiOnly: Boolean) = Requirements(if (wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK)

    // ---- pins ----

    fun isPinned(kind: Kind, id: String) = pins.value.contains(Pin(kind, id))

    fun pin(vararg cs: Collection) {
        cs.forEach(::saveSnapshot)
        setPins(pins.value + cs.map { Pin(it.kind, it.id) })
        sync()
    }

    fun unpin(kind: Kind, id: String) {
        setPins(pins.value - Pin(kind, id))
        File(snapshots, "$kind-$id.json").delete()
        sync()
    }

    fun unpinAll() {
        setPins(emptyList())
        snapshots.listFiles()?.forEach { it.delete() }
        DownloadService.sendRemoveAllDownloads(app, DownloadSvc::class.java, false)
    }

    private fun setPins(list: List<Pin>) {
        pins.value = list.distinct()
        app.prefs.pins = pins.value.map { it.toString() }
    }

    fun snapshot(kind: Kind, id: String): Collection? =
        File(snapshots, "$kind-$id.json").takeIf { it.exists() }?.let { runCatching { Collection.from(JSONObject(it.readText())) }.getOrNull() }

    private fun saveSnapshot(c: Collection) = File(snapshots, "${c.kind}-${c.id}.json").writeText(c.toJson().toString())

    fun pause() = DownloadService.sendPauseDownloads(app, DownloadSvc::class.java, false)
    fun resume() = DownloadService.sendResumeDownloads(app, DownloadSvc::class.java, false)

    // ---- sync ----

    /** Only call while the app is in the foreground: it starts the download service. */
    fun syncIfStale() {
        if (System.currentTimeMillis() - app.prefs.lastSync > 30 * 60_000) sync()
        // Picks up downloads left queued when the app was last closed.
        else DownloadService.start(app, DownloadSvc::class.java)
    }

    fun sync() {
        if (!syncing.compareAndSet(false, true)) return
        app.scope.launch {
            try {
                doSync()
            } finally {
                syncing.value = false
            }
        }
    }

    private suspend fun doSync() {
        val want = LinkedHashMap<String, Song>()
        var complete = true
        val collections = pins.value.mapNotNull { p ->
            val fresh = runCatching { app.api.collection(p.kind, p.id) }.getOrNull()
            fresh?.let(::saveSnapshot)
            (fresh ?: snapshot(p.kind, p.id).also { complete = false })
        }
        collections.forEach { c -> c.songs.forEach { want.putIfAbsent(it.id, it) } }

        ready.await()
        val pref = app.prefs.downloadBitrate
        // Re-encoding a song that is already small costs quality and saves little: keep those as they are.
        // ponytail: 1.25x the target; a 192 kbps MP3 stays, a 256 kbps AAC or any lossless file becomes Opus.
        fun br(s: Song) = if (s.bitRate in 1..pref * 5 / 4) 0 else pref
        // Downloaded at another quality: fetch again so the setting applies to songs already on the phone.
        val stale = want.keys.filter { id ->
            manager.downloadIndex.getDownload(id)?.takeIf { it.state == Download.STATE_COMPLETED }
                ?.let { JSONObject(String(it.request.data)).optInt("br") != br(want.getValue(id)) } == true
        }.toSet()
        withContext(Dispatchers.Main) {
            val have = states.value
            for (song in want.values) {
                val s = have[song.id]
                if (s == null || s == Download.STATE_FAILED || song.id in stale) {
                    // Removing first makes the manager wipe the old bytes before downloading again.
                    if (song.id in stale) DownloadService.sendRemoveDownload(app, DownloadSvc::class.java, song.id, false)
                    val req = DownloadRequest.Builder(song.id, songUri(song.id, br(song)))
                        .setCustomCacheKey(song.id)
                        .setData(song.toJson().put("br", br(song)).toString().toByteArray())
                        .build()
                    DownloadService.sendAddDownload(app, DownloadSvc::class.java, req, false)
                }
            }
            // Never delete on a partial picture: an unreachable playlist would look empty.
            if (complete) (have.keys - want.keys).forEach { DownloadService.sendRemoveDownload(app, DownloadSvc::class.java, it, false) }
        }
        if (!complete) return

        val coverIds = (collections.map { it.coverArt } + want.values.map { it.coverArt }).filterNotNull().distinct()
        val gate = Semaphore(4)
        coroutineScope {
            coverIds.filter { !coverFile(it).exists() }.map { id ->
                async(Dispatchers.IO) { gate.withPermit { runCatching { fetchCover(id) } } }
            }.awaitAll()
        }
        if (app.api.online.value) app.prefs.lastSync = System.currentTimeMillis()
    }

    fun coverFile(id: String) = File(covers, id.replace('/', '_'))

    private fun fetchCover(id: String) {
        app.imageHttp.newCall(Request.Builder().url(app.api.coverUrl(id, 600)).build()).execute().use { r ->
            if (!r.isSuccessful) return
            val tmp = File(covers, "$id.part")
            tmp.outputStream().use { r.body!!.byteStream().copyTo(it) }
            tmp.renameTo(coverFile(id))
        }
    }

    /** Bytes used by downloaded songs and their covers. */
    fun bytesUsed() = cache.cacheSpace + (covers.listFiles()?.sumOf { it.length() } ?: 0L)

    /** Bytes used by songs kept from streaming and preloading. */
    fun streamBytes() = streamCache.cacheSpace

    fun clearStreamCache() = streamCache.keys.toList().forEach(streamCache::removeResource)

    /** What the stream cache holds, last heard first. */
    fun streamed() = streamCache.keys.map { key ->
        val spans = streamCache.getCachedSpans(key)
        val bytes = spans.sumOf { it.length }
        Streamed(
            key.substringBeforeLast('@'),
            key.substringAfterLast('@').removeSuffix("mp3").toIntOrNull() ?: 0,
            bytes,
            ContentMetadata.getContentLength(streamCache.getContentMetadata(key)).let { it > 0 && bytes >= it },
            spans.maxOfOrNull { it.lastTouchTimestamp } ?: 0,
        )
    }.sortedByDescending { it.touched }

    companion object {
        const val CHANNEL = "downloads"
    }
}

/** A song in the stream cache; [bitrate] 0 is the original file, and it isn't [whole] when only its start was fetched ahead. */
class Streamed(val id: String, val bitrate: Int, val bytes: Long, val whole: Boolean, val touched: Long)

class DownloadSvc : DownloadService(1, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, Offline.CHANNEL, R.string.downloads, 0) {
    private val offline get() = (application as App).offline

    override fun getDownloadManager() = offline.manager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        offline.notifications.buildProgressNotification(this, R.drawable.ic_download, null, null, downloads, notMetRequirements)
}
