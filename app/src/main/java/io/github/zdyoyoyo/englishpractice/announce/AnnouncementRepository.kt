package io.github.zdyoyoyo.englishpractice.announce

import android.content.Context
import androidx.core.content.edit
import io.github.zdyoyoyo.englishpractice.data.bank.RemoteBankSource
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 下載 announcements.json → 存成本機快取 → 算出現在該顯示的公告。
 * 已讀（看過或通知過）與「知道了」的 ID 存在 SharedPreferences。
 */
class AnnouncementRepository(
    context: Context,
    private val remote: RemoteBankSource,
    private val clock: AppClock,
    private val versionCode: Int,
) {
    private val cacheFile = File(context.filesDir, "announcements.json")
    private val sp = context.getSharedPreferences("announcements", Context.MODE_PRIVATE)

    private var all: List<Announcement> = emptyList()
    private val _active = MutableStateFlow<List<Announcement>>(emptyList())
    /** 首頁要顯示的公告（第一則顯示成橫幅） */
    val active: StateFlow<List<Announcement>> = _active.asStateFlow()

    /** 背景通知開關（設定頁） */
    var notifyEnabled: Boolean
        get() = sp.getBoolean(KEY_NOTIFY, true)
        set(value) = sp.edit { putBoolean(KEY_NOTIFY, value) }

    private var dismissed: Set<String>
        get() = sp.getStringSet(KEY_DISMISSED, emptySet()).orEmpty()
        set(value) = sp.edit { putStringSet(KEY_DISMISSED, value) }

    private var seen: Set<String>
        get() = sp.getStringSet(KEY_SEEN, emptySet()).orEmpty()
        set(value) = sp.edit { putStringSet(KEY_SEEN, value) }

    suspend fun loadCached() = withContext(Dispatchers.IO) {
        if (all.isEmpty()) all = cacheFile.takeIf { it.exists() }?.let { parseOrNull(it.readText()) }.orEmpty()
        recompute()
    }

    /** 下載最新公告；失敗（離線）就沿用快取。回傳是否成功 */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = remote.fetch(FILE)
            val parsed = parseOrNull(text) ?: return@withContext false // 格式錯誤不覆蓋快取
            cacheFile.writeText(text)
            all = parsed
            recompute()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (all.isEmpty()) loadCached()
            false
        }
    }

    /** 首頁顯示過 → 不用再發通知 */
    fun markSeen(id: String) {
        if (id !in seen) seen = seen + id
    }

    /** 按「知道了」→ 不再顯示 */
    fun dismiss(id: String) {
        dismissed = dismissed + id
        markSeen(id)
        recompute()
    }

    /** 背景檢查：回傳需要發通知的公告，並記為已通知 */
    fun takeToNotify(): List<Announcement> {
        val list = Announcements.toNotify(_active.value, seen)
        if (list.isNotEmpty()) seen = seen + list.map { it.id }
        return list
    }

    fun findActive(id: String): Announcement? = _active.value.firstOrNull { it.id == id }

    private fun recompute() {
        // 清掉檔案裡已經不存在的 ID，避免越存越多
        val ids = all.map { it.id }.toSet()
        if (all.isNotEmpty()) {
            if (!ids.containsAll(dismissed)) dismissed = dismissed intersect ids
            if (!ids.containsAll(seen)) seen = seen intersect ids
        }
        _active.value = Announcements.active(all, clock.today(), versionCode, dismissed)
    }

    private fun parseOrNull(text: String) = runCatching { Announcements.parse(text).announcements }.getOrNull()

    companion object {
        const val FILE = "announcements.json"
        private const val KEY_NOTIFY = "notify_enabled"
        private const val KEY_DISMISSED = "dismissed_ids"
        private const val KEY_SEEN = "seen_ids"
    }
}
