package xyz.yuuki.navidroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.Collection
import xyz.yuuki.navidroid.Home
import xyz.yuuki.navidroid.Kind
import xyz.yuuki.navidroid.Pin
import xyz.yuuki.navidroid.Tile
import xyz.yuuki.navidroid.buildHome
import xyz.yuuki.navidroid.list
import xyz.yuuki.navidroid.tile
import java.io.File
import java.util.Calendar

private val api get() = App.it.api
private val offline get() = App.it.offline

fun displayName() = App.it.prefs.username.orEmpty().replaceFirstChar { it.uppercase() }

/** Profile picture, kept on-device since Subsonic has no avatar upload. Bump [avatarVersion] after writing it. */
val avatarFile get() = File(App.it.filesDir, "avatar")
var avatarVersion by mutableIntStateOf(0)

@Composable
fun Avatar(size: Dp, onClick: () -> Unit) = Box(
    Modifier.size(size).clip(CircleShape).background(Color(0xFFE1118C)).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    Text(displayName().take(1), fontWeight = FontWeight.W800)
    val v = avatarVersion
    if (avatarFile.exists()) AsyncImage(
        ImageRequest.Builder(LocalContext.current).data(avatarFile).memoryCacheKey("avatar:$v").build(),
        null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
    )
}

suspend fun loadHome(): Home = coroutineScope {
    val playlists = async { api.playlists() }
    val recent = async { api.albumList("recent") }
    val newest = async { api.albumList("newest") }
    val frequent = async { api.albumList("frequent", 100) }
    val liked = async { runCatching { api.starred().list("song").size }.getOrNull() }
    buildHome(displayName(), Calendar.getInstance().get(Calendar.HOUR_OF_DAY), playlists.await(), recent.await(), newest.await(), frequent.await(), liked.await())
}

/** Pinned collections as tiles, from their saved snapshots, for when the server is unreachable. */
fun pinnedTiles(pins: List<Pin>) = pins.mapNotNull { p ->
    offline.snapshot(p.kind, p.id)?.let { Tile(it.kind, it.id, it.title, "${it.songs.size} songs", it.coverArt, it.mix) }
}

// ---- home -----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    val home = rememberLoad("home") { loadHome() }
    val online by api.online.collectAsStateWithLifecycle()
    val pins by offline.pins.collectAsStateWithLifecycle()
    val refresh = {
        api.networkChanged(true)
        home.refresh()
    }
    PullToRefreshBox(home.loading && home.data != null, refresh, Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0xFF2E2745), .35f to Bg))) {
            item("top") {
                Row(Modifier.statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(34.dp) { nav.go(Route.Settings) }
                    Spacer(Modifier.width(12.dp))
                    Text(home.data?.greeting ?: "Hello", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton({ nav.go(Route.Downloads) }) { Icon(Icons.Rounded.DownloadForOffline, "Downloads") }
                    IconButton({ nav.go(Route.Settings) }) { Icon(Icons.Rounded.Settings, "Settings") }
                }
            }
            if (!online || (home.data == null && home.error != null)) item("offline") { OfflineHome(pinnedTiles(pins)) }
            home.data?.let { h ->
                item("quick") { QuickGrid(h.quick) }
                items(h.sections, key = { it.key }) { s ->
                    SectionHeader(s.title) { nav.go(Route.More(s.key)) }
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(s.tiles, key = { "${it.kind}:${it.id}" }) { Card(it) }
                    }
                }
            }
            if (home.data == null && home.loading) item("loading") { Loading() }
            item("end") { BottomSpacer() }
        }
    }
}

@Composable
private fun OfflineHome(tiles: List<Tile>) = Column(Modifier.padding(16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CloudOff, null, tint = Sub)
        Spacer(Modifier.width(8.dp))
        Text("Can't reach your server. Your downloads still work.", style = MaterialTheme.typography.bodySmall)
    }
    if (tiles.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        QuickGrid(tiles, Modifier)
    }
}

@Composable
private fun QuickGrid(tiles: List<Tile>, modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    val nav = LocalNav.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    Row(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(4.dp)).background(Tile).clickable { nav.open(t) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(56.dp), 120)
                        Text(
                            t.title, style = MaterialTheme.typography.labelLarge, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun Loading() = Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = Green, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
}

@Composable
fun MoreScreen(key: String) {
    val section = cachedUi<Home>("home")?.sections?.firstOrNull { it.key == key }
    LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { BackTitle(section?.title ?: "") }
        items(section?.tiles.orEmpty(), key = { "${it.kind}:${it.id}" }) { Card(it, width = 200.dp) }
        item(span = { GridItemSpan(maxLineSpan) }) { BottomSpacer() }
    }
}

