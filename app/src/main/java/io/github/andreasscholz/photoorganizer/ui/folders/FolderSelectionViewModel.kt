package io.github.andreasscholz.photoorganizer.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.andreasscholz.photoorganizer.data.media.MediaRepository
import io.github.andreasscholz.photoorganizer.data.settings.SettingsRepository
import io.github.andreasscholz.photoorganizer.domain.model.FolderFilter
import io.github.andreasscholz.photoorganizer.domain.model.MediaFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderSelectionUiState(
    val isLoading: Boolean = true,
    val folders: List<MediaFolder> = emptyList(),
    val filter: FolderFilter = FolderFilter.AllFolders,
)

@HiltViewModel
class FolderSelectionViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val folders = MutableStateFlow<List<MediaFolder>?>(null)

    val uiState: StateFlow<FolderSelectionUiState> =
        combine(folders, settingsRepository.folderFilter) { folders, filter ->
            FolderSelectionUiState(isLoading = folders == null, folders = folders.orEmpty(), filter = filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderSelectionUiState())

    init {
        viewModelScope.launch { folders.value = mediaRepository.folders() }
    }

    /** Switching "all folders" off starts with an empty selection for the user to fill. */
    fun setAllFolders(allFolders: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFolderFilter(
                if (allFolders) FolderFilter.AllFolders else FolderFilter.Only(emptySet()),
            )
        }
    }

    fun setFolderSelected(folder: MediaFolder, selected: Boolean) {
        viewModelScope.launch {
            // Read the stored value rather than the UI state so quick taps cannot overwrite each other.
            val current = settingsRepository.folderFilter.first() as? FolderFilter.Only ?: return@launch
            settingsRepository.setFolderFilter(current.with(folder.relativePath, selected))
        }
    }
}
