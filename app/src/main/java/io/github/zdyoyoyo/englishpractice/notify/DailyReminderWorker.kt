package io.github.zdyoyoyo.englishpractice.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.zdyoyoyo.englishpractice.EnglishApp
import io.github.zdyoyoyo.englishpractice.MainActivity
import io.github.zdyoyoyo.englishpractice.R
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker

/** 每天被 WorkManager 叫起來一次：組出提醒內容（全部在本機計算）並發通知 */
class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as EnglishApp).container
        if (!c.prefs.reminderEnabled) return Result.success()

        val bank = c.questionBank.ensureLoaded()
        val today = c.clock.today().toEpochDay()
        val word = DailyPicker.dailyWord(bank.questions, today)
        val due = c.study.dueCountNow()
        val streak = c.study.streakNow()

        val title = word?.word?.let { "今日單字：$it" } ?: "每日一題時間到"
        val body = buildString {
            append("點我做每日一題")
            if (due > 0) append("・有 $due 題該複習")
            if (streak.current > 0 && !streak.studiedToday) append("・別讓 ${streak.current} 天連續打卡中斷！")
        }
        notify(title, body)
        return Result.success()
    }

    private fun notify(title: String, body: String) {
        val ctx = applicationContext
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_DAILY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(ctx, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