@Composable
fun BackTitle(title: String) = Row(Modifier.statusBarsPadding().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    val nav = LocalNav.current
    IconButton(nav::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
    Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

// ---- search ---------------------------------------------------------------------------------

@Composable
fun SearchScreen() {
    val nav = LocalNav.current
    val player = LocalPlayer.current
    var q by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<xyz.yuuki.navidroid.SearchResult?>(null) }
    LaunchedEffect(q) {
        if (q.isBlank()) return@LaunchedEffect run { result = null }
        delay(250)
        result = runCatching { api.search(q.trim()) }.getOrNull()
    }
    val genres = rememberLoad("genres") { api.genres() }
    // Tapping the Search tab again jumps into the search box.
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val retapAtOpen = remember { nav.retap }
    LaunchedEffect(nav.retap) {
        if (nav.retap != retapAtOpen) { focus.requestFocus(); keyboard?.show() }
    }

    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.statusBarsPadding()) {
                Text("Search", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color.Black)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Text("What do you want to listen to?", color = Color(0xFF6A6A6A), style = MaterialTheme.typography.bodyLarge)
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black), cursorBrush = SolidColor(Color.Black), modifier = Modifier.fillMaxWidth().focusRequester(focus))
                    }
                    if (q.isNotEmpty()) IconButton({ q = "" }) { Icon(Icons.Rounded.Close, "Clear", tint = Color.Black) }
                }
            }
        }
        val r = result
        if (q.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text("Browse all", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(genres.data.orEmpty().take(60), key = { it.name }) { GenreCard(it) }
        } else {
            val genreHits = genres.data.orEmpty().filter { it.name.contains(q.trim(), ignoreCase = true) }.take(4)
            items(rankHits(q, genreHits, r?.artists.orEmpty(), r?.albums.orEmpty(), r?.songs.orEmpty()), key = { hitKey(it) }, span = { GridItemSpan(maxLineSpan) }) { h ->
                when (h) {
                    is xyz.yuuki.navidroid.Genre -> ListRow(h.name, "Genre", { GenreThumb(h.name) }) { nav.go(Route.Open(Kind.Genre, h.name)) }
                    is xyz.yuuki.navidroid.Artist -> ListRow(h.name, "Artist", { Cover(h.coverArt, 120, Modifier.size(48.dp), CircleShape) }) { nav.go(Route.Open(Kind.Artist, h.id)) }
                    is xyz.yuuki.navidroid.Album -> ListRow(h.name, "Album • ${h.artist}", { Cover(h.coverArt, 120, Modifier.size(48.dp)) }) { nav.go(Route.Open(Kind.Album, h.id)) }
                    is xyz.yuuki.navidroid.Song -> TrackRow(h, { player?.play(listOf(h), from = "Search") }, inset = 0.dp)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { BottomSpacer() }
    }
}

private fun hitKey(h: Any) = when (h) {
    is xyz.yuuki.navidroid.Genre -> "g" + h.name
    is xyz.yuuki.navidroid.Artist -> "ar" + h.id
    is xyz.yuuki.navidroid.Album -> "al" + h.id
    else -> "s" + (h as xyz.yuuki.navidroid.Song).id
}

/**
 * One mixed list, best match first: exact name, then name starting with the query, then every
 * word found in the name, then every word found across name + artist/album (so "artist song"
 * works in any order). Ties keep the server's order and interleave the kinds.
 */
fun rankHits(q: String, vararg kinds: List<Any>): List<Any> {
    val query = q.trim()
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    fun tier(h: Any): Int {
        val (name, rest) = when (h) {
            is xyz.yuuki.navidroid.Genre -> h.name to ""
            is xyz.yuuki.navidroid.Artist -> h.name to ""
            is xyz.yuuki.navidroid.Album -> h.name to h.artist
            is xyz.yuuki.navidroid.Song -> h.title to "${h.artist} ${h.album}"
            else -> "" to ""
        }
        val n = name.lowercase()
        return when {
            n == query.lowercase() -> 0
            n.startsWith(query.lowercase()) -> 1
            words.all { it in n } -> 2
            words.all { it in "$n ${rest.lowercase()}" } -> 3
            else -> 4
        }
    }
    return kinds.flatMap { it.withIndex() }.sortedWith(compareBy({ tier(it.value) }, { it.index })).map { it.value }
}

@Composable
private fun GenreThumb(name: String) {
    val art by androidx.compose.runtime.produceState<String?>(null, name) { value = runCatching { api.genreCover(name) }.getOrNull() }
    Cover(art, 120, Modifier.size(48.dp))
}

@Composable
private fun GenreCard(g: xyz.yuuki.navidroid.Genre) {
    val nav = LocalNav.current
    val art by androidx.compose.runtime.produceState<String?>(null, g.name) { value = runCatching { api.genreCover(g.name) }.getOrNull() }
    // Tinted from the cover so card and art match; the palette colour shows until it loads.
    // The cover colour is darkened for headers, so lift it back to one even, vivid brightness.
    val cover = rememberCoverColor(art, Color(xyz.yuuki.navidroid.paletteColor(g.name)))
    val bg = remember(cover) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(cover.toArgb(), it) }
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceAtLeast(0.45f), 0.6f)))
    }
    Box(Modifier.height(100.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable { nav.go(Route.Open(Kind.Genre, g.name)) }) {
        // The cover again, blurred behind a wash that is solid under the title and thins out towards the art.
        if (art != null) Cover(art, 200, Modifier.fillMaxSize().scale(1.3f).blur(10.dp), RoundedCornerShape(0.dp))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(bg.copy(alpha = 0.92f), bg.copy(alpha = 0.6f), Color.Transparent))))
        if (art != null) Cover(
            art, 200,
            Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 0.dp).size(78.dp).rotate(25f).shadow(10.dp, RoundedCornerShape(4.dp)),
        )
        Text(
            g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 56.dp),
        )
    }
}

