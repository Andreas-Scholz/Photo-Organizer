package io.github.andreasscholz.photoorganizer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.andreasscholz.photoorganizer.data.ReviewMediaSource
import io.github.andreasscholz.photoorganizer.data.settings.SettingsRepository
import io.github.andreasscholz.photoorganizer.domain.model.FolderFilter
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.reviewDayOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class YearGroup(val year: Int, val items: List<MediaItem>)

data class HomeUiState(
    /** The day being shown; its media covers this calendar day in all years. */
    val date: LocalDate,
    val isToday: Boolean = true,
    val isLoading: Boolean = true,
    /** The day's media grouped by the year it was captured in, newest year first. */
    val years: List<YearGroup> = emptyList(),
    val isCompleted: Boolean = false,
    /** Number of folders the review is restricted to, or null if it covers all of them. */
    val selectedFolderCount: Int? = null,
) {
    val mediaCount: Int get() = years.sumOf { it.items.size }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reviewMediaSource: ReviewMediaSource,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private data class Snapshot(val date: LocalDate, val isToday: Boolean, val media: List<MediaItem>?)

    /** The day the user paged to, or null to follow the current date. */
    private var selectedDay: LocalDate? = null

    private val snapshot = MutableStateFlow(Snapshot(LocalDate.now(), isToday = true, media = null))
    private var loadJob: Job? = null

    val uiState: StateFlow<HomeUiState> =
        combine(
            snapshot,
            settingsRepository.completedDays,
            settingsRepository.folderFilter,
        ) { snapshot, completedDays, folderFilter ->
            HomeUiState(
                date = snapshot.date,
                isToday = snapshot.isToday,
                isLoading = snapshot.media == null,
                years = snapshot.media.orEmpty()
                    .groupBy { it.capturedAt.year }
                    .map { (year, items) -> YearGroup(year, items) },
                isCompleted = snapshot.date in completedDays,
                selectedFolderCount = (folderFilter as? FolderFilter.Only)?.relativePaths?.size,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(date = snapshot.value.date),
        )

    /** Reloads the shown day. The library and even the current date may have changed while away. */
    fun refresh() = load(keepContent = true)

    fun showPreviousDay() = showDay(snapshot.value.date.minusDays(1))

    fun showNextDay() = showDay(snapshot.value.date.plusDays(1))

    fun showToday() {
        selectedDay = null
        load(keepContent = false)
    }

    /** Shows the calendar day of [date]; its year does not matter. */
    fun showDay(date: LocalDate) {
        val today = LocalDate.now()
        selectedDay = reviewDayOf(date, today).takeIf { it != today }
        load(keepContent = false)
    }

    private fun load(keepContent: Boolean) {
        val today = LocalDate.now()
        val date = selectedDay ?: today
        val current = snapshot.value
        val sameDay = current.date == date
        // Old content stays visible during a refresh, but must not be shown under a new date.
        snapshot.value = Snapshot(date, isToday = date == today, media = current.media.takeIf { keepContent && sameDay })

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            snapshot.value = Snapshot(date, isToday = date == today, media = reviewMediaSource.mediaOnThisDay(date))
        }
    }
}
