package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.ads.AdPolicy
import io.github.zdyoyoyo.englishpractice.ads.HintWallet
import io.github.zdyoyoyo.englishpractice.domain.HintLogic
import io.github.zdyoyoyo.englishpractice.fakes.question
import io.github.zdyoyoyo.englishpractice.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AdsAndHintsTest {
    private val min = 60_000L

    private fun show(total: Int, since: Int, last: Long, now: Long, count: Int = 10) =
        AdPolicy.shouldShowInterstitial(total, since, last, now, count)

    @Test fun `no interstitial during first free sessions`() {
        assertFalse(show(total = 1, since = 1, last = 0, now = 100 * min))
        assertFalse(show(total = 2, since = 2, last = 0, now = 100 * min))
        assertTrue(show(total = 3, since = 3, last = 0, now = 100 * min))
    }

    @Test fun `at most every two sessions and three minutes apart`() {
        assertFalse(show(total = 10, since = 1, last = 0, now = 100 * min))
        assertFalse(show(total = 10, since = 2, last = 99 * min, now = 100 * min))
        assertTrue(show(total = 10, since = 2, last = 97 * min, now = 100 * min))
    }

    @Test fun `short sessions never show interstitial`() {
        assertFalse(show(total = 10, since = 5, last = 0, now = 100 * min, count = 1))
    }

    @Test fun `hint wallet consumes and rewards`() {
        val state = object : SyncState {
            override var lastLocalChangeAt = 0L
            override var lastSyncedAt = 0L
            override var hintCredits = 0
            override var streakFreezes = 0
            override var achievements: Map<String, Long> = emptyMap()
            override var perfectRounds = 0
        }
        val wallet = HintWallet(state) { 42L }
        assertFalse(wallet.tryConsume())
        wallet.add(AdPolicy.HINTS_PER_REWARD)
        assertEquals(3, wallet.credits.value)
        assertTrue(wallet.tryConsume())
        assertEquals(2, state.hintCredits)
        assertEquals(42L, state.lastLocalChangeAt)
    }

    @Test fun `option hint hides two wrong options and never the answer`() {
        val q = question("q", options = listOf("a", "b", "c", "d"), answer = "a")
        repeat(20) {
            val hidden = HintLogic.optionsToHide(q, Random(it))
            assertEquals(2, hidden.size)
            assertFalse("a" in hidden)
        }
        val two = question("q2", options = listOf("a", "b"), answer = "a")
        assertTrue(HintLogic.optionsToHide(two).isEmpty())
    }
}
