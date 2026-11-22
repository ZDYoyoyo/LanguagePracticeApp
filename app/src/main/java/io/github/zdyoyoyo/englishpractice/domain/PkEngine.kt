package io.github.zdyoyoyo.englishpractice.domain

import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * 單機模擬 PK：和「預錄」的電腦對手比速度與正確率。
 * 對手的每題表現由 (對手, 題目, 日期) 決定的亂數種子事先產生成「劇本」，
 * 所以同一天同一組題目，對手的表現固定 —— 就像預先錄好的對戰紀錄。
 */
data class BotProfile(
    val id: String,
    val name: String,
    val emoji: String,
    val accuracy: Double,
    val meanMs: Long,
    val spreadMs: Long,
) {
    companion object {
        val ALL = listOf(
            BotProfile("rookie", "新手 Tom", "🐣", accuracy = 0.60, meanMs = 9_000, spreadMs = 3_000),
            BotProfile("steady", "穩健 Amy", "🦊", accuracy = 0.80, meanMs = 7_000, spreadMs = 2_500),
            BotProfile("master", "高手 Leo", "🦁", accuracy = 0.92, meanMs = 4_500, spreadMs = 1_500),
        )
        fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: ALL.first()
    }
}

data class BotMove(val correct: Boolean, val timeMs: Long)

object PkEngine {
    const val QUESTIONS_PER_MATCH = 5
    const val TIME_LIMIT_MS = 15_000L
    const val BASE_POINTS = 100
    const val MAX_SPEED_BONUS = 50

    fun botScript(bot: BotProfile, questionIds: List<String>, seed: Long): List<BotMove> {
        val random = Random(seed xor bot.id.hashCode().toLong() xor questionIds.hashCode().toLong())
        return questionIds.map {
            val correct = random.nextDouble() < bot.accuracy
            // 三個均勻亂數平均 ≈ 常態分佈，讓答題時間比較自然
            val noise = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0 * 2 - 1
            val time = (bot.meanMs + noise * bot.spreadMs).roundToLong().coerceIn(1_500, TIME_LIMIT_MS - 500)
            BotMove(correct, time)
        }
    }

    /** 答對 100 分 + 速度加成（越快越多，最多 50）；答錯或超時 0 分 */
    fun score(correct: Boolean, elapsedMs: Long): Int {
        if (!correct || elapsedMs >= TIME_LIMIT_MS) return 0
        val bonus = MAX_SPEED_BONUS * (1.0 - elapsedMs.toDouble() / TIME_LIMIT_MS)
        return BASE_POINTS + bonus.toInt().coerceIn(0, MAX_SPEED_BONUS)
    }

    enum class Outcome { WIN, LOSE, DRAW }

    data class Side(val score: Int = 0, val correct: Int = 0, val totalTimeMs: Long = 0) {
        fun add(correct: Boolean, timeMs: Long) = Side(
            score = score + score(correct, timeMs),
            correct = this.correct + if (correct) 1 else 0,
            totalTimeMs = totalTimeMs + timeMs,
        )
    }

    /** 先比分數；同分比答對題數；再同就比總時間（快者勝） */
    fun outcome(me: Side, bot: Side): Outcome = when {
        me.score != bot.score -> if (me.score > bot.score) Outcome.WIN else Outcome.LOSE
        me.correct != bot.correct -> if (me.correct > bot.correct) Outcome.WIN else Outcome.LOSE
        me.totalTimeMs != bot.totalTimeMs -> if (me.totalTimeMs < bot.totalTimeMs) Outcome.WIN else Outcome.LOSE
        else -> Outcome.DRAW
    }
}
