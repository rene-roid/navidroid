package xyz.yuuki.navidroid

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// ---- models ---------------------------------------------------------------------------------

enum class Kind { Playlist, Album, Artist, Liked, Genre }

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String?,
    val album: String,
    val albumId: String?,
    val coverArt: String?,
    val duration: Int,
    val track: Int,
    val year: Int,
    val starred: Boolean,
    val suffix: String?,
    val bitRate: Int,
    /** Every credited artist as (id, name), when the server lists them. */
    val artists: List<Pair<String, String>> = emptyList(),
) {
    /** Same field names as the Subsonic response, so [from] reads both API and saved JSON. */
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("artist", artist).put("artistId", artistId)
        .put("album", album).put("albumId", albumId).put("coverArt", coverArt)
        .put("duration", duration).put("track", track).put("year", year)
        .put("suffix", suffix).put("bitRate", bitRate)
        .put("artists", JSONArray(artists.map { JSONObject().put("id", it.first).put("name", it.second) }))
        .apply { if (starred) put("starred", "1") }

    companion object {
        fun from(o: JSONObject) = Song(
            id = o.getString("id"),
            title = o.s("title") ?: "",
            artist = o.s("displayArtist") ?: o.s("artist") ?: "Unknown artist",
            artistId = o.s("artistId"),
            album = o.s("album") ?: "",
            albumId = o.s("albumId"),
            coverArt = o.s("coverArt") ?: o.s("albumId"),
            duration = o.optInt("duration"),
            track = o.optInt("track"),
            year = o.optInt("year"),
            starred = o.has("starred"),
            suffix = o.s("suffix"),
            bitRate = o.optInt("bitRate"),
            artists = o.optJSONArray("artists").objects().mapNotNull { a -> a.s("id")?.let { it to (a.s("name") ?: "") } },
        )
    }
}

data class Album(
    val id: String, val name: String, val artist: String, val artistId: String?,
    val coverArt: String?, val year: Int, val songCount: Int, val playCount: Int, val starred: Boolean = false,
    val single: Boolean = false,
) {
    companion object {
        fun from(o: JSONObject) = Album(
            id = o.getString("id"),
            name = o.s("name") ?: o.s("title") ?: "",
            artist = o.s("displayArtist") ?: o.s("artist") ?: "",
            artistId = o.s("artistId"),
            coverArt = o.s("coverArt") ?: o.getString("id"),
            year = o.optInt("year"),
            songCount = o.optInt("songCount"),
            playCount = o.optInt("playCount"),
            starred = o.has("starred"),
            single = isSingle(o.optJSONArray("releaseTypes")?.let { t -> List(t.length()) { t.optString(it) } }.orEmpty(), o.optInt("songCount"), o.optInt("duration")),
        )

        /** Tagged release type when there is one, otherwise Spotify's rule: up to 6 songs and under 30 minutes. */
        fun isSingle(types: List<String>, songs: Int, seconds: Int) =
            if (types.isNotEmpty()) types.any { it.equals("single", true) || it.equals("ep", true) } else songs in 1..6 && seconds < 1800
    }
}

data class Artist(val id: String, val name: String, val coverArt: String?, val albumCount: Int, val starred: Boolean = false) {
    companion object {
        fun from(o: JSONObject) = Artist(
            o.getString("id"), o.s("name") ?: "", o.s("coverArt") ?: "ar-${o.getString("id")}", o.optInt("albumCount"), o.has("starred"),
        )
    }
}

data class Playlist(
    val id: String, val name: String, val comment: String, val owner: String,
    val songCount: Int, val duration: Int, val coverArt: String?,
) {
    val mix: Mix? = parseMix(name, comment)

    companion object {
        fun from(o: JSONObject) = Playlist(
            o.getString("id"), o.s("name") ?: "", o.s("comment") ?: "", o.s("owner") ?: "",
            o.optInt("songCount"), o.optInt("duration"), o.s("coverArt"),
        )
    }
}

