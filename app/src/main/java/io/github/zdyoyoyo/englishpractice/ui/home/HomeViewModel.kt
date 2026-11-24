package io.github.zdyoyoyo.englishpractice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.data.bank.BankHttpException
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository.RefreshResult
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker
import io.github.zdyoyoyo.englishpractice.domain.StreakCalculator
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bankVersion: Int = 0,
    val questionCount: Int = 0,
    val wrongCount: Int = 0,
    val totalAttempts: Int = 0,
    val distinctAnswered: Int = 0,
    val dueCount: Int = 0,
    val streak: StreakCalculator.Streak = StreakCalculator.Streak(0, 0, false),
    val dailyWord: Question? = null,
    val favoriteCount: Int = 0,
    val bankMessage: String? = null,
)

/**
 * ViewModel：保存畫面狀態、處理畫面事件；螢幕旋轉時不會被重建。
 * 畫面只「觀察」uiState（StateFlow），資料變了畫面就自動更新。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val bank: QuestionBankRepository,
    study: StudyRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val bankMessage = MutableStateFlow<String?>(null)
    /** 「今天」也是狀態：App 跨過午夜回到首頁時重新計算 */
    private val today = MutableStateFlow(clock.today().toEpochDay())

    private val dueCount = today.flatMapLatest { study.observeDueCount(it) }
    private val streak = today.flatMapLatest { study.observeStreak(it) }

    private val counts = combine(
        study.activeWrongCount, study.totalAttempts, dueCount, streak, study.favoriteCount,
    ) { w, t, d, s, f ->
        arrayOf<Any>(w, t, d, s, f)
    }

    val uiState: StateFlow<HomeUiState> = combine(bank.bank, counts, bankMessage, today, study.distinctAnswered) { b, c, msg, day, distinct ->
        HomeUiState(
            bankVersion = b?.version ?: 0,
            questionCount = b?.questions?.size ?: 0,
            wrongCount = c[0] as Int,
            totalAttempts = c[1] as Int,
            distinctAnswered = distinct,
            dueCount = c[2] as Int,
            streak = c[3] as StreakCalculator.Streak,
            favoriteCount = c[4] as Int,
            dailyWord = b?.let { DailyPicker.dailyWord(it.questions, day) },
            bankMessage = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            bank.ensureLoaded()
            refreshBank()
        }
    }

    fun onResume() {
        today.value = clock.today().toEpochDay()
    }

    fun refreshBank() {
        viewModelScope.launch {
            bankMessage.value = when (val r = bank.refreshIfNeeded()) {
                is RefreshResult.Updated -> "題庫已更新到 v${r.version}（${r.questionCount} 題）"
                is RefreshResult.UpToDate -> null
                is RefreshResult.AppTooOld -> "有新題庫，但需要更新 App 才能使用"
                is RefreshResult.Failed ->
                    if (r.error is BankHttpException) "線上題庫尚未發布，使用 App 內建題庫" else "目前離線，使用本機題庫"
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                HomeViewModel(c.questionBank, c.study, c.clock)
            }
        }
    }
}
