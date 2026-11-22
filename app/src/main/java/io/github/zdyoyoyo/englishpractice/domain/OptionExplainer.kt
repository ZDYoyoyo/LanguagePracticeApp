package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question

/**
 * 答錯時，說明「你選的那個選項」是什麼意思。來源優先順序：
 * 1. 題目本身的 optionNotes（出題者寫的說明）
 * 2. 題庫裡剛好有以這個字為 word 的單字題 → 借用那題的解析
 */
object OptionExplainer {
    fun noteFor(question: Question, chosen: String?, bank: List<Question>): String? {
        if (chosen == null || chosen == question.answer) return null
        question.optionNotes[chosen]?.let { return it }
        return bank.firstOrNull { it.id != question.id && it.word.equals(chosen.trim(), ignoreCase = true) }
            ?.explanation?.takeIf { it.isNotBlank() }
    }
}
