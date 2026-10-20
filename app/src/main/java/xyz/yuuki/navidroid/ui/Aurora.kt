package xyz.yuuki.navidroid.ui

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import androidx.palette.graphics.Palette
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.Levels
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// Runs once per pixel. Three curtains, each a wavy lower edge with light rising from it in thin vertical rays.
// The rays drift sideways and sway with the folds. level (loudness) makes the curtains taller, kick (a bass hit)
// lights up the rim along the edge.
private const val AURORA_AGSL = """
uniform float2 scale;
uniform float t;
uniform float level;
uniform float kick;
uniform float3 c0;
uniform float3 c1;
uniform float3 c2;
uniform float3 c3;

float hash(float2 p) { return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453); }

float rays(float x, float s) {
    return 0.55 + 0.2 * sin(x * 61.0 + s * 0.9) + 0.14 * sin(x * 97.0 - s * 1.3)
        + 0.1 * sin(x * 151.0 + s * 2.1) + 0.07 * sin(x * 233.0 - s * 2.9);
}

float3 curtain(float2 uv, float base, float h, float speed, float drift, float seed, float3 a, float3 b) {
    float s = t * speed;
    // Waves at unrelated frequencies, so the edge folds instead of sliding sideways.
    float edge = base + 0.045 * sin(uv.x * 2.3 + s + seed) + 0.025 * sin(uv.x * 5.1 - s * 1.4 + seed * 1.7)
        + 0.012 * sin(uv.x * 11.7 + s * 2.3 + seed * 0.6);
    float d = edge - uv.y;
    float x = uv.x + seed * 3.0 + s * drift + 0.5 * (edge - base);
    float ray = rays(x, s);
    // Two sines at different speeds make patches of rays that grow and brighten, then fade away.
    float pulse = 0.5 + 0.5 * sin(x * 13.0 + s * 5.0) * sin(x * 31.0 - s * 7.0 + 1.3);
    float rise = d > 0.0 ? exp(-d / (h * ray * (0.55 + 0.9 * pulse))) : exp(d / 0.012);
    // Faint ripples running up the rays.
    rise *= 0.88 + 0.12 * sin(d * 90.0 - s * 25.0 + x * 40.0);
    float rim = exp(-abs(d) / 0.009) * (0.45 + 0.8 * kick);
    return mix(a, b, clamp(d / (h * 1.6), 0.0, 1.0)) * (rise * ray * (0.65 + 0.85 * pulse) * 0.55 + rim * 0.5);
}

half4 main(float2 p) {
    float2 uv = p * scale;
    float lift = 0.75 + 0.6 * level + 0.1 * kick;
    float3 acc = curtain(uv, 0.30, 0.10 * lift, 0.23, 0.12, 0.0, c0, c1)
        + curtain(uv, 0.22, 0.08 * lift, 0.31, -0.09, 2.1, c1, c2)
        + curtain(uv, 0.37, 0.12 * lift, 0.17, 0.07, 4.7, c2, c3);
    acc *= (0.85 + 0.15 * sin(t * 0.3)) * (0.8 + 0.25 * level + 0.15 * kick);
    // Dither against banding in the dark gradients.
    acc = clamp(acc + (hash(p + fract(t)) - 0.5) / 255.0, 0.0, 1.0);
    // Premultiplied with alpha = brightness, so dark parts let the sky through.
    return half4(acc, max(acc.r, max(acc.g, acc.b)));
}
"""

private val Night = Color(0xFF0C1030)
private val defaults = listOf(Color(0xFF2EE6C5), Color(0xFF48B4FF), Color(0xFF8E6CFF), Color(0xFFFF6FC1))

// ponytail: fixed guess at the speaker's own delay; Bluetooth adds ~200 ms more. Tune by eye.
private const val OUTPUT_LATENCY_NS = 60_000_000L

private class Star(val x: Float, val y: Float, val depth: Int, val twinkle: Float, val phase: Float)

private val stars = Random(42).let { r -> List(150) { Star(r.nextFloat(), r.nextFloat(), r.nextInt(3), .5f + r.nextFloat() * 2f, r.nextFloat() * 6.28f) } }

private val auroraColors = LruCache<String, List<Color>>(50)

