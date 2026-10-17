package xyz.yuuki.navidroid

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.google.common.util.concurrent.ListenableFuture
import xyz.yuuki.navidroid.ui.ArtistScreen
import xyz.yuuki.navidroid.ui.Bg
import xyz.yuuki.navidroid.ui.CollectionScreen
import xyz.yuuki.navidroid.ui.DockedPlayer
import xyz.yuuki.navidroid.ui.DownloadsScreen
import xyz.yuuki.navidroid.ui.HomeTint
import xyz.yuuki.navidroid.ui.HomeScreen
import xyz.yuuki.navidroid.ui.LibraryScreen
import xyz.yuuki.navidroid.ui.LocalNav
import xyz.yuuki.navidroid.ui.LocalPlayer
import xyz.yuuki.navidroid.ui.LoginScreen
import xyz.yuuki.navidroid.ui.MiniPlayer
import xyz.yuuki.navidroid.ui.MoreScreen
import xyz.yuuki.navidroid.ui.Nav
import xyz.yuuki.navidroid.ui.NavidroidTheme
import xyz.yuuki.navidroid.ui.NowPlaying
import xyz.yuuki.navidroid.ui.PlayerState
import xyz.yuuki.navidroid.ui.Route
import xyz.yuuki.navidroid.ui.SearchScreen
import xyz.yuuki.navidroid.ui.SettingsScreen
import xyz.yuuki.navidroid.ui.Sub
import xyz.yuuki.navidroid.ui.isWide

class MainActivity : ComponentActivity() {
    private var future: ListenableFuture<MediaController>? = null
    private var player by mutableStateOf<PlayerState?>(null)
    private var loggedIn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        loggedIn = App.it.prefs.loggedIn
        if (loggedIn) connect()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            NavidroidTheme {
                if (!loggedIn) LoginScreen {
                    loggedIn = true
                    connect()
                    App.it.offline.sync()
                } else CompositionLocalProvider(LocalPlayer provides player) {
                    Shell(::logout)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (loggedIn) App.it.offline.syncIfStale()
    }

    private fun connect() {
        if (future != null) return
        val f = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        future = f
        f.addListener({ player = PlayerState(f.get()) }, mainExecutor)
    }

    private fun logout() {
        player?.c?.run {
            stop()
            clearMediaItems()
        }
        val prefs = App.it.prefs
        prefs.token = null
        prefs.salt = null
        prefs.endpoints = emptyList()
        loggedIn = false
    }

    override fun onDestroy() {
        future?.let(MediaController::releaseFuture)
        super.onDestroy()
    }
}

@Composable
private fun Shell(onLogout: () -> Unit) {
    val nav = remember { Nav() }
    val player = LocalPlayer.current
    var nowPlaying by rememberSaveable { mutableStateOf(false) }
    val wide = isWide()
    val docked = wide && player?.song != null

    CompositionLocalProvider(LocalNav provides nav) {
        // In landscape the rail, the page and the player card share one background tinted by the page, so it runs edge to edge.
        val page = nav.stack.last()
        val tint by animateColorAsState(if (!wide) Bg else nav.tints[page] ?: if (page == Route.Home) HomeTint else Bg, tween(300))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tint, .35f to Bg))) {
            // Landscape has its own layout: an icon rail, the page, and while something plays the player as a card on the right.
            Row(Modifier.fillMaxSize()) {
                if (wide) Column(
                    Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                        .width(76.dp).padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Tabs line up with the page header, downloads and settings with the bottom of the player card.
                    val top = nav.stack.last()
                    Tabs(nav, Modifier, rail = true)
                    Spacer(Modifier.weight(1f))
                    RailItem("Downloads", Icons.Rounded.DownloadForOffline, top == Route.Downloads) { if (top != Route.Downloads) nav.go(Route.Downloads) }
                    RailItem("Settings", Icons.Rounded.Settings, top == Route.Settings) { if (top != Route.Settings) nav.go(Route.Settings) }
                }
                Box(Modifier.weight(1f).then(if (docked) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End)))) {
                    NavDisplay(
                        backStack = nav.stack,
                        onBack = { nav.back() },
                        transitionSpec = { (slideInHorizontally(tween(260)) { it / 5 } + fadeIn(tween(260))) togetherWith fadeOut(tween(160)) },
                        popTransitionSpec = { fadeIn(tween(200)) togetherWith (slideOutHorizontally(tween(220)) { it / 5 } + fadeOut(tween(220))) },
                        predictivePopTransitionSpec = { fadeIn(tween(200)) togetherWith (slideOutHorizontally(tween(220)) { it / 5 } + fadeOut(tween(220))) },
                        entryProvider = { r ->
                            NavEntry(r) {
                                when (r) {
                                    Route.Home -> HomeScreen()
                                    Route.Search -> SearchScreen()
                                    Route.Library -> LibraryScreen()
                                    Route.Settings -> SettingsScreen(onLogout)
                                    Route.Downloads -> DownloadsScreen()
                                    is Route.More -> MoreScreen(r.section)
                                    is Route.Open -> if (r.kind == Kind.Artist) ArtistScreen(r.id) else CollectionScreen(r.kind, r.id)
                                }
                            }
                        },
                    )

                    if (!wide) Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .background(Brush.verticalGradient(0f to Color.Transparent, .35f to Color.Black.copy(alpha = .85f), 1f to Color.Black)),
                    ) {
                        Spacer(Modifier.height(16.dp))
                        player?.let { MiniPlayer(it) { nowPlaying = true } }
                        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(60.dp)) { Tabs(nav, Modifier.weight(1f)) }
                    }
                }
                if (docked) DockedPlayer(
                    player,
                    Modifier.width(360.dp).fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))
                        .padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
                )
            }

            AnimatedVisibility(
                nowPlaying && !wide && player?.song != null,
                enter = slideInVertically(tween(320)) { it },
                exit = slideOutVertically(tween(260)) { it },
            ) {
                player?.let { NowPlaying(it) { nowPlaying = false } }
            }
            BackHandler(nowPlaying && !wide) { nowPlaying = false }
        }
    }
}

@Composable
private fun Tabs(nav: Nav, item: Modifier, rail: Boolean = false) {
    val tab = nav.stack.first()
    @Composable
    fun tab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) =
        if (rail) RailItem(label, icon, selected, onClick) else TabItem(label, icon, selected, item, onClick)
    tab("Home", if (tab == Route.Home) Icons.Rounded.Home else Icons.Outlined.Home, tab == Route.Home) { nav.tab(Route.Home) }
    tab("Search", Icons.Rounded.Search, tab == Route.Search) { nav.tab(Route.Search) }
    tab("Your Library", if (tab == Route.Library) Icons.Rounded.LibraryMusic else Icons.Outlined.LibraryMusic, tab == Route.Library) { nav.tab(Route.Library) }
}

/** Icon-only tab for the landscape rail; the selected one sits in a pill. */
@Composable
private fun RailItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) = Box(
    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (selected) Color.White.copy(alpha = .16f) else Color.Transparent).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    Icon(icon, label, Modifier.size(26.dp), tint = if (selected) Color.White else Sub)
}

@Composable
private fun TabItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) = Column(
    modifier.heightIn(min = 60.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(top = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val tint = if (selected) Color.White else Sub
    Icon(icon, label, Modifier.size(26.dp), tint = tint)
    Text(label, style = MaterialTheme.typography.labelSmall, color = tint, textAlign = TextAlign.Center)
}
