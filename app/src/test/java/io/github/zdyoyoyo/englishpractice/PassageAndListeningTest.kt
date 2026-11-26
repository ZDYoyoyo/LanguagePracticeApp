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
        val lines = DialogueScript.parse("W: Hello there.\n\nM：Hi!")
        assertEquals(listOf("Hello there.", "Hi!"), lines.map { it.text })
        assertEquals(listOf("W", "M"), lines.map { it.speaker })
        assertTrue(lines[0].pitch > lines[1].pitch)
        // 完全沒標說話者的稿子：整段照念，用一般音高
        assertEquals(1.0f, DialogueScript.parse("No label line").single().pitch)
    }

    @Test fun `graphic block is split out and never spoken`() {
        val passage = "Floor Directory\n1F Reception\n3F Accounting\n\nM: Where is Accounting?\nW: On the third floor."
        val (graphic, script) = DialogueScript.splitGraphic(passage)
        assertEquals("Floor Directory\n1F Reception\n3F Accounting", graphic)
        assertTrue(script.trimStart().startsWith("M:"))
        // 就算整段丟進去念，圖表行也會被略過
        assertEquals(listOf("Where is Accounting?", "On the third floor."), DialogueScript.parse(passage).map { it.text })
        // 沒有圖表的對話不受影響
        assertNull(DialogueScript.splitGraphic("M: Hi.\nW: Hello.").first)
        assertEquals(1, DialogueScript.parse("No label line").size)
    }
}
