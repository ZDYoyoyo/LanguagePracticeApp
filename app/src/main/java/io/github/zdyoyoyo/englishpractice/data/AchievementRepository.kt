package io.github.zdyoyoyo.englishpractice.data

import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.local.MockResultDao
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerDao
import io.github.zdyoyoyo.englishpractice.data.prefs.AppPreferences
import io.github.zdyoyoyo.englishpractice.domain.Achievement
import io.github.zdyoyoyo.englishpractice.domain.AchievementContext
import io.github.zdyoyoyo.englishpractice.domain.Achievements
import io.github.zdyoyoyo.englishpractice.domain.QuestionCategory
import io.github.zdyoyoyo.englishpractice.domain.StreakCalculator
import io.github.zdyoyoyo.englishpractice.domain.category
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 成就：從本機資料計算進度，達成時記下解鎖時間（存在設定裡，也會一起備份） */
class AchievementRepository(
    private val prefs: AppPreferences,
    private val bank: QuestionBankRepository,
    private val study: StudyRepository,
    private val wrongDao: WrongAnswerDao,
    private val mockDao: MockResultDao,
    private val clock: AppClock,
    private val onLocalDataChanged: (Long) -> Unit = {},
) {
    private val mutex = Mutex()
    private val _unlocked = MutableStateFlow(prefs.achievements)
    val unlocked: StateFlow<Map<String, Long>> = _unlocked.asStateFlow()

    /** 一輪（5 題以上）全對時呼叫 */
    fun recordPerfectRound() {
        prefs.perfectRounds = prefs.perfectRounds + 1
    }

    suspend fun context(): AchievementContext {
        val questions = bank.ensureLoaded().questions
        val byId = questions.associateBy { it.id }
        val src = study.statsSource()
        val correctAttempts = src.attempts.filter { it.correct }
        fun correctIn(cat: QuestionCategory) = correctAttempts.count { byId[it.questionId]?.category == cat }
        val mocks = mockDao.getAll()
        val answeredIds = src.attempts.map { it.questionId }.toSet()
        return AchievementContext(
            totalAttempts = src.attempts.size,
            totalCorrect = correctAttempts.size,
            longestStreak = StreakCalculator.compute(src.days.map { it.epochDay }, clock.today().toEpochDay()).longest,
            goalDays = src.days.count { it.answeredCount >= prefs.dailyGoal },
            perfectRounds = prefs.perfectRounds,
            mockCount = mocks.size,
            bestMockScore = mocks.maxOfOrNull { it.estimatedScore } ?: 0,
            listeningCorrect = correctIn(QuestionCategory.LISTENING),
            readingCorrect = correctIn(QuestionCategory.READING),
            vocabPracticed = answeredIds.count { byId[it]?.category == QuestionCategory.VOCABULARY },
            masteredWrong = wrongDao.masteredCount(),
            coveragePercent = if (questions.isEmpty()) 0 else answeredIds.count { it in byId } * 100 / questions.size,
        )
    }

    /** 檢查有沒有新達成的成就；有就記錄並回傳 */
    suspend fun checkAndUnlock(): List<Achievement> = mutex.withLock {
        val already = prefs.achievements
        val fresh = Achievements.newlyUnlocked(context(), already.keys)
        if (fresh.isNotEmpty()) {
            val now = clock.nowMillis()
            val updated = already + fresh.associate { it.id to now }
            prefs.achievements = updated
            _unlocked.value = updated
            onLocalDataChanged(now)
        }
        fresh
    }

    /** 從雲端還原或清除資料後重新讀取 */
    fun reload() {
        _unlocked.value = prefs.achievements
    }
}
