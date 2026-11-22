package io.github.zdyoyoyo.englishpractice.data.bank

import io.github.zdyoyoyo.englishpractice.data.model.BankManifest
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.data.model.QuestionBank
import kotlinx.serialization.json.Json

/** 解析並驗證題庫 JSON。壞掉的單題會被略過（記在 problems），不會讓整個題庫失效。 */
object QuestionBankParser {

    val json = Json {
        ignoreUnknownKeys = true   // 之後題庫新增欄位，舊版 App 也不會壞
        isLenient = true
    }

    data class Result(val bank: QuestionBank, val problems: List<String>)

    fun parseManifest(text: String): BankManifest = json.decodeFromString(BankManifest.serializer(), text)

    /** 失敗（JSON 格式錯誤或沒有任何有效題目）時丟出例外 */
    fun parse(text: String): Result {
        val raw = json.decodeFromString(QuestionBank.serializer(), text)
        val problems = mutableListOf<String>()
        val seen = HashSet<String>()
        val valid = raw.questions.filter { q ->
            val problem = validate(q, seen)
            if (problem != null) problems += "${q.id}: $problem"
            seen += q.id
            problem == null
        }
        require(valid.isNotEmpty()) { "題庫沒有任何有效題目" }
        return Result(raw.copy(questions = valid), problems)
    }

    private fun validate(q: Question, seen: Set<String>): String? = when {
        q.id.isBlank() -> "id 不可為空"
        q.id in seen -> "id 重複"
        q.prompt.isBlank() -> "題幹不可為空"
        q.answer.isBlank() -> "正解不可為空"
        q.options.isNotEmpty() && q.answer !in q.options -> "正解必須是選項之一"
        q.options.isNotEmpty() && q.options.toSet().size != q.options.size -> "選項重複"
        q.difficulty !in 1..3 -> "難度需為 1~3"
        else -> null
    }
}
