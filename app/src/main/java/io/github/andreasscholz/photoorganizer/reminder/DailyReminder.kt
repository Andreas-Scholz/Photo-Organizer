package io.github.andreasscholz.photoorganizer.reminder

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.andreasscholz.photoorganizer.data.media.MediaAccess
import io.github.andreasscholz.photoorganizer.data.ReviewMediaSource
import io.github.andreasscholz.photoorganizer.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the reminder alarm in line with the settings and decides whether to notify. */
@Singleton
class DailyReminder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val reviewMediaSource: ReviewMediaSource,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) {
    /** Arms or cancels the alarm according to the current settings. Safe to call repeatedly. */
    suspend fun sync() {
        val settings = settingsRepository.reminderSettings.first()
        if (settings.enabled) scheduler.schedule(settings.time) else scheduler.cancel()
    }

    /** Called when the alarm fires: re-arms it for the next day and notifies if there is work. */
    suspend fun onAlarm() {
        sync()
        if (!settingsRepository.reminderSettings.first().enabled) return

        val today = LocalDate.now()
        if (settingsRepository.lastCompletedDay.first() == today) return

        val mediaCount = if (MediaAccess.of(context).canRead) {
            reviewMediaSource.mediaOnThisDay(today).size
        } else {
            null
        }
        if (mediaCount == 0) return

        notifier.showReminder(mediaCount)
    }
}