/** Anything with a track list: a playlist, album, Liked Songs or a genre. */
data class Collection(
    val kind: Kind,
    val id: String,
    val title: String,
    val description: String?,
    val by: String?,
    val coverArt: String?,
    val songs: List<Song>,
    val mix: Mix? = null,
    val year: Int = 0,
    val artistId: String? = null,
    val starred: Boolean = false,
) {
    val duration get() = songs.sumOf { it.duration }

    fun toJson(): JSONObject = JSONObject()
        .put("kind", kind.name).put("id", id).put("title", title).put("description", description)
        .put("by", by).put("coverArt", coverArt).put("year", year).put("artistId", artistId)
        .put("mix", mix?.toJson())
        .put("songs", JSONArray(songs.map { it.toJson() }))

    companion object {
        fun from(o: JSONObject) = Collection(
            Kind.valueOf(o.getString("kind")), o.getString("id"), o.getString("title"), o.s("description"), o.s("by"),
            o.s("coverArt"), o.getJSONArray("songs").objects().map(Song::from),
            o.optJSONObject("mix")?.let(Mix::from), o.optInt("year"), o.s("artistId"),
        )

        fun of(p: Playlist, songs: List<Song>) = Collection(
            Kind.Playlist, p.id, p.mix?.title ?: p.name, p.mix?.description ?: p.comment.ifBlank { null },
            p.mix?.by ?: p.owner, p.coverArt, songs, p.mix,
        )
    }
}

data class ArtistPage(val artist: Artist, val albums: List<Album>, val top: List<Song>, val appearsOn: List<Album> = emptyList())
data class SearchResult(val artists: List<Artist>, val albums: List<Album>, val songs: List<Song>)
data class Genre(val name: String, val songCount: Int)
data class LyricLine(val ms: Long, val text: String)
data class Lyrics(val synced: Boolean, val lines: List<LyricLine>)

/** An LRC header line like `[ti:Song]` or `[ar:Artist]`, which some servers pass through as if it were lyrics. */
fun isLrcTag(line: String) = line.trim().matches(Regex("""\[[a-zA-Z#]+:[^\]]*]"""))

class ApiException(message: String) : Exception(message)

/** Makes [Api.call] answer only from what is saved on disk, to show a screen before the server answers. */
class CacheOnly : AbstractCoroutineContextElement(CacheOnly) {
    companion object Key : CoroutineContext.Key<CacheOnly>
}

fun JSONObject.s(k: String): String? = if (isNull(k)) null else optString(k)
fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }
fun JSONObject.list(k: String): List<JSONObject> = optJSONArray(k).objects()

