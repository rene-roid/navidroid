package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.yuuki.navidroid.ui.rankHits

class SearchRankTest {
    private fun song(id: String, title: String, artist: String) = Song(id, title, artist, null, "", null, null, 0, 0, 0, false, null, 0)

    @Test
    fun mixesKindsBestMatchFirst() {
        val genre = Genre("J-Rock", 10)
        val artist = Artist("a", "Rock Lee", null, 1)
        val exact = song("s1", "Rock", "X")
        val combo = song("s2", "Usseewa", "Ado")
        val hits = rankHits("rock", listOf(genre), listOf(artist), listOf(exact, combo))
        assertEquals(listOf(exact, artist, genre, combo), hits)
        // Words in any order, split across title and artist.
        assertEquals(listOf(combo, exact), rankHits("ado usseewa", listOf(exact, combo)))
    }
}
