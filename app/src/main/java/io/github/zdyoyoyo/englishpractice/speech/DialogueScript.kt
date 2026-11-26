package io.github.zdyoyoyo.englishpractice.speech

/** 把聽力對話稿拆成一句一句，並依說話者決定音高（純邏輯，可測試） */
object DialogueScript {
    data class Line(val speaker: String?, val text: String, val pitch: Float)

    const val MALE_PITCH = 0.85f
    const val FEMALE_PITCH = 1.2f

    private val label = Regex("^\\s*(M|W|Man|Woman|A|B)\\s*[:：]\\s*", RegexOption.IGNORE_CASE)

    /**
     * 圖表題（Part 3 "Look at the graphic"）：對話稿最前面、第一個「M:/W:」之前的文字是圖表。
     * 回傳 (圖表, 對話稿)；沒有圖表時圖表為 null。圖表要一直顯示，但不要念出來。
     */
    fun splitGraphic(passage: String): Pair<String?, String> {
        val lines = passage.lines()
        val first = lines.indexOfFirst { label.containsMatchIn(it) }
        if (first <= 0) return null to passage
        val graphic = lines.take(first).joinToString("\n").trim()
        return graphic.ifEmpty { null } to lines.drop(first).joinToString("\n")
    }

    fun parse(script: String): List<Line> = script.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        // 有標說話者的稿子，沒標的行（例如圖表）不念
        .let { all -> if (all.any { label.containsMatchIn(it) }) all.filter { label.containsMatchIn(it) } else all }
        .map { raw ->
            val m = label.find(raw)
            val speaker = m?.groupValues?.get(1)?.uppercase()
            val text = if (m != null) raw.substring(m.range.last + 1).trim() else raw
            val pitch = when (speaker) {
                "M", "MAN", "A" -> MALE_PITCH
                "W", "WOMAN", "B" -> FEMALE_PITCH
                else -> 1.0f
            }
            Line(speaker, text, pitch)
        }
}