@Composable
fun ListRow(title: String, subtitle: String, cover: @Composable () -> Unit, trailing: @Composable () -> Unit = {}, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    cover()
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailing()
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---- library --------------------------------------------------------------------------------

@Composable
fun LibraryScreen() {
    val nav = LocalNav.current
    var tab by rememberSaveable { mutableStateOf("Playlists") }
    var onlySaved by rememberSaveable { mutableStateOf(false) }
    val pins by offline.pins.collectAsStateWithLifecycle()
    val playlists = rememberLoad("playlists") { api.playlists() }
    val artists = rememberLoad("artists") { api.artists() }
    val starred = rememberLoad("starred") { api.starred() }
    val albums = remember { mutableStateListOf<xyz.yuuki.navidroid.Album>() }
    var albumsDone by remember { mutableStateOf(false) }
    val list = rememberLazyListState()

    // Albums page in as you scroll.
    LaunchedEffect(tab, onlySaved) {
        if (tab != "Albums" || onlySaved) return@LaunchedEffect
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .filter { it >= albums.size - 20 && !albumsDone }
            .distinctUntilChanged()
            .collect {
                val page = runCatching { api.albumList("alphabeticalByName", 100, albums.size) }.getOrNull() ?: return@collect
                albums += page.filter { a -> albums.none { it.id == a.id } }
                if (page.size < 100) albumsDone = true
            }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(34.dp) { nav.go(Route.Settings) }
                Spacer(Modifier.width(12.dp))
                Text("Your Library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton({ nav.go(Route.Downloads) }) { Icon(Icons.Rounded.DownloadForOffline, "Downloads") }
            }
            val chipColors = FilterChipDefaults.filterChipColors(
                containerColor = Tile, labelColor = Color.White, iconColor = Color.White,
                selectedContainerColor = Green, selectedLabelColor = Color.Black, selectedLeadingIconColor = Color.Black,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Narrows whichever tab is open to what you saved.
                FilterChip(
                    onlySaved, { onlySaved = !onlySaved },
                    { Icon(if (onlySaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Only saved", Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(32.dp), border = null, colors = chipColors,
                )
                LIBRARY_TABS.forEach { (t, icon) ->
                    FilterChip(
                        tab == t, { tab = t }, { Text(t, maxLines = 1, softWrap = false) },
                        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(32.dp), border = null, colors = chipColors,
                    )
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(horizontal = 16.dp)) {
            val pinnedIds = pins.map { "${it.kind}:${it.id}" }.toSet()
            @Composable
            fun mark(kind: Kind, id: String, saved: Boolean = false) {
                if (saved) FavoriteMark()
                if ("$kind:$id" in pinnedIds) DownloadedMark()
            }
            // Saved items first; the server list is refreshed on every visit, overrides cover the gap.
            val savedAlbums = starred.data?.list("album").orEmpty().map(xyz.yuuki.navidroid.Album::from).filter { Likes.isLiked(it.id, true) }
            val savedArtists = starred.data?.list("artist").orEmpty().map(xyz.yuuki.navidroid.Artist::from).filter { Likes.isLiked(it.id, true) }
            @Composable
            fun playlistRow(p: xyz.yuuki.navidroid.Playlist) {
                val t = p.tile()
                ListRow(t.title, if (p.mix != null) "Mix • ${p.mix.by}" else "Playlist • ${p.owner}", { TileCover(Kind.Playlist, p.id, p.coverArt, p.mix, Modifier.size(56.dp), 120) }, { mark(Kind.Playlist, p.id, p.id in Likes.playlists) }) {
                    nav.open(t)
                }
            }
            val savedAlbumIds = savedAlbums.map { it.id }.toSet()
            val savedArtistIds = savedArtists.map { it.id }.toSet()
            fun saved(kind: Kind, id: String) = when (kind) {
                Kind.Playlist -> id in Likes.playlists
                Kind.Album -> id in savedAlbumIds
                Kind.Artist -> id in savedArtistIds
                Kind.Liked -> true
                Kind.Genre -> false
            }
            fun emptySaved() = item("none") {
                Text("Nothing saved here yet. Tap the heart on a playlist, album or artist.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp))
            }
            val (mixes, ownPlaylists) = playlists.data.orEmpty()
                .filter { !onlySaved || it.id in Likes.playlists }
                .sortedBy { it.id !in Likes.playlists }.partition { it.mix != null }
            when (tab) {
                "Playlists" -> {
                    item("liked") {
                        ListRow("Liked Songs", "Playlist", { LikedCover(Modifier.size(56.dp)) }, { mark(Kind.Liked, "liked") }) { nav.go(Route.Open(Kind.Liked, "liked")) }
                    }
                    items(ownPlaylists, key = { it.id }) { playlistRow(it) }
                }
                "Mixes" -> {
                    if (onlySaved && mixes.isEmpty()) emptySaved()
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val sorted = mixes.sortedWith(compareBy({ it.id !in Likes.playlists }, { xyz.yuuki.navidroid.mixOrder(it.mix!!, hour) }))
                    MIX_SECTIONS.forEach { (section, title) ->
                        val group = sorted.filter { it.mix!!.section == section }
                        if (group.isNotEmpty()) item("section:$section") {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                        }
                        items(group, key = { it.id }) { playlistRow(it) }
                    }
                }
                "Albums" -> {
                    if (onlySaved && savedAlbums.isEmpty()) emptySaved()
                    items(if (onlySaved) savedAlbums else savedAlbums + albums.filter { it.id !in savedAlbumIds }, key = { it.id }) { a ->
                        ListRow(a.name, "Album • ${a.artist}", { Cover(a.coverArt, 120, Modifier.size(56.dp)) }, { mark(Kind.Album, a.id, a.id in savedAlbumIds) }) { nav.go(Route.Open(Kind.Album, a.id)) }
                    }
                }
                "Artists" -> {
                    if (onlySaved && savedArtists.isEmpty()) emptySaved()
                    items(if (onlySaved) savedArtists else savedArtists + artists.data.orEmpty().filter { it.id !in savedArtistIds }, key = { it.id }) { a ->
                        ListRow(a.name, "Artist", { Cover(a.coverArt, 120, Modifier.size(56.dp), CircleShape) }, { mark(Kind.Artist, a.id, a.id in savedArtistIds) }) { nav.go(Route.Open(Kind.Artist, a.id)) }
                    }
                }
                else -> {
                    val tiles = pinnedTiles(pins).filter { !onlySaved || saved(it.kind, it.id) }
                    if (tiles.isEmpty() && onlySaved) emptySaved()
                    else if (tiles.isEmpty()) item("none") {
                        Text("Tap the download button on a playlist or album to keep it on this phone.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp))
                    }
                    items(tiles, key = { "${it.kind}:${it.id}" }) { t ->
                        ListRow(t.title, t.subtitle, { TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(56.dp), 120) }, { mark(t.kind, t.id, t.kind != Kind.Liked && saved(t.kind, t.id)) }) { nav.open(t) }
                    }
                }
            }
            item("end") { BottomSpacer() }
        }
    }
}

private val LIBRARY_TABS = listOf(
    "Playlists" to Icons.AutoMirrored.Rounded.QueueMusic,
    "Mixes" to Icons.Rounded.AutoAwesome,
    "Albums" to Icons.Rounded.Album,
    "Artists" to Icons.Rounded.Person,
    "Downloaded" to Icons.Rounded.DownloadDone,
)

/** Mix sections, in the same order and wording as the home screen. */
private val MIX_SECTIONS = listOf(
    "madeForYou" to "Made For You",
    "topMixes" to "Your top mixes",
    "timeOfDay" to "Mixes for your day",
    "favorites" to "Your favorites, through the years",
)

// ---- playlist / album / liked / genre ---------------------------------------------------------

@Composable
fun CollectionScreen(kind: Kind, id: String) {
    val player = LocalPlayer.current
    val load = rememberLoad("c:$kind:$id") { api.collection(kind, id) }
    val c = load.data
    LaunchedEffect(c) { c?.let { App.it.offline.warm(it.songs) } }
    val color = c?.mix?.let { Color(it.color) }
        ?: if (kind == Kind.Liked) Color(0xFF5038A0) else if (kind == Kind.Genre) rememberCoverColor(c?.coverArt, Color(xyz.yuuki.navidroid.paletteColor(id))) else rememberCoverColor(c?.coverArt)
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 600 } }
    val wide = isWide()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = list) {
            item("header") {
                Column(
                    Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(color, Bg))).statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp),
                ) {
                    val mosaic = remember(c) { c?.takeIf { kind == Kind.Genre }?.songs?.mapNotNull { it.coverArt }?.distinct()?.take(4)?.takeIf { it.size == 4 } }
                    val coverMod = Modifier.size(if (wide) 160.dp else 240.dp).shadow(20.dp, RoundedCornerShape(6.dp))
                    val cover = @Composable { if (mosaic != null) CoverMosaic(mosaic, coverMod) else TileCover(kind, id, c?.coverArt, c?.mix, coverMod, 600) }
                    if (wide) Row(verticalAlignment = Alignment.Bottom) {
                        cover()
                        Column(Modifier.padding(start = 20.dp)) { CollectionInfo(kind, c) }
                    } else {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { cover() }
                        Spacer(Modifier.height(20.dp))
                        CollectionInfo(kind, c)
                    }
                }
            }
            item("actions") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (c != null) PinButton(c)
                    if (c != null && kind == Kind.Album) HeartButton(Likes.isLiked(c.id, c.starred)) { Likes.toggle(c.id, c.starred, "albumId") }
                    if (c != null && kind == Kind.Playlist) HeartButton(c.id in Likes.playlists) { Likes.togglePlaylist(c.id) }
                    Spacer(Modifier.weight(1f))
                    IconButton({ c?.let { player?.play(it.songs, shuffled = true, from = it.title) } }) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle play", Modifier.size(28.dp), tint = Sub)
                    }
                    Spacer(Modifier.width(8.dp))
                    val playingThis = player?.isPlaying == true && player.from == c?.title
                    PlayButton(playingThis) {
                        if (playingThis) player.toggle() else c?.let { player?.play(it.songs, from = it.title) }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
            if (c == null) item("loading") { if (load.error != null) ErrorText(load.error) else Loading() }
            if (c != null) itemsIndexed(c.songs, key = { i, s -> "$i:${s.id}" }) { i, s ->
                TrackRow(s, { player?.play(c.songs, i, from = c.title) }, number = if (kind == Kind.Album) s.track.takeIf { it > 0 } ?: (i + 1) else null, showArt = kind != Kind.Album)
            }
            item("end") { BottomSpacer() }
        }
        TopBar(c?.title ?: "", color, collapsed)
    }
}

