package io.github.zdyoyoyo.englishpractice.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [
        AttemptEntity::class,
        WrongAnswerEntity::class,
        ReviewCardEntity::class,
        StudyDayEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attemptDao(): AttemptDao
    abstract fun wrongAnswerDao(): WrongAnswerDao
    abstract fun reviewCardDao(): ReviewCardDao
    abstract fun studyDayDao(): StudyDayDao

    companion object {
        fun create(context: Context): AppDatabase =
            // 上架前 schema 還會變動；正式上架後若要改資料表，請改用 Migration 而不是升版號就好
            Room.databaseBuilder(context, AppDatabase::class.java, "english_practice.db").build()
    }
}

/** 交易包裝成介面，單元測試時可換成「直接執行」的假實作 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner(private val db: AppDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
