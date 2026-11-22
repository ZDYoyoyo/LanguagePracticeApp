package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.data.bank.BankStore
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository.RefreshResult
import io.github.zdyoyoyo.englishpractice.data.bank.RemoteBankSource
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class QuestionBankRepositoryTest {

    private fun bankJson(version: Int, ids: List<String> = listOf("a")) =
        """{"version":$version,"questions":[${ids.joinToString(",") {
            """{"id":"$it","type":"grammar","prompt":"p","options":["x","y"],"answer":"x"}"""
        }}]}"""

    private class MemStore(var cached: String?, val bundled: String) : BankStore {
        override fun readCached() = cached
        override fun writeCached(text: String) { cached = text }
        override fun readBundled() = bundled
    }

    private class FakeRemote(val files: Map<String, String>) : RemoteBankSource {
        val requested = mutableListOf<String>()
        override suspend fun fetch(path: String): String {
            requested += path
            return files[path] ?: throw IOException("404")
        }
    }

    @Test fun `uses bundled bank when no cache`() = runTest {
        val repo = QuestionBankRepository(FakeRemote(emptyMap()), MemStore(null, bankJson(1)), 1, StandardTestDispatcher(testScheduler))
        assertEquals(1, repo.loadLocal().version)
    }

    @Test fun `picks newer of cache and bundled`() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        assertEquals(5, QuestionBankRepository(FakeRemote(emptyMap()), MemStore(bankJson(5), bankJson(3)), 1, d).loadLocal().version)
        assertEquals(3, QuestionBankRepository(FakeRemote(emptyMap()), MemStore(bankJson(2), bankJson(3)), 1, d).loadLocal().version)
    }

    @Test fun `downloads full bank only when remote is newer`() = runTest {
        val store = MemStore(null, bankJson(1))
        val remote = FakeRemote(mapOf(
            "version.json" to """{"version":2,"file":"questions.json"}""",
            "questions.json" to bankJson(2, listOf("a", "b")),
        ))
        val repo = QuestionBankRepository(remote, store, 1, StandardTestDispatcher(testScheduler))
        val result = repo.refreshIfNeeded()
        assertEquals(RefreshResult.Updated(2, 2), result)
        assertEquals(2, repo.currentVersion)
        assertTrue(store.cached!!.contains("\"version\":2"))
    }

    @Test fun `same version only fetches manifest`() = runTest {
        val remote = FakeRemote(mapOf("version.json" to """{"version":1}"""))
        val repo = QuestionBankRepository(remote, MemStore(null, bankJson(1)), 1, StandardTestDispatcher(testScheduler))
        assertEquals(RefreshResult.UpToDate, repo.refreshIfNeeded())
        assertEquals(listOf("version.json"), remote.requested)
    }

    @Test fun `offline keeps local bank`() = runTest {
        val repo = QuestionBankRepository(FakeRemote(emptyMap()), MemStore(null, bankJson(1)), 1, StandardTestDispatcher(testScheduler))
        assertTrue(repo.refreshIfNeeded() is RefreshResult.Failed)
        assertEquals(1, repo.currentVersion)
    }

    @Test fun `broken download does not overwrite cache`() = runTest {
        val store = MemStore(null, bankJson(1))
        val remote = FakeRemote(mapOf("version.json" to """{"version":2}""", "questions.json" to "{broken"))
        val repo = QuestionBankRepository(remote, store, 1, StandardTestDispatcher(testScheduler))
        assertTrue(repo.refreshIfNeeded() is RefreshResult.Failed)
        assertNull(store.cached)
        assertEquals(1, repo.currentVersion)
    }

    @Test fun `app too old does not update`() = runTest {
        val remote = FakeRemote(mapOf("version.json" to """{"version":2,"minAppVersionCode":9}"""))
        val repo = QuestionBankRepository(remote, MemStore(null, bankJson(1)), 1, StandardTestDispatcher(testScheduler))
        assertEquals(RefreshResult.AppTooOld(9), repo.refreshIfNeeded())
    }
}
