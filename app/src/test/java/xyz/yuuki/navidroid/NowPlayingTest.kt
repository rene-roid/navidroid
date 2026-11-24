package xyz.yuuki.navidroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NowPlayingTest {
    @Test
    fun readsWhatTheServiceWrote() {
        val np = NowPlaying.from(JSONObject("""{"id":"s1","title":"Song","artist":"Artist","from":"Mix","cover":"/data/c.jpg","isPlaying":true,"color":-14575885}"""))
        assertEquals(NowPlaying("s1", "Song", "Artist", "Mix", "/data/c.jpg", true, -14575885), np)
        assertEquals(np, NowPlaying.from(JSONObject(np.toJson().toString())))
    }

    @Test
    fun aSongWithoutCoverOrSource() {
        val np = NowPlaying.from(JSONObject("""{"id":"s1","title":"Song","artist":"Artist"}"""))
        assertEquals(NowPlaying("s1", "Song", "Artist", null, null, false, 0), np)
        val json = JSONObject(np.toJson().toString())
        assertFalse(json.has("from") || json.has("cover"))
        assertEquals(np, NowPlaying.from(json))
    }
}
