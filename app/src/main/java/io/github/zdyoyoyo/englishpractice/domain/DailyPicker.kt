package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question

/**
 * 每日練習 = 「今日單字」1 題 + 「每日一題」綜合題 1 題（兩題不重複）。
 * 用日期決定，同一天整天固定、離線也算得出來；首頁、通知、作答畫面都用同一份結果。
 */
object DailyPicker {
    fun dailyWord(all: List<Question>, epochDay: Long): Question? =
        all.filter { !it.word.isNullOrBlank() }.sortedBy { it.id }
            .let { list -> list.getOrNull(index(epochDay + 7, list.size)) }

    fun dailyQuestion(all: List<Question>, epochDay: Long): Question? {
        val wordId = dailyWord(all, epochDay)?.id
        return all.filter { it.id != wordId }.sortedBy { it.id }
            .let { list -> list.getOrNull(index(epochDay, list.size)) }
    }

    /** 桌面小工具的「今日單字卡」：直接秀出中文，所以刻意和每日練習的單字錯開，避免先看到答案 */
    fun widgetWord(all: List<Question>, epochDay: Long): Question? {
        val dailyId = dailyWord(all, epochDay)?.id
        return all.filter { !it.word.isNullOrBlank() && it.id != dailyId }.sortedBy { it.id }
            .let { list -> list.getOrNull(index(epochDay + 101, list.size)) }
    }

    /** 每日練習的題目：先單字、再綜合題 */
    fun dailySet(all: List<Question>, epochDay: Long): List<Question> =
        listOfNotNull(dailyWord(all, epochDay), dailyQuestion(all, epochDay))

    /** 簡單的雜湊，讓連續幾天不會剛好選到相鄰的題目 */
    private fun index(epochDay: Long, size: Int): Int {
        if (size == 0) return -1
        val mixed = (epochDay * 2654435761L) xor (epochDay ushr 7)
        return Math.floorMod(mixed, size.toLong()).toInt()
    }
}
