package io.github.zdyoyoyo.englishpractice

import android.content.Context
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.data.bank.FileBankStore
import io.github.zdyoyoyo.englishpractice.data.bank.HttpRemoteBankSource
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.local.AppDatabase
import io.github.zdyoyoyo.englishpractice.data.local.RoomTransactionRunner
import io.github.zdyoyoyo.englishpractice.data.prefs.AppPreferences
import io.github.zdyoyoyo.englishpractice.util.AppClock
import io.github.zdyoyoyo.englishpractice.util.SystemClock

/**
 * 手動依賴注入（Manual DI）：整個 App 共用的物件都在這裡建立一次。
 * 概念等同 Spring 的 ApplicationContext，但不用框架，專案小時最好懂。
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: AppClock = SystemClock
    val prefs = AppPreferences(appContext)
    val database = AppDatabase.create(appContext)

    val questionBank = QuestionBankRepository(
        remote = HttpRemoteBankSource(BuildConfig.QUESTION_BANK_BASE_URL),
        store = FileBankStore(appContext),
        appVersionCode = BuildConfig.VERSION_CODE,
    )

    val study = StudyRepository(
        tx = RoomTransactionRunner(database),
        attemptDao = database.attemptDao(),
        wrongDao = database.wrongAnswerDao(),
        reviewDao = database.reviewCardDao(),
        dayDao = database.studyDayDao(),
        clock = clock,
        onLocalDataChanged = { prefs.lastLocalChangeAt = it },
    )
}