val auroraModes = listOf(0 to "Song, classic when idle", 1 to "Song, else the page", 2 to "Always classic")

/** Index into [auroraModes]; global so the setting applies everywhere at once. */
var auroraMode by mutableIntStateOf(App.it.prefs.auroraColors)

/** An on/off aurora setting, saved as it changes. */
class Toggle(private val key: String, val title: String, val subtitle: String) {
    private val state = mutableStateOf(App.it.prefs.flag(key))
    var on: Boolean
        get() = state.value
        set(v) {
            state.value = v
            App.it.prefs.flag(key, v)
        }
}

val auroraOn = Toggle("aurora", "Aurora background", "A night sky with northern lights behind the app")
val auroraMotion = Toggle("auroraMotion", "Movement", "Curtains fold and drift, stars twinkle")
val auroraMusic = Toggle("auroraMusic", "React to music", "Taller curtains when it's loud, a flash on the beat")
val auroraStars = Toggle("auroraStars", "Stars", "Drifting stars behind the curtains")
val auroraShootingStars = Toggle("auroraShooting", "Shooting stars", "One every ten seconds or so")

/** The settings that only matter while the aurora is on. */
val auroraDetails = listOf(auroraMotion, auroraMusic, auroraStars, auroraShootingStars)

private fun hsv(c: Color) = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toArgb(), it) }

/** The colour as light: full brightness, saturated, hue turned by [turn] degrees. */
private fun glow(c: Color, turn: Float = 0f) = hsv(c).let { Color.hsv((it[0] + turn + 360f) % 360f, it[1].coerceAtLeast(.55f), 1f) }

private fun spread(c: Color) = listOf(0f, 30f, 70f, 110f).map { glow(c, it) }

/** Up to four clearly different colours from the cover, topped up with neighbours of the first. */
private fun fromPalette(p: Palette): List<Color>? {
    val picks = mutableListOf<Color>()
    for (sw in listOfNotNull(p.vibrantSwatch, p.lightVibrantSwatch, p.darkVibrantSwatch, p.mutedSwatch, p.lightMutedSwatch, p.dominantSwatch)) {
        val h = hsv(Color(sw.rgb))
        if (h[1] > .2f && picks.none { abs(hsv(it)[0] - h[0]).let { d -> minOf(d, 360 - d) } < 25f }) picks += Color(sw.rgb)
    }
    if (picks.isEmpty()) return null
    return (picks.take(4).map { glow(it) } + spread(picks[0]).drop(1)).take(4)
}

/**
 * The night sky behind the app: drifting stars, a shooting star now and then, and an aurora that swells with the music.
 * Per [auroraMode] it's coloured by the cover [art], the [page]'s colour, or the classic teal to pink; the sky is a
 * dark shade of the same colours so the two never clash.
 */
@Composable
fun Aurora(art: String?, page: Color?, modifier: Modifier = Modifier) {
    SideEffect { Levels.listening = auroraOn.on && auroraMusic.on }
    // Off, it's the plain background the app had before: the page's colour fading into the dark.
    if (auroraOn.on) Sky(art, page, modifier) else Box(modifier.background(Brush.verticalGradient(0f to (page ?: Bg), .45f to Bg)))
}

