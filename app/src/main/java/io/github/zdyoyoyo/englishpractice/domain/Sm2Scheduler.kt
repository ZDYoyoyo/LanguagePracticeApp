package io.github.zdyoyoyo.englishpractice.domain

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SM-2 間隔重複演算法（SuperMemo 2，Anki 的前身）。純計算、無副作用，方便測試。
 *
 * quality（作答品質）0~5：
 *   5 = 很快且正確、4 = 正確但想了一下、3 = 勉強答對（例如用了提示）
 *   0~2 = 答錯 → 重新開始（明天再複習）
 */
object Sm2Scheduler {
    const val INITIAL_EASE = 2.5
    const val MIN_EASE = 1.3
    /** 超過這個秒數才答對，視為「想了一下」 */
    const val SLOW_ANSWER_MS = 15_000L

    data class State(
        val repetitions: Int = 0,
        val intervalDays: Int = 0,
        val easeFactor: Double = INITIAL_EASE,
    )

    fun review(state: State, quality: Int): State {
        require(quality in 0..5) { "quality must be 0..5" }
        val newEase = max(
            MIN_EASE,
            state.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)),
        )
        if (quality < 3) {
            return State(repetitions = 0, intervalDays = 1, easeFactor = newEase)
        }
        val interval = when (state.repetitions) {
            0 -> 1
            1 -> 6
            else -> (state.intervalDays * state.easeFactor).roundToInt().coerceAtLeast(1)
        }
        return State(repetitions = state.repetitions + 1, intervalDays = interval, easeFactor = newEase)
    }

    /** 把 App 裡的作答結果轉成 SM-2 的 quality */
    fun qualityFor(correct: Boolean, elapsedMs: Long, usedHint: Boolean = false): Int = when {
        !correct -> 1
        usedHint -> 3
        elapsedMs > SLOW_ANSWER_MS -> 4
        else -> 5
    }
}
