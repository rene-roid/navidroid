package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoPinTest {
    private val liked = Pin(Kind.Liked, "liked")
    private fun mix(id: String) = Pin(Kind.Playlist, id)

    @Test fun addsNewMixesAndDropsGoneOnes() {
        val hand = Pin(Kind.Album, "x")
        val (pins, tracked) = autoPinPlan(listOf(hand, mix("old")), listOf(mix("old")), listOf(mix("new")))
        assertEquals(listOf(hand, mix("new")), pins)
        assertEquals(listOf(mix("new")), tracked)
    }

    @Test fun aPinMadeByHandStaysWhenSwitchedOff() {
        val (pins, tracked) = autoPinPlan(listOf(liked), emptyList(), emptyList())
        assertEquals(listOf(liked), pins)
        assertEquals(emptyList<Pin>(), tracked)
    }

    @Test fun switchingOffRemovesWhatItAdded() {
        val (pins, tracked) = autoPinPlan(listOf(liked, mix("a")), listOf(liked, mix("a")), emptyList())
        assertEquals(emptyList<Pin>(), pins)
        assertEquals(emptyList<Pin>(), tracked)
    }
}
