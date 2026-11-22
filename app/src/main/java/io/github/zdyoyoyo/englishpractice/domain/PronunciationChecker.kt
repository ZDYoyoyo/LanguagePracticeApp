package io.github.zdyoyoyo.englishpractice.domain

/** 比對語音辨識結果與目標單字（忽略大小寫、標點、多餘空白） */
object PronunciationChecker {
    fun matches(target: String, recognized: List<String>): Boolean {
        val t = normalize(target)
        return t.isNotEmpty() && recognized.any { normalize(it) == t }
    }

    fun normalize(s: String): String = s.lowercase()
        .replace('’', '\'')
        .replace(Regex("[^a-z0-9' ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
