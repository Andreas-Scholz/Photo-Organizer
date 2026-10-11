package io.github.andreasscholz.photoorganizer.domain.review

import io.github.andreasscholz.photoorganizer.domain.model.Album
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem

/** What should happen to the file of a reviewed item. */
sealed interface Disposition {
    data object Keep : Disposition
    data object Delete : Disposition
    data class MoveTo(val album: Album) : Disposition
}

data class Decision(val item: MediaItem, val disposition: Disposition)

/**
 * A review of [items], which records a decision for each of them.
 *
 * The items can be browsed freely: looking at an item does not decide anything, and an item
 * that has been decided can be visited and decided again. The review is finished once every
 * item has a decision.
 *
 * Nothing is changed on the device while reviewing: the session only collects decisions,
 * which are turned into a [ChangePlan] at the end. This keeps every step undoable and lets
 * all changes be confirmed in one go.
 */
data class ReviewSession(
    val items: List<MediaItem>,
    /** Position of the item being looked at. */
    val index: Int = 0,
    /** At most one decision per item, in the order they were made. */
    val decisions: List<Decision> = emptyList(),
    /** Favorite state chosen for items whose state was toggled, by item id. */
    private val favorites: Map<Long, Boolean> = emptyMap(),
) {
    val isFinished: Boolean get() = decisions.size >= items.size

    /** The item being looked at, or null once the review is finished. */
    val current: MediaItem? get() = if (isFinished) null else items.getOrNull(index)

    val canUndo: Boolean get() = decisions.isNotEmpty()

    /** One-based position of the current item. */
    val position: Int get() = index + 1

    val currentIsFavorite: Boolean get() = current?.let(::isFavorite) ?: false

    /** The decision made for [item], or null if it is still open. */
    fun dispositionOf(item: MediaItem): Disposition? =
        decisions.firstOrNull { it.item.id == item.id }?.disposition

    fun goForward(): ReviewSession = goTo(index + 1)

    fun goBack(): ReviewSession = goTo(index - 1)

    /** Looks at [itemId] without deciding anything. */
    fun goTo(itemId: Long): ReviewSession = goTo(items.indexOfFirst { it.id == itemId })

    fun toggleFavorite(): ReviewSession {
        val item = current ?: return this
        return copy(favorites = favorites + (item.id to !isFavorite(item)))
    }

    /** Decides the current item, replacing an earlier decision, and moves on to the next open item. */
    fun decide(disposition: Disposition): ReviewSession {
        val item = current ?: return this
        return withDecision(item, disposition).atNextOpenItem()
    }

    /** Takes back the most recent decision and returns to its item. */
    fun undo(): ReviewSession {
        val last = decisions.lastOrNull() ?: return this
        return copy(
            index = items.indexOfFirst { it.id == last.item.id },
            decisions = decisions.dropLast(1),
        )
    }

    fun toChangePlan(): ChangePlan {
        val kept = decisions.filter { it.disposition != Disposition.Delete }.map { it.item }
        return ChangePlan(
            toTrash = decisions.filter { it.disposition == Disposition.Delete }.map { it.item },
            // Favorite changes on items that get deleted anyway are pointless.
            toFavorite = kept.filter { isFavorite(it) && !it.isFavorite },
            toUnfavorite = kept.filter { !isFavorite(it) && it.isFavorite },
            toMove = decisions
                .filter { it.disposition is Disposition.MoveTo }
                .groupBy({ (it.disposition as Disposition.MoveTo).album }, { it.item }),
            reviewedCount = decisions.size,
        )
    }

    private fun isFavorite(item: MediaItem): Boolean = favorites[item.id] ?: item.isFavorite

    private fun goTo(newIndex: Int): ReviewSession =
        if (isFinished || newIndex !in items.indices) this else copy(index = newIndex)

    private fun withDecision(item: MediaItem, disposition: Disposition): ReviewSession =
        copy(decisions = decisions.filterNot { it.item.id == item.id } + Decision(item, disposition))

    /** Moves to the first open item after the current one, continuing from the start if needed. */
    private fun atNextOpenItem(): ReviewSession {
        val decided = decisions.mapTo(mutableSetOf()) { it.item.id }
        val next = (1..items.size)
            .map { (index + it) % items.size }
            .firstOrNull { items[it].id !in decided }
        return if (next == null) this else copy(index = next)
    }

    companion object {
        /** A session over all [items] that begins at the item [startItemId], if there is one. */
        fun startingAt(items: List<MediaItem>, startItemId: Long?): ReviewSession =
            ReviewSession(items, index = items.indexOfFirst { it.id == startItemId }.coerceAtLeast(0))
    }
}
