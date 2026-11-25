package io.github.zdyoyoyo.englishpractice.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.zdyoyoyo.englishpractice.EnglishApp
import io.github.zdyoyoyo.englishpractice.MainActivity
import io.github.zdyoyoyo.englishpractice.R
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker
import io.github.zdyoyoyo.englishpractice.domain.Flashcards
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 桌面小工具：連續打卡天數、今日目標進度、今日單字卡（單字＋中文）。
 * 資料全部在本機計算；點一下開啟 App。
 */
class DailyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val views = buildViews(context)
                ids.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun buildViews(context: Context): RemoteViews {
        val c = (context.applicationContext as EnglishApp).container
        val today = c.clock.today().toEpochDay()
        val bank = c.questionBank.ensureLoaded().questions
        val streak = c.study.streakNow()
        val answered = c.progress.observeAnsweredOn(today).first()
        val goal = c.progress.dailyGoal.value
        val card = DailyPicker.widgetWord(bank, today)?.let { Flashcards.cardOf(it) }

        return RemoteViews(context.packageName, R.layout.widget_daily).apply {
            setTextViewText(R.id.widget_streak, "🔥 連續 ${streak.current} 天")
            setTextViewText(R.id.widget_goal, if (answered >= goal) "🏆 今日 $answered/$goal" else "今日 $answered/$goal 題")
            setTextViewText(R.id.widget_word, card?.word ?: "英語練習")
            setTextViewText(
                R.id.widget_meaning,
                card?.let { listOfNotNull(it.partOfSpeech?.let { p -> "（$p）" }, it.meaning).joinToString("") } ?: "點我開始今天的練習",
            )
            val open = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            setOnClickPendingIntent(R.id.widget_root, open)
        }
    }

    companion object {
        /** 請系統重畫所有小工具（離開 App 時呼叫，讓打卡和進度保持最新） */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DailyWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, DailyWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }
}
