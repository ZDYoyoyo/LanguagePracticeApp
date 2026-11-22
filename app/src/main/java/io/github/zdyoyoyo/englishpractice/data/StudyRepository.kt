package io.github.zdyoyoyo.englishpractice.data

import io.github.zdyoyoyo.englishpractice.data.local.AttemptDao
import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.TransactionRunner
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerDao
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerEntity
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.flow.Flow

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
    private val clock: AppClock,
    /** 本機資料有變動時呼叫（雲端同步判斷「誰比較新」會用到） */
    private val onLocalDataChanged: (Long) -> Unit = {},
) {
    val activeWrongAnswers: Flow<List<WrongAnswerEntity>> = wrongDao.observeActive()
    val activeWrongCount: Flow<Int> = wrongDao.observeActiveCount()
    val totalAttempts: Flow<Int> = attemptDao.observeTotal()

    suspend fun activeWrongQuestionIds(): List<String> = wrongDao.getActive().map { it.questionId }

    /**
     * 記錄一次作答。規則：
     * - 每次作答都寫入 attempts
     * - 答錯：自動收錄進錯題本（已存在則錯誤次數 +1，且重新標為未掌握）
     * - 在「錯題本模式」答對：標記為已掌握，從錯題清單移除
     * - 其他模式答對：錯題本不變（要在錯題本重做答對才算掌握）
     */
    suspend fun recordAnswer(
        question: Question,
        userAnswer: String,
        correct: Boolean,
        elapsedMs: Long,
        mode: StudyMode,
    ) {
        val now = clock.nowMillis()
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
        }
        onLocalDataChanged(now)
    }

    /** 使用者手動從錯題本移除 */
    suspend fun removeFromWrongBook(questionId: String) {
        wrongDao.delete(questionId)
        onLocalDataChanged(clock.nowMillis())
    }
}
