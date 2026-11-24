package xyz.yuuki.navidroid

enum class LibType { Playlists, Radios, Mixes, Albums, Artists }
enum class Facet { ByYou, ByOthers, Saved, Downloaded }
enum class LibSort { Default, Alphabetical, Creator }

data class LibFilter(val type: LibType? = null, val facets: Set<Facet> = emptySet()) {
    val active get() = type != null || facets.isNotEmpty()

    /** Picking a type keeps only the facets it offers. */
    fun pick(t: LibType?) = LibFilter(t, facets intersect FACETS.getValue(t).toSet())

    /** ByYou and ByOthers exclude each other. */
    fun toggle(f: Facet) = copy(
        facets = if (f in facets) facets - f else facets - (if (f == Facet.ByYou) Facet.ByOthers else if (f == Facet.ByOthers) Facet.ByYou else f) + f,
    )

    fun encode() = "${type?.name.orEmpty()}|${facets.joinToString(",") { it.name }}"

    companion object {
        fun decode(s: String): LibFilter {
            val p = s.split('|')
            return LibFilter(
                LibType.entries.firstOrNull { it.name == p[0] },
                p.getOrElse(1) { "" }.split(',').mapNotNull { n -> Facet.entries.firstOrNull { it.name == n } }.toSet(),
            )
        }
    }
}

/** The facet chips each type offers, in chip order; null is the unfiltered view. */
val FACETS: Map<LibType?, List<Facet>> = mapOf(
    null to listOf(Facet.Saved, Facet.Downloaded),
    LibType.Playlists to listOf(Facet.ByYou, Facet.ByOthers, Facet.Downloaded),
    LibType.Radios to listOf(Facet.Saved, Facet.Downloaded),
    LibType.Mixes to listOf(Facet.Saved, Facet.Downloaded),
    LibType.Albums to listOf(Facet.Saved, Facet.Downloaded),
    LibType.Artists to listOf(Facet.Saved),
)

private val RADIO_KINDS = setOf("artistradio", "genreradio", "artist-radio")

/** Which type filter a library row is under; downloaded genres only show with none picked. */
fun libType(t: Tile): LibType? = when {
    t.kind == Kind.Liked || t.kind == Kind.Playlist && t.mix == null -> LibType.Playlists
    t.kind == Kind.ArtistRadio || t.mix?.kind in RADIO_KINDS -> LibType.Radios
    t.mix != null -> LibType.Mixes
    t.kind == Kind.Album -> LibType.Albums
    t.kind == Kind.Artist -> LibType.Artists
    else -> null
}

/** Radios and Mixes chips only when there is something under them. */
fun visibleTypes(tiles: List<Tile>) = LibType.entries.filter { it != LibType.Radios && it != LibType.Mixes || tiles.any { t -> libType(t) == it } }

/** Who made a row, as the library's subtitles spell it ("Playlist • owner", "Album • artist"). */
/** "Radio • NaviBeat", "Mix • NaviBeat": what a generated row is, by whom. */
fun generatedSubtitle(t: Tile) = "${if (libType(t) == LibType.Radios) "Radio" else "Mix"} • ${t.mix?.by}"

/** A playlist's row: generated ones say what they are, the rest "Playlist • owner" ("Smart playlist" for .nsp ones). */
fun libraryPlaylist(p: Playlist) = p.tile().let { it.copy(subtitle = if (p.mix != null) generatedSubtitle(it) else "${if (p.smart) "Smart playlist" else "Playlist"} • ${p.owner}") }

fun creator(t: Tile, user: String) = if (t.kind == Kind.Liked) user else t.mix?.by ?: t.subtitle.substringAfter(" • ", "")

/**
 * The rows the library shows, in order: Liked Songs first, then everything else, then (unfiltered only, when [showMixes])
 * every generated mix and radio, grouped at the end. [hit] is the search box.
 */
fun libraryRows(
    tiles: List<Tile>, f: LibFilter, sort: LibSort, user: String, showMixes: Boolean,
    saved: (Tile) -> Boolean, pinned: (Tile) -> Boolean, hit: (Tile) -> Boolean = { true },
): List<Tile> {
    val facets = f.facets intersect FACETS.getValue(f.type).toSet()
    val kept = tiles.filter { t ->
        (f.type == null || libType(t) == f.type) && hit(t) && facets.all {
            when (it) {
                Facet.ByYou -> creator(t, user) == user
                Facet.ByOthers -> creator(t, user) != user
                Facet.Saved -> saved(t)
                Facet.Downloaded -> pinned(t)
            }
        }
    }
    val ci = String.CASE_INSENSITIVE_ORDER
    val order = when (sort) {
        LibSort.Default -> compareBy<Tile> { !saved(it) }
        LibSort.Alphabetical -> compareBy(ci) { it.title }
        LibSort.Creator -> compareBy<Tile, String>(ci) { creator(it, user) }.thenBy(ci) { it.title }
    }
    val sorted = kept.sortedWith(compareBy<Tile> { it.kind != Kind.Liked }.then(order))
    if (f.type != null) return sorted
    fun generated(t: Tile) = libType(t).let { it == LibType.Radios || it == LibType.Mixes }
    return sorted.filterNot(::generated) + if (showMixes) sorted.filter(::generated) else emptyList()
}
