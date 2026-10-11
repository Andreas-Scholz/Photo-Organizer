package io.github.andreasscholz.photoorganizer.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.data.media.MediaAccess
import io.github.andreasscholz.photoorganizer.ui.common.MediaThumbnail
import io.github.andreasscholz.photoorganizer.ui.common.formatDayOfYear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mediaAccess by remember { mutableStateOf(MediaAccess.of(context)) }

    fun onAccessChanged() {
        mediaAccess = MediaAccess.of(context)
        if (mediaAccess.canRead) viewModel.refresh()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onAccessChanged() }

    // Also picks up permission changes made in the system settings and newly taken photos.
    LifecycleResumeEffect(Unit) {
        onAccessChanged()
        onPauseOrDispose { }
    }

    // Ask once up front; the reminder is of little use without notifications.
    var permissionsRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!permissionsRequested) {
            permissionsRequested = true
            permissionLauncher.launch(
                (MediaAccess.permissions + Manifest.permission.POST_NOTIFICATIONS).toTypedArray(),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (mediaAccess.canRead && state.mediaCount > 0) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = onStartReview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.isCompleted) R.string.home_review_again else R.string.home_start_review,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            !mediaAccess.canRead -> PermissionRequired(
                onGrant = { permissionLauncher.launch(MediaAccess.permissions.toTypedArray()) },
                onOpenSettings = { context.openAppSettings() },
                modifier = Modifier.padding(innerPadding),
            )

            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> DayOverview(
                state = state,
                showPartialAccessHint = mediaAccess == MediaAccess.PARTIAL,
                onOpenSettings = { context.openAppSettings() },
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun DayOverview(
    state: HomeUiState,
    showPartialAccessHint: Boolean,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = formatDayOfYear(state.date),
                    style = MaterialTheme.typography.displaySmall,
                )
                if (state.mediaCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.home_summary,
                            pluralStringResource(R.plurals.home_media_count, state.mediaCount, state.mediaCount),
                            pluralStringResource(R.plurals.home_year_count, state.years.size, state.years.size),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.selectedFolderCount?.let { folderCount ->
                    Text(
                        text = pluralStringResource(R.plurals.home_folder_filter, folderCount, folderCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isCompleted) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.home_completed),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }

        if (showPartialAccessHint) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.home_partial_access))
                        TextButton(onClick = onOpenSettings) {
                            Text(stringResource(R.string.permission_open_settings))
                        }
                    }
                }
            }
        }

        if (state.mediaCount == 0) {
            item {
                Message(
                    title = stringResource(R.string.home_empty_title),
                    text = stringResource(R.string.home_empty_text),
                    modifier = Modifier.fillParentMaxHeight(0.6f),
                )
            }
        }

        items(state.years, key = { it.year }) { group ->
            Text(
                text = stringResource(R.string.home_year_header, group.year, group.items.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(group.items, key = { it.id }) { item ->
                    MediaThumbnail(
                        item = item,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequired(
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Message(
        title = stringResource(R.string.permission_title),
        text = stringResource(R.string.permission_text),
        modifier = modifier.fillMaxSize(),
    ) {
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text(stringResource(R.string.permission_grant)) }
        // The system stops showing the permission dialog after repeated denials.
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.permission_open_settings)) }
    }
}

@Composable
private fun Message(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        actions()
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )
}
