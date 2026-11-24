package xyz.yuuki.navidroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamTest {
    private val host: String? = null

    @Test
    fun guestsTakeTurnsBehindTheCurrentSong() {
        // The first guest song goes right after what's playing, ahead of the host's own queue.
        assertEquals(1, jamInsertIndex(listOf(host, host, host), 0, "Ana", JamOrder.Turns))
        // What already played doesn't count.
        assertEquals(3, jamInsertIndex(listOf("Ana", "Bob", host, host), 2, "Ana", JamOrder.Turns))
        // Someone new plays before anyone's second song.
        assertEquals(2, jamInsertIndex(listOf(host, "Ana", "Ana", host), 0, "Bob", JamOrder.Turns))
        assertEquals(3, jamInsertIndex(listOf(host, "Ana", "Bob", "Ana", "Bob", host), 0, "Cy", JamOrder.Turns))
        // A second song waits for the round to finish.
        assertEquals(3, jamInsertIndex(listOf(host, "Ana", "Bob", host), 0, "Ana", JamOrder.Turns))
        assertEquals(4, jamInsertIndex(listOf(host, "Ana", "Bob", "Ana", host), 0, "Bob", JamOrder.Turns))
        // The song playing is its guest's turn in the first round.
        assertEquals(2, jamInsertIndex(listOf("Ana", "Bob", host), 0, "Ana", JamOrder.Turns))
        assertEquals(1, jamInsertIndex(listOf("Ana", "Ana", host), 0, "Bob", JamOrder.Turns))
        assertEquals(3, jamInsertIndex(listOf("Ana", "Bob", "Bob"), 0, "Ana", JamOrder.Turns))
    }

    @Test
    fun atTheEndGoesBehindEverything() {
        assertEquals(4, jamInsertIndex(listOf(host, "Ana", host, host), 0, "Bob", JamOrder.End))
        assertEquals(1, jamInsertIndex(listOf(host), 0, "Bob", JamOrder.End))
    }

    @Test
    fun limitCountsOnlySongsStillWaiting() {
        val queue = listOf("Ana", "Ana", host, "Ana", "Bob", "Ana")
        assertFalse(jamCanAdd(queue, 2, "Ana", 2))
        assertTrue(jamCanAdd(queue, 2, "Ana", 3))
        assertTrue(jamCanAdd(queue, 2, "Bob", 2))
        assertFalse(jamCanAdd(queue, 2, "Bob", 1))
        assertTrue(jamCanAdd(queue, 0, "Ana", 0))
    }

    @Test
    fun votesToSkip() {
        assertEquals(listOf(0, 0), listOf(0, 9).map { votesNeeded(SkipVotes.Off, it) })
        assertEquals(listOf(1, 1), listOf(1, 9).map { votesNeeded(SkipVotes.One, it) })
        assertEquals(listOf(1, 1, 2, 2), listOf(0, 1, 2, 9).map { votesNeeded(SkipVotes.Two, it) })
        assertEquals(listOf(1, 1, 1, 2, 2, 3, 5), listOf(0, 1, 2, 3, 4, 5, 10).map { votesNeeded(SkipVotes.Half, it) })
    }

    @Test
    fun guestNames() {
        assertEquals("Ana", guestName("  Ana\n", emptyList()))
        assertEquals("Ana 2", guestName("Ana", listOf("Ana")))
        assertEquals("Ana 3", guestName("Ana", listOf("Ana", "Ana 2")))
        assertEquals("a".repeat(24), guestName("a".repeat(80), emptyList()))
        assertNull(guestName(" \t ", emptyList()))
    }

    @Test
    fun parsesRequests() {
        assertEquals(
            HttpRequest("POST", listOf("j", "ABC234", "join"), mapOf("name" to "Ana María", "g" to "")),
            parseRequest("POST /j/ABC234/join?name=Ana+Mar%C3%ADa&g= HTTP/1.1\r\nHost: 192.168.1.5:8765\r\n\r\n"),
        )
        assertEquals(HttpRequest("GET", listOf("j", "ABC234"), emptyMap()), parseRequest("GET /j/ABC234 HTTP/1.1\r\n\r\n"))
        assertEquals(HttpRequest("GET", listOf("j", "ABC234", "cover", "al-1/2"), emptyMap()), parseRequest("GET /j/ABC234/cover/al-1%2F2 HTTP/1.0\r\n\r\n"))
        assertEquals(HttpRequest("GET", emptyList(), emptyMap()), parseRequest("GET / HTTP/1.1\r\n\r\n"))
        assertNull(parseRequest("GET /j/ABC234\r\n\r\n"))
        assertNull(parseRequest("GET http://evil/ HTTP/1.1\r\n\r\n"))
        assertNull(parseRequest("GET /j/%zz HTTP/1.1\r\n\r\n"))
        assertNull(parseRequest(""))
    }

    @Test
    fun readsHeadsUpToTheCap() {
        assertEquals("GET / HTTP/1.1\r\nA: b\r\n\r\n", readHead("GET / HTTP/1.1\r\nA: b\r\n\r\nbody".byteInputStream()))
        assertNull(readHead("GET / HTTP/1.1\r\nA: b\r\n".byteInputStream()))
        assertNull(readHead(("GET /" + "a".repeat(9000) + " HTTP/1.1\r\n\r\n").byteInputStream()))
    }

    @Test
    fun jamLinks() {
        assertEquals("http://192.168.1.5:8765/j/ABC234", jamBase(" http://192.168.1.5:8765/j/ABC234\n"))
        assertEquals("http://192.168.1.5:8765/j/ABC234", jamBase("http://192.168.1.5:8765/j/ABC234/state?g=1"))
        assertEquals("http://192.168.1.5:8765/j/ABC234", jamBase("navidroid://jam?url=http%3A%2F%2F192.168.1.5%3A8765%2Fj%2FABC234"))
        assertNull(jamBase("https://example.com/j/ABC234"))
        assertNull(jamBase("http://192.168.1.5:8765/j/ABC"))
        assertNull(jamBase("http://192.168.1.5:8765/rest/ping"))
        assertNull(jamBase("hello"))
    }

    @Test
    fun stateAndSettingsSurviveJson() {
        val song = Song("s1", "Title", "Artist", "ar1", "Album", "al1", "al1", 200, 1, 2020, false, "mp3", 320)
        val state = JamState("Party", "Ana", listOf("Ana", "Bob"), true, 3, 1, 2, 1, true, JamSong(song, null, 4), 12_345, true, listOf(JamSong(song, "Ana", 5)))
        assertEquals(state, JamState.from(JSONObject(state.toJson().toString())))
        val empty = state.copy(now = null, queue = emptyList())
        assertEquals(empty, JamState.from(JSONObject(empty.toJson().toString())))
        val settings = JamSettings("Party", false, 0, SkipVotes.Two, JamOrder.End)
        assertEquals(settings, JamSettings.from(JSONObject(settings.toJson().toString())))
        assertEquals(JamSettings(), JamSettings.from(JSONObject()))
    }
}
