package io.github.zdyoyoyo.englishpractice.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/* Room 的 @Dao ≈ Spring Data Repository：只寫介面，SQL 由編譯期（KSP）產生實作 */

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Query("SELECT COUNT(*) FROM attempts")
    fun observeTotal(): Flow<Int>

    @Query("SELECT * FROM attempts ORDER BY answeredAt")
    suspend fun getAll(): List<AttemptEntity>

    @Insert
    suspend fun insertAll(items: List<AttemptEntity>)

    @Query("DELETE FROM attempts")
    suspend fun clear()
}

@Dao
interface WrongAnswerDao {
    @Query("SELECT * FROM wrong_answers WHERE mastered = 0 ORDER BY lastWrongAt DESC")
    fun observeActive(): Flow<List<WrongAnswerEntity>>

    @Query("SELECT COUNT(*) FROM wrong_answers WHERE mastered = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM wrong_answers WHERE mastered = 0 ORDER BY lastWrongAt DESC")
    suspend fun getActive(): List<WrongAnswerEntity>

    @Query("SELECT * FROM wrong_answers WHERE questionId = :questionId")
    suspend fun get(questionId: String): WrongAnswerEntity?

    @Upsert
    suspend fun upsert(item: WrongAnswerEntity)

    @Query("SELECT * FROM wrong_answers")
    suspend fun getAll(): List<WrongAnswerEntity>

    @Upsert
    suspend fun upsertAll(items: List<WrongAnswerEntity>)

    @Query("DELETE FROM wrong_answers WHERE questionId = :questionId")
    suspend fun delete(questionId: String)

    @Query("DELETE FROM wrong_answers")
    suspend fun clear()
}
