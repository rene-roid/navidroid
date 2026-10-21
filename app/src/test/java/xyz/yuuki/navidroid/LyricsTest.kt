package xyz.yuuki.navidroid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {
    @Test
    fun lrcHeadersAreNotLyrics() {
        assertTrue(isLrcTag("[ti:Don't Forget Me]"))
        assertTrue(isLrcTag(" [re:Rescale] "))
        assertTrue(isLrcTag("[ar:]"))
        assertFalse(isLrcTag("[Chorus]"))
        assertFalse(isLrcTag("[00:12.34]"))
        assertFalse(isLrcTag("She said: [laughs] no"))
    }
}
