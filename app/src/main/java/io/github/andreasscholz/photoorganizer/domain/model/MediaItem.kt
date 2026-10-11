package io.github.andreasscholz.photoorganizer.domain.model

import java.time.LocalDateTime

enum class MediaType { IMAGE, VIDEO }

/** A photo or video from the device's media library. */
data class MediaItem(
    val id: Long,
    val type: MediaType,
    val displayName: String,
    /** Capture time in the device's time zone. */
    val capturedAt: LocalDateTime,
    val isFavorite: Boolean,
)
