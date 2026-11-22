package io.github.zdyoyoyo.englishpractice.ui.pk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.domain.BotMove
import io.github.zdyoyoyo.englishpractice.domain.BotProfile
import io.github.zdyoyoyo.englishpractice.domain.PkEngine
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PkPhase { CHOOSE_BOT, PLAYING, ROUND_RESULT, FINISHED }

data class PkUiState(
    val phase: PkPhase = PkPhase.CHOOSE_BOT,
    val bot: BotProfile = BotProfile.ALL.first(),
    val questions: List<Question> = emptyList(),
    val script: List<BotMove> = emptyList(),
    val index: Int = 0,
    val elapsedMs: Long = 0,
    val selected: String? = null,
    val lastMyCorrect: Boolean = false,
    val lastMyTimeMs: Long = 0,
    val me: PkEngine.Side = PkEngine.Side(),
    val botSide: PkEngine.Side = PkEngine.Side(),
    val message: String? = null,
) {
    val current: Question? get() = questions.getOrNull(index)
    val botMove: BotMove? get() = script.getOrNull(index)
    val botHasAnswered: Boolean get() = botMove?.let { elapsedMs >= it.timeMs } ?: false
}

class PkViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(PkUiState())
    val ui: StateFlow<PkUiState> = _ui.asStateFlow()
    private var ticker: Job? = null
    private var questionStart = 0L

    fun start(bot: BotProfile) {
        viewModelScope.launch {
            val all = c.questionBank.ensureLoaded().questions.filter { !it.isTypingQuestion }
            if (all.size < PkEngine.QUESTIONS_PER_MATCH) {
                _ui.update { it.copy(message = "題庫的選擇題不足 ${PkEngine.QUESTIONS_PER_MATCH} 題") }
                return@launch
            }
            val questions = all.shuffled().take(PkEngine.QUESTIONS_PER_MATCH)
            val script = PkEngine.botScript(bot, questions.map { it.id }, seed = c.clock.today().toEpochDay())
            _ui.value = PkUiState(phase = PkPhase.PLAYING, bot = bot, questions = questions, script = script)
            startQuestion()
        }
    }

    private fun startQuestion() {
        questionStart = c.clock.nowMillis()
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val elapsed = c.clock.nowMillis() - questionStart
                _ui.update { it.copy(elapsedMs = elapsed) }
                if (elapsed >= PkEngine.TIME_LIMIT_MS) {
                    answer(null) // 超時
                    break
                }
                delay(100)
            }
        }
    }

    fun answer(option: String?) {
        val s = _ui.value
        if (s.phase != PkPhase.PLAYING) return
        val q = s.current ?: return
        val bot = s.botMove ?: return
        ticker?.cancel()
        val elapsed = (c.clock.nowMillis() - questionStart).coerceAtMost(PkEngine.TIME_LIMIT_MS)
        val correct = option == q.answer
        _ui.update {
            it.copy(
                phase = PkPhase.ROUND_RESULT,
                selected = option,
                lastMyCorrect = correct,
                lastMyTimeMs = elapsed,
                elapsedMs = maxOf(elapsed, bot.timeMs), // 揭曉時對手一定也作答了
                me = it.me.add(correct, elapsed),
                botSide = it.botSide.add(bot.correct, bot.timeMs),
            )
        }
        // PK 的作答一樣會進錯題本與複習排程
        viewModelScope.launch { c.study.recordAnswer(q, option ?: "（超時）", correct, elapsed, StudyMode.PK) }
    }

    fun next() {
        val s = _ui.value
        if (s.index >= s.questions.lastIndex) {
            _ui.update { it.copy(phase = PkPhase.FINISHED) }
        } else {
            _ui.update { it.copy(phase = PkPhase.PLAYING, index = it.index + 1, selected = null, elapsedMs = 0) }
            startQuestion()
        }
    }

    fun backToChooser() {
        ticker?.cancel()
        _ui.value = PkUiState()
    }

    companion object {
        val Factory = viewModelFactory { initializer { PkViewModel(appContainer()) } }
    }
}
