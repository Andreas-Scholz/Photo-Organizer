package io.github.andreasscholz.photoorganizer.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.domain.model.FolderFilter
import io.github.andreasscholz.photoorganizer.ui.common.formatTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenFolderSelection: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()
    val folderFilter by viewModel.folderFilter.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    var notificationsEnabled by remember { mutableStateOf(context.areNotificationsEnabled()) }
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = context.areNotificationsEnabled()
        onPauseOrDispose { }
    }

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val settings = reminderSettings ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reminder)) },
                supportingContent = { Text(stringResource(R.string.settings_reminder_summary)) },
                trailingContent = {
                    Switch(checked = settings.enabled, onCheckedChange = viewModel::setReminderEnabled)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reminder_time)) },
                supportingContent = { Text(formatTime(settings.time, is24Hour)) },
                modifier = Modifier.clickable(enabled = settings.enabled) { showTimePicker = true },
            )

            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_folders)) },
                supportingContent = {
                    Text(
                        when (val filter = folderFilter) {
                            FolderFilter.AllFolders -> stringResource(R.string.folders_all)
                            is FolderFilter.Only -> pluralStringResource(
                                R.plurals.settings_folders_selected,
                                filter.relativePaths.size,
                                filter.relativePaths.size,
                            )
                        },
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenFolderSelection),
            )

            if (settings.enabled && !notificationsEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.settings_notifications_disabled))
                        TextButton(onClick = { context.openNotificationSettings() }) {
                            Text(stringResource(R.string.settings_enable_notifications))
                        }
                    }
                }
            }
        }

        if (showTimePicker) {
            ReminderTimeDialog(
                initialTime = settings.time,
                is24Hour = is24Hour,
                onConfirm = { time ->
                    showTimePicker = false
                    viewModel.setReminderTime(time)
                },
                onDismiss = { showTimePicker = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialTime: LocalTime,
    is24Hour: Boolean,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24Hour,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun Context.areNotificationsEnabled(): Boolean =
    NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
    )
}
