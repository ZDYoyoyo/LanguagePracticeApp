package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question
import java.text.Normalizer
import kotlin.random.Random

/** 一句聽寫題 */
data class DictationItem(val id: String, val text: String)

object DictationSource {
    const val SESSION_SIZE = 10
    private val example = Regex("例：(.+)$")
    private val speakerLabel = Regex("^\\s*(M|W)\\s*:\\s*")

    /**
     * 句子來源：單字題解析裡的例句、Part 2 聽力的問句。
     * 只留 4～18 個字、沒有中文的英文句子。
     */
    fun sentences(all: List<Question>): List<DictationItem> {
        val fromExamples = all.filter { !it.word.isNullOrBlank() }.mapNotNull { q ->
            example.find(q.explanation)?.groupValues?.get(1)?.trim()?.let { DictationItem("ex-${q.id}", it) }
        }
        val fromPart2 = all.filter { it.listening && it.passage != null && "part2" in it.skills }.map { q ->
            DictationItem("p2-${q.id}", q.passage!!.replace(speakerLabel, "").trim())
        }
        return (fromExamples + fromPart2)
            .filter { item -> item.text.none { it in '一'..'鿿' } && DictationChecker.tokens(item.text).size in 4..18 }
            .distinctBy { it.text }
    }

    fun session(all: List<Question>, size: Int = SESSION_SIZE, random: Random = Random.Default) =
        sentences(all).shuffled(random).take(size)
}

/** 聽寫比對：忽略大小寫、標點、重音符號；用最長共同子序列（LCS）逐字對齊 */
object DictationChecker {
    const val PASS_RATIO = 0.9

    enum class Status { CORRECT, MISSED }
    data class WordResult(val word: String, val status: Status)
    data class Result(val words: List<WordResult>) {
        val correct: Int get() = words.count { it.status == Status.CORRECT }
        val ratio: Double get() = if (words.isEmpty()) 0.0 else correct.toDouble() / words.size
        val passed: Boolean get() = ratio >= PASS_RATIO
    }

    /** 斷字並正規化（顯示用的原字保留在 [displayTokens]） */
    fun tokens(s: String): List<String> = displayTokens(s).map { normalize(it) }.filter { it.isNotEmpty() }

    fun displayTokens(s: String): List<String> =
        s.replace('’', '\'').split(Regex("\\s+")).filter { normalize(it).isNotEmpty() }

    fun normalize(token: String): String {
        val noAccent = Normalizer.normalize(token, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        return noAccent.lowercase().replace('’', '\'').replace(Regex("[^a-z0-9']"), "").trim('\'')
    }

    fun check(target: String, input: String): Result {
        val display = displayTokens(target)
        val t = display.map { normalize(it) }
        val u = tokens(input)
        // LCS 動態規劃
        val dp = Array(t.size + 1) { IntArray(u.size + 1) }
        for (i in t.indices.reversed()) for (j in u.indices.reversed()) {
            dp[i][j] = if (t[i] == u[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
        val matched = BooleanArray(t.size)
        var i = 0; var j = 0
        while (i < t.size && j < u.size) {
            when {
                t[i] == u[j] -> { matched[i] = true; i++; j++ }
                dp[i + 1][j] >= dp[i][j + 1] -> i++
                else -> j++
            }
        }
        return Result(display.mapIndexed { k, w -> WordResult(w, if (matched[k]) Status.CORRECT else Status.MISSED) })
    }
}
