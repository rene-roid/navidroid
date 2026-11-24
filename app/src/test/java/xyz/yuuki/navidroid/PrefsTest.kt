package xyz.yuuki.navidroid

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SharedPreferences in a map, as Android's own is a stub in local unit tests. */
private class MapPrefs : SharedPreferences, SharedPreferences.Editor {
    val map = mutableMapOf<String, Any?>()

    override fun getAll() = map
    override fun getString(k: String, d: String?) = map[k] as String? ?: d
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(k: String, d: MutableSet<String>?) = map[k] as MutableSet<String>? ?: d
    override fun getInt(k: String, d: Int) = map[k] as Int? ?: d
    override fun getLong(k: String, d: Long) = map[k] as Long? ?: d
    override fun getFloat(k: String, d: Float) = map[k] as Float? ?: d
    override fun getBoolean(k: String, d: Boolean) = map[k] as Boolean? ?: d
    override fun contains(k: String) = k in map
    override fun edit() = this
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {}

    private fun put(k: String, v: Any?) = apply { map[k] = v }
    override fun putString(k: String, v: String?) = put(k, v)
    override fun putStringSet(k: String, v: MutableSet<String>?) = put(k, v)
    override fun putInt(k: String, v: Int) = put(k, v)
    override fun putLong(k: String, v: Long) = put(k, v)
    override fun putFloat(k: String, v: Float) = put(k, v)
    override fun putBoolean(k: String, v: Boolean) = put(k, v)
    override fun remove(k: String) = apply { map.remove(k) }
    override fun clear() = apply { map.clear() }
    override fun commit() = true
    override fun apply() {}
}

class PrefsTest {
    private val files = mutableMapOf<String, MapPrefs>()
    private fun prefs(account: String) = Prefs(account) { files.getOrPut(it) { MapPrefs() } }

    @Test
    fun howThePhoneLooksAndPlaysIsTheSameInEveryAccount() {
        val ana = prefs("3f9a1c2e")
        ana.accent = 0xFF112233.toInt()
        ana.streamBitrateMobile = 96
        ana.wifiOnlyDownloads = false
        ana.libraryGrid = true
        ana.auroraCustom = "1,2,3,4"
        ana.flag("auroraStars", false)

        for (other in listOf(prefs("77aa"), prefs("default"))) {
            assertEquals(0xFF112233.toInt(), other.accent)
            assertEquals(96, other.streamBitrateMobile)
            assertEquals(false, other.wifiOnlyDownloads)
            assertEquals(true, other.libraryGrid)
            assertEquals("1,2,3,4", other.auroraCustom)
            assertEquals(false, other.flag("auroraStars"))
        }
        // In the file every install already has, so nothing moves on upgrade.
        assertEquals(setOf("accent", "streamMobile", "wifiOnly", "libraryGrid", "auroraCustom", "auroraStars"), files["navidroid"]!!.map.keys)
    }

    @Test
    fun whoYouAreAndWhatYouKeepIsTheAccountsOwn() {
        val ana = prefs("3f9a1c2e")
        ana.username = "ana"
        ana.token = "t"
        ana.endpoints = listOf("http://10.0.2.2:4533")
        ana.pins = listOf("Album:al-1")
        ana.hiddenPlaylists = setOf("pl-1")
        ana.lastSync = 5

        val other = prefs("77aa")
        assertNull(other.username)
        assertEquals(false, other.loggedIn)
        assertEquals(emptyList<String>(), other.pins)
        assertEquals(emptySet<String>(), other.hiddenPlaylists)
        assertEquals(0L, other.lastSync)
        assertEquals(setOf("username", "token", "endpoints", "pins", "hiddenPlaylists", "lastSync"), files["navidroid-3f9a1c2e"]!!.map.keys)
        assertEquals(emptySet<String>(), files["navidroid"]!!.map.keys)
    }
}
