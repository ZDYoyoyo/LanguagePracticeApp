package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.fakes.DirectTx
import io.github.zdyoyoyo.englishpractice.fakes.FakeAttemptDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeClock
import io.github.zdyoyoyo.englishpractice.fakes.FakeFavoriteDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeReviewCardDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeStudyDayDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeWrongAnswerDao
import io.github.zdyoyoyo.englishpractice.fakes.question
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 作答 → SM-2 排程 → 「今天該複習幾題」的整合測試 */
class ReviewSchedulingTest {
    private val clock = FakeClock()
    private val cards = FakeReviewCardDao()
    private val days = FakeStudyDayDao()
    private val repo = StudyRepository(DirectTx, FakeAttemptDao(), FakeWrongAnswerDao(), cards, days, FakeFavoriteDao(), clock)
    private val q = question("q1")
    private val today get() = clock.today().toEpochDay()

    @Test fun `first correct answer schedules review tomorrow`() = runTest {
        repo.recordAnswer(q, "a", true, 2000, StudyMode.PRACTICE)
        assertEquals(today + 1, cards.get("q1")!!.dueEpochDay)
        assertEquals(0, repo.observeDueCount(today).first())
        clock.advanceDays(1)
        assertEquals(1, repo.observeDueCount(today).first())
        assertEquals(listOf("q1"), repo.dueQuestionIds())
    }

    @Test fun `correct reviews on due dates grow interval 1 then 6`() = runTest {
        repo.recordAnswer(q, "a", true, 2000, StudyMode.PRACTICE)
        clock.advanceDays(1)
        repo.recordAnswer(q, "a", true, 2000, StudyMode.REVIEW)
        assertEquals(6, cards.get("q1")!!.intervalDays)
        assertEquals(today + 6, cards.get("q1")!!.dueEpochDay)
    }

    @Test fun `early correct practice does not change schedule`() = runTest {
        repo.recordAnswer(q, "a", true, 2000, StudyMode.PRACTICE)
        val before = cards.get("q1")
        repo.recordAnswer(q, "a", true, 2000, StudyMode.PRACTICE) // 同一天又答對
        assertEquals(before, cards.get("q1"))
    }

    @Test fun `early wrong answer resets to tomorrow`() = runTest {
        repo.recordAnswer(q, "a", true, 2000, StudyMode.PRACTICE)
        clock.advanceDays(1)
        repo.recordAnswer(q, "a", true, 2000, StudyMode.REVIEW) // 6 天後到期
        repo.recordAnswer(q, "b", false, 2000, StudyMode.PRACTICE)
        val card = cards.get("q1")!!
        assertEquals(0, card.repetitions)
        assertEquals(today + 1, card.dueEpochDay)
    }

    @Test fun `answers mark study days for streak`() = runTest {
        repo.recordAnswer(q, "a", true, 2000, StudyMode.PRACTICE)
        clock.advanceDays(1)
        repo.recordAnswer(q, "a", true, 2000, StudyMode.PRACTICE)
        val streak = repo.streakNow()
        assertEquals(2, streak.current)
        assertTrue(streak.studiedToday)
        assertEquals(1, days.get(today)!!.answeredCount)
    }
}
