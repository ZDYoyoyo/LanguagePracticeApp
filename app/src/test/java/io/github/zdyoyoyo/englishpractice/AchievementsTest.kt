package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.domain.AchievementContext
import io.github.zdyoyoyo.englishpractice.domain.Achievements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {
    @Test fun `ids are unique`() {
        assertEquals(Achievements.ALL.size, Achievements.ALL.map { it.id }.toSet().size)
    }

    @Test fun `nothing unlocked for a new user`() {
        assertTrue(Achievements.newlyUnlocked(AchievementContext(), emptySet()).isEmpty())
    }

    @Test fun `unlocks by thresholds and skips already unlocked`() {
        val ctx = AchievementContext(totalAttempts = 120, totalCorrect = 80, longestStreak = 7, bestMockScore = 845, mockCount = 1)
        val ids = Achievements.newlyUnlocked(ctx, emptySet()).map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("first_step", "answers_100", "streak_3", "streak_7", "mock_first", "mock_600", "mock_800")))
        assertTrue("answers_500" !in ids && "mock_900" !in ids && "streak_30" !in ids)
        val again = Achievements.newlyUnlocked(ctx, ids)
        assertTrue(again.isEmpty())
    }

    @Test fun `progress is capped at target`() {
        val a = Achievements.byId("answers_100")!!
        assertEquals(100, a.current(AchievementContext(totalAttempts = 250)))
        assertEquals(42, a.current(AchievementContext(totalAttempts = 42)))
    }
}
