package io.github.zdyoyoyo.englishpractice.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 用 WorkManager 排每日提醒。WorkManager 會在手機重開機後自動恢復排程，
 * 而且不需要「精確鬧鐘」權限（代價是時間可能有幾分鐘誤差，提醒類用途完全夠用）。
 */
object ReminderScheduler {
    const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "daily_reminder"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "每日提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "每日一題、每日單字與複習提醒"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, minuteOfDay: Int, now: LocalDateTime = LocalDateTime.now()) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(minuteOfDay, now).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** 距離下一次「每天 HH:mm」還有多久（今天已過就排明天） */
    fun delayUntil(minuteOfDay: Int, now: LocalDateTime): Duration {
        var next = now.toLocalDate().atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
