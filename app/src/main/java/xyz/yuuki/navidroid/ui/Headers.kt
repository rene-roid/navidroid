package xyz.yuuki.navidroid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.yuuki.navidroid.Collection
import xyz.yuuki.navidroid.Mix
import xyz.yuuki.navidroid.Song
import xyz.yuuki.navidroid.paletteColor
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot

// Animated headers for collections and artists, one idea per kind: a genre is a hand of cards, an album a record
// sliding out of its sleeve, a playlist its cover before a wall of its albums, a mix a cover flow, an artist their releases rolling past.

/**
 * One cover per album, leaving out [except]'s album. Songs each carry their own cover id (`mf-…`) even within one
 * album, and an album's own is `al-<albumId>_…`, so they are told apart by album.
 *
 * A folder of loose songs is often tagged as one album (every song in "2021 nightcore" is on the album "2021 nightcore")
 * while each file has its own art, so with too few albums to go round the songs' own covers make up the rest.
 */
fun albumCovers(songs: List<Song>, except: String? = null): List<String> {
    val own = songs.filter { it.albumId != null && except?.contains(it.albumId) != true }
    val perAlbum = own.distinctBy { it.albumId }.mapNotNull { it.coverArt }
    return if (perAlbum.size >= 6) perAlbum else (perAlbum + own.mapNotNull { it.coverArt }).distinct()
}

/** [covers] minus those that failed to load, so a blank tile drops out and the next cover takes its place. */
@Composable
private fun rememberLoadable(covers: List<String>): Pair<List<String>, (String) -> Unit> {
    val broken = remember { mutableStateListOf<String>() }
    return covers.filter { it !in broken } to { id: String -> broken += id }
}

/** Seconds since this was first shown, ticking every frame. Read it in draw or layer blocks so only drawing repeats. */
@Composable
private fun rememberSeconds(): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t.floatValue = (it - start) / 1e9f }
    }
    return t
}

// ---- genre: a hand of cards ----------------------------------------------------------------------

/** Tinted from the genre's cover so card and header match; the palette colour shows until it loads. */
@Composable
fun rememberGenreTint(name: String, art: String?): Color {
    // The cover colour is darkened for headers, so lift it back to one even, vivid brightness.
    val cover = rememberCoverColor(art, Color(paletteColor(name)))
    return remember(cover) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(cover.toArgb(), it) }
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceAtLeast(0.45f), 0.6f)))
    }
}

/** The card's cover first, then one per other album. */
fun genreHand(c: Collection?) = listOfNotNull(c?.coverArt) + albumCovers(c?.songs.orEmpty(), c?.coverArt)

/** A genre's header: up to three of its covers fanned out like a hand of cards, over bands of its name sliding past. */
@Composable
fun GenreHeader(name: String, covers: List<String>, modifier: Modifier) = BoxWithConstraints(modifier) {
    val s = maxWidth
    val bg = rememberGenreTint(name, covers.firstOrNull())
    val measurer = rememberTextMeasurer()
    val band = remember(name, s) {
        measurer.measure(name.uppercase() + "  ", TextStyle(fontSize = (s.value * 0.2f).sp, fontWeight = FontWeight.Black, lineHeight = (s.value * 0.2f).sp))
    }
    val slide = rememberInfiniteTransition("bands").animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing)), "slide")
    val stroke = with(LocalDensity.current) { Stroke(1.5.dp.toPx()) }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(lerp(bg, Color.White, 0.15f), lerp(bg, Color.Black, 0.6f))))
        // Rows alternate filled and outlined and slide opposite ways.
        val w = band.size.width.toFloat()
        val h = band.size.height * 0.92f
        for (row in 0..(size.height / h).toInt()) {
            val even = row % 2 == 0
            var x = -w * (if (even) slide.value else 1f - slide.value) - (row % 3) * w / 3
            while (x < size.width) {
                drawText(band, Color.White, Offset(x, row * h - h * 0.15f), alpha = if (even) 0.1f else 0.28f, drawStyle = if (even) Fill else stroke)
                x += w
            }
        }
    }
    // Fanned out from a neat stack when the covers arrive. The first cover is the card's, so it goes on top.
    val fan = remember { Animatable(0f) }
    LaunchedEffect(covers.size) { if (covers.isNotEmpty()) fan.animateTo(1f, spring(0.55f, Spring.StiffnessVeryLow)) }
    val (ok, onError) = rememberLoadable(covers)
    val hand = ok.take(3)
    val spots = when (hand.size) {
        1 -> listOf(0f to -6f)
        2 -> listOf(-0.1f to -8f, 0.1f to 8f)
        else -> listOf(0f to -3f, -0.22f to -16f, 0.22f to 13f)
    }
    hand.zip(spots).asReversed().forEach { (id, spot) ->
        val (dx, angle) = spot
        Cover(
            id, 300, onError = { onError(id) }, modifier = Modifier.align(Alignment.Center).graphicsLayer {
                translationX = s.toPx() * dx * fan.value
                translationY = s.toPx() * (0.06f + abs(dx) * 0.2f * fan.value)
                rotationZ = angle * fan.value
            }.size(s * 0.46f).shadow(16.dp, RoundedCornerShape(4.dp)),
        )
    }
}

