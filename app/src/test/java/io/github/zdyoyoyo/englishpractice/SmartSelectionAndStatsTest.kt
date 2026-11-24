package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import io.github.zdyoyoyo.englishpractice.data.model.QuestionType
import io.github.zdyoyoyo.englishpractice.domain.QuestionCategory
import io.github.zdyoyoyo.englishpractice.domain.QuestionSelector
import io.github.zdyoyoyo.englishpractice.domain.QuestionStat
import io.github.zdyoyoyo.englishpractice.domain.QuestionStats
import io.github.zdyoyoyo.englishpractice.domain.StatsCalculator
import io.github.zdyoyoyo.englishpractice.domain.category
import io.github.zdyoyoyo.englishpractice.fakes.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SmartSelectionAndStatsTest {
    private fun reading(id: String, passage: String) = question(id).copy(passage = passage)
    private fun listening(id: String, passage: String) = question(id).copy(passage = passage, listening = true)

    @Test fun `category is derived from passage, listening and type`() {
        assertEquals(QuestionCategory.LISTENING, listening("l", "M: hi").category)
        assertEquals(QuestionCategory.READING, reading("r", "text").category)
        assertEquals(QuestionCategory.VOCABULARY, question("v", type = QuestionType.VOCABULARY).category)
        assertEquals(QuestionCategory.CLOZE, question("c", type = QuestionType.CLOZE, options = emptyList()).category)
    }

    @Test fun `unseen questions come before seen ones, and weak before mastered`() {
        val all = (1..6).map { question("q$it") }
        val stats = mapOf(
            "q1" to QuestionStat(3, 3, 0), // 全對
            "q2" to QuestionStat(2, 0, 0), // 全錯
            "q3" to QuestionStat(4, 2, 0), // 一半
        )
        repeat(20) { seed ->
            val picked = QuestionSelector.selectSmart(all, QuestionSelector.Filter(), 6, stats, random = Random(seed)).map { it.id }
            assertEquals(setOf("q4", "q5", "q6"), picked.take(3).toSet())
            assertEquals(listOf("q2", "q3", "q1"), picked.drop(3))
        }
    }

    @Test fun `prefer new off is plain random but still complete`() {
        val all = (1..10).map { question("q$it") }
        val picked = QuestionSelector.selectSmart(all, QuestionSelector.Filter(), 10, emptyMap(), preferNew = false, random = Random(1))
        assertEquals(all.map { it.id }.toSet(), picked.map { it.id }.toSet())
    }

    @Test fun `passage groups stay together in original order and are never split`() {
        val all = listOf(
            reading("r1", "A"), reading("r2", "A"), reading("r3", "A"),
            question("s1"), question("s2"),
        )
        repeat(30) { seed ->
            val picked = QuestionSelector.selectSmart(all, QuestionSelector.Filter(), 4, emptyMap(), random = Random(seed)).map { it.id }
            assertTrue(picked.size <= 4)
            if ("r1" in picked) {
                val i = picked.indexOf("r1")
                assertEquals(listOf("r1", "r2", "r3"), picked.subList(i, i + 3))
            } else {
                assertTrue("沒有拆開的組，因此只會出現單題", picked.none { it.startsWith("r") })
            }
        }
    }

    @Test fun `filter by category`() {
        val all = listOf(listening("l1", "M: a"), reading("r1", "x"), question("g1"))
        val picked = QuestionSelector.selectSmart(all, QuestionSelector.Filter(categories = setOf(QuestionCategory.LISTENING)), 10, emptyMap())
        assertEquals(listOf("l1"), picked.map { it.id })
    }

    @Test fun `multi select filter is OR within a field and AND across fields`() {
        val all = listOf(
            question("a", tags = listOf("travel"), difficulty = 1),
            question("b", tags = listOf("work"), difficulty = 2),
            question("c", tags = listOf("food"), difficulty = 1),
            listening("d", "M: x").copy(tags = listOf("travel")),
        )
        val f = QuestionSelector.Filter(tags = setOf("travel", "work"), difficulties = setOf(1, 2))
        assertEquals(setOf("a", "b", "d"), QuestionSelector.filter(all, f).map { it.id }.toSet())
        val f2 = f.copy(categories = setOf(QuestionCategory.LISTENING))
        assertEquals(listOf("d"), QuestionSelector.filter(all, f2).map { it.id })
        // 編碼成路由字串再解回來要一樣
        val round = QuestionSelector.Filter.decode(f2.encodeTags(), f2.encodeDifficulties(), f2.encodeCategories())
        assertEquals(f2, round)
        assertEquals(QuestionSelector.Filter(), QuestionSelector.Filter.decode(null, null, null))
    }

    @Test fun `stats summary`() {
        val bank = listOf(question("a", type = QuestionType.VOCABULARY), question("b"), listening("c", "M: x"))
        val attempts = listOf(
            AttemptEntity(1, "a", true, 0, 1000, "practice"),
            AttemptEntity(2, "a", false, 0, 1000, "practice"),
            AttemptEntity(3, "c", true, 0, 1000, "practice"),
            AttemptEntity(4, "gone", true, 0, 1000, "practice"), // 題庫已移除的題目
        )
        val cards = listOf(ReviewCardEntity("a", 4, 30, 2.5, 100, 0), ReviewCardEntity("c", 1, 1, 2.5, 100, 0))
        val today = 20_000L
        val days = listOf(StudyDayEntity(today, 3), StudyDayEntity(today - 1, 1), StudyDayEntity(today - 10, 5))
        val s = StatsCalculator.summarize(bank, attempts, cards, days, today)
        assertEquals(4, s.totalAttempts)
        assertEquals(0.75, s.accuracy, 1e-9)
        assertEquals(2, s.answeredQuestions)
        assertEquals(1, s.matureQuestions)
        assertEquals(2, s.streak.current)
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 3), s.last7Days.map { it.count })
        val vocab = s.byCategory.first { it.category == QuestionCategory.VOCABULARY }
        assertEquals(0.5, vocab.accuracy, 1e-9)
        assertEquals(1.0, vocab.coverage, 1e-9)
        assertTrue(s.byCategory.none { it.totalQuestions == 0 })
    }

    @Test fun `question stats from attempts`() {
        val st = QuestionStats.from(listOf(
            AttemptEntity(1, "a", true, 10, 0, "p"), AttemptEntity(2, "a", false, 20, 0, "p"),
        ))["a"]!!
        assertEquals(2, st.attempts); assertEquals(1, st.correct); assertEquals(20L, st.lastAnsweredAt)
    }
}
