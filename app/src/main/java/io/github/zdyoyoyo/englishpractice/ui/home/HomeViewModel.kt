package io.github.zdyoyoyo.englishpractice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository.RefreshResult
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bankVersion: Int = 0,
    val questionCount: Int = 0,
    val wrongCount: Int = 0,
    val totalAttempts: Int = 0,
    val bankMessage: String? = null,
)

/**
 * ViewModel：保存畫面狀態、處理畫面事件；螢幕旋轉時不會被重建。
 * 畫面只「觀察」uiState（StateFlow），資料變了畫面就自動更新。
 */
class HomeViewModel(
    private val bank: QuestionBankRepository,
    study: StudyRepository,
) : ViewModel() {

    private val bankMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        bank.bank, study.activeWrongCount, study.totalAttempts, bankMessage,
    ) { b, wrong, total, msg ->
        HomeUiState(
            bankVersion = b?.version ?: 0,
            questionCount = b?.questions?.size ?: 0,
            wrongCount = wrong,
            totalAttempts = total,
            bankMessage = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            bank.ensureLoaded()
            refreshBank()
        }
    }

    fun refreshBank() {
        viewModelScope.launch {
            bankMessage.value = when (val r = bank.refreshIfNeeded()) {
                is RefreshResult.Updated -> "題庫已更新到 v${r.version}（${r.questionCount} 題）"
                is RefreshResult.UpToDate -> null
                is RefreshResult.AppTooOld -> "有新題庫，但需要更新 App 才能使用"
                is RefreshResult.Failed -> "目前離線，使用本機題庫"
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                HomeViewModel(c.questionBank, c.study)
            }
        }
    }
}
