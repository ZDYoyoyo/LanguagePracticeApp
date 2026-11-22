package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.fakes.DirectTx
import io.github.zdyoyoyo.englishpractice.fakes.FakeAttemptDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeClock
import io.github.zdyoyoyo.englishpractice.fakes.FakeWrongAnswerDao
import io.github.zdyoyoyo.englishpractice.fakes.question
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WrongBookRecordingTest {
    private val clock = FakeClock()
    private val attempts = FakeAttemptDao()
    private val wrongs = FakeWrongAnswerDao()
    private var changedAt = 0L
    private val repo = StudyRepository(DirectTx, attempts, wrongs, clock, onLocalDataChanged = { changedAt = it })
    private val q = question("q1")

    @Test fun `wrong answer is added to wrong book`() = runTest {
        repo.recordAnswer(q, "b", correct = false, elapsedMs = 1000, mode = StudyMode.PRACTICE)
        val w = wrongs.get("q1")!!
        assertEquals(1, w.wrongCount)
        assertEquals("b", w.lastWrongAnswer)
        assertFalse(w.mastered)
        assertEquals(1, attempts.items.value.size)
        assertEquals(clock.nowMillis(), changedAt)
    }

    @Test fun `correct answer is not added`() = runTest {
        repo.recordAnswer(q, "a", correct = true, elapsedMs = 1000, mode = StudyMode.PRACTICE)
        assertNull(wrongs.get("q1"))
        assertEquals(1, attempts.items.value.size)
    }

    @Test fun `repeated wrong increments count`() = runTest {
        repo.recordAnswer(q, "b", false, 1000, StudyMode.PRACTICE)
        repo.recordAnswer(q, "c", false, 1000, StudyMode.PRACTICE)
        val w = wrongs.get("q1")!!
        assertEquals(2, w.wrongCount)
        assertEquals("c", w.lastWrongAnswer)
    }

    @Test fun `correct in practice does not remove from wrong book`() = runTest {
        repo.recordAnswer(q, "b", false, 1000, StudyMode.PRACTICE)
        repo.recordAnswer(q, "a", true, 1000, StudyMode.PRACTICE)
        assertEquals(listOf("q1"), repo.activeWrongQuestionIds())
    }

    @Test fun `correct in wrong book mode masters then wrong again re-adds`() = runTest {
        repo.recordAnswer(q, "b", false, 1000, StudyMode.PRACTICE)
        repo.recordAnswer(q, "a", true, 1000, StudyMode.WRONG_BOOK)
        assertTrue(wrongs.get("q1")!!.mastered)
        assertEquals(0, repo.activeWrongCount.first())

        repo.recordAnswer(q, "c", false, 1000, StudyMode.REVIEW)
        val w = wrongs.get("q1")!!
        assertFalse(w.mastered)
        assertEquals(2, w.wrongCount)
        assertEquals(1, repo.activeWrongCount.first())
    }

    @Test fun `manual removal`() = runTest {
        repo.recordAnswer(q, "b", false, 1000, StudyMode.PRACTICE)
        repo.removeFromWrongBook("q1")
        assertTrue(repo.activeWrongQuestionIds().isEmpty())
    }
}
