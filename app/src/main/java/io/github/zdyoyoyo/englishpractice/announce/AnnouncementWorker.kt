package io.github.zdyoyoyo.englishpractice.announce

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.zdyoyoyo.englishpractice.EnglishApp
import io.github.zdyoyoyo.englishpractice.MainActivity
import io.github.zdyoyoyo.englishpractice.R
import io.github.zdyoyoyo.englishpractice.notify.ReminderNotifier
import java.util.concurrent.TimeUnit

/**
 * 背景大約每 1～3 小時（有網路時）檢查一次公告，有新的就發通知。
 * 這種「時間不用很準、需要網路」的工作適合 WorkManager（每日提醒則用鬧鐘，因為要準時）。
 */
class AnnouncementWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as EnglishApp).container.announcements
        if (!repo.notifyEnabled) return Result.success()
        repo.loadCached()
        repo.refresh()
        if (!ReminderNotifier.canNotify(applicationContext)) return Result.success()
        repo.takeToNotify().forEach { show(applicationContext, it) }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "announcements"
        private const val WORK_NAME = "announcement-check"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "公告與活動", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "新題庫、新功能與活動消息"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            // 每 3 小時一個週期，在週期的最後 2 小時內執行 → 間隔約 1～3 小時（手機待機省電時系統可能再延後）
            val request = PeriodicWorkRequestBuilder<AnnouncementWorker>(3, TimeUnit.HOURS, 2, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            // UPDATE：已安裝舊版（12 小時）的用戶更新後也會套用新間隔
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun show(context: Context, a: Announcement) {
            val action = a.action?.takeIf { Announcements.isValidAction(it) }
            val intent = if (action != null && action.startsWith("https://")) {
                Intent(Intent.ACTION_VIEW, Uri.parse(action))
            } else {
                Intent(context, MainActivity::class.java)
                    .apply { action?.let { putExtra(MainActivity.EXTRA_OPEN, it) } }
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val requestCode = 2000 + (a.id.hashCode() and 0xFFF)
            val pending = PendingIntent.getActivity(
                context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(a.title)
                .setContentText(a.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(a.body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(requestCode, n)
            } catch (_: SecurityException) {
            }
        }
    }
}
