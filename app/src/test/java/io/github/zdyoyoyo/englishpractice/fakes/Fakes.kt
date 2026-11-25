package io.github.zdyoyoyo.englishpractice.fakes

import io.github.zdyoyoyo.englishpractice.data.local.AttemptDao
import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.FavoriteDao
import io.github.zdyoyoyo.englishpractice.data.local.FavoriteEntity
import io.github.zdyoyoyo.englishpractice.data.local.MockResultDao
import io.github.zdyoyoyo.englishpractice.data.local.MockResultEntity
import io.github.zdyoyoyo.englishpractice.data.local.QuestionNoteDao
import io.github.zdyoyoyo.englishpractice.data.local.QuestionNoteEntity
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardDao
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayDao
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import io.github.zdyoyoyo.englishpractice.data.local.TransactionRunner
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerDao
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerEntity
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.data.model.QuestionType
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset

/* 單元測試用的假實作：用記憶體裡的 Map 模擬資料庫，不需要 Android 模擬器 */

class FakeClock(var date: LocalDate = LocalDate.of(2026, 1, 1), var millisOfDay: Long = 9 * 3600_000L) : AppClock {
    override fun nowMillis(): Long = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() + millisOfDay
    override fun today(): LocalDate = date
    fun advanceDays(days: Long) { date = date.plusDays(days) }
}

object DirectTx : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

class FakeAttemptDao : AttemptDao {
    val items = MutableStateFlow<List<AttemptEntity>>(emptyList())
    override suspend fun insert(attempt: AttemptEntity): Long {
        val id = items.value.size + 1L
        items.value = items.value + attempt.copy(id = id)
        return id
    }
    override fun observeTotal(): Flow<Int> = items.map { it.size }
    override fun observeDistinctQuestions(): Flow<Int> = items.map { list -> list.map { it.questionId }.distinct().size }
    override suspend fun getAll() = items.value
    override suspend fun insertAll(items: List<AttemptEntity>) { this.items.value = this.items.value + items }
    override suspend fun clear() { items.value = emptyList() }
}

class FakeWrongAnswerDao : WrongAnswerDao {
    val map = MutableStateFlow<Map<String, WrongAnswerEntity>>(emptyMap())
    private fun active() = map.value.values.filter { !it.mastered }.sortedByDescending { it.lastWrongAt }
    override fun observeActive(): Flow<List<WrongAnswerEntity>> = map.map { active() }
    override fun observeActiveCount(): Flow<Int> = map.map { active().size }
    override suspend fun masteredCount() = map.value.values.count { it.mastered }
    override suspend fun getActive() = active()
    override suspend fun get(questionId: String) = map.value[questionId]
    override suspend fun upsert(item: WrongAnswerEntity) { map.value = map.value + (item.questionId to item) }
    override suspend fun getAll() = map.value.values.toList()
    override suspend fun upsertAll(items: List<WrongAnswerEntity>) { items.forEach { upsert(it) } }
    override suspend fun delete(questionId: String) { map.value = map.value - questionId }
    override suspend fun clear() { map.value = emptyMap() }
}

fun question(
    id: String,
    options: List<String> = listOf("a", "b", "c"),
    answer: String = "a",
    tags: List<String> = listOf("daily-life"),
    difficulty: Int = 1,
    type: QuestionType = QuestionType.GRAMMAR,
    accepted: List<String> = emptyList(),
    word: String? = null,
) = Question(
    id = id, type = type, prompt = "prompt $id", word = word, options = options, answer = answer,
    acceptedAnswers = accepted, explanation = "because", difficulty = difficulty, tags = tags,
)

class FakeReviewCardDao : ReviewCardDao {
    val map = MutableStateFlow<Map<String, ReviewCardEntity>>(emptyMap())
    override suspend fun get(questionId: String) = map.value[questionId]
    override suspend fun upsert(card: ReviewCardEntity) { map.value = map.value + (card.questionId to card) }
    override fun observeDueCount(today: Long): Flow<Int> = map.map { m -> m.values.count { it.dueEpochDay <= today } }
    override suspend fun dueCount(today: Long) = map.value.values.count { it.dueEpochDay <= today }
    override suspend fun getDue(today: Long, limit: Int) =
        map.value.values.filter { it.dueEpochDay <= today }.sortedWith(compareBy({ it.dueEpochDay }, { it.questionId })).take(limit)
    override suspend fun getAll() = map.value.values.toList()
    override suspend fun upsertAll(items: List<ReviewCardEntity>) { items.forEach { upsert(it) } }
    override suspend fun clear() { map.value = emptyMap() }
}

class FakeStudyDayDao : StudyDayDao {
    val map = MutableStateFlow<Map<Long, StudyDayEntity>>(emptyMap())
    override suspend fun get(epochDay: Long) = map.value[epochDay]
    override suspend fun upsert(day: StudyDayEntity) { map.value = map.value + (day.epochDay to day) }
    override fun observeDays(): Flow<List<Long>> = map.map { it.keys.toList() }
    override fun observeDay(epochDay: Long): Flow<StudyDayEntity?> = map.map { it[epochDay] }
    override suspend fun getDays() = map.value.keys.toList()
    override suspend fun getAll() = map.value.values.toList()
    override suspend fun upsertAll(items: List<StudyDayEntity>) { items.forEach { upsert(it) } }
    override suspend fun clear() { map.value = emptyMap() }
}

class FakeFavoriteDao : FavoriteDao {
    val map = MutableStateFlow<Map<String, FavoriteEntity>>(emptyMap())
    private fun sorted() = map.value.values.sortedByDescending { it.addedAt }
    override fun observeAll(): Flow<List<FavoriteEntity>> = map.map { sorted() }
    override fun observeCount(): Flow<Int> = map.map { it.size }
    override fun observeIds(): Flow<List<String>> = map.map { m -> m.keys.toList() }
    override suspend fun getAll() = sorted()
    override suspend fun exists(questionId: String) = questionId in map.value
    override suspend fun upsert(item: FavoriteEntity) { map.value = map.value + (item.questionId to item) }
    override suspend fun upsertAll(items: List<FavoriteEntity>) { items.forEach { upsert(it) } }
    override suspend fun delete(questionId: String) { map.value = map.value - questionId }
    override suspend fun clear() { map.value = emptyMap() }
}

class FakeMockResultDao : MockResultDao {
    val items = MutableStateFlow<List<MockResultEntity>>(emptyList())
    override suspend fun insert(result: MockResultEntity): Long {
        val id = items.value.size + 1L
        items.value = items.value + result.copy(id = id)
        return id
    }
    override fun observeRecent(limit: Int): Flow<List<MockResultEntity>> = items.map { it.sortedByDescending { r -> r.takenAt }.take(limit) }
    override suspend fun getAll() = items.value
    override suspend fun insertAll(items: List<MockResultEntity>) { this.items.value = this.items.value + items }
    override suspend fun clear() { items.value = emptyList() }
}

class FakeQuestionNoteDao : QuestionNoteDao {
    val map = MutableStateFlow<Map<String, QuestionNoteEntity>>(emptyMap())
    override fun observeAll(): Flow<List<QuestionNoteEntity>> = map.map { it.values.toList() }
    override suspend fun getAll() = map.value.values.toList()
    override suspend fun upsert(note: QuestionNoteEntity) { map.value = map.value + (note.questionId to note) }
    override suspend fun upsertAll(items: List<QuestionNoteEntity>) { items.forEach { upsert(it) } }
    override suspend fun delete(questionId: String) { map.value = map.value - questionId }
    override suspend fun clear() { map.value = emptyMap() }
}
