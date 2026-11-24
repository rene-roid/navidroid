package xyz.yuuki.navidroid

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

// ---- the session as data --------------------------------------------------------------------

/** Where a guest's song goes: right after the current song, guests taking turns, or behind everything queued. */
enum class JamOrder(val label: String) { Turns("After the current song, in turns"), End("At the end of the queue") }

/** How many guests have to ask before a song is skipped. */
enum class SkipVotes(val label: String) { Off("Off"), One("1"), Two("2"), Half("Half") }

data class JamSettings(
    val name: String = "Jam",
    val canAdd: Boolean = true,
    /** Songs one guest may have waiting; 0 is no limit. */
    val maxPerGuest: Int = 3,
    val skip: SkipVotes = SkipVotes.Half,
    val order: JamOrder = JamOrder.Turns,
) {
    fun toJson(): JSONObject = JSONObject().put("name", name).put("canAdd", canAdd).put("maxPerGuest", maxPerGuest).put("skip", skip.name).put("order", order.name)

    companion object {
        fun from(o: JSONObject) = JamSettings(
            o.optString("name", "Jam"), o.optBoolean("canAdd", true), o.optInt("maxPerGuest", 3).coerceIn(0, 10),
            SkipVotes.entries.firstOrNull { it.name == o.optString("skip") } ?: SkipVotes.Half,
            JamOrder.entries.firstOrNull { it.name == o.optString("order") } ?: JamOrder.Turns,
        )
    }
}

/** [id] is the secret a guest's requests carry; [name] is what everyone sees, unique within the Jam. */
data class Guest(val id: String, val name: String, val lastSeen: Long)

/** A song in the Jam at timeline [index]. [by]: the guest who added it, null for the host's own. */
data class JamSong(val song: Song, val by: String?, val index: Int) {
    fun toJson(): JSONObject = song.toJson().put("by", by).put("index", index)

    companion object {
        fun from(o: JSONObject) = JamSong(Song.from(o), o.s("by"), o.optInt("index"))
    }
}

/** What one guest sees, served at `/j/<code>/state`. [mine], [voted] and [you] are about the guest asking. */
data class JamState(
    val name: String,
    val you: String,
    val guests: List<String>,
    val canAdd: Boolean,
    val maxPerGuest: Int,
    val mine: Int,
    /** 0 when guests can't vote to skip. */
    val votesNeeded: Int,
    val votes: Int,
    val voted: Boolean,
    val now: JamSong?,
    val positionMs: Long,
    val playing: Boolean,
    val queue: List<JamSong>,
) {
    fun toJson(): JSONObject = JSONObject().put("name", name).put("you", you).put("guests", JSONArray(guests)).put("canAdd", canAdd)
        .put("maxPerGuest", maxPerGuest).put("mine", mine).put("votesNeeded", votesNeeded).put("votes", votes).put("voted", voted)
        .put("now", now?.toJson()).put("positionMs", positionMs).put("playing", playing).put("queue", JSONArray(queue.map { it.toJson() }))

    companion object {
        fun from(o: JSONObject) = JamState(
            o.optString("name"), o.optString("you"), o.optJSONArray("guests").let { a -> List(a?.length() ?: 0) { a!!.getString(it) } },
            o.optBoolean("canAdd"), o.optInt("maxPerGuest"), o.optInt("mine"), o.optInt("votesNeeded"), o.optInt("votes"), o.optBoolean("voted"),
            o.optJSONObject("now")?.let(JamSong::from), o.optLong("positionMs"), o.optBoolean("playing"), o.list("queue").map(JamSong::from),
        )
    }
}

// ---- the rules ------------------------------------------------------------------------------

/**
 * Where [guest]'s new song goes in [queue], the play order as each song's guest (null: not a guest's), with [current] playing.
 * In turns: the guests' songs sit right behind the current one in rounds of one song each, and the new one joins the first
 * round its guest isn't in yet, so nobody plays twice while someone else waits.
 */
