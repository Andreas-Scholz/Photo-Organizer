package io.github.andreasscholz.photoorganizer.data

import io.github.andreasscholz.photoorganizer.data.media.MediaRepository
import io.github.andreasscholz.photoorganizer.data.settings.SettingsRepository
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/** The media that is up for review on a day, honoring the user's folder selection. */
class ReviewMediaSource @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun mediaOnThisDay(date: LocalDate): List<MediaItem> =
        mediaRepository.mediaOnThisDay(date, settingsRepository.folderFilter.first())
}
