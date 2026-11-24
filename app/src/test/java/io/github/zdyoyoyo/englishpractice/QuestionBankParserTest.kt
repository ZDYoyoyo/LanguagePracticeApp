package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuestionBankParserTest {

    @Test fun `skips invalid questions but keeps valid ones`() {
        val json = """
        {"version": 2, "unknownField": true, "questions": [
          {"id":"ok","type":"grammar","prompt":"p","options":["a","b"],"answer":"a"},
          {"id":"bad-answer","type":"grammar","prompt":"p","options":["a","b"],"answer":"c"},
          {"id":"ok","type":"grammar","prompt":"dup","options":["a","b"],"answer":"a"},
          {"id":"bad-level","type":"cloze","prompt":"p","answer":"x","difficulty":9},
          {"id":"typing","type":"cloze","prompt":"p","answer":"x"}
        ]}
        """.trimIndent()
        val result = QuestionBankParser.parse(json)
        assertEquals(listOf("ok", "typing"), result.bank.questions.map { it.id })
        assertEquals(3, result.problems.size)
        assertEquals(2, result.bank.version)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when no valid questions`() {
        QuestionBankParser.parse("""{"version":1,"questions":[]}""")
    }

    /** 確保 repo 裡的正式題庫檔永遠可以被解析 —— 新增題目後跑測試就能抓到格式錯誤 */
    @Test fun `bundled questions json in repo is valid`() {
        val file = File("../question-bank/questions.json").takeIf { it.exists() } ?: File("question-bank/questions.json")
        val result = QuestionBankParser.parse(file.readText())
        assertTrue("題庫錯誤：${result.problems}", result.problems.isEmpty())
        assertTrue(result.bank.questions.size >= 20)

        // 內容規範：有短文 / 對話的題目都要附「生字」和「中文翻譯」
        val missing = result.bank.questions.filter { it.passage != null && (it.glossary.isEmpty() || it.passageTranslation.isNullOrBlank()) }
        assertTrue("這些題目缺生字或翻譯：${missing.map { it.id }}", missing.isEmpty())

        // 每題都要有考點，而且考點 key 必須是 App 認得的（SkillLabels）
        val noSkill = result.bank.questions.filter { it.skills.isEmpty() }
        assertTrue("這些題目沒有考點：${noSkill.map { it.id }}", noSkill.isEmpty())
        val unknown = result.bank.questions.flatMap { q -> q.skills.filterNot { io.github.zdyoyoyo.englishpractice.domain.SkillLabels.isKnown(it) }.map { "${q.id}:$it" } }
        assertTrue("未知的考點：$unknown", unknown.isEmpty())

        val manifestFile = File(file.parentFile, "version.json")
        val manifest = QuestionBankParser.parseManifest(manifestFile.readText())
        assertEquals("version.json 與 questions.json 的版本號要一致", manifest.version, result.bank.version)
    }
}