/** "A, B & C (feat. D)" → [A, B, C, D]. */
fun artistNames(credit: String): List<String> =
    credit.split(Regex("""\s*(?:[,&/;()]|\bfeat\b\.?|\bft\b\.?)\s*""", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

// ---- settings -------------------------------------------------------------------------------

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("navidroid", Context.MODE_PRIVATE)

    var endpoints: List<String>
        get() = p.getString("endpoints", null)?.let { JSONArray(it) }?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
        set(v) = p.edit().putString("endpoints", JSONArray(v).toString()).apply()
    var active by str("active")
    var username by str("username")
    var token by str("token")
    var salt by str("salt")
    var streamBitrateWifi by int("streamWifi", 0)
    var streamBitrateMobile by int("streamMobile", 192)
    var downloadBitrate by int("downloadBitrate", 160)
    var wifiOnlyDownloads by bool("wifiOnly", true)
    var auroraColors by int("auroraColors", 0)
    var accent by int("accent", 0xFF0085FF.toInt())
    var lastSync by long("lastSync")
    var autoplay by bool("autoplay", false)

    /** Playlist ids saved to the library; Subsonic can't star playlists, so they live here. */
    var savedPlaylists: Set<String>
        get() = p.getStringSet("savedPlaylists", null) ?: emptySet()
        set(v) = p.edit().putStringSet("savedPlaylists", v).apply()

    var hiddenPlaylists: Set<String>
        get() = p.getStringSet("hiddenPlaylists", null) ?: emptySet()
        set(v) = p.edit().putStringSet("hiddenPlaylists", v).apply()

    /** Pinned collections, "Kind:id", kept downloaded and re-synced. */
    var pins: List<String>
        get() = p.getString("pins", null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
        set(v) = p.edit().putString("pins", v.joinToString("\n")).apply()

    /** Scrobbles that failed (offline), "songId:epochMs", retried later. */
    var pendingScrobbles: List<String>
        get() = p.getString("scrobbles", null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
        set(v) = p.edit().putString("scrobbles", v.joinToString("\n")).apply()

    val loggedIn get() = token != null && endpoints.isNotEmpty()

    fun flag(k: String) = p.getBoolean(k, true)

    fun flag(k: String, v: Boolean) = p.edit().putBoolean(k, v).apply()

    fun clear() = p.edit().clear().apply()

    private fun str(k: String) = object : kotlin.properties.ReadWriteProperty<Any, String?> {
        override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>) = p.getString(k, null)
        override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: String?) = p.edit().putString(k, value).apply()
    }

    private fun int(k: String, d: Int) = object : kotlin.properties.ReadWriteProperty<Any, Int> {
        override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>) = p.getInt(k, d)
        override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: Int) = p.edit().putInt(k, value).apply()
    }

    private fun long(k: String) = object : kotlin.properties.ReadWriteProperty<Any, Long> {
        override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>) = p.getLong(k, 0)
        override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: Long) = p.edit().putLong(k, value).apply()
    }

    private fun bool(k: String, d: Boolean) = object : kotlin.properties.ReadWriteProperty<Any, Boolean> {
        override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>) = p.getBoolean(k, d)
        override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: Boolean) = p.edit().putBoolean(k, value).apply()
    }
}

fun normalizeEndpoint(input: String): String {
    var u = input.trim().trimEnd('/')
    if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
    // People paste the web UI address; the API lives at the root.
    u = u.substringBefore("/app/#").substringBefore("/app").trimEnd('/')
    // Phone keyboards capitalise the first letter ("Http://Music..."); scheme and host are case-insensitive.
    return u.toHttpUrlOrNull()?.toString()?.trimEnd('/') ?: u
}

fun md5(s: String): String = MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

// ---- client ---------------------------------------------------------------------------------

/**
 * Subsonic client over several endpoints of the same server (LAN address, public domain, ...).
 * All endpoints are pinged in parallel and the first to answer becomes [active]; any network
 * failure re-runs that race, so moving between Wi-Fi and mobile data just picks the next one.
 */
class Api(private val prefs: Prefs, private val scope: CoroutineScope) {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val pingHttp = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    @Volatile var active: String = prefs.active ?: prefs.endpoints.firstOrNull() ?: ""
        private set

    /** Last measured round trip per endpoint, null when it did not answer. */
    val latency = MutableStateFlow<Map<String, Long?>>(emptyMap())
    val online = MutableStateFlow(true)

    private var race: Deferred<String?>? = null
    @Volatile private var lastFailedRace = 0L
    private val memo = ConcurrentHashMap<String, Any>()

    fun url(method: String, params: List<Pair<String, Any?>> = emptyList(), base: String = active): String {
        val b = "$base/rest/$method".toHttpUrl().newBuilder()
            .addQueryParameter("u", prefs.username)
            .addQueryParameter("t", prefs.token)
            .addQueryParameter("s", prefs.salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "navidroid")
            .addQueryParameter("f", "json")
        for ((k, v) in params) if (v != null) b.addQueryParameter(k, v.toString())
        return b.build().toString()
    }

    fun coverUrl(id: String, size: Int) = url("getCoverArt", listOf("id" to id, "size" to size))

    /**
     * Constant-bitrate MP3, which starts fast and seeks without knowing its length. No length estimate:
     * Navidrome's default Opus outgrows it, and the player stops at the estimate, cutting songs short.
     */
    fun streamUrl(id: String, maxBitRate: Int) = url(
        "stream",
        if (maxBitRate > 0) listOf("id" to id, "maxBitRate" to maxBitRate, "format" to "mp3") else listOf("id" to id),
    )

