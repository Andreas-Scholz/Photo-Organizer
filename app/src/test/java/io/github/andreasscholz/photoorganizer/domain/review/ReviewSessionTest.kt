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
    fun `undo returns to the previous item and restores its favorite toggle`() {
        val session = ReviewSession(listOf(first, second))
            .toggleFavorite()
            .decide(Disposition.Delete)
        assertTrue(session.canUndo)

        val undone = session.undo()

        assertEquals(first, undone.current)
        assertTrue(undone.currentIsFavorite)
        assertFalse(undone.canUndo)
        assertTrue(undone.decisions.isEmpty())
    }

    @Test
    fun `undo discards a favorite toggle on the item being left`() {
        val session = ReviewSession(listOf(first, second))
            .decide(Disposition.Keep)
            .toggleFavorite()
            .undo()
            .decide(Disposition.Keep)

        assertEquals(second, session.current)
        assertFalse(session.currentIsFavorite)
    }

    @Test
    fun `undo on a fresh session does nothing`() {
        val session = ReviewSession(listOf(first))

        assertEquals(session, session.undo())
    }
}