/** Four album covers in a 2x2 grid, for a genre's header. */
@Composable
private fun CoverMosaic(ids: List<String>, modifier: Modifier) = Column(modifier.clip(RoundedCornerShape(6.dp))) {
    ids.chunked(2).forEach { row ->
        Row(Modifier.weight(1f)) { row.forEach { Cover(it, 300, Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp)) } }
    }
}

@Composable
private fun CollectionInfo(kind: Kind, c: Collection?) {
    val nav = LocalNav.current
    Text(c?.title ?: "", style = MaterialTheme.typography.headlineMedium)
    c?.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp), maxLines = 3, overflow = TextOverflow.Ellipsis) }
    if (c != null) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        c.by?.let { by ->
            Text(
                by, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(enabled = c.artistId != null) { nav.go(Route.Open(Kind.Artist, c.artistId!!)) },
            )
            Text("  •  ", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            listOfNotNull(
                when (kind) { Kind.Album -> "Album"; else -> if (c.mix != null) "Made For You" else "Playlist" },
                c.year.takeIf { it > 0 }?.toString(),
                "${c.songs.size} songs, ${formatLength(c.duration)}",
            ).joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun ErrorText(msg: String) = Text(msg, Modifier.fillMaxWidth().padding(32.dp), style = MaterialTheme.typography.bodySmall)

/** Back button that grows a coloured bar with the title once the header scrolls away. */
@Composable
private fun TopBar(title: String, color: Color, solid: Boolean) {
    val nav = LocalNav.current
    val bg = if (solid) Modifier.background(androidx.compose.ui.graphics.lerp(color, Color.Black, .45f))
    else Modifier.background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent)))
    Column(Modifier.fillMaxWidth().then(bg)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(nav::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            if (solid) Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Keep a collection downloaded; it re-syncs on every app start so rotating mixes stay fresh. */
@Composable
private fun PinButton(c: Collection) {
    val pins by offline.pins.collectAsStateWithLifecycle()
    val states by offline.states.collectAsStateWithLifecycle()
    val pinned = Pin(c.kind, c.id) in pins
    val done = c.songs.count { states[it.id] == Download.STATE_COMPLETED }
    IconButton({ if (pinned) offline.unpin(c.kind, c.id) else offline.pin(c) }) {
        Box(contentAlignment = Alignment.Center) {
            if (pinned && done < c.songs.size) CircularProgressIndicator(
                progress = { if (c.songs.isEmpty()) 0f else done / c.songs.size.toFloat() },
                Modifier.size(30.dp), color = Green, strokeWidth = 2.dp, trackColor = Color.White.copy(alpha = .15f),
            )
            Icon(Icons.Rounded.ArrowCircleDown, if (pinned) "Remove download" else "Download", Modifier.size(if (pinned && done < c.songs.size) 22.dp else 30.dp), tint = if (pinned) Green else Sub)
        }
    }
}

// ---- artist ---------------------------------------------------------------------------------

@Composable
fun ArtistScreen(id: String) {
    val player = LocalPlayer.current
    val load = rememberLoad("artist:$id") { api.artist(id) }
    val a = load.data
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 } }
    // Like the web UI: most artists have no photo, so show their most played album.
    val image = a?.albums?.maxByOrNull { it.playCount }?.coverArt ?: a?.artist?.coverArt
    val color = rememberCoverColor(image)
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = list) {
            item("header") {
                Box(Modifier.fillMaxWidth().height(if (isWide()) 220.dp else 340.dp)) {
                    Cover(image, 600, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .55f to Color.Transparent, 1f to Bg)))
                    Text(
                        a?.artist?.name ?: "", style = MaterialTheme.typography.displaySmall, fontSize = 40.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    )
                }
            }
            if (a == null) item("loading") { if (load.error != null) ErrorText(load.error) else Loading() }
            if (a != null) {
                item("actions") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeartButton(Likes.isLiked(a.artist.id, a.artist.starred)) { Likes.toggle(a.artist.id, a.artist.starred, "artistId") }
                        Text("${a.albums.size} releases", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton({
                            scope.launch { runCatching { player?.play(api.artistSongs(a), shuffled = true, from = a.artist.name) } }
                        }) { Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(28.dp), tint = Sub) }
                        Spacer(Modifier.width(8.dp))
                        PlayButton(player?.isPlaying == true && player.from == a.artist.name) {
                            scope.launch { runCatching { player?.play(a.top.ifEmpty { api.artistSongs(a) }, from = a.artist.name) } }
                        }
                    }
                }
                if (a.top.isNotEmpty()) {
                    item("popular") { SectionHeader("Popular") }
                    itemsIndexed(a.top.take(5), key = { _, s -> "top" + s.id }) { i, s ->
                        TrackRow(s, { player?.play(a.top, i, from = a.artist.name) }, number = i + 1)
                    }
                }
                item("disco") {
                    SectionHeader("Discography")
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(a.albums, key = { it.id }) { Card(it.tile()) }
                    }
                }
            }
            item("end") { BottomSpacer() }
        }
        TopBar(a?.artist?.name ?: "", color, collapsed)
    }
}
