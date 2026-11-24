package io.github.zdyoyoyo.englishpractice.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.domain.StatsCalculator
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(private val c: AppContainer) : ViewModel() {
    private val _summary = MutableStateFlow<StatsCalculator.Summary?>(null)
    val summary: StateFlow<StatsCalculator.Summary?> = _summary.asStateFlow()

    /** 每次回到畫面都重新計算（資料量小，算一次很快） */
    fun refresh() {
        viewModelScope.launch {
            val bank = c.questionBank.ensureLoaded().questions
            val src = c.study.statsSource()
            _summary.value = StatsCalculator.summarize(bank, src.attempts, src.cards, src.days, c.clock.today().toEpochDay())
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { StatsViewModel(appContainer()) } }
    }
}
