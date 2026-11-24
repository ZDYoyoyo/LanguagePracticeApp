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
import io.github.zdyoyoyo.englishpractice.ads.AdPolicy
import io.github.zdyoyoyo.englishpractice.domain.AnswerChecker
import io.github.zdyoyoyo.englishpractice.domain.HintLogic
import io.github.zdyoyoyo.englishpractice.domain.MockScore
import io.github.zdyoyoyo.englishpractice.domain.MockTestBuilder
import io.github.zdyoyoyo.englishpractice.domain.OptionExplainer
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker
import io.github.zdyoyoyo.englishpractice.domain.QuestionSelector
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import io.github.zdyoyoyo.englishpractice.ui.navigation.QuizRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    // ---- 提示（看獎勵廣告可換）----
    val hintCredits: Int = 0,
    val usedHint: Boolean = false,
    val hiddenOptions: Set<String> = emptySet(),
    val hintText: String? = null,
    val showRewardOffer: Boolean = false,
    /** 按了「放棄這題」 */
    val gaveUp: Boolean = false,
    /** 答錯時，對「你選的那個選項」的說明 */
    val wrongOptionNote: String? = null,
    // ---- 收藏 ----
    val favoriteIds: Set<String> = emptySet(),
    val markedGuessed: Boolean = false,
    val rewardMessage: String? = null,
    // ---- 結算 ----
    /** 這一輪答錯（含放棄）的題目，結算畫面列出並可一鍵重做 */
    val wrongInSession: List<Question> = emptyList(),
    // ---- 模擬考 ----
    val isMock: Boolean = false,
    /** 剩餘時間（毫秒）；null = 不限時 */
    val remainingMs: Long? = null,
    /** 題目 id → 使用者的答案（模擬考結束後檢討用） */
    val mockAnswers: Map<String, String> = emptyMap(),
    val mockScore: MockScore? = null,
    val mockTimedOut: Boolean = false,
    val mockDurationMs: Long = 0,
) {
    val current: Question? get() = questions.getOrNull(index)
    val isLast: Boolean get() = index == questions.lastIndex
    val isCurrentFavorite: Boolean get() = current?.id in favoriteIds
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
    private var roundStartedAt = 0L
    private var timerJob: Job? = null
    private val mockCorrectIds = mutableSetOf<String>()

    init {
        viewModelScope.launch { start() }
        viewModelScope.launch { c.hints.credits.collect { n -> _ui.update { it.copy(hintCredits = n) } } }
        viewModelScope.launch { c.study.favoriteIds.collect { ids -> _ui.update { it.copy(favoriteIds = ids.toSet()) } } }
    }

    private suspend fun start() {
        c.questionBank.ensureLoaded()
        beginRound(loadQuestions(), titleFor(mode))
    }

    /** 開始新的一輪（保留提示次數與收藏狀態） */
    private fun beginRound(questions: List<Question>, title: String) {
        val isMock = mode == StudyMode.MOCK
        timerJob?.cancel()
        mockCorrectIds.clear()
        _ui.update {
            QuizUiState(
                loading = false,
                title = title,
                questions = questions,
                emptyMessage = if (questions.isEmpty()) emptyMessageFor(mode) else null,
                hintCredits = it.hintCredits,
                favoriteIds = it.favoriteIds,
                isMock = isMock,
                remainingMs = if (isMock) MockTestBuilder.timeLimitMs(questions.size) else null,
            )
        }
        questionShownAt = c.clock.nowMillis()
        roundStartedAt = questionShownAt
        if (isMock && questions.isNotEmpty()) startMockTimer(MockTestBuilder.timeLimitMs(questions.size))
    }

    /** 模擬考倒數計時：時間到自動交卷 */
    private fun startMockTimer(limitMs: Long) {
        timerJob = viewModelScope.launch {
            while (isActive) {
                val left = limitMs - (c.clock.nowMillis() - roundStartedAt)
                _ui.update { it.copy(remainingMs = left.coerceAtLeast(0)) }
                if (left <= 0) {
                    finishMock(timedOut = true)
                    break
                }
                delay(500)
            }
        }
    }

    /** 模擬考：作答後不顯示對錯，直接下一題 */
    private fun answerMock(answer: String, skipped: Boolean) {
        val s = _ui.value
        if (s.finished) return
        val q = s.current ?: return
        val correct = !skipped && AnswerChecker.isCorrect(q, answer)
        if (correct) mockCorrectIds += q.id
        val elapsed = c.clock.nowMillis() - questionShownAt
        viewModelScope.launch { c.study.recordAnswer(q, answer, correct, elapsed, mode) }
        _ui.update { it.copy(mockAnswers = it.mockAnswers + (q.id to answer)) }
        if (s.isLast) {
            finishMock(timedOut = false)
        } else {
            _ui.update { it.copy(index = it.index + 1, selected = null, typed = "") }
            questionShownAt = c.clock.nowMillis()
        }
    }

    /** 交卷：計算預估分數並存檔（沒作答的題目算錯，但不寫入作答紀錄） */
    private fun finishMock(timedOut: Boolean) {
        val s = _ui.value
        if (s.finished) return
        timerJob?.cancel()
        val score = MockScore.from(s.questions, mockCorrectIds.toSet())
        val duration = c.clock.nowMillis() - roundStartedAt
        _ui.update {
            it.copy(
                finished = true,
                mockScore = score,
                mockTimedOut = timedOut,
                mockDurationMs = duration,
                correctCount = score.totalCorrect,
                wrongInSession = it.questions.filter { q -> q.id !in mockCorrectIds },
            )
        }
        viewModelScope.launch { c.progress.saveMockResult(score, duration) }
    }

    /** 模擬考：提前交卷 */
    fun onSubmitMock() = finishMock(timedOut = false)

    /** 結算畫面：「重做這輪答錯的題目」 */
    fun onRetryWrong() {
        val wrong = _ui.value.wrongInSession
        if (wrong.isNotEmpty()) beginRound(wrong, "重做本輪錯題")
    }

    /** 結算畫面：「再來一輪」（重新選題） */
    fun onPlayAgain() {
        viewModelScope.launch { beginRound(loadQuestions(), titleFor(mode)) }
    }

    /** 每日練習固定兩題，不提供「再來一輪」 */
    val canPlayAgain: Boolean get() = mode != StudyMode.DAILY

    /** 模擬考的錯題已經收進錯題本，結算時改成看詳解，不提供立即重做 */
    val canRetryWrong: Boolean get() = mode != StudyMode.MOCK

    /** 依模式決定這一輪要做哪些題目 */
    private suspend fun loadQuestions(): List<Question> = when (mode) {
        StudyMode.WRONG_BOOK -> c.study.activeWrongQuestionIds()
            .mapNotNull { c.questionBank.findById(it) }
            .take(QuestionSelector.DEFAULT_SESSION_SIZE)
        StudyMode.FAVORITES -> c.study.favoriteQuestionIds()
            .mapNotNull { c.questionBank.findById(it) }
            .shuffled()
            .take(QuestionSelector.DEFAULT_SESSION_SIZE)
        StudyMode.REVIEW -> c.study.dueQuestionIds(limit = 20)
            .mapNotNull { c.questionBank.findById(it) }
        StudyMode.DAILY -> DailyPicker.dailySet(c.questionBank.questions, c.clock.today().toEpochDay())
        StudyMode.MOCK -> MockTestBuilder.build(c.questionBank.questions, route.count, c.study.questionStats())
        else -> QuestionSelector.selectSmart(
            all = c.questionBank.questions,
            filter = QuestionSelector.Filter.decode(route.tags, route.difficulties, route.categories),
            limit = route.count,
            stats = c.study.questionStats(),
            preferNew = route.preferNew,
        )
    }

    fun onTypedChange(text: String) = _ui.update { it.copy(typed = text) }

    /** 選擇題：點選項即作答 */
    fun onOptionSelected(option: String) {
        if (_ui.value.isMock) return answerMock(option, skipped = false)
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

    /** ⭐ 收藏 / 取消收藏目前這題 */
    fun onToggleFavorite() {
        val q = _ui.value.current ?: return
        viewModelScope.launch { c.study.toggleFavorite(q.id) }
    }

    /** 🎲 答對了但其實是猜的：加入收藏，明天再複習 */
    fun onMarkGuessed() {
        val s = _ui.value
        val q = s.current ?: return
        if (!s.revealed || !s.lastCorrect || s.markedGuessed) return
        _ui.update { it.copy(markedGuessed = true) }
        viewModelScope.launch { c.study.markGuessed(q.id) }
    }

    /** 「不會，看答案」：直接揭曉，算答錯（收進錯題本、明天複習） */
    fun onGiveUp() {
        if (_ui.value.isMock) return answerMock(SKIP_ANSWER, skipped = true)
        val s = _ui.value
        if (s.revealed) return
        _ui.update { it.copy(gaveUp = true, selected = null) }
        submit(GIVE_UP_ANSWER, gaveUp = true)
    }

    private fun submit(answer: String, gaveUp: Boolean = false) {
        val q = _ui.value.current ?: return
        val correct = !gaveUp && AnswerChecker.isCorrect(q, answer)
        val elapsed = c.clock.nowMillis() - questionShownAt
        val note = if (!correct && !gaveUp && !q.isTypingQuestion) {
            OptionExplainer.noteFor(q, answer, c.questionBank.questions)
        } else null
        _ui.update {
            it.copy(
                revealed = true,
                lastCorrect = correct,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongOptionNote = note,
                wrongInSession = if (correct || it.wrongInSession.any { w -> w.id == q.id }) it.wrongInSession
                else it.wrongInSession + q,
            )
        }
        val usedHint = _ui.value.usedHint
        viewModelScope.launch { c.study.recordAnswer(q, answer, correct, elapsed, mode, usedHint) }
    }

    /** 按「提示」：有次數就直接用；沒有就「詢問」要不要看廣告換（不強迫） */
    fun onHint() {
        val s = _ui.value
        if (s.revealed || s.usedHint) return
        if (c.hints.tryConsume()) applyHint() else _ui.update { it.copy(showRewardOffer = true) }
    }

    private fun applyHint() {
        val q = _ui.value.current ?: return
        _ui.update {
            if (q.isTypingQuestion) it.copy(usedHint = true, hintText = HintLogic.typingHint(q))
            else it.copy(usedHint = true, hiddenOptions = HintLogic.optionsToHide(q))
        }
    }

    fun onRewardEarned() {
        c.hints.add(AdPolicy.HINTS_PER_REWARD)
        _ui.update { it.copy(showRewardOffer = false, rewardMessage = "獲得 ${AdPolicy.HINTS_PER_REWARD} 次提示！") }
    }

    fun onRewardUnavailable() =
        _ui.update { it.copy(showRewardOffer = false, rewardMessage = "廣告還沒準備好，請稍後再試") }

    fun dismissRewardOffer() = _ui.update { it.copy(showRewardOffer = false) }

    fun onNext() {
        val s = _ui.value
        if (s.isLast) {
            _ui.update { it.copy(finished = true) }
        } else {
            _ui.update {
                it.copy(
                    index = it.index + 1, selected = null, typed = "", revealed = false,
                    usedHint = false, hiddenOptions = emptySet(), hintText = null, rewardMessage = null,
                    gaveUp = false, wrongOptionNote = null, markedGuessed = false,
                )
            }
            questionShownAt = c.clock.nowMillis()
        }
    }

    companion object {
        const val GIVE_UP_ANSWER = "（放棄）"
        const val SKIP_ANSWER = "（跳過）"

        fun titleFor(mode: StudyMode) = when (mode) {
            StudyMode.WRONG_BOOK -> "錯題重做"
            StudyMode.REVIEW -> "今日複習"
            StudyMode.FAVORITES -> "收藏練習"
            StudyMode.DAILY -> "每日練習"
            StudyMode.MOCK -> "模擬考"
            StudyMode.PK -> "PK 對戰"
            StudyMode.PRACTICE -> "做題"
        }

        fun emptyMessageFor(mode: StudyMode) = when (mode) {
            StudyMode.WRONG_BOOK -> "錯題本是空的，太棒了！"
            StudyMode.REVIEW -> "今天沒有要複習的題目 🎉"
            StudyMode.FAVORITES -> "還沒有收藏的題目。作答時按右上角 ☆ 就能收藏。"
            else -> "沒有符合條件的題目"
        }

        val Factory = viewModelFactory {
            initializer { QuizViewModel(createSavedStateHandle(), appContainer()) }
        }
    }
}
