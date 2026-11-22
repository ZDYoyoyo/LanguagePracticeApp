package io.github.zdyoyoyo.englishpractice.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.domain.QuestionSelector
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PracticeSetupViewModel(private val bank: QuestionBankRepository) : ViewModel() {
    val tags: StateFlow<List<String>> = bank.bank
        .map { QuestionSelector.allTags(it?.questions.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { bank.ensureLoaded() }
    }

    fun countMatching(tag: String?, difficulty: Int?): Int =
        QuestionSelector.select(bank.questions, tag, difficulty, limit = Int.MAX_VALUE).size

    companion object {
        val Factory = viewModelFactory { initializer { PracticeSetupViewModel(appContainer().questionBank) } }
    }
}
