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

    /** 做過幾道「不同的」題目 */
    @Query("SELECT COUNT(DISTINCT questionId) FROM attempts")
    fun observeDistinctQuestions(): Flow<Int>

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

@Dao
interface ReviewCardDao {
    @Query("SELECT * FROM review_cards WHERE questionId = :questionId")
    suspend fun get(questionId: String): ReviewCardEntity?

    @Upsert
    suspend fun upsert(card: ReviewCardEntity)

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueEpochDay <= :today")
    fun observeDueCount(today: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueEpochDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT * FROM review_cards WHERE dueEpochDay <= :today ORDER BY dueEpochDay, questionId LIMIT :limit")
    suspend fun getDue(today: Long, limit: Int): List<ReviewCardEntity>

    @Query("SELECT * FROM review_cards")
    suspend fun getAll(): List<ReviewCardEntity>

    @Upsert
    suspend fun upsertAll(items: List<ReviewCardEntity>)

    @Query("DELETE FROM review_cards")
    suspend fun clear()
}

@Dao
interface StudyDayDao {
    @Query("SELECT * FROM study_days WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): StudyDayEntity?

    @Upsert
    suspend fun upsert(day: StudyDayEntity)

    @Query("SELECT epochDay FROM study_days")
    fun observeDays(): Flow<List<Long>>

    @Query("SELECT epochDay FROM study_days")
    suspend fun getDays(): List<Long>

    @Query("SELECT * FROM study_days")
    suspend fun getAll(): List<StudyDayEntity>

    @Upsert
    suspend fun upsertAll(items: List<StudyDayEntity>)

    @Query("DELETE FROM study_days")
    suspend fun clear()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeCount(): Flow<Int>

    @Query("SELECT questionId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE questionId = :questionId)")
    suspend fun exists(questionId: String): Boolean

    @Upsert
    suspend fun upsert(item: FavoriteEntity)

    @Upsert
    suspend fun upsertAll(items: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE questionId = :questionId")
    suspend fun delete(questionId: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}
