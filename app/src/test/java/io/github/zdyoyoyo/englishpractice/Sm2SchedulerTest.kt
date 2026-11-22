package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.domain.Sm2Scheduler
import io.github.zdyoyoyo.englishpractice.domain.Sm2Scheduler.State
import org.junit.Assert.assertEquals
import org.junit.Test

class Sm2SchedulerTest {
    private val eps = 1e-9

    @Test fun `first correct review gives 1 day then 6 days then interval times ease`() {
        var s = Sm2Scheduler.review(State(), 5)
        assertEquals(1, s.repetitions); assertEquals(1, s.intervalDays); assertEquals(2.6, s.easeFactor, eps)
        s = Sm2Scheduler.review(s, 5)
        assertEquals(2, s.repetitions); assertEquals(6, s.intervalDays); assertEquals(2.7, s.easeFactor, eps)
        s = Sm2Scheduler.review(s, 5)
        // 第三次：6 * 2.7（使用「更新前」的 EF）= 16.2 → 16
        assertEquals(3, s.repetitions); assertEquals(16, s.intervalDays)
    }

    @Test fun `quality 4 keeps ease unchanged`() {
        val s = Sm2Scheduler.review(State(easeFactor = 2.5), 4)
        assertEquals(2.5, s.easeFactor, eps)
    }

    @Test fun `quality 3 lowers ease by 0_14`() {
        val s = Sm2Scheduler.review(State(easeFactor = 2.5), 3)
        assertEquals(2.36, s.easeFactor, eps)
        assertEquals(1, s.intervalDays)
    }

    @Test fun `failing resets repetitions and schedules tomorrow`() {
        val learned = State(repetitions = 4, intervalDays = 40, easeFactor = 2.5)
        val s = Sm2Scheduler.review(learned, 1)
        assertEquals(0, s.repetitions)
        assertEquals(1, s.intervalDays)
        assertEquals(2.5 - 0.54, s.easeFactor, eps)
    }

    @Test fun `ease never drops below 1_3`() {
        var s = State()
        repeat(20) { s = Sm2Scheduler.review(s, 0) }
        assertEquals(Sm2Scheduler.MIN_EASE, s.easeFactor, eps)
    }

    @Test fun `quality mapping`() {
        assertEquals(1, Sm2Scheduler.qualityFor(correct = false, elapsedMs = 1000))
        assertEquals(5, Sm2Scheduler.qualityFor(correct = true, elapsedMs = 3000))
        assertEquals(4, Sm2Scheduler.qualityFor(correct = true, elapsedMs = 20_000))
        assertEquals(3, Sm2Scheduler.qualityFor(correct = true, elapsedMs = 1000, usedHint = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid quality throws`() {
        Sm2Scheduler.review(State(), 6)
    }
}
