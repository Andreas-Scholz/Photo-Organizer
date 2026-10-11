package io.github.andreasscholz.photoorganizer.ui.review

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.review.Disposition
import io.github.andreasscholz.photoorganizer.ui.common.formatFullDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onClose: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.session
    val currentItem = session.current
    val currentOnClose by rememberUpdatedState(onClose)

    val confirmationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onConfirmationResult(confirmed = result.resultCode == Activity.RESULT_OK) }

    LaunchedEffect(viewModel) {
        viewModel.confirmationRequests.collect { request ->
            confirmationLauncher.launch(IntentSenderRequest.Builder(request).build())
        }
    }
    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) currentOnClose()
    }

    // Leaving would silently drop decisions that have not been applied yet.
    val hasUnappliedDecisions = session.canUndo && !state.hasAppliedChanges && !state.isCompleted
    var showDiscardDialog by remember { mutableStateOf(false) }
    val close = { if (hasUnappliedDecisions) showDiscardDialog = true else onClose() }
    BackHandler(enabled = hasUnappliedDecisions) { showDiscardDialog = true }

    var showAlbumPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentItem != null) {
                        Column {
                            Text(
                                text = formatFullDate(currentItem.capturedAt),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.review_position, session.position, session.items.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (!state.isLoading) {
                        Text(stringResource(R.string.summary_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = close) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.review_close),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_undo),
                            contentDescription = stringResource(R.string.review_undo),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            currentItem != null -> ReviewCard(
                item = currentItem,
                isFavorite = session.currentIsFavorite,
                disposition = session.dispositionOf(currentItem),
                series = state.currentSeries,
                dispositionOf = session::dispositionOf,
                onShowItem = viewModel::showItem,
                onKeepOnlyCurrent = viewModel::keepOnlyCurrent,
                onPrevious = viewModel::showPrevious,
                onNext = viewModel::showNext,
                onDelete = viewModel::delete,
                onKeep = viewModel::keep,
                onToggleFavorite = viewModel::toggleFavorite,
                onChooseAlbum = { showAlbumPicker = true },
                modifier = contentModifier,
            )

            else -> ReviewSummary(
                plan = remember(session) { session.toChangePlan() },
                state = state,
                onApply = viewModel::applyChanges,
                onUndo = viewModel::undo,
                modifier = contentModifier,
            )
        }
    }

    if (showAlbumPicker) {
        AlbumPicker(
            albums = state.albums,
            onAlbumSelected = { album ->
                showAlbumPicker = false
                viewModel.moveTo(album)
            },
            onDismiss = { showAlbumPicker = false },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onClose()
                    },
                ) { Text(stringResource(R.string.discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.discard_dismiss))
                }
            },
        )
    }
}

/**
 * The current item with its actions: swipe left to delete, right to keep, or tap its left
 * or right side to browse without deciding. [disposition] is an earlier decision, if any.
 */
@Composable
private fun ReviewCard(
    item: MediaItem,
    isFavorite: Boolean,
    disposition: Disposition?,
    series: List<MediaItem>,
    dispositionOf: (MediaItem) -> Disposition?,
    onShowItem: (MediaItem) -> Unit,
    onKeepOnlyCurrent: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChooseAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Keyed so that every item starts with a centered card of its own.
        key(item.id) {
            SwipeableCard(
                onSwipedLeft = onDelete,
                onSwipedRight = onKeep,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) { progress ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    MediaViewer(
                        item = item,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (disposition != null) {
                        DecisionBadge(disposition, modifier = Modifier.align(Alignment.BottomStart))
                    }
                    SwipeLabel(
                        text = stringResource(R.string.review_keep),
                        color = MaterialTheme.colorScheme.primary,
                        alpha = { progress().coerceAtLeast(0f) },
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                    SwipeLabel(
                        text = stringResource(R.string.review_delete),
                        color = MaterialTheme.colorScheme.error,
                        alpha = { (-progress()).coerceAtLeast(0f) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }

        if (series.isNotEmpty()) {
            SeriesStrip(
                series = series,
                currentItem = item,
                dispositionOf = dispositionOf,
                onItemClick = onShowItem,
                onKeepOnlyCurrent = onKeepOnlyCurrent,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ReviewAction(
                icon = R.drawable.ic_delete,
                label = stringResource(R.string.review_delete),
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
            ReviewAction(
                icon = R.drawable.ic_folder,
                label = stringResource(R.string.review_album),
                onClick = onChooseAlbum,
            )
            ReviewAction(
                icon = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                label = stringResource(R.string.review_favorite),
                onClick = onToggleFavorite,
            )
            ReviewAction(
                icon = R.drawable.ic_check,
                label = stringResource(R.string.review_keep),
                onClick = onKeep,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Fades in while dragging to announce what releasing the card will do. */
@Composable
private fun SwipeLabel(
    text: String,
    color: Color,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    CardLabel(text = text, color = color, modifier = modifier.graphicsLayer { this.alpha = alpha() })
}

/** Shows what has already been decided for the item, as it can be visited again. */
@Composable
private fun DecisionBadge(disposition: Disposition, modifier: Modifier = Modifier) {
    when (disposition) {
        Disposition.Keep -> CardLabel(
            text = stringResource(R.string.review_keep),
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
        Disposition.Delete -> CardLabel(
            text = stringResource(R.string.review_delete),
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        is Disposition.MoveTo -> CardLabel(
            text = disposition.album.name,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier,
        )
    }
}

@Composable
private fun CardLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = modifier
            .padding(16.dp)
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ReviewAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Icon(painter = painterResource(icon), contentDescription = null)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
