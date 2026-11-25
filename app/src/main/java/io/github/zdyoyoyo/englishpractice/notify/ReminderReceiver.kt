package io.github.zdyoyoyo.englishpractice.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.zdyoyoyo.englishpractice.EnglishApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 收到鬧鐘 → 發通知並排明天；收到開機 / 改時間 / App 更新 → 重新排程 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> {
                // goAsync：讓我們在背景執行緒讀資料庫，最多約 10 秒
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val prefs = (context.applicationContext as EnglishApp).container.prefs
                        if (prefs.reminderEnabled) {
                            ReminderNotifier.showDaily(context)
                            ReminderScheduler.schedule(context, prefs.reminderMinuteOfDay)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_WEEKLY -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val prefs = (context.applicationContext as EnglishApp).container.prefs
                        if (prefs.weeklyReportEnabled) {
                            ReminderNotifier.showWeekly(context)
                            ReminderScheduler.scheduleWeekly(context, java.time.LocalDateTime.now().plusMinutes(1))
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
            else -> ReminderScheduler.rescheduleFromPrefs(context)
        }
    }

    companion object {
        const val ACTION_FIRE = "io.github.zdyoyoyo.englishpractice.action.DAILY_REMINDER"
        const val ACTION_WEEKLY = "io.github.zdyoyoyo.englishpractice.action.WEEKLY_REPORT"
    }
}
