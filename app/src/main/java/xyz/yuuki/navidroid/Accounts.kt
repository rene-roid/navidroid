package xyz.yuuki.navidroid

import org.json.JSONArray
import org.json.JSONObject

/** The account every install had before there could be several; it keeps the paths and prefs name of back then. */
const val DEFAULT_ACCOUNT = "default"

/** A login on some server. [label] is "username on host". */
data class Account(val id: String, val label: String)

/** Every logged-in account and the one the process runs as. One process serves one account; switching restarts it. */
data class Accounts(val list: List<Account> = emptyList(), val active: String = DEFAULT_ACCOUNT) {
    /** [a] added, or relabelled where it already is. */
    fun with(a: Account) = copy(list = if (list.any { it.id == a.id }) list.map { if (it.id == a.id) a else it } else list + a)

    /** [id] logged out; when it was the active one the first of the rest takes over, or the default slot for the login screen. */
    fun without(id: String) = list.filterNot { it.id == id }.let { rest -> Accounts(rest, if (active == id) rest.firstOrNull()?.id ?: DEFAULT_ACCOUNT else active) }

    /** The id a new login goes under: the default slot while it is free, otherwise [fresh]. */
    fun newId(fresh: String) = if (list.none { it.id == DEFAULT_ACCOUNT }) DEFAULT_ACCOUNT else fresh

    fun toJson() = JSONObject().put("active", active)
        .put("list", JSONArray(list.map { JSONObject().put("id", it.id).put("label", it.label) })).toString()

    companion object {
        fun from(json: String?) = json?.let(::JSONObject)?.let { o ->
            Accounts(o.list("list").map { Account(it.getString("id"), it.getString("label")) }, o.getString("active"))
        } ?: Accounts()
    }
}

fun accountLabel(username: String, endpoint: String) = "$username on ${endpoint.substringAfter("://")}"

/** Where [name] lives under filesDir or cacheDir for account [id]. */
fun accountPath(id: String, name: String) = if (id == DEFAULT_ACCOUNT) name else "a-$id/$name"

fun prefsName(id: String) = if (id == DEFAULT_ACCOUNT) "navidroid" else "navidroid-$id"

/** The Media3 database of an account other than the default, which keeps Media3's own. */
fun accountDb(id: String) = "a-$id.db"

/** Ids whose `a-<id>` folder in [dirs] is left over from an account that has since logged out. */
fun orphans(dirs: List<String>, accounts: Accounts) =
    dirs.filter { it.startsWith("a-") }.map { it.removePrefix("a-") }.filter { id -> id != accounts.active && accounts.list.none { it.id == id } }
