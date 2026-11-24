package xyz.yuuki.navidroid

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import xyz.yuuki.navidroid.ui.pinnedTiles
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Calendar

/**
 * A place in the library that Android Auto and other media browsers walk. They keep only its [mediaId] and hand it back
 * to open it or play it, so the id says everything about the place.
 */
sealed interface Node {
    data object Root : Node
    data object Home : Node
    data object Recent : Node
    data object Downloaded : Node

    /** The track list of a playlist, an album or Liked Songs. */
    data class Songs(val kind: Kind, val id: String) : Node

    /** One song. It plays with the rest of [from] around it; a search hit has none and plays alone. */
    data class Track(val songId: String, val from: Songs? = null) : Node

    val mediaId: String
        get() = when (this) {
            Root -> "root"
            Home -> "home"
            Recent -> "recent"
            Downloaded -> "downloaded"
            is Songs -> "songs/$kind/${enc(id)}"
            is Track -> "track/${enc(songId)}" + (from?.let { "/${it.kind}/${enc(it.id)}" } ?: "")
        }

    companion object {
        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

        /** Null for anything that isn't a place here, like the plain song ids the app's own queue uses. */
        fun parse(mediaId: String): Node? {
            val p = mediaId.split('/')
            fun songs(at: Int) = runCatching { Songs(Kind.valueOf(p[at]), dec(p[at + 1])) }.getOrNull()
            return when (p[0]) {
                "root" -> Root
                "home" -> Home
                "recent" -> Recent
                "downloaded" -> Downloaded
                "songs" -> songs(1)
                "track" -> p.getOrNull(1)?.let { Track(dec(it), if (p.size > 2) songs(2) ?: return null else null) }
                else -> null
            }
        }
    }
}

/** The top of the tree: what shows as tabs in a car. */
val BROWSE_TABS = listOf(Node.Home to "Home", Node.Recent to "Recently played", Node.Downloaded to "Downloaded")

/** Home for a media browser: Liked Songs, the playlists people made, then the generated mixes in the app's order. */
fun browseHome(playlists: List<Playlist>, hidden: Set<String>, hour: Int): List<Tile> {
    val (mixes, made) = playlists.filterNot { it.id in hidden }.partition { it.mix != null }
    return listOf(likedTile(null)) + made.map(::libraryPlaylist) + mixes.sortedBy { mixOrder(it.mix!!, hour) }.map(::libraryPlaylist)
}

/** Page [page] of a list handed out [size] at a time; a browser that wants it all asks for a page of Int.MAX_VALUE. */
fun <T> List<T>.page(page: Int, size: Int): List<T> = drop((page.toLong() * size).coerceIn(0, this.size.toLong()).toInt()).take(size.coerceAtLeast(0))

// ponytail: browsers fetch the art themselves from the server's address, so a car shows none offline or when it won't load plain http; a ContentProvider over the cover cache would fix both.
private fun art(coverArt: String?) = coverArt?.let { Uri.parse(App.it.api.coverUrl(it, 300)) }

private fun folder(node: Node, title: String, subtitle: String? = null, coverArt: String? = null, type: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED) =
    MediaItem.Builder().setMediaId(node.mediaId).setMediaMetadata(
        MediaMetadata.Builder().setTitle(title).setSubtitle(subtitle).setArtworkUri(art(coverArt)).setIsBrowsable(true).setIsPlayable(false).setMediaType(type).build(),
    ).build()

private fun Tile.browsable() = folder(Node.Songs(kind, id), title, subtitle, coverArt, if (kind == Kind.Album) MediaMetadata.MEDIA_TYPE_ALBUM else MediaMetadata.MEDIA_TYPE_PLAYLIST)

private fun Song.browsable(from: Node.Songs?) = toMediaItem().let {
    it.buildUpon().setMediaId(Node.Track(id, from).mediaId).setMediaMetadata(it.mediaMetadata.buildUpon().setArtworkUri(art(coverArt)).build()).build()
}

fun browseRoot() = folder(Node.Root, "Navidroid")

/** What's inside the place [mediaId] names; null when it names none. Signed out, every place is empty. */
suspend fun browseChildren(app: App, mediaId: String): List<MediaItem>? {
    val node = Node.parse(mediaId)
    if (node == Node.Root) return BROWSE_TABS.map { (n, title) -> folder(n, title) }
    if (!app.prefs.loggedIn) return emptyList<MediaItem>().takeIf { node != null && node !is Node.Track }
    return when (node) {
        Node.Home -> browseHome(app.api.playlists(), app.prefs.hiddenPlaylists, Calendar.getInstance().get(Calendar.HOUR_OF_DAY)).map { it.browsable() }
        Node.Recent -> app.api.albumList("recent", 30).map { it.tile().browsable() }
        Node.Downloaded -> pinnedTiles(app.offline.pins.value).map { it.browsable() }
        is Node.Songs -> app.api.collection(node.kind, node.id).songs.map { it.browsable(node) }
        else -> null
    }
}

suspend fun browseItem(app: App, mediaId: String): MediaItem? = when (val node = Node.parse(mediaId)) {
    Node.Root -> browseRoot()
    is Node.Songs -> app.api.collection(node.kind, node.id).let { Tile(it.kind, it.id, it.title, it.by.orEmpty(), it.coverArt).browsable() }
    is Node.Track -> app.api.song(node.songId).browsable(node.from)
    else -> BROWSE_TABS.firstOrNull { it.first == node }?.let { (n, title) -> folder(n, title) }
}

/** Search hits: albums to open, then songs to play. */
suspend fun browseSearch(app: App, query: String): List<MediaItem> =
    if (!app.prefs.loggedIn || query.isBlank()) emptyList() else app.api.search(query).let { r -> r.albums.take(10).map { it.tile().browsable() } + r.songs.map { it.browsable(null) } }

/** What a browser asked to hear: the [songs], which one starts, and what to say they're playing [from]. */
class Asked(val songs: List<Song>, val start: Int = 0, val from: String? = null)

/**
 * Turns a request to play a place in the tree, or whatever was asked for by voice, into songs. A song from a list brings
 * the list along, like tapping it in the app. Null when [item] is neither, so one of the app's own.
 */
suspend fun browsePlay(app: App, item: MediaItem): Asked? {
    val node = Node.parse(item.mediaId)
    val from = (node as? Node.Songs) ?: (node as? Node.Track)?.from
    val query = item.requestMetadata.searchQuery
    return when {
        from != null -> app.api.collection(from.kind, from.id).let { c -> Asked(c.songs, c.songs.indexOfFirst { it.id == (node as? Node.Track)?.songId }.coerceAtLeast(0), c.title) }
        node is Node.Track -> Asked(listOf(item.song ?: app.api.song(node.songId)))
        // "Play some music" comes with nothing to look for.
        node == null && query != null -> if (query.isBlank()) Asked(app.api.randomSongs(50)) else Asked(app.api.search(query).songs, from = query)
        else -> null
    }
}
