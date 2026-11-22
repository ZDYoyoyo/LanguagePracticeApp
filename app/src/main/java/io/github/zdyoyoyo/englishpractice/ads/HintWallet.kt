package io.github.zdyoyoyo.englishpractice.ads

import io.github.zdyoyoyo.englishpractice.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 「提示」次數錢包：看獎勵廣告 +3，作答時使用 -1。存在本機設定，也會一起備份到 Drive。 */
class HintWallet(
    private val state: SyncState,
    private val nowMillis: () -> Long,
) {
    private val _credits = MutableStateFlow(state.hintCredits)
    val credits: StateFlow<Int> = _credits.asStateFlow()

    fun tryConsume(): Boolean {
        if (state.hintCredits <= 0) return false
        set(state.hintCredits - 1)
        return true
    }

    fun add(amount: Int) = set(state.hintCredits + amount)

    /** 從雲端還原或清除資料後重新讀取 */
    fun reload() {
        _credits.value = state.hintCredits
    }

    private fun set(value: Int) {
        state.hintCredits = value
        state.lastLocalChangeAt = nowMillis()
        _credits.value = value
    }
}
