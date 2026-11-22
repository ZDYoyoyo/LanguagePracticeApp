package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question

/** 每日一題 / 每日單字：用日期決定，同一天整天固定、離線也算得出來 */
object DailyPicker {
    fun dailyQuestion(all: List<Question>, epochDay: Long): Question? =
        all.sortedBy { it.id }.let { list -> list.getOrNull(index(epochDay, list.size)) }

    fun dailyWord(all: List<Question>, epochDay: Long): Question? =
        all.filter { !it.word.isNullOrBlank() }.sortedBy { it.id }
            .let { list -> list.getOrNull(index(epochDay + 7, list.size)) }

    /** 簡單的雜湊，讓連續幾天不會剛好選到相鄰的題目 */
    private fun index(epochDay: Long, size: Int): Int {
        if (size == 0) return -1
        val mixed = (epochDay * 2654435761L) xor (epochDay ushr 7)
        return Math.floorMod(mixed, size.toLong()).toInt()
    }
}
