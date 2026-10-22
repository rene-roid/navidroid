package xyz.yuuki.navidroid.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.R

// Night-sky navy and indigo, after the aurora app icons.
val Bg = Color(0xFF0A0D24)
val Surface = Color(0xFF14183A)
val Elevated = Color(0xFF1C2050)
val Tile = Color(0xFF1F2456)
val Blue = Color(0xFF0085FF) // Navidrome's brand blue
val Green = Color(0xFF1DB954)
val Purple = Color(0xFF8B5CF6)

/** Colour of buttons and highlights, picked in Settings; global so a change applies everywhere at once. */
var Accent by mutableStateOf(Color(App.it.prefs.accent))
val OnAccent get() = if (Accent.luminance() > .18f) Color.Black else Color.White
val Sub = Color(0xFFB8BBE6)
val Faint = Color(0xFF7D81B3)
val HomeTint = Color(0xFF2B2470)

/** The accent turning toward violet, like the icons' neon edges; for the main buttons. */
val AccentGlow get() = Brush.linearGradient(listOf(Accent, lerp(Accent, Color(0xFFB06CFF), .6f)))

/** Frosted glass like the icon tiles: a faint fill, lit along the top-left edge and glowing in the accent at the bottom-right. */
fun Modifier.glass(shape: Shape) = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0x1FA7B4FF), Color(0x0D7C6CFF))))
    .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .3f), Color.White.copy(alpha = .05f), Accent.copy(alpha = .35f))), shape)

private fun figtree(weight: Int) = Font(R.font.figtree, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700), figtree(800), figtree(900))

private val base = TextStyle(fontFamily = Figtree, color = Color.White)

private val typography = Typography(
    displaySmall = base.copy(fontSize = 34.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.5).sp),
    headlineMedium = base.copy(fontSize = 26.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.3).sp),
    headlineSmall = base.copy(fontSize = 22.sp, fontWeight = FontWeight.W700),
    titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.W700),
    titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.W600),
    titleSmall = base.copy(fontSize = 14.sp, fontWeight = FontWeight.W600),
    bodyLarge = base.copy(fontSize = 16.sp),
    bodyMedium = base.copy(fontSize = 14.sp),
    bodySmall = base.copy(fontSize = 13.sp, color = Sub),
    labelLarge = base.copy(fontSize = 14.sp, fontWeight = FontWeight.W700),
    labelMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.W600),
    labelSmall = base.copy(fontSize = 11.sp, fontWeight = FontWeight.W600),
)

@Composable
fun NavidroidTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Accent,
        onPrimary = OnAccent,
        secondary = Accent,
        background = Bg,
        onBackground = Color.White,
        surface = Bg,
        onSurface = Color.White,
        onSurfaceVariant = Sub,
        surfaceContainer = Elevated,
        surfaceContainerHigh = Elevated,
        surfaceContainerHighest = Tile,
        surfaceContainerLow = Surface,
        outline = Faint,
        outlineVariant = Color(0xFF2E3370),
    ),
    typography = typography,
) {
    CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
}

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

fun formatLength(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "$h hr $m min" else "$m min ${seconds % 60} sec"
}

fun formatBytes(b: Long): String = when {
    b >= 1L shl 30 -> "%.1f GB".format(b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> "%.0f MB".format(b / (1L shl 20).toDouble())
    else -> "%.0f KB".format(b / 1024.0)
}
