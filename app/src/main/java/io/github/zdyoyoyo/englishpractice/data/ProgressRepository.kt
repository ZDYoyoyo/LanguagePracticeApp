package io.github.zdyoyoyo.englishpractice.data

import io.github.zdyoyoyo.englishpractice.data.local.MockResultDao
import io.github.zdyoyoyo.englishpractice.data.local.MockResultEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayDao
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import io.github.zdyoyoyo.englishpractice.data.prefs.AppPreferences
import io.github.zdyoyoyo.englishpractice.domain.MockScore
import io.github.zdyoyoyo.englishpractice.domain.StreakCalculator
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * 每日目標、連勝保護、模擬考成績。
 * 連勝保護的做法：在漏掉的那天寫一筆「作答 0 題」的打卡紀錄，連續天數就不會中斷
 * （統計頁的「學習天數」只算有作答的天）。
 */
class ProgressRepository(
    private val prefs: AppPreferences,
    private val dayDao: StudyDayDao,
    private val mockDao: MockResultDao,
    private val clock: AppClock,
    private val onLocalDataChanged: (Long) -> Unit = {},
) {
    private val _dailyGoal = MutableStateFlow(prefs.dailyGoal)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()
    private val _freezes = MutableStateFlow(prefs.streakFreezes)
    val freezes: StateFlow<Int> = _freezes.asStateFlow()

    fun setDailyGoal(goal: Int) {
        prefs.dailyGoal = goal
        _dailyGoal.value = goal
    }

    fun observeAnsweredOn(epochDay: Long): Flow<Int> = dayDao.observeDay(epochDay).map { it?.answeredCount ?: 0 }

    fun observeRepairableDay(today: Long): Flow<Long?> =
        dayDao.observeDays().map { StreakCalculator.repairableDay(it, today) }

    /** 看完獎勵廣告：+1 次連勝保護（最多 [MAX_FREEZES] 次） */
    fun addFreeze() = setFreezes((prefs.streakFreezes + 1).coerceAtMost(MAX_FREEZES))

    /** 用一次連勝保護補回漏掉的那天；成功回傳 true */
    suspend fun useFreeze(): Boolean {
        if (prefs.streakFreezes <= 0) return false
        val today = clock.today().toEpochDay()
        val day = StreakCalculator.repairableDay(dayDao.getDays(), today) ?: return false
        dayDao.upsert(StudyDayEntity(day, 0))
        setFreezes(prefs.streakFreezes - 1)
        return true
    }

    /** 從雲端還原或清除資料後重新讀取 */
    fun reload() {
        _freezes.value = prefs.streakFreezes
        _dailyGoal.value = prefs.dailyGoal
    }

    private fun setFreezes(n: Int) {
        prefs.streakFreezes = n
        _freezes.value = n
        onLocalDataChanged(clock.nowMillis())
    }

    // ---- 模擬考 ----
    fun observeMockResults(limit: Int = 20): Flow<List<MockResultEntity>> = mockDao.observeRecent(limit)

    suspend fun saveMockResult(score: MockScore, durationMs: Long): MockResultEntity {
        val entity = MockResultEntity(
            takenAt = clock.nowMillis(),
            totalQuestions = score.totalQuestions,
            listeningCorrect = score.listeningCorrect,
            listeningTotal = score.listeningTotal,
            readingCorrect = score.readingCorrect,
            readingTotal = score.readingTotal,
            estimatedScore = score.estimatedTotal,
            durationMs = durationMs,
        )
        val id = mockDao.insert(entity)
        onLocalDataChanged(clock.nowMillis())
        return entity.copy(id = id)
    }

    companion object {
        const val MAX_FREEZES = 2
        val GOAL_OPTIONS = listOf(5, 10, 20, 30)
    }
}
