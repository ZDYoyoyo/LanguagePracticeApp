package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.model.QuestionType
import io.github.zdyoyoyo.englishpractice.domain.BotProfile
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker
import io.github.zdyoyoyo.englishpractice.domain.PkEngine
import io.github.zdyoyoyo.englishpractice.fakes.question
import io.github.zdyoyoyo.englishpractice.notify.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class DailyAndPkTest {
    private val questions = (1..10).map {
        question("q$it", type = QuestionType.VOCABULARY, word = if (it % 2 == 0) "word$it" else null)
    }

    @Test fun `daily question is stable within a day and list order independent`() {
        val a = DailyPicker.dailyQuestion(questions, 20_000)
        val b = DailyPicker.dailyQuestion(questions.reversed(), 20_000)
        assertEquals(a, b)
        val picks = (20_000L until 20_030L).map { DailyPicker.dailyQuestion(questions, it)!!.id }.toSet()
        assertTrue("30 天內應該會輪到多題", picks.size > 3)
    }

    @Test fun `daily set is word first then a different question, same everywhere`() {
        repeat(60) {
            val day = 20_000L + it
            val set = DailyPicker.dailySet(questions, day)
            assertEquals(2, set.size)
            assertEquals(DailyPicker.dailyWord(questions, day), set[0])
            assertEquals(DailyPicker.dailyQuestion(questions, day), set[1])
            assertTrue(set[0].id != set[1].id)
        }
    }

    @Test fun `daily word only picks questions with word`() {
        repeat(30) { assertNotNull(DailyPicker.dailyWord(questions, 20_000L + it)!!.word) }
        assertNull(DailyPicker.dailyWord(emptyList(), 1))
    }

    @Test fun `bot script is deterministic and within time limit`() {
        val ids = (1..5).map { "q$it" }
        val bot = BotProfile.byId("steady")
        val s1 = PkEngine.botScript(bot, ids, seed = 123)
        assertEquals(s1, PkEngine.botScript(bot, ids, seed = 123))
        assertTrue(s1.all { it.timeMs in 1_500 until PkEngine.TIME_LIMIT_MS })
    }

    @Test fun `stronger bot is more accurate over many questions`() {
        val ids = (1..500).map { "q$it" }
        val rookie = PkEngine.botScript(BotProfile.byId("rookie"), ids, 1).count { it.correct }
        val master = PkEngine.botScript(BotProfile.byId("master"), ids, 1).count { it.correct }
        assertTrue(master > rookie)
    }

    @Test fun `scoring rewards speed and correctness`() {
        assertEquals(0, PkEngine.score(false, 1000))
        assertEquals(0, PkEngine.score(true, PkEngine.TIME_LIMIT_MS))
        assertEquals(150, PkEngine.score(true, 0))
        assertTrue(PkEngine.score(true, 2000) > PkEngine.score(true, 9000))
    }

    @Test fun `outcome tie breaks by correct count then time`() {
        val a = PkEngine.Side(score = 300, correct = 3, totalTimeMs = 10_000)
        assertEquals(PkEngine.Outcome.WIN, PkEngine.outcome(a, a.copy(score = 200)))
        assertEquals(PkEngine.Outcome.LOSE, PkEngine.outcome(a, a.copy(correct = 4)))
        assertEquals(PkEngine.Outcome.WIN, PkEngine.outcome(a, a.copy(totalTimeMs = 12_000)))
        assertEquals(PkEngine.Outcome.DRAW, PkEngine.outcome(a, a))
    }

    @Test fun `reminder delay goes to today or tomorrow`() {
        val now = LocalDateTime.of(2026, 1, 1, 19, 0)
        assertEquals(Duration.ofHours(1), ReminderScheduler.delayUntil(20 * 60, now))
        assertEquals(Duration.ofHours(23), ReminderScheduler.delayUntil(18 * 60, now))
        assertEquals(Duration.ofHours(24), ReminderScheduler.delayUntil(19 * 60, now))
    }
}
