package io.github.andreasscholz.photoorganizer.domain.review

import io.github.andreasscholz.photoorganizer.domain.model.Album
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReviewSessionTest {

    private fun item(id: Long, isFavorite: Boolean = false) = MediaItem(
        id = id,
        type = MediaType.IMAGE,
        displayName = "IMG_$id.jpg",
        capturedAt = LocalDateTime.of(2025, 3, 3, 12, 0),
        isFavorite = isFavorite,
    )

    private val first = item(1)
    private val second = item(2)
    private val third = item(3)
    private val holiday = Album.create("Holiday")

    @Test
    fun `deciding advances to the next item until the session is finished`() {
        var session = ReviewSession(listOf(first, second))
        assertEquals(first, session.current)
        assertEquals(1, session.position)

        session = session.decide(Disposition.Keep)
        assertEquals(second, session.current)
        assertFalse(session.isFinished)

        session = session.decide(Disposition.Delete)
        assertNull(session.current)
        assertTrue(session.isFinished)
    }

    @Test
    fun `a session without items is finished and plans no changes`() {
        val session = ReviewSession(emptyList())

        assertTrue(session.isFinished)
        assertTrue(session.toChangePlan().isEmpty)
        assertEquals(session, session.decide(Disposition.Delete))
    }

    @Test
    fun `plan groups items by what happens to them`() {
        val plan = ReviewSession(listOf(first, second, third))
            .decide(Disposition.Delete)
            .decide(Disposition.MoveTo(holiday))
            .decide(Disposition.Keep)
            .toChangePlan()

        assertEquals(listOf(first), plan.toTrash)
        assertEquals(mapOf(holiday to listOf(second)), plan.toMove)
        assertEquals(2, plan.keptCount)
        assertFalse(plan.isEmpty)
    }

    @Test
    fun `keeping everything plans no changes`() {
        val plan = ReviewSession(listOf(first, second))
            .decide(Disposition.Keep)
            .decide(Disposition.Keep)
            .toChangePlan()

        assertTrue(plan.isEmpty)
        assertEquals(2, plan.keptCount)
    }

    // Browsing

    @Test
    fun `browsing moves between items without deciding anything`() {
        val session = ReviewSession(listOf(first, second, third)).goForward().goForward()

        assertEquals(third, session.current)
        assertEquals(3, session.position)
        assertEquals(second, session.goBack().current)
        assertTrue(session.decisions.isEmpty())
    }

    @Test
    fun `browsing stops at the first and the last item`() {
        val session = ReviewSession(listOf(first, second))

        assertEquals(first, session.goBack().current)
        assertEquals(second, session.goForward().goForward().current)
    }

    @Test
    fun `any item can be jumped to`() {
        val session = ReviewSession(listOf(first, second, third))

        assertEquals(third, session.goTo(third.id).current)
        assertEquals(session, session.goTo(99))
    }

    @Test
    fun `a session can begin at any item and keeps the order of the items`() {
        val session = ReviewSession.startingAt(listOf(first, second, third), startItemId = second.id)

        assertEquals(listOf(first, second, third), session.items)
        assertEquals(second, session.current)
    }

    @Test
    fun `a session begins at the first item if the start item is missing or unknown`() {
        val items = listOf(first, second, third)

        assertEquals(first, ReviewSession.startingAt(items, startItemId = null).current)
        assertEquals(first, ReviewSession.startingAt(items, startItemId = 99).current)
    }

    // Deciding while browsing

    @Test
    fun `deciding skips items that are already decided`() {
        val session = ReviewSession(listOf(first, second, third))
            .goTo(second.id)
            .decide(Disposition.Delete)
            .goBack()
            .goBack()
            .decide(Disposition.Keep)

        assertEquals(third, session.current)
    }

    @Test
    fun `after the last item deciding continues with open items from the start`() {
        val session = ReviewSession(listOf(first, second, third))
            .goTo(third.id)
            .decide(Disposition.Keep)

        assertEquals(first, session.current)
        assertFalse(session.isFinished)
    }

    @Test
    fun `a decided item can be visited and decided again`() {
        val session = ReviewSession(listOf(first, second))
            .decide(Disposition.Delete)
            .goBack()
        assertEquals(Disposition.Delete, session.dispositionOf(first))

        val plan = session
            .decide(Disposition.Keep)
            .decide(Disposition.Keep)
            .toChangePlan()

        assertTrue(plan.toTrash.isEmpty())
        assertEquals(2, plan.reviewedCount)
    }

    @Test
    fun `open items have no disposition`() {
        assertNull(ReviewSession(listOf(first)).dispositionOf(first))
    }

    // Favorites

    @Test
    fun `favorite toggle applies to the current item only`() {
        val plan = ReviewSession(listOf(first, second))
            .toggleFavorite()
            .decide(Disposition.Keep)
            .decide(Disposition.Keep)
            .toChangePlan()

        assertEquals(listOf(first), plan.toFavorite)
    }

    @Test
    fun `toggling twice leaves the favorite state untouched`() {
        val plan = ReviewSession(listOf(first))
            .toggleFavorite()
            .toggleFavorite()
            .decide(Disposition.Keep)
            .toChangePlan()

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `existing favorites can be removed`() {
        val favorite = item(4, isFavorite = true)
        val session = ReviewSession(listOf(favorite))
        assertTrue(session.currentIsFavorite)

        val plan = session.toggleFavorite().decide(Disposition.Keep).toChangePlan()

        assertEquals(listOf(favorite), plan.toUnfavorite)
        assertTrue(plan.toFavorite.isEmpty())
    }

    @Test
    fun `favorite changes are dropped for deleted items`() {
        val plan = ReviewSession(listOf(first))
            .toggleFavorite()
            .decide(Disposition.Delete)
            .toChangePlan()

        assertEquals(listOf(first), plan.toTrash)
        assertTrue(plan.toFavorite.isEmpty())
    }

    @Test
    fun `an item stays favorite while browsing away and back`() {
        val session = ReviewSession(listOf(first, second))
            .toggleFavorite()
            .goForward()
        assertFalse(session.currentIsFavorite)

        assertTrue(session.goBack().currentIsFavorite)
    }

    @Test
    fun `a decided item can still be marked as favorite`() {
        val plan = ReviewSession(listOf(first, second))
            .decide(Disposition.Keep)
            .goBack()
            .toggleFavorite()
            .goForward()
            .decide(Disposition.Keep)
            .toChangePlan()

        assertEquals(listOf(first), plan.toFavorite)
    }

    // Undo

    @Test
    fun `undo takes back the latest decision and returns to its item`() {
        val session = ReviewSession(listOf(first, second, third))
            .decide(Disposition.Delete)
            .goForward()
        assertTrue(session.canUndo)

        val undone = session.undo()

        assertEquals(first, undone.current)
        assertNull(undone.dispositionOf(first))
        assertFalse(undone.canUndo)
    }

    @Test
    fun `undo reopens a finished session`() {
        val session = ReviewSession(listOf(first, second))
            .decide(Disposition.Keep)
            .decide(Disposition.Delete)
            .undo()

        assertFalse(session.isFinished)
        assertEquals(second, session.current)
    }

    @Test
    fun `undo on a fresh session does nothing`() {
        val session = ReviewSession(listOf(first))

        assertEquals(session, session.undo())
    }
}
