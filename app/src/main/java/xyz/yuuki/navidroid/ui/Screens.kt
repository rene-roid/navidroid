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
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import xyz.yuuki.navidroid.Facet
import xyz.yuuki.navidroid.FACETS
import xyz.yuuki.navidroid.LibFilter
import xyz.yuuki.navidroid.LibSort
import xyz.yuuki.navidroid.LibType
import xyz.yuuki.navidroid.creator
import xyz.yuuki.navidroid.generatedSubtitle
import xyz.yuuki.navidroid.libType
import xyz.yuuki.navidroid.libraryRows
import xyz.yuuki.navidroid.visibleTypes
import androidx.compose.runtime.saveable.Saver
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import xyz.yuuki.navidroid.Album
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.Section
import xyz.yuuki.navidroid.becauseShelf
import xyz.yuuki.navidroid.jumpBackShelf
import xyz.yuuki.navidroid.shuffledRadio
import xyz.yuuki.navidroid.throwbackYears
import xyz.yuuki.navidroid.unplayedShelf
import xyz.yuuki.navidroid.Collection
import xyz.yuuki.navidroid.Home
import xyz.yuuki.navidroid.Kind
import xyz.yuuki.navidroid.Pin
import xyz.yuuki.navidroid.Recent
import xyz.yuuki.navidroid.Song
import xyz.yuuki.navidroid.Tile
import xyz.yuuki.navidroid.buildHome
import xyz.yuuki.navidroid.likedTile
import xyz.yuuki.navidroid.listeningTiles
import xyz.yuuki.navidroid.list
import xyz.yuuki.navidroid.tile
import xyz.yuuki.navidroid.withOthers
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

