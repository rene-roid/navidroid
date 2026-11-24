package xyz.yuuki.navidroid

/** One row of a ranking: an artist or an album, with how many times its songs were played. */
data class Ranked(val id: String, val name: String, val coverArt: String?, val plays: Int)

data class Stats(
    val artists: List<Ranked>,
    val albums: List<Ranked>,
    val songs: List<Song>,
    val plays: Int,
    val minutes: Long,
    /** First year of the decade, e.g. 1990. */
    val decade: Int?,
    val genre: String?,
    val discovered: List<Album>,
)

/**
 * What the server's per-album play counts add up to. [albums] are what getAlbumList2 "frequent" returns; [songs] are
 * those of the most played albums, the only ones whose own play counts are known. Albums "discovered" this [year] are
 * the ones added to the library this year that you have played, as the server doesn't say when a first play happened.
 */
fun computeStats(albums: List<Album>, songs: List<Song>, year: Int): Stats {
    val played = albums.filter { it.playCount > 0 }
    fun <K : Any> top(key: (Album) -> K?) = played.mapNotNull { a -> key(a)?.let { it to a.playCount } }
        .groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }.maxByOrNull { it.value }?.key
    return Stats(
        artists = played.filter { it.artistId != null }.groupBy { it.artistId!! }
            .map { (id, a) -> Ranked(id, a.first().artist, a.maxBy { it.playCount }.coverArt, a.sumOf { it.playCount }) }
            .sortedByDescending { it.plays }.take(10),
        albums = played.sortedByDescending { it.playCount }.take(10).map { Ranked(it.id, it.name, it.coverArt, it.playCount) },
        songs = songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(10),
        plays = played.sumOf { it.playCount },
        minutes = played.sumOf { it.playCount.toLong() * it.duration / it.songCount.coerceAtLeast(1) } / 60,
        decade = top { a -> a.year.takeIf { it > 0 }?.let { it / 10 * 10 } },
        genre = top { it.genre?.takeIf(String::isNotBlank) },
        discovered = played.filter { it.created?.startsWith("$year-") == true },
    )
}
