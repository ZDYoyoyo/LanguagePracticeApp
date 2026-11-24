package io.github.zdyoyoyo.englishpractice.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AttemptEntity::class,
        WrongAnswerEntity::class,
        ReviewCardEntity::class,
        StudyDayEntity::class,
        FavoriteEntity::class,
        MockResultEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attemptDao(): AttemptDao
    abstract fun wrongAnswerDao(): WrongAnswerDao
    abstract fun reviewCardDao(): ReviewCardDao
    abstract fun studyDayDao(): StudyDayDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun mockResultDao(): MockResultDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "english_practice.db")
                // 改資料表結構時：版本號 +1，並寫一個 Migration，已安裝的使用者資料才不會遺失
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        const val CREATE_FAVORITES_SQL =
            "CREATE TABLE IF NOT EXISTS `favorites` (`questionId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`questionId`))"

        const val CREATE_MOCK_RESULTS_SQL =
            "CREATE TABLE IF NOT EXISTS `mock_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `takenAt` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `listeningCorrect` INTEGER NOT NULL, `listeningTotal` INTEGER NOT NULL, `readingCorrect` INTEGER NOT NULL, `readingTotal` INTEGER NOT NULL, `estimatedScore` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL)"
        const val CREATE_MOCK_RESULTS_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS `index_mock_results_takenAt` ON `mock_results` (`takenAt`)"

        /** v2 → v3：新增「模擬考成績」資料表 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_MOCK_RESULTS_SQL)
                db.execSQL(CREATE_MOCK_RESULTS_INDEX_SQL)
            }
        }

        /** v1 → v2：新增「收藏」資料表 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_FAVORITES_SQL)
            }
        }
    }
}

/** 交易包裝成介面，單元測試時可換成「直接執行」的假實作 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner(private val db: AppDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
