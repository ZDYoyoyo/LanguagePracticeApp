package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.fakes.DirectTx
import io.github.zdyoyoyo.englishpractice.fakes.FakeAttemptDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeClock
import io.github.zdyoyoyo.englishpractice.fakes.FakeFavoriteDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeReviewCardDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeStudyDayDao
import io.github.zdyoyoyo.englishpractice.fakes.FakeWrongAnswerDao
import io.github.zdyoyoyo.englishpractice.fakes.question
import io.github.zdyoyoyo.englishpractice.sync.BackupSerializer
import io.github.zdyoyoyo.englishpractice.sync.BackupSnapshot
import io.github.zdyoyoyo.englishpractice.sync.RemoteBackupStore
import io.github.zdyoyoyo.englishpractice.sync.SyncManager
import io.github.zdyoyoyo.englishpractice.sync.SyncOutcome
import io.github.zdyoyoyo.englishpractice.sync.SyncPolicy
import io.github.zdyoyoyo.englishpractice.sync.SyncPolicy.Action
import io.github.zdyoyoyo.englishpractice.sync.SyncState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPolicyTest {
    private fun decide(local: Long, localData: Boolean, lastSync: Long, remote: Long?, remoteData: Boolean = remote != null) =
        SyncPolicy.decide(local, localData, lastSync, remote, remoteData)

    @Test fun `no remote uploads local data`() {
        assertEquals(Action.UPLOAD, decide(100, true, 0, null))
        assertEquals(Action.NOTHING, decide(0, false, 0, null))
    }

    @Test fun `newer side wins`() {
        assertEquals(Action.DOWNLOAD, decide(100, true, 50, 200))
        assertEquals(Action.UPLOAD, decide(300, true, 50, 200))
        assertEquals(Action.NOTHING, decide(200, true, 50, 200))
    }

    @Test fun `fresh phone with no data restores`() {
        assertEquals(Action.DOWNLOAD, decide(0, false, 0, 200))
    }

    @Test fun `first sync with data on both sides asks user`() {
        assertEquals(Action.ASK_USER, decide(500, true, 0, 200))
    }
}

class SyncManagerTest {
    private class FakeState : SyncState {
        override var lastLocalChangeAt = 0L
        override var lastSyncedAt = 0L
        override var hintCredits = 0
    }

    private class MemStore(var content: String? = null) : RemoteBackupStore {
        override suspend fun read() = content
        override suspend fun write(content: String) { this.content = content }
        override suspend fun delete() { content = null }
    }

    /** 模擬一支手機：自己的資料庫 + 設定 */
    private inner class Phone {
        val attempts = FakeAttemptDao()
        val wrongs = FakeWrongAnswerDao()
        val cards = FakeReviewCardDao()
        val days = FakeStudyDayDao()
        val favs = FakeFavoriteDao()
        val state = FakeState()
        val study = StudyRepository(DirectTx, attempts, wrongs, cards, days, favs, clock, { state.lastLocalChangeAt = it })
        val sync = SyncManager(BackupSerializer(DirectTx, attempts, wrongs, cards, days, favs), state, clock)
    }

    private val clock = FakeClock()
    private val cloud = MemStore()

    @Test fun `backup on phone A and restore on new phone B`() = runTest {
        val a = Phone()
        a.study.recordAnswer(question("q1"), "b", false, 1000, StudyMode.PRACTICE)
        a.state.hintCredits = 3
        a.study.toggleFavorite("q7")
        assertEquals(SyncOutcome.Uploaded, a.sync.sync(cloud))

        val b = Phone()
        assertEquals(SyncOutcome.Downloaded, b.sync.sync(cloud))
        assertEquals(1, b.attempts.items.value.size)
        assertEquals(listOf("q1"), b.study.activeWrongQuestionIds())
        assertTrue(b.cards.get("q1") != null)
        assertEquals(3, b.state.hintCredits)
        assertEquals(listOf("q7"), b.study.favoriteQuestionIds())
        assertEquals(a.state.lastLocalChangeAt, b.state.lastLocalChangeAt)
    }

    @Test fun `newer cloud copy overwrites older local after both synced`() = runTest {
        val a = Phone(); val b = Phone()
        a.study.recordAnswer(question("q1"), "b", false, 1000, StudyMode.PRACTICE)
        a.sync.sync(cloud)
        b.sync.sync(cloud) // B 還原

        clock.millisOfDay += 60_000
        a.study.recordAnswer(question("q2"), "b", false, 1000, StudyMode.PRACTICE)
        assertEquals(SyncOutcome.Uploaded, a.sync.sync(cloud))
        assertEquals(SyncOutcome.Downloaded, b.sync.sync(cloud))
        assertEquals(setOf("q1", "q2"), b.study.activeWrongQuestionIds().toSet())
    }

    @Test fun `first sync with data on both sides needs user choice then forced restore`() = runTest {
        val a = Phone()
        a.study.recordAnswer(question("q1"), "b", false, 1000, StudyMode.PRACTICE)
        a.sync.sync(cloud)

        val b = Phone()
        clock.millisOfDay += 60_000
        b.study.recordAnswer(question("q9"), "b", false, 1000, StudyMode.PRACTICE)
        assertTrue(b.sync.sync(cloud) is SyncOutcome.NeedsChoice)

        assertEquals(SyncOutcome.Downloaded, b.sync.sync(cloud, force = Action.DOWNLOAD))
        assertEquals(listOf("q1"), b.study.activeWrongQuestionIds())
    }

    @Test fun `corrupted cloud file reports failure without touching local`() = runTest {
        val a = Phone()
        a.study.recordAnswer(question("q1"), "b", false, 1000, StudyMode.PRACTICE)
        cloud.content = "{not json"
        assertTrue(a.sync.sync(cloud) is SyncOutcome.Failed)
        assertEquals(listOf("q1"), a.study.activeWrongQuestionIds())
    }

    @Test fun `delete remote and clear local`() = runTest {
        val a = Phone()
        a.study.recordAnswer(question("q1"), "b", false, 1000, StudyMode.PRACTICE)
        a.sync.sync(cloud)
        a.sync.deleteRemote(cloud)
        assertNull(cloud.content)
        a.sync.clearLocal()
        assertTrue(a.attempts.items.value.isEmpty())
        assertTrue(a.study.activeWrongQuestionIds().isEmpty())
        assertEquals(0L, a.state.lastLocalChangeAt)
    }

    @Test fun `snapshot json round trip ignores unknown fields`() {
        val snap = BackupSnapshot(exportedAt = 1, lastLocalChangeAt = 2, hintCredits = 5)
        assertEquals(snap, BackupSnapshot.fromJson(snap.toJson()))
        // 舊版備份（沒有 favorites 欄位）也要能讀
        assertTrue(BackupSnapshot.fromJson("""{"exportedAt":1,"lastLocalChangeAt":2}""").favorites.isEmpty())
        val future = """{"formatVersion":2,"exportedAt":1,"lastLocalChangeAt":2,"newField":[1,2]}"""
        assertEquals(2L, BackupSnapshot.fromJson(future).lastLocalChangeAt)
    }
}
