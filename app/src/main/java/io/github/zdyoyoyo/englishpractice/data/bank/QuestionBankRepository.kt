package io.github.zdyoyoyo.englishpractice.data.bank

import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.data.model.QuestionBank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * 題庫的「下載 → 快取 → 讀取」流程：
 * 1. loadLocal()：先讀本機快取；沒有（或比 App 內建的舊）就用 App 內建題庫 → 離線也能作答
 * 2. refreshIfNeeded()：下載很小的 version.json，版本較新才下載完整 questions.json，驗證通過才覆蓋快取
 */
class QuestionBankRepository(
    private val remote: RemoteBankSource,
    private val store: BankStore,
    private val appVersionCode: Int,
    private val io: CoroutineContext = Dispatchers.IO,
) {
    sealed interface RefreshResult {
        data object UpToDate : RefreshResult
        data class Updated(val version: Int, val questionCount: Int) : RefreshResult
        data class AppTooOld(val required: Int) : RefreshResult
        data class Failed(val error: Throwable) : RefreshResult
    }

    private val _bank = MutableStateFlow<QuestionBank?>(null)
    val bank: StateFlow<QuestionBank?> = _bank.asStateFlow()

    val questions: List<Question> get() = _bank.value?.questions.orEmpty()
    val currentVersion: Int get() = _bank.value?.version ?: 0

    fun findById(id: String): Question? = questions.firstOrNull { it.id == id }

    suspend fun loadLocal(): QuestionBank = withContext(io) {
        val bundled = QuestionBankParser.parse(store.readBundled()).bank
        val cached = store.readCached()?.let { runCatching { QuestionBankParser.parse(it).bank }.getOrNull() }
        // App 更新後內建題庫可能比快取新，取較新的
        val chosen = if (cached != null && cached.version >= bundled.version) cached else bundled
        _bank.value = chosen
        chosen
    }

    /** 尚未載入才載入（任何畫面都可以安心呼叫） */
    suspend fun ensureLoaded(): QuestionBank = _bank.value ?: loadLocal()

    suspend fun refreshIfNeeded(): RefreshResult = withContext(io) {
        try {
            if (_bank.value == null) loadLocal()
            val manifest = QuestionBankParser.parseManifest(remote.fetch("version.json"))
            when {
                manifest.version <= currentVersion -> RefreshResult.UpToDate
                manifest.minAppVersionCode > appVersionCode -> RefreshResult.AppTooOld(manifest.minAppVersionCode)
                else -> {
                    val text = remote.fetch(manifest.file)
                    val parsed = QuestionBankParser.parse(text).bank // 驗證失敗會丟例外，不會覆蓋舊快取
                    store.writeCached(text)
                    _bank.value = parsed
                    RefreshResult.Updated(parsed.version, parsed.questions.size)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RefreshResult.Failed(e) // 沒網路就繼續用本機題庫
        }
    }
}
