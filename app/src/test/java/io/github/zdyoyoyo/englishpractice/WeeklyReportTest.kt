package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import io.github.zdyoyoyo.englishpractice.domain.WeeklyReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class WeeklyReportTest {
    private val zone = ZoneOffset.UTC
    private fun at(date: LocalDate, correct: Boolean) =
        AttemptEntity(0, "q", correct, date.atTime(10, 0).toInstant(zone).toEpochMilli(), 1000, "p")

    // 2026-11-25 是週三；本週一 = 11/23，上週一 = 11/16
    private val today = LocalDate.of(2026, 11, 25)

    @Test fun `this week vs last week`() {
        val attempts = listOf(
            at(LocalDate.of(2026, 11, 23), true), at(LocalDate.of(2026, 11, 24), true),
            at(LocalDate.of(2026, 11, 25), false), at(LocalDate.of(2026, 11, 25), true),  // 本週 4 題、3 對
            at(LocalDate.of(2026, 11, 16), true), at(LocalDate.of(2026, 11, 22), false),  // 上週 2 題、1 對
            at(LocalDate.of(2026, 11, 10), true),                                          // 更早，不算
        )
        val days = listOf(23L, 24L, 25L, 16L, 22L).map { StudyDayEntity(LocalDate.of(2026, 11, it.toInt()).toEpochDay(), 1) } +
            StudyDayEntity(LocalDate.of(2026, 11, 21).toEpochDay(), 0) // 連勝保護補的日子不算學習天
        val r = WeeklyReport.compute(attempts, days, today, zone)
        assertEquals(4, r.thisWeek.answers); assertEquals(3, r.thisWeek.correct); assertEquals(3, r.thisWeek.studyDays)
        assertEquals(2, r.lastWeek.answers); assertEquals(2, r.lastWeek.studyDays)
        assertEquals(2, r.answersDiff)
        assertEquals(25, r.accuracyDiffPoints)
        val text = r.summary()
        assertTrue(text, "本週作答 4 題（+2）" in text && "75%（+25%）" in text && "學習 3 天" in text)
    }

    @Test fun `empty weeks`() {
        val r = WeeklyReport.compute(emptyList(), emptyList(), today, zone)
        assertEquals(0, r.thisWeek.answers)
        assertNull(r.accuracyDiffPoints)
        assertEquals("本週作答 0 題・學習 0 天", r.summary())
    }

    @Test fun `next report is the coming Sunday 8pm`() {
        assertEquals(LocalDateTime.of(2026, 11, 29, 20, 0), WeeklyReport.nextReportTime(LocalDateTime.of(2026, 11, 25, 9, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 29, 20, 0), WeeklyReport.nextReportTime(LocalDateTime.of(2026, 11, 29, 19, 59)))
        assertEquals(LocalDateTime.of(2026, 12, 6, 20, 0), WeeklyReport.nextReportTime(LocalDateTime.of(2026, 11, 29, 20, 0)))
    }
}
