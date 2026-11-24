package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {
    private fun nb(text: String, tag: String) = "NaviBeat Mixes: $text\nMade by NaviBeat  ·  navibeat.app\nnb1:$tag:2026-10-15:fallback:30"
    private fun pl(id: String, name: String, owner: String, comment: String = "") = Playlist(id, name, comment, owner, 10, 0, null)

    // Tiles the way the screen builds them.
    private fun playlist(p: Playlist) = libraryPlaylist(p)
    private val liked = likedTile(null)
    private val road = playlist(pl("road", "Road trip", "yuuki"))
    private val gym = playlist(pl("gym", "Gym", "ana"))
    private val chill = playlist(pl("chill", "chill", "bob"))
    private val daily = playlist(pl("d1", "x", "NaviBeat", nb("built around LEVEL NINE and what sits near it.", "dailymix:dailymix-1")))
    private val artistRadio = playlist(pl("r1", "x", "NaviBeat", nb("everything you have by MakenCat.", "artistradio:artistradio-1")))
    private val genreRadio = playlist(pl("g1", "x", "NaviBeat", nb("Rock, one of the genres you play most.", "genreradio:genreradio-1")))
    private val evening = playlist(pl("ev", "x", "NaviBeat", nb("your evening.", "timeofday:evening")))
    private val discovery = playlist(pl("dw", "x", "NaviBeat", nb("new to you.", "discovery:discovery-1")))
    private val onPhoneRadio = artistMixTile(Kind.ArtistRadio, "a9", "Ado", null).let { it.copy(subtitle = generatedSubtitle(it)) }
    private val album = Album("al", "Zeta", "Ado", null, null, 2020, 10, 0).tile().copy(subtitle = "Album • Ado")
    private val tiles = listOf(liked, road, gym, chill, daily, artistRadio, genreRadio, evening, discovery, onPhoneRadio, album)
    private val saved = setOf("gym", "d1", "r1", "g1", "al", "a9")
    private val downloaded = setOf("chill", "d1", "ev")

    private fun rows(f: LibFilter = LibFilter(), sort: LibSort = LibSort.Default, showMixes: Boolean = true, hit: (Tile) -> Boolean = { true }) =
        libraryRows(tiles, f, sort, "yuuki", showMixes, { it.id in saved || it.kind == Kind.Liked }, { it.id in downloaded }, hit).map { it.id }

    @Test
    fun smartPlaylistsSaySoAndKeepTheirOwner() {
        val smart = libraryPlaylist(Playlist("s", "Never played", "", "yuuki", 0, 0, null, smart = true))
        assertEquals("Smart playlist • yuuki", smart.subtitle)
        assertEquals("yuuki", creator(smart, "yuuki"))
    }

    @Test
    fun radiosSayRadio() = assertEquals(listOf("Radio • NaviBeat", "Mix • NaviBeat", "Radio • Ado"), listOf(artistRadio, daily, onPhoneRadio).map { it.subtitle })

    @Test
    fun classifiesTypes() {
        assertEquals(
            listOf(LibType.Playlists, LibType.Playlists, LibType.Mixes, LibType.Radios, LibType.Radios, LibType.Mixes, LibType.Radios, LibType.Albums),
            listOf(liked, road, daily, artistRadio, genreRadio, evening, onPhoneRadio, album).map { libType(it) },
        )
    }

    @Test
    fun unfilteredGroupsMixesAtTheEnd() {
        assertEquals(listOf("liked", "gym", "al", "road", "chill", "d1", "r1", "g1", "a9", "ev", "dw"), rows())
        assertEquals(listOf("liked", "gym", "al", "road", "chill"), rows(showMixes = false))
    }

    @Test
    fun facetsCombine() {
        assertEquals(listOf("liked", "road"), rows(LibFilter(LibType.Playlists).toggle(Facet.ByYou)))
        assertEquals(listOf("gym", "chill"), rows(LibFilter(LibType.Playlists).toggle(Facet.ByOthers)))
        assertEquals(listOf("chill"), rows(LibFilter(LibType.Playlists).toggle(Facet.ByOthers).toggle(Facet.Downloaded)))
        assertEquals(listOf("d1", "ev"), rows(LibFilter(LibType.Mixes).toggle(Facet.Downloaded)))
        assertEquals(listOf("r1", "g1", "a9"), rows(LibFilter(LibType.Radios).toggle(Facet.Saved)))
        assertEquals(listOf("liked", "gym", "al", "d1", "r1", "g1", "a9"), rows(LibFilter().toggle(Facet.Saved)))
    }

    @Test
    fun filterTransitions() {
        val f = LibFilter(LibType.Playlists).toggle(Facet.ByYou).toggle(Facet.Downloaded)
        assertEquals(setOf(Facet.ByYou, Facet.Downloaded), f.facets)
        assertEquals(setOf(Facet.ByOthers, Facet.Downloaded), f.toggle(Facet.ByOthers).facets)
        assertEquals(LibFilter(LibType.Albums, setOf(Facet.Downloaded)), f.pick(LibType.Albums))
        assertEquals(LibFilter(LibType.Artists), f.pick(LibType.Artists))
        assertEquals(f, LibFilter.decode(f.encode()))
        assertEquals(LibFilter(), LibFilter.decode(LibFilter().encode()))
    }

    @Test
    fun sorts() {
        val p = LibFilter(LibType.Playlists)
        assertEquals(listOf("liked", "chill", "gym", "road"), rows(p, LibSort.Alphabetical))
        assertEquals(listOf("liked", "gym", "chill", "road"), rows(p, LibSort.Creator))
        assertEquals(listOf("liked", "gym", "road", "chill"), rows(p))
    }

    @Test
    fun searchAndHiddenTypes() {
        assertEquals(listOf("road"), rows(hit = { it.title.contains("road", true) }))
        assertEquals(LibType.entries, visibleTypes(tiles))
        assertEquals(listOf(LibType.Playlists, LibType.Albums, LibType.Artists), visibleTypes(listOf(liked, album)))
    }
}
