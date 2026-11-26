package io.github.zdyoyoyo.englishpractice.data.prefs

import android.content.Context
import androidx.core.content.edit
import io.github.zdyoyoyo.englishpractice.sync.SyncState
import kotlinx.serialization.json.Json

/** 簡單的鍵值設定（SharedPreferences）。只存非機密的小設定。 */
class AppPreferences(context: Context) : SyncState {
    private val sp = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /** 本機學習資料最後一次變動時間（同步時用來判斷哪邊比較新） */
    override var lastLocalChangeAt: Long
        get() = sp.getLong(KEY_LAST_CHANGE, 0L)
        set(value) = sp.edit { putLong(KEY_LAST_CHANGE, value) }

    /** 最後一次成功同步的時間；0 = 這支手機從沒同步過 */
    override var lastSyncedAt: Long
        get() = sp.getLong(KEY_LAST_SYNC, 0L)
        set(value) = sp.edit { putLong(KEY_LAST_SYNC, value) }

    /** 「提示」可用次數（看獎勵廣告取得） */
    override var hintCredits: Int
        get() = sp.getInt(KEY_HINTS, 0)
        set(value) = sp.edit { putInt(KEY_HINTS, value) }

    override var streakFreezes: Int
        get() = sp.getInt(KEY_FREEZES, 0)
        set(value) = sp.edit { putInt(KEY_FREEZES, value) }

    override var achievements: Map<String, Long>
        get() = sp.getString(KEY_ACHIEVEMENTS, null)?.let {
            runCatching { Json.decodeFromString<Map<String, Long>>(it) }.getOrNull()
        } ?: emptyMap()
        set(value) = sp.edit { putString(KEY_ACHIEVEMENTS, Json.encodeToString(value)) }

    override var perfectRounds: Int
        get() = sp.getInt(KEY_PERFECT_ROUNDS, 0)
        set(value) = sp.edit { putInt(KEY_PERFECT_ROUNDS, value) }

    /** 每日目標題數 */
    var dailyGoal: Int
        get() = sp.getInt(KEY_DAILY_GOAL, 10)
        set(value) = sp.edit { putInt(KEY_DAILY_GOAL, value) }

    /** 每週日晚上的學習報告通知 */
    var weeklyReportEnabled: Boolean
        get() = sp.getBoolean(KEY_WEEKLY, true)
        set(value) = sp.edit { putBoolean(KEY_WEEKLY, value) }

    /** 是否已看過新手引導 */
    var onboardingDone: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING, false)
        set(value) = sp.edit { putBoolean(KEY_ONBOARDING, value) }

    var reminderEnabled: Boolean
        get() = sp.getBoolean(KEY_REMINDER_ON, false)
        set(value) = sp.edit { putBoolean(KEY_REMINDER_ON, value) }

    /** 提醒時間（一天中的第幾分鐘），預設 20:00 */
    var reminderMinuteOfDay: Int
        get() = sp.getInt(KEY_REMINDER_MIN, 20 * 60)
        set(value) = sp.edit { putInt(KEY_REMINDER_MIN, value) }

    /** 登入的 Google 帳號 email：只存在本機用來顯示「已登入：xxx」，不會傳給任何伺服器 */
    var signedInEmail: String?
        get() = sp.getString(KEY_EMAIL, null)
        set(value) = sp.edit { putString(KEY_EMAIL, value) }

    companion object {
        private const val KEY_LAST_CHANGE = "last_local_change_at"
        private const val KEY_LAST_SYNC = "last_synced_at"
        private const val KEY_HINTS = "hint_credits"
        private const val KEY_FREEZES = "streak_freezes"
        private const val KEY_WEEKLY = "weekly_report"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val KEY_PERFECT_ROUNDS = "perfect_rounds"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_REMINDER_ON = "reminder_enabled"
        private const val KEY_REMINDER_MIN = "reminder_minute_of_day"
        private const val KEY_EMAIL = "signed_in_email"
    }
}
