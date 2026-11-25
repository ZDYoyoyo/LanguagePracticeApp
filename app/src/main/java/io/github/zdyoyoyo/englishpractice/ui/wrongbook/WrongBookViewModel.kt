package io.github.zdyoyoyo.englishpractice.ui.wrongbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerEntity
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WrongItem(val record: WrongAnswerEntity, val question: Question?)

class WrongBookViewModel(
    private val bank: QuestionBankRepository,
    private val study: StudyRepository,
    private val c: io.github.zdyoyoyo.englishpractice.AppContainer,
) : ViewModel() {

    val items: StateFlow<List<WrongItem>> = combine(study.activeWrongAnswers, bank.bank) { list, b ->
        list.map { w -> WrongItem(w, b?.questions?.firstOrNull { it.id == w.questionId }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { bank.ensureLoaded() }
    }

    val notes = c.notes.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun saveNote(questionId: String, text: String) { viewModelScope.launch { c.notes.save(questionId, text) } }

    fun remove(questionId: String) {
        viewModelScope.launch { study.removeFromWrongBook(questionId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { WrongBookViewModel(it.questionBank, it.study, it) } }
        }
    }
}
