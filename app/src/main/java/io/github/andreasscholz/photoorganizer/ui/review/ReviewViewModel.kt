package io.github.andreasscholz.photoorganizer.ui.review

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.andreasscholz.photoorganizer.data.ReviewMediaSource
import io.github.andreasscholz.photoorganizer.data.media.MediaRepository
import io.github.andreasscholz.photoorganizer.data.settings.SettingsRepository
import io.github.andreasscholz.photoorganizer.domain.model.Album
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.review.ChangePlan
import io.github.andreasscholz.photoorganizer.domain.review.Disposition
import io.github.andreasscholz.photoorganizer.domain.review.ReviewSession
import io.github.andreasscholz.photoorganizer.reminder.ReminderNotifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface ReviewNotice {
    /** The user dismissed a system confirmation dialog. */
    data object ConfirmationDeclined : ReviewNotice
    data class MoveFailed(val count: Int) : ReviewNotice
}

data class ReviewUiState(
    val isLoading: Boolean = true,
    val session: ReviewSession = ReviewSession(emptyList()),
    val albums: List<Album> = emptyList(),
    val isApplying: Boolean = false,
    /** Some changes are already on the device, so decisions can no longer be revised. */
    val hasAppliedChanges: Boolean = false,
    val notice: ReviewNotice? = null,
    val isCompleted: Boolean = false,
) {
    val canUndo: Boolean get() = session.canUndo && !isApplying && !hasAppliedChanges
}

/** One kind of change, confirmed by the user in a single system dialog. */
private sealed interface ChangeStep {
    data class Trash(val items: List<MediaItem>) : ChangeStep
    data class Favorite(val items: List<MediaItem>, val favorite: Boolean) : ChangeStep
    data class Move(val itemsByAlbum: Map<Album, List<MediaItem>>) : ChangeStep
}

private fun ChangePlan.toSteps(): List<ChangeStep> = buildList {
    if (toTrash.isNotEmpty()) add(ChangeStep.Trash(toTrash))
    if (toFavorite.isNotEmpty()) add(ChangeStep.Favorite(toFavorite, favorite = true))
    if (toUnfavorite.isNotEmpty()) add(ChangeStep.Favorite(toUnfavorite, favorite = false))
    if (toMove.isNotEmpty()) add(ChangeStep.Move(toMove))
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val reviewMediaSource: ReviewMediaSource,
    private val settingsRepository: SettingsRepository,
    private val reminderNotifier: ReminderNotifier,
) : ViewModel() {

    private val date: LocalDate = LocalDate.now()

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _confirmationRequests = Channel<IntentSender>(Channel.BUFFERED)

    /** System dialogs the screen has to show; answer each via [onConfirmationResult]. */
    val confirmationRequests: Flow<IntentSender> = _confirmationRequests.receiveAsFlow()

    /** The steps still to run, or null while the plan may still change. */
    private var pendingSteps: List<ChangeStep>? = null

    init {
        viewModelScope.launch {
            val items = reviewMediaSource.mediaOnThisDay(date)
            val albums = mediaRepository.albums()
            _uiState.update { it.copy(isLoading = false, session = ReviewSession(items), albums = albums) }
        }
    }

    fun keep() = updateSession { it.decide(Disposition.Keep) }

    fun delete() = updateSession { it.decide(Disposition.Delete) }

    fun moveTo(album: Album) {
        _uiState.update { state ->
            if (album in state.albums) state
            else state.copy(albums = (state.albums + album).sortedBy { it.name.lowercase() })
        }
        updateSession { it.decide(Disposition.MoveTo(album)) }
    }

    fun toggleFavorite() = updateSession { it.toggleFavorite() }

    fun undo() {
        if (_uiState.value.canUndo) updateSession { it.undo() }
    }

    /** Starts applying the decisions, or continues after a declined or failed step. */
    fun applyChanges() {
        val state = _uiState.value
        if (state.isApplying || !state.session.isFinished) return

        if (pendingSteps == null) pendingSteps = state.session.toChangePlan().toSteps()
        _uiState.update { it.copy(isApplying = true, notice = null) }
        runNextStep()
    }

    fun onConfirmationResult(confirmed: Boolean) {
        val steps = pendingSteps ?: return
        val step = steps.firstOrNull() ?: return

        if (!confirmed) {
            // Nothing has been changed yet, so the decisions may still be revised.
            if (!_uiState.value.hasAppliedChanges) pendingSteps = null
            _uiState.update { it.copy(isApplying = false, notice = ReviewNotice.ConfirmationDeclined) }
            return
        }

        viewModelScope.launch {
            // Trashing and favoriting are done by the system once confirmed; moving is on us.
            val failedMoves = if (step is ChangeStep.Move) {
                step.itemsByAlbum.entries.sumOf { (album, items) ->
                    items.size - mediaRepository.moveToAlbum(items, album)
                }
            } else {
                0
            }
            pendingSteps = steps.drop(1)

            if (failedMoves > 0) {
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        hasAppliedChanges = true,
                        notice = ReviewNotice.MoveFailed(failedMoves),
                    )
                }
            } else {
                _uiState.update { it.copy(hasAppliedChanges = true) }
                runNextStep()
            }
        }
    }

    private fun runNextStep() {
        val request = when (val step = pendingSteps.orEmpty().firstOrNull()) {
            null -> return complete()
            is ChangeStep.Trash -> mediaRepository.createTrashRequest(step.items)
            is ChangeStep.Favorite -> mediaRepository.createFavoriteRequest(step.items, step.favorite)
            is ChangeStep.Move -> mediaRepository.createWriteRequest(step.itemsByAlbum.values.flatten())
        }
        _confirmationRequests.trySend(request)
    }

    private fun complete() {
        viewModelScope.launch {
            settingsRepository.markDayCompleted(date)
            reminderNotifier.dismissReminder()
            _uiState.update { it.copy(isApplying = false, isCompleted = true) }
        }
    }

    private fun updateSession(transform: (ReviewSession) -> ReviewSession) {
        _uiState.update { it.copy(session = transform(it.session), notice = null) }
    }
}
