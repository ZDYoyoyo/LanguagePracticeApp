package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankParser
import io.github.zdyoyoyo.englishpractice.data.model.QuestionType
import io.github.zdyoyoyo.englishpractice.domain.OptionExplainer
import io.github.zdyoyoyo.englishpractice.domain.PronunciationChecker
import io.github.zdyoyoyo.englishpractice.fakes.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackFeaturesTest {
    private val q = question("q1", options = listOf("punctual", "patient", "polite"), answer = "punctual")
        .copy(optionNotes = mapOf("patient" to "有耐心的"))
    private val politeWord = question(
        "v9", type = QuestionType.VOCABULARY, word = "polite",
        options = listOf("x", "y"), answer = "x",
    ).copy(explanation = "polite 有禮貌的")

    @Test fun `wrong option note comes from optionNotes first`() {
        assertEquals("有耐心的", OptionExplainer.noteFor(q, "patient", listOf(q, politeWord)))
    }

    @Test fun `falls back to a vocabulary question for the chosen word`() {
        assertEquals("polite 有禮貌的", OptionExplainer.noteFor(q, "polite", listOf(q, politeWord)))
    }

    @Test fun `no note for correct answer or unknown option`() {
        assertNull(OptionExplainer.noteFor(q, "punctual", listOf(q)))
        assertNull(OptionExplainer.noteFor(q, "polite", listOf(q)))
        assertNull(OptionExplainer.noteFor(q, null, listOf(q)))
    }

    @Test fun `parser rejects optionNotes keys that are not options`() {
        val json = """{"version":1,"questions":[
          {"id":"ok","type":"grammar","prompt":"p","options":["a","b"],"answer":"a","optionNotes":{"b":"note"}},
          {"id":"bad","type":"grammar","prompt":"p","options":["a","b"],"answer":"a","optionNotes":{"z":"note"}}
        ]}"""
        val r = QuestionBankParser.parse(json)
        assertEquals(listOf("ok"), r.bank.questions.map { it.id })
        assertEquals("note", r.bank.questions[0].optionNotes["b"])
    }

    @Test fun `pronunciation matching ignores case and punctuation`() {
        assertTrue(PronunciationChecker.matches("punctual", listOf("Punctual.", "punch")))
        assertTrue(PronunciationChecker.matches("turn off", listOf("turn  off!")))
        assertFalse(PronunciationChecker.matches("punctual", listOf("punch you all")))
        assertFalse(PronunciationChecker.matches("punctual", emptyList()))
    }

    @Test fun `speakable text prefers word and skips Chinese answers`() {
        assertEquals("polite", politeWord.speakableText)
        assertEquals("goes", question("g", options = listOf("goes", "go"), answer = "goes").speakableText)
        assertNull(question("c", options = listOf("靠窗", "走道"), answer = "走道").speakableText)
    }
}
