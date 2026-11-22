package io.github.zdyoyoyo.englishpractice.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * Room 的 @Entity ≈ JPA 的 @Entity：一個 class 對應一張 SQLite 資料表。
 * 加上 @Serializable 是為了之後備份到 Google Drive 時可以直接轉成 JSON。
 */

/** 每一次作答紀錄 */
@Serializable
@Entity(tableName = "attempts", indices = [Index("questionId"), Index("answeredAt")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val correct: Boolean,
    val answeredAt: Long,
    val elapsedMs: Long,
    val mode: String,
)

/** 錯題本：每題一列，答錯自動收錄；在錯題本重做答對後標記為已掌握 */
@Serializable
@Entity(tableName = "wrong_answers")
data class WrongAnswerEntity(
    @PrimaryKey val questionId: String,
    val wrongCount: Int,
    val lastWrongAnswer: String,
    val lastWrongAt: Long,
    val mastered: Boolean = false,
    val updatedAt: Long,
)
