package io.github.zdyoyoyo.englishpractice.ui.settings

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.ads.AdPolicy
import io.github.zdyoyoyo.englishpractice.notify.ReminderScheduler
import io.github.zdyoyoyo.englishpractice.sync.CloudStatus
import io.github.zdyoyoyo.englishpractice.sync.SyncPolicy
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val reminderEnabled: Boolean = false,
    val reminderMinuteOfDay: Int = 20 * 60,
)

class SettingsViewModel(
    private val app: Application,
    private val c: AppContainer,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        SettingsUiState(c.prefs.reminderEnabled, c.prefs.reminderMinuteOfDay)
    )
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()
    val cloud: StateFlow<CloudStatus> = c.cloudSync.status
    val hintCredits: StateFlow<Int> = c.hints.credits
    val rewardedReady: StateFlow<Boolean> = c.ads.rewardedReady
    val privacyOptionsRequired: StateFlow<Boolean> = c.ads.privacyOptionsRequired
    val adStatus: StateFlow<String> = c.ads.status

    fun retryAds() = c.ads.retryNow()

    // ---- 廣告（選擇性）----
    fun watchRewardedAd(activity: Activity) {
        c.ads.showRewarded(activity) { c.hints.add(AdPolicy.HINTS_PER_REWARD) }
    }

    fun showAdPrivacyOptions(activity: Activity) = c.ads.showPrivacyOptions(activity)

    // ---- 每日提醒 ----
    fun setReminder(enabled: Boolean) {
        c.prefs.reminderEnabled = enabled
        if (enabled) ReminderScheduler.schedule(app, c.prefs.reminderMinuteOfDay) else ReminderScheduler.cancel(app)
        _ui.update { it.copy(reminderEnabled = enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val m = hour * 60 + minute
        c.prefs.reminderMinuteOfDay = m
        if (c.prefs.reminderEnabled) ReminderScheduler.schedule(app, m)
        _ui.update { it.copy(reminderMinuteOfDay = m) }
    }

    // ---- 帳號與同步 ----
    fun signIn(activity: Activity) = viewModelScope.launch { c.cloudSync.signIn(activity) }
    fun syncNow() = viewModelScope.launch { c.cloudSync.syncNow() }
    fun restoreFromCloud() = viewModelScope.launch { c.cloudSync.syncNow(force = SyncPolicy.Action.DOWNLOAD) }
    fun uploadLocal() = viewModelScope.launch { c.cloudSync.syncNow(force = SyncPolicy.Action.UPLOAD) }
    fun deleteCloudBackup() = viewModelScope.launch { c.cloudSync.deleteCloudBackup() }
    fun signOut(clearLocal: Boolean) = viewModelScope.launch { c.cloudSync.signOut(clearLocal) }
    fun clearLocalData() = viewModelScope.launch { c.cloudSync.clearLocalData() }
    fun onConsentResult(data: Intent?) = viewModelScope.launch { c.cloudSync.onConsentResult(data) }
    fun dismissChoice() = c.cloudSync.dismissChoice()
    fun clearMessage() = c.cloudSync.clearMessage()

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY]!!, appContainer()) }
        }
    }
}
