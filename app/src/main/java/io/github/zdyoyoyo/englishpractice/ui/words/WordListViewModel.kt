package io.github.zdyoyoyo.englishpractice.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.domain.QuestionStat
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordItem(val word: String, val question: Question, val stat: QuestionStat?, val favorite: Boolean)

/** 單字庫：題庫中所有單字題的單字，可搜尋、聽發音、收藏 */
class WordListViewModel(private val c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    private val stats = MutableStateFlow<Map<String, QuestionStat>>(emptyMap())

    val items: StateFlow<List<WordItem>> = combine(c.questionBank.bank, c.study.favoriteIds, stats, query) { b, favs, st, q ->
        val favSet = favs.toSet()
        val needle = q.trim().lowercase()
        b?.questions.orEmpty()
            .filter { !it.word.isNullOrBlank() }
            .map { WordItem(it.word!!, it, st[it.id], it.id in favSet) }
            .filter { needle.isEmpty() || it.word.lowercase().contains(needle) || it.question.explanation.contains(needle) }
            .sortedBy { it.word.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            c.questionBank.ensureLoaded()
            stats.value = c.study.questionStats()
        }
    }

    fun onQueryChange(text: String) = query.update { text }

    fun toggleFavorite(questionId: String) {
        viewModelScope.launch { c.study.toggleFavorite(questionId) }
    }

    companion object {
        val Factory = viewModelFactory { initializer { WordListViewModel(appContainer()) } }
    }
}
