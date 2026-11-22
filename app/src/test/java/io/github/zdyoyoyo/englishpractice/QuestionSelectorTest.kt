package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.domain.QuestionSelector
import io.github.zdyoyoyo.englishpractice.fakes.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionSelectorTest {
    private val all = listOf(
        question("1", tags = listOf("travel"), difficulty = 1),
        question("2", tags = listOf("travel", "food"), difficulty = 2),
        question("3", tags = listOf("work"), difficulty = 2),
    ) + (4..30).map { question("$it", tags = listOf("school"), difficulty = 3) }

    @Test fun `filters by tag and difficulty`() {
        assertEquals(setOf("1", "2"), QuestionSelector.select(all, tag = "travel", random = Random(1)).map { it.id }.toSet())
        assertEquals(listOf("2"), QuestionSelector.select(all, tag = "travel", difficulty = 2).map { it.id })
    }

    @Test fun `at most ten unique per session`() {
        val picked = QuestionSelector.select(all, random = Random(42))
        assertEquals(10, picked.size)
        assertEquals(10, picked.map { it.id }.toSet().size)
    }

    @Test fun `lists all tags`() {
        assertTrue(QuestionSelector.allTags(all).containsAll(listOf("travel", "food", "work", "school")))
    }
}