// ---- album: a record out of its sleeve -------------------------------------------------------------

/**
 * An album's header: its sleeve, with the record sliding out from behind once the art is in. The record's label is the
 * cover too, and turns at 33⅓ rpm while the album plays. It is [sleeve] tall and 1.4 sleeves wide.
 */
@Composable
fun AlbumHeader(art: String?, spinning: Boolean, sleeve: Dp, modifier: Modifier = Modifier) {
    val out = remember { Animatable(0f) }
    LaunchedEffect(art != null) {
        if (art == null) return@LaunchedEffect
        delay(200)
        out.animateTo(1f, spring(0.65f, Spring.StiffnessVeryLow))
    }
    // Keeps its angle when paused, like a real turntable.
    val angle = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) withFrameNanos {
            angle.floatValue = (angle.floatValue + (it - last) / 1e9f * 200f) % 360f
            last = it
        }
    }
    val d = sleeve * 0.94f
    Box(modifier.width(sleeve * 1.4f).height(sleeve)) {
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = (sleeve - d) / 2).size(d)
                .graphicsLayer { translationX = out.value * sleeve.toPx() * 0.42f }.shadow(10.dp, CircleShape),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(Color(0xFF1C1C1C), Color(0xFF080808)), center, r))
                // Fine grooves, darker gaps between tracks, and a lighter lip at the edge.
                var g = r * 0.42f
                while (g < r * 0.97f) {
                    drawCircle(Color.White.copy(alpha = 0.05f), g, style = Stroke(0.6.dp.toPx()))
                    g += 1.8.dp.toPx()
                }
                for (gap in floatArrayOf(0.55f, 0.68f, 0.83f)) drawCircle(Color.Black, r * gap, style = Stroke(2.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.12f), r - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
            Cover(art, 300, Modifier.align(Alignment.Center).size(d * 0.38f).graphicsLayer { rotationZ = angle.floatValue }, CircleShape)
            // Light catching the grooves stays put while the record turns under it.
            Canvas(Modifier.fillMaxSize()) {
                val glint = Color.White.copy(alpha = 0.13f)
                drawCircle(Brush.sweepGradient(listOf(Color.Transparent, glint, Color.Transparent, Color.Transparent, glint, Color.Transparent, Color.Transparent)))
                drawCircle(Color(0xFF121212), 2.5.dp.toPx())
            }
        }
        Cover(art, 600, Modifier.size(sleeve).shadow(20.dp, RoundedCornerShape(4.dp)))
    }
}

// ---- playlist: a wall of covers --------------------------------------------------------------------

