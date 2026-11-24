package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.data.local.AppDatabase
import io.github.zdyoyoyo.englishpractice.fakes.DirectTx
import io.github.zdyoyoyo.englishpractice.fakes.FakeAttemptDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeClock
import io.github.zdyoyoyo.englishpractice.fakes.FakeFavoriteDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeReviewCardDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeStudyDayDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeWrongAnswerDao
import io.github.zdyoyoyo.englishpractice.fakes.question
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FavoritesTest {
    private val clock = FakeClock()
    private val cards = FakeReviewCardDao()
    private val wrongs = FakeWrongAnswerDao()
    private val favs = FakeFavoriteDao()
    private val repo = StudyRepository(DirectTx, FakeAttemptDao(), wrongs, cards, FakeStudyDayDao(), favs, clock)
    private val today get() = clock.today().toEpochDay()

    @Test fun `toggle favorite on and off`() = runTest {
        assertTrue(repo.toggleFavorite("q1"))
        assertTrue(repo.isFavorite("q1"))
        assertEquals(1, repo.favoriteCount.first())
        assertFalse(repo.toggleFavorite("q1"))
        assertEquals(0, repo.favoriteCount.first())
    }

    @Test fun `guessed correct answer is favorited and due tomorrow, not in wrong book`() = runTest {
        val q = question("q1")
        // 已經學很久的題目，下次複習本來在很久以後
        repeat(3) {
            repo.recordAnswer(q, "a", true, 1000, StudyMode.REVIEW)
        }
        assertTrue(cards.get("q1")!!.dueEpochDay > today + 1)

        repo.markGuessed("q1")
        val card = cards.get("q1")!!
        assertEquals(today + 1, card.dueEpochDay)
        assertEquals(0, card.repetitions)
        assertEquals(listOf("q1"), repo.favoriteQuestionIds())
        assertTrue(repo.activeWrongQuestionIds().isEmpty())
    }

    @Test fun `practicing favorites correctly keeps them favorited`() = runTest {
        repo.toggleFavorite("q1")
        repo.recordAnswer(question("q1"), "a", true, 1000, StudyMode.FAVORITES)
        assertEquals(listOf("q1"), repo.favoriteQuestionIds())
    }

    /** Migration 建的表必須和 Room 產生的 schema 完全一致，否則已安裝的使用者升級會閃退 */
    @Test fun `migration 1 to 2 SQL matches exported schema`() {
        val schema = listOf(
            File("schemas/io.github.zdyoyoyo.englishpractice.data.local.AppDatabase/2.json"),
            File("app/schemas/io.github.zdyoyoyo.englishpractice.data.local.AppDatabase/2.json"),
        ).first { it.exists() }.readText()
        val expected = Regex("\"tableName\": \"favorites\",\\s*\"createSql\": \"(.*?)\",").find(schema)!!
            .groupValues[1].replace("\\u0060", "`").replace("\${TABLE_NAME}", "favorites")
        assertEquals(expected, AppDatabase.CREATE_FAVORITES_SQL)
    }

    @Test fun `migration 2 to 3 SQL matches exported schema`() {
        val schema = listOf(
            File("schemas/io.github.zdyoyoyo.englishpractice.data.local.AppDatabase/3.json"),
            File("app/schemas/io.github.zdyoyoyo.englishpractice.data.local.AppDatabase/3.json"),
        ).first { it.exists() }.readText()
        val table = Regex("\"tableName\": \"mock_results\",\\s*\"createSql\": \"(.*?)\",").find(schema)!!
            .groupValues[1].replace("\\u0060", "`").replace("\${TABLE_NAME}", "mock_results")
        assertEquals(table, AppDatabase.CREATE_MOCK_RESULTS_SQL)
        val index = Regex("\"name\": \"index_mock_results_takenAt\",.*?\"createSql\": \"(.*?)\"", RegexOption.DOT_MATCHES_ALL).find(schema)!!
            .groupValues[1].replace("\\u0060", "`").replace("\${TABLE_NAME}", "mock_results")
        assertEquals(index, AppDatabase.CREATE_MOCK_RESULTS_INDEX_SQL)
    }
}
