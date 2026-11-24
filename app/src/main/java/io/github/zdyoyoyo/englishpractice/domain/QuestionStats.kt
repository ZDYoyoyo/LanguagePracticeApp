package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity

/** 每一題的作答統計（從作答紀錄算出來） */
data class QuestionStat(val attempts: Int, val correct: Int, val lastAnsweredAt: Long) {
    val accuracy: Double get() = if (attempts == 0) 0.0 else correct.toDouble() / attempts
}

object QuestionStats {
    fun from(attempts: List<AttemptEntity>): Map<String, QuestionStat> =
        attempts.groupBy { it.questionId }.mapValues { (_, list) ->
            QuestionStat(
                attempts = list.size,
                correct = list.count { it.correct },
                lastAnsweredAt = list.maxOf { it.answeredAt },
            )
        }
}