fun jamInsertIndex(queue: List<String?>, current: Int, guest: String, order: JamOrder): Int {
    if (order == JamOrder.End) return queue.size
    var round = setOfNotNull(queue.getOrNull(current))
    var i = current + 1
    while (i < queue.size) {
        val g = queue[i] ?: break
        if (g in round) {
            if (guest !in round) return i
            round = emptySet()
        }
        round = round + g
        i++
    }
    return i
}

/** Whether [guest] is still under [max] songs waiting behind [current]; 0 is no limit. */
fun jamCanAdd(queue: List<String?>, current: Int, guest: String, max: Int) = max == 0 || queue.drop(current + 1).count { it == guest } < max

/** Votes that skip a song with [guests] in the Jam; 0 when voting is off. Never more than there are guests to give them. */
fun votesNeeded(rule: SkipVotes, guests: Int) = when (rule) {
    SkipVotes.Off -> 0
    SkipVotes.One -> 1
    SkipVotes.Two -> minOf(2, guests).coerceAtLeast(1)
    SkipVotes.Half -> ((guests + 1) / 2).coerceAtLeast(1)
}

/** A name as guests may have it: trimmed, one line, 24 characters, and told apart from the [taken] ones by a number. Null when empty. */
fun guestName(input: String, taken: List<String>): String? {
    val name = input.filterNot { it.isISOControl() }.trim().take(24).trim().ifEmpty { return null }
    return generateSequence(1) { it + 1 }.map { if (it == 1) name else "$name $it" }.first { it !in taken }
}

/** The `http://host:port/j/code` a Jam link points at, whether pasted as is or wrapped in `navidroid://jam?url=`. Null when it isn't one. */
fun jamBase(link: String): String? {
    val l = link.trim()
    val url = if (l.startsWith("navidroid://jam?url=")) runCatching { URLDecoder.decode(l.substringAfter("url="), "UTF-8") }.getOrNull() ?: return null else l
    return Regex("""^http://[A-Za-z0-9.\-]+(:\d{1,5})?/j/[A-Za-z0-9]{6}""").find(url)?.value
}

// ---- HTTP -----------------------------------------------------------------------------------

/** [path] and [query] are already decoded. */
data class HttpRequest(val method: String, val path: List<String>, val query: Map<String, String>)

