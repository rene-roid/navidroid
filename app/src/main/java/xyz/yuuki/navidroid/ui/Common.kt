package xyz.yuuki.navidroid.ui

import android.content.Context
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.CacheOnly
import xyz.yuuki.navidroid.Kind
import xyz.yuuki.navidroid.Mix
import xyz.yuuki.navidroid.Playlist
import xyz.yuuki.navidroid.Song
import xyz.yuuki.navidroid.Tile

// ---- navigation -----------------------------------------------------------------------------

sealed interface Route {
    data object Home : Route
    data object Search : Route
    data object Library : Route
    data object Settings : Route
    data object Downloads : Route
    data object Appearance : Route
    data object HiddenPlaylists : Route
    data object SongCache : Route
    data object ImageCache : Route
    data object SavedInfo : Route
    data class Open(val kind: Kind, val id: String) : Route
    data class More(val section: String) : Route
}

class Nav {
    val stack = androidx.compose.runtime.mutableStateListOf<Route>(Route.Home)

    /** The colour a page wants behind it; in landscape the shell paints it under the rail and the player card too. */
    val tints = mutableStateMapOf<Route, Color>()

    /** The cover a page is about, so the aurora can take its colours. */
    val arts = mutableStateMapOf<Route, String>()

    fun go(r: Route) = stack.add(r)

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** Bumped when the tab the user is already on gets tapped again. */
    var retap by androidx.compose.runtime.mutableIntStateOf(0)

    fun tab(r: Route) {
        if (stack.toList() == listOf(r)) retap++
        stack.clear()
        stack.add(r)
    }

    fun open(t: Tile) = go(Route.Open(t.kind, t.id))
}

val LocalNav = staticCompositionLocalOf<Nav> { error("no nav") }
val LocalPlayer = staticCompositionLocalOf<PlayerState?> { null }

// ---- data loading ---------------------------------------------------------------------------

private val uiCache = HashMap<String, Any?>()

class Load<T>(val data: T?, val error: String?, val loading: Boolean, val refresh: () -> Unit)

