package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Expected values are what the server's web UI shows for the same playlists. */
class MixesTest {
    private fun nb(text: String, tag: String) = "NaviBeat Mixes: $text\nMade by NaviBeat  ·  navibeat.app\nnb1:$tag:2026-10-15:fallback:30"

    private val daily1 = Playlist("d1", "🟠 Daily Mix 1", nb("built around LEVEL NINE and what sits near it. Rotates weekly.", "dailymix:dailymix-1"), "yuuki", 30, 0, null)
    private val radio2 = Playlist("r2", "🟠 Artist Radio 2", nb("everything you have by MakenCat. A different artist each week.", "artistradio:artistradio-2"), "yuuki", 28, 0, null)
    private val evening = Playlist("ev", "🟠 Evening", nb("your evening, built from what you actually play at this time of day.", "timeofday:evening"), "yuuki", 30, 0, null)
    private val night = Playlist("ni", "🟠 Night", nb("your night. Still learning when you listen.", "timeofday:night"), "yuuki", 30, 0, null)
    private val discovery = Playlist("wd", "🟠 Weekly Discovery", nb("music you own, played once or twice, and then forgot.", "discovery:discovery"), "yuuki", 22, 0, null)
    private val explo = Playlist("we", "Weekly-Exploration", "Created for fertileforg by Explo, using ListenBrainz recommendations.", "yuuki", 50, 0, null)
    private val jams = Playlist("dj", "Daily-Jams", "Built 2026-10-15 by explo-maintainer from your play history (ListenBrainz had no Daily Jams).", "yuuki", 30, 0, null)
    private val mine = Playlist("mine", "Ado", "Imported by Explo for fertileforg", "yuuki", 267, 0, null)

    @Test
    fun parsesLikeTheWebUi() {
        daily1.mix!!.run {
            assertEquals("Daily Mix 1", title)
            assertEquals("LEVEL NINE and more", description)
            assertEquals(0xFFE8115B, color)
            assertEquals("madeForYou", section)
        }
        radio2.mix!!.run {
            assertEquals("MakenCat Radio", title)
            assertEquals("Everything you have by MakenCat", description)
            assertEquals("topMixes", section)
        }
        assertEquals("Your evening, built from what you actually play at this time of day.", evening.mix!!.description)
        assertEquals("Your night.", night.mix!!.description)
        explo.mix!!.run {
            assertEquals("Weekly Exploration", title)
            assertEquals("From your ListenBrainz recommendations", description)
            assertEquals(0xFF1E8A6E, color)
        }
        assertEquals("Built from your play history", jams.mix!!.description)
        assertNull(mine.mix)
    }

    @Test
    fun homeMatchesTheWebUiOrder() {
        val home = buildHome("Yuuki", 20, listOf(mine, jams, explo, discovery, night, evening, radio2, daily1), emptyList(), emptyList(), emptyList(), 17)
        assertEquals(listOf("Liked Songs", "Evening", "Daily Mix 1", "Weekly Discovery", "Weekly Exploration"), home.quick.map { it.title })
        assertEquals("Playlist • 17 songs", home.quick.first().subtitle)
        val sections = home.sections.associate { it.title to it.tiles.map { t -> t.title } }
        assertEquals(listOf("Daily Mix 1", "Weekly Discovery", "Weekly Exploration", "Daily Jams"), sections["Made For Yuuki"])
        assertEquals(listOf("Evening", "Night"), sections["Mixes for your day"])
        assertEquals(listOf("Ado"), sections["Your playlists"])
    }

    @Test
    fun snapshotRoundTrips() {
        val c = Collection.of(daily1, listOf(Song("s", "t", "a", null, "al", null, "c", 100, 1, 2020, true, "flac", 900)))
        assertEquals(c, Collection.from(org.json.JSONObject(c.toJson().toString())))
    }

    @Test
    fun endpointsAreNormalized() {
        assertEquals("https://music.thekenji.xyz", normalizeEndpoint("music.thekenji.xyz/app/#/"))
        assertEquals("http://192.168.68.63:4533", normalizeEndpoint("http://192.168.68.63:4533/app/#/"))
        assertEquals("https://music.thekenji.xyz", normalizeEndpoint("Music.thekenji.xyz"))
        assertEquals("http://192.168.68.63:4533", normalizeEndpoint("Http://192.168.68.63:4533"))
    }
}
