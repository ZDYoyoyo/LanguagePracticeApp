package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 迷你模擬考：照多益的比例出題（聽力一半、閱讀一半），先聽力、後閱讀，和正式考試順序一樣。
 * 閱讀部分：約 3 成是 Part 5 單句題（單字、文法），其餘是 Part 6/7 短文題。
 */
object MockTestBuilder {
    val SIZES = listOf(20, 40)
    /** 每題平均可用秒數（正式多益約 2 小時 200 題 = 36 秒／題，這裡放寬一點） */
    const val SECONDS_PER_QUESTION = 45

    fun timeLimitMs(questionCount: Int): Long = questionCount * SECONDS_PER_QUESTION * 1000L

    fun build(
        all: List<Question>,
        size: Int,
        stats: Map<String, QuestionStat>,
        random: Random = Random.Default,
    ): List<Question> {
        val usable = all.filter { !it.isTypingQuestion }
        val listeningTarget = size / 2
        val part5Target = ((size - listeningTarget) * 0.3).roundToInt()
        val readingTarget = size - listeningTarget - part5Target

        fun pick(categories: Set<QuestionCategory>, n: Int, exclude: Set<String>) =
            QuestionSelector.selectSmart(
                usable.filter { it.id !in exclude },
                QuestionSelector.Filter(categories = categories), n, stats, preferNew = true, random = random,
            )

        val listening = pick(setOf(QuestionCategory.LISTENING), listeningTarget, emptySet())
        val reading = pick(setOf(QuestionCategory.READING), readingTarget, emptySet())
        // 短文題因為整組出不一定剛好湊滿，差額用 Part 5 單句題補
        val part5Need = size - listening.size - reading.size
        val part5 = pick(setOf(QuestionCategory.VOCABULARY, QuestionCategory.GRAMMAR), part5Need, emptySet())
        return listening + part5 + reading
    }
}

/** 粗估多益分數（非官方換算，僅供參考） */
data class MockScore(
    val listeningCorrect: Int,
    val listeningTotal: Int,
    val readingCorrect: Int,
    val readingTotal: Int,
) {
    val totalQuestions: Int get() = listeningTotal + readingTotal
    val totalCorrect: Int get() = listeningCorrect + readingCorrect
    val listeningScore: Int get() = section(listeningCorrect, listeningTotal)
    val readingScore: Int get() = section(readingCorrect, readingTotal)
    val estimatedTotal: Int get() = (listeningScore + readingScore).coerceIn(10, 990)

    /**
     * 題數少時誤差很大，用統計上的標準誤當作區間（約 68% 信心）：990 × √(p(1−p)/n)
     * p 太接近 0 或 1 時至少用 0.1，避免區間變成 0。
     */
    val margin: Int get() {
        if (totalQuestions == 0) return 0
        val p = totalCorrect.toDouble() / totalQuestions
        return round5(990 * sqrt(max(p * (1 - p), 0.1) / totalQuestions))
    }
    val low: Int get() = (estimatedTotal - margin).coerceAtLeast(10)
    val high: Int get() = (estimatedTotal + margin).coerceAtMost(990)

    companion object {
        /** 每個部分 5～495 分，依正確率線性換算、取 5 的倍數 */
        fun section(correct: Int, total: Int): Int =
            if (total == 0) 5 else round5(5 + 490.0 * correct / total).coerceIn(5, 495)

        private fun round5(v: Double): Int = (v / 5).roundToInt() * 5

        fun from(questions: List<Question>, correctIds: Set<String>): MockScore {
            val (listening, reading) = questions.partition { it.category == QuestionCategory.LISTENING }
            return MockScore(
                listeningCorrect = listening.count { it.id in correctIds },
                listeningTotal = listening.size,
                readingCorrect = reading.count { it.id in correctIds },
                readingTotal = reading.size,
            )
        }
    }
}
