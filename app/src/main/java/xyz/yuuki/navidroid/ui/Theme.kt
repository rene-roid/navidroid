package xyz.yuuki.navidroid.ui

import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import xyz.yuuki.navidroid.App
import xyz.yuuki.navidroid.R

// The server's "Spotify" theme.
val Bg = Color(0xFF121212)
val Surface = Color(0xFF1F1F1F)
val Elevated = Color(0xFF282828)
val Tile = Color(0xFF2A2A2A)
val Blue = Color(0xFF0085FF) // Navidrome's brand blue
val Green = Color(0xFF1DB954)
val Purple = Color(0xFF8B5CF6)

/** Colour of buttons and highlights, picked in Settings; global so a change applies everywhere at once. */
var Accent by mutableStateOf(App.it.prefs.run { (if (accentWallpaper) wallpaperAccent(App.it) else null) ?: Color(accent) })

/** The wallpaper's colour (Material You), on the Androids that have one. */
fun wallpaperAccent(ctx: Context) = if (Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(ctx).primary else null

// What Settings > Appearance chooses besides colours; global like Accent, and saved by whoever changes them.
var accentFromWallpaper by mutableStateOf(App.it.prefs.accentWallpaper)
var homeSaved by mutableStateOf(App.it.prefs.homeOrder)
var homeHidden by mutableStateOf(App.it.prefs.homeHidden)
var compactLists by mutableStateOf(App.it.prefs.compactLists)
/** 0 the cover, 1 the cover on a turning record, 2 lyrics first. */
var nowPlayingStyle by mutableIntStateOf(App.it.prefs.nowPlayingStyle)
var largeControls by mutableStateOf(App.it.prefs.largeControls)

/** Whether lists are drawn tight: smaller covers, less air between rows. */
val LocalCompact = compositionLocalOf { false }
val OnAccent get() = if (Accent.luminance() > .18f) Color.Black else Color.White
val Sub = Color(0xFFB3B3B3)
val Faint = Color(0xFF7A7A7A)
val HomeTint = Color(0xFF2E2745)

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
        outlineVariant = Color(0xFF3E3E3E),
    ),
    typography = typography,
) {
    val ctx = LocalContext.current
    // A new wallpaper recreates the activity, which comes back through here.
    if (accentFromWallpaper) LaunchedEffect(LocalConfiguration.current) { wallpaperAccent(ctx)?.let { Accent = it } }
    CompositionLocalProvider(LocalContentColor provides Color.White, LocalCompact provides compactLists, content = content)
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