/** Shows the last result for [key] instantly, from memory or else from disk, then refreshes it. Makes back navigation free. */
@Suppress("UNCHECKED_CAST")
@Composable
fun <T> rememberLoad(key: String, loader: suspend () -> T): Load<T> {
    var data by remember(key) { mutableStateOf(uiCache[key] as T?) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    var loading by remember(key) { mutableStateOf(true) }
    var tick by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key, tick) {
        loading = true
        // Nothing in memory yet: open from the server's last answers on disk while the fresh ones load.
        if (data == null) try {
            data = withContext(CacheOnly()) { loader() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        try {
            val v = loader()
            uiCache[key] = v
            data = v
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Couldn't reach the server"
        }
        loading = false
    }
    return Load(data, error, loading) { tick++ }
}

@Suppress("UNCHECKED_CAST")
fun <T> cachedUi(key: String) = uiCache[key] as T?

// ---- likes ----------------------------------------------------------------------------------

/** Starred songs, albums and artists (on the server) and saved playlists (on the phone). */
object Likes {
    private val overrides = mutableStateMapOf<String, Boolean>()
    var playlists by mutableStateOf(App.it.prefs.savedPlaylists)
        private set

    fun isLiked(s: Song) = isLiked(s.id, s.starred)
    fun isLiked(id: String, starred: Boolean) = overrides[id] ?: starred

    fun toggle(s: Song) = toggle(s.id, s.starred)

    /** [param] as in [xyz.yuuki.navidroid.Api.setStarred]. */
    fun toggle(id: String, starred: Boolean, param: String = "id") {
        val on = !isLiked(id, starred)
        overrides[id] = on
        App.it.scope.launch {
            runCatching { App.it.api.setStarred(id, on, param) }.onFailure { withContext(Dispatchers.Main) { overrides[id] = !on } }
        }
    }

    fun togglePlaylist(id: String) {
        playlists = if (id in playlists) playlists - id else playlists + id
        App.it.prefs.savedPlaylists = playlists
    }
}

@Composable
fun HeartButton(liked: Boolean, size: Dp = 28.dp, onClick: () -> Unit) = IconButton(onClick) {
    if (liked) LikedHeart(Modifier.size(size), "Remove from Your Library")
    else Icon(Icons.Rounded.FavoriteBorder, "Save to Your Library", Modifier.size(size), tint = Sub)
}

/** Accent-filled with a white rim, so it stands out on any cover-tinted background, even one close to the accent. */
@Composable
fun LikedHeart(modifier: Modifier, description: String?) = Box(modifier) {
    Icon(Icons.Rounded.Favorite, description, Modifier.matchParentSize(), tint = Color.White)
    Icon(Icons.Rounded.Favorite, null, Modifier.matchParentSize().scale(.82f), tint = Accent)
}

/** Playlists hidden from home and the library, on this phone only; Settings lists them to bring back. */
object Hidden {
    var playlists by mutableStateOf(App.it.prefs.hiddenPlaylists)
        private set

    fun set(ids: Collection<String>, hidden: Boolean) {
        playlists = if (hidden) playlists + ids else playlists - ids.toSet()
        App.it.prefs.hiddenPlaylists = playlists
    }

    fun shows(t: Tile) = t.kind != Kind.Playlist || t.id !in playlists
}

// ---- selection ------------------------------------------------------------------------------

/** Songs, or playlists, albums and artists, picked by pressing and holding; [SelectionBar] acts on them all. Never both at once. */
object Selection {
    val items = mutableStateListOf<Any>()
    val active get() = items.isNotEmpty()

    private fun key(x: Any) = if (x is Song) "song:${x.id}" else (x as Tile).run { "$kind:$id" }

    fun has(x: Any) = items.any { key(it) == key(x) }

    fun toggle(x: Any) {
        if (items.firstOrNull()?.let { it::class != x::class } == true) items.clear()
        if (!items.removeAll { key(it) == key(x) }) items += x
    }

    fun clear() = items.clear()
}

/** Tap does [onClick], or while picking adds or drops [item]; press and hold starts picking. */
@Composable
fun Modifier.pickable(item: Any, onClick: () -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    return combinedClickable(
        onClick = { if (Selection.active) Selection.toggle(item) else onClick() },
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            Selection.toggle(item)
        },
    ).then(if (Selection.has(item)) Modifier.background(Accent.copy(alpha = .25f)) else Modifier)
}

private suspend fun songsOf(t: Tile) = with(App.it.api) { if (t.kind == Kind.Artist) artistSongs(artist(t.id)) else collection(t.kind, t.id).songs }

/** What can be done with everything picked; floats at the bottom of the page while picking. */
@Composable
fun SelectionBar(modifier: Modifier = Modifier) {
    val player = LocalPlayer.current
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf<List<Song>?>(null) }
    adding?.let { AddToPlaylist(it) { adding = null } }
    val items = Selection.items.toList()
    if (items.isEmpty()) return
    val songs = items.filterIsInstance<Song>()
    val tiles = items.filterIsInstance<Tile>()
    // Playlists, albums and artists are fetched only once an action needs their songs.
    fun withSongs(action: (List<Song>) -> Unit) {
        Selection.clear()
        scope.launch {
            val all = songs + coroutineScope { tiles.map { t -> async { runCatching { songsOf(t) }.getOrDefault(emptyList()) } }.awaitAll().flatten() }
            if (all.isNotEmpty()) action(all)
        }
    }
    Column(modifier.padding(horizontal = 8.dp).fillMaxWidth().background(Bg.copy(alpha = .85f), RoundedCornerShape(22.dp)).glass(RoundedCornerShape(22.dp)).padding(bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(Selection::clear) { Icon(Icons.Rounded.Close, "Cancel") }
            Text("${items.size} selected", style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            @Composable
            fun action(label: String, icon: ImageVector, onClick: () -> Unit) = Column(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, null)
                val style = MaterialTheme.typography.labelSmall
                // Shrinks rather than cut off with large fonts and six actions.
                BasicText(label, Modifier.padding(top = 2.dp), style.copy(color = Color.White), maxLines = 1, autoSize = TextAutoSize.StepBased(8.sp, style.fontSize))
            }
            action("Play", Icons.Rounded.PlayArrow) { withSongs { player?.play(it, from = tiles.singleOrNull()?.title) } }
            action("Next", Icons.AutoMirrored.Rounded.PlaylistPlay) { withSongs { player?.playNext(it) } }
            action("Queue", Icons.AutoMirrored.Rounded.QueueMusic) { withSongs { player?.enqueue(it) } }
            action("Playlist", Icons.AutoMirrored.Rounded.PlaylistAdd) { withSongs { adding = it } }
            if (tiles.isEmpty()) {
                val all = songs.all(Likes::isLiked)
                action(if (all) "Unlike" else "Like", if (all) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder) {
                    songs.filter { Likes.isLiked(it) == all }.forEach(Likes::toggle)
                    Selection.clear()
                }
            }
            val keepable = tiles.filter { it.kind != Kind.Artist }
            if (keepable.isNotEmpty()) action("Download", Icons.Rounded.DownloadForOffline) {
                Selection.clear()
                scope.launch {
                    val cs = keepable.mapNotNull { t -> runCatching { App.it.api.collection(t.kind, t.id) }.getOrNull() }
                    App.it.offline.pin(*cs.toTypedArray())
                }
            }
            if (tiles.isNotEmpty() && tiles.all { it.kind == Kind.Playlist }) action("Hide", Icons.Rounded.VisibilityOff) {
                Hidden.set(tiles.map { it.id }, true)
                Selection.clear()
            }
        }
    }
}

