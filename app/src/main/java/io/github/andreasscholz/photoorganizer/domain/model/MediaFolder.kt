package io.github.andreasscholz.photoorganizer.domain.model

/** A folder on the device that contains photos or videos. */
data class MediaFolder(
    val name: String,
    /** Path relative to the storage root, always ending in a slash. */
    val relativePath: String,
    val itemCount: Int,
)
