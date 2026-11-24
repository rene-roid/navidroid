package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {
    private fun album(id: String, artist: String, plays: Int, year: Int, genre: String, created: String, songs: Int = 4, secs: Int = 800) =
        Album(id, "A$id", artist, "ar-$artist", "c$id", year, songs, plays, genre = genre, duration = secs, created = created)

    private val albums = listOf(
        album("1", "Kaito", 8, 2024, "Electronic", "2026-03-01T10:00:00Z"),
        album("2", "Kaito", 6, 2021, "Electronic", "2025-01-01T10:00:00Z"),
        album("3", "Rita", 9, 1994, "Pop", "2026-05-01T10:00:00Z"),
        album("4", "Rita", 0, 1995, "Pop", "2026-05-01T10:00:00Z"),
    )

    @Test fun rankingsTotalsAndFavourites() {
        val s = computeStats(albums, emptyList(), 2026)
        assertEquals(listOf("Kaito" to 14, "Rita" to 9), s.artists.map { it.name to it.plays })
        assertEquals(listOf("3", "1", "2"), s.albums.map { it.id })
        assertEquals(23, s.plays)
        // 200 s a play: 23 plays is 4600 s.
        assertEquals(76L, s.minutes)
        assertEquals(2020, s.decade)
        assertEquals("Electronic", s.genre)
        assertEquals(listOf("1", "3"), s.discovered.map { it.id })
    }

    @Test fun nothingPlayed() {
        val s = computeStats(listOf(album("1", "Kaito", 0, 2024, "Pop", "2026-01-01")), emptyList(), 2026)
        assertEquals(0, s.plays)
        assertEquals(null, s.decade)
        assertEquals(null, s.genre)
        assertEquals(emptyList<Ranked>(), s.artists)
    }
}
