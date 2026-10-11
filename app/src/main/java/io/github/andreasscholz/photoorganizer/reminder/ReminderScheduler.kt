package io.github.andreasscholz.photoorganizer.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.andreasscholz.photoorganizer.domain.nextOccurrenceOf
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Arms the alarm that triggers [ReminderReceiver]. There is at most one pending alarm. */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedules the next reminder at [time], replacing any pending one.
     *
     * The alarm is inexact on purpose: the system may deliver it up to an hour late to save
     * battery, which is fine for a reminder, while exact alarms need a special permission.
     */
    fun schedule(time: LocalTime) {
        val triggerAt = nextOccurrenceOf(time, ZonedDateTime.now())
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt.toInstant().toEpochMilli(),
            alarmIntent(),
        )
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
