package io.github.zdyoyoyo.englishpractice.ads

/**
 * 插頁廣告頻率規則（純邏輯，方便測試與調整）。原則：寧可少放，不打斷學習。
 * - 只在「完成一輪題目」後才可能出現，作答途中絕不出現
 * - 剛安裝的前 [FREE_SESSIONS] 輪不放廣告，讓新使用者先體驗
 * - 每 [SESSIONS_BETWEEN_ADS] 輪最多一次
 * - 兩次之間至少間隔 [MIN_INTERVAL_MS]
 * - 太短的一輪（例如每日一題）不放
 */
object AdPolicy {
    const val FREE_SESSIONS = 2
    const val SESSIONS_BETWEEN_ADS = 2
    const val MIN_INTERVAL_MS = 3 * 60_000L
    const val MIN_QUESTIONS_IN_SESSION = 3
    const val HINTS_PER_REWARD = 3

    fun shouldShowInterstitial(
        totalSessionsCompleted: Int,
        sessionsSinceLastAd: Int,
        lastAdShownAt: Long,
        now: Long,
        questionsInSession: Int,
    ): Boolean = questionsInSession >= MIN_QUESTIONS_IN_SESSION &&
        totalSessionsCompleted > FREE_SESSIONS &&
        sessionsSinceLastAd >= SESSIONS_BETWEEN_ADS &&
        now - lastAdShownAt >= MIN_INTERVAL_MS
}
