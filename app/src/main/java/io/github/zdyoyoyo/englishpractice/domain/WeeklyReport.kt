package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 每週學習報告：本週（週一～今天）和上週比較 */
object WeeklyReport {
    data class Week(val answers: Int, val correct: Int, val studyDays: Int) {
        val accuracy: Double get() = if (answers == 0) 0.0 else correct.toDouble() / answers
    }

    data class Report(val thisWeek: Week, val lastWeek: Week) {
        val answersDiff: Int get() = thisWeek.answers - lastWeek.answers
        /** 正確率差距（百分點）；任一週沒作答則為 null */
        val accuracyDiffPoints: Int?
            get() = if (thisWeek.answers == 0 || lastWeek.answers == 0) null
            else ((thisWeek.accuracy - lastWeek.accuracy) * 100).toInt()

        /** 通知用的一行摘要 */
        fun summary(): String = buildString {
            append("本週作答 ${thisWeek.answers} 題")
            if (lastWeek.answers > 0) append("（${sign(answersDiff)}）")
            if (thisWeek.answers > 0) {
                append("・正確率 ${(thisWeek.accuracy * 100).toInt()}%")
                accuracyDiffPoints?.let { append("（${sign(it)}%）") }
            }
            append("・學習 ${thisWeek.studyDays} 天")
        }

        private fun sign(n: Int) = if (n >= 0) "+$n" else "$n"
    }

    fun compute(
        attempts: List<AttemptEntity>,
        days: List<StudyDayEntity>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Report {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastMonday = monday.minusWeeks(1)
        fun dateOf(a: AttemptEntity) = Instant.ofEpochMilli(a.answeredAt).atZone(zone).toLocalDate()
        fun week(from: LocalDate, toInclusive: LocalDate): Week {
            val inRange = attempts.filter { dateOf(it) in from..toInclusive }
            val studied = days.count { it.answeredCount > 0 && LocalDate.ofEpochDay(it.epochDay) in from..toInclusive }
            return Week(inRange.size, inRange.count { it.correct }, studied)
        }
        return Report(week(monday, today), week(lastMonday, monday.minusDays(1)))
    }

    /** 下一個週日晚上 8 點（現在已過就排下週） */
    fun nextReportTime(now: LocalDateTime): LocalDateTime {
        var t = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.of(20, 0))
        if (!t.isAfter(now)) t = t.plusWeeks(1)
        return t
    }
}