// ---- images ---------------------------------------------------------------------------------

fun coverRequest(ctx: Context, id: String, size: Int): ImageRequest.Builder {
    val file = App.it.offline.coverFile(id).takeIf { it.exists() }
    return ImageRequest.Builder(ctx)
        .data(file ?: App.it.api.coverUrl(id, size))
        // Keyed by id, not URL: the URL changes with the endpoint and the auth salt.
        .memoryCacheKey("$id@$size")
        .diskCacheKey("$id@$size")
}

@Composable
fun Cover(
    id: String?, size: Int, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp), alpha: Float = 1f, colorFilter: ColorFilter? = null,
    onError: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val req = remember(id, size) { id?.let { coverRequest(ctx, it, size).build() } }
    AsyncImage(
        model = req, contentDescription = null, contentScale = ContentScale.Crop, alpha = alpha, colorFilter = colorFilter,
        onError = onError?.let { { _ -> it() } },
        modifier = modifier.clip(shape).background(Elevated),
    )
}

private val colors = LruCache<String, Color>(300)

/** A dark, saturated colour from the cover, for headers and the player background. */
@Composable
fun rememberCoverColor(id: String?, fallback: Color = Color(0xFF404040)): Color {
    val ctx = LocalContext.current
    var c by remember(id) { mutableStateOf(id?.let { colors[it] } ?: fallback) }
    LaunchedEffect(id) {
        if (id == null || colors[id] != null) return@LaunchedEffect
        val p = coverPalette(ctx, id) ?: return@LaunchedEffect
        val sw = p.vibrantSwatch ?: p.darkVibrantSwatch ?: p.dominantSwatch ?: return@LaunchedEffect
        var color = Color(sw.rgb)
        while (color.luminance() > 0.22f) color = lerp(color, Color.Black, 0.15f)
        colors.put(id, color)
        c = color
    }
    return animateColorAsState(c, tween(400), label = "cover").value
}

