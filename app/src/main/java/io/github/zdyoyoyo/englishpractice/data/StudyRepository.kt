package io.github.zdyoyoyo.englishpractice.data

import io.github.zdyoyoyo.englishpractice.data.local.AttemptDao
import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardDao
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayDao
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import io.github.zdyoyoyo.englishpractice.data.local.TransactionRunner
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerDao
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerEntity
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.domain.Sm2Scheduler
import io.github.zdyoyoyo.englishpractice.domain.StreakCalculator
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 作答模式：決定作答後要更新哪些本機資料 */
enum class StudyMode(val key: String) {
    PRACTICE("practice"),
    WRONG_BOOK("wrong"),
    REVIEW("review"),
    DAILY("daily"),
    PK("pk");

    companion object {
        fun fromKey(key: String?): StudyMode = entries.firstOrNull { it.key == key } ?: PRACTICE
    }
}

/**
 * 使用者學習資料的唯一入口（Repository 模式）。
 * UI / ViewModel 只呼叫這裡，不直接碰 DAO，之後換儲存方式也不影響畫面。
 */
class StudyRepository(
    private val tx: TransactionRunner,
    private val attemptDao: AttemptDao,
    private val wrongDao: WrongAnswerDao,
    private val reviewDao: ReviewCardDao,
    private val dayDao: StudyDayDao,
    private val clock: AppClock,
    /** 本機資料有變動時呼叫（雲端同步判斷「誰比較新」會用到） */
    private val onLocalDataChanged: (Long) -> Unit = {},
) {
    val activeWrongAnswers: Flow<List<WrongAnswerEntity>> = wrongDao.observeActive()
    val activeWrongCount: Flow<Int> = wrongDao.observeActiveCount()
    val totalAttempts: Flow<Int> = attemptDao.observeTotal()

    fun observeDueCount(todayEpochDay: Long): Flow<Int> = reviewDao.observeDueCount(todayEpochDay)

    fun observeStreak(todayEpochDay: Long): Flow<StreakCalculator.Streak> =
        dayDao.observeDays().map { StreakCalculator.compute(it, todayEpochDay) }

    suspend fun streakNow(): StreakCalculator.Streak =
        StreakCalculator.compute(dayDao.getDays(), clock.today().toEpochDay())

    suspend fun dueCountNow(): Int = reviewDao.dueCount(clock.today().toEpochDay())

    suspend fun activeWrongQuestionIds(): List<String> = wrongDao.getActive().map { it.questionId }

    suspend fun dueQuestionIds(limit: Int = 20): List<String> =
        reviewDao.getDue(clock.today().toEpochDay(), limit).map { it.questionId }

    /**
     * 記錄一次作答。規則：
     * 1. 每次作答都寫入 attempts，並記錄今天已打卡
     * 2. 錯題本：
     *    - 答錯：自動收錄（已存在則錯誤次數 +1，且重新標為未掌握）
     *    - 在「錯題本模式」答對：標記為已掌握，從錯題清單移除
     *    - 其他模式答對：錯題本不變
     * 3. SM-2 複習卡：見 [updateReviewCard]
     */
    suspend fun recordAnswer(
        question: Question,
        userAnswer: String,
        correct: Boolean,
        elapsedMs: Long,
        mode: StudyMode,
        usedHint: Boolean = false,
    ) {
        val now = clock.nowMillis()
        val today = clock.today().toEpochDay()
        tx.run {
            attemptDao.insert(
                AttemptEntity(
                    questionId = question.id,
                    correct = correct,
                    answeredAt = now,
                    elapsedMs = elapsedMs,
                    mode = mode.key,
                )
            )
            val day = dayDao.get(today)
            dayDao.upsert(StudyDayEntity(today, (day?.answeredCount ?: 0) + 1))

            val existing = wrongDao.get(question.id)
            if (!correct) {
                wrongDao.upsert(
                    WrongAnswerEntity(
                        questionId = question.id,
                        wrongCount = (existing?.wrongCount ?: 0) + 1,
                        lastWrongAnswer = userAnswer,
                        lastWrongAt = now,
                        mastered = false,
                        updatedAt = now,
                    )
                )
            } else if (mode == StudyMode.WRONG_BOOK && existing != null && !existing.mastered) {
                wrongDao.upsert(existing.copy(mastered = true, updatedAt = now))
            }

            updateReviewCard(question.id, correct, elapsedMs, usedHint, mode, today, now)
        }
        onLocalDataChanged(now)
    }

    /**
     * SM-2 排程規則：
     * - 第一次作答的題目：建立複習卡並依結果排程
     * - 已到期（或複習模式）：依結果套用 SM-2
     * - 尚未到期就提前答對：不改排程（避免同一天狂刷把間隔拉太長）
     * - 尚未到期但答錯：視為遺忘，重新排程（明天複習）
     */
    private suspend fun updateReviewCard(
        questionId: String, correct: Boolean, elapsedMs: Long, usedHint: Boolean,
        mode: StudyMode, today: Long, now: Long,
    ) {
        val card = reviewDao.get(questionId)
        val isDue = card == null || card.dueEpochDay <= today || mode == StudyMode.REVIEW
        if (!isDue && correct) return

        val before = card?.let { Sm2Scheduler.State(it.repetitions, it.intervalDays, it.easeFactor) }
            ?: Sm2Scheduler.State()
        val after = Sm2Scheduler.review(before, Sm2Scheduler.qualityFor(correct, elapsedMs, usedHint))
        reviewDao.upsert(
            ReviewCardEntity(
                questionId = questionId,
                repetitions = after.repetitions,
                intervalDays = after.intervalDays,
                easeFactor = after.easeFactor,
                dueEpochDay = today + after.intervalDays,
                lastReviewedAt = now,
            )
        )
    }

    /** 使用者手動從錯題本移除 */
    suspend fun removeFromWrongBook(questionId: String) {
        wrongDao.delete(questionId)
        onLocalDataChanged(clock.nowMillis())
    }
}
