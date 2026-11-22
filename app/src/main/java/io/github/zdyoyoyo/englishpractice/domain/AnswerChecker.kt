package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question

/** 判斷答案是否正確。輸入題會忽略大小寫、前後空白、多餘空白與結尾標點。 */
object AnswerChecker {

    fun isCorrect(question: Question, userAnswer: String): Boolean {
        if (!question.isTypingQuestion) return userAnswer == question.answer
        val given = normalize(userAnswer)
        if (given.isEmpty()) return false
        return (listOf(question.answer) + question.acceptedAnswers).any { normalize(it) == given }
    }

    fun normalize(s: String): String = s
        .replace('’', '\'').replace('‘', '\'')
        .trim()
        .trimEnd('.', '!', '?', ',', '。', '！', '？')
        .replace(Regex("\\s+"), " ")
        .lowercase()
}