/** The home shelves that need extra calls; loaded apart from [loadHome] so Home paints first. A shelf that fails or has nothing is left out. */
suspend fun loadShelves(): List<Section> = coroutineScope {
    val recent = api.albumList("recent", 40)
    fun shelf(key: String, title: String, albums: List<Album>) = Section(key, title, albums.map { it.tile() })
    val because = async {
        val top = recent.firstOrNull() ?: return@async null
        val similar = top.artistId?.let { api.similarArtists(it, 6) }.orEmpty()
        shelf("because", "Because you listened to ${top.name}", becauseShelf(similar.map { a -> async { api.artistAlbums(a.id) } }.awaitAll(), recent))
    }
    val unplayed = async { shelf("unplayed", "Albums you haven't played yet", unplayedShelf(api.albumList("random", 60))) }
    val throwback = async {
        throwbackYears(Calendar.getInstance().get(Calendar.YEAR)).map { (y, title) -> async { shelf("throwback", title, api.albumsByYear(y, y, 20)) } }
            .awaitAll().firstOrNull { it.tiles.isNotEmpty() }
    }
    val jump = async { shelf("jump-back", "Jump back in", jumpBackShelf(api.albumList("frequent", 100), recent)) }
    listOf(because, unplayed, throwback, jump).mapNotNull { runCatching { it.await() }.getOrNull() }.filter { it.tiles.isNotEmpty() }
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
    val shelves = rememberLoad("home-shelves") { loadShelves() }
    val refresh = {
        api.networkChanged(true)
        home.refresh()
        shelves.refresh()
    }
    val here = LocalPlayer.current?.song?.id
    // Polled only while Home is on screen; a server that won't say just shows nothing.
    val listeners by remember {
        flow { while (true) { emit(runCatching { api.listeners() }.getOrDefault(emptyList())); delay(30_000) } }
    }.collectAsStateWithLifecycle(emptyList())
    val listening = remember(listeners, here) { listeningTiles(listeners, App.it.prefs.username, here) }
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
                if (online && listening.isNotEmpty()) item("listening") {
                    SectionHeader("Listening now")
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(listening, key = { "${it.id}:${it.subtitle}" }) { Card(it) }
                    }
                }
                items(h.sections.map { it.copy(tiles = it.tiles.filter(Hidden::shows)) }.filter { it.tiles.isNotEmpty() }, key = { it.key }) { s ->
                    SectionHeader(s.title) { nav.go(Route.More(s.key)) }
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(s.tiles, key = { "${it.kind}:${it.id}" }) { Card(it) }
                    }
                }
                if (online) items(shelves.data.orEmpty(), key = { "shelf:${it.key}" }) { s ->
                    SectionHeader(s.title)
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

/** Albums never played, for the Search tab. Kept while the app runs, so the shelf doesn't change at every visit. */
private var discover by mutableStateOf<List<xyz.yuuki.navidroid.Album>?>(null)

private suspend fun loadDiscover() {
    runCatching { api.albumList("random", 40).filter { it.playCount == 0 }.take(12) }.onSuccess { discover = it }
}

@Composable
fun SearchScreen() {
    val nav = LocalNav.current
    val player = LocalPlayer.current
    val scope = rememberCoroutineScope()
    var field by rememberSaveable(stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue()) }
    val q = field.text
    // Focusing the box selects what's there so typing replaces it. The tap that focuses it then
    // places the cursor, so the first selection-only change after focus is turned back into select-all.
    var selectOnFocus by remember { mutableStateOf(false) }
    fun selectAll(v: androidx.compose.ui.text.input.TextFieldValue) = v.copy(selection = androidx.compose.ui.text.TextRange(0, v.text.length))
    var result by remember { mutableStateOf<xyz.yuuki.navidroid.SearchResult?>(null) }
    var searched by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf("All") }
    LaunchedEffect(q) {
        failed = false
        searching = q.isNotBlank()
        // Typing on narrows what is already shown; any other change starts from nothing.
        if (q.isBlank() || !q.startsWith(searched)) result = null
        if (q.isBlank()) return@LaunchedEffect
        delay(250)
        try {
            result = api.search(q.trim())
            searched = q
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            result = null
            failed = true
        }
        searching = false
    }
    val genres = rememberLoad("genres") { api.genres() }
    val decades = rememberLoad("decades") { api.decades() }
    LaunchedEffect(Unit) { if (discover == null) loadDiscover() }
    var recents by remember { mutableStateOf(App.it.prefs.recentSearches) }
    fun save(v: List<Recent>) {
        recents = v
        App.it.prefs.recentSearches = v
    }
    /** A song starts its radio; anything else opens its page, or with [play] starts playing. */
    fun open(r: Recent, play: Boolean = false) {
        save(Recent.add(recents, r))
        val kind = r.kind
        when {
            r.song != null -> player?.playRadio(r.song)
            kind == null -> {}
            !play -> nav.go(Route.Open(kind, r.id))
            else -> scope.launch {
                runCatching {
                    if (kind == Kind.Artist) api.artist(r.id).let { a -> player?.play(a.top.ifEmpty { api.artistSongs(a) }, from = r.title) }
                    else player?.play(api.collection(kind, r.id).songs, shuffled = kind == Kind.Genre, from = r.title)
                }
            }
        }
    }
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

    val wide = GridItemSpan(2)
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { wide }) {
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
                // Always takes its room, so results don't jump when a search starts or ends.
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(2.dp)) {
                    if (searching) LinearProgressIndicator(Modifier.fillMaxSize(), color = Accent, trackColor = Color.Transparent)
                }
            }
        }
        if (q.isBlank()) {
            if (recents.isNotEmpty()) {
                item(span = { wide }) { SearchHeader("Recent searches") { TextButton({ save(emptyList()) }) { Text("Clear all", color = Sub) } } }
                items(recents, key = { "recent:${it.key}" }, span = { wide }) { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { ListRow(r.title, r.subtitle, { HitCover(r) }) { open(r) } }
                        IconButton({ save(recents - r) }) { Icon(Icons.Rounded.Close, "Remove", tint = Sub) }
                    }
                }
            }
            item(span = { wide }) {
                Column {
                    val albums = discover.orEmpty()
                    if (albums.isNotEmpty()) {
                        SearchHeader("Discover") { IconButton({ scope.launch { loadDiscover() } }) { Icon(Icons.Rounded.Refresh, "Other albums", tint = Sub) } }
                        Text("Albums you haven't played yet", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                        LazyRow { items(albums, key = { it.id }) { a -> ShelfCard(Recent.of(a)) { nav.go(Route.Open(Kind.Album, a.id)) } } }
                    }
                    Row(
                        Modifier.padding(top = 12.dp).clip(CircleShape).background(Tile)
                            .clickable { scope.launch { runCatching { player?.play(api.randomSongs(100), from = "Your library") } } }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, Modifier.size(20.dp))
                        Text("Shuffle your library", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item(span = { wide }) { SearchHeader("Browse all") }
            val decades = decades.data.orEmpty()
            if (decades.isNotEmpty()) item(span = { wide }) {
                Column {
                    Text("Decades", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(decades, key = { it.first }) { (start, art) -> BrowseCard("${start}s", art.ifEmpty { null }, Modifier.width(160.dp)) { nav.go(Route.Open(Kind.Decade, "$start")) } }
                    }
                }
            }
            items(genres.data.orEmpty().take(60), key = { it.name }) { GenreCard(it) { nav.go(Route.Open(Kind.Genre, it.name)) } }
        } else {
            val r = result
            val genreHits = rankHits(q, genres.data.orEmpty().filter { it.name.contains(q.trim(), ignoreCase = true) })
            val songs = rankHits(q, r?.songs.orEmpty())
            val artists = rankHits(q, r?.artists.orEmpty())
            val albums = rankHits(q, r?.albums.orEmpty())
            val lists = mapOf("Songs" to songs, "Artists" to artists, "Albums" to albums, "Genres" to genreHits).filterValues { it.isNotEmpty() }
            val shown = tab.takeIf { it in lists } ?: "All"
            if (lists.isNotEmpty()) item(span = { wide }) {
                val chipColors = FilterChipDefaults.filterChipColors(containerColor = Tile, labelColor = Color.White, selectedContainerColor = Accent, selectedLabelColor = OnAccent)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("All") + lists.keys).forEach { t ->
                        FilterChip(shown == t, { tab = t }, { Text(t, maxLines = 1, softWrap = false) }, shape = RoundedCornerShape(32.dp), border = null, colors = chipColors)
                    }
                }
            }
            if (failed || (lists.isEmpty() && r != null && !searching)) item(span = { wide }) {
                Text(
                    if (failed) "Couldn't search, check your connection" else "No results for \"${q.trim()}\"",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            if (shown == "Genres") items(genreHits.map(Recent::of), key = { it.key }) { g -> GenreCard(xyz.yuuki.navidroid.Genre(g.id, 0)) { open(g) } }
            else if (shown != "All") items(lists.getValue(shown).map(Recent::of), key = { it.key }, span = { wide }) { h -> HitRow(h) { open(h) } }
            else if (lists.isNotEmpty()) {
                val top = Recent.of(rankHits(q, genreHits.take(4), artists, albums, songs).first())
                item(span = { wide }) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Tile).clickable { open(top) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HitCover(top, 92.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text("Top result", style = MaterialTheme.typography.labelMedium, color = Sub)
                            Text(top.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(top.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        PlayButton(false, 48.dp) { open(top, play = true) }
                    }
                }
                if (songs.isNotEmpty()) {
                    item(span = { wide }) { SearchHeader("Songs") { if (songs.size > 4) TextButton({ tab = "Songs" }) { Text("See all", color = Sub) } } }
                    items(songs.take(4).map(Recent::of), key = { it.key }, span = { wide }) { h -> HitRow(h) { open(h) } }
                }
                for ((title, list) in listOf("Artists" to artists, "Albums" to albums)) if (list.isNotEmpty()) item(span = { wide }) {
                    Column {
                        SearchHeader(title)
                        LazyRow { items(list.map(Recent::of), key = { it.key }) { h -> ShelfCard(h) { open(h) } } }
                    }
                }
            }
        }
        item(span = { wide }) { BottomSpacer() }
    }
}

private val Recent.key get() = "$kind:$id"

/** For pressing and holding to select; songs and genres have their own ways. */
private fun Recent.tile() = if (kind == Kind.Album || kind == Kind.Artist) Tile(kind, id, title, subtitle, coverArt) else null

@Composable
private fun SearchHeader(title: String, trailing: @Composable () -> Unit = {}) = Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
    trailing()
}

@Composable
private fun HitCover(r: Recent, size: Dp = 48.dp) {
    val art = if (r.kind != Kind.Genre) r.coverArt else androidx.compose.runtime.produceState<String?>(null, r.id) { value = runCatching { api.genreCover(r.id) }.getOrNull() }.value
    Cover(art, if (size > 48.dp) 300 else 120, Modifier.size(size), if (r.kind == Kind.Artist) CircleShape else RoundedCornerShape(4.dp))
}

@Composable
private fun HitRow(r: Recent, onClick: () -> Unit) =
    if (r.song != null) TrackRow(r.song, onClick, inset = 0.dp) else ListRow(r.title, r.subtitle, { HitCover(r) }, pick = r.tile(), onClick = onClick)

/** An album or artist in a sideways row. */
@Composable
private fun ShelfCard(r: Recent, onClick: () -> Unit) = Column(
    Modifier.width(132.dp).clip(RoundedCornerShape(6.dp)).then(r.tile()?.let { Modifier.pickable(it, onClick) } ?: Modifier.clickable(onClick = onClick)).padding(6.dp),
) {
    HitCover(r, 120.dp)
    Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    Text(r.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun GenreCard(g: xyz.yuuki.navidroid.Genre, onClick: () -> Unit) {
    val art by androidx.compose.runtime.produceState<String?>(null, g.name) { value = runCatching { api.genreCover(g.name) }.getOrNull() }
    BrowseCard(g.name, art, onClick = onClick)
}

@Composable
private fun BrowseCard(name: String, art: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = rememberGenreTint(name, art)
    Box(modifier.height(100.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable(onClick = onClick)) {
        // The cover again, blurred behind a wash that is solid under the title and thins out towards the art.
        if (art != null) Cover(art, 300, Modifier.fillMaxSize().scale(1.3f).blur(10.dp), RoundedCornerShape(0.dp))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(bg.copy(alpha = 0.92f), bg.copy(alpha = 0.6f), Color.Transparent))))
        if (art != null) Cover(
            art, 300,
            Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 0.dp).size(78.dp).rotate(25f).shadow(10.dp, RoundedCornerShape(4.dp)),
        )
        Text(
            name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(column: Boolean = false) {
    val nav = LocalNav.current
    val prefs = App.it.prefs
    // No type picked shows everything in one list; the facet chips narrow whatever is shown.
    var filter by rememberSaveable(stateSaver = Saver({ it.encode() }, LibFilter::decode)) { mutableStateOf(LibFilter()) }
    var sort by remember { mutableStateOf(LibSort.entries[prefs.librarySort.coerceIn(0, LibSort.entries.size - 1)]) }
    var grid by remember { mutableStateOf(prefs.libraryGrid) }
    var showMixes by remember { mutableStateOf(prefs.libraryMixes) }
    var options by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    fun hit(vararg text: String?) = query.isBlank() || text.any { it?.contains(query.trim(), ignoreCase = true) == true }
    val pins by offline.pins.collectAsStateWithLifecycle()
    val downloaded = remember(pins) { pinnedTiles(pins) }
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
    val list = rememberLazyGridState()

    // Albums page in as you scroll, or all the way through while searching so every album can match.
    LaunchedEffect(filter) {
        if (filter.type != LibType.Albums || filter.facets.isNotEmpty()) return@LaunchedEffect
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
        }
        val user = prefs.username.orEmpty()
        val pinnedIds = pins.map { "${it.kind}:${it.id}" }.toSet()
        fun pinned(t: Tile) = "${t.kind}:${t.id}" in pinnedIds
        // Saved items first; the server list is refreshed on every visit, overrides cover the gap.
        val savedAlbums = starred.data?.list("album").orEmpty().map(xyz.yuuki.navidroid.Album::from).filter { Likes.isLiked(it.id, true) }
        val savedArtists = starred.data?.list("artist").orEmpty().map(xyz.yuuki.navidroid.Artist::from).filter { Likes.isLiked(it.id, true) }
        val savedAlbumIds = savedAlbums.map { it.id }.toSet()
        val savedArtistIds = savedArtists.map { it.id }.toSet()
        fun saved(t: Tile) = when (t.kind) {
            Kind.Playlist -> t.id in Likes.playlists
            Kind.Album -> t.id in savedAlbumIds
            Kind.Artist -> t.id in savedArtistIds
            Kind.Liked -> true
            Kind.Genre, Kind.Decade -> false
            Kind.ArtistMix, Kind.ArtistRadio -> Likes.hasMix(t.kind, t.id)
        }
        // Every album and artist on the server only under their own type; everywhere else just the saved ones.
        val rows = listOf(likedTile(null)) +
            playlists.data.orEmpty().filter { it.id !in Hidden.playlists }.map { p -> p.tile().let { it.copy(subtitle = if (p.mix != null) generatedSubtitle(it) else "Playlist • ${p.owner}") } } +
            Likes.mixes.map { it.copy(subtitle = generatedSubtitle(it)) } +
            (savedAlbums + if (filter.type == LibType.Albums) albums.filter { it.id !in savedAlbumIds } else emptyList()).map { it.tile().copy(subtitle = "Album • ${it.artist}") } +
            (savedArtists + if (filter.type == LibType.Artists) artists.data.orEmpty().filter { it.id !in savedArtistIds } else emptyList()).map { it.tile() }
        val keys = rows.map { "${it.kind}:${it.id}" }.toSet()
        val all = rows + downloaded.filter { "${it.kind}:${it.id}" !in keys && Hidden.shows(it) }
        val shown = libraryRows(all, filter, sort, user, showMixes, ::saved, ::pinned) { hit(it.title, creator(it, user)) }

        val chipColors = @Composable { on: Boolean -> if (on) Accent to OnAccent else Tile to Color.White }
        @Composable
        fun LazyItemScope.chip(on: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) = chipColors(on).let { (bg, fg) ->
            Surface(onClick, Modifier.animateItem().height(32.dp), shape = RoundedCornerShape(16.dp), color = bg, contentColor = fg) {
                Row(Modifier.padding(horizontal = 12.dp).semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) { content() }
            }
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filter.active) item("clear") { chip(false, "Clear filters", { filter = LibFilter() }) { Icon(Icons.Rounded.Close, null, Modifier.size(18.dp)) } }
                if (filter.type == null) items(visibleTypes(all), key = { it.name }) { t -> chip(false, t.name, { filter = filter.pick(t) }) { Text(t.name, maxLines = 1, softWrap = false) } }
                else item(filter.type!!.name) { chip(true, filter.type!!.name, { filter = filter.pick(null) }) { Text(filter.type!!.name, maxLines = 1, softWrap = false) } }
                items(FACETS.getValue(filter.type), key = { "f:${it.name}" }) { f ->
                    val on = f in filter.facets
                    chip(on, f.name, { filter = filter.toggle(f) }) {
                        when (f) {
                            Facet.ByYou -> Text("By you", maxLines = 1, softWrap = false)
                            Facet.ByOthers -> Text("By others", maxLines = 1, softWrap = false)
                            Facet.Saved -> Icon(if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, Modifier.size(18.dp))
                            Facet.Downloaded -> Icon(if (on) Icons.Rounded.ArrowCircleDown else Icons.Outlined.ArrowCircleDown, null, Modifier.size(18.dp))
                        }
                    }
                }
            }
            IconButton({ options = true }) { Icon(Icons.Rounded.Tune, "View options") }
        }
        // Rows fade out under the filters instead of being cut off by a hard line, once there is something above to fade.
        val topFade = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
            drawContent()
            if (list.canScrollBackward) drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = 24.dp.toPx()), blendMode = BlendMode.DstIn)
        }
        // The gap under the filters is list padding, so rows scroll right up to the filters before fading.
        LazyVerticalGrid(
            if (grid) GridCells.Adaptive(110.dp) else GridCells.Fixed(1), Modifier.fillMaxSize().then(topFade), state = list,
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(if (grid) 4.dp else 0.dp),
        ) {
            val marks = @Composable { t: Tile ->
                if (saved(t) && t.kind != Kind.Liked) FavoriteMark()
                if (pinned(t)) DownloadedMark()
            }
            @Composable
            fun cell(t: Tile) = if (!grid) ListRow(t.title, t.subtitle, { TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.size(56.dp), 120) }, { marks(t) }, t) { nav.open(t) }
            else Column(Modifier.clip(RoundedCornerShape(6.dp)).pickable(t) { nav.open(t) }.padding(6.dp)) {
                TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    marks(t)
                    Text(t.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            fun LazyGridScope.header(key: String, title: String) = item(key, span = { GridItemSpan(maxLineSpan) }) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            fun LazyGridScope.group(ts: List<Tile>) = items(ts, key = { "${it.kind}:${it.id}" }) { cell(it) }

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val sections = when (filter.type) {
                LibType.Radios -> listOf<Pair<String, (Tile) -> Boolean>>("Artist radios" to { it.mix?.kind != "genreradio" }, "Genre radios" to { it.mix?.kind == "genreradio" })
                LibType.Mixes -> MIX_SECTIONS.map { (s, title) -> title to { t: Tile -> t.mix?.section == s } }
                else -> null
            }
            if (sections != null) {
                sections.forEach { (title, pick) ->
                    val g = shown.filter(pick).let { if (sort == LibSort.Default) it.sortedWith(compareBy({ !saved(it) }, { xyz.yuuki.navidroid.mixOrder(it.mix!!, hour) })) else it }
                    if (g.isNotEmpty()) header("section:$title", title)
                    group(g)
                }
            } else {
                val (generated, own) = shown.partition { libType(it) == LibType.Radios || libType(it) == LibType.Mixes }
                if (filter.type == LibType.Playlists && filter.facets.isEmpty() && query.isBlank()) item("new", span = { GridItemSpan(maxLineSpan) }) {
                    ListRow("Create playlist", "Then add songs from any song's menu", { Box(Modifier.size(56.dp).background(Tile, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null) } }) { creating = true }
                }
                group(own)
                if (generated.isNotEmpty()) header("section:generated", "Mixes & radios")
                group(generated)
            }
            if (shown.isEmpty() && filter.facets.isNotEmpty()) item("none", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (Facet.Downloaded in filter.facets) "Nothing downloaded here yet. Tap the download button on a playlist or album to keep it on this phone."
                    else "Nothing here yet. Tap the heart on a playlist, album or artist.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            item("end", span = { GridItemSpan(maxLineSpan) }) { if (column) Spacer(Modifier.height(8.dp)) else BottomSpacer() }
        }
    }
    if (options) ModalBottomSheet({ options = false }, containerColor = Tile) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            @Composable
            fun choice(title: String, on: Boolean, pick: () -> Unit) = Row(Modifier.fillMaxWidth().clickable(onClick = pick), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(on, pick)
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
            Text("Sort by", style = MaterialTheme.typography.titleMedium)
            listOf(LibSort.Default to "Saved first", LibSort.Alphabetical to "Alphabetical", LibSort.Creator to "Creator").forEach { (s, title) ->
                choice(title, sort == s) { sort = s; prefs.librarySort = s.ordinal }
            }
            Text("Layout", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            choice("List", !grid) { grid = false; prefs.libraryGrid = false }
            choice("Grid", grid) { grid = true; prefs.libraryGrid = true }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Show mixes & radios in Your Library", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(showMixes, { showMixes = it; prefs.libraryMixes = it })
            }
        }
    }
}

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
        ?: if (kind == Kind.Liked) Color(0xFF5038A0) else if (kind == Kind.Genre || kind == Kind.Decade) rememberCoverColor(c?.coverArt, Color(xyz.yuuki.navidroid.paletteColor(id))) else rememberCoverColor(c?.coverArt)
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 600 } }
    val wide = isWide()
    val nav = LocalNav.current
    LaunchedEffect(color) { nav.tints[Route.Open(kind, id)] = color }
    // Mixes paint their own colour instead of the cover's.
    LaunchedEffect(c) { c?.coverArt?.takeIf { c.mix == null }?.let { nav.arts[Route.Open(kind, id)] = it } }

    val playingThis = player?.isPlaying == true && player.from == c?.title
    val artistMix = kind == Kind.ArtistMix || kind == Kind.ArtistRadio

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
                } else if (kind == Kind.Playlist || artistMix) {
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
                    if (artistMix && c?.by != null) HeartButton(Likes.hasMix(kind, id)) { Likes.toggleMix(xyz.yuuki.navidroid.artistMixTile(kind, id, c.by, c.coverArt)) }
                    Spacer(Modifier.weight(1f))
                    if (c != null && (kind == Kind.Genre || kind == Kind.Decade)) IconButton({ player?.play(shuffledRadio(c.songs, App.it.prefs.radioFresh), from = "${c.title} Radio") }) {
                        Icon(Icons.Rounded.Radio, "Radio", Modifier.size(28.dp), tint = Sub)
                    }
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
    val image = a?.image
    val color = rememberCoverColor(image)
    val scope = rememberCoroutineScope()
    val nav = LocalNav.current
    LaunchedEffect(color) { nav.tints[Route.Open(Kind.Artist, id)] = color }
    LaunchedEffect(image) { image?.let { nav.arts[Route.Open(Kind.Artist, id)] = it } }
    val wide = isWide()
    // Songs they share the credit on, wherever they came out, newest first.
    val collabs = remember(a) { a?.songs.orEmpty().filter { it.withOthers(id) }.sortedByDescending { it.year } }
    val similar = rememberLoad("similar:$id") { api.similarArtists(id) }
    var allTop by rememberSaveable { mutableStateOf(false) }
    var allCollabs by rememberSaveable { mutableStateOf(false) }

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
                val (singles, albums) = a.albums.partition { it.single }
                item("actions") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeartButton(Likes.isLiked(a.artist.id, a.artist.starred)) { Likes.toggle(a.artist.id, a.artist.starred, "artistId") }
                        Text(
                            listOfNotNull(count(albums.size, "album"), count(singles.size, "single")).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall, maxLines = 2, modifier = Modifier.weight(1f),
                        )
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
                    itemsIndexed(a.top.take(if (allTop) 10 else 5), key = { _, s -> "top" + s.id }) { i, s ->
                        TrackRow(s, { player?.play(a.top, i, from = a.artist.name) }, number = i + 1)
                    }
                    if (a.top.size > 5) item("popular-more") { SeeMore(allTop) { allTop = !allTop } }
                }
                item("mixes") {
                    SectionHeader("Mixes")
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(listOf(Kind.ArtistMix, Kind.ArtistRadio), key = { it.name }) { Card(xyz.yuuki.navidroid.artistMixTile(it, id, a.artist.name, image)) }
                    }
                }
                for ((title, releases) in listOf("Albums" to albums, "Singles and EPs" to singles)) if (releases.isNotEmpty()) item(title) {
                    SectionHeader(title)
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        // Their own name under each would only repeat the page's.
                        items(releases, key = { it.id }) { r ->
                            Card(r.tile().copy(subtitle = listOfNotNull(r.year.takeIf { it > 0 }?.toString(), count(r.songCount, "song")).joinToString(" • ")))
                        }
                    }
                }
                if (!similar.data.isNullOrEmpty()) item("similar") {
                    SectionHeader("Fans also like")
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(similar.data.orEmpty(), key = { it.id }) { Card(it.tile()) }
                    }
                }
                if (collabs.isNotEmpty()) {
                    item("collabs") { SectionHeader("Collaborations") }
                    itemsIndexed(if (allCollabs) collabs else collabs.take(5), key = { _, s -> "collab" + s.id }) { i, s ->
                        TrackRow(s, { player?.play(collabs, i, from = a.artist.name) })
                    }
                    if (collabs.size > 5) item("collabs-more") { SeeMore(allCollabs) { allCollabs = !allCollabs } }
                }
                if (a.appearsOn.isNotEmpty()) item("Appears on") {
                    SectionHeader("Appears on")
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(a.appearsOn, key = { it.id }) { Card(it.tile()) }
                    }
                }
            }
            item("end") { BottomSpacer() }
        }
        TopBar(a?.artist?.name ?: "", color, collapsed, backdrop)
    }
}

/** "3 albums"; null for none. */
private fun count(n: Int, what: String) = if (n <= 0) null else "$n $what${if (n == 1) "" else "s"}"

@Composable
private fun SeeMore(open: Boolean, onClick: () -> Unit) = TextButton(onClick, Modifier.padding(start = 6.dp)) {
    Text(if (open) "Show less" else "See more", style = MaterialTheme.typography.labelLarge, color = Sub)
}
