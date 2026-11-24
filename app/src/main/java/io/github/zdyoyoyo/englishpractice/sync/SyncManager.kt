package io.github.zdyoyoyo.englishpractice.sync

import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 同步需要讀寫的本機狀態（AppPreferences 實作它；測試用假的） */
interface SyncState {
    var lastLocalChangeAt: Long
    var lastSyncedAt: Long
    var hintCredits: Int
    /** 連勝保護（可補回漏掉的一天）剩餘次數 */
    var streakFreezes: Int
}

sealed interface SyncOutcome {
    data object Uploaded : SyncOutcome
    data object Downloaded : SyncOutcome
    data object NothingToDo : SyncOutcome
    /** 這支手機第一次同步、兩邊都有資料：請使用者選擇要保留哪一份 */
    data class NeedsChoice(val remoteChangedAt: Long, val localChangedAt: Long) : SyncOutcome
    data class Failed(val error: Throwable) : SyncOutcome
}

class SyncManager(
    private val backup: BackupSerializer,
    private val state: SyncState,
    private val clock: AppClock,
) {
    private val mutex = Mutex()

    /**
     * @param force 使用者手動指定方向（「從雲端還原」/「用本機覆蓋雲端」），null = 自動判斷
     */
    suspend fun sync(store: RemoteBackupStore, force: SyncPolicy.Action? = null): SyncOutcome = mutex.withLock {
        try {
            val local = backup.export(clock.nowMillis(), state.lastLocalChangeAt, state.hintCredits, state.streakFreezes)
            val remote = store.read()?.let { BackupSnapshot.fromJson(it) }
            val action = force ?: SyncPolicy.decide(
                localChangedAt = state.lastLocalChangeAt,
                localHasData = !local.isEmpty,
                lastSyncedAt = state.lastSyncedAt,
                remoteChangedAt = remote?.lastLocalChangeAt,
                remoteHasData = remote?.isEmpty == false,
            )
            when (action) {
                SyncPolicy.Action.UPLOAD -> {
                    store.write(local.toJson())
                    state.lastSyncedAt = clock.nowMillis()
                    SyncOutcome.Uploaded
                }
                SyncPolicy.Action.DOWNLOAD -> {
                    if (remote == null) return@withLock SyncOutcome.NothingToDo
                    backup.restore(remote)
                    state.hintCredits = remote.hintCredits
                    state.streakFreezes = remote.streakFreezes
                    state.lastLocalChangeAt = remote.lastLocalChangeAt
                    state.lastSyncedAt = clock.nowMillis()
                    SyncOutcome.Downloaded
                }
                SyncPolicy.Action.NOTHING -> {
                    state.lastSyncedAt = clock.nowMillis()
                    SyncOutcome.NothingToDo
                }
                SyncPolicy.Action.ASK_USER ->
                    SyncOutcome.NeedsChoice(remote!!.lastLocalChangeAt, state.lastLocalChangeAt)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncOutcome.Failed(e)
        }
    }

    suspend fun deleteRemote(store: RemoteBackupStore) = mutex.withLock {
        store.delete()
        state.lastSyncedAt = 0L
    }

    /** 清除這支手機上的所有學習資料（登出時可選，或在設定裡手動清除） */
    suspend fun clearLocal() = mutex.withLock {
        backup.clearAll()
        state.lastLocalChangeAt = 0L
        state.lastSyncedAt = 0L
        state.hintCredits = 0
        state.streakFreezes = 0
    }
}