@Composable
private fun Sky(art: String?, page: Color?, modifier: Modifier) {
    val ctx = LocalContext.current
    val source = art.takeIf { auroraMode != 2 }
    val fromArt by produceState<List<Color>?>(null, source) {
        if (source == null) value = null
        else value = auroraColors[source] ?: coverPalette(ctx, source)?.let(::fromPalette)?.also { auroraColors.put(source, it) }
    }
    val target = fromArt ?: page?.takeIf { auroraMode == 1 && hsv(it)[1] > .2f }?.let(::spread) ?: defaults
    val colors = target.map { animateColorAsState(it, tween(1200), label = "aurora").value }
    val sky by animateColorAsState(if (target == defaults) Night else Color.hsv(hsv(target[0])[0], .6f, .2f), tween(1200), label = "sky")

    val clock = remember { mutableFloatStateOf(0f) }
    val phase = remember { mutableFloatStateOf(0f) }
    val level = remember { mutableFloatStateOf(0f) }
    val kick = remember { mutableFloatStateOf(0f) }
    // With system animations off the sky is a still picture.
    val still = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val motion = auroraMotion.on
    val music = auroraMusic.on
    LaunchedEffect(still, motion, music) {
        if (still || !(motion || music)) return@LaunchedEffect
        val out = FloatArray(2)
        var last = withFrameNanos { it }
        while (true) withFrameNanos { now ->
            val dt = ((now - last) / 1e9f).coerceAtMost(.1f)
            last = now
            if (music) Levels.at(now - OUTPUT_LATENCY_NS, out) else out.fill(0f)
            // Quick to rise, slow to settle; a hit flashes and fades over ~150 ms.
            level.floatValue += (out[0] - level.floatValue) * (1 - exp(-dt / if (out[0] > level.floatValue) .06f else .4f))
            kick.floatValue = maxOf(out[1], kick.floatValue * exp(-dt / .15f))
            // Louder music also moves the curtains faster.
            if (motion) {
                phase.floatValue += dt * (1 + 1.2f * level.floatValue)
                clock.floatValue += dt
            }
        }
    }

    val showStars = auroraStars.on
    val shooting = auroraShootingStars.on
    Box(
        modifier.drawBehind {
            drawRect(Brush.verticalGradient(0f to sky, .45f to Bg))
            if (showStars) drawStars(clock.floatValue, kick.floatValue)
            if (shooting) drawShootingStar(clock.floatValue)
        },
    ) {
        if (Build.VERSION.SDK_INT >= 33) Curtains(colors, phase, level, kick)
    }
}

/**
 * The shader is costly per pixel, but the curtains only have fine detail across. So it renders the top half of the
 * screen at full width and a sixth of the height, stretched back up by the layer.
 */
@RequiresApi(33)
@Composable
private fun Curtains(colors: List<Color>, phase: FloatState, level: FloatState, kick: FloatState) {
    val shader = remember { RuntimeShader(AURORA_AGSL) }
    val brush = remember { ShaderBrush(shader) }
    Canvas(Modifier.fillMaxWidth().fillMaxHeight(.5f / 6).graphicsLayer { scaleY = 6f; transformOrigin = TransformOrigin(.5f, 0f) }) {
        // Units of the whole height, so the curtains sit at the same place on any screen.
        shader.setFloatUniform("scale", 1 / (size.height * 6), .5f / size.height)
        shader.setFloatUniform("t", phase.floatValue)
        shader.setFloatUniform("level", level.floatValue)
        shader.setFloatUniform("kick", kick.floatValue)
        colors.forEachIndexed { i, c -> shader.setFloatUniform("c$i", c.red, c.green, c.blue) }
        drawRect(brush)
    }
}

/** Three layers of stars; nearer ones are bigger, brighter and drift faster, which reads as depth. */
private fun DrawScope.drawStars(t: Float, kick: Float) {
    val r = 1.dp.toPx()
    for (s in stars) {
        val x = (s.x + t * .0015f * (s.depth + 1)) % 1f
        val a = (.25f + .25f * s.depth) * (.65f + .35f * sin(t * s.twinkle + s.phase)) * (1 + .25f * kick)
        drawCircle(Color.White.copy(alpha = a.coerceIn(0f, 1f)), r * (.5f + .45f * s.depth), Offset(x * size.width, s.y * size.height))
    }
}

/** One shooting star every 11 s, seeded by its period so each starts somewhere else without any stored state. */
private fun DrawScope.drawShootingStar(t: Float) {
    val n = floor(t / 11f)
    val p = (t - n * 11f) / .9f
    if (p >= 1f) return
    val rnd = Random(n.toInt())
    val start = Offset(size.width * (.35f + .6f * rnd.nextFloat()), size.height * (.04f + .25f * rnd.nextFloat()))
    val travel = Offset(-size.width * .45f, size.height * .18f)
    val head = start + travel * p
    val tail = head - travel * .3f
    val fade = sin(p * PI.toFloat())
    drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = fade)), tail, head), tail, head, 1.5.dp.toPx())
    drawCircle(Color(0xFF7FFFE6).copy(alpha = .35f * fade), 3.dp.toPx(), head)
}
