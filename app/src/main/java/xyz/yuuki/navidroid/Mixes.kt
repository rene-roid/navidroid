package xyz.yuuki.navidroid

import org.json.JSONObject

/**
 * Generated playlists (NaviBeat's `nb1:` comments and Explo's), parsed the same way the
 * server's "spotify" web UI does, so the app shows the same titles, colours and sections.
 */
data class Mix(
    val kind: String,
    val slot: String,
    val n: Int,
    val title: String,
    val subject: String?,
    val description: String,
    val by: String,
    val color: Long,
    val photo: Boolean,
    val section: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("kind", kind).put("slot", slot).put("n", n).put("title", title).put("subject", subject)
        .put("description", description).put("by", by).put("color", color).put("photo", photo).put("section", section)

    companion object {
        fun from(o: JSONObject) = Mix(
            o.getString("kind"), o.getString("slot"), o.optInt("n"), o.getString("title"), o.s("subject"),
            o.getString("description"), o.getString("by"), o.getLong("color"), o.optBoolean("photo"), o.getString("section"),
        )
    }
}

val PALETTE = longArrayOf(0xFFE8115B, 0xFF148A08, 0xFF509BF5, 0xFFBA5D07, 0xFF8D67AB, 0xFF1E3264, 0xFFE1118C, 0xFF27856A)

/** The web UI's string hash, kept bit-identical so every mix gets the same colour as on the web. */
fun colorHash(s: String?): Int {
    var h = 7L
    (s ?: "").codePoints().forEach { cp -> h = (h * 31 + Character.toChars(cp)[0].code) and 0xFFFFFFFFL }
    return (h % PALETTE.size).toInt()
}

fun paletteColor(s: String?) = PALETTE[colorHash(s)]

private val NB = Regex("(^|\n)nb1:([^:\n]+):([^:\n]+)")
private val EXPLO = Regex("^(Created for .+ by Explo|Built .+ by explo-maintainer)")
private val EXPLO_ORDER = listOf("weekly-exploration", "daily-jams", "weekly-jams", "on-repeat")
private val EXPLO_COLORS = mapOf("weekly-exploration" to 0xFF1E8A6E, "daily-jams" to 0xFFF59B23, "weekly-jams" to 0xFFAF2896, "on-repeat" to 0xFFE91429)
private val KIND_ORDER = listOf(
    "dailymix", "discovery", "explo", "newmusic", "onrepeat", "rediscover", "artistradio", "genreradio",
    "timeofday", "essentials", "loved", "wrapped", "decade",
)
val DAYPARTS = listOf("morning", "afternoon", "evening", "night")

fun daypart(hour: Int) = when (hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    in 17..21 -> "evening"
    else -> "night"
}

fun parseMix(name: String, comment: String): Mix? {
    if (EXPLO.containsMatchIn(comment)) {
        val slot = name.lowercase().replace(Regex("[^a-z0-9]+"), "-")
        val source = Regex("using (.+?) recommendations").find(comment)?.groupValues?.get(1)
        return Mix(
            kind = "explo", slot = slot,
            n = EXPLO_ORDER.indexOf(slot).takeIf { it >= 0 } ?: EXPLO_ORDER.size,
            title = name.replace('-', ' '), subject = null,
            description = when {
                source != null -> "From your $source recommendations"
                "from your play history" in comment -> "Built from your play history"
                else -> "Recommended for you"
            },
            by = source ?: "Explo",
            color = EXPLO_COLORS[slot] ?: paletteColor(slot),
            photo = false, section = "madeForYou",
        )
    }
    val m = NB.find(comment) ?: return null
    val kind = m.groupValues[2]
    val slot = m.groupValues[3]
    val first = comment.lineSequence().first().removePrefix("NaviBeat Mixes:").trim()
    val n = Regex("(\\d+)$").find(slot)?.value?.toInt() ?: 0
    var title = name.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trim()
    var subject: String? = null
    var desc = (Regex("^(.+?\\.)(\\s|$)").find(first)?.groupValues?.get(1) ?: first).replaceFirstChar { it.uppercase() }
    var color = paletteColor(slot)
    var photo = true
    when (kind) {
        "dailymix" -> {
            subject = Regex("built around (.+?) and what sits near it").find(first)?.groupValues?.get(1)
            if (subject != null) desc = "$subject and more"
            color = PALETTE[Math.floorMod(n - 1, PALETTE.size)]
        }
        "artistradio" -> {
            subject = Regex("everything you have by (.+?)\\.(\\s|$)").find(first)?.groupValues?.get(1)
            if (subject != null) {
                title = "$subject Radio"
                desc = "Everything you have by $subject"
            }
            color = PALETTE[(n + 2) % PALETTE.size]
        }
        "genreradio" -> {
            subject = Regex("^(.+?), one of the genres").find(first)?.groupValues?.get(1)
            if (subject != null) {
                title = "$subject Mix"
                desc = "$subject, one of the genres you play most"
            }
            color = paletteColor(subject)
        }
        "timeofday" -> color = mapOf("morning" to 0xFFF59B23, "afternoon" to 0xFFE8115B, "evening" to 0xFF7358FF, "night" to 0xFF1E3264)[slot] ?: color
        "decade" -> color = if (slot.endsWith("2010")) 0xFFDC148C else 0xFF27856A
        else -> {
            photo = false
            color = when (kind) {
                "discovery" -> 0xFF7358FF
                "newmusic" -> 0xFF0D73EC
                "onrepeat" -> 0xFFFF4632
                "rediscover" -> 0xFFBA5D07
                "essentials" -> 0xFF1E3264
                "loved" -> 0xFF450AF5
                "wrapped" -> 0xFFF037A5
                else -> color
            }
        }
    }
    val section = when (kind) {
        "artistradio", "genreradio" -> "topMixes"
        "timeofday" -> "timeOfDay"
        "essentials", "loved", "wrapped", "decade" -> "favorites"
        else -> "madeForYou"
    }
    return Mix(kind, slot, n, title, subject, desc, "NaviBeat", color, photo, section)
}