/** A playlist's backdrop: its albums' covers on a tilted wall whose columns slide past each other, up and down. */
@Composable
fun PlaylistWall(covers: List<String>, modifier: Modifier) = BoxWithConstraints(modifier.clipToBounds()) {
    // Two dozen are plenty to look endless, and loading every cover of a long playlist starves the track list's art.
    val (loadable, onError) = rememberLoadable(covers)
    val ok = loadable.take(24)
    val t = rememberSeconds()
    val tint = rememberCoverColor(ok.firstOrNull())
    Canvas(Modifier.fillMaxSize()) { drawRect(Brush.verticalGradient(listOf(tint, lerp(tint, Color.Black, 0.5f)))) }
    if (ok.isEmpty()) return@BoxWithConstraints
    // The tilted grid has to reach past the corners, so it is as wide and tall as the diagonal.
    val diag = hypot(maxWidth.value, maxHeight.value).dp
    val tile = (maxWidth / 3.4f).coerceAtMost(130.dp)
    val gap = 6.dp
    val step = tile + gap
    val cols = ceil(diag / step).toInt() + 1
    // Each column repeats its run of covers until one run is taller than the wall, then twice, and loops by one run.
    val run = maxOf(ok.size, ceil(diag / step).toInt())
    Row(Modifier.align(Alignment.Center).requiredSize(diag).graphicsLayer { rotationZ = -14f }) {
        repeat(cols) { col ->
            val up = col % 2 == 0
            val speed = if (up) 9f else 6.5f
            Column(
                Modifier.width(step).wrapContentHeight(Alignment.Top, unbounded = true).graphicsLayer {
                    val loop = run * step.toPx()
                    val y = (t.value * speed.dp.toPx() + col * step.toPx() * 0.37f) % loop
                    translationY = if (up) -y else y - loop
                },
            ) {
                repeat(run * 2) { row ->
                    val id = ok[(row + col * 3) % ok.size]
                    Cover(id, 300, Modifier.padding(end = gap, bottom = gap).size(tile), RoundedCornerShape(4.dp), onError = { onError(id) })
                }
            }
        }
    }
}

/** A playlist's own cover in front of its wall: square, lifted off it by a shadow and a faint rim. */
@Composable
fun PlaylistFront(art: String?, modifier: Modifier) =
    Cover(art, 600, modifier.shadow(24.dp, RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp)), RoundedCornerShape(6.dp))

// ---- mix: a cover flow ----------------------------------------------------------------------------

/**
 * A mix's header: its own tile, as on Home, leading its albums in a cover flow that steps along by itself, reflected on
 * a dark floor in the mix's colour.
 */
@Composable
fun MixFlow(mix: Mix, coverArt: String?, covers: List<String>, modifier: Modifier) = BoxWithConstraints(modifier.clipToBounds()) {
    val color = Color(mix.color)
    val (loadable, onError) = rememberLoadable(covers)
    // null is the mix's own tile.
    val ok = listOf(null) + loadable.take(9)
    val n = ok.size
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(0f to lerp(color, Color.White, 0.08f), 0.55f to lerp(color, Color.Black, 0.35f), 0.7f to lerp(color, Color.Black, 0.85f), 1f to Bg))
        drawCircle(Brush.radialGradient(listOf(lerp(color, Color.White, 0.35f).copy(alpha = 0.55f), Color.Transparent), Offset(center.x, size.height * 0.45f), size.minDimension * 0.5f), size.minDimension * 0.5f, Offset(center.x, size.height * 0.45f))
    }
    val side = minOf(maxWidth * 0.42f, maxHeight * 0.5f)
    // Position along the flow in covers; whole numbers are a cover dead centre.
    val pos = remember { Animatable(0f) }
    // Bumped by every swipe, which stops the spin or the stepping and starts the stepping again after a pause.
    var touched by remember { mutableIntStateOf(0) }
    LaunchedEffect(n > 1, touched) {
        if (n <= 1) return@LaunchedEffect
        // On arrival it spins through at least a dozen covers like a wheel, slowing to land on the mix's own tile.
        if (touched == 0) pos.animateTo(n * ceil(12f / n), tween(2600, easing = CubicBezierEasing(0.1f, 0.75f, 0.1f, 1f)))
        else delay(3000)
        while (true) {
            delay(2800)
            pos.animateTo(kotlin.math.round(pos.value) + 1f, spring(0.85f, Spring.StiffnessVeryLow))
        }
    }
    val scope = rememberCoroutineScope()
    val decay = rememberSplineBasedDecay<Float>()
    val step = with(LocalDensity.current) { side.toPx() * 0.78f }
    val swipe = Modifier.pointerInput(n) {
        if (n <= 1) return@pointerInput
        val tracker = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = {
                tracker.resetTracking()
                touched++
            },
            onHorizontalDrag = { change, dx ->
                tracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
                scope.launch { pos.snapTo(pos.value - dx / step) }
            },
            onDragEnd = {
                // Flung, it coasts on and snaps to whichever cover it slows down at.
                val v = -tracker.calculateVelocity().x / step
                scope.launch { pos.animateTo(kotlin.math.round(decay.calculateTargetValue(pos.value, v)), spring(0.8f, Spring.StiffnessLow), v) }
            },
        )
    }
    Box(Modifier.fillMaxSize().then(swipe))
    // Each cover's place relative to the front one, wrapping round so the flow never runs out; drawn back to front.
    val places = List(n) { i -> i to ((i - pos.value) % n + n) % n }.map { (i, d) -> i to if (d > n / 2f) d - n else d }
    places.filter { abs(it.second) < 2.6f }.sortedByDescending { abs(it.second) }.forEach { (i, d) ->
        val near = d.coerceIn(-1f, 1f)
        val far = abs(d) - abs(near)
        Column(
            Modifier.align(Alignment.Center).offset(y = side * 0.2f).graphicsLayer {
                translationX = (near * 0.78f + kotlin.math.sign(d) * far * 0.32f) * side.toPx()
                rotationY = -near * 48f
                cameraDistance = 10 * density
                scaleX = 1 - 0.2f * abs(near)
                scaleY = scaleX
                alpha = (1 - (abs(d) - 2f) / 0.6f).coerceIn(0f, 1f)
            },
        ) {
            val id = ok[i]
            val card = @Composable { m: Modifier -> if (id == null) MixCover(mix, coverArt, m) else Cover(id, 600, m, onError = { onError(id) }) }
            card(Modifier.size(side).shadow(14.dp, RoundedCornerShape(4.dp)))
            // The floor's reflection: the cover flipped, fading out fast.
            Box(
                Modifier.padding(top = 3.dp).size(side, side * 0.4f).clipToBounds().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)), blendMode = BlendMode.DstIn)
                    },
            ) {
                card(Modifier.wrapContentHeight(Alignment.Top, unbounded = true).size(side).graphicsLayer { scaleY = -1f })
            }
        }
    }
}