suspend fun coverPalette(ctx: Context, id: String): Palette? {
    val res = ctx.imageLoader.execute(coverRequest(ctx, id, 300).allowHardware(false).memoryCacheKey("pal:$id").build())
    val bmp = (res as? SuccessResult)?.image?.toBitmap() ?: return null
    return withContext(Dispatchers.Default) { Palette.from(bmp).maximumColorCount(16).generate() }
}

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

private fun darken(c: Color, by: Float = 0.55f) = Color(c.red * (1 - by), c.green * (1 - by), c.blue * (1 - by))

/** Cover art for a NaviBeat/Explo mix, drawn like the web UI: photo with a colour band, or a gradient. */
@Composable
fun MixCover(mix: Mix, coverArt: String?, modifier: Modifier = Modifier) {
    val color = Color(mix.color)
    val art by produceState(
        initialValue = if (mix.photo && mix.subject != null && (mix.kind == "dailymix" || mix.kind == "artistradio")) null else coverArt,
        mix.subject,
    ) {
        if (value == null && mix.subject != null) value = runCatching { App.it.api.artistCover(mix.subject) }.getOrNull() ?: coverArt
    }
    BoxWithConstraints(modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Elevated)) {
        val w = maxWidth
        val sp = with(LocalDensity.current) { { f: Float -> (w * f).toSp() } }
        val shadow = Shadow(Color(0x40000000), androidx.compose.ui.geometry.Offset(0f, 2f), 12f)
        if (mix.photo) {
            Cover(art, 300, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .25f), .22f to Color.Transparent,
                        .34f to color.copy(alpha = 0f), .42f to color.copy(alpha = .04f), .49f to color.copy(alpha = .12f),
                        .56f to color.copy(alpha = .25f), .62f to color.copy(alpha = .42f), .67f to color.copy(alpha = .6f),
                        .72f to color.copy(alpha = .76f), .77f to color.copy(alpha = .89f), .82f to color.copy(alpha = .97f),
                        .86f to color, 1f to color,
                    ),
                ),
            )
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().fillMaxHeight(.28f).padding(horizontal = w * .07f), contentAlignment = Alignment.CenterStart) {
                Text(
                    mix.title, fontSize = sp(.10f), lineHeight = sp(.105f), fontWeight = FontWeight.W800, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium.copy(shadow = shadow),
                )
            }
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(color, darken(color)))))
            Cover(art, 300, Modifier.fillMaxSize(), RoundedCornerShape(0.dp), alpha = .35f, colorFilter = grayscale)
            Text(
                mix.title, fontSize = sp(.13f), lineHeight = sp(.135f), fontWeight = FontWeight.W800, maxLines = 2,
                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium.copy(shadow = shadow),
                modifier = Modifier.align(Alignment.BottomStart).padding(w * .07f),
            )
        }
        Box(Modifier.offset(w * .06f, w * .06f).size(w * .09f).clip(CircleShape).background(Color.White).padding(w * .09f * .28f)) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(color))
        }
    }
}

@Composable
fun LikedCover(modifier: Modifier = Modifier) = BoxWithConstraints(
    modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF2EE6C5), Color(0xFF48B4FF), Color(0xFF8E6CFF), Color(0xFFFF6FC1)))),
    contentAlignment = Alignment.Center,
) {
    Icon(Icons.Rounded.Favorite, null, Modifier.size(maxWidth * .38f), tint = Color.White)
}

@Composable
fun GenreCover(name: String, modifier: Modifier = Modifier) = BoxWithConstraints(
    modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Color(xyz.yuuki.navidroid.paletteColor(name))),
) {
    Text(name, Modifier.padding(maxWidth * .08f), fontWeight = FontWeight.W800, fontSize = with(LocalDensity.current) { (maxWidth * .14f).toSp() }, maxLines = 2)
}

