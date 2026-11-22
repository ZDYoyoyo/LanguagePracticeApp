package io.github.zdyoyoyo.englishpractice.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.domain.AnswerChecker
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker
import io.github.zdyoyoyo.englishpractice.domain.QuestionSelector
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import io.github.zdyoyoyo.englishpractice.ui.navigation.QuizRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val loading: Boolean = true,
    val title: String = "",
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selected: String? = null,
    val typed: String = "",
    val revealed: Boolean = false,
    val lastCorrect: Boolean = false,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    val emptyMessage: String? = null,
) {
    val current: Question? get() = questions.getOrNull(index)
    val isLast: Boolean get() = index == questions.lastIndex
}

/** 一輪作答的流程：出題 → 作答 → 顯示對錯與解析 → 下一題 → 結算 */
class QuizViewModel(
    savedState: SavedStateHandle,
    private val c: AppContainer,
) : ViewModel() {

    private val route = savedState.toRoute<QuizRoute>()
    val mode = StudyMode.fromKey(route.mode)

    private val _ui = MutableStateFlow(QuizUiState(title = titleFor(mode)))
    val ui: StateFlow<QuizUiState> = _ui.asStateFlow()

    private var questionShownAt = 0L

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        c.questionBank.ensureLoaded()
        val questions = loadQuestions()
        _ui.value = QuizUiState(
            loading = false,
            title = titleFor(mode),
            questions = questions,
            emptyMessage = if (questions.isEmpty()) emptyMessageFor(mode) else null,
        )
        questionShownAt = c.clock.nowMillis()
    }

    /** 依模式決定這一輪要做哪些題目 */
    private suspend fun loadQuestions(): List<Question> = when (mode) {
        StudyMode.WRONG_BOOK -> c.study.activeWrongQuestionIds()
            .mapNotNull { c.questionBank.findById(it) }
            .take(QuestionSelector.DEFAULT_SESSION_SIZE)
        StudyMode.REVIEW -> c.study.dueQuestionIds(limit = 20)
            .mapNotNull { c.questionBank.findById(it) }
        StudyMode.DAILY -> listOfNotNull(
            DailyPicker.dailyQuestion(c.questionBank.questions, c.clock.today().toEpochDay())
        )
        else -> QuestionSelector.select(c.questionBank.questions, route.tag, route.difficulty)
    }

    fun onTypedChange(text: String) = _ui.update { it.copy(typed = text) }

    /** 選擇題：點選項即作答 */
    fun onOptionSelected(option: String) {
        if (_ui.value.revealed) return
        _ui.update { it.copy(selected = option) }
        submit(option)
    }

    /** 輸入題：按「送出」 */
    fun onSubmitTyped() {
        val s = _ui.value
        if (s.revealed || s.typed.isBlank()) return
        submit(s.typed)
    }

    private fun submit(answer: String) {
        val q = _ui.value.current ?: return
        val correct = AnswerChecker.isCorrect(q, answer)
        val elapsed = c.clock.nowMillis() - questionShownAt
        _ui.update {
            it.copy(
                revealed = true,
                lastCorrect = correct,
                correctCount = it.correctCount + if (correct) 1 else 0,
            )
        }
        viewModelScope.launch { c.study.recordAnswer(q, answer, correct, elapsed, mode) }
    }

    fun onNext() {
        val s = _ui.value
        if (s.isLast) {
            _ui.update { it.copy(finished = true) }
        } else {
            _ui.update { it.copy(index = it.index + 1, selected = null, typed = "", revealed = false) }
            questionShownAt = c.clock.nowMillis()
        }
    }

    companion object {
        fun titleFor(mode: StudyMode) = when (mode) {
            StudyMode.WRONG_BOOK -> "錯題重做"
            StudyMode.REVIEW -> "今日複習"
            StudyMode.DAILY -> "每日一題"
            StudyMode.PK -> "PK 對戰"
            StudyMode.PRACTICE -> "做題"
        }

        fun emptyMessageFor(mode: StudyMode) = when (mode) {
            StudyMode.WRONG_BOOK -> "錯題本是空的，太棒了！"
            StudyMode.REVIEW -> "今天沒有要複習的題目 🎉"
            else -> "沒有符合條件的題目"
        }

        val Factory = viewModelFactory {
            initializer { QuizViewModel(createSavedStateHandle(), appContainer()) }
        }
    }
}
