package xyz.yuuki.navidroid

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class LevelsTest {
    @Test
    fun loudnessAndKicksLineUpWithWhenTheyAreHeard() {
        Levels.flush(44_100, 1, C.ENCODING_PCM_16BIT)
        val out = FloatArray(2)
        // A quiet hiss for a second, then a loud 60 Hz thump, all decoded at t=0 as ExoPlayer does on start.
        repeat(44_100) { Levels.feed(if (it % 2 == 0) .1f else -.1f, 0L) }
        repeat(4_410) { Levels.feed(.8f * sin(2 * PI * 60 * it / 44_100).toFloat(), 0L) }

        Levels.at(500_000_000L, out)
        assertEquals("the hiss is near its own peak", 1f, out[0], .2f)
        assertEquals(0f, out[1])

        Levels.at(1_040_000_000L, out)
        assertEquals(1f, out[0], .15f)
        assertTrue("the thump is a kick", out[1] > .5f)

        Levels.at(5_000_000_000L, out)
        assertEquals("silence once the queue has played out", 0f, out[0])
    }
}
