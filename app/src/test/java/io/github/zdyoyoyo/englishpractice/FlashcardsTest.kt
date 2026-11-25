package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankParser
import io.github.zdyoyoyo.englishpractice.data.model.QuestionType
import io.github.zdyoyoyo.englishpractice.domain.DailyPicker
import io.github.zdyoyoyo.englishpractice.domain.Flashcards
import io.github.zdyoyoyo.englishpractice.domain.QuestionStat
import io.github.zdyoyoyo.englishpractice.fakes.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FlashcardsTest {
    private fun vocab(id: String, word: String, expl: String = "$word（n.）意思$id。例：Example $id.") =
        question(id, type = QuestionType.VOCABULARY, word = word).copy(explanation = expl)

    @Test fun `card parses part of speech, meaning and example`() {
        val c = Flashcards.cardOf(vocab("v1", "punctual", "punctual（adj.）準時的。例：She is always punctual for meetings."))!!
        assertEquals("adj.", c.partOfSpeech)
        assertEquals("準時的", c.meaning)
        assertEquals("She is always punctual for meetings.", c.example)
        val noExample = Flashcards.cardOf(vocab("v2", "deadline", "deadline（n.）截止期限。"))!!
        assertEquals("截止期限", noExample.meaning)
        assertNull(noExample.example)
        assertNull(Flashcards.cardOf(question("g1")))
    }

    @Test fun `every vocabulary question in the bank becomes a proper card`() {
        val file = File("../question-bank/questions.json").takeIf { it.exists() } ?: File("question-bank/questions.json")
        val bank = QuestionBankParser.parse(file.readText()).bank.questions
        val words = bank.filter { !it.word.isNullOrBlank() }
        val bad = words.filter { Flashcards.cardOf(it)?.partOfSpeech == null }
        assertTrue("這些單字的解析格式無法拆出詞性：${bad.map { it.id }}", bad.isEmpty())
    }

    @Test fun `weak deck puts unseen and weak words first`() {
        val words = (1..6).map { vocab("v$it", "word$it") }
        val stats = mapOf("v1" to QuestionStat(3, 3, 0), "v2" to QuestionStat(2, 0, 0))
        val deck = Flashcards.buildDeck(words, Flashcards.Deck.WEAK, stats, emptySet(), emptySet(), size = 6, random = Random(1))
        assertEquals(setOf("v3", "v4", "v5", "v6"), deck.take(4).map { it.questionId }.toSet())
        assertEquals(listOf("v2", "v1"), deck.drop(4).map { it.questionId })
    }

    @Test fun `favorites and wrong decks filter by ids`() {
        val words = (1..5).map { vocab("v$it", "word$it") }
        assertEquals(listOf("v2"), Flashcards.buildDeck(words, Flashcards.Deck.FAVORITES, emptyMap(), setOf("v2", "x"), emptySet()).map { it.questionId })
        assertTrue(Flashcards.buildDeck(words, Flashcards.Deck.WRONG, emptyMap(), emptySet(), emptySet()).isEmpty())
    }

    @Test fun `widget word differs from the daily practice word`() {
        val words = (1..30).map { vocab("v$it", "word$it") }
        repeat(60) { d ->
            val day = 20_000L + d
            assertNotEquals(DailyPicker.dailyWord(words, day)?.id, DailyPicker.widgetWord(words, day)?.id)
        }
    }
}
