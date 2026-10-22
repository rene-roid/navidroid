package xyz.yuuki.navidroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
import xyz.yuuki.navidroid.likedTile
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
        LazyColumn(Modifier.fillMaxSize()) {
            item("top") {
                // In landscape downloads and settings live in the rail.
                val wide = isWide()
                Row(Modifier.statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(34.dp) { nav.go(Route.Settings) }
                    Spacer(Modifier.width(12.dp))
                    Text(home.data?.greeting ?: "Hello", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    if (!wide) {
                        IconButton({ nav.go(Route.Downloads) }) { Icon(Icons.Rounded.DownloadForOffline, "Downloads") }
                        IconButton({ nav.go(Route.Settings) }) { Icon(Icons.Rounded.Settings, "Settings") }
                    }
                }
            }
            if (!online || (home.data == null && home.error != null)) item("offline") { OfflineHome(pinnedTiles(pins)) }
            home.data?.let { h ->
                item("quick") { QuickGrid(h.quick.filter(Hidden::shows)) }
                items(h.sections.map { it.copy(tiles = it.tiles.filter(Hidden::shows)) }.filter { it.tiles.isNotEmpty() }, key = { it.key }) { s ->
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
    BoxWithConstraints(modifier) {
    val cols = (maxWidth / 180.dp).toInt().coerceAtLeast(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    Row(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(4.dp)).background(Tile).pickable(t) { nav.open(t) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(56.dp), 120)
                        Text(
                            t.title, style = MaterialTheme.typography.labelLarge, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    }
}

@Composable
fun Loading() = Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
}

@Composable
fun MoreScreen(key: String) {
    val section = cachedUi<Home>("home")?.sections?.firstOrNull { it.key == key }
    LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { BackTitle(section?.title ?: "") }
        items(section?.tiles.orEmpty().filter(Hidden::shows), key = { "${it.kind}:${it.id}" }) { Card(it, width = 200.dp) }
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
    var field by rememberSaveable(stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue()) }
    val q = field.text
    // Focusing the box selects what's there so typing replaces it. The tap that focuses it then
    // places the cursor, so the first selection-only change after focus is turned back into select-all.
    var selectOnFocus by remember { mutableStateOf(false) }
    fun selectAll(v: androidx.compose.ui.text.input.TextFieldValue) = v.copy(selection = androidx.compose.ui.text.TextRange(0, v.text.length))
    var result by remember { mutableStateOf<xyz.yuuki.navidroid.SearchResult?>(null) }
    var searched by remember { mutableStateOf("") }
    LaunchedEffect(q) {
        if (q.isBlank()) return@LaunchedEffect run { result = null }
        delay(250)
        result = runCatching { api.search(q.trim()) }.getOrNull()
        searched = q
    }
    val genres = rememberLoad("genres") { api.genres() }
    // Tapping the Search tab again jumps into the search box.
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val retapAtOpen = remember { nav.retap }
    LaunchedEffect(nav.retap) {
        if (nav.retap != retapAtOpen) { focus.requestFocus(); keyboard?.show() }
    }
    // Closing the keyboard with Back leaves the box focused; drop focus so the next tap selects again.
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) { if (!imeVisible) focusManager.clearFocus() }

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
                        BasicTextField(
                            field,
                            { field = if (selectOnFocus && it.text == field.text) selectAll(it) else it; selectOnFocus = false },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                            cursorBrush = SolidColor(Color.Black),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged {
                                if (it.isFocused && field.text.isNotEmpty()) { field = selectAll(field); selectOnFocus = true }
                            },
                        )
                    }
                    if (q.isNotEmpty()) IconButton({ field = androidx.compose.ui.text.input.TextFieldValue() }) { Icon(Icons.Rounded.Close, "Clear", tint = Color.Black) }
                }
            }
        }
        val r = result
        if (q.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text("Browse all", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(genres.data.orEmpty().take(60), key = { it.name }) { GenreCard(it) }
        } else {
            val genreHits = genres.data.orEmpty().filter { it.name.contains(q.trim(), ignoreCase = true) }.take(4)
            val hits = rankHits(q, genreHits, r?.artists.orEmpty(), r?.albums.orEmpty(), r?.songs.orEmpty())
            if (r != null && searched == q && hits.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text("No results for \"${q.trim()}\"", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(hits, key = { hitKey(it) }, span = { GridItemSpan(maxLineSpan) }) { h ->
                when (h) {
                    is xyz.yuuki.navidroid.Genre -> ListRow(h.name, "Genre", { GenreThumb(h.name) }) { nav.go(Route.Open(Kind.Genre, h.name)) }
                    is xyz.yuuki.navidroid.Artist -> ListRow(h.name, "Artist", { Cover(h.coverArt, 120, Modifier.size(48.dp), CircleShape) }, pick = h.tile()) { nav.go(Route.Open(Kind.Artist, h.id)) }
                    is xyz.yuuki.navidroid.Album -> ListRow(h.name, "Album • ${h.artist}", { Cover(h.coverArt, 120, Modifier.size(48.dp)) }, pick = h.tile()) { nav.go(Route.Open(Kind.Album, h.id)) }
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
    val bg = rememberGenreTint(g.name, art)
    Box(Modifier.height(100.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable { nav.go(Route.Open(Kind.Genre, g.name)) }) {
        // The cover again, blurred behind a wash that is solid under the title and thins out towards the art.
        if (art != null) Cover(art, 300, Modifier.fillMaxSize().scale(1.3f).blur(10.dp), RoundedCornerShape(0.dp))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(bg.copy(alpha = 0.92f), bg.copy(alpha = 0.6f), Color.Transparent))))
        if (art != null) Cover(
            art, 300,
            Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 0.dp).size(78.dp).rotate(25f).shadow(10.dp, RoundedCornerShape(4.dp)),
        )
        Text(
            g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 56.dp),
        )
    }
}

@Composable
fun ListRow(title: String, subtitle: String, cover: @Composable () -> Unit, trailing: @Composable () -> Unit = {}, pick: Any? = null, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).then(if (pick != null) Modifier.pickable(pick, onClick) else Modifier.clickable(onClick = onClick)).padding(vertical = 6.dp),
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

/** [column]: the library as the side column of a landscape tablet, under the shell's Home and Search, instead of a page. */
@Composable
fun LibraryScreen(column: Boolean = false) {
    val nav = LocalNav.current
    var tab by rememberSaveable { mutableStateOf("Playlists") }
    var onlySaved by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    fun hit(vararg text: String?) = query.isBlank() || text.any { it?.contains(query.trim(), ignoreCase = true) == true }
    val pins by offline.pins.collectAsStateWithLifecycle()
    val playlists = rememberLoad("playlists") { api.playlists() }
    var creating by remember { mutableStateOf(false) }
    if (creating) NewPlaylist { id ->
        creating = false
        if (id != null) { playlists.refresh(); nav.go(Route.Open(Kind.Playlist, id)) }
    }
    val artists = rememberLoad("artists") { api.artists() }
    val starred = rememberLoad("starred") { api.starred() }
    val albums = remember { mutableStateListOf<xyz.yuuki.navidroid.Album>() }
    var albumsDone by remember { mutableStateOf(false) }
    val list = rememberLazyListState()

    // Albums page in as you scroll, or all the way through while searching so every album can match.
    LaunchedEffect(tab, onlySaved) {
        if (tab != "Albums" || onlySaved) return@LaunchedEffect
        snapshotFlow { if (!albumsDone && (query.isNotBlank() || (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= albums.size - 20)) albums.size else -1 }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .collect {
                val page = runCatching { api.albumList("alphabeticalByName", 100, albums.size) }.getOrNull() ?: return@collect
                albums += page.filter { a -> albums.none { it.id == a.id } }
                if (page.size < 100) albumsDone = true
            }
    }

    // The search box scrolls away with the list and only the filters stay; where height is short (a phone in landscape), the profile row goes too.
    val short = LocalConfiguration.current.screenHeightDp < 600
    var headerH by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableFloatStateOf(0f) }
    val collapse = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0) return Offset.Zero
                val take = minOf(-available.y, headerH - hidden).coerceAtLeast(0f)
                hidden += take
                return Offset(0f, -take)
            }

            // It only comes back once the list is at the top.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0) return Offset.Zero
                val take = minOf(available.y, hidden)
                hidden -= take
                return Offset(0f, take)
            }
        }
    }

    val profile = @Composable {
        Row(Modifier.padding(top = if (column) 4.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!column) {
                Avatar(34.dp) { nav.go(Route.Settings) }
                Spacer(Modifier.width(12.dp))
            }
            Text("Your Library", style = if (column) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton({ nav.go(Route.Downloads) }) { Icon(Icons.Rounded.DownloadForOffline, "Downloads") }
            if (column) IconButton({ nav.go(Route.Settings) }) { Icon(Icons.Rounded.Settings, "Settings") }
        }
    }

    Column(Modifier.fillMaxSize().nestedScroll(collapse)) {
        Column(Modifier.then(if (column) Modifier else Modifier.statusBarsPadding()).padding(horizontal = 16.dp)) {
            if (!short) profile()
            Column(
                Modifier.clipToBounds().layout { m, c ->
                    val p = m.measure(c)
                    headerH = p.height
                    if (hidden > p.height) hidden = p.height.toFloat()
                    val h = p.height - hidden.roundToInt()
                    layout(p.width, h) { p.place(0, h - p.height) }
                },
            ) {
                if (short) profile()
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp)).background(Tile).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = Sub)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Search your library", color = Sub, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        BasicTextField(
                            query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), cursorBrush = SolidColor(Accent),
                        )
                    }
                    if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Rounded.Close, "Clear", Modifier.size(20.dp), tint = Sub) }
                }
            }
            val chipColors = FilterChipDefaults.filterChipColors(
                containerColor = Tile, labelColor = Color.White, iconColor = Color.White,
                selectedContainerColor = Accent, selectedLabelColor = OnAccent, selectedLeadingIconColor = OnAccent,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        // Rows fade out under the filters instead of being cut off by a hard line, once there is something above to fade.
        val topFade = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
            drawContent()
            if (list.canScrollBackward) drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = 24.dp.toPx()), blendMode = BlendMode.DstIn)
        }
        // The gap under the filters is list padding, so rows scroll right up to the filters before fading.
        LazyColumn(Modifier.fillMaxSize().then(topFade), state = list, contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp)) {
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
                ListRow(t.title, if (p.mix != null) "Mix • ${p.mix.by}" else "Playlist • ${p.owner}", { TileCover(Kind.Playlist, p.id, p.coverArt, p.mix, Modifier.size(56.dp), 120) }, { mark(Kind.Playlist, p.id, p.id in Likes.playlists) }, t) {
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
                .filter { it.id !in Hidden.playlists && (!onlySaved || it.id in Likes.playlists) && hit(it.tile().title, it.owner) }
                .sortedBy { it.id !in Likes.playlists }.partition { it.mix != null }
            when (tab) {
                "Playlists" -> {
                    if (hit("Liked Songs")) item("liked") {
                        ListRow("Liked Songs", "Playlist", { LikedCover(Modifier.size(56.dp)) }, { mark(Kind.Liked, "liked") }, likedTile(null)) { nav.go(Route.Open(Kind.Liked, "liked")) }
                    }
                    if (!onlySaved && query.isBlank()) item("new") {
                        ListRow("Create playlist", "Then add songs from any song's menu", { Box(Modifier.size(56.dp).background(Tile, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null) } }) { creating = true }
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
                    items((if (onlySaved) savedAlbums else savedAlbums + albums.filter { it.id !in savedAlbumIds }).filter { hit(it.name, it.artist) }, key = { it.id }) { a ->
                        ListRow(a.name, "Album • ${a.artist}", { Cover(a.coverArt, 120, Modifier.size(56.dp)) }, { mark(Kind.Album, a.id, a.id in savedAlbumIds) }, a.tile()) { nav.go(Route.Open(Kind.Album, a.id)) }
                    }
                }
                "Artists" -> {
                    if (onlySaved && savedArtists.isEmpty()) emptySaved()
                    items((if (onlySaved) savedArtists else savedArtists + artists.data.orEmpty().filter { it.id !in savedArtistIds }).filter { hit(it.name) }, key = { it.id }) { a ->
                        ListRow(a.name, "Artist", { Cover(a.coverArt, 120, Modifier.size(56.dp), CircleShape) }, { mark(Kind.Artist, a.id, a.id in savedArtistIds) }, a.tile()) { nav.go(Route.Open(Kind.Artist, a.id)) }
                    }
                }
                else -> {
                    val tiles = pinnedTiles(pins).filter { (!onlySaved || saved(it.kind, it.id)) && hit(it.title, it.subtitle) }
                    if (tiles.isEmpty() && onlySaved) emptySaved()
                    else if (tiles.isEmpty() && query.isBlank()) item("none") {
                        Text("Tap the download button on a playlist or album to keep it on this phone.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp))
                    }
                    items(tiles, key = { "${it.kind}:${it.id}" }) { t ->
                        ListRow(t.title, t.subtitle, { TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(56.dp), 120) }, { mark(t.kind, t.id, t.kind != Kind.Liked && saved(t.kind, t.id)) }, t) { nav.open(t) }
                    }
                }
            }
            item("end") { if (column) Spacer(Modifier.height(8.dp)) else BottomSpacer() }
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
    val nav = LocalNav.current
    LaunchedEffect(color) { nav.tints[Route.Open(kind, id)] = color }
    // Mixes paint their own colour instead of the cover's.
    LaunchedEffect(c) { c?.coverArt?.takeIf { c.mix == null }?.let { nav.arts[Route.Open(kind, id)] = it } }

    val playingThis = player?.isPlaying == true && player.from == c?.title

    val backdrop = rememberGraphicsLayer()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().underBar(backdrop), state = list) {
            item("header") { BoxWithConstraints {
                val mix = c?.mix
                val size = if (!wide) 240.dp else if (isTablet()) 200.dp else 128.dp
                // Side by side only when the title gets real room beside the header, which is 1.4 covers wide: 420 dp for a phone's cover.
                val row = wide && maxWidth >= size * 1.4f + 240.dp
                val coverMod = Modifier.size(size).shadow(20.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp))
                val covers = remember(c) { albumCovers(c?.songs.orEmpty()) }
                val cover = @Composable {
                    when {
                        kind == Kind.Genre -> GenreHeader(id, remember(c) { genreHand(c) }, coverMod)
                        mix != null -> MixFlow(mix, c?.coverArt, covers, coverMod)
                        kind == Kind.Album -> AlbumHeader(c?.coverArt, playingThis, size)
                        kind == Kind.Playlist -> Box(coverMod) {
                            PlaylistWall(covers, Modifier.fillMaxSize())
                            PlaylistFront(c?.coverArt, Modifier.align(Alignment.Center).fillMaxSize(.6f))
                        }
                        else -> TileCover(kind, id, c?.coverArt, null, coverMod, 600)
                    }
                }
                if (row) Row(Modifier.statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.Bottom) {
                    cover()
                    Column(Modifier.padding(start = 20.dp)) { CollectionInfo(kind, c) }
                } else if (kind == Kind.Playlist) {
                    // Playlists and mixes run edge to edge and up under the status bar, fading out behind the title.
                    Box(Modifier.fillMaxWidth()) {
                        val fade = Modifier.matchParentSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
                                drawContent()
                                // Dimmed under the status bar and the title so they stay readable over busy covers.
                                drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = .5f), .25f to Color.Transparent, .6f to Color.Transparent, 1f to Color.Black.copy(alpha = .7f)))
                                // A playlist's wall is a backdrop for its cover, so it steps back a little.
                                if (mix == null) drawRect(Color.Black.copy(alpha = .3f))
                                drawRect(Brush.verticalGradient(.5f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                                // In landscape the wall stops short of the screen edges, so it fades out at the sides like the artist header.
                                if (wide) drawRect(Brush.horizontalGradient(0f to Color.Transparent, .12f to Color.Black, .88f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                            }
                        if (mix != null) MixFlow(mix, c?.coverArt, covers, fade) else PlaylistWall(covers, fade)
                        Column(Modifier.statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp)) {
                            // A playlist's own cover, which may be one set by hand, stands in front of its wall.
                            if (mix == null) Box(Modifier.fillMaxWidth().height(194.dp), contentAlignment = Alignment.TopCenter) {
                                PlaylistFront(c?.coverArt, Modifier.size(176.dp))
                            } else Spacer(Modifier.height(194.dp))
                            CollectionInfo(kind, c)
                        }
                    }
                } else Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { cover() }
                    Spacer(Modifier.height(20.dp))
                    CollectionInfo(kind, c)
                }
            } }
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
        TopBar(c?.title ?: "", color, collapsed, backdrop)
    }
}

