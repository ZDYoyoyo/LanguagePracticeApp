package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculatorTest {
    private val today = 20_000L

    @Test fun `no study days means zero`() {
        assertEquals(StreakCalculator.Streak(0, 0, false), StreakCalculator.compute(emptyList(), today))
    }

    @Test fun `consecutive days including today`() {
        val s = StreakCalculator.compute(listOf(today - 2, today - 1, today), today)
        assertEquals(3, s.current)
        assertTrue(s.studiedToday)
    }

    @Test fun `not yet studied today keeps yesterday streak`() {
        val s = StreakCalculator.compute(listOf(today - 2, today - 1), today)
        assertEquals(2, s.current)
        assertFalse(s.studiedToday)
    }

    @Test fun `missing a day resets to zero`() {
        val s = StreakCalculator.compute(listOf(today - 5, today - 4, today - 3, today - 2), today)
        assertEquals(0, s.current)
        assertEquals(4, s.longest)
    }

    @Test fun `gap restarts the count`() {
        val s = StreakCalculator.compute(listOf(today - 10, today - 9, today - 1, today), today)
        assertEquals(2, s.current)
        assertEquals(2, s.longest)
    }
}
