package io.github.zdyoyoyo.englishpractice.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 題目類型，對應 questions.json 的 "type" 欄位 */
@Serializable
enum class QuestionType(val label: String) {
    @SerialName("vocabulary") VOCABULARY("單字"),
    @SerialName("grammar") GRAMMAR("文法"),
    @SerialName("cloze") CLOZE("填空"),
    @SerialName("multiple_choice") MULTIPLE_CHOICE("選擇"),
}

/**
 * 一道題目。Kotlin 的 data class ≈ Java 的 POJO + 自動產生 equals/hashCode/toString。
 * 有預設值的欄位在 JSON 裡可以省略。
 */
@Serializable
data class Question(
    val id: String,
    val type: QuestionType,
    val prompt: String,
    /** 單字題的目標單字（每日單字會用到），其他題型可省略 */
    val word: String? = null,
    /** 選項；空陣列代表「自行輸入答案」的填空題 */
    val options: List<String> = emptyList(),
    val answer: String,
    /** 輸入題可接受的其他寫法（不分大小寫） */
    val acceptedAnswers: List<String> = emptyList(),
    val explanation: String = "",
    /** 1 = 初級, 2 = 中級, 3 = 進階 */
    val difficulty: Int = 1,
    val tags: List<String> = emptyList(),
) {
    val isTypingQuestion: Boolean get() = options.isEmpty()
}

@Serializable
data class QuestionBank(
    val schemaVersion: Int = 1,
    val version: Int,
    val updatedAt: String = "",
    val questions: List<Question>,
)

/** 遠端 version.json：很小的檔案，App 先比對它，版本較新才下載完整題庫 */
@Serializable
data class BankManifest(
    val version: Int,
    val file: String = "questions.json",
    val minAppVersionCode: Int = 1,
)

object Difficulty {
    fun label(level: Int): String = when (level) {
        1 -> "初級"
        2 -> "中級"
        3 -> "進階"
        else -> "Lv.$level"
    }
}
