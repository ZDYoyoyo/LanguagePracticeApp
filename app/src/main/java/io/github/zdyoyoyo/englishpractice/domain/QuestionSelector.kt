package io.github.zdyoyoyo.englishpractice.domain

import io.github.zdyoyoyo.englishpractice.data.model.Question
import kotlin.random.Random

/** 依主題 / 難度挑題（純邏輯，方便測試） */
object QuestionSelector {
    const val DEFAULT_SESSION_SIZE = 10

    fun select(
        all: List<Question>,
        tag: String? = null,
        difficulty: Int? = null,
        limit: Int = DEFAULT_SESSION_SIZE,
        random: Random = Random.Default,
    ): List<Question> = all
        .filter { tag == null || tag in it.tags }
        .filter { difficulty == null || it.difficulty == difficulty }
        .shuffled(random)
        .take(limit)

    fun allTags(all: List<Question>): List<String> = all.flatMap { it.tags }.distinct().sorted()
}