@Composable
private fun CollectionInfo(kind: Kind, c: Collection?) {
    val nav = LocalNav.current
    // Long words hyphenate instead of breaking mid-word in the narrow landscape column.
    Text(
        c?.title ?: "", style = MaterialTheme.typography.headlineMedium.copy(hyphens = Hyphens.Auto, lineBreak = LineBreak.Heading),
        maxLines = 3, overflow = TextOverflow.Ellipsis,
    )
    c?.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp), maxLines = 3, overflow = TextOverflow.Ellipsis) }
    if (c != null) FlowRow(Modifier.padding(top = 8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
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

/** Whether the bar frosts the page behind it instead of covering it, so the aurora shows through. Blurring needs Android 12. */
private val acrylic get() = auroraOn.on && Build.VERSION.SDK_INT >= 31

/**
 * Draws the page and records it into [layer] for [TopBar]. When [acrylic], the strip under the bar is left for the bar to draw frosted.
 * In landscape the bar only spans the page column, so a frosted box would cut a hard-edged block out of the aurora; there the page fades out under the title instead.
 */
@Composable
private fun Modifier.underBar(layer: GraphicsLayer): Modifier {
    val density = LocalDensity.current
    val bar = WindowInsets.statusBars.getTop(density) + with(density) { 56.dp.toPx() }
    val wide = isWide()
    val frost = acrylic && !wide
    val fade = acrylic && wide
    // Clear behind the whole title even at large font scales, back to full strength just below the bar.
    val fadeFrom = bar - with(density) { 12.dp.toPx() }
    val fadeTo = bar + with(density) { 8.dp.toPx() }
    return then(if (fade) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else Modifier).drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        when {
            frost -> clipRect(top = bar) { drawLayer(layer) }
            fade -> {
                drawLayer(layer)
                drawRect(Brush.verticalGradient(0f to Color.Transparent, fadeFrom / size.height to Color.Transparent, fadeTo / size.height to Color.Black), blendMode = BlendMode.DstIn)
            }
            else -> drawLayer(layer)
        }
    }
}

/** Back button that eases in a coloured bar with the title once the header scrolls away; with the aurora on the bar is frosted glass. */
@Composable
private fun TopBar(title: String, color: Color, solid: Boolean, backdrop: GraphicsLayer) {
    val nav = LocalNav.current
    val t by animateFloatAsState(if (solid) 1f else 0f, tween(400), label = "bar")
    val wide = isWide()
    val frost = acrylic && !wide
    // In landscape it paints exactly the shell's background (the tint fading to Bg over the top 45% of the window), so it has no edges.
    val shellH = LocalWindowInfo.current.containerSize.height * .45f
    Box(Modifier.fillMaxWidth()) {
        // The page behind the bar, sharp while the header shows, frosting over as the bar fills in.
        if (frost) Box(
            Modifier.matchParentSize().graphicsLayer {
                clip = true
                renderEffect = (24.dp.toPx() * t).takeIf { it > 0f }?.let { BlurEffect(it, it) }
            }.drawBehind { drawLayer(backdrop) },
        )
        Column(
            Modifier.fillMaxWidth().drawBehind {
                if (!wide) drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent)), alpha = 1 - t)
                when {
                    frost -> drawRect(lerp(color, Color.Black, .45f), alpha = .55f * t)
                    // Landscape over the aurora: no bar at all, the page fades out under the title instead (see underBar).
                    acrylic -> {}
                    wide -> drawRect(Brush.verticalGradient(listOf(color, Bg), startY = 0f, endY = shellH), alpha = t)
                    else -> drawRect(lerp(color, Color.Black, .45f), alpha = t)
                }
            },
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(nav::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                if (t > 0f) Text(title, Modifier.graphicsLayer { alpha = t }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
                Modifier.size(30.dp), color = Accent, strokeWidth = 2.dp, trackColor = Color.White.copy(alpha = .15f),
            )
            Icon(Icons.Rounded.ArrowCircleDown, if (pinned) "Remove download" else "Download", Modifier.size(if (pinned && done < c.songs.size) 22.dp else 30.dp), tint = if (pinned) Accent else Sub)
        }
    }
}

