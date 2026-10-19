package xyz.yuuki.navidroid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumTypeTest {
    @Test
    fun tagsWinOverTrackCount() {
        assertTrue(Album.isSingle(listOf("EP"), 8, 3000))
        assertFalse(Album.isSingle(listOf("Album"), 2, 400))
        assertTrue(Album.isSingle(emptyList(), 2, 400))
        assertFalse(Album.isSingle(emptyList(), 5, 2400))
        assertFalse(Album.isSingle(emptyList(), 12, 2400))
    }
}

class ArtistNamesTest {
    @Test
    fun splitsCredits() {
        org.junit.Assert.assertEquals(listOf("The Living Tombstone", "Black Gryph0n", "LittleJayneyCakes"), artistNames("The Living Tombstone, Black Gryph0n & LittleJayneyCakes"))
        org.junit.Assert.assertEquals(listOf("A", "B"), artistNames("A (feat. B)"))
        org.junit.Assert.assertEquals(listOf("A", "B"), artistNames("A ft. B"))
        org.junit.Assert.assertEquals(listOf("Feathered Sentinels"), artistNames("Feathered Sentinels"))
    }
}
