package io.github.zdyoyoyo.englishpractice.data.bank

import io.github.zdyoyoyo.englishpractice.data.model.BankManifest
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.data.model.QuestionBank
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        val root = json.parseToJsonElement(text).jsonObject
        val problems = mutableListOf<String>()
        // 一題一題解析：遇到「新版 App 才認得的題型」只略過那一題，不會讓整個題庫失效
        val decoded = root["questions"]?.jsonArray.orEmpty().mapNotNull { el ->
            try {
                json.decodeFromJsonElement(Question.serializer(), el)
            } catch (e: IllegalArgumentException) { // SerializationException 是它的子類別
                val id = (el as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull ?: "?"
                problems += "$id: 無法解析（${e.message?.take(80)}）"
                null
            }
        }
        val raw = QuestionBank(
            schemaVersion = root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1,
            version = root["version"]?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("缺少 version"),
            updatedAt = root["updatedAt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            questions = decoded,
        )
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
        q.optionNotes.keys.any { it !in q.options } -> "optionNotes 的 key 必須是選項之一"
        q.listening && q.passage.isNullOrBlank() -> "聽力題需要 passage（對話稿）"
        else -> null
    }
}
