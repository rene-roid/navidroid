package xyz.yuuki.navidroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.yuuki.navidroid.ui.Queued
import xyz.yuuki.navidroid.ui.draggedTo
import xyz.yuuki.navidroid.ui.queueKeys
import kotlin.random.Random

class PlayerTest {
    private fun song(id: String, gain: String = "{}") = Song.from(JSONObject("""{"id":"$id","title":"$id","replayGain":$gain}"""))

    @Test
    fun smartShuffleKeepsAnArtistFromPlayingTwiceInARow() {
        // Three of five by one artist leave a single way to spread them.
        val few = listOf("A", "A", "A", "B", "B")
        repeat(20) { seed -> assertEquals(listOf("A", "B", "A", "B", "A"), smartShuffle(few, Random(seed)).map { few[it] }) }

        val many = List(60) { "ABCDEF"[it % 6].toString() } + List(40) { "A" }
        repeat(20) { seed ->
            val order = smartShuffle(many, Random(seed))
            assertEquals(many.indices.toList(), order.sorted())
            assertEquals(0, order.zipWithNext().count { (a, b) -> many[a] == many[b] })
        }
        // It's still a shuffle.
        assertEquals(20, (0 until 20).map { smartShuffle(many, Random(it)) }.distinct().size)
    }

    @Test
    fun smartShuffleDoesNotOpenWithTheArtistPlayingNow() {
        val artists = listOf("A", "A", "B", "C")
        repeat(20) { seed -> assertEquals(false, artists[smartShuffle(artists, Random(seed), after = "A").first()] == "A") }
        // One artist only: nothing to spread, every song still plays once.
        assertEquals(listOf(0, 1, 2), smartShuffle(listOf("A", "A", "A"), Random(1)).sorted())
        assertEquals(emptyList<Int>(), smartShuffle(emptyList()))
    }

    @Test
    fun aDraggedSongTakesTheMarkerOfTheOneBeforeIt() {
        val rows = listOf(Queued(3, song("a"), null), Queued(4, song("b"), null), Queued(5, song("c"), "Seed"), Queued(6, song("d"), "Seed"))
        fun List<Queued>.show() = map { it.song.id to it.auto }
        // An autoplay song dragged up among the user's becomes theirs.
        assertEquals(listOf("a" to null, "d" to null, "b" to null, "c" to "Seed"), draggedTo(rows, 3, 1).show())
        // To the top too: nothing of autoplay's is before it.
        assertEquals(listOf("c" to null, "a" to null, "b" to null, "d" to "Seed"), draggedTo(rows, 2, 0).show())
        // One of the user's dropped into autoplay's run joins it, so the run stays whole.
        assertEquals(listOf("b" to null, "c" to "Seed", "a" to "Seed", "d" to "Seed"), draggedTo(rows, 0, 2).show())
        // Just above the run it stays the user's.
        assertEquals(listOf("b" to null, "a" to null, "c" to "Seed", "d" to "Seed"), draggedTo(rows, 0, 1).show())
        assertEquals(listOf(1, 2, 0, 3), listOf(0, 1, 2, 3).moved(0, 2))
        assertEquals(listOf(3, 0, 1, 2), listOf(0, 1, 2, 3).moved(3, 0))
    }

    @Test
    fun queueKeysTellASongQueuedTwiceApart() {
        assertEquals(listOf("a#1", "b#1", "a#2"), queueKeys(listOf(Queued(1, song("a"), null), Queued(2, song("b"), null), Queued(7, song("a"), null))))
    }

    @Test
    fun sleepFadesOutOverTheLastTenSeconds() {
        assertEquals(1f, sleepFade(60_000), 0f)
        assertEquals(1f, sleepFade(10_000), 0f)
        assertEquals(.5f, sleepFade(5_000), 0f)
        assertEquals(0f, sleepFade(0), 0f)
        assertEquals(0f, sleepFade(-300), 0f)
    }