// ---- artist ---------------------------------------------------------------------------------

@Composable
fun ArtistScreen(id: String) {
    val player = LocalPlayer.current
    val load = rememberLoad("artist:$id") { api.artist(id) }
    val a = load.data
    LaunchedEffect(a) { a?.let { App.it.offline.warm(it.top) } }
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 } }
    // Like the web UI: most artists have no photo, so show their most played album.
    val image = a?.albums?.maxByOrNull { it.playCount }?.coverArt ?: a?.artist?.coverArt
    val color = rememberCoverColor(image)
    val scope = rememberCoroutineScope()
    val nav = LocalNav.current
    LaunchedEffect(color) { nav.tints[Route.Open(Kind.Artist, id)] = color }
    LaunchedEffect(image) { image?.let { nav.arts[Route.Open(Kind.Artist, id)] = it } }
    val wide = isWide()

    val backdrop = rememberGraphicsLayer()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().underBar(backdrop), state = list) {
            item("header") {
                // In landscape the bar fades out whatever is under it, so the header starts below it.
                Box(Modifier.fillMaxWidth().then(if (wide) Modifier.statusBarsPadding().padding(top = 56.dp).height(if (isTablet()) 300.dp else 220.dp) else Modifier.height(340.dp))) {
                    // The photo fades out into the shared background instead of meeting it at an edge, and at the sides too in landscape.
                    ArtistHeader(
                        a?.artist?.coverArt, image, remember(a) { (a?.albums.orEmpty() + a?.appearsOn.orEmpty()).mapNotNull { it.coverArt } }, color,
                        Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
                            drawContent()
                            if (wide) drawRect(Brush.horizontalGradient(0f to Color.Transparent, .12f to Color.Black, .88f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                            drawRect(if (wide) Brush.verticalGradient(0f to Color.Transparent, .12f to Color.Black, .45f to Color.Black, 1f to Color.Transparent) else Brush.verticalGradient(.6f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                        },
                    )
                    Text(
                        a?.artist?.name ?: "", style = MaterialTheme.typography.displaySmall, fontSize = 40.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    )
                }
            }
            if (a == null) item("loading") { if (load.error != null) ErrorText(load.error) else Loading() }
            if (a != null) {
                item("actions") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeartButton(Likes.isLiked(a.artist.id, a.artist.starred)) { Likes.toggle(a.artist.id, a.artist.starred, "artistId") }
                        Text(if (a.albums.size == 1) "1 release" else "${a.albums.size} releases", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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
                val (singles, albums) = a.albums.partition { it.single }
                for ((title, releases) in listOf("Albums" to albums, "Singles and EPs" to singles, "Appears on" to a.appearsOn)) if (releases.isNotEmpty()) item(title) {
                    SectionHeader(title)
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(releases, key = { it.id }) { Card(it.tile()) }
                    }
                }
            }
            item("end") { BottomSpacer() }
        }
        TopBar(a?.artist?.name ?: "", color, collapsed, backdrop)
    }
}
