package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.announce.Announcement
import io.github.zdyoyoyo.englishpractice.announce.Announcements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class AnnouncementsTest {
    private val today = LocalDate.of(2026, 12, 10)

    @Test
    fun `date window and version range filter announcements`() {
        val list = listOf(
            Announcement("a", "always"),
            Announcement("b", "future", start = "2026-12-11"),
            Announcement("c", "expired", end = "2026-12-09"),
            Announcement("d", "in window", start = "2026-12-10", end = "2026-12-10"),
            Announcement("e", "old app only", maxVersionCode = 20),
            Announcement("f", "bad date", start = "12/01"),
        )
        val ids = Announcements.active(list, today, versionCode = 30, dismissed = emptySet()).map { it.id }
        assertEquals(listOf("a", "d"), ids)
    }

    @Test
    fun `dismissed announcements are hidden and seen ones are not notified`() {
        val list = listOf(Announcement("a", "A", notify = true), Announcement("b", "B", notify = true), Announcement("c", "C"))
        val active = Announcements.active(list, today, 30, dismissed = setOf("a"))
        assertEquals(listOf("b", "c"), active.map { it.id })
        assertEquals(listOf("b"), Announcements.toNotify(active, alreadySeen = emptySet()).map { it.id })
        assertTrue(Announcements.toNotify(active, alreadySeen = setOf("b")).isEmpty())
    }

    @Test
    fun `actions must be a known screen or https`() {
        assertTrue(Announcements.isValidAction("mock"))
        assertTrue(Announcements.isValidAction("https://example.com"))
        assertFalse(Announcements.isValidAction("http://example.com"))
        assertFalse(Announcements.isValidAction("unknown"))
        assertFalse(Announcements.isValidAction(null))
    }

    @Test
    fun `published announcements file is valid`() {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "question-bank").isDirectory }
        val file = Announcements.parse(File(dir, "question-bank/announcements.json").readText())
        val ids = file.announcements.map { it.id }
        assertEquals("announcement ids must be unique", ids.size, ids.toSet().size)
        file.announcements.forEach { a ->
            assertTrue(a.id, a.title.isNotBlank())
            if (a.action != null) assertTrue("bad action in ${a.id}", Announcements.isValidAction(a.action))
            listOfNotNull(a.start, a.end).forEach { LocalDate.parse(it) }
        }
    }
}
