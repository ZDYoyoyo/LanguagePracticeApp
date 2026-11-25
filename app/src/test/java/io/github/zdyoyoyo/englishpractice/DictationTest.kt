package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankParser
import io.github.zdyoyoyo.englishpractice.domain.DictationChecker
import io.github.zdyoyoyo.englishpractice.domain.DictationChecker.Status
import io.github.zdyoyoyo.englishpractice.domain.DictationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DictationTest {
    @Test fun `perfect answer ignoring case and punctuation`() {
        val r = DictationChecker.check("She is always punctual for meetings.", "she is ALWAYS punctual for meetings")
        assertEquals(1.0, r.ratio, 1e-9)
        assertTrue(r.passed)
    }

    @Test fun `missing and wrong words are marked`() {
        val r = DictationChecker.check("The invoice must be paid within 30 days.", "the invoice must paid with 30 days")
        assertEquals(listOf(true, true, true, false, true, false, true, true), r.words.map { it.status == Status.CORRECT })
        assertEquals("be", r.words[3].word)
        assertFalse(r.passed)
    }

    @Test fun `numbers, apostrophes and accents`() {
        assertEquals(1.0, DictationChecker.check("The contractor gave us an estimate of \$5,000.", "the contractor gave us an estimate of 5000").ratio, 1e-9)
        assertEquals(1.0, DictationChecker.check("Who's going to pick up the clients?", "who’s going to pick up the clients").ratio, 1e-9)
        assertEquals(1.0, DictationChecker.check("Each applicant must submit a résumé.", "each applicant must submit a resume").ratio, 1e-9)
    }

    @Test fun `extra words do not count against you but order matters`() {
        assertEquals(1.0, DictationChecker.check("Please hold on.", "please please hold on now").ratio, 1e-9)
        assertTrue(DictationChecker.check("one two three four", "four three two one").ratio < 0.5)
    }

    @Test fun `bank provides enough clean English sentences`() {
        val file = File("../question-bank/questions.json").takeIf { it.exists() } ?: File("question-bank/questions.json")
        val bank = QuestionBankParser.parse(file.readText()).bank.questions
        val items = DictationSource.sentences(bank)
        assertTrue("聽寫句子太少：${items.size}", items.size >= 100)
        assertTrue(items.none { it.text.startsWith("M:") || it.text.startsWith("W:") })
        assertTrue(items.all { DictationChecker.tokens(it.text).size in 4..18 })
    }
}
