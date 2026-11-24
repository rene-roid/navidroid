package xyz.yuuki.navidroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DiscoveryTest {
    private fun album(id: String, plays: Int = 0) = Album(id, id, "", null, null, 2000, 5, plays)
    private fun song(id: String, artistId: String?, plays: Int = 0) = Song(id, id, "", artistId, "", null, null, 0, 0, 0, false, null, 0, playCount = plays)

    @Test
    fun becauseShelfTakesTurnsAndSkipsRecent() {
        val shelf = becauseShelf(listOf(listOf(album("a1"), album("a2"), album("a3")), listOf(album("b1"), album("a1"))), listOf(album("a2")))
        assertEquals(listOf("a1", "b1", "a3"), shelf.map { it.id })
        assertEquals(10, becauseShelf(listOf((1..30).map { album("x$it") }), emptyList()).size)
    }

    @Test
    fun unplayedKeepsOnlyZeroPlaysUpToTwelve() {
        val random = (1..40).map { album("r$it", plays = it % 2) }
        assertEquals((1..40).filter { it % 2 == 0 }.take(12).map { "r$it" }, unplayedShelf(random).map { it.id })
    }

    @Test
    fun jumpBackIsPlayedAlbumsNotInRecent() {
        val frequent = listOf(album("f1", 3), album("f2", 0), album("f3", 5), album("f4", 1))
        val shelf = jumpBackShelf(frequent, listOf(album("f3")), Random(1)).map { it.id }
        assertEquals(setOf("f1", "f4"), shelf.toSet())
        assertEquals(10, jumpBackShelf((1..50).map { album("g$it", 1) }, emptyList()).size)
    }

    @Test
    fun throwbackLooksBackTenTwentyThirty() {
        assertEquals(listOf(2016 to "Released 10 years ago", 2006 to "Released 20 years ago", 1996 to "Released 30 years ago"), throwbackYears(2026))
    }

    @Test
    fun freshRadioPutsUnplayedFirstAndKeepsTheInterleave() {
        val seed = song("seed", "A")
        val src = listOf(song("a1", "A", 4), song("a2", "A"), song("b1", "B", 2), song("b2", "B"), song("c1", "C"))
        assertEquals(listOf("a1", "b1", "b2", "a2", "c1"), radioMix(seed, listOf(src)).map { it.id })
        // Unplayed first inside each group: own = a2, a1; others = b2, c1, b1.
        assertEquals(listOf("a2", "b2", "c1", "a1", "b1"), radioMix(seed, listOf(src), fresh = true).map { it.id })
    }

    @Test
    fun genreRadioStartsWithAnySongAndHasEachOnce() {
        val songs = (1..9).map { song("s$it", "A${it % 3}") }
        val mix = shuffledRadio(songs, fresh = true, rnd = Random(7))
        assertEquals(9, mix.size)
        assertEquals(songs.map { it.id }.toSet(), mix.map { it.id }.toSet())
        assertTrue(shuffledRadio(emptyList(), true).isEmpty())
    }

    @Test
    fun smartPlaylistsAreTheOnesWithValidUntil() {
        val json = """{"id":"1","name":"Never played","comment":"Auto-imported from 'Unplayed.nsp'","owner":"test","readonly":true,"validUntil":"2026-11-24T19:55:43Z"}"""
        assertTrue(Playlist.from(JSONObject(json)).smart)
        assertEquals("Smart playlist • By test", Playlist.from(JSONObject(json)).tile().subtitle)
        assertEquals("By test", Playlist.from(JSONObject("""{"id":"2","name":"x","owner":"test"}""")).tile().subtitle)
    }
}