/** The right artwork for any tile: mix art, Liked Songs, an artist (round) or plain cover. */
@Composable
fun TileCover(kind: Kind, id: String, coverArt: String?, mix: Mix?, modifier: Modifier = Modifier, size: Int = 300) = when {
    mix != null -> MixCover(mix, coverArt, modifier)
    kind == Kind.Liked -> LikedCover(modifier)
    kind == Kind.Genre && coverArt == null -> GenreCover(id, modifier)
    kind == Kind.Artist -> Cover(coverArt, size, modifier.aspectRatio(1f), CircleShape)
    else -> Cover(coverArt, size, modifier.aspectRatio(1f))
}

// ---- building blocks ------------------------------------------------------------------------

/** Landscape, or a tablet in portrait: the shell has a rail and screens split into two sides instead of stretching the phone layout. */
@Composable
fun isWide() = LocalConfiguration.current.run { screenWidthDp > screenHeightDp || screenWidthDp >= 600 }

/** At least 600 dp both ways: room for labels on the rail and, in landscape, the library beside the page. */
@Composable
fun isTablet() = LocalConfiguration.current.run { minOf(screenWidthDp, screenHeightDp) >= 600 }

/** Room at the end of every list for the mini player, the tab bar, the fade above them and the system navigation bar. */
@Composable
fun BottomSpacer() = Spacer(Modifier.navigationBarsPadding().height(200.dp))

