package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningTest {
    private fun song(id: String, album: String?) = Song(id, "T$id", "A", null, "Al", album, null, 0, 0, 0, false, null, 0)

    @Test
    fun leavesOutThisPhoneAndNamesTheRest() {
        val entries = listOf(
            Listener("me", "navidroid", 0, song("1", "a1")),
            Listener("me", "Feishin", 0, song("2", "a2")),
            Listener("ana", "navidroid", 3, song("1", "a1")),
            Listener("bob", "Sonixd", 0, song("4", null)),
        )
        assertEquals(
            listOf("a2" to "You on Feishin", "a1" to "ana on navidroid • 3 min ago"),
            listeningTiles(entries, "me", "1").map { it.id to it.subtitle },
        )
    }
}
