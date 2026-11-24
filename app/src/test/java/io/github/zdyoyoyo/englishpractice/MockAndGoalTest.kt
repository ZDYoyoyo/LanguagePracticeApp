package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.model.QuestionType
import io.github.zdyoyoyo.englishpractice.domain.MockScore
import io.github.zdyoyoyo.englishpractice.domain.MockTestBuilder
import io.github.zdyoyoyo.englishpractice.domain.QuestionCategory
import io.github.zdyoyoyo.englishpractice.domain.StreakCalculator
import io.github.zdyoyoyo.englishpractice.domain.category
import io.github.zdyoyoyo.englishpractice.fakes.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MockAndGoalTest {
    private val bank = buildList {
        // 聽力：10 組對話（每組 3 題）+ 20 題單句
        repeat(10) { g -> repeat(3) { i -> add(question("l$g-$i").copy(passage = "M: dialogue $g", listening = true)) } }
        repeat(20) { add(question("p2-$it").copy(passage = "W: line $it", listening = true)) }
        // 閱讀：8 篇短文（每篇 3 題）
        repeat(8) { g -> repeat(3) { i -> add(question("r$g-$i").copy(passage = "Reading $g")) } }
        // Part 5：單字、文法；以及不能出現在模擬考的輸入題
        repeat(30) { add(question("v$it", type = QuestionType.VOCABULARY)) }
        repeat(30) { add(question("g$it", type = QuestionType.GRAMMAR)) }
        repeat(10) { add(question("c$it", type = QuestionType.CLOZE, options = emptyList())) }
    }

    @Test fun `mock test has right size, listening first, no typing questions`() {
        MockTestBuilder.SIZES.forEach { n ->
            repeat(10) { seed ->
                val qs = MockTestBuilder.build(bank, n, emptyMap(), Random(seed))
                assertEquals(n, qs.size)
                assertEquals(n, qs.map { it.id }.toSet().size)
                assertTrue(qs.none { it.isTypingQuestion })
                val cats = qs.map { it.category }
                val lastListening = cats.lastIndexOf(QuestionCategory.LISTENING)
                val firstOther = cats.indexOfFirst { it != QuestionCategory.LISTENING }
                assertTrue("聽力要全部在前面", lastListening < firstOther)
                assertEquals(n / 2, cats.count { it == QuestionCategory.LISTENING })
            }
        }
    }

    @Test fun `mock test never splits a passage group`() {
        val qs = MockTestBuilder.build(bank, 20, emptyMap(), Random(3))
        qs.filter { it.passage != null }.groupBy { it.passage }.forEach { (passage, members) ->
            val inBank = bank.count { it.passage == passage }
            assertEquals("同一篇要整組出現：$passage", inBank, members.size)
        }
    }

    @Test fun `score conversion`() {
        val perfect = MockScore(10, 10, 10, 10)
        assertEquals(495, perfect.listeningScore)
        assertEquals(990, perfect.estimatedTotal)
        val zero = MockScore(0, 10, 0, 10)
        assertEquals(10, zero.estimatedTotal)
        val half = MockScore(5, 10, 5, 10)
        assertEquals(250, half.listeningScore)
        assertEquals(500, half.estimatedTotal)
        assertEquals(110, half.margin)            // 990 × √(0.25/20) ≈ 110.7 → 110
        assertEquals(390, half.low); assertEquals(610, half.high)
        assertTrue(MockScore(10, 20, 10, 20).margin < half.margin) // 題數越多誤差越小
        assertTrue(perfect.high <= 990 && zero.low >= 10)
    }

    @Test fun `score from questions splits listening and reading`() {
        val qs = listOf(
            question("l1").copy(passage = "M: x", listening = true), question("l2").copy(passage = "M: y", listening = true),
            question("r1").copy(passage = "text"), question("v1", type = QuestionType.VOCABULARY),
        )
        val s = MockScore.from(qs, setOf("l1", "v1"))
        assertEquals(MockScore(1, 2, 1, 2), s)
    }

    @Test fun `streak repair only for a single missed yesterday`() {
        val today = 20_000L
        assertEquals(today - 1, StreakCalculator.repairableDay(listOf(today - 3, today - 2), today))
        assertEquals(today - 1, StreakCalculator.repairableDay(listOf(today - 2, today), today))
        assertNull(StreakCalculator.repairableDay(listOf(today - 2, today - 1), today)) // 沒漏
        assertNull(StreakCalculator.repairableDay(listOf(today - 5), today))            // 漏太多天
        assertNull(StreakCalculator.repairableDay(emptyList(), today))
        // 補回後連勝恢復
        val repaired = listOf(today - 3, today - 2, today - 1, today)
        assertEquals(4, StreakCalculator.compute(repaired, today).current)
    }
}