    /**
     * Opus, about half the size of MP3 at the same quality. No length estimate: a variable-bitrate
     * stream can outgrow it and get cut short, and the cache records the real length anyway.
     */
    fun downloadUrl(id: String, maxBitRate: Int) =
        url("stream", if (maxBitRate > 0) listOf("id" to id, "maxBitRate" to maxBitRate, "format" to "opus") else listOf("id" to id))

    private var reconnecting: Job? = null

    /** Races the endpoints, retrying for a few seconds: a network that just came up often can't route yet. */
    fun reconnect() {
        reconnecting?.cancel()
        reconnecting = scope.launch {
            for (wait in listOf(0L, 1_000, 2_000, 4_000, 8_000)) {
                delay(wait)
                if (pickFastest() != null) return@launch
            }
        }
    }

    /** Pings every endpoint at once and switches to the first that answers. Concurrent callers share one race. */
    suspend fun pickFastest(): String? {
        val r = synchronized(this) {
            race?.takeIf { it.isActive } ?: scope.async { runRace() }.also { race = it }
        }
        return r.await()
    }

    private suspend fun runRace(): String? {
        val eps = prefs.endpoints
        if (eps.isEmpty()) return null
        val winner = CompletableDeferred<String?>()
        var left = eps.size
        for (ep in eps) scope.launch(Dispatchers.IO) {
            val ms = ping(ep)
            latency.update { it + (ep to ms) }
            if (ms != null) winner.complete(ep)
            if (synchronized(winner) { --left } == 0) winner.complete(null)
        }
        val w = winner.await()
        online.value = w != null
        lastFailedRace = if (w == null) System.currentTimeMillis() else 0
        if (w != null && w != active) {
            active = w
            prefs.active = w
        }
        // Not awaited: a failing scrobble would wait on this very race.
        if (w != null) scope.launch { flushScrobbles() }
        return w
    }

    private fun ping(base: String): Long? = try {
        val t0 = System.nanoTime()
        pingHttp.newCall(Request.Builder().url(url("ping", base = base)).build()).execute().use {
            if (it.isSuccessful && it.body!!.string().contains("subsonic-response")) (System.nanoTime() - t0) / 1_000_000 else null
        }
    } catch (_: Exception) {
        null
    }

    /** Calls [method] and returns the `subsonic-response` object; retries once on the next-best endpoint. */
    suspend fun call(method: String, vararg params: Pair<String, Any?>): JSONObject = withContext(Dispatchers.IO) {
        // Reads are kept on disk, and answer instead when asked to ([CacheOnly]) or when the server can't be reached.
        val copy = if (method.startsWith("get") || method == "search3") File(saved, md5(method + params.joinToString { "${it.first}=${it.second}" })) else null
        val fresh = coroutineContext[CacheOnly] == null
        val body = if (!fresh) copy?.takeIf { it.exists() }?.readText() ?: throw IOException("Not saved")
        else try {
            fetch(method, params.toList())
        } catch (e: IOException) {
            copy?.takeIf { it.exists() }?.readText() ?: throw e
        }
        val r = JSONObject(body).getJSONObject("subsonic-response")
        if (r.optString("status") != "ok") throw ApiException(r.optJSONObject("error")?.optString("message") ?: "Server error")
        if (fresh && copy != null) File.createTempFile("part", null, saved).apply { writeText(body) }.renameTo(copy)
        r
    }

    private suspend fun fetch(method: String, params: List<Pair<String, Any?>>): String {
        // Every endpoint just failed: answer at once so screens fall back to downloads without waiting on timeouts.
        if (System.currentTimeMillis() - lastFailedRace < 15_000) throw IOException("Server unreachable")
        val tried = active
        val body = try {
            get(url(method, params))
        } catch (e: IOException) {
            val next = pickFastest()
            if (next == null || next == tried) throw e
            get(url(method, params))
        }
        online.value = true
        return body
    }

