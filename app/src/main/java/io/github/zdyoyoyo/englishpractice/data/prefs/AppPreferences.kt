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

    fun clearAll() = sp.edit { clear() }

    companion object {
        private const val KEY_LAST_CHANGE = "last_local_change_at"
    }
}
