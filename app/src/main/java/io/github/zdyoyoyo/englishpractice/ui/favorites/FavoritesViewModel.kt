package io.github.zdyoyoyo.englishpractice.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoriteItem(val questionId: String, val question: Question?)

class FavoritesViewModel(
    private val bank: QuestionBankRepository,
    private val study: StudyRepository,
) : ViewModel() {

    val items: StateFlow<List<FavoriteItem>> = combine(study.favorites, bank.bank) { list, b ->
        list.map { f -> FavoriteItem(f.questionId, b?.questions?.firstOrNull { it.id == f.questionId }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { bank.ensureLoaded() }
    }

    fun remove(questionId: String) {
        viewModelScope.launch { study.toggleFavorite(questionId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { FavoritesViewModel(it.questionBank, it.study) } }
        }
    }
}