    /** Server answers kept on disk, so screens open at once and still open offline. */
    private val saved = File(App.it.cacheDir, "meta").apply { mkdirs() }

    init {
        // ponytail: trims the least recently refreshed at startup to 64 MB; scans the whole folder, fine for thousands of files.
        scope.launch(Dispatchers.IO) {
            var total = 0L
            saved.listFiles()?.sortedByDescending { it.lastModified() }?.forEach { total += it.length(); if (total > 64L shl 20) it.delete() }
        }
    }

    fun savedBytes() = saved.listFiles()?.sumOf { it.length() } ?: 0L

    fun clearSaved() = saved.listFiles()?.forEach { it.delete() }

    private fun get(url: String): String = http.newCall(Request.Builder().url(url).build()).execute().use {
        if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
        it.body!!.string()
    }

    /** For lookups that never change during a session. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> once(key: String, load: suspend () -> T): T = memo[key] as T? ?: load().also { memo[key] = it }

    // ---- endpoints ----

    suspend fun playlists() = run { call("getPlaylists").getJSONObject("playlists").list("playlist").map(Playlist::from) }

    suspend fun albumList(type: String, size: Int = 20, offset: Int = 0) =
        call("getAlbumList2", "type" to type, "size" to size, "offset" to offset)
            .getJSONObject("albumList2").list("album").map(Album::from)

    suspend fun artists() = run {
        call("getArtists").getJSONObject("artists").list("index").flatMap { it.list("artist") }.map(Artist::from)
    }

    suspend fun starred() = call("getStarred2").getJSONObject("starred2")

    suspend fun collection(kind: Kind, id: String): Collection =
        try {
            when (kind) {
                Kind.Playlist -> call("getPlaylist", "id" to id).getJSONObject("playlist").let {
                    Collection.of(Playlist.from(it), it.list("entry").map(Song::from))
                }
                Kind.Album -> call("getAlbum", "id" to id).getJSONObject("album").let {
                    val a = Album.from(it)
                    Collection(Kind.Album, id, a.name, null, a.artist, a.coverArt, it.list("song").map(Song::from), year = a.year, artistId = a.artistId, starred = a.starred)
                }
                Kind.Liked -> Collection(Kind.Liked, "liked", "Liked Songs", null, prefs.username, null,
                    starred().list("song").map(Song::from).reversed())
                Kind.Genre -> Collection(Kind.Genre, id, id, null, null, runCatching { genreCover(id) }.getOrNull(),
                    call("getSongsByGenre", "genre" to id, "count" to 500).getJSONObject("songsByGenre").list("song").map(Song::from))
                Kind.Artist -> error("artists are not collections")
            }
        } catch (e: IOException) {
            App.it.offline.snapshot(kind, id) ?: throw e
        }

    suspend fun artist(id: String) = run {
        val o = call("getArtist", "id" to id).getJSONObject("artist")
        val artist = Artist.from(o)
        val top = runCatching {
            call("getTopSongs", "artist" to artist.name, "count" to 10).optJSONObject("topSongs")?.list("song")?.map(Song::from)
        }.getOrNull().orEmpty()
        val own = o.list("album").map(Album::from)
        // getArtist only lists albums credited to this artist. Their songs on releases credited to someone
        // else (a duo, a label, a compilation) are found by searching; a release made only of their songs is theirs.
        val songs = runCatching {
            call("search3", "query" to artist.name, "artistCount" to 0, "albumCount" to 0, "songCount" to 200)
                .optJSONObject("searchResult3")?.list("song")?.filter { it.isBy(id) }
        }.getOrNull().orEmpty()
        val known = own.map { it.id }.toSet()
        // ponytail: one getAlbum per extra release, capped at 20; fine for featured artists, not for prolific session players.
        val extra = coroutineScope {
            songs.mapNotNull { it.s("albumId") }.distinct().filter { it !in known }.take(20)
                .map { async { runCatching { call("getAlbum", "id" to it).getJSONObject("album") }.getOrNull() } }
                .mapNotNull { it.await() }
        }
        val (theirs, others) = extra.partition { a -> a.list("song").all { it.isBy(id) } }
        ArtistPage(
            artist, (own + theirs.map(Album::from)).sortedByDescending { it.year },
            top.ifEmpty { songs.map(Song::from).take(10) }, others.map(Album::from).sortedByDescending { it.year },
        )
    }

    /**
     * Everyone on a song: the artists the server lists, plus each name in "A, B & C feat. D" that is an
     * artist of its own, since servers often keep such credits as one combined artist.
     */
    suspend fun songArtists(song: Song): List<Artist> = coroutineScope {
        val listed = song.artists.ifEmpty { listOfNotNull(song.artistId?.let { it to song.artist }) }
        val names = listed.map { it.second.lowercase() }.toSet()
        val split = artistNames(song.artist).filter { it.lowercase() !in names }
            .map { async { runCatching { findArtist(it) }.getOrNull() } }.awaitAll().filterNotNull()
        (listed.map { (id, name) -> Artist(id, name, "ar-$id", 0) } + split).distinctBy { it.id }
    }

