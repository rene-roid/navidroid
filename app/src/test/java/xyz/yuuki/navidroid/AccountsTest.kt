package xyz.yuuki.navidroid

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountsTest {
    private val two = Accounts(listOf(Account("default", "test on music.example.com"), Account("3f9a1c2e", "ana on 10.0.2.2:4533")), "3f9a1c2e")

    @Test
    fun registrySurvivesBeingSaved() {
        assertEquals(
            """{"active":"3f9a1c2e","list":[{"id":"default","label":"test on music.example.com"},{"id":"3f9a1c2e","label":"ana on 10.0.2.2:4533"}]}""",
            two.toJson(),
        )
        assertEquals(two, Accounts.from(two.toJson()))
    }

    @Test
    fun anInstallFromBeforeAccountsStartsOnTheDefaultOne() {
        assertEquals(Accounts(emptyList(), "default"), Accounts.from(null))
    }

    @Test
    fun theDefaultAccountKeepsTheOldPaths() {
        assertEquals("media", accountPath("default", "media"))
        assertEquals("queue.json", accountPath("default", "queue.json"))
        assertEquals("", accountPath("default", ""))
        assertEquals("navidroid", prefsName("default"))
    }

    @Test
    fun otherAccountsGetAFolderAndPrefsOfTheirOwn() {
        assertEquals("a-3f9a1c2e/media", accountPath("3f9a1c2e", "media"))
        assertEquals("a-3f9a1c2e/", accountPath("3f9a1c2e", ""))
        assertEquals("navidroid-3f9a1c2e", prefsName("3f9a1c2e"))
    }

    @Test
    fun labelIsTheUserAndTheHost() {
        assertEquals("ana on 10.0.2.2:4533", accountLabel("ana", "http://10.0.2.2:4533"))
        assertEquals("test on music.example.com", accountLabel("test", "https://music.example.com"))
    }

    @Test
    fun loggingOutOfTheActiveAccountMovesToAnotherOrBackToTheLogin() {
        assertEquals(Accounts(listOf(Account("default", "test on music.example.com")), "default"), two.without("3f9a1c2e"))
        assertEquals(Accounts(listOf(Account("3f9a1c2e", "ana on 10.0.2.2:4533")), "3f9a1c2e"), two.without("default"))
        assertEquals(Accounts(emptyList(), "default"), two.without("default").without("3f9a1c2e"))
    }

    @Test
    fun aLoginIsAddedOnceAndRelabelledInPlace() {
        val renamed = Account("default", "test on 192.168.1.10:4533")
        assertEquals(listOf(renamed, two.list[1]), two.with(renamed).list)
        assertEquals(listOf(Account("default", "a on b")), Accounts().with(Account("default", "a on b")).list)
    }

    @Test
    fun aNewLoginTakesTheDefaultSlotWhileItIsFree() {
        assertEquals("default", Accounts().newId("77aa"))
        assertEquals("77aa", two.newId("77aa"))
        assertEquals("default", two.without("default").newId("77aa"))
    }

    @Test
    fun foldersOfLoggedOutAccountsAreOrphans() {
        assertEquals(listOf("dead"), orphans(listOf("media", "covers", "a-3f9a1c2e", "a-dead", "avatar"), two))
        // The active one is kept even before it is registered.
        assertEquals(emptyList<String>(), orphans(listOf("a-new"), Accounts(emptyList(), "new")))
    }
}
