package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.domain.MockScore
import io.github.zdyoyoyo.englishpractice.domain.StatsCalculator
import io.github.zdyoyoyo.englishpractice.share.ShareCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareCardTest {
    @Test fun `mock share card shows score and breakdown`() {
        val c = ShareCards.forMock(MockScore(10, 10, 7, 10), streakDays = 5)
        assertEquals("845", c.bigValue)
        assertTrue(c.lines.any { "10/10" in it && "7/10" in it })
        assertTrue(c.lines.any { "5 天" in it })
        assertTrue(ShareCards.forMock(MockScore(1, 2, 1, 2), 0).lines.none { "打卡" in it })
    }

    @Test fun `progress share card skips missing data`() {
        val s = StatsCalculator.summarize(emptyList(), emptyList(), emptyList(), emptyList(), 1)
        val c = ShareCards.forProgress(s, bestMock = null, achievementCount = 0)
        assertTrue(c.lines.none { "模擬考" in it || "成就" in it })
        val c2 = ShareCards.forProgress(s, bestMock = 700, achievementCount = 3)
        assertTrue(c2.lines.any { "700" in it } && c2.lines.any { "3 個成就" in it })
    }
}
