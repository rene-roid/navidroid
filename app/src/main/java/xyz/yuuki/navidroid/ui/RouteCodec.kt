package xyz.yuuki.navidroid.ui

import xyz.yuuki.navidroid.Kind

/**
 * A route as one string, so the back stack survives process death: "Home", "Open:Playlist:42", "More:top-mixes".
 * Spelled out because class names don't survive R8, and exhaustive so a new Route can't be added without a name here.
 */
fun Route.encode(): String = when (this) {
    is Route.Open -> "Open:${kind.name}:$id"
    is Route.More -> "More:$section"
    is Route.Jam -> "Jam:$base"
    Route.Home -> "Home"
    Route.Search -> "Search"
    Route.Library -> "Library"
    Route.Settings -> "Settings"
    Route.Downloads -> "Downloads"
    Route.Appearance -> "Appearance"
    Route.HiddenPlaylists -> "HiddenPlaylists"
    Route.SongCache -> "SongCache"
    Route.ImageCache -> "ImageCache"
    Route.SavedInfo -> "SavedInfo"
    Route.Playback -> "Playback"
    Route.DownloadSettings -> "DownloadSettings"
    Route.Server -> "Server"
    Route.Storage -> "Storage"
    Route.About -> "About"
    Route.Stats -> "Stats"
}

private val simple = listOf(
    Route.Home, Route.Search, Route.Library, Route.Settings, Route.Downloads, Route.Appearance, Route.HiddenPlaylists, Route.SongCache,
    Route.ImageCache, Route.SavedInfo, Route.Playback, Route.DownloadSettings, Route.Server, Route.Storage, Route.About, Route.Stats,
).associateBy { it.encode() }

/** Null for anything [encode] doesn't produce, such as a route a newer or older build had. */
fun decodeRoute(s: String): Route? {
    simple[s]?.let { return it }
    val p = s.split(":", limit = 3)
    return when (p[0]) {
        "Open" -> Route.Open(runCatching { Kind.valueOf(p.getOrNull(1) ?: return null) }.getOrNull() ?: return null, p.getOrNull(2) ?: return null)
        "More" -> p.getOrNull(1)?.let { Route.More(s.substringAfter(":")) }
        "Jam" -> Route.Jam(s.substringAfter(":"))
        else -> null
    }
}
