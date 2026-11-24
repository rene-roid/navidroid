package xyz.yuuki.navidroid.ui

import xyz.yuuki.navidroid.Kind

// Spelled out: class names don't survive R8, and this gets stored.
private val simple = mapOf(
    "Home" to Route.Home, "Search" to Route.Search, "Library" to Route.Library, "Settings" to Route.Settings, "Downloads" to Route.Downloads,
    "Appearance" to Route.Appearance, "HiddenPlaylists" to Route.HiddenPlaylists, "SongCache" to Route.SongCache, "ImageCache" to Route.ImageCache,
    "SavedInfo" to Route.SavedInfo, "Playback" to Route.Playback, "DownloadSettings" to Route.DownloadSettings, "Server" to Route.Server,
    "Storage" to Route.Storage, "About" to Route.About,
)

/** A route as one string, so the back stack survives process death: "Home", "Open:Playlist:42", "More:top-mixes". */
fun Route.encode(): String = when (this) {
    is Route.Open -> "Open:${kind.name}:$id"
    is Route.More -> "More:$section"
    is Route.Jam -> "Jam:$base"
    else -> simple.entries.first { it.value == this }.key
}

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