@Composable
fun Card(t: Tile, width: Dp = 150.dp, modifier: Modifier = Modifier) {
    val nav = LocalNav.current
    Column(modifier.width(width).clip(RoundedCornerShape(14.dp)).pickable(t) { nav.open(t) }.padding(6.dp)) {
        TileCover(t.kind, t.id, t.coverArt, t.mix, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(t.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
    }
}

@Composable
fun SectionHeader(title: String, onMore: (() -> Unit)? = null) = Row(
    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (onMore != null) TextButton(onMore) { Text("Show all", style = MaterialTheme.typography.labelLarge, color = Sub) }
}

@Composable
fun PlayButton(playing: Boolean, size: Dp = 56.dp, onClick: () -> Unit) = Box(
    Modifier.size(size).shadow(16.dp, CircleShape, ambientColor = Accent, spotColor = Accent).clip(CircleShape).background(AccentGlow).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", Modifier.size(size * .55f), tint = OnAccent)
}

@Composable
fun FavoriteMark() = LikedHeart(Modifier.padding(end = 4.dp).size(14.dp), "Saved")

@Composable
fun DownloadedMark() = Icon(Icons.Rounded.ArrowCircleDown, "Downloaded", Modifier.padding(end = 4.dp).size(14.dp), tint = Accent)

@Composable
fun TrackRow(song: Song, onClick: () -> Unit, number: Int? = null, showArt: Boolean = true, inset: androidx.compose.ui.unit.Dp = 16.dp) {
    val player = LocalPlayer.current
    val current = player?.song?.id == song.id
    val states by App.it.offline.states.collectAsStateWithLifecycle()
    Row(
        Modifier.fillMaxWidth().pickable(song, onClick).padding(start = inset, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) Text("$number", Modifier.width(28.dp), color = if (current) Accent else Sub, style = MaterialTheme.typography.bodyMedium)
        if (showArt) {
            Cover(song.coverArt, 120, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, color = if (current) Accent else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (states[song.id] == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) DownloadedMark()
                Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (Likes.isLiked(song)) LikedHeart(Modifier.size(16.dp), "Liked")
        SongMenu(song)
    }
}

@Composable
fun SongMenu(song: Song, tint: Color = Sub) {
    val nav = LocalNav.current
    val player = LocalPlayer.current
    var open by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf(false) }
    val goToArtist = rememberGoToArtist()
    Box {
        IconButton({ open = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = tint) }
        DropdownMenu(open, { open = false }, containerColor = Elevated) {
            @Composable
            fun item(text: String, icon: ImageVector, action: () -> Unit) =
                DropdownMenuItem({ Text(text) }, { open = false; action() }, leadingIcon = { Icon(icon, null) })

            val liked = Likes.isLiked(song)
            item(if (liked) "Remove from Liked Songs" else "Add to Liked Songs", if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder) { Likes.toggle(song) }
            item("Play next", Icons.AutoMirrored.Rounded.PlaylistPlay) { player?.playNext(listOf(song)) }
            item("Add to queue", Icons.AutoMirrored.Rounded.QueueMusic) { player?.enqueue(listOf(song)) }
            item("Add to playlist", Icons.AutoMirrored.Rounded.PlaylistAdd) { pick = true }
            song.albumId?.let { id -> item("Go to album", Icons.Rounded.Album) { nav.go(Route.Open(Kind.Album, id)) } }
            if (song.artistId != null) item("Go to artist", Icons.Rounded.Person) { goToArtist(song) }
        }
    }
    if (pick) AddToPlaylist(listOf(song)) { pick = false }
}

/** Opens a song's artist, or asks which one when it has several. [before] runs right before leaving (closes the player). */
@Composable
fun rememberGoToArtist(before: () -> Unit = {}): (Song) -> Unit {
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var choices by remember { mutableStateOf<List<xyz.yuuki.navidroid.Artist>>(emptyList()) }
    fun go(id: String) {
        choices = emptyList()
        before()
        nav.go(Route.Open(Kind.Artist, id))
    }
    if (choices.isNotEmpty()) AlertDialog(
        onDismissRequest = { choices = emptyList() },
        containerColor = Elevated,
        title = { Text("Choose artist") },
        text = {
            Column {
                choices.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { go(a.id) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Cover(a.coverArt, 120, Modifier.size(44.dp), CircleShape)
                        Text(a.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton({ choices = emptyList() }) { Text("Cancel", color = Sub) } },
    )
    return { song ->
        scope.launch {
            val all = runCatching { App.it.api.songArtists(song) }.getOrNull().orEmpty()
            if (all.size > 1) choices = all else (all.firstOrNull()?.id ?: song.artistId)?.let(::go)
        }
    }
}

@Composable
fun AddToPlaylist(songs: List<Song>, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val lists = rememberLoad("playlists") { App.it.api.playlists() }
    val user = App.it.prefs.username
    var naming by remember { mutableStateOf(false) }
    if (naming) return NewPlaylist(songs) { onDone() }
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Elevated,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn(Modifier.height(360.dp)) {
                item("new") {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { naming = true }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(44.dp).background(Surface, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null) }
                        Text("New playlist")
                    }
                }
                items(lists.data.orEmpty().filter { it.mix == null && it.owner == user && it.id !in Hidden.playlists }, key = Playlist::id) { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable {
                            scope.launch {
                                runCatching { App.it.api.addToPlaylist(p.id, songs.map { it.id }) }
                                onDone()
                            }
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Cover(p.coverArt, 120, Modifier.size(44.dp))
                        Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDone) { Text("Cancel", color = Sub) } },
    )
}

/** Names and creates a playlist holding [songs]; [onDone] gets its id, or null if cancelled or it failed. */
@Composable
fun NewPlaylist(songs: List<Song> = emptyList(), onDone: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun create() {
        if (name.isBlank() || busy) return
        busy = true
        scope.launch {
            runCatching { App.it.api.createPlaylist(name.trim(), songs.map { it.id }) }
                .onSuccess { onDone(it) }
                .onFailure { error = it.message ?: "Couldn't create the playlist"; busy = false }
        }
    }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        containerColor = Elevated,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                name, { name = it; error = null }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true, colors = fieldColors,
                placeholder = { Text("Playlist name") }, isError = error != null, supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { create() }),
            )
        },
        confirmButton = { TextButton(::create, enabled = name.isNotBlank() && !busy) { Text("Create", color = Accent) } },
        dismissButton = { TextButton({ onDone(null) }) { Text("Cancel", color = Sub) } },
    )
}
