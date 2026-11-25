package io.github.zdyoyoyo.englishpractice.data

import io.github.zdyoyoyo.englishpractice.data.local.QuestionNoteDao
import io.github.zdyoyoyo.englishpractice.data.local.QuestionNoteEntity
import io.github.zdyoyoyo.englishpractice.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 題目筆記：題目 id → 筆記文字。空白筆記 = 刪除 */
class NoteRepository(
    private val dao: QuestionNoteDao,
    private val clock: AppClock,
    private val onLocalDataChanged: (Long) -> Unit = {},
) {
    val notes: Flow<Map<String, String>> = dao.observeAll().map { list -> list.associate { it.questionId to it.text } }

    suspend fun save(questionId: String, text: String) {
        val now = clock.nowMillis()
        if (text.isBlank()) dao.delete(questionId) else dao.upsert(QuestionNoteEntity(questionId, text.trim(), now))
        onLocalDataChanged(now)
    }

    companion object {
        const val MAX_LENGTH = 500
    }
}
