package xyz.yuuki.navidroid

import android.app.Notification
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
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
import java.util.concurrent.Executors

/** Song URIs are resolved against whichever endpoint is active when the bytes are actually fetched. */
fun songUri(id: String, bitrate: Int? = null): Uri =
    Uri.parse("navidroid://song/$id" + (bitrate?.let { "?br=$it" } ?: ""))

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
    private val covers = File(app.filesDir, "covers").apply { mkdirs() }
    private val snapshots = File(app.filesDir, "collections").apply { mkdirs() }

    private val upstream = ResolvingDataSource.Factory(
        DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true),
    ) { spec ->
        if (spec.uri.scheme != "navidroid") spec
        else spec.withUri(Uri.parse(app.api.streamUrl(spec.uri.lastPathSegment!!, spec.uri.getQueryParameter("br")?.toInt() ?: app.streamBitrate)))
    }

    /** Reads downloaded songs from disk, streams the rest. Streams are not written to the download cache. */
    val playback: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(upstream)
        .setCacheWriteDataSinkFactory(null)

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

    fun pin(c: Collection) {
        saveSnapshot(c)
        setPins(pins.value + Pin(c.kind, c.id))
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
        withContext(Dispatchers.Main) {
            val have = states.value
            for (song in want.values) {
                val s = have[song.id]
                if (s == null || s == Download.STATE_FAILED) {
                    val req = DownloadRequest.Builder(song.id, songUri(song.id, app.prefs.downloadBitrate))
                        .setCustomCacheKey(song.id)
                        .setData(song.toJson().toString().toByteArray())
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
        app.api.http.newCall(Request.Builder().url(app.api.coverUrl(id, 600)).build()).execute().use { r ->
            if (!r.isSuccessful) return
            val tmp = File(covers, "$id.part")
            tmp.outputStream().use { r.body!!.byteStream().copyTo(it) }
            tmp.renameTo(coverFile(id))
        }
    }

    /** Bytes used by downloaded songs. */
    fun bytesUsed() = cache.cacheSpace

    companion object {
        const val CHANNEL = "downloads"
    }
}

class DownloadSvc : DownloadService(1, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, Offline.CHANNEL, R.string.downloads, 0) {
    private val offline get() = (application as App).offline

    override fun getDownloadManager() = offline.manager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        offline.notifications.buildProgressNotification(this, R.drawable.ic_download, null, null, downloads, notMetRequirements)
}
