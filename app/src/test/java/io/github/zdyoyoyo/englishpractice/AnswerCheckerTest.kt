package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.domain.AnswerChecker
import io.github.zdyoyoyo.englishpractice.fakes.question
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {
    @Test fun `multiple choice requires exact match`() {
        val q = question("q1", options = listOf("goes", "go"), answer = "goes")
        assertTrue(AnswerChecker.isCorrect(q, "goes"))
        assertFalse(AnswerChecker.isCorrect(q, "go"))
    }

    @Test fun `typing ignores case spaces and trailing punctuation`() {
        val q = question("q2", options = emptyList(), answer = "turn off", accepted = listOf("switch off"))
        assertTrue(AnswerChecker.isCorrect(q, "  Turn   OFF. "))
        assertTrue(AnswerChecker.isCorrect(q, "switch off"))
        assertFalse(AnswerChecker.isCorrect(q, "turn on"))
        assertFalse(AnswerChecker.isCorrect(q, "   "))
    }

    @Test fun `curly quotes treated as straight quotes`() {
        val q = question("q3", options = emptyList(), answer = "don't")
        assertTrue(AnswerChecker.isCorrect(q, "don’t"))
    }
}
