package io.github.zdyoyoyo.englishpractice.sync

import io.github.zdyoyoyo.englishpractice.data.local.AppDatabase
import io.github.zdyoyoyo.englishpractice.data.local.AttemptDao
import io.github.zdyoyoyo.englishpractice.data.local.AttemptEntity
import io.github.zdyoyoyo.englishpractice.data.local.FavoriteDao
import io.github.zdyoyoyo.englishpractice.data.local.FavoriteEntity
import io.github.zdyoyoyo.englishpractice.data.local.MockResultDao
import io.github.zdyoyoyo.englishpractice.data.local.MockResultEntity
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardDao
import io.github.zdyoyoyo.englishpractice.data.local.ReviewCardEntity
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayDao
import io.github.zdyoyoyo.englishpractice.data.local.StudyDayEntity
import io.github.zdyoyoyo.englishpractice.data.local.TransactionRunner
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerDao
import io.github.zdyoyoyo.englishpractice.data.local.WrongAnswerEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 備份到 Google Drive 的內容：使用者全部的學習資料（不含任何個人身分資訊）。
 * 整份資料一起備份 / 還原，衝突時以 lastLocalChangeAt 較新的一方為準。
 */
@Serializable
data class BackupSnapshot(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    /** 這份資料最後一次被「學習行為」改動的時間 */
    val lastLocalChangeAt: Long,
    val attempts: List<AttemptEntity> = emptyList(),
    val wrongAnswers: List<WrongAnswerEntity> = emptyList(),
    val reviewCards: List<ReviewCardEntity> = emptyList(),
    val studyDays: List<StudyDayEntity> = emptyList(),
    val hintCredits: Int = 0,
    /** v2 新增；舊備份沒有這個欄位時視為空清單 */
    val favorites: List<FavoriteEntity> = emptyList(),
    /** v3 新增 */
    val mockResults: List<MockResultEntity> = emptyList(),
    val streakFreezes: Int = 0,
) {
    val isEmpty: Boolean get() = attempts.isEmpty() && wrongAnswers.isEmpty() && reviewCards.isEmpty() &&
        studyDays.isEmpty() && favorites.isEmpty() && mockResults.isEmpty()

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun fromJson(text: String): BackupSnapshot = json.decodeFromString(serializer(), text)
    }
}

/** 把 Room 裡的資料匯出成 BackupSnapshot，或從 BackupSnapshot 整份覆蓋回 Room */
class BackupSerializer(
    private val tx: TransactionRunner,
    private val attemptDao: AttemptDao,
    private val wrongDao: WrongAnswerDao,
    private val reviewDao: ReviewCardDao,
    private val dayDao: StudyDayDao,
    private val favoriteDao: FavoriteDao,
    private val mockDao: MockResultDao,
) {
    constructor(db: AppDatabase, tx: TransactionRunner) :
        this(tx, db.attemptDao(), db.wrongAnswerDao(), db.reviewCardDao(), db.studyDayDao(), db.favoriteDao(), db.mockResultDao())

    suspend fun export(exportedAt: Long, lastLocalChangeAt: Long, hintCredits: Int, streakFreezes: Int = 0): BackupSnapshot = tx.run {
        BackupSnapshot(
            exportedAt = exportedAt,
            lastLocalChangeAt = lastLocalChangeAt,
            attempts = attemptDao.getAll(),
            wrongAnswers = wrongDao.getAll(),
            reviewCards = reviewDao.getAll(),
            studyDays = dayDao.getAll(),
            hintCredits = hintCredits,
            favorites = favoriteDao.getAll(),
            mockResults = mockDao.getAll(),
            streakFreezes = streakFreezes,
        )
    }

    /** 還原：先清空再寫入（在同一個交易中，失敗會整個回滾） */
    suspend fun restore(snapshot: BackupSnapshot) = tx.run {
        clearInternal()
        attemptDao.insertAll(snapshot.attempts)
        wrongDao.upsertAll(snapshot.wrongAnswers)
        reviewDao.upsertAll(snapshot.reviewCards)
        dayDao.upsertAll(snapshot.studyDays)
        favoriteDao.upsertAll(snapshot.favorites)
        mockDao.insertAll(snapshot.mockResults)
    }

    suspend fun clearAll() = tx.run { clearInternal() }

    private suspend fun clearInternal() {
        attemptDao.clear()
        wrongDao.clear()
        reviewDao.clear()
        dayDao.clear()
        favoriteDao.clear()
        mockDao.clear()
    }
}
