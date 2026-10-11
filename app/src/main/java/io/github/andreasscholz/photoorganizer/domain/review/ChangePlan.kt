package io.github.andreasscholz.photoorganizer.domain.review

import io.github.andreasscholz.photoorganizer.domain.model.Album
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem

/** The device changes resulting from a finished [ReviewSession]. */
data class ChangePlan(
    val toTrash: List<MediaItem>,
    val toFavorite: List<MediaItem>,
    val toUnfavorite: List<MediaItem>,
    val toMove: Map<Album, List<MediaItem>>,
    val reviewedCount: Int,
) {
    val isEmpty: Boolean
        get() = toTrash.isEmpty() && toFavorite.isEmpty() && toUnfavorite.isEmpty() && toMove.isEmpty()

    val keptCount: Int get() = reviewedCount - toTrash.size
}
