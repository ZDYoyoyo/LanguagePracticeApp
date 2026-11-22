package io.github.zdyoyoyo.englishpractice

import android.app.Application
import io.github.zdyoyoyo.englishpractice.notify.ReminderScheduler

/** Application 在 App 啟動時最先建立，且整個生命週期只有一個 */
class EnglishApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
    }
}
