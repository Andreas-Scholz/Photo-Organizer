package io.github.andreasscholz.photoorganizer.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomStateTest {

    private val box = Size(1000f, 2000f)

    /** Where a point of the unzoomed content ends up, measured from the centre of the box. */
    private fun ZoomState.positionOf(contentPoint: Offset) = contentPoint * scale + offset

    @Test
    fun `starts unzoomed`() {
        val state = ZoomState()

        assertFalse(state.isZoomed)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `pinching keeps the content under the fingers in place`() {
        val state = ZoomState()
        val centroid = Offset(200f, -300f)

        state.transform(centroid, pan = Offset.Zero, zoom = 2f, boxSize = box)

        assertEquals(2f, state.scale)
        assertEquals(centroid, state.positionOf(centroid))
    }

    @Test
    fun `zoom stays between the normal size and the maximum`() {
        val state = ZoomState(maxScale = 5f)

        state.transform(Offset.Zero, Offset.Zero, zoom = 0.5f, boxSize = box)
        assertEquals(1f, state.scale)

        state.transform(Offset.Zero, Offset.Zero, zoom = 100f, boxSize = box)
        assertEquals(5f, state.scale)
    }

    @Test
    fun `content cannot be panned beyond its edges`() {
        val state = ZoomState()
        state.transform(Offset.Zero, Offset.Zero, zoom = 2f, boxSize = box)

        state.transform(Offset.Zero, pan = Offset(10_000f, -10_000f), zoom = 1f, boxSize = box)

        // At twice the size, half the box size is hidden on each side.
        assertEquals(Offset(500f, -1000f), state.offset)
    }

    @Test
    fun `zooming out fully recentres the content`() {
        val state = ZoomState()
        state.transform(Offset(400f, 400f), Offset.Zero, zoom = 3f, boxSize = box)

        state.transform(Offset.Zero, Offset.Zero, zoom = 0.1f, boxSize = box)

        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `barely zoomed content snaps back when the fingers are lifted`() {
        val state = ZoomState()
        state.transform(Offset(100f, 100f), Offset.Zero, zoom = 1.04f, boxSize = box)
        assertTrue(state.isZoomed)

        state.settle()

        assertFalse(state.isZoomed)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `deliberately zoomed content stays zoomed when the fingers are lifted`() {
        val state = ZoomState()
        state.transform(Offset.Zero, Offset.Zero, zoom = 2f, boxSize = box)

        state.settle()

        assertEquals(2f, state.scale)
    }

    @Test
    fun `double tap zooms in on the tapped position and back out`() {
        val state = ZoomState()
        val tap = Offset(100f, 100f)

        state.toggle(tap, box)
        assertTrue(state.isZoomed)
        assertEquals(tap, state.positionOf(tap))

        state.toggle(tap, box)
        assertFalse(state.isZoomed)
        assertEquals(Offset.Zero, state.offset)
    }
}
