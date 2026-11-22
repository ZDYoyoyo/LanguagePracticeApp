package io.github.zdyoyoyo.englishpractice.sync

/**
 * 同步決策（純邏輯，方便測試）：「整份資料，較新的為準」。
 *
 * - 雲端沒有備份 → 上傳
 * - 這支手機「從沒同步過」且兩邊都有資料 → 不自動決定，請使用者選（避免新手機剛做幾題就把舊進度蓋掉）
 * - 雲端較新 → 下載還原
 * - 本機較新 → 上傳
 * - 一樣新 → 不動作
 */
object SyncPolicy {
    enum class Action { UPLOAD, DOWNLOAD, NOTHING, ASK_USER }

    fun decide(
        localChangedAt: Long,
        localHasData: Boolean,
        lastSyncedAt: Long,
        remoteChangedAt: Long?,
        remoteHasData: Boolean,
    ): Action = when {
        remoteChangedAt == null -> if (localHasData) Action.UPLOAD else Action.NOTHING
        lastSyncedAt == 0L && localHasData && remoteHasData -> Action.ASK_USER
        !localHasData && remoteHasData -> Action.DOWNLOAD
        remoteChangedAt > localChangedAt -> Action.DOWNLOAD
        localChangedAt > remoteChangedAt -> Action.UPLOAD
        else -> Action.NOTHING
    }
}
