package io.github.andreasscholz.photoorganizer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.andreasscholz.photoorganizer.data.settings.ReminderSettings
import io.github.andreasscholz.photoorganizer.data.settings.SettingsRepository
import io.github.andreasscholz.photoorganizer.reminder.DailyReminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dailyReminder: DailyReminder,
) : ViewModel() {

    /** Null until the stored settings have been read. */
    val reminderSettings: StateFlow<ReminderSettings?> = settingsRepository.reminderSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            dailyReminder.sync()
        }
    }

    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(time)
            dailyReminder.sync()
        }
    }
}
