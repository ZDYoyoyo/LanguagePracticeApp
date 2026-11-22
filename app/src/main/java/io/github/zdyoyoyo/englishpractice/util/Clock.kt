package io.github.zdyoyoyo.englishpractice.util

import java.time.LocalDate
import java.time.ZoneId

/** 時間來源抽成介面：正式環境用系統時間，測試時可以固定成某一天 */
interface AppClock {
    fun nowMillis(): Long
    fun today(): LocalDate
}

object SystemClock : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}
