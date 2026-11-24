package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import io.github.zdyoyoyo.englishpractice.data.model.Question

/** 學習統計（純計算，方便測試） */
object StatsCalculator {
    /** 複習間隔 ≥ 21 天視為「已熟練」（和 Anki 的 mature card 定義相同） */
    const val MATURE_INTERVAL_DAYS = 21

    data class CategoryStat(
        val category: QuestionCategory,
        val totalQuestions: Int,
        val answeredQuestions: Int,
        val attempts: Int,
        val correct: Int,
    ) {
        val accuracy: Double get() = if (attempts == 0) 0.0 else correct.toDouble() / attempts
        val coverage: Double get() = if (totalQuestions == 0) 0.0 else answeredQuestions.toDouble() / totalQuestions
    }

    data class DayCount(val epochDay: Long, val count: Int)

    data class Summary(
        val totalAttempts: Int,
        val totalCorrect: Int,
        val answeredQuestions: Int,
        val totalQuestions: Int,
        val matureQuestions: Int,
        val studyDays: Int,
        val streak: StreakCalculator.Streak,
        val byCategory: List<CategoryStat>,
        val last7Days: List<DayCount>,
    ) {
        val accuracy: Double get() = if (totalAttempts == 0) 0.0 else totalCorrect.toDouble() / totalAttempts
    }

    fun summarize(
        bank: List<Question>,
        attempts: List<AttemptEntity>,
        cards: List<ReviewCardEntity>,
        days: List<StudyDayEntity>,
        todayEpochDay: Long,
    ): Summary {
        val bankIds = bank.associateBy { it.id }
        val byQuestion = attempts.groupBy { it.questionId }
        val byCategory = QuestionCategory.entries.map { cat ->
            val qs = bank.filter { it.category == cat }
            val catAttempts = qs.flatMap { byQuestion[it.id].orEmpty() }
            CategoryStat(
                category = cat,
                totalQuestions = qs.size,
                answeredQuestions = qs.count { byQuestion.containsKey(it.id) },
                attempts = catAttempts.size,
                correct = catAttempts.count { it.correct },
            )
        }.filter { it.totalQuestions > 0 }
        val dayMap = days.associate { it.epochDay to it.answeredCount }
        return Summary(
            totalAttempts = attempts.size,
            totalCorrect = attempts.count { it.correct },
            answeredQuestions = byQuestion.keys.count { it in bankIds },
            totalQuestions = bank.size,
            matureQuestions = cards.count { it.intervalDays >= MATURE_INTERVAL_DAYS && it.questionId in bankIds },
            studyDays = days.size,
            streak = StreakCalculator.compute(days.map { it.epochDay }, todayEpochDay),
            byCategory = byCategory,
            last7Days = (6 downTo 0).map { back ->
                val d = todayEpochDay - back
                DayCount(d, dayMap[d] ?: 0)
            },
        )
    }
}
