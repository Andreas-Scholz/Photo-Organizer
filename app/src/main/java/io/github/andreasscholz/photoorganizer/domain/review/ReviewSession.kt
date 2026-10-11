package io.github.andreasscholz.photoorganizer.domain.review

import io.github.andreasscholz.photoorganizer.domain.model.Album
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem

/** What should happen to the file of a reviewed item. */
sealed interface Disposition {
    data object Keep : Disposition
    data object Delete : Disposition
    data class MoveTo(val album: Album) : Disposition
}

data class Decision(
    val item: MediaItem,
    val disposition: Disposition,
    /** The favorite state the item should end up with. */
    val favorite: Boolean,
)

/**
 * Walks through [items] one at a time and records a decision for each.
 *
 * Nothing is changed on the device while reviewing: the session only collects decisions,
 * which are turned into a [ChangePlan] once every item has been looked at. This keeps
 * every step undoable and lets all changes be confirmed in one go.
 */
data class ReviewSession(
    val items: List<MediaItem>,
    val decisions: List<Decision> = emptyList(),
    /** Favorite state chosen for the current item, if it was toggled. */
    private val favoriteOverride: Boolean? = null,
) {
    val current: MediaItem? get() = items.getOrNull(decisions.size)

    val isFinished: Boolean get() = decisions.size >= items.size

    val canUndo: Boolean get() = decisions.isNotEmpty()

    /** One-based position of the current item. */
    val position: Int get() = decisions.size + 1

    val currentIsFavorite: Boolean get() = favoriteOverride ?: current?.isFavorite ?: false

    fun toggleFavorite(): ReviewSession =
        if (isFinished) this else copy(favoriteOverride = !currentIsFavorite)

    fun decide(disposition: Disposition): ReviewSession {
        val item = current ?: return this
        return copy(
            decisions = decisions + Decision(item, disposition, currentIsFavorite),
            favoriteOverride = null,
        )
    }

    /** Steps back to the previous item, restoring the favorite state chosen for it. */
    fun undo(): ReviewSession {
        val last = decisions.lastOrNull() ?: return this
        return copy(
            decisions = decisions.dropLast(1),
            favoriteOverride = last.favorite.takeIf { it != last.item.isFavorite },
        )
    }

    fun toChangePlan(): ChangePlan {
        val kept = decisions.filter { it.disposition != Disposition.Delete }
        return ChangePlan(
            toTrash = decisions.filter { it.disposition == Disposition.Delete }.map { it.item },
            // Favorite changes on items that get deleted anyway are pointless.
            toFavorite = kept.filter { it.favorite && !it.item.isFavorite }.map { it.item },
            toUnfavorite = kept.filter { !it.favorite && it.item.isFavorite }.map { it.item },
            toMove = decisions
                .filter { it.disposition is Disposition.MoveTo }
                .groupBy({ (it.disposition as Disposition.MoveTo).album }, { it.item }),
            reviewedCount = decisions.size,
        )
    }

    companion object {
        /**
         * A session over all [items] that begins with the item [startItemId]. The items before
         * it are not skipped but come last, so the review still covers everything.
         */
        fun startingAt(items: List<MediaItem>, startItemId: Long?): ReviewSession {
            val startIndex = items.indexOfFirst { it.id == startItemId }
            return if (startIndex <= 0) {
                ReviewSession(items)
            } else {
                ReviewSession(items.drop(startIndex) + items.take(startIndex))
            }
        }
    }
}
