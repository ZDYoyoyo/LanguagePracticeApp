package io.github.zdyoyoyo.englishpractice.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.notify.ReminderScheduler
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY]!!, appContainer()) }
        }
    }
}
