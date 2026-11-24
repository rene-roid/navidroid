package xyz.yuuki.navidroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class LikedTest {
    @Test
    fun newestLikeComesFirst() {
        val starred = JSONObject(
            """{"song":[
              {"id":"old","title":"Old","starred":"2026-01-01T10:00:00Z"},
              {"id":"new","title":"New","starred":"2026-03-01T10:00:00Z"},
              {"id":"mid","title":"Mid","starred":"2026-02-01T10:00:00Z"}]}""",
        )
        assertEquals(listOf("new", "mid", "old"), likedSongs(starred).map { it.id })
    }
}
