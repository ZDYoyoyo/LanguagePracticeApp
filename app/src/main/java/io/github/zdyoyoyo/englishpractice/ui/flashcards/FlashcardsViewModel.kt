package io.github.zdyoyoyo.englishpractice.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.domain.Achievement
import io.github.zdyoyoyo.englishpractice.domain.Flashcards
import io.github.zdyoyoyo.englishpractice.domain.WordCard
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

enum class CardPhase { CHOOSE, PLAYING, DONE }

data class FlashcardsUiState(
    val phase: CardPhase = CardPhase.CHOOSE,
    val deck: Flashcards.Deck = Flashcards.Deck.WEAK,
    val cards: List<WordCard> = emptyList(),
    val index: Int = 0,
    val flipped: Boolean = false,
    val known: List<WordCard> = emptyList(),
    val unknown: List<WordCard> = emptyList(),
    val message: String? = null,
    val newAchievements: List<Achievement> = emptyList(),
) {
    val current: WordCard? get() = cards.getOrNull(index)
}

class FlashcardsViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(FlashcardsUiState())
    val ui: StateFlow<FlashcardsUiState> = _ui.asStateFlow()
    private var shownAt = 0L
    private val recordJobs = mutableListOf<Job>()

    fun start(deck: Flashcards.Deck) {
        viewModelScope.launch {
            val all = c.questionBank.ensureLoaded().questions
            val cards = Flashcards.buildDeck(
                all = all,
                deck = deck,
                stats = c.study.questionStats(),
                favoriteIds = c.study.favoriteIds.first().toSet(),
                wrongIds = c.study.activeWrongQuestionIds().toSet(),
            )
            if (cards.isEmpty()) {
                _ui.update { it.copy(message = "「${deck.label}」目前沒有單字，換一個牌組試試") }
            } else {
                begin(deck, cards)
            }
        }
    }

    private fun begin(deck: Flashcards.Deck, cards: List<WordCard>) {
        _ui.value = FlashcardsUiState(phase = CardPhase.PLAYING, deck = deck, cards = cards)
        shownAt = c.clock.nowMillis()
    }

    fun flip() = _ui.update { it.copy(flipped = !it.flipped) }

    /** 認識 / 不認識：寫入複習排程（不認識 = 明天再複習，但不進錯題本） */
    fun answer(known: Boolean) {
        val s = _ui.value
        val card = s.current ?: return
        val q = c.questionBank.findById(card.questionId)
        val elapsed = c.clock.nowMillis() - shownAt
        if (q != null) {
            recordJobs += viewModelScope.launch {
                c.study.recordAnswer(q, if (known) card.word else "（不認識）", known, elapsed, StudyMode.FLASHCARD)
            }
        }
        val known2 = if (known) s.known + card else s.known
        val unknown2 = if (!known) s.unknown + card else s.unknown
        if (s.index >= s.cards.lastIndex) {
            _ui.update { it.copy(phase = CardPhase.DONE, known = known2, unknown = unknown2, flipped = false) }
            checkAchievements()
        } else {
            _ui.update { it.copy(index = it.index + 1, flipped = false, known = known2, unknown = unknown2) }
            shownAt = c.clock.nowMillis()
        }
    }

    private fun checkAchievements() {
        val jobs = recordJobs.toList()
        recordJobs.clear()
        viewModelScope.launch {
            jobs.joinAll()
            val fresh = c.achievements.checkAndUnlock()
            if (fresh.isNotEmpty()) _ui.update { it.copy(newAchievements = fresh) }
        }
    }

    /** 只重練這輪不認識的 */
    fun retryUnknown() {
        val s = _ui.value
        if (s.unknown.isNotEmpty()) begin(s.deck, s.unknown.shuffled())
    }

    fun backToChooser() {
        _ui.value = FlashcardsUiState(deck = _ui.value.deck)
    }

    companion object {
        val Factory = viewModelFactory { initializer { FlashcardsViewModel(appContainer()) } }
    }
}
