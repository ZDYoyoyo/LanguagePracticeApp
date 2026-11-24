package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question
import kotlin.random.Random

/** 依主題 / 難度 / 題型挑題（純邏輯，方便測試） */
object QuestionSelector {
    const val DEFAULT_SESSION_SIZE = 10
    val SESSION_SIZES = listOf(10, 20, 30)

    data class Filter(
        val tag: String? = null,
        val difficulty: Int? = null,
        val category: QuestionCategory? = null,
    ) {
        fun matches(q: Question) =
            (tag == null || tag in q.tags) &&
                (difficulty == null || q.difficulty == difficulty) &&
                (category == null || q.category == category)
    }

    fun filter(all: List<Question>, filter: Filter): List<Question> = all.filter(filter::matches)

    /** 舊介面：單純隨機挑題 */
    fun select(
        all: List<Question>,
        tag: String? = null,
        difficulty: Int? = null,
        limit: Int = DEFAULT_SESSION_SIZE,
        random: Random = Random.Default,
    ): List<Question> = filter(all, Filter(tag, difficulty)).shuffled(random).take(limit)

    /**
     * 聰明選題：
     * 1. 同一篇閱讀 / 聽力（相同 passage）的題目視為一組，一起出、依題庫原本順序排列
     * 2. preferNew = true 時，優先順序：沒做過 → 正確率低（常錯）→ 正確率高；同一層內隨機
     * 3. 盡量湊滿 limit 題，但不會把同一組拆開（放不下的組會跳過，改找小一點的）
     */
    fun selectSmart(
        all: List<Question>,
        filter: Filter,
        limit: Int,
        stats: Map<String, QuestionStat>,
        preferNew: Boolean = true,
        random: Random = Random.Default,
    ): List<Question> {
        val units: List<List<Question>> = filter(all, filter)
            .groupBy { it.passage ?: "#${it.id}" }
            .values.toList()

        val ordered = if (!preferNew) {
            units.shuffled(random)
        } else {
            units.shuffled(random).sortedWith(compareBy({ priorityTier(it, stats) }, { avgAccuracy(it, stats) }))
        }

        val picked = mutableListOf<Question>()
        for (unit in ordered) {
            if (picked.size >= limit) break
            if (picked.size + unit.size <= limit) picked += unit
        }
        return picked
    }

    /** 0 = 組內有沒做過的題、1 = 都做過但有答錯過、2 = 全部都答對過 */
    private fun priorityTier(unit: List<Question>, stats: Map<String, QuestionStat>): Int = when {
        unit.any { (stats[it.id]?.attempts ?: 0) == 0 } -> 0
        unit.any { (stats[it.id]?.accuracy ?: 0.0) < 1.0 } -> 1
        else -> 2
    }

    private fun avgAccuracy(unit: List<Question>, stats: Map<String, QuestionStat>): Double =
        unit.map { stats[it.id]?.accuracy ?: 0.0 }.average()

    fun allTags(all: List<Question>): List<String> = all.flatMap { it.tags }.distinct().sorted()
}
