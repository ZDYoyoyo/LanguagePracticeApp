package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankParser
import io.github.zdyoyoyo.englishpractice.speech.DialogueScript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassageAndListeningTest {

    @Test fun `unknown future question type only skips that question`() {
        val json = """{"version":9,"questions":[
          {"id":"ok","type":"grammar","prompt":"p","options":["a","b"],"answer":"a"},
          {"id":"future","type":"speaking_video","prompt":"p","answer":"x"}
        ]}"""
        val r = QuestionBankParser.parse(json)
        assertEquals(listOf("ok"), r.bank.questions.map { it.id })
        assertTrue(r.problems.single().startsWith("future"))
    }

    @Test fun `listening question requires passage`() {
        val json = """{"version":1,"questions":[
          {"id":"ok","type":"multiple_choice","passage":"M: Hi.","listening":true,"prompt":"p","options":["a","b"],"answer":"a"},
          {"id":"bad","type":"multiple_choice","listening":true,"prompt":"p","options":["a","b"],"answer":"a"}
        ]}"""
        val r = QuestionBankParser.parse(json)
        assertEquals(listOf("ok"), r.bank.questions.map { it.id })
        val q = r.bank.questions.single()
        assertTrue(q.listening)
        assertNull("聽力題不顯示單字發音練習（避免洩漏答案）", q.speakableText)
    }

    @Test fun `dialogue script strips speaker labels and sets pitch`() {
        val lines = DialogueScript.parse("W: Hello there.\n\nM：Hi!\nNo label line")
        assertEquals(listOf("Hello there.", "Hi!", "No label line"), lines.map { it.text })
        assertEquals(listOf("W", "M", null), lines.map { it.speaker })
        assertTrue(lines[0].pitch > lines[1].pitch)
        assertEquals(1.0f, lines[2].pitch)
    }
}
