package io.github.andreasscholz.photoorganizer.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.andreasscholz.photoorganizer.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Keeps the receiver alive until [block] has finished; receivers are otherwise killed on return. */
internal fun BroadcastReceiver.launchAsync(scope: CoroutineScope, block: suspend () -> Unit) {
    val pendingResult = goAsync()
    scope.launch {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}

/** Receives the daily alarm. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var dailyReminder: DailyReminder

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        launchAsync(applicationScope) { dailyReminder.onAlarm() }
    }
}

/** Re-arms the alarm after events that drop or invalidate it, see the manifest. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {

    @Inject lateinit var dailyReminder: DailyReminder

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        launchAsync(applicationScope) { dailyReminder.sync() }
    }
}
