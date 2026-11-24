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
    /** 選填：閱讀短文（多益 Part 6/7）或聽力對話稿（多益 Part 3） */
    val passage: String? = null,
    /** true = passage 是聽力對話：先用語音播放、作答後才顯示原文 */
    val listening: Boolean = false,
    /** 選填：短文 / 對話裡的重點生字（英文 → 中文） */
    val glossary: Map<String, String> = emptyMap(),
    /** 選填：短文 / 對話的中文翻譯（作答後才顯示） */
    val passageTranslation: String? = null,
    /** 單字題的目標單字（每日單字會用到），其他題型可省略 */
    val word: String? = null,
    /** 選項；空陣列代表「自行輸入答案」的填空題 */
    val options: List<String> = emptyList(),
    val answer: String,
    /** 輸入題可接受的其他寫法（不分大小寫） */
    val acceptedAnswers: List<String> = emptyList(),
    val explanation: String = "",
    /** 選填：各個「錯誤選項」的說明，答錯時告訴使用者「你選的那個是什麼意思」。key 必須是選項之一 */
    val optionNotes: Map<String, String> = emptyMap(),
    /** 1 = 初級, 2 = 中級, 3 = 進階 */
    val difficulty: Int = 1,
    val tags: List<String> = emptyList(),
) {
    val isTypingQuestion: Boolean get() = options.isEmpty()

    /** 可以朗讀 / 練習發音的英文：單字題用 word，其他題目若正解是英文也可以 */
    val speakableText: String?
        get() = if (listening) null else word?.takeIf { it.isNotBlank() } ?: answer.takeIf { it.isEnglishText() }
}

private fun String.isEnglishText(): Boolean = isNotBlank() && all { it.code < 128 } && any { it.isLetter() }

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
