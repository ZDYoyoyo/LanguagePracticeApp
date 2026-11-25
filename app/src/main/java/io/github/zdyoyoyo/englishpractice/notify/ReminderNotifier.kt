package io.github.zdyoyoyo.englishpractice.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.zdyoyoyo.englishpractice.EnglishApp
import io.github.zdyoyoyo.englishpractice.MainActivity
import io.github.zdyoyoyo.englishpractice.R
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker
import io.github.zdyoyoyo.englishpractice.domain.WeeklyReport

/** 組出提醒內容（全部在本機計算）並發通知 */
object ReminderNotifier {
    private const val NOTIFICATION_ID = 1001
    private const val WEEKLY_ID = 1002

    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    suspend fun showDaily(context: Context): Boolean {
        val c = (context.applicationContext as EnglishApp).container
        val bank = c.questionBank.ensureLoaded()
        val today = c.clock.today().toEpochDay()
        val hasWord = DailyPicker.dailyWord(bank.questions, today) != null
        val due = c.study.dueCountNow()
        val streak = c.study.streakNow()

        // 不在通知裡寫出單字：有些題目的答案就是那個單字，先看到就沒有練習效果了
        val title = if (hasWord) "今日單字和每日一題準備好了" else "每日一題時間到"
        val body = buildString {
            append("花一分鐘練習一下")
            if (due > 0) append("・有 $due 題該複習")
            if (streak.current > 0 && !streak.studiedToday) append("・別讓 ${streak.current} 天連續打卡中斷！")
        }
        return show(context, title, body)
    }

    /** 每週學習報告 */
    suspend fun showWeekly(context: Context): Boolean {
        val c = (context.applicationContext as EnglishApp).container
        val src = c.study.statsSource()
        val report = WeeklyReport.compute(src.attempts, src.days, c.clock.today())
        val title = if (report.thisWeek.answers > 0) "📊 你的本週學習報告" else "📊 這週還沒練習喔"
        val body = if (report.thisWeek.answers > 0) report.summary() + "。下週繼續加油！"
        else "花幾分鐘做幾題，下週一起進步吧！"
        return show(context, title, body, id = WEEKLY_ID, open = null)
    }

    /** 設定頁的「傳送測試通知」 */
    fun showTest(context: Context): Boolean =
        show(context, "測試通知 ✅", "看到這則通知，代表每日提醒可以正常顯示。點我開啟每日練習。")

    private fun show(
        context: Context, title: String, body: String,
        id: Int = NOTIFICATION_ID, open: String? = MainActivity.OPEN_DAILY,
    ): Boolean {
        if (!canNotify(context)) return false
        val intent = Intent(context, MainActivity::class.java)
            .apply { open?.let { putExtra(MainActivity.EXTRA_OPEN, it) } }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            return false
        }
        return true
    }
}