/** Reads the request line of an HTTP/1.x head; null when it isn't one. Headers aren't needed: everything a guest sends is in the URL. */
fun parseRequest(head: String): HttpRequest? {
    val p = head.lineSequence().first().trim().split(' ')
    if (p.size != 3 || !p[2].startsWith("HTTP/1.") || !p[1].startsWith("/")) return null
    return try {
        fun dec(s: String) = URLDecoder.decode(s, "UTF-8")
        HttpRequest(
            p[0],
            p[1].substringBefore('?').split('/').filter { it.isNotEmpty() }.map(::dec),
            p[1].substringAfter('?', "").split('&').filter { it.isNotEmpty() }.associate { dec(it.substringBefore('=')) to dec(it.substringAfter('=', "")) },
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** The head of a request, up to its blank line; null when it ends early or runs past [max] bytes. */
fun readHead(input: InputStream, max: Int = 8192): String? {
    val b = StringBuilder()
    while (b.length < max) {
        val c = input.read()
        if (c < 0) return null
        b.append(c.toChar())
        if (c == '\n'.code && b.endsWith("\r\n\r\n")) return b.toString()
    }
    return null
}

class Reply(val code: Int, val body: ByteArray, val type: String = "application/json")

private fun ok(o: JSONObject = JSONObject()) = Reply(200, o.toString().toByteArray())
private fun err(code: Int, message: String) = Reply(code, JSONObject().put("error", message).toString().toByteArray())

// ---- the host -------------------------------------------------------------------------------

/**
 * A Jam: the phone playing the music serves a page on the Wi-Fi where guests add songs to its queue and vote to skip.
 * Lives with the playback service, which hands over its [player], so it carries on with the screen off.
 */
object Jam {
    /** [guests]: the names of those seen in the last seconds. */
    data class Session(val url: String, val guests: List<String>)

    private const val PORT = 8765
    private const val ACTIVE_MS = 15_000L
    private const val MAX_GUESTS = 50

    val settings by lazy { MutableStateFlow(App.it.prefs.jam?.let { runCatching { JamSettings.from(JSONObject(it)) }.getOrNull() } ?: JamSettings()) }
    val session = MutableStateFlow<Session?>(null)
    @Volatile var player: ExoPlayer? = null

    @Volatile private var server: ServerSocket? = null
    @Volatile private var code = ""
    private var pool: ExecutorService? = null
    private var ticker: Job? = null
    private val guests = ConcurrentHashMap<String, Guest>()
    private val lastAdd = ConcurrentHashMap<String, Long>()
    // Only touched on the main thread, like the player.
    private var votes = emptySet<String>()
    private var votesFor: String? = null

    fun update(s: JamSettings) {
        settings.value = s
        App.it.prefs.jam = s.toJson().toString()
    }

    /** Opens the Jam; returns what went wrong, or null. */
    suspend fun start(): String? = withContext(Dispatchers.IO) {
        if (server != null) return@withContext null
        val ip = lanAddress() ?: return@withContext "Connect to Wi-Fi to start a Jam"
        val any = InetAddress.getByAddress(ByteArray(4))
        val s = try {
            runCatching { ServerSocket(PORT, 50, any) }.getOrElse { ServerSocket(0, 50, any) }
        } catch (e: IOException) {
            return@withContext "Couldn't open the Jam: ${e.message}"
        }
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        code = SecureRandom().let { r -> String(CharArray(6) { alphabet[r.nextInt(alphabet.length)] }) }
        // ponytail: 8 requests at a time and the rest wait in line; plenty for a room of phones polling every few seconds.
        val p = Executors.newFixedThreadPool(8)
        pool = p
        server = s
        thread(name = "jam", isDaemon = true) {
            while (true) {
                val c = try { s.accept() } catch (_: IOException) { break }
                runCatching { p.execute { serve(c) } }.onFailure { c.close() }
            }
        }
        session.value = Session("http://$ip:${s.localPort}/j/$code", emptyList())
        ticker = App.it.scope.launch {
            while (isActive) {
                delay(3_000)
                refresh()
            }
        }
        null
    }

    fun stop() {
        ticker?.cancel()
        runCatching { server?.close() }
        server = null
        pool?.shutdownNow()
        pool = null
        guests.clear()
        lastAdd.clear()
        session.value = null
    }

    private fun active() = System.currentTimeMillis().let { now -> guests.values.filter { now - it.lastSeen < ACTIVE_MS } }

    private fun refresh() = session.update { it?.copy(guests = active().map(Guest::name).sorted()) }

    /** The Wi-Fi address, or failing that any private one, which is what the phone has as a hotspot. */
    @Suppress("DEPRECATION")
    private fun lanAddress(): String? {
        val cm = App.it.getSystemService(ConnectivityManager::class.java)
        return cm.allNetworks.filter { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
            .firstNotNullOfOrNull { n -> cm.getLinkProperties(n)?.linkAddresses?.firstNotNullOfOrNull { (it.address as? Inet4Address)?.hostAddress } }
            ?: runCatching {
                NetworkInterface.getNetworkInterfaces().asSequence().filter { it.isUp && !it.isLoopback }.flatMap { it.inetAddresses.asSequence() }
                    .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
            }.getOrNull()
    }

    private fun serve(c: Socket) = runCatching {
        c.use {
            c.soTimeout = 5_000
            // The Jam is for the people in the room: nothing from beyond the local network gets an answer.
            if (!c.inetAddress.run { isSiteLocalAddress || isLoopbackAddress || isLinkLocalAddress }) return@use
            val req = readHead(c.getInputStream().buffered())?.let(::parseRequest)
            val r = if (req == null) err(400, "Bad request") else runCatching { handle(req) }.getOrElse { err(500, "Something went wrong") }
            c.getOutputStream().run {
                val cache = if (r.type.startsWith("image/")) "max-age=86400" else "no-store"
                write("HTTP/1.1 ${r.code} ${if (r.code < 400) "OK" else "Error"}\r\nContent-Type: ${r.type}\r\nContent-Length: ${r.body.size}\r\nCache-Control: $cache\r\nConnection: close\r\n\r\n".toByteArray())
                write(r.body)
                flush()
            }
        }
    }

    private fun <T> onMain(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun handle(req: HttpRequest): Reply {
        val p = req.path
        if (p.size < 2 || p[0] != "j" || !MessageDigest.isEqual(p[1].toByteArray(), code.toByteArray())) return err(403, "This Jam has ended")
        val post = req.method == "POST"
        if (!post && req.method != "GET") return err(405, "Not allowed")
        val q = req.query
        when (p.getOrNull(2)) {
            null -> return Reply(200, App.it.assets.open("jam.html").use { it.readBytes() }, "text/html; charset=utf-8")
            "cover" -> return cover(p.getOrNull(3).orEmpty())
            "join" -> return if (post) join(q["g"], q["name"].orEmpty()) else err(405, "Not allowed")
        }
        val now = System.currentTimeMillis()
        val g = q["g"]?.let { guests.computeIfPresent(it) { _, v -> v.copy(lastSeen = now) } } ?: return err(401, "Join the Jam first")
        return when (p[2]) {
            "state" -> onMain { state(g) }?.let { ok(it.toJson()) } ?: err(503, "The player isn't running")
            "search" -> {
                val text = q["q"].orEmpty().trim().take(100)
                val songs = if (text.isEmpty()) emptyList() else runBlocking { App.it.api.search(text) }.songs.take(30)
                ok(JSONObject().put("songs", JSONArray(songs.map { it.toJson() })))
            }
            "add" -> if (post) add(g, q["id"].orEmpty(), now) else err(405, "Not allowed")
            "skip" -> if (post) onMain { skip(g) } else err(405, "Not allowed")
            "remove" -> if (post) onMain { remove(g, q["index"]?.toIntOrNull() ?: -1, q["id"].orEmpty()) } else err(405, "Not allowed")
            else -> err(404, "Not found")
        }
    }

    private fun join(id: String?, name: String): Reply {
        val now = System.currentTimeMillis()
        // Coming back with the id the page kept: the same guest, under the same name.
        val known = id?.let { guests.computeIfPresent(it) { _, v -> v.copy(lastSeen = now) } }
        val g = known ?: synchronized(guests) {
            if (guests.size >= MAX_GUESTS) guests.values.removeIf { now - it.lastSeen > ACTIVE_MS }
            if (guests.size >= MAX_GUESTS) return err(403, "This Jam is full")
            val n = guestName(name, guests.values.map { it.name }) ?: return err(400, "Type your name")
            Guest(ByteArray(12).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }, n, now).also { guests[it.id] = it }
        }
        refresh()
        return ok(JSONObject().put("id", g.id).put("name", g.name))
    }

    /** Covers go through the host, so guests never see its server or its login. */
    private fun cover(id: String): Reply {
        if (id.isEmpty() || id.length > 128) return err(404, "No cover")
        val app = App.it
        val bytes = app.offline.coverFile(id).takeIf { it.isFile }?.readBytes()
            ?: app.imageHttp.newCall(Request.Builder().url(app.api.coverUrl(id, 300)).build()).execute().use { r ->
                if (!r.isSuccessful || r.body?.contentType()?.type != "image") return err(404, "No cover")
                r.body!!.bytes()
            }
        // WebP when the app re-encoded it, otherwise whatever the server had, nearly always JPEG.
        return Reply(200, bytes, if (bytes.size > 4 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "RIFF") "image/webp" else "image/jpeg")
    }

    /** Timeline indices in the order they play, which shuffle makes its own. */
    private fun playOrder(p: Player): List<Int> {
        val t = p.currentTimeline
        val shuffle = p.shuffleModeEnabled
        return generateSequence(t.getFirstWindowIndex(shuffle).takeIf { it != C.INDEX_UNSET }) { i ->
            t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, shuffle).takeIf { it != C.INDEX_UNSET }
        }.toList()
    }

    /** Votes are for one song: a new one starts from none. Those of guests who left don't count. */
    private fun countVotes(p: Player): Pair<Int, Int> {
        val id = p.currentMediaItem?.mediaId
        if (id != votesFor) {
            votesFor = id
            votes = emptySet()
        }
        val here = active().map { it.id }
        votes = votes.filter { it in here }.toSet()
        return votes.size to votesNeeded(settings.value.skip, here.size)
    }

    private fun state(g: Guest): JamState? {
        val p = player ?: return null
        val s = settings.value
        val order = playOrder(p)
        fun at(i: Int) = p.getMediaItemAt(i).run { song?.let { JamSong(it, jam, i) } }
        val next = order.drop(order.indexOf(p.currentMediaItemIndex) + 1).mapNotNull(::at)
        val (n, needed) = countVotes(p)
        return JamState(
            s.name, g.name, active().map(Guest::name).sorted(), s.canAdd, s.maxPerGuest, next.count { it.by == g.name }, needed, n, g.id in votes,
            if (p.mediaItemCount > 0) at(p.currentMediaItemIndex) else null, p.currentPosition, p.isPlaying, next.take(100),
        )
    }

    private fun add(g: Guest, id: String, now: Long): Reply {
        val s = settings.value
        if (!s.canAdd) return err(403, "The host turned off adding songs")
        if (id.isEmpty() || id.length > 128) return err(400, "No song")
        if (now - (lastAdd.put(g.id, now) ?: 0) < 2_000) return err(429, "One at a time, try again in a moment")
        // Only the id comes from the guest; what gets queued is what the host's server says that song is.
        val song = runCatching { Song.from(runBlocking { App.it.api.call("getSong", "id" to id) }.getJSONObject("song")) }.getOrNull() ?: return err(404, "Song not found")
        return onMain {
            val p = player ?: return@onMain err(503, "The player isn't running")
            val item = song.toMediaItem(jam = g.name)
            if (p.mediaItemCount == 0) {
                p.setMediaItem(item)
                p.prepare()
                p.play()
                return@onMain ok()
            }
            val order = playOrder(p)
            val at = order.indexOf(p.currentMediaItemIndex)
            if (order.drop(at + 1).any { p.getMediaItemAt(it).mediaId == id }) return@onMain err(409, "Already in the queue")
            // Songs autoplay tacked on are filler: "at the end" means in front of them.
            val own = maxOf(at, order.indexOfLast { p.getMediaItemAt(it).auto == null }) + 1
            val jams = order.take(own).map { p.getMediaItemAt(it).jam }
            if (!jamCanAdd(jams, at, g.name, s.maxPerGuest)) return@onMain err(403, "You already have ${s.maxPerGuest} waiting. Add more once one has played")
            val pos = jamInsertIndex(jams, at, g.name, s.order)
            if (p.shuffleModeEnabled) {
                // A shuffled queue plays in an order of its own, so the song goes last in the list and into its place in that order.
                val n = p.mediaItemCount
                p.addMediaItem(item)
                p.setShuffleOrder(DefaultShuffleOrder((order.take(pos) + n + order.drop(pos)).toIntArray(), System.nanoTime()))
            } else p.addMediaItem(pos, item)
            ok()
        }
    }

    private fun skip(g: Guest): Reply {
        val p = player ?: return err(503, "The player isn't running")
        if (settings.value.skip == SkipVotes.Off) return err(403, "The host turned off voting")
        countVotes(p)
        votes = votes + g.id
        val (n, needed) = countVotes(p)
        if (n >= needed && p.hasNextMediaItem()) p.seekToNextMediaItem()
        return ok()
    }

    /** A guest takes back a song of their own that hasn't started. */
    private fun remove(g: Guest, index: Int, id: String): Reply {
        val p = player ?: return err(503, "The player isn't running")
        val item = if (index in 0 until p.mediaItemCount && index != p.currentMediaItemIndex) p.getMediaItemAt(index) else null
        if (item?.mediaId != id || item.jam != g.name) return err(403, "That isn't yours to remove")
        p.removeMediaItem(index)
        return ok()
    }
}
