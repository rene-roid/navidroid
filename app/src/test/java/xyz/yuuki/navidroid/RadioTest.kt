package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class RadioTest {
    private fun song(id: String, artistId: String?) = Song(id, id, "", artistId, "", null, null, 0, 0, 0, false, null, 0)

    @Test
    fun mixesSourcesWithoutRepeatsOrTheSeed() {
        val seed = song("seed", "A")
        val similar = listOf(song("a1", "A"), seed, song("b1", "B"), song("a2", "A"), song("a3", "A"))
        val genre = listOf(song("b1", "B"), song("c1", "C"), song("queued", "C"), song("c2", "C"), song("d1", "D"))
        // One by the seed's artist, then two by others, each source in its own order; the rest of the artist's last.
        assertEquals(listOf("a1", "b1", "c1", "a2", "c2", "d1", "a3"), radioMix(seed, listOf(similar, genre), setOf("queued")).map { it.id })
        assertEquals(listOf("a1", "b1", "c1"), radioMix(seed, listOf(similar, genre), limit = 3).map { it.id })
    }

    @Test
    fun unknownArtistsAreNotTheSeeds() {
        val mix = radioMix(song("seed", null), listOf(listOf(song("x", null), song("y", "B"))))
        assertEquals(listOf("x", "y"), mix.map { it.id })
    }

    @Test
    fun recentsKeepOneOfEachNewestFirst() {
        val album = Recent(Kind.Album, "1", "Album", "Album", null)
        val artist = Recent(Kind.Artist, "1", "Artist", "Artist", null)
        val songs = (1..20).map { Recent.of(song("s$it", null)) }
        assertEquals(listOf(album, artist), Recent.add(listOf(artist, album), album))
        val full = songs.fold(listOf(album)) { l, r -> Recent.add(l, r) }
        assertEquals((20 downTo 6).map { "s$it" }, full.map { it.id })
        assertEquals(full, full.map { Recent.from(it.toJson()) })
    }
}
