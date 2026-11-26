package io.github.zdyoyoyo.englishpractice.announce

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * 開發者公告（「客製化廣播」）：放在 question-bank/announcements.json，跟題庫一樣放在靜態主機上。
 * 改檔案 → commit → 用戶打開 App 時在首頁看到；背景每天檢查一次，有新公告會發一則通知。
 */
@Serializable
data class AnnouncementFile(
    val schemaVersion: Int = 1,
    val announcements: List<Announcement> = emptyList(),
)

@Serializable
data class Announcement(
    /** 唯一 ID；改內容想讓大家再看一次就換一個新 ID */
    val id: String,
    val title: String,
    val body: String = "",
    /** 按鈕文字；沒有就只顯示「知道了」 */
    val actionLabel: String? = null,
    /** 按鈕動作：App 內畫面（見 [OpenTarget]）或 https 網址 */
    val action: String? = null,
    /** 開始／結束日期（含），格式 2026-12-01；不填 = 不限制 */
    val start: String? = null,
    val end: String? = null,
    /** 只給某些版本看（例如提醒舊版用戶更新） */
    val minVersionCode: Int = 0,
    val maxVersionCode: Int = Int.MAX_VALUE,
    /** 是否在背景發通知（false = 只在首頁顯示） */
    val notify: Boolean = false,
)

/** 公告按鈕能打開的 App 內畫面 */
object OpenTarget {
    const val DAILY = "daily"
    const val PRACTICE = "practice"
    const val MOCK = "mock"
    const val FLASHCARDS = "flashcards"
    const val DICTATION = "dictation"
    const val PK = "pk"
    const val REVIEW = "review"
    const val ACHIEVEMENTS = "achievements"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    val ALL = setOf(DAILY, PRACTICE, MOCK, FLASHCARDS, DICTATION, PK, REVIEW, ACHIEVEMENTS, STATS, SETTINGS)
}

object Announcements {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): AnnouncementFile = json.decodeFromString(AnnouncementFile.serializer(), text)

    /** 這則公告的按鈕是否可用（認得的畫面，或 https 網址） */
    fun isValidAction(action: String?): Boolean =
        action != null && (action in OpenTarget.ALL || action.startsWith("https://"))

    /** 現在該顯示的公告（依檔案順序），排除已按「知道了」的 */
    fun active(
        all: List<Announcement>,
        today: LocalDate,
        versionCode: Int,
        dismissed: Set<String>,
    ): List<Announcement> = all.filter { a ->
        a.id.isNotBlank() && a.title.isNotBlank() &&
            a.id !in dismissed &&
            versionCode in a.minVersionCode..a.maxVersionCode &&
            startedBy(a.start, today) && notEndedBy(a.end, today)
    }

    private fun startedBy(start: String?, today: LocalDate): Boolean {
        if (start == null) return true
        val d = start.toDateOrNull() ?: return false
        return !today.isBefore(d)
    }

    private fun notEndedBy(end: String?, today: LocalDate): Boolean {
        if (end == null) return true
        val d = end.toDateOrNull() ?: return false
        return !today.isAfter(d)
    }

    /** 要在背景發通知的：有勾 notify、還沒通知過、也還沒在 App 裡看過 */
    fun toNotify(active: List<Announcement>, alreadySeen: Set<String>): List<Announcement> =
        active.filter { it.notify && it.id !in alreadySeen }

    /** 日期寫錯（解析不了）時視為「不顯示」，避免錯誤公告永遠掛著 */
    private fun String?.toDateOrNull(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
