package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeTest {
    @Test
    fun navidromeSaysWhatItIsWhileRefusing() {
        val found = parseProbe(
            """{"subsonic-response":{"status":"failed","version":"1.16.1","type":"navidrome","serverVersion":"0.64.2 (10114574)","openSubsonic":true,"error":{"code":10,"message":"missing parameter: 'u'"}}}""",
        )
        assertEquals(Probe.Found("navidrome", "0.64.2", true), found)
        assertEquals("Navidrome 0.64.2", (found as Probe.Found).name)
    }

    @Test
    fun plainSubsonicHasNoTypeOrVersion() {
        val found = parseProbe("""{"subsonic-response":{"status":"failed","version":"1.16.1","error":{"code":10,"message":"Required parameter is missing."}}}""")
        assertEquals(Probe.Found("subsonic", "", false), found)
        assertEquals("Subsonic", (found as Probe.Found).name)
        assertEquals(Probe.Found("subsonic", "", false), parseProbe("""<subsonic-response xmlns="http://subsonic.org/restapi" status="failed" version="1.1.0"></subsonic-response>"""))
    }

    @Test
    fun otherAnswersAreNotSubsonic() {
        assertEquals(Probe.NotSubsonic, parseProbe("<!doctype html><html><body>Welcome to nginx!</body></html>"))
        assertEquals(Probe.NotSubsonic, parseProbe("""{"error":"not found"}"""))
        assertEquals(Probe.NotSubsonic, parseProbe(""))
    }
}
