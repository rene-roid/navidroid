package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistEditTest {
    private fun song(id: String) = Song(id, id, "a", null, "", null, null, 0, 0, 0, false, null, 0)
    private fun draft(vararg ids: String) = EditDraft("n", "", false, ids.map(::song))
    private val orig = listOf("a", "b", "c", "d").map(::song)

    @Test
    fun untouchedNeedsNothing() = assertEquals(EditPlan(emptyList(), null), editPlan(orig, draft("a", "b", "c", "d")))

    @Test
    fun removalsUseOriginalIndexes() = assertEquals(EditPlan(listOf(1, 3), null), editPlan(orig, draft("a", "c")))

    @Test
    fun reorderRewritesEverything() = assertEquals(EditPlan(emptyList(), listOf("c", "a", "d")), editPlan(orig, draft("c", "a", "d")))

    @Test
    fun repeatedSongRemovesOnlyTheDroppedCopy() =
        assertEquals(EditPlan(listOf(2), null), editPlan(listOf("a", "b", "a").map(::song), draft("a", "b")))

    @Test
    fun splitsSongsAlreadyThere() {
        val (fresh, dups) = splitDuplicates(orig, listOf("b", "x", "d").map(::song))
        assertEquals(listOf("x"), fresh.map { it.id })
        assertEquals(listOf("b", "d"), dups.map { it.id })
    }
}
