package io.github.andreasscholz.photoorganizer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.andreasscholz.photoorganizer.domain.model.FolderFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

data class ReminderSettings(
    val enabled: Boolean = true,
    val time: LocalTime = LocalTime.of(20, 0),
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val reminderSettings: Flow<ReminderSettings> = dataStore.data.map { preferences ->
        val defaults = ReminderSettings()
        ReminderSettings(
            enabled = preferences[REMINDER_ENABLED] ?: defaults.enabled,
            time = preferences[REMINDER_MINUTE_OF_DAY]
                ?.let { LocalTime.ofSecondOfDay(it * 60L) }
                ?: defaults.time,
        )
    }.distinctUntilChanged()

    /** The most recent day whose review was finished, if any. */
    val lastCompletedDay: Flow<LocalDate?> = dataStore.data.map { preferences ->
        preferences[LAST_COMPLETED_DAY]?.let(LocalDate::parse)
    }.distinctUntilChanged()

    /** Which folders the review covers; everything unless the user picked folders. */
    val folderFilter: Flow<FolderFilter> = dataStore.data.map { preferences ->
        preferences[REVIEW_FOLDERS]?.let { FolderFilter.Only(it) } ?: FolderFilter.AllFolders
    }.distinctUntilChanged()

    suspend fun setFolderFilter(filter: FolderFilter) {
        dataStore.edit { preferences ->
            when (filter) {
                FolderFilter.AllFolders -> preferences.remove(REVIEW_FOLDERS)
                is FolderFilter.Only -> preferences[REVIEW_FOLDERS] = filter.relativePaths
            }
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(time: LocalTime) {
        dataStore.edit { it[REMINDER_MINUTE_OF_DAY] = time.toSecondOfDay() / 60 }
    }

    suspend fun markDayCompleted(day: LocalDate) {
        dataStore.edit { it[LAST_COMPLETED_DAY] = day.toString() }
    }

    private companion object {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("reminder_minute_of_day")
        val LAST_COMPLETED_DAY = stringPreferencesKey("last_completed_day")
        val REVIEW_FOLDERS = stringSetPreferencesKey("review_folders")
    }
}
