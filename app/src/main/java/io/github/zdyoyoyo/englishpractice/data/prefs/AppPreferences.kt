package io.github.zdyoyoyo.englishpractice.data.prefs

import android.content.Context
import androidx.core.content.edit

/** 簡單的鍵值設定（SharedPreferences）。只存非機密的小設定。 */
class AppPreferences(context: Context) {
    private val sp = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /** 本機學習資料最後一次變動時間（同步時用來判斷哪邊比較新） */
    var lastLocalChangeAt: Long
        get() = sp.getLong(KEY_LAST_CHANGE, 0L)
        set(value) = sp.edit { putLong(KEY_LAST_CHANGE, value) }

    var reminderEnabled: Boolean
        get() = sp.getBoolean(KEY_REMINDER_ON, false)
        set(value) = sp.edit { putBoolean(KEY_REMINDER_ON, value) }

    /** 提醒時間（一天中的第幾分鐘），預設 20:00 */
    var reminderMinuteOfDay: Int
        get() = sp.getInt(KEY_REMINDER_MIN, 20 * 60)
        set(value) = sp.edit { putInt(KEY_REMINDER_MIN, value) }

    fun clearAll() = sp.edit { clear() }

    companion object {
        private const val KEY_LAST_CHANGE = "last_local_change_at"
        private const val KEY_REMINDER_ON = "reminder_enabled"
        private const val KEY_REMINDER_MIN = "reminder_minute_of_day"
    }
}
