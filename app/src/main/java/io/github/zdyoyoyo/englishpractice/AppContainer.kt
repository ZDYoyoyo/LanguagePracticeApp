package io.github.zdyoyoyo.englishpractice

import android.content.Context
import io.github.zdyoyoyo.englishpractice.ads.AdsManager
import io.github.zdyoyoyo.englishpractice.ads.HintWallet
import io.github.zdyoyoyo.englishpractice.auth.GoogleAuthManager
import io.github.zdyoyoyo.englishpractice.data.AchievementRepository
import io.github.zdyoyoyo.englishpractice.data.NoteRepository
import io.github.zdyoyoyo.englishpractice.data.ProgressRepository
import io.github.zdyoyoyo.englishpractice.data.StudyRepository
import io.github.zdyoyoyo.englishpractice.data.bank.FileBankStore
import io.github.zdyoyoyo.englishpractice.data.bank.HttpRemoteBankSource
import io.github.zdyoyoyo.englishpractice.data.bank.QuestionBankRepository
import io.github.zdyoyoyo.englishpractice.data.local.AppDatabase
import io.github.zdyoyoyo.englishpractice.data.local.RoomTransactionRunner
import io.github.zdyoyoyo.englishpractice.data.prefs.AppPreferences
import io.github.zdyoyoyo.englishpractice.data.prefs.DisplaySettings
import io.github.zdyoyoyo.englishpractice.sync.BackupSerializer
import io.github.zdyoyoyo.englishpractice.sync.CloudSync
import io.github.zdyoyoyo.englishpractice.sync.SyncManager
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
    val display = DisplaySettings(appContext)
    val database = AppDatabase.create(appContext)
    private val tx = RoomTransactionRunner(database)

    val questionBank = QuestionBankRepository(
        remote = HttpRemoteBankSource(BuildConfig.QUESTION_BANK_BASE_URL),
        store = FileBankStore(appContext),
        appVersionCode = BuildConfig.VERSION_CODE,
    )

    val study = StudyRepository(
        tx = tx,
        attemptDao = database.attemptDao(),
        wrongDao = database.wrongAnswerDao(),
        reviewDao = database.reviewCardDao(),
        dayDao = database.studyDayDao(),
        favoriteDao = database.favoriteDao(),
        clock = clock,
        onLocalDataChanged = { prefs.lastLocalChangeAt = it },
    )

    val progress = ProgressRepository(
        prefs = prefs,
        dayDao = database.studyDayDao(),
        mockDao = database.mockResultDao(),
        clock = clock,
        onLocalDataChanged = { prefs.lastLocalChangeAt = it },
    )

    val notes = NoteRepository(database.questionNoteDao(), clock, onLocalDataChanged = { prefs.lastLocalChangeAt = it })

    val achievements = AchievementRepository(
        prefs = prefs,
        bank = questionBank,
        study = study,
        wrongDao = database.wrongAnswerDao(),
        mockDao = database.mockResultDao(),
        clock = clock,
        onLocalDataChanged = { prefs.lastLocalChangeAt = it },
    )

    val hints = HintWallet(prefs, clock::nowMillis)
    val ads = AdsManager(appContext, clock::nowMillis)

    val auth = GoogleAuthManager(appContext, BuildConfig.GOOGLE_WEB_CLIENT_ID)
    val syncManager = SyncManager(BackupSerializer(database, tx), prefs, clock)
    val cloudSync = CloudSync(auth, syncManager, prefs, onLocalDataReplaced = {
        hints.reload()
        progress.reload()
        achievements.reload()
    })
}