    private fun JSONObject.isBy(artistId: String) =
        s("artistId") == artistId || optJSONArray("artists").objects().any { it.s("id") == artistId }

    suspend fun artistSongs(page: ArtistPage): List<Song> = page.albums.flatMap { collection(Kind.Album, it.id).songs }

    /**
     * Cover for an artist name, as the web UI picks it: the artist's most played album
     * (most artists have no photo). "A, B" falls back to "A". Daily Mix / Artist Radio art uses this.
     */
    suspend fun artistCover(name: String): String? = once("artistCover:$name") {
        val artist = findArtist(name) ?: name.substringBefore(", ").takeIf { it != name }?.let { findArtist(it) }
        artist?.let { topAlbumCover(it.id) ?: it.coverArt } ?: ""
    }.ifEmpty { null }

    private suspend fun findArtist(name: String) =
        call("search3", "query" to name, "artistCount" to 10, "albumCount" to 0, "songCount" to 0)
            .optJSONObject("searchResult3")?.list("artist")?.map(Artist::from)
            ?.firstOrNull { it.name.equals(name, true) }

    private suspend fun topAlbumCover(artistId: String) =
        call("getArtist", "id" to artistId).getJSONObject("artist").list("album").map(Album::from).maxByOrNull { it.playCount }?.coverArt

    /**
     * Up to 20 songs like [seeds] and not in [exclude], from the server's similar-artist data (Last.fm and the like);
     * random songs when it has none.
     */
    suspend fun recommendations(seeds: List<String>, exclude: Set<String>): List<Song> = coroutineScope {
        fun List<Song>.fresh() = filter { it.id !in exclude }.distinctBy { it.id }
        val similar = seeds.map { id ->
            async { runCatching { call("getSimilarSongs2", "id" to id, "count" to 10).optJSONObject("similarSongs2")?.list("song")?.map(Song::from) }.getOrNull().orEmpty() }
        }.awaitAll().flatten().fresh()
        similar.ifEmpty { call("getRandomSongs", "size" to 50).optJSONObject("randomSongs")?.list("song")?.map(Song::from).orEmpty().fresh() }
            .shuffled().take(20)
    }

    suspend fun search(q: String): SearchResult {
        val r = call("search3", "query" to q, "artistCount" to 6, "albumCount" to 12, "songCount" to 30)
            .optJSONObject("searchResult3") ?: JSONObject()
        return SearchResult(r.list("artist").map(Artist::from), r.list("album").map(Album::from), r.list("song").map(Song::from))
    }

    suspend fun genres() = once("genres") {
        call("getGenres").getJSONObject("genres").list("genre")
            .map { Genre(it.optString("value"), it.optInt("songCount")) }
            .filter { it.name.isNotBlank() }
            .sortedByDescending { it.songCount }
    }

    private val genreCoversTaken = mutableSetOf<String>()

