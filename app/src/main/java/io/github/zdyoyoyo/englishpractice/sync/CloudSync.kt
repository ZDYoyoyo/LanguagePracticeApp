package io.github.zdyoyoyo.englishpractice.sync

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.credentials.exceptions.GetCredentialCancellationException
import io.github.zdyoyoyo.englishpractice.auth.GoogleAuthManager
import io.github.zdyoyoyo.englishpractice.auth.GoogleAuthManager.DriveAuth
import io.github.zdyoyoyo.englishpractice.data.prefs.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CloudStatus(
    val configured: Boolean,
    val email: String? = null,
    val busy: Boolean = false,
    val lastSyncedAt: Long = 0,
    val message: String? = null,
    /** 不為 null 時，UI 要啟動 Google 授權同意畫面 */
    val consentIntent: PendingIntent? = null,
    /** 不為 null 時，UI 要問使用者保留雲端還是本機 */
    val choice: SyncOutcome.NeedsChoice? = null,
)

/**
 * 把「Google 登入 / Drive 授權 / 同步」串起來給畫面使用。
 * - 開 App：已登入就自動同步（不跳任何畫面）
 * - 手動：設定頁的「立即同步」「從雲端還原」「刪除雲端備份」「登出」
 */
class CloudSync(
    private val auth: GoogleAuthManager,
    private val sync: SyncManager,
    private val prefs: AppPreferences,
) {
    private val _status = MutableStateFlow(
        CloudStatus(configured = auth.isConfigured, email = prefs.signedInEmail, lastSyncedAt = prefs.lastSyncedAt)
    )
    val status: StateFlow<CloudStatus> = _status.asStateFlow()

    /** 授權同意畫面回來後要繼續做的事 */
    private var afterConsent: (suspend (String) -> Unit)? = null

    val isSignedIn: Boolean get() = prefs.signedInEmail != null

    suspend fun signIn(activity: Activity) {
        try {
            val account = auth.signIn(activity)
            prefs.signedInEmail = account.email
            _status.update { it.copy(email = account.email, message = "已登入 ${account.email}") }
            syncNow()
        } catch (e: GetCredentialCancellationException) {
            // 使用者自己取消，不顯示錯誤
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.update { it.copy(message = "登入失敗：${e.message}") }
        }
    }

    /** 開啟 App 時呼叫：只在已登入、且不需要跳畫面時同步 */
    suspend fun autoSyncOnOpen() {
        if (!isSignedIn) return
        val token = when (val a = runCatching { auth.authorizeDrive() }.getOrNull()) {
            is DriveAuth.Granted -> a.accessToken
            else -> return // 需要重新授權就等使用者手動同步
        }
        runSync(token, force = null)
    }

    suspend fun syncNow(force: SyncPolicy.Action? = null) = withDriveToken { runSync(it, force) }

    suspend fun deleteCloudBackup() = withDriveToken { token ->
        busy {
            sync.deleteRemote(DriveAppDataClient(token))
            "已刪除雲端備份"
        }
    }

    suspend fun signOut(clearLocalData: Boolean) {
        runCatching { auth.signOut() }
        prefs.signedInEmail = null
        if (clearLocalData) sync.clearLocal()
        _status.update {
            it.copy(email = null, lastSyncedAt = prefs.lastSyncedAt,
                message = if (clearLocalData) "已登出並清除這支手機上的學習資料" else "已登出")
        }
    }

    suspend fun clearLocalData() {
        sync.clearLocal()
        _status.update { it.copy(lastSyncedAt = 0, message = "已清除這支手機上的學習資料") }
    }

    /** UI 啟動同意畫面後，把結果交回來 */
    suspend fun onConsentResult(data: Intent?) {
        _status.update { it.copy(consentIntent = null) }
        val token = auth.tokenFromConsentResult(data)
        val next = afterConsent
        afterConsent = null
        if (token == null) {
            _status.update { it.copy(message = "未授權 Google Drive，無法同步") }
        } else {
            next?.invoke(token)
        }
    }

    fun dismissChoice() = _status.update { it.copy(choice = null) }
    fun clearMessage() = _status.update { it.copy(message = null) }

    private suspend fun withDriveToken(block: suspend (String) -> Unit) {
        if (!isSignedIn) {
            _status.update { it.copy(message = "請先登入 Google") }
            return
        }
        try {
            when (val a = auth.authorizeDrive()) {
                is DriveAuth.Granted -> block(a.accessToken)
                is DriveAuth.NeedsConsent -> {
                    afterConsent = block
                    _status.update { it.copy(consentIntent = a.pendingIntent) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.update { it.copy(message = "Google Drive 授權失敗：${e.message}") }
        }
    }

    private suspend fun runSync(token: String, force: SyncPolicy.Action?) = busy {
        when (val r = sync.sync(DriveAppDataClient(token), force)) {
            SyncOutcome.Uploaded -> "已備份到 Google Drive"
            SyncOutcome.Downloaded -> "已從 Google Drive 還原進度"
            SyncOutcome.NothingToDo -> "已是最新"
            is SyncOutcome.NeedsChoice -> {
                _status.update { it.copy(choice = r) }
                null
            }
            is SyncOutcome.Failed -> "同步失敗：${r.error.message}"
        }
    }

    private suspend fun busy(block: suspend () -> String?) {
        _status.update { it.copy(busy = true) }
        val msg = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "操作失敗：${e.message}"
        }
        _status.update { it.copy(busy = false, lastSyncedAt = prefs.lastSyncedAt, message = msg ?: it.message) }
    }
}
