package io.github.zdyoyoyo.englishpractice.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.zdyoyoyo.englishpractice.EnglishApp
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 每日提醒用系統鬧鐘（AlarmManager）排程：時間比 WorkManager 準確，也不需要「精確鬧鐘」權限。
 * 每次響完會自己排下一天；開機、改時間、App 更新後由 [ReminderReceiver] 重新排程。
 * 注意：Android 為了省電，提醒仍可能晚幾分鐘；部分手機（小米等）需要把 App 的省電策略設為「無限制」。
 */
object ReminderScheduler {
    // 通知頻道建立後重要性就不能改，所以改設定時要換新 ID（v2 = 改成「高」重要性，會跳出橫幅）
    const val CHANNEL_ID = "daily_reminder_v2"

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).deleteNotificationChannel("daily_reminder")
        val channel = NotificationChannel(CHANNEL_ID, "每日提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "每日一題、每日單字與複習提醒"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, minuteOfDay: Int, now: LocalDateTime = LocalDateTime.now()) {
        val triggerAt = now.plus(delayUntil(minuteOfDay, now))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        // AllowWhileIdle：手機待機（Doze）時也能響
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    /** 依設定重新排程（App 啟動、開機、改時間時呼叫，重複呼叫沒關係） */
    fun rescheduleFromPrefs(context: Context) {
        val prefs = (context.applicationContext as EnglishApp).container.prefs
        if (prefs.reminderEnabled) schedule(context, prefs.reminderMinuteOfDay) else cancel(context)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** 距離下一次「每天 HH:mm」還有多久（今天已過就排明天） */
    fun delayUntil(minuteOfDay: Int, now: LocalDateTime): Duration {
        var next = now.toLocalDate().atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