// ---- artist: their releases rolling past ---------------------------------------------------------

/**
 * An artist's header: their photo in a ring over rows of their releases sliding past in alternate directions.
 * The photo falls back to [fallback] (their most played album) when the server has none.
 */
@Composable
fun ArtistHeader(photo: String?, fallback: String?, covers: List<String>, tint: Color, modifier: Modifier) = BoxWithConstraints(modifier.clipToBounds()) {
    val t = rememberSeconds()
    val (ok, onError) = rememberLoadable(covers)
    val rows = 3
    val gap = 6.dp
    val tile = (maxHeight - gap * (rows - 1)) / rows
    val step = tile + gap
    // Each row repeats its run of covers until one run is wider than the screen, then twice, and loops by one run.
    val run = maxOf(ok.size, ceil(maxWidth / step).toInt() + 1)
    if (ok.isNotEmpty()) Column(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap)) {
        repeat(rows) { row ->
            Row(
                Modifier.wrapContentWidth(Alignment.Start, unbounded = true).graphicsLayer {
                    val loop = run * step.toPx()
                    val x = (t.value * (if (row == 1) 11f else 8f).dp.toPx() + row * step.toPx() * 0.5f) % loop
                    translationX = if (row % 2 == 0) -x else x - loop
                },
            ) {
                repeat(run * 2) { i ->
                    val id = ok[(i + row * 2) % ok.size]
                    Cover(id, 300, Modifier.padding(end = gap).size(tile), RoundedCornerShape(4.dp), onError = { onError(id) })
                }
            }
        }
    }
    // Dim the rows in the artist's colour so the photo stands out.
    Canvas(Modifier.fillMaxSize()) { drawRect(Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.3f).copy(alpha = 0.65f), Color.Black.copy(alpha = 0.7f)))) }
    var src by remember(photo) { mutableStateOf(photo ?: fallback) }
    val ring = maxHeight * 0.42f
    Cover(
        src, 600,
        Modifier.align(Alignment.Center).offset(y = -maxHeight * 0.1f).size(ring).shadow(24.dp, CircleShape)
            .border(3.dp, lerp(tint, Color.White, 0.55f), CircleShape),
        CircleShape, onError = { if (src != fallback) src = fallback },
    )
}
