package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.data.model.QuestionType

/** 給使用者看的題型分類（比 JSON 的 type 更貼近多益：閱讀、聽力另外分開） */
enum class QuestionCategory(val key: String, val label: String) {
    VOCABULARY("vocab", "單字"),
    GRAMMAR("grammar", "文法"),
    CLOZE("cloze", "填空輸入"),
    CHOICE("choice", "情境選擇"),
    READING("reading", "閱讀"),
    LISTENING("listening", "聽力");

    companion object {
        fun of(q: Question): QuestionCategory = when {
            q.listening -> LISTENING
            q.passage != null -> READING
            q.type == QuestionType.VOCABULARY -> VOCABULARY
            q.type == QuestionType.GRAMMAR -> GRAMMAR
            q.type == QuestionType.CLOZE -> CLOZE
            else -> CHOICE
        }

        fun fromKey(key: String?): QuestionCategory? = entries.firstOrNull { it.key == key }
    }
}

val Question.category: QuestionCategory get() = QuestionCategory.of(this)
