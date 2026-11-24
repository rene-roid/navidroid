package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.yuuki.navidroid.ui.Route
import xyz.yuuki.navidroid.ui.decodeRoute
import xyz.yuuki.navidroid.ui.encode

class ShortcutsTest {
    private val routes = listOf(
        Route.Home, Route.Search, Route.Library, Route.Settings, Route.Downloads, Route.Appearance, Route.HiddenPlaylists, Route.SongCache,
        Route.ImageCache, Route.SavedInfo, Route.Playback, Route.DownloadSettings, Route.Server, Route.Storage, Route.About,
        Route.Open(Kind.Playlist, "pl-1"), Route.Open(Kind.Album, "a:b:c"), Route.Open(Kind.Artist, "ar-9"), Route.Open(Kind.Liked, "liked"),
        Route.Open(Kind.Genre, "Bossa Nova"), Route.Open(Kind.ArtistMix, "7"), Route.Open(Kind.ArtistRadio, "7"),
        Route.More("top-mixes"), Route.More("a:b"), Route.Jam("http://10.0.2.18:8765/j/ABC123"), Route.Open(Kind.Decade, "1990"),
    )

    @Test
    fun everyRouteSurvivesEncoding() {
        assertEquals(routes, routes.map { decodeRoute(it.encode())!! })
        assertEquals("Open:Playlist:pl-1", Route.Open(Kind.Playlist, "pl-1").encode())
        assertEquals("More:top-mixes", Route.More("top-mixes").encode())
    }

    @Test
    fun everySimpleRouteHasADistinctName() {
        assertEquals(routes.size, routes.map { it.encode() }.toSet().size)
    }

    @Test
    fun unknownRoutesDecodeToNull() {
        listOf("", "Nope", "Open", "Open:Playlist", "Open:Nope:1", "More").forEach { assertNull(it, decodeRoute(it)) }
    }

    @Test
    fun shortcutsRoundTrip() {
        val all = listOf(
            ShortcutAction.Search, ShortcutAction.Shuffle, ShortcutAction.Play(Kind.Liked, "liked"),
            ShortcutAction.Open(Kind.Playlist, "x:y"), ShortcutAction.Open(Kind.Album, "al-1"), ShortcutAction.Play(Kind.Playlist, "p1"),
        )
        assertEquals(all, all.map { ShortcutAction.parse(it.spec) })
        assertEquals("play:Liked:liked", ShortcutAction.Play(Kind.Liked, "liked").spec)
        assertEquals(ShortcutAction.Open(Kind.Playlist, "x:y"), ShortcutAction.parse("open:Playlist:x:y"))
    }

    @Test
    fun badShortcutsParseToNull() {
        listOf(null, "", "nope", "play", "play:Liked", "play:Liked:", "play:Artist:1", "open:Nope:1", "search:x").forEach { assertNull(it, ShortcutAction.parse(it)) }
    }
}
