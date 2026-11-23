package io.github.zdyoyoyo.englishpractice.speech

/** 把聽力對話稿拆成一句一句，並依說話者決定音高（純邏輯，可測試） */
object DialogueScript {
    data class Line(val speaker: String?, val text: String, val pitch: Float)

    private val label = Regex("^\\s*(M|W|Man|Woman|A|B)\\s*[:：]\\s*", RegexOption.IGNORE_CASE)

    fun parse(script: String): List<Line> = script.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { raw ->
            val m = label.find(raw)
            val speaker = m?.groupValues?.get(1)?.uppercase()
            val text = if (m != null) raw.substring(m.range.last + 1).trim() else raw
            val pitch = when (speaker) {
                "M", "MAN", "A" -> 0.85f
                "W", "WOMAN", "B" -> 1.2f
                else -> 1.0f
            }
            Line(speaker, text, pitch)
        }
}