/** Sort key: by kind, then slot number; the time-of-day mixes start at the current part of the day. */
fun mixOrder(m: Mix, hour: Int): Int {
    val k = KIND_ORDER.indexOf(m.kind).takeIf { it >= 0 } ?: KIND_ORDER.size
    val i = if (m.kind == "timeofday") Math.floorMod(DAYPARTS.indexOf(m.slot) - DAYPARTS.indexOf(daypart(hour)), 4) else m.n
    return k * 100 + i
}

// ---- home -----------------------------------------------------------------------------------

/** One card on the home screen. */
data class Tile(
    val kind: Kind,
    val id: String,
    val title: String,
    val subtitle: String,
    val coverArt: String?,
    val mix: Mix? = null,
)

data class Section(val key: String, val title: String, val tiles: List<Tile>)
data class Home(val greeting: String, val quick: List<Tile>, val sections: List<Section>)

fun Playlist.tile() = Tile(Kind.Playlist, id, mix?.title ?: name, mix?.description ?: "By $owner", coverArt, mix)
fun Album.tile() = Tile(Kind.Album, id, name, listOfNotNull(year.takeIf { it > 0 }, artist.ifBlank { null }).joinToString(" • "), coverArt)
fun Artist.tile() = Tile(Kind.Artist, id, name, "Artist", coverArt)
fun likedTile(count: Int?) = Tile(Kind.Liked, "liked", "Liked Songs", if (count == null) "Playlist" else "Playlist • $count song${if (count == 1) "" else "s"}", null)

fun buildHome(
    name: String,
    hour: Int,
    playlists: List<Playlist>,
    recent: List<Album>,
    newest: List<Album>,
    frequent: List<Album>,
    likedCount: Int?,
): Home {
    val mixes = playlists.filter { it.mix != null }.sortedBy { mixOrder(it.mix!!, hour) }
    fun section(s: String) = mixes.filter { it.mix!!.section == s }.map { it.tile() }
    fun find(kind: String, slot: String? = null) = mixes.firstOrNull { it.mix!!.kind == kind && (slot == null || it.mix.slot == slot) }?.tile()

    // Navidrome has no "top artists" call, so rank artists by the play counts of their albums.
    val topArtists = frequent.filter { it.artistId != null }
        .groupBy { it.artistId!! }
        .map { (id, albums) -> Artist(id, albums.first().artist, albums.maxBy { it.playCount }.coverArt, albums.size) to albums.sumOf { it.playCount } }
        .sortedByDescending { it.second }
        .take(20)
        .map { it.first.tile() }

    val quick = (listOf(likedTile(likedCount)) + listOfNotNull(
        find("timeofday", daypart(hour)), find("onrepeat"),
        find("dailymix", "dailymix-1"), find("dailymix", "dailymix-2"), find("dailymix", "dailymix-3"),
        find("discovery"), find("explo", "weekly-exploration"),
    ) + recent.take(2).map { it.tile() }).take(8)

    val sections = listOf(
        Section("made-for-you", if (name.isBlank()) "Made For You" else "Made For $name", section("madeForYou")),
        Section("recently-played", "Recently played", recent.map { it.tile() }),
        Section("top-mixes", "Your top mixes", section("topMixes")),
        Section("time-of-day", "Mixes for your day", section("timeOfDay")),
        Section("your-playlists", "Your playlists", playlists.filter { it.mix == null }.map { it.tile() }),
        Section("favorites", "Your favorites, through the years", section("favorites")),
        Section("top-artists", "Your top artists", topArtists),
        Section("recently-added", "Recently added", newest.map { it.tile() }),
    ).filter { it.tiles.isNotEmpty() }

    val greeting = when (daypart(hour)) {
        "morning" -> "Good morning"
        "afternoon" -> "Good afternoon"
        else -> "Good evening"
    }
    return Home(greeting, quick, sections)
}
