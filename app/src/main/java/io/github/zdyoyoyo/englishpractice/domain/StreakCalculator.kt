package io.github.zdyoyoyo.englishpractice.domain

/**
 * 連續打卡計算。只要當天作答 ≥ 1 題就算打卡。
 * - 今天已打卡：從今天往回數連續天數
 * - 今天還沒打卡但昨天有：streak 仍保留（今天還有機會），studiedToday = false
 * - 昨天和今天都沒打卡：中斷，歸零
 */
object StreakCalculator {
    data class Streak(val current: Int, val longest: Int, val studiedToday: Boolean)

    /** @param studyDays 有作答的日期（以 epochDay 表示，LocalDate.toEpochDay()） */
    fun compute(studyDays: Collection<Long>, today: Long): Streak {
        val days = studyDays.toSortedSet()
        val studiedToday = today in days
        var current = 0
        var cursor = if (studiedToday) today else today - 1
        while (cursor in days) {
            current++
            cursor--
        }
        var longest = 0
        var run = 0
        var prev: Long? = null
        for (d in days) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            longest = maxOf(longest, run)
            prev = d
        }
        return Streak(current, longest, studiedToday)
    }
}