    /** Art for a genre card: one of its albums, avoiding covers another genre card already shows. */
    suspend fun genreCover(name: String): String? = once("genreCover:$name") {
        val covers = call("getAlbumList2", "type" to "byGenre", "genre" to name, "size" to 10).getJSONObject("albumList2")
            .list("album").mapNotNull { Album.from(it).coverArt }
        synchronized(genreCoversTaken) {
            (covers.firstOrNull { it !in genreCoversTaken } ?: covers.firstOrNull())?.also { genreCoversTaken += it }
        } ?: ""
    }.ifEmpty { null }

    suspend fun lyrics(id: String): Lyrics? = once("lyrics:$id") {
        val list = call("getLyricsBySongId", "id" to id).optJSONObject("lyricsList")?.list("structuredLyrics").orEmpty()
        val pick = list.firstOrNull { it.optBoolean("synced") } ?: list.firstOrNull()
        val synced = pick?.optBoolean("synced") == true
        val offset = pick?.optLong("offset") ?: 0
        Lyrics(synced, pick?.list("line").orEmpty().map { LyricLine(if (synced) it.optLong("start") + offset else -1, it.optString("value")) }.filterNot { isLrcTag(it.text) })
    }.takeIf { it.lines.isNotEmpty() }

    /** [param] is "id" for songs, "albumId" or "artistId" for those. */
    suspend fun setStarred(id: String, on: Boolean, param: String = "id") {
        call(if (on) "star" else "unstar", param to id)
    }

    suspend fun addToPlaylist(playlistId: String, songIds: List<String>) {
        call("updatePlaylist", "playlistId" to playlistId, *songIds.map { "songIdToAdd" to it }.toTypedArray())
    }

    /** Returns the new playlist's id. */
    suspend fun createPlaylist(name: String, songIds: List<String>): String? =
        call("createPlaylist", "name" to name, *songIds.map { "songId" to it }.toTypedArray()).optJSONObject("playlist")?.optString("id")

    // ---- scrobbling: also called for downloaded songs, so Navidrome's stats stay complete ----

    /** Network went away or came back; forget the fail-fast window so the next call tries again. */
    fun networkChanged(available: Boolean) {
        lastFailedRace = 0
        // Pooled connections belong to the old network and would hang until they time out.
        // Closing a TLS socket writes to it, so not on the main thread (pull to refresh calls this).
        scope.launch(Dispatchers.IO) { http.connectionPool.evictAll() }
        if (!available) online.value = false
    }

    fun nowPlaying(id: String) = scope.launch {
        runCatching { call("scrobble", "id" to id, "submission" to false) }
    }

    fun scrobble(id: String, playedAt: Long) = scope.launch {
        try {
            call("scrobble", "id" to id, "time" to playedAt, "submission" to true)
        } catch (_: Exception) {
            synchronized(prefs) { prefs.pendingScrobbles += "$id:$playedAt" }
        }
    }

    private suspend fun flushScrobbles() {
        for (entry in prefs.pendingScrobbles) {
            val (id, time) = entry.split(':')
            try {
                call("scrobble", "id" to id, "time" to time, "submission" to true)
            } catch (_: IOException) {
                return
            } catch (_: ApiException) {
                // the song is gone from the server; drop it
            }
            synchronized(prefs) { prefs.pendingScrobbles -= entry }
        }
    }

    companion object {
        /** Validates credentials against [endpoint] without touching the saved session. */
        fun login(endpoint: String, user: String, password: String): Pair<String, String> {
            val salt = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
            val token = md5(password + salt)
            val url = "$endpoint/rest/ping".toHttpUrl().newBuilder()
                .addQueryParameter("u", user).addQueryParameter("t", token).addQueryParameter("s", salt)
                .addQueryParameter("v", "1.16.1").addQueryParameter("c", "navidroid").addQueryParameter("f", "json")
                .build()
            val body = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
                .newCall(Request.Builder().url(url).build()).execute().use { it.body!!.string() }
            val r = JSONObject(body).getJSONObject("subsonic-response")
            if (r.optString("status") != "ok") throw ApiException(r.optJSONObject("error")?.optString("message") ?: "Login failed")
            return token to salt
        }
    }
}
