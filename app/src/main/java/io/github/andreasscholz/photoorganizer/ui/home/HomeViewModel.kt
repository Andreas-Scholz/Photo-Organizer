package io.github.andreasscholz.photoorganizer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.andreasscholz.photoorganizer.data.ReviewMediaSource
import io.github.andreasscholz.photoorganizer.data.settings.SettingsRepository
import io.github.andreasscholz.photoorganizer.domain.model.FolderFilter
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
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
    val date: LocalDate,
    val isLoading: Boolean = true,
    /** Today's media grouped by the year it was captured in, newest year first. */
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

    private data class Snapshot(val date: LocalDate, val media: List<MediaItem>?)

    private val snapshot = MutableStateFlow(Snapshot(LocalDate.now(), media = null))

    val uiState: StateFlow<HomeUiState> =
        combine(
            snapshot,
            settingsRepository.lastCompletedDay,
            settingsRepository.folderFilter,
        ) { snapshot, lastCompletedDay, folderFilter ->
            HomeUiState(
                date = snapshot.date,
                isLoading = snapshot.media == null,
                years = snapshot.media.orEmpty()
                    .groupBy { it.capturedAt.year }
                    .map { (year, items) -> YearGroup(year, items) },
                isCompleted = lastCompletedDay == snapshot.date,
                selectedFolderCount = (folderFilter as? FolderFilter.Only)?.relativePaths?.size,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(date = snapshot.value.date),
        )

    /** Reloads today's media. The library and even the date may have changed while away. */
    fun refresh() {
        viewModelScope.launch {
            val today = LocalDate.now()
            snapshot.value = Snapshot(today, reviewMediaSource.mediaOnThisDay(today))
        }
    }
}
