package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question
import kotlin.random.Random

/** 提示內容：選擇題刪去錯誤選項（最多 2 個、至少留 1 個錯的），輸入題給開頭字母 */
object HintLogic {
    fun optionsToHide(q: Question, random: Random = Random.Default): Set<String> {
        val wrong = q.options.filter { it != q.answer }
        val hideCount = (wrong.size - 1).coerceIn(0, 2)
        return wrong.shuffled(random).take(hideCount).toSet()
    }

    fun typingHint(q: Question): String =
        "開頭是「${q.answer.first()}」，共 ${q.answer.length} 個字元"
}
