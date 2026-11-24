package xyz.yuuki.navidroid

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import xyz.yuuki.navidroid.ui.Nav
import xyz.yuuki.navidroid.ui.PlayerState
import xyz.yuuki.navidroid.ui.Route

const val ACTION_SHORTCUT = "xyz.yuuki.navidroid.SHORTCUT"
const val EXTRA_SHORTCUT = "shortcut"

/** What a launcher shortcut or the playlist widget asks MainActivity to do; [spec] is how it travels in the intent. */
sealed interface ShortcutAction {
    data object Search : ShortcutAction
    data object Shuffle : ShortcutAction
    data class Open(val kind: Kind, val id: String) : ShortcutAction
    data class Play(val kind: Kind, val id: String) : ShortcutAction

    val spec: String
        get() = when (this) {
            Search -> "search"
            Shuffle -> "shuffle"
            is Open -> "open:${kind.name}:$id"
            is Play -> "play:${kind.name}:$id"
        }

    companion object {
        fun parse(spec: String?): ShortcutAction? {
            val p = spec?.split(":", limit = 3) ?: return null
            if (p.size == 1) return when (p[0]) { "search" -> Search; "shuffle" -> Shuffle; else -> null }
            val kind = runCatching { Kind.valueOf(p[1]) }.getOrNull() ?: return null
            val id = p.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: return null
            return when (p[0]) {
                "open" -> Open(kind, id)
                // An artist page has no song list to start from.
                "play" -> if (kind == Kind.Artist) null else Play(kind, id)
                else -> null
            }
        }
    }
}

fun ShortcutAction.intent(ctx: Context): Intent = Intent(ctx, MainActivity::class.java).setAction(ACTION_SHORTCUT).putExtra(EXTRA_SHORTCUT, spec)

fun Intent.shortcut(): ShortcutAction? = if (action == ACTION_SHORTCUT) ShortcutAction.parse(getStringExtra(EXTRA_SHORTCUT)) else null

/** Set by MainActivity from the launch intent; [ShortcutHandler] runs it once it can, which for playback is when the player has connected. */
object PendingShortcut {
    var action by mutableStateOf<ShortcutAction?>(null)
}

@Composable
fun ShortcutHandler(nav: Nav, player: PlayerState?) {
    val a = PendingShortcut.action ?: return
    LaunchedEffect(a, player != null) {
        when {
            a is ShortcutAction.Search -> nav.tab(Route.Search)
            a is ShortcutAction.Open -> nav.go(Route.Open(a.kind, a.id))
            player == null -> return@LaunchedEffect
            else -> try {
                val api = App.it.api
                when (a) {
                    ShortcutAction.Shuffle -> player.play(api.randomSongs(100), shuffled = true, from = "Shuffled library")
                    is ShortcutAction.Play -> api.collection(a.kind, a.id).let { player.play(it.songs, from = it.title); recordRecent(it) }
                    else -> {}
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(App.it, "Couldn't start playback", Toast.LENGTH_SHORT).show()
            }
        }
        PendingShortcut.action = null
    }
}

private val RECENT_KINDS = setOf(Kind.Playlist, Kind.Album, Kind.ArtistMix, Kind.ArtistRadio)

/** Keeps the latest collection played as a dynamic launcher shortcut (launchers show 4, three of them the static ones); the shortcut is the storage. */
fun recordRecent(c: Collection) {
    if (c.kind !in RECENT_KINDS) return
    val ctx = App.it
    val sm = ctx.getSystemService(ShortcutManager::class.java) ?: return
    val open = ShortcutAction.Open(c.kind, c.id)
    runCatching {
        sm.dynamicShortcuts = listOf(
            ShortcutInfo.Builder(ctx, open.spec).setShortLabel(c.title).setLongLabel(c.title)
                .setIcon(Icon.createWithResource(ctx, R.mipmap.ic_launcher)).setIntent(open.intent(ctx)).build(),
        )
    }
}
