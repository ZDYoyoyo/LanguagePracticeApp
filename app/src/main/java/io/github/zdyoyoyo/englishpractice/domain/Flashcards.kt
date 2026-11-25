package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question
import kotlin.random.Random

/** 單字卡的內容：從單字題的 word + explanation 拆出來 */
data class WordCard(
    val questionId: String,
    val word: String,
    val partOfSpeech: String?,
    val meaning: String,
    val example: String?,
)

object Flashcards {
    const val DECK_SIZE = 20

    /** 牌組 */
    enum class Deck(val key: String, val label: String, val description: String) {
        WEAK("weak", "還不熟的單字", "沒練過或答錯過的優先"),
        ALL("all", "全部單字", "隨機抽 $DECK_SIZE 張"),
        FAVORITES("fav", "我的收藏", "收藏題目中的單字"),
        WRONG("wrong", "錯題本的單字", "錯題本裡的單字題");

        companion object {
            fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: WEAK
        }
    }

    // explanation 的格式：「punctual（adj.）準時的。例：She is always punctual.」
    private val pattern = Regex("^(.+?)（([^）]+)）(.+?)。(?:例：(.*))?$", RegexOption.DOT_MATCHES_ALL)

    fun cardOf(q: Question): WordCard? {
        val word = q.word?.takeIf { it.isNotBlank() } ?: return null
        val m = pattern.find(q.explanation.trim())
        return if (m != null) {
            WordCard(q.id, word, m.groupValues[2].trim(), m.groupValues[3].trim(), m.groupValues[4].trim().ifEmpty { null })
        } else {
            // 格式不同的舊題目：整段解析當作意思
            WordCard(q.id, word, null, q.explanation.ifBlank { q.answer }, null)
        }
    }

    /**
     * 挑牌：
     * - WEAK：沒練過 → 正確率低 → 其他；同一層隨機
     * - ALL：完全隨機
     * - FAVORITES / WRONG：只從指定的題目 id 中挑
     */
    fun buildDeck(
        all: List<Question>,
        deck: Deck,
        stats: Map<String, QuestionStat>,
        favoriteIds: Set<String>,
        wrongIds: Set<String>,
        size: Int = DECK_SIZE,
        random: Random = Random.Default,
    ): List<WordCard> {
        val words = all.filter { !it.word.isNullOrBlank() }
        val pool = when (deck) {
            Deck.FAVORITES -> words.filter { it.id in favoriteIds }
            Deck.WRONG -> words.filter { it.id in wrongIds }
            else -> words
        }
        val ordered = when (deck) {
            Deck.WEAK -> pool.shuffled(random).sortedBy { q ->
                val st = stats[q.id]
                when {
                    st == null || st.attempts == 0 -> 0.0
                    else -> 1.0 + st.accuracy
                }
            }
            else -> pool.shuffled(random)
        }
        return ordered.take(size).mapNotNull { cardOf(it) }
    }
}