    @Test
    fun replayGainTurnsLoudSongsDown() {
        val g = song("s", """{"trackGain":-6,"albumGain":-3,"trackPeak":0.9,"albumPeak":0.95}""").gain
        assertEquals(ReplayGain(-6.0, -3.0, 0.9, 0.95), g)
        assertEquals(.5012f, replayVolume(g, GainMode.Track), .0001f)
        assertEquals(.7079f, replayVolume(g, GainMode.Album), .0001f)
        assertEquals(1f, replayVolume(g, GainMode.Off), 0f)
        // Only the track was measured: album mode makes do with it.
        assertEquals(.5012f, replayVolume(ReplayGain(-6.0, null, null, null), GainMode.Album), .0001f)
        // Quiet songs can't be turned up, and a peak over full scale is brought under it.
        assertEquals(1f, replayVolume(ReplayGain(4.0, null, 0.5, null), GainMode.Track), 0f)
        assertEquals(.8f, replayVolume(ReplayGain(0.0, null, 1.25, null), GainMode.Track), .0001f)
    }

    @Test
    fun songsWithoutMeasurementsPlayAsTheyAre() {
        // What Navidrome sends for files without ReplayGain tags.
        assertNull(song("s").gain)
        assertNull(Song.from(JSONObject("""{"id":"s"}""")).gain)
        assertEquals(1f, replayVolume(null, GainMode.Track), 0f)
        // The gain survives the trip through the saved queue.
        val s = song("s", """{"trackGain":-7.5,"trackPeak":1}""")
        assertEquals(s.gain, Song.from(s.toJson()).gain)
        assertNull(Song.from(song("s").toJson()).gain)
    }

    @Test
    fun browsePlacesSurviveTheTripThroughTheirId() {
        assertEquals("root", Node.Root.mediaId)
        assertEquals("songs/Album/al-1", Node.Songs(Kind.Album, "al-1").mediaId)
        assertEquals("track/s1/Playlist/p%2F1", Node.Track("s1", Node.Songs(Kind.Playlist, "p/1")).mediaId)
        assertEquals("track/s1", Node.Track("s1").mediaId)
        val places = listOf(
            Node.Root, Node.Home, Node.Recent, Node.Downloaded, Node.Songs(Kind.Liked, "liked"), Node.Songs(Kind.Genre, "Rock / Pop & more"),
            Node.Track("s1"), Node.Track("s 1", Node.Songs(Kind.Playlist, "p/1")),
        )
        places.forEach { assertEquals(it, Node.parse(it.mediaId)) }
        // The app's own queue items carry plain song ids.
        assertNull(Node.parse("0Tpv2EKAJb7r5RfbbQT1O7"))
        assertNull(Node.parse(""))
        assertNull(Node.parse("songs/Nope/1"))
        assertNull(Node.parse("songs/Album"))
        assertNull(Node.parse("track/s1/Nope/1"))
    }

    @Test
    fun browseHomeListsLikedThenPlaylistsThenMixes() {
        fun playlist(id: String, name: String, comment: String = "") = Playlist(id, name, comment, "ana", 3, 100, null)
        val all = listOf(
            playlist("m2", "On Repeat", "NaviBeat Mixes: the songs you play most.\nnb1:onrepeat:onrepeat"),
            playlist("p1", "Road trip"),
            playlist("m1", "Daily Mix 1", "NaviBeat Mixes: built around Mono Kids and what sits near it.\nnb1:dailymix:dailymix-1"),
            playlist("p2", "Hidden one"),
        )
        val home = browseHome(all, setOf("p2"), 9)
        assertEquals(listOf(Kind.Liked to "liked", Kind.Playlist to "p1", Kind.Playlist to "m1", Kind.Playlist to "m2"), home.map { it.kind to it.id })
        assertEquals(listOf("Liked Songs", "Road trip", "Daily Mix 1", "On Repeat"), home.map { it.title })
        assertEquals("Playlist • ana", home[1].subtitle)
    }

    @Test
    fun pagesOfAList() {
        val l = listOf(1, 2, 3, 4, 5)
        assertEquals(listOf(1, 2), l.page(0, 2))
        assertEquals(listOf(5), l.page(2, 2))
        assertEquals(emptyList<Int>(), l.page(3, 2))
        assertEquals(l, l.page(0, Int.MAX_VALUE))
        assertEquals(emptyList<Int>(), l.page(1, Int.MAX_VALUE))
    }
}
